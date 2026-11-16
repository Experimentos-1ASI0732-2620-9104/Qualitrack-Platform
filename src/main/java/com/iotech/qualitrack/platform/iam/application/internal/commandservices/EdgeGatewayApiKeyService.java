package com.iotech.qualitrack.platform.iam.application.internal.commandservices;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.acl.IamExternalEquipmentService;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.credentials.EdgeGatewayApiKeyStore;
import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Application service responsible for provisioning
 * API keys for registered Edge Gateways.
 *
 * <p>Verifies gateway eligibility through the Equipment
 * ACL service and delegates API key persistence to IAM.</p>
 *
 * <p>Generated API keys are returned once. Only their
 * BCrypt hashes are stored.</p>
 */
@Service
public class EdgeGatewayApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int API_KEY_SIZE_BYTES = 32;

    private final IamExternalEquipmentService equipmentService;
    private final EdgeGatewayApiKeyStore apiKeyStore;
    private final HashingService hashingService;

    public EdgeGatewayApiKeyService(
            IamExternalEquipmentService equipmentService,
            EdgeGatewayApiKeyStore apiKeyStore,
            HashingService hashingService) {

        this.equipmentService = equipmentService;
        this.apiKeyStore = apiKeyStore;
        this.hashingService = hashingService;
    }

    /**
     * Generates an API key for an operational Edge Gateway.
     *
     * <p>The Gateway must belong to the specified laboratory
     * and have an assigned environment.</p>
     *
     * <p>A Gateway cannot receive another API key if one
     * has already been provisioned.</p>
     *
     * @param laboratoryId owning laboratory identifier
     * @param equipmentId Edge Gateway equipment identifier
     * @return API key issued for the Gateway
     * @throws ApplicationException if the Gateway is not found,
     *                              is not eligible, or already
     *                              has an API key
     */
    @Transactional
    public IssuedApiKey provision(
            Long laboratoryId,
            Long equipmentId) {

        var gateway = equipmentService.findEdgeGateway(
                laboratoryId,
                equipmentId
        ).orElseThrow(() -> new ApplicationException(
                ApplicationError.notFound(
                        "EdgeGateway",
                        equipmentId
                )
        ));

        if (!gateway.canConnect()) {
            throw new ApplicationException(
                    ApplicationError.conflict(
                            "EdgeGateway",
                            "Gateway must be operational and assigned "
                                    + "to an environment"
                    )
            );
        }

        if (apiKeyStore.existsByEquipmentId(gateway.equipmentId())) {
            throw alreadyProvisioned();
        }

        // Generate a cryptographically secure random API key.
        byte[] randomBytes = new byte[API_KEY_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);

        String apiKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        // Persist only the BCrypt hash.
        String apiKeyHash = hashingService.encode(apiKey);

        try {
            apiKeyStore.create(
                    gateway.equipmentId(),
                    apiKeyHash
            );
        } catch (DataIntegrityViolationException exception) {
            // Handles concurrent provisioning requests.
            throw alreadyProvisioned();
        }

        return new IssuedApiKey(
                gateway.sensorExternalId(),
                apiKey
        );
    }

    /**
     * Tells whether an API key has already been generated for an Edge Gateway.
     *
     * <p>Clients use it to offer or hide the generation of the key, which is shown only once. Neither the key
     * nor its hash is returned.</p>
     *
     * @param laboratoryId owning laboratory identifier
     * @param equipmentId Edge Gateway equipment identifier
     * @return true when the Gateway already has an API key
     * @throws ApplicationException if the Gateway is not found in the laboratory
     */
    @Transactional(readOnly = true)
    public boolean isProvisioned(
            Long laboratoryId,
            Long equipmentId) {

        var gateway = equipmentService.findEdgeGateway(
                laboratoryId,
                equipmentId
        ).orElseThrow(() -> new ApplicationException(
                ApplicationError.notFound(
                        "EdgeGateway",
                        equipmentId
                )
        ));

        return apiKeyStore.existsByEquipmentId(gateway.equipmentId());
    }

    /**
     * Builds the conflict raised when credentials already exist.
     *
     * @return API key provisioning conflict
     */
    private static ApplicationException alreadyProvisioned() {
        return new ApplicationException(
                ApplicationError.conflict(
                        "EdgeGatewayApiKey",
                        "An API key has already been provisioned "
                                + "for this Gateway"
                )
        );
    }

    /**
     * Result of provisioning an Edge Gateway API key.
     *
     * @param sensorExternalId public Gateway identifier
     * @param apiKey one-time plaintext API key
     */
    public record IssuedApiKey(
            String sensorExternalId,
            String apiKey
    ) {}
}