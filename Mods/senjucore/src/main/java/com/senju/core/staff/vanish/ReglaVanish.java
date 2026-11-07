package com.senju.core.staff.vanish;

/** Regla pura de visibilidad del Vanish. */
public final class ReglaVanish {
    private ReglaVanish() {}

    public static boolean puedeVer(boolean objetivoEnVanish, boolean espectadorEsStaff, boolean mismoJugador) {
        return mismoJugador || !objetivoEnVanish || espectadorEsStaff;
    }
}
