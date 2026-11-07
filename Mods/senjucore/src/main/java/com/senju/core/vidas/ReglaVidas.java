package com.senju.core.vidas;

/** Lógica pura de las vidas. */
public final class ReglaVidas {
    private ReglaVidas() {}

    /** Resta una vida (mínimo 0). */
    public static int restar(int vidas) {
        return Math.max(0, vidas - 1);
    }

    public static boolean eliminado(int vidas) {
        return vidas <= 0;
    }

    public static TipoAviso avisoTrasMuerte(int vidasDespues) {
        return eliminado(vidasDespues) ? TipoAviso.ELIMINADO : TipoAviso.MUERTE;
    }
}
