package com.iotech.qualitrack.platform.ra.interfaces.acl;
import com.iotech.qualitrack.platform.ra.domain.repositories.AuditReportRepository;
import com.iotech.qualitrack.platform.shared.application.security.ResourceOwner;
import com.iotech.qualitrack.platform.shared.application.security.TenantResourceLookup;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;

/**
 * {@link TenantResourceLookup} implementation for the RA module.
 * Allows the security framework to resolve the owner (laboratoryId) of a resource
 * identified by {@code type} + {@code id}, so that
 * {@code @PreAuthorize("@tenantAccess.allows(...)")} annotations can enforce
 * multi-tenant access without controllers knowing the concrete repository.
 */
@Component
public class RaTenantResourceLookup implements TenantResourceLookup {
    private final AuditReportRepository reports;
    public RaTenantResourceLookup(AuditReportRepository reports) { this.reports = reports; }

    /** Resource types this module can resolve. Currently only reports. */
    public Set<String> types() { return Set.of("reportId"); }

    /**
     * Returns the owner (laboratoryId) of the requested resource.
     * <ul>
     *   <li>{@code Optional.empty()} if the resource does not exist -> security engine treats it as 404/403.</li>
     *   <li>{@code ResourceOwner} with field "laboratoryId" when it exists.</li>
     * </ul>
     */
    public Optional<ResourceOwner> owner(String type, Long id) {
        return reports.findById(id).map(item -> new ResourceOwner("laboratoryId", item.getLaboratoryId()));
    }
}
