package com.senju.core.staff.menu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PaginacionTest {
    @Test void totalPaginas() {
        assertEquals(1, Paginacion.totalPaginas(0, 45));
        assertEquals(1, Paginacion.totalPaginas(45, 45));
        assertEquals(2, Paginacion.totalPaginas(46, 45));
    }
    @Test void acotar() {
        assertEquals(1, Paginacion.acotar(5, 46, 45));
        assertEquals(0, Paginacion.acotar(-1, 10, 45));
    }
}
