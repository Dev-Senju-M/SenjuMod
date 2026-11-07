package com.senju.core.staff.castigos;

import java.util.ArrayList;
import java.util.List;

/** Lógica pura del historial: tope de entradas (las más nuevas) y consulta de las últimas, la más reciente primero. */
public final class HistorialLimitado {
    private HistorialLimitado() {}

    /** Añade al final (más reciente) y descarta las más antiguas hasta quedar en {@code max}. */
    public static <T> void agregar(List<T> lista, T entrada, int max) {
        lista.add(entrada);
        int sobran = lista.size() - Math.max(max, 0);
        if (sobran > 0) lista.subList(0, sobran).clear();
    }

    /** Últimas {@code n} entradas, la más reciente primero (si n supera el tamaño, todas). */
    public static <T> List<T> ultimos(List<T> lista, int n) {
        List<T> r = new ArrayList<>();
        for (int i = lista.size() - 1; i >= 0 && r.size() < n; i--) r.add(lista.get(i));
        return r;
    }
}
