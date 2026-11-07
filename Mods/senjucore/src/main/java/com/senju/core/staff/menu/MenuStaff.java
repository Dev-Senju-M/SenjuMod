package com.senju.core.staff.menu;

import com.senju.core.staff.vanish.Vanish;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;

/** Panel del staff: cofre de 3 filas de solo lectura (ver {@link MenuSoloLectura}). */
public class MenuStaff extends MenuSoloLectura {
    private static final int FILAS = 3;
    /** Ticks mínimos entre dos cambios de Vanish desde el menú (evita alternar por clics repetidos). */
    private static final int ESPERA_VANISH = 10;
    /** Último tick de servidor en que cada jugador alternó Vanish desde el menú. */
    private static final Map<UUID, Integer> ULTIMO_VANISH = new HashMap<>();

    private MenuStaff(int id, Inventory inv, Container c) {
        super(id, inv, c, FILAS);
    }

    public static void abrir(ServerPlayer p) {
        MenuProvider prov = new SimpleMenuProvider(
                (id, inv, pl) -> new MenuStaff(id, inv, construir(p)),
                Component.translatable("senjucore.menu.titulo"));
        p.openMenu(prov);
    }

    private static SimpleContainer construir(ServerPlayer p) {
        SimpleContainer c = new SimpleContainer(FILAS * 9);
        for (int i = 0; i < c.getContainerSize(); i++) c.setItem(i, BotonesMenu.relleno());
        c.setItem(AccionMenu.SLOT_CASTIGAR, BotonesMenu.castigar());
        c.setItem(AccionMenu.SLOT_VANISH, BotonesMenu.vanish(Vanish.esta(p)));
        c.setItem(AccionMenu.SLOT_INVENTARIO, BotonesMenu.inventario());
        c.setItem(AccionMenu.SLOT_PROHIBIDOS, BotonesMenu.prohibidos());
        c.setItem(AccionMenu.SLOT_CERRAR, BotonesMenu.cerrar());
        return c;
    }

    @Override
    protected boolean alClicar(int slot, ClickType tipo, ServerPlayer sp) {
        switch (AccionMenu.deSlot(slot)) {
            case VANISH -> {
                int ahora = sp.server.getTickCount();
                Integer antes = ULTIMO_VANISH.get(sp.getUUID());
                if (antes == null || ahora - antes >= ESPERA_VANISH || ahora < antes) {
                    ULTIMO_VANISH.put(sp.getUUID(), ahora);
                    Vanish.alternar(sp);
                    contenedor.setItem(AccionMenu.SLOT_VANISH, BotonesMenu.vanish(Vanish.esta(sp)));
                }
            }
            case INVENTARIO -> {
                MenuSelector.abrir(sp, 0);
                return false;
            }
            case PROHIBIDOS -> {
                MenuProhibidos.abrir(sp, 0);
                return false;
            }
            case CASTIGAR -> sp.displayClientMessage(Component.translatable("senjucore.menu.proximamente"), true);
            case CERRAR -> {
                sp.closeContainer();
                return false;
            }
            case NINGUNA -> { }
        }
        return true;
    }
}
