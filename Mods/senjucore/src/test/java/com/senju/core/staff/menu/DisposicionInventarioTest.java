package com.senju.core.staff.menu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DisposicionInventarioTest {
    @Test void mapeo() {
        assertEquals(9, DisposicionInventario.indiceInventario(0));
        assertEquals(35, DisposicionInventario.indiceInventario(26));
        assertEquals(0, DisposicionInventario.indiceInventario(27));
        assertEquals(8, DisposicionInventario.indiceInventario(35));
        assertEquals(39, DisposicionInventario.indiceInventario(36));
        assertEquals(36, DisposicionInventario.indiceInventario(39));
        assertEquals(40, DisposicionInventario.indiceInventario(40));
        assertEquals(-1, DisposicionInventario.indiceInventario(41));
        assertEquals(-1, DisposicionInventario.indiceInventario(53));
        assertEquals(-1, DisposicionInventario.indiceInventario(-999));
    }
}
