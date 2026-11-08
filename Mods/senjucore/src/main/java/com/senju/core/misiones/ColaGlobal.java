package com.senju.core.misiones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

/** Cola ordenada de ids de misiones candidatas a ser la misión global. */
public final class ColaGlobal {
    private final List<Integer> ids;

    public ColaGlobal(List<Integer> ids) {
        this.ids = new ArrayList<>(ids);
    }

    public void agregar(int id) {
        ids.add(id);
    }

    public OptionalInt sacarSiguiente() {
        if (ids.isEmpty()) return OptionalInt.empty();
        return OptionalInt.of(ids.remove(0));
    }

    /** Sube una posición; false si no está o ya es la cabeza. */
    public boolean subir(int id) {
        int i = ids.indexOf(id);
        if (i <= 0) return false;
        Collections.swap(ids, i, i - 1);
        return true;
    }

    public boolean quitar(int id) {
        return ids.remove((Integer) id);
    }

    public List<Integer> ids() {
        return Collections.unmodifiableList(ids);
    }
}
