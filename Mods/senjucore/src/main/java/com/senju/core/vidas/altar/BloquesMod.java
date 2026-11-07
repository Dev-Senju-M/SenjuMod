package com.senju.core.vidas.altar;

import com.senju.core.SenjuCore;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro de bloques del mod (y su ítem). */
public final class BloquesMod {
    private BloquesMod() {}

    public static final DeferredRegister<Block> BLOQUES =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SenjuCore.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SenjuCore.MODID);

    public static final RegistryObject<Block> ALTAR_DRACONICO =
            BLOQUES.register("altar_draconico", AltarDraconicoBlock::new);
    public static final RegistryObject<Item> ALTAR_DRACONICO_ITEM =
            ITEMS.register("altar_draconico", () -> new BlockItem(ALTAR_DRACONICO.get(), new Item.Properties()));
}
