package com.THproject.tharidia_simpleweight.client;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side mod event handlers - Weight only
 */
@EventBusSubscriber(modid = TharidiaSimpleWeight.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TharidiaSimpleWeight.LOGGER.info("Weight client setup complete");
    }
}
