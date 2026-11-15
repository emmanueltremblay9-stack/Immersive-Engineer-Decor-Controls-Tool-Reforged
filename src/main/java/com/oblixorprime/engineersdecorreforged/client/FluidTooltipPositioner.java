package com.oblixorprime.engineersdecorreforged.client;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/** Keeps the native tooltip's three-pixel padding and one-pixel outer strip on screen. */
final class FluidTooltipPositioner implements ClientTooltipPositioner {
    static final FluidTooltipPositioner INSTANCE = new FluidTooltipPositioner();
    private static final int FRAME_MARGIN = 4;

    private FluidTooltipPositioner() {}

    @Override
    public Vector2ic positionTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY,
                                    int tooltipWidth, int tooltipHeight) {
        Vector2ic nativePosition = DefaultTooltipPositioner.INSTANCE.positionTooltip(
            screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight);
        return new Vector2i(
            clampOrigin(nativePosition.x(), screenWidth, tooltipWidth),
            clampOrigin(nativePosition.y(), screenHeight, tooltipHeight));
    }

    private static int clampOrigin(int nativeOrigin, int viewportSize, int contentSize) {
        return Math.max(FRAME_MARGIN, Math.min(nativeOrigin, viewportSize - contentSize - FRAME_MARGIN));
    }
}
