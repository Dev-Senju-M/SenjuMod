package com.senju.core.staff.castigos;

import com.mojang.brigadier.context.ParsedCommandNode;
import com.senju.core.config.CommonConfig;
import com.senju.core.staff.Staff;
import com.senju.core.tiempo.ControlTiempo;
import com.senju.core.tiempo.EstadoTiempo;
import com.senju.core.tiempo.RegistroTiempo;
import com.senju.core.tiempo.ReglaTiempo;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Efectos de silenciar, expulsar, banear y quitar horas, y vencimientos de castigos.
 * Registrar una instancia en el bus de Forge y llamar a {@link #registrarEfectos()} al iniciar el mod.
 */
public class EfectosCastigos {
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    /** Comandos de chat bloqueados a los silenciados (raíz, sin espacio de nombres). */
    static final Set<String> COMANDOS_CHAT = Set.of("msg", "tell", "w", "me", "say", "teammsg", "tm");

    private int ticks = 0;

    public static void registrarEfectos() {
        Castigos.registrarEfecto(TipoCastigo.SILENCIO, (server, id, online, datos) -> { });
        Castigos.registrarEfecto(TipoCastigo.EXPULSION, (server, id, online, datos) -> {
            if (online != null) {
                online.connection.disconnect(Component.translatable("senjucore.castigo.expulsado", datos.motivo()));
            }
        });
        Castigos.registrarEfecto(TipoCastigo.BAN, (server, id, online, datos) -> {
            if (online != null) online.connection.disconnect(mensajeBan(datos.vence(), datos.motivo()));
        });
        Castigos.registrarEfecto(TipoCastigo.QUITAR_HORAS, EfectosCastigos::quitarHoras);
    }

    // ---------------------------------------------------------------- textos

    static Component fechaHasta(long vence) {
        return vence == Duracion.PERMANENTE
                ? Component.translatable("senjucore.castigo.permanente")
                : Component.literal(FECHA.format(Instant.ofEpochMilli(vence)));
    }

    static Component mensajeBan(long vence, String motivo) {
        return Component.translatable("senjucore.castigo.baneado", fechaHasta(vence), motivo);
    }

    /** Motivo del castigo activo: el de la última entrada APLICAR de ese tipo en el historial ("" si ya no está). */
    static String motivoActivo(MinecraftServer server, UUID id, TipoCastigo t) {
        for (RegistroCastigos.Entrada e : Castigos.historial(server, id, RegistroCastigos.MAX_HISTORIAL)) {
            if (e.tipo() == t && e.accion().equals("APLICAR")) return e.motivo();
        }
        return "";
    }

    private static Component mensajeSilencio(MinecraftServer server, ServerPlayer p) {
        return Component.translatable("senjucore.castigo.silenciado",
                fechaHasta(Castigos.vence(server, p.getUUID(), TipoCastigo.SILENCIO)),
                motivoActivo(server, p.getUUID(), TipoCastigo.SILENCIO));
    }

    private static boolean silenciado(ServerPlayer p) {
        return Castigos.activo(p.server, p.getUUID(), TipoCastigo.SILENCIO) && !Staff.esStaff(p);
    }

    // ---------------------------------------------------------------- quitar horas

    private static void quitarHoras(MinecraftServer server, UUID id, @Nullable ServerPlayer online,
            Castigos.Aplicado datos) {
        long min = datos.duracionMs() == Duracion.PERMANENTE ? -1 : datos.duracionMs() / 60000L;
        long lim = ReglaTiempo.limiteSegundos(CommonConfig.HORAS_DIARIAS.get());
        RegistroTiempo reg = RegistroTiempo.de(server);
        EstadoTiempo e = ReglaTiempo.normalizar(reg.get(id), ControlTiempo.hoy());
        e = ReglaTiempo.quitar(e, min, lim);
        reg.set(id, e);
        if (online == null) return;
        if (ControlTiempo.aplicaLimite(online) && ReglaTiempo.agotado(e, lim)) {
            ControlTiempo.expulsar(online);
        } else {
            ControlTiempo.sincronizar(online);
        }
    }

    // ---------------------------------------------------------------- eventos

    @SubscribeEvent
    public void alChatear(ServerChatEvent ev) {
        ServerPlayer p = ev.getPlayer();
        if (!silenciado(p)) return;
        ev.setCanceled(true);
        p.sendSystemMessage(mensajeSilencio(p.server, p));
    }

    @SubscribeEvent
    public void alComando(CommandEvent ev) {
        CommandSourceStack src = ev.getParseResults().getContext().getSource();
        if (!(src.getEntity() instanceof ServerPlayer p)) return;
        List<ParsedCommandNode<CommandSourceStack>> nodos = ev.getParseResults().getContext().getNodes();
        if (nodos.isEmpty()) return;
        String raiz = nodos.get(0).getNode().getName();
        int i = raiz.indexOf(':');
        if (i >= 0) raiz = raiz.substring(i + 1);
        if (!COMANDOS_CHAT.contains(raiz.toLowerCase(Locale.ROOT))) return;
        if (!silenciado(p)) return;
        ev.setCanceled(true);
        p.sendSystemMessage(mensajeSilencio(p.server, p));
    }

    /** Login: primero cierra vencidos, luego expulsa si el BAN sigue activo. */
    @SubscribeEvent
    public void alEntrar(PlayerEvent.PlayerLoggedInEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        MinecraftServer server = p.server;
        Castigos.procesarVencidos(server, p.getUUID(), p);
        if (Castigos.activo(server, p.getUUID(), TipoCastigo.BAN) && !Staff.esStaff(p)) {
            p.connection.disconnect(mensajeBan(Castigos.vence(server, p.getUUID(), TipoCastigo.BAN),
                    motivoActivo(server, p.getUUID(), TipoCastigo.BAN)));
        }
    }

    /** Cada 20 ticks cierra los castigos vencidos de los jugadores conectados. */
    @SubscribeEvent
    public void alTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (++ticks < 20) return;
        ticks = 0;
        MinecraftServer server = ev.getServer();
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            Castigos.procesarVencidos(server, p.getUUID(), p);
        }
    }
}
