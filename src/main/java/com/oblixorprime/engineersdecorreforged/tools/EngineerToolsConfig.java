package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class EngineerToolsConfig {
   public static final int DEFAULT_REDIA_DURABILITY = 3000;
   public static final int DEFAULT_REDIA_INITIAL_DURABILITY_PERCENT = 100;
   public static final String DEFAULT_REDIA_EFFICIENCY_CURVE = "0,1,1,2,2,3,3,3,3,4";
   public static final String DEFAULT_REDIA_FORTUNE_CURVE = "0,0,0,0,1,1,1,1,2,3";
   public static final ModConfigSpec COMMON_SPEC;
   public static final ModConfigSpec SERVER_SPEC;
   public static final ConfigValue<String> PATTERN_EXCLUDES;
   public static final ConfigValue<String> PATTERN_INCLUDES;
   public static final BooleanValue WITH_EXPERIMENTAL;
   public static final BooleanValue WITH_CONFIG_LOGGING;
   public static final BooleanValue WITH_CHEST_LOOT;
   public static final IntValue REDIA_TOOL_DURABILITY;
   public static final IntValue REDIA_TOOL_INITIAL_DURABILITY_PERCENT;
   public static final ConfigValue<String> REDIA_TOOL_EFFICIENCY_CURVE;
   public static final ConfigValue<String> REDIA_TOOL_FURTUNE_CURVE;
   public static final BooleanValue WITHOUT_SAFE_ATTACKING;

   private EngineerToolsConfig() {
   }

   public static boolean withChestLoot() {
      return !COMMON_SPEC.isLoaded() ? false : WITH_CHEST_LOOT.get();
   }

   public static int rediaToolDurability() {
      return !SERVER_SPEC.isLoaded() ? DEFAULT_REDIA_DURABILITY : REDIA_TOOL_DURABILITY.get();
   }

   public static int rediaInitialDurabilityPercent() {
      return !SERVER_SPEC.isLoaded() ? DEFAULT_REDIA_INITIAL_DURABILITY_PERCENT : REDIA_TOOL_INITIAL_DURABILITY_PERCENT.get();
   }

   public static boolean withSafeAttacking() {
      return !SERVER_SPEC.isLoaded() || !WITHOUT_SAFE_ATTACKING.get();
   }

   public static int[] rediaEfficiencyCurve() {
      String value = !SERVER_SPEC.isLoaded() ? DEFAULT_REDIA_EFFICIENCY_CURVE : REDIA_TOOL_EFFICIENCY_CURVE.get();
      return parseCurve(value, DEFAULT_REDIA_EFFICIENCY_CURVE);
   }

   public static int[] rediaFortuneCurve() {
      String value = !SERVER_SPEC.isLoaded() ? DEFAULT_REDIA_FORTUNE_CURVE : REDIA_TOOL_FURTUNE_CURVE.get();
      return parseCurve(value, DEFAULT_REDIA_FORTUNE_CURVE);
   }

   public static boolean isItemEnabled(String registryPath) {
      String path = registryPath.toLowerCase(Locale.ROOT);
      List<String> includes = patterns(!COMMON_SPEC.isLoaded() ? "" : PATTERN_INCLUDES.get());
      for (String pattern : includes) {
         if (path.matches(pattern)) {
            return true;
         }
      }
      List<String> excludes = patterns(!COMMON_SPEC.isLoaded() ? "" : PATTERN_EXCLUDES.get());
      for (String pattern : excludes) {
         if (path.matches(pattern)) {
            return false;
         }
      }
      return true;
   }

   public static void logAppliedValues() {
      if (COMMON_SPEC.isLoaded() && WITH_CONFIG_LOGGING.get()) {
         EngineersDecorReforged.LOGGER.info(
            "Engineer's Tools config: chest loot={}, experimental={}, includes='{}', excludes='{}', REDIA durability={}, initial durability={}%, safe attacks={}, efficiency={}, fortune={}",
            WITH_CHEST_LOOT.get(),
            WITH_EXPERIMENTAL.get(),
            PATTERN_INCLUDES.get(),
            PATTERN_EXCLUDES.get(),
            rediaToolDurability(),
            rediaInitialDurabilityPercent(),
            withSafeAttacking(),
            SERVER_SPEC.isLoaded() ? REDIA_TOOL_EFFICIENCY_CURVE.get() : DEFAULT_REDIA_EFFICIENCY_CURVE,
            SERVER_SPEC.isLoaded() ? REDIA_TOOL_FURTUNE_CURVE.get() : DEFAULT_REDIA_FORTUNE_CURVE
         );
      }
   }

   private static List<String> patterns(String configured) {
      String sanitized = configured.toLowerCase(Locale.ROOT)
         .replace(EngineersDecorReforged.MOD_ID + ":", "")
         .replace("engineerstools:", "")
         .replaceAll("[^*_,a-z0-9]", "");
      List<String> patterns = new ArrayList<>();
      for (String candidate : sanitized.split(",")) {
         if (!candidate.isEmpty()) {
            patterns.add(candidate.replace("*", ".*?"));
         }
      }
      return patterns;
   }

   private static int[] parseCurve(String configured, String fallback) {
      int[] defaults = parseDefaultCurve(fallback);
      String[] values = configured.trim().replaceAll("[^,0-9]", "").split(",");
      List<Integer> parsed = new ArrayList<>();
      try {
         for (int index = 0; index < values.length && index < defaults.length; index++) {
            parsed.add(Math.max(0, Math.min(4, Integer.parseInt(values[index]))));
         }
      } catch (RuntimeException exception) {
         return defaults;
      }
      if (parsed.isEmpty()) {
         return defaults;
      }
      for (int index = 1; index < parsed.size(); index++) {
         if (parsed.get(index) < parsed.get(index - 1)) {
            parsed.set(index, parsed.get(index - 1));
         }
      }
      while (parsed.size() < defaults.length) {
         parsed.add(parsed.getLast());
      }
      int[] result = new int[defaults.length];
      for (int index = 0; index < result.length; index++) {
         result[index] = parsed.get(index);
      }
      return result;
   }

   private static int[] parseDefaultCurve(String configured) {
      String[] values = configured.split(",");
      int[] result = new int[values.length];
      for (int index = 0; index < values.length; index++) {
         result[index] = Integer.parseInt(values[index]);
      }
      return result;
   }

   static {
      ModConfigSpec.Builder common = new ModConfigSpec.Builder();
      common.comment("Settings affecting the logical server side.").push("server");
      common.comment("Opt-out settings").push("optout");
      PATTERN_EXCLUDES = common.comment("Opt out Engineer's Tools items by registry path. '*' is a wildcard and entries are comma-separated.")
         .define("pattern_excludes", "");
      PATTERN_INCLUDES = common.comment("Keep matching Engineer's Tools items enabled before exclusions are evaluated.")
         .define("pattern_includes", "");
      common.pop();
      common.comment("Miscellaneous settings").push("miscellaneous");
      WITH_EXPERIMENTAL = common.comment("Enables experimental features. The published 1.1.7 artifact contains no experimental recipes.")
         .define("with_experimental", false);
      WITH_CONFIG_LOGGING = common.comment("Logs applied Engineer's Tools configuration values.").define("with_config_logging", false);
      WITH_CHEST_LOOT = common.comment("Enables the original optional village, dungeon, treasure, outpost, and mineshaft chest loot.")
         .define("with_chest_loot", false);
      common.pop(2);
      COMMON_SPEC = common.build();

      ModConfigSpec.Builder server = new ModConfigSpec.Builder();
      server.comment("Settings affecting the logical server side, including single-player worlds.").push("server");
      server.comment("Engineer Tool tuning").push("tweaks");
      REDIA_TOOL_DURABILITY = server.comment("Maximum durability of the REDIA tool.")
         .defineInRange("redia_tool_durability", DEFAULT_REDIA_DURABILITY, 750, 4000);
      REDIA_TOOL_INITIAL_DURABILITY_PERCENT = server.comment("Durability percentage of a newly crafted REDIA tool.")
         .defineInRange("redia_tool_initial_durability_percent", DEFAULT_REDIA_INITIAL_DURABILITY_PERCENT, 50, 100);
      REDIA_TOOL_EFFICIENCY_CURVE = server.comment("Ten nondecreasing efficiency levels for successive ten-percent durability bands.")
         .define("redia_tool_efficiency_curve", DEFAULT_REDIA_EFFICIENCY_CURVE);
      REDIA_TOOL_FURTUNE_CURVE = server.comment("Ten nondecreasing fortune levels. The key keeps the original 'furtune' spelling for compatibility.")
         .define("redia_tool_furtune_curve", DEFAULT_REDIA_FORTUNE_CURVE);
      WITHOUT_SAFE_ATTACKING = server.comment("Disables protection against accidentally attacking villagers, owned pets, and neutral zombified piglins.")
         .define("without_safe_attacking", false);
      server.pop(2);
      SERVER_SPEC = server.build();
   }
}
