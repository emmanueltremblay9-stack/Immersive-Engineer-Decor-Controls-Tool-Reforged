package com.oblixorprime.engineersdecorreforged.fluid;

import blusunrize.immersiveengineering.common.blocks.BlockIESlab;
import blusunrize.immersiveengineering.common.blocks.IEStairsBlock;
import blusunrize.immersiveengineering.common.blocks.IETrapDoorBlock;
import blusunrize.immersiveengineering.common.blocks.IEWallBlock;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.common.extensions.IBlockGetterExtension;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;

/** Qualified external containers. No extra blockstate properties and no subclass-wide opt-in. */
public final class ExternalFluidAdapter {
   private static final Set<Class<?>> VANILLA_CLASSES = Set.of(
      StairBlock.class, SlabBlock.class, FenceBlock.class, WallBlock.class,
      IronBarsBlock.class, StainedGlassPaneBlock.class, LadderBlock.class, TrapDoorBlock.class,
      WeatheringCopperStairBlock.class, WeatheringCopperSlabBlock.class, WeatheringCopperTrapDoorBlock.class);
   private static final Map<String, Class<?>> IE_BLOCKS = Map.ofEntries(
      Map.entry("stairs_treated_wood_horizontal", IEStairsBlock.class),
      Map.entry("stairs_treated_wood_vertical", IEStairsBlock.class),
      Map.entry("stairs_treated_wood_packaged", IEStairsBlock.class),
      Map.entry("slab_treated_wood_horizontal", BlockIESlab.class),
      Map.entry("slab_treated_wood_vertical", BlockIESlab.class),
      Map.entry("slab_treated_wood_packaged", BlockIESlab.class),
      Map.entry("wall_clinker_brick", IEWallBlock.class),
      Map.entry("treated_trapdoor", IETrapDoorBlock.class),
      Map.entry("treated_trapdoor_framed", IETrapDoorBlock.class),
      Map.entry("steel_trapdoor", IETrapDoorBlock.class));
   private static final Set<String> IE_FLUIDS = Set.of("creosote", "ethanol", "biodiesel", "plantoil");

   // Inspect only chunkless preview classes, once per class. Custom managers need no ChunkSource.
   private static final ClassValue<Boolean> DEFAULT_AUXILIARY_LOOKUP = new ClassValue<>() {
      @Override
      protected Boolean computeValue(Class<?> type) {
         try {
            return type.getMethod("getAuxLightManager", BlockPos.class).getDeclaringClass() == IBlockGetterExtension.class
               && type.getMethod("getAuxLightManager", ChunkPos.class).getDeclaringClass() == IBlockGetterExtension.class;
         } catch (NoSuchMethodException impossible) {
            throw new IllegalStateException("Missing NeoForge auxiliary-light API", impossible);
         }
      }
   };

   private ExternalFluidAdapter() {}

   @Nullable
   public static AuxiliaryLightManager auxiliaryLight(BlockGetter level, BlockPos pos) {
      // Only NeoForge's inherited implementation dereferences the absent ChunkSource.
      // Overrides remain authoritative, including their failures; do not catch render exceptions.
      if (level instanceof LevelAccessor accessor && accessor.getChunkSource() == null
            && DEFAULT_AUXILIARY_LOOKUP.get(level.getClass())) return null;
      return level.getAuxLightManager(pos);
   }

   public static boolean supported(BlockState state) {
      if (!state.hasProperty(BlockStateProperties.WATERLOGGED) || state.hasBlockEntity()) return false;
      if (state.hasProperty(BlockStateProperties.SLAB_TYPE)
            && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) return false;
      var block = state.getBlock();
      var id = BuiltInRegistries.BLOCK.getKey(block);
      if (id == null) return false;
      if (id.getNamespace().equals("minecraft")) return VANILLA_CLASSES.contains(block.getClass());
      return id.getNamespace().equals("immersiveengineering") && IE_BLOCKS.get(id.getPath()) == block.getClass();
   }

   public static boolean supportsFluid(Fluid fluid) {
      if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER || fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) return true;
      if (!(fluid instanceof FlowingFluid flowing) || !NativeFluidCells.standard(fluid)) return false;
      var id = BuiltInRegistries.FLUID.getKey(flowing.getSource());
      return id != null && id.getNamespace().equals("immersiveengineering") && IE_FLUIDS.contains(id.getPath());
   }

   public static boolean canPlace(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
      if (!(level instanceof Level) || !supported(state) || !supportsFluid(fluid)) return false;
      var current = level.getFluidState(pos);
      if (NativeFluidCells.reserved(level, pos) && current.isEmpty()) return false;
      return current.isEmpty() || (!current.isSource() && current.getType().isSame(fluid));
   }

   public static boolean place(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
      if (!canPlace(level, pos, state, fluid.getType())) return false;
      // Intentional canonical water sources keep the native save path. Currents never set WATERLOGGED.
      if (fluid.equals(Fluids.WATER.getSource(false))) {
         if (!level.isClientSide()) {
            if (!level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_ALL)) return false;
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
         }
         return true;
      }
      return NativeFluidCells.place(level, pos, state, fluid);
   }

   public static ItemStack pickup(LevelAccessor level, BlockPos pos, BlockState state) {
      var fluid = level.getFluidState(pos);
      if (!fluid.isSource() || !supportsFluid(fluid.getType()) || fluid.getType().getBucket() == Items.AIR) return ItemStack.EMPTY;
      if (!level.isClientSide()) {
         if (NativeFluidCells.active(level, pos)) NativeFluidCells.set(level, pos, Fluids.EMPTY.defaultFluidState());
         else if (!level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_ALL)) return ItemStack.EMPTY;
         if (!state.canSurvive(level, pos)) level.destroyBlock(pos, true);
      }
      return new ItemStack(fluid.getType().getBucket());
   }
}
