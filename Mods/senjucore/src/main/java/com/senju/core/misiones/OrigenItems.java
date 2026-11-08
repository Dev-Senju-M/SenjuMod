package com.senju.core.misiones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Origen de los ítems para el crédito personal: marcas de zona (jugador, dimensión, punto, radio, tick) que etiquetan
 * los {@link ItemEntity} que aparecen ahí en el mismo tick o el siguiente, e instantáneas del inventario para lo que
 * llega directo a la mano (cubo de leche, botella de miel). Todo en el hilo del servidor.
 */
public final class OrigenItems {
    public static final String TAG_ORIGEN = "senjucore_origen";

    private record Marca(UUID jugador, ResourceKey<Level> dim, Vec3 pos, double radio, long tick) {}

    /** Conteo antes de la interacción y lo que entró por otras vías (recogidas, crafteos…) desde entonces. */
    private static final class Instantanea {
        final Map<String, Integer> antes;
        final Map<String, Integer> restar = new HashMap<>();

        Instantanea(Map<String, Integer> antes) {
            this.antes = antes;
        }
    }

    private static final List<Marca> marcas = new ArrayList<>();
    private static final Map<UUID, Instantanea> instantaneas = new HashMap<>();

    private OrigenItems() {}

    /** Marca la zona: los ítems que aparezcan en ella este tick o el siguiente son de {@code p}. */
    public static void marcarZona(ServerPlayer p, Vec3 centro, double radio) {
        Level l = p.level();
        marcas.add(new Marca(p.getUUID(), l.dimension(), centro, radio, l.getGameTime()));
    }

    /** Etiqueta un ítem como conseguido por {@code jugador} (si no tenía ya dueño). */
    public static void etiquetar(ItemEntity e, UUID jugador) {
        CompoundTag t = e.getPersistentData();
        if (!t.hasUUID(TAG_ORIGEN)) t.putUUID(TAG_ORIGEN, jugador);
    }

    /** Ítem recién aparecido (no cargado de disco): primera marca que coincide → se etiqueta. */
    public static void alAparecer(ItemEntity e, Level l) {
        if (marcas.isEmpty() || e.getPersistentData().hasUUID(TAG_ORIGEN)) return;
        long ahora = l.getGameTime();
        for (Marca m : marcas) {
            if (!m.dim.equals(l.dimension())) continue;
            if (ReglaOrigen.coincide(m.tick, ahora, e.position().distanceToSqr(m.pos), m.radio)) {
                e.getPersistentData().putUUID(TAG_ORIGEN, m.jugador);
                return;
            }
        }
    }

    /** Jugador que consiguió el ítem, o null si no tiene marca. */
    @Nullable
    public static UUID duenio(ItemEntity e) {
        CompoundTag t = e.getPersistentData();
        return t.hasUUID(TAG_ORIGEN) ? t.getUUID(TAG_ORIGEN) : null;
    }

    /**
     * Guarda el conteo actual de los ids que piden sus misiones activas; al final del tick se acredita el aumento.
     * Si ya había una instantánea pendiente se conserva la primera.
     */
    public static void instantanea(ServerPlayer p) {
        if (instantaneas.containsKey(p.getUUID())) return;
        Set<String> ids = Misiones.idsPedidos(p);
        if (ids.isEmpty()) return;
        Map<String, Integer> antes = new HashMap<>();
        for (String id : ids) antes.put(id, 0);
        antes.putAll(Misiones.contarInventario(p, ids));
        instantaneas.put(p.getUUID(), new Instantanea(antes));
    }

    /** Entró {@code n} de {@code item} por otra vía (recogida o crédito directo): no cuenta como "directo a la mano". */
    public static void registrarEntrada(ServerPlayer p, Item item, int n) {
        if (instantaneas.isEmpty() || n <= 0) return;
        Instantanea in = instantaneas.get(p.getUUID());
        if (in == null) return;
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        if (in.antes.containsKey(id)) in.restar.merge(id, n, Integer::sum);
    }

    /** Fin de tick: acredita las instantáneas pendientes y descarta las marcas de más de 1 tick. */
    public static void finDeTick(MinecraftServer s) {
        if (!instantaneas.isEmpty()) {
            Map<UUID, Instantanea> copia = new HashMap<>(instantaneas);
            instantaneas.clear();
            for (Map.Entry<UUID, Instantanea> e : copia.entrySet()) {
                ServerPlayer p = s.getPlayerList().getPlayer(e.getKey());
                // Si abrió un contenedor, el aumento puede venir de él: no se acredita.
                if (p == null || p.containerMenu != p.inventoryMenu) continue;
                Instantanea in = e.getValue();
                Map<String, Integer> ahora = Misiones.contarInventario(p, in.antes.keySet());
                for (Map.Entry<String, Integer> a : in.antes.entrySet()) {
                    String id = a.getKey();
                    int aumento = ahora.getOrDefault(id, 0) - a.getValue() - in.restar.getOrDefault(id, 0);
                    if (aumento <= 0) continue;
                    ResourceLocation rl = ResourceLocation.tryParse(id);
                    if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) continue;
                    Misiones.acreditar(p, BuiltInRegistries.ITEM.get(rl), aumento);
                }
            }
        }
        if (!marcas.isEmpty()) {
            Iterator<Marca> it = marcas.iterator();
            while (it.hasNext()) {
                Marca m = it.next();
                ServerLevel l = s.getLevel(m.dim);
                if (l == null || l.getGameTime() - m.tick > 1) it.remove();
            }
        }
    }

    /** Al salir el jugador: se olvidan sus marcas e instantánea. */
    public static void olvidar(UUID jugador) {
        instantaneas.remove(jugador);
        marcas.removeIf(m -> m.jugador.equals(jugador));
    }

    /** Al parar el servidor. */
    public static void limpiar() {
        instantaneas.clear();
        marcas.clear();
    }
}
