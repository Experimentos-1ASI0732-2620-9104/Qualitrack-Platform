package com.iotech.qualitrack.platform.ra.application.internal.outboundservices;

import java.time.Instant;
import java.util.List;

/** Current equipment configuration and dated activity records, kept as separate evidence. */
public record EquipmentReportData(Long equipmentId, String name, String laboratory, String type,
                                  String model, String serialNumber, String status, String sensorId,
                                  String from, String to, List<Parameter> parameters,
                                  List<Maintenance> maintenance, List<LogEntry> entries,
                                  String generatedBy, Instant generatedAt) {
    public EquipmentReportData {
        parameters = List.copyOf(parameters);
        maintenance = List.copyOf(maintenance);
        entries = List.copyOf(entries);
    }

    /**
     * Configured limit for one parameter at export time. {@code minimum}/{@code maximum}
     * are null when the limit is unbounded on that side.
     */
    public record Parameter(String name, Double minimum, Double maximum, String unit) {}

    /** Maintenance intervention performed within the report period. */
    public record Maintenance(Long id, String date, String type, String technician, String description) {}

    /**
     * One audit trail entry, filtered to the report period.
     * {@code actorId == null} means "system"; {@code action} is the enum name as a string.
     */
    public record LogEntry(Long id, String recordedAt, String action, Long actorId, String details) {}
}
