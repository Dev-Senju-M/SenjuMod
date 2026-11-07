package com.senju.core.staff.menu;

/** Correspondencia slot del menú (6 filas) -> índice en Inventory. Lógica pura. */
public final class DisposicionInventario {
    private DisposicionInventario() {}

    public static int indiceInventario(int slotMenu) {
        if (slotMenu >= 0 && slotMenu <= 26) return slotMenu + 9;
        if (slotMenu >= 27 && slotMenu <= 35) return slotMenu - 27;
        if (slotMenu >= 36 && slotMenu <= 39) return 39 - (slotMenu - 36);
        if (slotMenu == 40) return 40;
        return -1;
    }
}
