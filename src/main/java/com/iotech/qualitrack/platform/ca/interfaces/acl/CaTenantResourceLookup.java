package com.iotech.qualitrack.platform.ca.interfaces.acl;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.shared.application.security.ResourceOwner;
import com.iotech.qualitrack.platform.shared.application.security.TenantResourceLookup;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;

@Component
public class CaTenantResourceLookup implements TenantResourceLookup {
    private final DeviationAlertRepository alerts;
    public CaTenantResourceLookup(DeviationAlertRepository alerts) { this.alerts = alerts; }
    public Set<String> types() { return Set.of("alertId"); }
    public Optional<ResourceOwner> owner(String type, Long id) {
        return alerts.findById(id).map(item -> item.getLaboratoryId() != null
                ? new ResourceOwner("laboratoryId", item.getLaboratoryId())
                : new ResourceOwner("equipmentId", item.getEquipmentId()));
    }
}
