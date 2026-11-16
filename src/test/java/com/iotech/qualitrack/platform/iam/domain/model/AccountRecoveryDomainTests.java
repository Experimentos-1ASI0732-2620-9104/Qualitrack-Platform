package com.iotech.qualitrack.platform.iam.domain.model;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.PasswordRecovery;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.EmailAddress;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.PasswordRecoveryStatus;
import com.iotech.qualitrack.platform.shared.application.notifications.EmailSender;
import com.iotech.qualitrack.platform.shared.infrastructure.notifications.email.ConfiguredEmailSender;
import com.iotech.qualitrack.platform.shared.infrastructure.notifications.email.ResendEmailSender;
import com.iotech.qualitrack.platform.shared.infrastructure.notifications.email.SmtpEmailSender;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Rules of the password recovery codes, the account e-mail and the e-mail provider.
 */
class AccountRecoveryDomainTests {
    private static final Instant NOW = Instant.parse("2026-10-04T15:00:00Z");

    @Test
    void theCodeExpiresAfterFifteenMinutesAndIsUsedOnce() {
        var recovery = PasswordRecovery.start(7L, "hash", NOW);
        assertThat(recovery.isUsable(NOW.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(recovery.isUsable(NOW.plus(Duration.ofMinutes(15)))).isFalse();
        recovery.complete(NOW.plusSeconds(30));
        assertThat(recovery.getStatus()).isEqualTo(PasswordRecoveryStatus.COMPLETED);
        assertThat(recovery.isUsable(NOW.plusSeconds(40))).isFalse();
        assertThatThrownBy(() -> recovery.complete(NOW.plusSeconds(50))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fiveWrongCodesRevokeTheRecovery() {
        var recovery = PasswordRecovery.start(7L, "hash", NOW);
        for (var attempt = 0; attempt < 4; attempt++) recovery.registerFailedAttempt();
        assertThat(recovery.isUsable(NOW)).isTrue();
        recovery.registerFailedAttempt();
        assertThat(recovery.getStatus()).isEqualTo(PasswordRecoveryStatus.REVOKED);
        assertThat(recovery.isUsable(NOW)).isFalse();
    }

    @Test
    void aNewCodeWaitsOneMinuteAndReplacesThePreviousOne() {
        var recovery = PasswordRecovery.start(7L, "hash", NOW);
        assertThat(recovery.blocksNewCode(NOW.plusSeconds(59))).isTrue();
        assertThat(recovery.blocksNewCode(NOW.plusSeconds(60))).isFalse();
        recovery.revoke();
        assertThat(recovery.isUsable(NOW.plusSeconds(61))).isFalse();
    }

    @Test
    void theEmailIsNormalizedAndValidated() {
        assertThat(new EmailAddress("  Ana.Torres@Laboratorio.PE ").value()).isEqualTo("ana.torres@laboratorio.pe");
        assertThatThrownBy(() -> new EmailAddress("ana.torres")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmailAddress(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(EmailAddress.looksLikeEmail("qa-manager")).isFalse();
    }

    @Test
    void resendReceivesTheMessageWithTheApiKey() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var authorization = new AtomicReference<String>();
        var body = new AtomicReference<String>();
        server.createContext("/emails", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var answer = "{\"id\":\"email-1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        server.start();
        try {
            var resend = new ResendEmailSender("re_test_key", "http://127.0.0.1:" + server.getAddress().getPort(), "");
            var smtp = mock(SmtpEmailSender.class);
            var sent = new ConfiguredEmailSender(resend, smtp)
                    .send(new EmailSender.EmailMessage("ana@laboratorio.pe", "Código", "Tu código es 123456"));
            assertThat(sent).isTrue();
            assertThat(authorization.get()).isEqualTo("Bearer re_test_key");
            assertThat(body.get()).contains("\"to\":[\"ana@laboratorio.pe\"]").contains("onboarding@resend.dev")
                    .contains("Tu código es 123456");
            verifyNoInteractions(smtp);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void withoutResendKeyTheMessageGoesBySmtp() {
        var resend = new ResendEmailSender("", "http://127.0.0.1:9", "");
        var smtp = mock(SmtpEmailSender.class);
        var message = new EmailSender.EmailMessage("ana@laboratorio.pe", "Código", "Tu código es 123456");
        when(smtp.send(message)).thenReturn(true);
        assertThat(new ConfiguredEmailSender(resend, smtp).send(message)).isTrue();
        assertThat(resend.send(message)).isFalse();
    }
}
