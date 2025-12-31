package com.THproject.tharidia_things.weight;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Manages weight calculations for players
 */
public class WeightManager {
    
    /**
     * Checks if a player should be exempt from weight restrictions
     * Masters (OP level 2+) are exempt from weight
     */
    public static boolean isMaster(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.hasPermissions(2);
        }
        return false;
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
            }
        }
        
        // Armor slots (slots 36-39: boots, leggings, chestplate, helmet)
        for (ItemStack armorStack : inventory.armor) {
            if (!armorStack.isEmpty()) {
                double itemWeight = WeightRegistry.getItemWeight(armorStack.getItem());
                totalWeight += itemWeight * armorStack.getCount();
            }
        }
        
        // Offhand (slot 40)
        ItemStack offhandStack = inventory.offhand.get(0);
        if (!offhandStack.isEmpty()) {
            double itemWeight = WeightRegistry.getItemWeight(offhandStack.getItem());
            totalWeight += itemWeight * offhandStack.getCount();
        }
        
        // Accessories mod slots (if present)
        totalWeight += calculateAccessoriesWeight(player);
        
        return totalWeight;
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
                    }
                }
            }
            
            return accessoriesWeight;
            
        } catch (ClassNotFoundException e) {
            // Accessories mod not present - this is fine
            return 0.0;
        } catch (Exception e) {
            // Log error but don't crash
            TharidiaThings.LOGGER.error("Error calculating accessories weight", e);
            return 0.0;
        }
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
        WeightData.WeightStatus status = getPlayerWeightStatus(player);
        return WeightRegistry.getDebuffs().getSpeedMultiplier(status);
    }
    
    /**
     * Checks if a player should be unable to swim up
     */
    public static boolean isSwimUpDisabled(Player player) {
        WeightData.WeightStatus status = getPlayerWeightStatus(player);
        return WeightRegistry.getDebuffs().isSwimUpDisabled(status);
    }
}
