package com.senju.core.staff.menu;

/** Acción asociada a cada slot del menú del staff (lógica pura). */
public enum AccionMenu {
    CASTIGAR, VANISH, INVENTARIO, PROHIBIDOS, CERRAR, NINGUNA;

    public static final int SLOT_CASTIGAR = 10;
    public static final int SLOT_VANISH = 13;
    public static final int SLOT_INVENTARIO = 16;
    public static final int SLOT_PROHIBIDOS = 19;
    public static final int SLOT_CERRAR = 22;

    public static AccionMenu deSlot(int slot) {
        return switch (slot) {
            case SLOT_CASTIGAR -> CASTIGAR;
            case SLOT_VANISH -> VANISH;
            case SLOT_INVENTARIO -> INVENTARIO;
            case SLOT_PROHIBIDOS -> PROHIBIDOS;
            case SLOT_CERRAR -> CERRAR;
            default -> NINGUNA;
        };
    }
}
