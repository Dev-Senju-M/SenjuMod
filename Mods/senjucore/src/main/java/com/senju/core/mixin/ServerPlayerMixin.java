package com.senju.core.mixin;

import com.senju.core.staff.vanish.Vanish;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entidad: un jugador en Vanish no se envía (rastrea) a quien no puede verlo. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true)
    private void senjucore$ocultarVanish(ServerPlayer espectador, CallbackInfoReturnable<Boolean> cir) {
        if (!Vanish.puedeVer((ServerPlayer) (Object) this, espectador)) cir.setReturnValue(false);
    }
}
