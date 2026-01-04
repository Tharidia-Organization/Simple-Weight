package com.THproject.tharidia_simpleweight;

import com.THproject.tharidia_simpleweight.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TharidiaSimpleWeight.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = TharidiaSimpleWeight.MODID, value = Dist.CLIENT)
public class TharidiaSimpleWeightClient {

    public TharidiaSimpleWeightClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        
        TharidiaSimpleWeight.LOGGER.info("Tharidia Simple Weight - Client initialized");
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Register the weight HUD overlay in the lower left corner
        event.registerAbove(
            VanillaGuiLayers.HOTBAR,
            TharidiaSimpleWeight.modLoc("weight_overlay"),
            new WeightHudOverlay()
        );
    }
}
