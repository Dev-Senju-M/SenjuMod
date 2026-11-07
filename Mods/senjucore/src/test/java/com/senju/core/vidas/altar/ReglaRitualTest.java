package com.senju.core.vidas.altar;

import static org.junit.jupiter.api.Assertions.*;

import com.senju.core.vidas.altar.ReglaRitual.Condiciones;
import com.senju.core.vidas.altar.ReglaRitual.Falta;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReglaRitualTest {
    private static Condiciones ok() {
        return new Condiciones(true, true, true, 3, 3, true, "Pepe", true);
    }

    @Test
    void todoOkNoFaltaNada() {
        assertTrue(ReglaRitual.verificar(ok()).isEmpty());
    }

    @Test
    void estructura() {
        var c = new Condiciones(false, true, true, 3, 3, true, "Pepe", true);
        assertEquals(List.of(Falta.ESTRUCTURA), ReglaRitual.verificar(c));
    }

    @Test
    void noche() {
        var c = new Condiciones(true, false, true, 3, 3, true, "Pepe", true);
        assertEquals(List.of(Falta.NOCHE), ReglaRitual.verificar(c));
    }

    @Test
    void diaIgnoradoSiNoRequiereNoche() {
        var c = new Condiciones(true, false, false, 3, 3, true, "Pepe", true);
        assertTrue(ReglaRitual.verificar(c).isEmpty());
    }

    @Test
    void jugadores() {
        var c = new Condiciones(true, true, true, 2, 3, true, "Pepe", true);
        assertEquals(List.of(Falta.JUGADORES), ReglaRitual.verificar(c));
    }

    @Test
    void masJugadoresDeLosNecesariosVale() {
        var c = new Condiciones(true, true, true, 5, 3, true, "Pepe", true);
        assertTrue(ReglaRitual.verificar(c).isEmpty());
    }

    @Test
    void totem() {
        var c = new Condiciones(true, true, true, 3, 3, false, "Pepe", true);
        assertEquals(List.of(Falta.TOTEM), ReglaRitual.verificar(c));
    }

    @Test
    void pergaminoSinNombreOVacio() {
        assertEquals(List.of(Falta.PERGAMINO),
                ReglaRitual.verificar(new Condiciones(true, true, true, 3, 3, true, null, false)));
        assertEquals(List.of(Falta.PERGAMINO),
                ReglaRitual.verificar(new Condiciones(true, true, true, 3, 3, true, "   ", false)));
    }

    @Test
    void noEliminado() {
        var c = new Condiciones(true, true, true, 3, 3, true, "Pepe", false);
        assertEquals(List.of(Falta.NO_ELIMINADO), ReglaRitual.verificar(c));
    }

    @Test
    void variasFaltasEnOrden() {
        var c = new Condiciones(false, false, true, 1, 3, false, "Pepe", false);
        assertEquals(List.of(Falta.ESTRUCTURA, Falta.NOCHE, Falta.JUGADORES, Falta.TOTEM, Falta.NO_ELIMINADO),
                ReglaRitual.verificar(c));
    }
}
