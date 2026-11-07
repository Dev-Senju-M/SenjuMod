package com.senju.core.staff.vanish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.senju.core.network.PaqueteVanish;
import com.senju.core.network.Red;
import com.senju.core.staff.Staff;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Estado y acciones de Vanish. */
public class Vanish {
    private int ticks = 0;
    /** Último estado de staff conocido de cada jugador conectado, para detectar quién gana o pierde staff. */
    private final Map<UUID, Boolean> staffConocido = new HashMap<>();

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
        if (esta(p) == activo) return;
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

    /** ¿Puede {@code espectador} ver a {@code objetivo} (entidad, TAB, mensajes de entrada/salida)? */
    public static boolean puedeVer(ServerPlayer objetivo, ServerPlayer espectador) {
        boolean mismo = objetivo == espectador;
        if (mismo) return true;
        boolean oculto = esta(objetivo);
        if (!oculto) return true; // camino rápido: no consulta staff si no está en Vanish
        return ReglaVanish.puedeVer(true, Staff.esStaff(espectador), false);
    }

    /** Los jugadores de {@code jugadores} que {@code espectador} puede ver. */
    public static List<ServerPlayer> visiblesPara(ServerPlayer espectador, Collection<ServerPlayer> jugadores) {
        List<ServerPlayer> visibles = new ArrayList<>(jugadores.size());
        for (ServerPlayer j : jugadores) if (puedeVer(j, espectador)) visibles.add(j);
        return visibles;
    }

    /** Envía {@code paquete} a cada conectado que puede ver a {@code sujeto}. */
    public static void difundirA(PlayerList lista, Packet<?> paquete, ServerPlayer sujeto) {
        for (ServerPlayer p : lista.getPlayers()) if (puedeVer(sujeto, p)) p.connection.send(paquete);
    }

    /**
     * Mensaje de sistema sobre {@code sujeto} (entrada/salida): normal si no está en Vanish;
     * si lo está, solo a la consola y a quien puede verlo (staff).
     */
    public static void difundirMensaje(PlayerList lista, Component mensaje, boolean overlay, ServerPlayer sujeto) {
        if (sujeto == null || !esta(sujeto)) {
            lista.broadcastSystemMessage(mensaje, overlay);
            return;
        }
        lista.getServer().sendSystemMessage(mensaje);
        for (ServerPlayer p : lista.getPlayers()) if (puedeVer(sujeto, p)) p.sendSystemMessage(mensaje, overlay);
    }

    /** Como {@link PlayerList#broadcastSystemToTeam}, pero si {@code sujeto} está en Vanish solo a quien puede verlo. */
    public static void difundirAEquipo(PlayerList lista, ServerPlayer sujeto, Component mensaje) {
        if (!esta(sujeto)) {
            lista.broadcastSystemToTeam(sujeto, mensaje);
            return;
        }
        Team equipo = sujeto.getTeam();
        if (equipo == null) return;
        for (String nombre : equipo.getPlayers()) {
            ServerPlayer p = lista.getPlayerByName(nombre);
            if (p != null && p != sujeto && puedeVer(sujeto, p)) p.sendSystemMessage(mensaje);
        }
    }

    /** Como {@link PlayerList#broadcastSystemToAllExceptTeam}, filtrado por Vanish. */
    public static void difundirFueraDeEquipo(PlayerList lista, ServerPlayer sujeto, Component mensaje) {
        if (!esta(sujeto)) {
            lista.broadcastSystemToAllExceptTeam(sujeto, mensaje);
            return;
        }
        Team equipo = sujeto.getTeam();
        if (equipo == null) {
            difundirMensaje(lista, mensaje, false, sujeto);
            return;
        }
        for (ServerPlayer p : lista.getPlayers()) {
            if (p.getTeam() != equipo && puedeVer(sujeto, p)) p.sendSystemMessage(mensaje);
        }
    }

    /**
     * Tras un cambio de estado de Vanish de {@code p}: TAB y entidad para los no staff.
     * Orden: al hacerse visible, la entrada de TAB va antes que la entidad (el cliente ignora un jugador
     * sin entrada de TAB); al ocultarse, la entidad se retira antes que la entrada de TAB.
     */
    public static void refrescar(ServerPlayer p) {
        boolean oculto = esta(p);
        List<ServerPlayer> otros = new ArrayList<>(p.server.getPlayerList().getPlayers());
        otros.remove(p);
        if (!oculto) {
            // A todos los demás: el staff ya tiene la entrada y el cliente ignora un alta repetida (putIfAbsent);
            // así también la recibe quien ganó staff hace menos de 1 s y aún no fue sincronizado.
            Packet<?> alta = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(p));
            for (ServerPlayer otro : otros) otro.connection.send(alta);
        }
        refrescarEntidad(p);
        if (oculto) {
            Packet<?> baja = new ClientboundPlayerInfoRemovePacket(List.of(p.getUUID()));
            for (ServerPlayer otro : otros) if (!Staff.esStaff(otro)) otro.connection.send(baja);
        }
    }

    /** Reevalúa quién rastrea la entidad de {@code p} (llama a broadcastToPlayer para cada jugador). */
    private static void refrescarEntidad(ServerPlayer p) {
        p.serverLevel().getChunkSource().move(p);
    }

    @SubscribeEvent
    public void alEntrar(PlayerEvent.PlayerLoggedInEvent ev) {
        if (ev.getEntity() instanceof ServerPlayer p) {
            Red.enviarA(p, new PaqueteVanish(esta(p)));
            staffConocido.put(p.getUUID(), Staff.esStaff(p));
        }
    }

    @SubscribeEvent
    public void alTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (++ticks < 20) return;
        ticks = 0;
        List<ServerPlayer> jugadores = List.copyOf(ev.getServer().getPlayerList().getPlayers());
        for (ServerPlayer p : jugadores) {
            if (esta(p) && !Staff.esStaff(p)) poner(p, false);
        }
        sincronizarEspectadores(jugadores);
    }

    /**
     * Cubre al espectador que gana o pierde staff mientras otro está en Vanish (≤ 1 s):
     * TAB de los ocultos para quien cambió y reevaluación de la entidad de cada oculto.
     */
    private void sincronizarEspectadores(List<ServerPlayer> jugadores) {
        List<ServerPlayer> ocultos = new ArrayList<>();
        List<ServerPlayer> ganan = new ArrayList<>();
        List<ServerPlayer> pierden = new ArrayList<>();
        Map<UUID, Boolean> ahora = new HashMap<>();
        for (ServerPlayer p : jugadores) {
            if (esta(p)) ocultos.add(p);
            boolean staff = Staff.esStaff(p);
            ahora.put(p.getUUID(), staff);
            Boolean antes = staffConocido.get(p.getUUID());
            if (antes != null && antes != staff) (staff ? ganan : pierden).add(p);
        }
        staffConocido.clear();
        staffConocido.putAll(ahora);
        if (ocultos.isEmpty()) return;

        for (ServerPlayer espectador : ganan) {
            List<ServerPlayer> nuevos = new ArrayList<>(ocultos);
            nuevos.remove(espectador);
            if (!nuevos.isEmpty()) espectador.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(nuevos));
        }
        for (ServerPlayer oculto : ocultos) refrescarEntidad(oculto);
        for (ServerPlayer espectador : pierden) {
            List<UUID> ids = new ArrayList<>();
            for (ServerPlayer oculto : ocultos) if (oculto != espectador) ids.add(oculto.getUUID());
            if (!ids.isEmpty()) espectador.connection.send(new ClientboundPlayerInfoRemovePacket(ids));
        }
    }
}
