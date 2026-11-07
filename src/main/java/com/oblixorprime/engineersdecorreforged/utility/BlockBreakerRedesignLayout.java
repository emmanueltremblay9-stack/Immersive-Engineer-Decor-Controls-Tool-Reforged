package com.oblixorprime.engineersdecorreforged.utility;

/** Pure geometry for the unmodified 1254-square Small Block Breaker v3 artwork.
 * Coordinates are GUI pixels relative to the top-left background, never screen pixels.
 * The 256-square mapping is the smallest integer size that contains all 36 full
 * 16-square items with a common Y per row in the measured dark-face rectangles.
 * These bounds establish geometry only, not in-game or gameplay validation.
 */
public final class BlockBreakerRedesignLayout {
   public static final int SOURCE_SIZE = 1254;
   public static final int IMAGE_WIDTH = 256;
   public static final int IMAGE_HEIGHT = 256;
   public static final int SLOT_SIZE = 16;
   public static final int OUTPUT_CONTAINER_INDEX = 0;
   public static final int OUTPUT_X = 207;
   public static final int OUTPUT_Y = 103;
   public static final int HOTBAR_Y = 226;

   // Independently rounded centers, not a constant-pitch approximation.
   private static final int[] COLUMN_X = {34, 55, 77, 99, 120, 142, 164, 185, 207};
   private static final int[] ROW_Y = {161, 180, 200};

   public static final Rect FE = insideSourceRect(943, 253, 1155, 286);
   public static final Rect FLUID = insideSourceRect(943, 357, 1155, 392);
   public static final Rect POWER = insideSourceRect(184, 538, 256, 619);
   public static final Rect OUTPUT_WINDOW = insideSourceRect(966, 443, 1138, 647);
   public static final Rect LEFT_DECORATIVE_WINDOW = insideSourceRect(135, 274, 310, 459);
   public static final Rect CENTRAL_DECORATIVE_SCENE = insideSourceRect(519, 448, 676, 600);
   // The kit labels the frame an estimate; the inset below was visually measured
   // inside its dark center, excluding both yellow/black end caps and metal rim.
   public static final Rect PROGRESS_FRAME = insideSourceRect(332, 663, 971, 735);
   public static final Rect PROGRESS = insideSourceRect(380, 690, 920, 722);

   private BlockBreakerRedesignLayout() {}

   public static int playerSlotX(int column) {
      checkIndex(column, COLUMN_X.length, "column");
      return COLUMN_X[column];
   }

   public static int rowY(int row) {
      checkIndex(row, ROW_Y.length, "row");
      return ROW_Y[row];
   }

   public static int playerSlotY(int row) { return rowY(row); }
   public static int hotbarX(int column) { return playerSlotX(column); }
   public static int hotbarSlotX(int column) { return hotbarX(column); }
   public static int hotbarY() { return HOTBAR_Y; }

   private static void checkIndex(int index, int size, String label) {
      if (index < 0 || index >= size) throw new IllegalArgumentException("Invalid " + label + ": " + index);
   }

   private static Rect insideSourceRect(int x0, int y0, int x1, int y1) {
      int x = (x0 * IMAGE_WIDTH + SOURCE_SIZE - 1) / SOURCE_SIZE;
      int y = (y0 * IMAGE_HEIGHT + SOURCE_SIZE - 1) / SOURCE_SIZE;
      return new Rect(x, y, x1 * IMAGE_WIDTH / SOURCE_SIZE - x, y1 * IMAGE_HEIGHT / SOURCE_SIZE - y);
   }

   /** Half-open rectangle, suitable for drawing or a matching hover hitbox. */
   public record Rect(int x, int y, int width, int height) {
      public int right() { return x + width; }
      public int bottom() { return y + height; }
      public boolean contains(int px, int py) {
         return px >= x && px < right() && py >= y && py < bottom();
      }
   }
}
