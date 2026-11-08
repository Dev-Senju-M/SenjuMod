package com.senju.core.misiones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ColaGlobalTest {
    private static ColaGlobal cola123() {
        ColaGlobal c = new ColaGlobal(List.of());
        c.agregar(1); c.agregar(2); c.agregar(3);
        return c;
    }

    @Test void sacarSiguiente() {
        ColaGlobal c = cola123();
        assertEquals(OptionalInt.of(1), c.sacarSiguiente());
        assertEquals(List.of(2, 3), c.ids());
    }
    @Test void subir() {
        ColaGlobal c = cola123();
        c.sacarSiguiente();
        assertTrue(c.subir(3));
        assertEquals(List.of(3, 2), c.ids());
        assertFalse(c.subir(3));
    }
    @Test void quitarInexistente() {
        assertFalse(cola123().quitar(9));
    }
    @Test void quitar() {
        ColaGlobal c = cola123();
        assertTrue(c.quitar(2));
        assertEquals(List.of(1, 3), c.ids());
    }
    @Test void vacia() {
        assertEquals(OptionalInt.empty(), new ColaGlobal(List.of()).sacarSiguiente());
    }
    @Test void idsInmodificable() {
        assertThrows(UnsupportedOperationException.class, () -> cola123().ids().add(5));
    }
    @Test void copiaDefensiva() {
        List<Integer> orig = new java.util.ArrayList<>(List.of(1));
        ColaGlobal c = new ColaGlobal(orig);
        orig.add(2);
        assertEquals(List.of(1), c.ids());
    }
}
