package com.senju.core.mixin;

import com.senju.core.staff.vanish.Vanish;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entidad: un jugador en Vanish no se envía (rastrea) a quien no puede verlo.
 * Muerte: el mensaje de muerte de un jugador en Vanish solo lo recibe quien puede verlo (staff).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true)
    private void senjucore$ocultarVanish(ServerPlayer espectador, CallbackInfoReturnable<Boolean> cir) {
        if (!Vanish.puedeVer((ServerPlayer) (Object) this, espectador)) cir.setReturnValue(false);
    }

    /** Mensaje de muerte general (sin equipo o visibilidad ALWAYS). */
    @Redirect(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void senjucore$muerte(PlayerList lista, Component mensaje, boolean overlay) {
        Vanish.difundirMensaje(lista, mensaje, overlay, (ServerPlayer) (Object) this);
    }

    /** Mensaje de muerte con visibilidad HIDE_FOR_OTHER_TEAMS (solo a su equipo). */
    @Redirect(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
    private void senjucore$muerteEquipo(PlayerList lista, Player jugador, Component mensaje) {
        Vanish.difundirAEquipo(lista, (ServerPlayer) (Object) this, mensaje);
    }

    /** Mensaje de muerte con visibilidad HIDE_FOR_OWN_TEAM (a todos menos a su equipo). */
    @Redirect(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToAllExceptTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
    private void senjucore$muerteFueraEquipo(PlayerList lista, Player jugador, Component mensaje) {
        Vanish.difundirFueraDeEquipo(lista, (ServerPlayer) (Object) this, mensaje);
    }
}
