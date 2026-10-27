package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.DoubleTLightBlockEntity;
import com.oblixorprime.engineersdecorreforged.block.SupportLights;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** Original baked light quads, translated without rotations, UV edits or shape substitution. */
@EventBusSubscriber(modid=EngineersDecorReforged.MOD_ID,value=Dist.CLIENT)
public final class DoubleTLightModels {
   private static final ModelProperty<DoubleTLightBlockEntity.Attachment> LIGHT=DoubleTLightBlockEntity.MODEL_ATTACHMENT;
   private DoubleTLightModels(){}
   @SubscribeEvent public static void wrap(ModelEvent.ModifyBakingResult event){
      Map<BlockState,BakedModel> lamps=new IdentityHashMap<>();
      for(var block:java.util.List.of(ModBlocks.IRON_BULB_LIGHT.get(),ModBlocks.IRON_INSET_LIGHT.get(),ModBlocks.IRON_CEILING_EDGE_LIGHT.get(),ModBlocks.IRON_FLOOR_EDGE_LIGHT.get()))
         for(var state:block.getStateDefinition().getPossibleStates())lamps.put(state,event.getModels().get(BlockModelShaper.stateToModelLocation(state)));
      Map<BlockState,BakedModel> sharedLamps=Map.copyOf(lamps);
      Map<BakedModel,BakedModel> wrappers=new IdentityHashMap<>();
      for(var block:java.util.List.of(ModBlocks.STEEL_DOUBLE_T_SUPPORT.get(),ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get()))
         for(var state:block.getStateDefinition().getPossibleStates()){
            var key=BlockModelShaper.stateToModelLocation(state);var original=event.getModels().get(key);
            if(original==null)throw new IllegalStateException("Missing Double T model "+key);
            event.getModels().put(key,wrappers.computeIfAbsent(original,m->new Model(m,sharedLamps)));
         }
   }
   public static final class Model extends BakedModelWrapper<BakedModel> {
      private final Map<BlockState,BakedModel> lamps;
      Model(BakedModel original,Map<BlockState,BakedModel> lamps){super(original);this.lamps=lamps;}
      @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){
         return originalModel.getModelData(level,pos,state,data).derive().with(LIGHT,SupportLights.attached(state)?data.get(LIGHT):null).build();
      }
      @Override public java.util.List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random,ModelData data,@Nullable RenderType type){
         var result=new ArrayList<BakedQuad>();
         if(type==null||originalModel.getRenderTypes(state,random,data).contains(type))result.addAll(originalModel.getQuads(state,side,random,data,type));
         var light=data.get(LIGHT);
         if(light==null)return result;
         var model=lamps.get(light.lamp);if(model==null)throw new IllegalStateException("Unbaked support lamp "+light.lamp);
         if(type!=null&&!model.getRenderTypes(light.lamp,random,ModelData.EMPTY).contains(type))return result;
         for(var quad:model.getQuads(light.lamp,side,random,ModelData.EMPTY,type)){
            int[] vertices=quad.getVertices().clone();int stride=vertices.length/4;
            for(int i=0;i<4;i++)for(int axis=0;axis<3;axis++){
               double delta=axis==0?light.offset.x:axis==1?light.offset.y:light.offset.z;
               vertices[i*stride+axis]=Float.floatToRawIntBits((float)(Float.intBitsToFloat(vertices[i*stride+axis])+delta));
            }
            result.add(new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion()));
         }
         return result;
      }
      @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data){
         var light=data.get(LIGHT);return light==null?originalModel.getRenderTypes(state,random,data):ChunkRenderTypeSet.union(originalModel.getRenderTypes(state,random,data),lamps.get(light.lamp).getRenderTypes(light.lamp,random,ModelData.EMPTY));
      }
   }
}
