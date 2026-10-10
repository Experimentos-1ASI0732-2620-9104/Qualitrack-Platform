package com.iotech.qualitrack.platform.iam.application.internal.commandservices;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.CredentialsNotifier;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.TemporaryPasswordGenerator;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.commands.AssignRoleCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.ChangePasswordCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.CreateStaffAccountCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.DeactivateUserCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.SignInCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.SignUpCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.UpdateAccountCommand;
import com.iotech.qualitrack.platform.iam.domain.model.entities.Role;
import com.iotech.qualitrack.platform.iam.domain.model.events.UserAccountUpdatedEvent;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.EmailAddress;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.PasswordPolicy;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Roles;
import com.iotech.qualitrack.platform.iam.domain.repositories.RoleRepository;
import com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

/**
 * Application command service implementation for IAM users.
 */
@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final TemporaryPasswordGenerator passwordGenerator;
    private final CredentialsNotifier credentialsNotifier;
    private final ApplicationEventPublisher eventPublisher;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            HashingService hashingService,
            TokenService tokenService,
            TemporaryPasswordGenerator passwordGenerator,
            CredentialsNotifier credentialsNotifier,
            ApplicationEventPublisher eventPublisher
    ) {
        this.passwordGenerator = passwordGenerator;
        this.credentialsNotifier = credentialsNotifier;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<AuthenticatedUser, ApplicationError> handle(SignInCommand command) {
        var userResult = userRepository.findByUsername(command.username());

        if (userResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.username()));
        }

        var user = userResult.get();

        if (!user.isActive()) {
            return Result.failure(ApplicationError.conflict("User", "User account is not active"));
        }

        if (!hashingService.matches(command.password(), user.getPasswordValue())) {
            return Result.failure(ApplicationError.validationError("Credentials", "Invalid username or password"));
        }

        var token = tokenService.generateToken(user);

        return Result.success(new AuthenticatedUser(user, token));
    }

    @Override
    public Result<User, ApplicationError> handle(SignUpCommand command) {
        if (command.roles().contains("ROLE_ADMIN")) {
            return Result.failure(ApplicationError.validationError("roles", "Administrator accounts cannot self-register"));
        }
        if (!command.roles().equals(java.util.List.of("ROLE_QA_MANAGER"))) {
            return Result.failure(ApplicationError.validationError("roles",
                    "Public registration creates quality manager accounts; staff accounts are created by a quality manager"));
        }
        if (command.laboratoryId() != null) {
            return Result.failure(ApplicationError.validationError("laboratoryId",
                    "Public registration cannot join an existing laboratory"));
        }
        if (userRepository.existsByUsername(command.username())) {
            return Result.failure(ApplicationError.conflict(
                    "User",
                    "Username '%s' is already registered".formatted(command.username())
            ));
        }
        EmailAddress email;
        try {
            email = new EmailAddress(command.email());
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("email", e.getMessage()));
        }
        try {
            PasswordPolicy.validate(command.password());
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("password", e.getMessage()));
        }
        if (userRepository.existsByEmail(email.value())) {
            return Result.failure(ApplicationError.conflict("User", "The e-mail is already registered"));
        }

        try {
            var roles = new ArrayList<Role>();

            for (var roleName : command.roles()) {
                var role = roleRepository.findByName(Roles.valueOf(roleName))
                        .orElseGet(() -> roleRepository.save(new Role(Roles.valueOf(roleName))));

                roles.add(role);
            }

            var encodedPassword = hashingService.encode(command.password());

            var user = new User(
                    command.username(),
                    email.value(),
                    encodedPassword,
                    roles,
                    command.laboratoryId()
            );

            var savedUser = userRepository.save(user);

            return Result.success(savedUser);

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("User", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("sign-up", e.getMessage()));
        }
    }

    @Override
    public Result<Long, ApplicationError> handle(AssignRoleCommand command) {
        var userResult = userRepository.findById(command.userId());

        if (userResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.userId()));
        }

        try {
            var roleName = Roles.valueOf(command.roleName());

            var role = roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(roleName)));

            var user = userResult.get();
            user.addRole(role);

            var savedUser = userRepository.save(user);

            return Result.success(savedUser.getId());

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("Role", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("assign-role", e.getMessage()));
        }
    }

    @Override
    public Result<Long, ApplicationError> handle(DeactivateUserCommand command) {
        var userResult = userRepository.findById(command.userId());

        if (userResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.userId()));
        }

        try {
            var user = userResult.get();
            user.deactivate();

            var savedUser = userRepository.save(user);

            return Result.success(savedUser.getId());

        } catch (IllegalStateException e) {
            return Result.failure(ApplicationError.conflict("User", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("deactivate-user", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public Result<StaffAccount, ApplicationError> handle(CreateStaffAccountCommand command) {
        if (userRepository.existsByUsername(command.email())
                || (EmailAddress.looksLikeEmail(command.email()) && userRepository.existsByEmail(new EmailAddress(command.email()).value()))) {
            return Result.failure(ApplicationError.conflict("User",
                    "An account with username or e-mail '%s' already exists".formatted(command.email())));
        }
        var role = roleRepository.findByName(command.role()).orElseGet(() -> roleRepository.save(new Role(command.role())));
        var temporaryPassword = passwordGenerator.generate();
        User account;
        try {
            account = userRepository.save(User.staffAccount(command.email(), hashingService.encode(temporaryPassword),
                    role, command.laboratoryId()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("User", e.getMessage()));
        }
        var sent = credentialsNotifier.send(new CredentialsNotifier.StaffCredentials(command.email(), command.fullName(),
                account.getUsernameValue(), temporaryPassword));
        return Result.success(new StaffAccount(account, temporaryPassword, sent));
    }

    @Override
    @Transactional
    public Result<User, ApplicationError> handle(ChangePasswordCommand command) {
        var found = userRepository.findById(command.userId()).filter(User::isActive);
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.userId()));
        }
        var user = found.get();
        if (!hashingService.matches(command.currentPassword(), user.getPasswordValue())) {
            return Result.failure(ApplicationError.validationError("currentPassword", "The current password is not correct"));
        }
        user.changePassword(hashingService.encode(command.newPassword()));
        return Result.success(userRepository.save(user));
    }

    @Override
    @Transactional
    public Result<AuthenticatedUser, ApplicationError> handle(UpdateAccountCommand command) {
        var found = userRepository.findById(command.userId()).filter(User::isActive);
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.userId()));
        }
        var user = found.get();
        if (!hashingService.matches(command.currentPassword(), user.getPasswordValue())) {
            return Result.failure(ApplicationError.validationError("currentPassword", "The current password is not correct"));
        }
        var username = command.username().value();
        var email = command.email().value();
        // Recovery accepts the username or the e-mail, so neither may match another account in either field.
        if (belongsToAnotherAccount(userRepository.findByUsername(username), user)
                || (EmailAddress.looksLikeEmail(username)
                    && belongsToAnotherAccount(userRepository.findByEmail(username.toLowerCase(Locale.ROOT)), user))) {
            return Result.failure(ApplicationError.conflict("User", "The username '%s' is already in use".formatted(username)));
        }
        if (belongsToAnotherAccount(userRepository.findByEmail(email), user)
                || belongsToAnotherAccount(userRepository.findByUsername(email), user)) {
            return Result.failure(ApplicationError.conflict("User", "The e-mail '%s' is already in use".formatted(email)));
        }
        if (user.updateAccount(command.username(), command.email())) {
            user = userRepository.save(user);
            eventPublisher.publishEvent(new UserAccountUpdatedEvent(user.getId(), user.getLaboratoryId(),
                    user.getUsernameValue(), user.getEmailValue()));
        }
        return Result.success(new AuthenticatedUser(user, tokenService.generateToken(user)));
    }

    private static boolean belongsToAnotherAccount(Optional<User> account, User user) {
        return account.filter(other -> !other.getId().equals(user.getId())).isPresent();
    }
}
