package com.senju.core.staff.castigos;

/** Geometría pura de congelar y cárcel (sin Minecraft). */
public final class ReglaPosicion {
    private ReglaPosicion() {}

    /** Radio de la cárcel (horizontal y vertical, misma dimensión). */
    public static final double RADIO_CARCEL = 8.0;
    /** Movimiento tolerado al congelado (bloques, horizontal y vertical). */
    public static final double TOLERANCIA = 0.1;

    /** true si (x,y,z) está a más de {@code max} del centro: horizontalmente (euclídea) o verticalmente. */
    public static boolean fuera(double x, double y, double z, double cx, double cy, double cz, double max) {
        double dx = x - cx, dz = z - cz;
        return dx * dx + dz * dz > max * max || Math.abs(y - cy) > max;
    }

    /** Dentro de la cárcel: misma dimensión y a no más de {@link #RADIO_CARCEL}. */
    public static boolean dentroCarcel(String dim, double x, double y, double z,
            String cDim, double cx, double cy, double cz) {
        return dim.equals(cDim) && !fuera(x, y, z, cx, cy, cz, RADIO_CARCEL);
    }

    /** El congelado se movió: otra dimensión o más de {@link #TOLERANCIA}. */
    public static boolean seMovio(String dim, double x, double y, double z,
            String fDim, double fx, double fy, double fz) {
        return !dim.equals(fDim) || fuera(x, y, z, fx, fy, fz, TOLERANCIA);
    }
}
