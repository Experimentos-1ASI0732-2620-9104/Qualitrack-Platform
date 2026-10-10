package com.iotech.qualitrack.platform.iam.infrastructure.notifications.email;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.CredentialsNotifier;
import com.iotech.qualitrack.platform.shared.application.notifications.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * E-mails the staff credentials through the configured provider (Resend or SMTP). Without a provider, or when the
 * delivery fails, nothing is sent and the quality manager receives the temporary password once to hand it over.
 */
@Component
public class EmailCredentialsNotifier implements CredentialsNotifier {
    private final EmailSender emailSender;
    private final String signInUrl;

    public EmailCredentialsNotifier(EmailSender emailSender,
                                    @Value("${qualitrack.web.sign-in-url:${application.frontend-url:http://localhost:4200}/iam/sign-in}") String signInUrl) {
        this.emailSender = emailSender;
        this.signInUrl = signInUrl;
    }

    @Override
    public boolean send(StaffCredentials credentials) {
        return emailSender.send(new EmailSender.EmailMessage(credentials.email(),
                "QualiTrack - Credenciales de acceso / Access credentials", """
                Hola %1$s,

                Tu responsable de calidad te registró en QualiTrack.
                Usuario: %2$s
                Contraseña temporal: %3$s
                Ingresa en %4$s; al iniciar sesión deberás definir una nueva contraseña.

                Hello %1$s,

                Your quality manager registered you in QualiTrack.
                Username: %2$s
                Temporary password: %3$s
                Sign in at %4$s; you will be asked to choose a new password.
                """.formatted(credentials.fullName(), credentials.username(), credentials.temporaryPassword(), signInUrl)));
    }
}
