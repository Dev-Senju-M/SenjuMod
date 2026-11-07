package com.senju.core.staff.menu;

import com.senju.core.staff.PlacaStaffItem;
import com.senju.core.staff.Staff;
import com.senju.core.staff.prohibidos.Prohibidos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Lista de objetos prohibidos (6 filas; el cliente usa la pantalla vanilla de cofre).
 *
 * No extiende {@link MenuSoloLectura}: aquí el cursor es el ítem real del staff y no se limpia, y el staff
 * puede mover sus propios ítems para elegir cuál prohibir. Garantía: ningún ítem entra ni sale de las
 * casillas del menú (0-53):
 * <ul>
 *   <li>las casillas del menú son {@link SlotFijo} ({@code mayPlace} y {@code mayPickup} siempre false), así
 *       la lógica vanilla nunca coloca ni toma nada en ellas;</li>
 *   <li>{@link #clicked} solo llama a {@code super} para slots del inventario del jugador y solo con
 *       PICKUP, SWAP (botón 0-8 o 40: ambos lados son del jugador) y PICKUP_ALL; THROW (nunca se suelta
 *       nada al suelo), QUICK_CRAFT (arrastre), CLONE, clics fuera de la ventana y cualquier tipo sobre una casilla del menú nunca
 *       llegan a {@code super};</li>
 *   <li>{@link #quickMoveStack} no mueve nada (shift-clic agrega a la lista sin mover el ítem).</li>
 * </ul>
 */
public class MenuProhibidos extends AbstractContainerMenu {
    private static final int FILAS = 6;
    private static final int TAM = FILAS * 9;
    private static final int POR_PAGINA = 45;
    static final int SLOT_VOLVER = 45;
    static final int SLOT_ANTERIOR = 48;
    static final int SLOT_SIGUIENTE = 50;
    /** Cada cuántos ticks se reconstruye la página (otro staff puede cambiar la lista). */
    private static final int PERIODO_REFRESCO = 20;

    private final SimpleContainer contenedor = new SimpleContainer(TAM);
    private final ServerPlayer staff;
    /** ID mostrado en cada casilla 0-44 de la página actual. */
    private final List<String> mostrados = new ArrayList<>();
    private int pagina;
    private int ticks;

    /** Casilla del menú: nada entra ni sale por la lógica vanilla (solo el servidor la escribe con set). */
    private static final class SlotFijo extends Slot {
        SlotFijo(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private MenuProhibidos(int id, Inventory inv, ServerPlayer staff, int pagina) {
        super(MenuType.GENERIC_9x6, id);
        this.staff = staff;
        this.pagina = pagina;
        // Misma disposición y orden de slots que ChestMenu (el cliente crea un ChestMenu de 6 filas).
        int desp = (FILAS - 4) * 18;
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < 9; c++) addSlot(new SlotFijo(contenedor, c + f * 9, 8 + c * 18, 18 + f * 18));
        }
        for (int f = 0; f < 3; f++) {
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + f * 9 + 9, 8 + c * 18, 103 + f * 18 + desp));
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 161 + desp));
    }

    public static void abrir(ServerPlayer staff, int pagina) {
        MenuProvider prov = new SimpleMenuProvider((id, inv, pl) -> {
            MenuProhibidos m = new MenuProhibidos(id, inv, staff, pagina);
            m.reconstruir();
            return m;
        }, Component.translatable("senjucore.menu.prohibidos"));
        staff.openMenu(prov);
    }

    private void reconstruir() {
        int total = Prohibidos.tamano(staff.server);
        pagina = Paginacion.acotar(pagina, total, POR_PAGINA);
        int paginas = Paginacion.totalPaginas(total, POR_PAGINA);
        List<String> ids = Prohibidos.pagina(staff.server, pagina, POR_PAGINA);
        mostrados.clear();
        for (int i = 0; i < POR_PAGINA; i++) contenedor.setItem(i, ItemStack.EMPTY);
        for (int i = POR_PAGINA; i < TAM; i++) contenedor.setItem(i, BotonesMenu.relleno());
        for (int i = 0; i < ids.size() && i < POR_PAGINA; i++) {
            mostrados.add(ids.get(i));
            contenedor.setItem(i, icono(ids.get(i)));
        }
        contenedor.setItem(SLOT_VOLVER, BotonesMenu.volver());
        if (pagina > 0) contenedor.setItem(SLOT_ANTERIOR, BotonesMenu.anterior());
        if (pagina < paginas - 1) contenedor.setItem(SLOT_SIGUIENTE, BotonesMenu.siguiente());
    }

    /** Un ítem nuevo del tipo listado; barrera con el ID si no existe en el registro o sería una Placa. */
    private static ItemStack icono(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && ForgeRegistries.ITEMS.containsKey(rl)) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            // Nunca una Placa en el contenedor: ProteccionPlaca la "extraería" al staff (duplicado).
            if (item != null && item != Items.AIR && !PlacaStaffItem.esPlaca(new ItemStack(item))) {
                return BotonesMenu.listado(item);
            }
        }
        return BotonesMenu.listadoDesconocido(id);
    }

    @Override
    public void clicked(int slotId, int button, ClickType tipo, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        boolean enMenu = slotId >= 0 && slotId < TAM;
        boolean enJugador = slotId >= TAM && slotId < slots.size();
        switch (tipo) {
            case PICKUP -> {
                if (enMenu) {
                    if (!alClicarMenu(slotId, sp)) return;
                } else if (enJugador) {
                    super.clicked(slotId, button, tipo, player); // mover sus propios ítems
                }
                // -999 (fuera de la ventana) y -1: ignorado, nunca se suelta el cursor al suelo.
            }
            case QUICK_MOVE -> {
                if (enJugador) {
                    ItemStack s = slots.get(slotId).getItem();
                    if (!s.isEmpty()) Prohibidos.agregar(sp, s); // sin mover el ítem
                }
            }
            case SWAP -> {
                // Teclas 1-9 / F entre slots del jugador; sobre una casilla del menú: ignorado.
                if (enJugador && ((button >= 0 && button < 9) || button == Inventory.SLOT_OFFHAND)) {
                    super.clicked(slotId, button, tipo, player);
                }
            }
            case PICKUP_ALL -> {
                // Doble clic: las casillas del menú no dejan tomar (SlotFijo + canTakeItemForPickAll).
                if (enJugador) super.clicked(slotId, button, tipo, player);
            }
            default -> { } // THROW (nunca se suelta nada al suelo), QUICK_CRAFT (arrastre) y CLONE: ignorados
        }
        reconstruir();
        // El cursor no se limpia (es el ítem real del staff); se resincroniza todo con el cliente.
        sendAllDataToRemote();
    }

    /**
     * Clic simple (izquierdo o derecho) sobre una casilla del menú. El cursor nunca cambia.
     * @return false si se abrió otro menú
     */
    private boolean alClicarMenu(int slot, ServerPlayer sp) {
        ItemStack cursor = getCarried();
        if (slot < POR_PAGINA) {
            if (!cursor.isEmpty()) {
                Prohibidos.agregar(sp, cursor); // solo lee el tipo; no consume el ítem
            } else if (slot < mostrados.size()) {
                Prohibidos.quitar(sp, mostrados.get(slot));
            }
        } else if (slot == SLOT_VOLVER) {
            // openMenu cierra este menú: removed() devuelve el cursor al inventario del staff.
            MenuStaff.abrir(sp);
            return false;
        } else if (slot == SLOT_ANTERIOR && pagina > 0) {
            pagina--;
        } else if (slot == SLOT_SIGUIENTE) {
            pagina++; // reconstruir() acota si no hay más páginas
        }
        return true;
    }

    /** Se llama cada tick del jugador: refresco periódico de la página. Solo viajan los slots que cambian. */
    @Override
    public void broadcastChanges() {
        if (++ticks >= PERIODO_REFRESCO) {
            ticks = 0;
            reconstruir();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != contenedor;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.container != contenedor;
    }

    @Override
    public boolean stillValid(Player player) {
        return player instanceof ServerPlayer sp && Staff.esStaff(sp);
    }
}
