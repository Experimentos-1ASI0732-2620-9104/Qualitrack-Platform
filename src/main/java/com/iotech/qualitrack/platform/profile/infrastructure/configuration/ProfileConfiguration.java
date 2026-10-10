package com.iotech.qualitrack.platform.profile.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProfileConfiguration {
    /** Clock used to date the profile photos. */
    @Bean
    public Clock profileClock() { return Clock.systemUTC(); }
}
