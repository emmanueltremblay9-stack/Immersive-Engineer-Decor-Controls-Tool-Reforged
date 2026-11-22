package com.oblixorprime.engineersdecorreforged.utility;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ThermalVerticalGeometryTest {
   @Test
   public void verticalMachineSlotMapAndThirtySixPlayerSlotsDoNotOverlap() {
      var input = ThermalVerticalGeometry.slot(0);
      var output = ThermalVerticalGeometry.slot(2);
      assertEquals(input.x(), output.x());
      assertTrue(input.y() + 16 < output.y());
      Set<String> occupied = new HashSet<>();
      for (int index = 0; index <= 38; index++) {
         if (index == 1) continue;
         var p = ThermalVerticalGeometry.slot(index);
         for (int x = p.x(); x < p.x() + 16; x++) {
            for (int y = p.y(); y < p.y() + 16; y++)
               assertTrue("Overlapping slot pixel", occupied.add(x + ":" + y));
         }
      }
      assertEquals(38 * 16 * 16, occupied.size());
      assertThrows(IllegalArgumentException.class, () -> ThermalVerticalGeometry.slot(1));
   }

   @Test
   public void allPlayerSlotsInNineColumnsAndFourRows() {
      for (int row = 0; row < 4; row++) {
         int start = 3 + row * 9;
         int y = ThermalVerticalGeometry.slot(start).y();
         for (int col = 0; col < 9; col++) {
            var p = ThermalVerticalGeometry.slot(start + col);
            assertEquals(y, p.y());
            assertEquals(ThermalVerticalGeometry.slot(start).x() + col * 25, p.x());
         }
      }
      assertTrue(ThermalVerticalGeometry.contains(0, 150, 55));
      assertFalse(ThermalVerticalGeometry.contains(2, 150, 55));
   }
}
