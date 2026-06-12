package com.THproject.tharidia_simpleweight.event;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import com.THproject.tharidia_simpleweight.network.TestModeSyncPayload;
import com.THproject.tharidia_simpleweight.weight.WeightData;
import com.THproject.tharidia_simpleweight.weight.WeightManager;
import com.THproject.tharidia_simpleweight.weight.WeightRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
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
     * Prevent swimming up when heavy/overencumbered.
     * Runs every tick on BOTH sides: the client is authoritative for its own
     * movement, so blocking only on the server let the space bar still float
     * the player upward. Jumping while standing on the bottom / shoreline is
     * still allowed (onGround check). Cheap thanks to the cached weight status.
     */
    @SubscribeEvent
    public static void onPlayerSwim(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only relevant while actually in water and not standing on the bottom
        if (!player.isInWater() || player.onGround()) {
            return;
        }

        if (isExemptFromMovementDebuffs(player)) {
            return;
        }

        WeightData.WeightStatus status = WeightManager.getCachedWeightStatus(player);
        if (WeightRegistry.getDebuffs().isSwimUpDisabled(status)) {
            // Prevent upward movement in water
            if (player.getDeltaMovement().y > 0) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
            }
        }
    }

    /**
     * Optionally cancel jumps when heavy/overencumbered
     * (heavy_disable_jump / overencumbered_disable_jump in the datapack config).
     * Useful with auto-jump/step-up mods.
     */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (isExemptFromMovementDebuffs(player)) {
            return;
        }

        WeightData.WeightStatus status = WeightManager.getCachedWeightStatus(player);
        if (WeightRegistry.getDebuffs().isJumpDisabled(status)) {
            // The jump event cannot be cancelled, so zero out the vertical boost
            var delta = player.getDeltaMovement();
            player.setDeltaMovement(delta.x, 0.0, delta.z);
        }
    }

    /**
     * Shared exemption check for the per-tick movement debuffs. On the server
     * this respects test mode; on the client (where test mode is unknown) OPs
     * are exempt, the server-side check remains authoritative.
     */
    private static boolean isExemptFromMovementDebuffs(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        if (player.level().isClientSide) {
            return player.hasPermissions(2) && !WeightManager.isClientTestMode();
        }
        return WeightManager.isMaster(player);
    }

    /**
     * Restore the player's persisted /weight testmode preference on login
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            WeightManager.loadTestMode(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer,
                new TestModeSyncPayload(WeightManager.isTestModeEnabled(serverPlayer)));
        }
    }

    /**
     * Remove weight modifiers when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        removeWeightModifier(player);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            WeightManager.clearTestMode(serverPlayer);
        }
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
