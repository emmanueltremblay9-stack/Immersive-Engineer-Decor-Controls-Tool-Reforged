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
   public static void hopper_real_explosion_preserves_contents_without_block_or_decay(GameTestHelper h) {
      assertRealExplosion(h, true, false);
   }

   @GameTest(template="empty")
   public static void hopper_real_explosion_with_decay_preserves_contents(GameTestHelper h) {
      assertRealExplosion(h, true, true);
   }

   @GameTest(template="empty")
   public static void hopper_real_explosion_with_drops_disabled_emits_nothing(GameTestHelper h) {
      assertRealExplosion(h, false, true);
   }

   private static void assertRealExplosion(GameTestHelper h, boolean dropsEnabled, boolean decay) {
      var rules=h.getLevel().getGameRules();
      var dropsRule=rules.getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
      var decayRule=rules.getRule(net.minecraft.world.level.GameRules.RULE_TNT_EXPLOSION_DROP_DECAY);
      boolean oldDrops=dropsRule.get(), oldDecay=decayRule.get();
      try {
         dropsRule.set(dropsEnabled,h.getLevel().getServer());
         decayRule.set(decay,h.getLevel().getServer());
         MachineBlockEntity m=place(h);
         m.setItem(0,new ItemStack(Items.DIAMOND,5));
         m.setItem(26,new ItemStack(Items.EMERALD,2));
         BlockPos pos=h.absolutePos(POS);
         // Confine the real blast to this test's own cavity; never affect adjacent GameTests.
         for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
            if(x!=0||y!=0||z!=0) h.setBlock(POS.offset(x,y,z),Blocks.BEDROCK);
         CompoundTag configured=m.saveWithoutMetadata(h.getLevel().registryAccess());
         configured.putInt("HopperRange",4);configured.putInt("HopperTransferCount",17);
         configured.putInt("HopperLogic",1);configured.putInt("HopperPeriod",99);
         m.loadWithComponents(configured,h.getLevel().registryAccess());
         // A weak real blast must leave the steel machine intact; resistance is not invulnerability.
         h.getLevel().explode(null,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,
            2.0F,false,net.minecraft.world.level.Level.ExplosionInteraction.TNT);
         h.assertTrue(h.getLevel().getBlockState(pos).is(ModBlocks.FACTORY_HOPPER.get()),"weak blast preserves steel hopper");
         h.assertTrue(h.getLevel().getBlockEntity(pos)==m,"weak blast retains original block entity");
         h.assertValueEqual(5,m.getItem(0).getCount(),"weak blast retains active inventory");
         h.assertValueEqual(2,m.getItem(26).getCount(),"weak blast retains legacy inventory");
         CompoundTag survived=m.saveWithoutMetadata(h.getLevel().registryAccess());
         h.assertValueEqual(4,survived.getInt("HopperRange"),"weak blast retains range");
         h.assertValueEqual(17,survived.getInt("HopperTransferCount"),"weak blast retains count");
         h.assertValueEqual(1,survived.getInt("HopperLogic"),"weak blast retains logic");
         h.assertValueEqual(99,survived.getInt("HopperPeriod"),"weak blast retains period");
         h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(0.5)).isEmpty(),"weak blast emits no drops");
         h.getLevel().explode(null,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,
            4.0F,false,net.minecraft.world.level.Level.ExplosionInteraction.TNT);
         h.assertTrue(!h.getLevel().getBlockState(pos).is(ModBlocks.FACTORY_HOPPER.get()),"real explosion removes hopper");
         var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(0.5));
         for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
            if(x!=0||y!=0||z!=0) h.assertTrue(h.getLevel().getBlockState(pos.offset(x,y,z)).is(Blocks.BEDROCK),"blast containment intact");
         h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModBlocks.FACTORY_HOPPER.get().asItem())),"explosion never emits hopper capsule");
         int diamonds=drops.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
         int emeralds=drops.stream().filter(e->e.getItem().is(Items.EMERALD)).mapToInt(e->e.getItem().getCount()).sum();
         h.assertValueEqual(dropsEnabled?5:0,diamonds,"active contents emitted exactly once iff drops enabled");
         h.assertValueEqual(dropsEnabled?2:0,emeralds,"legacy contents emitted exactly once iff drops enabled");
         h.assertTrue(m.isEmpty(),"removed backing inventory drained");
      } finally {
         dropsRule.set(oldDrops,h.getLevel().getServer());decayRule.set(oldDecay,h.getLevel().getServer());
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void hopper_nodrop_destruction_and_same_block_state_change_do_not_duplicate(GameTestHelper h) {
      MachineBlockEntity m=place(h);m.setItem(0,new ItemStack(Items.DIAMOND,5));
      BlockPos pos=h.absolutePos(POS);
      h.getLevel().setBlock(pos,m.getBlockState().setValue(com.oblixorprime.engineersdecorreforged.utility.MachineBlocks.FACING,
         net.minecraft.core.Direction.WEST),3);
      h.assertValueEqual(5,((MachineBlockEntity)h.getLevel().getBlockEntity(pos)).getItem(0).getCount(),"same-block rotation retains inventory");
      h.assertTrue(h.getLevel().destroyBlock(pos,false),"no-drop destruction succeeds");
      h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).isEmpty(),"no-drop destruction does not spill contents or capsule");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void hopper_owner_steel_upgrade_is_local_and_preserves_mining_hardness(GameTestHelper h) {
      MachineBlockEntity m=place(h);
      h.assertValueEqual(15.0F,ModBlocks.FACTORY_HOPPER.get().getExplosionResistance(),"owner steel upgrade resistance15");
      h.assertValueEqual(12.0F,ModBlocks.FACTORY_DROPPER.get().getExplosionResistance(),"other machines retain resistance12");
      h.assertTrue(ModBlocks.FACTORY_HOPPER.get().getExplosionResistance()>Blocks.HOPPER.getExplosionResistance(),"industrial hopper exceeds vanilla hopper resistance");
      h.assertValueEqual(2.0F,m.getBlockState().getDestroySpeed(h.getLevel(),h.absolutePos(POS)),"mining hardness remains2");
      h.succeed();
   }

   private static MachineBlockEntity place(GameTestHelper h) {
      h.setBlock(POS,Blocks.AIR);h.setBlock(POS,ModBlocks.FACTORY_HOPPER.get());
      return (MachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
   }
}
