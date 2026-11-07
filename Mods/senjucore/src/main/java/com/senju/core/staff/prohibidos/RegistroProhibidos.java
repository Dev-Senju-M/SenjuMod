package com.senju.core.staff.prohibidos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Lista de objetos prohibidos, persistida en el Overworld. */
public class RegistroProhibidos extends SavedData {
    public static final String NOMBRE = "senjucore_prohibidos";

    private final ListaProhibidos lista = new ListaProhibidos();

    public static RegistroProhibidos de(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(RegistroProhibidos::cargar, RegistroProhibidos::new, NOMBRE);
    }

    public static RegistroProhibidos cargar(CompoundTag tag) {
        RegistroProhibidos r = new RegistroProhibidos();
        ListTag ids = tag.getList("ids", Tag.TAG_STRING);
        for (int i = 0; i < ids.size(); i++) r.lista.agregar(ids.getString(i));
        return r;
    }

    public ListaProhibidos lista() {
        return lista;
    }

    public boolean agregar(String id) {
        boolean c = lista.agregar(id);
        if (c) setDirty();
        return c;
    }

    public boolean quitar(String id) {
        boolean c = lista.quitar(id);
        if (c) setDirty();
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag ids = new ListTag();
        for (String id : lista.todos()) ids.add(StringTag.valueOf(id));
        tag.put("ids", ids);
        return tag;
    }
}
