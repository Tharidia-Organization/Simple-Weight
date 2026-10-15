package com.THproject.tharidia_simpleweight.network;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sends the local player's carried weight as computed by the server. Some
 * contents only exist server-side (e.g. Sophisticated Backpacks storage), so
 * the client cannot compute the full weight on its own.
 */
public record PlayerWeightSyncPayload(double weight) implements CustomPacketPayload {

    public static final Type<PlayerWeightSyncPayload> TYPE =
            new Type<>(TharidiaSimpleWeight.modLoc("player_weight_sync"));

    public static final StreamCodec<ByteBuf, PlayerWeightSyncPayload> STREAM_CODEC =
            ByteBufCodecs.DOUBLE.map(PlayerWeightSyncPayload::new, PlayerWeightSyncPayload::weight);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
