package com.oblixorprime.engineersdecorreforged.utility;

import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.*;

/** Tests the approved image bytes and real item footprint, not a duplicated drawing algorithm. */
public final class FactoryStudyGeometryTest {
   private static final List<MachineKind> KINDS = List.of(
      MachineKind.FACTORY_HOPPER, MachineKind.FACTORY_DROPPER, MachineKind.SMALL_LAB_FURNACE);

   private static int machineSlots(MachineKind kind) {
      return switch (kind) {
         case FACTORY_HOPPER -> 18;
         case FACTORY_DROPPER -> 15;
         case SMALL_LAB_FURNACE -> 11;
         default -> throw new IllegalArgumentException();
      };
   }

   private static String retainedHash(MachineKind kind) {
      return FactoryStudyGeometry.sourceSha256(kind);
   }

   @Test public void coversExactlyTheThreeMachinesAndTheirExistingMenuOrder() {
      int total = 0;
      for (var kind : MachineKind.values()) {
         assertEquals(kind.toString(), KINDS.contains(kind), FactoryStudyGeometry.supports(kind));
      }
      for (var kind : KINDS) {
         var anchors = FactoryStudyGeometry.anchors(kind);
         int expectedMachineSlots = machineSlots(kind);
         assertEquals(expectedMachineSlots, FactoryStudyGeometry.machineSlotCount(kind));
         assertEquals(expectedMachineSlots + 36, anchors.size());
         assertEquals(expectedMachineSlots, MachineLayout.forKind(kind).machineSlots().size());
         for (int i = 0; i < anchors.size(); i++) {
            assertEquals("Stable menu index " + kind, i, anchors.get(i).menuIndex());
            assertEquals(anchors.get(i), FactoryStudyGeometry.anchor(kind, i));
            if (i < expectedMachineSlots) {
               assertEquals("Stable backing slot " + kind, i,
                  MachineLayout.forKind(kind).machineSlots().get(i).containerIndex());
            }
         }
         total += anchors.size();
      }
      assertEquals(152, total);
   }

   @Test public void everyWholeItemAndHoverAreaFitsItsMeasuredWellWithoutOverlap() {
      for (var kind : KINDS) {
         var anchors = FactoryStudyGeometry.anchors(kind);
         for (var a : anchors) {
            String tag = kind + " slot " + a.menuIndex();
            assertTrue(tag, a.sourceX() >= 0 && a.sourceY() >= 0);
            assertTrue(tag, a.sourceWidth() > 0 && a.sourceHeight() > 0);
            assertTrue(tag, a.sourceX() + a.sourceWidth() <= FactoryStudyGeometry.SOURCE_WIDTH);
            assertTrue(tag, a.sourceY() + a.sourceHeight() <= FactoryStudyGeometry.SOURCE_HEIGHT);
            assertTrue(tag, a.x() >= a.interiorLeft());
            assertTrue(tag, a.y() >= a.interiorTop());
            assertTrue(tag, a.x() + 16 <= a.interiorRight());
            assertTrue(tag, a.y() + 16 <= a.interiorBottom());
            assertTrue(tag, a.hoverLeft() >= a.interiorLeft());
            assertTrue(tag, a.hoverTop() >= a.interiorTop());
            assertTrue(tag, a.hoverRight() <= a.interiorRight());
            assertTrue(tag, a.hoverBottom() <= a.interiorBottom());
            assertTrue(tag, a.containsHover(a.x() + 8, a.y() + 8));
            assertFalse(tag, a.containsHover(a.hoverLeft() - 0.001, a.y() + 8));
            assertFalse(tag, a.containsHover(a.hoverRight(), a.y() + 8));
            assertFalse(tag, a.containsHover(a.x() + 8, a.hoverTop() - 0.001));
            assertFalse(tag, a.containsHover(a.x() + 8, a.hoverBottom()));
            for (var b : anchors) {
               if (a.menuIndex() >= b.menuIndex()) continue;
               assertFalse(tag + " overlaps " + b.menuIndex(),
                  overlaps(a.x(), a.y(), 16, 16, b.x(), b.y(), 16, 16));
            }
         }
      }
   }

