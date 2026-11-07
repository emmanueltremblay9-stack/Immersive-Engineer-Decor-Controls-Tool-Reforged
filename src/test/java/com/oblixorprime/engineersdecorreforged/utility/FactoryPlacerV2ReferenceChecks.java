package com.oblixorprime.engineersdecorreforged.utility;

import java.util.HashSet;

/** Dependency-free geometry/arithmetic regression checks, not a Minecraft runtime proof. */
public final class FactoryPlacerV2ReferenceChecks {
   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
   public static void main(String[] args) {
      check(FactoryPlacerV2ReferenceGeometry.SOURCE_WIDTH == 1199 && FactoryPlacerV2ReferenceGeometry.SOURCE_HEIGHT == 1312, "retained source dimensions");
      check(FactoryPlacerV2ReferenceGeometry.CANVAS_WIDTH == 400
         && FactoryPlacerV2ReferenceGeometry.CANVAS_HEIGHT == 438, "outward rounded logical canvas");
      check(Math.abs(FactoryPlacerV2ReferenceGeometry.SOURCE_SCALE - 1.0D / 3.0D) < 0.0000001, "uniform source scale");
      var indices = new HashSet<Integer>();
      check(FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS.size() == 54, "54 slots");
      for (var a : FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS) {
         check(indices.add(a.menuIndex()), "unique index");
         check(FactoryPlacerV2ReferenceGeometry.anchor(a.menuIndex()) == a, "index lookup");
         check(a.x() >= a.interiorLeft() && a.y() >= a.interiorTop(), "item top/left inside artwork");
         check(a.x()+16 <= a.interiorRight() && a.y()+16 <= a.interiorBottom(), "item bottom/right inside artwork");
         check(a.containsHover(a.x()+8, a.y()+8), "hover centre");
         check(!a.containsHover(a.hoverLeft()-0.001, a.y()+8), "outside left");
         check(!a.containsHover(a.hoverRight(), a.y()+8), "outside right");
         check(!a.containsHover(a.x()+8, a.hoverTop()-0.001), "outside top");
         check(!a.containsHover(a.x()+8, a.hoverBottom()), "outside bottom");
         for (var b : FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS) {
            if (a != b) check(a.hoverRight() <= b.hoverLeft() || b.hoverRight() <= a.hoverLeft()
               || a.hoverBottom() <= b.hoverTop() || b.hoverBottom() <= a.hoverTop(), "nonoverlapping targets");
         }
      }
      for (int i=0;i<54;i++) check(indices.contains(i), "protocol index present");
      for (int invalid : new int[]{-1,54,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
         try { FactoryPlacerV2ReferenceGeometry.anchor(invalid); throw new AssertionError("Invalid menu index accepted"); }
         catch (IllegalArgumentException expected) { }
      }
      for (int[] viewport : new int[][]{{1920,1080},{960,540},{640,360},{1280,260},{480,270},{320,240},{300,600},{1,1},{1,1000},{1000,1}}) {
         var v = FactoryPlacerV2ReferenceGeometry.fit(viewport[0], viewport[1]);
         check(v.originX() >= -0.0001 && v.originY() >= -0.0001, "positive origins");
         check(v.screenX(400) <= viewport[0]+0.0001 && v.screenY(438) <= viewport[1]+0.0001, "uniform fit");
         check(v.guiLeft() <= v.screenX(0) && v.guiTop() <= v.screenY(0), "external bounds enclose top/left");
         check(v.guiLeft()+v.guiWidth() >= v.screenX(400)
            && v.guiTop()+v.guiHeight() >= v.screenY(438), "external bounds enclose bottom/right");
         check(v.guiWidth()>0 && v.guiHeight()>0, "positive external extent");
         for (var a : FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS) {
            check(Math.abs(v.localX(v.screenX(a.x()+8))-(a.x()+8))<0.0001, "pointer x roundtrip");
            check(Math.abs(v.localY(v.screenY(a.y()+8))-(a.y()+8))<0.0001, "pointer y roundtrip");
            // Fractional pointers immediately outside every target stay outside after inverse mapping.
            check(!a.containsHover(v.localX(v.screenX(a.hoverLeft()-0.001)),a.y()+8),"fractional left outside");
            check(!a.containsHover(v.localX(v.screenX(a.hoverRight()+0.001)),a.y()+8),"fractional right outside");
            check(!a.containsHover(a.x()+8,v.localY(v.screenY(a.hoverTop()-0.001))),"fractional top outside");
            check(!a.containsHover(a.x()+8,v.localY(v.screenY(a.hoverBottom()+0.001))),"fractional bottom outside");
         }
         check(Math.abs(v.localDelta(v.screenDelta(17.3))-17.3)<0.0001, "drag delta roundtrip");
         check(v.renderPointerX(v.screenX(-0.1)) == -1 && v.renderPointerY(v.screenY(-0.1)) == -1,"negative pointer flooring");
      }
      for (int[] invalid : new int[][]{{0,100},{100,0},{-1,10}}) {
         try { FactoryPlacerV2ReferenceGeometry.fit(invalid[0],invalid[1]); throw new AssertionError("Invalid viewport accepted"); }
         catch (IllegalArgumentException expected) { }
      }
      for (int[] sample : new int[][] {{1920,240},{640,240},{320,240},{256,240},{255,239},{200,184},{32,16},{17,1},{16,1},{1,1},{0,1},{Integer.MIN_VALUE,1},{Integer.MAX_VALUE,240}}) {
         check(FactoryPlacerV2ReferenceGeometry.tooltipWidth(sample[0]) == sample[1], "bounded native tooltip width");
      }
      for (float invalid : new float[] {0.0F, -1.0F, 1.001F, Float.NaN, Float.POSITIVE_INFINITY}) {
         try { new FactoryPlacerV2ReferenceGeometry.ViewTransform(invalid, 0, 0); throw new AssertionError("Invalid transform accepted"); }
         catch (IllegalArgumentException expected) { }
      }
      check(FactoryPlacerV2ReferenceGeometry.controlAt(Double.NaN, 65) == null, "nonfinite control input ignored");
      for (int i = 0; i < 18; i++) {
         var a = FactoryPlacerV2ReferenceGeometry.anchor(i);
         check(a.x() - 1 >= a.interiorLeft() && a.y() - 1 >= a.interiorTop(), "selection border top/left inside interior");
         check(a.x() + 17 <= a.interiorRight() && a.y() + 17 <= a.interiorBottom(), "selection border bottom/right inside interior");
         check(FactoryPlacerV2ReferenceState.selectedSlot(i) == i, "cursor preserved");
         check(FactoryPlacerV2ReferenceState.selectedRow(i) == i / 6, "row derived from cursor");
      }
      check(FactoryPlacerV2ReferenceState.selectedSlot(-1) == 0, "negative cursor defensive bound");
      check(FactoryPlacerV2ReferenceState.selectedSlot(99) == 17, "large cursor defensive bound");
      for (var control : FactoryPlacerV2ReferenceGeometry.Control.values()) {
         var r = FactoryPlacerV2ReferenceGeometry.controlRect(control);
         double x = r.x() + r.width() / 2.0D, y = r.y() + r.height() / 2.0D;
         check(FactoryPlacerV2ReferenceGeometry.controlAt(x, y) == control, "control centre");
         check(!r.contains(r.x() - 0.001, y) && !r.contains(r.x() + r.width(), y), "control x edges");
         check(!r.contains(x, r.y() - 0.001) && !r.contains(x, r.y() + r.height()), "control y edges");
         check(FactoryPlacerV2ReferenceState.actionAt(x, y, 0, false, 0) != null, "empty left click sends");
         check(FactoryPlacerV2ReferenceState.actionAt(x, y, 1, false, 0) == null, "right click never sends");
         check(FactoryPlacerV2ReferenceState.actionAt(x, y, 2, false, 0) == null, "middle click never sends");
         check(FactoryPlacerV2ReferenceState.actionAt(x, y, 0, true, 0) == null, "carried stack never sends");
         for (var a : FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS) {
            check(a.hoverRight() <= r.x() || r.x() + r.width() <= a.hoverLeft()
               || a.hoverBottom() <= r.y() || r.y() + r.height() <= a.hoverTop(), "controls never intercept slots");
         }
      }
      check(FactoryPlacerV2ReferenceState.actionAt(-0.001, -0.001, 0, false, 0) == null, "outside routes to vanilla");
      check(FactoryPlacerV2ReferenceState.actionAt(150, 20, 0, false, 0) == null, "blank header no action");
      for (int row = 0; row < 3; row++) {
         var action = FactoryPlacerV2ReferenceState.actionFor(FactoryPlacerV2ReferenceGeometry.Control.values()[row], 0);
         check(action.action() == 22 && action.valueA() == row, "row selection native action22");
      }
      var manual = FactoryPlacerV2ReferenceState.actionFor(FactoryPlacerV2ReferenceGeometry.Control.MANUAL_TRIGGER, 6);
      check(manual.action() == 21 && manual.valueA() == 1, "manual trigger native action21");
      for (int base : new int[] {0, 2}) {
         int logic = base;
         for (int next : new int[] {1, 4, 0, 1, 4, 0}) {
            var action = FactoryPlacerV2ReferenceState.actionFor(FactoryPlacerV2ReferenceGeometry.Control.INVERSION, logic);
            check(action.action() == 20 && action.valueA() == (base | next), "two exact inversion cycles preserve bit2");
            logic = action.valueA();
         }
      }
      for (int logic = 0; logic < 8; logic++) {
         var action = FactoryPlacerV2ReferenceState.actionFor(FactoryPlacerV2ReferenceGeometry.Control.TRIGGER_MODE, logic);
         check(action.action() == 20 && action.valueA() == (logic ^ 2), "trigger toggles only bit2");
         check(FactoryPlacerV2ReferenceState.continuous(logic) == ((logic & 2) != 0), "truthful trigger state");
         check(FactoryPlacerV2ReferenceState.redstoneMode(logic) == ((logic & 4) != 0 ? 2 : logic & 1), "truthful three-state inversion");
      }
      check(!FactoryPlacerV2ReferenceState.signalActive(0) && FactoryPlacerV2ReferenceState.signalActive(1), "signal field projection");
      System.out.println("PASS: 54 measured slots, selection containment, ten viewport transforms, hover/drag, controls and native state/action parity");
   }
}
