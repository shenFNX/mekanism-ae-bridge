package io.github.shenfnx.mekanismae.util;

import java.util.Locale;

/** Shared compact FE formatting for GUI and Jade displays. */
public final class EnergyFormatter {
    private static final long KILO = 1_000L;
    private static final long MEGA = 1_000_000L;

    public static String format(long energy) {
        long value = Math.max(0L, energy);
        return value >= MEGA ? formatScaled(value, MEGA, "M") : formatScaled(value, KILO, "K");
    }

    private static String formatScaled(long value, long unit, String suffix) {
        if (value % unit == 0) {
            return (value / unit) + suffix;
        }
        return String.format(Locale.ROOT, "%.1f%s", value / (double) unit, suffix);
    }

    private EnergyFormatter() {
    }
}
