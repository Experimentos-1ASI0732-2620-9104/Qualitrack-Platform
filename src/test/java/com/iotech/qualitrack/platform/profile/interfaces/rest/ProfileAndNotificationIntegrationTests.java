package com.iotech.qualitrack.platform.profile.interfaces.rest;

import com.iotech.qualitrack.platform.testing.TestStaff;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchReleasedIntegrationEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Profile of each person (personal data, photo and account) and the notifications of the laboratory: the bell of the
 * web application and the e-mail notice of critical alerts (EP09: US83, US84, TS78).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileAndNotificationIntegrationTests {
    private static final byte[] PNG = Arrays.copyOf(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 64);
    private static final AtomicLong ruc = new AtomicLong(20970000000L);

    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired ApplicationEventPublisher events;
    @MockitoBean EmailSender emails;
    @MockitoBean ExternalStripeService stripe;
    private final HttpClient http = HttpClient.newHttpClient();

    private record Account(Long id, String username, String token) { }

    private record Lab(Account manager, long id, long environment, long spaceMonitor) {
        String alerts() { return "/laboratories/" + id + "/environments/" + environment + "/deviation-alerts"; }
    }

    /**
     * Without an e-mail provider the temporary password of a new staff member is returned, which is how the tests
     * sign staff members in.
     */
    @BeforeEach
    void noEmailProvider() {
        reset(emails);
    }

    @Test
    void eachPersonKeepsAProfileThatTheQualityManagerSeesInTheStaffList() throws Exception {
        var lab = laboratory();
        var manager = lab.manager();

        var empty = call("GET", "/users/me/profile", manager.token(), null);
        assertThat(empty.statusCode()).withFailMessage(empty.body()).isEqualTo(200);
        assertThat(empty.body()).contains("\"username\":\"" + manager.username() + "\"", "\"fullName\":null",
                "\"hasPhoto\":false", "\"updatedAt\":null", "\"staffId\":null");
        assertThat(call("PUT", "/users/me/profile", manager.token(), profile("Ana Torres", "1234567", null, null)).statusCode())
                .isEqualTo(400);
        var saved = call("PUT", "/users/me/profile", manager.token(),
                profile("  Ana   Torres ", "45678912", "+51 987 654 321", "Miraflores, Lima"));
        assertThat(saved.statusCode()).withFailMessage(saved.body()).isEqualTo(200);
        assertThat(saved.body()).contains("\"fullName\":\"Ana Torres\"", "\"dni\":\"45678912\"",
                "\"phoneNumber\":\"+51 987 654 321\"", "\"location\":\"Miraflores, Lima\"").doesNotContain("\"updatedAt\":null");

        var operator = TestStaff.register(this::call, lab.id(), manager.token(), "Luis Rojas", "OPERATOR");
        var registered = call("GET", "/users/me/profile", operator.token(), null);
        assertThat(registered.body()).contains("\"fullName\":\"Luis Rojas\"", "\"position\":\"Production operator\"",
                "\"staffId\":" + operator.staffId());
        assertThat(upload(operator.token(), "image/png", "not an image".getBytes()).statusCode()).isEqualTo(400);
        assertThat(upload(operator.token(), "image/gif", PNG).statusCode()).isEqualTo(415);
        assertThat(upload(operator.token(), "image/png", new byte[2 * 1024 * 1024 + 1]).statusCode()).isEqualTo(413);
        var withPhoto = upload(operator.token(), "image/png", PNG);
        assertThat(withPhoto.statusCode()).withFailMessage(withPhoto.body()).isEqualTo(200);
        assertThat(withPhoto.body()).contains("\"hasPhoto\":true");
        assertThat(call("PUT", "/users/me/profile", operator.token(),
                profile("Luis Rojas Vega", null, null, "Callao")).statusCode()).isEqualTo(200);

        var staffProfile = "/laboratories/" + lab.id() + "/staff/" + operator.staffId() + "/profile";
        var seen = call("GET", staffProfile, manager.token(), null);
        assertThat(seen.statusCode()).withFailMessage(seen.body()).isEqualTo(200);
        assertThat(seen.body()).contains("\"fullName\":\"Luis Rojas Vega\"", "\"location\":\"Callao\"", "\"hasPhoto\":true",
                "\"email\":\"" + operator.email() + "\"");
        var photo = download(staffProfile + "/photo", manager.token());
        assertThat(photo.statusCode()).isEqualTo(200);
        assertThat(photo.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(photo.body()).isEqualTo(PNG);
        assertThat(call("GET", "/laboratories/" + lab.id() + "/staff/" + operator.staffId(), manager.token(), null).body())
                .contains("\"fullName\":\"Luis Rojas Vega\"");

        var auditor = TestStaff.register(this::call, lab.id(), manager.token(), "Rosa Diaz", "AUDITOR");
        assertThat(call("PUT", "/users/me/profile", auditor.token(), profile("Rosa Diaz Paz", null, null, null)).statusCode())
                .isEqualTo(200);
        assertThat(call("GET", staffProfile, auditor.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", staffProfile, operator.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", staffProfile, laboratory().manager().token(), null).statusCode()).isEqualTo(403);

        assertThat(call("DELETE", "/users/me/profile/photo", operator.token(), null).statusCode()).isEqualTo(204);
        assertThat(download("/users/me/profile/photo", operator.token()).statusCode()).isEqualTo(404);
        assertThat(call("DELETE", "/users/me/profile/photo", operator.token(), null).statusCode()).isEqualTo(204);
    }

    @Test
    void aPersonChangesTheUsernameAndEmailOfTheAccountWithTheCurrentPassword() throws Exception {
        var lab = laboratory();
        var manager = lab.manager();
        var operator = TestStaff.register(this::call, lab.id(), manager.token(), "Carla Soto", "OPERATOR");
        var newEmail = "carla-" + UUID.randomUUID() + "@labsur.test";

        assertThat(call("PUT", "/users/me", operator.token(), account("carla.soto", newEmail, "WrongPassword1")).statusCode())
                .isEqualTo(400);
        assertThat(call("PUT", "/users/me", operator.token(),
                account("carla.soto", manager.username() + "@qualitrack.test", TestStaff.PASSWORD)).statusCode()).isEqualTo(409);
        assertThat(call("PUT", "/users/me", operator.token(),
                account(manager.username(), newEmail, TestStaff.PASSWORD)).statusCode()).isEqualTo(409);

        var username = "carla.soto." + UUID.randomUUID().toString().substring(0, 6);
        var updated = call("PUT", "/users/me", operator.token(), account(username, newEmail.toUpperCase(), TestStaff.PASSWORD));
        assertThat(updated.statusCode()).withFailMessage(updated.body()).isEqualTo(200);
        String token = JsonPath.read(updated.body(), "$.token");
        assertThat(updated.body()).contains("\"username\":\"" + username + "\"");
        assertThat(call("GET", "/users/me/profile", operator.token(), null).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/users/me", token, null).body()).contains("\"email\":\"" + newEmail + "\"");
        assertThat(call("GET", "/laboratories/" + lab.id() + "/staff/" + operator.staffId(), manager.token(), null).body())
                .contains("\"email\":\"" + newEmail + "\"");
        var signIn = call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"%s"}
                """.formatted(username, TestStaff.PASSWORD));
        assertThat(signIn.statusCode()).withFailMessage(signIn.body()).isEqualTo(200);

        // Someone else may now take the released username; the old token never reaches that account.
        var taken = call("POST", "/authentication/sign-up", null, """
                {"username":"%s","email":"other-%s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"]}
                """.formatted(operator.email(), UUID.randomUUID()));
        assertThat(taken.statusCode()).withFailMessage(taken.body()).isEqualTo(201);
        assertThat(call("GET", "/users/me", operator.token(), null).statusCode()).isEqualTo(401);
    }

    @Test
    void theLaboratoryIsNotifiedOfWhatOthersDoWithItsAlertsAndBatches() throws Exception {
        var lab = laboratory();
        var manager = lab.manager();
        var operator = TestStaff.register(this::call, lab.id(), manager.token(), "Pedro Quispe", "OPERATOR");
        var auditor = TestStaff.register(this::call, lab.id(), manager.token(), "Elena Ruiz", "AUDITOR");
        when(emails.send(any())).thenReturn(true);
        clearInvocations(emails);
        assertThat(call("PUT", "/users/me/profile", operator.token(), profile("Pedro Quispe", null, null, null)).statusCode())
                .isEqualTo(200);
        var preferences = call("PUT", "/users/me/notification-preferences", auditor.token(), """
                {"emailEnabled":false,"inAppEnabled":true,"minimumSeverity":"CRITICAL"}
                """);
        assertThat(preferences.statusCode()).withFailMessage(preferences.body()).isEqualTo(200);
        assertThat(call("GET", "/users/me/notification-preferences", auditor.token(), null).body())
                .contains("\"minimumSeverity\":\"CRITICAL\"", "\"emailEnabled\":false").doesNotContain("sms");

        var detectedAt = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        var opened = call("POST", lab.alerts(), manager.token(), """
                {"deviceId":null,"parameterName":"AIR_QUALITY","recordedValue":850.0,"thresholdValue":800.0,"unit":"ppm","severity":"WARNING","detectedAt":"%s"}
                """.formatted(detectedAt));
        assertThat(opened.statusCode()).withFailMessage(opened.body()).isEqualTo(201);
        long alertId = ((Number) JsonPath.read(opened.body(), "$.id")).longValue();

        var operatorBell = call("GET", "/users/me/notifications", operator.token(), null);
        assertThat(operatorBell.statusCode()).withFailMessage(operatorBell.body()).isEqualTo(200);
        assertThat(operatorBell.body()).contains("\"type\":\"ALERT_OPENED\"", "\"severity\":\"WARNING\"",
                "\"subjectType\":\"ALERT\"", "\"subjectId\":" + alertId, "\"parameterName\":\"AIR_QUALITY\"",
                "\"environmentName\":\"Zone ", "\"subjectName\":\"Space monitor\"", "\"readAt\":null");
        assertThat(unread(auditor.token())).isZero();
        assertThat(call("POST", "/deviation-alerts/" + alertId + "/email-notifications", manager.token(), null).statusCode())
                .isEqualTo(409);
        verify(emails, after(300).never()).send(any());

        var acknowledged = call("POST", "/deviation-alerts/" + alertId + "/acknowledgements", operator.token(), null);
        assertThat(acknowledged.statusCode()).isEqualTo(201);
        var managerBell = call("GET", "/users/me/notifications?unread=true", manager.token(), null);
        assertThat(JsonPath.<List<String>>read(managerBell.body(), "$[*].type")).containsExactly("ALERT_ACKNOWLEDGED", "ALERT_OPENED");
        assertThat(managerBell.body()).contains("\"actorName\":\"Pedro Quispe\"");
        assertThat(JsonPath.<List<String>>read(call("GET", "/users/me/notifications", operator.token(), null).body(), "$[*].type"))
                .containsExactly("ALERT_OPENED");

        var escalated = call("POST", lab.alerts(), manager.token(), """
                {"deviceId":%d,"parameterName":"AIR_QUALITY","recordedValue":1300.0,"thresholdValue":1200.0,"unit":"ppm","severity":"CRITICAL","detectedAt":"%s"}
                """.formatted(lab.spaceMonitor(), detectedAt.plusSeconds(60)));
        assertThat(escalated.statusCode()).isEqualTo(200);
        assertThat(unread(auditor.token())).isEqualTo(1);
        var message = ArgumentCaptor.forClass(EmailSender.EmailMessage.class);
        verify(emails, timeout(3000).times(2)).send(message.capture());
        assertThat(message.getAllValues()).extracting(EmailSender.EmailMessage::to)
                .containsExactlyInAnyOrder(manager.username() + "@qualitrack.test", operator.email());
        assertThat(message.getValue().subject()).contains("Alerta crítica", "Critical alert");
        assertThat(message.getValue().text()).contains("1300", "/alerts/deviation-detail/" + alertId);

        var resent = call("POST", "/deviation-alerts/" + alertId + "/email-notifications", manager.token(), null);
        assertThat(resent.statusCode()).withFailMessage(resent.body()).isEqualTo(201);
        assertThat(resent.body()).contains("\"recipients\":1", "\"delivered\":1");
        assertThat(call("POST", "/deviation-alerts/" + alertId + "/email-notifications", auditor.token(), null).statusCode())
                .isEqualTo(403);
        when(emails.send(any())).thenReturn(false);
        assertThat(call("POST", "/deviation-alerts/" + alertId + "/email-notifications", manager.token(), null).statusCode())
                .isEqualTo(502);

        events.publishEvent(new BatchReleasedIntegrationEvent(9001L, lab.id(), 1L, "PB-2026-001", "2026-10-04",
                manager.id()));
        var released = call("GET", "/users/me/notifications?limit=1", operator.token(), null);
        assertThat(released.body()).contains("\"type\":\"BATCH_RELEASED\"", "\"subjectName\":\"PB-2026-001\"",
                "\"subjectType\":\"BATCH\"");
        assertThat(call("GET", "/users/me/notifications", manager.token(), null).body()).doesNotContain("BATCH_RELEASED");

        long notificationId = ((Number) JsonPath.read(released.body(), "$[0].id")).longValue();
        assertThat(call("POST", "/users/me/notifications/" + notificationId + "/read-receipts", manager.token(), null)
                .statusCode()).isEqualTo(404);
        var read = call("POST", "/users/me/notifications/" + notificationId + "/read-receipts", operator.token(), null);
        assertThat(read.statusCode()).withFailMessage(read.body()).isEqualTo(201);
        assertThat(read.body()).doesNotContain("\"readAt\":null");
        assertThat(unread(operator.token())).isEqualTo(2);
        var all = call("POST", "/users/me/notifications/read-receipts", operator.token(), null);
        assertThat(all.body()).contains("\"markedAsRead\":2");
        assertThat(unread(operator.token())).isZero();
        assertThat(call("GET", "/users/me/notifications?limit=0", operator.token(), null).statusCode()).isEqualTo(400);
        assertThat(call("POST", "/users/me/notifications/read-receipts", auditor.token(), null).statusCode()).isEqualTo(201);
    }

    private long unread(String token) throws Exception {
        var response = call("GET", "/users/me/notifications/unread-count", token, null);
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return ((Number) JsonPath.read(response.body(), "$.unreadCount")).longValue();
    }

    private static String profile(String fullName, String dni, String phoneNumber, String location) {
        return """
                {"fullName":%s,"dni":%s,"phoneNumber":%s,"location":%s}
                """.formatted(json(fullName), json(dni), json(phoneNumber), json(location));
    }

    private static String account(String username, String email, String currentPassword) {
        return """
                {"username":"%s","email":"%s","currentPassword":"%s"}
                """.formatted(username, email, currentPassword);
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private Lab laboratory() throws Exception {
        var manager = manager();
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Profile laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long lab = id(created);
        var code = "WH-" + suffix();
        var environment = call("POST", "/laboratories/" + lab + "/environments", manager.token(),
                "{\"code\":\"" + code + "\",\"name\":\"Zone " + code + "\"}");
        assertThat(environment.statusCode()).withFailMessage(environment.body()).isEqualTo(201);
        long environmentId = id(environment);
        long spaceMonitor = id(call("POST", "/laboratories/" + lab + "/devices/environmental-devices", manager.token(), """
                {"name":"Space monitor","sensorExternalId":"ENV-%s","serialNumber":"MAC-%s","model":"ESP32-WROOM-32","firmwareVersion":"1.0.3"}
                """.formatted(suffix(), suffix())));
        assertThat(call("POST", "/laboratories/" + lab + "/environments/" + environmentId + "/environmental-devices",
                manager.token(), "{\"deviceId\":" + spaceMonitor + "}").statusCode()).isEqualTo(201);
        return new Lab(manager, lab, environmentId, spaceMonitor);
    }

    private Account manager() throws Exception {
        var username = "profile-" + UUID.randomUUID();
        var registration = call("POST", "/authentication/sign-up", null, """
                {"username":"%1$s","email":"%1$s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"],"laboratoryId":null}
                """.formatted(username));
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        var response = call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"TestPassword123!"}
                """.formatted(username));
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return new Account(((Number) JsonPath.read(response.body(), "$.id")).longValue(), username,
                JsonPath.read(response.body(), "$.token"));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static long id(HttpResponse<String> response) {
        return ((Number) JsonPath.read(response.body(), "$.id")).longValue();
    }

    private HttpResponse<String> upload(String token, String contentType, byte[] image) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/users/me/profile/photo"))
                .header("Content-Type", contentType)
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(image));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> download(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Authorization", "Bearer " + token).GET();
        return http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
