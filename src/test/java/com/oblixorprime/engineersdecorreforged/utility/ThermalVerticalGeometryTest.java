package com.oblixorprime.engineersdecorreforged.utility;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThermalVerticalGeometryTest {
   @Test public void eachMachineHasExactlyThirtyEightUniqueSixteenPixelSlots() {
      for (MachineKind kind : new MachineKind[]{MachineKind.SMALL_FREEZER,MachineKind.SMALL_MINERAL_SMELTER}) {
         ThermalVerticalGeometry.validate(kind);
         Set<String> occupied=new HashSet<>();
         for(int i:new int[]{0,2}) assertRect(kind,i,occupied);
         for(int i=3;i<=38;i++) assertRect(kind,i,occupied);
         assertEquals(38*16*16,occupied.size());
         assertThrows(IllegalArgumentException.class,()->ThermalVerticalGeometry.slot(1,kind));
      }
   }
   private static void assertRect(MachineKind kind,int i,Set<String> occupied) {
      var p=ThermalVerticalGeometry.slot(i,kind);
      assertTrue(ThermalVerticalGeometry.contains(i,kind,p.x()+8,p.y()+8));
      assertFalse(ThermalVerticalGeometry.contains(i,kind,p.x()+16,p.y()+8));
      for(int x=p.x();x<p.x()+16;x++) for(int y=p.y();y<p.y()+16;y++)
         assertTrue("Overlapping slot pixel for "+kind+" "+i,occupied.add(x+":"+y));
   }
   @Test public void inactiveEffectsAndEmptyEnergyHaveNoOverlay() {
      assertFalse(ThermalVerticalGeometry.effectVisible(MachineKind.SMALL_FREEZER,0,0));
      assertFalse(ThermalVerticalGeometry.effectVisible(MachineKind.SMALL_MINERAL_SMELTER,0,0));
      assertFalse(ThermalVerticalGeometry.effectVisible(MachineKind.SMALL_MINERAL_SMELTER,0,3));
      assertTrue(ThermalVerticalGeometry.effectVisible(MachineKind.SMALL_MINERAL_SMELTER,1,0));
      assertEquals(192,ThermalVerticalGeometry.energyClipTop(89,192,0,100));
      assertEquals(192,ThermalVerticalGeometry.energyClipTop(89,192,0,0));
      assertEquals(140,ThermalVerticalGeometry.energyClipTop(89,192,50,100));
      assertEquals(89,ThermalVerticalGeometry.energyClipTop(89,192,100,100));
   }

   @Test public void freezerOutputAndThermalViewportUseTheCorrectedUniformTransform() {
      var output=ThermalVerticalGeometry.slot(2,MachineKind.SMALL_FREEZER);
      assertEquals(206,output.y());
      var view=FreezerReferenceGeometry.fitCanvas(960,570,347,381,0.78F);
      assertEquals(0.78F,view.scale(),0.0001F);
      double screenX=view.originX()+view.scale()*(output.x()+8);
      double screenY=view.originY()+view.scale()*(output.y()+8);
      assertEquals(output.x()+8,view.localX(screenX),0.0001D);
      assertEquals(output.y()+8,view.localY(screenY),0.0001D);
   }
}
