package com.iotech.qualitrack.platform.testing;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Calendar dates in the zone of the backend clocks (equipment and inventory), so the tests do not depend on the zone of the machine.
 */
public final class LimaDates {
    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    private LimaDates() { }

    /** Today in Lima, the date the backend rejects anything after. */
    public static LocalDate today() { return LocalDate.now(LIMA); }
}
