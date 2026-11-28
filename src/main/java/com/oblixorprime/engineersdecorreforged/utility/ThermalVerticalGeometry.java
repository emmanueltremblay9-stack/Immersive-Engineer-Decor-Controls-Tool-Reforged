package com.oblixorprime.engineersdecorreforged.utility;

import java.util.HashSet;
import java.util.Set;

/** Runtime coordinates for the selected GUI11-R3 V5-Corrigee thermal panels. */
public final class ThermalVerticalGeometry {
   /** Freezer dimensions retained for older validation metadata; use canvasWidth/Height(kind) for either machine. */
   public static final int CANVAS_WIDTH = Gui11V5CRuntimeGeometry.width(MachineKind.SMALL_FREEZER);
   public static final int CANVAS_HEIGHT = Gui11V5CRuntimeGeometry.height(MachineKind.SMALL_FREEZER);
   public static final int ITEM_SIZE = 16;
   public static final int VISIBLE_SLOTS = 38;

   private ThermalVerticalGeometry() { }
   public record Point(int x,int y) {
      public Point { if(x<0||y<0) throw new IllegalArgumentException("Thermal slot outside canvas"); }
   }
   public static int canvasWidth(MachineKind kind) {
      requireThermal(kind);
      return Gui11V5CRuntimeGeometry.width(kind);
   }
   public static int canvasHeight(MachineKind kind) {
      requireThermal(kind);
      return Gui11V5CRuntimeGeometry.height(kind);
   }
   public static Point slot(int menuIndex, MachineKind kind) {
      requireThermal(kind);
      Point point = Gui11V5CRuntimeGeometry.slot(kind, menuIndex);
      if (point == null || menuIndex == 1) throw new IllegalArgumentException("No visible thermal slot "+menuIndex);
      if (point.x() + ITEM_SIZE > canvasWidth(kind) || point.y() + ITEM_SIZE > canvasHeight(kind))
         throw new IllegalStateException("Thermal slot outside canvas: " + menuIndex);
      return point;
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
   private static void requireThermal(MachineKind kind) {
      if (kind != MachineKind.SMALL_FREEZER && kind != MachineKind.SMALL_MINERAL_SMELTER)
         throw new IllegalArgumentException("Not a thermal machine: " + kind);
   }
}
