package com.iotech.qualitrack.platform.tracking.infrastructure.configuration;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ExpectedCommunicationPeriod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
public class TrackingConfiguration {
    @Bean
    public Clock trackingClock() { return Clock.system(ZoneId.of("America/Lima")); }

    /** Silence after which an IoT device requires review; override with tracking.devices.expected-communication-period. */
    @Bean
    public ExpectedCommunicationPeriod expectedCommunicationPeriod(
            @Value("${tracking.devices.expected-communication-period:PT5M}") Duration period) {
        return new ExpectedCommunicationPeriod(period);
    }
}
