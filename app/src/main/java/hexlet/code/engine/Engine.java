package hexlet.code.engine;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.Comparator;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Engine {

    private Map<String, List<Map<String, Object>>> index;

    Engine(Map<String, List<Map<String, Object>>> index) {
        this.index = index;
    }

    private static Map<String, List<Map<String, Object>>> merge(
        Map<String, ?> object1,
        Map<String, ?> object2) {

        Set<String> keys = new TreeSet<>(object1.keySet());
        keys.addAll(object2.keySet());

        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        for (var key: keys) {
            if (object1.containsKey(key) && object2.containsKey(key)) {
                var value1 = object1.get(key);
                var value2 = object2.get(key);
                var valuesJoint = new ArrayList<Map<String, Object>>();
                if (value1 instanceof List<?>) {
                    valuesJoint.addAll((List<Map<String, Object>>) value1);
                    valuesJoint.add((Map<String, Object>) value2);
                } else {
                    valuesJoint.add((Map<String, Object>) value1);
                    valuesJoint.add((Map<String, Object>) value2);
                }
                result.put(key, valuesJoint);
            } else if (object1.containsKey(key)) {
                var value = object1.get(key);
                List<Map<String, Object>> normalizedValue;
                if (value instanceof List<?>) {
                    normalizedValue = (List<Map<String, Object>>) value;
                } else {
                    normalizedValue = new ArrayList<>();
                    normalizedValue.add((Map<String, Object>) value);
                }
                result.put(key, normalizedValue);
            } else {
                var value = object2.get(key);
                List<Map<String, Object>> normalizedValue;
                if (value instanceof List<?>) {
                    normalizedValue = (List<Map<String, Object>>) value;
                } else {
                    normalizedValue = new ArrayList<>();
                    normalizedValue.add((Map<String, Object>) value);
                }
                result.put(key, normalizedValue);
            }
        }

        return result;
    }

    private static String normalizeToken(String word) {
        return Pattern.compile("\\w+")
                .matcher(word)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.joining())
                .toLowerCase();
    }

    private static Map<String, Long> buildInvertedIndex(List<String> terms) {
        return terms.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    private static double calcIDF(int docsCount, int termCount) {
        return log2((1 + (docsCount - termCount + 1) / (termCount + 0.5)));
    }

    public static Engine getSearchEngine(List<Map<String, String>> documents) {

        final var docsCount = documents.size();
        Map<String, List<String>> docTerms = documents.stream().collect(Collectors.toMap(
                doc -> doc.get("id"),
                doc -> {
                    var lines = doc.get("text").split("\n");
                    return Arrays.stream(lines).flatMap(line -> Arrays.stream(line.split(" ")))
                            .map(Engine::normalizeToken)
                            .filter(Objects::nonNull).filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                }
        ));

        var invertedIndexes = documents.stream().map(doc -> {
            var terms = docTerms.get(doc.get("id"));
            var docInvertedIndex = buildInvertedIndex(terms);
            return docInvertedIndex.keySet().stream().collect(Collectors.toMap(
                    Function.identity(),
                    term -> {
                        var termCount = (double) docInvertedIndex.get(term);
                        var termFrequency = termCount / terms.size();
                        Map<String, Object> docTerm = new HashMap<>();
                        docTerm.put("docId", doc.get("id"));
                        docTerm.put("termFrequency", termFrequency);
                        docTerm.put("count", termCount);
                        return docTerm;
                    }
            ));
        }).toList();

        Map<String, List<Map<String, Object>>> index = new HashMap<>();

        for (var invertedIndex: invertedIndexes) {
            index = merge(index, invertedIndex);
        }

        Map<String, List<Map<String, Object>>> finalIndex = index;

        index.keySet().forEach(term -> {
            final List<Map<String, Object>> termDocs = finalIndex.get(term);
            var termDocsCount = termDocs.size();

            termDocs.forEach(doc -> {
                var termFrequency = (double) doc.get("termFrequency");
                var docIdf = calcIDF(docsCount, termDocsCount);
                var tfIDF = termFrequency * docIdf;
                doc.put("tfIDF", tfIDF);
            });
        });

        return new Engine(index);
    }

    public List<String> search(String needle) {
        List<String> terms = Arrays.stream(needle.split(" "))
                .map(Engine::normalizeToken)
                .filter(Objects::nonNull).toList();

        Map<String, List<Map<String, Object>>> currentIndex = index.entrySet().stream()
                .filter(entry -> terms.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, List<Map<String, Object>>> groupByDocId = currentIndex.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(doc -> doc.get("docId").toString()));

        Set<String> currentDocsIds = groupByDocId.keySet();

        var weightedDocs = currentDocsIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        docId -> {
                            List<Map<String, Object>> values = groupByDocId.get(docId);
                            return values.stream().mapToDouble(v -> (double) v.get("tfIDF")).sum();
                        })
                );

        return currentDocsIds.stream()
                .sorted(Comparator.comparing(weightedDocs::get).reversed())
                .collect(Collectors.toList());

    }
}

