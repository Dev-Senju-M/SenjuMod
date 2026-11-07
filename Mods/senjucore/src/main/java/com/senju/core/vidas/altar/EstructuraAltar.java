package com.senju.core.vidas.altar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Comprobación pura de los cuatro pilares del altar. Cada pilar está en (±2, ·, ±2) respecto al altar:
 * dos bloques de obsidiana (dy 0 y 1) y encima (dy 2) una fuente de fuego de almas.
 * Tipos lógicos: "obsidiana", "fuego_almas", "aire", "otro".
 */
public final class EstructuraAltar {
    private EstructuraAltar() {}

    public static final String OBSIDIANA = "obsidiana";
    public static final String FUEGO_ALMAS = "fuego_almas";

    /** Devuelve una descripción por cada pilar incompleto (vacía si la estructura es válida). */
    public static List<String> faltantes(Function<int[], String> bloqueEn) {
        List<String> faltan = new ArrayList<>();
        for (int sx : new int[] {-2, 2}) {
            for (int sz : new int[] {-2, 2}) {
                boolean ok = OBSIDIANA.equals(bloqueEn.apply(new int[] {sx, 0, sz}))
                        && OBSIDIANA.equals(bloqueEn.apply(new int[] {sx, 1, sz}))
                        && FUEGO_ALMAS.equals(bloqueEn.apply(new int[] {sx, 2, sz}));
                if (!ok) faltan.add("pilar (" + sx + ", " + sz + ")");
            }
        }
        return faltan;
    }
}
