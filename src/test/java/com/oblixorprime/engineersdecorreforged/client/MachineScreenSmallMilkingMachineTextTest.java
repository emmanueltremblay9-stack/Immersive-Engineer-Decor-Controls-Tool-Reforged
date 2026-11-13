package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertEquals;

import java.text.NumberFormat;
import java.util.Locale;
import org.junit.Test;

public class MachineScreenSmallMilkingMachineTextTest {
   @Test
   public void tankFillClampsToTheSynchronizedAmountAndCapacity() {
      assertEquals(0, MachineScreen.smallMilkingMachineFillPixels(0, 4000, 51));
      assertEquals(25, MachineScreen.smallMilkingMachineFillPixels(2000, 4000, 51));
      assertEquals(51, MachineScreen.smallMilkingMachineFillPixels(5000, 4000, 51));
      assertEquals(0, MachineScreen.smallMilkingMachineFillPixels(-1, 4000, 51));
      assertEquals(0, MachineScreen.smallMilkingMachineFillPixels(1000, 0, 51));
   }

   @Test
   public void amountTooltipUsesTheSelectedLocaleAndMenuCapacity() {
      assertEquals("2,000 / 4,000 mB", MachineScreen.formatSmallMilkingMachineAmount(2000, 4000, Locale.US));
      NumberFormat french = NumberFormat.getIntegerInstance(Locale.FRANCE);
      assertEquals(french.format(2000) + " / " + french.format(4000) + " mB",
         MachineScreen.formatSmallMilkingMachineAmount(2000, 4000, Locale.FRANCE));
   }
}
