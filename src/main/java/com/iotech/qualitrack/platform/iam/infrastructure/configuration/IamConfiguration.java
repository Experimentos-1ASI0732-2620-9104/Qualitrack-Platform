package com.iotech.qualitrack.platform.iam.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class IamConfiguration {
    /** Clock used to date password recoveries and expire their codes. */
    @Bean
    public Clock iamClock() { return Clock.systemUTC(); }
}
