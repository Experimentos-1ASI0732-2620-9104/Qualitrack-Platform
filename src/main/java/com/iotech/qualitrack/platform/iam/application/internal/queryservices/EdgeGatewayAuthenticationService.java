package com.iotech.qualitrack.platform.iam.application.internal.queryservices;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.acl.IamExternalEquipmentService;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.EdgeGatewayApiKeyStore;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.EdgeGatewayIdentity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Application service for authenticating Edge Gateways
 * using their external identifier and API key.
 *
 * <p>Gateway authentication is independent of the
 * JWT authentication used by human users.</p>
 */
@Service
@Transactional(readOnly = true)
public class EdgeGatewayAuthenticationService {

    private final IamExternalEquipmentService equipmentService;
    private final EdgeGatewayApiKeyStore apiKeyStore;
    private final HashingService hashingService;

    public EdgeGatewayAuthenticationService(
            IamExternalEquipmentService equipmentService,
            EdgeGatewayApiKeyStore apiKeyStore,
            HashingService hashingService) {

        this.equipmentService = equipmentService;
        this.apiKeyStore = apiKeyStore;
        this.hashingService = hashingService;
    }

    /**
     * Authenticates an Edge Gateway using its API key.
     *
     * <p>The Gateway must exist, be operational, have
     * an assigned environment and possess a matching
     * API key hash in IAM.</p>
     *
     * @param deviceId Gateway external identifier
     * @param apiKey API key received through X-API-Key
     * @return authenticated Gateway identity, if valid
     */
    public Optional<AuthenticatedGateway> authenticate(
            String deviceId,
            String apiKey) {

        if (deviceId == null || deviceId.isBlank()
                || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        return equipmentService.findEdgeGatewayByExternalId(deviceId)
                .filter(EdgeGatewayIdentity::canConnect)
                .flatMap(gateway ->
                        apiKeyStore.findApiKeyHashByEquipmentId(
                                        gateway.equipmentId()
                                )
                                .filter(hash ->
                                        hashingService.matches(apiKey, hash)
                                )
                                .map(hash -> new AuthenticatedGateway(
                                        gateway.equipmentId(),
                                        gateway.sensorExternalId(),
                                        gateway.laboratoryId(),
                                        gateway.environmentId()
                                ))
                );
    }

    /**
     * Identity of an authenticated Edge Gateway.
     *
     * <p>Contains the current laboratory and environment
     * assignment obtained from Equipment, not from
     * request parameters supplied by the client.</p>
     *
     * @param equipmentId Gateway equipment identifier
     * @param deviceId Gateway external identifier
     * @param laboratoryId owning laboratory
     * @param environmentId assigned environment
     */
    public record AuthenticatedGateway(
            Long equipmentId,
            String deviceId,
            Long laboratoryId,
            Long environmentId
    ) {}
}