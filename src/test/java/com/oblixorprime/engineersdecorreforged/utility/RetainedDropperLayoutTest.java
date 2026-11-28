package com.oblixorprime.engineersdecorreforged.utility;
import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
public class RetainedDropperLayoutTest {
 @Test public void preservesHistoricalTextureAndTwelveInputsAndThreeFilters(){
  var l=MachineLayout.forKind(MachineKind.FACTORY_DROPPER);
  assertEquals("factory_dropper_gui",l.textureName());
  assertEquals(176,l.imageWidth());
  assertEquals(166,l.imageHeight());
  assertEquals(15,l.machineSlots().size());
  for(int i=0;i<15;i++) {
   var slot=l.machineSlots().get(i);
   assertEquals(i,slot.containerIndex());
   assertEquals(i<12?"Input":"Filter",slot.role());
   int expectedX=i<12?10+(i%6)*18:new int[]{19,55,91}[i-12];
   int expectedY=i<12?6+(i/6)*18:48;
   assertEquals(expectedX,slot.x());
   assertEquals(expectedY,slot.y());
   assertFalse(l.isOutputSlot(i));
  }
 }
 @Test public void allFiftyOneAnchorsStayBoundedAndSeparate(){
  var l=MachineLayout.forKind(MachineKind.FACTORY_DROPPER);
  var boxes=new ArrayList<int[]>();
  for(var s:l.machineSlots())boxes.add(new int[]{s.x(),s.y()});
  for(int r=0;r<3;r++)for(int c=0;c<9;c++)boxes.add(new int[]{l.playerSlotX(c),l.playerSlotY(r)});
  for(int c=0;c<9;c++)boxes.add(new int[]{l.hotbarSlotX(c),l.hotbarY()});
  assertEquals(51,boxes.size());
  for(int i=0;i<boxes.size();i++){
   int[] a=boxes.get(i);assertTrue(a[0]>=0&&a[1]>=0&&a[0]+16<=l.imageWidth()&&a[1]+16<=l.imageHeight());
   for(int j=0;j<i;j++) {int[] b=boxes.get(j);assertFalse(a[0]<b[0]+16&&a[0]+16>b[0]&&a[1]<b[1]+16&&a[1]+16>b[1]);}
  }
 }
}
