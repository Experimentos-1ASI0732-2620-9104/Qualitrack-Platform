package com.iotech.qualitrack.platform.iam.interfaces.acl;

import java.util.List;
import java.util.Optional;

/**
 * ACL facade exposed by the IAM bounded context.
 *
 * <p>Provides stable user identity and authorization information to other
 * bounded contexts without exposing IAM internals.</p>
 */
public interface IamContextFacade {

    /**
     * Creates the sign-in account of a staff member registered by a quality manager and sends the credentials.
     *
     * @param laboratoryId laboratory the staff member works for
     * @param email e-mail of the staff member, used as username
     * @param fullName name of the staff member
     * @param auditor true for a read-only auditor, false for an operator
     * @return the account; the temporary password is returned so it can be handed over when it was not e-mailed
     * @throws com.iotech.qualitrack.platform.shared.application.result.ApplicationException CONFLICT when the e-mail
     * is already the username of another account
     */
    StaffAccountReference createStaffAccount(Long laboratoryId, String email, String fullName, boolean auditor);

    /**
     * Whether an account already uses the username.
     */
    boolean existsUsername(String username);

    /**
     * Prevents a deactivated staff member from signing in.
     *
     * @param userId the account of the staff member
     */
    void deactivateUser(Long userId);

    /**
     * Sign-in account created for a staff member.
     *
     * @param userId the account identifier
     * @param username the username (the staff member e-mail)
     * @param temporaryPassword the password to change at the first sign in
     * @param credentialsSent whether the credentials were e-mailed
     */
    /**
     * Sign-in account created for a staff member.
     *
     * @param userId the account identifier
     * @param username the username (the staff member e-mail)
     * @param temporaryPassword the password to change at the first sign in
     * @param credentialsSent whether the credentials were e-mailed
     */
    public record StaffAccountReference(Long userId, String username, String temporaryPassword, boolean credentialsSent) {
    }

    /**
     * Locks the account until the transaction ends.
     *
     * @return the laboratory associated with the account, or null when it has none yet
     * @throws IllegalArgumentException when the account does not exist
     */
    Long lockLaboratoryAssociation(Long userId);

    /**
     * Associates the account with the laboratory created during the onboarding.
     *
     * @throws IllegalArgumentException when the account does not exist
     */
    void assignLaboratory(Long userId, Long laboratoryId);

    /**
     * Checks whether a user exists.
     *
     * @param userId user identifier
     * @return true if the user exists
     */
    boolean existsUserById(Long userId);

    /**
     * Gets the laboratory associated with a user.
     *
     * @param userId user identifier
     * @return laboratory identifier or null if user does not exist or has no laboratory
     */
    Long getLaboratoryIdByUserId(Long userId);

    /**
     * Gets the username for a user.
     *
     * @param userId user identifier
     * @return username or null if user does not exist
     */
    String getUsernameByUserId(Long userId);

    /**
     * Gets assigned role names for a user.
     *
     * @param userId user identifier
     * @return role names or empty list if user does not exist
     */
    List<String> getRoleNamesByUserId(Long userId);

    /**
     * Checks whether a user has a given role.
     *
     * @param userId user identifier
     * @param roleName role name
     * @return true if the user has the role
     */
    boolean userHasRole(Long userId, String roleName);

    /**
     * @param userId the account
     * @return username, e-mail, roles and laboratory of the account
     */
    Optional<AccountReference> findAccount(Long userId);

    /**
     * @param laboratoryId the laboratory
     * @return the accounts of the laboratory that can still sign in
     */
    List<AccountReference> findActiveAccounts(Long laboratoryId);

    /**
     * Sign-in account seen from other contexts.
     *
     * @param userId the account identifier
     * @param laboratoryId laboratory of the account, or null before the onboarding
     * @param username username used to sign in
     * @param email e-mail of the account, or null for accounts registered before it was required
     * @param roles role names, for example ROLE_QA_MANAGER
     * @param active whether the account can sign in
     */
    /**
     * Sign-in account seen from other contexts.
     *
     * @param userId the account identifier
     * @param laboratoryId laboratory of the account, or null before the onboarding
     * @param username username used to sign in
     * @param email e-mail of the account, or null for accounts registered before it was required
     * @param roles role names, for example ROLE_QA_MANAGER
     * @param active whether the account can sign in
     */
    public record AccountReference(Long userId, Long laboratoryId, String username, String email, List<String> roles,
                                   boolean active) {
    }
}
