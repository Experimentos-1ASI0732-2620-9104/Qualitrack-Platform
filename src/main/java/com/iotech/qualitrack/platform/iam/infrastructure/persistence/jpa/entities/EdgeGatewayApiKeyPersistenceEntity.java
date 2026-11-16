package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/**
 * JPA entity that stores an Edge Gateway API key hash.
 *
 * <p>The gateway is identified by its Equipment ID,
 * while the original API key is never persisted.</p>
 */
@Getter
@Entity
@Table(name = "iam_edge_gateway_api_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_gateway_api_key_equipment", columnNames = "equipment_id")
        }
)
public class EdgeGatewayApiKeyPersistenceEntity
        extends AuditableAbstractPersistenceEntity {

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    protected EdgeGatewayApiKeyPersistenceEntity() {
    }

    /**
     * Creates an API key persistence entity.
     *
     * @param equipmentId registered Edge Gateway identifier
     * @param apiKeyHash cryptographic hash of the API key
     */
    public EdgeGatewayApiKeyPersistenceEntity(Long equipmentId, String apiKeyHash) {
        this.equipmentId = equipmentId;
        this.apiKeyHash = apiKeyHash;
    }

}