package com.oblixorprime.engineersdecorreforged.fluid;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.common.extensions.IBlockGetterExtension;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;

/** A fallback for NeoForge's inherited chunk lookup, without suppressing custom light managers. */
public final class PreviewLightAccess {
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

   private PreviewLightAccess() {}

   @Nullable
   public static AuxiliaryLightManager auxiliaryLight(BlockGetter level, BlockPos pos) {
      // Inspect chunkless classes once. Custom overrides and their failures remain authoritative.
      if (level instanceof LevelAccessor accessor && accessor.getChunkSource() == null
            && DEFAULT_AUXILIARY_LOOKUP.get(level.getClass())) return null;
      return level.getAuxLightManager(pos);
   }
}
