package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
import org.junit.Test;

public class MachineScreenSolarPanelBoundsTest {
   @Test
   public void readoutMaskAndTooltipStayInsideThePrintedEnergyPanel() {
      MachineLayout layout = MachineLayout.forKind(MachineKind.SMALL_SOLAR_PANEL);
      MachineLayout.Panel meter = panel(layout, "solar-energy-bar");
      MachineLayout.Panel readout = panel(layout, "solar-energy-readout-mask");

      assertEquals(26, meter.x());
      assertEquals(140, meter.x() + meter.width());
      assertEquals(146, readout.x());
      assertEquals(60, readout.width());
      assertEquals(206, readout.x() + readout.width());
      assertTrue(readout.x() + readout.width() <= layout.imageWidth());

      assertTrue(MachineScreen.containsSolarPanelEnergyTooltip(layout, 26, 104));
      assertTrue(MachineScreen.containsSolarPanelEnergyTooltip(layout, 205, 120));
      assertFalse(MachineScreen.containsSolarPanelEnergyTooltip(layout, 25, 110));
      assertFalse(MachineScreen.containsSolarPanelEnergyTooltip(layout, 206, 110));
      assertFalse(MachineScreen.containsSolarPanelEnergyTooltip(layout, 180, 121));
      assertFalse(MachineScreen.containsSolarPanelEnergyTooltip(layout, 179, 62));
      assertFalse(MachineScreen.containsSolarPanelEnergyTooltip(layout, layout.playerSlotX(0), layout.playerSlotY(0)));
   }

   @Test
   public void energyReadoutShrinksToFitInsteadOfClippingItsFinalDigitsOrUnit() {
      float scale = MachineScreen.fittedTextScale(92, 58, 0.72F);

      assertTrue(scale > 0.0F);
      assertTrue(scale < 0.72F);
      assertTrue(92 * scale <= 58.0F);
      assertEquals(0.0F, MachineScreen.fittedTextScale(0, 58, 0.72F), 0.0F);
      assertEquals(0.0F, MachineScreen.fittedTextScale(92, 0, 0.72F), 0.0F);
   }

   private static MachineLayout.Panel panel(MachineLayout layout, String label) {
      return layout.panels().stream().filter(candidate -> candidate.label().equals(label)).findFirst().orElseThrow();
   }
}
