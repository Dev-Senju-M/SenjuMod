package com.senju.core.staff.vanish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Vanish sin rastro: los mobs no lo persiguen, no recoge items y abre contenedores en silencio. */
public class EventosVanish {
    private static final double RADIO_MOBS = 64.0;
    private int ticks = 0;

    // ---- Mobs ----

    @SubscribeEvent
    public void alCambiarObjetivo(LivingChangeTargetEvent e) {
        if (e.getNewTarget() instanceof ServerPlayer p && Vanish.esta(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public void alTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (++ticks < 20) return;
        ticks = 0;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (!Vanish.esta(p)) continue;
            AABB zona = p.getBoundingBox().inflate(RADIO_MOBS);
            for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, zona)) {
                if (m.getTarget() == p) m.setTarget(null);
                // Mobs con cerebro (Warden, Piglin, Hoglin...) guardan el objetivo en memoria.
                var cerebro = m.getBrain();
                // getMemory lanza si el tipo no esta registrado; hasMemoryValue devuelve false en ese caso.
                if (cerebro.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                        && cerebro.getMemory(MemoryModuleType.ATTACK_TARGET).filter(t -> t == p).isPresent()) {
                    cerebro.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                }
                if (cerebro.hasMemoryValue(MemoryModuleType.ANGRY_AT)
                        && cerebro.getMemory(MemoryModuleType.ANGRY_AT).filter(u -> u.equals(p.getUUID())).isPresent()) {
                    cerebro.eraseMemory(MemoryModuleType.ANGRY_AT);
                }
                if (m instanceof Warden w) w.clearAnger(p);
                if (m instanceof NeutralMob n && p.getUUID().equals(n.getPersistentAngerTarget())) n.stopBeingAngry();
            }
        }
    }

    // ---- Items ----

    @SubscribeEvent
    public void alRecoger(EntityItemPickupEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && Vanish.esta(p)) e.setCanceled(true);
    }

    // ---- Contenedores en silencio ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void alClicBloque(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (!Vanish.esta(p) || p.isSpectator()) return;
        // Misma precedencia que vanilla: agachado con algo en la mano -> usar el item sobre el bloque.
        if (p.isSecondaryUseActive() && (!p.getMainHandItem().isEmpty() || !p.getOffhandItem().isEmpty())) return;

        Level level = e.getLevel();
        BlockPos pos = e.getPos();
        BlockState estado = level.getBlockState(pos);
        boolean manejado;
        if (estado.getBlock() instanceof ChestBlock cofre) {
            manejado = abrirCofre(p, cofre, estado, level, pos);
        } else if (estado.getBlock() instanceof EnderChestBlock) {
            manejado = abrirEnder(p, level, pos);
        } else if (estado.getBlock() instanceof BarrelBlock) {
            manejado = abrirBarril(p, level, pos);
        } else if (estado.getBlock() instanceof ShulkerBoxBlock) {
            manejado = abrirShulker(p, estado, level, pos);
        } else {
            return;
        }
        if (manejado) {
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
        }
    }

    /** Devuelve true si el bloque es un contenedor gestionado (aunque vanilla se negara a abrirlo). */
    private static boolean abrirCofre(ServerPlayer p, ChestBlock cofre, BlockState estado, Level level, BlockPos pos) {
        List<ChestBlockEntity> partes = cofre.combine(estado, level, pos, false).apply(
                new DoubleBlockCombiner.Combiner<ChestBlockEntity, List<ChestBlockEntity>>() {
                    @Override
                    public List<ChestBlockEntity> acceptDouble(ChestBlockEntity a, ChestBlockEntity b) {
                        return List.of(a, b);
                    }

                    @Override
                    public List<ChestBlockEntity> acceptSingle(ChestBlockEntity a) {
                        return List.of(a);
                    }

                    @Override
                    public List<ChestBlockEntity> acceptNone() {
                        return List.of();
                    }
                });
        // Bloqueado (bloque solido encima / gato sentado): vanilla no abre nada.
        if (partes.isEmpty()) return true;
        Container real = ChestBlock.getContainer(cofre, estado, level, pos, false);
        if (real == null) return true;
        for (ChestBlockEntity c : partes) {
            if (!c.canOpen(p)) return true; // muestra el mensaje de candado
        }
        for (ChestBlockEntity c : partes) c.unpackLootTable(p);

        Component titulo;
        if (partes.size() == 2) {
            titulo = partes.get(0).hasCustomName() ? partes.get(0).getDisplayName()
                    : partes.get(1).hasCustomName() ? partes.get(1).getDisplayName()
                    : Component.translatable("container.chestDouble");
        } else {
            titulo = partes.get(0).getDisplayName();
        }
        Container proxy = new ContenedorSilencioso(real);
        boolean doble = partes.size() == 2;
        p.openMenu(new SimpleMenuProvider((id, inv, jug) ->
                doble ? ChestMenu.sixRows(id, inv, proxy) : ChestMenu.threeRows(id, inv, proxy), titulo));
        return true;
    }

    private static boolean abrirEnder(ServerPlayer p, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EnderChestBlockEntity ender)) return false;
        BlockPos arriba = pos.above();
        if (level.getBlockState(arriba).isRedstoneConductor(level, arriba)) return true; // bloqueado
        var inventario = p.getEnderChestInventory();
        // Como vanilla: stillValid del inventario depende del cofre activo (distancia / bloque roto).
        inventario.setActiveChest(ender);
        Container proxy = new ContenedorSilencioso(inventario, jug -> inventario.setActiveChest(null));
        p.openMenu(new SimpleMenuProvider((id, inv, jug) -> ChestMenu.threeRows(id, inv, proxy),
                Component.translatable("container.enderchest")));
        return true;
    }

    private static boolean abrirBarril(ServerPlayer p, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BarrelBlockEntity barril)) return false;
        if (!barril.canOpen(p)) return true;
        barril.unpackLootTable(p);
        Container proxy = new ContenedorSilencioso(barril);
        p.openMenu(new SimpleMenuProvider((id, inv, jug) -> ChestMenu.threeRows(id, inv, proxy),
                barril.getDisplayName()));
        return true;
    }

    private static boolean abrirShulker(ServerPlayer p, BlockState estado, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ShulkerBoxBlockEntity shulker)) return false;
        // Replica de ShulkerBoxBlock.canOpen (privado): si esta cerrada, nada debe obstruirla.
        if (shulker.getAnimationStatus() == ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
            AABB caja = Shulker.getProgressDeltaAabb(estado.getValue(ShulkerBoxBlock.FACING), 0.0F, 0.5F)
                    .move(pos).deflate(1.0E-6D);
            if (!level.noCollision(caja)) return true;
        }
        if (!shulker.canOpen(p)) return true;
        shulker.unpackLootTable(p);
        Container proxy = new ContenedorSilencioso(shulker);
        p.openMenu(new SimpleMenuProvider((id, inv, jug) -> new ShulkerBoxMenu(id, inv, proxy),
                shulker.getDisplayName()));
        return true;
    }

    /** Delega todo en el contenedor real salvo startOpen/stopOpen (sin sonido, animacion ni senal de redstone). */
    static final class ContenedorSilencioso implements Container {
        private final Container real;
        private final java.util.function.Consumer<Player> alCerrar;

        ContenedorSilencioso(Container real) {
            this(real, null);
        }

        ContenedorSilencioso(Container real, java.util.function.Consumer<Player> alCerrar) {
            this.real = real;
            this.alCerrar = alCerrar;
        }

        @Override public int getContainerSize() { return real.getContainerSize(); }
        @Override public boolean isEmpty() { return real.isEmpty(); }
        @Override public ItemStack getItem(int i) { return real.getItem(i); }
        @Override public ItemStack removeItem(int i, int n) { return real.removeItem(i, n); }
        @Override public ItemStack removeItemNoUpdate(int i) { return real.removeItemNoUpdate(i); }
        @Override public void setItem(int i, ItemStack s) { real.setItem(i, s); }
        @Override public int getMaxStackSize() { return real.getMaxStackSize(); }
        @Override public void setChanged() { real.setChanged(); }
        @Override public boolean stillValid(Player p) { return real.stillValid(p); }
        @Override public void startOpen(Player p) { }
        @Override public void stopOpen(Player p) { if (alCerrar != null) alCerrar.accept(p); }
        @Override public boolean canPlaceItem(int i, ItemStack s) { return real.canPlaceItem(i, s); }
        @Override public boolean canTakeItem(Container destino, int i, ItemStack s) { return real.canTakeItem(destino, i, s); }
        @Override public void clearContent() { real.clearContent(); }
    }
}
