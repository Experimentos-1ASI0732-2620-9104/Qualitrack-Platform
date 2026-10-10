package com.iotech.qualitrack.platform.ra.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reporting &amp; Audit configuration: the calendar days of a report are those of the laboratory (America/Lima), as
 * in Inventory, Equipment and Tracking.
 */
@Configuration
public class RaConfiguration {
    @Bean
    public Clock reportingClock() { return Clock.system(ZoneId.of("America/Lima")); }
}
