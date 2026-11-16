package com.iotech.qualitrack.platform.equipment.interfaces.acl;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository;
import com.iotech.qualitrack.platform.shared.application.security.ResourceOwner;
import com.iotech.qualitrack.platform.shared.application.security.TenantResourceLookup;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;

/**
 * Tells the tenant access check which laboratory owns an equipment or IoT device, so the {@code equipmentId} and
 * {@code deviceId} path variables are only available to accounts of that laboratory.
 */
@Component
public class EquipmentTenantResourceLookup implements TenantResourceLookup {
    private final EquipmentRepository equipment;

    /**
     * Creates the lookup with the equipment repository.
     *
     * @param equipment equipment persistence port
     */
    public EquipmentTenantResourceLookup(EquipmentRepository equipment) { this.equipment = equipment; }

    /**
     * Path variables this lookup resolves.
     *
     * @return {@code equipmentId} and {@code deviceId}
     */
    public Set<String> types() { return Set.of("equipmentId", "deviceId"); }

    /**
     * Finds the laboratory that owns the equipment or device.
     *
     * @param type path variable name
     * @param id equipment or device id
     * @return the owning laboratory, or empty when the equipment does not exist
     */
    public Optional<ResourceOwner> owner(String type, Long id) {
        return equipment.findById(id).map(item -> new ResourceOwner("laboratoryId", item.getLabId()));
    }
}
