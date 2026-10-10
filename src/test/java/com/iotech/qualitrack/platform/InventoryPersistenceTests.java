package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryImportService;
import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryCommandService;
import com.iotech.qualitrack.platform.inventory.application.queryservices.InventoryQueryService;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.*;
import com.iotech.qualitrack.platform.inventory.domain.model.queries.*;
import com.iotech.qualitrack.platform.inventory.interfaces.acl.InventoryContextFacade;
import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalLaboratoryService;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.EnvironmentInventoryController;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.ReviewRawMaterialBatchResource;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import com.iotech.qualitrack.platform.inventory.interfaces.acl.InventoryContextFacade.ConsumptionRequest;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.domain.repositories.*;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.RawMaterialRepository;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:inventory_receipts;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class InventoryPersistenceTests {
    @Autowired InventoryContextFacade inventory;
    @Autowired InventoryCommandService commands;
    @Autowired InventoryQueryService queries;
    @Autowired EnvironmentInventoryController controller;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("inventoryClock") java.time.Clock clock;
    private LocalDate today() { return LocalDate.now(clock); }
    @Autowired InventoryImportService imports;
    @Autowired BatchRepository batches;
    @Autowired RawMaterialRepository legacy;
    @MockitoSpyBean RawMaterialUsageRepository usages;
    @MockitoBean CurrentUser actor;
    @MockitoBean InventoryExternalLaboratoryService laboratories;

    @AfterEach void clearAuthentication() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private void authenticate(Long laboratoryId, String role) {
        var authorities = java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
        var principal = new com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl(
            27L, "Reviewer", "", laboratoryId, authorities, true);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @Test void onlyQualityReviewerInSameLaboratoryCanRelease() {
        var receipt = receive("kg", "100");
        var resource = new ReviewRawMaterialBatchResource(RawMaterialBatchStatus.RELEASED, "Certificate checked");
        authenticate(42L, "ROLE_LAB_OPERATOR");
        assertThatThrownBy(() -> controller.reviewBatch(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), resource))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        authenticate(43L, "ROLE_QA_MANAGER");
        assertThatThrownBy(() -> controller.reviewBatch(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), resource))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        authenticate(42L, "ROLE_QA_MANAGER");
        assertThat(controller.reviewBatch(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), resource).getStatusCode().value()).isEqualTo(201);
        assertThat(queries.handle(new GetRawMaterialMovementsQuery(42L, 1L, receipt.getRawMaterialId())))
            .filteredOn(movement -> movement.type().equals("REVIEW")).hasSize(1);
    }

    @Test void simultaneousRetriesRecordOnlyOneConsumption() throws Exception {
        var receipt = receive("kg", "100");
        commands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), RawMaterialBatchStatus.RELEASED, "Reviewed"));
        var request = request(receipt, batch(42, BatchStatus.IN_PROGRESS), "50", UUID.randomUUID().toString());
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<InventoryContextFacade.Consumption> task = () -> {
                start.await();
                return inventory.consume(request);
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            start.countDown();
            assertThat(first.get(10, java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(second.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertThat(usages.findAllByBatchId(request.productBatchId())).hasSize(1);
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, 1L, receipt.getRawMaterialId()))
            .getFirst().getAvailableAmount()).isEqualByComparingTo("50");
    }

    @BeforeEach void actor() {
        when(actor.userId()).thenReturn(27L);
        when(laboratories.existsEnvironment(42L, 1L)).thenReturn(true);
    }
    private RawMaterialBatch receive(String unit, String amount) {
        var material = commands.handle(new SaveRawMaterialCommand(42L, 1L, null, "RM-" + UUID.randomUUID(), "Fixture", unit, BigDecimal.TEN)).toOptional().orElseThrow();
        return commands.handle(new ReceiveRawMaterialBatchCommand(42L, 1L, material.getId(), "Supplier", "SUP-1", unit,
            new BigDecimal(amount), today().minusDays(1), today().plusYears(1))).toOptional().orElseThrow();
    }
    private BigDecimal usableStock(Long materialId) {
        return inventory.findInventory(42L, 1L).stream().filter(material -> material.id().equals(materialId))
            .findFirst().map(InventoryContextFacade.InventoryMaterial::usableStock).orElse(BigDecimal.ZERO);
    }
    private long batch(long lab, BatchStatus status) {
        return batches.save(new Batch(null, lab, 1L, 1L, "Fixture", "B-" + UUID.randomUUID(), 10.0,
            "units", status, LocalDate.now().toString(), null, "Test")).getId();
    }
    private ConsumptionRequest request(RawMaterialBatch receipt, long batch, String amount, String operation) {
        return new ConsumptionRequest(42L, receipt.getId(), batch, new BigDecimal(amount), receipt.getUnit(), operation);
    }

    @Test void quarantineReviewConsumptionAndAffectedBatchHistoryPersist() {
        var receipt = receive("kg", "100");
        long batch = batch(42, BatchStatus.IN_PROGRESS);
        assertThat(usableStock(receipt.getRawMaterialId())).isZero();
        assertThatThrownBy(() -> inventory.consume(request(receipt, batch, "50", "blocked-" + UUID.randomUUID())))
            .satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("not available"));
        commands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), RawMaterialBatchStatus.RELEASED, "Certificate checked")).toOptional().orElseThrow();
        var consumed = inventory.consume(request(receipt, batch, "50", UUID.randomUUID().toString()));
        assertThat(consumed.stockAfter()).isEqualByComparingTo("50");
        assertThat(usages.findAllByBatchId(batch)).singleElement().satisfies(usage -> {
            assertThat(usage.getInventoryReceiptId()).isEqualTo(receipt.getId());
            assertThat(usage.getStockAfter()).isEqualByComparingTo("50");
        });
        commands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), RawMaterialBatchStatus.OBSERVED, "Supplier warning received")).toOptional().orElseThrow();
        assertThat(usableStock(receipt.getRawMaterialId())).isZero();
        assertThat(queries.handle(new GetRawMaterialMovementsQuery(42L, 1L, receipt.getRawMaterialId()))).anySatisfy(movement -> {
            assertThat(movement.productBatchId()).isEqualTo(batch);
            assertThat(movement.amount()).isEqualByComparingTo("-50");
        });
    }

    @Test void retryDoesNotDoubleConsumeAndConflictingKeyFails() {
        var receipt = receive("L", "100");
        commands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), RawMaterialBatchStatus.RELEASED, "Reviewed")).toOptional().orElseThrow();
        var request = request(receipt, batch(42, BatchStatus.IN_PROGRESS), "25", UUID.randomUUID().toString());
        assertThat(inventory.consume(request)).isEqualTo(inventory.consume(request));
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, 1L, receipt.getRawMaterialId())).getFirst().getAvailableAmount()).isEqualByComparingTo("75");
        assertThat(usages.findAllByBatchId(request.productBatchId())).hasSize(1);
        assertThatThrownBy(() -> inventory.consume(new ConsumptionRequest(42L, receipt.getId(), request.productBatchId(),
            BigDecimal.TEN, "L", request.operationId()))).satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("different values"));
    }

    @Test void failedBatchUsageRollsBackReceiptAndMovementTogether() {
        var receipt = receive("kg", "100");
        commands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), RawMaterialBatchStatus.RELEASED, "Reviewed")).toOptional().orElseThrow();
        long batch = batch(42, BatchStatus.IN_PROGRESS);
        doThrow(new IllegalStateException("Injected usage failure")).when(usages).save(any());
        assertThatThrownBy(() -> inventory.consume(request(receipt, batch, "50", UUID.randomUUID().toString())))
            .isInstanceOf(RuntimeException.class);
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, 1L, receipt.getRawMaterialId())).getFirst().getAvailableAmount()).isEqualByComparingTo("100");
        assertThat(queries.handle(new GetRawMaterialMovementsQuery(42L, 1L, receipt.getRawMaterialId()))).noneMatch(movement -> movement.type().equals("CONSUMPTION"));
    }

    @Test void duplicatesAndUnitChangesCannotCreateInconsistentStock() {
        var receipt = receive("kg", "100");
        assertThatThrownBy(() -> commands.handle(new ReceiveRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), "Supplier", "SUP-1", "kg",
            BigDecimal.TEN, today(), today().plusDays(4))).toOptional().orElseThrow()).satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("already exists"));
        assertThatThrownBy(() -> commands.handle(new ReceiveRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), "Supplier", "SUP-2", "L",
            BigDecimal.TEN, today(), today().plusDays(4))).toOptional().orElseThrow()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> commands.handle(new SaveRawMaterialCommand(42L, 1L, receipt.getRawMaterialId(), "C", "Name", "L", BigDecimal.ZERO)).toOptional().orElseThrow())
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, 1L, receipt.getRawMaterialId()))).hasSize(1);
    }

    @Test void expiredReceiptAndClosedOrForeignBatchCannotConsume() {
        var receipt = receive("kg", "100");
        commands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), RawMaterialBatchStatus.RELEASED, "Reviewed")).toOptional().orElseThrow();
        for (var status : new BatchStatus[]{BatchStatus.RELEASED, BatchStatus.REJECTED}) {
            assertThatThrownBy(() -> inventory.consume(request(receipt, batch(42, status), "1", UUID.randomUUID().toString())))
                .satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("Closed"));
        }
        assertThatThrownBy(() -> inventory.consume(request(receipt, batch(43, BatchStatus.IN_PROGRESS), "1", UUID.randomUUID().toString())))
            .hasMessageContaining("not found");
        var expired = commands.handle(new ReceiveRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), "Supplier", "EXPIRED", "kg", BigDecimal.TEN,
            today().minusDays(2), today())).toOptional().orElseThrow();
        assertThatThrownBy(() -> commands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, expired.getRawMaterialId(), expired.getId(), RawMaterialBatchStatus.RELEASED, "Review")).toOptional().orElseThrow())
            .satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("Expired"));
        assertThatThrownBy(() -> commands.handle(new ReviewRawMaterialBatchCommand(42L, 1L, receipt.getRawMaterialId(), receipt.getId(), RawMaterialBatchStatus.OBSERVED, " ")).toOptional().orElseThrow())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void importIsExplicitIdempotentAndDoesNotReleaseOrDeductHistoricalStock() {
        var old = legacy.save(new RawMaterial(null, 42L, "Legacy", "OLD-" + UUID.randomUUID(), "Supplier", "SUP-OLD",
            today().plusYears(1).toString(), new BigDecimal("50"), "kg", BigDecimal.TEN));
        Long material = imports.handle(new ImportLegacyRawMaterialCommand(42L, 1L, old.getId()));
        assertThat(imports.handle(new ImportLegacyRawMaterialCommand(42L, 1L, old.getId()))).isEqualTo(material);
        assertThat(legacy.findById(old.getId()).orElseThrow().getCurrentStock()).isEqualByComparingTo("50");
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, 1L, material))).singleElement().satisfies(receipt -> {
            assertThat(receipt.getStatus()).isEqualTo(RawMaterialBatchStatus.QUARANTINED);
            assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("50");
        });
        assertThat(queries.handle(new GetRawMaterialMovementsQuery(42L, 1L, material))).singleElement().satisfies(movement ->
            assertThat(movement.type()).isEqualTo("OPENING_BALANCE"));
        assertThat(usableStock(material)).isZero();
    }

    @Test void foreignTenantCannotReadReviewOrConsumeReceipt() {
        var receipt = receive("kg", "100");
        assertThatThrownBy(() -> queries.handle(new GetRawMaterialBatchesQuery(43L, 1L, receipt.getRawMaterialId()))).hasMessageContaining("not found");
        assertThatThrownBy(() -> commands.handle(new ReviewRawMaterialBatchCommand(43L, 1L, receipt.getRawMaterialId(), receipt.getId(), RawMaterialBatchStatus.RELEASED, "Review")).toOptional().orElseThrow()).hasMessageContaining("not found");
        assertThatThrownBy(() -> inventory.consume(new ConsumptionRequest(43L, receipt.getId(), 1L,
            BigDecimal.ONE, "kg", UUID.randomUUID().toString()))).hasMessageContaining("not found");
    }
}
