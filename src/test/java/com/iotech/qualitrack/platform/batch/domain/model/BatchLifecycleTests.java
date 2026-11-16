package com.iotech.qualitrack.platform.batch.domain.model;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RejectBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.ReleaseBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.ContainerAssignment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * A batch starts pending (US73), is in progress from its first raw material consumption (US75) until it is released or
 * rejected once (US81, US82), and can be stored in a monitored container (US78).
 */
class BatchLifecycleTests {
    private Batch batch(BatchStatus status) {
        return new Batch(1L, 1L, 2L, 3L, "Paracetamol 500 mg", "PB-LIFE", 1000.0, "units", status, "2026-10-01", null, null);
    }

    @Test void theFirstRawMaterialConsumptionStartsAPendingBatch() {
        var batch = batch(BatchStatus.PENDING);
        assertThat(batch.registerRawMaterialConsumption()).isTrue();
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.IN_PROGRESS);
        assertThat(batch.registerRawMaterialConsumption()).isFalse();
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.IN_PROGRESS);
        assertThat(batch.isOpen()).isTrue();
    }

    @Test void closedBatchesAreNotStartedAgain() {
        for (var status : List.of(BatchStatus.RELEASED, BatchStatus.REJECTED)) {
            var batch = batch(status);
            assertThat(batch.registerRawMaterialConsumption()).isFalse();
            assertThat(batch.getStatus()).isEqualTo(status);
        }
    }

    @Test void newBatchesStartPendingWithTheProductOfTheirEnvironment() {
        var command = new CreateBatchCommand(1L, 2L, 3L, "PB-NEW", 1000.0, "units", "2026-10-01", null);
        var batch = new Batch(command, product(3L, 2L));
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.PENDING);
        assertThat(batch.getProductName()).isEqualTo("Paracetamol 500 mg");
        assertThat(batch.belongsTo(1L, 2L, 3L)).isTrue();
        assertThat(batch.belongsTo(1L, 9L, 3L)).isFalse();

        assertThatIllegalArgumentException().isThrownBy(() -> new Batch(command, product(3L, 9L)));
        assertThatIllegalArgumentException().isThrownBy(() -> new Batch(command, product(4L, 2L)));
    }

    @Test void releasingClosesTheBatchWithItsDateAndNotes() {
        var batch = batch(BatchStatus.IN_PROGRESS);
        batch.release(new ReleaseBatchCommand(1L, 2L, 3L, 1L, "2026-10-10", "All controls passed"));
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.RELEASED);
        assertThat(batch.getEndDate()).isEqualTo("2026-10-10");
        assertThat(batch.getNotes()).isEqualTo("All controls passed");
        assertThat(batch.isOpen()).isFalse();

        assertThatIllegalStateException().isThrownBy(() ->
                batch.release(new ReleaseBatchCommand(1L, 2L, 3L, 1L, "2026-10-11", "Released again")));
        assertThatIllegalStateException().isThrownBy(() ->
                batch.reject(new RejectBatchCommand(1L, 2L, 3L, 1L, "2026-10-11", "Rejected after the release")));
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.RELEASED);
    }

    @Test void rejectingClosesTheBatchWithItsDateAndReason() {
        var batch = batch(BatchStatus.PENDING);
        batch.reject(new RejectBatchCommand(1L, 2L, 3L, 1L, "2026-10-08", "Out of specification"));
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.REJECTED);
        assertThat(batch.getEndDate()).isEqualTo("2026-10-08");
        assertThat(batch.getNotes()).isEqualTo("Out of specification");
        assertThat(batch.isOpen()).isFalse();

        assertThatIllegalStateException().isThrownBy(() ->
                batch.reject(new RejectBatchCommand(1L, 2L, 3L, 1L, "2026-10-09", "Rejected again")));
        assertThatIllegalStateException().isThrownBy(() ->
                batch.release(new ReleaseBatchCommand(1L, 2L, 3L, 1L, "2026-10-09", "Released after the rejection")));
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.REJECTED);
    }

    @Test void storingTheBatchInItsSameContainerAgainChangesNothing() {
        var batch = batch(BatchStatus.RELEASED);
        assertThat(batch.container()).isEmpty();

        assertThat(batch.storeIn(new ContainerAssignment(12L, 4L, 7L, Instant.parse("2026-10-11T10:00:00Z")))).isTrue();
        assertThat(batch.storeIn(new ContainerAssignment(12L, 4L, 8L, Instant.parse("2026-10-12T10:00:00Z")))).isFalse();
        assertThat(batch.container()).hasValueSatisfying(container -> assertThat(container.assignedBy()).isEqualTo(7L));

        assertThat(batch.storeIn(new ContainerAssignment(13L, 4L, 8L, Instant.parse("2026-10-12T10:00:00Z")))).isTrue();
        assertThat(batch.container()).hasValueSatisfying(container -> assertThat(container.containerMonitorId()).isEqualTo(13L));
        assertThatIllegalArgumentException().isThrownBy(() -> new ContainerAssignment(null, 4L, 7L, Instant.now()));
    }

    private static PharmaceuticalProduct product(Long id, Long environmentId) {
        return new PharmaceuticalProduct(id, 1L, environmentId, "PRD-PAR", "Paracetamol 500 mg", null, "Tablet, blister pack", true);
    }
}
