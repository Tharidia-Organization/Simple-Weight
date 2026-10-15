package com.THproject.tharidia_simpleweight.client;

import com.THproject.tharidia_simpleweight.weight.WeightData;
import com.THproject.tharidia_simpleweight.weight.WeightManager;
import com.THproject.tharidia_simpleweight.weight.WeightRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/**
 * HUD overlay that displays the player's current weight status
 * Shows on the left side near the health bar with color-coded indicator
 */
public class WeightHudOverlay implements LayeredDraw.Layer {
    private static final int INDICATOR_WIDTH = 25;
    private static final int INDICATOR_HEIGHT = 35;
    private static final int MARGIN = 3;
    private static final int TEXT_PADDING = 2;
    // The full inventory weight (containers, curios, reflection...) is far too
    // expensive to recompute every frame; refresh it a few times per second.
    private static final int REFRESH_TICKS = 5;

    private double cachedWeight;
    private int cachedTick = -REFRESH_TICKS;
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null || mc.options.hideGui) {
            return;
        }
        
        if (player.tickCount < cachedTick || player.tickCount - cachedTick >= REFRESH_TICKS) {
            cachedWeight = WeightManager.calculatePlayerWeight(player);
            cachedTick = player.tickCount;
        }
        double currentWeight = cachedWeight;
        WeightData.WeightStatus status = WeightRegistry.getWeightStatus(currentWeight);
        WeightData.WeightThresholds thresholds = WeightRegistry.getThresholds();
        
        // Position from datapack hud config: anchor corner + pixel offsets.
        // Offsets always push the indicator INWARD from the chosen corner.
        WeightData.HudConfig hud = WeightRegistry.getHudConfig();

        // Widen the box when the number (decimals from the display config) does not fit
        String weightText = WeightRegistry.getDisplayConfig().formatNumber(currentWeight);
        int width = Math.max(INDICATOR_WIDTH, mc.font.width(weightText) + TEXT_PADDING * 2 + 2);
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        boolean right = hud.getPosition().endsWith("right");
        boolean top = hud.getPosition().startsWith("top");

        int x = right
            ? screenWidth - width - MARGIN - hud.getXOffset()
            : MARGIN + hud.getXOffset();
        int y = top
            ? MARGIN + hud.getYOffset()
            : screenHeight - INDICATOR_HEIGHT - MARGIN - hud.getYOffset();
        
        // Draw background box
        guiGraphics.fill(x, y, x + width, y + INDICATOR_HEIGHT, 0xAA000000);
        
        // Draw colored weight indicator bar (fills from bottom to top)
        int barHeight = (int) (INDICATOR_HEIGHT * getWeightPercentage(currentWeight, thresholds));
        int barY = y + INDICATOR_HEIGHT - barHeight;
        int color = status.getColor() | 0xFF000000; // Add alpha
        guiGraphics.fill(x + 1, barY, x + width - 1, y + INDICATOR_HEIGHT - 1, color);
        
        // Draw border
        drawBorder(guiGraphics, x, y, width, INDICATOR_HEIGHT, 0xFFFFFFFF);
        
        // Draw weight text (centered)
        int textX = x + (width - mc.font.width(weightText)) / 2;
        int textY = y + INDICATOR_HEIGHT / 2 - 4;
        
        // Draw text with shadow
        guiGraphics.drawString(mc.font, weightText, textX + 1, textY + 1, 0xFF000000, false);
        guiGraphics.drawString(mc.font, weightText, textX, textY, 0xFFFFFFFF, false);
        
        // Draw compact status indicator (just a colored dot or small text at top)
        String statusLabel = getStatusLabel(status);
        int labelWidth = mc.font.width(statusLabel);
        int labelX = x + (width - labelWidth) / 2;
        int labelY = y + 2;
        guiGraphics.drawString(mc.font, statusLabel, labelX, labelY, color, false);
    }
    
    /**
     * Calculate weight as percentage of maximum threshold
     */
    private double getWeightPercentage(double weight, WeightData.WeightThresholds thresholds) {
        double max = thresholds.getOverencumbered();
        if (weight >= max) {
            return 1.0;
        }
        return Math.min(weight / max, 1.0);
    }
    
    /**
     * Get compact status label text
     */
    private String getStatusLabel(WeightData.WeightStatus status) {
        return switch (status) {
            case NORMAL -> "•";
            case LIGHT -> "•";
            case MEDIUM -> "••";
            case HEAVY -> "•••";
            case OVERENCUMBERED -> "!";
        };
    }
    
    /**
     * Draw a border around a rectangle
     */
    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Top
        guiGraphics.fill(x, y, x + width, y + 1, color);
        // Bottom
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        // Left
        guiGraphics.fill(x, y, x + 1, y + height, color);
        // Right
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
