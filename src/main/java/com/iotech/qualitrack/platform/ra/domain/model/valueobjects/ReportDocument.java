package com.iotech.qualitrack.platform.ra.domain.model.valueobjects;

import java.util.Objects;

/**
 * Immutable value object representing the stored binary document of a report.
 *
 * <p>Combines report id, {@link ReportFormat} and the raw bytes. The {@code byte[]}
 * is defensively cloned on construction and on access so external callers can never
 * mutate the internal state — this preserves the integrity of the stored document
 * against the checksum recorded in {@code AuditReport}.
 */
public record ReportDocument(Long reportId, ReportFormat format, byte[] content) {
    public ReportDocument {
        if (reportId == null || reportId <= 0) throw new IllegalArgumentException("A report ID is required");
        Objects.requireNonNull(format, "Report format is required");
        Objects.requireNonNull(content, "Report content is required");
        if (content.length == 0) throw new IllegalArgumentException("Report content cannot be empty");
        content = content.clone();
    }

    @Override
    public byte[] content() { return content.clone(); }
}
