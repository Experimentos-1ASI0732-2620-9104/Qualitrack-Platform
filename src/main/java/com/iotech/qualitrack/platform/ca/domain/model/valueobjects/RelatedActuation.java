package com.iotech.qualitrack.platform.ca.domain.model.valueobjects;

import java.time.Instant;

/**
 * Action executed by the container monitor of an alert for the same variable while the incident was open (US86).
 *
 * @param id           the Tracking actuation event
 * @param action       the executed action (for example COOLING_ON)
 * @param triggerState the condition that triggered it, when the device reported it
 * @param result       EXECUTED or FAILED
 * @param occurredAt   when the device executed it
 */
public record RelatedActuation(Long id, String action, String triggerState, String result, Instant occurredAt) {
}
