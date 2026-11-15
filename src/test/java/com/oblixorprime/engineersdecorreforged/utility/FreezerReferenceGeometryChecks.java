package com.oblixorprime.engineersdecorreforged.utility;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;

import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry.SlotAnchor;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry.ViewTransform;

/** Plain Java checks shared by the offline main entry point and JUnit wrapper. */
public final class FreezerReferenceGeometryChecks {
   private static final double EPSILON = 0.0001D;
   private static final int[] EXPECTED_INVENTORY_X = {43, 70, 96, 123, 149, 176, 202, 229, 255};
   private static final int[] EXPECTED_INVENTORY_Y = {191, 215, 239, 272};

   private FreezerReferenceGeometryChecks() {
   }

   public static void main(String[] arguments) throws IOException, NoSuchAlgorithmException {
      checkMenuMapping();
      checkMeasuredCenters();
      checkItemAndClippedHoverContainment();
      checkHoverHalfOpenEdges();
      checkUniformTransforms();
      checkInvalidInputs();
      System.out.println("PASS: 6 dependency-free geometry groups; 38 visible anchors; 39 menu indices with index 1 reserved.");
      System.out.println("PASS: 38 item rectangles and 38 clipped hover rectangles contained; 703 distinct slot pairs nonoverlapping.");
      System.out.println("PASS: exhaustive 1..640 by 1..480 viewport fit matrix plus large/portrait cases; uniform transforms, deltas, negative floor, and fractional raw-hover/render policy.");
      System.out.println("DISCLOSED: ordinary vanilla hover padding exceeds 36 conservative V5 inventory interiors; opt-in clipping removes that spill.");
      if (arguments.length == 1) {
         checkRetainedReference(Path.of(arguments[0]));
         System.out.println("PASS: retained reference SHA256, 1254x1254 dimensions, RGB color, and no alpha.");
      } else if (arguments.length == 0) {
         System.out.println("NOT RUN: retained source identity check (pass its path as the sole argument).");
      } else {
         throw new IllegalArgumentException("Usage: FreezerReferenceGeometryChecks [retained-reference.png]");
      }
      System.out.println("NOT RUN: Gradle/JUnit, Minecraft build, client rendering, inventory interactions, and exact graphical acceptance.");
   }

   public static void checkMenuMapping() {
      equal(39, FreezerReferenceGeometry.MENU_SLOT_COUNT, "retained menu count");
      equal(38, FreezerReferenceGeometry.SLOT_ANCHORS.size(), "visible anchors");
      int visible = 0;
      for (int index = 0; index < 39; index++) {
         if (index == 1) {
            expectIllegalArgument(() -> FreezerReferenceGeometry.anchor(1));
         } else {
            SlotAnchor anchor = FreezerReferenceGeometry.anchor(index);
            equal(index, anchor.menuIndex(), "anchor index");
            check(anchor == FreezerReferenceGeometry.SLOT_ANCHORS.get(visible++), "index lookup retains immutable list entry");
         }
      }
      try {
         FreezerReferenceGeometry.SLOT_ANCHORS.clear();
         throw new AssertionError("Anchor list must be immutable");
      } catch (UnsupportedOperationException expected) {
         // Expected: callers cannot change the reference mapping.
      }
      try {
         FreezerReferenceGeometry.INVENTORY_SOURCE_X.set(0, 0);
         throw new AssertionError("Source-coordinate list must be immutable");
      } catch (UnsupportedOperationException expected) {
         // Expected.
      }
   }

   public static void checkMeasuredCenters() {
      checkAnchor(FreezerReferenceGeometry.anchor(0), 0, 154, 502, 130, 120, 47, 133);
      checkAnchor(FreezerReferenceGeometry.anchor(2), 2, 964, 502, 130, 120, 250, 133);
      int[] sourceX = {164, 270, 376, 482, 588, 694, 800, 906, 1012};
      int[] sourceY = {758, 854, 952, 1082};
      for (int row = 0; row < 4; row++) {
         for (int column = 0; column < 9; column++) {
            int index = 3 + row * 9 + column;
            checkAnchor(FreezerReferenceGeometry.anchor(index), index, sourceX[column], sourceY[row],
               80, 72, EXPECTED_INVENTORY_X[column], EXPECTED_INVENTORY_Y[row]);
         }
      }
      // Rounded 106-source-pixel strides alternate between 26 and 27; rows are not an 18-pixel grid.
      equal(27, EXPECTED_INVENTORY_X[1] - EXPECTED_INVENTORY_X[0], "first column stride");
      equal(26, EXPECTED_INVENTORY_X[2] - EXPECTED_INVENTORY_X[1], "second column stride");
      equal(24, EXPECTED_INVENTORY_Y[1] - EXPECTED_INVENTORY_Y[0], "first row stride");
      equal(24, EXPECTED_INVENTORY_Y[2] - EXPECTED_INVENTORY_Y[1], "second row stride");
      equal(33, EXPECTED_INVENTORY_Y[3] - EXPECTED_INVENTORY_Y[2], "hotbar gap");
   }

