package com.senju.core.staff.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AccionMenuTest {
    @Test void castigar() { assertEquals(AccionMenu.CASTIGAR, AccionMenu.deSlot(10)); }
    @Test void vanish() { assertEquals(AccionMenu.VANISH, AccionMenu.deSlot(13)); }
    @Test void inventario() { assertEquals(AccionMenu.INVENTARIO, AccionMenu.deSlot(16)); }
    @Test void cerrar() { assertEquals(AccionMenu.CERRAR, AccionMenu.deSlot(22)); }
    @Test void otrosNinguna() {
        assertEquals(AccionMenu.NINGUNA, AccionMenu.deSlot(0));
        assertEquals(AccionMenu.NINGUNA, AccionMenu.deSlot(26));
        assertEquals(AccionMenu.NINGUNA, AccionMenu.deSlot(-999));
        assertEquals(AccionMenu.NINGUNA, AccionMenu.deSlot(40));
    }
}
