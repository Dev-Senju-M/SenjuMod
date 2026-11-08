package com.senju.core.misiones;

import com.mojang.authlib.GameProfile;
import com.senju.core.misiones.RegistroMisiones.DatosJugador;
import com.senju.core.tiempo.ControlTiempo;
import com.senju.core.vidas.Vidas;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Servicio de misiones del servidor (crear, editar, asignar, consultar). */
public final class Misiones {
    /** Jugadores que necesitan revisión y reenvío del HUD en el próximo tick. */
    private static final Set<UUID> pendientes = new HashSet<>();

    private Misiones() {}

    /**
     * Misiones activas del jugador: la global de hoy (si existe, no la completó hoy y no está eliminado) y luego las
     * personales asignadas, por antigüedad. Crea su progreso la primera vez que una le queda activa.
     */
    public static List<Mision> activas(ServerPlayer p) {
        MinecraftServer s = p.server;
        RegistroMisiones r = RegistroMisiones.de(s);
        UUID uuid = p.getUUID();
        DatosJugador d = r.datos(uuid);
        List<Mision> out = new ArrayList<>();
        Mision g = r.globalHoy >= 0 ? r.misiones.get(r.globalHoy) : null;
        if (g != null && d.globalCompletadaDia != ControlTiempo.hoy() && !Vidas.eliminado(s, p.getGameProfile())) {
            asegurarProgreso(r, d, g.id);
            out.add(g);
        }
        List<Mision> personales = new ArrayList<>();
        for (Mision m : r.misiones.values()) {
            if (m.tipo != TipoMision.PERSONAL || !m.asignados.contains(uuid)) continue;
            asegurarProgreso(r, d, m.id);
            personales.add(m);
        }
        personales.sort(Comparator.comparingLong((Mision m) -> d.desde.getOrDefault(m.id, 0L)).thenComparingInt(m -> m.id));
        out.addAll(personales);
        return out;
    }

    /**
     * Guarda el borrador. Nueva: id nuevo; global → al final de la cola; personal → se asigna y avisa. Edición:
     * reemplaza los campos salvo tipo e id sin tocar progresos; los nuevos asignados empiezan de cero y los quitados
     * pierden su progreso de esa misión.
     *
     * @return la misión guardada, o null si se editaba una misión que ya no existe.
     * @throws IllegalArgumentException si el borrador no es válido.
     */
    @Nullable
    public static Mision guardar(MinecraftServer s, Borrador b) {
        LinkedHashMap<String, Integer> objetivos = new LinkedHashMap<>(b.objetivos);
        Mision.sanear(objetivos);
        if (!b.valido() || objetivos.isEmpty() || Mision.recortarNombre(b.nombre).isEmpty()) {
            throw new IllegalArgumentException("Borrador de misión no válido");
        }
        RegistroMisiones r = RegistroMisiones.de(s);
        Mision m;
        Set<UUID> nuevos;
        if (b.idEditando < 0) {
            m = new Mision();
            m.id = r.nuevoId();
            m.tipo = b.tipo;
            copiarCampos(b, m);
            r.misiones.put(m.id, m);
            if (m.tipo == TipoMision.GLOBAL) r.cola.agregar(m.id);
            nuevos = new LinkedHashSet<>(m.asignados);
        } else {
            m = r.misiones.get(b.idEditando);
            if (m == null) return null;
            Set<UUID> antes = new LinkedHashSet<>(m.asignados);
            copiarCampos(b, m);
            nuevos = new LinkedHashSet<>(m.asignados);
            nuevos.removeAll(antes);
            antes.removeAll(m.asignados);
            for (UUID quitado : antes) quitarProgreso(r, quitado, m.id);
        }
        for (UUID u : nuevos) crearProgreso(r, u, m.id);
        r.setDirty();
        avisarNueva(s, m, nuevos);
        marcarConectados(s);
        return m;
    }

    /** Elimina la misión de todas partes (cola, global de hoy y progresos). */
    public static void eliminar(MinecraftServer s, int id) {
        RegistroMisiones r = RegistroMisiones.de(s);
        r.misiones.remove(id);
        r.cola.quitar(id);
        if (r.globalHoy == id) r.globalHoy = -1;
        for (DatosJugador d : r.jugadores.values()) {
            d.progreso.remove(id);
            d.desde.remove(id);
        }
        r.setDirty();
        marcarConectados(s);
    }

