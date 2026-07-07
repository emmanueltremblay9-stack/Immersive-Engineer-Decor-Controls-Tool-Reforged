package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MaterialBoxItem;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressItem;
import com.oblixorprime.engineersdecorreforged.tools.RediaToolItem;
import com.oblixorprime.engineersdecorreforged.tools.RediaToolRepairRecipe;
import com.oblixorprime.engineersdecorreforged.tools.TooltipItem;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
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
   public static void redia_tool_rejects_blocked_air_sensitive_ground_states(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setShiftKeyDown(true);
      ItemStack redia = new ItemStack((ItemLike)EngineerToolsModule.REDIA_TOOL.get());
      helper.setBlock(TEST_POS, Blocks.DIRT);
      helper.setBlock(TEST_POS.above(), Blocks.STONE);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.DIRT), "REDIA Tool should not till dirt when the block above is occupied");
      helper.assertValueEqual(0, redia.getDamageValue(), "blocked REDIA tilling should not damage the tool");

      helper.setBlock(TEST_POS, Blocks.COARSE_DIRT);
      helper.setBlock(TEST_POS.above(), Blocks.STONE);
      ((Item)EngineerToolsModule.REDIA_TOOL.get()).useOn(context(helper, player, redia, TEST_POS, Direction.UP));
      helper.assertTrue(helper.getBlockState(TEST_POS).is(Blocks.COARSE_DIRT), "REDIA Tool should not flatten coarse dirt when the block above is occupied");
      helper.assertValueEqual(0, redia.getDamageValue(), "blocked REDIA flattening should not damage the tool");
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
         player.getFoodData().getExhaustionLevel() > exhaustionBefore, "REDIA Tool tree felling should apply the original hunger exhaustion cost"
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
   public static void ariadne_coal_places_route_marker(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack marker = new ItemStack((ItemLike)EngineerToolsModule.ARIADNE_COAL.get());
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.north(), Blocks.AIR);
      helper.assertValueEqual(1, marker.getMaxStackSize(), "Ariadne Coal should use the original single-item stack size");
      helper.assertValueEqual(100, marker.getMaxDamage(), "Ariadne Coal should use the original 100-use durability");
      ((Item)EngineerToolsModule.ARIADNE_COAL.get()).useOn(context(helper, player, marker, TEST_POS, Direction.NORTH, 0.95, 0.95, 0.0));
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
      ((Item)EngineerToolsModule.ARIADNE_COAL.get()).useOn(context(helper, player, marker, TEST_POS, Direction.NORTH));
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
      ((Item)EngineerToolsModule.ARIADNE_COAL.get()).useOn(context(helper, player, marker, TEST_POS, Direction.NORTH));
      helper.assertTrue(helper.getBlockState(TEST_POS.north()).is((Block)ModBlocks.ARIADNE_MARKER.get()), "final Ariadne Coal use should still place a marker");
      helper.assertTrue(player.getMainHandItem().isEmpty(), "Ariadne Coal should break after the original 100th use");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void ariadne_coal_creative_placement_does_not_wear_or_break(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.getAbilities().instabuild = true;
      ItemStack marker = new ItemStack((ItemLike)EngineerToolsModule.ARIADNE_COAL.get());
      marker.setDamageValue(99);
      player.setItemInHand(InteractionHand.MAIN_HAND, marker);
      helper.setBlock(TEST_POS, Blocks.STONE);
      helper.setBlock(TEST_POS.north(), Blocks.AIR);
      ((Item)EngineerToolsModule.ARIADNE_COAL.get()).useOn(context(helper, player, marker, TEST_POS, Direction.NORTH));
      helper.assertTrue(helper.getBlockState(TEST_POS.north()).is((Block)ModBlocks.ARIADNE_MARKER.get()), "creative Ariadne Coal use should still place a marker");
      helper.assertTrue(player.getMainHandItem().is((Item)EngineerToolsModule.ARIADNE_COAL.get()), "creative Ariadne Coal use should not break the item");
      helper.assertValueEqual(99, player.getMainHandItem().getDamageValue(), "creative Ariadne Coal use should not wear the item");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sleeping_bag_skips_night_without_setting_spawn(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack bag = new ItemStack((ItemLike)EngineerToolsModule.SLEEPING_BAG.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, bag);
      helper.getLevel().setDayTime(13000L);
      bag.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(0L, Math.floorMod(helper.getLevel().getDayTime(), 24000L), "sleeping bag should advance night to morning");
      helper.assertValueEqual(1, bag.getDamageValue(), "sleeping bag should wear after a successful rest");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sleeping_bag_rejects_daytime_without_damage(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack bag = new ItemStack((ItemLike)EngineerToolsModule.SLEEPING_BAG.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, bag);
      helper.getLevel().setDayTime(1000L);
      bag.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(0, bag.getDamageValue(), "daytime sleeping bag use should not damage the item");
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
      stimpack.getItem().inventoryTick(stimpack, helper.getLevel(), player, 0, false);
      helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "stimpack should apply original regeneration");
      helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "stimpack should apply original movement speed");
      helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "stimpack should apply original damage resistance");
      helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "stimpack should apply original fire resistance");
      helper.assertValueEqual(1, stimpack.getDamageValue(), "automatic stimpack injection should consume one use");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void diving_capsule_uses_original_air_trigger_and_refill_fraction(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack capsule = new ItemStack((ItemLike)EngineerToolsModule.DIVING_CAPSULE.get());
      int maxAir = player.getMaxAirSupply();
      int startingAir = maxAir * 8 / 30;
      int expectedAir = Math.min(maxAir, startingAir + maxAir * 6 / 10);
      player.setAirSupply(startingAir);
      capsule.getItem().inventoryTick(capsule, helper.getLevel(), player, 0, false);
      helper.assertValueEqual(expectedAir, player.getAirSupply(), "diving capsule should trigger below 30 percent air and restore 60 percent of max air");
      helper.assertValueEqual(1, capsule.getDamageValue(), "automatic diving capsule refill should consume one use");
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
   public static void material_box_stores_and_retrieves_one_material_stack(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, box);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 64));
      box.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(64, MaterialBoxItem.storedCount(box), "material box should store the offhand stack");
      helper.assertTrue(player.getOffhandItem().isEmpty(), "material box should empty the offhand source stack");
      box.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(0, MaterialBoxItem.storedCount(box), "material box should reduce stored count after retrieval");
      helper.assertTrue(player.getOffhandItem().is(Items.COBBLESTONE), "material box should retrieve the stored material to the offhand");
      helper.assertValueEqual(64, player.getOffhandItem().getCount(), "material box should retrieve one full stack");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_rejects_mixed_materials_without_deleting_items(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, box);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 16));
      box.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT, 16));
      box.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(16, MaterialBoxItem.storedCount(box), "material box should keep existing stored material after mismatch");
      helper.assertTrue(player.getOffhandItem().is(Items.DIRT), "mismatched offhand stack should remain untouched");
      helper.assertValueEqual(16, player.getOffhandItem().getCount(), "mismatched offhand count should remain untouched");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_rejects_modified_stacks_without_deleting_items(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      ItemStack namedCobble = new ItemStack(Items.COBBLESTONE, 16);
      namedCobble.set(DataComponents.CUSTOM_NAME, Component.literal("Named Cobble"));
      player.setItemInHand(InteractionHand.MAIN_HAND, box);
      player.setItemInHand(InteractionHand.OFF_HAND, namedCobble);
      box.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(0, MaterialBoxItem.storedCount(box), "material box should not store modified stacks");
      helper.assertTrue(player.getOffhandItem().is(Items.COBBLESTONE), "rejected modified stack should remain in the offhand");
      helper.assertValueEqual(16, player.getOffhandItem().getCount(), "rejected modified stack count should remain untouched");
      helper.assertTrue(player.getOffhandItem().has(DataComponents.CUSTOM_NAME), "rejected modified stack should keep its custom data");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_clamps_tampered_stored_count_to_capacity(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      CompoundTag tag = new CompoundTag();
      tag.putString("stored_item", "minecraft:cobblestone");
      tag.putInt("stored_count", MaterialBoxItem.CAPACITY + 100);
      box.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      player.setItemInHand(InteractionHand.MAIN_HAND, box);
      helper.assertValueEqual(MaterialBoxItem.CAPACITY, MaterialBoxItem.storedCount(box), "material box should clamp malformed stored counts to capacity");
      box.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(player.getOffhandItem().is(Items.COBBLESTONE), "material box should retrieve the clamped stored material");
      helper.assertValueEqual(64, player.getOffhandItem().getCount(), "material box should retrieve one normal stack from clamped storage");
      helper.assertValueEqual(
         MaterialBoxItem.CAPACITY - 64, MaterialBoxItem.storedCount(box), "material box should keep the remaining count within capacity after retrieval"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_recovers_from_invalid_stored_item_data(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack box = new ItemStack((ItemLike)EngineerToolsModule.MATERIAL_BOX.get());
      CompoundTag tag = new CompoundTag();
      tag.putString("stored_item", "immersive_engineer_decor_controls_tool_reforged:missing_material");
      tag.putInt("stored_count", 48);
      box.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      player.setItemInHand(InteractionHand.MAIN_HAND, box);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 12));
      helper.assertValueEqual(0, MaterialBoxItem.storedCount(box), "material box should ignore invalid stored item ids");
      box.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(12, MaterialBoxItem.storedCount(box), "material box should reuse invalid saved data as empty storage");
      helper.assertTrue(MaterialBoxItem.storedItem(box) == Items.COBBLESTONE, "material box should replace invalid saved item data with the new material");
      helper.assertTrue(player.getOffhandItem().isEmpty(), "material box should store the offhand stack after recovering from invalid data");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_converts_ingredients_atomically(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      player.getInventory().add(new ItemStack(Items.BREAD));
      player.getInventory().add(new ItemStack(Items.APPLE));
      player.getInventory().add(new ItemStack(Items.WHEAT_SEEDS));
      press.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(4, countItem(player.getInventory(), (Item)EngineerToolsModule.MUSLI_BAR.get()), "press should create four Muslee bars");
      helper.assertValueEqual(0, countItem(player.getInventory(), Items.BREAD), "press should consume bread on success");
      helper.assertValueEqual(0, countItem(player.getInventory(), Items.APPLE), "press should consume apple on success");
      helper.assertValueEqual(0, countItem(player.getInventory(), Items.WHEAT_SEEDS), "press should consume seeds on success");
      helper.assertValueEqual(1, press.getDamageValue(), "press should wear after a successful batch");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_accepts_original_seed_variants(GameTestHelper helper) {
      assertMusliSeedAccepted(helper, Items.MELON_SEEDS);
      assertMusliSeedAccepted(helper, Items.PUMPKIN_SEEDS);
      assertMusliSeedAccepted(helper, Items.BEETROOT_SEEDS);
      assertMusliSeedAccepted(helper, (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "seed")));
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_missing_ingredient_consumes_nothing(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      player.getInventory().add(new ItemStack(Items.BREAD));
      player.getInventory().add(new ItemStack(Items.APPLE));
      press.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(0, countItem(player.getInventory(), (Item)EngineerToolsModule.MUSLI_BAR.get()), "failed press should not create bars");
      helper.assertValueEqual(1, countItem(player.getInventory(), Items.BREAD), "failed press should keep bread");
      helper.assertValueEqual(1, countItem(player.getInventory(), Items.APPLE), "failed press should keep apple");
      helper.assertValueEqual(0, press.getDamageValue(), "failed press should not wear");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_uses_space_freed_by_consumed_ingredients(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      Inventory inventory = player.getInventory();
      fillMainInventory(inventory, Items.COBBLESTONE);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      inventory.selected = 0;
      inventory.items.set(0, press);
      inventory.items.set(1, new ItemStack(Items.BREAD));
      inventory.items.set(2, new ItemStack(Items.APPLE));
      inventory.items.set(3, new ItemStack(Items.WHEAT_SEEDS));
      press.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(4, countItem(inventory, (Item)EngineerToolsModule.MUSLI_BAR.get()), "press should use slots freed by consumed ingredients");
      helper.assertValueEqual(0, countItem(inventory, Items.BREAD), "press should consume the single bread stack");
      helper.assertValueEqual(0, countItem(inventory, Items.APPLE), "press should consume the single apple stack");
      helper.assertValueEqual(0, countItem(inventory, Items.WHEAT_SEEDS), "press should consume the single seed stack");
      helper.assertValueEqual(1, press.getDamageValue(), "press should wear after crafting into freed ingredient space");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_fills_split_existing_bar_stacks(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      Inventory inventory = player.getInventory();
      fillMainInventory(inventory, Items.COBBLESTONE);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      inventory.selected = 0;
      inventory.items.set(0, press);
      inventory.items.set(1, new ItemStack(Items.BREAD, 64));
      inventory.items.set(2, new ItemStack(Items.APPLE, 64));
      inventory.items.set(3, new ItemStack(Items.WHEAT_SEEDS, 64));
      inventory.items.set(4, new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR.get(), 62));
      inventory.items.set(5, new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR.get(), 62));
      press.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(128, countItem(inventory, (Item)EngineerToolsModule.MUSLI_BAR.get()), "press should fill split matching output stacks");
      helper.assertValueEqual(63, ((ItemStack)inventory.items.get(1)).getCount(), "press should consume one bread from a full stack");
      helper.assertValueEqual(63, ((ItemStack)inventory.items.get(2)).getCount(), "press should consume one apple from a full stack");
      helper.assertValueEqual(63, ((ItemStack)inventory.items.get(3)).getCount(), "press should consume one seed from a full stack");
      helper.assertValueEqual(64, ((ItemStack)inventory.items.get(4)).getCount(), "first partial bar stack should be filled");
      helper.assertValueEqual(64, ((ItemStack)inventory.items.get(5)).getCount(), "second partial bar stack should be filled");
      helper.assertValueEqual(1, press.getDamageValue(), "press should wear after crafting into split stacks");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void musli_bar_press_does_not_count_offhand_as_output_space(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      Inventory inventory = player.getInventory();
      fillMainInventory(inventory, Items.COBBLESTONE);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      inventory.selected = 0;
      inventory.items.set(0, press);
      inventory.items.set(1, new ItemStack(Items.BREAD, 64));
      inventory.items.set(2, new ItemStack(Items.APPLE, 64));
      inventory.items.set(3, new ItemStack(Items.WHEAT_SEEDS, 64));
      inventory.offhand.set(0, new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR.get(), 60));
      press.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(60, countItem(inventory, (Item)EngineerToolsModule.MUSLI_BAR.get()), "press should not craft into offhand-only output space");
      helper.assertValueEqual(64, ((ItemStack)inventory.items.get(1)).getCount(), "failed offhand-space press should keep bread");
      helper.assertValueEqual(64, ((ItemStack)inventory.items.get(2)).getCount(), "failed offhand-space press should keep apple");
      helper.assertValueEqual(64, ((ItemStack)inventory.items.get(3)).getCount(), "failed offhand-space press should keep seeds");
      helper.assertValueEqual(0, press.getDamageValue(), "failed offhand-space press should not wear");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void passive_grits_stay_registered_as_recipe_materials(GameTestHelper helper) {
      helper.assertTrue(EngineerToolsModule.IRON_GRIT.get() instanceof TooltipItem, "iron grit should remain a passive material item");
      helper.assertTrue(EngineerToolsModule.GOLD_GRIT.get() instanceof TooltipItem, "gold grit should remain a passive material item");
      assertRecipe(helper, "iron_grit_smelting");
      assertRecipe(helper, "iron_grit_blasting");
      assertRecipe(helper, "gold_grit_smelting");
      assertRecipe(helper, "gold_grit_blasting");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void crushing_hammer_outputs_immersive_engineering_dusts(GameTestHelper helper) {
      assertRecipeResult(helper, "iron_grit_from_iron_ore", "immersiveengineering", "dust_iron", 2);
      assertRecipeResult(helper, "iron_grit_from_deepslate_iron_ore", "immersiveengineering", "dust_iron", 2);
      assertRecipeResult(helper, "iron_grit_from_raw_iron", "immersiveengineering", "dust_iron", 2);
      assertRecipeResult(helper, "gold_grit_from_gold_ore", "immersiveengineering", "dust_gold", 2);
      assertRecipeResult(helper, "gold_grit_from_deepslate_gold_ore", "immersiveengineering", "dust_gold", 2);
      assertRecipeResult(helper, "gold_grit_from_raw_gold", "immersiveengineering", "dust_gold", 2);
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

   private static void assertMusliSeedAccepted(GameTestHelper helper, Item seed) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack press = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR_PRESS.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, press);
      player.getInventory().add(new ItemStack(Items.BREAD));
      player.getInventory().add(new ItemStack(Items.APPLE));
      player.getInventory().add(new ItemStack(seed));
      helper.assertTrue(new ItemStack(seed).is(MusliBarPressItem.ACCEPTED_SEEDS), seed + " should be in the Muslee Press accepted seed tag");
      press.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertValueEqual(4, countItem(player.getInventory(), (Item)EngineerToolsModule.MUSLI_BAR.get()), seed + " should press into four Muslee bars");
      helper.assertValueEqual(0, countItem(player.getInventory(), seed), seed + " should be consumed as the seed ingredient");
      helper.assertValueEqual(1, press.getDamageValue(), "press should wear after using " + seed);
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
