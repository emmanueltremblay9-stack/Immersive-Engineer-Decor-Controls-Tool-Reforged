package com.oblixorprime.engineersdecorreforged.utility;

import java.util.List;

/**
 * Exact client geometry for the owner-selected GUI11-R3 V5-Corrigee Factory Study
 * panels. Logical item anchors come from that panel's runtime map; matching source
 * coordinates account for each panel's actual raster-to-canvas ratio. This class
 * changes no inventory or machine semantics.
 */
public final class FactoryStudyGeometry {
   /** Legacy V3 raster dimensions retained for old reference helpers only. */
   @Deprecated public static final int SOURCE_WIDTH = 1261;
   @Deprecated public static final int SOURCE_HEIGHT = 1247;
   @Deprecated public static final int CANVAS_WIDTH = 316;
   @Deprecated public static final int CANVAS_HEIGHT = 312;
   @Deprecated public static final float SOURCE_SCALE = 0.25F;
   public static final int ITEM_SIZE = 16;
   public static final int VIEWPORT_MARGIN = 6;
   private FactoryStudyGeometry() {}

   public static boolean supports(MachineKind kind) {
      return kind == MachineKind.FACTORY_HOPPER || kind == MachineKind.FACTORY_DROPPER
         || kind == MachineKind.SMALL_LAB_FURNACE;
   }

   public static int machineSlotCount(MachineKind kind) {
      return switch (kind) {
         case FACTORY_HOPPER -> 18;
         case FACTORY_DROPPER -> 15;
         case SMALL_LAB_FURNACE -> 11;
         default -> throw new IllegalArgumentException("Unsupported study GUI: " + kind);
      };
   }

   public static String texturePath(MachineKind kind) {
      if (!supports(kind)) throw new IllegalArgumentException("Unsupported study GUI: " + kind);
      return Gui11V5CRuntimeGeometry.texturePath(kind);
   }

   public static int canvasWidth(MachineKind kind) { return Gui11V5CRuntimeGeometry.width(kind); }
   public static int canvasHeight(MachineKind kind) { return Gui11V5CRuntimeGeometry.height(kind); }
   public static int sourceWidth(MachineKind kind) { return Gui11V5CRuntimeGeometry.sourceWidth(kind); }
   public static int sourceHeight(MachineKind kind) { return Gui11V5CRuntimeGeometry.sourceHeight(kind); }

   public static String sourceSha256(MachineKind kind) {
      return switch (kind) {
         case FACTORY_HOPPER -> "83d993acb4cfc23e4ebbd8309881fa8f5f2730ef5f4d5369ef9302455ddd24ef";
         case FACTORY_DROPPER -> "b952405bfc920a27de03aa22577dd7e71a7d14624315c720e256a535a080839e";
         case SMALL_LAB_FURNACE -> "d42e232cf8d21093f34155a879194ce72f06a40451177f3208dadfc0dbd96509";
         default -> throw new IllegalArgumentException("Unsupported study GUI: " + kind);
      };
   }

