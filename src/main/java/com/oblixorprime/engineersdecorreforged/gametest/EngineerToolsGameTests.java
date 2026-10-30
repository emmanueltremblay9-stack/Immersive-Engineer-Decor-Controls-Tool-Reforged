package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.menu.MaterialBoxMenu;
import com.oblixorprime.engineersdecorreforged.menu.MusliBarPressMenu;
import com.oblixorprime.engineersdecorreforged.recipe.CrushingRecipe;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsConfig;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsLoot;
import com.oblixorprime.engineersdecorreforged.tools.MaterialBoxItem;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressItem;
import com.oblixorprime.engineersdecorreforged.tools.RediaToolItem;
import com.oblixorprime.engineersdecorreforged.tools.RediaToolRepairRecipe;
import com.oblixorprime.engineersdecorreforged.tools.SleepingBagItem;
import com.oblixorprime.engineersdecorreforged.tools.TooltipItem;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class EngineerToolsGameTests {
   private static final String TEMPLATE = "empty";
   private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

   private EngineerToolsGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void charged_lapis_squeezer_uses_original_costs_without_wearing_tool(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack squeezer = new ItemStack((ItemLike)EngineerToolsModule.CHARGED_LAPIS_SQUEEZER.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, squeezer);
      player.experienceLevel = 1;
      player.getInventory().add(new ItemStack(Items.LAPIS_LAZULI));
      player.setHealth(3.0F);
      float healthBefore = player.getHealth();
      float exhaustionBefore = player.getFoodData().getExhaustionLevel();
      squeezer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      float expectedHealth = healthBefore - player.getMaxHealth() / 10.0F;
      helper.assertValueEqual(0, countItem(player.getInventory(), Items.LAPIS_LAZULI), "squeezer should consume one lapis");
      helper.assertValueEqual(0, player.experienceLevel, "squeezer should consume one XP level");
      helper.assertValueEqual(1, countItem(player.getInventory(), (Item)EngineerToolsModule.CHARGED_LAPIS.get()), "squeezer should create one charged lapis");
      helper.assertValueEqual(0, squeezer.getDamageValue(), "squeezer should not lose durability on success");
      helper.assertTrue(Math.abs(player.getHealth() - expectedHealth) < 0.001F, "squeezer should apply the original max-health/10 health cost");
      helper.assertTrue(
         player.getFoodData().getExhaustionLevel() > exhaustionBefore, "squeezer should apply the original hunger exhaustion cost"
      );
      helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "squeezer should briefly blind the user on success");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void charged_lapis_squeezer_without_lapis_keeps_xp_and_item(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack squeezer = new ItemStack((ItemLike)EngineerToolsModule.CHARGED_LAPIS_SQUEEZER.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, squeezer);
      player.experienceLevel = 1;
      squeezer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(1, player.experienceLevel, "missing lapis should not consume XP");
      helper.assertValueEqual(0, squeezer.getDamageValue(), "missing lapis should not wear the squeezer");
      helper.assertValueEqual(
         0, countItem(player.getInventory(), (Item)EngineerToolsModule.CHARGED_LAPIS.get()), "missing lapis should not create charged lapis"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void charged_lapis_squeezer_without_xp_keeps_lapis_and_item(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack squeezer = new ItemStack((ItemLike)EngineerToolsModule.CHARGED_LAPIS_SQUEEZER.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, squeezer);
      player.getInventory().add(new ItemStack(Items.LAPIS_LAZULI));
      squeezer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(1, countItem(player.getInventory(), Items.LAPIS_LAZULI), "missing XP should not consume lapis");
      helper.assertValueEqual(0, squeezer.getDamageValue(), "missing XP should not wear the squeezer");
      helper.assertValueEqual(0, countItem(player.getInventory(), (Item)EngineerToolsModule.CHARGED_LAPIS.get()), "missing XP should not create charged lapis");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void charged_lapis_squeezer_is_original_stackable_reusable_item(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack squeezer = new ItemStack((ItemLike)EngineerToolsModule.CHARGED_LAPIS_SQUEEZER.get(), 2);
      player.setItemInHand(InteractionHand.MAIN_HAND, squeezer);
      player.experienceLevel = 1;
      player.getInventory().add(new ItemStack(Items.LAPIS_LAZULI));
      helper.assertValueEqual(64, squeezer.getMaxStackSize(), "squeezer should use the original stack size");
      helper.assertFalse(squeezer.isDamageableItem(), "squeezer should not be a durability item");
      squeezer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(2, player.getMainHandItem().getCount(), "successful conversion should not consume or break the squeezer");
      helper.assertValueEqual(
         1, countItem(player.getInventory(), (Item)EngineerToolsModule.CHARGED_LAPIS.get()), "successful use should create exactly one charged lapis"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void charged_lapis_squeezer_creative_does_not_bypass_original_costs(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.getAbilities().instabuild = true;
      ItemStack squeezer = new ItemStack((ItemLike)EngineerToolsModule.CHARGED_LAPIS_SQUEEZER.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, squeezer);
      squeezer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(
         0, countItem(player.getInventory(), (Item)EngineerToolsModule.CHARGED_LAPIS.get()), "creative squeezer use should still require the original inputs"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void charged_lapis_restores_original_healing_fire_and_glint_behavior(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack chargedLapis = new ItemStack((ItemLike)EngineerToolsModule.CHARGED_LAPIS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, chargedLapis);
      player.setHealth(10.0F);
      player.setRemainingFireTicks(100);
      player.experienceLevel = 0;
      chargedLapis.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(Math.abs(player.getHealth() - 11.0F) < 0.001F, "charged lapis should heal the original max-health/20 amount");
      helper.assertValueEqual(0, player.getRemainingFireTicks(), "charged lapis should clear fire like the original item");
      helper.assertValueEqual(1, player.experienceLevel, "charged lapis should grant one XP level");
      helper.assertTrue(chargedLapis.isEmpty(), "charged lapis should consume one item on use");
      helper.assertTrue(
         EngineerToolsModule.CHARGED_LAPIS.get().isFoil(new ItemStack((ItemLike)EngineerToolsModule.CHARGED_LAPIS.get())),
         "charged lapis should keep the original foil glint"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_tills_workable_soil(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.setBlock(TEST_POS, Blocks.DIRT);
      helper.setBlock(TEST_POS.above(), Blocks.AIR);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.FARMLAND), "sneak-use REDIA Tool should till workable soil");
      helper.assertValueEqual(1, redia.getDamageValue(), "REDIA Tool should wear on successful tilling");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_creative_tilling_does_not_damage_tool(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.getAbilities().instabuild = true;
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.setBlock(TEST_POS, Blocks.DIRT);
      helper.setBlock(TEST_POS.above(), Blocks.AIR);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.FARMLAND), "creative sneak-use REDIA Tool should still till workable soil");
      helper.assertValueEqual(0, redia.getDamageValue(), "creative REDIA Tool use should not damage the tool");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_rejects_unworkable_target_without_damage(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.setBlock(TEST_POS, Blocks.STONE);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.STONE), "REDIA Tool should leave unworkable blocks unchanged");
      helper.assertValueEqual(0, redia.getDamageValue(), "failed REDIA use should not damage the tool");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_preserves_original_ground_cycle_under_occupied_blocks(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.setBlock(TEST_POS, Blocks.DIRT);
      helper.setBlock(TEST_POS.above(), Blocks.STONE);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.FARMLAND), "original REDIA ground cycling should not require air above dirt");
      helper.assertValueEqual(1, redia.getDamageValue(), "occupied-block REDIA tilling should damage the tool once");

      helper.setBlock(TEST_POS, Blocks.COARSE_DIRT);
      helper.setBlock(TEST_POS.above(), Blocks.STONE);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.DIRT_PATH), "original REDIA ground cycling should not require air above coarse dirt");
      helper.assertValueEqual(2, redia.getDamageValue(), "each occupied-block ground conversion should damage the tool once");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_places_torch_into_replaceable_blocks(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      player.getInventory().items.set(1, new ItemStack(Items.TORCH));
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.north(), Blocks.SHORT_GRASS);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.NORTH));
      helper.assertTrue(helper.getBlockState(TEST_POS.north()).is(Blocks.WALL_TORCH), "REDIA Tool should replace short grass with a wall torch");
      helper.assertValueEqual(0, countItem(player.getInventory(), Items.TORCH), "REDIA Tool should consume one carried torch");
      helper.assertValueEqual(0, redia.getDamageValue(), "original REDIA torch placement should not damage the tool");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_delegates_replaceable_target_placement_to_torch_item(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, redia);
      player.getInventory().items.set(1, new ItemStack(Items.TORCH));
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.above(), Blocks.SHORT_GRASS);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS.above(), Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS.above()).is(Blocks.TORCH), "REDIA should preserve vanilla torch placement into the clicked replaceable block");
      helper.assertValueEqual(0, countItem(player.getInventory(), Items.TORCH), "delegated REDIA torch placement should consume one torch");
      helper.assertValueEqual(0, redia.getDamageValue(), "torch placement should not wear REDIA");
      helper.assertTrue(player.getMainHandItem() == redia, "successful delegated torch placement must restore the same REDIA stack to hand");

      player.getInventory().items.set(1, new ItemStack(Items.TORCH));
      BlockPos unsupported = TEST_POS.above(4);
      helper.setBlock(unsupported, Blocks.AIR);
      helper.setBlock(unsupported.below(), Blocks.AIR);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, unsupported, Direction.UP));
      helper.assertTrue(helper.getBlockState(unsupported).isAir(), "delegated torch placement should retain vanilla support checks");
      helper.assertValueEqual(1, countItem(player.getInventory(), Items.TORCH), "failed delegated placement should not consume a torch");
      helper.assertTrue(player.getMainHandItem() == redia, "failed delegated torch placement must restore the same REDIA stack to hand");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_preserves_original_sneak_use_face_routing(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.setBlock(TEST_POS, Blocks.OAK_LOG);

      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.OAK_LOG), "top-face REDIA use should not fall through to axe stripping");
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.NORTH));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.OAK_LOG), "horizontal REDIA use without a torch should not fall through to axe stripping");
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.DOWN));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.STRIPPED_OAK_LOG), "bottom-face REDIA use should retain the original axe fallback");
      helper.assertValueEqual(1, redia.getDamageValue(), "only the successful bottom-face axe action should wear REDIA");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_exposes_original_multitool_actions(GameTestHelper helper) {
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.assertValueEqual(3000, redia.getMaxDamage(), "REDIA Tool should use the original 3000 durability");
      helper.assertTrue(redia.canPerformAction(ItemAbilities.AXE_STRIP), "REDIA Tool should expose axe actions");
      helper.assertTrue(redia.canPerformAction(ItemAbilities.PICKAXE_DIG), "REDIA Tool should expose pickaxe actions");
      helper.assertTrue(redia.canPerformAction(ItemAbilities.SHOVEL_FLATTEN), "REDIA Tool should expose shovel actions");
      helper.assertTrue(redia.canPerformAction(ItemAbilities.HOE_TILL), "REDIA Tool should expose hoe actions");
      helper.assertTrue(redia.canPerformAction(ItemAbilities.SHEARS_DIG), "REDIA Tool should expose shears actions");
      helper.assertTrue(redia.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "REDIA Tool should mine diamond-grade drops");
      helper.assertTrue(redia.getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 1.0F, "REDIA Tool should mine dirt faster than a hand");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_preserves_original_repair_and_enchantment_compatibility(GameTestHelper helper) {
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.assertTrue(redia.getItem().isValidRepairItem(redia, new ItemStack(Items.DIAMOND)), "REDIA should retain its Diamond-tier anvil repair ingredient");
      helper.assertFalse(redia.getItem().isValidRepairItem(redia, new ItemStack(Items.IRON_INGOT)), "REDIA should reject unrelated anvil repair materials");

      Holder<Enchantment> silkTouch = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
      Holder<Enchantment> efficiency = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
      Holder<Enchantment> fortune = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
      helper.assertTrue(redia.getItem().supportsEnchantment(redia, silkTouch), "REDIA should support Silk Touch like the original digger-category fallback");
      helper.assertTrue(redia.getItem().isPrimaryItemFor(redia, silkTouch), "Silk Touch should remain available as a primary REDIA enchantment");
      helper.assertTrue(redia.getItem().supportsEnchantment(redia, efficiency), "REDIA should retain Efficiency support for coating and books");
      helper.assertFalse(redia.getItem().isPrimaryItemFor(redia, efficiency), "Efficiency should remain excluded from direct enchanting-table selection");
      helper.assertTrue(redia.getItem().supportsEnchantment(redia, fortune), "REDIA should retain Fortune support for coating and books");
      helper.assertFalse(redia.getItem().isPrimaryItemFor(redia, fortune), "Fortune should remain excluded from direct enchanting-table selection");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_uses_original_enchantment_decay_curves(GameTestHelper helper) {
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      redia.setDamageValue(2700);
      helper.assertValueEqual(1, invokeRediaCurve("durabilityDependentEfficiency", redia), "REDIA Tool should allow Efficiency I at 10 percent durability");
      helper.assertValueEqual(0, invokeRediaCurve("durabilityDependentFortune", redia), "REDIA Tool should not allow Fortune below 40 percent durability");
      redia.setDamageValue(1800);
      helper.assertValueEqual(2, invokeRediaCurve("durabilityDependentEfficiency", redia), "REDIA Tool should allow Efficiency II at 40 percent durability");
      helper.assertValueEqual(1, invokeRediaCurve("durabilityDependentFortune", redia), "REDIA Tool should allow Fortune I at 40 percent durability");
      redia.setDamageValue(300);
      helper.assertValueEqual(4, invokeRediaCurve("durabilityDependentEfficiency", redia), "REDIA Tool should allow Efficiency IV near full durability");
      helper.assertValueEqual(3, invokeRediaCurve("durabilityDependentFortune", redia), "REDIA Tool should allow Fortune III near full durability");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_sneak_cycles_original_ground_states(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.setBlock(TEST_POS, Blocks.DIRT);
      helper.setBlock(TEST_POS.above(), Blocks.AIR);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.FARMLAND), "REDIA Tool should cycle dirt to farmland");
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.COARSE_DIRT), "REDIA Tool should cycle farmland to coarse dirt");
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.DIRT_PATH), "REDIA Tool should cycle coarse dirt to dirt path");
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.DIRT), "REDIA Tool should cycle dirt path back to dirt");
      helper.assertValueEqual(4, redia.getDamageValue(), "each successful ground cycle should damage the tool once");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_sneak_snips_shearable_plants(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.setBlock(TEST_POS, Blocks.SHORT_GRASS);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).isAir(), "REDIA Tool should snip shearable plants while sneaking");
      helper.assertValueEqual(1, redia.getDamageValue(), "REDIA Tool should wear after snipping a plant");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_shears_entities(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      Sheep sheep = (Sheep)EntityType.SHEEP.create(helper.getLevel());
      helper.assertTrue(sheep != null, "sheep test target should be creatable");
      BlockPos absolute = helper.absolutePos(TEST_POS);
      sheep.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
      helper.getLevel().addFreshEntity(sheep);
      InteractionResult result = redia.getItem().interactLivingEntity(redia, player, sheep, InteractionHand.MAIN_HAND);
      helper.assertTrue(result.consumesAction(), "REDIA Tool should shear a ready sheep");
      helper.assertTrue(sheep.isSheared(), "REDIA Tool should apply sheep shearing state");
      helper.assertValueEqual(1, redia.getDamageValue(), "REDIA Tool should wear after entity shearing");
      InteractionResult alreadySheared = redia.getItem().interactLivingEntity(redia, player, sheep, InteractionHand.MAIN_HAND);
      helper.assertTrue(alreadySheared.consumesAction(), "REDIA should preserve the original handled result for an already-sheared target");
      helper.assertValueEqual(1, redia.getDamageValue(), "an already-sheared target should not add REDIA wear");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_safe_attack_cancels_protected_targets(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      Villager villager = (Villager)EntityType.VILLAGER.create(helper.getLevel());
      ZombifiedPiglin piglin = (ZombifiedPiglin)EntityType.ZOMBIFIED_PIGLIN.create(helper.getLevel());
      Pig pig = (Pig)EntityType.PIG.create(helper.getLevel());
      helper.assertTrue(villager != null, "villager test target should be creatable");
      helper.assertTrue(piglin != null, "zombified piglin test target should be creatable");
      helper.assertTrue(pig != null, "pig test target should be creatable");
      helper.assertTrue(redia.getItem().onLeftClickEntity(redia, player, villager), "REDIA Tool should cancel villager attacks");
      helper.assertTrue(redia.getItem().onLeftClickEntity(redia, player, piglin), "REDIA Tool should cancel neutral zombified piglin attacks");
      helper.assertFalse(redia.getItem().onLeftClickEntity(redia, player, pig), "REDIA Tool should allow normal passive target attacks");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_sneak_break_fells_connected_logs(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, redia);
      helper.setBlock(TEST_POS, Blocks.OAK_LOG);
      helper.setBlock(TEST_POS.above(), Blocks.OAK_LOG);
      helper.setBlock(TEST_POS.above(2), Blocks.OAK_LOG);
      redia.getItem().mineBlock(redia, helper.getLevel(), Blocks.OAK_LOG.defaultBlockState(), helper.absolutePos(TEST_POS), player);
      helper.assertTrue(helper.getBlockState(TEST_POS.above()).isAir(), "REDIA Tool should fell connected upper logs");
      helper.assertTrue(helper.getBlockState(TEST_POS.above(2)).isAir(), "REDIA Tool should continue felling connected logs");
      helper.assertTrue(redia.getDamageValue() > 1, "tree felling should add extra durability cost");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_tree_felling_adds_original_exhaustion_cost(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, redia);
      helper.setBlock(TEST_POS, Blocks.OAK_LOG);
      helper.setBlock(TEST_POS.above(), Blocks.OAK_LOG);
      helper.setBlock(TEST_POS.above(2), Blocks.OAK_LOG);
      float exhaustionBefore = player.getFoodData().getExhaustionLevel();
      redia.getItem().mineBlock(redia, helper.getLevel(), Blocks.OAK_LOG.defaultBlockState(), helper.absolutePos(TEST_POS), player);
      helper.assertTrue(
         player.getFoodData().getExhaustionLevel() - exhaustionBefore >= 0.5F,
         "REDIA Tool tree felling should apply the original post-fell exhaustion clamp in addition to per-block cost"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_repairs_and_overrepairs_with_diamonds(GameTestHelper helper) {
      RediaToolRepairRecipe recipe = new RediaToolRepairRecipe(CraftingBookCategory.EQUIPMENT);
      ItemStack damaged = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      damaged.setDamageValue(2900);
      CraftingInput repairInput = CraftingInput.of(2, 1, List.of(damaged, new ItemStack(Items.DIAMOND)));
      helper.assertTrue(recipe.matches(repairInput, helper.getLevel()), "REDIA diamond repair recipe should match one tool and one diamond");
      ItemStack repaired = recipe.assemble(repairInput, helper.getLevel().registryAccess());
      helper.assertValueEqual(950, repaired.getDamageValue(), "REDIA diamond repair should restore 65 percent of max durability");

      Holder<Enchantment> efficiency = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
      Holder<Enchantment> fortune = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
      ItemStack pristine = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      ItemStack overrepaired = recipe.assemble(CraftingInput.of(2, 1, List.of(pristine, new ItemStack(Items.DIAMOND))), helper.getLevel().registryAccess());
      helper.assertValueEqual(2, EnchantmentHelper.getItemEnchantmentLevel(efficiency, overrepaired), "first over-repair should add Efficiency II");
      helper.assertValueEqual(0, EnchantmentHelper.getItemEnchantmentLevel(fortune, overrepaired), "first over-repair should not add Fortune yet");
      overrepaired = recipe.assemble(CraftingInput.of(2, 1, List.of(overrepaired, new ItemStack(Items.DIAMOND))), helper.getLevel().registryAccess());
      helper.assertValueEqual(4, EnchantmentHelper.getItemEnchantmentLevel(efficiency, overrepaired), "second over-repair should cap Efficiency at IV");
      overrepaired = recipe.assemble(CraftingInput.of(2, 1, List.of(overrepaired, new ItemStack(Items.DIAMOND))), helper.getLevel().registryAccess());
      helper.assertValueEqual(2, EnchantmentHelper.getItemEnchantmentLevel(fortune, overrepaired), "third over-repair should begin Fortune progression");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void redia_tool_initial_durability_does_not_undo_coating_or_named_repairs(GameTestHelper helper) {
      ItemStack plainCraft = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      invokeRediaInitialCraftingDamage(plainCraft, 50);
      helper.assertValueEqual(1500, plainCraft.getDamageValue(), "plain REDIA crafts should apply the configured initial durability");

      Holder<Enchantment> efficiency = helper.getLevel()
         .registryAccess()
         .lookupOrThrow(Registries.ENCHANTMENT)
         .getOrThrow(Enchantments.EFFICIENCY);
      ItemStack coatedRepair = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      coatedRepair.enchant(efficiency, 2);
      invokeRediaInitialCraftingDamage(coatedRepair, 50);
      helper.assertValueEqual(0, coatedRepair.getDamageValue(), "over-repaired REDIA coatings must remain fully repaired after crafting");

      ItemStack namedRepair = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      namedRepair.set(DataComponents.CUSTOM_NAME, Component.literal("Field REDIA"));
      invokeRediaInitialCraftingDamage(namedRepair, 50);
      helper.assertValueEqual(0, namedRepair.getDamageValue(), "REDIA repairs carrying non-damage data must not be reset to initial wear");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void ariadne_coal_places_route_marker(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack marker = new ItemStack((ItemLike)EngineerToolsModule.ARIADNE_COAL.get());
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.north(), Blocks.AIR);
      helper.assertValueEqual(1, marker.getMaxStackSize(), "Ariadne Coal should use the original single-item stack size");
      helper.assertValueEqual(100, marker.getMaxDamage(), "Ariadne Coal should use the original 100-use durability");
      ((Item)EngineerToolsModule.ARIADNE_COAL.get()).onItemUseFirst(marker, context(helper, player, marker, TEST_POS, Direction.NORTH, 0.95, 0.95, 0.0));
      BlockState placed = helper.getBlockState(TEST_POS.north());
      helper.assertTrue(placed.is((Block)ModBlocks.ARIADNE_MARKER.get()), "Ariadne Coal should place a black arrow route marker");
      helper.assertTrue(placed.getValue(PortedBlocks.FACING) == Direction.NORTH, "Ariadne Coal marker should point toward the clicked border");
      helper.assertValueEqual(
         3, (Integer)placed.getValue(PortedBlocks.ARIADNE_MARKER_ROTATION), "Ariadne Coal marker should support original diagonal clicked face directions"
      );
      helper.assertValueEqual(1, marker.getDamageValue(), "Ariadne Coal should lose one durability per marker");
      helper.assertValueEqual(1, marker.getCount(), "Ariadne Coal should not be consumed until its durability is depleted");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void ariadne_coal_rejects_unsupported_face_without_wearing(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack marker = new ItemStack((ItemLike)EngineerToolsModule.ARIADNE_COAL.get());
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.north(), Blocks.STONE);
      InteractionResult result = ((Item)EngineerToolsModule.ARIADNE_COAL.get()).onItemUseFirst(
         marker, context(helper, player, marker, TEST_POS, Direction.NORTH)
      );
      helper.assertTrue(result == InteractionResult.PASS, "invalid Ariadne placement should pass through to the clicked block interaction");
      helper.assertValueEqual(0, marker.getDamageValue(), "failed Ariadne Coal use should not wear the item");
      helper.assertTrue(helper.getBlockState(TEST_POS.north()).is(Blocks.STONE), "failed Ariadne Coal use should not replace occupied blocks");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void ariadne_coal_breaks_after_original_final_use(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack marker = new ItemStack((ItemLike)EngineerToolsModule.ARIADNE_COAL.get());
      marker.setDamageValue(99);
      player.setItemInHand(InteractionHand.MAIN_HAND, marker);
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.north(), Blocks.AIR);
      ((Item)EngineerToolsModule.ARIADNE_COAL.get()).onItemUseFirst(marker, context(helper, player, marker, TEST_POS, Direction.NORTH));
      helper.assertTrue(helper.getBlockState(TEST_POS.north()).is((Block)ModBlocks.ARIADNE_MARKER.get()), "final Ariadne Coal use should still place a marker");
      helper.assertTrue(player.getMainHandItem().isEmpty(), "Ariadne Coal should break after the original 100th use");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void ariadne_coal_creative_placement_preserves_original_wear(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.getAbilities().instabuild = true;
      ItemStack marker = new ItemStack((ItemLike)EngineerToolsModule.ARIADNE_COAL.get());
      marker.setDamageValue(99);
      player.setItemInHand(InteractionHand.MAIN_HAND, marker);
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.north(), Blocks.AIR);
      ((Item)EngineerToolsModule.ARIADNE_COAL.get()).onItemUseFirst(marker, context(helper, player, marker, TEST_POS, Direction.NORTH));
      helper.assertTrue(helper.getBlockState(TEST_POS.north()).is((Block)ModBlocks.ARIADNE_MARKER.get()), "creative Ariadne Coal use should still place a marker");
      helper.assertTrue(player.getMainHandItem().isEmpty(), "the original Ariadne Coal should wear and break even in creative mode");
      helper.succeed();
   }

   @GameTest(template = "empty", batch = "sleepingBagNight", timeoutTicks = 40)
   public static void sleeping_bag_enters_normal_sleep_without_shortcutting_world_time(GameTestHelper helper) {
      ServerPlayer player = FakePlayerFactory.get(
         helper.getLevel(), new GameProfile(UUID.fromString("8c6e5cb4-5c5e-4fb8-8a9b-c4b0a9c42601"), "sleeping-bag-night-test")
      );
      player.setGameMode(GameType.SURVIVAL);
      Vec3 sleepCenter = Vec3.atCenterOf(helper.absolutePos(TEST_POS.above()));
      player.setPos(sleepCenter.x, sleepCenter.y, sleepCenter.z);
      ItemStack bag = new ItemStack((ItemLike)EngineerToolsModule.SLEEPING_BAG.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, bag);
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.above(), Blocks.AIR);
      helper.getLevel().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10.0)).forEach(Entity::discard);
      helper.assertFalse(player.isCreative(), "night sleeping-bag fixture must be a survival player");
      helper.assertFalse(player.isSpectator(), "night sleeping-bag fixture must not be a spectator");
      helper.assertTrue(player.isAlive(), "night sleeping-bag fixture must be alive");
      helper.getLevel().setDayTime(13000L);
      helper.runAfterDelay(2, () -> {
         helper.assertTrue(helper.getLevel().isNight(), "night sleeping-bag fixture must let the level refresh its sky state");
         long timeBeforeUse = helper.getLevel().getDayTime();
         InteractionResult result = bag.getItem().onItemUseFirst(bag, context(helper, player, bag, TEST_POS, Direction.UP));
         helper.assertTrue(result.consumesAction(), "sleeping bag should consume a valid nighttime top-face use");
         helper.assertTrue(player.isSleeping(), "sleeping bag should enter Minecraft's normal sleeping state at night");
         helper.assertValueEqual(timeBeforeUse, helper.getLevel().getDayTime(), "sleeping bag should use the sleep system instead of directly changing time");
         helper.assertValueEqual(0, bag.getDamageValue(), "the original sleeping bag does not consume durability");
         player.stopSleepInBed(true, true);
         helper.succeed();
      });
   }

   @GameTest(template = "empty", batch = "sleepingBagDay", timeoutTicks = 40)
   public static void sleeping_bag_rejects_daytime_without_damage(GameTestHelper helper) {
      ServerPlayer player = FakePlayerFactory.get(
         helper.getLevel(), new GameProfile(UUID.fromString("9319e69e-ad9c-47f1-85f1-24e7c8caf799"), "sleeping-bag-day-test")
      );
      player.setGameMode(GameType.SURVIVAL);
      Vec3 sleepCenter = Vec3.atCenterOf(helper.absolutePos(TEST_POS.above()));
      player.setPos(sleepCenter.x, sleepCenter.y, sleepCenter.z);
      ItemStack bag = new ItemStack((ItemLike)EngineerToolsModule.SLEEPING_BAG.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, bag);
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.above(), Blocks.AIR);
      helper.getLevel().setDayTime(1000L);
      bag.getItem().onItemUseFirst(bag, context(helper, player, bag, TEST_POS, Direction.UP));
      helper.assertFalse(player.isSleeping(), "daytime sleeping bag use should be rejected before the air-position sleep call");
      helper.assertValueEqual(0, bag.getDamageValue(), "daytime sleeping bag use should not damage the item");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sleeping_bag_offhand_keeps_air_position_sleep_valid(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack bag = new ItemStack((ItemLike)EngineerToolsModule.SLEEPING_BAG.get());
      player.setItemInHand(InteractionHand.OFF_HAND, bag);
      CanContinueSleepingEvent offhandEvent = new CanContinueSleepingEvent(player, Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
      SleepingBagItem.onCanContinueSleeping(offhandEvent);
      helper.assertTrue(offhandEvent.mayContinueSleeping(), "an offhand sleeping bag should keep the air-position sleep valid");

      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      CanContinueSleepingEvent emptyHandsEvent = new CanContinueSleepingEvent(player, Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
      SleepingBagItem.onCanContinueSleeping(emptyHandsEvent);
      helper.assertFalse(emptyHandsEvent.mayContinueSleeping(), "the sleep exception should not apply after the sleeping bag leaves both hands");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sleeping_bag_uses_original_durability_and_hidden_bar(GameTestHelper helper) {
      ItemStack bag = new ItemStack((ItemLike)EngineerToolsModule.SLEEPING_BAG.get());
      helper.assertValueEqual(4096, bag.getMaxDamage(), "sleeping bag should use the original 4096 durability");
      bag.setDamageValue(1);
      helper.assertFalse(bag.isBarVisible(), "sleeping bag should keep the original hidden durability bar");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void stimpack_auto_injection_applies_original_protection_buffs(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack stimpack = new ItemStack((ItemLike)EngineerToolsModule.STIMPACK.get());
      player.setHealth(4.0F);
      for (int tick = 0; tick <= 20; tick++) {
         stimpack.getItem().inventoryTick(stimpack, helper.getLevel(), player, 0, false);
      }
      helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "stimpack should apply original regeneration");
      helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "stimpack should apply original movement speed");
      helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "stimpack should apply original damage resistance");
      helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "stimpack should apply original fire resistance");
      helper.assertValueEqual(2, stimpack.getMaxDamage(), "stimpack should preserve the original two-use capacity");
      helper.assertValueEqual(1, stimpack.getDamageValue(), "automatic stimpack injection should consume one use after arming");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void stimpack_preserves_partial_arming_when_health_recovers(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack stimpack = new ItemStack((ItemLike)EngineerToolsModule.STIMPACK.get());
      player.setHealth(4.0F);
      for (int tick = 0; tick < 5; tick++) {
         stimpack.getItem().inventoryTick(stimpack, helper.getLevel(), player, 0, false);
      }
      int armedBeforeRecovery = stimpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("ta");
      player.setHealth(player.getMaxHealth());
      stimpack.getItem().inventoryTick(stimpack, helper.getLevel(), player, 0, false);
      int armedAfterRecovery = stimpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("ta");
      helper.assertValueEqual(5, armedBeforeRecovery, "stimpack should accumulate five arming ticks");
      helper.assertValueEqual(armedBeforeRecovery, armedAfterRecovery, "health recovery should pause rather than reset upstream arming progress");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void diving_capsule_uses_original_air_trigger_and_refill_fraction(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack capsule = new ItemStack((ItemLike)EngineerToolsModule.DIVING_CAPSULE.get());
      int maxAir = player.getMaxAirSupply();
      int startingAir = maxAir * 8 / 30;
      int expectedAir = Math.min(maxAir, startingAir + maxAir * 7 / 10);
      player.setAirSupply(startingAir);
      capsule.getItem().inventoryTick(capsule, helper.getLevel(), player, 0, false);
      helper.assertValueEqual(expectedAir, player.getAirSupply(), "diving capsule should trigger below 30 percent air and restore 70 percent of max air");
      helper.assertValueEqual(100, capsule.getMaxDamage(), "diving capsule should preserve the original ten-use durability scale");
      helper.assertValueEqual(10, capsule.getDamageValue(), "automatic diving capsule refill should consume one of ten uses");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void diving_capsule_curios_tick_respects_inventory_precedence(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack inventoryCapsule = new ItemStack((ItemLike)EngineerToolsModule.DIVING_CAPSULE.get());
      ItemStack equippedCapsule = new ItemStack((ItemLike)EngineerToolsModule.DIVING_CAPSULE.get());
      player.getInventory().items.set(0, inventoryCapsule);
      int startingAir = player.getMaxAirSupply() / 4;
      player.setAirSupply(startingAir);
      equippedCapsule.getItem().inventoryTick(equippedCapsule, helper.getLevel(), player, -1, false);
      helper.assertValueEqual(startingAir, player.getAirSupply(), "a Curios-style slot -1 tick should defer to an earlier inventory capsule");
      helper.assertValueEqual(0, equippedCapsule.getDamageValue(), "deferred equipped capsule should not consume durability");
      helper.succeed();
   }

   @SuppressWarnings("unchecked")
   @GameTest(template = "empty", timeoutTicks = 40)
   public static void curios_player_slots_run_real_diving_capsule_inventory_tick(GameTestHelper helper) {
      helper.assertTrue(ModList.get().isLoaded("curios"), "Curios must be present in the GameTest runtime for the real integration smoke");
      ServerPlayer player = FakePlayerFactory.get(
         helper.getLevel(),
         new GameProfile(UUID.randomUUID(), "curios-test-player")
      );
      ItemStack capsule = new ItemStack((ItemLike)EngineerToolsModule.DIVING_CAPSULE.get());
      ItemStack materialBox = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      ItemStack stimpack = new ItemStack((ItemLike)EngineerToolsModule.STIMPACK.get());
      helper.assertTrue(
         capsule.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm"))),
         "the modernized #curios:charm tag should accept the diving capsule"
      );
      helper.assertTrue(
         materialBox.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "belt")))
            && materialBox.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "body"))),
         "the modernized Curios tags should preserve the material box belt and body slots"
      );
      helper.assertTrue(
         stimpack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "belt")))
            && stimpack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "body")))
            && stimpack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm"))),
         "the modernized Curios tags should preserve all three upstream stimpack slots"
      );
      try {
         Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
         Optional<?> inventory = (Optional<?>)curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class)
            .invoke(null, player);
         helper.assertTrue(inventory.isPresent(), "Curios should attach an inventory to a player entity");
         Class<?> handlerType = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
         Object handler = inventory.orElseThrow();
         Map<?, ?> slots = (Map<?, ?>)handlerType.getMethod("getCurios").invoke(handler);
         helper.assertTrue(
            slots.containsKey("belt") && slots.containsKey("body") && slots.containsKey("charm"),
            "Curios should attach every upstream Engineer's Tools slot to players"
         );
         handlerType.getMethod("setEquippedCurio", String.class, int.class, ItemStack.class)
            .invoke(handler, "belt", 0, materialBox);
         handlerType.getMethod("setEquippedCurio", String.class, int.class, ItemStack.class)
            .invoke(handler, "body", 0, stimpack);
         handlerType.getMethod("setEquippedCurio", String.class, int.class, ItemStack.class)
            .invoke(handler, "charm", 0, capsule);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Could not exercise the Curios 9.5.1 integration", exception);
      }

      int maxAir = player.getMaxAirSupply();
      int startingAir = maxAir * 8 / 30;
      int expectedAir = Math.min(maxAir, startingAir + maxAir * 7 / 10);
      player.setAirSupply(startingAir);
      EventHooks.fireEntityTickPost(player);
      helper.assertTrue(
         player.getAirSupply() >= expectedAir,
         "Curios should invoke the equipped capsule's vanilla inventory tick from NeoForge's post-entity-tick event"
      );
      helper.assertValueEqual(10, capsule.getDamageValue(), "the Curios-driven refill should consume exactly one capsule use");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void engineer_tools_config_preserves_upstream_defaults(GameTestHelper helper) {
      helper.assertFalse(EngineerToolsConfig.withChestLoot(), "optional Engineer's Tools chest loot must remain disabled by default");
      helper.assertValueEqual(3000, EngineerToolsConfig.rediaToolDurability(), "REDIA default durability should match 1.1.7");
      helper.assertValueEqual(100, EngineerToolsConfig.rediaInitialDurabilityPercent(), "REDIA should be crafted at full durability by default");
      helper.assertTrue(EngineerToolsConfig.withSafeAttacking(), "REDIA safe-attacking should remain enabled by default");
      helper.assertTrue(EngineerToolsConfig.isItemEnabled("redia_tool"), "empty include/exclude patterns should leave Engineer's Tools items enabled");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void optional_chest_loot_uses_exact_sixteen_target_gate(GameTestHelper helper) {
      LootTable enabledTarget = LootTable.lootTable().build();
      boolean injected = EngineerToolsLoot.injectAdditionalLoot(
         enabledTarget,
         ResourceLocation.withDefaultNamespace("chests/abandoned_mineshaft"),
         true
      );
      helper.assertTrue(injected, "enabled upstream mineshaft loot should receive the additional wrapper pool");
      helper.assertTrue(enabledTarget.getPool(EngineerToolsLoot.POOL_NAME) != null, "the injected pool should use the stable upstream-derived name");
      LootTable disabledTarget = LootTable.lootTable().build();
      helper.assertFalse(
         EngineerToolsLoot.injectAdditionalLoot(disabledTarget, ResourceLocation.withDefaultNamespace("chests/abandoned_mineshaft"), false),
         "the authoritative default-off setting should prevent injection"
      );
      LootTable unrelated = LootTable.lootTable().build();
      helper.assertFalse(
         EngineerToolsLoot.injectAdditionalLoot(unrelated, ResourceLocation.withDefaultNamespace("chests/end_city_treasure"), true),
         "loot injection must not expand beyond the sixteen upstream targets"
      );
      helper.assertValueEqual(16, EngineerToolsLoot.targetCount(), "the port should retain every and only upstream chest target");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void tracker_tooltip_restores_distance_and_single_line_format(GameTestHelper helper) {
      ItemStack tracker = new ItemStack((ItemLike)EngineerToolsModule.TRACKER.get());
      CompoundTag data = new CompoundTag();
      data.putString("target_dimension", Level.OVERWORLD.location().toString());
      data.putInt("target_x", 10);
      data.putInt("target_y", 64);
      data.putInt("target_z", -5);
      data.putLong("client_player_position", new BlockPos(13, 68, -5).asLong());
      tracker.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
      List<Component> tooltip = new ArrayList<>();
      tracker.getItem().appendHoverText(tracker, TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.NORMAL);
      List<String> targetLines = tooltip.stream().map(Component::getString).filter(line -> line.startsWith("Location:")).toList();
      helper.assertValueEqual(1, targetLines.size(), "tracker target details should occupy one contextual line");
      helper.assertValueEqual("Location: [10,64,-5] | 5 blocks away", targetLines.getFirst(), "tracker distance and bracket formatting should match upstream");
      helper.assertFalse(
         tooltip.stream().map(Component::getString).anyMatch(line -> line.equals(Level.OVERWORLD.location().toString())),
         "tracker tooltip should never expose a raw dimension resource location"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void tracker_tooltip_uses_dimension_suffix_when_distance_is_unavailable(GameTestHelper helper) {
      ItemStack tracker = new ItemStack((ItemLike)EngineerToolsModule.TRACKER.get());
      CompoundTag data = new CompoundTag();
      data.putString("target_dimension", Level.NETHER.location().toString());
      data.putInt("target_x", 1);
      data.putInt("target_y", 2);
      data.putInt("target_z", 3);
      data.putString("target_entity_name", "Test Friend");
      tracker.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
      List<Component> tooltip = new ArrayList<>();
      tracker.getItem().appendHoverText(tracker, TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.NORMAL);
      helper.assertTrue(
         tooltip.stream().map(Component::getString).anyMatch(line -> line.equals("Entity: Test Friend in the Nether")),
         "cross-dimension tracker targets should append the translated dimension suffix to the target line"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void tracker_tooltip_ignores_incomplete_saved_target_data(GameTestHelper helper) {
      ItemStack tracker = new ItemStack((ItemLike)EngineerToolsModule.TRACKER.get());
      CompoundTag incomplete = new CompoundTag();
      incomplete.putString("target_dimension", helper.getLevel().dimension().location().toString());
      tracker.set(DataComponents.CUSTOM_DATA, CustomData.of(incomplete));
      List<Component> tooltip = new ArrayList<>();
      tracker.getItem().appendHoverText(tracker, TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.NORMAL);
      helper.assertFalse(
         tooltip.stream().anyMatch(line -> line.getString().contains("Location:")), "tracker should not show a location when coordinate data is incomplete"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void tracker_tooltip_ignores_malformed_saved_target_data(GameTestHelper helper) {
      ItemStack tracker = new ItemStack((ItemLike)EngineerToolsModule.TRACKER.get());
      CompoundTag malformed = new CompoundTag();
      malformed.putString("target_dimension", helper.getLevel().dimension().location().toString());
      malformed.putString("target_x", "not-a-number");
      malformed.putString("target_y", "not-a-number");
      malformed.putString("target_z", "not-a-number");
      tracker.set(DataComponents.CUSTOM_DATA, CustomData.of(malformed));
      List<Component> tooltip = new ArrayList<>();
      tracker.getItem().appendHoverText(tracker, TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.NORMAL);
      helper.assertFalse(
         tooltip.stream().anyMatch(line -> line.getString().contains("Location:")), "tracker should not show a fallback origin location for malformed coordinate data"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_preserves_original_54_slot_inventory(GameTestHelper helper) {
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      NonNullList<ItemStack> contents = NonNullList.withSize(MaterialBoxItem.SLOT_COUNT, ItemStack.EMPTY);
      contents.set(0, new ItemStack(Items.COBBLESTONE, 64));
      contents.set(53, new ItemStack(Items.DIRT, 32));
      MaterialBoxItem.saveContents(box, contents, helper.getLevel().registryAccess());
      NonNullList<ItemStack> loaded = MaterialBoxItem.loadContents(box, helper.getLevel().registryAccess());
      helper.assertValueEqual(MaterialBoxItem.SLOT_COUNT, loaded.size(), "material box should expose all original 54 storage slots");
      helper.assertTrue(loaded.get(0).is(Items.COBBLESTONE), "material box should preserve the first stored stack");
      helper.assertValueEqual(64, loaded.get(0).getCount(), "material box should preserve first stack count");
      helper.assertTrue(loaded.get(53).is(Items.DIRT), "material box should preserve the last storage slot");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_accepts_mixed_materials_like_original(GameTestHelper helper) {
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      NonNullList<ItemStack> contents = NonNullList.withSize(MaterialBoxItem.SLOT_COUNT, ItemStack.EMPTY);
      contents.set(0, new ItemStack(Items.COBBLESTONE, 16));
      contents.set(1, new ItemStack(Items.DIRT, 16));
      MaterialBoxItem.saveContents(box, contents, helper.getLevel().registryAccess());
      NonNullList<ItemStack> loaded = MaterialBoxItem.loadContents(box, helper.getLevel().registryAccess());
      helper.assertTrue(loaded.get(0).is(Items.COBBLESTONE), "material box should preserve one material type");
      helper.assertTrue(loaded.get(1).is(Items.DIRT), "material box should preserve a second material type");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_preserves_modified_item_stacks(GameTestHelper helper) {
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      ItemStack namedCobble = new ItemStack(Items.COBBLESTONE, 16);
      namedCobble.set(DataComponents.CUSTOM_NAME, Component.literal("Named Cobble"));
      NonNullList<ItemStack> contents = NonNullList.withSize(MaterialBoxItem.SLOT_COUNT, ItemStack.EMPTY);
      contents.set(0, namedCobble);
      MaterialBoxItem.saveContents(box, contents, helper.getLevel().registryAccess());
      ItemStack loaded = MaterialBoxItem.loadContents(box, helper.getLevel().registryAccess()).get(0);
      helper.assertTrue(loaded.is(Items.COBBLESTONE), "material box should preserve a modified stack item");
      helper.assertValueEqual(16, loaded.getCount(), "material box should preserve modified stack count");
      helper.assertTrue(loaded.has(DataComponents.CUSTOM_NAME), "material box should preserve stack components");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void filled_material_box_uses_original_protected_dropped_entity(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MaterialBoxItem item = (MaterialBoxItem)EngineerToolsModule.MATERIAL_BOX.get();
      ItemStack box = new ItemStack(item);
      NonNullList<ItemStack> contents = NonNullList.withSize(MaterialBoxItem.SLOT_COUNT, ItemStack.EMPTY);
      contents.set(0, new ItemStack(Items.COBBLESTONE));
      MaterialBoxItem.saveContents(box, contents, helper.getLevel().registryAccess());
      BlockPos absolute = helper.absolutePos(TEST_POS);
      ItemEntity original = new ItemEntity(helper.getLevel(), absolute.getX(), absolute.getY(), absolute.getZ(), box.copy());
      original.setThrower(player);
      Entity replacement = item.createEntity(helper.getLevel(), original, box.copy());
      helper.assertTrue(replacement instanceof ItemEntity, "material box should create a custom dropped item entity");
      ItemEntity dropped = (ItemEntity)replacement;
      helper.assertTrue(dropped.isInvulnerable(), "filled material box entity should keep upstream invulnerability");
      helper.assertTrue(dropped.lifespan >= 72_000, "filled material box entity should keep upstream extended lifespan");
      helper.assertTrue(dropped.getOwner() == player, "custom material box entity should preserve its thrower");
      int lifespanBeforeDamage = dropped.lifespan;
      helper.assertFalse(dropped.hurt(helper.getLevel().damageSources().generic(), 1.0F), "filled material box should ignore item-entity damage");
      helper.assertValueEqual(lifespanBeforeDamage - 200, dropped.lifespan, "attempted damage should apply the upstream lifespan penalty");
      Pig pig = (Pig)EntityType.PIG.create(helper.getLevel());
      helper.assertTrue(pig != null && item.onLeftClickEntity(box, player, pig), "material box should suppress entity attacks while held");
      helper.assertFalse(item.canAttackBlock(Blocks.STONE.defaultBlockState(), helper.getLevel(), absolute, player), "material box should suppress block attacks while held");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void portable_tool_menus_reject_offhand_swap_packets(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      player.setItemInHand(InteractionHand.OFF_HAND, box);
      MaterialBoxMenu boxMenu = new MaterialBoxMenu(9, player.getInventory(), InteractionHand.OFF_HAND);
      boxMenu.clicked(0, 40, ClickType.SWAP, player);
      helper.assertTrue(player.getOffhandItem() == box, "offhand SWAP must not move the open material box into its own storage");
      helper.assertTrue(boxMenu.getSlot(0).getItem().isEmpty(), "rejected material-box SWAP should leave storage unchanged");

      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.OFF_HAND, press);
      MusliBarPressMenu pressMenu = new MusliBarPressMenu(10, player.getInventory(), InteractionHand.OFF_HAND);
      pressMenu.clicked(0, 40, ClickType.SWAP, player);
      helper.assertTrue(player.getOffhandItem() == press, "offhand SWAP must not move the open Musli press into its own input");
      helper.assertTrue(pressMenu.getSlot(0).getItem().isEmpty(), "rejected Musli-press SWAP should leave input unchanged");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_migrates_legacy_counter_storage(GameTestHelper helper) {
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      CompoundTag tag = new CompoundTag();
      tag.putString("stored_item", "minecraft:cobblestone");
      tag.putInt("stored_count", MaterialBoxItem.CAPACITY + 100);
      box.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      helper.assertValueEqual(1, box.getMaxStackSize(), "legacy filled boxes must be unstackable before their contents are explicitly read");
      NonNullList<ItemStack> migrated = MaterialBoxItem.loadContents(box, helper.getLevel().registryAccess());
      helper.assertValueEqual(MaterialBoxItem.CAPACITY, migrated.stream().mapToInt(ItemStack::getCount).sum(), "legacy single-material data should migrate without exceeding its old cap");
      helper.assertValueEqual(1, box.getMaxStackSize(), "loaded legacy filled boxes should immediately normalize to the upstream single-item stack limit");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_recovers_from_invalid_legacy_item_data(GameTestHelper helper) {
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      CompoundTag tag = new CompoundTag();
      tag.putString("stored_item", "immersive_engineer_decor_controls_tool_reforged:missing_material");
      tag.putInt("stored_count", 48);
      box.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      NonNullList<ItemStack> migrated = MaterialBoxItem.loadContents(box, helper.getLevel().registryAccess());
      helper.assertTrue(migrated.stream().allMatch(ItemStack::isEmpty), "invalid legacy item ids should migrate as empty storage");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_uses_original_seed_and_food_point_contract(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      MusliBarPressMenu menu = new MusliBarPressMenu(1, player.getInventory(), InteractionHand.MAIN_HAND);
      menu.getSlot(0).set(new ItemStack(Items.BREAD, 2));
      helper.assertValueEqual(10, menu.foodStored(), "two bread should contribute ten hunger points");
      menu.getSlot(0).set(new ItemStack(Items.WHEAT_SEEDS));
      helper.assertTrue(menu.getSlot(1).getItem().is(EngineerToolsModule.MUSLI_BAR.get()), "one seed and eight food points should produce one bar");
      helper.assertValueEqual(1, menu.getSlot(1).getItem().getCount(), "press should produce exactly one bar per point threshold");
      helper.assertValueEqual(2, menu.foodStored(), "press should retain unused food points");
      helper.assertValueEqual(0, press.getDamageValue(), "the original press is reusable and has no wear");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_accepts_original_seed_variants(GameTestHelper helper) {
      assertMusliSeedAccepted(helper, Items.MELON_SEEDS);
      assertMusliSeedAccepted(helper, Items.PUMPKIN_SEEDS);
      assertMusliSeedAccepted(helper, Items.BEETROOT_SEEDS);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_stores_partial_food_until_seed_arrives(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      MusliBarPressMenu menu = new MusliBarPressMenu(2, player.getInventory(), InteractionHand.MAIN_HAND);
      menu.getSlot(0).set(new ItemStack(Items.BREAD));
      helper.assertValueEqual(5, menu.foodStored(), "press should store the food value even before a seed is available");
      helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "press should not produce a bar without both point thresholds");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_consumes_partial_overflow_item_like_original(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      MusliBarPressItem.saveState(
         press,
         MusliBarPressItem.ListLike.copyOf(List.of(ItemStack.EMPTY, ItemStack.EMPTY)),
         0,
         MusliBarPressItem.MAX_FOOD - 1,
         helper.getLevel().registryAccess()
      );
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      MusliBarPressMenu menu = new MusliBarPressMenu(11, player.getInventory(), InteractionHand.MAIN_HAND);
      menu.getSlot(0).set(new ItemStack(Items.APPLE));
      helper.assertValueEqual(MusliBarPressItem.MAX_FOOD, menu.foodStored(), "press should clamp food storage at the original 768-point cap");
      helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "upstream press consumes the item whose nutrition partially crosses the cap");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_clamps_original_seed_capacity(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      MusliBarPressMenu menu = new MusliBarPressMenu(3, player.getInventory(), InteractionHand.MAIN_HAND);
      for (int stack = 0; stack < 8; stack++) {
         menu.getSlot(0).set(new ItemStack(Items.WHEAT_SEEDS, 64));
      }
      helper.assertValueEqual(MusliBarPressItem.MAX_SEEDS, menu.seedsStored(), "press should clamp storage to the original 512 seeds");
      menu.getSlot(0).set(new ItemStack(Items.WHEAT_SEEDS, 64));
      helper.assertValueEqual(64, menu.getSlot(0).getItem().getCount(), "overflowing seeds should remain in the input slot");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_stops_at_one_output_stack(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      MusliBarPressMenu menu = new MusliBarPressMenu(4, player.getInventory(), InteractionHand.MAIN_HAND);
      menu.getSlot(0).set(new ItemStack(Items.WHEAT_SEEDS, 64));
      menu.getSlot(0).set(new ItemStack(Items.COOKED_BEEF, 64));
      helper.assertValueEqual(64, menu.getSlot(1).getItem().getCount(), "press should stop production at one full output stack");
      helper.assertValueEqual(0, menu.seedsStored(), "one seed should be consumed per output bar");
      helper.assertValueEqual(0, menu.foodStored(), "eight hunger points should be consumed per output bar");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_persists_state_in_the_item(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      MusliBarPressMenu first = new MusliBarPressMenu(5, player.getInventory(), InteractionHand.MAIN_HAND);
      first.getSlot(0).set(new ItemStack(Items.BREAD));
      first.removed(player);
      MusliBarPressMenu reopened = new MusliBarPressMenu(6, player.getInventory(), InteractionHand.MAIN_HAND);
      helper.assertValueEqual(5, reopened.foodStored(), "press should persist food points on its item stack");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void portable_tool_menus_close_if_the_held_stack_is_replaced(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, box);
      MaterialBoxMenu boxMenu = new MaterialBoxMenu(7, player.getInventory(), InteractionHand.MAIN_HAND);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get()));
      helper.assertFalse(boxMenu.stillValid(player), "material box menu should not save into a stack that is no longer held");

      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      MusliBarPressMenu pressMenu = new MusliBarPressMenu(8, player.getInventory(), InteractionHand.MAIN_HAND);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get()));
      helper.assertFalse(pressMenu.stillValid(player), "Musli bar press menu should not save into a stack that is no longer held");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void legacy_grits_stay_registered_for_world_compatibility(GameTestHelper helper) {
      helper.assertTrue(EngineerToolsModule.IRON_GRIT.get() instanceof TooltipItem, "legacy iron grit should remain registered");
      helper.assertTrue(EngineerToolsModule.GOLD_GRIT.get() instanceof TooltipItem, "legacy gold grit should remain registered");
      helper.assertTrue(EngineerToolsModule.isLegacyCompatibilityItem(EngineerToolsModule.IRON_GRIT.getId().getPath()), "iron grit should be classified as legacy content");
      helper.assertTrue(EngineerToolsModule.isLegacyCompatibilityItem(EngineerToolsModule.GOLD_GRIT.getId().getPath()), "gold grit should be classified as legacy content");
      assertRecipeResult(helper, "iron_grit_smelting", "minecraft", "iron_ingot", 1);
      assertRecipeResult(helper, "iron_grit_blasting", "minecraft", "iron_ingot", 1);
      assertRecipeResult(helper, "gold_grit_smelting", "minecraft", "gold_ingot", 1);
      assertRecipeResult(helper, "gold_grit_blasting", "minecraft", "gold_ingot", 1);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void crushing_hammer_outputs_immersive_engineering_dusts(GameTestHelper helper) {
      assertRecipeResult(helper, "iron_grit_from_iron_ore", "immersiveengineering", "dust_iron", 2);
      assertRecipeResult(helper, "gold_grit_from_gold_ore", "immersiveengineering", "dust_gold", 2);
      assertRecipeResult(helper, "aluminum_grit_from_ore", "immersiveengineering", "dust_aluminum", 2);
      assertRecipeResult(helper, "copper_grit_from_ore", "immersiveengineering", "dust_copper", 2);
      assertRecipeResult(helper, "lead_grit_from_ore", "immersiveengineering", "dust_lead", 2);
      assertRecipeResult(helper, "nickel_grit_from_ore", "immersiveengineering", "dust_nickel", 2);
      assertRecipeResult(helper, "silver_grit_from_ore", "immersiveengineering", "dust_silver", 2);
      assertRecipeResult(helper, "uranium_grit_from_ore", "immersiveengineering", "dust_uranium", 2);
      ResourceLocation goldId = ResourceLocation.fromNamespaceAndPath(
         "immersive_engineer_decor_controls_tool_reforged", "gold_grit_from_gold_ore"
      );
      CrushingRecipe goldRecipe = (CrushingRecipe)helper.getLevel().getRecipeManager().byKey(goldId).orElseThrow().value();
      ItemStack hammer = new ItemStack((ItemLike)EngineerToolsModule.CRUSHING_HAMMER.get());
      CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.GOLD_ORE), hammer));
      helper.assertTrue(goldRecipe.matches(input, helper.getLevel()), "crushing should remain available in a normal crafting grid");
      CraftingInput deepslateInput = CraftingInput.of(2, 1, List.of(new ItemStack(Items.DEEPSLATE_GOLD_ORE), hammer));
      helper.assertTrue(goldRecipe.matches(deepslateInput, helper.getLevel()), "common gold ore tag should modernize the upstream ore recipe for deepslate");
      CraftingInput rawInput = CraftingInput.of(2, 1, List.of(new ItemStack(Items.RAW_GOLD), hammer));
      helper.assertFalse(goldRecipe.matches(rawInput, helper.getLevel()), "raw gold should not expand the original ore-only crushing contract");
      helper.assertFalse(goldRecipe.isSpecial(), "dust crushing should remain visible in the recipe book like upstream shapeless recipes");
      helper.assertValueEqual("dust", goldRecipe.getGroup(), "recipe-book grouping should reflect the intentional grit-to-dust modernization");
      helper.assertValueEqual(8, goldRecipe.getRemainingItems(input).get(1).getDamageValue(), "gold crushing should apply the original eight hammer wear");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void crushing_hammer_entity_hit_cancels_vanilla_attack(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack hammer = new ItemStack((ItemLike)EngineerToolsModule.CRUSHING_HAMMER.get());
      Pig target = (Pig)EntityType.PIG.create(helper.getLevel());
      helper.assertTrue(target != null, "pig test target should be creatable");
      boolean cancelled = hammer.getItem().onLeftClickEntity(hammer, player, target);
      helper.assertTrue(cancelled, "crushing hammer should keep original knockback-only hits by cancelling vanilla attack damage");
      helper.assertTrue(hammer.canDisableShield(new ItemStack(Items.SHIELD), target, player), "crushing hammer should disable shields like the original item");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void special_tools_reject_vanilla_repair_and_enchanting(GameTestHelper helper) {
      RepairItemRecipe repairRecipe = new RepairItemRecipe(CraftingBookCategory.MISC);
      ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
      List<Item> damageableTools = List.of(
         (Item)EngineerToolsModule.CRUSHING_HAMMER.get(),
         (Item)EngineerToolsModule.ARIADNE_COAL.get(),
         (Item)EngineerToolsModule.STIMPACK.get(),
         (Item)EngineerToolsModule.SLEEPING_BAG.get(),
         (Item)EngineerToolsModule.DIVING_CAPSULE.get()
      );

      for (Item tool : damageableTools) {
         ItemStack first = new ItemStack(tool);
         ItemStack second = new ItemStack(tool);
         first.setDamageValue(1);
         second.setDamageValue(1);
         helper.assertFalse(first.isRepairable(), tool + " should preserve the original setNoRepair contract");
         helper.assertFalse(tool.isEnchantable(first), tool + " should reject enchanting-table use");
         helper.assertFalse(tool.isBookEnchantable(first, enchantedBook), tool + " should reject enchanted books");
         helper.assertFalse(
            repairRecipe.matches(CraftingInput.of(2, 1, List.of(first, second)), helper.getLevel()),
            tool + " should not combine through the vanilla repair recipe"
         );
      }

      ItemStack tracker = new ItemStack((Item)EngineerToolsModule.TRACKER.get());
      helper.assertFalse(tracker.isRepairable(), "tracker should preserve the original setNoRepair contract");
      helper.assertFalse(tracker.getItem().isEnchantable(tracker), "tracker should reject enchanting-table use");
      helper.assertFalse(tracker.getItem().isBookEnchantable(tracker, enchantedBook), "tracker should reject enchanted books");
      helper.succeed();
   }

   private static UseOnContext context(GameTestHelper helper, Player player, ItemStack stack, BlockPos localPos, Direction face) {
      return context(helper, player, stack, localPos, face, 0.5, 0.5, 0.5);
   }

   private static UseOnContext context(
      GameTestHelper helper, Player player, ItemStack stack, BlockPos localPos, Direction face, double hitX, double hitY, double hitZ
   ) {
      BlockPos absolutePos = helper.absolutePos(localPos);
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      Vec3 hit = new Vec3(absolutePos.getX() + hitX, absolutePos.getY() + hitY, absolutePos.getZ() + hitZ);
      return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, absolutePos, false));
   }

   private static int countItem(Inventory inventory, Item item) {
      int count = 0;

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.is(item)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private static int invokeRediaCurve(String methodName, ItemStack stack) {
      try {
         Method method = RediaToolItem.class.getDeclaredMethod(methodName, ItemStack.class);
         method.setAccessible(true);
         return (Integer)method.invoke(null, stack);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Could not invoke REDIA durability curve helper " + methodName, exception);
      }
   }

   private static void invokeRediaInitialCraftingDamage(ItemStack stack, int durabilityPercent) {
      try {
         Method method = RediaToolItem.class.getDeclaredMethod("applyInitialCraftingDamage", ItemStack.class, int.class);
         method.setAccessible(true);
         method.invoke(null, stack, durabilityPercent);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Could not invoke REDIA initial durability helper", exception);
      }
   }

   private static void assertMusliSeedAccepted(GameTestHelper helper, Item seed) {
      helper.assertTrue(new ItemStack(seed).is(MusliBarPressItem.ACCEPTED_SEEDS), seed + " should be in the Muslee Press accepted seed tag");
   }

   private static void fillMainInventory(Inventory inventory, Item item) {
      int maxStackSize = item.getDefaultInstance().getMaxStackSize();

      for (int i = 0; i < inventory.items.size(); i++) {
         inventory.items.set(i, new ItemStack(item, maxStackSize));
      }
   }

   private static void assertRecipe(GameTestHelper helper, String name) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", name);
      helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isPresent(), "missing recipe " + id);
   }

   private static void assertRecipeResult(GameTestHelper helper, String name, String expectedNamespace, String expectedPath, int expectedCount) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", name);
      var recipe = helper.getLevel().getRecipeManager().byKey(id);
      helper.assertTrue(recipe.isPresent(), "missing recipe " + id);
      ItemStack result = recipe.get().value().getResultItem(helper.getLevel().registryAccess());
      ResourceLocation expectedItem = ResourceLocation.fromNamespaceAndPath(expectedNamespace, expectedPath);
      helper.assertValueEqual(expectedItem, BuiltInRegistries.ITEM.getKey(result.getItem()), "wrong output item for recipe " + id);
      helper.assertValueEqual(expectedCount, result.getCount(), "wrong output count for recipe " + id);
   }
}
