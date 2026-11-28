package com.oblixorprime.engineersdecorreforged.utility;

import static org.junit.Assert.*;
import java.util.HashSet;
import org.junit.Test;

public class RetainedFurnaceLayoutTest {
   @Test public void electricalPreservesSevenIndicesAndOutputs() {
      MachineLayout l=MachineLayout.forKind(MachineKind.SMALL_ELECTRICAL_FURNACE);
      assertEquals(RetainedFurnaceLayout.ELECTRICAL,l.textureName());
      check(l,7);
      assertTrue(l.isOutputSlot(2)); assertTrue(l.isOutputSlot(5)); assertTrue(l.isOutputSlot(6));
      assertFalse(l.isOutputSlot(0));
   }
   @Test public void incineratorPreservesSixteenRecoverableQueueSlots() {
      MachineLayout l=MachineLayout.forKind(MachineKind.SMALL_WASTE_INCINERATOR);
      assertEquals(RetainedFurnaceLayout.INCINERATOR,l.textureName());
      check(l,16);
      for (int i=0;i<16;i++) assertFalse(l.isOutputSlot(i));
   }
   @Test public void provisionalLabArtIsNotSelected() {
      assertEquals("small_lab_furnace_gui",MachineLayout.forKind(MachineKind.SMALL_LAB_FURNACE).textureName());
   }
   private void check(MachineLayout l,int count) {
      assertEquals(count,l.machineSlots().size());
      HashSet<Integer> seen=new HashSet<>();
      for(var s:l.machineSlots()) {
         assertTrue(seen.add(s.containerIndex()));
         assertTrue(s.x()>=0 && s.x()+16<=l.imageWidth());
         assertTrue(s.y()>=0 && s.y()+16<l.playerInventoryY());
      }
      for(int i=0;i<count;i++) assertTrue(seen.contains(i));
      for(int c=0;c<9;c++) {
         assertTrue(l.playerSlotX(c)>=0 && l.playerSlotX(c)+16<=l.imageWidth());
         assertTrue(l.hotbarSlotX(c)>=0 && l.hotbarSlotX(c)+16<=l.imageWidth());
         if(c>0) {assertTrue(l.playerSlotX(c)-l.playerSlotX(c-1)>=16); assertTrue(l.hotbarSlotX(c)-l.hotbarSlotX(c-1)>=16);}
      }
      for(int row=1;row<3;row++) assertTrue(l.playerSlotY(row)-l.playerSlotY(row-1)>=16);
      assertTrue(l.hotbarY()>=l.playerSlotY(2)+16);
   }
}
