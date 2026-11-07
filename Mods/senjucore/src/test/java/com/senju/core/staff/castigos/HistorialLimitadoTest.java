package com.senju.core.staff.castigos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistorialLimitadoTest {
    @Test
    void ciento_uno_entradas_conservan_las_100_nuevas() {
        List<Integer> h = new ArrayList<>();
        for (int i = 1; i <= 101; i++) HistorialLimitado.agregar(h, i, 100);
        assertEquals(100, h.size());
        assertEquals(2, h.get(0));
        assertEquals(101, h.get(99));
    }

    @Test
    void ultimos_devuelve_la_mas_reciente_primero() {
        List<Integer> h = new ArrayList<>(List.of(1, 2, 3, 4));
        assertEquals(List.of(4, 3), HistorialLimitado.ultimos(h, 2));
    }

    @Test
    void n_mayor_que_el_tamano_devuelve_todo() {
        List<Integer> h = new ArrayList<>(List.of(1, 2, 3));
        assertEquals(List.of(3, 2, 1), HistorialLimitado.ultimos(h, 50));
    }
}
