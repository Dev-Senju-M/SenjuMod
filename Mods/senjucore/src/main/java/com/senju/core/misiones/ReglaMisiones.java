package com.senju.core.misiones;

import java.util.Map;

/** Lógica pura de las misiones (objetivos, progreso y entrega). */
public final class ReglaMisiones {
    public static final int MAX_CANTIDAD = 9999;

    private ReglaMisiones() {}

    /** Lista para entregar: hay objetivos y, para cada uno, progreso e inventario alcanzan lo pedido. */
    public static boolean lista(Map<String, Integer> req, Map<String, Integer> prog, Map<String, Integer> inv) {
        if (req.isEmpty()) return false;
        for (Map.Entry<String, Integer> e : req.entrySet()) {
            int pedido = e.getValue();
            if (prog.getOrDefault(e.getKey(), 0) < pedido) return false;
            if (inv.getOrDefault(e.getKey(), 0) < pedido) return false;
        }
        return true;
    }

    /** Suma de min(progreso, requerido) por objetivo. */
    public static int progreso(Map<String, Integer> req, Map<String, Integer> prog) {
        int suma = 0;
        for (Map.Entry<String, Integer> e : req.entrySet()) {
            suma += Math.min(prog.getOrDefault(e.getKey(), 0), e.getValue());
        }
        return suma;
    }

    public static int total(Map<String, Integer> req) {
        int suma = 0;
        for (int v : req.values()) suma += v;
        return suma;
    }

    /** Resta lo entregado del progreso (mínimo 0). Muta {@code prog}. */
    public static void descontar(Map<String, Integer> prog, Map<String, Integer> entregado) {
        for (Map.Entry<String, Integer> e : entregado.entrySet()) {
            prog.computeIfPresent(e.getKey(), (k, v) -> Math.max(0, v - e.getValue()));
        }
    }

    /** Clic izquierdo suma, derecho resta; con shift el paso es 16. Límites 1..max. */
    public static int ajustar(int actual, boolean derecho, boolean shift, int max) {
        int paso = shift ? 16 : 1;
        long nuevo = (long) actual + (derecho ? -paso : paso);
        return (int) Math.max(1, Math.min(max, nuevo));
    }
}
