package com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.LegacyRawMaterial;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LegacyInventoryFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application-level integration service for accessing Laboratory
 * bounded context information required by Inventory.
 *
 * <p>Uses the public Laboratory context facade to retrieve
 * laboratory and environment references without accessing
 * Laboratory persistence entities or repositories directly.</p>
 *
 */
@Service
public class InventoryExternalLaboratoryService {

    private final LaboratoryContextFacade laboratoryContextFacade;
    private final LegacyInventoryFacade legacyInventoryFacade;

    public InventoryExternalLaboratoryService(LaboratoryContextFacade laboratoryContextFacade,
                                              LegacyInventoryFacade legacyInventoryFacade) {
        this.laboratoryContextFacade = laboratoryContextFacade;
        this.legacyInventoryFacade = legacyInventoryFacade;
    }

    /**
     * Checks that the environment exists and belongs to the laboratory.
     *
     * @param laboratoryId the laboratory that must own the environment
     * @param environmentId the environment identifier
     * @return true when the environment belongs to the laboratory
     */
    public boolean existsEnvironment(Long laboratoryId, Long environmentId) {
        return laboratoryContextFacade.existsEnvironment(laboratoryId, environmentId);
    }

    /**
     * Retrieves an environment reference from the Laboratory
     * bounded context.
     *
     * @param laboratoryId identifier of the laboratory owning the environment
     * @param environmentId identifier of the environment to retrieve
     * @return an optional containing the environment reference when found,
     *         or an empty optional when no matching environment exists
     */
    public Optional<LaboratoryContextFacade.EnvironmentReference> findEnvironment(Long laboratoryId, Long environmentId) {
        return laboratoryContextFacade.findEnvironment(laboratoryId, environmentId);
    }

    /**
     * Retrieves raw materials registered in the Laboratory bounded context
     * before the current Inventory module was introduced.
     *
     * @param laboratoryId identifier of the laboratory whose legacy
     *                     raw materials will be retrieved
     * @return a list of legacy raw materials converted into
     *         Inventory-specific representations
     */
    public List<LegacyRawMaterial> findLegacyMaterials(Long laboratoryId) {
        return legacyInventoryFacade.materials(laboratoryId).stream()
                .map(InventoryExternalLaboratoryService::toLegacyRawMaterial).toList();
    }

    /**
     * Retrieves and locks a legacy raw material during an inventory
     * import operation.
     *
     * <p>The locked material is converted into an Inventory-specific
     * value object before being returned to the application service.</p>
     *
     * @param laboratoryId identifier of the laboratory owning the legacy material
     * @param legacyId identifier of the legacy raw material to lock
     * @return the locked legacy raw material converted into an
     *         Inventory-specific representation
     * @throws IllegalArgumentException when the legacy material does not
     *                                  belong to the specified laboratory
     *                                  or cannot be found
     */
    public LegacyRawMaterial lockLegacyMaterial(Long laboratoryId, Long legacyId) {
        return toLegacyRawMaterial(legacyInventoryFacade.lock(laboratoryId, legacyId));
    }

    /**
     * Converts a legacy raw material reference from the Laboratory
     * bounded context into an Inventory-specific value object.
     *
     * <p>This conversion isolates Inventory from the internal
     * representation of legacy laboratory materials.</p>
     *
     * @param material legacy raw material information obtained
     *                 from the Laboratory integration facade
     * @return an Inventory-specific representation of the legacy material
     */
    private static LegacyRawMaterial toLegacyRawMaterial(LegacyInventoryFacade.Material material) {
        return new LegacyRawMaterial(material.id(), material.code(), material.name(), material.unit(),
                material.minimumStock(), material.supplier(), material.batchNumber(), material.expiresOn(),
                material.balance());
    }
}
