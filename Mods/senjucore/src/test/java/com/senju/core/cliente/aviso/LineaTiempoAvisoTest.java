package com.senju.core.cliente.aviso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LineaTiempoAvisoTest {
    private static final float E = 1e-3f;

    @Test
    void inicio() {
        assertEquals(0f, LineaTiempoAviso.velo(0), E);
        assertEquals(0f, LineaTiempoAviso.emblema(0), E);
        assertEquals(0f, LineaTiempoAviso.aperturaOjo(0), E);
        assertEquals(0f, LineaTiempoAviso.alas(0), E);
        assertEquals(0f, LineaTiempoAviso.titulo(0), E);
        assertEquals(10f, LineaTiempoAviso.espaciadoTitulo(0), E);
        assertEquals(0f, LineaTiempoAviso.subtitulo(0), E);
        assertEquals(0f, LineaTiempoAviso.huevos(0), E);
        assertFalse(LineaTiempoAviso.terminado(0));
    }

    @Test
    void a400() {
        assertEquals(1f, LineaTiempoAviso.velo(400), E);
        assertEquals(0.25f, LineaTiempoAviso.emblema(400), E);
        assertEquals(0f, LineaTiempoAviso.aperturaOjo(400), E);
        assertEquals(0f, LineaTiempoAviso.alas(400), E);
        assertEquals(0f, LineaTiempoAviso.titulo(400), E);
        assertEquals(0f, LineaTiempoAviso.huevos(400), E);
    }

    @Test
    void a1300() {
        assertEquals(1f, LineaTiempoAviso.velo(1300), E);
        assertEquals(1f, LineaTiempoAviso.emblema(1300), E);
        assertEquals(1f, LineaTiempoAviso.aperturaOjo(1300), E);
        assertEquals(0.3f + 0.7f * 700f / 900f, LineaTiempoAviso.alas(1300), E);
        assertEquals(0.2f, LineaTiempoAviso.titulo(1300), E);
        assertEquals(8.75f, LineaTiempoAviso.espaciadoTitulo(1300), E);
        assertEquals(0f, LineaTiempoAviso.subtitulo(1300), E);
        assertEquals(0f, LineaTiempoAviso.huevos(1300), E);
    }

    @Test
    void aperturaOjoEaseOutCubico() {
        // mitad del tramo (900 ms): 1 - (1 - 0.5)^3 = 0.875
        assertEquals(0.875f, LineaTiempoAviso.aperturaOjo(900), E);
    }

    @Test
    void a2500TodoVisible() {
        assertEquals(1f, LineaTiempoAviso.velo(2500), E);
        assertEquals(1f, LineaTiempoAviso.emblema(2500), E);
        assertEquals(1f, LineaTiempoAviso.aperturaOjo(2500), E);
        assertEquals(1f, LineaTiempoAviso.alas(2500), E);
        assertEquals(1f, LineaTiempoAviso.titulo(2500), E);
        assertEquals(0f, LineaTiempoAviso.espaciadoTitulo(2500), E);
        assertEquals(1f, LineaTiempoAviso.subtitulo(2500), E);
        assertEquals(1f, LineaTiempoAviso.huevos(2500), E);
        assertFalse(LineaTiempoAviso.terminado(2500));
    }

    @Test
    void a5300FundidoMitad() {
        assertEquals(0.5f, LineaTiempoAviso.velo(5300), E);
        assertEquals(0.5f, LineaTiempoAviso.emblema(5300), E);
        assertEquals(0.5f, LineaTiempoAviso.aperturaOjo(5300), E);
        assertEquals(0.5f, LineaTiempoAviso.alas(5300), E);
        assertEquals(0.5f, LineaTiempoAviso.titulo(5300), E);
        assertEquals(0.5f, LineaTiempoAviso.subtitulo(5300), E);
        assertEquals(0.5f, LineaTiempoAviso.huevos(5300), E);
        assertFalse(LineaTiempoAviso.terminado(5300));
    }

    @Test
    void finalYTerminado() {
        assertEquals(0f, LineaTiempoAviso.velo(5600), E);
        assertEquals(0f, LineaTiempoAviso.emblema(5600), E);
        assertEquals(0f, LineaTiempoAviso.aperturaOjo(5600), E);
        assertEquals(0f, LineaTiempoAviso.alas(5600), E);
        assertEquals(0f, LineaTiempoAviso.titulo(5600), E);
        assertEquals(0f, LineaTiempoAviso.subtitulo(5600), E);
        assertEquals(0f, LineaTiempoAviso.huevos(5600), E);
        assertFalse(LineaTiempoAviso.terminado(5599));
        assertTrue(LineaTiempoAviso.terminado(5600));
        assertTrue(LineaTiempoAviso.terminado(99999));
    }

    @Test
    void siempreEnRango() {
        for (long t = -100; t <= 6000; t += 37) {
            for (float v : new float[] {LineaTiempoAviso.velo(t), LineaTiempoAviso.emblema(t),
                    LineaTiempoAviso.aperturaOjo(t), LineaTiempoAviso.alas(t), LineaTiempoAviso.titulo(t),
                    LineaTiempoAviso.subtitulo(t), LineaTiempoAviso.huevos(t)}) {
                assertTrue(v >= 0f && v <= 1f, "t=" + t + " v=" + v);
            }
        }
    }
}
