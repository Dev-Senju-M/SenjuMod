package com.senju.core.staff.menu;

import net.minecraft.ChatFormatting;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

/** Ítems de botón del menú del staff. Nunca son placas. */
public final class BotonesMenu {
    private BotonesMenu() {}

    static MutableComponent nombre(String clave, ChatFormatting color) {
        return Component.translatable(clave).withStyle(Style.EMPTY.withItalic(false).withColor(color));
    }

    private static Component desc(String clave) {
        return Component.translatable(clave).withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY));
    }

    /** @param nombre null conserva el nombre natural del ítem (sin cursiva y con el color de su rareza). */
    static ItemStack boton(ItemStack s, MutableComponent nombre, String descClave) {
        if (nombre != null) s.setHoverName(nombre);
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
                "senjucore.menu.inventario.desc");
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

    public static ItemStack prohibidos() {
        return boton(new ItemStack(Items.WRITABLE_BOOK), nombre("senjucore.menu.prohibidos", ChatFormatting.GOLD),
                "senjucore.menu.prohibidos.desc");
    }

    public static ItemStack volver() {
        return boton(new ItemStack(Items.ARROW), nombre("senjucore.menu.volver", ChatFormatting.YELLOW), null);
    }

    public static ItemStack anterior() {
        return boton(new ItemStack(Items.SPECTRAL_ARROW), nombre("senjucore.menu.anterior", ChatFormatting.WHITE), null);
    }

    public static ItemStack siguiente() {
        return boton(new ItemStack(Items.SPECTRAL_ARROW), nombre("senjucore.menu.siguiente", ChatFormatting.WHITE), null);
    }

    public static ItemStack ender() {
        return boton(new ItemStack(Items.ENDER_CHEST), nombre("senjucore.menu.ender", ChatFormatting.DARK_PURPLE), null);
    }

    public static ItemStack actualizar() {
        return boton(new ItemStack(Items.CLOCK), nombre("senjucore.menu.actualizar", ChatFormatting.GREEN), null);
    }

    /** Cabeza del jugador (perfil con propiedades, para que se vea su skin). */
    public static ItemStack cabeza(GameProfile perfil) {
        ItemStack s = new ItemStack(Items.PLAYER_HEAD);
        s.getOrCreateTag().put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), perfil));
        s.setHoverName(Component.literal(perfil.getName())
                .withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE)));
        return s;
    }

    /** Marcador que sustituye a una Placa de Staff en las vistas (nunca la placa real ni una copia). */
    public static ItemStack marcadorPlaca() {
        return boton(new ItemStack(Items.PAPER), nombre("senjucore.menu.placa", ChatFormatting.GRAY), null);
    }

    /** Ítem de la lista de prohibidos (tipo conocido, nunca la Placa): nombre natural + "Clic para permitir de nuevo". */
    public static ItemStack listado(Item item) {
        return boton(new ItemStack(item), null, "senjucore.menu.quitar");
    }

    /** ID de la lista que no corresponde a un ítem mostrable (quitado del registro, mal formado o la Placa). */
    public static ItemStack listadoDesconocido(String id) {
        return boton(new ItemStack(Items.BARRIER),
                Component.literal(id).withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.RED)),
                "senjucore.menu.quitar");
    }
}
