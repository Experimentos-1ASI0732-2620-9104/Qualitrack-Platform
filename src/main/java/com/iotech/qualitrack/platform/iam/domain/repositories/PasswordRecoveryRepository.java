package com.iotech.qualitrack.platform.iam.domain.repositories;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.PasswordRecovery;

import java.util.Optional;

/**
 * Repository of the password recoveries of the accounts.
 */
public interface PasswordRecoveryRepository {

    PasswordRecovery save(PasswordRecovery recovery);

    /**
     * @param userId the account
     * @return its most recent recovery, whatever its status
     */
    Optional<PasswordRecovery> findLatestByUserId(Long userId);
}
