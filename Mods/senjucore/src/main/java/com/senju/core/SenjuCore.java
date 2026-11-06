package com.senju.core;

import com.senju.core.config.ClientConfig;
import com.senju.core.config.CommonConfig;
import com.senju.core.network.Red;
import com.senju.core.tiempo.ControlTiempo;
import net.minecraftforge.common.MinecraftForge;
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
        MinecraftForge.EVENT_BUS.register(new ControlTiempo());
        LOGGER.info("Senju Core cargado");
    }
}