   private static final List<SlotAnchor> FACTORY_HOPPER_ANCHORS = List.of(
      new SlotAnchor(0, 130, 265, 73, 75, 34, 68),
      new SlotAnchor(1, 225, 265, 72, 76, 57, 68),
      new SlotAnchor(2, 320, 265, 73, 76, 81, 68),
      new SlotAnchor(3, 415, 265, 73, 76, 105, 68),
      new SlotAnchor(4, 510, 265, 72, 75, 128, 68),
      new SlotAnchor(5, 605, 265, 72, 75, 152, 68),
      new SlotAnchor(6, 130, 365, 73, 76, 34, 93),
      new SlotAnchor(7, 225, 365, 72, 76, 57, 93),
      new SlotAnchor(8, 320, 365, 73, 76, 81, 93),
      new SlotAnchor(9, 415, 365, 72, 75, 105, 93),
      new SlotAnchor(10, 510, 365, 72, 75, 128, 93),
      new SlotAnchor(11, 604, 365, 73, 76, 152, 93),
      new SlotAnchor(12, 130, 465, 73, 77, 34, 118),
      new SlotAnchor(13, 225, 465, 73, 77, 57, 118),
      new SlotAnchor(14, 320, 465, 73, 76, 81, 118),
      new SlotAnchor(15, 415, 465, 73, 77, 105, 118),
      new SlotAnchor(16, 510, 466, 72, 76, 128, 118),
      new SlotAnchor(17, 605, 465, 72, 77, 152, 118),
      new SlotAnchor(18, 173, 713, 83, 80, 46, 180),
      new SlotAnchor(19, 278, 713, 83, 80, 72, 180),
      new SlotAnchor(20, 383, 713, 83, 80, 98, 180),
      new SlotAnchor(21, 487, 712, 83, 81, 124, 180),
      new SlotAnchor(22, 592, 712, 83, 81, 150, 180),
      new SlotAnchor(23, 697, 712, 81, 81, 176, 180),
      new SlotAnchor(24, 800, 712, 82, 81, 202, 180),
      new SlotAnchor(25, 904, 713, 82, 80, 228, 180),
      new SlotAnchor(26, 1008, 713, 83, 80, 254, 180),
      new SlotAnchor(27, 173, 815, 84, 80, 46, 206),
      new SlotAnchor(28, 279, 815, 82, 80, 72, 206),
      new SlotAnchor(29, 383, 815, 83, 80, 98, 206),
      new SlotAnchor(30, 487, 813, 83, 82, 124, 205),
      new SlotAnchor(31, 592, 815, 84, 80, 150, 206),
      new SlotAnchor(32, 696, 815, 82, 80, 176, 206),
      new SlotAnchor(33, 800, 815, 82, 80, 202, 206),
      new SlotAnchor(34, 904, 815, 82, 80, 228, 206),
      new SlotAnchor(35, 1008, 815, 83, 80, 254, 206),
      new SlotAnchor(36, 173, 918, 84, 79, 46, 231),
      new SlotAnchor(37, 279, 918, 82, 79, 72, 231),
      new SlotAnchor(38, 383, 918, 83, 79, 98, 231),
      new SlotAnchor(39, 488, 918, 82, 79, 124, 231),
      new SlotAnchor(40, 592, 918, 83, 79, 150, 231),
      new SlotAnchor(41, 697, 918, 81, 79, 176, 231),
      new SlotAnchor(42, 800, 918, 82, 79, 202, 231),
      new SlotAnchor(43, 904, 918, 82, 79, 228, 231),
      new SlotAnchor(44, 1008, 917, 83, 80, 254, 231),
      new SlotAnchor(45, 170, 1065, 82, 81, 45, 268),
      new SlotAnchor(46, 275, 1067, 83, 79, 71, 269),
      new SlotAnchor(47, 381, 1067, 83, 79, 98, 269),
      new SlotAnchor(48, 486, 1067, 84, 79, 124, 269),
      new SlotAnchor(49, 592, 1067, 83, 79, 150, 269),
      new SlotAnchor(50, 697, 1067, 83, 79, 177, 269),
      new SlotAnchor(51, 802, 1067, 84, 79, 203, 269),
      new SlotAnchor(52, 908, 1067, 82, 79, 229, 269),
      new SlotAnchor(53, 1012, 1066, 81, 79, 255, 268)
   );

