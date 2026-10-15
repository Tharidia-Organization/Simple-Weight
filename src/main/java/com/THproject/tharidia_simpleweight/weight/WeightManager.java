package com.THproject.tharidia_simpleweight.weight;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages weight calculations for players
 */
public class WeightManager {

    // Players (by UUID) who have opted into "test mode" via /weight testmode
    // While enabled, weight restrictions apply to them even though they are OP
    private static final Set<UUID> TEST_MODE_PLAYERS = ConcurrentHashMap.newKeySet();

    // Key used to persist test mode preference across logout/relog and server restarts
    private static final String TEST_MODE_NBT_KEY = "tharidia_simpleweight_testmode";

    // Client-side mirror of the local player's test mode flag (synced via TestModeSyncPayload)
    private static volatile boolean clientTestMode = false;

    public static void setClientTestMode(boolean enabled) {
        clientTestMode = enabled;
    }

    public static boolean isClientTestMode() {
        return clientTestMode;
    }

    /**
     * Checks if a player should be exempt from weight restrictions
     * Masters (OP level 2+) are exempt from weight, unless they enabled test mode
     */
    public static boolean isMaster(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (TEST_MODE_PLAYERS.contains(serverPlayer.getUUID())) {
                return false;
            }
            return serverPlayer.hasPermissions(2);
        }
        return false;
    }

    /**
     * Toggles test mode for a player, returning the new state.
     * When test mode is enabled, the weight system applies even to OP players.
     */
    public static boolean toggleTestMode(ServerPlayer player) {
        UUID uuid = player.getUUID();
        boolean enabled;
        if (TEST_MODE_PLAYERS.remove(uuid)) {
            enabled = false;
        } else {
            TEST_MODE_PLAYERS.add(uuid);
            enabled = true;
        }
        player.getPersistentData().putBoolean(TEST_MODE_NBT_KEY, enabled);
        return enabled;
    }

    /**
     * Checks whether a player currently has test mode enabled
     */
    public static boolean isTestModeEnabled(ServerPlayer player) {
        return TEST_MODE_PLAYERS.contains(player.getUUID());
    }

    /**
     * Restores test mode state from the player's persistent data on login,
     * so the preference set via /weight testmode survives relogging and
     * server restarts.
     */
    public static void loadTestMode(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(TEST_MODE_NBT_KEY)) {
            TEST_MODE_PLAYERS.add(player.getUUID());
        }
    }

    /**
     * Clears in-memory test mode state (e.g. on logout). The persisted
     * preference is kept so it can be restored on the next login.
     */
    public static void clearTestMode(ServerPlayer player) {
        TEST_MODE_PLAYERS.remove(player.getUUID());
        SERVER_STATUS_CACHE.remove(player.getUUID());
        CLIENT_STATUS_CACHE.remove(player.getUUID());
    }
    
    /**
     * Calculates the total weight carried by a player: main inventory, armor,
     * offhand, Accessories/Curios slots, everything stored inside carried
     * containers (backpacks, shulker boxes, bundles, quivers, packed tents)
     * and whatever the player is holding with Carry On.
     * Returns 0 if the player is a master.
     */
    public static double calculatePlayerWeight(Player player) {
        // Masters bypass weight system
        if (isMaster(player)) {
            return 0.0;
        }
        return calculateCarriedWeight(player);
    }

    /**
     * Total carried weight (with the global multiplier), ignoring the OP bypass.
     */
    public static double calculateCarriedWeight(Player player) {
        Inventory inventory = player.getInventory();
        HolderLookup.Provider registries = player.registryAccess();
        double totalWeight = 0.0;

        // Main inventory (slots 0-35: hotbar + main inventory)
        for (int i = 0; i < 36; i++) {
            totalWeight += getStackWeight(inventory.getItem(i), registries);
        }

        // Armor slots (boots, leggings, chestplate, helmet)
        for (ItemStack armorStack : inventory.armor) {
            totalWeight += getStackWeight(armorStack, registries);
        }

        // Offhand
        totalWeight += getStackWeight(inventory.offhand.get(0), registries);

        // Accessories mod slots (if present)
        totalWeight += calculateAccessoriesWeight(player);

        // Curios mod slots (if present)
        totalWeight += calculateCuriosWeight(player);

        // Block or entity carried with the Carry On mod (if present)
        totalWeight += ContainerWeights.carryOnWeight(player, registries);

        // Apply global weight multiplier from datapack config
        totalWeight *= WeightRegistry.getWeightMultiplier();

        return totalWeight;
    }

    /**
     * Weight of a whole stack: the item weight times the count, plus the
     * weight of everything stored inside it. The global multiplier is NOT applied.
     */
    public static double getStackWeight(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        return WeightRegistry.getItemWeight(stack.getItem()) * stack.getCount()
            + ContainerWeights.contentsWeight(stack, registries, 0);
    }

    /**
     * Calculates weight from Accessories mod slots
     * Uses reflection to avoid hard dependency on Accessories mod
     */
    private static double calculateAccessoriesWeight(Player player) {
        try {
            // Try to get AccessoriesCapability from the player
            Class<?> accessoriesCapabilityClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            java.lang.reflect.Method getMethod = accessoriesCapabilityClass.getMethod("get", net.minecraft.world.entity.LivingEntity.class);
            Object capability = getMethod.invoke(null, player);
            
            if (capability == null) {
                return 0.0;
            }
            
            // Get the accessories container
            java.lang.reflect.Method getContainerMethod = capability.getClass().getMethod("getContainers");
            Object containers = getContainerMethod.invoke(capability);
            
            if (!(containers instanceof java.util.Map)) {
                return 0.0;
            }
            
            double accessoriesWeight = 0.0;
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> containerMap = (java.util.Map<String, Object>) containers;
            
            // Iterate through all accessory containers
            for (Object container : containerMap.values()) {
                // Get accessories from container
                java.lang.reflect.Method getAccessoriesMethod = container.getClass().getMethod("getAccessories");
                Object accessories = getAccessoriesMethod.invoke(container);
                
                if (!(accessories instanceof net.neoforged.neoforge.items.IItemHandlerModifiable)) {
                    continue;
                }
                
                net.neoforged.neoforge.items.IItemHandlerModifiable handler = 
                    (net.neoforged.neoforge.items.IItemHandlerModifiable) accessories;
                
                // Calculate weight for all items in this container
                for (int i = 0; i < handler.getSlots(); i++) {
                    accessoriesWeight += getStackWeight(handler.getStackInSlot(i), player.registryAccess());
                }
            }
            
            return accessoriesWeight;
            
        } catch (ClassNotFoundException e) {
            // Accessories mod not present - this is fine
            return 0.0;
        } catch (Exception e) {
            // Log error but don't crash
            TharidiaSimpleWeight.LOGGER.error("Error calculating accessories weight", e);
            return 0.0;
        }
    }
    
    /**
     * Calculates weight from Curios mod slots (e.g. backpacks equipped in a
     * curio "back" slot). Uses reflection to avoid a hard dependency.
     */
    private static double calculateCuriosWeight(Player player) {
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            java.lang.reflect.Method getInventoryMethod =
                curiosApiClass.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optionalInventory = getInventoryMethod.invoke(null, player);

            if (!(optionalInventory instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
                return 0.0;
            }

            Object curiosInventory = optional.get();
            java.lang.reflect.Method getEquippedMethod = curiosInventory.getClass().getMethod("getEquippedCurios");
            Object equipped = getEquippedMethod.invoke(curiosInventory);

            if (!(equipped instanceof IItemHandler handler)) {
                return 0.0;
            }

            double curiosWeight = 0.0;
            for (int i = 0; i < handler.getSlots(); i++) {
                curiosWeight += getStackWeight(handler.getStackInSlot(i), player.registryAccess());
            }
            return curiosWeight;

        } catch (ClassNotFoundException e) {
            // Curios mod not present - this is fine
            return 0.0;
        } catch (Exception e) {
            TharidiaSimpleWeight.LOGGER.error("Error calculating curios weight", e);
            return 0.0;
        }
    }

    // Per-player weight status cache so movement checks can run every tick
    // without recalculating the full inventory weight each time.
    private static final java.util.Map<UUID, CachedStatus> SERVER_STATUS_CACHE = new ConcurrentHashMap<>();
    private static final java.util.Map<UUID, CachedStatus> CLIENT_STATUS_CACHE = new ConcurrentHashMap<>();
    private static final int STATUS_CACHE_TICKS = 20;

    private record CachedStatus(WeightData.WeightStatus status, int tick) {}

    /**
     * Gets the player's weight status, recalculated at most once per second.
     * Safe to call every tick on both client and server.
     */
    public static WeightData.WeightStatus getCachedWeightStatus(Player player) {
        java.util.Map<UUID, CachedStatus> cache =
            player.level().isClientSide ? CLIENT_STATUS_CACHE : SERVER_STATUS_CACHE;
        UUID uuid = player.getUUID();
        CachedStatus cached = cache.get(uuid);
        if (cached != null
                && player.tickCount >= cached.tick()
                && player.tickCount - cached.tick() < STATUS_CACHE_TICKS) {
            return cached.status();
        }
        WeightData.WeightStatus status = getPlayerWeightStatus(player);
        cache.put(uuid, new CachedStatus(status, player.tickCount));
        return status;
    }

    /**
     * Gets the weight status for a player
     */
    public static WeightData.WeightStatus getPlayerWeightStatus(Player player) {
        double weight = calculatePlayerWeight(player);
        return WeightRegistry.getWeightStatus(weight);
    }
    
    /**
     * Gets the speed multiplier for a player based on their weight
     */
    public static double getSpeedMultiplier(Player player) {
        double weight = calculatePlayerWeight(player);
        return WeightRegistry.getSpeedMultiplier(weight);
    }
    
    /**
     * Checks if a player should be unable to swim up
     */
    public static boolean isSwimUpDisabled(Player player) {
        WeightData.WeightStatus status = getPlayerWeightStatus(player);
        return WeightRegistry.getDebuffs().isSwimUpDisabled(status);
    }
}
