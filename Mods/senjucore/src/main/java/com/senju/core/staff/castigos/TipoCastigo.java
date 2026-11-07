package com.senju.core.staff.castigos;

import java.util.Locale;
import java.util.Optional;

/** Castigos disponibles. */
public enum TipoCastigo {
    SILENCIO(true), EXPULSION(false), BAN(true), CONGELAR(true), CARCEL(true), QUITAR_HORAS(false);

    private final boolean conDuracion;

    TipoCastigo(boolean conDuracion) {
        this.conDuracion = conDuracion;
    }

    /** true si queda como castigo activo con vencimiento (falso: acción puntual). */
    public boolean tieneDuracion() {
        return conDuracion;
    }

    /** Clave en minúsculas (comandos, lang, NBT). */
    public String clave() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TipoCastigo> deClave(String s) {
        if (s == null) return Optional.empty();
        for (TipoCastigo t : values()) if (t.clave().equals(s.trim().toLowerCase(Locale.ROOT))) return Optional.of(t);
        return Optional.empty();
    }
}
