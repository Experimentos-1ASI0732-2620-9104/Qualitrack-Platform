package com.iotech.qualitrack.platform.laboratory.interfaces.acl;

import com.iotech.qualitrack.platform.laboratory.domain.repositories.*;
import com.iotech.qualitrack.platform.shared.application.security.*;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;

@Component
public class LaboratoryTenantResourceLookup implements TenantResourceLookup {
    private final RawMaterialRepository materials;
    private final StaffRepository staff;
    private final EnvironmentRepository environments;
    public LaboratoryTenantResourceLookup(RawMaterialRepository materials, StaffRepository staff,
                                          EnvironmentRepository environments) {
        this.materials = materials;
        this.staff = staff;
        this.environments = environments;
    }
    public Set<String> types() { return Set.of("legacyRawMaterialId", "staffId", "environmentId"); }
    public Optional<ResourceOwner> owner(String type, Long id) {
        return switch (type) {
            case "legacyRawMaterialId" -> materials.findById(id).map(item -> new ResourceOwner("laboratoryId", item.getLaboratoryId()));
            case "staffId" -> staff.findById(id).map(item -> new ResourceOwner("laboratoryId", item.getLaboratoryId()));
            case "environmentId" -> environments.findById(id).map(item -> new ResourceOwner("laboratoryId", item.getLaboratoryId()));
            default -> Optional.empty();
        };
    }
}
