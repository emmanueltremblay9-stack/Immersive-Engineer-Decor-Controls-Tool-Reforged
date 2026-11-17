package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTSupportBlock;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTTopology;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTTopology.Contact;
import java.util.ArrayList;
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

/** MK3 (connected geometry/material) + MK3.1 (face-contact plates) using NeoForge ModelData. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class HeavyDoubleTConnectedModels {
   private static final ModelProperty<HeavyDoubleTTopology.Result> TOPOLOGY = new ModelProperty<>();
   private HeavyDoubleTConnectedModels() {}

   private static String anchor(Direction face) { return "anchor_" + face.getName(); }
   private static String junction(Direction face) { return "junction_" + face.getName(); }
   private static String end(Direction axis, int roll, boolean positive) {
      return "end_" + axis.getName() + "_" + roll + (positive ? "_positive" : "_negative");
   }
   private static ModelResourceLocation standalone(String name) {
      return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
         EngineersDecorReforged.MOD_ID, "block/heavy_double_t/" + name));
   }

   @SubscribeEvent public static void register(ModelEvent.RegisterAdditional event) {
      for (Direction face : Direction.values()) {
         event.register(standalone(anchor(face)));
         event.register(standalone(junction(face)));
         for (int roll = 0; roll < 4; roll++) {
            event.register(standalone(end(face, roll, true)));
            event.register(standalone(end(face, roll, false)));
         }
      }
   }

   @SubscribeEvent public static void wrap(ModelEvent.ModifyBakingResult event) {
      Map<String, BakedModel> parts = new HashMap<>();
      for (Direction face : Direction.values()) {
         parts.put(anchor(face), require(event, anchor(face)));
         parts.put(junction(face), require(event, junction(face)));
         for (int roll = 0; roll < 4; roll++) {
            parts.put(end(face, roll, true), require(event, end(face, roll, true)));
            parts.put(end(face, roll, false), require(event, end(face, roll, false)));
         }
      }
      Map<BakedModel, BakedModel> wrapped = new IdentityHashMap<>();
      for (BlockState state : ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get().getStateDefinition().getPossibleStates()) {
         ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
         BakedModel original = event.getModels().get(key);
         if (original == null) throw new IllegalStateException("Missing heavy Double-T core model " + key);
         event.getModels().put(key, wrapped.computeIfAbsent(original, model -> new Model(model, parts)));
      }
   }

   private static BakedModel require(ModelEvent.ModifyBakingResult event, String part) {
      BakedModel model = event.getModels().get(standalone(part));
      if (model == null) throw new IllegalStateException("Missing heavy Double-T part model: " + part);
      return model;
   }

   private static final class Model extends BakedModelWrapper<BakedModel> {
      private final Map<String, BakedModel> parts;
      private Model(BakedModel original, Map<String, BakedModel> parts) {
         super(original);
         this.parts = Map.copyOf(parts);
      }

      @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
         ModelData core = originalModel.getModelData(level, pos, state, data);
         return core.derive().with(TOPOLOGY, HeavyDoubleTTopology.resolve(level, pos, state)).build();
      }

      private List<String> choose(BlockState state, ModelData data) {
         HeavyDoubleTTopology.Result topology = data.get(TOPOLOGY);
         if (topology == null) topology = new HeavyDoubleTTopology.Result(0, 0, 0);
         Direction axis = state.getValue(HeavyDoubleTSupportBlock.FACING);
         int roll = state.getValue(HeavyDoubleTSupportBlock.ROLL);
         List<String> selections = new ArrayList<>();
         for (Direction face : Direction.values()) {
            Contact contact = topology.contact(face);
            if (contact == Contact.ANCHOR) {
               selections.add(anchor(face));
            } else if (contact == Contact.JUNCTION) {
               selections.add(junction(face));
            } else if (contact == Contact.NONE && face.getAxis() == axis.getAxis()) {
               // Ends are absent for aligned neighbors, visible only at open beam ends.
               selections.add(end(axis, roll, face == axis));
            }
         }
         return selections;
      }

      @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
            RandomSource random, ModelData data, @Nullable RenderType type) {
         ArrayList<BakedQuad> quads = new ArrayList<>(originalModel.getQuads(state, side, random, data, type));
         if (state == null) return quads;
         for (String part : choose(state, data)) {
            BakedModel overlay = parts.get(part);
            if (overlay == null) throw new IllegalStateException("Unbaked heavy Double-T part " + part);
            if (type == null || overlay.getRenderTypes(state, random, ModelData.EMPTY).contains(type)) {
               quads.addAll(overlay.getQuads(state, side, random, ModelData.EMPTY, type));
            }
         }
         return quads;
      }

      @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
         ChunkRenderTypeSet types = originalModel.getRenderTypes(state, random, data);
         if (state == null) return types;
         for (String part : choose(state, data))
            types = ChunkRenderTypeSet.union(types, parts.get(part).getRenderTypes(state, random, ModelData.EMPTY));
         return types;
      }
   }
}
