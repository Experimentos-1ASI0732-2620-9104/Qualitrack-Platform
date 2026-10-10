package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.PasswordRecovery;
import com.iotech.qualitrack.platform.iam.domain.repositories.PasswordRecoveryRepository;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.assemblers.PasswordRecoveryPersistenceAssembler;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities.PasswordRecoveryPersistenceEntity;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.repositories.PasswordRecoveryPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter of the password recovery repository.
 */
@Repository
public class PasswordRecoveryRepositoryImpl implements PasswordRecoveryRepository {
    private final PasswordRecoveryPersistenceRepository recoveries;

    public PasswordRecoveryRepositoryImpl(PasswordRecoveryPersistenceRepository recoveries) {
        this.recoveries = recoveries;
    }

    @Override
    public PasswordRecovery save(PasswordRecovery recovery) {
        var entity = recovery.getId() == null ? new PasswordRecoveryPersistenceEntity()
                : recoveries.findById(recovery.getId()).orElseGet(PasswordRecoveryPersistenceEntity::new);
        var saved = recoveries.save(PasswordRecoveryPersistenceAssembler.toPersistenceFromDomain(recovery, entity));
        recovery.setId(saved.getId());
        return recovery;
    }

    @Override
    public Optional<PasswordRecovery> findLatestByUserId(Long userId) {
        return recoveries.findFirstByUserIdOrderByRequestedAtDescIdDesc(userId)
                .map(PasswordRecoveryPersistenceAssembler::toDomainFromPersistence);
    }
}
