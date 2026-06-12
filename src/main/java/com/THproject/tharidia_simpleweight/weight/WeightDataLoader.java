package com.THproject.tharidia_simpleweight.weight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads weight data from datapacks
 * JSON files should be placed in: data/[namespace]/weight_config/[filename].json
 */
public class WeightDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeightDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "weight_config";

    // Raw (unexpanded) config kept so tag entries can be (re)resolved once
    // tags are actually bound to the registry. During a datapack reload this
    // listener runs BEFORE tags are bound, so expanding tags here would
    // resolve against stale/empty tag data and items would fall back to 1.0.
    private static WeightData pendingRawData;

    public WeightDataLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading weight configuration from datapacks...");

        WeightRegistry.clear();
        pendingRawData = null;

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
                    pendingRawData = weightData;
                    // Best-effort immediate expansion (uses whatever tags are
                    // currently bound); re-expanded in onTagsUpdated() once
                    // the new tags are available.
                    WeightRegistry.setWeightData(expandTags(weightData));
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
     * Re-expands tag entries against the freshly bound tags. Called from
     * TagsUpdatedEvent, which fires after datapack reload listeners but with
     * the new tags actually bound to the item registry.
     */
    public static void onTagsUpdated() {
        if (pendingRawData != null) {
            WeightRegistry.setWeightData(expandTags(pendingRawData));
            LOGGER.info("Re-resolved weight config tags after tag reload");
        }
    }

    /**
     * Expands "#namespace:path" item tag entries in item_weights into their
     * concrete item ids. Specific item entries always take precedence over
     * tag-derived weights.
     */
    private static WeightData expandTags(WeightData data) {
        Map<String, Double> rawWeights = data.getItemWeights();
        Map<String, Double> resolved = new HashMap<>();

        // Direct item entries first, so they win over tag entries
        for (Map.Entry<String, Double> entry : rawWeights.entrySet()) {
            if (!entry.getKey().startsWith("#")) {
                resolved.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, Double> entry : rawWeights.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("#")) {
                continue;
            }

            ResourceLocation tagId = ResourceLocation.tryParse(key.substring(1));
            if (tagId == null) {
                LOGGER.warn("Invalid item tag '{}' in weight configuration", key);
                continue;
            }

            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            var tag = BuiltInRegistries.ITEM.getTag(tagKey);
            if (tag.isEmpty()) {
                LOGGER.warn("Unknown item tag '{}' in weight configuration", key);
                continue;
            }

            for (Holder<Item> itemHolder : tag.get()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemHolder.value());
                resolved.putIfAbsent(itemId.toString(), entry.getValue());
            }
        }

        return new WeightData(resolved, data.getThresholds(), data.getDebuffs(), data.getWeightMultiplier(), data.isSmoothTransition(), data.getHudConfig());
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
            
            WeightData defaultData = new WeightData(defaultWeights, thresholds, debuffs, 1.0);
            WeightRegistry.setWeightData(defaultData);
            
            LOGGER.info("Loaded default weight configuration");
        } catch (Exception e) {
            LOGGER.error("Failed to load default weight configuration", e);
        }
    }
}
