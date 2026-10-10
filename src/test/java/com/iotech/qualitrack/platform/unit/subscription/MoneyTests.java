package com.iotech.qualitrack.platform.unit.subscription;

import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTests {
    @Test
    void acceptsNonNegativeAmountsInIsoCurrencies() {
        assertThat(new Money(new BigDecimal("49.90"), "USD").amount()).isEqualByComparingTo("49.90");
        assertThat(Money.zero("PEN").amount()).isZero();
    }

    @Test
    void rejectsNegativeAmountsAndUnknownCurrencies() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-0.01"), "USD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "XYZ1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(null, "USD")).isInstanceOf(NullPointerException.class);
    }
}
