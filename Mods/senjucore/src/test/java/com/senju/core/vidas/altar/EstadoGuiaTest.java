package com.senju.core.vidas.altar;

import static com.senju.core.vidas.altar.EstadoGuia.Esperado.*;
import static com.senju.core.vidas.altar.EstadoGuia.EstadoPos.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EstadoGuiaTest {
    @Test
    void estadoPorPosicion() {
        assertEquals(OK, EstadoGuia.estado(OBSIDIANA, "obsidiana"));
        assertEquals(FALTA, EstadoGuia.estado(OBSIDIANA, "aire"));
        assertEquals(INCORRECTO, EstadoGuia.estado(OBSIDIANA, "otro"));
        assertEquals(OK, EstadoGuia.estado(FUEGO_ALMAS, "fuego_almas"));
        assertEquals(INCORRECTO, EstadoGuia.estado(FUEGO_ALMAS, "obsidiana"));
        assertEquals(FALTA, EstadoGuia.estado(FUEGO_ALMAS, "aire"));
    }

    private static List<EstadoGuia.EstadoPos> todos(int pilaresOk) {
        List<EstadoGuia.EstadoPos> l = new ArrayList<>();
        for (int i = 0; i < 12; i++) l.add(i / 3 < pilaresOk ? OK : FALTA);
        return l;
    }

    @Test
    void posicionesSonDoceEnTresPorPilar() {
        var p = EstructuraAltar.posiciones();
        assertEquals(12, p.size());
        for (int i = 0; i < 12; i++) assertEquals(i % 3 == 2 ? FUEGO_ALMAS : OBSIDIANA, p.get(i).esperado());
    }

    @Test
    void pilaresCompletos() {
        assertEquals(0, EstadoGuia.pilaresCompletos(todos(0)));
        assertEquals(2, EstadoGuia.pilaresCompletos(todos(2)));
        assertEquals(4, EstadoGuia.pilaresCompletos(todos(4)));
    }

    @Test
    void faltanCuentaFaltaEIncorrectoPorTipo() {
        var esp = new ArrayList<EstadoGuia.Esperado>();
        for (var p : EstructuraAltar.posiciones()) esp.add(p.esperado());
        assertArrayEquals(new int[] {0, 0}, EstadoGuia.faltan(esp, todos(4)));
        assertArrayEquals(new int[] {8, 4}, EstadoGuia.faltan(esp, todos(0)));
        var l = todos(4);
        l.set(0, FALTA);
        l.set(1, INCORRECTO);
        l.set(2, INCORRECTO);
        assertArrayEquals(new int[] {2, 1}, EstadoGuia.faltan(esp, l));
    }
}
