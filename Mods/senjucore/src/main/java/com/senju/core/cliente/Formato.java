package com.senju.core.cliente;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formato puro de hora y tiempo del panel (sin dependencias de Minecraft). */
public final class Formato {
    private static final DateTimeFormatter HORA12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private Formato() {}

    public static String duracion(long segundos) {
        long min = Math.max(0, segundos) / 60;
        return String.format(Locale.ROOT, "%dh %02dm", min / 60, min % 60);
    }

    public static String hora12(LocalTime t) {
        return HORA12.format(t);
    }

    public static int colorTiempo(long restantes) {
        if (restantes < 600) return 0xFF5555;
        if (restantes < 1800) return 0xFFFF55;
        return 0xFFFFFF;
    }
}
