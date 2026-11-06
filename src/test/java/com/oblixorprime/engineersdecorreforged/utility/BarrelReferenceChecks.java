package com.oblixorprime.engineersdecorreforged.utility;

import java.util.HashSet;

/** Dependency-free checks, executable with javac/java; not a Minecraft proof. */
public final class BarrelReferenceChecks {
   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
   public static void main(String[] args) {
      check(BarrelReferenceGeometry.SOURCE_WIDTH == 1137 && BarrelReferenceGeometry.SOURCE_HEIGHT == 1383, "retained source dimensions");
      check(BarrelReferenceGeometry.SOURCE_WIDTH == 3 * BarrelReferenceGeometry.CANVAS_WIDTH
         && BarrelReferenceGeometry.SOURCE_HEIGHT == 3 * BarrelReferenceGeometry.CANVAS_HEIGHT, "exact uniform 1:3 canvas");
      var indices = new HashSet<Integer>();
      check(BarrelReferenceGeometry.SLOT_ANCHORS.size() == 38, "38 slots");
      for (var a : BarrelReferenceGeometry.SLOT_ANCHORS) {
         check(indices.add(a.menuIndex()), "unique index");
         check(BarrelReferenceGeometry.anchor(a.menuIndex()) == a, "index lookup");
         check(a.x() >= a.interiorLeft() && a.y() >= a.interiorTop(), "item top/left inside artwork");
         check(a.x()+16 <= a.interiorRight() && a.y()+16 <= a.interiorBottom(), "item bottom/right inside artwork");
         check(a.containsHover(a.x()+8, a.y()+8), "hover centre");
         check(!a.containsHover(a.hoverLeft()-0.001, a.y()+8), "outside left");
         check(!a.containsHover(a.hoverRight(), a.y()+8), "outside right");
         check(!a.containsHover(a.x()+8, a.hoverTop()-0.001), "outside top");
         check(!a.containsHover(a.x()+8, a.hoverBottom()), "outside bottom");
         for (var b : BarrelReferenceGeometry.SLOT_ANCHORS) {
            if (a != b) check(a.hoverRight() <= b.hoverLeft() || b.hoverRight() <= a.hoverLeft()
               || a.hoverBottom() <= b.hoverTop() || b.hoverBottom() <= a.hoverTop(), "nonoverlapping targets");
         }
      }
      for (int i=0;i<38;i++) check(indices.contains(i), "protocol index present");
      for (int[] viewport : new int[][]{{1920,1080},{960,540},{640,360},{480,270},{320,240},{300,600},{1,1},{1,1000},{1000,1}}) {
         var v = BarrelReferenceGeometry.fit(viewport[0], viewport[1]);
         check(v.originX() >= -0.0001 && v.originY() >= -0.0001, "positive origins");
         check(v.screenX(379) <= viewport[0]+0.0001 && v.screenY(461) <= viewport[1]+0.0001, "uniform fit");
         for (var a : BarrelReferenceGeometry.SLOT_ANCHORS) {
            check(Math.abs(v.localX(v.screenX(a.x()+8))-(a.x()+8))<0.0001, "pointer x roundtrip");
            check(Math.abs(v.localY(v.screenY(a.y()+8))-(a.y()+8))<0.0001, "pointer y roundtrip");
         }
         check(Math.abs(v.localDelta(v.screenDelta(17.3))-17.3)<0.0001, "drag delta roundtrip");
         check(v.renderPointerX(v.screenX(-0.1)) == -1, "negative pointer flooring");
      }
      // These fixed framebuffer bounds also appear in the Python evidence tests.
      // Round GUI scissor coordinates first, then apply the actual window scale.
      for(int[] profile:new int[][]{{1280,720,1,621,245,40,77},{1280,260,1,630,68,21,41},
         {1920,1080,1,941,425,40,77},{1920,1080,2,922,310,80,154},
         {1920,1080,3,918,279,90,174},{1921,1081,3,918,282,90,174}}) {
         int scale=profile[2];
         var v=BarrelReferenceGeometry.fit((int)Math.ceil(profile[0]/(double)scale),(int)Math.ceil(profile[1]/(double)scale));
         int x0=(int)Math.ceil(v.screenX(170))*scale,y0=(int)Math.ceil(v.screenY(115))*scale;
         int x1=(int)Math.floor(v.screenX(211))*scale,y1=(int)Math.floor(v.screenY(193))*scale;
         check(x0==profile[3] && y0==profile[4] && x1-x0==profile[5] && y1-y0==profile[6],"Native framebuffer crop geometry");
      }
      check(BarrelReferenceState.fillHeight(-1,16000)==0,"negative amount");
      check(BarrelReferenceState.fillHeight(1,0)==0,"invalid capacity");
      check(BarrelReferenceState.fillHeight(0,16000)==0,"empty");
      check(BarrelReferenceState.fillHeight(8000,16000)==39,"half");
      check(BarrelReferenceState.fillHeight(16000,16000)==78,"full");
      check(BarrelReferenceState.fillHeight(Integer.MAX_VALUE,16000)==78,"overflow safe");
      check(BarrelReferenceState.fillHeight(Integer.MAX_VALUE,Integer.MAX_VALUE)==78,"large capacity");
      int previous=0;
      for(int i=0;i<=16000;i++) {
         int fill=BarrelReferenceState.fillHeight(i,16000);
         check(fill>=previous && fill<=78,"monotonic bounded fill"); previous=fill;
      }
      System.out.println("PASS: 38-slot mapping, bounds, nine viewport transforms, six framebuffer crops, hover, drag and fluid state checks");
   }
}
