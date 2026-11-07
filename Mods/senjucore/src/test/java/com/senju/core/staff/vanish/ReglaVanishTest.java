package com.senju.core.staff.vanish;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReglaVanishTest {
    @Test void objetivoVisibleSeVe() { assertTrue(ReglaVanish.puedeVer(false, false, false)); }
    @Test void ocultoNoStaffNoVe() { assertFalse(ReglaVanish.puedeVer(true, false, false)); }
    @Test void ocultoStaffVe() { assertTrue(ReglaVanish.puedeVer(true, true, false)); }
    @Test void unoMismoSeVe() { assertTrue(ReglaVanish.puedeVer(true, false, true)); }
}
