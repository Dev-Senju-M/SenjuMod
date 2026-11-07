package com.senju.core.staff.castigos;

import java.util.Locale;
import java.util.OptionalLong;

/** Utilidades puras de duraciones (milisegundos) y vencimientos (milisegundos de época). */
public final class Duracion {
    public static final long PERMANENTE = Long.MAX_VALUE;
    private static final long MIN = 60_000L, HORA = 60 * MIN, DIA = 24 * HORA;

    private Duracion() {}

    /** Vencimiento absoluto; PERMANENTE si la duración lo es o si la suma desborda. */
    public static long vencimiento(long ahora, long duracionMs) {
        if (duracionMs == PERMANENTE || duracionMs > PERMANENTE - ahora) return PERMANENTE;
        return ahora + duracionMs;
    }

    /** "10m", "1h", "7d" (cualquier n positivo) o "perm". Vacío si no es válido. */
    public static OptionalLong parsear(String s) {
        if (s == null) return OptionalLong.empty();
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.equals("perm")) return OptionalLong.of(PERMANENTE);
        if (t.length() < 2) return OptionalLong.empty();
        long unidad;
        switch (t.charAt(t.length() - 1)) {
            case 'm' -> unidad = MIN;
            case 'h' -> unidad = HORA;
            case 'd' -> unidad = DIA;
            default -> { return OptionalLong.empty(); }
        }
        String num = t.substring(0, t.length() - 1);
        for (int i = 0; i < num.length(); i++) if (num.charAt(i) < '0' || num.charAt(i) > '9') return OptionalLong.empty();
        long n;
        try {
            n = Long.parseLong(num);
        } catch (NumberFormatException e) {
            return OptionalLong.of(PERMANENTE); // solo dígitos y demasiado grande
        }
        if (n <= 0) return OptionalLong.empty();
        if (n > PERMANENTE / unidad) return OptionalLong.of(PERMANENTE);
        return OptionalLong.of(n * unidad);
    }

    /** "permanente" / "2d 3h" / "1h 05m" / "9m" (0m si ya venció o falta menos de 1 min). */
    public static String formatoRestante(long ahora, long vence) {
        if (vence == PERMANENTE) return "permanente";
        long ms = vence - ahora;
        if (ms < MIN) return "0m";
        long dias = ms / DIA, horas = (ms % DIA) / HORA, min = (ms % HORA) / MIN;
        if (dias > 0) return dias + "d " + horas + "h";
        if (horas > 0) return horas + "h " + String.format("%02d", min) + "m";
        return min + "m";
    }

    /** Duración (no vencimiento) como texto: igual que formatoRestante(0, ms). */
    public static String formatoDuracion(long duracionMs) {
        return formatoRestante(0, duracionMs);
    }
}
