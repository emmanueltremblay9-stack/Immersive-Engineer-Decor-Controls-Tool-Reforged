package com.oblixorprime.engineersdecorreforged.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TrackerTooltipClientState {
   private TrackerTooltipClientState() {
   }

   public static boolean helpRequested() {
      return Screen.hasShiftDown();
   }
}
