package com.senju.core.misiones;

import com.senju.core.staff.castigos.TipoCastigo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Misiones, cola global, progreso por jugador, ausencias e incumplidores (Overworld).
 * Quien mute algo llama a {@link #setDirty()}.
 */
public class RegistroMisiones extends SavedData {
    public static final String NOMBRE = "senjucore_misiones";

    public static final class DatosJugador {
        /** id de misión → (id de ítem → cantidad conseguida por él). */
        public final Map<Integer, Map<String, Integer>> progreso = new HashMap<>();
        /** id de misión → momento (ms) en que le quedó activa. */
        public final Map<Integer, Long> desde = new HashMap<>();
        public long globalCompletadaDia = -1;
        public long avisoDia = -1;
    }

    public record Incumplidor(UUID id, String nombre, @Nullable TipoCastigo castigo, boolean perdonado) {}

    public final Map<Integer, Mision> misiones = new LinkedHashMap<>();
    public int siguienteId = 1;
    public ColaGlobal cola = new ColaGlobal(List.of());
    /** Id de la global de hoy (−1 = ninguna). */
    public int globalHoy = -1;
    /** Último día procesado para la global. */
    public long diaGlobal = -1;
    public final Map<UUID, DatosJugador> jugadores = new HashMap<>();
    /** Días (epochDay) con permiso de ausencia por jugador. */
    public final Map<UUID, Set<Long>> ausencias = new HashMap<>();
    public final List<Incumplidor> incumplidores = new ArrayList<>();
    public long diaIncumplidores = -1;
    /** Expulsiones a aplicar al conectarse (jugador → motivo). */
    public final Map<UUID, String> expulsionesPendientes = new HashMap<>();

    public static RegistroMisiones de(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(RegistroMisiones::cargar, RegistroMisiones::new, NOMBRE);
    }

    /** Datos del jugador (los crea si no existen). */
    public DatosJugador datos(UUID id) {
        DatosJugador d = jugadores.get(id);
        if (d == null) {
            d = new DatosJugador();
            jugadores.put(id, d);
            setDirty();
        }
        return d;
    }

    @Nullable
    public DatosJugador datosSiExisten(UUID id) {
        return jugadores.get(id);
    }

    public int nuevoId() {
        setDirty();
        return siguienteId++;
    }

    public static RegistroMisiones cargar(CompoundTag tag) {
        RegistroMisiones r = new RegistroMisiones();
        ListTag ms = tag.getList("misiones", Tag.TAG_COMPOUND);
        int maxId = 0;
        for (int i = 0; i < ms.size(); i++) {
            Mision m = Mision.cargar(ms.getCompound(i));
            if (m.id <= 0) continue;
            r.misiones.put(m.id, m);
            maxId = Math.max(maxId, m.id);
        }
        r.siguienteId = Math.max(tag.getInt("siguienteId"), maxId + 1);

        List<Integer> cola = new ArrayList<>();
        ListTag c = tag.getList("cola", Tag.TAG_INT);
        for (int i = 0; i < c.size(); i++) {
            int id = c.getInt(i);
            Mision m = r.misiones.get(id);
            if (m != null && m.tipo == TipoMision.GLOBAL && !cola.contains(id)) cola.add(id);
        }
        r.cola = new ColaGlobal(cola);
        r.globalHoy = tag.contains("globalHoy", Tag.TAG_INT) ? tag.getInt("globalHoy") : -1;
        if (!r.misiones.containsKey(r.globalHoy)) r.globalHoy = -1;
        r.diaGlobal = tag.contains("diaGlobal", Tag.TAG_LONG) ? tag.getLong("diaGlobal") : -1;

        ListTag js = tag.getList("jugadores", Tag.TAG_COMPOUND);
        for (int i = 0; i < js.size(); i++) {
            CompoundTag t = js.getCompound(i);
            if (!t.hasUUID("id")) continue;
            DatosJugador d = new DatosJugador();
            ListTag ps = t.getList("progreso", Tag.TAG_COMPOUND);
            for (int k = 0; k < ps.size(); k++) {
                CompoundTag p = ps.getCompound(k);
                int mid = p.getInt("mision");
                if (!r.misiones.containsKey(mid)) continue;
                Map<String, Integer> prog = new HashMap<>();
                ListTag its = p.getList("items", Tag.TAG_COMPOUND);
                for (int x = 0; x < its.size(); x++) {
                    CompoundTag it = its.getCompound(x);
                    int n = it.getInt("n");
                    if (n > 0) prog.put(it.getString("item"), n);
                }
                d.progreso.put(mid, prog);
                d.desde.put(mid, p.getLong("desde"));
            }
            d.globalCompletadaDia = t.contains("globalCompletadaDia", Tag.TAG_LONG) ? t.getLong("globalCompletadaDia") : -1;
            d.avisoDia = t.contains("avisoDia", Tag.TAG_LONG) ? t.getLong("avisoDia") : -1;
            r.jugadores.put(t.getUUID("id"), d);
        }

        ListTag aus = tag.getList("ausencias", Tag.TAG_COMPOUND);
        for (int i = 0; i < aus.size(); i++) {
            CompoundTag t = aus.getCompound(i);
            if (!t.hasUUID("id")) continue;
            Set<Long> dias = new TreeSet<>();
            for (long dia : t.getLongArray("dias")) dias.add(dia);
            if (!dias.isEmpty()) r.ausencias.put(t.getUUID("id"), dias);
        }

        ListTag inc = tag.getList("incumplidores", Tag.TAG_COMPOUND);
        for (int i = 0; i < inc.size(); i++) {
            CompoundTag t = inc.getCompound(i);
            if (!t.hasUUID("id")) continue;
            TipoCastigo cast = t.contains("castigo", Tag.TAG_STRING)
                    ? TipoCastigo.deClave(t.getString("castigo")).orElse(null) : null;
            r.incumplidores.add(new Incumplidor(t.getUUID("id"), t.getString("nombre"), cast, t.getBoolean("perdonado")));
        }
        r.diaIncumplidores = tag.contains("diaIncumplidores", Tag.TAG_LONG) ? tag.getLong("diaIncumplidores") : -1;

        ListTag exp = tag.getList("expulsiones", Tag.TAG_COMPOUND);
        for (int i = 0; i < exp.size(); i++) {
            CompoundTag t = exp.getCompound(i);
            if (t.hasUUID("id")) r.expulsionesPendientes.put(t.getUUID("id"), t.getString("motivo"));
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag ms = new ListTag();
        for (Mision m : misiones.values()) ms.add(m.guardar());
        tag.put("misiones", ms);
        tag.putInt("siguienteId", siguienteId);
        ListTag c = new ListTag();
        for (int id : cola.ids()) c.add(IntTag.valueOf(id));
        tag.put("cola", c);
        tag.putInt("globalHoy", globalHoy);
        tag.putLong("diaGlobal", diaGlobal);

        ListTag js = new ListTag();
        for (Map.Entry<UUID, DatosJugador> en : jugadores.entrySet()) {
            DatosJugador d = en.getValue();
            CompoundTag t = new CompoundTag();
            t.putUUID("id", en.getKey());
            ListTag ps = new ListTag();
            for (Map.Entry<Integer, Map<String, Integer>> pe : d.progreso.entrySet()) {
                CompoundTag p = new CompoundTag();
                p.putInt("mision", pe.getKey());
                p.putLong("desde", d.desde.getOrDefault(pe.getKey(), 0L));
                ListTag its = new ListTag();
                for (Map.Entry<String, Integer> ie : pe.getValue().entrySet()) {
                    CompoundTag it = new CompoundTag();
                    it.putString("item", ie.getKey());
                    it.putInt("n", ie.getValue());
                    its.add(it);
                }
                p.put("items", its);
                ps.add(p);
            }
            t.put("progreso", ps);
            t.putLong("globalCompletadaDia", d.globalCompletadaDia);
            t.putLong("avisoDia", d.avisoDia);
            js.add(t);
        }
        tag.put("jugadores", js);

        ListTag aus = new ListTag();
        for (Map.Entry<UUID, Set<Long>> en : ausencias.entrySet()) {
            if (en.getValue().isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putUUID("id", en.getKey());
            t.putLongArray("dias", en.getValue().stream().mapToLong(Long::longValue).toArray());
            aus.add(t);
        }
        tag.put("ausencias", aus);

        ListTag inc = new ListTag();
        for (Incumplidor i : incumplidores) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", i.id());
            t.putString("nombre", i.nombre() == null ? "" : i.nombre());
            if (i.castigo() != null) t.putString("castigo", i.castigo().clave());
            t.putBoolean("perdonado", i.perdonado());
            inc.add(t);
        }
        tag.put("incumplidores", inc);
        tag.putLong("diaIncumplidores", diaIncumplidores);

        ListTag exp = new ListTag();
        for (Map.Entry<UUID, String> en : expulsionesPendientes.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", en.getKey());
            t.putString("motivo", en.getValue() == null ? "" : en.getValue());
            exp.add(t);
        }
        tag.put("expulsiones", exp);
        return tag;
    }
}
