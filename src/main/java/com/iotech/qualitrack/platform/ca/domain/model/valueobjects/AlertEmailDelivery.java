package com.iotech.qualitrack.platform.ca.domain.model.valueobjects;

/**
 * Result of e-mailing an alert to the people of the laboratory.
 *
 * @param alertId the alert
 * @param recipients people who enabled e-mail notices and have an e-mail
 * @param delivered e-mails the provider accepted
 * @param sentAt when it was sent (ISO-8601)
 */
public record AlertEmailDelivery(Long alertId, int recipients, int delivered, String sentAt) {
    /**
     * @return whether the provider failed to take any of the e-mails it was given
     */
    public boolean failed() {
        return recipients > 0 && delivered == 0;
    }
}
