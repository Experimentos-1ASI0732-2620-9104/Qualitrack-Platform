package com.iotech.qualitrack.platform.iam.domain.model.valueobjects;

/**
 * Edge Gateway as IAM needs it to issue its API key and to authenticate it.
 *
 * @param equipmentId      the equipment identifier of the Gateway
 * @param laboratoryId     the laboratory that owns the Gateway
 * @param environmentId    the environment where the Gateway is located, or null when it is not located
 * @param sensorExternalId the identifier with which the Gateway signs its requests
 * @param operational      true when the Gateway is operational
 */
public record EdgeGatewayIdentity(Long equipmentId, Long laboratoryId, Long environmentId, String sensorExternalId,
                                  boolean operational) {

    /**
     * Whether the Gateway is located in an environment.
     *
     * @return true when it has an environment
     */
    public boolean isLocated() {
        return environmentId != null;
    }

    /**
     * Whether the Gateway may hold and use an API key: operational and located in an environment.
     *
     * @return true when it is operational and located
     */
    public boolean canConnect() {
        return operational && isLocated();
    }
}
