package com.senju.core.staff.castigos;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReglaPosicionTest {
    static final String OW = "minecraft:overworld", NE = "minecraft:the_nether";

    @Test void dentroDelRadioDeLaCarcel() {
        assertTrue(ReglaPosicion.dentroCarcel(OW, 0, 64, 0, OW, 0, 64, 0));
        assertTrue(ReglaPosicion.dentroCarcel(OW, 8, 72, 0, OW, 0, 64, 0)); // borde exacto
        assertTrue(ReglaPosicion.dentroCarcel(OW, 5, 64, 5, OW, 0, 64, 0)); // ~7.07
    }
    @Test void fueraDelRadioOdeDimension() {
        assertFalse(ReglaPosicion.dentroCarcel(OW, 8.01, 64, 0, OW, 0, 64, 0));
        assertFalse(ReglaPosicion.dentroCarcel(OW, 6, 64, 6, OW, 0, 64, 0)); // ~8.49 en diagonal
        assertFalse(ReglaPosicion.dentroCarcel(OW, 0, 72.5, 0, OW, 0, 64, 0)); // vertical
        assertFalse(ReglaPosicion.dentroCarcel(OW, 0, 55, 0, OW, 0, 64, 0)); // cayendo
        assertFalse(ReglaPosicion.dentroCarcel(NE, 0, 64, 0, OW, 0, 64, 0)); // otra dimensión
    }
    @Test void congeladoToleraPocoMovimiento() {
        assertFalse(ReglaPosicion.seMovio(OW, 0.05, 64, 0.05, OW, 0, 64, 0));
        assertFalse(ReglaPosicion.seMovio(OW, 0, 64.1, 0, OW, 0, 64, 0));
    }
    @Test void congeladoSeMovio() {
        assertTrue(ReglaPosicion.seMovio(OW, 0.2, 64, 0, OW, 0, 64, 0));
        assertTrue(ReglaPosicion.seMovio(OW, 0, 64.42, 0, OW, 0, 64, 0)); // salto
        assertTrue(ReglaPosicion.seMovio(NE, 0, 64, 0, OW, 0, 64, 0));
    }
}
