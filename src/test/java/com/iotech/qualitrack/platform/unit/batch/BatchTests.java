package com.iotech.qualitrack.platform.unit.batch;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RejectBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.ReleaseBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BatchTests {
    @Test
    void newBatchStartsPendingWithCommandData() {
        var batch = newBatch();

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.PENDING);
        assertThat(batch.getLabId()).isEqualTo(1L);
        assertThat(batch.getProductName()).isEqualTo("Ibuprofen 400mg");
        assertThat(batch.getBatchNumber()).isEqualTo("LOT-2026-A");
        assertThat(batch.getEndDate()).isNull();
    }

    @Test
    void createCommandRejectsInvalidDataAndNormalizesOptionalFields() {
        assertThatThrownBy(() -> new CreateBatchCommand(0L, 1L, 10L, "LOT", 5.0, "units", "2026-05-12", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateBatchCommand(1L, 1L, 10L, " ", 5.0, "units", "2026-05-12", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateBatchCommand(1L, 1L, 10L, "LOT", -1.0, "units", "2026-05-12", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateBatchCommand(1L, 1L, 10L, "LOT", Double.NaN, "units", "2026-05-12", null))
                .isInstanceOf(IllegalArgumentException.class);

        var command = new CreateBatchCommand(1L, 1L, 10L, "LOT", 5.0, null, "2026-05-12", "  ");
        assertThat(command.notes()).isNull();
        assertThat(command.unit()).isEqualTo("units");
    }

    @Test
    void onlyPendingBatchesCanBeStarted() {
        var batch = newBatch();
        batch.start();

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.IN_PROGRESS);
        assertThatThrownBy(batch::start).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void releaseClosesTheBatchAndCannotBeRepeatedOrRejected() {
        var batch = newBatch();
        batch.release(new ReleaseBatchCommand(1L, 1L, 10L, 1L, "2026-05-20", "All quality controls passed"));

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.RELEASED);
        assertThat(batch.getEndDate()).isEqualTo("2026-05-20");
        assertThat(batch.getNotes()).isEqualTo("All quality controls passed");
        assertThatThrownBy(() -> batch.release(new ReleaseBatchCommand(1L, 1L, 10L, 1L, "2026-05-21", "Again")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> batch.reject(new RejectBatchCommand(1L, 1L, 10L, 1L, "2026-05-21", "Late failure")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectionStoresReasonAndBlocksLaterRelease() {
        var batch = newBatch();
        batch.reject(new RejectBatchCommand(1L, 1L, 10L, 1L, "2026-05-20", "Failed final quality control"));

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.REJECTED);
        assertThat(batch.getNotes()).isEqualTo("Failed final quality control");
        assertThatThrownBy(() -> batch.release(new ReleaseBatchCommand(1L, 1L, 10L, 1L, "2026-05-21", "Override")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> batch.reject(new RejectBatchCommand(1L, 1L, 10L, 1L, "2026-05-21", "Again")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void statusCommandsRequireJustification() {
        assertThatThrownBy(() -> new ReleaseBatchCommand(1L, 1L, 10L, 1L, "2026-05-20", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RejectBatchCommand(1L, 1L, 10L, 1L, "2026-05-20", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RejectBatchCommand(1L, 1L, 10L, null, "2026-05-20", "Reason"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Batch newBatch() {
        var product = new PharmaceuticalProduct(10L, 1L, 1L, "IBU-400", "Ibuprofen 400mg", null, "Tablets", true);
        return new Batch(new CreateBatchCommand(1L, 1L, 10L, "LOT-2026-A", 5000.0, "units", "2026-05-12", "Standard run"),
                product);
    }
}
