package com.senju.core.staff.castigos;

import com.mojang.brigadier.context.CommandContextBuilder;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Efectos de silenciar, expulsar, banear y quitar horas, y vencimientos de castigos.
 * Registrar una instancia en el bus de Forge y llamar a {@link #registrarEfectos()} al iniciar el mod.
 */
public class EfectosCastigos {
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    /** Comandos de chat bloqueados a los silenciados (raíz del comando). */
    static final Set<String> COMANDOS_CHAT = Set.of("msg", "tell", "w", "me", "say", "teammsg", "tm");

    private int ticks = 0;

    public static void registrarEfectos() {
        Castigos.registrarEfecto(TipoCastigo.SILENCIO, new EfectoCastigo() {
            @Override
            public void alAplicar(MinecraftServer server, UUID id, @Nullable ServerPlayer online, Castigos.Aplicado datos) {
                SILENCIOS.put(id, new Silencio(datos.vence(), datos.motivo().isEmpty() ? "-" : datos.motivo()));
            }

            @Override
            public void alQuitar(MinecraftServer server, UUID id, @Nullable ServerPlayer online) {
                SILENCIOS.remove(id);
            }
        });
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

    /** Motivo del castigo activo: el de la última entrada APLICAR de ese tipo en el historial ("-" si ya no está). */
    static String motivoActivo(MinecraftServer server, UUID id, TipoCastigo t) {
        for (RegistroCastigos.Entrada e : Castigos.historial(server, id, RegistroCastigos.MAX_HISTORIAL)) {
            if (e.tipo() == t && e.accion().equals("APLICAR")) return e.motivo().isEmpty() ? "-" : e.motivo();
        }
        return "-";
    }

    // ---------------------------------------------------------------- copia segura de silencios

    /** Silencio activo (copia inmutable para leer desde otros hilos). */
    record Silencio(long vence, String motivo) {}

    /**
     * Copia de los silencios activos. El chat (ServerChatEvent) se publica desde un hilo de trabajo y NO puede tocar
     * RegistroCastigos. Solo se modifica en el hilo del servidor: alAplicar/alQuitar de SILENCIO, login,
     * ServerStarted y ServerStopped.
     */
    private static final Map<UUID, Silencio> SILENCIOS = new ConcurrentHashMap<>();

    /** Recalcula la entrada de un jugador desde el registro (hilo del servidor). */
    private static void reconstruirSilencio(MinecraftServer server, UUID id) {
        long vence = Castigos.vence(server, id, TipoCastigo.SILENCIO);
        if (vence == 0) SILENCIOS.remove(id);
        else SILENCIOS.put(id, new Silencio(vence, motivoActivo(server, id, TipoCastigo.SILENCIO)));
    }

    /** Silencio vigente (lectura segura desde cualquier hilo) o null. */
    @Nullable
    private static Silencio silencioVigente(UUID id) {
        Silencio s = SILENCIOS.get(id);
        return s != null && s.vence() > System.currentTimeMillis() ? s : null;
    }

    private static Component mensajeSilencio(Silencio s) {
        return Component.translatable("senjucore.castigo.silenciado", fechaHasta(s.vence()), s.motivo());
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
        // Hilo de trabajo: solo se lee la copia segura, nunca el registro.
        ServerPlayer p = ev.getPlayer();
        Silencio s = silencioVigente(p.getUUID());
        if (s == null) return;
        ev.setCanceled(true);
        p.server.execute(() -> p.sendSystemMessage(mensajeSilencio(s)));
    }

    @SubscribeEvent
    public void alComando(CommandEvent ev) {
        if (!(ev.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer p)) return;
        Silencio s = silencioVigente(p.getUUID());
        if (s == null) return;
        // Recorre la cadena de contextos (p. ej. /execute ... run msg) y mira la raíz de cada uno.
        CommandContextBuilder<CommandSourceStack> ctx = ev.getParseResults().getContext();
        boolean bloquear = false;
        while (ctx != null) {
            List<ParsedCommandNode<CommandSourceStack>> nodos = ctx.getNodes();
            if (!nodos.isEmpty()
                    && COMANDOS_CHAT.contains(nodos.get(0).getNode().getName().toLowerCase(Locale.ROOT))) {
                bloquear = true;
                break;
            }
            ctx = ctx.getChild();
        }
        if (!bloquear) return;
        ev.setCanceled(true);
        p.sendSystemMessage(mensajeSilencio(s));
    }

    /** Login: primero cierra vencidos, reconstruye el silencio, expulsa si el BAN sigue activo y luego congelar/cárcel. */
    @SubscribeEvent
    public void alEntrar(PlayerEvent.PlayerLoggedInEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        MinecraftServer server = p.server;
        Castigos.procesarVencidos(server, p.getUUID(), p);
        reconstruirSilencio(server, p.getUUID());
        if (Castigos.activo(server, p.getUUID(), TipoCastigo.BAN) && !Staff.esStaff(p)) {
            p.connection.disconnect(mensajeBan(Castigos.vence(server, p.getUUID(), TipoCastigo.BAN),
                    motivoActivo(server, p.getUUID(), TipoCastigo.BAN)));
            return;
        }
        // Congelar/cárcel SIEMPRE después de procesarVencidos: un castigo vencido nunca se refuerza.
        CongelarYCarcel.alEntrar(p);
    }

    @SubscribeEvent
    public void alIniciar(ServerStartedEvent ev) {
        SILENCIOS.clear();
        MinecraftServer server = ev.getServer();
        for (UUID id : new ArrayList<>(RegistroCastigos.de(server).todos().keySet())) reconstruirSilencio(server, id);
    }

    @SubscribeEvent
    public void alDetener(ServerStoppedEvent ev) {
        SILENCIOS.clear();
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
