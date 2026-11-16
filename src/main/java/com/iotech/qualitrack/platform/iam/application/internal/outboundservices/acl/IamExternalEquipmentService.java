package com.iotech.qualitrack.platform.iam.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.EdgeGatewayIdentity;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ACL service used by IAM to read, through Equipment, the Edge Gateways whose API keys it issues and checks. The
 * Gateway is equipment of the laboratory; IAM only keeps the hash of its key.
 */
@Service
public class IamExternalEquipmentService {
    private final EquipmentContextFacade equipmentContextFacade;

    public IamExternalEquipmentService(EquipmentContextFacade equipmentContextFacade) {
        this.equipmentContextFacade = equipmentContextFacade;
    }

    /**
     * Edge Gateway of a laboratory.
     *
     * @param laboratoryId the laboratory of the Gateway
     * @param equipmentId  the equipment identifier of the Gateway
     * @return the Gateway, or empty when the laboratory has no Gateway with that identifier
     */
    public Optional<EdgeGatewayIdentity> findEdgeGateway(Long laboratoryId, Long equipmentId) {
        return equipmentContextFacade.findEdgeGateway(laboratoryId, equipmentId).map(IamExternalEquipmentService::toIdentity);
    }

    /**
     * Edge Gateway by the identifier with which it signs its requests.
     *
     * @param sensorExternalId the identifier of the Gateway
     * @return the Gateway, or empty when no Gateway has that identifier
     */
    public Optional<EdgeGatewayIdentity> findEdgeGatewayByExternalId(String sensorExternalId) {
        return equipmentContextFacade.findEdgeGatewayByExternalId(sensorExternalId)
                .map(IamExternalEquipmentService::toIdentity);
    }

    private static EdgeGatewayIdentity toIdentity(EquipmentContextFacade.EdgeGatewayReference gateway) {
        return new EdgeGatewayIdentity(gateway.equipmentId(), gateway.laboratoryId(), gateway.environmentId(),
                gateway.sensorExternalId(), gateway.isOperational());
    }
}
