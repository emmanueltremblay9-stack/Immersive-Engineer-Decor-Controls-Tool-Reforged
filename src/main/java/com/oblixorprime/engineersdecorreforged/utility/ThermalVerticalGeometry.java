package com.oblixorprime.engineersdecorreforged.utility;

import java.util.HashSet;
import java.util.Set;

/** Machine-specific presentation anchors measured from the normalized thermal artwork. */
public final class ThermalVerticalGeometry {
   public static final int CANVAS_WIDTH = 347;
   public static final int CANVAS_HEIGHT = 381;
   public static final int ITEM_SIZE = 16;
   public static final int VISIBLE_SLOTS = 38;
   private static final int[] F_INV_X={53,81,110,139,168,197,226,254,283};
   private static final int[] F_INV_Y={247,272,299};
   private static final int[] F_HOT_X={55,84,112,140,168,195,223,252,280};
   private static final int[] S_INV_X={55,83,110,137,164,191,218,245,272};
   private static final int[] S_INV_Y={246,273,299};
   private static final int[] S_HOT_X={56,83,110,137,164,191,217,244,272};

   private ThermalVerticalGeometry() { }
   public record Point(int x,int y) {
      public Point { if(x<0||y<0||x+ITEM_SIZE>CANVAS_WIDTH||y+ITEM_SIZE>CANVAS_HEIGHT) throw new IllegalArgumentException("Thermal slot outside canvas"); }
   }
   public static Point slot(int menuIndex, MachineKind kind) {
      boolean freezer=kind==MachineKind.SMALL_FREEZER;
      if(!freezer&&kind!=MachineKind.SMALL_MINERAL_SMELTER) throw new IllegalArgumentException("Not a thermal machine: "+kind);
      if(menuIndex==0) return freezer?new Point(167,67):new Point(164,66);
      if(menuIndex==2) return freezer?new Point(167,206):new Point(164,205);
      if(menuIndex==1||menuIndex<3||menuIndex>38) throw new IllegalArgumentException("No visible thermal slot "+menuIndex);
      if(menuIndex<30) {
         int i=menuIndex-3,row=i/9,col=i%9;
         return new Point(freezer?F_INV_X[col]:S_INV_X[col],freezer?F_INV_Y[row]:S_INV_Y[row]);
      }
      int col=menuIndex-30;
      return new Point(freezer?F_HOT_X[col]:S_HOT_X[col],freezer?335:337);
   }
   public static boolean contains(int menuIndex, MachineKind kind, double x,double y) {
      Point p=slot(menuIndex,kind); return x>=p.x&&x<p.x+ITEM_SIZE&&y>=p.y&&y<p.y+ITEM_SIZE;
   }
   public static boolean effectVisible(MachineKind kind, int phase, int flags) {
      return (kind==MachineKind.SMALL_FREEZER || kind==MachineKind.SMALL_MINERAL_SMELTER) && phase>0;
   }
   public static int energyClipTop(int y0,int y1,int stored,int capacity) {
      if(capacity<=0||stored<=0||y1<=y0) return y1;
      double fraction=Math.max(0.0,Math.min(1.0,(double)stored/capacity));
      return y1-(int)Math.round((y1-y0)*fraction);
   }
   public static void validate(MachineKind kind) {
      Set<String> seen=new HashSet<>();
      for(int i:new int[]{0,2}) { Point p=slot(i,kind); if(!seen.add(p.x+":"+p.y)) throw new IllegalStateException("Duplicate thermal slot"); }
      for(int i=3;i<=38;i++) { Point p=slot(i,kind); if(!seen.add(p.x+":"+p.y)) throw new IllegalStateException("Duplicate thermal slot"); }
      if(seen.size()!=VISIBLE_SLOTS) throw new IllegalStateException("Expected exactly 38 visible thermal slots");
   }
}
