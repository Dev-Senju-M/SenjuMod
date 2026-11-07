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
import net.minecraft.world.item.ItemStack;

/**
 * Asistente de castigo, paso 4: resumen y confirmacion. Es el UNICO sitio donde se aplica algo (boton Aplicar).
 * Tras aplicar se abre otro menu (con otro id de ventana), asi que un segundo clic rapido sobre este menu viejo lo
 * ignora el servidor vanilla; ademas {@code aplicado} lo bloquea por si el clic llegara igualmente.
 */
public class MenuConfirmar extends MenuSoloLectura {
    private static final int FILAS = 3;
    static final int SLOT_APLICAR = 11;
    static final int SLOT_RESUMEN = 13;
    static final int SLOT_CANCELAR = 15;
    static final int SLOT_VOLVER = 22;

    private final GameProfile objetivo;
    private final TipoCastigo castigo;
    private final long duracion;
    private final String motivo;
    private final int paginaSelector;
    private final int paginaMotivo;
    private boolean aplicado;

    private MenuConfirmar(int id, Inventory inv, Container c, GameProfile objetivo, TipoCastigo castigo,
            long duracion, String motivo, int paginaSelector, int paginaMotivo) {
        super(id, inv, c, FILAS);
        this.objetivo = objetivo;
        this.castigo = castigo;
        this.duracion = duracion;
        this.motivo = motivo;
        this.paginaSelector = paginaSelector;
        this.paginaMotivo = paginaMotivo;
    }

    public static void abrir(ServerPlayer staff, GameProfile objetivo, TipoCastigo castigo, long duracion,
            String motivo, int paginaSelector, int paginaMotivo) {
        MenuProvider prov = new SimpleMenuProvider((id, inv, pl) -> {
            MenuConfirmar m = new MenuConfirmar(id, inv, new SimpleContainer(FILAS * 9), objetivo, castigo, duracion,
                    motivo, paginaSelector, paginaMotivo);
            m.construir();
            return m;
        }, Component.translatable("senjucore.menu.castigo.confirmar"));
        staff.openMenu(prov);
    }

    /** Texto de la duracion para mostrar: raya si no aplica, "todo el dia" al quitar horas permanente. */
    static Component textoDuracion(TipoCastigo t, long ms) {
        if (t == TipoCastigo.EXPULSION) return Component.literal("—");
        if (t == TipoCastigo.QUITAR_HORAS && ms == Duracion.PERMANENTE) return Component.literal(Castigos.TODO_EL_DIA);
        if (ms == Duracion.PERMANENTE) return Component.translatable("senjucore.castigo.permanente");
        return Component.literal(Duracion.formatoDuracion(ms));
    }

    private void construir() {
        for (int i = 0; i < contenedor.getContainerSize(); i++) contenedor.setItem(i, BotonesMenu.relleno());
        ItemStack resumen = BotonesMenu.botonLore(BotonesMenu.cabeza(objetivo),
                BotonesMenu.nombreComp(Component.translatable("senjucore.menu.castigo.resumen", objetivo.getName()),
                        ChatFormatting.GOLD),
                List.of(Component.translatable("senjucore.menu.castigo.lore.castigo",
                                Component.translatable("senjucore.castigo.tipo." + castigo.clave())),
                        Component.translatable("senjucore.menu.castigo.lore.duracion", textoDuracion(castigo, duracion)),
                        Component.translatable("senjucore.menu.castigo.lore.motivo", motivo)));
        contenedor.setItem(SLOT_RESUMEN, resumen);
        contenedor.setItem(SLOT_APLICAR, BotonesMenu.aplicar());
        contenedor.setItem(SLOT_CANCELAR, BotonesMenu.cancelar());
        contenedor.setItem(SLOT_VOLVER, BotonesMenu.volver());
    }

    @Override
    protected boolean alClicar(int slot, ClickType tipo, ServerPlayer sp) {
        if (aplicado) return false;
        switch (slot) {
            case SLOT_APLICAR -> {
                aplicado = true;
                Castigos.Resultado r = Castigos.aplicar(sp, objetivo, castigo, duracion, motivo);
                if (r == Castigos.Resultado.SIN_CARCEL) {
                    sp.displayClientMessage(Component.translatable("senjucore.castigo.sincarcel"), true);
                }
                // OK, ES_STAFF y VETADO: de vuelta al panel (ES_STAFF ya aviso al actor desde Castigos).
                MenuStaff.abrir(sp);
                return false;
            }
            case SLOT_CANCELAR -> {
                aplicado = true; // cierra este menu a cualquier otro clic
                MenuStaff.abrir(sp);
                return false;
            }
            case SLOT_VOLVER -> {
                MenuMotivo.abrir(sp, objetivo, castigo, duracion, paginaSelector, paginaMotivo);
                return false;
            }
            default -> { }
        }
        return true;
    }
}
