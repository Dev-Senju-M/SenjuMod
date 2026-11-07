package com.senju.core.staff.prohibidos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ListaProhibidosTest {
    @Test void agregarDosVeces() {
        ListaProhibidos l = new ListaProhibidos();
        assertTrue(l.agregar("minecraft:diamond"));
        assertFalse(l.agregar("minecraft:diamond"));
        assertTrue(l.contiene("minecraft:diamond"));
        assertEquals(1, l.tamano());
    }
    @Test void quitar() {
        ListaProhibidos l = new ListaProhibidos();
        l.agregar("a:b");
        assertTrue(l.quitar("a:b"));
        assertFalse(l.quitar("a:b"));
        assertFalse(l.contiene("a:b"));
    }
    @Test void paginas() {
        ListaProhibidos l = new ListaProhibidos();
        for (int i = 0; i < 46; i++) l.agregar("m:i" + i);
        assertEquals(45, l.pagina(0, 45).size());
        assertEquals(List.of("m:i45"), l.pagina(1, 45));
        assertTrue(l.pagina(2, 45).isEmpty());
        assertTrue(l.pagina(-1, 45).isEmpty());
    }
    @Test void ordenDeInsercion() {
        ListaProhibidos l = new ListaProhibidos();
        l.agregar("z:z"); l.agregar("a:a"); l.agregar("m:m");
        assertEquals(List.of("z:z", "a:a", "m:m"), l.pagina(0, 10));
    }
}
