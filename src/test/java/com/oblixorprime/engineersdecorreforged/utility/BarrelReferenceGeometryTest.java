package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.Test;
import static org.junit.Assert.*;

public final class BarrelReferenceGeometryTest {
   @Test public void selectedReferenceGeometryAndState() { BarrelReferenceChecks.main(new String[0]); }

   @Test public void activePngMatchesMeasuredGeometryAndRetainsTheHistoricalPng() throws Exception {
      Gui11V5CArtworkChecks.panel("barrel_reference_v5/background.png",
         "db926e05f68a75e5fd84a6b67e2d7d6ad875d90de52122a3a584759d55c47c6c", 1137, 1383);
      var image = Gui11V5CArtworkChecks.panel("gui11_v5c/15.png",
         "eb22fd2eba33a68ebb462ca8dbe6bc2fab41720c26a1ca49436af06b2a490388", 1182, 1330);
      assertEquals(BarrelReferenceGeometry.SOURCE_WIDTH, image.getWidth());
      assertEquals(BarrelReferenceGeometry.SOURCE_HEIGHT, image.getHeight());
      assertEquals(292, BarrelReferenceGeometry.CANVAS_WIDTH);
      assertEquals(329, BarrelReferenceGeometry.CANVAS_HEIGHT);
      for (var a : BarrelReferenceGeometry.SLOT_ANCHORS) {
         Gui11V5CArtworkChecks.darkInterior(image, 292, 329, a.x()+6, a.y()+6, 4, 4);
      }
      Gui11V5CArtworkChecks.darkInterior(image, 292, 329, BarrelReferenceState.TANK_X, BarrelReferenceState.TANK_Y,
         BarrelReferenceState.TANK_WIDTH, BarrelReferenceState.TANK_HEIGHT);
   }
}
