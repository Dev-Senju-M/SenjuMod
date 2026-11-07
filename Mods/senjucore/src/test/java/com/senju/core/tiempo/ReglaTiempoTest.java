package com.senju.core.tiempo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ReglaTiempoTest {
    static final long LIMITE = 6 * 3600;

    @Test void nuevoDiaReinicia() {
        assertEquals(new EstadoTiempo(101, 0), ReglaTiempo.normalizar(new EstadoTiempo(100, 5000), 101));
    }
    @Test void mismoDiaConserva() {
        assertEquals(new EstadoTiempo(100, 5000), ReglaTiempo.normalizar(new EstadoTiempo(100, 5000), 100));
    }
    @Test void relojAtrasNoReinicia() {
        assertEquals(new EstadoTiempo(100, 5000), ReglaTiempo.normalizar(new EstadoTiempo(100, 5000), 99));
    }
    @Test void sumarAcumula() {
        assertEquals(11, ReglaTiempo.sumar(new EstadoTiempo(100, 10), 100, 1).usados());
    }
    @Test void sumarCruzandoMedianoche() {
        assertEquals(new EstadoTiempo(101, 1), ReglaTiempo.sumar(new EstadoTiempo(100, 21599), 101, 1));
    }
    @Test void restantesNuncaNegativos() {
        EstadoTiempo e = new EstadoTiempo(100, 30000);
        assertEquals(0, ReglaTiempo.restantes(e, LIMITE));
        assertTrue(ReglaTiempo.agotado(e, LIMITE));
    }
    @Test void limiteMenorQueUsado() {
        assertEquals(0, ReglaTiempo.restantes(new EstadoTiempo(100, 7200), ReglaTiempo.limiteSegundos(1.0)));
    }
    @Test void darNoBajaDeCero() {
        assertEquals(0, ReglaTiempo.dar(new EstadoTiempo(100, 300), 60).usados());
        assertEquals(5400, ReglaTiempo.dar(new EstadoTiempo(100, 7200), 30).usados());
    }
    @Test void avisoA10() {
        assertEquals(OptionalInt.of(10), ReglaTiempo.avisoCruzado(601, 600, List.of(10, 1)));
    }
    @Test void avisoA1() {
        assertEquals(OptionalInt.of(1), ReglaTiempo.avisoCruzado(61, 60, List.of(10, 1)));
    }
    @Test void sinAvisoFueraDeUmbral() {
        assertTrue(ReglaTiempo.avisoCruzado(700, 699, List.of(10, 1)).isEmpty());
    }
    @Test void sinAvisoSiSube() {
        assertTrue(ReglaTiempo.avisoCruzado(60, 21600, List.of(10, 1)).isEmpty());
    }
    @Test void limiteSeisHoras() {
        assertEquals(21600, ReglaTiempo.limiteSegundos(6.0));
    }
    @Test void quitarSuma() {
        assertEquals(1800 + 100, ReglaTiempo.quitar(new EstadoTiempo(100, 100), 30, LIMITE).usados());
    }
    @Test void quitarTopeAlLimite() {
        assertEquals(LIMITE, ReglaTiempo.quitar(new EstadoTiempo(100, LIMITE - 60), 30, LIMITE).usados());
    }
    @Test void quitarTodoElDia() {
        EstadoTiempo r = ReglaTiempo.quitar(new EstadoTiempo(100, 5), -1, LIMITE);
        assertEquals(LIMITE, r.usados());
        assertEquals(100, r.dia());
        assertTrue(ReglaTiempo.agotado(r, LIMITE));
    }
    @Test void quitarNoBajaUsadosSiYaExcedido() {
        assertEquals(LIMITE + 500, ReglaTiempo.quitar(new EstadoTiempo(100, LIMITE + 500), 30, LIMITE).usados());
    }
}
