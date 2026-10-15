package com.THproject.tharidia_simpleweight.client;

import com.THproject.tharidia_simpleweight.weight.ContainerWeights;
import com.THproject.tharidia_simpleweight.weight.WeightData;
import com.THproject.tharidia_simpleweight.weight.WeightRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * Client-side handler for adding weight to item tooltips
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class WeightTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (stack.isEmpty() || tooltip.isEmpty()) {
            return;
        }

        // Weights include the global multiplier, so the tooltip matches what
        // the item actually adds to carried weight
        WeightData.DisplayConfig display = WeightRegistry.getDisplayConfig();
        double multiplier = WeightRegistry.getWeightMultiplier();
        double weight = WeightRegistry.getItemWeight(stack.getItem()) * multiplier;

        String weightText = weight == 0 ? "Weightless" : display.format(weight);
        if (stack.getCount() > 1 && weight != 0) {
            weightText += " (x" + stack.getCount() + ": " + display.format(weight * stack.getCount()) + ")";
        }

        // Insert after the item name
        int index = 1;
        tooltip.add(index++, Component.literal("Weight: " + weightText).withStyle(ChatFormatting.GRAY));

        // Things stored inside the item (backpacks, shulker boxes, quivers, packed tents...)
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            double contents = ContainerWeights.contentsWeight(stack, mc.level.registryAccess(), 0) * multiplier;
            if (contents > 0) {
                tooltip.add(index, Component.literal("Contents: " + display.format(contents)).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
