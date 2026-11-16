package com.iotech.qualitrack.platform.tracking.infrastructure.configuration;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ExpectedCommunicationPeriod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

/**
 * Spring configuration of Tracking &amp; Telemetry.
 */
@Configuration
public class TrackingConfiguration {
    /**
     * Clock used to reject future readings and to evaluate the connection of the devices, in the time zone of the
     * laboratories (America/Lima).
     */
    @Bean
    public Clock trackingClock() { return Clock.system(ZoneId.of("America/Lima")); }

    /** Silence after which an IoT device requires review; override with tracking.devices.expected-communication-period. */
    @Bean
    public ExpectedCommunicationPeriod expectedCommunicationPeriod(
            @Value("${tracking.devices.expected-communication-period:PT5M}") Duration period) {
        return new ExpectedCommunicationPeriod(period);
    }
}
