package com.senju.core.staff.vanish;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Jugadores en Vanish, persistido en el Overworld. */
public class RegistroVanish extends SavedData {
    public static final String NOMBRE = "senjucore_vanish";

    private final Set<UUID> ocultos = new HashSet<>();

    public static RegistroVanish de(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(RegistroVanish::cargar, RegistroVanish::new, NOMBRE);
    }

    public static RegistroVanish cargar(CompoundTag tag) {
        RegistroVanish r = new RegistroVanish();
        ListTag lista = tag.getList("ocultos", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < lista.size(); i++) r.ocultos.add(NbtUtils.loadUUID(lista.get(i)));
        return r;
    }

    public boolean contiene(UUID id) {
        return ocultos.contains(id);
    }

    public void poner(UUID id, boolean activo) {
        boolean cambio = activo ? ocultos.add(id) : ocultos.remove(id);
        if (cambio) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag lista = new ListTag();
        for (UUID id : ocultos) lista.add(NbtUtils.createUUID(id));
        tag.put("ocultos", lista);
        return tag;
    }
}
