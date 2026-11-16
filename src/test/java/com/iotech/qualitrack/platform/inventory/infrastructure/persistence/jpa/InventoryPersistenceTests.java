package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa;

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
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.LegacyRawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import com.iotech.qualitrack.platform.inventory.interfaces.acl.InventoryContextFacade.ConsumptionRequest;
import com.iotech.qualitrack.platform.batch.application.commandservices.RawMaterialUsageCommandService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterRawMaterialUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.domain.repositories.*;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.RawMaterialRepository;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LegacyInventoryFacade;
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
    @Autowired RawMaterialUsageCommandService usageCommands;
    @Autowired RawMaterialRepository legacy;
    @Autowired LegacyInventoryFacade legacyMaterials;
    @MockitoSpyBean RawMaterialUsageRepository usages;
    @MockitoBean CurrentUser actor;
    @MockitoBean InventoryExternalLaboratoryService laboratories;
    private static final Long ENV = 7L;

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
        assertThatThrownBy(() -> controller.reviewBatch(42L, ENV, receipt.getRawMaterialId(), receipt.getId(), resource))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        authenticate(43L, "ROLE_QA_MANAGER");
        assertThatThrownBy(() -> controller.reviewBatch(42L, ENV, receipt.getRawMaterialId(), receipt.getId(), resource))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        authenticate(42L, "ROLE_QA_MANAGER");
        assertThat(controller.reviewBatch(42L, ENV, receipt.getRawMaterialId(), receipt.getId(), resource).getStatusCode().value()).isEqualTo(201);
        assertThat(queries.handle(new GetRawMaterialMovementsQuery(42L, ENV, receipt.getRawMaterialId())))
            .filteredOn(movement -> movement.type().equals("REVIEW")).hasSize(1);
    }

    @Test void simultaneousRetriesRecordOnlyOneConsumption() throws Exception {
        var receipt = receive("kg", "100");
        commands.handle(review(42L, receipt, RawMaterialBatchStatus.RELEASED, "Reviewed"));
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
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, ENV, receipt.getRawMaterialId()))
            .getFirst().getAvailableAmount()).isEqualByComparingTo("50");
    }

    @BeforeEach void actor() {
        when(actor.userId()).thenReturn(27L);
        when(laboratories.existsEnvironment(42L, ENV)).thenReturn(true);
    }
    private ReviewRawMaterialBatchCommand review(Long lab, RawMaterialBatch receipt, RawMaterialBatchStatus status, String reason) {
        return new ReviewRawMaterialBatchCommand(lab, ENV, receipt.getRawMaterialId(), receipt.getId(), status, reason);
    }
    private RawMaterialBatch receive(String unit, String amount) {
        var material = commands.handle(new SaveRawMaterialCommand(42L, ENV, null, "RM-" + UUID.randomUUID(), "Fixture", unit, BigDecimal.TEN)).toOptional().orElseThrow();
        return commands.handle(new ReceiveRawMaterialBatchCommand(42L, ENV, material.getId(), "Supplier", "SUP-1", unit,
            new BigDecimal(amount), today().minusDays(1), today().plusYears(1))).toOptional().orElseThrow();
    }
    private long batch(long lab, BatchStatus status) {
        return batches.save(new Batch(null, lab, null, 1L, "Fixture", "B-" + UUID.randomUUID(), 10.0,
            "units", status, LocalDate.now().toString(), null, "Test")).getId();
    }
    private long batchInEnvironment(long lab, BatchStatus status) {
        return batches.save(new Batch(null, lab, ENV, 1L, "Fixture", "B-" + UUID.randomUUID(), 10.0,
            "units", status, LocalDate.now().toString(), null, "Test")).getId();
    }
    private RegisterRawMaterialUsageCommand usage(Long lab, long batch, RawMaterialBatch receipt) {
        return new RegisterRawMaterialUsageCommand(lab, ENV, 1L, batch, receipt.getId(), BigDecimal.ONE, receipt.getUnit(),
            UUID.randomUUID().toString());
    }
    private java.util.List<RawMaterialBatch> usable(Long material) {
        return queries.handle(new GetRawMaterialBatchesQuery(42L, ENV, material)).stream().filter(lot -> lot.isUsableOn(today())).toList();
    }
    private ConsumptionRequest request(RawMaterialBatch receipt, long batch, String amount, String operation) {
        return new ConsumptionRequest(42L, receipt.getId(), batch, new BigDecimal(amount), receipt.getUnit(), operation);
    }

    @Test void quarantineReviewConsumptionAndAffectedBatchHistoryPersist() {
        var receipt = receive("kg", "100");
        long batch = batch(42, BatchStatus.IN_PROGRESS);
        assertThat(usable(receipt.getRawMaterialId())).isEmpty();
        assertThatThrownBy(() -> inventory.consume(request(receipt, batch, "50", "blocked-" + UUID.randomUUID())))
            .satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("not available"));
        commands.handle(review(42L, receipt, RawMaterialBatchStatus.RELEASED, "Certificate checked")).toOptional().orElseThrow();
        var consumed = inventory.consume(request(receipt, batch, "50", UUID.randomUUID().toString()));
        assertThat(consumed.stockAfter()).isEqualByComparingTo("50");
        assertThat(usages.findAllByBatchId(batch)).singleElement().satisfies(usage -> {
            assertThat(usage.getInventoryReceiptId()).isEqualTo(receipt.getId());
            assertThat(usage.getStockAfter()).isEqualByComparingTo("50");
        });
        commands.handle(review(42L, receipt, RawMaterialBatchStatus.OBSERVED, "Supplier warning received")).toOptional().orElseThrow();
        assertThat(usable(receipt.getRawMaterialId())).isEmpty();
        assertThat(queries.handle(new GetRawMaterialMovementsQuery(42L, ENV, receipt.getRawMaterialId()))).anySatisfy(movement -> {
            assertThat(movement.productBatchId()).isEqualTo(batch);
            assertThat(movement.amount()).isEqualByComparingTo("-50");
        });
    }

    @Test void retryDoesNotDoubleConsumeAndConflictingKeyFails() {
        var receipt = receive("L", "100");
        commands.handle(review(42L, receipt, RawMaterialBatchStatus.RELEASED, "Reviewed")).toOptional().orElseThrow();
        var request = request(receipt, batch(42, BatchStatus.IN_PROGRESS), "25", UUID.randomUUID().toString());
        assertThat(inventory.consume(request)).isEqualTo(inventory.consume(request));
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, ENV, receipt.getRawMaterialId())).getFirst().getAvailableAmount()).isEqualByComparingTo("75");
        assertThat(usages.findAllByBatchId(request.productBatchId())).hasSize(1);
        assertThatThrownBy(() -> inventory.consume(new ConsumptionRequest(42L, receipt.getId(), request.productBatchId(),
            BigDecimal.TEN, "L", request.operationId()))).satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("different values"));
    }

    @Test void failedBatchUsageRollsBackReceiptAndMovementTogether() {
        var receipt = receive("kg", "100");
        commands.handle(review(42L, receipt, RawMaterialBatchStatus.RELEASED, "Reviewed")).toOptional().orElseThrow();
        long batch = batch(42, BatchStatus.IN_PROGRESS);
        doThrow(new IllegalStateException("Injected usage failure")).when(usages).save(any());
        assertThatThrownBy(() -> inventory.consume(request(receipt, batch, "50", UUID.randomUUID().toString())))
            .isInstanceOf(RuntimeException.class);
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, ENV, receipt.getRawMaterialId())).getFirst().getAvailableAmount()).isEqualByComparingTo("100");
        assertThat(queries.handle(new GetRawMaterialMovementsQuery(42L, ENV, receipt.getRawMaterialId()))).noneMatch(movement -> movement.type().equals("CONSUMPTION"));
    }

    @Test void duplicatesAndUnitChangesCannotCreateInconsistentStock() {
        var receipt = receive("kg", "100");
        assertThatThrownBy(() -> commands.handle(new ReceiveRawMaterialBatchCommand(42L, ENV, receipt.getRawMaterialId(), "Supplier", "SUP-1", "kg",
            BigDecimal.TEN, today(), today().plusDays(4))).toOptional().orElseThrow()).satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("already exists"));
        assertThatThrownBy(() -> commands.handle(new ReceiveRawMaterialBatchCommand(42L, ENV, receipt.getRawMaterialId(), "Supplier", "SUP-2", "L",
            BigDecimal.TEN, today(), today().plusDays(4))).toOptional().orElseThrow()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> commands.handle(new SaveRawMaterialCommand(42L, ENV, receipt.getRawMaterialId(), "C", "Name", "L", BigDecimal.ZERO)).toOptional().orElseThrow())
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, ENV, receipt.getRawMaterialId()))).hasSize(1);
    }

    @Test void closedOrForeignBatchCannotConsumeAndTheStockIsKept() {
        var receipt = receive("kg", "100");
        commands.handle(review(42L, receipt, RawMaterialBatchStatus.RELEASED, "Reviewed")).toOptional().orElseThrow();
        for (var status : new BatchStatus[]{BatchStatus.RELEASED, BatchStatus.REJECTED}) {
            var closed = usageCommands.handle(usage(42L, batchInEnvironment(42, status), receipt));
            assertThat(closed.isFailure()).isTrue();
            assertThat(closed.mapError(error -> error.details()).toString()).contains("cannot consume");
        }
        var foreign = usageCommands.handle(usage(42L, batchInEnvironment(43, BatchStatus.IN_PROGRESS), receipt));
        assertThat(foreign.mapError(error -> error.code()).toString()).contains("NOT_FOUND");
        assertThat(usable(receipt.getRawMaterialId())).singleElement()
            .satisfies(lot -> assertThat(lot.getAvailableAmount()).isEqualByComparingTo("100"));
    }

    @Test void expiredReceiptCannotBeReleasedAndAReviewNeedsAReason() {
        var receipt = receive("kg", "100");
        commands.handle(review(42L, receipt, RawMaterialBatchStatus.RELEASED, "Reviewed")).toOptional().orElseThrow();
        var expired = commands.handle(new ReceiveRawMaterialBatchCommand(42L, ENV, receipt.getRawMaterialId(), "Supplier", "EXPIRED", "kg", BigDecimal.TEN,
            today().minusDays(2), today())).toOptional().orElseThrow();
        assertThatThrownBy(() -> commands.handle(review(42L, expired, RawMaterialBatchStatus.RELEASED, "Review")).toOptional().orElseThrow())
            .satisfies(error -> assertThat(((com.iotech.qualitrack.platform.shared.application.result.ApplicationException) error).error().details()).contains("Expired"));
        assertThatThrownBy(() -> commands.handle(review(42L, receipt, RawMaterialBatchStatus.OBSERVED, " ")).toOptional().orElseThrow())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void importIsExplicitIdempotentAndDoesNotReleaseOrDeductHistoricalStock() {
        var old = legacy.save(new RawMaterial(null, 42L, "Legacy", "OLD-" + UUID.randomUUID(), "Supplier", "SUP-OLD",
            today().plusYears(1).toString(), new BigDecimal("50"), "kg", BigDecimal.TEN));
        // The ACL service is mocked for the environments; the legacy material is still locked by Laboratory.
        when(laboratories.lockLegacyMaterial(42L, old.getId())).thenAnswer(call -> {
            var locked = legacyMaterials.lock(42L, old.getId());
            return new LegacyRawMaterial(locked.id(), locked.code(), locked.name(), locked.unit(), locked.minimumStock(),
                locked.supplier(), locked.batchNumber(), locked.expiresOn(), locked.balance());
        });
        Long material = imports.handle(new ImportLegacyRawMaterialCommand(42L, ENV, old.getId())).toOptional().orElseThrow();
        assertThat(imports.handle(new ImportLegacyRawMaterialCommand(42L, ENV, old.getId())).toOptional()).contains(material);
        assertThat(legacy.findById(old.getId()).orElseThrow().getCurrentStock()).isEqualByComparingTo("50");
        assertThat(queries.handle(new GetRawMaterialBatchesQuery(42L, ENV, material))).singleElement().satisfies(receipt -> {
            assertThat(receipt.getStatus()).isEqualTo(RawMaterialBatchStatus.QUARANTINED);
            assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("50");
        });
        assertThat(queries.handle(new GetRawMaterialMovementsQuery(42L, ENV, material))).singleElement().satisfies(movement ->
            assertThat(movement.type()).isEqualTo("OPENING_BALANCE"));
        assertThat(usable(material)).isEmpty();
    }

    @Test void foreignTenantCannotReadReviewOrConsumeReceipt() {
        var receipt = receive("kg", "100");
        assertThatThrownBy(() -> queries.handle(new GetRawMaterialBatchesQuery(43L, ENV, receipt.getRawMaterialId()))).hasMessageContaining("not found");
        assertThatThrownBy(() -> commands.handle(review(43L, receipt, RawMaterialBatchStatus.RELEASED, "Review")).toOptional().orElseThrow()).hasMessageContaining("not found");
        assertThatThrownBy(() -> inventory.consume(new ConsumptionRequest(43L, receipt.getId(), 1L,
            BigDecimal.ONE, "kg", UUID.randomUUID().toString()))).hasMessageContaining("not found");
    }
}
