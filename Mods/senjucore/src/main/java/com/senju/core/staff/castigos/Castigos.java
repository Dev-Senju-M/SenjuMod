package com.senju.core.staff.castigos;

import com.mojang.authlib.GameProfile;
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
 * Los efectos de mundo los aportan las tareas siguientes con {@link #registrarEfecto}.
 */
public final class Castigos {
    private Castigos() {}

    /** Resultado de {@link #aplicar}. Todo distinto de OK significa que no se aplicó ni se registró nada. */
    public enum Resultado { OK, ES_STAFF, SIN_CARCEL, VETADO }

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
        List<Entrada> r = new ArrayList<>();
        if (d == null) return r;
        for (int i = d.historial.size() - 1; i >= 0 && r.size() < max; i--) r.add(d.historial.get(i));
        return r;
    }

    /** Staff para perfiles conectados o no: en la lista {@code staff} u operador (según config). */
    public static boolean esStaff(MinecraftServer server, GameProfile perfil) {
        ServerPlayer on = server.getPlayerList().getPlayer(perfil.getId());
        if (on != null) return Staff.esStaff(on);
        return ReglaStaff.esStaff(server.getPlayerList().isOp(perfil), perfil.getName(),
                CommonConfig.STAFF.get(), CommonConfig.STAFF_INCLUYE_OPERADORES.get());
    }

    // ---------------------------------------------------------------- aplicar

    /**
     * Aplica un castigo. {@code duracionMs}: milisegundos o {@link Duracion#PERMANENTE}; se ignora para
     * EXPULSION; para QUITAR_HORAS es el tiempo a restar (PERMANENTE = todo el día).
     * Orden: staff -> veto del efecto -> estado/historial/log/avisos -> {@code alAplicar}.
     */
    public static Resultado aplicar(ServerPlayer staff, GameProfile objetivo, TipoCastigo t, long duracionMs,
            String motivo) {
        MinecraftServer server = staff.server;
        UUID id = objetivo.getId();
        if (esStaff(server, objetivo)) {
            staff.sendSystemMessage(Component.translatable("senjucore.castigo.esstaff"));
            return Resultado.ES_STAFF;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        EfectoCastigo ef = EFECTOS.get(t);
        if (ef != null) {
            Resultado v = ef.puedeAplicar(server, id, online, t);
            if (v != Resultado.OK) return v;
        }
        long ahora = System.currentTimeMillis();
        long vence = t.tieneDuracion() ? Duracion.vencimiento(ahora, duracionMs) : 0;
        String durTxt = t == TipoCastigo.EXPULSION ? "-" : Duracion.formatoDuracion(duracionMs);
        String nombreStaff = staff.getGameProfile().getName();
        String nombre = objetivo.getName() == null ? id.toString() : objetivo.getName();

        RegistroCastigos reg = RegistroCastigos.de(server);
        Datos d = reg.datos(id);
        d.nombre = nombre;
        if (t.tieneDuracion()) d.estado.aplicar(t, vence);
        reg.agregarHistorial(id, new Entrada(ahora, nombreStaff, "APLICAR", t, durTxt, motivo));
        reg.setDirty();
        RegistroLogCastigos.anotar(server, nombreStaff, "APLICAR", nombre, id.toString(), t.clave(), durTxt, motivo);

        Component tipoTxt = Component.translatable("senjucore.castigo.tipo." + t.clave());
        if (online != null) {
            online.sendSystemMessage(Component.translatable("senjucore.castigo.jugador", tipoTxt, durTxt, motivo));
        }
        avisarStaff(server, Component.translatable("senjucore.castigo.staff", nombreStaff, nombre, tipoTxt, durTxt, motivo));

        if (ef != null) ef.alAplicar(server, id, online, new Aplicado(t, duracionMs, vence, motivo, nombreStaff));
        return Resultado.OK;
    }

    // ---------------------------------------------------------------- perdonar / vencer

    /** Perdona un tipo (o todos si {@code t} es null). Devuelve true si se quitó algo. */
    public static boolean perdonar(CommandSourceStack src, GameProfile objetivo, @Nullable TipoCastigo t) {
        return perdonar(src.getServer(), src.getTextName(), objetivo, t);
    }

    /** Igual que {@link #perdonar(CommandSourceStack, GameProfile, TipoCastigo)} con el nombre del actor explícito. */
    public static boolean perdonar(MinecraftServer server, String actor, GameProfile objetivo, @Nullable TipoCastigo t) {
        UUID id = objetivo.getId();
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
            if (ef != null) ef.alQuitar(server, id, online);
        }
        reg.setDirty();
        return true;
    }

    /**
     * Quita los castigos vencidos del jugador, anota VENCER en el historial, avisa al jugador si está conectado
     * y llama a {@code alQuitar}. Barato si no hay nada: pensado para el barrido periódico y el login.
     * Devuelve los tipos que vencieron.
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
            if (online != null) {
                online.sendSystemMessage(Component.translatable("senjucore.castigo.vencido",
                        Component.translatable("senjucore.castigo.tipo." + x.clave())));
            }
            EfectoCastigo ef = EFECTOS.get(x);
            if (ef != null) ef.alQuitar(server, id, online);
        }
        reg.setDirty();
        return v;
    }

    private static void avisarStaff(MinecraftServer server, Component msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Staff.esStaff(p)) p.sendSystemMessage(msg);
        }
    }
}