   public static void checkItemAndClippedHoverContainment() {
      List<SlotAnchor> anchors = FreezerReferenceGeometry.SLOT_ANCHORS;
      int vanillaSpillCount = 0;
      for (SlotAnchor anchor : anchors) {
         check(contains(anchor.interiorLeft(), anchor.interiorTop(), anchor.interiorRight(), anchor.interiorBottom(),
            anchor.x(), anchor.y(), anchor.x() + 16.0D, anchor.y() + 16.0D), "16x16 item inside source interior: " + anchor.menuIndex());
         check(contains(anchor.interiorLeft(), anchor.interiorTop(), anchor.interiorRight(), anchor.interiorBottom(),
            anchor.hoverLeft(), anchor.hoverTop(), anchor.hoverRight(), anchor.hoverBottom()), "clipped hover inside source interior: " + anchor.menuIndex());
         check(contains(anchor.hoverLeft(), anchor.hoverTop(), anchor.hoverRight(), anchor.hoverBottom(),
            anchor.x(), anchor.y(), anchor.x() + 16.0D, anchor.y() + 16.0D), "clipped hover still contains whole item: " + anchor.menuIndex());
         check(contains(0, 0, 314, 314, anchor.hoverLeft(), anchor.hoverTop(), anchor.hoverRight(), anchor.hoverBottom()), "hover inside canvas");
         boolean vanillaContained = contains(anchor.interiorLeft(), anchor.interiorTop(), anchor.interiorRight(), anchor.interiorBottom(),
            anchor.x() - 1.0D, anchor.y() - 1.0D, anchor.x() + 17.0D, anchor.y() + 17.0D);
         check(vanillaContained == (anchor.menuIndex() < 3), "V5 conservative inventory interiors clip padding");
         if (!vanillaContained) vanillaSpillCount++;
      }
      equal(36, vanillaSpillCount, "unclipped vanilla hover spill count");
      for (int first = 0; first < anchors.size(); first++) {
         SlotAnchor a = anchors.get(first);
         for (int second = first + 1; second < anchors.size(); second++) {
            SlotAnchor b = anchors.get(second);
            check(!overlaps(a.x(), a.y(), a.x() + 16, a.y() + 16, b.x(), b.y(), b.x() + 16, b.y() + 16), "item rectangles do not overlap");
            check(!overlaps(a.hoverLeft(), a.hoverTop(), a.hoverRight(), a.hoverBottom(),
               b.hoverLeft(), b.hoverTop(), b.hoverRight(), b.hoverBottom()), "clipped hover rectangles do not overlap");
            check(!overlaps(a.x() - 1, a.y() - 1, a.x() + 17, a.y() + 17,
               b.x() - 1, b.y() - 1, b.x() + 17, b.y() + 17), "ordinary vanilla hover rectangles do not overlap");
         }
      }
   }

