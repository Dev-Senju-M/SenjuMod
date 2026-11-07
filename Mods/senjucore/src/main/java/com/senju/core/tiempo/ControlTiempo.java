package com.senju.core.tiempo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import com.senju.core.config.CommonConfig;
import com.senju.core.network.PaqueteInfoServidor;
import com.senju.core.network.PaqueteTiempo;
import com.senju.core.network.Red;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Suma el tiempo jugado y aplica el límite diario (solo el servidor decide el día). */
public class ControlTiempo {
    private int ticks = 0;
    private int segundos = 0;

    public static long hoy() {
        return LocalDate.now(ZoneId.systemDefault()).toEpochDay();
    }

    public static boolean aplicaLimite(ServerPlayer p) {
        MinecraftServer server = p.server;
        if (!server.isDedicatedServer()) return false;
        return !(CommonConfig.EXIMIR_OPERADORES.get() && server.getPlayerList().isOp(p.getGameProfile()));
    }

    private static long limite() {
        return ReglaTiempo.limiteSegundos(CommonConfig.HORAS_DIARIAS.get());
    }

    public static void sincronizar(ServerPlayer p) {
        EstadoTiempo e = RegistroTiempo.de(p.server).get(p.getUUID());
        Red.enviarA(p, new PaqueteTiempo(ReglaTiempo.restantes(e, limite()), e.usados()));
    }

    public static void expulsar(ServerPlayer p) {
        com.senju.core.ExpulsadosAlEntrar.marcar(p.getUUID());
        p.connection.disconnect(Component.translatable("senjucore.expulsado"));
        guardar(p.server);
    }

    static void guardar(MinecraftServer server) {
        server.overworld().getDataStorage().save();
    }

    @SubscribeEvent
    public void alSalir(PlayerEvent.PlayerLoggedOutEvent ev) {
        if (ev.getEntity() instanceof ServerPlayer p) guardar(p.server);
    }

    @SubscribeEvent
    public void alEntrar(PlayerEvent.PlayerLoggedInEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        RegistroTiempo reg = RegistroTiempo.de(p.server);
        EstadoTiempo e = ReglaTiempo.normalizar(reg.get(p.getUUID()), hoy());
        reg.set(p.getUUID(), e);
        boolean aplica = aplicaLimite(p);
        Red.enviarA(p, new PaqueteInfoServidor(CommonConfig.NOMBRE_SERVIDOR.get(), limite(), !aplica));
        if (aplica && ReglaTiempo.agotado(e, limite())) {
            expulsar(p);
        } else {
            sincronizar(p);
        }
    }

    @SubscribeEvent
    public void alTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (++ticks < 20) return;
        ticks = 0;
        segundos++;
        boolean sincTodos = segundos % 30 == 0;
        MinecraftServer server = ev.getServer();
        RegistroTiempo reg = RegistroTiempo.de(server);
        long hoy = hoy();
        long lim = limite();
        List<? extends Integer> avisos = CommonConfig.AVISOS_MINUTOS.get();
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            EstadoTiempo antes = reg.get(p.getUUID());
            EstadoTiempo norm = ReglaTiempo.normalizar(antes, hoy);
            boolean cambioDia = norm.dia() != antes.dia();
            EstadoTiempo despues = ReglaTiempo.sumar(norm, hoy, 1);
            reg.set(p.getUUID(), despues);
            boolean sinc = sincTodos || cambioDia;
            if (aplicaLimite(p)) {
                OptionalInt aviso = ReglaTiempo.avisoCruzado(
                        ReglaTiempo.restantes(norm, lim), ReglaTiempo.restantes(despues, lim), List.copyOf(avisos));
                if (aviso.isPresent()) {
                    p.sendSystemMessage(Component.translatable("senjucore.aviso", aviso.getAsInt()));
                    sinc = true;
                }
                if (ReglaTiempo.agotado(despues, lim)) {
                    expulsar(p);
                    continue;
                }
            }
            if (sinc) sincronizar(p);
        }
    }
}
