package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.compat.ImmersiveEngineeringTemplateRepair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class ImmersiveEngineeringTemplateRepairGameTests {
   private ImmersiveEngineeringTemplateRepairGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void fluid_pipe_template_state_is_repaired_from_malformed_entity_nbt(GameTestHelper helper) {
      ResourceLocation fluidPipeId = ImmersiveEngineeringTemplateRepair.FLUID_PIPE_BLOCK;
      ResourceLocation lightEngineeringId = ResourceLocation.fromNamespaceAndPath("immersiveengineering", "light_engineering");
      Block fluidPipe = BuiltInRegistries.BLOCK.get(fluidPipeId);
      Block lightEngineering = BuiltInRegistries.BLOCK.get(lightEngineeringId);
      helper.assertTrue(fluidPipe != Blocks.AIR, "IE fluid_pipe must be registered");
      helper.assertTrue(lightEngineering != Blocks.AIR, "IE light_engineering must be registered");

      CompoundTag nbt = new CompoundTag();
      nbt.putString("id", ImmersiveEngineeringTemplateRepair.FLUID_PIPE_BLOCK_ENTITY.toString());
      StructureBlockInfo malformed = new StructureBlockInfo(BlockPos.ZERO, lightEngineering.defaultBlockState(), nbt);
      StructureBlockInfo repaired = ImmersiveEngineeringTemplateRepair.repairFluidPipeState(malformed);

      helper.assertTrue(repaired.state().is(fluidPipe), "fluidpipe NBT must use the IE fluid_pipe block state");
      helper.assertTrue(repaired.nbt() == nbt, "template repair must preserve the block entity NBT");

      StructureBlockInfo alreadyValid = new StructureBlockInfo(BlockPos.ZERO, fluidPipe.defaultBlockState(), nbt);
      helper.assertTrue(
         ImmersiveEngineeringTemplateRepair.repairFluidPipeState(alreadyValid) == alreadyValid,
         "valid fluid_pipe template entries must remain unchanged"
      );

      StructureBlockInfo unrelatedState = new StructureBlockInfo(BlockPos.ZERO, Blocks.STONE.defaultBlockState(), nbt);
      helper.assertTrue(
         ImmersiveEngineeringTemplateRepair.repairFluidPipeState(unrelatedState) == unrelatedState,
         "only the light_engineering mismatch must be repaired"
      );

      CompoundTag unrelatedNbt = new CompoundTag();
      unrelatedNbt.putString("id", "minecraft:chest");
      StructureBlockInfo unrelatedEntity = new StructureBlockInfo(BlockPos.ZERO, lightEngineering.defaultBlockState(), unrelatedNbt);
      helper.assertTrue(
         ImmersiveEngineeringTemplateRepair.repairFluidPipeState(unrelatedEntity) == unrelatedEntity,
         "only the immersiveengineering:fluidpipe entity id must be repaired"
      );
      helper.succeed();
   }
}
