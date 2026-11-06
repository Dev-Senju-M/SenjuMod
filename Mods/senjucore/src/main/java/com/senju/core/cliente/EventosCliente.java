package com.senju.core.cliente;

import com.senju.core.SenjuCore;
import com.senju.core.config.ClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Listeners solo de cliente (registrados bajo Dist.CLIENT). */
public final class EventosCliente {
    private EventosCliente() {}

    @Mod.EventBusSubscriber(modid = SenjuCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Mod_ {
        @SubscribeEvent
        public static void teclas(RegisterKeyMappingsEvent e) {
            e.register(Teclas.MOSTRAR);
            e.register(Teclas.POSICION);
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("panel", new PanelHud());
        }
    }

    @Mod.EventBusSubscriber(modid = SenjuCore.MODID, value = Dist.CLIENT)
    public static final class Forge_ {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (DatosCliente.tieneDatos()) DatosCliente.tick();
            boolean cambio = false;
            while (Teclas.MOSTRAR.consumeClick()) {
                ClientConfig.VISIBLE.set(!ClientConfig.VISIBLE.get());
                cambio = true;
            }
            while (Teclas.POSICION.consumeClick()) {
                ClientConfig.POSICION.set(ClientConfig.POSICION.get().siguiente());
                cambio = true;
            }
            if (cambio) ClientConfig.SPEC.save();
        }

        @SubscribeEvent
        public static void salir(ClientPlayerNetworkEvent.LoggingOut e) {
            DatosCliente.limpiar();
        }
    }
}
