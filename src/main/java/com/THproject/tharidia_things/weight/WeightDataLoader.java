package com.THproject.tharidia_things.weight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Loads weight data from datapacks
 * JSON files should be placed in: data/[namespace]/weight_config/[filename].json
 */
public class WeightDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeightDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "weight_config";
    
    public WeightDataLoader() {
        super(GSON, DIRECTORY);
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading weight configuration from datapacks...");
        
        WeightRegistry.clear();
        
        if (data.isEmpty()) {
            LOGGER.warn("No weight configuration found! Using defaults.");
            loadDefaults();
            return;
        }
        
        // Load the first weight config found (or merge multiple if needed)
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                ResourceLocation location = entry.getKey();
                JsonElement json = entry.getValue();
                
                LOGGER.info("Loading weight config from: {}", location);
                
                // Decode using codec
                WeightData weightData = WeightData.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Failed to parse weight data: {}", error))
                    .orElse(null);
                
                if (weightData != null) {
                    WeightRegistry.setWeightData(weightData);
                    LOGGER.info("Successfully loaded weight configuration from {}", location);
                    return; // Use first valid config
                }
            } catch (Exception e) {
                LOGGER.error("Error loading weight data from {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        // If no valid config was loaded, use defaults
        if (!WeightRegistry.isLoaded()) {
            LOGGER.warn("No valid weight configuration loaded. Using defaults.");
            loadDefaults();
        }
    }
    
    /**
     * Loads default weight configuration
     */
    private void loadDefaults() {
        try {
            // Create default configuration
            Map<String, Double> defaultWeights = Map.of(
                "minecraft:stone", 2.0,
                "minecraft:iron_ingot", 3.0,
                "minecraft:gold_ingot", 4.0,
                "minecraft:diamond", 5.0,
                "minecraft:netherite_ingot", 6.0,
                "minecraft:anvil", 50.0,
                "minecraft:feather", 0.1
            );
            
            WeightData.WeightThresholds thresholds = new WeightData.WeightThresholds(
                100.0,  // light
                200.0,  // medium
                300.0,  // heavy
                400.0   // overencumbered
            );
            
            WeightData.WeightDebuffs debuffs = new WeightData.WeightDebuffs(
                0.95,   // light speed multiplier
                0.85,   // medium speed multiplier
                0.70,   // heavy speed multiplier
                0.50,   // overencumbered speed multiplier
                true,   // heavy disable swim up
                true    // overencumbered disable swim up
            );
            
            WeightData defaultData = new WeightData(defaultWeights, thresholds, debuffs);
            WeightRegistry.setWeightData(defaultData);
            
            LOGGER.info("Loaded default weight configuration");
        } catch (Exception e) {
            LOGGER.error("Failed to load default weight configuration", e);
        }
    }
}
