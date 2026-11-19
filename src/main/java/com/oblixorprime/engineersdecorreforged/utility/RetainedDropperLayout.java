package com.oblixorprime.engineersdecorreforged.utility;
import java.util.ArrayList;
import java.util.List;
/** Approved V2 source coordinates; unchanged slot IDs and action protocol. */
public final class RetainedDropperLayout {
 public static final String TEXTURE="retained_dropper/background";
 private RetainedDropperLayout() {}
 public static int x(int source) {return (int)Math.round(source*228.0/1199);}
 public static int y(int source) {return (int)Math.round(source*250.0/1312);}
 public static int playerX(int column) {return x(199+100*column)-8;}
 public static int playerY(int row) {return y(740+96*row)-8;}
 public static int hotbarX(int column) {return x(207+98*column)-8;}
 public static MachineLayout layout() {
  var slots=new ArrayList<MachineLayout.MachineSlot>();
  for(int row=0;row<2;row++)for(int col=0;col<6;col++)
   slots.add(new MachineLayout.MachineSlot(row*6+col,x(154+99*col)-8,y(239+108*row)-8,"Input"));
  for(int i=0;i<3;i++)slots.add(new MachineLayout.MachineSlot(12+i,x(202+174*i)-8,y(474)-8,"Filter"));
  return new MachineLayout(228,250,playerX(0),playerY(0),hotbarX(0),y(1075)-8,
   slots,List.of(),MachineLayout.LayoutStyle.ORIGINAL_TEXTURE,TEXTURE,19,18,19);
 }
}
