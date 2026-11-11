package com.oblixorprime.engineersdecorreforged.utility;

/** Dependency-free contract checks; these do not substitute for native client qualification. */
public final class TreeCutterV3ReferenceChecks {
   private static void require(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
   private static void close(double expected, double actual) {
      require(Math.abs(expected - actual) < 0.0001, expected + " != " + actual);
   }
   public static void main(String[] args) {
      require(TreeCutterV3ReferenceGeometry.SLOT_ANCHORS.size() == 37, "One output and 36 player slots");
      for (int index = 0; index < 37; index++) {
         var a = TreeCutterV3ReferenceGeometry.anchor(index);
         require(a.menuIndex() == index, "Unique contiguous menu index");
         require(a.x() >= a.interiorLeft() && a.y() >= a.interiorTop()
            && a.x() + 16 <= a.interiorRight() && a.y() + 16 <= a.interiorBottom(), "Item outside retained window: " + index);
         require(a.containsHover(a.x() + 8, a.y() + 8), "Center must be interactive");
         require(!a.containsHover(a.hoverLeft() - 0.0001, a.y() + 8), "Left padding escaped");
         require(!a.containsHover(a.hoverRight(), a.y() + 8), "Right half-open edge");
         require(!a.containsHover(a.x() + 8, a.hoverTop() - 0.0001), "Top padding escaped");
         require(!a.containsHover(a.x() + 8, a.hoverBottom()), "Bottom half-open edge");
         for (int next = index + 1; next < 37; next++) {
            var b = TreeCutterV3ReferenceGeometry.anchor(next);
            require(a.hoverRight() <= b.hoverLeft() || b.hoverRight() <= a.hoverLeft()
               || a.hoverBottom() <= b.hoverTop() || b.hoverBottom() <= a.hoverTop(), "Overlapping slots");
         }
      }
      var output = TreeCutterV3ReferenceGeometry.anchor(0);
      require(output.sourceX() == 895 && output.sourceY() == 279 && output.sourceWidth() == 119 && output.sourceHeight() == 115,
         "Output must use the sole retained aperture");
      int[][] frames = {{1280,720},{640,360},{426,240},{346,260},{320,180},{1920,1080},{1,1},{10,5}};
      for (var frame : frames) {
         var view = TreeCutterV3ReferenceGeometry.fit(frame[0], frame[1]);
         require(view.scale() > 0 && view.scale() <= 1, "Invalid fit");
         require(view.screenX(0) >= 0 && view.screenY(0) >= 0
            && view.screenX(418) <= frame[0] + 0.0001 && view.screenY(418) <= frame[1] + 0.0001, "Canvas escaped viewport");
         for (var a : TreeCutterV3ReferenceGeometry.SLOT_ANCHORS) {
            close(a.x()+8, view.localX(view.screenX(a.x()+8)));
            close(a.y()+8, view.localY(view.screenY(a.y()+8)));
         }
         close(7.25, view.localDelta(view.screenDelta(7.25)));
         require(view.renderPointerX(view.screenX(-0.1)) == -1, "Negative pointer rounded inside");
         require(view.renderPointerY(view.screenY(-0.1)) == -1, "Negative pointer rounded inside");
         require(view.guiLeft() <= view.screenX(0) && view.guiTop() <= view.screenY(0), "Public origin excludes edge");
         require(view.guiLeft()+view.guiWidth() >= view.screenX(418)
            && view.guiTop()+view.guiHeight() >= view.screenY(418), "Public extent excludes edge");
      }
      var energy = TreeCutterV3ReferenceGeometry.ENERGY;
      require(energy.x()*3 >= 838 && energy.y()*3 >= 170
         && (energy.x()+energy.width())*3 <= 1027 && (energy.y()+energy.height())*3 <= 189, "FE paints outside source mask");
      require(TreeCutterV3ReferenceGeometry.energyFillWidth(0,32000)==0, "Empty FE");
      require(TreeCutterV3ReferenceGeometry.energyFillWidth(16000,32000)==31, "Half FE");
      require(TreeCutterV3ReferenceGeometry.energyFillWidth(Integer.MAX_VALUE,32000)==62, "FE overflow clamp");
      require(TreeCutterV3ReferenceGeometry.energyFillWidth(1,0)==0, "Zero capacity");
      require(TreeCutterV3ReferenceGeometry.tooltipWidth(260)==240, "Bounded tooltip");
      System.out.println("Tree Cutter V3: 37 anchors, half-open hitboxes, transforms and FE safety passed");
   }
}