   private static final List<SlotAnchor> FACTORY_DROPPER_ANCHORS = List.of(
      new SlotAnchor(0, 125, 265, 76, 79, 33, 68),
      new SlotAnchor(1, 224, 265, 76, 79, 57, 68),
      new SlotAnchor(2, 323, 265, 75, 79, 82, 68),
      new SlotAnchor(3, 420, 265, 76, 79, 106, 68),
      new SlotAnchor(4, 519, 266, 76, 78, 131, 68),
      new SlotAnchor(5, 617, 265, 77, 79, 156, 68),
      new SlotAnchor(6, 125, 368, 76, 78, 33, 94),
      new SlotAnchor(7, 224, 368, 76, 78, 57, 94),
      new SlotAnchor(8, 323, 368, 75, 78, 82, 94),
      new SlotAnchor(9, 421, 368, 76, 78, 107, 94),
      new SlotAnchor(10, 519, 368, 78, 78, 131, 94),
      new SlotAnchor(11, 617, 368, 77, 78, 156, 94),
      new SlotAnchor(12, 133, 524, 74, 74, 34, 132),
      new SlotAnchor(13, 333, 524, 74, 74, 84, 132),
      new SlotAnchor(14, 532, 524, 74, 74, 134, 132),
      new SlotAnchor(15, 171, 739, 85, 80, 45, 187),
      new SlotAnchor(16, 277, 739, 84, 80, 72, 187),
      new SlotAnchor(17, 382, 739, 83, 80, 98, 187),
      new SlotAnchor(18, 486, 738, 84, 81, 124, 187),
      new SlotAnchor(19, 591, 739, 84, 80, 150, 187),
      new SlotAnchor(20, 697, 739, 84, 80, 177, 187),
      new SlotAnchor(21, 801, 739, 84, 80, 203, 187),
      new SlotAnchor(22, 907, 739, 84, 80, 229, 187),
      new SlotAnchor(23, 1012, 739, 83, 80, 255, 187),
      new SlotAnchor(24, 171, 842, 85, 79, 45, 212),
      new SlotAnchor(25, 277, 842, 84, 79, 72, 212),
      new SlotAnchor(26, 381, 842, 84, 79, 98, 212),
      new SlotAnchor(27, 485, 842, 84, 79, 124, 212),
      new SlotAnchor(28, 591, 842, 85, 79, 150, 212),
      new SlotAnchor(29, 697, 842, 84, 79, 177, 212),
      new SlotAnchor(30, 801, 842, 84, 79, 203, 212),
      new SlotAnchor(31, 907, 842, 84, 79, 229, 212),
      new SlotAnchor(32, 1012, 842, 84, 79, 255, 212),
      new SlotAnchor(33, 171, 944, 86, 81, 45, 238),
      new SlotAnchor(34, 277, 944, 84, 81, 72, 238),
      new SlotAnchor(35, 381, 944, 84, 81, 98, 238),
      new SlotAnchor(36, 485, 944, 84, 81, 124, 238),
      new SlotAnchor(37, 591, 944, 84, 81, 150, 238),
      new SlotAnchor(38, 697, 944, 84, 81, 177, 238),
      new SlotAnchor(39, 801, 944, 84, 81, 203, 238),
      new SlotAnchor(40, 907, 944, 84, 81, 229, 238),
      new SlotAnchor(41, 1012, 944, 84, 81, 255, 238),
      new SlotAnchor(42, 172, 1097, 84, 78, 45, 276),
      new SlotAnchor(43, 277, 1097, 84, 78, 72, 276),
      new SlotAnchor(44, 382, 1097, 84, 78, 98, 276),
      new SlotAnchor(45, 487, 1097, 83, 78, 124, 276),
      new SlotAnchor(46, 591, 1097, 84, 79, 150, 276),
      new SlotAnchor(47, 697, 1097, 83, 78, 177, 276),
      new SlotAnchor(48, 801, 1097, 84, 79, 203, 276),
      new SlotAnchor(49, 906, 1097, 83, 78, 229, 276),
      new SlotAnchor(50, 1010, 1097, 83, 78, 255, 276)
   );

