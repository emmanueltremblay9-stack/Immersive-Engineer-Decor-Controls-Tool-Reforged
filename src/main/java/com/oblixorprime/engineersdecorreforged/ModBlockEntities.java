package com.oblixorprime.engineersdecorreforged;

import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
      Registries.BLOCK_ENTITY_TYPE, "immersive_engineer_decor_controls_tool_reforged"
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> MACHINE = BLOCK_ENTITY_TYPES.register(
      "machine",
      () -> Builder.of(MachineBlockEntity::new, machineBlocks()).build(null)
   );

   private static Block[] machineBlocks() {
      return new Block[] {
         ModBlocks.METAL_CRAFTING_TABLE.get(),
         ModBlocks.LABELED_CRATE.get(),
         ModBlocks.FACTORY_HOPPER.get(),
         ModBlocks.FACTORY_DROPPER.get(),
         ModBlocks.FACTORY_PLACER.get(),
         ModBlocks.SMALL_BLOCK_BREAKER.get(),
         ModBlocks.SMALL_WASTE_INCINERATOR.get(),
         ModBlocks.SMALL_LAB_FURNACE.get(),
         ModBlocks.SMALL_ELECTRICAL_FURNACE.get(),
         ModBlocks.SMALL_MINERAL_SMELTER.get(),
         ModBlocks.SMALL_FREEZER.get(),
         ModBlocks.FLUID_BARREL.get(),
         ModBlocks.SMALL_FLUID_FUNNEL.get(),
         ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get(),
         ModBlocks.SMALL_SOLAR_PANEL.get(),
         ModBlocks.SMALL_MILKING_MACHINE.get(),
         ModBlocks.SMALL_TREE_CUTTER.get()
      };
   }

   private ModBlockEntities() {
   }
}
