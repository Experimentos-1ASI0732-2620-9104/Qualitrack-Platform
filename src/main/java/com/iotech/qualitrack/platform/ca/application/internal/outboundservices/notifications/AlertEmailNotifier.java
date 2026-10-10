package com.iotech.qualitrack.platform.ca.application.internal.outboundservices.notifications;

/**
 * E-mails the notice of a critical alert (US84, TS78).
 */
public interface AlertEmailNotifier {

    /**
     * @return whether the provider accepted the e-mail
     */
    boolean send(AlertNotice notice);

    /**
     * Data of the e-mail of a critical alert.
     *
     * @param email recipient
     * @param alertId the alert, to link its detail
     * @param environmentName environment of the alert
     * @param deviceName device that measured the deviation
     * @param parameterName variable, for example TEMPERATURE
     * @param recordedValue value that deviated
     * @param thresholdValue limit it crossed
     * @param unit unit of the values
     * @param detectedAt when it was detected (ISO-8601)
     */
    record AlertNotice(String email, Long alertId, String environmentName, String deviceName, String parameterName,
                       Double recordedValue, Double thresholdValue, String unit, String detectedAt) {
    }
}
