package com.iotech.qualitrack.platform.iam.domain.model.aggregates;

import com.iotech.qualitrack.platform.iam.domain.model.entities.Role;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.EmailAddress;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.PasswordHash;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.UserStatus;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Username;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User aggregate root.
 *
 * <p>Represents an authenticated platform account with assigned authorization
 * roles and an optional laboratory association.</p>
 */
@Getter
public class User extends AbstractDomainAggregateRoot<User> {

    /**
     * User numeric identifier.
     */
    @Setter
    private Long id;

    /**
     * Account username.
     */
    private Username username;

    /**
     * E-mail where the platform sends credentials and recovery codes; null for accounts registered before it was
     * required.
     */
    private EmailAddress email;

    /**
     * Hashed account password.
     */
    private PasswordHash password;

    /**
     * Assigned authorization roles.
     */
    private Set<Role> roles;

    /**
     * Laboratory associated with the account.
     */
    private Long laboratoryId;

    /**
     * User lifecycle status.
     */
    private UserStatus status;

    /**
     * Whether the user signs in with a temporary password that must be replaced before using the platform.
     */
    private boolean passwordChangeRequired;

    /**
     * Required empty constructor for reconstruction.
     */
    public User() {
        this.roles = new HashSet<>();
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Creates a new active user.
     *
     * @param username account username
     * @param password hashed account password
     * @param roles assigned role collection
     * @param laboratoryId associated laboratory identifier
     */
    public User(
            String username,
            String password,
            Collection<Role> roles,
            Long laboratoryId
    ) {
        this(username, null, password, roles, laboratoryId);
    }

    /**
     * Creates a new active user with the e-mail used to recover its password.
     *
     * @param username account username
     * @param email account e-mail, or null for accounts registered before it was required
     * @param password hashed account password
     * @param roles assigned role collection
     * @param laboratoryId associated laboratory identifier
     */
    public User(String username, String email, String password, Collection<Role> roles, Long laboratoryId) {
        this.username = new Username(username);
        this.email = email == null ? null : new EmailAddress(email);
        this.password = new PasswordHash(password);
        this.roles = Role.validateRoleSet(roles);
        setLaboratoryId(laboratoryId);
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Reconstructs a user from persistence data.
     *
     * @param id user identifier
     * @param username account username
     * @param password hashed account password
     * @param roles assigned roles
     * @param laboratoryId associated laboratory identifier
     * @param status user status
     */
    public User(
            Long id,
            String username,
            String password,
            Collection<Role> roles,
            Long laboratoryId,
            UserStatus status
    ) {
        this(username, password, roles, laboratoryId);
        this.id = id;
        this.status = status;
    }

    /**
     * Reconstructs a user from persistence data, including its e-mail and whether the password is temporary.
     */
    public User(Long id, String username, String email, String password, Collection<Role> roles, Long laboratoryId,
                UserStatus status, boolean passwordChangeRequired) {
        this(username, email, password, roles, laboratoryId);
        this.id = id;
        this.status = status;
        this.passwordChangeRequired = passwordChangeRequired;
    }

    /**
     * Creates the account of a staff member of a laboratory with a temporary password.
     *
     * @param email the staff member e-mail, used as username and to recover the password
     * @param temporaryPasswordHash hash of the generated temporary password
     * @param role the access role of the staff member
     * @param laboratoryId the laboratory the staff member works for
     * @return the account, which must change its password at the first sign in
     */
    public static User staffAccount(String email, String temporaryPasswordHash, Role role, Long laboratoryId) {
        if (laboratoryId == null) throw new IllegalArgumentException("A staff account belongs to a laboratory");
        var user = new User(email, email, temporaryPasswordHash, List.of(role), laboratoryId);
        user.passwordChangeRequired = true;
        return user;
    }

    /**
     * Replaces the password and clears the temporary password requirement.
     *
     * @param newPasswordHash hash of the password chosen by the user
     */
    public void changePassword(String newPasswordHash) {
        this.password = new PasswordHash(newPasswordHash);
        this.passwordChangeRequired = false;
    }

    /**
     * Replaces the username used to sign in and the e-mail of the account. Uniqueness against other accounts is
     * checked by the application service.
     *
     * @param username new username
     * @param email new e-mail address
     * @return true when at least one of them changed
     */
    public boolean updateAccount(Username username, EmailAddress email) {
        if (username == null || email == null) {
            throw new IllegalArgumentException("The username and the e-mail are required");
        }
        var changed = !username.equals(this.username) || !email.equals(this.email);
        this.username = username;
        this.email = email;
        return changed;
    }

    /**
     * Adds a role to this user.
     *
     * @param role role to assign
     */
    public void addRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role cannot be null");
        }

        this.roles.add(role);
    }

    /**
     * Adds multiple roles to this user.
     *
     * @param roles roles to assign
     */
    public void addRoles(Collection<Role> roles) {
        this.roles.addAll(Role.validateRoleSet(roles));
    }

    /**
     * Deactivates the user account.
     */
    public void deactivate() {
        if (status == UserStatus.INACTIVE) {
            throw new IllegalStateException("User is already inactive");
        }

        this.status = UserStatus.INACTIVE;
    }

    /**
     * Checks whether this user is active.
     *
     * @return true when user status is active
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Returns username raw value.
     *
     * @return username string
     */
    public String getUsernameValue() {
        return username.value();
    }

    /**
     * @return the e-mail of the account, or null when it was registered before it was required
     */
    public String getEmailValue() {
        return email == null ? null : email.value();
    }

    /**
     * Returns password hash raw value.
     *
     * @return password hash string
     */
    public String getPasswordValue() {
        return password.value();
    }

    /**
     * Assigns a laboratory identifier to this user.
     *
     * @param laboratoryId laboratory identifier
     */
    private void setLaboratoryId(Long laboratoryId) {
        if (laboratoryId != null && laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be less than 1");
        }

        this.laboratoryId = laboratoryId;
    }

    public void assignLaboratory(Long laboratoryId) {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("A positive laboratory identifier is required");
        }
        if (this.laboratoryId != null && !this.laboratoryId.equals(laboratoryId)) {
            throw new IllegalStateException("The user already belongs to a laboratory");
        }
        this.laboratoryId = laboratoryId;
    }
}
