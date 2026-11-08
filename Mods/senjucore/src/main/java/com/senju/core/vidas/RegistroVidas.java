package com.senju.core.vidas;

import com.mojang.authlib.GameProfile;
import com.senju.core.config.CommonConfig;
import com.senju.core.staff.castigos.RegistroCastigos.Posicion;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Vidas persistidas por jugador (Overworld). Quien modifique un {@link Datos} debe llamar a {@link #setDirty()}. */
public class RegistroVidas extends SavedData {
    public static final String NOMBRE = "senjucore_vidas";

    public static final class Datos {
        public String nombre = "";
        public int vidas;
        /** Vidas a dar en cuanto esté conectado y vivo (0 = nada pendiente). */
        public int pendienteRevivir;
        /** Dónde aparecer al revivir (opcional). */
        @Nullable public Posicion posRevivir;
    }

    private final Map<UUID, Datos> jugadores = new HashMap<>();

    public static RegistroVidas de(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(RegistroVidas::cargar, RegistroVidas::new, NOMBRE);
    }

    /** Datos del jugador; el primer acceso lo crea con {@code vidasIniciales}. */
    public Datos datos(UUID id) {
        Datos d = jugadores.get(id);
        if (d == null) {
            d = new Datos();
            d.vidas = CommonConfig.VIDAS_INICIALES.get();
            jugadores.put(id, d);
            setDirty();
        }
        return d;
    }

    @Nullable
    public Datos datosSiExisten(UUID id) {
        return jugadores.get(id);
    }

    public Map<UUID, Datos> todos() {
        return Collections.unmodifiableMap(jugadores);
    }

    /** true si tiene registro con 0 vidas (no crea registro). */
    public boolean eliminado(UUID id) {
        Datos d = jugadores.get(id);
        return d != null && ReglaVidas.eliminado(d.vidas);
    }

    /** Busca por nombre sin distinguir mayúsculas entre los jugadores conocidos. */
    public Optional<GameProfile> buscarPorNombre(String nombre) {
        if (nombre == null) return Optional.empty();
        Map.Entry<UUID, Datos> primero = null;
        for (Map.Entry<UUID, Datos> e : jugadores.entrySet()) {
            if (e.getValue().nombre.isEmpty() || !e.getValue().nombre.equalsIgnoreCase(nombre)) continue;
            if (ReglaVidas.eliminado(e.getValue().vidas)) {
                return Optional.of(new GameProfile(e.getKey(), e.getValue().nombre));
            }
            if (primero == null) primero = e;
        }
        if (primero != null) return Optional.of(new GameProfile(primero.getKey(), primero.getValue().nombre));
        return Optional.empty();
    }

    public static RegistroVidas cargar(CompoundTag tag) {
        RegistroVidas r = new RegistroVidas();
        ListTag lista = tag.getList("jugadores", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag t = lista.getCompound(i);
            if (!t.hasUUID("id")) continue;
            Datos d = new Datos();
            d.nombre = t.getString("nombre");
            d.vidas = t.getInt("vidas");
            d.pendienteRevivir = t.getInt("pendiente");
            if (t.contains("pos", Tag.TAG_COMPOUND)) {
                CompoundTag p = t.getCompound("pos");
                d.posRevivir = new Posicion(p.getString("dim"), p.getDouble("x"), p.getDouble("y"),
                        p.getDouble("z"), p.getFloat("yRot"), p.getFloat("xRot"));
            }
            r.jugadores.put(t.getUUID("id"), d);
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag lista = new ListTag();
        for (Map.Entry<UUID, Datos> en : jugadores.entrySet()) {
            Datos d = en.getValue();
            CompoundTag t = new CompoundTag();
            t.putUUID("id", en.getKey());
            t.putString("nombre", d.nombre);
            t.putInt("vidas", d.vidas);
            t.putInt("pendiente", d.pendienteRevivir);
            if (d.posRevivir != null) {
                CompoundTag p = new CompoundTag();
                p.putString("dim", d.posRevivir.dimension());
                p.putDouble("x", d.posRevivir.x());
                p.putDouble("y", d.posRevivir.y());
                p.putDouble("z", d.posRevivir.z());
                p.putFloat("yRot", d.posRevivir.yRot());
                p.putFloat("xRot", d.posRevivir.xRot());
                t.put("pos", p);
            }
            lista.add(t);
        }
        tag.put("jugadores", lista);
        return tag;
    }
}
