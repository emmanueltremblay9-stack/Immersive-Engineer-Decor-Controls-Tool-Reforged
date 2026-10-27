package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock.Section;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/** Contextual models only. Never changes a pole's state, shape, drops or ownership. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class RaisedStairPoleModels {
   private static final ModelProperty<BakedModel> CONNECTION = new ModelProperty<>();
   private RaisedStairPoleModels() {}

   public static ModelResourceLocation partModel(String part, Section material, Direction facing) {
      return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,
         "block/raised_stair_v/" + part + "_" + material.getSerializedName() + "_" + facing.getName()));
   }

   @SubscribeEvent public static void register(ModelEvent.RegisterAdditional event) {
      for (Section material : List.of(Section.WOOD, Section.THIN_STEEL, Section.THICK_STEEL))
         for (Direction facing : Direction.Plane.HORIZONTAL)
            for (String part : List.of("lower", "lower_open", "upper")) event.register(partModel(part, material, facing));
   }

   @SubscribeEvent public static void wrap(ModelEvent.ModifyBakingResult event) {
      Map<String, Map<Section, Map<Direction, BakedModel>>> parts = new HashMap<>();
      for (String part : List.of("lower", "lower_open", "upper")) {
      Map<Section, Map<Direction, BakedModel>> replacements = new HashMap<>();
      for (Section material : List.of(Section.WOOD, Section.THIN_STEEL, Section.THICK_STEEL)) {
         Map<Direction, BakedModel> rotations = new HashMap<>();
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            BakedModel model = event.getModels().get(partModel(part, material, facing));
            if (model == null) throw new IllegalStateException("Missing raised stair pole model: " + partModel(part, material, facing));
            rotations.put(facing, model);
         }
         replacements.put(material, Map.copyOf(rotations));
      }
      parts.put(part, Map.copyOf(replacements));
      }
      Map<BakedModel, BakedModel> wrappers = new IdentityHashMap<>();
      for (var pole : List.of(ModBlocks.TREATED_WOOD_POLE.get(), ModBlocks.TREATED_WOOD_POLE_HEAD.get(),
            ModBlocks.TREATED_WOOD_POLE_SUPPORT.get(), ModBlocks.THICK_STEEL_POLE.get(),
            ModBlocks.THICK_STEEL_POLE_HEAD.get(), ModBlocks.THIN_STEEL_POLE.get(), ModBlocks.THIN_STEEL_POLE_HEAD.get())) {
         for (BlockState state : pole.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel original = event.getModels().get(key);
            if (original == null) throw new IllegalStateException("Missing existing pole model: " + key);
            event.getModels().put(key, wrappers.computeIfAbsent(original, model -> new PoleModel(model, parts, false)));
         }
      }
      Map<BakedModel, BakedModel> stairWrappers = new IdentityHashMap<>();
      for (BlockState state : ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get().getStateDefinition().getPossibleStates()) {
         ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
         BakedModel original = event.getModels().get(key);
         if (original == null) throw new IllegalStateException("Missing raised stair model: " + key);
         event.getModels().put(key, stairWrappers.computeIfAbsent(original, model -> new PoleModel(model, parts, true)));
      }
   }

   public static final class PoleModel extends BakedModelWrapper<BakedModel> {
      private final Map<String, Map<Section, Map<Direction, BakedModel>>> replacements;
      private final boolean stair;
      private PoleModel(BakedModel original, Map<String, Map<Section, Map<Direction, BakedModel>>> replacements, boolean stair) {
         super(original);
         this.replacements = Map.copyOf(replacements);
         this.stair = stair;
      }

      @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
         ModelData original = originalModel.getModelData(level, pos, state, data);
         BlockState above = stair ? state : level.getBlockState(pos.above());
         Section material = RaisedCatwalkStairsBlock.section(stair ? level.getBlockState(pos.below()) : state);
         BakedModel replacement = null;
         if (above.is(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get()) && material != Section.NONE) {
            String part = stair ? "upper" : RaisedCatwalkStairsBlock.matchesStraightSection(level.getBlockState(pos.below()), material) ? "lower_open" : "lower";
            replacement = replacements.get(part).get(material).get(above.getValue(PortedBlocks.HORIZONTAL_FACING));
         }
         // Explicitly clear stale contextual data when the stair/support changes.
         return original.derive().with(CONNECTION, replacement).build();
      }

      private BakedModel selected(ModelData data) {
         BakedModel model = data.get(CONNECTION);
         return model == null ? originalModel : model;
      }

      @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
            RandomSource random, ModelData data, @Nullable RenderType type) {
         BakedModel model = selected(data);
         if (!stair || model == originalModel) return model.getQuads(state, side, random, data, type);
         var result = new java.util.ArrayList<>(originalModel.getQuads(state, side, random, data, type));
         result.addAll(model.getQuads(state, side, random, data, type));
         return result;
      }

      @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
         BakedModel model = selected(data);
         return !stair || model == originalModel ? model.getRenderTypes(state, random, data)
            : ChunkRenderTypeSet.union(originalModel.getRenderTypes(state, random, data), model.getRenderTypes(state, random, data));
      }
   }
}
