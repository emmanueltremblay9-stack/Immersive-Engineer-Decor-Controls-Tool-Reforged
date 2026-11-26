package com.oblixorprime.engineersdecorreforged.client;

import org.junit.Test;
import static org.junit.Assert.*;

public class LabeledCrateBlockEntityRendererTest {
   @Test public void frameIconKeepsWorldSkyAndAddsOnlyAMinimumBlockLight() {
      int day = (13 << 20) | (2 << 4);
      int night = (1 << 20) | (0 << 4);
      int dayResult=LabeledCrateBlockEntityRenderer.minimumItemLight(day);
      int nightResult=LabeledCrateBlockEntityRenderer.minimumItemLight(night);
      assertEquals(13,(dayResult>>>20)&0xF);
      assertEquals(1,(nightResult>>>20)&0xF);
      assertEquals(4,(dayResult>>>4)&0xF);
      assertEquals(4,(nightResult>>>4)&0xF);
      int bright=(15<<20)|(9<<4);
      assertEquals(bright,LabeledCrateBlockEntityRenderer.minimumItemLight(bright));
   }
}
