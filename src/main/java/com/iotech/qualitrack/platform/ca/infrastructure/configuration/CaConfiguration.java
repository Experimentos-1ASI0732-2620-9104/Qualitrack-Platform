package com.iotech.qualitrack.platform.ca.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CaConfiguration {
    /** Clock used to date notifications, their reading and the e-mail notices of alerts. */
    @Bean
    public Clock complianceClock() { return Clock.systemUTC(); }
}