   public static void checkHoverHalfOpenEdges() {
      for (SlotAnchor anchor : FreezerReferenceGeometry.SLOT_ANCHORS) {
         double middleX = (anchor.hoverLeft() + anchor.hoverRight()) / 2.0D;
         double middleY = (anchor.hoverTop() + anchor.hoverBottom()) / 2.0D;
         check(anchor.containsHover(anchor.hoverLeft(), anchor.hoverTop()), "top-left included");
         check(anchor.containsHover(Math.nextDown(anchor.hoverRight()), Math.nextDown(anchor.hoverBottom())), "last interior point included");
         check(!anchor.containsHover(Math.nextDown(anchor.hoverLeft()), middleY), "left exterior excluded");
         check(!anchor.containsHover(middleX, Math.nextDown(anchor.hoverTop())), "top exterior excluded");
         check(!anchor.containsHover(anchor.hoverRight(), middleY), "right edge excluded");
         check(!anchor.containsHover(middleX, anchor.hoverBottom()), "bottom edge excluded");
         check(!anchor.containsHover(Double.NaN, middleY), "NaN excluded");
      }
      SlotAnchor middleRow = FreezerReferenceGeometry.anchor(12);
      double centerX = middleRow.x() + 8.0D;
      check(middleRow.containsHover(centerX, 231.8691D), "fractional pointer just inside row 2 bottom is accepted");
      check(!middleRow.containsHover(centerX, 231.8693D), "fractional pointer just outside row 2 bottom is rejected");
      check(!middleRow.containsHover(centerX, 231.99D), "231.99 must not hover or click row 2");
      for (int[] viewport : new int[][] {{320, 240}, {1, 1}, {12, 13}, {1920, 1080}, {240, 320}}) {
         ViewTransform transform = FreezerReferenceGeometry.fit(viewport[0], viewport[1]);
         double physicalX = transform.screenX(centerX);
         double physicalY = transform.screenY(231.99D);
         // These ints remain appropriate for vanilla's render signature, but not
         // the clipped slot hover predicate. Lock in the counterexample explicitly.
         int renderX = transform.renderPointerX(physicalX);
         int renderY = transform.renderPointerY(physicalY);
         equal(231, renderY, "render pointer is intentionally floored");
         check(middleRow.containsHover(renderX, renderY), "floor-only hover would incorrectly expand the hitbox");
         check(!middleRow.containsHover(transform.localX(physicalX), transform.localY(physicalY)),
            "raw local hover must override floored render pointer outside row 2");
         for (SlotAnchor anchor : FreezerReferenceGeometry.SLOT_ANCHORS) {
            physicalX = transform.screenX(anchor.x() + 8.0D);
            physicalY = transform.screenY(anchor.y() + 8.0D);
            double rawX = transform.localX(physicalX);
            double rawY = transform.localY(physicalY);
            renderX = transform.renderPointerX(physicalX);
            renderY = transform.renderPointerY(physicalY);
            check(rawX >= renderX && rawX < renderX + 1.0D, "raw x is preserved beside render floor");
            check(rawY >= renderY && rawY < renderY + 1.0D, "raw y is preserved beside render floor");
            check(anchor.containsHover(rawX, rawY), "all 38 centers hit under floor-render/raw-hover policy");
         }
      }
   }

   public static void checkUniformTransforms() {
      // Exhaust all small integer GUI-scale viewport sizes, including sub-margin viewports.
      for (int width = 1; width <= 640; width++) {
         for (int height = 1; height <= 480; height++) {
            checkFit(width, height);
         }
      }
      int[][] extraViewports = {{854, 480}, {1280, 720}, {1920, 1080}, {3840, 2160}, {4320, 7680}, {1920, 1}, {1, 1920}};
      for (int[] viewport : extraViewports) {
         checkFit(viewport[0], viewport[1]);
      }
      ViewTransform standard = FreezerReferenceGeometry.fit(320, 240);
      near(228.0D / 314.0D, standard.scale(), EPSILON, "320x240 scale");
      near(228.0D, standard.screenDelta(314), EPSILON, "320x240 displayed square");
      near(46.0D, standard.originX(), EPSILON, "320x240 left");
      near(6.0D, standard.originY(), EPSILON, "320x240 top");
      near(16.0D * 228.0D / 314.0D, standard.screenDelta(16), EPSILON, "320x240 item displays at about 11.62px");
      near(1.0D, FreezerReferenceGeometry.fit(1920, 1080).scale(), 0, "never upscale");
      check(FreezerReferenceGeometry.fit(1, 1).scale() < 0.01F, "tiny viewport scale is not clamped to 0.1");
      double[] localPoints = {-400.25D, -1.25D, -0.001D, 0.0D, 0.99D, 15.99D, 157.5D, 314.0D, 400.25D};
      for (int[] viewport : new int[][] {{320, 240}, {1, 1}, {12, 13}, {1920, 1080}, {240, 320}}) {
         ViewTransform transform = FreezerReferenceGeometry.fit(viewport[0], viewport[1]);
         for (double local : localPoints) {
            near(local, transform.localX(transform.screenX(local)), 1.0e-8D, "x roundtrip");
            near(local, transform.localY(transform.screenY(local)), 1.0e-8D, "y roundtrip");
            near(local, transform.localDelta(transform.screenDelta(local)), 1.0e-8D, "delta roundtrip");
            equal((int)Math.floor(local), transform.renderPointerX(transform.screenX(local)), "x pointer floors including negative values");
            equal((int)Math.floor(local), transform.renderPointerY(transform.screenY(local)), "y pointer floors including negative values");
         }
         near(transform.screenDelta(16), transform.screenX(48) - transform.screenX(32), EPSILON, "uniform horizontal scale");
         near(transform.screenDelta(16), transform.screenY(48) - transform.screenY(32), EPSILON, "uniform vertical scale");
         // Correct absolute inverse transforms must also preserve every physical hit region.
         for (SlotAnchor anchor : FreezerReferenceGeometry.SLOT_ANCHORS) {
            double physicalX = transform.screenX(anchor.x() + 8);
            double physicalY = transform.screenY(anchor.y() + 8);
            check(anchor.containsHover(transform.localX(physicalX), transform.localY(physicalY)), "transformed center hits matching slot");
         }
      }
   }

