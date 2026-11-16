package com.iotech.qualitrack.platform.batch.domain.model;

import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RejectBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.ReleaseBatchCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * The batch commands clean what they receive and reject invalid values before they reach the aggregate
 * (TS63, TS71, TS72).
 */
class BatchCommandTests {
    private static CreateBatchCommand create(String batchNumber, Double quantity, String unit, String startDate, String notes) {
        return new CreateBatchCommand(1L, 2L, 3L, batchNumber, quantity, unit, startDate, notes);
    }

    @Test void newBatchesAreTrimmedAndCountedInUnitsByDefault() {
        var tablets = create("  PB-001  ", 500.0, null, " 2026-10-02 ", "   ");
        assertThat(tablets.batchNumber()).isEqualTo("PB-001");
        assertThat(tablets.unit()).isEqualTo("units");
        assertThat(tablets.startDate()).isEqualTo("2026-10-02");
        assertThat(tablets.notes()).isNull();

        var syrup = create("PB-002", 120.5, " Litros ", "2026-10-03", "  Night shift  ");
        assertThat(syrup.unit()).isEqualTo("L");
        assertThat(syrup.notes()).isEqualTo("Night shift");

        var longest = create("B".repeat(50), 1.0, "units", "2026-10-03", "N".repeat(500));
        assertThat(longest.batchNumber()).hasSize(50);
        assertThat(longest.notes()).hasSize(500);
    }

    @Test void newBatchesNeedTheirPlaceANumberAPositiveQuantityAndAnIsoStartDate() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CreateBatchCommand(null, 2L, 3L, "PB-001", 1.0, "units", "2026-10-02", null));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CreateBatchCommand(1L, 0L, 3L, "PB-001", 1.0, "units", "2026-10-02", null));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CreateBatchCommand(1L, 2L, -3L, "PB-001", 1.0, "units", "2026-10-02", null));

        for (var number : new String[] {null, "  ", "B".repeat(51)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> create(number, 1.0, "units", "2026-10-02", null));
        }
        for (var quantity : new Double[] {null, 0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatIllegalArgumentException().isThrownBy(() -> create("PB-001", quantity, "units", "2026-10-02", null));
        }
        assertThatIllegalArgumentException().isThrownBy(() -> create("PB-001", 1.0, "boxes", "2026-10-02", null));
        for (var startDate : new String[] {null, " ", "10/02/2026", "2026-02-30"}) {
            assertThatIllegalArgumentException().isThrownBy(() -> create("PB-001", 1.0, "units", startDate, null));
        }
        assertThatIllegalArgumentException().isThrownBy(() -> create("PB-001", 1.0, "units", "2026-10-02", "N".repeat(501)));
    }

    @Test void releasesAndRejectionsNeedAnIsoDateAndTheirText() {
        var release = new ReleaseBatchCommand(1L, 2L, 3L, 4L, " 2026-10-10 ", "  All controls passed  ");
        assertThat(release.releaseDate()).isEqualTo("2026-10-10");
        assertThat(release.notes()).isEqualTo("All controls passed");
        var rejection = new RejectBatchCommand(1L, 2L, 3L, 4L, "2026-10-10", "  Out of specification  ");
        assertThat(rejection.rejectionDate()).isEqualTo("2026-10-10");
        assertThat(rejection.reason()).isEqualTo("Out of specification");

        assertThatIllegalArgumentException().isThrownBy(() -> new ReleaseBatchCommand(1L, 2L, 3L, 0L, "2026-10-10", "Approved"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ReleaseBatchCommand(1L, 2L, 3L, 4L, "10/10/2026", "Approved"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ReleaseBatchCommand(1L, 2L, 3L, 4L, "2026-10-10", " "));
        assertThatIllegalArgumentException().isThrownBy(() -> new ReleaseBatchCommand(1L, 2L, 3L, 4L, "2026-10-10", "N".repeat(501)));

        assertThatIllegalArgumentException().isThrownBy(() -> new RejectBatchCommand(1L, 2L, 3L, null, "2026-10-10", "Contamination"));
        assertThatIllegalArgumentException().isThrownBy(() -> new RejectBatchCommand(1L, 2L, 3L, 4L, "2026-13-01", "Contamination"));
        assertThatIllegalArgumentException().isThrownBy(() -> new RejectBatchCommand(1L, 2L, 3L, 4L, "2026-10-10", null));
        assertThatIllegalArgumentException().isThrownBy(() -> new RejectBatchCommand(1L, 2L, 3L, 4L, "2026-10-10", "R".repeat(501)));
    }
}
