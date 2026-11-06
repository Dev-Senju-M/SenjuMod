package com.senju.core.cliente;

/** Lógica pura de máquina de escribir, parpadeo y color RGB del panel (sin dependencias de Minecraft). */
public final class Efectos {
    public static final int DESFASE_LETRA = 25;
    public static final int DESFASE_LINEA = 40;

    private static final long MS_POR_LETRA = 100;
    private static final long MS_PAUSA = 3000;
    private static final long MS_BORRAR = 50;
    private static final long MS_VACIO = 500;

    private Efectos() {}

    public static int letrasVisibles(long ms, int largo) {
        if (largo <= 0) return 0;
        long escritura = largo * MS_POR_LETRA;
        long finPausa = escritura + MS_PAUSA;
        long finBorrado = finPausa + largo * MS_BORRAR;
        long ciclo = finBorrado + MS_VACIO;
        long t = Math.floorMod(ms, ciclo);
        if (t < escritura) return (int) Math.min(largo, t / MS_POR_LETRA);
        if (t < finPausa) return largo;
        if (t < finBorrado) return (int) Math.max(0, largo - (t - finPausa) / MS_BORRAR);
        return 0;
    }

    public static boolean cursorVisible(long ms) {
        return (ms / 400) % 2 == 0;
    }

    public static boolean parpadeoVisible(long ms) {
        return (ms / 500) % 2 == 0;
    }

    public static int colorRgb(long ms, double velocidad, int desfaseGrados) {
        double tono = (ms * velocidad / 4000.0 * 360.0 + desfaseGrados) % 360.0;
        if (tono < 0) tono += 360.0;
        double s = 0.6;
        double v = 1.0;
        double c = v * s;
        double h6 = tono / 60.0;
        double x = c * (1 - Math.abs(h6 % 2 - 1));
        double m = v - c;
        double r, g, b;
        switch ((int) h6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        int ri = (int) Math.round((r + m) * 255);
        int gi = (int) Math.round((g + m) * 255);
        int bi = (int) Math.round((b + m) * 255);
        return (ri << 16) | (gi << 8) | bi;
    }
}