   private static final List<SlotAnchor> SMALL_LAB_FURNACE_ANCHORS = List.of(
      new SlotAnchor(0, 426, 223, 102, 99, 111, 60),
      new SlotAnchor(1, 426, 428, 104, 105, 111, 112),
      new SlotAnchor(2, 676, 245, 93, 95, 173, 65),
      new SlotAnchor(3, 289, 232, 98, 100, 76, 62),
      new SlotAnchor(4, 165, 232, 99, 100, 46, 62),
      new SlotAnchor(5, 290, 431, 98, 100, 77, 112),
      new SlotAnchor(6, 166, 431, 99, 100, 46, 112),
      new SlotAnchor(7, 791, 245, 93, 95, 201, 65),
      new SlotAnchor(8, 906, 245, 92, 95, 230, 65),
      new SlotAnchor(9, 734, 431, 94, 97, 187, 112),
      new SlotAnchor(10, 855, 431, 96, 97, 218, 112),
      new SlotAnchor(11, 145, 673, 94, 95, 40, 172),
      new SlotAnchor(12, 256, 673, 90, 95, 67, 172),
      new SlotAnchor(13, 369, 673, 89, 95, 95, 172),
      new SlotAnchor(14, 478, 673, 90, 95, 123, 172),
      new SlotAnchor(15, 585, 673, 92, 95, 150, 172),
      new SlotAnchor(16, 694, 673, 90, 95, 177, 172),
      new SlotAnchor(17, 804, 673, 93, 95, 205, 172),
      new SlotAnchor(18, 914, 673, 92, 95, 232, 172),
      new SlotAnchor(19, 1024, 673, 92, 95, 259, 172),
      new SlotAnchor(20, 146, 789, 93, 96, 40, 201),
      new SlotAnchor(21, 256, 789, 92, 95, 67, 201),
      new SlotAnchor(22, 368, 789, 90, 96, 95, 201),
      new SlotAnchor(23, 476, 789, 92, 96, 122, 201),
      new SlotAnchor(24, 585, 789, 92, 95, 150, 201),
      new SlotAnchor(25, 695, 789, 91, 95, 177, 201),
      new SlotAnchor(26, 804, 789, 92, 95, 204, 201),
      new SlotAnchor(27, 914, 792, 91, 92, 232, 201),
      new SlotAnchor(28, 1023, 789, 93, 95, 259, 201),
      new SlotAnchor(29, 146, 906, 93, 94, 40, 230),
      new SlotAnchor(30, 256, 906, 92, 94, 67, 230),
      new SlotAnchor(31, 366, 906, 92, 94, 95, 230),
      new SlotAnchor(32, 476, 906, 92, 94, 122, 230),
      new SlotAnchor(33, 585, 906, 92, 94, 150, 230),
      new SlotAnchor(34, 695, 906, 91, 94, 177, 230),
      new SlotAnchor(35, 804, 906, 92, 94, 204, 230),
      new SlotAnchor(36, 914, 906, 91, 94, 232, 230),
      new SlotAnchor(37, 1023, 906, 91, 94, 259, 230),
      new SlotAnchor(38, 147, 1058, 91, 92, 40, 268),
      new SlotAnchor(39, 256, 1060, 89, 90, 67, 268),
      new SlotAnchor(40, 368, 1059, 90, 91, 95, 268),
      new SlotAnchor(41, 476, 1060, 92, 90, 122, 268),
      new SlotAnchor(42, 586, 1060, 91, 90, 150, 268),
      new SlotAnchor(43, 694, 1058, 92, 92, 177, 268),
      new SlotAnchor(44, 804, 1058, 90, 92, 204, 268),
      new SlotAnchor(45, 917, 1058, 88, 92, 232, 268),
      new SlotAnchor(46, 1024, 1058, 90, 92, 259, 268)
   );

   public static List<SlotAnchor> anchors(MachineKind kind) {
      if (Gui11V5CRuntimeGeometry.supports(kind) && supports(kind)) {
         List<SlotAnchor> mapped = new java.util.ArrayList<>();
         List<Gui11V5CRuntimeGeometry.Point> points = Gui11V5CRuntimeGeometry.slots(kind);
         for (int i = 0; i < points.size(); i++) {
            var point = points.get(i);
            if (point != null) mapped.add(mappedAnchor(kind, i, point));
         }
         return List.copyOf(mapped);
      }
      return switch (kind) {
         case FACTORY_HOPPER -> FACTORY_HOPPER_ANCHORS;
         case FACTORY_DROPPER -> FACTORY_DROPPER_ANCHORS;
         case SMALL_LAB_FURNACE -> SMALL_LAB_FURNACE_ANCHORS;
         default -> throw new IllegalArgumentException("Unsupported study GUI: " + kind);
      };
   }

   public static SlotAnchor anchor(MachineKind kind, int menuIndex) {
      if (supports(kind)) {
         var point = Gui11V5CRuntimeGeometry.slot(kind, menuIndex);
         if (point == null) throw new IllegalArgumentException("No visible anchor for menu index " + menuIndex);
         return mappedAnchor(kind, menuIndex, point);
      }
      var anchors = anchors(kind);
      if (menuIndex < 0 || menuIndex >= anchors.size()) throw new IllegalArgumentException("Invalid menu index " + menuIndex);
      return anchors.get(menuIndex);
   }

