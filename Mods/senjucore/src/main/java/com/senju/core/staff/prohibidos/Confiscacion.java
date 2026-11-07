package com.senju.core.staff.prohibidos;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

    private static void barrer(MinecraftServer server, net.minecraft.world.Container c, Map<String, int[]> grupos) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (Prohibidos.esProhibido(server, s)) {
                sumar(grupos, s, "ender");
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
