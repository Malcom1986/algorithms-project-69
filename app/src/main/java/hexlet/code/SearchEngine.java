package hexlet.code;

import java.util.List;
import java.util.Map;
import hexlet.code.engine.Engine;

public class SearchEngine {
    public static List<String> search(List<Map<String, String>> docs, String needle) {
        Engine engine = Engine.getSearchEngine(docs);
        return engine.search(needle);
    }
}
