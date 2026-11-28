package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FactoryPlacerV2ReferenceGeometryTest {
   @Test public void selectedReferenceGeometryAndState() { FactoryPlacerV2ReferenceChecks.main(new String[0]); }

   @Test public void activePngMatchesMeasuredGeometryAndRetainsTheHistoricalPng() throws Exception {
      Gui11V5CArtworkChecks.panel("factory_placer_v2_reference/background.png",
         "bc0f0426f308435585e66f39c6a34c08b38173a3f7927ac0fadafe9d161eb99d", 1199, 1312);
      var image = Gui11V5CArtworkChecks.panel("gui11_v5c/04.png",
         "d7bce151b9baad808b69954ffa574b174534a3cefceff8aed76bb65060d8700c", 1254, 1254);
      assertEquals(FactoryPlacerV2ReferenceGeometry.SOURCE_WIDTH, image.getWidth());
      assertEquals(FactoryPlacerV2ReferenceGeometry.SOURCE_HEIGHT, image.getHeight());
      assertEquals(323, FactoryPlacerV2ReferenceGeometry.CANVAS_WIDTH);
      assertEquals(323, FactoryPlacerV2ReferenceGeometry.CANVAS_HEIGHT);
      for (var a : FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS) {
         Gui11V5CArtworkChecks.darkInterior(image, 323, 323, a.x()+6, a.y()+6, 4, 4);
      }
   }
}
