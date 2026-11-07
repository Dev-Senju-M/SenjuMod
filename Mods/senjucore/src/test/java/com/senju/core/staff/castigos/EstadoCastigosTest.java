package com.senju.core.staff.castigos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class EstadoCastigosTest {
    @Test void activoAntesYDespuesDeVencer() {
        EstadoCastigos e = new EstadoCastigos();
        e.aplicar(TipoCastigo.SILENCIO, 1000);
        assertTrue(e.activo(TipoCastigo.SILENCIO, 999));
        assertFalse(e.activo(TipoCastigo.SILENCIO, 1000));
        assertFalse(e.activo(TipoCastigo.SILENCIO, 2000));
        assertFalse(e.activo(TipoCastigo.BAN, 0));
    }
    @Test void reemplazaElAnterior() {
        EstadoCastigos e = new EstadoCastigos();
        e.aplicar(TipoCastigo.BAN, 1000);
        e.aplicar(TipoCastigo.BAN, 5000);
        assertEquals(5000, e.vence(TipoCastigo.BAN));
        assertEquals(1, e.todos().size());
    }
    @Test void venceDeInactivoEsCero() {
        assertEquals(0, new EstadoCastigos().vence(TipoCastigo.CARCEL));
    }
    @Test void vencidosDevuelveYQuita() {
        EstadoCastigos e = new EstadoCastigos();
        e.aplicar(TipoCastigo.SILENCIO, 1000);
        e.aplicar(TipoCastigo.BAN, 9000);
        assertEquals(List.of(TipoCastigo.SILENCIO), e.vencidos(1000));
        assertEquals(0, e.vence(TipoCastigo.SILENCIO));
        assertTrue(e.vencidos(1000).isEmpty());
        assertTrue(e.activo(TipoCastigo.BAN, 1000));
    }
    @Test void permanenteNuncaVence() {
        EstadoCastigos e = new EstadoCastigos();
        e.aplicar(TipoCastigo.BAN, Duracion.PERMANENTE);
        assertTrue(e.activo(TipoCastigo.BAN, Long.MAX_VALUE - 1));
        assertTrue(e.vencidos(Long.MAX_VALUE - 1).isEmpty());
    }
    @Test void quitar() {
        EstadoCastigos e = new EstadoCastigos();
        e.aplicar(TipoCastigo.CONGELAR, 1000);
        e.quitar(TipoCastigo.CONGELAR);
        assertFalse(e.activo(TipoCastigo.CONGELAR, 0));
        assertTrue(e.estaVacio());
    }
    @Test void tieneDuracion() {
        assertFalse(TipoCastigo.EXPULSION.tieneDuracion());
        assertFalse(TipoCastigo.QUITAR_HORAS.tieneDuracion());
        assertTrue(TipoCastigo.BAN.tieneDuracion());
        assertEquals(TipoCastigo.CARCEL, TipoCastigo.deClave("carcel").orElseThrow());
        assertTrue(TipoCastigo.deClave("zzz").isEmpty());
    }
}
