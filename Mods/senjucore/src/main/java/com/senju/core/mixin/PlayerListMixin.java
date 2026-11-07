package com.senju.core.mixin;

import java.util.Collection;
import com.senju.core.staff.vanish.Vanish;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entrada de un jugador (placeNewPlayer): mensaje "se unió" y entradas de TAB filtrados según Vanish.
 * Se filtran los paquetes salientes (no se añade y retira), así que nadie ve un parpadeo.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    /** Jugador que está entrando en la llamada actual a placeNewPlayer (hilo del servidor). */
    @Unique
    private ServerPlayer senjucore$entrando;

    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void senjucore$inicio(Connection conexion, ServerPlayer jugador, CallbackInfo ci) {
        senjucore$entrando = jugador;
    }

    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("RETURN"))
    private void senjucore$fin(Connection conexion, ServerPlayer jugador, CallbackInfo ci) {
        senjucore$entrando = null;
    }

    /** "X se unió a la partida": si X está en Vanish, solo al staff (y a la consola). */
    @Redirect(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void senjucore$mensajeEntrada(PlayerList lista, Component mensaje, boolean overlay) {
        Vanish.difundirMensaje(lista, mensaje, overlay, senjucore$entrando);
    }

    /** TAB inicial del que entra: sin los jugadores en Vanish que no puede ver. */
    @Redirect(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;createPlayerInitializing(Ljava/util/Collection;)Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;"))
    private ClientboundPlayerInfoUpdatePacket senjucore$tabInicial(Collection<ServerPlayer> jugadores) {
        return ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Vanish.visiblesPara(senjucore$entrando, jugadores));
    }

    /** Entrada de TAB del que entra: si está en Vanish, solo al staff y a él mismo. */
    @Redirect(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void senjucore$tabDelQueEntra(PlayerList lista, Packet<?> paquete) {
        Vanish.difundirA(lista, paquete, senjucore$entrando);
    }
}
