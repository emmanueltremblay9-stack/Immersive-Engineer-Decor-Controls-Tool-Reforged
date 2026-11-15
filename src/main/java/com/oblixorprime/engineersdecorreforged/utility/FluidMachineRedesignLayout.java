package com.oblixorprime.engineersdecorreforged.utility;

/** GUI-local geometry for the byte-identical retained Funnel and Accumulator V1s.
 * The square mapping preserves aspect ratio. Independently fitted item anchors
 * stay in the original dark cells, including the separate hotbar, at 16x16.
 */
public final class FluidMachineRedesignLayout {
   public static final int SOURCE_SIZE = 1254;
   public static final int SIZE = 256;
   public static final int SLOT_SIZE = 16;
   public static final int HOTBAR_Y = 206;
   private static final int[] COLUMNS = {44, 63, 82, 101, 120, 139, 158, 177, 196};
   private static final int[] ROWS = {138, 156, 175};

   public static final Rect HEADER = new Rect(50, 5, 156, 18);
   public static final Rect FUNNEL_INPUT = new Rect(40, 70, 16, 16);
   public static final Rect FUNNEL_OUTPUT = new Rect(200, 70, 16, 16);
   public static final Rect ACCUMULATOR_INPUT = new Rect(40, 100, 16, 16);
   public static final Rect ACCUMULATOR_OUTPUT = new Rect(200, 100, 16, 16);
   public static final Rect FUNNEL_TANK = new Rect(86, 94, 84, 16);
   public static final Rect ACCUMULATOR_TANK = new Rect(111, 50, 34, 58);
   public static final Rect FUNNEL_OUTLET = new Rect(116, 118, 21, 11);
   public static final Rect ACCUMULATOR_COLLECTION = new Rect(110, 28, 36, 15);

   private FluidMachineRedesignLayout() {}

   public static boolean usesRetainedTexture(String texture) {
      return "small_fluid_funnel_v1/background".equals(texture)
         || "passive_fluid_accumulator_v1/background".equals(texture);
   }

   public static int playerSlotX(int column) {
      if (column < 0 || column >= COLUMNS.length) throw new IllegalArgumentException("Invalid inventory column");
      return COLUMNS[column];
   }

   public static int playerSlotY(int row) {
      if (row < 0 || row >= ROWS.length) throw new IllegalArgumentException("Invalid inventory row");
      return ROWS[row];
   }

   public static int fillHeight(int amount, int capacity, int height) {
      if (amount <= 0 || capacity <= 0 || height <= 0) return 0;
      return (int)((long)Math.min(amount, capacity) * height / capacity);
   }

   /** Clip the fill inside the bevelled glass. Never paint the metal frame. */
   public static int glassInset(Rect tank, int row) {
      if (row < 0 || row >= tank.height()) throw new IllegalArgumentException("Row outside tank");
      int corner = tank.equals(ACCUMULATOR_TANK) ? 3 : 2;
      return Math.max(0, corner - Math.min(row, tank.height() - 1 - row));
   }

   public record Rect(int x, int y, int width, int height) {
      public int right() { return x + width; }
      public int bottom() { return y + height; }
      public boolean contains(int px, int py) {
         return px >= x && px < right() && py >= y && py < bottom();
      }
   }
}
