package com.iotech.qualitrack.platform.ra.domain.model.valueobjects;

/**
 * Environment of the laboratory covered by an indicator or a report section.
 *
 * @param id   environment identifier
 * @param code environment code
 * @param name environment name
 */
public record ReportEnvironment(Long id, String code, String name) {
}
