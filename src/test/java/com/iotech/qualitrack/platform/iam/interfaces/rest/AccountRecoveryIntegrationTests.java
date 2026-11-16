package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.shared.application.notifications.EmailSender;
import com.iotech.qualitrack.platform.subscription.application.internal.outboundservices.acl.ExternalStripeService;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Account recovery and subscription renewal (EP02 US16-US17, TS05-TS06; EP03 US23, TS11).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountRecoveryIntegrationTests {
    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @MockitoBean EmailSender emails;
    @MockitoBean ExternalStripeService stripe;
    private final HttpClient http = HttpClient.newHttpClient();

    private record Account(Long id, String username, String email, String token) { }

    @BeforeEach
    void emailsAreAccepted() {
        when(emails.send(any())).thenReturn(true);
    }

    @Test
    void signUpRequiresAUniqueEmail() throws Exception {
        var username = "recovery-" + UUID.randomUUID();
        assertThat(signUp(username, null).statusCode()).isEqualTo(400);
        assertThat(signUp(username, "not-an-email").statusCode()).isEqualTo(400);
        assertThat(call("POST", "/authentication/sign-up", null, """
                {"username":"%1$s","email":"%1$s@qualitrack.test","password":"onlyletters","roles":["ROLE_QA_MANAGER"]}
                """.formatted(username)).statusCode()).isEqualTo(400);
        var created = signUp(username, username.toUpperCase() + "@QualiTrack.test");
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        assertThat(JsonPath.<String>read(created.body(), "$.email")).isEqualTo(username + "@qualitrack.test");
        var duplicated = signUp("other-" + UUID.randomUUID(), username + "@qualitrack.test");
        assertThat(duplicated.statusCode()).withFailMessage(duplicated.body()).isEqualTo(409);
    }

    @Test
    void thePasswordIsResetWithTheCodeSentToTheEmail() throws Exception {
        var account = account();
        var accepted = call("POST", "/authentication/password-recovery-requests", null,
                "{\"account\":\"" + account.email().toUpperCase() + "\"}");
        assertThat(accepted.statusCode()).withFailMessage(accepted.body()).isEqualTo(202);
        assertThat(JsonPath.<Integer>read(accepted.body(), "$.codeValidityMinutes")).isEqualTo(15);
        var message = sentTo(account.email());
        var code = code(message);
        assertThat(message.text()).contains(account.username());

        assertThat(reset(account.username(), wrong(code), "NewPassword456").statusCode()).isEqualTo(400);
        assertThat(reset(account.username(), code, "short1").statusCode()).isEqualTo(400);
        var reset = reset(account.username(), code, "NewPassword456");
        assertThat(reset.statusCode()).withFailMessage(reset.body()).isEqualTo(200);
        assertThat(JsonPath.<String>read(reset.body(), "$.username")).isEqualTo(account.username());

        assertThat(signIn(account.username(), "TestPassword123!").statusCode()).isNotEqualTo(200);
        assertThat(signIn(account.username(), "NewPassword456").statusCode()).isEqualTo(200);
        assertThat(reset(account.username(), code, "OtherPassword789").statusCode()).isEqualTo(400);
    }

    @Test
    void theAnswerDoesNotRevealWhichAccountsExist() throws Exception {
        var unknown = call("POST", "/authentication/password-recovery-requests", null,
                "{\"account\":\"nobody-" + UUID.randomUUID() + "@qualitrack.test\"}");
        assertThat(unknown.statusCode()).isEqualTo(202);
        assertThat(JsonPath.<Integer>read(unknown.body(), "$.codeValidityMinutes")).isEqualTo(15);
        verify(emails, never()).send(any());
        assertThat(call("POST", "/authentication/password-recovery-requests", null, "{\"account\":\" \"}").statusCode())
                .isEqualTo(400);
        assertThat(reset("nobody-" + UUID.randomUUID(), "123456", "NewPassword456").statusCode()).isEqualTo(400);
    }

    @Test
    void aNewCodeIsSentAtMostOnceAMinuteAndFiveWrongCodesRevokeIt() throws Exception {
        var account = account();
        for (var attempt = 0; attempt < 2; attempt++) {
            assertThat(call("POST", "/authentication/password-recovery-requests", null,
                    "{\"account\":\"" + account.username() + "\"}").statusCode()).isEqualTo(202);
        }
        var code = code(sentTo(account.email()));
        for (var attempt = 0; attempt < 5; attempt++) {
            assertThat(reset(account.email(), wrong(code), "NewPassword456").statusCode()).isEqualTo(400);
        }
        assertThat(reset(account.email(), code, "NewPassword456").statusCode()).isEqualTo(400);
        assertThat(signIn(account.username(), "TestPassword123!").statusCode()).isEqualTo(200);
    }

    @Test
    void wrongCodesSentAtTheSameTimeStillRevokeTheCode() throws Exception {
        var account = account();
        assertThat(call("POST", "/authentication/password-recovery-requests", null,
                "{\"account\":\"" + account.username() + "\"}").statusCode()).isEqualTo(202);
        var code = code(sentTo(account.email()));
        var guesses = 20;
        try (var pool = Executors.newFixedThreadPool(guesses)) {
            var start = new CountDownLatch(1);
            var answers = new ArrayList<Future<HttpResponse<String>>>();
            for (var guess = 0; guess < guesses; guess++) {
                answers.add(pool.submit(() -> {
                    start.await();
                    return reset(account.email(), wrong(code), "NewPassword456");
                }));
            }
            start.countDown();
            for (var answer : answers) assertThat(answer.get().statusCode()).isEqualTo(400);
        }
        assertThat(reset(account.email(), code, "NewPassword456").statusCode()).isEqualTo(400);
        assertThat(signIn(account.username(), "TestPassword123!").statusCode()).isEqualTo(200);
    }

    @Test
    void cancellingTheRenewalKeepsTheSubscriptionUntilThePeriodEnds() throws Exception {
        var account = account();
        var end = OffsetDateTime.now().plusDays(20);
        var unique = UUID.randomUUID().toString();
        var subscription = subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(account.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_" + unique, "sub_" + unique, "cs_" + unique,
                end.minusMonths(1).toString(), end.toString())));

        var cancelled = call("POST", "/subscriptions/" + subscription.getId() + "/cancellation-requests", account.token(), null);
        assertThat(cancelled.statusCode()).withFailMessage(cancelled.body()).isEqualTo(201);
        assertThat(JsonPath.<String>read(cancelled.body(), "$.status")).isEqualTo("ACTIVE");
        assertThat(JsonPath.<Boolean>read(cancelled.body(), "$.cancelAtPeriodEnd")).isTrue();
        assertThat(JsonPath.<String>read(cancelled.body(), "$.currentPeriodEnd")).isEqualTo(end.toString());
        assertThat(((Number) JsonPath.read(cancelled.body(), "$.cancelledBy")).longValue()).isEqualTo(account.id());
        verify(stripe).cancelRenewal("sub_" + unique);

        var onboarding = call("GET", "/users/me/onboarding", account.token(), null);
        assertThat(JsonPath.<String>read(onboarding.body(), "$.subscriptionStatus")).isEqualTo("ACTIVE");
        assertThat(call("POST", "/subscriptions/" + subscription.getId() + "/cancellation-requests", account.token(), null)
                .statusCode()).isEqualTo(409);
        verify(stripe, times(1)).cancelRenewal(any());
    }

    private EmailSender.EmailMessage sentTo(String email) {
        var captor = ArgumentCaptor.forClass(EmailSender.EmailMessage.class);
        verify(emails, atLeastOnce()).send(captor.capture());
        var messages = captor.getAllValues().stream().filter(message -> message.to().equals(email)).toList();
        assertThat(messages).hasSize(1);
        return messages.getFirst();
    }

    private static String code(EmailSender.EmailMessage message) {
        var matcher = CODE.matcher(message.text());
        assertThat(matcher.find()).withFailMessage(message.text()).isTrue();
        return matcher.group(1);
    }

    private static String wrong(String code) {
        return "%06d".formatted((Integer.parseInt(code) + 1) % 1_000_000);
    }

    private Account account() throws Exception {
        var username = "recovery-" + UUID.randomUUID();
        var email = username + "@qualitrack.test";
        var registration = signUp(username, email);
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        var response = signIn(username, "TestPassword123!");
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return new Account(((Number) JsonPath.read(response.body(), "$.id")).longValue(), username, email,
                JsonPath.read(response.body(), "$.token"));
    }

    private HttpResponse<String> signUp(String username, String email) throws Exception {
        var emailField = email == null ? "" : ",\"email\":\"" + email + "\"";
        return call("POST", "/authentication/sign-up", null, """
                {"username":"%s"%s,"password":"TestPassword123!","roles":["ROLE_QA_MANAGER"],"laboratoryId":null}
                """.formatted(username, emailField));
    }

    private HttpResponse<String> signIn(String username, String password) throws Exception {
        return call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"%s"}
                """.formatted(username, password));
    }

    private HttpResponse<String> reset(String account, String code, String newPassword) throws Exception {
        return call("POST", "/authentication/password-resets", null, """
                {"account":"%s","code":"%s","newPassword":"%s"}
                """.formatted(account, code, newPassword));
    }

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
