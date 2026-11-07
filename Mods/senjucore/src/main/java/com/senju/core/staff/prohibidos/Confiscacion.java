package com.senju.core.staff.prohibidos;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import com.senju.core.staff.Staff;

/** Confisca objetos prohibidos a los jugadores que no son staff. Nunca crea ItemEntity. */
public class Confiscacion {
    private int ticks = 0;

    @SubscribeEvent
    public void alTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (++ticks < 20) return;
        ticks = 0;
        MinecraftServer server = e.getServer();
        if (server == null || Prohibidos.tamano(server) == 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) revisar(p);
    }

    @SubscribeEvent
    public void alRecoger(PlayerEvent.ItemPickupEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) revisar(p);
    }

    /** Antes de guardar y antes de que menu.removed() suelte cursor/rejilla al suelo. */
    @SubscribeEvent
    public void alSalir(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) revisar(p);
    }

    /** Un no staff tira un objeto prohibido (Q, arrastrar fuera): se cancela y se confisca. */
    @SubscribeEvent
    public void alTirar(ItemTossEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p)) return;
        confiscarSuelto(p, java.util.List.of(e.getEntity()), "tirado", () -> e.setCanceled(true));
    }

    /** Un no staff muere: sus objetos prohibidos no llegan al suelo. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void alMorirDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        java.util.List<ItemEntity> prohibidos = new java.util.ArrayList<>();
        MinecraftServer server = p.server;
        if (server == null || Prohibidos.tamano(server) == 0 || Staff.esStaff(p)) return;
        for (ItemEntity ie : e.getDrops()) if (Prohibidos.esProhibido(server, ie.getItem())) prohibidos.add(ie);
        confiscarSuelto(p, prohibidos, "muerte", () -> e.getDrops().removeAll(prohibidos));
    }

    private static void confiscarSuelto(ServerPlayer p, java.util.List<ItemEntity> items, String origen, Runnable quitar) {
        try {
            MinecraftServer server = p.server;
            if (server == null || Prohibidos.tamano(server) == 0 || Staff.esStaff(p)) return;
            Map<String, int[]> grupos = new LinkedHashMap<>();
            for (ItemEntity ie : items) {
                ItemStack s = ie.getItem();
                if (Prohibidos.esProhibido(server, s)) sumar(grupos, s, origen);
            }
            if (grupos.isEmpty()) return;
            quitar.run();
            avisar(server, p, grupos);
        } catch (RuntimeException ex) {
            com.senju.core.SenjuCore.LOGGER.error("Error al confiscar objetos prohibidos", ex);
        }
    }

    /** Revisa a un jugador (no staff) y confisca lo prohibido. */
    public static void revisar(ServerPlayer p) {
        try {
            MinecraftServer server = p.server;
            if (server == null || Prohibidos.tamano(server) == 0 || Staff.esStaff(p)) return;
            Map<String, int[]> grupos = new LinkedHashMap<>(); // "id|origen" -> cantidad
            Inventory inv = p.getInventory();
            barrer(server, inv.items, "inventario", grupos);
            barrer(server, inv.armor, "armadura", grupos);
            barrer(server, inv.offhand, "mano_secundaria", grupos);
            ItemStack cursor = p.containerMenu.getCarried();
            if (Prohibidos.esProhibido(server, cursor)) {
                sumar(grupos, cursor, "cursor");
                p.containerMenu.setCarried(ItemStack.EMPTY);
                p.containerMenu.sendAllDataToRemote();
            }
            barrer(server, p.getEnderChestInventory(), grupos);
            barrer(server, p.inventoryMenu.getCraftSlots(), "crafteo", grupos);
            AbstractContainerMenu menu = p.containerMenu;
            if (menu != p.inventoryMenu) {
                java.util.Set<Container> vistos = new java.util.HashSet<>();
                for (Slot sl : menu.slots) {
                    if (esTransitorio(menu, sl.container) && vistos.add(sl.container)) {
                        barrer(server, sl.container, "crafteo", grupos);
                    }
                }
            }
            if (grupos.isEmpty()) return;
            avisar(server, p, grupos);
        } catch (RuntimeException ex) {
            com.senju.core.SenjuCore.LOGGER.error("Error al confiscar objetos prohibidos", ex);
        }
    }

    private static void barrer(MinecraftServer server, java.util.List<ItemStack> lista, String origen, Map<String, int[]> grupos) {
        for (int i = 0; i < lista.size(); i++) {
            ItemStack s = lista.get(i);
            if (Prohibidos.esProhibido(server, s)) {
                sumar(grupos, s, origen);
                lista.set(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Contenedores de entrada de un menú (rejilla, yunque, piedra de afilar...) cuyo contenido se suelta
     * al cerrar. Nunca el Inventory, un BlockEntity ni un Entity. Los de resultado se ignoran: son derivados.
     */
    private static boolean esTransitorio(AbstractContainerMenu menu, Container c) {
        if (c instanceof Inventory || c instanceof ResultContainer
                || c instanceof net.minecraft.world.level.block.entity.BlockEntity
                || c instanceof net.minecraft.world.entity.Entity
                || c instanceof net.minecraft.world.inventory.PlayerEnderChestContainer) return false;
        if (c instanceof CraftingContainer) return true;
        return menu instanceof ItemCombinerMenu || menu instanceof GrindstoneMenu || menu instanceof StonecutterMenu
                || menu instanceof LoomMenu || menu instanceof CartographyTableMenu || menu instanceof EnchantmentMenu;
    }

    private static void barrer(MinecraftServer server, Container c, Map<String, int[]> grupos) {
        barrer(server, c, "ender", grupos);
    }

    private static void barrer(MinecraftServer server, Container c, String origen, Map<String, int[]> grupos) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (Prohibidos.esProhibido(server, s)) {
                sumar(grupos, s, origen);
                c.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static void sumar(Map<String, int[]> grupos, ItemStack s, String origen) {
        String clave = ForgeRegistries.ITEMS.getKey(s.getItem()) + "|" + origen;
        grupos.computeIfAbsent(clave, k -> new int[1])[0] += s.getCount();
    }

    private static void avisar(MinecraftServer server, ServerPlayer p, Map<String, int[]> grupos) {
        String nombre = p.getGameProfile().getName();
        String uuid = p.getUUID().toString();
        Map<String, Integer> porItem = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> g : grupos.entrySet()) {
            int corte = g.getKey().lastIndexOf('|');
            String id = g.getKey().substring(0, corte);
            String origen = g.getKey().substring(corte + 1);
            RegistroConfiscados.anotar(server, nombre, uuid, id, g.getValue()[0], origen);
            porItem.merge(id, g.getValue()[0], Integer::sum);
        }
        for (Map.Entry<String, Integer> it : porItem.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(it.getKey()));
            Component nom = item != null ? new ItemStack(item).getHoverName() : Component.literal(it.getKey());
            p.sendSystemMessage(Component.translatable("senjucore.confiscado.jugador", nom, it.getValue()));
            Component aviso = Component.translatable("senjucore.confiscado.staff", nombre, nom, it.getValue());
            for (ServerPlayer s : server.getPlayerList().getPlayers()) {
                if (Staff.esStaff(s)) s.sendSystemMessage(aviso);
            }
        }
    }
}
