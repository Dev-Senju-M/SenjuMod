package com.senju.core.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class FormatoTest {
    @Test
    void duracion() {
        assertEquals("5h 32m", Formato.duracion(19920));
        assertEquals("0h 00m", Formato.duracion(0));
        assertEquals("6h 00m", Formato.duracion(21600));
        assertEquals("0h 00m", Formato.duracion(59));
    }

    @Test
    void hora12() {
        assertEquals("9:45 PM", Formato.hora12(LocalTime.of(21, 45)));
        assertEquals("12:05 AM", Formato.hora12(LocalTime.of(0, 5)));
        assertEquals("12:00 PM", Formato.hora12(LocalTime.of(12, 0)));
    }

    @Test
    void colorTiempo() {
        assertEquals(0xFF5555, Formato.colorTiempo(599));
        assertEquals(0xFFFF55, Formato.colorTiempo(1799));
        assertEquals(0xFFFFFF, Formato.colorTiempo(1800));
    }
}
