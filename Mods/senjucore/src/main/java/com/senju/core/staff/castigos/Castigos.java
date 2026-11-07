package com.senju.core.staff.castigos;

import com.mojang.authlib.GameProfile;
import com.senju.core.SenjuCore;
import com.senju.core.staff.ReglaStaff;
import com.senju.core.staff.Staff;
import com.senju.core.config.CommonConfig;
import com.senju.core.staff.castigos.RegistroCastigos.Datos;
import com.senju.core.staff.castigos.RegistroCastigos.Entrada;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Servicio de castigos: aplica, perdona, consulta y registra (estado, historial, log y avisos).
 * Todo se ejecuta en el hilo del servidor. Los efectos de mundo los aportan las tareas siguientes con {@link #registrarEfecto}.
 */
public final class Castigos {
    private Castigos() {}

    /** Resultado de {@link #aplicar}. Todo distinto de OK significa que no se aplicó ni se registró nada. */
    public enum Resultado { OK, ES_STAFF, SIN_CARCEL, VETADO, DESCONECTADO }

    /** Datos de una aplicación (se entregan a {@link EfectoCastigo#alAplicar}). */
    public record Aplicado(TipoCastigo tipo, long duracionMs, long vence, String motivo, String staff) {}

    private static final Map<TipoCastigo, EfectoCastigo> EFECTOS = new EnumMap<>(TipoCastigo.class);

    /** Registra el efecto de un tipo (se llama una vez al iniciar el mod; reemplaza el anterior). */
    public static void registrarEfecto(TipoCastigo tipo, EfectoCastigo efecto) {
        EFECTOS.put(tipo, efecto);
    }

    // ---------------------------------------------------------------- consulta

    public static boolean activo(MinecraftServer server, UUID id, TipoCastigo t) {
        Datos d = RegistroCastigos.de(server).datosSiExisten(id);
        return d != null && d.estado.activo(t, System.currentTimeMillis());
    }

    /** Vencimiento (ms de época; PERMANENTE) o 0 si no está activo. */
    public static long vence(MinecraftServer server, UUID id, TipoCastigo t) {
        Datos d = RegistroCastigos.de(server).datosSiExisten(id);
        long ahora = System.currentTimeMillis();
        return d != null && d.estado.activo(t, ahora) ? d.estado.vence(t) : 0;
    }

    /** Últimas {@code max} entradas, la más reciente primero. */
    public static List<Entrada> historial(MinecraftServer server, UUID id, int max) {
        Datos d = RegistroCastigos.de(server).datosSiExisten(id);
        return d == null ? new ArrayList<>() : HistorialLimitado.ultimos(d.historial, max);
    }

    /** Staff para perfiles conectados o no: anfitrión de un mundo local, en la lista {@code staff} u operador (según config). */
    public static boolean esStaff(MinecraftServer server, GameProfile perfil) {
        if (server.isSingleplayerOwner(perfil)) return true; // el anfitrión (un invitado LAN no puede castigarlo)
        ServerPlayer on = server.getPlayerList().getPlayer(perfil.getId());
        if (on != null) return Staff.esStaff(on);
        return ReglaStaff.esStaff(server.getPlayerList().isOp(perfil), perfil.getName(),
                CommonConfig.STAFF.get(), CommonConfig.STAFF_INCLUYE_OPERADORES.get());
    }

    // ---------------------------------------------------------------- aplicar

    /** Texto de duración para QUITAR_HORAS con PERMANENTE. */
    public static final String TODO_EL_DIA = "todo el día";

    /**
     * Aplica un castigo como un jugador staff. Ver la sobrecarga con servidor/actor para el contrato.
     */
    public static Resultado aplicar(ServerPlayer staff, GameProfile objetivo, TipoCastigo t, long duracionMs,
            String motivo) {
        return aplicar(staff.server, staff.getGameProfile().getName(), staff, objetivo, t, duracionMs, motivo);
    }

    /**
     * Aplica un castigo (también desde consola: {@code actorJugador} null). {@code duracionMs}: milisegundos o
     * {@link Duracion#PERMANENTE}; se ignora para EXPULSION; para QUITAR_HORAS es el tiempo a restar
     * (PERMANENTE = todo el día, texto "todo el día").
     * Orden: perfil válido -> staff -> cierra vencidos ({@link #procesarVencidos}) -> veto del efecto ->
     * estado/historial/log/avisos -> {@code alAplicar} (con excepciones aisladas).
     * Con ES_STAFF el mensaje {@code senjucore.castigo.esstaff} se envía a {@code actorJugador} si existe; si es
     * null (consola) lo debe mostrar quien llama. Un perfil sin UUID devuelve VETADO sin hacer nada.
     */
    public static Resultado aplicar(MinecraftServer server, String actorNombre, @Nullable ServerPlayer actorJugador,
            GameProfile objetivo, TipoCastigo t, long duracionMs, String motivo) {
        UUID id = objetivo == null ? null : objetivo.getId();
        if (id == null) return Resultado.VETADO;
        if (esStaff(server, objetivo)) {
            if (actorJugador != null) actorJugador.sendSystemMessage(Component.translatable("senjucore.castigo.esstaff"));
            return Resultado.ES_STAFF;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        procesarVencidos(server, id, online);
        EfectoCastigo ef = EFECTOS.get(t);
        if (ef != null) {
            Resultado v = ef.puedeAplicar(server, id, online, t);
            if (v != Resultado.OK) return v;
        }
        long ahora = System.currentTimeMillis();
        long vence = t.tieneDuracion() ? Duracion.vencimiento(ahora, duracionMs) : 0;
        String durTxt;
        if (t == TipoCastigo.EXPULSION) durTxt = "-";
        else if (t == TipoCastigo.QUITAR_HORAS && duracionMs == Duracion.PERMANENTE) durTxt = TODO_EL_DIA;
        else durTxt = Duracion.formatoDuracion(duracionMs);

        RegistroCastigos reg = RegistroCastigos.de(server);
        Datos d = reg.datos(id);
        String nombre = objetivo.getName() != null ? objetivo.getName() : (d.nombre.isEmpty() ? id.toString() : d.nombre);
        d.nombre = nombre;
        if (t.tieneDuracion()) d.estado.aplicar(t, vence);
        reg.agregarHistorial(id, new Entrada(ahora, actorNombre, "APLICAR", t, durTxt, motivo));
        reg.setDirty();
        RegistroLogCastigos.anotar(server, actorNombre, "APLICAR", nombre, id.toString(), t.clave(), durTxt, motivo);

        Component tipoTxt = Component.translatable("senjucore.castigo.tipo." + t.clave());
        if (online != null) {
            online.sendSystemMessage(Component.translatable("senjucore.castigo.jugador", tipoTxt, durTxt, motivo));
        }
        avisarStaff(server, Component.translatable("senjucore.castigo.staff", actorNombre, nombre, tipoTxt, durTxt, motivo));

        if (ef != null) {
            try {
                ef.alAplicar(server, id, online, new Aplicado(t, duracionMs, vence, motivo, actorNombre));
            } catch (RuntimeException ex) {
                SenjuCore.LOGGER.error("Fallo en el efecto alAplicar de " + t, ex);
            }
        }
        return Resultado.OK;
    }

    // ---------------------------------------------------------------- perdonar / vencer

    /** Perdona un tipo (o todos si {@code t} es null). Devuelve true si se quitó algo. */
    public static boolean perdonar(CommandSourceStack src, GameProfile objetivo, @Nullable TipoCastigo t) {
        return perdonar(src.getServer(), src.getTextName(), objetivo, t);
    }

    /** Igual que {@link #perdonar(CommandSourceStack, GameProfile, TipoCastigo)} con el nombre del actor explícito. */
    public static boolean perdonar(MinecraftServer server, String actor, GameProfile objetivo, @Nullable TipoCastigo t) {
        UUID id = objetivo == null ? null : objetivo.getId();
        if (id == null) return false;
        RegistroCastigos reg = RegistroCastigos.de(server);
        procesarVencidos(server, id, server.getPlayerList().getPlayer(id)); // los vencidos se cierran como vencidos
        Datos d = reg.datosSiExisten(id);
        if (d == null) return false;
        long ahora = System.currentTimeMillis();
        List<TipoCastigo> quitar = new ArrayList<>();
        for (TipoCastigo x : TipoCastigo.values()) {
            if (t != null && x != t) continue;
            if (d.estado.activo(x, ahora)) quitar.add(x);
        }
        if (quitar.isEmpty()) return false;
        String nombre = objetivo.getName() == null ? d.nombre : objetivo.getName();
        if (!nombre.isEmpty()) d.nombre = nombre;
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        for (TipoCastigo x : quitar) {
            d.estado.quitar(x);
            reg.agregarHistorial(id, new Entrada(ahora, actor, "PERDONAR", x, "-", ""));
            RegistroLogCastigos.anotar(server, actor, "PERDONAR", nombre, id.toString(), x.clave(), "-", "");
            Component tipoTxt = Component.translatable("senjucore.castigo.tipo." + x.clave());
            if (online != null) online.sendSystemMessage(Component.translatable("senjucore.castigo.perdonado.jugador", tipoTxt));
            avisarStaff(server, Component.translatable("senjucore.castigo.perdonado.staff", actor, nombre, tipoTxt));
            EfectoCastigo ef = EFECTOS.get(x);
            quitarEfecto(ef, server, id, online);
        }
        reg.setDirty();
        return true;
    }

    /**
     * Quita los castigos vencidos del jugador, anota VENCER en el historial, avisa al jugador si está conectado
     * y llama a {@code alQuitar}. Barato si no hay nada: pensado para el barrido periódico y el login.
     * Devuelve los tipos que vencieron.
     * IMPORTANTE: las Tasks 2/3 deben llamarlo en el login ANTES de aplicar cualquier refuerzo de CARCEL o CONGELAR.
     */
    public static List<TipoCastigo> procesarVencidos(MinecraftServer server, UUID id, @Nullable ServerPlayer online) {
        RegistroCastigos reg = RegistroCastigos.de(server);
        Datos d = reg.datosSiExisten(id);
        if (d == null || d.estado.estaVacio()) return List.of();
        long ahora = System.currentTimeMillis();
        List<TipoCastigo> v = d.estado.vencidos(ahora);
        if (v.isEmpty()) return v;
        for (TipoCastigo x : v) {
            reg.agregarHistorial(id, new Entrada(ahora, "-", "VENCER", x, "-", ""));
            String nom = d.nombre.isEmpty() ? id.toString() : d.nombre;
            RegistroLogCastigos.anotar(server, "sistema", "VENCER", nom, id.toString(), x.clave(), "-", "");
            if (online != null) {
                online.sendSystemMessage(Component.translatable("senjucore.castigo.vencido",
                        Component.translatable("senjucore.castigo.tipo." + x.clave())));
            }
            EfectoCastigo ef = EFECTOS.get(x);
            quitarEfecto(ef, server, id, online);
        }
        reg.setDirty();
        return v;
    }

    private static void quitarEfecto(@Nullable EfectoCastigo ef, MinecraftServer server, UUID id, @Nullable ServerPlayer online) {
        if (ef == null) return;
        try {
            ef.alQuitar(server, id, online);
        } catch (RuntimeException ex) {
            SenjuCore.LOGGER.error("Fallo en el efecto alQuitar", ex);
        }
    }

    private static void avisarStaff(MinecraftServer server, Component msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Staff.esStaff(p)) p.sendSystemMessage(msg);
        }
    }
}
