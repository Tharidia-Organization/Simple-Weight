package com.THproject.tharidia_things;

import com.THproject.tharidia_things.weight.WeightDataLoader;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(TharidiaThings.MODID)
public class TharidiaThings {
    public static final String MODID = "tharidiathings";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TharidiaThings(IEventBus modEventBus, ModContainer modContainer) {
        // No registrations needed
        // Data loader will be registered via AddReloadListenerEvent
        
        LOGGER.info("Tharidia Things - Weight System Loaded");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new WeightDataLoader());
        LOGGER.info("Weight data loader registered");
    }

}
