package com.oblixorprime.engineersdecorreforged.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class ImmersiveEngineeringTemplateRepair {
   public static final ResourceLocation FLUID_PIPE_BLOCK = ResourceLocation.fromNamespaceAndPath("immersiveengineering", "fluid_pipe");
   public static final ResourceLocation FLUID_PIPE_BLOCK_ENTITY = ResourceLocation.fromNamespaceAndPath("immersiveengineering", "fluidpipe");
   public static final ResourceLocation LIGHT_ENGINEERING_BLOCK = ResourceLocation.fromNamespaceAndPath("immersiveengineering", "light_engineering");

   private ImmersiveEngineeringTemplateRepair() {
   }

   public static StructureBlockInfo repairFluidPipeState(StructureBlockInfo info) {
      CompoundTag nbt = info.nbt();
      if (nbt == null || !FLUID_PIPE_BLOCK_ENTITY.toString().equals(nbt.getString("id"))) {
         return info;
      }

      Block fluidPipe = BuiltInRegistries.BLOCK.get(FLUID_PIPE_BLOCK);
      if (fluidPipe == Blocks.AIR) {
         return info;
      }

      BlockState state = info.state();
      Block lightEngineering = BuiltInRegistries.BLOCK.get(LIGHT_ENGINEERING_BLOCK);
      if (lightEngineering == Blocks.AIR || !state.is(lightEngineering)) {
         return info;
      }

      return new StructureBlockInfo(info.pos(), fluidPipe.defaultBlockState(), nbt);
   }
}