   private static SlotAnchor mappedAnchor(MachineKind kind, int menuIndex, Gui11V5CRuntimeGeometry.Point point) {
      int sourceWidth = sourceWidth(kind);
      int sourceHeight = sourceHeight(kind);
      int canvasWidth = canvasWidth(kind);
      int canvasHeight = canvasHeight(kind);
      int sourceX = (int)Math.round((double)point.x() * sourceWidth / canvasWidth);
      int sourceY = (int)Math.round((double)point.y() * sourceHeight / canvasHeight);
      int sourceItemWidth = Math.max(1, (int)Math.round((double)ITEM_SIZE * sourceWidth / canvasWidth));
      int sourceItemHeight = Math.max(1, (int)Math.round((double)ITEM_SIZE * sourceHeight / canvasHeight));
      return new SlotAnchor(menuIndex, sourceX, sourceY, sourceItemWidth, sourceItemHeight, point.x(), point.y());
   }

   public static Rect header(MachineKind kind) {
      if (!supports(kind)) throw new IllegalArgumentException("Unsupported study GUI: " + kind);
      return new Rect(Math.max(12, (canvasWidth(kind) - 180) / 2), 7, Math.min(180, canvasWidth(kind) - 24), 30);
   }

   public enum Control { RANGE, PERIOD, COUNT, MANUAL, REDSTONE, TRIGGER, VELOCITY, DIRECTION, FILTER_GATE }

   public static List<Control> controls(MachineKind kind) {
      return switch (kind) {
         case FACTORY_HOPPER -> List.of(Control.RANGE, Control.PERIOD, Control.COUNT,
            Control.MANUAL, Control.REDSTONE, Control.TRIGGER);
         case FACTORY_DROPPER -> List.of(Control.VELOCITY, Control.DIRECTION, Control.COUNT,
            Control.PERIOD, Control.MANUAL, Control.FILTER_GATE, Control.REDSTONE, Control.TRIGGER);
         case SMALL_LAB_FURNACE -> List.of();
         default -> throw new IllegalArgumentException("Unsupported study GUI: " + kind);
      };
   }

   public static Rect controlBounds(MachineKind kind, Control control) {
      if (supports(kind)) {
         String key = switch (control) {
            case RANGE -> "range"; case PERIOD -> "period"; case COUNT -> "count";
            case MANUAL -> "manual"; case REDSTONE -> "redstone"; case TRIGGER -> "cycle";
            case VELOCITY -> "velocity"; case DIRECTION -> "direction"; case FILTER_GATE -> "filter";
         };
         var rect = Gui11V5CRuntimeGeometry.control(kind, key);
         if (rect == null) throw new IllegalArgumentException("Control is not present: " + control);
         return new Rect(rect.x(), rect.y(), rect.width(), rect.height());
      }
      if (kind == MachineKind.FACTORY_HOPPER) return switch (control) {
         case RANGE -> new Rect(180, 44, 112, 24);
         case PERIOD -> new Rect(180, 69, 112, 22);
         case COUNT -> new Rect(180, 93, 112, 22);
         case MANUAL -> new Rect(187, 118, 28, 27);
         case REDSTONE -> new Rect(224, 118, 29, 27);
         case TRIGGER -> new Rect(261, 118, 28, 27);
         default -> throw new IllegalArgumentException("Control is not present: " + control);
      };
      if (kind == MachineKind.FACTORY_DROPPER) return switch (control) {
         case VELOCITY -> new Rect(184, 43, 28, 80);
         case DIRECTION -> new Rect(222, 49, 51, 37);
         case COUNT -> new Rect(213, 86, 78, 18);
         case PERIOD -> new Rect(213, 104, 78, 17);
         case MANUAL -> new Rect(177, 126, 26, 28);
         case FILTER_GATE -> new Rect(209, 126, 24, 28);
         case REDSTONE -> new Rect(238, 126, 26, 28);
         case TRIGGER -> new Rect(269, 126, 25, 28);
         default -> throw new IllegalArgumentException("Control is not present: " + control);
      };
      throw new IllegalArgumentException("No controls for " + kind);
   }

