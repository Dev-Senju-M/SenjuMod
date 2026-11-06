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
        assertArrayEquals(new int[] {276, 180}, Posicion.ABAJO_DER.origen(400, 300, 120, 60, 4, 0, 0));
    }

    @Test
    void panelMasGrandeQuePantalla() {
        assertArrayEquals(new int[] {0, 0}, Posicion.ABAJO_DER.origen(100, 50, 120, 60, 4, 0, 0));
    }

    @Test
    void abajoSobreBarraRapida() {
        assertArrayEquals(new int[] {4, 120}, Posicion.ABAJO_IZQ.origen(427, 240, 128, 60, 4, 0, 0));
    }

    @Test
    void abajoSinSolape() {
        assertArrayEquals(new int[] {4, 436}, Posicion.ABAJO_IZQ.origen(1000, 500, 128, 60, 4, 0, 0));
    }

    @Test
    void medioDerPanelGrande() {
        assertArrayEquals(new int[] {0, 0}, Posicion.MEDIO_DER.origen(100, 50, 120, 60, 4, 0, 0));
    }
}
