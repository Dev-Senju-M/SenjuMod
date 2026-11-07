package com.senju.core.staff.menu;

import com.senju.core.staff.PlacaStaffItem;
import com.senju.core.staff.prohibidos.Prohibidos;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/** Copias de ítems para mostrar en las vistas de inventario. Nunca se coloca el original. */
final class CopiaVista {
    private CopiaVista() {}

    /**
     * Copia para la vista. IMPORTANTE: una copia de la Placa de Staff NUNCA se pone en un contenedor de menú:
     * ProteccionPlaca la "extraería" y se la devolvería al espectador, duplicándola. Se muestra un marcador
     * (papel "Placa de Staff") en su lugar. Los ítems prohibidos llevan una línea de lore roja (solo en la copia).
     */
    static ItemStack de(MinecraftServer server, ItemStack original) {
        if (original.isEmpty()) return ItemStack.EMPTY;
        if (PlacaStaffItem.esPlaca(original)) return BotonesMenu.marcadorPlaca();
        ItemStack copia = original.copy();
        if (Prohibidos.esProhibido(server, original)) {
            CompoundTag display = copia.getOrCreateTagElement("display");
            ListTag lore = display.getList("Lore", Tag.TAG_STRING);
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.translatable("senjucore.menu.marcaprohibido")
                            .withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.RED)))));
            display.put("Lore", lore);
        }
        return copia;
    }
}
