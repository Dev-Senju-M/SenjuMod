package com.senju.core.staff.prohibidos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Lista ordenada (por inserción) de IDs de ítem prohibidos. Lógica pura. */
public class ListaProhibidos {
    private final Set<String> ids = new LinkedHashSet<>();

    public boolean agregar(String id) {
        return ids.add(id);
    }

    public boolean quitar(String id) {
        return ids.remove(id);
    }

    public boolean contiene(String id) {
        return ids.contains(id);
    }

    public int tamano() {
        return ids.size();
    }

    public List<String> pagina(int pagina, int porPagina) {
        if (pagina < 0 || porPagina <= 0) return List.of();
        long desde = (long) pagina * porPagina;
        if (desde >= ids.size()) return List.of();
        List<String> todos = new ArrayList<>(ids);
        return new ArrayList<>(todos.subList((int) desde, (int) Math.min(ids.size(), desde + porPagina)));
    }

    public List<String> todos() {
        return new ArrayList<>(ids);
    }
}
