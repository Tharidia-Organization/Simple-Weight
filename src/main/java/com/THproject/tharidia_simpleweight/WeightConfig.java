package com.THproject.tharidia_simpleweight;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (world/serverconfig/tharidia_simpleweight-server.toml), synced
 * to clients. Lets server owners tune the overall difficulty without writing
 * a datapack.
 */
public final class WeightConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue GLOBAL_WEIGHT_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        GLOBAL_WEIGHT_MULTIPLIER = builder
            .comment("Multiplies every carried weight, on top of the datapack's weight_multiplier.",
                "Below 1.0 makes everything lighter, above 1.0 heavier.")
            .translation("tharidia_simpleweight.configuration.globalWeightMultiplier")
            .defineInRange("globalWeightMultiplier", 1.0, 0.0, 100.0);
        SPEC = builder.build();
    }

    private WeightConfig() {}

    /** The configured multiplier, or 1.0 while the server config is not loaded (e.g. main menu). */
    public static double globalWeightMultiplier() {
        return SPEC.isLoaded() ? GLOBAL_WEIGHT_MULTIPLIER.get() : 1.0;
    }
}
