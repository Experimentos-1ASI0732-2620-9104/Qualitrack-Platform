package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.ca.interfaces.acl.ComplianceContextFacade;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportingPeriod;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Anti-corruption service that reads, through Compliance &amp; Alerting, the alerts of environments, batches and devices.
 */
@Service
public class RaExternalComplianceService {
    private final ComplianceContextFacade complianceContextFacade;

    public RaExternalComplianceService(ComplianceContextFacade complianceContextFacade) {
        this.complianceContextFacade = complianceContextFacade;
    }

    /**
     * Alerts of an environment whose incident started in the period, oldest first.
     */
    public List<ComplianceContextFacade.AlertReference> findEnvironmentAlerts(Long laboratoryId, Long environmentId,
                                                                             ReportingPeriod period) {
        return complianceContextFacade.findEnvironmentAlerts(laboratoryId, environmentId, period.from(), period.to());
    }

    /**
     * Alerts recorded for a product batch, in the order they were recorded.
     */
    public List<ComplianceContextFacade.AlertReference> findBatchAlerts(Long batchId) {
        return complianceContextFacade.findBatchAlerts(batchId);
    }

    /**
     * Alerts detected by any of the given devices.
     */
    public List<ComplianceContextFacade.AlertReference> findDeviceAlerts(Collection<Long> deviceIds) {
        return complianceContextFacade.findDeviceAlerts(deviceIds);
    }
}
