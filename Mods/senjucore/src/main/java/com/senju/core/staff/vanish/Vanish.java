package com.senju.core.staff.vanish;

import java.util.UUID;
import com.senju.core.network.PaqueteVanish;
import com.senju.core.network.Red;
import com.senju.core.staff.Staff;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Estado y acciones de Vanish. */
public class Vanish {
    private int ticks = 0;

    public static boolean esta(ServerPlayer p) {
        return esta(p.server, p.getUUID());
    }

    public static boolean esta(MinecraftServer server, UUID id) {
        return RegistroVanish.de(server).contiene(id);
    }

    public static void alternar(ServerPlayer p) {
        poner(p, !esta(p));
    }

    public static void poner(ServerPlayer p, boolean activo) {
        RegistroVanish.de(p.server).poner(p.getUUID(), activo);
        String estado = activo ? "on" : "off";
        p.sendSystemMessage(Component.translatable("senjucore.vanish." + estado));
        Component aviso = Component.translatable("senjucore.vanish.aviso." + estado, p.getGameProfile().getName());
        for (ServerPlayer otro : p.server.getPlayerList().getPlayers()) {
            if (otro != p && Staff.esStaff(otro)) otro.sendSystemMessage(aviso);
        }
        Red.enviarA(p, new PaqueteVanish(activo));
        refrescar(p);
    }

    /**
     * Gancho de visibilidad: tras un cambio de estado, hace que los no staff dejen de ver (o vuelvan a ver)
     * a este jugador (entidad y TAB). Lo rellena la Tarea 2 (Mixin); aqui no hace nada.
     */
    public static void refrescar(ServerPlayer p) {
    }

    @SubscribeEvent
    public void alEntrar(PlayerEvent.PlayerLoggedInEvent ev) {
        if (ev.getEntity() instanceof ServerPlayer p) Red.enviarA(p, new PaqueteVanish(esta(p)));
    }

    @SubscribeEvent
    public void alTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (++ticks < 20) return;
        ticks = 0;
        for (ServerPlayer p : ev.getServer().getPlayerList().getPlayers()) {
            if (esta(p) && !Staff.esStaff(p)) poner(p, false);
        }
    }
}
