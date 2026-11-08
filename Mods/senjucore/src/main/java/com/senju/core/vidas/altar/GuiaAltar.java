package com.senju.core.vidas.altar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.senju.core.config.CommonConfig;
import com.senju.core.network.PaqueteGuia;
import com.senju.core.network.Red;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Guía de construcción del altar (servidor): activa por jugador, se refresca cada 10 ticks. */
public final class GuiaAltar {
    static final int DURACION = 2400;
    static final int INTERVALO = 10;
    static final double DISTANCIA_MAX = 32.0;

    private record Guia(ResourceKey<Level> dim, BlockPos altar, int venceTick) {}

    private static final Map<UUID, Guia> GUIAS = new HashMap<>();

    private record Calculo(List<PaqueteGuia.Entrada> entradas, List<EstadoGuia.Esperado> esperados,
                           List<EstadoGuia.EstadoPos> estados) {
        boolean completa() {
            return EstadoGuia.pilaresCompletos(estados) == 4;
        }
    }

    private static Calculo calcular(ServerLevel level, BlockPos altar) {
        List<EstructuraAltar.Pos> pos = EstructuraAltar.posiciones();
        List<PaqueteGuia.Entrada> entradas = new ArrayList<>(pos.size());
        List<EstadoGuia.Esperado> esperados = new ArrayList<>(pos.size());
        List<EstadoGuia.EstadoPos> estados = new ArrayList<>(pos.size());
        for (EstructuraAltar.Pos p : pos) {
            String tipo = Ritual.tipoLogico(level.getBlockState(altar.offset(p.dx(), p.dy(), p.dz())));
            EstadoGuia.EstadoPos st = EstadoGuia.estado(p.esperado(), tipo);
            entradas.add(new PaqueteGuia.Entrada((byte) p.dx(), (byte) p.dy(), (byte) p.dz(), p.esperado(), st));
            esperados.add(p.esperado());
            estados.add(st);
        }
        return new Calculo(entradas, esperados, estados);
    }

    private static void enviarFin(ServerPlayer p, BlockPos altar) {
        Red.enviarA(p, new PaqueteGuia(altar, List.of(), false));
    }

    /** Activa (o reinicia) la guía del jugador para ese altar. */
    public static void activar(ServerPlayer jugador, BlockPos altar) {
        if (!(jugador.level() instanceof ServerLevel level)) return;
        Calculo c = calcular(level, altar);
        if (c.completa()) {
            if (GUIAS.remove(jugador.getUUID()) != null) enviarFin(jugador, altar);
            jugador.sendSystemMessage(listo());
            return;
        }
        int[] f = EstadoGuia.faltan(c.esperados(), c.estados());
        jugador.sendSystemMessage(Component.translatable("senjucore.guia.faltan", f[0], f[1]));
        GUIAS.put(jugador.getUUID(),
                new Guia(level.dimension(), altar.immutable(), jugador.server.getTickCount() + DURACION));
        enviar(jugador, altar, c);
    }

    private static Component listo() {
        return Component.translatable("senjucore.guia.listo", CommonConfig.JUGADORES_RITUAL.get());
    }

    private static void enviar(ServerPlayer p, BlockPos altar, Calculo c) {
        Red.enviarA(p, new PaqueteGuia(altar, c.entradas(), true));
        p.displayClientMessage(Component.translatable("senjucore.guia.progreso",
                EstadoGuia.pilaresCompletos(c.estados())), true);
    }

    @SubscribeEvent
    public void alColocar(BlockEvent.EntityPlaceEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        if (!ev.getPlacedBlock().is(BloquesMod.ALTAR_DRACONICO.get())) return;
        activar(p, ev.getPos());
    }

    @SubscribeEvent
    public void alTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || GUIAS.isEmpty()) return;
        MinecraftServer server = ev.getServer();
        int t = server.getTickCount();
        if (t % INTERVALO != 0) return;
        Iterator<Map.Entry<UUID, Guia>> it = GUIAS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Guia> en = it.next();
            Guia g = en.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) { // desconectado
                it.remove();
                continue;
            }
            BlockPos a = g.altar();
            if (t >= g.venceTick() || !(p.level() instanceof ServerLevel level) || level.dimension() != g.dim()
                    || p.distanceToSqr(a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5)
                            > DISTANCIA_MAX * DISTANCIA_MAX
                    || (level.isLoaded(a) && !level.getBlockState(a).is(BloquesMod.ALTAR_DRACONICO.get()))) {
                it.remove();
                enviarFin(p, a);
                continue;
            }
            if (!level.isLoaded(a)) continue;
            Calculo c = calcular(level, a);
            if (c.completa()) {
                it.remove();
                enviarFin(p, a);
                p.sendSystemMessage(listo());
            } else {
                enviar(p, a, c);
            }
        }
    }

    @SubscribeEvent
    public void alSalir(PlayerEvent.PlayerLoggedOutEvent ev) {
        GUIAS.remove(ev.getEntity().getUUID());
    }

    @SubscribeEvent
    public void alParar(ServerStoppingEvent ev) {
        GUIAS.clear();
    }
}
