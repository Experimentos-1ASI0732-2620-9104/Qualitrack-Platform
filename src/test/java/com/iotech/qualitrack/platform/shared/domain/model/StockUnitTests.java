package com.iotech.qualitrack.platform.shared.domain.model;

import com.iotech.qualitrack.platform.shared.domain.model.valueobjects.StockUnit;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.CreateRawMaterialCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class StockUnitTests {
    @Test
    void acceptsUnitAliasesButNeverImplicitlyConvertsMassToVolume() {
        assertThatCode(() -> StockUnit.requireSame("Kg", "kilograms")).doesNotThrowAnyException();
        assertThatCode(() -> StockUnit.requireSame("Liters", "L")).doesNotThrowAnyException();
        assertThatThrownBy(() -> StockUnit.requireSame("kg", "L")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StockUnit.requireSame("kg", "g")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrationDoesNotRoundStockOrAllowFractionalCountableUnits() {
        assertThatThrownBy(() -> create("0.0001", "kg")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> create("0.5", "units")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> create("-1", "kg")).isInstanceOf(IllegalArgumentException.class);
        assertThat(create("100.125", "Liters").quantityInStock()).isEqualByComparingTo("100.125");
        assertThat(create("0", "kg").quantityInStock()).isZero();
    }

    private CreateRawMaterialCommand create(String stock, String unit) {
        return new CreateRawMaterialCommand(1L, "Material", "RM-1", "Supplier", "SUP-1", "2028-01-01",
                new BigDecimal(stock), unit, BigDecimal.ZERO);
    }
}