   public static Rect track(MachineKind kind, Control control) {
      if (supports(kind)) {
         Rect bounds = controlBounds(kind, control);
         int inset = Math.min(4, Math.max(1, Math.min(bounds.width(), bounds.height()) / 4));
         return new Rect(bounds.x() + inset, bounds.y() + inset,
            Math.max(1, bounds.width() - inset * 2), Math.max(1, bounds.height() - inset * 2));
      }
      if (kind == MachineKind.FACTORY_HOPPER) return switch (control) {
         case RANGE -> new Rect(214, 52, 70, 9);
         case PERIOD -> new Rect(214, 75, 70, 10);
         case COUNT -> new Rect(214, 99, 70, 9);
         case MANUAL -> new Rect(194, 125, 14, 13);
         case REDSTONE -> new Rect(229, 125, 19, 12);
         case TRIGGER -> new Rect(266, 125, 17, 12);
         default -> throw new IllegalArgumentException("Control is not present: " + control);
      };
      if (kind == MachineKind.FACTORY_DROPPER) return switch (control) {
         case VELOCITY -> new Rect(193, 51, 11, 64);
         case DIRECTION -> new Rect(229, 55, 38, 27);
         case COUNT -> new Rect(220, 92, 64, 7);
         case PERIOD -> new Rect(220, 110, 64, 6);
         case MANUAL -> new Rect(183, 133, 13, 13);
         case FILTER_GATE -> new Rect(213, 133, 17, 14);
         case REDSTONE -> new Rect(244, 133, 16, 14);
         case TRIGGER -> new Rect(274, 133, 15, 14);
         default -> throw new IllegalArgumentException("Control is not present: " + control);
      };
      throw new IllegalArgumentException("No controls for " + kind);
   }

   public static Control controlAt(MachineKind kind, double x, double y) {
      for (var control : controls(kind)) if (controlBounds(kind, control).contains(x, y)) return control;
      return null;
   }

   public static Rect energy() {
      var component = Gui11V5CRuntimeGeometry.energy(MachineKind.SMALL_LAB_FURNACE);
      return new Rect(component.x() + 3, component.y() + 4,
         Math.max(1, component.width() - 6), Math.max(1, component.height() - 8));
   }
   public static Rect energyBounds() { var r = Gui11V5CRuntimeGeometry.energy(MachineKind.SMALL_LAB_FURNACE); return new Rect(r.x() - 5, r.y() - 4, r.width() + 10, r.height() + 8); }
   public static Rect progress() { return new Rect(130, 68, 18, 16); }
   public static Rect flame() { return new Rect(106, 74, 15, 14); }

   public static Rect filterIndicator(int index, int statusIndex) {
      if (index < 0 || index > 2 || statusIndex < 0 || statusIndex > 2)
         throw new IllegalArgumentException("Filter and status indices must be 0..2");
      var slot = Gui11V5CRuntimeGeometry.slot(MachineKind.FACTORY_DROPPER, 12 + index);
      int y = slot.y() + 10 + statusIndex;
      return new Rect(slot.x() + 3, y, 8, 1);
   }

   public record Rect(int x, int y, int width, int height) {
      public Rect {
         if (width <= 0 || height <= 0) throw new IllegalArgumentException("Rectangle must have positive area");
      }
      public int right() { return x + width; }
      public int bottom() { return y + height; }
      public boolean contains(double localX, double localY) {
         return localX >= x && localX < right() && localY >= y && localY < bottom();
      }
   }

   public record SlotAnchor(int menuIndex, int sourceX, int sourceY, int sourceWidth, int sourceHeight, int x, int y) {
      public double interiorLeft() { return sourceX * 0.25D; }
      public double interiorTop() { return sourceY * 0.25D; }
      public double interiorRight() { return (sourceX + sourceWidth) * 0.25D; }
      public double interiorBottom() { return (sourceY + sourceHeight) * 0.25D; }
      public double hoverLeft() { return Math.max(x - 1.0D, interiorLeft()); }
      public double hoverTop() { return Math.max(y - 1.0D, interiorTop()); }
      public double hoverRight() { return Math.min(x + ITEM_SIZE + 1.0D, interiorRight()); }
      public double hoverBottom() { return Math.min(y + ITEM_SIZE + 1.0D, interiorBottom()); }
      public boolean containsHover(double localX, double localY) {
         return localX >= hoverLeft() && localX < hoverRight()
            && localY >= hoverTop() && localY < hoverBottom();
      }
   }

   public static int tooltipWidth(int viewportWidth) {
      return (int)Math.max(1L, Math.min(240L, (long)viewportWidth - 16L));
   }

   public static ViewTransform fit(int viewportWidth, int viewportHeight) {
      return fit(MachineKind.FACTORY_HOPPER, viewportWidth, viewportHeight);
   }

