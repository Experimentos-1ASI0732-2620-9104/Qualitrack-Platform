package com.iotech.qualitrack.platform.shared.infrastructure.notifications.email;

import com.iotech.qualitrack.platform.shared.application.notifications.EmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * E-mail provider used by the bounded contexts: Resend when RESEND_API_KEY is set, otherwise SMTP.
 */
@Primary
@Component
public class ConfiguredEmailSender implements EmailSender {
    private final ResendEmailSender resend;
    private final SmtpEmailSender smtp;

    public ConfiguredEmailSender(ResendEmailSender resend, SmtpEmailSender smtp) {
        this.resend = resend;
        this.smtp = smtp;
    }

    @Override
    public boolean send(EmailMessage message) {
        return resend.isConfigured() ? resend.send(message) : smtp.send(message);
    }
}
