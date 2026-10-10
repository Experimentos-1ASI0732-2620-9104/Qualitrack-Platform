package com.iotech.qualitrack.platform.unit.laboratory;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Laboratory;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateProductCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.CreateLaboratoryCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterStaffCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.events.StaffRegisteredEvent;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.LaboratoryName;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.LaboratoryStatus;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.StaffAccessRole;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.StockQuantity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class LaboratoryTests {
    @Test
    void newLaboratoryIsActiveWithItsRegulations() {
        var laboratory = newLaboratory();

        assertThat(laboratory.getStatus()).isEqualTo(LaboratoryStatus.ACTIVE);
        assertThat(laboratory.getName().name()).isEqualTo("Lab Andino");
        assertThat(laboratory.getAddress().getAddress()).isEqualTo("Av. Javier Prado 123");
        assertThat(laboratory.getApplicableRegulations()).containsExactly("BPM", "DIGEMID");
    }

    @Test
    void createCommandRequiresRucAndAtLeastOneRegulation() {
        assertThatThrownBy(() -> new CreateLaboratoryCommand("Lab", " ", "Address", "999", List.of("BPM")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateLaboratoryCommand("Lab", "20123456789", "Address", "999", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LaboratoryName("L".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void suspensionCannotBeAppliedTwice() {
        var laboratory = newLaboratory();
        laboratory.suspendOperations();

        assertThat(laboratory.getStatus()).isEqualTo(LaboratoryStatus.SUSPENDED);
        assertThatThrownBy(laboratory::suspendOperations).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void registeredStaffIsActiveAndPublishesAnEvent() {
        var staff = new StaffMember(new RegisterStaffCommand(1L, "Ana Torres", "QA_ANALYST", "ana@lab.pe", StaffAccessRole.OPERATOR));

        assertThat(staff.isActive()).isTrue();
        assertThat(staff.domainEvents()).singleElement().isInstanceOf(StaffRegisteredEvent.class);

        staff.deactivate();
        assertThat(staff.isActive()).isFalse();
        assertThatThrownBy(staff::deactivate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productIsCreatedActiveInItsEnvironment() {
        assertThatThrownBy(() -> new CreateProductCommand(1L, 1L, "P-1", "Ibuprofen", "Tablets", " "))
                .isInstanceOf(IllegalArgumentException.class);

        var product = new PharmaceuticalProduct(new CreateProductCommand(1L, 1L, "P-1", "Ibuprofen", "Tablets", "400mg"));
        assertThat(product.isActive()).isTrue();
        assertThat(product.belongsTo(1L, 1L)).isTrue();
        assertThat(product.belongsTo(1L, 2L)).isFalse();
    }

    @Test
    void stockQuantityIsImmutableAndNeverNegative() {
        var stock = new StockQuantity(10);

        assertThat(stock.add(5).value()).isEqualTo(15);
        assertThat(stock.subtract(10).value()).isZero();
        assertThat(stock.value()).isEqualTo(10);
        assertThatThrownBy(() -> stock.subtract(11)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.add(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StockQuantity(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    private Laboratory newLaboratory() {
        return new Laboratory(new CreateLaboratoryCommand("Lab Andino", "20123456789",
                "Av. Javier Prado 123", "999123456", List.of("BPM", "DIGEMID")));
    }
}
