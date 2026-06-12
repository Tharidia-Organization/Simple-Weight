package com.THproject.tharidia_simpleweight.network;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import com.THproject.tharidia_simpleweight.weight.WeightData;
import com.THproject.tharidia_simpleweight.weight.WeightRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;

/**
 * Synchronizes the active weight configuration (item weights, thresholds,
 * debuffs, multiplier and smooth transition flag) from the server to clients,
 * so the client-side HUD and tooltips reflect the datapack-loaded values
 * on dedicated servers.
 */
public record WeightConfigSyncPayload(
        Map<String, Double> itemWeights,
        double light,
        double medium,
        double heavy,
        double overencumbered,
        double lightSpeedMultiplier,
        double mediumSpeedMultiplier,
        double heavySpeedMultiplier,
        double overencumberedSpeedMultiplier,
        boolean heavyDisableSwimUp,
        boolean overencumberedDisableSwimUp,
        boolean heavyDisableJump,
        boolean overencumberedDisableJump,
        double weightMultiplier,
        boolean smoothTransition,
        String hudPosition,
        int hudXOffset,
        int hudYOffset
) implements CustomPacketPayload {

    public static final Type<WeightConfigSyncPayload> TYPE =
            new Type<>(TharidiaSimpleWeight.modLoc("weight_config_sync"));

    private static final StreamCodec<ByteBuf, Map<String, Double>> ITEM_WEIGHTS_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE);

    public static final StreamCodec<ByteBuf, WeightConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ITEM_WEIGHTS_CODEC.encode(buf, payload.itemWeights());
                buf.writeDouble(payload.light());
                buf.writeDouble(payload.medium());
                buf.writeDouble(payload.heavy());
                buf.writeDouble(payload.overencumbered());
                buf.writeDouble(payload.lightSpeedMultiplier());
                buf.writeDouble(payload.mediumSpeedMultiplier());
                buf.writeDouble(payload.heavySpeedMultiplier());
                buf.writeDouble(payload.overencumberedSpeedMultiplier());
                buf.writeBoolean(payload.heavyDisableSwimUp());
                buf.writeBoolean(payload.overencumberedDisableSwimUp());
                buf.writeBoolean(payload.heavyDisableJump());
                buf.writeBoolean(payload.overencumberedDisableJump());
                buf.writeDouble(payload.weightMultiplier());
                buf.writeBoolean(payload.smoothTransition());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.hudPosition());
                buf.writeInt(payload.hudXOffset());
                buf.writeInt(payload.hudYOffset());
            },
            buf -> new WeightConfigSyncPayload(
                    ITEM_WEIGHTS_CODEC.decode(buf),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readDouble(),
                    buf.readBoolean(),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Builds a sync payload from the currently loaded server-side weight configuration.
     */
    public static WeightConfigSyncPayload fromRegistry() {
        WeightData.WeightThresholds thresholds = WeightRegistry.getThresholds();
        WeightData.WeightDebuffs debuffs = WeightRegistry.getDebuffs();
        WeightData.HudConfig hud = WeightRegistry.getHudConfig();

        return new WeightConfigSyncPayload(
                new HashMap<>(WeightRegistry.getItemWeights()),
                thresholds.getLight(),
                thresholds.getMedium(),
                thresholds.getHeavy(),
                thresholds.getOverencumbered(),
                debuffs.getLightSpeedMultiplier(),
                debuffs.getMediumSpeedMultiplier(),
                debuffs.getHeavySpeedMultiplier(),
                debuffs.getOverencumberedSpeedMultiplier(),
                debuffs.isHeavyDisableSwimUp(),
                debuffs.isOverencumberedDisableSwimUp(),
                debuffs.isHeavyDisableJump(),
                debuffs.isOverencumberedDisableJump(),
                WeightRegistry.getWeightMultiplier(),
                WeightRegistry.isSmoothTransition(),
                hud.getPosition(),
                hud.getXOffset(),
                hud.getYOffset()
        );
    }

    /**
     * Rebuilds the WeightData represented by this payload.
     */
    public WeightData toWeightData() {
        WeightData.WeightThresholds thresholds = new WeightData.WeightThresholds(light, medium, heavy, overencumbered);
        WeightData.WeightDebuffs debuffs = new WeightData.WeightDebuffs(
                lightSpeedMultiplier, mediumSpeedMultiplier, heavySpeedMultiplier, overencumberedSpeedMultiplier,
                heavyDisableSwimUp, overencumberedDisableSwimUp, heavyDisableJump, overencumberedDisableJump
        );
        WeightData.HudConfig hud = new WeightData.HudConfig(hudPosition, hudXOffset, hudYOffset);
        return new WeightData(itemWeights, thresholds, debuffs, weightMultiplier, smoothTransition, hud);
    }
}