    /** Asigna una misión personal a un jugador (aunque esté desconectado). false si no aplica o ya la tenía. */
    public static boolean asignar(MinecraftServer s, int id, GameProfile p) {
        RegistroMisiones r = RegistroMisiones.de(s);
        Mision m = r.misiones.get(id);
        if (m == null || m.tipo != TipoMision.PERSONAL || p.getId() == null) return false;
        if (!m.asignados.add(p.getId())) return false;
        m.nombresAsignados.put(p.getId(), p.getName() == null ? "" : p.getName());
        crearProgreso(r, p.getId(), id);
        r.setDirty();
        avisarNueva(s, m, Set.of(p.getId()));
        return true;
    }

    /** Cantidad por id de ítem en inventario principal, armadura y mano secundaria (solo los ids pedidos). */
    public static Map<String, Integer> contarInventario(ServerPlayer p, Set<String> ids) {
        Map<String, Integer> r = new HashMap<>();
        if (ids.isEmpty()) return r;
        Inventory inv = p.getInventory();
        for (NonNullList<ItemStack> lista : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack st : lista) {
                if (st.isEmpty()) continue;
                String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
                if (ids.contains(id)) r.merge(id, st.getCount(), Integer::sum);
            }
        }
        return r;
    }

    /** Pide revisión y reenvío del HUD de este jugador en el próximo tick. */
    public static void marcarCambio(ServerPlayer p) {
        synchronized (pendientes) {
            pendientes.add(p.getUUID());
        }
    }

    /** Devuelve y vacía los jugadores pendientes de revisión. */
    public static Set<UUID> consumirPendientes() {
        synchronized (pendientes) {
            Set<UUID> copia = new HashSet<>(pendientes);
            pendientes.clear();
            return copia;
        }
    }

    // ---------------------------------------------------------------- internos

    private static void copiarCampos(Borrador b, Mision m) {
        m.nombre = Mision.recortarNombre(b.nombre);
        m.objetivos = new LinkedHashMap<>(b.objetivos);
        Mision.sanear(m.objetivos);
        m.recompensa = new ArrayList<>();
        for (ItemStack st : b.recompensa) {
            if (st.isEmpty() || m.recompensa.size() >= Mision.MAX_RECOMPENSA) continue;
            ItemStack c = st.copy();
            c.setCount(Math.max(1, Math.min(c.getCount(), c.getMaxStackSize())));
            m.recompensa.add(c);
        }
        m.castigo = b.castigo;
        m.castigoMs = Math.max(0, b.castigoMs);
        if (m.tipo == TipoMision.PERSONAL) {
            m.asignados = new LinkedHashSet<>(b.asignados);
            m.nombresAsignados = new HashMap<>();
            for (UUID u : m.asignados) m.nombresAsignados.put(u, b.nombresAsignados.getOrDefault(u, ""));
        } else {
            m.asignados = new LinkedHashSet<>();
            m.nombresAsignados = new HashMap<>();
        }
    }

    private static void asegurarProgreso(RegistroMisiones r, DatosJugador d, int id) {
        if (d.progreso.containsKey(id)) return;
        d.progreso.put(id, new HashMap<>());
        d.desde.put(id, System.currentTimeMillis());
        r.setDirty();
    }

    /** Progreso vacío desde ahora (reemplaza uno anterior si lo hubiera). */
    private static void crearProgreso(RegistroMisiones r, UUID u, int id) {
        DatosJugador d = r.datos(u);
        d.progreso.put(id, new HashMap<>());
        d.desde.put(id, System.currentTimeMillis());
        r.setDirty();
    }

    private static void quitarProgreso(RegistroMisiones r, UUID u, int id) {
        DatosJugador d = r.datosSiExisten(u);
        if (d == null) return;
        d.progreso.remove(id);
        d.desde.remove(id);
        r.setDirty();
    }

    private static void avisarNueva(MinecraftServer s, Mision m, Set<UUID> a) {
        if (m.tipo != TipoMision.PERSONAL) return;
        for (UUID u : a) {
            ServerPlayer p = s.getPlayerList().getPlayer(u);
            if (p == null) continue;
            p.sendSystemMessage(Component.translatable("senjucore.mision.nueva_personal", m.nombre));
            marcarCambio(p);
        }
    }

    private static void marcarConectados(MinecraftServer s) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) marcarCambio(p);
    }
}
