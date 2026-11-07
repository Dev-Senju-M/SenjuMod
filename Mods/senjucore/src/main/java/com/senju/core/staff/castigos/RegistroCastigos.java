package com.senju.core.staff.castigos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Castigos persistidos (Overworld): estado, historial y posiciones por jugador, y la cárcel global. */
public class RegistroCastigos extends SavedData {
    public static final String NOMBRE = "senjucore_castigos";
    public static final int MAX_HISTORIAL = 100;

    /** Posición en un mundo (dimensión como texto "namespace:path"). */
    public record Posicion(String dimension, double x, double y, double z, float yRot, float xRot) {
        CompoundTag guardar() {
            CompoundTag t = new CompoundTag();
            t.putString("dim", dimension);
            t.putDouble("x", x);
            t.putDouble("y", y);
            t.putDouble("z", z);
            t.putFloat("yRot", yRot);
            t.putFloat("xRot", xRot);
            return t;
        }

        static Posicion cargar(CompoundTag t) {
            return new Posicion(t.getString("dim"), t.getDouble("x"), t.getDouble("y"), t.getDouble("z"),
                    t.getFloat("yRot"), t.getFloat("xRot"));
        }
    }

    /** Línea de historial. accion: APLICAR / PERDONAR / VENCER. */
    public record Entrada(long fecha, String staff, String accion, TipoCastigo tipo, String duracion, String motivo) {
        CompoundTag guardar() {
            CompoundTag t = new CompoundTag();
            t.putLong("fecha", fecha);
            t.putString("staff", staff);
            t.putString("accion", accion);
            t.putString("tipo", tipo.clave());
            t.putString("duracion", duracion);
            t.putString("motivo", motivo);
            return t;
        }

        @Nullable
        static Entrada cargar(CompoundTag t) {
            TipoCastigo tipo = TipoCastigo.deClave(t.getString("tipo")).orElse(null);
            if (tipo == null) return null;
            return new Entrada(t.getLong("fecha"), t.getString("staff"), t.getString("accion"), tipo,
                    t.getString("duracion"), t.getString("motivo"));
        }
    }

    /** Datos de un jugador. Quien los modifique debe llamar a {@link #setDirty()} del registro. */
    public static final class Datos {
        public String nombre = "";
        public final EstadoCastigos estado = new EstadoCastigos();
        /** Más reciente al final. */
        public final List<Entrada> historial = new ArrayList<>();
        @Nullable public Posicion retornoCarcel;
        @Nullable public Posicion posCongelado;

        boolean vacio() {
            return estado.estaVacio() && historial.isEmpty() && retornoCarcel == null && posCongelado == null;
        }
    }

    private final Map<UUID, Datos> jugadores = new HashMap<>();
    @Nullable private Posicion carcel;

    public static RegistroCastigos de(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(RegistroCastigos::cargar, RegistroCastigos::new, NOMBRE);
    }

    /** Datos del jugador, creándolos si no existen. */
    public Datos datos(UUID id) {
        return jugadores.computeIfAbsent(id, k -> new Datos());
    }

    /** Datos existentes o null (no crea). */
    @Nullable
    public Datos datosSiExisten(UUID id) {
        return jugadores.get(id);
    }

    public Map<UUID, Datos> todos() {
        return Collections.unmodifiableMap(jugadores);
    }

    @Nullable
    public Posicion carcel() {
        return carcel;
    }

    public void fijarCarcel(Posicion p) {
        carcel = p;
        setDirty();
    }

    public void agregarHistorial(UUID id, Entrada e) {
        List<Entrada> h = datos(id).historial;
        h.add(e);
        while (h.size() > MAX_HISTORIAL) h.remove(0);
        setDirty();
    }

    public static RegistroCastigos cargar(CompoundTag tag) {
        RegistroCastigos r = new RegistroCastigos();
        if (tag.contains("carcel", Tag.TAG_COMPOUND)) r.carcel = Posicion.cargar(tag.getCompound("carcel"));
        ListTag lista = tag.getList("jugadores", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag t = lista.getCompound(i);
            if (!t.hasUUID("id")) continue;
            Datos d = new Datos();
            d.nombre = t.getString("nombre");
            ListTag act = t.getList("activos", Tag.TAG_COMPOUND);
            for (int j = 0; j < act.size(); j++) {
                CompoundTag a = act.getCompound(j);
                TipoCastigo.deClave(a.getString("tipo")).ifPresent(tp -> d.estado.aplicar(tp, a.getLong("vence")));
            }
            ListTag hist = t.getList("historial", Tag.TAG_COMPOUND);
            for (int j = 0; j < hist.size(); j++) {
                Entrada e = Entrada.cargar(hist.getCompound(j));
                if (e != null) d.historial.add(e);
            }
            if (t.contains("retorno", Tag.TAG_COMPOUND)) d.retornoCarcel = Posicion.cargar(t.getCompound("retorno"));
            if (t.contains("congelado", Tag.TAG_COMPOUND)) d.posCongelado = Posicion.cargar(t.getCompound("congelado"));
            r.jugadores.put(t.getUUID("id"), d);
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (carcel != null) tag.put("carcel", carcel.guardar());
        ListTag lista = new ListTag();
        for (Map.Entry<UUID, Datos> en : jugadores.entrySet()) {
            Datos d = en.getValue();
            if (d.vacio()) continue;
            CompoundTag t = new CompoundTag();
            t.putUUID("id", en.getKey());
            t.putString("nombre", d.nombre);
            ListTag act = new ListTag();
            for (Map.Entry<TipoCastigo, Long> a : d.estado.todos().entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putString("tipo", a.getKey().clave());
                c.putLong("vence", a.getValue());
                act.add(c);
            }
            t.put("activos", act);
            ListTag hist = new ListTag();
            for (Entrada e : d.historial) hist.add(e.guardar());
            t.put("historial", hist);
            if (d.retornoCarcel != null) t.put("retorno", d.retornoCarcel.guardar());
            if (d.posCongelado != null) t.put("congelado", d.posCongelado.guardar());
            lista.add(t);
        }
        tag.put("jugadores", lista);
        return tag;
    }
}