   @Test public void packagedV5CorrectedImagesRetainIdentityAndClearEveryItemFootprint() throws Exception {
      for (var kind : KINDS) {
         String resource = "/assets/immersive_engineer_decor_controls_tool_reforged/"
            + FactoryStudyGeometry.texturePath(kind);
         byte[] bytes;
         try (var input = getClass().getResourceAsStream(resource)) {
            assertNotNull(resource, input);
            bytes = input.readAllBytes();
         }
         String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
         assertEquals("Approved original " + kind, retainedHash(kind), digest);
         assertEquals("Geometry provenance " + kind, digest, FactoryStudyGeometry.sourceSha256(kind));
         BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
         assertNotNull(image);
         assertEquals(FactoryStudyGeometry.sourceWidth(kind), image.getWidth());
         assertEquals(FactoryStudyGeometry.sourceHeight(kind), image.getHeight());
         assertTrue("Retained alpha channel", image.getColorModel().hasAlpha());
         for (var a : FactoryStudyGeometry.anchors(kind)) {
            int canvasWidth = FactoryStudyGeometry.canvasWidth(kind);
            int canvasHeight = FactoryStudyGeometry.canvasHeight(kind);
            int sourceWidth = FactoryStudyGeometry.sourceWidth(kind);
            int sourceHeight = FactoryStudyGeometry.sourceHeight(kind);
            // Check the interior of the exact 16x16 item footprint after the per-panel
            // source-to-canvas transform. The one-pixel bevel may be brighter by design.
            int left = (int)Math.floor((a.x() + 2.0D) * sourceWidth / canvasWidth);
            int top = (int)Math.floor((a.y() + 2.0D) * sourceHeight / canvasHeight);
            int right = (int)Math.ceil((a.x() + 14.0D) * sourceWidth / canvasWidth);
            int bottom = (int)Math.ceil((a.y() + 14.0D) * sourceHeight / canvasHeight);
            assertTrue(a.x() >= 0 && a.y() >= 0 && a.x() + 16 <= canvasWidth && a.y() + 16 <= canvasHeight);
            for (int sy = top; sy < bottom; sy++) {
               for (int sx = left; sx < right; sx++) {
                  int pixel = image.getRGB(sx, sy);
                  int alpha = pixel >>> 24;
                  int brightest = Math.max((pixel >>> 16) & 255,
                     Math.max((pixel >>> 8) & 255, pixel & 255));
                  assertTrue(kind + " slot " + a.menuIndex() + " opaque artwork under item at " + sx + "," + sy,
                     alpha > 245);
                  assertTrue(kind + " slot " + a.menuIndex() + " non-dark slot interior at " + sx + "," + sy,
                     brightest < 70);
               }
            }
         }
      }
   }

   @Test public void realControlsDoNotInterceptAnyItemFootprint() {
      assertEquals(6, FactoryStudyGeometry.controls(MachineKind.FACTORY_HOPPER).size());
      assertEquals(8, FactoryStudyGeometry.controls(MachineKind.FACTORY_DROPPER).size());
      assertTrue(FactoryStudyGeometry.controls(MachineKind.SMALL_LAB_FURNACE).isEmpty());
      for (var kind : KINDS) {
         var controls = FactoryStudyGeometry.controls(kind);
         for (int i = 0; i < controls.size(); i++) {
            var control = controls.get(i);
            var r = FactoryStudyGeometry.controlBounds(kind, control);
            assertTrue(r.x() >= 0 && r.y() >= 0 && r.width() > 0 && r.height() > 0);
            assertTrue(r.right() <= FactoryStudyGeometry.CANVAS_WIDTH);
            assertTrue(r.bottom() <= FactoryStudyGeometry.CANVAS_HEIGHT);
            assertEquals(control, FactoryStudyGeometry.controlAt(kind,
               r.x() + r.width() / 2.0, r.y() + r.height() / 2.0));
            for (var a : FactoryStudyGeometry.anchors(kind)) {
               assertFalse(kind + " " + control + " intercepts slot " + a.menuIndex(),
                  overlaps(r.x(), r.y(), r.width(), r.height(), a.x(), a.y(), 16, 16));
            }
            for (int j = i + 1; j < controls.size(); j++) {
               var other = FactoryStudyGeometry.controlBounds(kind, controls.get(j));
               assertFalse(kind + " ambiguous controls " + control + "/" + controls.get(j),
                  overlaps(r.x(), r.y(), r.width(), r.height(),
                     other.x(), other.y(), other.width(), other.height()));
            }
         }
      }
   }

