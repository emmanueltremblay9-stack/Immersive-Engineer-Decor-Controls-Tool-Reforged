package com.oblixorprime.engineersdecorreforged.utility;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import org.junit.Test;

import static org.junit.Assert.*;

/** Guards the opt-in GUI11 presentation without changing Solar V2 defaults or machine rules. */
public final class SolarGui11GeometryTest {
   private static final String RESOURCE =
      "/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/solar_gui11_r3/background.png";
   private static final String RESOURCE_SHA256 =
      "40630bd8360abb4c21170db2fca6d098359319225c5050b7c8783d8a50b01385";

   @Test public void allThirtySevenMenuItemsHaveUniqueWholeFootprintsOnCanvas() {
      int visibleMachineSlots = MachineLayout.forKind(MachineKind.SMALL_SOLAR_PANEL).machineSlots().size();
      assertEquals(1, visibleMachineSlots);
      assertEquals(37, visibleMachineSlots + 36);
      var positions = new HashSet<String>();
      for (int index = 0; index < visibleMachineSlots + 36; index++) {
         int x = SolarGui11Geometry.slotX(index);
         int y = SolarGui11Geometry.slotY(index);
         assertTrue("negative x for slot " + index, x >= 0);
         assertTrue("negative y for slot " + index, y >= 0);
         assertTrue("slot exceeds canvas width: " + index, x + 16 <= Math.ceil(SolarGui11Geometry.CANVAS_WIDTH));
         assertTrue("slot exceeds canvas height: " + index, y + 16 <= Math.ceil(SolarGui11Geometry.CANVAS_HEIGHT));
         assertTrue("duplicate item origin at slot " + index, positions.add(x + "," + y));
      }
      // Cell, 27 player inventory slots and 9 hotbar slots retain vanilla menu order.
      assertEquals(224, SolarGui11Geometry.slotX(0));
      assertEquals(76, SolarGui11Geometry.slotY(0));
      assertEquals(57, SolarGui11Geometry.slotX(1));
      assertEquals(159, SolarGui11Geometry.slotY(1));
      assertEquals(57, SolarGui11Geometry.slotX(28));
      assertEquals(238, SolarGui11Geometry.slotY(28));
   }

   @Test public void uniformFitAndEnergyHitTargetUseTheSameMeasuredCanvas() {
      for (int[] viewport : new int[][] {{1280, 720}, {320, 260}, {192, 180}}) {
         var view = SolarGui11Geometry.fit(viewport[0], viewport[1]);
         assertTrue(view.scale() > 0.0F && view.scale() <= 1.0F);
         assertTrue(view.screenX(SolarGui11Geometry.CANVAS_WIDTH) <= viewport[0] + 0.001D);
         assertTrue(view.screenY(SolarGui11Geometry.CANVAS_HEIGHT) <= viewport[1] + 0.001D);
         assertEquals(1.0D, view.localX(view.screenX(1.0D)), 0.0001D);
         assertEquals(1.0D, view.localY(view.screenY(1.0D)), 0.0001D);
      }
      double left = SolarGui11Geometry.METER_X * SolarGui11Geometry.NATIVE_TO_LOGICAL;
      double top = SolarGui11Geometry.METER_Y * SolarGui11Geometry.NATIVE_TO_LOGICAL;
      double rightInside = (SolarGui11Geometry.METER_X + SolarGui11Geometry.METER_WIDTH - 0.5D)
         * SolarGui11Geometry.NATIVE_TO_LOGICAL;
      double rightOutside = (SolarGui11Geometry.METER_X + SolarGui11Geometry.METER_WIDTH + 0.5D)
         * SolarGui11Geometry.NATIVE_TO_LOGICAL;
      double bottomInside = (SolarGui11Geometry.METER_Y + SolarGui11Geometry.METER_HEIGHT - 0.5D)
         * SolarGui11Geometry.NATIVE_TO_LOGICAL;
      double bottomOutside = (SolarGui11Geometry.METER_Y + SolarGui11Geometry.METER_HEIGHT + 0.5D)
         * SolarGui11Geometry.NATIVE_TO_LOGICAL;
      double rightEdge = (SolarGui11Geometry.METER_X + SolarGui11Geometry.METER_WIDTH)
         * SolarGui11Geometry.NATIVE_TO_LOGICAL;
      double bottomEdge = (SolarGui11Geometry.METER_Y + SolarGui11Geometry.METER_HEIGHT)
         * SolarGui11Geometry.NATIVE_TO_LOGICAL;
      assertTrue(SolarGui11Geometry.containsMeter(left, top));
      assertTrue(SolarGui11Geometry.containsMeter(rightInside, bottomInside));
      assertFalse(SolarGui11Geometry.containsMeter(rightOutside, top));
      assertFalse(SolarGui11Geometry.containsMeter(left, bottomOutside));
      assertFalse(SolarGui11Geometry.containsMeter(rightEdge, top));
      assertFalse(SolarGui11Geometry.containsMeter(left, bottomEdge));
   }

   @Test public void liveEnergyFillIsEmptyPartialAndFullWithoutInventingCapacity() {
      int width = SolarGui11Geometry.METER_WIDTH;
      assertEquals(0, SolarV2ReferenceState.fillWidth(0, 16000, width));
      assertEquals(0, SolarV2ReferenceState.fillWidth(-1, 16000, width));
      assertEquals(0, SolarV2ReferenceState.fillWidth(100, 0, width));
      assertEquals(width / 2, SolarV2ReferenceState.fillWidth(8000, 16000, width));
      assertEquals(width, SolarV2ReferenceState.fillWidth(16000, 16000, width));
      assertEquals(width, SolarV2ReferenceState.fillWidth(20000, 16000, width));
   }

   @Test public void packagedGui11R3ImageHasThePinnedIdentityAndMeasuredDimensions() throws Exception {
      try (InputStream input = getClass().getResourceAsStream(RESOURCE)) {
         assertNotNull("GUI11 R3 background is packaged", input);
         byte[] png = input.readAllBytes();
         assertEquals(RESOURCE_SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png)));
         ByteBuffer header = ByteBuffer.wrap(png);
         assertEquals(SolarGui11Geometry.SOURCE_WIDTH, header.getInt(16));
         assertEquals(SolarGui11Geometry.SOURCE_HEIGHT, header.getInt(20));
      }
   }
}
