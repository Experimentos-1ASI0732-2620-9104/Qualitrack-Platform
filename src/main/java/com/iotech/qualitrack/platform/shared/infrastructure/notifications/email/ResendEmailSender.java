package com.iotech.qualitrack.platform.shared.infrastructure.notifications.email;

import com.iotech.qualitrack.platform.shared.application.notifications.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends e-mails through the Resend API ({@code POST /emails}), the provider named by the Report for the transactional
 * e-mails. It is used when RESEND_API_KEY is set; Resend works over HTTPS, so it is not affected by hosts that block
 * SMTP ports.
 *
 * <p>Without a verified domain Resend only accepts {@code onboarding@resend.dev} as sender and delivers only to the
 * address of the Resend account; QUALITRACK_MAIL_FROM sets the sender of a verified domain.</p>
 */
@Slf4j
@Component
public class ResendEmailSender implements EmailSender {
    static final String DEFAULT_FROM = "QualiTrack <onboarding@resend.dev>";

    private final String apiKey;
    private final String from;
    private final RestClient client;

    public ResendEmailSender(@Value("${qualitrack.mail.resend.api-key:}") String apiKey,
                             @Value("${qualitrack.mail.resend.base-url:https://api.resend.com}") String baseUrl,
                             @Value("${qualitrack.mail.from:}") String from) {
        this.apiKey = apiKey;
        this.from = from == null || from.isBlank() ? DEFAULT_FROM : from;
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /**
     * @return true when RESEND_API_KEY is configured
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public boolean send(EmailMessage message) {
        if (!isConfigured()) return false;
        try {
            client.post().uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", from, "to", List.of(message.to()), "subject", message.subject(),
                            "text", message.text()))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            log.warn("Resend did not accept the e-mail to {}: {}", message.to(), exception.getMessage());
            return false;
        }
    }
}
