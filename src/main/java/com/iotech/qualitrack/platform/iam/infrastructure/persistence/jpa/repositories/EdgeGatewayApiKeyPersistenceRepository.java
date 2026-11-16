package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities.EdgeGatewayApiKeyPersistenceEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for Edge Gateway API keys.
 */
public interface EdgeGatewayApiKeyPersistenceRepository
        extends JpaRepository<EdgeGatewayApiKeyPersistenceEntity, Long> {

    /**
     * Checks whether an API key has already been issued.
     *
     * @param equipmentId Edge Gateway equipment identifier
     * @return true if the gateway has an API key
     */
    boolean existsByEquipmentId(Long equipmentId);

    /**
     * Finds the API key credentials associated with a Gateway.
     *
     * @param equipmentId Edge Gateway equipment identifier
     * @return persisted credential, if present
     */
    Optional<EdgeGatewayApiKeyPersistenceEntity> findByEquipmentId(Long equipmentId);
}
