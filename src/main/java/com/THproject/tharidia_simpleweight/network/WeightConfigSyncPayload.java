package com.THproject.tharidia_simpleweight.network;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import com.THproject.tharidia_simpleweight.weight.WeightData;
import com.THproject.tharidia_simpleweight.weight.WeightRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Synchronizes the active (tag-resolved) weight configuration from the server
 * to clients, so the client-side HUD, tooltips and movement checks reflect the
 * datapack-loaded values on dedicated servers.
 */
public record WeightConfigSyncPayload(WeightData weightData) implements CustomPacketPayload {

    public static final Type<WeightConfigSyncPayload> TYPE =
            new Type<>(TharidiaSimpleWeight.modLoc("weight_config_sync"));

    public static final StreamCodec<ByteBuf, WeightConfigSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodec(WeightData.CODEC).map(WeightConfigSyncPayload::new, WeightConfigSyncPayload::weightData);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Builds a sync payload from the currently loaded server-side weight configuration.
     */
    public static WeightConfigSyncPayload fromRegistry() {
        return new WeightConfigSyncPayload(WeightRegistry.getWeightData());
    }
}
