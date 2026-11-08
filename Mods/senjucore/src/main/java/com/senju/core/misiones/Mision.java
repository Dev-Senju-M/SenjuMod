package com.senju.core.misiones;

import com.senju.core.staff.castigos.TipoCastigo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Misión guardada (mutable). Objetivos: id de ítem → cantidad, en orden. */
public class Mision {
    public static final int MAX_NOMBRE = 32;
    public static final int MAX_OBJETIVOS = 18;
    public static final int MAX_RECOMPENSA = 9;

    public int id;
    public String nombre = "";
    public TipoMision tipo = TipoMision.GLOBAL;
    public LinkedHashMap<String, Integer> objetivos = new LinkedHashMap<>();
    public List<ItemStack> recompensa = new ArrayList<>();
    @Nullable public TipoCastigo castigo;
    public long castigoMs;
    public LinkedHashSet<UUID> asignados = new LinkedHashSet<>();
    public Map<UUID, String> nombresAsignados = new HashMap<>();

    public CompoundTag guardar() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("nombre", nombre);
        t.putString("tipo", tipo.name());
        ListTag obj = new ListTag();
        for (Map.Entry<String, Integer> e : objetivos.entrySet()) {
            CompoundTag o = new CompoundTag();
            o.putString("item", e.getKey());
            o.putInt("n", e.getValue());
            obj.add(o);
        }
        t.put("objetivos", obj);
        ListTag rec = new ListTag();
        for (ItemStack s : recompensa) {
            if (!s.isEmpty()) rec.add(s.save(new CompoundTag()));
        }
        t.put("recompensa", rec);
        if (castigo != null) t.putString("castigo", castigo.clave());
        t.putLong("castigoMs", castigoMs);
        ListTag asig = new ListTag();
        for (UUID u : asignados) {
            CompoundTag a = new CompoundTag();
            a.putUUID("id", u);
            a.putString("nombre", nombresAsignados.getOrDefault(u, ""));
            asig.add(a);
        }
        t.put("asignados", asig);
        return t;
    }

    public static Mision cargar(CompoundTag t) {
        Mision m = new Mision();
        m.id = t.getInt("id");
        m.nombre = recortarNombre(t.getString("nombre"));
        m.tipo = TipoMision.deNombre(t.getString("tipo"));
        ListTag obj = t.getList("objetivos", Tag.TAG_COMPOUND);
        for (int i = 0; i < obj.size(); i++) {
            CompoundTag o = obj.getCompound(i);
            String item = o.getString("item");
            if (!item.isEmpty()) m.objetivos.put(item, o.getInt("n"));
        }
        sanear(m.objetivos);
        ListTag rec = t.getList("recompensa", Tag.TAG_COMPOUND);
        for (int i = 0; i < rec.size() && m.recompensa.size() < MAX_RECOMPENSA; i++) {
            ItemStack s = ItemStack.of(rec.getCompound(i));
            if (!s.isEmpty()) m.recompensa.add(s);
        }
        m.castigo = t.contains("castigo", Tag.TAG_STRING) ? TipoCastigo.deClave(t.getString("castigo")).orElse(null) : null;
        m.castigoMs = Math.max(0, t.getLong("castigoMs"));
        ListTag asig = t.getList("asignados", Tag.TAG_COMPOUND);
        for (int i = 0; i < asig.size(); i++) {
            CompoundTag a = asig.getCompound(i);
            if (!a.hasUUID("id")) continue;
            UUID u = a.getUUID("id");
            m.asignados.add(u);
            m.nombresAsignados.put(u, a.getString("nombre"));
        }
        return m;
    }

    /** Quita objetivos con cantidad < 1, limita a {@link ReglaMisiones#MAX_CANTIDAD} y a {@link #MAX_OBJETIVOS}. */
    static void sanear(LinkedHashMap<String, Integer> objetivos) {
        LinkedHashMap<String, Integer> limpio = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : objetivos.entrySet()) {
            Integer n = e.getValue();
            if (e.getKey() == null || e.getKey().isEmpty() || n == null || n < 1) continue;
            if (limpio.size() >= MAX_OBJETIVOS) break;
            limpio.put(e.getKey(), Math.min(n, ReglaMisiones.MAX_CANTIDAD));
        }
        objetivos.clear();
        objetivos.putAll(limpio);
    }

    static String recortarNombre(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > MAX_NOMBRE ? s.substring(0, MAX_NOMBRE) : s;
    }
}
