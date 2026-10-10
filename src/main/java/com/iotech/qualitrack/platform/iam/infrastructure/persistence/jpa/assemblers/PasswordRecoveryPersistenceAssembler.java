package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.PasswordRecovery;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities.PasswordRecoveryPersistenceEntity;

/**
 * Maps password recoveries between the domain and persistence.
 */
public final class PasswordRecoveryPersistenceAssembler {

    private PasswordRecoveryPersistenceAssembler() {
    }

    public static PasswordRecovery toDomainFromPersistence(PasswordRecoveryPersistenceEntity entity) {
        return new PasswordRecovery(entity.getId(), entity.getUserId(), entity.getCodeHash(), entity.getRequestedAt(),
                entity.getExpiresAt(), entity.getFailedAttempts(), entity.getStatus(), entity.getCompletedAt());
    }

    public static PasswordRecoveryPersistenceEntity toPersistenceFromDomain(PasswordRecovery recovery,
                                                                            PasswordRecoveryPersistenceEntity entity) {
        entity.setUserId(recovery.getUserId());
        entity.setCodeHash(recovery.getCodeHash());
        entity.setRequestedAt(recovery.getRequestedAt());
        entity.setExpiresAt(recovery.getExpiresAt());
        entity.setFailedAttempts(recovery.getFailedAttempts());
        entity.setStatus(recovery.getStatus());
        entity.setCompletedAt(recovery.getCompletedAt());
        return entity;
    }
}
