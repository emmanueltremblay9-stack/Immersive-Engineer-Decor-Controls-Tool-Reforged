package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TreeCutterV3ReferenceGeometryTest {
   @Test public void selectedReferenceGeometryAndState() { TreeCutterV3ReferenceChecks.main(new String[0]); }

   @Test public void activePngMatchesMeasuredGeometryAndRetainsTheHistoricalPng() throws Exception {
      Gui11V5CArtworkChecks.panel("tree_cutter_v3_reference/background.png",
         "307b00efa8b982d10b4ba2ea03329687f88abf6605f13b4c7dba96350b9d23b4", 1254, 1254);
      var image = Gui11V5CArtworkChecks.panel("gui11_v5c/10.png",
         "c81ada9ba030d42eda8dac4eafba5b305ecb83cffed2ab7c77025aa34bb7b3ef", 1230, 1278);
      assertEquals(TreeCutterV3ReferenceGeometry.SOURCE_WIDTH, image.getWidth());
      assertEquals(TreeCutterV3ReferenceGeometry.SOURCE_HEIGHT, image.getHeight());
      assertEquals(283, TreeCutterV3ReferenceGeometry.CANVAS_WIDTH);
      assertEquals(294, TreeCutterV3ReferenceGeometry.CANVAS_HEIGHT);
      for (var a : TreeCutterV3ReferenceGeometry.SLOT_ANCHORS) {
         Gui11V5CArtworkChecks.darkInterior(image, 283, 294, a.x()+6, a.y()+6, 4, 4);
      }
   }
}
