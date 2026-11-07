package com.senju.core;

import com.senju.core.config.ClientConfig;
import com.senju.core.config.CommonConfig;
import com.senju.core.network.Red;
import com.senju.core.tiempo.ControlTiempo;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SenjuCore.MODID)
public class SenjuCore {
    public static final String MODID = "senjucore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SenjuCore() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "senjucore-common.toml");
        ctx.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "senjucore-client.toml");
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Red::registrar));
        com.senju.core.staff.ItemsMod.ITEMS.register(modBus);
        com.senju.core.vidas.altar.BloquesMod.BLOQUES.register(modBus);
        com.senju.core.vidas.altar.BloquesMod.ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.OP_BLOCKS && e.hasPermissions())
                e.accept(com.senju.core.staff.ItemsMod.PLACA_STAFF);
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
                e.accept(com.senju.core.vidas.altar.BloquesMod.ALTAR_DRACONICO_ITEM);
        });
        MinecraftForge.EVENT_BUS.register(new com.senju.core.staff.ComandoStaff());
        MinecraftForge.EVENT_BUS.register(new com.senju.core.staff.ProteccionPlaca());
        MinecraftForge.EVENT_BUS.register(new com.senju.core.staff.prohibidos.Confiscacion());
        MinecraftForge.EVENT_BUS.register(new ControlTiempo());
        MinecraftForge.EVENT_BUS.register(new com.senju.core.staff.vanish.Vanish());
        MinecraftForge.EVENT_BUS.register(new com.senju.core.staff.vanish.EventosVanish());
        MinecraftForge.EVENT_BUS.register(new com.senju.core.tiempo.ComandoTiempo());
        com.senju.core.staff.castigos.EfectosCastigos.registrarEfectos();
        MinecraftForge.EVENT_BUS.register(new com.senju.core.staff.castigos.EfectosCastigos());
        com.senju.core.staff.castigos.CongelarYCarcel.registrarEfectos();
        MinecraftForge.EVENT_BUS.register(new com.senju.core.staff.castigos.CongelarYCarcel());
        MinecraftForge.EVENT_BUS.register(new com.senju.core.vidas.EventosVidas());
        LOGGER.info("Senju Core cargado");
    }
}
