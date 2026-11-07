package com.senju.core.staff.castigos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class DuracionTest {
    static final long MIN = 60_000L;

    @Test void parseaMinutosHorasDias() {
        assertEquals(OptionalLong.of(10 * MIN), Duracion.parsear("10m"));
        assertEquals(OptionalLong.of(3_600_000L), Duracion.parsear("1h"));
        assertEquals(OptionalLong.of(86_400_000L), Duracion.parsear("1d"));
        assertEquals(OptionalLong.of(7 * 86_400_000L), Duracion.parsear("7d"));
    }
    @Test void parseaPermanenteYMayusculas() {
        assertEquals(OptionalLong.of(Duracion.PERMANENTE), Duracion.parsear("perm"));
        assertEquals(OptionalLong.of(Duracion.PERMANENTE), Duracion.parsear(" PERM "));
        assertEquals(OptionalLong.of(3_600_000L), Duracion.parsear("1H"));
    }
    @Test void rechazaInvalidos() {
        assertTrue(Duracion.parsear("abc").isEmpty());
        assertTrue(Duracion.parsear("0m").isEmpty());
        assertTrue(Duracion.parsear("-1h").isEmpty());
        assertTrue(Duracion.parsear("").isEmpty());
        assertTrue(Duracion.parsear(null).isEmpty());
        assertTrue(Duracion.parsear("10").isEmpty());
        assertTrue(Duracion.parsear("5x").isEmpty());
    }
    @Test void desbordeEnParseoEsPermanente() {
        assertEquals(OptionalLong.of(Duracion.PERMANENTE), Duracion.parsear("999999999999999d"));
    }
    @Test void vencimientoSuma() {
        assertEquals(1000 + 600, Duracion.vencimiento(1000, 600));
    }
    @Test void vencimientoPermanente() {
        assertEquals(Duracion.PERMANENTE, Duracion.vencimiento(1000, Duracion.PERMANENTE));
    }
    @Test void vencimientoSatura() {
        assertEquals(Duracion.PERMANENTE, Duracion.vencimiento(Long.MAX_VALUE - 5, 10));
    }
    @Test void formatoMinutos() {
        assertEquals("9m", Duracion.formatoRestante(0, 9 * MIN));
    }
    @Test void formatoHorasMinutos() {
        assertEquals("1h 05m", Duracion.formatoRestante(0, 65 * MIN));
    }
    @Test void formatoDiasHoras() {
        assertEquals("2d 3h", Duracion.formatoRestante(1000, 1000 + (2 * 1440 + 3 * 60) * MIN));
    }
    @Test void formatoPermanente() {
        assertEquals("permanente", Duracion.formatoRestante(5, Duracion.PERMANENTE));
    }
    @Test void formatoYaVencidoOMenosDeUnMinuto() {
        assertEquals("0m", Duracion.formatoRestante(100, 50));
        assertEquals("0m", Duracion.formatoRestante(0, 30_000));
    }
    @Test void formatoDuracionSinCerosSobrantes() {
        assertEquals("1d", Duracion.formatoDuracion(86_400_000L));
        assertEquals("7d", Duracion.formatoDuracion(7 * 86_400_000L));
        assertEquals("1h", Duracion.formatoDuracion(3_600_000L));
        assertEquals("10m", Duracion.formatoDuracion(10 * MIN));
        assertEquals("2d 3h", Duracion.formatoDuracion((2 * 1440 + 180) * MIN));
        assertEquals("1h 05m", Duracion.formatoDuracion(65 * MIN));
        assertEquals("permanente", Duracion.formatoDuracion(Duracion.PERMANENTE));
    }
}
