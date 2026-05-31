package com.lenin.stalkerhorror;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(StalkerHorrorMod.MODID)
public class StalkerHorrorMod {
    public static final String MODID = "stalker_horror";
    private static final Logger LOGGER = LogUtils.getLogger();

    public StalkerHorrorMod(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Stalker Horror Mod cargado");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Servidor iniciado con Stalker Horror Mod");
    }
}