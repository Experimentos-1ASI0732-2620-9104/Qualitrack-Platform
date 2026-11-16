package com.iotech.qualitrack.platform.shared.infrastructure.documentation;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that Swagger UI groups operations by the root resource of their path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocumentationTests {
    @LocalServerPort int port;

    @Test
    void groupsEveryOperationByTheFirstSegmentAfterTheApiVersion() throws Exception {
        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var body = response.body();

        Map<String, Map<String, Object>> paths = JsonPath.read(body, "$.paths");
        assertThat(paths).isNotEmpty();
        paths.forEach((path, operations) -> {
            var expected = expectedTag(path);
            operations.forEach((method, operation) -> {
                @SuppressWarnings("unchecked")
                var tags = (List<String>) ((Map<String, Object>) operation).get("tags");
                assertThat(tags).as("%s %s", method, path).containsExactly(expected);
            });
        });

        List<String> tagNames = JsonPath.read(body, "$.tags[*].name");
        assertThat(tagNames).contains("Laboratories").doesNotContain("Inventory", "Environments", "Equipment");
        assertThat(tagNames).containsExactlyElementsOf(tagNames.stream().sorted().toList());
        assertThat(paths.keySet())
                .filteredOn(path -> path.startsWith("/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/raw-materials"))
                .isNotEmpty()
                .allSatisfy(path -> assertThat(expectedTag(path)).isEqualTo("Laboratories"));
    }

    private static String expectedTag(String path) {
        var root = path.replaceFirst("^/api/v1/", "").split("/")[0];
        var words = root.split("-");
        var name = new StringBuilder();
        for (var word : words) {
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }
}
