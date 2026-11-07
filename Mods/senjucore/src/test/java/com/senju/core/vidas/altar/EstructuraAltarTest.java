package com.senju.core.vidas.altar;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class EstructuraAltarTest {
    private static String valida(int[] d) {
        if (Math.abs(d[0]) == 2 && Math.abs(d[2]) == 2) {
            if (d[1] == 0 || d[1] == 1) return "obsidiana";
            if (d[1] == 2) return "fuego_almas";
        }
        return "aire";
    }

    @Test
    void estructuraValidaNoFalta() {
        assertTrue(EstructuraAltar.faltantes(EstructuraAltarTest::valida).isEmpty());
    }

    @Test
    void pilarSinFuegoEsUnFaltante() {
        Function<int[], String> f = d -> (d[0] == 2 && d[1] == 2 && d[2] == -2) ? "aire" : valida(d);
        List<String> r = EstructuraAltar.faltantes(f);
        assertEquals(1, r.size());
    }

    @Test
    void pilarConObsidianaIncompletaCuenta() {
        Function<int[], String> f = d -> (d[0] == -2 && d[1] == 1 && d[2] == 2) ? "otro" : valida(d);
        assertEquals(1, EstructuraAltar.faltantes(f).size());
    }

    @Test
    void sinNadaFaltanLosCuatroPilares() {
        assertEquals(4, EstructuraAltar.faltantes(d -> "aire").size());
    }
}
