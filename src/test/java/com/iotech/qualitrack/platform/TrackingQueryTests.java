package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade.DeviceReference;
import com.iotech.qualitrack.platform.tracking.application.internal.outboundservices.acl.TrackingExternalEquipmentService;
import com.iotech.qualitrack.platform.tracking.application.internal.queryservices.TrackingQueryServiceImpl;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetMeasurementsQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ExpectedCommunicationPeriod;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import com.iotech.qualitrack.platform.tracking.domain.repositories.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TrackingQueryTests {
    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-02T00:00:00Z");

    @Test void measurementsRetainDistinctParametersUnitsAndDevice() {
        var repository = mock(MeasurementRepository.class);
        var equipment = mock(TrackingExternalEquipmentService.class);
        var service = new TrackingQueryServiceImpl(mock(EnvironmentalProfileRepository.class), repository,
                mock(ActuationEventRepository.class), equipment, new ExpectedCommunicationPeriod(Duration.ofMinutes(5)),
                Clock.fixed(TO, ZoneOffset.UTC));
        var monitor = new DeviceReference(23L, 1L, 2L, "Cold storage A", "CONTAINER_MONITOR", "SENSOR-23");
        var temperature = Measurement.receive(1L, 2L, 23L, MonitoredMetric.TEMPERATURE, 22.0, null,
                Instant.parse("2026-09-01T12:00:00Z"), null, null);
        var humidity = Measurement.receive(1L, 2L, 23L, MonitoredMetric.HUMIDITY, 60.0, null,
                Instant.parse("2026-09-01T12:00:00Z"), null, null);
        var older = Measurement.receive(1L, 2L, 23L, MonitoredMetric.TEMPERATURE, 20.0, null,
                Instant.parse("2026-09-01T11:00:00Z"), null, null);
        when(equipment.findContainerMonitor(1L, 2L, 23L)).thenReturn(Optional.of(monitor));
        when(repository.findByDeviceAndPeriod(23L, null, FROM, TO)).thenReturn(List.of(older, temperature, humidity));
        when(repository.findByDeviceAndPeriod(23L, "TEMPERATURE", FROM, TO)).thenReturn(List.of(older, temperature));

        var all = service.handle(new GetMeasurementsQuery(1L, 2L, 23L, null, FROM, TO)).orElseThrow();
        assertThat(all).containsExactly(older, temperature, humidity);
        assertThat(all).extracting(Measurement::getParameterName).containsExactly("TEMPERATURE", "TEMPERATURE", "HUMIDITY");
        assertThat(all).extracting(Measurement::getUnit).containsExactly("°C", "°C", "%RH");
        assertThat(all).extracting(Measurement::getEquipmentId).containsOnly(23L);
        assertThat(service.handle(new GetMeasurementsQuery(1L, 2L, 23L, MonitoredMetric.TEMPERATURE, FROM, TO)).orElseThrow())
                .containsExactly(older, temperature);
        verify(repository).findByDeviceAndPeriod(23L, "TEMPERATURE", FROM, TO);

        when(equipment.findContainerMonitor(9L, 2L, 23L)).thenReturn(Optional.empty());
        assertThat(service.handle(new GetMeasurementsQuery(9L, 2L, 23L, null, FROM, TO))).isEmpty();
    }
}
