package com.THproject.tharidia_simpleweight;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import com.THproject.tharidia_simpleweight.weight.WeightDataLoader;

@Mod(TharidiaSimpleWeight.MODID)
public class TharidiaSimpleWeight {
    public static final String MODID = "tharidia_simpleweight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TharidiaSimpleWeight(IEventBus modEventBus, ModContainer modContainer) {
        // Register this class to receive AddReloadListenerEvent
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
        
        // Register the weight debuff handler
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.THproject.tharidia_simpleweight.event.WeightDebuffHandler.class);

        LOGGER.info("Tharidia Simple Weight - Weight System Loaded");
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new WeightDataLoader());
        LOGGER.info("Weight data loader registered");
    }

    /**
     * Helper method to create a ResourceLocation for this mod
     */
    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
