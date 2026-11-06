package com.senju.core.tiempo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Tiempo jugado por jugador, persistido en el Overworld. */
public class RegistroTiempo extends SavedData {
    public static final String NOMBRE = "senjucore_tiempo";

    private final Map<UUID, EstadoTiempo> datos = new HashMap<>();

    public static RegistroTiempo de(MinecraftServer s) {
        ServerLevel overworld = s.overworld();
        return overworld.getDataStorage().computeIfAbsent(RegistroTiempo::cargar, RegistroTiempo::new, NOMBRE);
    }

    public static RegistroTiempo cargar(CompoundTag tag) {
        RegistroTiempo r = new RegistroTiempo();
        ListTag lista = tag.getList("jugadores", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag c = lista.getCompound(i);
            r.datos.put(c.getUUID("uuid"), new EstadoTiempo(c.getLong("dia"), c.getLong("usados")));
        }
        return r;
    }

    public EstadoTiempo get(UUID id) {
        return datos.getOrDefault(id, new EstadoTiempo(0, 0));
    }

    public void set(UUID id, EstadoTiempo e) {
        datos.put(id, e);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag lista = new ListTag();
        datos.forEach((id, e) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("uuid", id);
            c.putLong("dia", e.dia());
            c.putLong("usados", e.usados());
            lista.add(c);
        });
        tag.put("jugadores", lista);
        return tag;
    }
}