   public static void checkInvalidInputs() {
      expectIllegalArgument(() -> FreezerReferenceGeometry.fit(0, 240));
      expectIllegalArgument(() -> FreezerReferenceGeometry.fit(320, 0));
      expectIllegalArgument(() -> FreezerReferenceGeometry.fit(-1, 240));
      expectIllegalArgument(() -> FreezerReferenceGeometry.anchor(-1));
      expectIllegalArgument(() -> FreezerReferenceGeometry.anchor(39));
      expectIllegalArgument(() -> new ViewTransform(0, 0, 0));
      expectIllegalArgument(() -> new ViewTransform(1.01F, 0, 0));
      expectIllegalArgument(() -> new ViewTransform(Float.NaN, 0, 0));
      expectIllegalArgument(() -> new ViewTransform(1, Float.POSITIVE_INFINITY, 0));
   }

   public static void checkRetainedReference(Path path) throws IOException, NoSuchAlgorithmException {
      byte[] bytes = Files.readAllBytes(path);
      String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      check(FreezerReferenceGeometry.SOURCE_SHA256.equals(hash), "retained PNG byte identity");
      BufferedImage image = ImageIO.read(path.toFile());
      check(image != null, "readable PNG");
      equal(1254, image.getWidth(), "source width");
      equal(1254, image.getHeight(), "source height");
      equal(java.awt.color.ColorSpace.TYPE_RGB, image.getColorModel().getColorSpace().getType(), "source RGB mode");
      equal(3, image.getColorModel().getNumColorComponents(), "source RGB components");
      check(!image.getColorModel().hasAlpha(), "source has no alpha");
   }

   private static void checkFit(int width, int height) {
      ViewTransform transform = FreezerReferenceGeometry.fit(width, height);
      check(Float.isFinite(transform.scale()) && transform.scale() > 0 && transform.scale() <= 1, "positive finite non-upscaled fit");
      double margin = Math.min(6.0D, (Math.min(width, height) - 1.0D) / 2.0D);
      double side = transform.screenDelta(314);
      check(transform.originX() >= margin - EPSILON && transform.originY() >= margin - EPSILON, "leading fit margins");
      check(transform.screenX(314) <= width - margin + EPSILON && transform.screenY(314) <= height - margin + EPSILON, "trailing fit margins");
      near((width - side) / 2, transform.originX(), EPSILON, "centered horizontally");
      near((height - side) / 2, transform.originY(), EPSILON, "centered vertically");
      near(Math.min(314.0D, Math.min(width, height) - 2 * margin), side, EPSILON, "largest permitted square");
   }

   private static void checkAnchor(SlotAnchor actual, int index, int sourceX, int sourceY, int sourceWidth, int sourceHeight, int x, int y) {
      equal(index, actual.menuIndex(), "measured menu index");
      equal(sourceX, actual.sourceX(), "measured source x");
      equal(sourceY, actual.sourceY(), "measured source y");
      equal(sourceWidth, actual.sourceWidth(), "measured source width");
      equal(sourceHeight, actual.sourceHeight(), "measured source height");
      equal(x, actual.x(), "rounded x origin");
      equal(y, actual.y(), "rounded y origin");
   }

   private static boolean contains(double left, double top, double right, double bottom, double innerLeft, double innerTop, double innerRight, double innerBottom) {
      return innerLeft >= left && innerTop >= top && innerRight <= right && innerBottom <= bottom;
   }

   private static boolean overlaps(double aLeft, double aTop, double aRight, double aBottom, double bLeft, double bTop, double bRight, double bBottom) {
      return aLeft < bRight && aRight > bLeft && aTop < bBottom && aBottom > bTop;
   }

   private static void expectIllegalArgument(Runnable action) {
      try {
         action.run();
         throw new AssertionError("Expected IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
         // Expected.
      }
   }

   private static void equal(int expected, int actual, String message) {
      check(expected == actual, message + ": expected " + expected + ", got " + actual);
   }

   private static void near(double expected, double actual, double tolerance, String message) {
      check(Math.abs(expected - actual) <= tolerance, message + ": expected " + expected + ", got " + actual);
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
