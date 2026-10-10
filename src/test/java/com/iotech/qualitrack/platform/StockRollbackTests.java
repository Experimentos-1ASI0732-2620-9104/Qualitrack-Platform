package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.batch.application.commandservices.RawMaterialUsageCommandService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterRawMaterialUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.RawMaterialUsageRepository;
import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryCommandService;
import com.iotech.qualitrack.platform.inventory.application.queryservices.InventoryQueryService;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.ReceiveRawMaterialBatchCommand;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.ReviewRawMaterialBatchCommand;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.SaveRawMaterialCommand;
import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalLaboratoryService;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:stock_rollback;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class StockRollbackTests {
    @Autowired RawMaterialUsageCommandService commands;
    @Autowired InventoryCommandService inventoryCommands;
    @Autowired InventoryQueryService inventoryQueries;
    @Autowired BatchRepository batches;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("inventoryClock") Clock clock;
    @MockitoSpyBean RawMaterialUsageRepository usages;
    @MockitoBean CurrentUser actor;
    @MockitoBean InventoryExternalLaboratoryService laboratories;

    @BeforeEach void actor() { when(actor.userId()).thenReturn(27L);
        when(laboratories.existsEnvironment(42L, 1L)).thenReturn(true); }

    @Test
    void aFailedUsageWriteRollsBackTheAlreadyDecrementedInventory() {
        var today = LocalDate.now(clock);
        var material = inventoryCommands.handle(new SaveRawMaterialCommand(42L, 1L, null, "RM-" + UUID.randomUUID(),
                "Rollback fixture", "kg", BigDecimal.TEN)).toOptional().orElseThrow();
        var receipt = inventoryCommands.handle(new ReceiveRawMaterialBatchCommand(42L, 1L, material.getId(), "Supplier", "SUP-1",
                "kg", new BigDecimal("100"), today.minusDays(1), today.plusYears(1))).toOptional().orElseThrow();
        inventoryCommands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, material.getId(), receipt.getId(), RawMaterialBatchStatus.RELEASED,
                "Reviewed")).toOptional().orElseThrow();
        var batch = batches.save(new Batch(null, 42L, 1L, 1L, "Fixture", "ROLLBACK-LOT", 10.0,
                "units", BatchStatus.IN_PROGRESS, "2026-09-08", null, "Test"));
        doThrow(new IllegalStateException("Simulated usage write failure")).when(usages).save(any());
        assertThatThrownBy(() -> commands.handle(new RegisterRawMaterialUsageCommand(42L, 1L, 1L, batch.getId(),
                receipt.getId(), new BigDecimal("50"), "kg", UUID.randomUUID().toString())))
                .hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("Simulated usage write failure");
        assertThat(inventoryQueries.handle(new GetRawMaterialBatchesQuery(42L, 1L, material.getId())).getFirst().getAvailableAmount())
                .isEqualByComparingTo("100");
        assertThat(usages.findAllByRawMaterialId(material.getId())).isEmpty();
    }
}
