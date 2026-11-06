package com.senju.core;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SenjuCore.MODID)
public class SenjuCore {
    public static final String MODID = "senjucore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SenjuCore() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOGGER.info("Senju Core cargado ({})", modBus != null);
    }
}
