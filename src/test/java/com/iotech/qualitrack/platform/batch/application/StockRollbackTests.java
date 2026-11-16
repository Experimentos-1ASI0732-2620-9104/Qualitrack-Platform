package com.iotech.qualitrack.platform.batch.application;

import com.iotech.qualitrack.platform.testing.LimaDates;
import com.iotech.qualitrack.platform.batch.application.commandservices.RawMaterialUsageCommandService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterRawMaterialUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.RawMaterialUsageRepository;
import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryCommandService;
import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalLaboratoryService;
import com.iotech.qualitrack.platform.inventory.application.queryservices.InventoryQueryService;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.ReceiveRawMaterialBatchCommand;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.ReviewRawMaterialBatchCommand;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.SaveRawMaterialCommand;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.GetRawMaterialBatchesQuery;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:stock_rollback;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class StockRollbackTests {
    private static final Long LAB = 42L;
    private static final Long ENV = 7L;
    @Autowired RawMaterialUsageCommandService usageCommands;
    @Autowired InventoryCommandService inventory;
    @Autowired InventoryQueryService inventoryQueries;
    @Autowired BatchRepository batches;
    @MockitoSpyBean RawMaterialUsageRepository usages;
    @MockitoBean CurrentUser actor;
    @MockitoBean InventoryExternalLaboratoryService laboratories;

    @Test
    void aFailedUsageWriteRollsBackTheInventoryConsumption() {
        when(actor.userId()).thenReturn(27L);
        when(laboratories.existsEnvironment(LAB, ENV)).thenReturn(true);
        var material = inventory.handle(new SaveRawMaterialCommand(LAB, ENV, null, "RM-ROLLBACK", "Rollback fixture", "kg",
                BigDecimal.TEN)).toOptional().orElseThrow();
        var lot = inventory.handle(new ReceiveRawMaterialBatchCommand(LAB, ENV, material.getId(), "Supplier", "SUP-1", "kg",
                new BigDecimal("100"), LimaDates.today().minusDays(1), LimaDates.today().plusYears(1))).toOptional().orElseThrow();
        inventory.handle(new ReviewRawMaterialBatchCommand(LAB, ENV, material.getId(), lot.getId(), RawMaterialBatchStatus.RELEASED,
                "Certificate checked")).toOptional().orElseThrow();
        var batch = batches.save(new Batch(null, LAB, ENV, 1L, "Fixture", "ROLLBACK-LOT", 10.0,
                "units", BatchStatus.IN_PROGRESS, "2026-09-08", null, "Test"));
        doThrow(new IllegalStateException("Simulated usage write failure")).when(usages).save(any());

        assertThatThrownBy(() -> usageCommands.handle(new RegisterRawMaterialUsageCommand(LAB, ENV, 1L, batch.getId(), lot.getId(),
                new BigDecimal("50"), "kg", "rollback-operation")))
                .hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("Simulated usage write failure");
        assertThat(inventoryQueries.handle(new GetRawMaterialBatchesQuery(LAB, ENV, material.getId())).getFirst().getAvailableAmount())
                .isEqualByComparingTo("100");
        assertThat(usages.findAllByBatchId(batch.getId())).isEmpty();
    }
}
