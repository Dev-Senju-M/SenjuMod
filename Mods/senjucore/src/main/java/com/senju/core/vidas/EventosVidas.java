package com.senju.core.vidas;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Eventos de vidas: muerte, eliminación, reentrada y vigilancia del modo de juego. */
public class EventosVidas {
    /**
     * Cuenta la muerte solo si nadie la canceló (el tótem de la inmortalidad evita que la muerte ocurra).
     * Prioridad mínima y sin recibir eventos cancelados.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void alMorir(LivingDeathEvent ev) {
        if (ev.isCanceled()) return;
        if (!(ev.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        if (p.level().isClientSide) return;
        Vidas.alMorir(p);
    }

    /** Reaparecer: NO tocar el modo de juego aquí; el siguiente tick lo hace (como en CongelarYCarcel). */
    @SubscribeEvent
    public void alReaparecer(PlayerEvent.PlayerRespawnEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        if (Vidas.eliminado(p.server, p.getGameProfile()) || pendiente(p)) Vidas.REVISAR.add(p.getUUID());
    }

    @SubscribeEvent
    public void alEntrar(PlayerEvent.PlayerLoggedInEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        RegistroVidas reg = RegistroVidas.de(p.server);
        RegistroVidas.Datos d = reg.datos(p.getUUID());
        if (!d.nombre.equals(p.getGameProfile().getName())) {
            d.nombre = p.getGameProfile().getName();
            reg.setDirty();
        }
        // Otro handler ya lo desconectó en este login (ban, límite diario): no aplicar nada; sigue pendiente.
        // (Si otro handler expulsa DESPUÉS de este, el revivir ya se aplicó: aceptado, no depende del orden.)
        if (com.senju.core.ExpulsadosAlEntrar.esta(p.getUUID()) || p.hasDisconnected()) return;
        // Entra muerto (salió en la pantalla de muerte): el revivir espera a su primer tick vivo, sin rechazo.
        if (!p.isAlive() && pendiente(p)) {
            Vidas.REVISAR.add(p.getUUID());
            Vidas.enviarVidas(p);
            return;
        }
        // Primero el revivir pendiente, después el rechazo por eliminado.
        if (Vidas.aplicarPendiente(p)) return;
        if (Vidas.eliminado(p.server, p.getGameProfile())) {
            if (p.server.isSingleplayerOwner(p.getGameProfile())) {
                // El anfitrión de un mundo local no puede quedar fuera de su propio mundo: solo espectador.
                p.setGameMode(GameType.SPECTATOR);
            } else {
                com.senju.core.ExpulsadosAlEntrar.marcar(p.getUUID());
                p.connection.disconnect(Component.translatable("senjucore.vidas.rechazo"));
                return;
            }
        }
        Vidas.enviarVidas(p);
    }

    @SubscribeEvent
    public void alSalir(PlayerEvent.PlayerLoggedOutEvent ev) {
        Vidas.REVISAR.remove(ev.getEntity().getUUID());
        com.senju.core.ExpulsadosAlEntrar.quitar(ev.getEntity().getUUID());
    }

    @SubscribeEvent
    public void alDetener(ServerStoppedEvent ev) {
        Vidas.REVISAR.clear();
        com.senju.core.ExpulsadosAlEntrar.limpiar();
    }

    /**
     * Cada jugador vivo: si estaba en la lista de revisión (recién reaparecido / revivido estando muerto) se
     * atiende en el acto; además cada 20 ticks un eliminado que no esté en espectador (p. ej. /gamemode) vuelve a
     * espectador y un revivir pendiente se aplica.
     */
    @SubscribeEvent
    public void alTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || !ev.side.isServer()) return;
        if (!(ev.player instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        if (!p.isAlive()) return;
        boolean revisar = Vidas.REVISAR.remove(p.getUUID());
        if (!revisar && p.tickCount % 20 != 0) return;
        if (Vidas.aplicarPendiente(p)) return;
        if (Vidas.eliminado(p.server, p.getGameProfile())
                && p.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            p.setGameMode(GameType.SPECTATOR);
        }
    }

    private static boolean pendiente(ServerPlayer p) {
        RegistroVidas.Datos d = RegistroVidas.de(p.server).datosSiExisten(p.getUUID());
        return d != null && d.pendienteRevivir > 0;
    }
}
