package com.senju.core.misiones;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReglaOrigenTest {
    @Test
    void mismoTickDentroDelRadio() {
        assertTrue(ReglaOrigen.coincide(100, 100, 1.0, 1.5));
    }

    @Test
    void unTickDespuesDentroDelRadio() {
        assertTrue(ReglaOrigen.coincide(100, 101, 2.0, 1.5));
    }

    @Test
    void dosTicksDespuesNoCoincide() {
        assertFalse(ReglaOrigen.coincide(100, 102, 0, 1.5));
    }

    @Test
    void fueraDelRadioNoCoincide() {
        assertFalse(ReglaOrigen.coincide(100, 100, 2.26, 1.5));
    }
}
