package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.utility.Gui11V5CRuntimeGeometry;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import org.junit.Test;

import static org.junit.Assert.*;

/** Source-derived geometry and synchronized state rules for the GUI11 electrical panel. */
public final class Gui11V5CElectricalArtworkTest {
   @Test public void selectedModeKeepsTheExistingFourDetentsAndClickDirection() {
      assertEquals(270, ElectricalFurnaceArtwork.gui11DialAngleDegrees(0)); // OFF/down
      assertEquals(0, ElectricalFurnaceArtwork.gui11DialAngleDegrees(1));   // I/left (source pose)
      assertEquals(90, ElectricalFurnaceArtwork.gui11DialAngleDegrees(2));  // II/up
      assertEquals(180, ElectricalFurnaceArtwork.gui11DialAngleDegrees(3)); // III/right
      assertEquals(270, ElectricalFurnaceArtwork.gui11DialAngleDegrees(-1));
      assertEquals(180, ElectricalFurnaceArtwork.gui11DialAngleDegrees(4));
      assertEquals(1, ElectricalFurnaceArtwork.clickStep(0));
      assertEquals(-1, ElectricalFurnaceArtwork.clickStep(1));
      assertEquals(0, ElectricalFurnaceArtwork.clickStep(2));
   }

   @Test public void electricalModeActualDrawAndResidualHeatRemainSeparate() {
      // Selecting I alone is not proof of draw. OFF may still show residual warmth.
      assertFalse(ElectricalFurnaceArtwork.gui11CopperReflectionVisible(0));
      assertFalse(ElectricalFurnaceArtwork.gui11HeatWaveVisible(0, 0));
      assertTrue(ElectricalFurnaceArtwork.gui11HeatWaveVisible(12, 0));
      assertFalse(ElectricalFurnaceArtwork.gui11CopperReflectionVisible(0));
      assertTrue(ElectricalFurnaceArtwork.gui11HeatWaveVisible(0, 1600));
      assertTrue(ElectricalFurnaceArtwork.gui11CopperReflectionVisible(1600));
   }

   @Test public void energyFillIsZeroPartialOrFullInsideTheMeasuredGlassOnly() {
      assertEquals(0, Gui11V5CArtwork.energyFillPixels(MachineKind.SMALL_ELECTRICAL_FURNACE, 0, 32000));
      assertEquals(0, Gui11V5CArtwork.energyFillPixels(MachineKind.SMALL_ELECTRICAL_FURNACE, 100, 0));
      assertEquals(22, Gui11V5CArtwork.energyFillPixels(MachineKind.SMALL_ELECTRICAL_FURNACE, 16000, 32000));
      assertEquals(44, Gui11V5CArtwork.energyFillPixels(MachineKind.SMALL_ELECTRICAL_FURNACE, 32000, 32000));
      assertEquals(44, Gui11V5CArtwork.energyFillPixels(MachineKind.SMALL_ELECTRICAL_FURNACE, 33000, 32000));
      assertEquals(44, Gui11V5CArtwork.energyFillPixels(MachineKind.SMALL_LAB_FURNACE, 32000, 32000));
      var meter = Gui11V5CRuntimeGeometry.energy(MachineKind.SMALL_ELECTRICAL_FURNACE);
      assertNotNull(meter);
      assertTrue(meter.contains(meter.x() + 4, meter.y() + 3));
      assertTrue(meter.contains(meter.x() + 4 + 44 - 1, meter.y() + 3 + 5));
      int full = Gui11V5CArtwork.energyFillPixels(MachineKind.SMALL_ELECTRICAL_FURNACE, 32000, 32000);
      assertEquals("Full fill leaves the four-pixel right bezel intact", 4, meter.width() - 4 - full);
   }

   @Test public void rotaryHitBoxIsCenteredOnTheSourceArtPivot() {
      var dial = Gui11V5CRuntimeGeometry.control(MachineKind.SMALL_ELECTRICAL_FURNACE, "rotary");
      assertNotNull(dial);
      double middleY = dial.y() + dial.height() / 2.0;
      assertFalse("One pixel outside the dial is inert", dial.contains(dial.x() - 1, middleY));
      assertTrue("The first pixel in the dial is active", dial.contains(dial.x(), middleY));
      assertFalse("The far edge is exclusive", dial.contains(dial.x() + dial.width(), middleY));
      double pivotX = 988.0 * Gui11V5CRuntimeGeometry.width(MachineKind.SMALL_ELECTRICAL_FURNACE)
         / Gui11V5CRuntimeGeometry.sourceWidth(MachineKind.SMALL_ELECTRICAL_FURNACE);
      double pivotY = 482.0 * Gui11V5CRuntimeGeometry.height(MachineKind.SMALL_ELECTRICAL_FURNACE)
         / Gui11V5CRuntimeGeometry.sourceHeight(MachineKind.SMALL_ELECTRICAL_FURNACE);
      assertEquals(dial.x() + dial.width() / 2.0, pivotX, 0.5);
      assertEquals(dial.y() + dial.height() / 2.0, pivotY, 0.5);
   }
}
