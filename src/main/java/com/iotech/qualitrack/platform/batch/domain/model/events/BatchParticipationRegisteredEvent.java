package com.iotech.qualitrack.platform.batch.domain.model.events;

/**
 * Domain event raised when an equipment or a staff member is associated with a product batch.
 *
 * @param batchId the product batch
 * @param laboratoryId the laboratory of the batch
 * @param resourceType EQUIPMENT or STAFF
 * @param resourceId the equipment or staff identifier
 * @param resourceName the equipment or staff name at registration time
 */
public record BatchParticipationRegisteredEvent(Long batchId, Long laboratoryId, String resourceType, Long resourceId,
                                                String resourceName) {
    public static final String EQUIPMENT = "EQUIPMENT";
    public static final String STAFF = "STAFF";
}
