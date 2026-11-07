package com.senju.core.staff.castigos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Castigos activos de un jugador: tipo -> vencimiento (ms de época; PERMANENTE = nunca). */
public final class EstadoCastigos {
    private final Map<TipoCastigo, Long> vencimientos = new EnumMap<>(TipoCastigo.class);

    /** Aplica o reemplaza el castigo del mismo tipo. */
    public void aplicar(TipoCastigo t, long vence) {
        vencimientos.put(t, vence);
    }

    public boolean activo(TipoCastigo t, long ahora) {
        Long v = vencimientos.get(t);
        return v != null && (v == Duracion.PERMANENTE || v > ahora);
    }

    /** Vencimiento almacenado, o 0 si no hay. */
    public long vence(TipoCastigo t) {
        return vencimientos.getOrDefault(t, 0L);
    }

    /** Devuelve los castigos vencidos y los quita. */
    public List<TipoCastigo> vencidos(long ahora) {
        List<TipoCastigo> r = new ArrayList<>();
        for (Map.Entry<TipoCastigo, Long> e : vencimientos.entrySet()) {
            if (e.getValue() != Duracion.PERMANENTE && e.getValue() <= ahora) r.add(e.getKey());
        }
        r.forEach(vencimientos::remove);
        return r;
    }

    public void quitar(TipoCastigo t) {
        vencimientos.remove(t);
    }

    public boolean estaVacio() {
        return vencimientos.isEmpty();
    }

    /** Copia de solo lectura (para guardar en NBT). */
    public Map<TipoCastigo, Long> todos() {
        return new EnumMap<>(vencimientos);
    }
}
