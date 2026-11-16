package com.iotech.qualitrack.platform.tracking.application;

import com.iotech.qualitrack.platform.tracking.application.internal.outboundservices.acl.TrackingExternalEquipmentService;
import com.iotech.qualitrack.platform.tracking.application.internal.queryservices.TrackingQueryServiceImpl;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetDeviceConnectionQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.DeviceConnection;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.DeviceConnectionStatus;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ExpectedCommunicationPeriod;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredDevice;
import com.iotech.qualitrack.platform.tracking.domain.repositories.ActuationEventRepository;
import com.iotech.qualitrack.platform.tracking.domain.repositories.EnvironmentalProfileRepository;
import com.iotech.qualitrack.platform.tracking.domain.repositories.MeasurementRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrackingQueryTests {

    @Test
    void anActionOfTheDeviceAlsoCountsAsCommunication() {
        var now = Instant.parse("2026-10-03T15:00:00Z");
        var measurements = mock(MeasurementRepository.class);
        var actions = mock(ActuationEventRepository.class);
        var equipment = mock(TrackingExternalEquipmentService.class);
        var service = new TrackingQueryServiceImpl(mock(EnvironmentalProfileRepository.class), measurements, actions,
                equipment, new ExpectedCommunicationPeriod(Duration.ofMinutes(5)), Clock.fixed(now, ZoneOffset.UTC));
        when(equipment.findDevice(1L, 2L, 7L)).thenReturn(Optional.of(
                new MonitoredDevice(7L, "CONTAINER_MONITOR")));
        when(measurements.findLastReceivedAt(7L)).thenReturn(Optional.of(now.minusSeconds(900)));
        when(actions.findLastReceivedAt(7L)).thenReturn(Optional.of(now.minusSeconds(60)));

        var connection = service.handle(new GetDeviceConnectionQuery(1L, 2L, 7L)).orElseThrow();

        assertThat(connection.status()).isEqualTo(DeviceConnectionStatus.CONNECTED);
        assertThat(connection.lastCommunicationAt()).isEqualTo(now.minusSeconds(60));
        assertThat(service.handle(new GetDeviceConnectionQuery(1L, 2L, 8L))).isEmpty();
    }

    @Test
    void deviceRequiresReviewWhenSilentForLongerThanTheExpectedPeriod() {
        var period = new ExpectedCommunicationPeriod(Duration.ofMinutes(5));
        var now = Instant.parse("2026-10-03T15:00:00Z");

        assertThat(DeviceConnection.evaluate(7L, now.minusSeconds(300), now, period).status())
                .isEqualTo(DeviceConnectionStatus.CONNECTED);
        assertThat(DeviceConnection.evaluate(7L, now.minusSeconds(301), now, period).status())
                .isEqualTo(DeviceConnectionStatus.REQUIRES_REVIEW);
        assertThat(DeviceConnection.evaluate(7L, null, now, period).status())
                .isEqualTo(DeviceConnectionStatus.REQUIRES_REVIEW);
        assertThatThrownBy(() -> new ExpectedCommunicationPeriod(Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
