package com.senju.core.staff.menu;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * Vista en vivo (solo lectura) del inventario de un jugador conectado. Muestra COPIAS: nunca el original
 * (ver {@link CopiaVista}; la Placa de Staff se sustituye por un marcador). El objetivo se resuelve por UUID en
 * cada refresco, así que muerte, cambio de dimensión o desconexión no dejan referencias obsoletas.
 */
public class MenuVerInventario extends MenuSoloLectura {
    private static final int FILAS = 6;
    static final int SLOT_VOLVER = 45;
    static final int SLOT_ENDER = 49;
    static final int SLOT_ACTUALIZAR = 53;

    private final UUID objetivo;
    private final String nombreObjetivo;
    private final int paginaSelector;

    private MenuVerInventario(int id, Inventory inv, Container c, UUID objetivo, String nombre, int paginaSelector) {
        super(id, inv, c, FILAS);
        this.objetivo = objetivo;
        this.nombreObjetivo = nombre;
        this.paginaSelector = paginaSelector;
    }

    public static void abrir(ServerPlayer staff, UUID objetivo) {
        abrir(staff, objetivo, 0);
    }

    /** @param paginaSelector página del selector a la que vuelve "Volver". */
    public static void abrir(ServerPlayer staff, UUID objetivo, int paginaSelector) {
        ServerPlayer t = staff.server.getPlayerList().getPlayer(objetivo);
        if (t == null) {
            staff.sendSystemMessage(Component.translatable("senjucore.verinv.desconectado", "?"));
            MenuSelector.abrir(staff, paginaSelector);
            return;
        }
        String nombre = t.getGameProfile().getName();
        MenuProvider prov = new SimpleMenuProvider((id, inv, pl) -> {
            MenuVerInventario m = new MenuVerInventario(id, inv, new SimpleContainer(FILAS * 9), objetivo, nombre,
                    paginaSelector);
            m.copiar(t);
            return m;
        }, Component.translatable("senjucore.menu.verinv", nombre));
        staff.openMenu(prov);
    }

    /** Reconstruye las copias desde el inventario actual del objetivo. */
    private void copiar(ServerPlayer t) {
        Inventory inv = t.getInventory();
        for (int i = 0; i < contenedor.getContainerSize(); i++) {
            int idx = DisposicionInventario.indiceInventario(i);
            ItemStack s = idx >= 0 && idx < inv.getContainerSize()
                    ? CopiaVista.de(t.server, inv.getItem(idx)) : BotonesMenu.relleno();
            contenedor.setItem(i, s);
        }
        contenedor.setItem(SLOT_VOLVER, BotonesMenu.volver());
        contenedor.setItem(SLOT_ENDER, BotonesMenu.ender());
        contenedor.setItem(SLOT_ACTUALIZAR, BotonesMenu.actualizar());
    }

    /** @return el objetivo conectado, o null tras avisar y volver al selector. */
    private ServerPlayer objetivoOVolver(ServerPlayer staff) {
        ServerPlayer t = staff.server.getPlayerList().getPlayer(objetivo);
        if (t == null) {
            staff.sendSystemMessage(Component.translatable("senjucore.verinv.desconectado", nombreObjetivo));
            MenuSelector.abrir(staff, paginaSelector);
        }
        return t;
    }

    @Override
    protected boolean refrescar(ServerPlayer staff) {
        ServerPlayer t = objetivoOVolver(staff);
        if (t == null) return false;
        copiar(t);
        return true;
    }

    @Override
    protected boolean alClicar(int slot, ClickType tipo, ServerPlayer sp) {
        switch (slot) {
            case SLOT_VOLVER -> {
                MenuSelector.abrir(sp, paginaSelector);
                return false;
            }
            case SLOT_ENDER -> {
                if (objetivoOVolver(sp) == null) return false;
                MenuVerEnder.abrir(sp, objetivo, paginaSelector);
                return false;
            }
            case SLOT_ACTUALIZAR -> {
                return refrescar(sp);
            }
            default -> { }
        }
        return true;
    }
}
