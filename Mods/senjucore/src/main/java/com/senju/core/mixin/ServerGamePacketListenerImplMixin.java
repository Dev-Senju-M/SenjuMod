package com.senju.core.mixin;

import com.senju.core.staff.vanish.Vanish;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mensaje "X abandonó la partida": si X está en Vanish, solo lo recibe el staff.
 * Chat firmado de un jugador en Vanish: a quien no puede verlo se le envía como chat "disfrazado"
 * (el cliente 1.20.1 se desconecta si recibe chat firmado de un emisor sin entrada de TAB).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Redirect(method = "onDisconnect(Lnet/minecraft/network/chat/Component;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void senjucore$mensajeSalida(PlayerList lista, Component mensaje, boolean overlay) {
        Vanish.difundirMensaje(lista, mensaje, overlay, ((ServerGamePacketListenerImpl) (Object) this).player);
    }

    /** Cubre chat, /msg, /me, /say y /teammsg (todos acaban aquí por destinatario). */
    @Inject(method = "sendPlayerChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"), cancellable = true)
    private void senjucore$chatVanish(PlayerChatMessage mensaje, ChatType.Bound tipo, CallbackInfo ci) {
        ServerGamePacketListenerImpl self = (ServerGamePacketListenerImpl) (Object) this;
        ServerPlayer destinatario = self.player;
        ServerPlayer emisor = destinatario.server.getPlayerList().getPlayer(mensaje.link().sender());
        if (emisor == null || Vanish.puedeVer(emisor, destinatario)) return;
        self.sendDisguisedChatMessage(mensaje.decoratedContent(), tipo);
        ci.cancel();
    }
}
