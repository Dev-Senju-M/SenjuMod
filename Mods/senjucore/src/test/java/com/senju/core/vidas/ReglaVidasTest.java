package com.senju.core.vidas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReglaVidasTest {
    @Test void restarBaja1() { assertEquals(2, ReglaVidas.restar(3)); }
    @Test void restarLlegaA0() { assertEquals(0, ReglaVidas.restar(1)); }
    @Test void restarNoBajaDe0() { assertEquals(0, ReglaVidas.restar(0)); }
    @Test void restarNegativoSeQuedaEn0() { assertEquals(0, ReglaVidas.restar(-4)); }
    @Test void cero_eliminado() { assertTrue(ReglaVidas.eliminado(0)); }
    @Test void uno_noEliminado() { assertFalse(ReglaVidas.eliminado(1)); }
    @Test void avisoMuerte() { assertEquals(TipoAviso.MUERTE, ReglaVidas.avisoTrasMuerte(2)); }
    @Test void avisoEliminado() { assertEquals(TipoAviso.ELIMINADO, ReglaVidas.avisoTrasMuerte(0)); }
}
