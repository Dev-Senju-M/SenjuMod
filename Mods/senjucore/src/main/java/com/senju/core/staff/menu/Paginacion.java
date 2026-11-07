package com.senju.core.staff.menu;

/** Cálculo de páginas. Lógica pura. */
public final class Paginacion {
    private Paginacion() {}

    public static int totalPaginas(int total, int porPagina) {
        if (total <= 0 || porPagina <= 0) return 1;
        return (total + porPagina - 1) / porPagina;
    }

    public static int acotar(int pagina, int total, int porPagina) {
        return Math.max(0, Math.min(pagina, totalPaginas(total, porPagina) - 1));
    }
}
