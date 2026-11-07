package com.senju.core.staff.menu;

import com.senju.core.staff.Staff;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Base de los menús del staff: cofre solo de servidor (el cliente usa la pantalla vanilla).
 * {@link #clicked} nunca llama a super, así ningún tipo de clic (shift, arrastre, teclas, doble clic...)
 * mueve ítems. Solo PICKUP simple sobre slots del menú llega a {@link #alClicar}.
 */
public abstract class MenuSoloLectura extends ChestMenu {
    /** Cada cuántos ticks se llama a {@link #refrescar}. */
    protected static final int PERIODO_REFRESCO = 20;

    protected final Container contenedor;
    private final Player espectador;
    private int ticks;

    protected MenuSoloLectura(int id, Inventory inv, Container c, int filas) {
        super(tipo(filas), id, inv, c, filas);
        this.contenedor = c;
        this.espectador = inv.player;
    }

    private static MenuType<ChestMenu> tipo(int filas) {
        return switch (filas) {
            case 3 -> MenuType.GENERIC_9x3;
            case 6 -> MenuType.GENERIC_9x6;
            default -> throw new IllegalArgumentException("filas: " + filas);
        };
    }

    /**
     * Clic simple sobre un slot del menú.
     * @return false si el clic abrió otro menú o cerró este (no hay nada más que sincronizar)
     */
    protected abstract boolean alClicar(int slot, ClickType tipo, ServerPlayer p);

    /**
     * Refresco periódico (cada {@link #PERIODO_REFRESCO} ticks). Por defecto no hace nada.
     * @return false si el refresco abrió otro menú o cerró este
     */
    protected boolean refrescar(ServerPlayer p) {
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        // Sin super: bloquea todo tipo de clic. Solo PICKUP simple activa botones.
        if (player instanceof ServerPlayer sp && type == ClickType.PICKUP
                && slotId >= 0 && slotId < contenedor.getContainerSize()) {
            if (!alClicar(slotId, type, sp)) return;
        }
        // Deshace cualquier predicción del cliente (cursor, slots propios).
        if (!getCarried().isEmpty()) setCarried(ItemStack.EMPTY);
        if (player instanceof ServerPlayer) sendAllDataToRemote();
    }

    /** Se llama cada tick del jugador; aprovecha para el refresco en vivo. Solo se envían los slots que cambian. */
    @Override
    public void broadcastChanges() {
        if (++ticks >= PERIODO_REFRESCO) {
            ticks = 0;
            if (espectador instanceof ServerPlayer sp && !refrescar(sp)) return;
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return player instanceof ServerPlayer sp && Staff.esStaff(sp);
    }
}
