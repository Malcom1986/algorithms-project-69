package hexlet.code;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class SearchEngineTest {

    private Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName).toAbsolutePath().normalize();
    }

    private Map<String, String> getDocumentContent(String id) {
        Path path = getFixturePath(id);
        String text = null;
        try {
            text = Files.readString(path);
        } catch (Exception e) {
            throw new RuntimeException("File " + id + " not found");
        }
        return Map.of("id", id, "text", text);
    }

    @Test
    public void testSearch() {
        List<String> ids = List.of("garbage_patch_NG", "garbage_patch_ocean_clean", "garbage_patch_wiki");
        var searchText = "trash island";
        List<Map<String, String>> docs = ids.parallelStream().map(this::getDocumentContent).toList();
        List<String> actual = SearchEngine.search(docs, searchText);

        assertThat(actual).isEqualTo(ids);
    }

    @Test
    public void testSearchWithSpam() {
        List<String> ids = List.of(
            "garbage_patch_NG",
            "garbage_patch_ocean_clean",
            "garbage_patch_wiki",
            "garbage_patch_spam"
        );
        var searchText = "the trash island is a";
        List<Map<String, String>> docs = ids.parallelStream().map(this::getDocumentContent).toList();
        List<String> actual = SearchEngine.search(docs, searchText);

        assertThat(actual).isEqualTo(ids);
    }

    @Test
    public void testSearchWithShortText() {
        var doc1 = "I can't shoot straight unless I've had a pint!";
        var doc2 = "Don't shoot shoot shoot that thing at me.";
        var doc3 = "I'm your shooter.";
        List<Map<String, String>> docs = List.of(
            Map.of("id", "doc1", "text", doc1),
            Map.of("id", "doc2", "text", doc2),
            Map.of("id", "doc3", "text", doc3));

        List<String> expected = List.of("doc2", "doc1");

        List<String> actual = SearchEngine.search(docs, "shoot at me, nerd");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testSearchWithEmptyDocs() {
        List<Map<String, String>> docs = new ArrayList<>();

        List<String> actual = SearchEngine.search(docs, "");
        assertThat(actual).isEmpty();
    }
}
