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
import net.minecraft.world.item.ItemStack;

/** Misión en edición en el menú del staff (copia independiente de la guardada). */
public class Borrador {
    /** Id de la misión que se edita; −1 = nueva. */
    public int idEditando = -1;
    public String nombre = "";
    public TipoMision tipo = TipoMision.GLOBAL;
    public LinkedHashMap<String, Integer> objetivos = new LinkedHashMap<>();
    public List<ItemStack> recompensa = new ArrayList<>();
    @Nullable public TipoCastigo castigo;
    public long castigoMs;
    public LinkedHashSet<UUID> asignados = new LinkedHashSet<>();
    public Map<UUID, String> nombresAsignados = new HashMap<>();

    public static Borrador nueva() {
        return new Borrador();
    }

    public static Borrador de(Mision m) {
        Borrador b = new Borrador();
        b.idEditando = m.id;
        b.nombre = m.nombre;
        b.tipo = m.tipo;
        b.objetivos = new LinkedHashMap<>(m.objetivos);
        for (ItemStack s : m.recompensa) b.recompensa.add(s.copy());
        b.castigo = m.castigo;
        b.castigoMs = m.castigoMs;
        b.asignados = new LinkedHashSet<>(m.asignados);
        b.nombresAsignados = new HashMap<>(m.nombresAsignados);
        return b;
    }

    /** Nombre no vacío y al menos un objetivo. */
    public boolean valido() {
        return nombre != null && !nombre.isBlank() && !objetivos.isEmpty();
    }
}
