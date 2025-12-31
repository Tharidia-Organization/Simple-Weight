package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side mod event handlers - Weight only
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TharidiaThings.LOGGER.info("Weight client setup complete");
    }
}
