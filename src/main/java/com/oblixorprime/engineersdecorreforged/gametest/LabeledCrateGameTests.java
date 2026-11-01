package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.menu.LabeledCrateMenu;
import com.oblixorprime.engineersdecorreforged.network.LabeledCrateQuickMovePayload;
import com.oblixorprime.engineersdecorreforged.utility.LabeledCrateItemRules;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class LabeledCrateGameTests {
   private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

   private LabeledCrateGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_exposes_original_slots_and_automation_boundary(GameTestHelper helper) {
      MachineBlockEntity crate = placeCrate(helper, TEST_POS);
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      AbstractContainerMenu opened = helper.getBlockState(TEST_POS).getMenuProvider(
         helper.getLevel(), helper.absolutePos(TEST_POS)
      ).createMenu(1, player.getInventory(), player);
      helper.assertTrue(opened instanceof LabeledCrateMenu, "crate block should create its dedicated 55-slot menu");
      LabeledCrateMenu menu = (LabeledCrateMenu)opened;
      IItemHandler handler = crate.itemHandler(null);
      helper.assertValueEqual(crate.getContainerSize(), 55, "crate backing store should include 54 storage slots and one frame slot");
      helper.assertValueEqual(handler.getSlots(), 54, "item capability should expose storage slots only");
      helper.assertValueEqual(crate.getSlotsForFace(Direction.UP).length, 54, "sided automation should expose exactly 54 slots");
      helper.assertTrue(!crate.canPlaceItemThroughFace(54, new ItemStack(Items.DIRT), Direction.UP), "automation must not insert into the hint slot");
      helper.assertTrue(!crate.canTakeItemThroughFace(54, new ItemStack(Items.DIRT), Direction.UP), "automation must not extract the hint slot");
      helper.assertTrue(crate.canPlaceItem(53, new ItemStack(Items.DIRT)), "last storage slot should remain accessible");
      helper.assertTrue(!crate.canPlaceItem(54, new ItemStack(Items.DIRT)), "frame slot should not be part of storage input");
      helper.assertValueEqual(menu.slots.size(), 91, "crate menu should include 55 crate slots and 36 player slots");
      helper.assertValueEqual(menu.getSlot(0).x, 28, "first storage slot should match original x");
      helper.assertValueEqual(menu.getSlot(0).y, 10, "first storage slot should match original y");
      helper.assertValueEqual(menu.getSlot(54).x, 191, "hint slot should match original x");
      helper.assertValueEqual(menu.getSlot(54).y, 100, "hint slot should match original y");
      helper.assertValueEqual(menu.getSlot(54).getMaxStackSize(), 1, "hint slot should limit stacks to one");
      helper.assertValueEqual(menu.getSlot(55).y, 125, "first player inventory row should match original y");
      helper.assertValueEqual(menu.getSlot(82).y, 183, "hotbar should follow the main inventory rows");
      helper.assertTrue(menu.getSlot(54).mayPlace(new ItemStack(Items.DIAMOND)), "hint slot should accept a safe item");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_migrates_27_items_and_preserves_legacy_labels(GameTestHelper helper) {
      BlockState state = ModBlocks.LABELED_CRATE.get().defaultBlockState();
      NonNullList<ItemStack> oldItems = NonNullList.withSize(27, ItemStack.EMPTY);
      oldItems.set(0, new ItemStack(Items.IRON_INGOT, 5));
      oldItems.set(26, new ItemStack(Items.REDSTONE, 7));
      CompoundTag legacy = new CompoundTag();
      ContainerHelper.saveAllItems(legacy, oldItems, helper.getLevel().registryAccess());
      legacy.putString("LabelLine0", "Old stock label");
      legacy.putString("LabelLine1", "Second line");

      MachineBlockEntity migrated = new MachineBlockEntity(helper.absolutePos(TEST_POS), state);
      migrated.loadWithComponents(legacy, helper.getLevel().registryAccess());
      helper.assertValueEqual(migrated.getContainerSize(), 55, "legacy crate NBT should load into the expanded inventory");
      helper.assertValueEqual(migrated.getItem(0).getCount(), 5, "legacy slot 0 should remain intact");
      helper.assertValueEqual(migrated.getItem(26).getCount(), 7, "legacy slot 26 should remain intact");
      helper.assertTrue(migrated.getItem(54).isEmpty(), "old crate NBT should not invent hint-slot contents");
      helper.assertValueEqual(migrated.labelLine(0), "Old stock label", "legacy first label should be preserved");
      helper.assertValueEqual(migrated.labelLine(1), "Second line", "legacy second label should be preserved");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_rejects_nested_containers_safely(GameTestHelper helper) {
      MachineBlockEntity crate = placeCrate(helper, TEST_POS);
      IItemHandler handler = crate.itemHandler(null);
      ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
      helper.assertTrue(!LabeledCrateItemRules.canStore(shulker), "crate should reject shulker boxes");

      ItemStack filledContainer = new ItemStack(Items.CHEST);
      filledContainer.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
      helper.assertTrue(!LabeledCrateItemRules.canStore(filledContainer), "crate should reject nonempty modern container components");

      CompoundTag blockEntityData = new CompoundTag();
      ListTag storedItems = new ListTag();
      storedItems.add(new CompoundTag());
      blockEntityData.put("Items", storedItems);
      ItemStack legacyContainer = new ItemStack(Items.CHEST);
      legacyContainer.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityData));
      helper.assertTrue(!LabeledCrateItemRules.canStore(legacyContainer), "crate should reject nested block-entity inventories");

      helper.assertTrue(!handler.insertItem(0, filledContainer, false).isEmpty(), "automation should reject nested container contents");
      helper.assertTrue(crate.getItem(0).isEmpty(), "rejected nested item should not enter the crate");
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      LabeledCrateMenu menu = new LabeledCrateMenu(2, player.getInventory(), crate);
      helper.assertTrue(!menu.getSlot(0).mayPlace(legacyContainer), "the player menu should share nested-container protection");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_shift_click_and_ctrl_shift_click_preserve_components(GameTestHelper helper) {
      MachineBlockEntity crate = placeCrate(helper, TEST_POS);
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      LabeledCrateMenu menu = new LabeledCrateMenu(3, player.getInventory(), crate);
      CompoundTag gradeData = new CompoundTag();
      gradeData.putString("grade", "A");
      ItemStack first = new ItemStack(Items.IRON_INGOT, 16);
      first.set(DataComponents.CUSTOM_DATA, CustomData.of(gradeData));
      ItemStack second = new ItemStack(Items.IRON_INGOT, 5);
      second.set(DataComponents.CUSTOM_DATA, CustomData.of(gradeData));
      CompoundTag otherGrade = new CompoundTag();
      otherGrade.putString("grade", "B");
      ItemStack distinct = new ItemStack(Items.IRON_INGOT, 9);
      distinct.set(DataComponents.CUSTOM_DATA, CustomData.of(otherGrade));
      player.getInventory().setItem(9, first);
      player.getInventory().setItem(10, second);
      player.getInventory().setItem(11, distinct);

      helper.assertTrue(menu.quickMoveAll(player, 55), "Ctrl+Shift should move matching stacks from the selected player slot");
      helper.assertValueEqual(crate.getItem(0).getCount(), 21, "matching stacks with identical components should combine");
      helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "first matching player stack should move");
      helper.assertTrue(player.getInventory().getItem(10).isEmpty(), "second matching player stack should move");
      helper.assertValueEqual(player.getInventory().getItem(11).getCount(), 9, "same item with different components should stay behind");

      crate.setItem(1, new ItemStack(Items.DIRT, 4));
      ItemStack shifted = menu.quickMoveStack(player, 1);
      helper.assertTrue(shifted.is(Items.DIRT), "ordinary shift-click should transfer a stored stack");
      helper.assertTrue(player.getInventory().getItem(8).is(Items.DIRT), "ordinary shift-click should reach the player's hotbar");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void labeled_crate_survival_break_replaces_contents_with_named_crate_and_reloads(GameTestHelper helper) {
      MachineBlockEntity crate = placeNamedCrate(helper, TEST_POS, Component.literal("Named Crate"));
      FakePlayer player = fakePlayer(helper, GameType.SURVIVAL, TEST_POS);
      helper.assertTrue(!player.getAbilities().instabuild, "survival test player must not use creative break handling");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      ItemStack preserved = new ItemStack(Items.DIAMOND, 3);
      CompoundTag component = new CompoundTag();
      component.putString("source", "kept");
      preserved.set(DataComponents.CUSTOM_DATA, CustomData.of(component));
      crate.setItem(26, preserved);
      crate.setItem(54, new ItemStack(Items.COMPASS));
      crate.setLabeledCrateLabel("Old label", "Second line");
      helper.assertTrue(!crate.saveWithoutMetadata(helper.getLevel().registryAccess()).getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty(),
         "crate should serialize its contents before a normal break");
      BlockPos absolutePos = helper.absolutePos(TEST_POS);

      helper.assertTrue(player.gameMode.destroyBlock(absolutePos), "survival should break the labeled crate");
      List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolutePos).inflate(4.0));
      List<ItemStack> crateDrops = drops.stream().map(ItemEntity::getItem)
         .filter(stack -> stack.is(ModBlocks.LABELED_CRATE.get().asItem())).toList();
      helper.assertValueEqual(crateDrops.size(), 1, "normal break should produce exactly one crate drop");
      ItemStack droppedCrate = crateDrops.get(0);
      CompoundTag storedData = droppedCrate.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
      helper.assertTrue(storedData.contains("Items"), "normal crate drop should retain the inventory data");
      helper.assertTrue(!storedData.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty(),
         "normal crate drop should preserve the stored stacks");
      helper.assertValueEqual(storedData.getString("LabelLine0"), "Old label", "normal crate drop should retain the old label");
      helper.assertTrue(droppedCrate.has(DataComponents.CUSTOM_NAME), "normal crate drop should retain the crate name");

      BlockPos replacementPos = TEST_POS.east(3);
      BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(replacementPos)), Direction.UP, helper.absolutePos(replacementPos), false);
      player.setItemInHand(InteractionHand.MAIN_HAND, droppedCrate.copy());
      InteractionResult result = ((BlockItem)droppedCrate.getItem()).place(
         new net.minecraft.world.item.context.BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, droppedCrate.copy(), hit)
      );
      helper.assertTrue(result.consumesAction(), "the saved crate item should place again");
      BlockEntity placedEntity = helper.getLevel().getBlockEntity(helper.absolutePos(replacementPos));
      helper.assertTrue(placedEntity instanceof MachineBlockEntity, "placed crate item should restore its machine block entity");
      MachineBlockEntity restored = (MachineBlockEntity)placedEntity;
      helper.assertValueEqual(restored.getItem(26).getCount(), 3, "placed crate should reload the stored item");
      helper.assertValueEqual(restored.getItem(26).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("source"), "kept",
         "stored item components should survive break and place");
      helper.assertTrue(restored.getItem(54).is(Items.COMPASS), "placed crate should restore its hint item");
      helper.assertValueEqual(restored.labelLine(0), "Old label", "placed crate should restore the legacy label");
      helper.assertTrue(restored.getCustomName() != null, "placed crate should restore its custom name");
      helper.assertValueEqual(restored.getCustomName().getString(), "Named Crate", "placed crate should restore the original custom name");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void labeled_crate_creative_break_discards_contents(GameTestHelper helper) {
      MachineBlockEntity crate = placeCrate(helper, TEST_POS);
      FakePlayer player = fakePlayer(helper, GameType.CREATIVE, TEST_POS);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      crate.setItem(0, new ItemStack(Items.DIAMOND, 4));
      crate.setItem(54, new ItemStack(Items.COMPASS));
      BlockPos absolutePos = helper.absolutePos(TEST_POS);

      helper.assertTrue(player.gameMode.destroyBlock(absolutePos), "creative should remove the crate");
      helper.assertTrue(helper.getLevel().getBlockState(absolutePos).isAir(), "creative break should remove the block");
      helper.assertTrue(crate.getItem(0).isEmpty() && crate.getItem(54).isEmpty(), "creative break should not spill crate contents");
      helper.assertTrue(
         helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolutePos).inflate(4.0)).isEmpty(),
         "creative break should not create crate or contents drops"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_explosion_releases_items_without_crate_drop(GameTestHelper helper) {
      MachineBlockEntity crate = placeCrate(helper, TEST_POS);
      crate.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
      crate.setItem(54, new ItemStack(Items.COMPASS));
      BlockState state = crate.getBlockState();
      LootParams.Builder params = new LootParams.Builder(helper.getLevel())
         .withParameter(LootContextParams.BLOCK_ENTITY, crate)
         .withParameter(LootContextParams.EXPLOSION_RADIUS, 1.0F);
      List<ItemStack> drops = ModBlocks.LABELED_CRATE.get().getDrops(state, params);
      helper.assertValueEqual(drops.size(), 2, "explosion should release storage and hint items");
      helper.assertTrue(drops.stream().noneMatch(stack -> stack.is(ModBlocks.LABELED_CRATE.get().asItem())),
         "explosion should not drop the crate block");
      helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(Items.IRON_INGOT) && stack.getCount() == 4),
         "explosion should release stored item stacks intact");
      helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(Items.COMPASS)),
         "explosion should release the hint item too");
      helper.assertTrue(crate.getItem(0).isEmpty() && crate.getItem(54).isEmpty(), "explosion extraction should clear contents once");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_quick_move_payload_round_trips_container_and_slot(GameTestHelper helper) {
      LabeledCrateQuickMovePayload payload = new LabeledCrateQuickMovePayload(12, 64);
      RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
      try {
         LabeledCrateQuickMovePayload.STREAM_CODEC.encode(buffer, payload);
         LabeledCrateQuickMovePayload decoded = LabeledCrateQuickMovePayload.STREAM_CODEC.decode(buffer);
      helper.assertValueEqual(decoded.containerId(), 12, "quick-move packet should retain the open container id");
      helper.assertValueEqual(decoded.slotId(), 64, "quick-move packet should retain the selected slot");
      } finally {
         buffer.release();
      }
      helper.succeed();
   }

   private static MachineBlockEntity placeNamedCrate(GameTestHelper helper, BlockPos position, Component name) {
      BlockPos absolutePos = helper.absolutePos(position);
      FakePlayer player = fakePlayer(helper, GameType.CREATIVE, position);
      ItemStack stack = new ItemStack(ModBlocks.LABELED_CRATE.get());
      stack.set(DataComponents.CUSTOM_NAME, name);
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
      InteractionResult result = ((BlockItem)stack.getItem()).place(
         new net.minecraft.world.item.context.BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit)
      );
      if (!result.consumesAction()) {
         throw new AssertionError("Named labeled crate placement failed");
      }
      BlockEntity entity = helper.getLevel().getBlockEntity(absolutePos);
      if (!(entity instanceof MachineBlockEntity machine)) {
         throw new AssertionError("Named labeled crate did not create its machine block entity");
      }
      return machine;
   }

   private static FakePlayer fakePlayer(GameTestHelper helper, GameType gameType, BlockPos position) {
      BlockPos absolutePos = helper.absolutePos(position);
      FakePlayer player = new FakePlayer(
         helper.getLevel(), new GameProfile(UUID.randomUUID(), "LabeledCrateGameTest")
      );
      player.gameMode.changeGameModeForPlayer(gameType);
      player.setPos(absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 1.5);
      return player;
   }

   private static MachineBlockEntity placeCrate(GameTestHelper helper, BlockPos position) {
      helper.setBlock(position, ModBlocks.LABELED_CRATE.get().defaultBlockState());
      BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(position));
      if (!(entity instanceof MachineBlockEntity machine)) {
         throw new AssertionError("Labeled crate did not create its machine block entity");
      }
      return machine;
   }
}
