package com.iotech.qualitrack.platform.ca.interfaces.acl;

import java.math.BigDecimal;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventType;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * ACL facade exposed by the Compliance and Alerts bounded context.
 *
 * <p>Provides a stable interface for other bounded contexts to interact with CA
 * without depending on CA internal application services or domain commands.</p>
 */
@Service
public class ComplianceContextFacade {

    private final ComplianceEventRepository complianceEventRepository;
    private final DeviationAlertRepository deviationAlertRepository;

    /**
     * Creates a new ComplianceContextFacade.
     *
     * @param complianceEventRepository compliance event repository
     * @param deviationAlertRepository deviation alert repository
     */
    public ComplianceContextFacade(ComplianceEventRepository complianceEventRepository,
                                   DeviationAlertRepository deviationAlertRepository) {
        this.complianceEventRepository = complianceEventRepository;
        this.deviationAlertRepository = deviationAlertRepository;
    }

    /**
     * Alerts of an environment and its containers whose incident started in a period, oldest first (US95).
     *
     * @param laboratoryId the laboratory of the environment
     * @param environmentId the environment
     * @param from start of the period, inclusive
     * @param to end of the period, inclusive
     * @return the alerts with their current status
     */
    public List<AlertReference> findEnvironmentAlerts(Long laboratoryId, Long environmentId, Instant from, Instant to) {
        if (laboratoryId == null || environmentId == null || from == null || to == null) return List.of();
        return deviationAlertRepository.findAllByLaboratoryIdAndEnvironmentId(laboratoryId, environmentId).stream()
                .filter(alert -> alert.detectedAt() != null && !alert.detectedAt().isBefore(from) && !alert.detectedAt().isAfter(to))
                .sorted(Comparator.comparing(DeviationAlert::detectedAt))
                .map(alert -> new AlertReference(alert.getId(), alert.getEquipmentId(),
                        alert.getOrigin() == null ? null : alert.getOrigin().name(), alert.getParameterName(),
                        alert.getRecordedValue(), alert.getThresholdValue(), alert.getUnit(), alert.detectedAt(),
                        alert.getSeverity().name(), alert.getStatus().name(), alert.getDeviationCount(),
                        alert.getResolutionNotes()))
                .toList();
    }

    /**
     * Deviation alert shared with other bounded contexts.
     *
     * @param id the alert
     * @param deviceId the environmental device or container monitor that detected it
     * @param origin ENVIRONMENT or CONTAINER
     * @param parameterName the monitored variable
     * @param recordedValue the value of the deviation that set the current severity
     * @param thresholdValue the limit crossed by that value
     * @param unit the unit of the value
     * @param detectedAt when the incident started
     * @param severity the highest severity of the incident
     * @param status the current status
     * @param deviationCount deviations grouped in the incident
     * @param resolutionNotes notes of the resolution, if resolved
     */
    public record AlertReference(Long id, Long deviceId, String origin, String parameterName, Double recordedValue,
                                 Double thresholdValue, String unit, Instant detectedAt, String severity,
                                 String status, Integer deviationCount, String resolutionNotes) {
    }

    /**
     * Records a raw material low stock compliance event.
     *
     * @param rawMaterialId the raw material identifier
     * @param laboratoryId the laboratory identifier
     * @param materialName the raw material name
     * @param currentStock the current stock quantity
     * @param minimumThreshold the minimum required stock quantity
     */
    public void recordRawMaterialLowStockEvent(
            Long rawMaterialId,
            Long laboratoryId,
            String materialName,
            BigDecimal currentStock,
            BigDecimal minimumThreshold
    ) {
        complianceEventRepository.save(new ComplianceEvent(
                rawMaterialId,
                ComplianceEventType.RAW_MATERIAL_LOW_STOCK,
                "Raw material '%s' from laboratory ID %d is below minimum stock. Current stock: %s, minimum threshold: %s."
                        .formatted(materialName, laboratoryId, currentStock, minimumThreshold),
                Instant.now().toString(),
                null
        ));
    }

    /**
     * Verifies whether a batch can be released according to compliance rules.
     *
     * <p>At this stage, the rule is intentionally conservative and simple:
     * a batch can be released when there are no unresolved non-compliance events
     * associated with that batch identifier.</p>
     *
     * @param batchId the batch identifier
     * @return true when the batch can be released
     */
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
