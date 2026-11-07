package com.senju.core.staff.menu;

import com.senju.core.staff.Staff;
import com.senju.core.staff.vanish.Vanish;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Menú del staff: cofre de 3 filas solo de servidor (el cliente usa la pantalla vanilla).
 * {@link #clicked} nunca llama a super, así ningún tipo de clic mueve ítems.
 */
public class MenuStaff extends ChestMenu {
    private static final int FILAS = 3;
    /** Ticks mínimos entre dos cambios de Vanish desde el menú (evita alternar por clics repetidos). */
    private static final int ESPERA_VANISH = 10;
    /** Último tick de servidor en que cada jugador alternó Vanish desde el menú. */
    private static final java.util.Map<java.util.UUID, Integer> ULTIMO_VANISH = new java.util.HashMap<>();
    private final Container menuContainer;

    private MenuStaff(int id, Inventory inv, Container c) {
        super(MenuType.GENERIC_9x3, id, inv, c, FILAS);
        this.menuContainer = c;
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
        c.setItem(AccionMenu.SLOT_CERRAR, BotonesMenu.cerrar());
        return c;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        // Sin super: bloquea todo tipo de clic. Solo PICKUP simple activa botones.
        if (player instanceof ServerPlayer sp && type == ClickType.PICKUP
                && slotId >= 0 && slotId < menuContainer.getContainerSize()) {
            switch (AccionMenu.deSlot(slotId)) {
                case VANISH -> {
                    int ahora = sp.server.getTickCount();
                    Integer antes = ULTIMO_VANISH.get(sp.getUUID());
                    if (antes == null || ahora - antes >= ESPERA_VANISH || ahora < antes) {
                        ULTIMO_VANISH.put(sp.getUUID(), ahora);
                        Vanish.alternar(sp);
                        menuContainer.setItem(AccionMenu.SLOT_VANISH, BotonesMenu.vanish(Vanish.esta(sp)));
                    }
                }
                case CASTIGAR, INVENTARIO ->
                        sp.displayClientMessage(Component.translatable("senjucore.menu.proximamente"), true);
                case CERRAR -> {
                    sp.closeContainer();
                    return;
                }
                case NINGUNA -> { }
            }
        }
        // Deshace cualquier predicción del cliente (cursor, slots propios).
        if (!getCarried().isEmpty()) setCarried(ItemStack.EMPTY);
        if (player instanceof ServerPlayer) sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return player instanceof ServerPlayer sp && Staff.esStaff(sp);
    }
}
