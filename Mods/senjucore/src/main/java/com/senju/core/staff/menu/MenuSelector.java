package com.senju.core.staff.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;

/** Selector de jugador conectado (cabezas, 45 por página). Se actualiza cada segundo. */
public class MenuSelector extends MenuSoloLectura {
    private static final int FILAS = 6;
    private static final int POR_PAGINA = 45;
    static final int SLOT_VOLVER = 45;
    static final int SLOT_ANTERIOR = 48;
    static final int SLOT_SIGUIENTE = 50;

    private int pagina;
    /** UUID mostrado en cada slot de cabeza de la página actual (coincide con lo que ve el staff). */
    private final List<UUID> mostrados = new ArrayList<>();

    private MenuSelector(int id, Inventory inv, Container c, int pagina) {
        super(id, inv, c, FILAS);
        this.pagina = pagina;
    }

    public static void abrir(ServerPlayer staff, int pagina) {
        MenuProvider prov = new SimpleMenuProvider((id, inv, pl) -> {
            MenuSelector m = new MenuSelector(id, inv, new SimpleContainer(FILAS * 9), pagina);
            m.reconstruir(staff);
            return m;
        }, Component.translatable("senjucore.menu.selector"));
        staff.openMenu(prov);
    }

    private void reconstruir(ServerPlayer staff) {
        List<ServerPlayer> todos = new ArrayList<>(staff.server.getPlayerList().getPlayers());
        todos.sort(Comparator.comparing((ServerPlayer p) -> p.getGameProfile().getName().toLowerCase())
                .thenComparing(p -> p.getUUID()));
        pagina = Paginacion.acotar(pagina, todos.size(), POR_PAGINA);
        int total = Paginacion.totalPaginas(todos.size(), POR_PAGINA);
        mostrados.clear();
        for (int i = 0; i < contenedor.getContainerSize(); i++) contenedor.setItem(i, BotonesMenu.relleno());
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pagina * POR_PAGINA + i;
            if (idx >= todos.size()) break;
            ServerPlayer p = todos.get(idx);
            mostrados.add(p.getUUID());
            contenedor.setItem(i, BotonesMenu.cabeza(p.getGameProfile()));
        }
        contenedor.setItem(SLOT_VOLVER, BotonesMenu.volver());
        if (pagina > 0) contenedor.setItem(SLOT_ANTERIOR, BotonesMenu.anterior());
        if (pagina < total - 1) contenedor.setItem(SLOT_SIGUIENTE, BotonesMenu.siguiente());
    }

    @Override
    protected boolean refrescar(ServerPlayer p) {
        reconstruir(p);
        return true;
    }

    @Override
    protected boolean alClicar(int slot, ClickType tipo, ServerPlayer sp) {
        if (slot < POR_PAGINA) {
            if (slot < mostrados.size()) {
                MenuVerInventario.abrir(sp, mostrados.get(slot), pagina);
                return false;
            }
        } else if (slot == SLOT_VOLVER) {
            MenuStaff.abrir(sp);
            return false;
        } else if (slot == SLOT_ANTERIOR && pagina > 0) {
            pagina--;
            reconstruir(sp);
        } else if (slot == SLOT_SIGUIENTE) {
            pagina++; // reconstruir() acota si no hay más páginas
            reconstruir(sp);
        }
        return true;
    }
}
