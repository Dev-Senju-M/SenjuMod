package com.senju.core.misiones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReglaDiaTest {
    @Test void cambioDeDia() {
        assertFalse(ReglaDia.cambioDeDia(100, 100));
        assertTrue(ReglaDia.cambioDeDia(100, 103));
    }
    @Test void incumplidores() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        assertEquals(List.of(a, c), ReglaDia.incumplidores(List.of(a, b, c, d), Set.of(b), Set.of(d)));
    }
    @Test void segundosHastaMedianoche() {
        assertEquals(600, ReglaDia.segundosHastaMedianoche(LocalDateTime.of(2026, 11, 8, 23, 50)));
    }
    @Test void tocaAviso() {
        assertTrue(ReglaDia.tocaAviso(600, 10));
        assertFalse(ReglaDia.tocaAviso(601, 10));
        assertFalse(ReglaDia.tocaAviso(0, 10));
    }
}
