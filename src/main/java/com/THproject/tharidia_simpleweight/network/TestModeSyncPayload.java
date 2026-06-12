package com.THproject.tharidia_simpleweight.network;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells the client whether the local player has /weight testmode enabled,
 * so client-side movement debuffs (swim-up / jump blocking) can apply to
 * OP players that opted into test mode.
 */
public record TestModeSyncPayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<TestModeSyncPayload> TYPE =
            new Type<>(TharidiaSimpleWeight.modLoc("test_mode_sync"));

    public static final StreamCodec<ByteBuf, TestModeSyncPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(TestModeSyncPayload::new, TestModeSyncPayload::enabled);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
