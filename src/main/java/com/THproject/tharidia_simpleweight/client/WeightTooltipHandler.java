package com.THproject.tharidia_simpleweight.client;

import com.THproject.tharidia_simpleweight.weight.WeightRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client-side handler for adding weight to item tooltips
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class WeightTooltipHandler {
    
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        
        // Get the weight of the item (with the global multiplier applied, so
        // the tooltip matches what the item actually adds to carried weight)
        double weight = WeightRegistry.getItemWeight(stack.getItem()) * WeightRegistry.getWeightMultiplier();
        
        // Add weight info right after the item name (index 1, after the name at index 0)
        if (!event.getToolTip().isEmpty()) {
            String weightText = formatWeight(weight);
            Component weightComponent = Component.literal(weightText)
                .withStyle(ChatFormatting.GRAY);
            
            // Insert after the item name
            event.getToolTip().add(1, weightComponent);
        }
    }
    
    /**
     * Format weight value for display
     */
    private static String formatWeight(double weight) {
        if (weight == 0) {
            return "Weight: Weightless";
        } else if (weight < 1.0) {
            return String.format("Weight: %.1f", weight);
        } else if (weight == (int) weight) {
            return String.format("Weight: %d", (int) weight);
        } else {
            return String.format("Weight: %.1f", weight);
        }
    }
}
