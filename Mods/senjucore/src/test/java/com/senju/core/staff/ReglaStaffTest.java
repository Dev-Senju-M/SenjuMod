package com.senju.core.staff;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReglaStaffTest {
    @Test void operadorIncluido() { assertTrue(ReglaStaff.esStaff(true, "Ana", List.of(), true)); }
    @Test void operadorNoIncluido() { assertFalse(ReglaStaff.esStaff(true, "Ana", List.of(), false)); }
    @Test void sinMayusculas() { assertTrue(ReglaStaff.esStaff(false, "Bryant", List.of("bryant"), true)); }
    @Test void recortaEspacios() { assertTrue(ReglaStaff.esStaff(false, "Bryant", List.of("  Bryant "), false)); }
    @Test void noEnLista() { assertFalse(ReglaStaff.esStaff(false, "Pepe", List.of("Bryant"), true)); }
    @Test void nombreNulo() { assertFalse(ReglaStaff.esStaff(false, null, List.of("Bryant"), true)); }
    @Test void listaNula() { assertFalse(ReglaStaff.esStaff(false, "Bryant", null, true)); }
    @Test void nombreVacioNoCoincide() { assertFalse(ReglaStaff.esStaff(false, "", List.of(""), true)); }
    @Test void nombreEspaciosNoCoincide() { assertFalse(ReglaStaff.esStaff(false, "  ", List.of(" "), true)); }
    @Test void ignoraEntradasNulasOVacias() {
        assertTrue(ReglaStaff.esStaff(false, "Bryant", Arrays.asList(null, "", "Bryant"), true));
    }
}
