package com.oblixorprime.engineersdecorreforged.client;

import com.mojang.math.Axis;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Live components mapped in the retained 1199 x 1312 artwork's source coordinates. */
final class ElectricalFurnaceArtwork {
   private static final int SOURCE_WIDTH = 1199;
   private static final int SOURCE_HEIGHT = 1312;

   // Interior glass only: the original bevels, dark dividers and metal bezel remain visible.
   private static final int[][] ENERGY_CELLS = {
      {493, 527}, {532, 564}, {569, 600}, {606, 638},
      {643, 673}, {678, 708}, {714, 744}, {750, 780}
   };

   // Rotate the complete circular cap, including its original grip. This replaces every
   // painted grip pixel without the old "neutral" patch's residual upright silhouette.
   // The outer brass bezel, four fixed detent marks and mounting plate stay stationary.
   private static final int[][] ROTOR = rotorRuns();
   private static final int GUI11_SOURCE_WIDTH = 1268;
   private static final int GUI11_SOURCE_HEIGHT = 1241;
   private static final int GUI11_PIVOT_X = 988;
   private static final int GUI11_PIVOT_Y = 482;
   private static final int[][] GUI11_ROTOR = rotorRuns(56);

   private ElectricalFurnaceArtwork() {}

   static void render(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                      int width, int height, int energy, int capacity, int speed) {
      graphics.pose().pushPose();
      try {
         graphics.pose().translate(x, y, 0);
         graphics.pose().scale(width / (float)SOURCE_WIDTH, height / (float)SOURCE_HEIGHT, 1);
         for (int index = 0; index < ENERGY_CELLS.length; index++) {
            int left = ENERGY_CELLS[index][0];
            int fill = energyCellPixels(energy, capacity, index, ENERGY_CELLS[index][1] - left);
            if (fill > 0) {
               // Translucent warm glass keeps the retained texture's shading underneath.
               graphics.fillGradient(left, 450, left + fill, 483, 0, 0xD9FFD16A, 0xD9CE7518);
            }
         }
         graphics.flush();

         graphics.pose().translate(964, 462, 0);
         graphics.pose().mulPose(Axis.ZP.rotationDegrees(dialAngleDegrees(speed)));
         for (int[] strip : ROTOR) {
            graphics.blit(texture, strip[0], strip[1],
               (float)(964 + strip[0]), (float)(462 + strip[1]), strip[2], strip[3], SOURCE_WIDTH, SOURCE_HEIGHT);
         }
         // The grip's embedded index mark turns with the handle, including opposite detents.
         graphics.fill(-3, -29, 3, -23, 0xFFF9CE79);
         graphics.flush();
      } finally {
         graphics.pose().popPose();
      }
   }

   /** Rotates the GUI11-R3 dial around the measured cap center without changing hit routing. */
   static void renderGui11V5C(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                              int canvasWidth, int canvasHeight, int speed) {
      graphics.pose().pushPose();
      try {
         graphics.pose().translate(x, y, 0);
         graphics.pose().scale(canvasWidth / (float)GUI11_SOURCE_WIDTH,
            canvasHeight / (float)GUI11_SOURCE_HEIGHT, 1.0F);
         graphics.pose().translate(GUI11_PIVOT_X, GUI11_PIVOT_Y, 0);
         graphics.pose().mulPose(Axis.ZP.rotationDegrees(gui11DialAngleDegrees(speed)));
         for (int[] strip : GUI11_ROTOR) {
            graphics.blit(texture, strip[0], strip[1],
               (float)(GUI11_PIVOT_X + strip[0]), (float)(GUI11_PIVOT_Y + strip[1]),
               strip[2], strip[3], GUI11_SOURCE_WIDTH, GUI11_SOURCE_HEIGHT);
         }
      } finally {
         graphics.pose().popPose();
      }
   }

   private static int[][] rotorRuns() {
      return rotorRuns(38);
   }

   private static int[][] rotorRuns(int radius) {
      ArrayList<int[]> runs = new ArrayList<>();
      for (int y = -radius; y < radius; y++) {
         int halfWidth = (int)Math.floor(Math.sqrt(radius * radius - (y + 0.5) * (y + 0.5)));
         int[] previous = runs.isEmpty() ? null : runs.getLast();
         if (previous != null && previous[2] == halfWidth * 2) previous[3]++;
         else runs.add(new int[]{-halfWidth, y, halfWidth * 2, 1});
      }
      return runs.toArray(int[][]::new);
   }

   static int energyCellPixels(int stored, int capacity, int index, int width) {
      if (stored <= 0 || capacity <= 0 || index < 0 || index >= ENERGY_CELLS.length || width <= 0) return 0;
      long portion = (long)Math.min(stored, capacity) * ENERGY_CELLS.length - (long)index * capacity;
      return (int)(Math.max(0L, Math.min((long)capacity, portion)) * width / capacity);
   }

   static int dialAngleDegrees(int speed) {
      // Original grip points up. Clockwise detents: OFF/down, I/left, II/up, III/right.
      return (180 + 90 * Math.max(0, Math.min(3, speed))) % 360;
   }

   static int gui11DialAngleDegrees(int speed) {
      // The GUI11 artwork's unrotated cap points left (mode I). Clockwise states are OFF/down, I/left, II/up, III/right.
      return (270 + 90 * Math.max(0, Math.min(3, speed))) % 360;
   }

   static boolean gui11HeatWaveVisible(int residualHeat, int actualDraw) {
      return residualHeat > 0 || actualDraw > 0;
   }

   static boolean gui11CopperReflectionVisible(int actualDraw) {
      return actualDraw > 0;
   }

   static boolean containsDial(double x, double y) {
      return x >= 168 && x < 201 && y >= 74 && y < 103;
   }

   static int clickStep(int button) {
      return button == 0 ? 1 : button == 1 ? -1 : 0;
   }
}
