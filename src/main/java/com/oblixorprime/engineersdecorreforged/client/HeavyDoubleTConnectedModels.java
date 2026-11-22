package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import net.minecraft.core.registries.BuiltInRegistries;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTSupportBlock;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTTopology;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTTopology.Contact;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/** Hides the flush I-profile end faces between compatible Heavy supports. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class HeavyDoubleTConnectedModels {
   private static final ModelProperty<HeavyDoubleTTopology.Result> TOPOLOGY = new ModelProperty<>();
   private HeavyDoubleTConnectedModels() {}

   @SubscribeEvent public static void wrap(ModelEvent.ModifyBakingResult event) {
      Map<BakedModel, BakedModel> wrapped = new IdentityHashMap<>();
      for (var block : BuiltInRegistries.BLOCK) {
         if (!(block instanceof HeavyDoubleTSupportBlock)) continue;
         for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel original = event.getModels().get(key);
            if (original == null) throw new IllegalStateException("Missing heavy Double-T core model " + key);
            event.getModels().put(key, wrapped.computeIfAbsent(original, Model::new));
         }
      }
   }

   private static final class Model extends BakedModelWrapper<BakedModel> {
      private Model(BakedModel original) {
         super(original);
      }

      @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
         ModelData core = originalModel.getModelData(level, pos, state, data);
         return core.derive().with(TOPOLOGY, HeavyDoubleTTopology.resolve(level, pos, state)).build();
      }

      @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
            RandomSource random, ModelData data, @Nullable RenderType type) {
         HeavyDoubleTTopology.Result topology = data.get(TOPOLOGY);
         if (side != null && topology != null && topology.contact(side) == Contact.AXIAL) {
            return List.of();
         }
         return originalModel.getQuads(state, side, random, data, type);
      }
   }
}
