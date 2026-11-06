package com.senju.core.cliente;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PosicionTest {
    @Test
    void siguienteCiclico() {
        assertEquals(Posicion.ARRIBA_IZQ, Posicion.ABAJO_DER.siguiente());
        assertEquals(Posicion.ARRIBA_DER, Posicion.ABAJO_IZQ.siguiente());
    }

    @Test
    void origenes() {
        assertArrayEquals(new int[] {4, 120}, Posicion.MEDIO_IZQ.origen(400, 300, 120, 60, 4, 0, 0));
        assertArrayEquals(new int[] {276, 120}, Posicion.MEDIO_DER.origen(400, 300, 120, 60, 4, 0, 0));
        assertArrayEquals(new int[] {276, 236}, Posicion.ABAJO_DER.origen(400, 300, 120, 60, 4, 0, 0));
    }

    @Test
    void panelMasGrandeQuePantalla() {
        assertArrayEquals(new int[] {0, 0}, Posicion.ABAJO_DER.origen(100, 50, 120, 60, 4, 0, 0));
    }
}
