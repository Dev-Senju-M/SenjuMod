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
 * Vista en vivo (solo lectura) del cofre de ender de un jugador conectado. Forma elegida: 6 filas, las 3
 * primeras con el contenido (27 casillas), relleno en las demás y "Volver" (a la vista de inventario) en la 45.
 * Usa copias, igual que {@link MenuVerInventario}.
 */
public class MenuVerEnder extends MenuSoloLectura {
    private static final int FILAS = 6;
    private static final int CASILLAS = 27;
    static final int SLOT_VOLVER = 45;

    private final UUID objetivo;
    private final String nombreObjetivo;
    private final int paginaSelector;

    private MenuVerEnder(int id, Inventory inv, Container c, UUID objetivo, String nombre, int paginaSelector) {
        super(id, inv, c, FILAS);
        this.objetivo = objetivo;
        this.nombreObjetivo = nombre;
        this.paginaSelector = paginaSelector;
    }

    public static void abrir(ServerPlayer staff, UUID objetivo, int paginaSelector) {
        ServerPlayer t = staff.server.getPlayerList().getPlayer(objetivo);
        if (t == null) {
            staff.sendSystemMessage(Component.translatable("senjucore.verinv.desconectado", "?"));
            MenuSelector.abrir(staff, paginaSelector);
            return;
        }
        String nombre = t.getGameProfile().getName();
        MenuProvider prov = new SimpleMenuProvider((id, inv, pl) -> {
            MenuVerEnder m = new MenuVerEnder(id, inv, new SimpleContainer(FILAS * 9), objetivo, nombre,
                    paginaSelector);
            m.copiar(t);
            return m;
        }, Component.translatable("senjucore.menu.verender", nombre));
        staff.openMenu(prov);
    }

    private void copiar(ServerPlayer t) {
        Container ender = t.getEnderChestInventory();
        for (int i = 0; i < contenedor.getContainerSize(); i++) {
            ItemStack s = i < CASILLAS && i < ender.getContainerSize()
                    ? CopiaVista.de(t.server, ender.getItem(i)) : BotonesMenu.relleno();
            contenedor.setItem(i, s);
        }
        contenedor.setItem(SLOT_VOLVER, BotonesMenu.volver());
    }

    @Override
    protected boolean refrescar(ServerPlayer staff) {
        ServerPlayer t = staff.server.getPlayerList().getPlayer(objetivo);
        if (t == null) {
            staff.sendSystemMessage(Component.translatable("senjucore.verinv.desconectado", nombreObjetivo));
            MenuSelector.abrir(staff, paginaSelector);
            return false;
        }
        copiar(t);
        return true;
    }

    @Override
    protected boolean alClicar(int slot, ClickType tipo, ServerPlayer sp) {
        if (slot == SLOT_VOLVER) {
            MenuVerInventario.abrir(sp, objetivo, paginaSelector);
            return false;
        }
        return true;
    }
}
