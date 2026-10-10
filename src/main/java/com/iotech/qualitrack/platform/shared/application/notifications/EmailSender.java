package com.iotech.qualitrack.platform.shared.application.notifications;

/**
 * Delivers transactional e-mails of the platform (staff credentials, password recovery codes).
 *
 * <p>The provider is an infrastructure decision: Resend when its API key is configured, otherwise the SMTP server of
 * {@code spring.mail.*}.</p>
 */
public interface EmailSender {

    /**
     * Sends a plain text e-mail.
     *
     * @param message recipient, subject and text
     * @return true when the provider accepted the message; false when no provider is configured or the delivery failed
     */
    boolean send(EmailMessage message);

    /**
     * Plain text e-mail.
     *
     * @param to recipient address
     * @param subject subject line
     * @param text body
     */
    record EmailMessage(String to, String subject, String text) {
        public EmailMessage {
            if (to == null || to.isBlank()) throw new IllegalArgumentException("The recipient is required");
            if (subject == null || subject.isBlank()) throw new IllegalArgumentException("The subject is required");
            if (text == null || text.isBlank()) throw new IllegalArgumentException("The text is required");
        }
    }
}
