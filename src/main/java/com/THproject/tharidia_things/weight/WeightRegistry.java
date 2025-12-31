package com.THproject.tharidia_things.weight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry for item weights and weight configuration
 */
public class WeightRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeightRegistry.class);
    
    private static WeightData weightData;
    private static final double DEFAULT_WEIGHT = 1.0;
    
    /**
     * Sets the weight data (called when loading from datapacks)
     */
    public static void setWeightData(WeightData data) {
        weightData = data;
        LOGGER.info("Weight data loaded with {} custom item weights", 
            data.getItemWeights().size());
    }
    
    /**
     * Gets the weight of an item
     */
    public static double getItemWeight(Item item) {
        if (weightData == null) {
            return DEFAULT_WEIGHT;
        }
        
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        return weightData.getItemWeight(itemId);
    }
    
    /**
     * Gets the weight of an item by ResourceLocation
     */
    public static double getItemWeight(ResourceLocation itemId) {
        if (weightData == null) {
            return DEFAULT_WEIGHT;
        }
        return weightData.getItemWeight(itemId);
    }
    
    /**
     * Gets the current weight thresholds
     */
    public static WeightData.WeightThresholds getThresholds() {
        if (weightData == null) {
            // Return default thresholds
            return new WeightData.WeightThresholds(100, 200, 300, 400);
        }
        return weightData.getThresholds();
    }
    
    /**
     * Gets the current weight debuffs configuration
     */
    public static WeightData.WeightDebuffs getDebuffs() {
        if (weightData == null) {
            // Return default debuffs
            return new WeightData.WeightDebuffs(0.95, 0.85, 0.7, 0.5, true, true);
        }
        return weightData.getDebuffs();
    }
    
    /**
     * Gets the weight status for a given weight value
     */
    public static WeightData.WeightStatus getWeightStatus(double weight) {
        return getThresholds().getStatus(weight);
    }
    
    /**
     * Checks if weight data is loaded
     */
    public static boolean isLoaded() {
        return weightData != null;
    }
    
    /**
     * Clears the weight data (for reload)
     */
    public static void clear() {
        weightData = null;
        LOGGER.info("Weight data cleared");
    }
}
