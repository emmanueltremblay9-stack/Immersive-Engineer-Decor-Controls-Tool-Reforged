package com.oblixorprime.engineersdecorreforged.utility;

import java.util.ArrayList;
import java.util.List;

/** Retained source image coordinates; machine indices and server semantics are unchanged. */
public final class RetainedFurnaceLayout {
   public static final String ELECTRICAL = "retained_furnaces/electrical";
   public static final String INCINERATOR = "retained_furnaces/incinerator";
   private RetainedFurnaceLayout() {}
   public static boolean uses(String texture) { return ELECTRICAL.equals(texture) || INCINERATOR.equals(texture); }
   public static int px(String texture, int sourceX) { return (int)Math.round(sourceX * (ELECTRICAL.equals(texture) ? 228.0 / 1199 : 256.0 / 1254)); }
   public static int py(String texture, int sourceY) { return (int)Math.round(sourceY * (ELECTRICAL.equals(texture) ? 250.0 / 1312 : 256.0 / 1254)); }
   public static int playerX(String texture, int column) { return px(texture, ELECTRICAL.equals(texture) ? 199 + 100 * column : 196 + 108 * column) - 8; }
   public static int playerY(String texture, int row) { return py(texture, ELECTRICAL.equals(texture) ? 717 + 104 * row : 711 + 102 * row) - 8; }
   public static int hotbarX(String texture, int column) { return px(texture, ELECTRICAL.equals(texture) ? 207 + 98 * column : 207 + 106 * column) - 8; }
   private static MachineLayout.MachineSlot slot(String texture, int index, int x, int y, String role) {
      return new MachineLayout.MachineSlot(index, px(texture,x)-8, py(texture,y)-8, role);
   }
   public static MachineLayout electrical() {
      String t = ELECTRICAL;
      return new MachineLayout(228,250,playerX(t,0),playerY(t,0),hotbarX(t,0),py(t,1073)-8,
         List.of(slot(t,0,483,274,"Input"),slot(t,1,205,454,"Aux"),slot(t,2,724,274,"Output"),
            slot(t,3,342,274,"Input FIFO"),slot(t,4,200,274,"Input FIFO"),
            slot(t,5,865,274,"Output FIFO"),slot(t,6,1007,274,"Output FIFO")),
         List.of(),MachineLayout.LayoutStyle.ORIGINAL_TEXTURE,t,19,20,19);
   }
   public static MachineLayout incinerator() {
      String t = INCINERATOR;
      List<MachineLayout.MachineSlot> slots = new ArrayList<>();
      for (int i=0;i<8;i++) slots.add(slot(t,i,201+124*i,255,"Queue"));
      for (int i=8;i<15;i++) slots.add(slot(t,i,1094-113*(i-8),439,"Queue"));
      slots.add(slot(t,15,213,484,"Queue"));
      return new MachineLayout(256,256,playerX(t,0),playerY(t,0),hotbarX(t,0),py(t,1056)-8,
         slots,List.of(),MachineLayout.LayoutStyle.ORIGINAL_TEXTURE,t,22,21,22);
   }
}
