package com.senju.core.staff.menu;

import com.mojang.authlib.GameProfile;
import com.senju.core.staff.castigos.Duracion;
import com.senju.core.staff.castigos.TipoCastigo;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Asistente de castigo, paso 2: duración (o cantidad a quitar). Solo selecciona; no aplica nada. */
public class MenuDuracion extends MenuSoloLectura {
    private static final int FILAS = 3;
    static final int SLOT_PRIMERO = 11;
    static final int SLOT_VOLVER = 22;

    static final long[] DURACIONES = {600_000L, 3_600_000L, 86_400_000L, 604_800_000L, Duracion.PERMANENTE};
    static final String[] CLAVES_DURACION = {"10m", "1h", "1d", "7d", "perm"};
    static final long[] QUITAR = {1_800_000L, 3_600_000L, 7_200_000L, Duracion.PERMANENTE};
    static final String[] CLAVES_QUITAR = {"30m", "1h", "2h", "todo"};

    private final GameProfile objetivo;
    private final TipoCastigo castigo;
    private final int paginaSelector;

    private MenuDuracion(int id, Inventory inv, Container c, GameProfile objetivo, TipoCastigo castigo,
            int paginaSelector) {
        super(id, inv, c, FILAS);
        this.objetivo = objetivo;
        this.castigo = castigo;
        this.paginaSelector = paginaSelector;
    }

    public static void abrir(ServerPlayer staff, GameProfile objetivo, TipoCastigo castigo, int paginaSelector) {
        MenuProvider prov = new SimpleMenuProvider((id, inv, pl) -> {
            MenuDuracion m = new MenuDuracion(id, inv, new SimpleContainer(FILAS * 9), objetivo, castigo, paginaSelector);
            m.construir();
            return m;
        }, Component.translatable("senjucore.menu.castigo.duracion"));
        staff.openMenu(prov);
    }

    private long[] valores() {
        return castigo == TipoCastigo.QUITAR_HORAS ? QUITAR : DURACIONES;
    }

    private String[] claves() {
        return castigo == TipoCastigo.QUITAR_HORAS ? CLAVES_QUITAR : CLAVES_DURACION;
    }

    private void construir() {
        for (int i = 0; i < contenedor.getContainerSize(); i++) contenedor.setItem(i, BotonesMenu.relleno());
        String[] cl = claves();
        for (int i = 0; i < cl.length; i++) {
            contenedor.setItem(SLOT_PRIMERO + i, BotonesMenu.botonLore(new ItemStack(Items.CLOCK),
                    BotonesMenu.nombreComp(Component.translatable("senjucore.menu.castigo.dur." + cl[i]),
                            ChatFormatting.YELLOW), List.of()));
        }
        contenedor.setItem(SLOT_VOLVER, BotonesMenu.volver());
    }

    @Override
    protected boolean alClicar(int slot, ClickType tipo, ServerPlayer sp) {
        if (slot == SLOT_VOLVER) {
            MenuElegirCastigo.abrir(sp, objetivo, paginaSelector);
            return false;
        }
        int i = slot - SLOT_PRIMERO;
        if (i >= 0 && i < valores().length) {
            MenuMotivo.abrir(sp, objetivo, castigo, valores()[i], paginaSelector, 0);
            return false;
        }
        return true;
    }
}
