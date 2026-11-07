package com.senju.core.staff.menu;

import com.mojang.authlib.GameProfile;
import com.senju.core.staff.castigos.Castigos;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Asistente de castigo, paso 1: elegir el castigo (cabeza del objetivo + 6 botones). Solo selecciona; no aplica nada. */
public class MenuElegirCastigo extends MenuSoloLectura {
    private static final int FILAS = 3;
    static final int SLOT_CABEZA = 4;
    static final int SLOT_PRIMERO = 10; // 10..15
    static final int SLOT_VOLVER = 22;
    static final TipoCastigo[] TIPOS = TipoCastigo.values();

    private final GameProfile objetivo;
    private final int paginaSelector;

    private MenuElegirCastigo(int id, Inventory inv, Container c, GameProfile objetivo, int paginaSelector) {
        super(id, inv, c, FILAS);
        this.objetivo = objetivo;
        this.paginaSelector = paginaSelector;
    }

    public static void abrir(ServerPlayer staff, GameProfile objetivo, int paginaSelector) {
        MenuProvider prov = new SimpleMenuProvider((id, inv, pl) -> {
            MenuElegirCastigo m = new MenuElegirCastigo(id, inv, new SimpleContainer(FILAS * 9), objetivo, paginaSelector);
            m.construir(staff);
            return m;
        }, Component.translatable("senjucore.menu.castigo.elegir", objetivo.getName()));
        staff.openMenu(prov);
    }

    static Item item(TipoCastigo t) {
        return switch (t) {
            case SILENCIO -> Items.NOTE_BLOCK;
            case EXPULSION -> Items.LEATHER_BOOTS;
            case BAN -> Items.BARRIER;
            case CONGELAR -> Items.BLUE_ICE;
            case CARCEL -> Items.IRON_BARS;
            case QUITAR_HORAS -> Items.CLOCK;
        };
    }

    private void construir(ServerPlayer staff) {
        for (int i = 0; i < contenedor.getContainerSize(); i++) contenedor.setItem(i, BotonesMenu.relleno());
        contenedor.setItem(SLOT_CABEZA, BotonesMenu.cabeza(objetivo));
        long ahora = System.currentTimeMillis();
        for (int i = 0; i < TIPOS.length; i++) {
            TipoCastigo t = TIPOS[i];
            List<Component> lore = List.of();
            if (t.tieneDuracion() && Castigos.activo(staff.server, objetivo.getId(), t)) {
                long vence = Castigos.vence(staff.server, objetivo.getId(), t);
                Component resto = vence == Duracion.PERMANENTE
                        ? Component.translatable("senjucore.castigo.permanente")
                        : Component.literal(Duracion.formatoRestante(ahora, vence));
                lore = List.of(BotonesMenu.nombreComp(
                        Component.translatable("senjucore.menu.castigo.activo", resto), ChatFormatting.RED));
            }
            ItemStack s = BotonesMenu.botonLore(new ItemStack(item(t)),
                    BotonesMenu.nombreComp(Component.translatable("senjucore.castigo.tipo." + t.clave()),
                            ChatFormatting.GOLD), lore);
            contenedor.setItem(SLOT_PRIMERO + i, s);
        }
        contenedor.setItem(SLOT_VOLVER, BotonesMenu.volver());
    }

    @Override
    protected boolean refrescar(ServerPlayer p) {
        construir(p);
        return true;
    }

    @Override
    protected boolean alClicar(int slot, ClickType tipo, ServerPlayer sp) {
        if (slot == SLOT_VOLVER) {
            MenuSelector.abrir(sp, paginaSelector, MenuSelector.Modo.CASTIGAR);
            return false;
        }
        int i = slot - SLOT_PRIMERO;
        if (i >= 0 && i < TIPOS.length) {
            TipoCastigo t = TIPOS[i];
            if (t == TipoCastigo.EXPULSION) {
                MenuMotivo.abrir(sp, objetivo, t, 0, paginaSelector, 0);
            } else {
                MenuDuracion.abrir(sp, objetivo, t, paginaSelector);
            }
            return false;
        }
        return true;
    }
}
