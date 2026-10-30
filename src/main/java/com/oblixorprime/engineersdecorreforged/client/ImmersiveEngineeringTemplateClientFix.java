package com.oblixorprime.engineersdecorreforged.client;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.mixin.accessors.PaletteAccess;
import blusunrize.immersiveengineering.mixin.accessors.TemplateAccess;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.compat.ImmersiveEngineeringTemplateRepair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class ImmersiveEngineeringTemplateClientFix {
   private static final int SCAN_INTERVAL_TICKS = 10;
   private static final int MAX_PENDING_TICKS = 1200;
   private static boolean pending;
   private static int pendingTicks;

   private ImmersiveEngineeringTemplateClientFix() {
   }

   @SubscribeEvent
   public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
      TemplateMultiblock.SYNCED_CLIENT_TEMPLATES.clear();
      pending = true;
      pendingTicks = 0;
   }

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      TemplateMultiblock.SYNCED_CLIENT_TEMPLATES.clear();
      pending = false;
      pendingTicks = 0;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      if (!pending || Minecraft.getInstance().level == null) {
         return;
      }

      pendingTicks++;
      if (pendingTicks % SCAN_INTERVAL_TICKS != 0 && pendingTicks < MAX_PENDING_TICKS) {
         return;
      }
      int repaired = repairSyncedTemplates();
      if (repaired > 0) {
         pending = false;
         EngineersDecorReforged.LOGGER.info(
            "Repaired {} malformed Immersive Engineering fluid pipe template entries for manual compatibility.",
            repaired
         );
         ImmersiveEngineering.proxy.resetManual();
      } else if (pendingTicks >= MAX_PENDING_TICKS) {
         pending = false;
      }
   }

   static int repairSyncedTemplates() {
      Map<ResourceLocation, StructureTemplate> syncedTemplates = TemplateMultiblock.SYNCED_CLIENT_TEMPLATES;
      if (syncedTemplates.isEmpty()) {
         return 0;
      }

      Map<ResourceLocation, StructureTemplate> replacements = new java.util.HashMap<>();
      int repaired = 0;
      for (Map.Entry<ResourceLocation, StructureTemplate> entry : syncedTemplates.entrySet()) {
         StructureTemplate template = entry.getValue();
         TemplateAccess templateAccess = (TemplateAccess)template;
         List<Palette> palettes = templateAccess.getPalettes();
         List<Palette> replacementPalettes = new ArrayList<>(palettes.size());
         boolean templateChanged = false;
         for (Palette palette : palettes) {
            List<StructureBlockInfo> replacementBlocks = new ArrayList<>(palette.blocks().size());
            boolean paletteChanged = false;
            for (StructureBlockInfo info : palette.blocks()) {
               StructureBlockInfo replacement = ImmersiveEngineeringTemplateRepair.repairFluidPipeState(info);
               replacementBlocks.add(replacement);
               if (replacement != info) {
                  paletteChanged = true;
                  repaired++;
               }
            }
            replacementPalettes.add(paletteChanged ? PaletteAccess.construct(replacementBlocks) : palette);
            templateChanged |= paletteChanged;
         }

         if (templateChanged) {
            StructureTemplate replacement = new StructureTemplate();
            TemplateAccess replacementAccess = (TemplateAccess)replacement;
            replacementAccess.setSize(template.getSize());
            replacementAccess.getPalettes().addAll(replacementPalettes);
            replacements.put(entry.getKey(), replacement);
         }
      }

      syncedTemplates.putAll(replacements);
      return repaired;
   }
}
