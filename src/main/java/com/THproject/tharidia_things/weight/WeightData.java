package com.THproject.tharidia_things.weight;

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
            WeightDebuffs.CODEC.fieldOf("debuffs").forGetter(d -> d.debuffs)
        ).apply(instance, WeightData::new)
    );
    
    private final Map<String, Double> itemWeights;
    private final WeightThresholds thresholds;
    private final WeightDebuffs debuffs;
    
    public WeightData(Map<String, Double> itemWeights, WeightThresholds thresholds, WeightDebuffs debuffs) {
        this.itemWeights = itemWeights;
        this.thresholds = thresholds;
        this.debuffs = debuffs;
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
                Codec.BOOL.fieldOf("overencumbered_disable_swim_up").forGetter(d -> d.overencumberedDisableSwimUp)
            ).apply(instance, WeightDebuffs::new)
        );
        
        private final double lightSpeedMultiplier;
        private final double mediumSpeedMultiplier;
        private final double heavySpeedMultiplier;
        private final double overencumberedSpeedMultiplier;
        private final boolean heavyDisableSwimUp;
        private final boolean overencumberedDisableSwimUp;
        
        public WeightDebuffs(double lightSpeedMultiplier, double mediumSpeedMultiplier,
                           double heavySpeedMultiplier, double overencumberedSpeedMultiplier,
                           boolean heavyDisableSwimUp, boolean overencumberedDisableSwimUp) {
            this.lightSpeedMultiplier = lightSpeedMultiplier;
            this.mediumSpeedMultiplier = mediumSpeedMultiplier;
            this.heavySpeedMultiplier = heavySpeedMultiplier;
            this.overencumberedSpeedMultiplier = overencumberedSpeedMultiplier;
            this.heavyDisableSwimUp = heavyDisableSwimUp;
            this.overencumberedDisableSwimUp = overencumberedDisableSwimUp;
        }
        
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
    }
    
    /**
     * Weight status levels
     */
    public enum WeightStatus {
        NORMAL(0x00FF00),      // Green
        LIGHT(0x90EE90),       // Light green
        MEDIUM(0xFFFF00),      // Yellow
        HEAVY(0xFF8C00),       // Orange
        OVERENCUMBERED(0xFF0000); // Red
        
        private final int color;
        
        WeightStatus(int color) {
            this.color = color;
        }
        
        public int getColor() {
            return color;
        }
    }
}
