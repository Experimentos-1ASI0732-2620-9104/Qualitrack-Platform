package com.iotech.qualitrack.platform.ca.application.internal.commandservices;

import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalEquipmentService;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.commands.AcknowledgeAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.CreateDeviationAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.ResolveAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertCreatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertEscalatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.*;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.NotificationPreferenceRepository;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-04T15:00:00Z");
    private static final Instant DETECTED_AT = Instant.parse("2026-10-04T14:00:00Z");

    @Mock
    private DeviationAlertRepository deviationAlertRepository;
    @Mock
    private ComplianceEventRepository complianceEventRepository;
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock
    private CaExternalEquipmentService caExternalEquipmentService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CaCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CaCommandServiceImpl(deviationAlertRepository, complianceEventRepository,
                notificationPreferenceRepository, caExternalEquipmentService, eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static CreateDeviationAlertCommand deviation(AlertSeverity severity) {
        return new CreateDeviationAlertCommand(1L, 2L, 3L, 100L, "TEMPERATURE", 33.0, 30.0, "°C", severity, DETECTED_AT);
    }

    /**
     * Copies the alert with the id the database would assign.
     */
    private static DeviationAlert stored(DeviationAlert alert, Long id) {
        return new DeviationAlert(id, alert.getLaboratoryId(), alert.getEnvironmentId(), alert.getOrigin(),
                alert.getEquipmentId(), alert.getBatchId(), alert.getMeasurementId(), alert.getLastMeasurementId(),
                alert.getParameterName(), alert.getRecordedValue(), alert.getThresholdValue(), alert.getUnit(),
                alert.getTimestamp(), alert.getSeverity(), alert.getStatus(), alert.getDeviationCount(),
                alert.getLastDetectedAt(), alert.getNormalizedAt(), alert.getAcknowledgedBy(), alert.getAcknowledgedAt(),
                alert.getResolvedBy(), alert.getResolvedAt(), alert.getResolutionNotes());
    }

    private void givenTheDeviceIsInTheEnvironment() {
        when(caExternalEquipmentService.findSource(1L, 2L, 3L))
                .thenReturn(Optional.of(new CaExternalEquipmentService.AlertSource(3L, "Monitor 1", AlertOrigin.ENVIRONMENT)));
    }

    private void givenSavingReturnsTheAlertWithId(Long id) {
        when(deviationAlertRepository.save(any())).thenAnswer(call -> stored(call.getArgument(0), id));
    }

    @Test
    void aFirstDeviationOpensANewAlert() {
        givenTheDeviceIsInTheEnvironment();
        when(deviationAlertRepository.findOpenByEquipmentIdAndParameterName(3L, "TEMPERATURE")).thenReturn(Optional.empty());
        givenSavingReturnsTheAlertWithId(10L);

        var result = service.handle(deviation(AlertSeverity.WARNING));

        assertThat(result).isInstanceOf(Result.Success.class);
        var registration = ((Result.Success<DeviationRegistration, ?>) result).value();
        assertThat(registration.created()).isTrue();
        assertThat(registration.alert().getId()).isEqualTo(10L);

        var event = ArgumentCaptor.forClass(ComplianceEvent.class);
        verify(complianceEventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(ComplianceEventType.DEVIATION_ALERT_CREATED);
        assertThat(event.getValue().getTimestamp()).isEqualTo(NOW.toString());
        verify(eventPublisher).publishEvent(any(DeviationAlertCreatedEvent.class));
    }

    @Test
    void aWorseDeviationEscalatesTheOpenAlert() {
        givenTheDeviceIsInTheEnvironment();
        var open = stored(new DeviationAlert(deviation(AlertSeverity.WARNING), 3L, AlertOrigin.ENVIRONMENT), 10L);
        when(deviationAlertRepository.findOpenByEquipmentIdAndParameterName(3L, "TEMPERATURE")).thenReturn(Optional.of(open));
        givenSavingReturnsTheAlertWithId(10L);

        var result = service.handle(deviation(AlertSeverity.CRITICAL));

        var registration = ((Result.Success<DeviationRegistration, ?>) result).value();
        assertThat(registration.created()).isFalse();
        assertThat(registration.alert().getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(registration.alert().getDeviationCount()).isEqualTo(2);

        var event = ArgumentCaptor.forClass(ComplianceEvent.class);
        verify(complianceEventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(ComplianceEventType.DEVIATION_ALERT_ESCALATED);
        verify(eventPublisher).publishEvent(any(DeviationAlertEscalatedEvent.class));
    }

    @Test
    void aRepeatedDeviationIsRecordedWithoutPublishingEvents() {
        givenTheDeviceIsInTheEnvironment();
        var open = stored(new DeviationAlert(deviation(AlertSeverity.WARNING), 3L, AlertOrigin.ENVIRONMENT), 10L);
        when(deviationAlertRepository.findOpenByEquipmentIdAndParameterName(3L, "TEMPERATURE")).thenReturn(Optional.of(open));
        givenSavingReturnsTheAlertWithId(10L);

        service.handle(deviation(AlertSeverity.WARNING));

        var event = ArgumentCaptor.forClass(ComplianceEvent.class);
        verify(complianceEventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(ComplianceEventType.DEVIATION_REPEATED);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void aDeviceOutsideTheEnvironmentIsRejected() {
        when(caExternalEquipmentService.findSource(1L, 2L, 3L)).thenReturn(Optional.empty());

        var result = service.handle(deviation(AlertSeverity.WARNING));

        assertThat(result.isFailure()).isTrue();
        verify(deviationAlertRepository, never()).save(any());
    }

    @Test
    void acknowledgingAnUnknownAlertFails() {
        when(deviationAlertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.handle(new AcknowledgeAlertCommand(99L, 7L)).isFailure()).isTrue();
    }

    @Test
    void acknowledgingUsesTheClock() {
        var open = stored(new DeviationAlert(deviation(AlertSeverity.WARNING), 3L, AlertOrigin.ENVIRONMENT), 10L);
        when(deviationAlertRepository.findById(10L)).thenReturn(Optional.of(open));
        givenSavingReturnsTheAlertWithId(10L);

        var result = service.handle(new AcknowledgeAlertCommand(10L, 7L));

        assertThat(result).isInstanceOf(Result.Success.class);
        var saved = ArgumentCaptor.forClass(DeviationAlert.class);
        verify(deviationAlertRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(saved.getValue().getAcknowledgedAt()).isEqualTo(NOW);
    }

    @Test
    void resolvingAResolvedAlertIsAConflict() {
        var resolved = stored(new DeviationAlert(deviation(AlertSeverity.WARNING), 3L, AlertOrigin.ENVIRONMENT), 10L);
        resolved.resolve(new ResolveAlertCommand(10L, 7L, "Fixed"), NOW);
        when(deviationAlertRepository.findById(10L)).thenReturn(Optional.of(resolved));

        var result = service.handle(new ResolveAlertCommand(10L, 7L, "Again"));

        assertThat(result.isFailure()).isTrue();
        verify(deviationAlertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
