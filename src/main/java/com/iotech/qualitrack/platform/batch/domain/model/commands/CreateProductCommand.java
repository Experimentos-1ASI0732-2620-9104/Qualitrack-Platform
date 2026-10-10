package com.iotech.qualitrack.platform.batch.domain.model.commands;

/**
 * Command to register a pharmaceutical product in an environment of a laboratory (US71, TS61).
 *
 * @param laboratoryId the laboratory that owns the product
 * @param environmentId the environment where the product is manufactured
 * @param code the internal catalog code, unique in the laboratory (maximum 50 characters)
 * @param name the product name, unique in the laboratory (maximum 150 characters)
 * @param description optional description (maximum 500 characters)
 * @param specifications technical specifications (maximum 1000 characters)
 */
public record CreateProductCommand(
        Long laboratoryId,
        Long environmentId,
        String code,
        String name,
        String description,
        String specifications
) {
    public CreateProductCommand {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        if (environmentId == null || environmentId <= 0) throw new IllegalArgumentException("environmentId cannot be null or less than 1");
        code = required(code, "code", 50);
        name = required(name, "name", 150);
        description = description == null || description.isBlank() ? null : limited(description.trim(), "description", 500);
        specifications = required(specifications, "specifications", 1000);
    }

    private static String required(String value, String field, int maximum) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " cannot be null or blank");
        return limited(value.trim(), field, maximum);
    }

    private static String limited(String value, String field, int maximum) {
        if (value.length() > maximum) throw new IllegalArgumentException(field + " cannot exceed " + maximum + " characters");
        return value;
    }
}
