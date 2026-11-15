package com.oblixorprime.engineersdecorreforged.utility;

/** Pure geometry for the nine supplied 1254-square milking artwork layers.
 * Source rectangles crop independent sprites; they are not aligned full-canvas overlays.
 * Destination coordinates are GUI-local, before adding the screen origin.
 */
public final class MilkingRedesignLayout {
   public static final int WIDTH = 228;
   public static final int HEIGHT = 228;
   public static final int TEXTURE_SIZE = 1254;
   public static final int SLOT_SIZE = 16;
   public static final int PLAYER_X = 26;
   public static final int PLAYER_Y = 132;
   public static final int SLOT_SPACING = 20;
   public static final int HOTBAR_Y = 195;

   public static final Rect CANVAS = new Rect(0, 0, WIDTH, HEIGHT);
   public static final Rect INPUT_SLOT = new Rect(49, 94, SLOT_SIZE, SLOT_SIZE);
   public static final Rect OUTPUT_SLOT = new Rect(163, 94, SLOT_SIZE, SLOT_SIZE);
   public static final Rect PORTRAIT = new Rect(27, 34, 47, 41);
   public static final Rect TANK = new Rect(104, 49, 22, 51);
   public static final Sprite BACKGROUND = new Sprite(new Rect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE), CANVAS);

   // Small crop margins retain the supplied soft/antialiased edges. Keep original alpha.
   // Portrait sprites are fitted proportionally (rounded to GUI pixels), never stretched to the window.
   public static final Sprite COW = new Sprite(new Rect(293, 278, 687, 694), new Rect(30, 34, 41, 41));
   public static final Sprite MOOSHROOM = new Sprite(new Rect(292, 254, 693, 732), new Rect(31, 34, 39, 41));
   public static final Sprite PORTRAIT_VEIL = new Sprite(new Rect(124, 164, 304, 297), PORTRAIT);
   public static final Sprite MILK = new Sprite(new Rect(479, 228, 296, 802), TANK);
   public static final Sprite CREAM = new Sprite(new Rect(493, 225, 267, 529), TANK);
   public static final Sprite GLASS = new Sprite(new Rect(432, 217, 387, 832), new Rect(102, 47, 26, 55));
   public static final Sprite ENERGY = new Sprite(new Rect(93, 561, 1067, 131), new Rect(163, 44, 41, 7));
   public static final Sprite PROGRESS = new Sprite(new Rect(94, 566, 1067, 124), new Rect(167, 64, 37, 6));

   private MilkingRedesignLayout() {
   }

   public static Rect playerSlot(int column, int row) {
      if (column < 0 || column >= 9 || row < 0 || row >= 3) {
         throw new IllegalArgumentException("Player slot must be in the 9 by 3 inventory");
      }
      return new Rect(PLAYER_X + SLOT_SPACING * column, PLAYER_Y + SLOT_SPACING * row, SLOT_SIZE, SLOT_SIZE);
   }

   public static Rect hotbarSlot(int column) {
      if (column < 0 || column >= 9) {
         throw new IllegalArgumentException("Hotbar column must be between 0 and 8");
      }
      return new Rect(PLAYER_X + SLOT_SPACING * column, HOTBAR_Y, SLOT_SIZE, SLOT_SIZE);
   }

   /** Floor-scaled fill, bounded to [0, extent], including invalid/empty capacity. */
   public static int fillPixels(int amount, int capacity, int extent) {
      if (extent <= 0 || capacity <= 0 || amount <= 0) {
         return 0;
      }
      return (int)((long)Math.min(amount, capacity) * extent / capacity);
   }

   public static Rect bottomFill(Rect area, int amount, int capacity) {
      int height = fillPixels(amount, capacity, area.height());
      return new Rect(area.x(), area.y() + area.height() - height, area.width(), height);
   }

   public static Rect leftFill(Rect area, int amount, int capacity) {
      return new Rect(area.x(), area.y(), fillPixels(amount, capacity, area.width()), area.height());
   }

   /** Half-open rectangle; zero dimensions represent an empty fill. */
   public record Rect(int x, int y, int width, int height) {
      public Rect {
         if (x < 0 || y < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("Rectangle coordinates and dimensions must be nonnegative");
         }
      }
   }

   public record Sprite(Rect source, Rect destination) {
   }
}
