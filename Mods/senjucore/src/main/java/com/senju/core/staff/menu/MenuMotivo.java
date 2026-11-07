package com.senju.core.staff.menu;

import com.mojang.authlib.GameProfile;
import com.senju.core.config.CommonConfig;
import com.senju.core.staff.castigos.TipoCastigo;
import java.util.ArrayList;
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

/** Asistente de castigo, paso 3: motivo (uno por entrada de la config, paginado). Solo selecciona; no aplica nada. */
public class MenuMotivo extends MenuSoloLectura {
    private static final int FILAS = 6;
    private static final int POR_PAGINA = 45;
    static final int SLOT_VOLVER = 45;
    static final int SLOT_ANTERIOR = 48;
    static final int SLOT_SIGUIENTE = 50;

    private final GameProfile objetivo;
    private final TipoCastigo castigo;
    private final long duracion;
    private final int paginaSelector;
    private final List<String> motivos;
    private int pagina;

    private MenuMotivo(int id, Inventory inv, Container c, GameProfile objetivo, TipoCastigo castigo, long duracion,
            int paginaSelector, int pagina, List<String> motivos) {
        super(id, inv, c, FILAS);
        this.objetivo = objetivo;
        this.castigo = castigo;
        this.duracion = duracion;
        this.paginaSelector = paginaSelector;
        this.pagina = pagina;
        this.motivos = motivos;
    }

    /** Motivos de la config (sin vacios); si no hay ninguno, un unico "Otro". */
    static List<String> motivosConfigurados() {
        List<String> r = new ArrayList<>();
        for (String s : CommonConfig.MOTIVOS.get()) {
            if (s != null && !s.isBlank()) r.add(s);
        }
        if (r.isEmpty()) r.add(Component.translatable("senjucore.menu.castigo.motivo.otro").getString());
        return r;
    }

    /** @param pagina pagina de motivos a mostrar (se acota) */
    public static void abrir(ServerPlayer staff, GameProfile objetivo, TipoCastigo castigo, long duracion,
            int paginaSelector, int pagina) {
        List<String> motivos = motivosConfigurados(); // instantanea: el indice del boton siempre coincide
        MenuProvider prov = new SimpleMenuProvider((id, inv, pl) -> {
            MenuMotivo m = new MenuMotivo(id, inv, new SimpleContainer(FILAS * 9), objetivo, castigo, duracion,
                    paginaSelector, pagina, motivos);
            m.construir();
            return m;
        }, Component.translatable("senjucore.menu.castigo.motivo"));
        staff.openMenu(prov);
    }

    private void construir() {
        pagina = Paginacion.acotar(pagina, motivos.size(), POR_PAGINA);
        int total = Paginacion.totalPaginas(motivos.size(), POR_PAGINA);
        for (int i = 0; i < contenedor.getContainerSize(); i++) contenedor.setItem(i, BotonesMenu.relleno());
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pagina * POR_PAGINA + i;
            if (idx >= motivos.size()) break;
            contenedor.setItem(i, BotonesMenu.botonLore(new ItemStack(Items.PAPER),
                    BotonesMenu.nombreComp(Component.literal(motivos.get(idx)), ChatFormatting.WHITE), List.of()));
        }
        contenedor.setItem(SLOT_VOLVER, BotonesMenu.volver());
        if (pagina > 0) contenedor.setItem(SLOT_ANTERIOR, BotonesMenu.anterior());
        if (pagina < total - 1) contenedor.setItem(SLOT_SIGUIENTE, BotonesMenu.siguiente());
    }

    @Override
    protected boolean alClicar(int slot, ClickType tipo, ServerPlayer sp) {
        if (slot < POR_PAGINA) {
            int idx = pagina * POR_PAGINA + slot;
            if (idx < motivos.size()) {
                MenuConfirmar.abrir(sp, objetivo, castigo, duracion, motivos.get(idx), paginaSelector, pagina);
                return false;
            }
        } else if (slot == SLOT_VOLVER) {
            if (castigo == TipoCastigo.EXPULSION) MenuElegirCastigo.abrir(sp, objetivo, paginaSelector);
            else MenuDuracion.abrir(sp, objetivo, castigo, paginaSelector);
            return false;
        } else if (slot == SLOT_ANTERIOR && pagina > 0) {
            pagina--;
            construir();
        } else if (slot == SLOT_SIGUIENTE) {
            pagina++; // construir() acota
            construir();
        }
        return true;
    }
}
