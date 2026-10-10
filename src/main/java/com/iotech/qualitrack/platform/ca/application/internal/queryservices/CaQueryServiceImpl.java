package com.iotech.qualitrack.platform.ca.application.internal.queryservices;

import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalTrackingService;
import com.iotech.qualitrack.platform.ca.application.queryservices.CaQueryService;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetAlertByIdQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetAlertDetailQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetAlertsQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetComplianceEventsByRelatedEntityIdQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetNotificationPreferenceByUserIdQuery;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertOrigin;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.DeviationAlertDetail;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Application service implementation that executes CA read queries.
 *
 * <p>Handles retrieval of deviation alerts, compliance events, and notification
 * preferences using the domain repository ports.</p>
 */
@Service
public class CaQueryServiceImpl implements CaQueryService {

    private final DeviationAlertRepository deviationAlertRepository;
    private final ComplianceEventRepository complianceEventRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final CaExternalTrackingService caExternalTrackingService;

    /**
     * The device acts and measures at almost the same time; actions this long before the detection belong to it.
     */
    private static final Duration ACTUATION_TOLERANCE = Duration.ofMinutes(1);

    public CaQueryServiceImpl(
            DeviationAlertRepository deviationAlertRepository,
            ComplianceEventRepository complianceEventRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            CaExternalTrackingService caExternalTrackingService
    ) {
        this.deviationAlertRepository = deviationAlertRepository;
        this.complianceEventRepository = complianceEventRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.caExternalTrackingService = caExternalTrackingService;
    }

    @Override
    public List<DeviationAlert> handle(GetAlertsQuery query) {
        return deviationAlertRepository.findAllByLaboratoryIdAndEnvironmentId(query.laboratoryId(), query.environmentId())
                .stream()
                .filter(alert -> query.deviceId() == null || query.deviceId().equals(alert.getEquipmentId()))
                .filter(alert -> query.status() == null || query.status() == alert.getStatus())
                .filter(alert -> query.severity() == null || query.severity() == alert.getSeverity())
                .filter(alert -> !query.activeOnly() || alert.isOpen())
                .toList();
    }

    @Override
    public Optional<DeviationAlert> handle(GetAlertByIdQuery query) {
        return deviationAlertRepository.findById(query.alertId());
    }

    @Override
    public Optional<DeviationAlertDetail> handle(GetAlertDetailQuery query) {
        return deviationAlertRepository.findById(query.alertId()).map(alert -> {
            var detectedAt = alert.detectedAt();
            if (alert.getOrigin() != AlertOrigin.CONTAINER || alert.getLaboratoryId() == null || detectedAt == null) {
                return new DeviationAlertDetail(alert, List.of());
            }
            var until = alert.getResolvedAt() != null ? alert.getResolvedAt() : Instant.now();
            return new DeviationAlertDetail(alert, caExternalTrackingService.findActuations(alert.getLaboratoryId(),
                    alert.getEquipmentId(), alert.getParameterName(), detectedAt.minus(ACTUATION_TOLERANCE), until));
        });
    }

    @Override
    public List<ComplianceEvent> handle(GetComplianceEventsByRelatedEntityIdQuery query) {
        var direct = complianceEventRepository.findAllByRelatedEntityId(query.relatedEntityId()).stream()
                .filter(event -> event.getEventType().subject() == query.subject());
        var alerts = switch (query.subject()) {
            case EQUIPMENT -> deviationAlertRepository.findAllByEquipmentId(query.relatedEntityId());
            case BATCH -> deviationAlertRepository.findAllByBatchId(query.relatedEntityId());
            default -> List.<DeviationAlert>of();
        };
        var related = alerts.stream().flatMap(alert -> complianceEventRepository.findAllByRelatedEntityId(alert.getId()).stream())
                .filter(event -> event.getEventType().subject()
                        == com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventSubject.ALERT);
        return java.util.stream.Stream.concat(direct, related)
                .sorted(java.util.Comparator.comparing(ComplianceEvent::getTimestamp)).toList();
    }

    @Override
    public Optional<NotificationPreference> handle(GetNotificationPreferenceByUserIdQuery query) {
        return notificationPreferenceRepository.findByUserId(query.userId());
    }
}
