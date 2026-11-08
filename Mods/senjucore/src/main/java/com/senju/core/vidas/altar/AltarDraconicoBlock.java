package com.senju.core.vidas.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/** Altar Dracónico: clic derecho con un papel renombrado inicia el ritual de resurrección. */
public class AltarDraconicoBlock extends Block {
    public AltarDraconicoBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(50.0F, 1200.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .pushReaction(PushReaction.BLOCK)
                .lightLevel(s -> 10));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack mano = player.getItemInHand(hand);
        // Solo con la mano vacia o con papel se hace el ritual; con cualquier otra cosa se comporta como un bloque normal.
        if (!mano.isEmpty() && !mano.is(Items.PAPER)) return InteractionResult.PASS;
        // Mano principal vacia con algo en la otra mano: se deja usar la otra mano (colocar bloques).
        if (mano.isEmpty() && !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (mano.isEmpty() && player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp) GuiaAltar.activar(sp, pos);
            return InteractionResult.CONSUME;
        }
        if (mano.isEmpty()) {
            // Solo una pista, sin comprobar el resto.
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("senjucore.ritual.pergamino"));
            return InteractionResult.CONSUME;
        }
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) Ritual.intentar(sl, pos, sp, mano);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        if (r.nextInt(3) == 0)
            level.addParticle(ParticleTypes.FLAME, cx + (r.nextDouble() - 0.5) * 0.7, pos.getY() + 1.05,
                    cz + (r.nextDouble() - 0.5) * 0.7, 0, 0.01, 0);
        if (r.nextInt(6) == 0)
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, cx + (r.nextDouble() - 0.5) * 0.9,
                    pos.getY() + 1.0, cz + (r.nextDouble() - 0.5) * 0.9, 0, 0.02, 0);
    }
}
