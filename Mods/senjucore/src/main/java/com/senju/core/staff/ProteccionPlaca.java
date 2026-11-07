package com.senju.core.staff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Mantiene la Placa de Staff con su dueño (todo en el servidor).
 *
 * Regla de devolución: hueco libre del inventario; si no, cursor; si el cursor también está
 * ocupado, la placa queda en una lista de pendientes en memoria y se reintenta cada tick hasta que
 * haya hueco o el cursor quede libre. Nunca se crea un ItemEntity.
 */
public class ProteccionPlaca {
    /** Placas apartadas de los drops al morir; se devuelven en PlayerEvent.Clone. */
    private final Map<UUID, List<ItemStack>> alMorir = new HashMap<>();
    /** Placas que no cupieron ni en inventario ni en cursor; se reintentan cada tick. */
    private final Map<UUID, List<ItemStack>> pendientes = new HashMap<>();

    /** Devuelve la placa al jugador sin soltarla jamás al suelo. */
    private void devolver(ServerPlayer p, ItemStack placa) {
        Inventory inv = p.getInventory();
        // No usar inv.add(): en creativo descarta el stack si no hay hueco.
        int libre = inv.getFreeSlot();
        if (libre >= 0) {
            inv.setItem(libre, placa);
            return;
        }
        AbstractContainerMenu menu = p.containerMenu;
        if (menu.getCarried().isEmpty()) {
            menu.setCarried(placa);
            return;
        }
        pendientes.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(placa);
    }

    private void reintentarPendientes(ServerPlayer p) {
        List<ItemStack> lista = pendientes.remove(p.getUUID());
        if (lista == null) return;
        for (ItemStack s : lista) devolver(p, s);
    }

    // --- Tirar (Q, arrastrar fuera, inventario creativo) ---
    @SubscribeEvent
    public void alTirar(ItemTossEvent ev) {
        ItemStack item = ev.getEntity().getItem();
        if (!PlacaStaffItem.esPlaca(item)) return;
        // Cancelar solo elimina la entidad; hay que devolver el ítem a mano.
        ev.setCanceled(true);
        if (ev.getPlayer() instanceof ServerPlayer p) devolver(p, item.copy());
    }

    // --- Muerte ---
    @SubscribeEvent
    public void alMorirDrops(LivingDropsEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        Iterator<ItemEntity> it = ev.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack s = it.next().getItem();
            if (PlacaStaffItem.esPlaca(s)) {
                alMorir.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(s.copy());
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void alClonar(PlayerEvent.Clone ev) {
        if (!ev.isWasDeath() || !(ev.getEntity() instanceof ServerPlayer p)) return;
        List<ItemStack> guardadas = alMorir.remove(p.getUUID());
        if (guardadas == null) return;
        for (ItemStack s : guardadas) devolver(p, s);
    }

    // --- Contenedores y no staff ---
    @SubscribeEvent
    public void alTickJugador(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || !(ev.player instanceof ServerPlayer p)) return;

        if (p.tickCount % 20 == 0 && !Staff.esStaff(p)) {
            quitarATodos(p);
            return;
        }
        sacarDeContenedores(p);
        reintentarPendientes(p);
    }

    /** Ningún slot que no sea del inventario del jugador (incluida la cuadrícula 2x2) puede tener la placa. */
    private void sacarDeContenedores(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (Slot slot : p.containerMenu.slots) {
            if (slot.container == inv) continue;
            ItemStack s = slot.getItem();
            if (PlacaStaffItem.esPlaca(s)) {
                ItemStack placa = s.copy();
                slot.set(ItemStack.EMPTY);
                devolver(p, placa);
            }
        }
    }

    private void quitarATodos(ServerPlayer p) {
        boolean quito = false;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) { // principal, armadura y mano secundaria
            if (PlacaStaffItem.esPlaca(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY);
                quito = true;
            }
        }
        AbstractContainerMenu menu = p.containerMenu;
        if (PlacaStaffItem.esPlaca(menu.getCarried())) {
            menu.setCarried(ItemStack.EMPTY);
            quito = true;
        }
        for (Slot slot : menu.slots) {
            if (slot.container != inv && PlacaStaffItem.esPlaca(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
                quito = true;
            }
        }
        List<ItemStack> pend = pendientes.remove(p.getUUID());
        if (pend != null && !pend.isEmpty()) quito = true;
        List<ItemStack> muerte = alMorir.remove(p.getUUID());
        if (muerte != null && !muerte.isEmpty()) quito = true;
        if (quito) p.sendSystemMessage(Component.translatable("senjucore.placa.sinpermiso"));
    }

    // --- Marcos y soportes ---
    @SubscribeEvent
    public void alInteractuar(PlayerInteractEvent.EntityInteract ev) {
        if (bloquea(ev.getItemStack(), ev.getTarget())) ev.setCanceled(true);
    }

    @SubscribeEvent
    public void alInteractuarEspecifico(PlayerInteractEvent.EntityInteractSpecific ev) {
        if (bloquea(ev.getItemStack(), ev.getTarget())) ev.setCanceled(true);
    }

    private static boolean bloquea(ItemStack mano, Entity objetivo) {
        return PlacaStaffItem.esPlaca(mano) && (objetivo instanceof ItemFrame || objetivo instanceof ArmorStand);
    }
}
