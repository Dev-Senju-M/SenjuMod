package com.senju.core.vidas.altar;

import com.mojang.authlib.GameProfile;
import com.senju.core.config.CommonConfig;
import com.senju.core.staff.castigos.RegistroCastigos.Posicion;
import com.senju.core.vidas.RegistroVidas;
import com.senju.core.vidas.Vidas;
import com.senju.core.vidas.altar.ReglaRitual.Condiciones;
import com.senju.core.vidas.altar.ReglaRitual.Falta;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Logica del ritual de resurreccion en el Altar Draconico. */
public final class Ritual {
    private Ritual() {}

    public static final double RADIO = 8.0;

    /** Tipo logico de un bloque para {@link EstructuraAltar}. Solo la obsidiana normal cuenta (no la llorona). */
    static String tipoLogico(BlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.OBSIDIAN) return EstructuraAltar.OBSIDIANA;
        if (b == Blocks.SOUL_FIRE || b == Blocks.SOUL_TORCH) return EstructuraAltar.FUEGO_ALMAS;
        if (b == Blocks.SOUL_CAMPFIRE && s.getValue(CampfireBlock.LIT)) return EstructuraAltar.FUEGO_ALMAS;
        if (s.isAir()) return "aire";
        return "otro";
    }

    /** {@code mano}: la mano principal del activador (vacia o con papel). */
    public static void intentar(ServerLevel level, BlockPos altar, ServerPlayer activador, ItemStack mano) {
        var server = level.getServer();
        String nombre = null;
        if (mano.is(Items.PAPER) && mano.hasCustomHoverName()) nombre = mano.getHoverName().getString().trim();

        List<String> pilares = EstructuraAltar.faltantes(
                d -> tipoLogico(level.getBlockState(altar.offset(d[0], d[1], d[2]))));

        // Fuera del Overworld nunca es "de noche" para el ritual.
        boolean noche = level.dimension() == Level.OVERWORLD && level.isNight();
        Vec3 centro = Vec3.atCenterOf(altar);
        int cerca = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(centro) <= RADIO * RADIO) cerca++;
        }
        int necesarios = CommonConfig.JUGADORES_RITUAL.get();

        Optional<GameProfile> objetivo = nombre == null || nombre.isEmpty()
                ? Optional.empty() : RegistroVidas.de(server).buscarPorNombre(nombre);
        boolean eliminado = objetivo.isPresent() && Vidas.eliminado(server, objetivo.get());

        Condiciones c = new Condiciones(pilares.isEmpty(), noche, CommonConfig.RITUAL_DE_NOCHE.get(), cerca,
                necesarios, buscarTotem(activador) != null, nombre, eliminado);
        List<Falta> faltas = ReglaRitual.verificar(c);
        if (!faltas.isEmpty()) {
            for (Falta f : faltas) {
                Component msg = switch (f) {
                    case JUGADORES -> Component.translatable(clave(f), cerca, necesarios);
                    case NO_ELIMINADO -> Component.translatable(clave(f), nombre);
                    default -> Component.translatable(clave(f));
                };
                activador.sendSystemMessage(msg);
            }
            return;
        }

        // Todo en orden: se consume y se resucita (Vidas.revivir difunde el aviso RESUCITADO).
        ItemStack totem = buscarTotem(activador);
        mano.shrink(1);
        if (totem != null) totem.shrink(1);

        GameProfile perfil = objetivo.get();
        efectos(level, altar);
        Posicion sobre = new Posicion(level.dimension().location().toString(), altar.getX() + 0.5,
                altar.getY() + 1.0, altar.getZ() + 0.5, activador.getYRot(), 0F);
        Vidas.revivir(server, perfil, CommonConfig.VIDAS_AL_REVIVIR.get(), sobre);
        Component ok = Component.translatable("senjucore.ritual.completado", perfil.getName());
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(centro) <= RADIO * RADIO) p.sendSystemMessage(ok);
        }
    }

    private static String clave(Falta f) {
        return "senjucore.ritual." + switch (f) {
            case ESTRUCTURA -> "estructura";
            case NOCHE -> "noche";
            case JUGADORES -> "jugadores";
            case TOTEM -> "totem";
            case PERGAMINO -> "pergamino";
            case NO_ELIMINADO -> "noeliminado";
        };
    }

    /** Primer totem en el inventario (principal u offhand), o null. */
    private static ItemStack buscarTotem(ServerPlayer p) {
        var inv = p.getInventory();
        List<ItemStack> todos = new ArrayList<>(inv.items);
        todos.addAll(inv.offhand);
        for (ItemStack s : todos) if (s.is(Items.TOTEM_OF_UNDYING)) return s;
        return null;
    }

    private static void efectos(ServerLevel level, BlockPos altar) {
        LightningBolt rayo = EntityType.LIGHTNING_BOLT.create(level);
        if (rayo != null) {
            rayo.moveTo(Vec3.atBottomCenterOf(altar.above()));
            rayo.setVisualOnly(true);
            level.addFreshEntity(rayo);
        }
        double x = altar.getX() + 0.5, y = altar.getY() + 1.0, z = altar.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 80, 0.5, 0.6, 0.5, 0.08);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 60, 0.6, 0.8, 0.6, 0.06);
        level.playSound(null, altar, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
