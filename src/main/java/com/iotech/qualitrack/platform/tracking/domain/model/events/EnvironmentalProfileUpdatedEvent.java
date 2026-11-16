package com.iotech.qualitrack.platform.tracking.domain.model.events;

/**
 * The thresholds or actuation rules of an environmental profile changed, so Reporting &amp; Audit can record who
 * changed the configuration.
 *
 * @param profileId     changed profile
 * @param laboratoryId  laboratory that owns the profile
 * @param scope         ENVIRONMENT or CONTAINER_MONITOR
 * @param environmentId configured environment, for the ENVIRONMENT scope
 * @param deviceId      configured container monitor, for the CONTAINER_MONITOR scope
 * @param version       new version of the profile
 * @param updatedBy     user that made the change
 */
public record EnvironmentalProfileUpdatedEvent(Long profileId, Long laboratoryId, String scope, Long environmentId,
                                               Long deviceId, long version, Long updatedBy) {
}
