package com.iotech.qualitrack.platform.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Groups every documented operation by the root resource of its path.
 *
 * <p>The group is the first segment after {@code /api/v1}: every path that starts with
 * {@code /api/v1/laboratories} is listed under "Laboratories", {@code /api/v1/batches} under
 * "Batches", and so on. Grouping by path instead of by bounded context keeps Swagger UI aligned
 * with the REST hierarchy, so no group is shown for a resource that no path starts with.</p>
 */
@Component
public class RootResourceTagCustomizer implements OpenApiCustomizer {

    private static final String API_PREFIX = "api";

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) return;
        var rootResources = new TreeSet<String>();
        openApi.getPaths().forEach((path, item) -> {
            var rootResource = rootResource(path);
            rootResources.add(rootResource);
            item.readOperations().forEach(operation -> operation.setTags(List.of(tagName(rootResource))));
        });
        openApi.setTags(rootResources.stream()
                .map(rootResource -> new Tag()
                        .name(tagName(rootResource))
                        .description("Resources under /%s/v1/%s".formatted(API_PREFIX, rootResource)))
                .toList());
    }

    /**
     * Returns the first path segment after the API prefix and version, for example
     * {@code raw-materials} for {@code /api/v1/raw-materials/{legacyRawMaterialId}/usages}.
     *
     * @param path the documented path
     * @return the root resource segment
     */
    static String rootResource(String path) {
        var segments = Arrays.stream(path.split("/")).filter(segment -> !segment.isBlank()).toList();
        var start = segments.size() > 2 && API_PREFIX.equals(segments.get(0)) && segments.get(1).matches("v\\d+") ? 2 : 0;
        return segments.size() > start ? segments.get(start) : "root";
    }

    /**
     * Converts a root resource segment into a readable group name, for example
     * {@code subscription-plans} into {@code Subscription Plans}.
     *
     * @param rootResource the root resource segment
     * @return the group name shown in Swagger UI
     */
    static String tagName(String rootResource) {
        return Arrays.stream(rootResource.split("-"))
                .filter(word -> !word.isBlank())
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
