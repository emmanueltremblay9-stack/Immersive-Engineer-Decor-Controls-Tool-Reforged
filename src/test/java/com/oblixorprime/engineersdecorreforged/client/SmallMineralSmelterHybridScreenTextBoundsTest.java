package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SmallMineralSmelterHybridScreenTextBoundsTest {
   @Test
   public void textScalePreservesPreferredSizeWhenTheLocalizedStringFits() {
      assertEquals(0.60F,
         SmallMineralSmelterHybridScreen.fittedTextScale(48, 39, 0.60F), 0.0001F);
   }

   @Test
   public void textScaleFitsLongModeAndReadingLabelsInsideTheirPanels() {
      assertEquals(0.40F,
         SmallMineralSmelterHybridScreen.fittedTextScale(80, 32, 0.60F), 0.0001F);
      assertEquals(0.35F,
         SmallMineralSmelterHybridScreen.fittedTextScale(100, 35, 0.50F), 0.0001F);
   }

   @Test
   public void emptyTextOrInvalidPanelDoesNotProduceAnUnboundedScale() {
      assertEquals(0.0F,
         SmallMineralSmelterHybridScreen.fittedTextScale(0, 32, 0.60F), 0.0001F);
      assertEquals(0.0F,
         SmallMineralSmelterHybridScreen.fittedTextScale(80, 0, 0.60F), 0.0001F);
   }
}
