package com.iotech.qualitrack.platform.laboratory.interfaces.rest;

import com.iotech.qualitrack.platform.testing.LimaDates;
import com.iotech.qualitrack.platform.testing.TestStaff;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the accounts of the staff registered by a quality manager: temporary credentials, first password
 * change, operator and read-only auditor permissions, deactivation and the activity history of a staff member.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffAccountIntegrationTests {
    @LocalServerPort int port;
    @Autowired SubscriptionRepository subscriptions;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final AtomicLong ruc = new AtomicLong(20900000000L);

    private record Account(Long id, String token) { }

    private record Lab(Account manager, long id, long storage) {
        String staff() { return "/laboratories/" + id + "/staff"; }
        String environment() { return "/laboratories/" + id + "/environments/" + storage; }
    }

    @Test
    void registeredStaffSignsInWithTemporaryCredentialsAndMustChangeThePassword() throws Exception {
        var lab = laboratory();
        var email = "ana.torres-" + UUID.randomUUID() + "@qualitrack.test";
        var created = call("POST", lab.staff(), lab.manager().token(), staffBody("Ana Torres", email, "operator"));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long staffId = ((Number) JsonPath.read(created.body(), "$.staffMember.id")).longValue();
        assertThat(created.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith(lab.staff() + "/" + staffId));
        assertThat(created.body()).contains("\"accessRole\":\"OPERATOR\"").contains("\"delivery\":\"SHOWN_ONCE\"")
                .contains("\"username\":\"" + email + "\"");
        String temporary = JsonPath.read(created.body(), "$.credentials.temporaryPassword");
        assertThat(temporary).hasSize(12);
        assertThat(call("GET", lab.staff() + "/" + staffId, lab.manager().token(), null).statusCode()).isEqualTo(200);

        var firstSignIn = signIn(email, temporary);
        assertThat(firstSignIn.statusCode()).isEqualTo(200);
        assertThat(firstSignIn.body()).contains("\"passwordChangeRequired\":true").contains("\"laboratoryId\":" + lab.id());
        String token = JsonPath.read(firstSignIn.body(), "$.token");
        assertThat(call("GET", "/users/me/onboarding", token, null).body()).contains("\"nextStep\":\"PASSWORD_CHANGE\"");
        var blocked = call("GET", "/laboratories/" + lab.id() + "/environments", token, null);
        assertThat(blocked.statusCode()).isEqualTo(403);
        assertThat(blocked.body()).contains("PASSWORD_CHANGE");

        assertThat(changePassword(token, "wrong-password", "NewPassword123").statusCode()).isEqualTo(400);
        assertThat(changePassword(token, temporary, "short1").statusCode()).isEqualTo(400);
        assertThat(changePassword(token, temporary, "onlyletterspassword").statusCode()).isEqualTo(400);
        assertThat(changePassword(token, temporary, "NewPassword123").statusCode()).isEqualTo(204);

        assertThat(call("GET", "/users/me/onboarding", token, null).body()).contains("\"nextStep\":\"READY\"");
        assertThat(call("GET", "/laboratories/" + lab.id() + "/environments", token, null).statusCode()).isEqualTo(200);
        assertThat(signIn(email, temporary).statusCode()).isNotEqualTo(200);
        assertThat(signIn(email, "NewPassword123").body()).contains("\"passwordChangeRequired\":false");
    }

    @Test
    void onlyQualityManagersRegisterStaffAndEmailsAreUnique() throws Exception {
        var lab = laboratory();
        var operator = TestStaff.register(this::call, lab.id(), lab.manager().token(), "Operator", "OPERATOR");

        assertThat(call("POST", lab.staff(), operator.token(), staffBody("Other", "other-" + UUID.randomUUID() + "@qualitrack.test", "OPERATOR"))
                .statusCode()).isEqualTo(403);
        assertThat(call("POST", lab.staff(), lab.manager().token(), staffBody("Duplicate", operator.email(), "AUDITOR"))
                .statusCode()).isEqualTo(409);
        assertThat(call("POST", lab.staff(), lab.manager().token(), staffBody("Bad role", "bad-" + UUID.randomUUID() + "@qualitrack.test", "ADMIN"))
                .statusCode()).isEqualTo(400);
        assertThat(call("POST", lab.staff(), lab.manager().token(), staffBody("Bad mail", "not-an-email", "OPERATOR"))
                .statusCode()).isEqualTo(400);
        var foreign = laboratory();
        assertThat(call("GET", foreign.staff(), operator.token(), null).statusCode()).isEqualTo(403);
        assertThat(call("GET", lab.staff(), operator.token(), null).statusCode()).isEqualTo(200);

        for (var role : List.of("ROLE_LAB_OPERATOR", "ROLE_AUDITOR")) {
            assertThat(call("POST", "/authentication/sign-up", null, """
                    {"username":"self-%1$s","email":"self-%1$s@qualitrack.test","password":"TestPassword123!","roles":["%2$s"],"laboratoryId":null}
                    """.formatted(UUID.randomUUID(), role)).statusCode()).isEqualTo(400);
        }
    }

    @Test
    void staffCannotManageTheSubscriptionAndAuditorsOnlyConsult() throws Exception {
        var lab = laboratory();
        var operator = TestStaff.register(this::call, lab.id(), lab.manager().token(), "Operator", "OPERATOR");
        var auditor = TestStaff.register(this::call, lab.id(), lab.manager().token(), "Auditor", "AUDITOR");

        for (var staff : List.of(operator, auditor)) {
            assertThat(call("GET", "/laboratories/" + lab.id() + "/subscriptions", staff.token(), null).statusCode()).isEqualTo(403);
            assertThat(call("POST", "/subscriptions/1/cancellation-requests", staff.token(), null).statusCode()).isEqualTo(403);
        }
        assertThat(call("GET", "/laboratories/" + lab.id() + "/subscriptions", lab.manager().token(), null).statusCode())
                .isEqualTo(200);

        long press = located(lab, "Tablet press");
        var statusChanges = lab.environment() + "/equipments/" + press + "/status-changes";
        assertThat(call("GET", lab.environment() + "/equipments/" + press + "/maintenance-records", auditor.token(), null).statusCode())
                .isEqualTo(200);
        assertThat(call("POST", statusChanges, auditor.token(), "{\"status\":\"MAINTENANCE\"}").statusCode()).isEqualTo(403);
        assertThat(call("POST", lab.environment() + "/equipments/" + press + "/maintenance-records", auditor.token(),
                maintenanceBody(auditor.staffId())).statusCode()).isEqualTo(403);
        assertThat(call("POST", statusChanges, operator.token(), "{\"status\":\"MAINTENANCE\"}").statusCode()).isEqualTo(201);
        var profile = """
                {"name":"Renamed lab %s","phone":"+51987654322","applicableRegulations":["BPA"],"address":"Lima"}
                """.formatted(UUID.randomUUID());
        assertThat(call("PUT", "/laboratories/" + lab.id(), operator.token(), profile).statusCode()).isEqualTo(403);
        assertThat(call("PUT", "/laboratories/" + lab.id(), lab.manager().token(), profile).statusCode()).isEqualTo(200);
        assertThat(call("POST", lab.environment() + "/equipments/" + press + "/maintenance-records", lab.manager().token(),
                maintenanceBody(auditor.staffId())).statusCode()).isEqualTo(400);
        assertThat(call("GET", lab.staff() + "/" + operator.staffId() + "/audit-logs", auditor.token(), null).statusCode())
                .isEqualTo(200);
    }

    @Test
    void deactivatedStaffCannotSignInAnyMore() throws Exception {
        var lab = laboratory();
        var operator = TestStaff.register(this::call, lab.id(), lab.manager().token(), "Leaving operator", "OPERATOR");
        var deactivations = lab.staff() + "/" + operator.staffId() + "/deactivations";

        assertThat(call("POST", deactivations, operator.token(), null).statusCode()).isEqualTo(403);
        var deactivated = call("POST", deactivations, lab.manager().token(), null);
        assertThat(deactivated.statusCode()).withFailMessage(deactivated.body()).isEqualTo(201);
        assertThat(deactivated.body()).contains("\"active\":false");
        assertThat(call("POST", deactivations, lab.manager().token(), null).statusCode()).isEqualTo(409);
        assertThat(signIn(operator.email(), TestStaff.PASSWORD).statusCode()).isNotEqualTo(200);
        assertThat(call("POST", lab.staff() + "/999999/deactivations", lab.manager().token(), null).statusCode()).isIn(403, 404);
    }

    @Test
    void qualityManagerSeesWhatAStaffMemberRegistered() throws Exception {
        var lab = laboratory();
        var operator = TestStaff.register(this::call, lab.id(), lab.manager().token(), "Luis Paredes", "OPERATOR");
        var activity = lab.staff() + "/" + operator.staffId() + "/audit-logs";
        assertThat(call("GET", activity, lab.manager().token(), null).body()).isEqualTo("[]");

        long press = located(lab, "Tablet press");
        var maintenance = call("POST", lab.environment() + "/equipments/" + press + "/maintenance-records", operator.token(),
                maintenanceBody(operator.staffId()));
        assertThat(maintenance.statusCode()).withFailMessage(maintenance.body()).isEqualTo(201);
        var materials = lab.environment() + "/raw-materials";
        long material = ((Number) JsonPath.read(call("POST", materials, lab.manager().token(),
                "{\"code\":\"RM-" + UUID.randomUUID().toString().substring(0, 6) + "\",\"name\":\"Lactose\",\"unit\":\"kg\",\"minimumStock\":1}").body(),
                "$.id")).longValue();
        var lot = call("POST", materials + "/" + material + "/batches", operator.token(), """
                {"supplier":"Supplier","batchNumber":"SUP-9","unit":"kg","amount":10,"receivedOn":"%s","expiresOn":"%s"}
                """.formatted(LimaDates.today().minusDays(1), LimaDates.today().plusYears(1)));
        assertThat(lot.statusCode()).withFailMessage(lot.body()).isEqualTo(201);

        var history = call("GET", activity, lab.manager().token(), null);
        assertThat(history.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(history.body(), "$[*].entityType"))
                .contains("MAINTENANCE_RECORD", "RAW_MATERIAL_BATCH").doesNotContain("RAW_MATERIAL");
        assertThat(JsonPath.<List<Integer>>read(history.body(), "$[*].performedBy")).containsOnly((int) operator.userId());

        assertThat(call("GET", activity, operator.token(), null).statusCode()).isEqualTo(403);
        var foreign = laboratory();
        assertThat(call("GET", foreign.staff() + "/" + operator.staffId() + "/audit-logs", foreign.manager().token(), null)
                .statusCode()).isIn(403, 404);
    }

    private long located(Lab lab, String name) throws Exception {
        var token = lab.manager().token();
        var created = call("POST", "/laboratories/" + lab.id() + "/equipments", token, """
                {"name":"%s","type":"Press","model":"TP-1","serialNumber":"SN-%s"}
                """.formatted(name, UUID.randomUUID().toString().substring(0, 12)));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long equipmentId = id(created);
        assertThat(call("POST", lab.environment() + "/equipments", token, "{\"equipmentId\":" + equipmentId + "}").statusCode())
                .isEqualTo(201);
        return equipmentId;
    }

    private static String maintenanceBody(long technicianStaffId) {
        return """
                {"maintenanceDate":"%s","technicianStaffId":%d,"description":"Routine check","type":"INSPECTION"}
                """.formatted(LimaDates.today(), technicianStaffId);
    }

    private static String staffBody(String fullName, String email, String accessRole) {
        return """
                {"fullName":"%s","role":"Production operator","email":"%s","accessRole":"%s"}
                """.formatted(fullName, email, accessRole);
    }

    private HttpResponse<String> signIn(String username, String password) throws Exception {
        return call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"%s"}
                """.formatted(username, password));
    }

    private HttpResponse<String> changePassword(String token, String current, String next) throws Exception {
        return call("POST", "/users/me/password-changes", token, """
                {"currentPassword":"%s","newPassword":"%s"}
                """.formatted(current, next));
    }

    private Lab laboratory() throws Exception {
        var manager = manager();
        var unique = UUID.randomUUID().toString();
        var end = OffsetDateTime.now().plusDays(10);
        subscriptions.save(Subscription.activate(new ActivateSubscriptionCommand(manager.id(), null,
                PlanCode.BASIC, BillingCycle.MONTHLY, "cus_fixture_" + unique, "sub_fixture_" + unique,
                "cs_fixture_" + unique, end.minusMonths(1).toString(), end.toString())));
        var created = call("POST", "/laboratories", manager.token(), """
                {"name":"Staff laboratory %s","ruc":"%s","address":"Test address","phone":"999123456",
                "applicableRegulations":["BPM"]}
                """.formatted(unique, ruc.incrementAndGet()));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long lab = id(created);
        var storage = call("POST", "/laboratories/" + lab + "/environments", manager.token(),
                "{\"code\":\"WH-01\",\"name\":\"Warehouse\"}");
        assertThat(storage.statusCode()).withFailMessage(storage.body()).isEqualTo(201);
        return new Lab(manager, lab, id(storage));
    }

    private Account manager() throws Exception {
        var username = "manager-" + UUID.randomUUID();
        var registration = call("POST", "/authentication/sign-up", null, """
                {"username":"%1$s","email":"%1$s@qualitrack.test","password":"TestPassword123!","roles":["ROLE_QA_MANAGER"],"laboratoryId":null}
                """.formatted(username));
        assertThat(registration.statusCode()).withFailMessage(registration.body()).isEqualTo(201);
        var response = signIn(username, "TestPassword123!");
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return new Account(((Number) JsonPath.read(response.body(), "$.id")).longValue(), JsonPath.read(response.body(), "$.token"));
    }

    private static long id(HttpResponse<String> response) {
        return ((Number) JsonPath.read(response.body(), "$.id")).longValue();
    }

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
