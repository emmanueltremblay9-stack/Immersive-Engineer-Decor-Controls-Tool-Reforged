package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.*;
import org.junit.Test;

public class BlockBreakerRedesignLayoutTest {
   // Exact measured dark-face xywh from the v3 kit manifest, not the layout constants.
   private static final int[][][] FACES = {
      {{158, 786, 90, 82}, {264, 787, 91, 81}, {371, 784, 91, 84}, {476, 784, 92, 84}, {583, 787, 91, 81}, {689, 786, 91, 82}, {795, 784, 91, 84}, {901, 786, 90, 82}, {1007, 787, 91, 81}},
      {{159, 880, 91, 83}, {265, 880, 90, 83}, {371, 881, 91, 82}, {477, 880, 91, 83}, {583, 881, 91, 82}, {689, 880, 91, 83}, {795, 880, 91, 83}, {900, 880, 92, 83}, {1007, 881, 91, 82}},
      {{159, 975, 91, 84}, {265, 976, 90, 83}, {371, 976, 91, 83}, {477, 976, 91, 83}, {583, 976, 91, 83}, {689, 976, 91, 83}, {795, 976, 91, 83}, {901, 976, 90, 83}, {1007, 977, 91, 82}},
      {{159, 1106, 91, 84}, {266, 1106, 89, 84}, {371, 1106, 91, 84}, {477, 1106, 91, 85}, {583, 1107, 91, 83}, {689, 1106, 91, 84}, {795, 1106, 91, 84}, {900, 1106, 91, 84}, {1006, 1106, 92, 85}},
   };

   @Test
   public void all36VanillaItemsFitMeasuredRecessesWithoutOverlap() {
      int count = 0;
      for (int row = 0; row < 4; row++) {
         for (int column = 0; column < 9; column++) {
            int x = row == 3 ? BlockBreakerRedesignLayout.hotbarX(column) : BlockBreakerRedesignLayout.playerSlotX(column);
            int y = row == 3 ? BlockBreakerRedesignLayout.hotbarY() : BlockBreakerRedesignLayout.rowY(row);
            int[] face = FACES[row][column];
            // Integer cross-products avoid rounding away a partially crossed rim.
            assertTrue(x * 1254 >= face[0] * 256);
            assertTrue(y * 1254 >= face[1] * 256);
            assertTrue((x + 16) * 1254 <= (face[0] + face[2]) * 256);
            assertTrue((y + 16) * 1254 <= (face[1] + face[3]) * 256);
            if (column < 8) assertTrue(x + 16 <= BlockBreakerRedesignLayout.playerSlotX(column + 1));
            if (row < 2) assertTrue(y + 16 <= BlockBreakerRedesignLayout.rowY(row + 1));
            count++;
         }
      }
      assertEquals(36, count);
      assertEquals(256, BlockBreakerRedesignLayout.IMAGE_WIDTH);
      assertEquals(BlockBreakerRedesignLayout.IMAGE_WIDTH, BlockBreakerRedesignLayout.IMAGE_HEIGHT);
      assertEquals(16, BlockBreakerRedesignLayout.SLOT_SIZE);
   }

   @Test
   public void smallerSquareCannotFitAllRowsOnIntegerCoordinates() {
      for (int size = 1; size < 256; size++) {
         boolean allFit = true;
         for (int[][] row : FACES) {
            int minY = 0;
            int maxY = Integer.MAX_VALUE;
            for (int[] face : row) {
               minY = Math.max(minY, (face[1] * size + 1253) / 1254);
               maxY = Math.min(maxY, (face[1] + face[3]) * size / 1254 - 16);
            }
            allFit &= minY <= maxY;
         }
         assertFalse("Unexpected fit at " + size, allFit);
      }
   }

   @Test
   public void outputIsInRightWindowAndNeverInLeftDecor() {
      assertEquals(0, BlockBreakerRedesignLayout.OUTPUT_CONTAINER_INDEX);
      var window = BlockBreakerRedesignLayout.OUTPUT_WINDOW;
      int x = BlockBreakerRedesignLayout.OUTPUT_X;
      int y = BlockBreakerRedesignLayout.OUTPUT_Y;
      assertTrue(window.contains(x, y));
      assertTrue(window.contains(x + 15, y + 15));
      assertFalse(BlockBreakerRedesignLayout.LEFT_DECORATIVE_WINDOW.contains(x, y));
   }

   @Test
   public void indicatorsHavePositiveBoundedGeometryAndSeparateHitboxes() {
      for (var rect : new BlockBreakerRedesignLayout.Rect[] {
         BlockBreakerRedesignLayout.FE, BlockBreakerRedesignLayout.FLUID,
         BlockBreakerRedesignLayout.POWER, BlockBreakerRedesignLayout.PROGRESS}) {
         assertTrue(rect.width() > 0 && rect.height() > 0);
         assertTrue(rect.x() >= 0 && rect.y() >= 0);
         assertTrue(rect.right() <= 256 && rect.bottom() <= 256);
         assertFalse(rect.contains(rect.right(), rect.y()));
         assertFalse(rect.contains(rect.x(), rect.bottom()));
      }
      assertTrue(BlockBreakerRedesignLayout.FE.bottom() < BlockBreakerRedesignLayout.FLUID.y());
      var frame = BlockBreakerRedesignLayout.PROGRESS_FRAME;
      var fill = BlockBreakerRedesignLayout.PROGRESS;
      assertTrue(frame.contains(fill.x(), fill.y()));
      assertTrue(frame.contains(fill.right() - 1, fill.bottom() - 1));
   }

   @Test
   public void invalidPlayerIndicesFailInsteadOfCreatingPhantomSlots() {
      assertThrows(IllegalArgumentException.class, () -> BlockBreakerRedesignLayout.playerSlotX(-1));
      assertThrows(IllegalArgumentException.class, () -> BlockBreakerRedesignLayout.playerSlotX(9));
      assertThrows(IllegalArgumentException.class, () -> BlockBreakerRedesignLayout.hotbarX(9));
      assertThrows(IllegalArgumentException.class, () -> BlockBreakerRedesignLayout.rowY(-1));
      assertThrows(IllegalArgumentException.class, () -> BlockBreakerRedesignLayout.rowY(3));
   }
}