   @Test public void viewTransformRoundTripsItemsAndBordersAtSupportedAndSmallSizes() {
      int[][] viewports = {{1920,1080}, {960,540}, {640,360}, {480,270},
         {1280,720}, {640,360}, {427,240}, {320,180}, {316,312}, {13,13}, {1,1}};
      for (int[] viewport : viewports) {
         var view = FactoryStudyGeometry.fit(viewport[0], viewport[1]);
         assertTrue(Float.isFinite(view.scale()) && view.scale() > 0 && view.scale() <= 1);
         assertTrue(view.screenX(0) >= -0.0001);
         assertTrue(view.screenY(0) >= -0.0001);
         assertTrue(view.screenX(view.canvasWidth()) <= viewport[0] + 0.0001);
         assertTrue(view.screenY(view.canvasHeight()) <= viewport[1] + 0.0001);
         for (double value : new double[]{-1, 0, 0.001, 8, 15.999, 156.25, 311.999, 316, 317}) {
            assertEquals(value, view.localX(view.screenX(value)), 0.0001);
            assertEquals(value, view.localY(view.screenY(value)), 0.0001);
         }
         for (var kind : KINDS) {
            var kindView = FactoryStudyGeometry.fit(kind, viewport[0], viewport[1]);
            for (var a : FactoryStudyGeometry.anchors(kind)) {
               assertEquals(a.x() + 8, kindView.localX(kindView.screenX(a.x() + 8)), 0.0001);
               assertEquals(a.y() + 8, kindView.localY(kindView.screenY(a.y() + 8)), 0.0001);
            }
         }
         assertEquals(7.25, view.localDelta(7.25 * view.scale()), 0.0001);
      }
   }

   @Test public void liveFillMasksAndFilterLampsCannotPaintOverAnItem() {
      assertFalse(FactoryStudyGeometry.flameMask().isEmpty());
      assertFalse(FactoryStudyGeometry.progressMask().isEmpty());
      for (var r : FactoryStudyGeometry.flameMask()) {
         inside(FactoryStudyGeometry.flame(), r);
         clearOfItems(MachineKind.SMALL_LAB_FURNACE, r);
      }
      for (var r : FactoryStudyGeometry.progressMask()) {
         inside(FactoryStudyGeometry.progress(), r);
         clearOfItems(MachineKind.SMALL_LAB_FURNACE, r);
      }
      inside(FactoryStudyGeometry.energyBounds(), FactoryStudyGeometry.energy());
      clearOfItems(MachineKind.SMALL_LAB_FURNACE, FactoryStudyGeometry.energy());
      for (int filter = 0; filter < 3; filter++) {
         for (int state = 0; state < 3; state++) {
            clearOfItems(MachineKind.FACTORY_DROPPER, FactoryStudyGeometry.filterIndicator(filter, state));
         }
      }
   }

   @Test public void nonpositiveViewportsAreRejected() {
      for (int[] viewport : new int[][]{{0,100}, {100,0}, {-1,100}, {100,-1}}) {
         try {
            FactoryStudyGeometry.fit(viewport[0], viewport[1]);
            fail("Nonpositive viewport accepted");
         } catch (IllegalArgumentException expected) {
            // A zero-sized viewport cannot have a usable inverse input transform.
         }
      }
   }

   private static boolean overlaps(int x, int y, int width, int height,
                                   int otherX, int otherY, int otherWidth, int otherHeight) {
      return x < otherX + otherWidth && x + width > otherX
         && y < otherY + otherHeight && y + height > otherY;
   }

   private static void inside(FactoryStudyGeometry.Rect parent, FactoryStudyGeometry.Rect child) {
      assertTrue(child.x() >= parent.x() && child.y() >= parent.y()
         && child.right() <= parent.right() && child.bottom() <= parent.bottom());
   }

   private static void clearOfItems(MachineKind kind, FactoryStudyGeometry.Rect r) {
      for (var a : FactoryStudyGeometry.anchors(kind)) {
         assertFalse(kind + " live state paint overlaps slot " + a.menuIndex(),
            overlaps(r.x(), r.y(), r.width(), r.height(), a.x(), a.y(), 16, 16));
      }
   }
}
