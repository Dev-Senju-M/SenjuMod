package com.senju.core.network;

import java.util.Optional;
import com.senju.core.SenjuCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Red {
    public static final String PROTOCOLO = "1";
    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SenjuCore.MODID, "main"),
            () -> PROTOCOLO, PROTOCOLO::equals, PROTOCOLO::equals);

    private Red() {}

    public static void registrar() {
        int id = 0;
        CANAL.registerMessage(id++, PaqueteInfoServidor.class, PaqueteInfoServidor::encode,
                PaqueteInfoServidor::decode, PaqueteInfoServidor::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CANAL.registerMessage(id++, PaqueteTiempo.class, PaqueteTiempo::encode,
                PaqueteTiempo::decode, PaqueteTiempo::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CANAL.registerMessage(id++, PaqueteVanish.class, PaqueteVanish::encode,
                PaqueteVanish::decode, PaqueteVanish::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CANAL.registerMessage(id++, PaqueteAviso.class, PaqueteAviso::encode,
                PaqueteAviso::decode, PaqueteAviso::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CANAL.registerMessage(id++, PaqueteVidas.class, PaqueteVidas::encode,
                PaqueteVidas::decode, PaqueteVidas::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void enviarA(ServerPlayer p, Object paquete) {
        CANAL.send(PacketDistributor.PLAYER.with(() -> p), paquete);
    }
}
