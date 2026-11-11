package com.oblixorprime.engineersdecorreforged.utility;

import java.util.HashSet;

/** Dependency-free checks, executable with javac/java; not a Minecraft proof. */
public final class BarrelReferenceChecks {
   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }

   private static void checkExternalBounds(BarrelReferenceGeometry.ViewTransform v) {
      double left = v.screenX(0), top = v.screenY(0);
      double right = v.screenX(BarrelReferenceGeometry.CANVAS_WIDTH);
      double bottom = v.screenY(BarrelReferenceGeometry.CANVAS_HEIGHT);
      check(v.guiLeft() == Math.floor(left) && v.guiTop() == Math.floor(top), "external bounds floor top/left");
      check(v.guiLeft()+v.guiWidth() == Math.ceil(right)
         && v.guiTop()+v.guiHeight() == Math.ceil(bottom), "external bounds ceil bottom/right");
      check(left-v.guiLeft() >= 0 && left-v.guiLeft() < 1
         && top-v.guiTop() >= 0 && top-v.guiTop() < 1, "minimal outward top/left padding");
      check(v.guiLeft()+v.guiWidth()-right >= 0 && v.guiLeft()+v.guiWidth()-right < 1
         && v.guiTop()+v.guiHeight()-bottom >= 0 && v.guiTop()+v.guiHeight()-bottom < 1,
         "minimal outward bottom/right padding");
      check(v.guiWidth() > 0 && v.guiHeight() > 0, "positive external extent");
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
      for (int[] viewport : new int[][]{{1920,1080},{960,540},{640,360},{641,361},{1280,260},{480,270},{320,240},{300,600},{1,1},{1,1000},{1000,1}}) {
         var v = BarrelReferenceGeometry.fit(viewport[0], viewport[1]);
         check(v.originX() >= -0.0001 && v.originY() >= -0.0001, "positive origins");
         check(v.screenX(379) <= viewport[0]+0.0001 && v.screenY(461) <= viewport[1]+0.0001, "uniform fit");
         checkExternalBounds(v);
         for (var a : BarrelReferenceGeometry.SLOT_ANCHORS) {
            check(Math.abs(v.localX(v.screenX(a.x()+8))-(a.x()+8))<0.0001, "pointer x roundtrip");
            check(Math.abs(v.localY(v.screenY(a.y()+8))-(a.y()+8))<0.0001, "pointer y roundtrip");
            check(!a.containsHover(v.localX(v.screenX(a.hoverLeft()-0.001)),a.y()+8), "fractional left outside");
            check(!a.containsHover(v.localX(v.screenX(a.hoverRight()+0.001)),a.y()+8), "fractional right outside");
            check(!a.containsHover(a.x()+8,v.localY(v.screenY(a.hoverTop()-0.001))), "fractional top outside");
            check(!a.containsHover(a.x()+8,v.localY(v.screenY(a.hoverBottom()+0.001))), "fractional bottom outside");
         }
         check(Math.abs(v.localDelta(v.screenDelta(17.3))-17.3)<0.0001, "drag delta roundtrip");
         check(v.renderPointerX(v.screenX(-0.1)) == -1 && v.renderPointerY(v.screenY(-0.1)) == -1,
            "negative pointer flooring");
      }
      // Fractions on both axes, including negative origins, reject truncation or
      // rounding only the scaled extent instead of enclosing the transformed edges.
      for (float[] profile : new float[][]{{1,0.5F,0.5F,0,0,380,462},
         {0.5F,2.25F,-3.75F,2,-4,190,231}, {0.25F,-0.25F,0.25F,-1,0,96,116}}) {
         var v = new BarrelReferenceGeometry.ViewTransform(profile[0],profile[1],profile[2]);
         checkExternalBounds(v);
         check(v.guiLeft()==profile[3] && v.guiTop()==profile[4]
            && v.guiWidth()==profile[5] && v.guiHeight()==profile[6], "fractional external bounds");
      }
      // GUI pixels, not framebuffer pixels or the unscaled internal 379x461 canvas.
      // Even viewports center this odd-sized canvas on half-pixels at unit scale.
      for (int[] profile : new int[][]{{1280,720,1,450,129,380,462}, {1280,260,1,538,6,204,248},
         {1920,1080,1,770,309,380,462}, {1920,1080,2,290,39,380,462},
         {1920,1080,3,176,6,288,348}, {1921,1081,3,177,6,287,349}}) {
         var v=BarrelReferenceGeometry.fit((int)Math.ceil(profile[0]/(double)profile[2]),
            (int)Math.ceil(profile[1]/(double)profile[2]));
         checkExternalBounds(v);
         check(v.guiLeft()==profile[3] && v.guiTop()==profile[4]
            && v.guiWidth()==profile[5] && v.guiHeight()==profile[6], "external bounds match native profile");
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
      System.out.println("PASS: 38-slot mapping, eleven viewport transforms, three fractional and six native external-bound profiles, six framebuffer crops, hover, drag and fluid state checks");
   }
}
