package com.senju.core.staff;

import com.senju.core.SenjuCore;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registro de ítems del mod. */
public final class ItemsMod {
    private ItemsMod() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, SenjuCore.MODID);

    public static final RegistryObject<Item> PLACA_STAFF =
            ITEMS.register("placa_staff", () -> new PlacaStaffItem(
                    new Item.Properties().stacksTo(1).fireResistant()));
}
