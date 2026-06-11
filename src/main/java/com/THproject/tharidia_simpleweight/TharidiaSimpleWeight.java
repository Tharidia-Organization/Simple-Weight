package com.THproject.tharidia_simpleweight;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import com.THproject.tharidia_simpleweight.weight.WeightDataLoader;
import com.THproject.tharidia_simpleweight.network.WeightConfigSyncPayload;

@Mod(TharidiaSimpleWeight.MODID)
public class TharidiaSimpleWeight {
    public static final String MODID = "tharidia_simpleweight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TharidiaSimpleWeight(IEventBus modEventBus, ModContainer modContainer) {
        // Register this class to receive AddReloadListenerEvent
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
        
        // Register the weight debuff handler
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.THproject.tharidia_simpleweight.event.WeightDebuffHandler.class);

        // Register the /weight command
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.THproject.tharidia_simpleweight.command.WeightCommand.class);

        // Register network payloads
        modEventBus.addListener(this::registerPayloads);

        LOGGER.info("Tharidia Simple Weight - Weight System Loaded");
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new WeightDataLoader());
        LOGGER.info("Weight data loader registered");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1");
        registrar.playToClient(
                WeightConfigSyncPayload.TYPE,
                WeightConfigSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.THproject.tharidia_simpleweight.weight.WeightRegistry.setWeightData(payload.toWeightData()))
        );
    }

    /**
     * Sends the current weight configuration to clients whenever datapacks are
     * (re)synced - i.e. on player join and on /reload - so the client-side HUD
     * and tooltips reflect the server's datapack values on dedicated servers.
     */
    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        WeightConfigSyncPayload payload = WeightConfigSyncPayload.fromRegistry();
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            PacketDistributor.sendToPlayer(player, payload);
        } else {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }

    /**
     * Helper method to create a ResourceLocation for this mod
     */
    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
