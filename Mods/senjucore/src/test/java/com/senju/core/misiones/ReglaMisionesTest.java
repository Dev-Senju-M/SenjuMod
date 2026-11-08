package com.senju.core.misiones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReglaMisionesTest {
    private static Map<String, Integer> m(Object... kv) {
        Map<String, Integer> r = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], (Integer) kv[i + 1]);
        return r;
    }

    @Test void listaCompleta() {
        assertTrue(ReglaMisiones.lista(m("a", 10, "b", 2), m("a", 10, "b", 2), m("a", 10, "b", 2)));
    }
    @Test void listaFaltaInventario() {
        assertFalse(ReglaMisiones.lista(m("a", 10, "b", 2), m("a", 10, "b", 2), m("a", 9, "b", 2)));
    }
    @Test void listaFaltaProgreso() {
        assertFalse(ReglaMisiones.lista(m("a", 10, "b", 2), m("a", 9, "b", 5), m("a", 10, "b", 2)));
    }
    @Test void listaReqVacio() {
        assertFalse(ReglaMisiones.lista(m(), m(), m()));
    }
    @Test void progresoYTotal() {
        assertEquals(11, ReglaMisiones.progreso(m("a", 10, "b", 2), m("a", 15, "b", 1)));
        assertEquals(12, ReglaMisiones.total(m("a", 10, "b", 2)));
    }
    @Test void descontarResta() {
        Map<String, Integer> p = m("a", 10, "b", 3);
        ReglaMisiones.descontar(p, m("a", 4, "c", 1));
        assertEquals(m("a", 6, "b", 3), p);
    }
    @Test void descontarNoBajaDeCero() {
        Map<String, Integer> p = m("a", 2);
        ReglaMisiones.descontar(p, m("a", 5));
        assertEquals(m("a", 0), p);
    }
    @Test void ajustar() {
        assertEquals(1, ReglaMisiones.ajustar(1, true, false, 9999));
        assertEquals(21, ReglaMisiones.ajustar(5, false, true, 9999));
        assertEquals(9999, ReglaMisiones.ajustar(9990, false, true, 9999));
        assertEquals(1, ReglaMisiones.ajustar(10, true, true, 64));
        assertEquals(6, ReglaMisiones.ajustar(5, false, false, 9999));
        assertEquals(4, ReglaMisiones.ajustar(5, true, false, 9999));
    }
}
