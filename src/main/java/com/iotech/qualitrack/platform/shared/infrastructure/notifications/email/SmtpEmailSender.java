package com.iotech.qualitrack.platform.shared.infrastructure.notifications.email;

import com.iotech.qualitrack.platform.shared.application.notifications.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends e-mails through the SMTP server configured with {@code spring.mail.*} (SPRING_MAIL_HOST, SPRING_MAIL_PORT,
 * SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD), used when Resend is not configured. Without SPRING_MAIL_HOST nothing
 * is sent.
 */
@Slf4j
@Component
public class SmtpEmailSender implements EmailSender {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public SmtpEmailSender(ObjectProvider<JavaMailSender> mailSender, @Value("${qualitrack.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public boolean send(EmailMessage message) {
        var sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("No e-mail provider is configured; the e-mail to {} is not sent", message.to());
            return false;
        }
        var mail = new SimpleMailMessage();
        if (from != null && !from.isBlank()) mail.setFrom(from);
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.text());
        try {
            sender.send(mail);
            return true;
        } catch (MailException exception) {
            log.warn("The e-mail to {} could not be sent by SMTP: {}", message.to(), exception.getMessage());
            return false;
        }
    }
}
