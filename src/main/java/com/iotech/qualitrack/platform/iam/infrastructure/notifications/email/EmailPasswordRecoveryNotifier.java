package com.iotech.qualitrack.platform.iam.infrastructure.notifications.email;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.notifications.PasswordRecoveryNotifier;
import com.iotech.qualitrack.platform.shared.application.notifications.EmailSender;
import org.springframework.stereotype.Component;

/**
 * E-mails the verification code of a password recovery through the configured provider (Resend or SMTP).
 */
@Component
public class EmailPasswordRecoveryNotifier implements PasswordRecoveryNotifier {
    private final EmailSender emailSender;

    public EmailPasswordRecoveryNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public boolean send(RecoveryCode recoveryCode) {
        return emailSender.send(new EmailSender.EmailMessage(recoveryCode.email(),
                "QualiTrack - Código de recuperación / Recovery code", """
                Hola,

                Recibimos una solicitud para restablecer la contraseña de la cuenta %1$s en QualiTrack.
                Tu código de verificación es: %2$s
                Vence en %3$d minutos y solo puede usarse una vez. Si no solicitaste el cambio, ignora este correo:
                tu contraseña no cambiará.

                Hello,

                We received a request to reset the password of the account %1$s in QualiTrack.
                Your verification code is: %2$s
                It expires in %3$d minutes and can be used only once. If you did not request it, ignore this e-mail:
                your password will not change.
                """.formatted(recoveryCode.username(), recoveryCode.code(), recoveryCode.validity().toMinutes())));
    }
}
