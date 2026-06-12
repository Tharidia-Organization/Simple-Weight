package com.THproject.tharidia_simpleweight.weight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Represents the weight configuration data loaded from datapacks
 */
public class WeightData {
    public static final Codec<WeightData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("item_weights").forGetter(d -> d.itemWeights),
            WeightThresholds.CODEC.fieldOf("thresholds").forGetter(d -> d.thresholds),
            WeightDebuffs.CODEC.fieldOf("debuffs").forGetter(d -> d.debuffs),
            Codec.DOUBLE.optionalFieldOf("weight_multiplier", 1.0).forGetter(d -> d.weightMultiplier),
            Codec.BOOL.optionalFieldOf("smooth_transition", false).forGetter(d -> d.smoothTransition),
            HudConfig.CODEC.optionalFieldOf("hud", HudConfig.DEFAULT).forGetter(d -> d.hudConfig)
        ).apply(instance, WeightData::new)
    );

    private final Map<String, Double> itemWeights;
    private final WeightThresholds thresholds;
    private final WeightDebuffs debuffs;
    private final double weightMultiplier;
    private final boolean smoothTransition;
    private final HudConfig hudConfig;

    public WeightData(Map<String, Double> itemWeights, WeightThresholds thresholds, WeightDebuffs debuffs, double weightMultiplier) {
        this(itemWeights, thresholds, debuffs, weightMultiplier, false, HudConfig.DEFAULT);
    }

    public WeightData(Map<String, Double> itemWeights, WeightThresholds thresholds, WeightDebuffs debuffs, double weightMultiplier, boolean smoothTransition) {
        this(itemWeights, thresholds, debuffs, weightMultiplier, smoothTransition, HudConfig.DEFAULT);
    }

    public WeightData(Map<String, Double> itemWeights, WeightThresholds thresholds, WeightDebuffs debuffs, double weightMultiplier, boolean smoothTransition, HudConfig hudConfig) {
        this.itemWeights = itemWeights;
        this.thresholds = thresholds;
        this.debuffs = debuffs;
        this.weightMultiplier = weightMultiplier;
        this.smoothTransition = smoothTransition;
        this.hudConfig = hudConfig;
    }

    public HudConfig getHudConfig() {
        return hudConfig;
    }
    
    public double getItemWeight(ResourceLocation itemId) {
        return itemWeights.getOrDefault(itemId.toString(), 1.0);
    }
    
    public Map<String, Double> getItemWeights() {
        return itemWeights;
    }
    
    public WeightThresholds getThresholds() {
        return thresholds;
    }
    
    public WeightDebuffs getDebuffs() {
        return debuffs;
    }

    public double getWeightMultiplier() {
        return weightMultiplier;
    }

    public boolean isSmoothTransition() {
        return smoothTransition;
    }

    /**
     * Gets the movement speed multiplier for a given total weight.
     * If smooth_transition is enabled, linearly interpolates between
     * the multipliers of adjacent thresholds instead of jumping
     * instantly between the 4 weight steps.
     */
    public double getSpeedMultiplier(double weight) {
        if (!smoothTransition) {
            return debuffs.getSpeedMultiplier(thresholds.getStatus(weight));
        }

        double light = thresholds.getLight();
        double medium = thresholds.getMedium();
        double heavy = thresholds.getHeavy();
        double overencumbered = thresholds.getOverencumbered();

        double normalMult = 1.0;
        double lightMult = debuffs.getSpeedMultiplier(WeightStatus.LIGHT);
        double mediumMult = debuffs.getSpeedMultiplier(WeightStatus.MEDIUM);
        double heavyMult = debuffs.getSpeedMultiplier(WeightStatus.HEAVY);
        double overMult = debuffs.getSpeedMultiplier(WeightStatus.OVERENCUMBERED);

        if (weight <= 0) return normalMult;
        if (weight < light) return lerp(weight, 0, light, normalMult, lightMult);
        if (weight < medium) return lerp(weight, light, medium, lightMult, mediumMult);
        if (weight < heavy) return lerp(weight, medium, heavy, mediumMult, heavyMult);
        if (weight < overencumbered) return lerp(weight, heavy, overencumbered, heavyMult, overMult);
        return overMult;
    }

    private static double lerp(double weight, double rangeStart, double rangeEnd, double startValue, double endValue) {
        if (rangeEnd <= rangeStart) {
            return endValue;
        }
        double t = (weight - rangeStart) / (rangeEnd - rangeStart);
        return startValue + t * (endValue - startValue);
    }

    /**
     * Weight thresholds for different status levels
     */
    public static class WeightThresholds {
        public static final Codec<WeightThresholds> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("light").forGetter(t -> t.light),
                Codec.DOUBLE.fieldOf("medium").forGetter(t -> t.medium),
                Codec.DOUBLE.fieldOf("heavy").forGetter(t -> t.heavy),
                Codec.DOUBLE.fieldOf("overencumbered").forGetter(t -> t.overencumbered)
            ).apply(instance, WeightThresholds::new)
        );
        
        private final double light;      // 0-light: Normal (green)
        private final double medium;     // light-medium: Slightly encumbered (yellow)
        private final double heavy;      // medium-heavy: Encumbered (orange)
        private final double overencumbered; // heavy+: Overencumbered (red)
        
        public WeightThresholds(double light, double medium, double heavy, double overencumbered) {
            this.light = light;
            this.medium = medium;
            this.heavy = heavy;
            this.overencumbered = overencumbered;
        }
        
        public double getLight() { return light; }
        public double getMedium() { return medium; }
        public double getHeavy() { return heavy; }
        public double getOverencumbered() { return overencumbered; }
        
        /**
         * Determines the weight status based on current weight
         */
        public WeightStatus getStatus(double weight) {
            if (weight >= overencumbered) return WeightStatus.OVERENCUMBERED;
            if (weight >= heavy) return WeightStatus.HEAVY;
            if (weight >= medium) return WeightStatus.MEDIUM;
            if (weight >= light) return WeightStatus.LIGHT;
            return WeightStatus.NORMAL;
        }
    }
    
    /**
     * Debuff configuration for different weight levels
     */
    public static class WeightDebuffs {
        public static final Codec<WeightDebuffs> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("light_speed_multiplier").forGetter(d -> d.lightSpeedMultiplier),
                Codec.DOUBLE.fieldOf("medium_speed_multiplier").forGetter(d -> d.mediumSpeedMultiplier),
                Codec.DOUBLE.fieldOf("heavy_speed_multiplier").forGetter(d -> d.heavySpeedMultiplier),
                Codec.DOUBLE.fieldOf("overencumbered_speed_multiplier").forGetter(d -> d.overencumberedSpeedMultiplier),
                Codec.BOOL.fieldOf("heavy_disable_swim_up").forGetter(d -> d.heavyDisableSwimUp),
                Codec.BOOL.fieldOf("overencumbered_disable_swim_up").forGetter(d -> d.overencumberedDisableSwimUp),
                Codec.BOOL.optionalFieldOf("heavy_disable_jump", false).forGetter(d -> d.heavyDisableJump),
                Codec.BOOL.optionalFieldOf("overencumbered_disable_jump", false).forGetter(d -> d.overencumberedDisableJump)
            ).apply(instance, WeightDebuffs::new)
        );

        private final double lightSpeedMultiplier;
        private final double mediumSpeedMultiplier;
        private final double heavySpeedMultiplier;
        private final double overencumberedSpeedMultiplier;
        private final boolean heavyDisableSwimUp;
        private final boolean overencumberedDisableSwimUp;
        private final boolean heavyDisableJump;
        private final boolean overencumberedDisableJump;

        public WeightDebuffs(double lightSpeedMultiplier, double mediumSpeedMultiplier,
                           double heavySpeedMultiplier, double overencumberedSpeedMultiplier,
                           boolean heavyDisableSwimUp, boolean overencumberedDisableSwimUp) {
            this(lightSpeedMultiplier, mediumSpeedMultiplier, heavySpeedMultiplier, overencumberedSpeedMultiplier,
                heavyDisableSwimUp, overencumberedDisableSwimUp, false, false);
        }

        public WeightDebuffs(double lightSpeedMultiplier, double mediumSpeedMultiplier,
                           double heavySpeedMultiplier, double overencumberedSpeedMultiplier,
                           boolean heavyDisableSwimUp, boolean overencumberedDisableSwimUp,
                           boolean heavyDisableJump, boolean overencumberedDisableJump) {
            this.lightSpeedMultiplier = lightSpeedMultiplier;
            this.mediumSpeedMultiplier = mediumSpeedMultiplier;
            this.heavySpeedMultiplier = heavySpeedMultiplier;
            this.overencumberedSpeedMultiplier = overencumberedSpeedMultiplier;
            this.heavyDisableSwimUp = heavyDisableSwimUp;
            this.overencumberedDisableSwimUp = overencumberedDisableSwimUp;
            this.heavyDisableJump = heavyDisableJump;
            this.overencumberedDisableJump = overencumberedDisableJump;
        }
        
        public double getLightSpeedMultiplier() { return lightSpeedMultiplier; }
        public double getMediumSpeedMultiplier() { return mediumSpeedMultiplier; }
        public double getHeavySpeedMultiplier() { return heavySpeedMultiplier; }
        public double getOverencumberedSpeedMultiplier() { return overencumberedSpeedMultiplier; }
        public boolean isHeavyDisableSwimUp() { return heavyDisableSwimUp; }
        public boolean isOverencumberedDisableSwimUp() { return overencumberedDisableSwimUp; }
        public boolean isHeavyDisableJump() { return heavyDisableJump; }
        public boolean isOverencumberedDisableJump() { return overencumberedDisableJump; }

        public double getSpeedMultiplier(WeightStatus status) {
            return switch (status) {
                case LIGHT -> lightSpeedMultiplier;
                case MEDIUM -> mediumSpeedMultiplier;
                case HEAVY -> heavySpeedMultiplier;
                case OVERENCUMBERED -> overencumberedSpeedMultiplier;
                default -> 1.0;
            };
        }
        
        public boolean isSwimUpDisabled(WeightStatus status) {
            return switch (status) {
                case HEAVY -> heavyDisableSwimUp;
                case OVERENCUMBERED -> overencumberedDisableSwimUp;
                default -> false;
            };
        }

        public boolean isJumpDisabled(WeightStatus status) {
            return switch (status) {
                case HEAVY -> heavyDisableJump;
                case OVERENCUMBERED -> overencumberedDisableJump;
                default -> false;
            };
        }
    }

    /**
     * Client HUD configuration (anchor corner + pixel offsets), set via datapack
     */
    public static class HudConfig {
        public static final HudConfig DEFAULT = new HudConfig("bottom_left", 0, 0);

        public static final Codec<HudConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.optionalFieldOf("position", "bottom_left").forGetter(h -> h.position),
                Codec.INT.optionalFieldOf("x_offset", 0).forGetter(h -> h.xOffset),
                Codec.INT.optionalFieldOf("y_offset", 0).forGetter(h -> h.yOffset)
            ).apply(instance, HudConfig::new)
        );

        private final String position; // bottom_left, bottom_right, top_left, top_right
        private final int xOffset;
        private final int yOffset;

        public HudConfig(String position, int xOffset, int yOffset) {
            this.position = position;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }

        public String getPosition() { return position; }
        public int getXOffset() { return xOffset; }
        public int getYOffset() { return yOffset; }
    }

    /**
     * Weight status levels
     */
    public enum WeightStatus {
        NORMAL(0x555555),      // Dark gray (blends with the empty bar)
        LIGHT(0xFFFF00),       // Yellow
        MEDIUM(0xFFA500),      // Orange
        HEAVY(0xFF0000),       // Red
        OVERENCUMBERED(0xAA00FF); // Purple
        
        private final int color;
        
        WeightStatus(int color) {
            this.color = color;
        }
        
        public int getColor() {
            return color;
        }
    }
}
