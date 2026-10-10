package com.iotech.qualitrack.platform.iam.interfaces.acl;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.application.queryservices.UserQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.commands.CreateStaffAccountCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.DeactivateUserCommand;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Roles;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * ACL facade exposed by the IAM bounded context.
 *
 * <p>Provides stable user identity and authorization information to other
 * bounded contexts without exposing IAM internals.</p>
 */
@Service
public class IamContextFacade {

    private final UserQueryService userQueryService;
    private final com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository userRepository;
    private final UserCommandService userCommandService;

    public IamContextFacade(UserQueryService userQueryService,
            com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository userRepository,
            UserCommandService userCommandService) {
        this.userQueryService = userQueryService;
        this.userRepository = userRepository;
        this.userCommandService = userCommandService;
    }

    /**
     * Creates the sign-in account of a staff member registered by a quality manager and sends the credentials.
     *
     * @param laboratoryId laboratory the staff member works for
     * @param email e-mail of the staff member, used as username
     * @param fullName name of the staff member
     * @param auditor true for a read-only auditor, false for an operator
     * @return the account; the temporary password is returned so it can be handed over when it was not e-mailed
     * @throws ApplicationException CONFLICT when the e-mail is already the username of another account
     */
    public StaffAccountReference createStaffAccount(Long laboratoryId, String email, String fullName, boolean auditor) {
        var role = auditor ? Roles.ROLE_AUDITOR : Roles.ROLE_LAB_OPERATOR;
        return switch (userCommandService.handle(new CreateStaffAccountCommand(laboratoryId, email, fullName, role))) {
            case Result.Success<UserCommandService.StaffAccount, ApplicationError> success -> new StaffAccountReference(
                    success.value().user().getId(), success.value().user().getUsernameValue(),
                    success.value().temporaryPassword(), success.value().credentialsSent());
            case Result.Failure<UserCommandService.StaffAccount, ApplicationError> failure ->
                    throw new ApplicationException(failure.error());
        };
    }

    /**
     * Whether an account already uses the username.
     */
    public boolean existsUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Prevents a deactivated staff member from signing in.
     *
     * @param userId the account of the staff member
     */
    public void deactivateUser(Long userId) {
        var user = userRepository.findById(userId);
        if (user.isPresent() && user.get().isActive()) userCommandService.handle(new DeactivateUserCommand(userId));
    }

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

    public Long lockLaboratoryAssociation(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found")).getLaboratoryId();
    }

    public void assignLaboratory(Long userId, Long laboratoryId) {
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.assignLaboratory(laboratoryId);
        userRepository.save(user);
    }

    /**
     * Checks whether a user exists.
     *
     * @param userId user identifier
     * @return true if the user exists
     */
    public boolean existsUserById(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId)).isPresent();
    }

    /**
     * Gets the laboratory associated with a user.
     *
     * @param userId user identifier
     * @return laboratory identifier or null if user does not exist or has no laboratory
     */
    public Long getLaboratoryIdByUserId(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getLaboratoryId())
                .orElse(null);
    }

    /**
     * Gets the username for a user.
     *
     * @param userId user identifier
     * @return username or null if user does not exist
     */
    public String getUsernameByUserId(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getUsernameValue())
                .orElse(null);
    }

    /**
     * Gets assigned role names for a user.
     *
     * @param userId user identifier
     * @return role names or empty list if user does not exist
     */
    public List<String> getRoleNamesByUserId(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList())
                .orElse(List.of());
    }

    /**
     * Checks whether a user has a given role.
     *
     * @param userId user identifier
     * @param roleName role name
     * @return true if the user has the role
     */
    public boolean userHasRole(Long userId, String roleName) {
        return getRoleNamesByUserId(userId).contains(roleName);
    }

    /**
     * @param userId the account
     * @return username, e-mail, roles and laboratory of the account
     */
    public Optional<AccountReference> findAccount(Long userId) {
        return userRepository.findById(userId).map(IamContextFacade::toReference);
    }

    /**
     * @param laboratoryId the laboratory
     * @return the accounts of the laboratory that can still sign in
     */
    public List<AccountReference> findActiveAccounts(Long laboratoryId) {
        return userRepository.findByLaboratoryId(laboratoryId).stream()
                .filter(User::isActive)
                .map(IamContextFacade::toReference)
                .toList();
    }

    private static AccountReference toReference(User user) {
        return new AccountReference(user.getId(), user.getLaboratoryId(), user.getUsernameValue(), user.getEmailValue(),
                user.getRoles().stream().map(role -> role.getName().name()).toList(), user.isActive());
    }

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
