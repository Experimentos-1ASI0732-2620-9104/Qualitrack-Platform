package com.iotech.qualitrack.platform.equipment.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class EquipmentConfiguration {
    /** Clock used to date status changes and to reject maintenance dated in the future. */
    @Bean
    public Clock equipmentClock() { return Clock.system(ZoneId.of("America/Lima")); }
}
