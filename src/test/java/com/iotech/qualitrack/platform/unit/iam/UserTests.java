package com.iotech.qualitrack.platform.unit.iam;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.entities.Role;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Roles;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.UserOnboarding;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.UserStatus;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Username;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class UserTests {
    @Test
    void newUserIsActiveWithValidatedCredentialsAndRoles() {
        var user = newOperator();

        assertThat(user.isActive()).isTrue();
        assertThat(user.getUsernameValue()).isEqualTo("lab.operator");
        assertThat(user.getRoles()).extracting(Role::getStringName).containsExactly("ROLE_LAB_OPERATOR");
        assertThat(user.getLaboratoryId()).isNull();
    }

    @Test
    void usernameMustHaveBetweenThreeAndEightyCharacters() {
        assertThatThrownBy(() -> new Username("ab")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Username(" ".repeat(5))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Username("a".repeat(81))).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Username("abc").value()).isEqualTo("abc");
    }

    @Test
    void userRequiresRolesHashAndPositiveLaboratory() {
        assertThatThrownBy(() -> new User("operator", "hash", List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User("operator", " ", List.of(new Role(Roles.ROLE_LAB_OPERATOR)), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User("operator", "hash", List.of(new Role(Roles.ROLE_LAB_OPERATOR)), 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicatedRolesAreStoredOnce() {
        var user = newOperator();
        user.addRole(new Role(Roles.ROLE_LAB_OPERATOR));
        user.addRoles(List.of(new Role(Roles.ROLE_QA_MANAGER)));

        assertThat(user.getRoles()).hasSize(2);
        assertThatThrownBy(() -> user.addRole(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userCanOnlyBelongToOneLaboratory() {
        var user = newOperator();
        user.assignLaboratory(5L);
        user.assignLaboratory(5L);

        assertThat(user.getLaboratoryId()).isEqualTo(5L);
        assertThatThrownBy(() -> user.assignLaboratory(6L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> user.assignLaboratory(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivationIsOneWay() {
        var user = newOperator();
        user.deactivate();

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.isActive()).isFalse();
        assertThatThrownBy(user::deactivate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void onboardingRequiresActiveSubscriptionBeforeLaboratory() {
        assertThat(new UserOnboarding(1L, null, null, null).nextStep()).isEqualTo("SUBSCRIPTION");
        assertThat(new UserOnboarding(1L, 3L, 2L, "CANCELLED").nextStep()).isEqualTo("SUBSCRIPTION");
        assertThat(new UserOnboarding(1L, null, 2L, "ACTIVE").nextStep()).isEqualTo("LABORATORY");
        assertThat(new UserOnboarding(1L, 3L, 2L, "ACTIVE").nextStep()).isEqualTo("READY");
    }

    private User newOperator() {
        return new User("lab.operator", "$2a$10$encodedHash", List.of(new Role(Roles.ROLE_LAB_OPERATOR)), null);
    }
}
