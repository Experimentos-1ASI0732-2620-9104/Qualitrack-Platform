package com.iotech.qualitrack.platform.equipment.interfaces.acl;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository;
import com.iotech.qualitrack.platform.shared.application.security.ResourceOwner;
import com.iotech.qualitrack.platform.shared.application.security.TenantResourceLookup;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;

@Component
public class EquipmentTenantResourceLookup implements TenantResourceLookup {
    private final EquipmentRepository equipment;
    public EquipmentTenantResourceLookup(EquipmentRepository equipment) { this.equipment = equipment; }
    public Set<String> types() { return Set.of("equipmentId", "deviceId"); }
    public Optional<ResourceOwner> owner(String type, Long id) {
        return equipment.findById(id).map(item -> new ResourceOwner("laboratoryId", item.getLabId()));
    }
}
