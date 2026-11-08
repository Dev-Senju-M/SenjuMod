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

    /** Posición de la estructura respecto al altar y lo que debe haber en ella. */
    public record Pos(int dx, int dy, int dz, EstadoGuia.Esperado esperado) {}

    private static final List<Pos> POSICIONES = construir();

    private static List<Pos> construir() {
        List<Pos> l = new ArrayList<>();
        for (int sx : new int[] {-2, 2}) {
            for (int sz : new int[] {-2, 2}) {
                l.add(new Pos(sx, 0, sz, EstadoGuia.Esperado.OBSIDIANA));
                l.add(new Pos(sx, 1, sz, EstadoGuia.Esperado.OBSIDIANA));
                l.add(new Pos(sx, 2, sz, EstadoGuia.Esperado.FUEGO_ALMAS));
            }
        }
        return List.copyOf(l);
    }

    /** Las 12 posiciones, tres por pilar (obsidiana, obsidiana, fuego de almas). */
    public static List<Pos> posiciones() {
        return POSICIONES;
    }

    /** Devuelve una descripción por cada pilar incompleto (vacía si la estructura es válida). */
    public static List<String> faltantes(Function<int[], String> bloqueEn) {
        List<String> faltan = new ArrayList<>();
        for (int i = 0; i < POSICIONES.size(); i += 3) {
            boolean ok = true;
            for (int k = 0; k < 3; k++) {
                Pos p = POSICIONES.get(i + k);
                String bueno = p.esperado() == EstadoGuia.Esperado.OBSIDIANA ? OBSIDIANA : FUEGO_ALMAS;
                if (!bueno.equals(bloqueEn.apply(new int[] {p.dx(), p.dy(), p.dz()}))) ok = false;
            }
            if (!ok) {
                Pos p = POSICIONES.get(i);
                faltan.add("pilar (" + p.dx() + ", " + p.dz() + ")");
            }
        }
        return faltan;
    }
}
