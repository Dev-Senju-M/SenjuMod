package com.senju.core.staff.menu;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

/** Ítems de botón del menú del staff. Nunca son placas. */
public final class BotonesMenu {
    private BotonesMenu() {}

    private static MutableComponent nombre(String clave, ChatFormatting color) {
        return Component.translatable(clave).withStyle(Style.EMPTY.withItalic(false).withColor(color));
    }

    private static Component desc(String clave) {
        return Component.translatable(clave).withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY));
    }

    private static ItemStack boton(ItemStack s, MutableComponent nombre, String descClave) {
        s.setHoverName(nombre);
        if (descClave != null) {
            ListTag lore = new ListTag();
            lore.add(StringTag.valueOf(Component.Serializer.toJson(desc(descClave))));
            s.getOrCreateTagElement("display").put("Lore", lore);
        }
        // Oculta todo salvo nombre y lore (el lore no es un TooltipPart).
        for (ItemStack.TooltipPart parte : ItemStack.TooltipPart.values()) s.hideTooltipPart(parte);
        return s;
    }

    public static ItemStack relleno() {
        ItemStack s = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        s.setHoverName(Component.literal("").withStyle(Style.EMPTY.withItalic(false)));
        return s;
    }

    public static ItemStack castigar() {
        return boton(new ItemStack(Items.IRON_AXE), nombre("senjucore.menu.castigar", ChatFormatting.RED),
                "senjucore.menu.proximamente");
    }

    public static ItemStack inventario() {
        return boton(new ItemStack(Items.CHEST), nombre("senjucore.menu.inventario", ChatFormatting.AQUA),
                "senjucore.menu.proximamente");
    }

    public static ItemStack cerrar() {
        return boton(new ItemStack(Items.BARRIER), nombre("senjucore.menu.cerrar", ChatFormatting.WHITE), null);
    }

    /**
     * Las pociones con efecto brillan siempre en vanilla, así que el brillo marca el estado:
     * activo = poción de invisibilidad (brilla); inactivo = poción sin efectos del mismo color (no brilla).
     */
    public static ItemStack vanish(boolean activo) {
        ItemStack s = activo
                ? PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.INVISIBILITY)
                : new ItemStack(Items.POTION);
        if (!activo) s.getOrCreateTag().putInt("CustomPotionColor", 0x7F8392);
        return boton(s, nombre(activo ? "senjucore.menu.vanish.on" : "senjucore.menu.vanish.off",
                activo ? ChatFormatting.GREEN : ChatFormatting.RED), "senjucore.menu.vanish.desc");
    }
}
