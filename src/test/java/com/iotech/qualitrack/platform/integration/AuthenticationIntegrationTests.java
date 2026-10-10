package com.iotech.qualitrack.platform.integration;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationIntegrationTests extends IntegrationTestSupport {
    @Test
    void signUpThenSignInReturnsAUsableJwt() throws Exception {
        var username = "auth-" + UUID.randomUUID();

        var registration = signUp(username, "TestPassword123!");
        assertThat(registration.statusCode()).isEqualTo(201);
        assertThat(registration.body()).doesNotContain("TestPassword123!").doesNotContain("\"token\"");

        var session = signIn(username, "TestPassword123!");
        assertThat(session.statusCode()).isEqualTo(200);
        assertThat(json(session, "$.username").toString()).isEqualTo(username);
        String token = json(session, "$.token");
        assertThat(token).isNotBlank();

        var onboarding = call("GET", "/users/me/onboarding", token, null);
        assertThat(onboarding.statusCode()).isEqualTo(200);
        assertThat(onboarding.body()).contains("\"nextStep\":\"SUBSCRIPTION\"");
    }

    @Test
    void wrongPasswordAndUnknownUserDoNotIssueTokens() throws Exception {
        var username = "auth-" + UUID.randomUUID();
        assertThat(signUp(username, "TestPassword123!").statusCode()).isEqualTo(201);

        var wrongPassword = signIn(username, "WrongPassword123!");
        var unknownUser = signIn("missing-" + UUID.randomUUID(), "TestPassword123!");

        assertThat(wrongPassword.statusCode()).isBetween(400, 499);
        assertThat(wrongPassword.body()).doesNotContain("\"token\"");
        assertThat(unknownUser.statusCode()).isBetween(400, 499);
        assertThat(unknownUser.body()).doesNotContain("\"token\"");
    }

    @Test
    void duplicatedUsernameIsRejected() throws Exception {
        var username = "auth-" + UUID.randomUUID();
        assertThat(signUp(username, "TestPassword123!").statusCode()).isEqualTo(201);

        assertThat(signUp(username, "OtherPassword123!").statusCode()).isBetween(400, 499);
    }

    @Test
    void protectedEndpointsRequireAValidBearerToken() throws Exception {
        assertThat(call("GET", "/users/me/onboarding", null, null).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/users/me/onboarding", "not-a-jwt", null).statusCode()).isEqualTo(401);
    }
}
