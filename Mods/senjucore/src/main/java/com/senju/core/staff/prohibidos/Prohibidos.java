package com.senju.core.staff.prohibidos;

import java.util.List;
import com.senju.core.staff.PlacaStaffItem;
import com.senju.core.staff.Staff;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Fachada de la lista de objetos prohibidos. */
public final class Prohibidos {
    private Prohibidos() {}

    private static String id(Item item) {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(item));
    }

    public static boolean esProhibido(MinecraftServer server, ItemStack s) {
        if (s.isEmpty() || PlacaStaffItem.esPlaca(s)) return false;
        return RegistroProhibidos.de(server).lista().contiene(id(s.getItem()));
    }

    public static boolean agregar(ServerPlayer staff, ItemStack ejemplo) {
        if (ejemplo.isEmpty()) return false;
        if (PlacaStaffItem.esPlaca(ejemplo)) {
            staff.sendSystemMessage(Component.translatable("senjucore.prohibidos.noplaca"));
            return false;
        }
        if (!RegistroProhibidos.de(staff.server).agregar(id(ejemplo.getItem()))) {
            // Barra de acción: un shift+doble clic envía varios QUICK_MOVE seguidos.
            staff.displayClientMessage(Component.translatable("senjucore.prohibidos.yaesta",
                    new ItemStack(ejemplo.getItem()).getHoverName()), true);
            return false;
        }
        avisar(staff, "senjucore.prohibidos.agregado", new ItemStack(ejemplo.getItem()).getHoverName());
        return true;
    }

    public static boolean quitar(ServerPlayer staff, String id) {
        if (!RegistroProhibidos.de(staff.server).quitar(id)) return false;
        Component nombre = Component.literal(id);
        // tryParse: un ID mal formado (lista editada a mano) no debe lanzar excepción.
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && ForgeRegistries.ITEMS.containsKey(rl)) {
            nombre = new ItemStack(ForgeRegistries.ITEMS.getValue(rl)).getHoverName();
        }
        avisar(staff, "senjucore.prohibidos.quitado", nombre);
        return true;
    }

    public static List<String> pagina(MinecraftServer server, int pagina, int porPagina) {
        return RegistroProhibidos.de(server).lista().pagina(pagina, porPagina);
    }

    public static int tamano(MinecraftServer server) {
        return RegistroProhibidos.de(server).lista().tamano();
    }

    private static void avisar(ServerPlayer actor, String clave, Component item) {
        Component aviso = Component.translatable(clave, actor.getGameProfile().getName(), item);
        for (ServerPlayer p : actor.server.getPlayerList().getPlayers()) {
            if (Staff.esStaff(p)) p.sendSystemMessage(aviso);
        }
    }
}
