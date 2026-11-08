package com.senju.core.misiones;

/** Tipo de misión: la global del día (cola) o personal (asignada a jugadores concretos). */
public enum TipoMision {
    GLOBAL, PERSONAL;

    /** Tipo por nombre; desconocido → PERSONAL (no castiga). */
    public static TipoMision deNombre(String s) {
        for (TipoMision t : values()) if (t.name().equals(s)) return t;
        return PERSONAL;
    }
}
