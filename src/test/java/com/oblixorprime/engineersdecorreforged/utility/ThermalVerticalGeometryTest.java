package com.oblixorprime.engineersdecorreforged.utility;

import com.oblixorprime.engineersdecorreforged.client.Gui11V5CArtwork;
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

   @Test public void measuredThermalOutputAndFitMatchBothActivePanels() throws Exception {
      verifyActivePanel(MachineKind.SMALL_FREEZER, "05", "29ea3ff661c6591a3b89ee97d3a33d8817f437745926eb566338cbb22bcd8dc6", 281, 313, 133, 146);
      verifyActivePanel(MachineKind.SMALL_MINERAL_SMELTER, "06", "3b278f9065c945a697e6e0316f4c4e9b96815e7372eca7410979a5220c9c5f20", 279, 312, 132, 145);
   }

   private static void verifyActivePanel(MachineKind kind, String panel, String sha256,
                                         int width, int height, int outputX, int outputY) throws Exception {
      var image=Gui11V5CArtworkChecks.panel("gui11_v5c/"+panel+".png",sha256,1187,1325);
      assertEquals(width,ThermalVerticalGeometry.canvasWidth(kind));
      assertEquals(height,ThermalVerticalGeometry.canvasHeight(kind));
      var output=ThermalVerticalGeometry.slot(2,kind);
      assertEquals(outputX,output.x());
      assertEquals(outputY,output.y());
      for(int i=0;i<39;i++) {
         if(i==1) continue;
         var slot=ThermalVerticalGeometry.slot(i,kind);
         Gui11V5CArtworkChecks.darkInterior(image,width,height,slot.x()+6,slot.y()+6,4,4);
      }
      for(int[] viewport : new int[][] {{960,570}, {320,260}}) {
         var view=FreezerReferenceGeometry.fitCanvas(viewport[0],viewport[1],width,height,0.78F);
         assertTrue(view.originX()>=0 && view.originY()>=0);
         assertTrue(view.screenX(width)<=viewport[0]+0.0001 && view.screenY(height)<=viewport[1]+0.0001);
         assertEquals(output.x()+8,view.localX(view.screenX(output.x()+8)),0.0001D);
         assertEquals(output.y()+8,view.localY(view.screenY(output.y()+8)),0.0001D);
      }
      assertEquals(0,Gui11V5CArtwork.energyFillPixels(kind,0,16000));
      assertEquals(22,Gui11V5CArtwork.energyFillPixels(kind,8000,16000));
      assertEquals(44,Gui11V5CArtwork.energyFillPixels(kind,16000,16000));
   }
}
