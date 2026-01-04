package com.THproject.tharidia_simpleweight.event;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import com.THproject.tharidia_simpleweight.weight.WeightManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles weight-based debuffs for players
 * Optimized to reduce tick impact
 */
public class WeightDebuffHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeightDebuffHandler.class);
    private static final ResourceLocation WEIGHT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TharidiaSimpleWeight.MODID, "weight_speed_penalty");

    // Performance optimization: Check swimming every 5 ticks instead of every tick
    private static final int SWIM_CHECK_INTERVAL = 5;

    // LARGE SERVER OPTIMIZATION: Stagger player processing to distribute load
    // Process only 1/5th of players per tick for weight updates
    private static final int PLAYER_BATCH_SIZE = 5;

    /**
     * Apply speed debuff based on weight
     * LARGE SERVER: Uses player batching to distribute load
     */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only update every 20 ticks (1 second) to reduce performance impact
        if (player.tickCount % 20 != 0) {
            return;
        }

        // LARGE SERVER OPTIMIZATION: Process players in staggered batches
        int playerBatch = Math.abs(player.getUUID().hashCode() % PLAYER_BATCH_SIZE);
        if ((player.tickCount / 20) % PLAYER_BATCH_SIZE != playerBatch) {
            return; // Not this player's turn this second
        }

        // Server-side only
        if (player.level().isClientSide) {
            return;
        }

        // Masters bypass weight system
        if (WeightManager.isMaster(player)) {
            // Remove any existing weight modifiers for masters
            removeWeightModifier(player);
            return;
        }

        applyWeightDebuffs(player);
    }

    /**
     * Prevent swimming up when overencumbered
     * OPTIMIZED: Check only every SWIM_CHECK_INTERVAL ticks with player batching
     */
    @SubscribeEvent
    public static void onPlayerSwim(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only check every SWIM_CHECK_INTERVAL ticks to reduce overhead
        if (player.tickCount % SWIM_CHECK_INTERVAL != 0) {
            return;
        }

        // LARGE SERVER OPTIMIZATION: Batch players for swim checks too
        int playerBatch = Math.abs(player.getUUID().hashCode() % PLAYER_BATCH_SIZE);
        if ((player.tickCount / SWIM_CHECK_INTERVAL) % PLAYER_BATCH_SIZE != playerBatch) {
            return; // Not this player's turn
        }

        if (player.level().isClientSide) {
            return;
        }

        // Masters bypass weight system
        if (WeightManager.isMaster(player)) {
            return;
        }

        // Check if player is in water and trying to swim up
        if (player.isInWater() && WeightManager.isSwimUpDisabled(player)) {
            // Prevent upward movement in water
            if (player.getDeltaMovement().y > 0) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
            }
        }
    }

    /**
     * Remove weight modifiers when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        removeWeightModifier(player);
    }

    /**
     * Apply weight-based speed debuffs
     */
    private static void applyWeightDebuffs(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        // Remove existing modifier
        removeWeightModifier(player);

        // Calculate speed multiplier based on weight
        double speedMultiplier = WeightManager.getSpeedMultiplier(player);

        // Only apply modifier if speed is reduced
        if (speedMultiplier < 1.0) {
            // Create a new modifier (multiplicative)
            double modifierValue = speedMultiplier - 1.0; // Convert to modifier format (-0.05, -0.15, etc.)
            AttributeModifier modifier = new AttributeModifier(
                    WEIGHT_SPEED_MODIFIER_ID,
                    modifierValue,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

            movementSpeed.addPermanentModifier(modifier);
        }
    }

    /**
     * Remove weight modifier from player
     */
    private static void removeWeightModifier(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && movementSpeed.getModifier(WEIGHT_SPEED_MODIFIER_ID) != null) {
            movementSpeed.removeModifier(WEIGHT_SPEED_MODIFIER_ID);
        }
    }
}
