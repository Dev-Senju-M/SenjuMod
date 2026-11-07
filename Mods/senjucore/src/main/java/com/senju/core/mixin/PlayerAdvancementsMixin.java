package com.senju.core.mixin;

import com.senju.core.staff.vanish.Vanish;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Anuncio de logro: si el jugador está en Vanish, solo lo recibe quien puede verlo (staff) y la consola. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Redirect(method = "award(Lnet/minecraft/advancements/Advancement;Ljava/lang/String;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void senjucore$anuncioLogro(PlayerList lista, Component mensaje, boolean overlay) {
        Vanish.difundirMensaje(lista, mensaje, overlay, player);
    }
}
