package com.THproject.tharidia_simpleweight.weight;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
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
     * Calculates the total weight of a player's inventory
     * Returns 0 if player is a master
     * 
     * FIXED: Removed duplicate counting of armor and offhand slots
     * ADDED: Support for Accessories mod slots
     */
    public static double calculatePlayerWeight(Player player) {
        // Masters bypass weight system
        if (isMaster(player)) {
            return 0.0;
        }
        Inventory inventory = player.getInventory();
        double totalWeight = 0.0;
        
        // Main inventory (slots 0-35: hotbar + main inventory)
        // This includes ONLY the main inventory, NOT armor or offhand
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                double itemWeight = WeightRegistry.getItemWeight(stack.getItem());
                totalWeight += itemWeight * stack.getCount();
                totalWeight += calculateBackpackWeight(stack);
            }
        }

        // Armor slots (slots 36-39: boots, leggings, chestplate, helmet)
        for (ItemStack armorStack : inventory.armor) {
            if (!armorStack.isEmpty()) {
                double itemWeight = WeightRegistry.getItemWeight(armorStack.getItem());
                totalWeight += itemWeight * armorStack.getCount();
                totalWeight += calculateBackpackWeight(armorStack);
            }
        }

        // Offhand (slot 40)
        ItemStack offhandStack = inventory.offhand.get(0);
        if (!offhandStack.isEmpty()) {
            double itemWeight = WeightRegistry.getItemWeight(offhandStack.getItem());
            totalWeight += itemWeight * offhandStack.getCount();
            totalWeight += calculateBackpackWeight(offhandStack);
        }

        // Accessories mod slots (if present)
        totalWeight += calculateAccessoriesWeight(player);

        // Curios mod slots (if present)
        totalWeight += calculateCuriosWeight(player);

        // Apply global weight multiplier from datapack config
        totalWeight *= WeightRegistry.getWeightMultiplier();

        return totalWeight;
    }

    /**
     * Calculates the weight of items stored inside a backpack-like item.
     * Works generically with any item that exposes the NeoForge ItemHandler
     * capability on its ItemStack (e.g. Sophisticated Backpacks, L2 Backpack,
     * Simple Backpack), so no hard dependency on those mods is required.
     */
    private static double calculateBackpackWeight(ItemStack stack) {
        IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (handler == null) {
            return 0.0;
        }

        double backpackWeight = 0.0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack contained = handler.getStackInSlot(i);
            if (!contained.isEmpty()) {
                double itemWeight = WeightRegistry.getItemWeight(contained.getItem());
                backpackWeight += itemWeight * contained.getCount();
                // Recurse to support backpacks nested inside other backpacks
                backpackWeight += calculateBackpackWeight(contained);
            }
        }
        return backpackWeight;
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
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        double itemWeight = WeightRegistry.getItemWeight(stack.getItem());
                        accessoriesWeight += itemWeight * stack.getCount();
                        accessoriesWeight += calculateBackpackWeight(stack);
                    }
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
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    double itemWeight = WeightRegistry.getItemWeight(stack.getItem());
                    curiosWeight += itemWeight * stack.getCount();
                    curiosWeight += calculateBackpackWeight(stack);
                }
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
