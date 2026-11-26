package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Proxy;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import org.junit.Test;

public class LabeledCrateBlockEntityRendererTest {
   private static final BlockPos CRATE = new BlockPos(8, 81, 3);

   @Test
   public void frameSamplesBothLightChannelsOnItsFacingSide() {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         BlockAndTintGetter world = lightOnlyAt(CRATE.relative(facing), 9, 13);
         assertEquals(facing.toString(), LightTexture.pack(9, 13),
            LabeledCrateBlockEntityRenderer.frameItemLight(world, CRATE, facing));
      }
   }

   @Test
   public void frameKeepsNaturalNightLightingAndDoesNotGlowInDarkness() {
      BlockPos front = CRATE.north();
      assertEquals(LightTexture.pack(0, 15),
         LabeledCrateBlockEntityRenderer.frameItemLight(lightOnlyAt(front, 0, 15), CRATE, Direction.NORTH));
      assertEquals(LightTexture.pack(0, 0),
         LabeledCrateBlockEntityRenderer.frameItemLight(lightOnlyAt(front, 0, 0), CRATE, Direction.NORTH));
   }

   private static BlockAndTintGetter lightOnlyAt(BlockPos litPos, int blockLight, int skyLight) {
      return (BlockAndTintGetter) Proxy.newProxyInstance(
         BlockAndTintGetter.class.getClassLoader(),
         new Class<?>[]{BlockAndTintGetter.class},
         (world, method, args) -> {
            if (!method.getName().equals("getBrightness")) throw new AssertionError(method);
            if (!litPos.equals(args[1])) return 0;
            return args[0] == LightLayer.BLOCK ? blockLight : skyLight;
         }
      );
   }
}
