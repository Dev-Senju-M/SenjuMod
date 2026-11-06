package com.senju.core.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EfectosTest {
    @Test
    void letrasVisibles() {
        assertEquals(0, Efectos.letrasVisibles(0, 12));
        assertEquals(1, Efectos.letrasVisibles(100, 12));
        assertEquals(12, Efectos.letrasVisibles(1200, 12));
        assertEquals(12, Efectos.letrasVisibles(4199, 12));
        assertEquals(11, Efectos.letrasVisibles(4250, 12));
        assertEquals(0, Efectos.letrasVisibles(4800, 12));
        assertEquals(0, Efectos.letrasVisibles(5299, 12));
        assertEquals(1, Efectos.letrasVisibles(5400, 12));
    }

    @Test
    void letrasVisiblesBordes() {
        assertEquals(0, Efectos.letrasVisibles(12345, 0));
        int n = Efectos.letrasVisibles(Long.MAX_VALUE, 12);
        assertTrue(n >= 0 && n <= 12);
    }

    @Test
    void cursorYParpadeo() {
        assertTrue(Efectos.cursorVisible(0));
        assertFalse(Efectos.cursorVisible(400));
        assertTrue(Efectos.parpadeoVisible(0));
        assertFalse(Efectos.parpadeoVisible(500));
        assertTrue(Efectos.parpadeoVisible(1000));
    }

    @Test
    void colorRgb() {
        assertEquals(0xFF6666, Efectos.colorRgb(0, 1.0, 0));
        assertEquals(0x6666FF, Efectos.colorRgb(0, 1.0, 240));
        assertEquals(0x66FFFF, Efectos.colorRgb(1000, 2.0, 0));
        assertEquals(Efectos.colorRgb(0, 1.0, 0), Efectos.colorRgb(4000, 1.0, 0));
    }
}