   public static ViewTransform fit(MachineKind kind, int viewportWidth, int viewportHeight) {
      if (viewportWidth <= 0 || viewportHeight <= 0) throw new IllegalArgumentException("Positive viewport required");
      int canvasWidth = canvasWidth(kind), canvasHeight = canvasHeight(kind);
      double margin = Math.min(VIEWPORT_MARGIN, (Math.min(viewportWidth, viewportHeight) - 1.0D) / 2.0D);
      double availableWidth = viewportWidth - 2 * margin, availableHeight = viewportHeight - 2 * margin;
      float scale = (float)Math.min(1.0D, Math.min(availableWidth / canvasWidth, availableHeight / canvasHeight));
      if ((double)scale * canvasWidth > availableWidth || (double)scale * canvasHeight > availableHeight)
         scale = Math.nextDown(scale);
      return new ViewTransform(scale, (float)((viewportWidth - (double)canvasWidth * scale) / 2),
         (float)((viewportHeight - (double)canvasHeight * scale) / 2), canvasWidth, canvasHeight);
   }

   /** One transform shared by every render, absolute input and drag-delta path. */
   public record ViewTransform(float scale, float originX, float originY, int canvasWidth, int canvasHeight) {
      public ViewTransform {
         if (!Float.isFinite(scale) || scale <= 0 || scale > 1
            || !Float.isFinite(originX) || !Float.isFinite(originY)) throw new IllegalArgumentException("Invalid transform");
      }
      public double localX(double screenX) { return (screenX - originX) / scale; }
      public double localY(double screenY) { return (screenY - originY) / scale; }
      public double screenX(double localX) { return originX + localX * scale; }
      public double screenY(double localY) { return originY + localY * scale; }
      public double localDelta(double delta) { return delta / scale; }
      public double screenDelta(double delta) { return delta * scale; }
      public int renderPointerX(double x) { return (int)Math.floor(localX(x)); }
      public int renderPointerY(double y) { return (int)Math.floor(localY(y)); }
      public int guiLeft() { return (int)Math.floor(screenX(0)); }
      public int guiTop() { return (int)Math.floor(screenY(0)); }
      public int guiWidth() { return (int)Math.ceil(screenX(canvasWidth)) - guiLeft(); }
      public int guiHeight() { return (int)Math.ceil(screenY(canvasHeight)) - guiTop(); }
   }

   public static List<Rect> flameMask() { return List.of(
      new Rect(119, 87, 2, 1),
      new Rect(119, 88, 2, 1),
      new Rect(118, 89, 3, 1),
      new Rect(117, 90, 4, 1),
      new Rect(117, 91, 4, 1),
      new Rect(123, 91, 1, 1),
      new Rect(113, 92, 1, 1),
      new Rect(117, 92, 5, 1),
      new Rect(123, 92, 2, 1),
      new Rect(113, 93, 1, 1),
      new Rect(116, 93, 10, 1),
      new Rect(113, 94, 2, 1),
      new Rect(117, 94, 2, 1),
      new Rect(120, 94, 6, 1),
      new Rect(112, 95, 4, 1),
      new Rect(117, 95, 2, 1),
      new Rect(121, 95, 6, 1),
      new Rect(112, 96, 7, 1),
      new Rect(122, 96, 5, 1),
      new Rect(112, 97, 7, 1),
      new Rect(122, 97, 5, 1),
      new Rect(112, 98, 6, 1),
      new Rect(123, 98, 4, 1),
      new Rect(112, 99, 3, 1),
      new Rect(123, 99, 4, 1),
      new Rect(113, 100, 2, 1),
      new Rect(123, 100, 3, 1),
      new Rect(114, 101, 1, 1),
      new Rect(123, 101, 2, 1)
   ); }
   public static List<Rect> progressMask() { return List.of(
      new Rect(154, 85, 1, 1),
      new Rect(154, 86, 2, 1),
      new Rect(154, 87, 3, 1),
      new Rect(143, 88, 15, 1),
      new Rect(143, 89, 16, 1),
      new Rect(143, 90, 16, 1),
      new Rect(143, 91, 15, 1),
      new Rect(154, 92, 3, 1),
      new Rect(154, 93, 2, 1),
      new Rect(154, 94, 1, 1)
   ); }
}
