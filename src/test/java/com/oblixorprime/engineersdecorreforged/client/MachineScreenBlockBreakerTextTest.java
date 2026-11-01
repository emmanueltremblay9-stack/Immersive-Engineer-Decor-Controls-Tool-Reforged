package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.Test;

public class MachineScreenBlockBreakerTextTest {
   @Test
   public void targetPresenceUsesTheBlockNameInsteadOfTheOptionalItemPreview() {
      assertFalse(MachineScreen.blockBreakerHasTarget("No target"));
      assertTrue(MachineScreen.blockBreakerHasTarget("Stone"));
      assertTrue(MachineScreen.blockBreakerHasTarget("Block without an item form"));
   }

   @Test
   public void hardnessUsesMinecraftSupportedStringArgumentsAndLanguageDecimalSeparators() {
      assertEquals("0.50", MachineScreen.formatBlockBreakerHardness(0.5F, Locale.ROOT));
      assertEquals("0,50", MachineScreen.formatBlockBreakerHardness(0.5F, Locale.FRANCE));
   }

   @Test
   public void minecraftResolvesTheEnglishAndFrenchHardnessTranslations() throws IOException {
      Language originalLanguage = Language.getInstance();
      String key = "gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.hardness.value";

      try {
         Language.inject(testLanguage(loadTranslations("en_us")));
         assertEquals("H: 0.50", Component.translatable(key,
            MachineScreen.formatBlockBreakerHardness(0.5F, Locale.ROOT)).getString());

         Language.inject(testLanguage(loadTranslations("fr_fr")));
         assertEquals("D : 0,50", Component.translatable(key,
            MachineScreen.formatBlockBreakerHardness(0.5F, Locale.FRANCE)).getString());
      } finally {
         Language.inject(originalLanguage);
      }
   }

   private static Map<String, String> loadTranslations(String locale) throws IOException {
      String resource = "assets/immersive_engineer_decor_controls_tool_reforged/lang/" + locale + ".json";
      InputStream stream = MachineScreenBlockBreakerTextTest.class.getClassLoader().getResourceAsStream(resource);
      assertNotNull("Missing translation resource " + resource, stream);
      try (stream) {
         Map<String, String> translations = new HashMap<>();
         Language.loadFromJson(stream, translations::put);
         return translations;
      }
   }

   private static Language testLanguage(Map<String, String> translations) {
      return new Language() {
         @Override
         public String getOrDefault(String key, String defaultValue) {
            return translations.getOrDefault(key, defaultValue);
         }

         @Override
         public boolean has(String id) {
            return translations.containsKey(id);
         }

         @Override
         public boolean isDefaultRightToLeft() {
            return false;
         }

         @Override
         public FormattedCharSequence getVisualOrder(FormattedText text) {
            return FormattedCharSequence.forward(text.getString(), Style.EMPTY);
         }
      };
   }

   @Test
   public void costEstimateUsesTheMenuValueOrFallsBackToTheExistingHardnessFormula() {
      assertEquals(240, MachineScreen.blockBreakerEstimatedCost(0.5F, true, true, 240));
      assertEquals(240, MachineScreen.blockBreakerEstimatedCost(0.5F, true, false, 0));
      assertEquals(160, MachineScreen.blockBreakerEstimatedCost(0.0F, true, false, 0));
      assertEquals(-1, MachineScreen.blockBreakerEstimatedCost(0.5F, false, false, 0));
      assertEquals(-1, MachineScreen.blockBreakerEstimatedCost(-1.0F, true, false, 0));
      assertEquals(-1, MachineScreen.blockBreakerEstimatedCost(Float.NaN, true, false, 0));
   }

   @Test
   public void synchronizedHardnessRoundingCanEstimateAboveOrBelowServerBilling() {
      float lowerActualHardness = 0.504F;
      int lowerSerializedHundredths = Math.round(lowerActualHardness * 100.0F);
      float lowerSynchronizedHardness = Math.round(lowerActualHardness * 100.0F) / 100.0F;
      int lowerDisplayedCost = MachineScreen.blockBreakerEstimatedCost(
         lowerSynchronizedHardness,
         true,
         true,
         MachineBlockEntity.blockBreakerEnergyCostPerBlock(lowerSynchronizedHardness)
      );

      assertEquals(50, lowerSerializedHundredths);
      assertEquals(0.50F, lowerSynchronizedHardness, 0.0F);
      assertEquals(240, lowerDisplayedCost);
      assertEquals(241, MachineBlockEntity.blockBreakerEnergyCostPerBlock(lowerActualHardness));

      float higherActualHardness = 0.506F;
      int higherSerializedHundredths = Math.round(higherActualHardness * 100.0F);
      float higherSynchronizedHardness = Math.round(higherActualHardness * 100.0F) / 100.0F;
      int higherDisplayedCost = MachineScreen.blockBreakerEstimatedCost(
         higherSynchronizedHardness,
         true,
         true,
         MachineBlockEntity.blockBreakerEnergyCostPerBlock(higherSynchronizedHardness)
      );

      assertEquals(51, higherSerializedHundredths);
      assertEquals(0.51F, higherSynchronizedHardness, 0.0F);
      assertEquals(242, higherDisplayedCost);
      assertEquals(241, MachineBlockEntity.blockBreakerEnergyCostPerBlock(higherActualHardness));
   }

   @Test
   public void breakerStatesUseDistinctLocalizedLabelsAndDetails() {
      String[] labels = {
         "target.none",
         "state.redstone_stop",
         "state.waiting_for_fe",
         "state.output_blocked",
         "state.working",
         "state.cooldown",
         "state.unbreakable"
      };
      String[] details = {
         "detail.no_target",
         "detail.redstone_stop",
         "detail.waiting_for_fe",
         "detail.output_blocked",
         "detail.working",
         "detail.cooldown",
         "detail.unbreakable"
      };

      for (int state = 0; state < labels.length; state++) {
         assertEquals(labels[state], MachineScreen.blockBreakerStatusLabelKey(state));
         assertEquals(details[state], MachineScreen.blockBreakerStatusDetailKey(state));
      }

      assertEquals("state.unknown", MachineScreen.blockBreakerStatusLabelKey(-1));
      assertEquals("state.unknown", MachineScreen.blockBreakerStatusLabelKey(7));
      assertEquals("detail.unknown", MachineScreen.blockBreakerStatusDetailKey(-1));
      assertEquals("detail.unknown", MachineScreen.blockBreakerStatusDetailKey(7));
   }

   @Test
   public void hardnessDisplayHandlesMissingFiniteUnbreakableAndInvalidValues() {
      assertEquals("hardness.unavailable", MachineScreen.blockBreakerHardnessKey(0.0F, false));
      assertEquals("hardness.value", MachineScreen.blockBreakerHardnessKey(0.0F, true));
      assertEquals("hardness.value", MachineScreen.blockBreakerHardnessKey(0.5F, true));
      assertEquals("hardness.unbreakable", MachineScreen.blockBreakerHardnessKey(-1.0F, true));
      assertEquals("hardness.unknown", MachineScreen.blockBreakerHardnessKey(Float.NaN, true));
      assertEquals("hardness.unknown", MachineScreen.blockBreakerHardnessKey(Float.POSITIVE_INFINITY, true));
   }
}
