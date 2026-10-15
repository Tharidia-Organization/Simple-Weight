package com.THproject.tharidia_simpleweight.event;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import com.THproject.tharidia_simpleweight.network.PlayerWeightSyncPayload;
import com.THproject.tharidia_simpleweight.network.TestModeSyncPayload;
import com.THproject.tharidia_simpleweight.weight.WeightData;
import com.THproject.tharidia_simpleweight.weight.WeightManager;
import com.THproject.tharidia_simpleweight.weight.WeightRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
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

    // Last weight sent to each player's client, to only sync on change
    private static final java.util.Map<java.util.UUID, Double> LAST_SYNCED_WEIGHT = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Once per second: recalculates the player's weight, syncs it to their
     * client (for the HUD and client-side movement checks) and applies the
     * speed debuff. Players are spread over the 20 ticks of each second by
     * UUID so a full server does not recalculate everyone on the same tick.
     */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if ((player.tickCount + (player.getUUID().hashCode() & 0x7FFFFFFF)) % 20 != 0) {
            return;
        }

        // 0 for masters (OPs not in test mode)
        double weight = WeightManager.calculatePlayerWeight(player);
        Double lastSynced = LAST_SYNCED_WEIGHT.get(player.getUUID());
        if (lastSynced == null || Math.abs(lastSynced - weight) > 1.0E-4) {
            LAST_SYNCED_WEIGHT.put(player.getUUID(), weight);
            PacketDistributor.sendToPlayer(player, new PlayerWeightSyncPayload(weight));
        }

        if (WeightManager.isMaster(player)) {
            removeWeightModifier(player);
            return;
        }
        applyWeightDebuffs(player, weight);
    }

    /**
     * Prevents swimming upward when heavy/overencumbered. Called from
     * LivingEntityMixin at the start of travel(), on BOTH sides (the client is
     * authoritative for its own movement), so it sees the vertical speed after
     * every upward push of the tick has been applied: the space-bar swim boost,
     * sprint-swimming while looking up and the previous tick's wall-climb boost.
     * Players wading in shallow water (head above the surface, feet on the
     * bottom or pushing against the bank) can still climb out; staying afloat
     * at the surface of deep water is blocked like any other upward swim.
     */
    public static void limitSwimUp(Player player) {
        if (!player.isInWater()) {
            return;
        }
        if (!player.isEyeInFluid(FluidTags.WATER) && (player.onGround() || player.horizontalCollision)) {
            return;
        }

        var delta = player.getDeltaMovement();
        if (delta.y <= 0) {
            return;
        }

        if (isExemptFromMovementDebuffs(player)) {
            return;
        }

        WeightData.WeightStatus status = WeightManager.getCachedWeightStatus(player);
        if (WeightRegistry.getDebuffs().isSwimUpDisabled(status)) {
            player.setDeltaMovement(delta.x, 0.0, delta.z);
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
            LAST_SYNCED_WEIGHT.remove(serverPlayer.getUUID());
        }
    }

    /**
     * Apply weight-based speed debuffs
     */
    private static void applyWeightDebuffs(Player player, double weight) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        // Remove existing modifier
        removeWeightModifier(player);

        // Calculate speed multiplier based on weight
        double speedMultiplier = WeightRegistry.getSpeedMultiplier(weight);

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
