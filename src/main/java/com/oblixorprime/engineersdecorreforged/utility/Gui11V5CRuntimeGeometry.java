package com.oblixorprime.engineersdecorreforged.utility;

import java.util.List;

/** Runtime geometry extracted from the owner-selected GUI11-R3 V5-Corrigee contracts.
 * Inventory container indices and actions are unchanged; these points are client-only menu origins.
 */
public final class Gui11V5CRuntimeGeometry {
   private Gui11V5CRuntimeGeometry() {}
   public record Point(int x, int y) {}
   public record Rect(int x, int y, int width, int height) {
      public boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
   }
   public static boolean supports(MachineKind kind) { return switch (kind) {
      case FACTORY_HOPPER -> true;
      case FACTORY_DROPPER -> true;
      case SMALL_LAB_FURNACE -> true;
      case FACTORY_PLACER -> true;
      case SMALL_FREEZER -> true;
      case SMALL_MINERAL_SMELTER -> true;
      case SMALL_ELECTRICAL_FURNACE -> true;
      case SMALL_WASTE_INCINERATOR -> true;
      case SMALL_BLOCK_BREAKER -> true;
      case SMALL_TREE_CUTTER -> true;
      case SMALL_MILKING_MACHINE -> true;
      case SMALL_FLUID_FUNNEL -> true;
      case PASSIVE_FLUID_ACCUMULATOR -> true;
      case FLUID_BARREL -> true;
      case METAL_CRAFTING_TABLE -> true;
      case LABELED_CRATE -> true;
      default -> false;
   }; }
   public static String id(MachineKind kind) { return switch (kind) {
      case FACTORY_HOPPER -> "01";
      case FACTORY_DROPPER -> "02";
      case SMALL_LAB_FURNACE -> "03";
      case FACTORY_PLACER -> "04";
      case SMALL_FREEZER -> "05";
      case SMALL_MINERAL_SMELTER -> "06";
      case SMALL_ELECTRICAL_FURNACE -> "07";
      case SMALL_WASTE_INCINERATOR -> "08";
      case SMALL_BLOCK_BREAKER -> "09";
      case SMALL_TREE_CUTTER -> "10";
      case SMALL_MILKING_MACHINE -> "12";
      case SMALL_FLUID_FUNNEL -> "13";
      case PASSIVE_FLUID_ACCUMULATOR -> "14";
      case FLUID_BARREL -> "15";
      case METAL_CRAFTING_TABLE -> "16";
      case LABELED_CRATE -> "17";
      default -> throw new IllegalArgumentException("No GUI11 contract for " + kind);
   }; }
   public static int width(MachineKind kind) { return switch (id(kind)) {
      case "01" -> 321;
      case "02" -> 305;
      case "03" -> 289;
      case "04" -> 323;
      case "05" -> 281;
      case "06" -> 279;
      case "07" -> 291;
      case "08" -> 272;
      case "09" -> 272;
      case "10" -> 283;
      case "12" -> 283;
      case "13" -> 308;
      case "14" -> 303;
      case "15" -> 292;
      case "16" -> 285;
      case "17" -> 380;
      case "18" -> 368;
      default -> throw new IllegalArgumentException("No GUI11 width");
   }; }
   public static int height(MachineKind kind) { return switch (id(kind)) {
      case "01" -> 321;
      case "02" -> 305;
      case "03" -> 264;
      case "04" -> 323;
      case "05" -> 313;
      case "06" -> 312;
      case "07" -> 284;
      case "08" -> 289;
      case "09" -> 280;
      case "10" -> 294;
      case "12" -> 283;
      case "13" -> 289;
      case "14" -> 303;
      case "15" -> 329;
      case "16" -> 352;
      case "17" -> 357;
      case "18" -> 207;
      default -> throw new IllegalArgumentException("No GUI11 height");
   }; }
   public static int sourceWidth(MachineKind kind) { return switch (id(kind)) {
      case "01" -> 1254;
      case "02" -> 1254;
      case "03" -> 1312;
      case "04" -> 1254;
      case "05" -> 1187;
      case "06" -> 1187;
      case "07" -> 1268;
      case "08" -> 1216;
      case "09" -> 1236;
      case "10" -> 1230;
      case "12" -> 1254;
      case "13" -> 1294;
      case "14" -> 1254;
      case "15" -> 1182;
      case "16" -> 1129;
      case "17" -> 1294;
      case "18" -> 1672;
      default -> throw new IllegalArgumentException("No GUI11 source width");
   }; }
   public static int sourceHeight(MachineKind kind) { return switch (id(kind)) {
      case "01" -> 1254;
      case "02" -> 1254;
      case "03" -> 1199;
      case "04" -> 1254;
      case "05" -> 1325;
      case "06" -> 1325;
      case "07" -> 1241;
      case "08" -> 1294;
      case "09" -> 1272;
      case "10" -> 1278;
      case "12" -> 1254;
      case "13" -> 1216;
      case "14" -> 1254;
      case "15" -> 1330;
      case "16" -> 1393;
      case "17" -> 1216;
      case "18" -> 941;
      default -> throw new IllegalArgumentException("No GUI11 source height");
   }; }
   public static String texturePath(MachineKind kind) { return "textures/gui/gui11_v5c/" + id(kind) + ".png"; }
   private static final List<Point> EMPTY = List.of();
   public static List<Point> slots(MachineKind kind) { return switch (id(kind)) {
      case "01" -> java.util.Arrays.asList(new Point(44, 67), new Point(67, 67), new Point(89, 67), new Point(111, 67), new Point(133, 67), new Point(155, 67), new Point(44, 91), new Point(67, 91), new Point(89, 91), new Point(111, 91), new Point(133, 91), new Point(155, 91), new Point(44, 115), new Point(67, 115), new Point(89, 115), new Point(111, 115), new Point(133, 115), new Point(155, 115), new Point(55, 173), new Point(80, 173), new Point(104, 173), new Point(128, 173), new Point(153, 173), new Point(177, 173), new Point(201, 173), new Point(225, 173), new Point(250, 173), new Point(55, 198), new Point(80, 198), new Point(104, 198), new Point(128, 198), new Point(153, 198), new Point(177, 198), new Point(201, 198), new Point(225, 198), new Point(250, 198), new Point(55, 223), new Point(80, 223), new Point(104, 223), new Point(128, 223), new Point(153, 223), new Point(177, 223), new Point(201, 223), new Point(225, 223), new Point(250, 223), new Point(55, 259), new Point(80, 259), new Point(104, 259), new Point(128, 259), new Point(153, 259), new Point(177, 259), new Point(201, 259), new Point(225, 259), new Point(250, 259));
      case "02" -> java.util.Arrays.asList(new Point(40, 64), new Point(62, 64), new Point(85, 64), new Point(107, 64), new Point(129, 64), new Point(151, 64), new Point(40, 87), new Point(62, 87), new Point(85, 87), new Point(107, 87), new Point(129, 87), new Point(151, 87), new Point(42, 120), new Point(86, 120), new Point(130, 120), new Point(51, 169), new Point(75, 169), new Point(98, 169), new Point(121, 169), new Point(144, 169), new Point(168, 169), new Point(191, 169), new Point(214, 169), new Point(238, 169), new Point(51, 193), new Point(75, 193), new Point(98, 193), new Point(121, 193), new Point(144, 193), new Point(168, 193), new Point(191, 193), new Point(214, 193), new Point(238, 193), new Point(51, 218), new Point(75, 218), new Point(98, 218), new Point(121, 218), new Point(144, 218), new Point(168, 218), new Point(191, 218), new Point(214, 218), new Point(238, 218), new Point(52, 251), new Point(75, 251), new Point(99, 251), new Point(122, 251), new Point(144, 251), new Point(168, 251), new Point(191, 251), new Point(214, 251), new Point(237, 251));
      case "03" -> java.util.Arrays.asList(new Point(53, 53), new Point(53, 94), new Point(154, 55), new Point(78, 53), new Point(104, 52), new Point(78, 94), new Point(104, 94), new Point(177, 55), new Point(200, 55), new Point(166, 93), new Point(190, 93), new Point(48, 140), new Point(70, 140), new Point(92, 140), new Point(114, 140), new Point(136, 140), new Point(159, 140), new Point(181, 140), new Point(203, 140), new Point(225, 140), new Point(48, 162), new Point(70, 162), new Point(92, 162), new Point(114, 162), new Point(136, 162), new Point(159, 162), new Point(181, 162), new Point(203, 162), new Point(225, 162), new Point(48, 185), new Point(70, 185), new Point(92, 185), new Point(114, 185), new Point(136, 185), new Point(159, 185), new Point(181, 185), new Point(203, 185), new Point(225, 185), new Point(47, 216), new Point(70, 216), new Point(92, 216), new Point(114, 216), new Point(136, 216), new Point(159, 216), new Point(181, 216), new Point(203, 216), new Point(226, 216));
      case "04" -> java.util.Arrays.asList(new Point(45, 66), new Point(67, 66), new Point(90, 66), new Point(112, 66), new Point(134, 66), new Point(157, 66), new Point(45, 91), new Point(67, 91), new Point(90, 91), new Point(112, 91), new Point(134, 91), new Point(157, 91), new Point(45, 116), new Point(67, 116), new Point(90, 116), new Point(112, 116), new Point(134, 116), new Point(157, 116), new Point(53, 176), new Point(79, 176), new Point(103, 176), new Point(129, 176), new Point(153, 176), new Point(178, 176), new Point(203, 176), new Point(228, 176), new Point(254, 176), new Point(53, 202), new Point(79, 202), new Point(103, 202), new Point(129, 202), new Point(153, 202), new Point(178, 202), new Point(203, 202), new Point(229, 202), new Point(254, 202), new Point(53, 229), new Point(79, 229), new Point(103, 229), new Point(129, 229), new Point(153, 229), new Point(178, 229), new Point(203, 229), new Point(229, 229), new Point(254, 229), new Point(54, 267), new Point(79, 267), new Point(104, 267), new Point(129, 267), new Point(153, 267), new Point(178, 267), new Point(203, 267), new Point(227, 267), new Point(252, 267));
      case "05" -> java.util.Arrays.asList(new Point(133, 54), null, new Point(133, 146), new Point(44, 182), new Point(66, 182), new Point(89, 182), new Point(111, 182), new Point(133, 182), new Point(155, 182), new Point(177, 182), new Point(199, 182), new Point(222, 182), new Point(44, 205), new Point(66, 205), new Point(89, 205), new Point(111, 205), new Point(133, 205), new Point(155, 205), new Point(177, 205), new Point(199, 205), new Point(222, 205), new Point(44, 229), new Point(66, 229), new Point(89, 229), new Point(111, 229), new Point(133, 229), new Point(155, 229), new Point(177, 229), new Point(199, 229), new Point(222, 229), new Point(44, 262), new Point(66, 262), new Point(89, 262), new Point(111, 262), new Point(133, 262), new Point(155, 262), new Point(177, 262), new Point(199, 262), new Point(222, 262));
      case "06" -> java.util.Arrays.asList(new Point(132, 53), null, new Point(132, 145), new Point(44, 181), new Point(66, 181), new Point(88, 181), new Point(110, 181), new Point(132, 181), new Point(155, 181), new Point(177, 181), new Point(199, 181), new Point(221, 181), new Point(44, 204), new Point(66, 204), new Point(88, 204), new Point(110, 204), new Point(132, 204), new Point(155, 204), new Point(177, 204), new Point(199, 204), new Point(221, 204), new Point(44, 227), new Point(66, 227), new Point(88, 227), new Point(110, 227), new Point(132, 227), new Point(155, 227), new Point(177, 227), new Point(199, 227), new Point(221, 227), new Point(44, 261), new Point(66, 261), new Point(88, 261), new Point(110, 261), new Point(132, 261), new Point(155, 261), new Point(177, 261), new Point(199, 261), new Point(221, 261));
      case "07" -> java.util.Arrays.asList(new Point(50, 64), new Point(51, 101), new Point(163, 64), new Point(81, 64), new Point(112, 64), new Point(194, 64), new Point(225, 64), new Point(49, 156), new Point(72, 156), new Point(94, 156), new Point(116, 156), new Point(138, 156), new Point(161, 156), new Point(183, 156), new Point(205, 156), new Point(227, 156), new Point(49, 179), new Point(72, 179), new Point(94, 179), new Point(116, 179), new Point(138, 179), new Point(161, 179), new Point(183, 179), new Point(205, 179), new Point(227, 179), new Point(49, 202), new Point(72, 202), new Point(94, 202), new Point(116, 202), new Point(138, 202), new Point(161, 202), new Point(183, 202), new Point(205, 202), new Point(227, 202), new Point(49, 235), new Point(72, 235), new Point(94, 235), new Point(116, 235), new Point(138, 235), new Point(160, 235), new Point(183, 235), new Point(205, 235), new Point(227, 235));
      case "08" -> java.util.Arrays.asList(new Point(46, 60), new Point(70, 60), new Point(94, 60), new Point(119, 60), new Point(143, 60), new Point(167, 60), new Point(191, 60), new Point(215, 60), new Point(84, 100), new Point(108, 100), new Point(133, 100), new Point(157, 100), new Point(181, 100), new Point(206, 100), new Point(230, 100), new Point(42, 105), new Point(39, 159), new Point(62, 159), new Point(85, 159), new Point(107, 159), new Point(130, 159), new Point(152, 159), new Point(175, 159), new Point(197, 159), new Point(220, 159), new Point(39, 183), new Point(62, 183), new Point(85, 183), new Point(107, 183), new Point(130, 183), new Point(152, 183), new Point(175, 183), new Point(197, 183), new Point(220, 183), new Point(39, 207), new Point(62, 207), new Point(85, 207), new Point(107, 207), new Point(130, 207), new Point(152, 207), new Point(175, 207), new Point(197, 207), new Point(220, 207), new Point(41, 242), new Point(64, 242), new Point(86, 242), new Point(108, 242), new Point(130, 242), new Point(152, 242), new Point(175, 242), new Point(197, 242), new Point(219, 242));
      case "09" -> java.util.Arrays.asList(new Point(213, 98), new Point(39, 157), new Point(62, 157), new Point(84, 157), new Point(106, 157), new Point(128, 157), new Point(150, 157), new Point(172, 157), new Point(195, 157), new Point(217, 157), new Point(39, 180), new Point(62, 180), new Point(84, 180), new Point(106, 180), new Point(128, 180), new Point(150, 180), new Point(172, 180), new Point(195, 180), new Point(217, 180), new Point(39, 203), new Point(62, 203), new Point(84, 203), new Point(106, 203), new Point(128, 203), new Point(150, 203), new Point(172, 203), new Point(195, 203), new Point(217, 203), new Point(39, 237), new Point(62, 237), new Point(84, 237), new Point(106, 237), new Point(128, 237), new Point(150, 237), new Point(173, 237), new Point(195, 237), new Point(217, 237));
      case "10" -> java.util.Arrays.asList(new Point(221, 91), new Point(45, 160), new Point(68, 160), new Point(90, 160), new Point(112, 160), new Point(134, 160), new Point(156, 160), new Point(178, 160), new Point(200, 160), new Point(223, 160), new Point(45, 183), new Point(68, 183), new Point(90, 183), new Point(112, 183), new Point(134, 183), new Point(156, 183), new Point(178, 183), new Point(200, 183), new Point(223, 183), new Point(45, 206), new Point(68, 206), new Point(90, 206), new Point(112, 206), new Point(134, 206), new Point(156, 206), new Point(178, 206), new Point(200, 206), new Point(223, 206), new Point(45, 242), new Point(67, 242), new Point(90, 242), new Point(112, 242), new Point(134, 242), new Point(156, 242), new Point(179, 242), new Point(201, 242), new Point(223, 242));
      case "12" -> java.util.Arrays.asList(new Point(70, 109), new Point(197, 109), new Point(45, 154), new Point(67, 154), new Point(90, 154), new Point(112, 154), new Point(134, 154), new Point(156, 154), new Point(178, 154), new Point(200, 154), new Point(222, 154), new Point(45, 177), new Point(67, 177), new Point(90, 177), new Point(112, 177), new Point(134, 177), new Point(156, 177), new Point(178, 177), new Point(200, 177), new Point(222, 177), new Point(45, 200), new Point(67, 200), new Point(90, 200), new Point(112, 200), new Point(134, 200), new Point(156, 200), new Point(178, 200), new Point(200, 200), new Point(222, 200), new Point(45, 236), new Point(67, 236), new Point(90, 236), new Point(112, 236), new Point(134, 236), new Point(156, 236), new Point(178, 236), new Point(200, 236), new Point(222, 236));
      case "13" -> java.util.Arrays.asList(new Point(63, 75), new Point(230, 75), new Point(58, 155), new Point(80, 155), new Point(102, 155), new Point(124, 155), new Point(146, 155), new Point(169, 155), new Point(191, 155), new Point(213, 155), new Point(235, 155), new Point(58, 178), new Point(80, 178), new Point(102, 178), new Point(124, 178), new Point(146, 178), new Point(169, 178), new Point(191, 178), new Point(213, 178), new Point(235, 178), new Point(58, 201), new Point(80, 201), new Point(102, 201), new Point(124, 201), new Point(146, 201), new Point(169, 201), new Point(191, 201), new Point(213, 201), new Point(235, 201), new Point(58, 237), new Point(80, 237), new Point(102, 237), new Point(124, 237), new Point(146, 237), new Point(169, 237), new Point(191, 237), new Point(213, 237), new Point(235, 237));
      case "14" -> java.util.Arrays.asList(new Point(66, 109), new Point(222, 109), new Point(55, 167), new Point(77, 167), new Point(100, 167), new Point(122, 167), new Point(144, 167), new Point(166, 167), new Point(188, 167), new Point(211, 167), new Point(233, 167), new Point(55, 191), new Point(78, 191), new Point(100, 191), new Point(122, 191), new Point(144, 191), new Point(166, 191), new Point(188, 191), new Point(211, 191), new Point(233, 191), new Point(55, 214), new Point(78, 214), new Point(100, 214), new Point(122, 214), new Point(144, 214), new Point(166, 214), new Point(188, 214), new Point(211, 214), new Point(233, 214), new Point(54, 250), new Point(77, 250), new Point(99, 250), new Point(121, 250), new Point(144, 250), new Point(166, 250), new Point(189, 250), new Point(211, 250), new Point(234, 250));
      case "15" -> java.util.Arrays.asList(new Point(68, 107), new Point(212, 107), new Point(50, 191), new Point(73, 191), new Point(95, 191), new Point(117, 191), new Point(139, 191), new Point(161, 191), new Point(183, 191), new Point(206, 191), new Point(228, 191), new Point(50, 214), new Point(73, 214), new Point(95, 214), new Point(117, 214), new Point(139, 214), new Point(161, 214), new Point(183, 214), new Point(206, 214), new Point(228, 214), new Point(50, 237), new Point(73, 237), new Point(95, 237), new Point(117, 237), new Point(139, 237), new Point(161, 237), new Point(183, 237), new Point(206, 237), new Point(228, 237), new Point(50, 272), new Point(73, 272), new Point(95, 272), new Point(117, 272), new Point(139, 272), new Point(161, 272), new Point(183, 272), new Point(206, 272), new Point(228, 272));
      case "16" -> java.util.Arrays.asList(new Point(52, 89), new Point(218, 89), new Point(111, 65), new Point(135, 65), new Point(158, 65), new Point(111, 89), new Point(135, 89), new Point(158, 88), new Point(111, 112), new Point(135, 112), new Point(158, 112), new Point(46, 165), new Point(69, 165), new Point(91, 165), new Point(114, 165), new Point(136, 165), new Point(159, 165), new Point(181, 165), new Point(204, 165), new Point(226, 165), new Point(46, 188), new Point(69, 188), new Point(91, 188), new Point(114, 188), new Point(136, 188), new Point(159, 188), new Point(181, 188), new Point(204, 188), new Point(226, 188), new Point(47, 227), new Point(70, 227), new Point(92, 227), new Point(114, 227), new Point(136, 227), new Point(159, 227), new Point(181, 227), new Point(203, 227), new Point(226, 227), new Point(47, 249), new Point(70, 249), new Point(92, 249), new Point(114, 249), new Point(136, 249), new Point(159, 249), new Point(181, 249), new Point(203, 249), new Point(226, 249), new Point(47, 271), new Point(70, 271), new Point(92, 271), new Point(114, 271), new Point(136, 271), new Point(159, 271), new Point(181, 271), new Point(203, 271), new Point(226, 271), new Point(44, 305), new Point(67, 305), new Point(90, 305), new Point(113, 305), new Point(136, 305), new Point(158, 305), new Point(181, 305), new Point(204, 305), new Point(226, 305));
      case "17" -> java.util.Arrays.asList(new Point(69, 74), new Point(91, 74), new Point(113, 74), new Point(135, 74), new Point(158, 74), new Point(180, 74), new Point(202, 74), new Point(224, 74), new Point(247, 74), new Point(69, 95), new Point(91, 95), new Point(113, 95), new Point(135, 95), new Point(158, 95), new Point(180, 95), new Point(202, 95), new Point(225, 95), new Point(247, 96), new Point(69, 117), new Point(91, 117), new Point(113, 117), new Point(135, 117), new Point(158, 117), new Point(180, 117), new Point(202, 117), new Point(225, 117), new Point(247, 117), new Point(69, 139), new Point(91, 139), new Point(113, 139), new Point(135, 139), new Point(158, 139), new Point(180, 139), new Point(202, 139), new Point(225, 139), new Point(247, 139), new Point(69, 162), new Point(91, 162), new Point(113, 162), new Point(135, 162), new Point(158, 162), new Point(180, 162), new Point(202, 162), new Point(225, 162), new Point(247, 162), new Point(69, 184), new Point(91, 184), new Point(113, 184), new Point(135, 184), new Point(158, 184), new Point(180, 184), new Point(202, 184), new Point(225, 184), new Point(247, 184), null, new Point(87, 225), new Point(111, 225), new Point(135, 225), new Point(158, 225), new Point(182, 225), new Point(206, 225), new Point(229, 225), new Point(253, 225), new Point(277, 225), new Point(87, 247), new Point(111, 247), new Point(135, 247), new Point(158, 247), new Point(182, 247), new Point(206, 247), new Point(230, 247), new Point(253, 247), new Point(277, 247), new Point(87, 269), new Point(111, 269), new Point(135, 269), new Point(158, 269), new Point(182, 269), new Point(206, 269), new Point(230, 269), new Point(254, 269), new Point(277, 269), new Point(87, 298), new Point(111, 298), new Point(135, 298), new Point(158, 298), new Point(182, 298), new Point(206, 298), new Point(230, 298), new Point(254, 298), new Point(278, 298));
      default -> EMPTY;
   }; }
   public static Point slot(MachineKind kind, int menuIndex) {
      List<Point> slots = slots(kind);
      if (menuIndex < 0 || menuIndex >= slots.size()) return null;
      return slots.get(menuIndex);
   }
   /** Position for a real MachineMenu slot; some legacy containers omit a reserved source-map index. */
   public static Point machineSlot(MachineKind kind, int menuIndex, int containerIndex) {
      if (kind == MachineKind.LABELED_CRATE && containerIndex == 54) return frameHint();
      if (kind == MachineKind.SMALL_MINERAL_SMELTER && containerIndex == 2) return slot(kind, 2);
      return slot(kind, menuIndex);
   }
   /** Player inventory positions after the Small Mineral Smelter's omitted legacy index 1. */
   public static Point playerSlot(MachineKind kind, int menuIndex) {
      return slot(kind, kind == MachineKind.SMALL_MINERAL_SMELTER ? menuIndex + 1 : menuIndex);
   }
   public static Rect control(MachineKind kind, String key) { return switch (id(kind)) {
      case "01" -> switch (key) {
         case "range" -> new Rect(212, 52, 68, 9);
         case "period" -> new Rect(212, 75, 68, 9);
         case "count" -> new Rect(212, 97, 68, 9);
         case "manual" -> new Rect(186, 115, 28, 27);
         case "redstone" -> new Rect(223, 114, 27, 27);
         case "cycle" -> new Rect(258, 115, 26, 27);
         default -> null; };
      case "02" -> switch (key) {
         case "velocity" -> new Rect(183, 48, 11, 59);
         case "direction" -> new Rect(214, 50, 35, 27);
         case "count" -> new Rect(209, 85, 55, 7);
         case "period" -> new Rect(209, 100, 55, 7);
         case "manual" -> new Rect(169, 115, 25, 25);
         case "filter" -> new Rect(198, 115, 23, 25);
         case "redstone" -> new Rect(225, 115, 23, 25);
         case "cycle" -> new Rect(252, 115, 23, 25);
         default -> null; };
      case "04" -> switch (key) {
         case "manual" -> new Rect(183, 117, 28, 27);
         case "redstone" -> new Rect(218, 117, 29, 27);
         case "cycle" -> new Rect(253, 117, 29, 27);
         case "row0" -> new Rect(189, 51, 91, 20);
         case "row1" -> new Rect(189, 74, 91, 20);
         case "row2" -> new Rect(189, 96, 91, 20);
         default -> null; };
      case "07" -> switch (key) {
         case "rotary" -> new Rect(214, 98, 25, 25);
         default -> null; };
      case "17" -> switch (key) {
         case "preview" -> new Rect(276, 113, 48, 36);
         default -> null; };
      case "18" -> switch (key) {
         case "confirm" -> new Rect(77, 146, 103, 32);
         case "cancel" -> new Rect(188, 146, 104, 32);
         default -> null; };
      default -> null;
   }; }
   public static Rect energy(MachineKind kind) { return switch (id(kind)) {
      case "03" -> new Rect(230, 53, 12, 52);
      case "05" -> new Rect(59, 88, 12, 52);
      case "06" -> new Rect(58, 87, 12, 52);
      case "07" -> new Rect(125, 104, 52, 12);
      case "09" -> new Rect(193, 46, 52, 12);
      default -> null;
   }; }
   public static Rect fluidDisplay(MachineKind kind) { return switch (id(kind)) {
      case "13" -> new Rect(110, 103, 88, 20);
      case "14" -> new Rect(137, 65, 29, 52);
      default -> null;
   }; }
   public record Effect(String key, Rect bounds, int rgb, float maxOpacity) {}
   public static Rect effectBounds(MachineKind kind, String key) {
      for (Effect effect : effects(kind)) if (effect.key().equals(key)) return effect.bounds();
      return null;
   }
   public static List<Effect> effects(MachineKind kind) { return switch (id(kind)) {
      case "01" -> List.of(new Effect("manual-press", new Rect(193, 121, 15, 15), 0xDB4636, 1.0F), new Effect("feedback-redstone", new Rect(226, 121, 20, 13), 0xE9D5B7, 0.075F), new Effect("feedback-cycle", new Rect(261, 122, 20, 13), 0xE9D5B7, 0.075F), new Effect("range-indicator", new Rect(212, 53, 67, 8), 0xA8BFAA, 0.1F), new Effect("period-indicator", new Rect(212, 75, 67, 8), 0xB3C3C1, 0.1F), new Effect("count-indicator", new Rect(212, 98, 67, 8), 0xD0B585, 0.1F), new Effect("copper-manual-response", new Rect(182, 119, 4, 19), 0xE1A361, 0.06F));
      case "02" -> List.of(new Effect("manual-press", new Rect(174, 121, 14, 14), 0xDB4636, 1.0F), new Effect("feedback-filter", new Rect(201, 121, 17, 13), 0xE9D5B7, 0.075F), new Effect("feedback-redstone", new Rect(228, 121, 17, 13), 0xE9D5B7, 0.075F), new Effect("feedback-cycle", new Rect(255, 121, 17, 13), 0xE9D5B7, 0.075F), new Effect("velocity-indicator", new Rect(183, 48, 10, 57), 0xB3C3C1, 0.1F), new Effect("count-indicator", new Rect(209, 85, 54, 6), 0xD0B585, 0.1F), new Effect("period-indicator", new Rect(209, 101, 54, 6), 0xB3C3C1, 0.1F), new Effect("filter-gate-face", new Rect(204, 123, 11, 9), 0xA6C5AD, 0.11F));
      case "03" -> List.of(new Effect("lab-flame", new Rect(106, 74, 15, 14), 0xE7A164, 0.14F), new Effect("transfer-arrow-glint", new Rect(130, 68, 18, 16), 0xD9E2E4, 0.065F));
      case "04" -> List.of(new Effect("manual-press", new Rect(190, 123, 15, 15), 0xDB4636, 1.0F), new Effect("feedback-redstone", new Rect(222, 123, 20, 13), 0xE9D5B7, 0.075F), new Effect("feedback-cycle", new Rect(257, 123, 21, 13), 0xE9D5B7, 0.075F), new Effect("row0-relief", new Rect(220, 55, 25, 12), 0xA8BFAA, 0.08F), new Effect("row1-relief", new Rect(220, 78, 25, 12), 0xA8BFAA, 0.08F), new Effect("row2-relief", new Rect(220, 100, 25, 12), 0xA8BFAA, 0.08F));
      case "05" -> List.of(new Effect("cold-chamber", new Rect(102, 92, 75, 33), 0x9ED7DF, 0.12F), new Effect("chamber-copper-reflection", new Rect(104, 83, 72, 5), 0xE6BD91, 0.05F), new Effect("cold-pipe-reflection", new Rect(79, 86, 9, 25), 0xA6DCE4, 0.065F));
      case "06" -> List.of(new Effect("heat-chamber", new Rect(102, 92, 75, 33), 0xEFA05F, 0.15F), new Effect("chamber-copper-reflection", new Rect(103, 83, 72, 5), 0xE6BD91, 0.05F), new Effect("warm-pipe-reflection", new Rect(80, 87, 8, 31), 0xE8A565, 0.06F));
      case "07" -> List.of(new Effect("heat-wave-relief", new Rect(94, 102, 17, 19), 0xE4AD79, 0.1F), new Effect("active-copper-reflection", new Rect(138, 135, 16, 12), 0xD99C58, 0.055F));
      case "08" -> List.of(new Effect("burn-flame-relief", new Rect(44, 84, 13, 14), 0xE9A063, 0.16F), new Effect("burn-well-warmth", new Rect(42, 118, 15, 3), 0xDC8A4F, 0.085F), new Effect("queue-arrow-glint", new Rect(110, 48, 23, 5), 0xD9E2E4, 0.045F));
      case "09" -> List.of(new Effect("piston-shaft-glint", new Rect(123, 67, 15, 9), 0xD9E2E4, 0.085F), new Effect("copper-tube-glint", new Rect(155, 52, 6, 26), 0xE1A168, 0.055F), new Effect("conveyor-reflection", new Rect(100, 134, 90, 6), 0xD9E2E4, 0.045F), new Effect("target-preview-response", new Rect(36, 60, 50, 24), 0xD9E2E4, 0.055F));
      case "10" -> List.of(new Effect("blade_metal_sheen", new Rect(118, 85, 32, 34), 0xE2E9E9, 0.1F));
      case "12" -> List.of(new Effect("original_display_luminance", new Rect(41, 54, 57, 39), 0xD9E2E4, 0.035F));
      case "17" -> List.of(new Effect("preview_focus", new Rect(280, 118, 40, 26), 0xD9E2E4, 0.045F));
      case "18" -> List.of(new Effect("confirm_hover", new Rect(82, 151, 92, 23), 0xF4E8D5, 0.055F), new Effect("confirm_pressed", new Rect(82, 151, 92, 23), 0x171A1D, 1.0F), new Effect("cancel_hover", new Rect(193, 151, 93, 23), 0xF4E8D5, 0.055F), new Effect("cancel_pressed", new Rect(193, 151, 93, 23), 0x171A1D, 1.0F));
      default -> List.of();
   }; }
   public static Point frameHint() { return new Point(291, 123); }
}
