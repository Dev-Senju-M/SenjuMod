package com.senju.core.misiones;

/** Regla pura para atribuir un ítem recién aparecido a una marca de origen (zona + tick). */
public final class ReglaOrigen {
    private ReglaOrigen() {}

    /** El ítem apareció como mucho 1 tick después de la marca y a distancia² ≤ radio². */
    public static boolean coincide(long tickMarca, long tickAhora, double dist2, double radio) {
        return tickAhora - tickMarca <= 1 && dist2 <= radio * radio;
    }
}
