package com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials;

import java.util.Optional;

/**
 * Persistence contract for Edge Gateway API keys.
 *
 * <p>Each gateway has at most one active API key.
 * Only its cryptographic hash is persisted.</p>
 */
public interface EdgeGatewayApiKeyStore {

    /**
     * Checks whether an Edge Gateway already has an API key.
     *
     * @param equipmentId gateway equipment identifier
     * @return true if an API key has been provisioned
     */
    boolean existsByEquipmentId(Long equipmentId);

    /**
     * Persists the hash of a newly generated API key.
     *
     * @param equipmentId gateway equipment identifier
     * @param apiKeyHash encoded API key hash
     */
    void create(Long equipmentId, String apiKeyHash);

    /**
     * Retrieves the stored API key hash of an Edge Gateway.
     *
     * <p>The original API key is never returned because
     * IAM only persists its BCrypt hash.</p>
     *
     * @param equipmentId Edge Gateway equipment identifier
     * @return stored API key hash, if provisioned
     */
    Optional<String> findApiKeyHashByEquipmentId(Long equipmentId);
}