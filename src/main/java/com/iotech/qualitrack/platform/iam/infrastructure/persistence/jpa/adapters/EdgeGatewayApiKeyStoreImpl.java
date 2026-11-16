package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.EdgeGatewayApiKeyStore;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities.EdgeGatewayApiKeyPersistenceEntity;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.repositories.EdgeGatewayApiKeyPersistenceRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter for storing Edge Gateway API key hashes.
 *
 * <p>This adapter never receives or persists plaintext
 * API keys.</p>
 */
@Repository
public class EdgeGatewayApiKeyStoreImpl
        implements EdgeGatewayApiKeyStore {

    private final EdgeGatewayApiKeyPersistenceRepository repository;

    public EdgeGatewayApiKeyStoreImpl(
            EdgeGatewayApiKeyPersistenceRepository repository) {

        this.repository = repository;
    }

    @Override
    public boolean existsByEquipmentId(Long equipmentId) {
        return repository.existsByEquipmentId(equipmentId);
    }

    /**
     * Saves the API key hash and flushes it to the database.
     *
     * <p>Flushing allows unique constraint violations to be
     * detected before provisioning completes.</p>
     */
    @Override
    public void create(Long equipmentId, String apiKeyHash) {
        var entity = new EdgeGatewayApiKeyPersistenceEntity(equipmentId, apiKeyHash);

        repository.save(entity);
    }

    /**
     * Retrieves the BCrypt hash associated with an Edge Gateway.
     *
     * @param equipmentId Gateway equipment identifier
     * @return stored API key hash when available
     */
    @Override
    public Optional<String> findApiKeyHashByEquipmentId(Long equipmentId) {
        return repository.findByEquipmentId(equipmentId)
                .map(EdgeGatewayApiKeyPersistenceEntity::getApiKeyHash);
    }
}