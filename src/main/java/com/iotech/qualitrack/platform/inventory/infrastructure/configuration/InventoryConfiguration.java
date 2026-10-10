package com.iotech.qualitrack.platform.inventory.infrastructure.configuration;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.NearExpiryPeriod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class InventoryConfiguration {
    @Bean
    public Clock inventoryClock() { return Clock.system(ZoneId.of("America/Lima")); }

    /** Default near expiry period for raw material lots; override with inventory.near-expiry-days. */
    @Bean
    public NearExpiryPeriod inventoryNearExpiryPeriod(@Value("${inventory.near-expiry-days:30}") int days) {
        return new NearExpiryPeriod(days);
    }
}
