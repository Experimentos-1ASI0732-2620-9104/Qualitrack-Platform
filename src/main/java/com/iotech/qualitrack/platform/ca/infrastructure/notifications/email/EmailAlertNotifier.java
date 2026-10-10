package com.iotech.qualitrack.platform.ca.infrastructure.notifications.email;

import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.notifications.AlertEmailNotifier;
import com.iotech.qualitrack.platform.shared.application.notifications.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * E-mails the notice of a critical alert through the configured provider (Resend or SMTP), in Spanish and English,
 * with the link to the alert in the web application.
 */
@Component
public class EmailAlertNotifier implements AlertEmailNotifier {
    private static final Map<String, String[]> VARIABLES = Map.of(
            "AIR_QUALITY", new String[]{"Calidad del aire", "Air quality"},
            "TEMPERATURE", new String[]{"Temperatura", "Temperature"},
            "HUMIDITY", new String[]{"Humedad", "Humidity"},
            "LUMINOSITY", new String[]{"Luminosidad", "Luminosity"});
    private static final DateTimeFormatter PERU_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Lima"));

    private final EmailSender emailSender;
    private final String webUrl;

    public EmailAlertNotifier(EmailSender emailSender,
                              @Value("${application.frontend-url:http://localhost:4200}") String webUrl) {
        this.emailSender = emailSender;
        this.webUrl = webUrl.endsWith("/") ? webUrl.substring(0, webUrl.length() - 1) : webUrl;
    }

    @Override
    public boolean send(AlertNotice notice) {
        var variable = VARIABLES.getOrDefault(notice.parameterName(),
                new String[]{notice.parameterName(), notice.parameterName()});
        var environment = notice.environmentName() == null ? "—" : notice.environmentName();
        var device = notice.deviceName() == null ? "—" : notice.deviceName();
        var unit = notice.unit() == null ? "" : " " + notice.unit();
        var link = webUrl + "/alerts/deviation-detail/" + notice.alertId();
        return emailSender.send(new EmailSender.EmailMessage(notice.email(),
                "QualiTrack - Alerta crítica / Critical alert: %s · %s".formatted(variable[0], environment), """
                Hola,

                QualiTrack registró una alerta crítica que requiere atención.
                Ambiente: %1$s
                Dispositivo: %2$s
                Variable: %3$s
                Valor medido: %5$s%7$s (límite %6$s%7$s)
                Detectada: %8$s (hora de Perú)
                Revisa y reconoce la alerta en %9$s

                Hello,

                QualiTrack recorded a critical alert that needs attention.
                Environment: %1$s
                Device: %2$s
                Variable: %4$s
                Measured value: %5$s%7$s (limit %6$s%7$s)
                Detected: %8$s (Peru time)
                Review and acknowledge the alert at %9$s
                """.formatted(environment, device, variable[0], variable[1], format(notice.recordedValue()),
                format(notice.thresholdValue()), unit, peruTime(notice.detectedAt()), link)));
    }

    private static String peruTime(String instant) {
        if (instant == null) return "—";
        try {
            return PERU_TIME.format(Instant.parse(instant));
        } catch (DateTimeParseException exception) {
            return instant;
        }
    }

    private static String format(Double value) {
        if (value == null) return "—";
        return value == Math.rint(value) ? String.valueOf(value.longValue()) : String.valueOf(value);
    }
}
