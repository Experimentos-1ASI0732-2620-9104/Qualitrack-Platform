package com.iotech.qualitrack.platform.testing;

import com.jayway.jsonpath.JsonPath;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registers staff members through the API the way a quality manager does, and signs them in after replacing
 * the temporary password, so integration tests act as real operators and auditors.
 */
public final class TestStaff {
    public static final String PASSWORD = "StaffPassword123";

    private TestStaff() {
    }

    /** Sends a request to /api/v1 with an optional bearer token. */
    @FunctionalInterface
    public interface Api {
        HttpResponse<String> call(String method, String path, String token, String body) throws Exception;
    }

    /**
     * A signed-in staff member.
     *
     * @param staffId staff member identifier in the laboratory
     * @param userId account identifier
     * @param email e-mail, also the username
     * @param token bearer token after the password change
     */
    public record Member(long staffId, long userId, String email, String token) {
    }

    /**
     * Registers a staff member with a temporary password, signs in and replaces the password.
     *
     * @param accessRole OPERATOR or AUDITOR
     */
    public static Member register(Api api, long laboratoryId, String managerToken, String fullName, String accessRole) throws Exception {
        var email = "staff-" + UUID.randomUUID() + "@qualitrack.test";
        var created = api.call("POST", "/laboratories/" + laboratoryId + "/staff", managerToken, """
                {"fullName":"%s","role":"Production operator","email":"%s","accessRole":"%s"}
                """.formatted(fullName, email, accessRole));
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        long staffId = ((Number) JsonPath.read(created.body(), "$.staffMember.id")).longValue();
        long userId = ((Number) JsonPath.read(created.body(), "$.staffMember.userId")).longValue();
        String temporaryPassword = JsonPath.read(created.body(), "$.credentials.temporaryPassword");
        var signIn = api.call("POST", "/authentication/sign-in", null, """
                {"username":"%s","password":"%s"}
                """.formatted(email, temporaryPassword));
        assertThat(signIn.statusCode()).withFailMessage(signIn.body()).isEqualTo(200);
        String token = JsonPath.read(signIn.body(), "$.token");
        var changed = api.call("POST", "/users/me/password-changes", token, """
                {"currentPassword":"%s","newPassword":"%s"}
                """.formatted(temporaryPassword, PASSWORD));
        assertThat(changed.statusCode()).withFailMessage(changed.body()).isEqualTo(204);
        return new Member(staffId, userId, email, token);
    }
}
