package com.oblixorprime.engineersdecorreforged.utility;

import java.util.HashSet;

/** Dependency-free geometry/arithmetic regression checks, not a Minecraft runtime proof. */
public final class SolarV2ReferenceChecks {
   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
   public static void main(String[] args) {
      check(SolarV2ReferenceGeometry.SOURCE_WIDTH == 1254 && SolarV2ReferenceGeometry.SOURCE_HEIGHT == 1254, "retained source dimensions");
      check(SolarV2ReferenceGeometry.SOURCE_WIDTH == 3 * SolarV2ReferenceGeometry.CANVAS_WIDTH
         && SolarV2ReferenceGeometry.SOURCE_HEIGHT == 3 * SolarV2ReferenceGeometry.CANVAS_HEIGHT, "exact uniform 1:3 canvas");
      var indices = new HashSet<Integer>();
      check(SolarV2ReferenceGeometry.SLOT_ANCHORS.size() == 37, "37 slots");
      for (var a : SolarV2ReferenceGeometry.SLOT_ANCHORS) {
         check(indices.add(a.menuIndex()), "unique index");
         check(SolarV2ReferenceGeometry.anchor(a.menuIndex()) == a, "index lookup");
         check(a.x() >= a.interiorLeft() && a.y() >= a.interiorTop(), "item top/left inside artwork");
         check(a.x()+16 <= a.interiorRight() && a.y()+16 <= a.interiorBottom(), "item bottom/right inside artwork");
         check(a.containsHover(a.x()+8, a.y()+8), "hover centre");
         check(!a.containsHover(a.hoverLeft()-0.001, a.y()+8), "outside left");
         check(!a.containsHover(a.hoverRight(), a.y()+8), "outside right");
         check(!a.containsHover(a.x()+8, a.hoverTop()-0.001), "outside top");
         check(!a.containsHover(a.x()+8, a.hoverBottom()), "outside bottom");
         for (var b : SolarV2ReferenceGeometry.SLOT_ANCHORS) {
            if (a != b) check(a.hoverRight() <= b.hoverLeft() || b.hoverRight() <= a.hoverLeft()
               || a.hoverBottom() <= b.hoverTop() || b.hoverBottom() <= a.hoverTop(), "nonoverlapping targets");
         }
      }
      for (int i=0;i<37;i++) check(indices.contains(i), "protocol index present");
      for (int invalid : new int[]{-1,37,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
         try { SolarV2ReferenceGeometry.anchor(invalid); throw new AssertionError("Invalid menu index accepted"); }
         catch (IllegalArgumentException expected) { }
      }
      for (int[] viewport : new int[][]{{1920,1080},{960,540},{640,360},{1280,260},{480,270},{320,240},{300,600},{1,1},{1,1000},{1000,1}}) {
         var v = SolarV2ReferenceGeometry.fit(viewport[0], viewport[1]);
         check(v.originX() >= -0.0001 && v.originY() >= -0.0001, "positive origins");
         check(v.screenX(418) <= viewport[0]+0.0001 && v.screenY(418) <= viewport[1]+0.0001, "uniform fit");
         for (var a : SolarV2ReferenceGeometry.SLOT_ANCHORS) {
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
         try { SolarV2ReferenceGeometry.fit(invalid[0],invalid[1]); throw new AssertionError("Invalid viewport accepted"); }
         catch (IllegalArgumentException expected) { }
      }
      check(SolarV2ReferenceState.METER_X*3 >=146 && SolarV2ReferenceState.METER_Y*3 >=588
         && (SolarV2ReferenceState.METER_X+SolarV2ReferenceState.METER_WIDTH)*3<=997
         && (SolarV2ReferenceState.METER_Y+SolarV2ReferenceState.METER_HEIGHT)*3<=625,"meter inside measured source window");
      check(SolarV2ReferenceState.fillWidth(-1,64000)==0,"negative amount");
      check(SolarV2ReferenceState.fillWidth(1,0)==0,"invalid capacity");
      check(SolarV2ReferenceState.fillWidth(0,64000)==0,"empty");
      check(SolarV2ReferenceState.fillWidth(100,64000)==0,"subpixel charge remains visually empty");
      check(SolarV2ReferenceState.fillWidth(32000,64000)==141,"half");
      check(SolarV2ReferenceState.fillWidth(64000,64000)==283,"full");
      check(SolarV2ReferenceState.fillWidth(Integer.MAX_VALUE,64000)==283,"overflow safe");
      check(SolarV2ReferenceState.fillWidth(Integer.MAX_VALUE,Integer.MAX_VALUE)==283,"large capacity");
      check(SolarV2ReferenceState.FILL_COLOR==0xFFF2A432 && SolarV2ReferenceState.HIGHLIGHT_COLOR==0xFFFFD06A,"existing FE palette");
      check(SolarV2ReferenceState.containsMeter(49,196) && SolarV2ReferenceState.containsMeter(331.999,207.999),"meter hit inside");
      check(!SolarV2ReferenceState.containsMeter(48.999,196) && !SolarV2ReferenceState.containsMeter(49,195.999)
         && !SolarV2ReferenceState.containsMeter(332,196) && !SolarV2ReferenceState.containsMeter(49,208),"meter hit outside");
      int previous=0;
      for(int i=0;i<=64000;i++) {
         int fill=SolarV2ReferenceState.fillWidth(i,64000);
         check(fill>=previous && fill<=283,"monotonic bounded fill"); previous=fill;
      }
      System.out.println("PASS: 37-slot mapping, measured bounds, ten viewport transforms, hover, drag, empty/half/full and FE arithmetic");
   }
}
