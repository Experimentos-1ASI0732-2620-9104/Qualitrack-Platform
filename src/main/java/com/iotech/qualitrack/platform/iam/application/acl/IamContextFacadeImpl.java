package com.iotech.qualitrack.platform.iam.application.acl;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.application.queryservices.UserQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.commands.CreateStaffAccountCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.DeactivateUserCommand;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Roles;
import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Application-layer implementation of the {@link IamContextFacade} ACL facade.
 */
@Service
public class IamContextFacadeImpl implements IamContextFacade {

    private final UserQueryService userQueryService;
    private final com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository userRepository;
    private final UserCommandService userCommandService;

    public IamContextFacadeImpl(UserQueryService userQueryService,
            com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository userRepository,
            UserCommandService userCommandService) {
        this.userQueryService = userQueryService;
        this.userRepository = userRepository;
        this.userCommandService = userCommandService;
    }

    @Override
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

    @Override
    public boolean existsUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public void deactivateUser(Long userId) {
        var user = userRepository.findById(userId);
        if (user.isPresent() && user.get().isActive()) userCommandService.handle(new DeactivateUserCommand(userId));
    }

    @Override
    public Long lockLaboratoryAssociation(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found")).getLaboratoryId();
    }

    @Override
    public void assignLaboratory(Long userId, Long laboratoryId) {
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.assignLaboratory(laboratoryId);
        userRepository.save(user);
    }

    @Override
    public boolean existsUserById(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId)).isPresent();
    }

    @Override
    public Long getLaboratoryIdByUserId(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getLaboratoryId())
                .orElse(null);
    }

    @Override
    public String getUsernameByUserId(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getUsernameValue())
                .orElse(null);
    }

    @Override
    public List<String> getRoleNamesByUserId(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList())
                .orElse(List.of());
    }

    @Override
    public boolean userHasRole(Long userId, String roleName) {
        return getRoleNamesByUserId(userId).contains(roleName);
    }

    @Override
    public Optional<AccountReference> findAccount(Long userId) {
        return userRepository.findById(userId).map(IamContextFacadeImpl::toReference);
    }

    @Override
    public List<AccountReference> findActiveAccounts(Long laboratoryId) {
        return userRepository.findByLaboratoryId(laboratoryId).stream()
                .filter(User::isActive)
                .map(IamContextFacadeImpl::toReference)
                .toList();
    }

    private static AccountReference toReference(User user) {
        return new AccountReference(user.getId(), user.getLaboratoryId(), user.getUsernameValue(), user.getEmailValue(),
                user.getRoles().stream().map(role -> role.getName().name()).toList(), user.isActive());
    }
}
