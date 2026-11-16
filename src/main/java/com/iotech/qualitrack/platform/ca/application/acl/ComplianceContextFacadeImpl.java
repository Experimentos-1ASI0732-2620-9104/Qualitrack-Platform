package com.iotech.qualitrack.platform.ca.application.acl;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.ca.interfaces.acl.ComplianceContextFacade;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application-layer implementation of the {@link ComplianceContextFacade} ACL facade.
 */
@Service
public class ComplianceContextFacadeImpl implements ComplianceContextFacade {

    private final ComplianceEventRepository complianceEventRepository;
    private final DeviationAlertRepository deviationAlertRepository;

    /**
     * Creates the facade implementation.
     *
     * @param complianceEventRepository compliance event repository
     * @param deviationAlertRepository deviation alert repository
     */
    public ComplianceContextFacadeImpl(ComplianceEventRepository complianceEventRepository,
                                       DeviationAlertRepository deviationAlertRepository) {
        this.complianceEventRepository = complianceEventRepository;
        this.deviationAlertRepository = deviationAlertRepository;
    }

    @Override
    public List<AlertReference> findEnvironmentAlerts(Long laboratoryId, Long environmentId, Instant from, Instant to) {
        if (laboratoryId == null || environmentId == null || from == null || to == null) return List.of();
        return deviationAlertRepository.findAllByLaboratoryIdAndEnvironmentId(laboratoryId, environmentId).stream()
                .filter(alert -> alert.detectedAt() != null && !alert.detectedAt().isBefore(from) && !alert.detectedAt().isAfter(to))
                .sorted(Comparator.comparing(DeviationAlert::detectedAt))
                .map(ComplianceContextFacadeImpl::toReference)
                .toList();
    }

    @Override
    public List<AlertReference> findBatchAlerts(Long batchId) {
        if (batchId == null) return List.of();
        return deviationAlertRepository.findAllByBatchId(batchId).stream()
                .map(ComplianceContextFacadeImpl::toReference)
                .toList();
    }

    @Override
    public List<AlertReference> findDeviceAlerts(Collection<Long> deviceIds) {
        return deviceIds.stream()
                .flatMap(deviceId -> deviationAlertRepository.findAllByEquipmentId(deviceId).stream())
                .map(ComplianceContextFacadeImpl::toReference)
                .toList();
    }

    private static AlertReference toReference(DeviationAlert alert) {
        return new AlertReference(alert.getId(), alert.getEquipmentId(),
                alert.getOrigin() == null ? null : alert.getOrigin().name(), alert.getParameterName(),
                alert.getRecordedValue(), alert.getThresholdValue(), alert.getUnit(), alert.detectedAt(),
                alert.getSeverity().name(), alert.getStatus().name(), alert.getDeviationCount(),
                alert.getResolutionNotes(), alert.getTimestamp());
    }

    @Override
    public boolean canReleaseBatch(Long batchId) {
        if (batchId == null || batchId <= 0) {
            return false;
        }

        return complianceEventRepository.findAllByRelatedEntityId(batchId).stream()
                .filter(event -> event.getEventType().subject()
                        == com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventSubject.BATCH)
                .noneMatch(event -> !event.isResolved());
    }
}
