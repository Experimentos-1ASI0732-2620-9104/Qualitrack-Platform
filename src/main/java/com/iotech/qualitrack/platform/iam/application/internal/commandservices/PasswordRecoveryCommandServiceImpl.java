package com.iotech.qualitrack.platform.iam.application.internal.commandservices;

import com.iotech.qualitrack.platform.iam.application.commandservices.PasswordRecoveryCommandService;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.RecoveryCodeGenerator;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.notifications.PasswordRecoveryNotifier;
import com.iotech.qualitrack.platform.iam.domain.model.aggregates.PasswordRecovery;
import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.model.commands.RequestPasswordRecoveryCommand;
import com.iotech.qualitrack.platform.iam.domain.model.commands.ResetPasswordCommand;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.EmailAddress;
import com.iotech.qualitrack.platform.iam.domain.repositories.PasswordRecoveryRepository;
import com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/**
 * Password recovery: Request Password Reset, Send Verification Code, Verify Recovery Code and Reset Password of the
 * IAM event storming. Codes are kept as a hash and e-mailed through the configured provider.
 */
@Slf4j
@Service
public class PasswordRecoveryCommandServiceImpl implements PasswordRecoveryCommandService {
    static final String INVALID_CODE = "The verification code is not valid or has expired";

    private final UserRepository users;
    private final PasswordRecoveryRepository recoveries;
    private final HashingService hashing;
    private final RecoveryCodeGenerator codes;
    private final PasswordRecoveryNotifier notifier;
    private final Clock iamClock;

    public PasswordRecoveryCommandServiceImpl(UserRepository users, PasswordRecoveryRepository recoveries,
                                              HashingService hashing, RecoveryCodeGenerator codes,
                                              PasswordRecoveryNotifier notifier, Clock iamClock) {
        this.users = users;
        this.recoveries = recoveries;
        this.hashing = hashing;
        this.codes = codes;
        this.notifier = notifier;
        this.iamClock = iamClock;
    }

    @Override
    @Transactional
    public Result<Duration, ApplicationError> handle(RequestPasswordRecoveryCommand command) {
        var account = findAccount(command.account()).filter(User::isActive).filter(user -> user.getEmailValue() != null);
        if (account.isEmpty()) {
            log.info("Password recovery requested for an account that cannot receive a code");
            return Result.success(PasswordRecovery.CODE_VALIDITY);
        }
        var user = account.get();
        var now = iamClock.instant();
        var latest = recoveries.findLatestByUserId(user.getId());
        if (latest.filter(recovery -> recovery.blocksNewCode(now)).isPresent()) {
            return Result.success(PasswordRecovery.CODE_VALIDITY);
        }
        latest.ifPresent(recovery -> {
            recovery.revoke();
            recoveries.save(recovery);
        });
        var code = codes.generate();
        recoveries.save(PasswordRecovery.start(user.getId(), hashing.encode(code), now));
        if (!notifier.send(new PasswordRecoveryNotifier.RecoveryCode(user.getEmailValue(), user.getUsernameValue(), code,
                PasswordRecovery.CODE_VALIDITY))) {
            log.warn("The recovery code of account {} could not be e-mailed", user.getId());
        }
        return Result.success(PasswordRecovery.CODE_VALIDITY);
    }

    @Override
    @Transactional
    public Result<User, ApplicationError> handle(ResetPasswordCommand command) {
        var account = findAccount(command.account()).filter(User::isActive);
        if (account.isEmpty()) return Result.failure(ApplicationError.validationError("code", INVALID_CODE));
        var user = account.get();
        var now = iamClock.instant();
        var pending = recoveries.findLatestByUserId(user.getId()).filter(recovery -> recovery.isUsable(now));
        if (pending.isEmpty()) return Result.failure(ApplicationError.validationError("code", INVALID_CODE));
        var recovery = pending.get();
        if (!hashing.matches(command.code(), recovery.getCodeHash())) {
            recovery.registerFailedAttempt();
            recoveries.save(recovery);
            return Result.failure(ApplicationError.validationError("code", INVALID_CODE));
        }
        recovery.complete(now);
        recoveries.save(recovery);
        user.changePassword(hashing.encode(command.newPassword()));
        return Result.success(users.save(user));
    }

    /** The account whose username is the given text, or whose e-mail is the given text. */
    private Optional<User> findAccount(String account) {
        return users.findByUsername(account).or(() -> EmailAddress.looksLikeEmail(account)
                ? users.findByEmail(new EmailAddress(account).value()) : Optional.empty());
    }
}
