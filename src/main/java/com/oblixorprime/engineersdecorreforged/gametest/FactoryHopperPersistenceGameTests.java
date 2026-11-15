package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FactoryHopperPersistenceGameTests {
   private static final BlockPos POS = new BlockPos(2, 2, 2);

   @GameTest(template="empty")
   public static void hopper_loot_preview_is_pure_and_generic_break_encapsulates_once(GameTestHelper h) {
      MachineBlockEntity m = place(h);
      m.setItem(17, new ItemStack(Items.DIAMOND, 3));
      m.setItem(26, new ItemStack(Items.EMERALD, 2));
      for (int i=0;i<2;i++) {
         var drops = ModBlocks.FACTORY_HOPPER.get().getDrops(m.getBlockState(), new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.BLOCK_ENTITY,m));
         h.assertValueEqual(1,drops.size(),"one capsule preview");
         h.assertTrue(drops.get(0).has(DataComponents.CUSTOM_DATA),"nonempty preview has portable state");
         h.assertValueEqual(3,m.getItem(17).getCount(),"preview must not drain or mutate");
      }
      h.assertTrue(h.getLevel().destroyBlock(h.absolutePos(POS),true),"generic break succeeds");
      var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2));
      h.assertValueEqual(1,drops.size(),"one capsule, no loose duplicate");
      ItemStack capsule=drops.get(0).getItem();
      h.assertTrue(capsule.is(ModBlocks.FACTORY_HOPPER.get().asItem()),"drop is hopper");
      MachineBlockEntity restored=place(h);
      ModBlocks.FACTORY_HOPPER.get().setPlacedBy(h.getLevel(),h.absolutePos(POS),restored.getBlockState(),null,capsule);
      h.assertValueEqual(3,restored.getItem(17).getCount(),"slot17 survives placement");
      h.assertValueEqual(2,restored.getItem(26).getCount(),"legacy backing slot26 survives without becoming an automation slot");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void hopper_survival_break_preserves_settings_but_empty_break_does_not(GameTestHelper h) {
      FakePlayer p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"HopperFidelity"));
      p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
      MachineBlockEntity m=place(h);
      m.setItem(0,new ItemStack(Items.IRON_INGOT,7));
      m.handleGuiAction(p,1,4,0);m.handleGuiAction(p,2,17,0);m.handleGuiAction(p,3,1,0);m.handleGuiAction(p,4,99,0);
      h.assertTrue(p.gameMode.destroyBlock(h.absolutePos(POS)),"survival break succeeds");
      var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2));
      h.assertValueEqual(1,drops.size(),"survival lifecycle preserves one capsule");
      ItemStack capsule=drops.get(0).getItem();
      m=place(h);
      ModBlocks.FACTORY_HOPPER.get().setPlacedBy(h.getLevel(),h.absolutePos(POS),m.getBlockState(),p,capsule);
      CompoundTag saved=m.saveWithoutMetadata(h.getLevel().registryAccess());
      h.assertValueEqual(7,m.getItem(0).getCount(),"contents restored");
      h.assertValueEqual(4,saved.getInt("HopperRange"),"range restored");
      h.assertValueEqual(17,saved.getInt("HopperTransferCount"),"count restored");
      h.assertValueEqual(1,saved.getInt("HopperLogic"),"logic restored");
      h.assertValueEqual(99,saved.getInt("HopperPeriod"),"period restored");
      h.assertTrue(!saved.getBoolean("PreviousPowered")&&!saved.contains("Cooldown")&&!saved.contains("ManualTrigger"),"runtime state not carried");
      m.clearContent();
      var empty=ModBlocks.FACTORY_HOPPER.get().getDrops(m.getBlockState(),new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.BLOCK_ENTITY,m));
      h.assertTrue(!empty.get(0).has(DataComponents.CUSTOM_DATA),"original empty hopper loses configured settings on pickup");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void hopper_explosion_preview_contents_only_and_runtime_reload_resets_timers(GameTestHelper h) {
      MachineBlockEntity m=place(h);m.setItem(0,new ItemStack(Items.DIAMOND,5));
      var drops=ModBlocks.FACTORY_HOPPER.get().getDrops(m.getBlockState(),new LootParams.Builder(h.getLevel())
         .withParameter(LootContextParams.BLOCK_ENTITY,m).withParameter(LootContextParams.EXPLOSION_RADIUS,4.0F));
      h.assertValueEqual(1,drops.size(),"contents only");h.assertTrue(drops.get(0).is(Items.DIAMOND),"no hopper item in explosion loot");
      m.explodeHopperContents();m.explodeHopperContents();
      h.setBlock(POS,Blocks.AIR);
      var worldDrops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2));
      h.assertValueEqual(5,worldDrops.stream().mapToInt(e->e.getItem().getCount()).sum(),"explosion/removal emits contents once");
      m=place(h);m.setItem(0,new ItemStack(Items.IRON_INGOT,2));
      CompoundTag saved=m.saveWithoutMetadata(h.getLevel().registryAccess());
      saved.putInt("Cooldown",200);saved.putBoolean("ManualTrigger",true);saved.putBoolean("RedstoneUpdated",true);
      m.loadWithComponents(saved,h.getLevel().registryAccess());
      CompoundTag roundTrip=m.saveWithoutMetadata(h.getLevel().registryAccess());
      h.assertValueEqual(2,m.getItem(0).getCount(),"old saves retain contents");
      h.assertTrue(!roundTrip.contains("Cooldown")&&!roundTrip.contains("ManualTrigger")&&!roundTrip.contains("RedstoneUpdated"),"transient fields reset on load");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void hopper_comparator_uses18_active_slots_and_creative_drops_nothing(GameTestHelper h) {
      MachineBlockEntity m=place(h);
      m.setItem(26,new ItemStack(Items.DIAMOND,64));
      h.assertValueEqual(0,m.comparatorOutput(),"legacy hidden slot is not active fullness");
      m.setItem(0,new ItemStack(Items.DIRT,64));
      h.assertValueEqual(1,m.comparatorOutput(),"one active full stack");
      for(int i=1;i<18;i++)m.setItem(i,new ItemStack(Items.DIRT,64));
      h.assertValueEqual(15,m.comparatorOutput(),"18 full active slots signal15");
      h.assertValueEqual(18,m.getSlotsForFace(net.minecraft.core.Direction.UP).length,"automation cannot expose hidden legacy slots");
      FakePlayer p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"HopperCreative"));
      p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
      h.assertTrue(p.gameMode.destroyBlock(h.absolutePos(POS)),"creative removes hopper");
      h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2)).isEmpty(),"creative drops no capsule or loose inventory");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void hopper_explosion_with_drops_disabled_clears_without_spill(GameTestHelper h) {
      var rule=h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
      boolean old=rule.get();
      try {
         rule.set(false,h.getLevel().getServer());
         MachineBlockEntity m=place(h);m.setItem(0,new ItemStack(Items.DIAMOND,5));
         m.explodeHopperContents();h.setBlock(POS,Blocks.AIR);
         h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2)).isEmpty(),"disabled drops do not spill explosion items");
      } finally { rule.set(old,h.getLevel().getServer()); }
      h.succeed();
   }

   private static MachineBlockEntity place(GameTestHelper h) {
      h.setBlock(POS,Blocks.AIR);h.setBlock(POS,ModBlocks.FACTORY_HOPPER.get());
      return (MachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
   }
}
