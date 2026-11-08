package com.senju.core.misiones;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;

/** Eventos del crédito personal de misiones (§3.4): qué consiguió el propio jugador. Solo servidor. */
public class CreditoMisiones {
    private static final double RADIO_BLOQUE = 1.5;
    private static final double RADIO_INTERACCION = 3;

    /** Pociones vistas en las ranuras de salida del soporte abierto (el shift‑clic avisa con un stack vacío). */
    private final Map<UUID, ItemStack[]> pocionesVistas = new HashMap<>();

    /** Jugador real del servidor, no falso, ni creativo ni espectador. */
    @Nullable
    private static ServerPlayer jugador(@Nullable Object o) {
        if (!(o instanceof ServerPlayer p) || p instanceof FakePlayer) return null;
        if (p.isCreative() || p.isSpectator()) return null;
        return p;
    }

    /** Crédito directo (no por recogida): también cuenta como entrada para la instantánea "directo a la mano". */
    private static void acreditar(ServerPlayer p, ItemStack st) {
        if (st.isEmpty()) return;
        OrigenItems.registrarEntrada(p, st.getItem(), st.getCount());
        Misiones.acreditar(p, st.getItem(), st.getCount());
    }

    /** Bloque con contenido (cofre, horno, tolva…): lo que suelta al romperlo no es "conseguido por él". */
    private static boolean tieneContenido(Level l, BlockPos pos) {
        BlockEntity be = l.getBlockEntity(pos);
        if (be == null) return false;
        if (be instanceof Container c && !c.isEmpty()) return true;
        IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (h == null) return false;
        for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    // ---------------------------------------------------------------- marcas de origen

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void alRomper(BlockEvent.BreakEvent e) {
        ServerPlayer p = jugador(e.getPlayer());
        if (p == null || Misiones.idsPedidos(p).isEmpty()) return;
        if (tieneContenido(p.level(), e.getPos())) return;
        OrigenItems.marcarZona(p, Vec3.atCenterOf(e.getPos()), RADIO_BLOQUE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void alInteractuarEntidad(PlayerInteractEvent.EntityInteract e) {
        ServerPlayer p = jugador(e.getEntity());
        if (p == null || e.getTarget() instanceof Player || p.containerMenu != p.inventoryMenu) return;
        if (Misiones.idsPedidos(p).isEmpty()) return;
        OrigenItems.marcarZona(p, e.getTarget().position(), RADIO_INTERACCION);
        OrigenItems.instantanea(p);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void alInteractuarBloque(PlayerInteractEvent.RightClickBlock e) {
        ServerPlayer p = jugador(e.getEntity());
        if (p == null || p.containerMenu != p.inventoryMenu) return;
        if (tieneContenido(p.level(), e.getPos()) || Misiones.idsPedidos(p).isEmpty()) return;
        OrigenItems.marcarZona(p, Vec3.atCenterOf(e.getPos()), RADIO_INTERACCION);
        OrigenItems.instantanea(p);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void alSoltarDrops(LivingDropsEvent e) {
        if (e.getEntity() instanceof Player) return; // el inventario de otro jugador nunca da crédito
        ServerPlayer p = jugador(e.getSource().getEntity());
        if (p == null) return;
        for (ItemEntity ie : e.getDrops()) OrigenItems.etiquetar(ie, p.getUUID());
    }

    @SubscribeEvent
    public void alAparecer(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk()) return;
        if (!(e.getEntity() instanceof ItemEntity ie)) return;
        if (ie.getOwner() != null) return; // tirado por un jugador u otra entidad
        OrigenItems.alAparecer(ie, e.getLevel());
    }

    // ---------------------------------------------------------------- créditos

    @SubscribeEvent
    public void alRecoger(PlayerEvent.ItemPickupEvent e) {
        ServerPlayer p = jugador(e.getEntity());
        if (p == null) return;
        ItemStack st = e.getStack();
        if (st.isEmpty()) return;
        OrigenItems.registrarEntrada(p, st.getItem(), st.getCount());
        if (p.getUUID().equals(OrigenItems.duenio(e.getOriginalEntity()))) {
            Misiones.acreditar(p, st.getItem(), st.getCount());
        }
    }

    @SubscribeEvent
    public void alFundir(PlayerEvent.ItemSmeltedEvent e) {
        ServerPlayer p = jugador(e.getEntity());
        if (p == null) return;
        ItemStack st = e.getSmelting();
        // Tras un shift‑clic vanilla vuelve a avisar con lo que quedó en la ranura: eso no se sacó.
        if (st.isEmpty() || esItemDeRanura(p, st)) return;
        acreditar(p, st);
    }

    @SubscribeEvent
    public void alCraftear(PlayerEvent.ItemCraftedEvent e) {
        ServerPlayer p = jugador(e.getEntity());
        if (p == null) return;
        ItemStack st = e.getCrafting();
        if (st.isEmpty() || esItemDeRanura(p, st)) return;
        acreditar(p, st);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void alPescar(ItemFishedEvent e) {
        ServerPlayer p = jugador(e.getEntity());
        if (p == null) return;
        for (ItemStack st : e.getDrops()) acreditar(p, st);
    }

    @SubscribeEvent
    public void alSacarPocion(PlayerBrewedPotionEvent e) {
        ServerPlayer p = jugador(e.getEntity());
        if (p == null) return;
        ItemStack st = e.getStack();
        if (!st.isEmpty()) {
            acreditar(p, st);
            verPociones(p);
            return;
        }
        // Shift‑clic: vanilla pasa el stack ya vaciado; se deduce de la ranura que se vació.
        ItemStack[] vistas = pocionesVistas.get(p.getUUID());
        if (vistas == null || !(p.containerMenu instanceof BrewingStandMenu m)) return;
        for (int i = 0; i < 3; i++) {
            ItemStack antes = vistas[i];
            if (antes.isEmpty() || !m.getSlot(i).getItem().isEmpty()) continue;
            acreditar(p, antes);
            vistas[i] = ItemStack.EMPTY;
            return;
        }
    }

    @SubscribeEvent
    public void alComerciar(TradeWithVillagerEvent e) {
        ServerPlayer p = jugador(e.getEntity());
        if (p == null) return;
        acreditar(p, e.getMerchantOffer().getResult().copy());
    }

    // ---------------------------------------------------------------- mantenimiento

    @SubscribeEvent
    public void alTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        OrigenItems.finDeTick(e.getServer());
        pocionesVistas.clear();
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (p.containerMenu instanceof BrewingStandMenu) verPociones(p);
        }
    }

    @SubscribeEvent
    public void alSalir(PlayerEvent.PlayerLoggedOutEvent e) {
        OrigenItems.olvidar(e.getEntity().getUUID());
        pocionesVistas.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public void alParar(ServerStoppedEvent e) {
        OrigenItems.limpiar();
        pocionesVistas.clear();
    }

    private void verPociones(ServerPlayer p) {
        if (!(p.containerMenu instanceof BrewingStandMenu m)) return;
        ItemStack[] v = new ItemStack[3];
        for (int i = 0; i < 3; i++) v[i] = m.getSlot(i).getItem().copy();
        pocionesVistas.put(p.getUUID(), v);
    }

    /** El stack es el mismo objeto que está en una ranura del menú abierto (resto que no se llegó a sacar). */
    private static boolean esItemDeRanura(ServerPlayer p, ItemStack st) {
        for (Slot s : p.containerMenu.slots) if (s.getItem() == st) return true;
        return false;
    }
}
