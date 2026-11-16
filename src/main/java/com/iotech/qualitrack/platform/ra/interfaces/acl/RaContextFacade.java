package com.iotech.qualitrack.platform.ra.interfaces.acl;


/**
 * Inbound Anti-Corruption Layer (ACL) facade for the Reporting and Analysis bounded context.
 *
 * <p>Exposes simplified RA capabilities to other bounded contexts without
 * exposing internal command services, query services, or domain model details.</p>
 */
public interface RaContextFacade {

    /**
     * Records an audit log entry from another bounded context.
     *
     * @param action The performed audit action, for example CREATE, UPDATE, RELEASE or GENERATE.
     * @param entityType The affected entity type.
     * @param entityId The affected entity identifier, if applicable.
     * @param performedBy The actor identifier.
     * @param details Optional action details.
     * @return true if the audit log entry was recorded successfully.
     */
    boolean recordAuditLog(
            String action,
            String entityType,
            Long entityId,
            Long performedBy,
            String details
    );

    /**
     * Verifies whether an audit report exists by its ID.
     *
     * @param reportId The numeric report identifier.
     * @return true if the report exists, false otherwise.
     */
    boolean existsAuditReportById(Long reportId);

    /**
     * Verifies whether a KPI dashboard exists for a laboratory.
     *
     * @param laboratoryId The numeric laboratory identifier.
     * @return true if a KPI dashboard exists for the laboratory, false otherwise.
     */
    boolean existsKpiDashboardByLaboratoryId(Long laboratoryId);
}