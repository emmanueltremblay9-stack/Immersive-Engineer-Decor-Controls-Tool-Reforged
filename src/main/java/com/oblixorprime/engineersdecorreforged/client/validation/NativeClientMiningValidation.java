package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkBlock;
import com.oblixorprime.engineersdecorreforged.block.RailingMining;
import com.oblixorprime.engineersdecorreforged.client.ClientInsetLightMiningEvents;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Explicitly opt-in CI gameplay fixture. Never opens an existing world, changes
 * ordinary options, or runs without a nonce marker in a fresh script-owned game
 * directory. A loaded title screen is NOT a successful gameplay test.
 *
 * Uses a real integrated server, LocalPlayer, ClientLevel prediction, native
 * gameMode START/HOLD and vanilla packets/ACKs. A non-pausing screen suppresses
 * unrelated OS input; scripted attacks enter Minecraft's real NeoForge input
 * gate before calling gameMode, exactly as Minecraft's attack handlers do.
 */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class NativeClientMiningValidation {
   private static final boolean ENABLED = Boolean.getBoolean("iedctr.clientGameplay");
   private static final String SUITE = "native-client-lamp-mining-v1";
   private static final BlockPos POS = new BlockPos(0, 80, 0);
   private static final List<Direction> APPROACHES = List.of(Direction.DOWN, Direction.NORTH,
      Direction.EAST, Direction.SOUTH, Direction.WEST);
   private static final JsonArray RESULTS = new JsonArray();
   private static Phase phase = Phase.BOOT;
   private static long deadline;
   private static Path evidence;
   private static String nonce;
   private static int caseIndex;
   private static int ticks;
   private static int miningTicks;
   private static int holdTicks;
   private static int baselineSequence;
   private static int lampSequence;
   private static int completionSequence = -1;
   private static int heldPacketCount;
   private static int canceledHolds;
   private static int stageTicks;
   private static Probe probe;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<ServerEvidence> inspection;
   private static Vec3 eye;
   private static Vec3 lampTarget;
   private static JsonObject result;
   private static boolean complete;
   private static IKeyConflictContext originalAttackContext;
   private static boolean fixtureAttackContextVerified;
   private static int clientLampRemovals;
   private static Prediction prediction;
   private static int caseStartPackets;
   private static int caseAbortPackets;
   private static int caseStopPackets;
   private static int survivalLampHoldChecks;
   private static int targetMismatchHolds;
   private static int maxNativeDestroyStage;

   private NativeClientMiningValidation() {}

   private enum Phase { BOOT, WORLD, PROBE, SETUP, READY, STALE_POSE, MINE_LAMP,
      LAMP_ACK, HOLD, INSPECT_LAMP, RELEASE, MINE_DECK, DECK_ACK, INSPECT_DECK, FLUIDLOGGING, WATER_CANAL, CATALOGUE, SUBMERGED_CATWALK, GENERIC_FLUID, WATER_DISTANCE, LAVA_SURFACE, DOOR_SENSOR, PANZER_GLASS }

   @SubscribeEvent
   public static void removed(RailingMining.InsetLightRemoved event) {
      if (!ENABLED || complete) return;
      Minecraft mc = Minecraft.getInstance();
      if (event.player() == mc.player && event.player().level() == mc.level && event.pos().equals(POS)) {
         clientLampRemovals++;
      }
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      if (!ENABLED || complete) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      try {
         if (phase == Phase.BOOT) {
            if (deadline == 0) {
               // The full catalogue fixture adds 8960 settling/recession ticks; retain the old flow-only bound.
               deadline = System.nanoTime() + (Boolean.getBoolean("iedctr.flowDistanceOnly") ? 360_000_000_000L : 900_000_000_000L);
            }
            require(System.nanoTime() < deadline, "Timed out before the title screen");
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
               boot(mc);
            }
            return;
         }
         require(System.nanoTime() < deadline, "Gameplay suite deadline exceeded at " + phase);
         require(++stageTicks < (phase == Phase.WATER_DISTANCE ? 4800 : phase == Phase.CATALOGUE ? 14000 : 2400), "Gameplay phase made no progress: " + phase);
         if (phase == Phase.WORLD) {
            if (mc.level != null && mc.player != null && mc.gameMode != null && mc.screen == null) {
               require(mc.getSingleplayerServer() != null, "Fixture must use its own integrated server");
               Connection connection = mc.player.connection.getConnection();
               require(connection.isMemoryConnection(), "Fixture must never connect to a remote server");
               if (Boolean.getBoolean("iedctr.panzerGlassOnly")) {
                  mc.setScreen(new FixtureScreen());
                  enter(Phase.PANZER_GLASS);
                  return;
               }
               if (Boolean.getBoolean("iedctr.doorSensorOnly")) {
                  mc.setScreen(new FixtureScreen());
                  enter(Phase.DOOR_SENSOR);
                  return;
               }
               TrackerRenderValidation.verify(mc, evidence.getParent(), nonce);
               mc.setScreen(new FixtureScreen());
               if (Boolean.getBoolean("iedctr.lavaSurfaceOnly")) { enter(Phase.LAVA_SURFACE); return; }
               if (Boolean.getBoolean("iedctr.flowDistanceOnly")) {
                  enter(Phase.WATER_DISTANCE);
                  return;
               }
               originalAttackContext = mc.options.keyAttack.getKeyConflictContext();
               // NeoForge gates isDown() by IN_GAME even after setDown(true).
               // The isolation screen intentionally blocks vanilla/OS key handling,
               // so simulate an active attack mapping only inside this fresh fixture.
               require(originalAttackContext == KeyConflictContext.IN_GAME,
                  "Fixture expects the native attack binding's IN_GAME conflict context");
               mc.options.keyAttack.setDown(true);
               require(!mc.options.keyAttack.isDown(),
                  "An open fixture screen must disable the unmodified IN_GAME attack binding");
               mc.options.keyAttack.setKeyConflictContext(KeyConflictContext.UNIVERSAL);
               require(mc.options.keyAttack.isDown(), "Fixture attack mapping did not become active");
               mc.options.keyAttack.setDown(false);
               fixtureAttackContextVerified = true;
               probe = new Probe(mc);
               CompletableFuture<Void> installed = new CompletableFuture<>();
               pending = installed;
               connection.channel().eventLoop().execute(() -> {
                  try {
                     String handler = connection.channel().pipeline().context(connection).name();
                     connection.channel().pipeline().addBefore(handler, "iedctr_ci_packet_observer", probe);
                     installed.complete(null);
                  } catch (Throwable error) {
                     installed.completeExceptionally(error);
                  }
               });
               enter(Phase.PROBE);
            }
            return;
         }
         require(mc.level != null && mc.player != null && mc.gameMode != null
            && mc.getSingleplayerServer() != null, "Integrated world disconnected during gameplay");
         mc.player.setNoGravity(true);
         mc.player.setDeltaMovement(Vec3.ZERO);
         switch (phase) {
            case PROBE -> {
               if (awaitPending()) setup(mc);
            }
            case SETUP -> {
               if (awaitPending()) {
                  result.addProperty("equal_id_cross_side_players_verified", true);
                  enter(Phase.READY);
               }
            }
            case READY -> {
               if (!isLit(mc.level.getBlockState(POS)) || mc.gameMode.getPlayerMode() != mode()
                  || !mc.player.getMainHandItem().is(Items.IRON_PICKAXE)
                  || mc.player.getEyePosition().distanceTo(eye) > 0.02) return;
               // Establish a genuine previous client pose that selects the deck. The
               // next attack turns to the lamp in the same client tick as START.
               boolean aimedDeck = false;
               for (double[] corner : new double[][]{{.08, .08}, {.92, .92}, {.08, .92}, {.92, .08}}) {
                  look(mc, Vec3.atLowerCornerOf(POS).add(corner[0], .90, corner[1]));
                  PortedBlocks.RailingPart part = selected(mc);
                  if (part != null && part.kind() == PortedBlocks.PartKind.BASE) {
                     aimedDeck = true;
                     break;
                  }
               }
               require(aimedDeck, "Fixture could not establish a stale deck-facing pose");
               mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                  mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
               ticks = 0;
               enter(Phase.STALE_POSE);
            }
            case STALE_POSE -> {
               if (++ticks < 8) return;
               // Server-thread inspection after the prior pose has traversed the
               // normal client connection. This is observation, not pose injection.
               if (pending == null) {
                  pending = mc.getSingleplayerServer().submit(() -> {
                     ServerPlayer player = serverPlayer(mc);
                     Vec3 start = player.getEyePosition();
                     PortedBlocks.RailingPart part = PortedBlocks.selectRailingPart(player.level(), POS, start,
                        start.add(player.getLookAngle().scale(player.blockInteractionRange())), player);
                     require(part != null && part.kind() == PortedBlocks.PartKind.BASE,
                        "Server must initially see the stale deck-facing pose");
                  });
                  return;
               }
               if (!awaitPending()) return;
               look(mc, lampTarget);
               require(selected(mc) != null && selected(mc).kind() == PortedBlocks.PartKind.INSET_LIGHT,
                  "The actual LocalPlayer eye ray must select the inset lamp");
               float progress = mc.level.getBlockState(POS).getDestroyProgress(mc.player, mc.level, POS);
               require(progress > 0 && progress < 1, "Fixture must exercise multi-tick survival mining");
               miningTicks = (int)Math.ceil(1.0F / progress) + 8;
               require(miningTicks < 350, "Fixture mining duration exceeds its bounded budget");
               baselineSequence = probe.latestSequence;
               caseStartPackets = probe.startPackets;
               caseAbortPackets = probe.abortPackets;
               caseStopPackets = probe.stopPackets;
               completionSequence = -1;
               inspection = null;
               canceledHolds = 0;
               ticks = 0;
               mc.options.keyAttack.setDown(true);
               attack(mc, true, false);
               if (mode() == GameType.SURVIVAL) {
                  require(isLit(mc.level.getBlockState(POS)), "Survival START must retain the lamp until native completion");
               } else {
                  require(!isLit(mc.level.getBlockState(POS)),
                     "Creative START must predict lamp removal synchronously before any server update");
               }
               enter(Phase.MINE_LAMP);
               observeLampPrediction(mc);
            }
            case MINE_LAMP -> {
               require(++ticks < miningTicks + 100, "Native lamp mining did not finish");
               attack(mc, false, false);
               observeLampPrediction(mc);
            }
            case LAMP_ACK -> {
               attack(mc, false, true);
               BlockState clientState = mc.level.getBlockState(POS);
               require(clientState.is(ModBlocks.STEEL_CATWALK_TA.get()),
                  "Lamp reconciliation must never remove the retained client catwalk");
               if (isLit(clientState)) result.addProperty("temporary_lit_rollback_observed", true);
               // Native early STOP can be acknowledged before delayed server mining
               // finishes. Keep the input latch through that legitimate lit rollback,
               // but require eventual client AND authoritative lamp-only completion.
               if (!completionAcknowledged() || isLit(clientState)) return;
               assertUnlit(clientState, "Reconciled client after lamp completion ACK");
               if (inspection == null) {
                  inspection = inspect(mc);
                  return;
               }
               if (!inspection.isDone()) return;
               ServerEvidence state = inspection.join();
               inspection = null;
               require(state.state().is(ModBlocks.STEEL_CATWALK_TA.get()),
                  "Lamp reconciliation must never remove the authoritative catwalk");
               if (isLit(state.state())) return;
               assertUnlit(state.state(), "Authoritative lamp completion after exact native ACK");
               lampSequence = completionSequence;
               heldPacketCount = probe.miningPackets;
               result.addProperty("lamp_sequence", lampSequence);
               result.addProperty("lamp_completion_action", completionAction().name());
               result.addProperty("lamp_ack_applied", probe.ackApplied);
               ticks = 0;
               holdTicks = miningTicks + 12;
               enter(Phase.HOLD);
            }
            case HOLD -> {
               attack(mc, false, true);
               assertUnlit(mc.level.getBlockState(POS), "Client during continuous held attack");
               require(probe.miningPackets == heldPacketCount, "Held attack leaked a START/STOP mining packet");
               if (++ticks >= holdTicks) {
                  inspection = inspect(mc);
                  enter(Phase.INSPECT_LAMP);
               }
            }
            case INSPECT_LAMP -> {
               attack(mc, false, true);
               if (!inspection.isDone()) return;
               ServerEvidence state = inspection.join();
               assertUnlit(state.state(), "Authoritative server after ACK and held attack");
               int expected = mode() == GameType.SURVIVAL ? 1 : 0;
               require(state.lamps() == expected && state.decks() == 0,
                  "Lamp mining must yield only the paid lamp, never a catwalk: " + state);
               require(state.toolDamage() == expected, "Lamp mining must apply exactly the native tool wear: " + state);
               require(probe.miningPackets == heldPacketCount, "Input gate emitted an extra mining packet");
               result.addProperty("authoritative_lamp_only", true);
               require(clientLampRemovals == 1, "Each case must complete exactly one native client lamp removal");
               result.addProperty("client_lamp_removal_notifications", clientLampRemovals);
               result.addProperty("held_attack_cancellations", canceledHolds);
               result.addProperty("held_ticks", holdTicks);
               result.addProperty("held_extra_mining_packets", probe.miningPackets - heldPacketCount);
               result.addProperty("lamp_drops_or_inventory", state.lamps());
               result.addProperty("lamp_tool_damage", state.toolDamage());
               mc.options.keyAttack.setDown(false);
               enter(Phase.RELEASE);
            }
            case RELEASE -> {
               // ClientTick.Pre has now observed release. The same target must be
               // mineable again; this also prevents a blanket-cancellation false pass.
               baselineSequence = probe.latestSequence;
               completionSequence = -1;
               inspection = null;
               mc.options.keyAttack.setDown(true);
               attack(mc, true, false);
               ticks = 0;
               enter(Phase.MINE_DECK);
               observeDeckPrediction(mc);
            }
            case MINE_DECK -> {
               require(++ticks < miningTicks + 100, "Released attack could not mine the retained deck");
               attack(mc, false, false);
               observeDeckPrediction(mc);
            }
            case DECK_ACK -> {
               BlockState clientState = mc.level.getBlockState(POS);
               require(clientState.isAir() || clientState.is(ModBlocks.STEEL_CATWALK_TA.get()),
                  "Deck reconciliation replaced the fixture with an unexpected block");
               // A native early STOP may temporarily restore the deck as well.
               // A permanent denial remains pending and fails the bounded deadline.
               if (completionAcknowledged() && clientState.isAir()) {
                  result.addProperty("deck_sequence", completionSequence);
                  result.addProperty("deck_completion_action", completionAction().name());
                  result.addProperty("deck_ack_applied", probe.ackApplied);
                  inspection = inspect(mc);
                  enter(Phase.INSPECT_DECK);
               }
            }
            case INSPECT_DECK -> {
               if (!inspection.isDone()) return;
               ServerEvidence state = inspection.join();
               if (!state.state().isAir()) {
                  require(state.state().is(ModBlocks.STEEL_CATWALK_TA.get()),
                     "Authoritative deck was replaced with an unexpected block");
                  enter(Phase.DECK_ACK);
                  return;
               }
               require(state.decks() == (mode() == GameType.SURVIVAL ? 1 : 0), "Deck native loot differs from game mode");
               result.addProperty("release_allows_native_deck_mining", true);
               result.addProperty("authoritative_deck_removed", true);
               RESULTS.add(result);
               write("running", null);
               EngineersDecorReforged.LOGGER.info("IEDCTR_CLIENT_GAMEPLAY case {}/10 passed: {}", caseIndex + 1, result);
               mc.options.keyAttack.setDown(false);
               if (++caseIndex == 10) enter(Phase.FLUIDLOGGING);
               else setup(mc);
            }
            case FLUIDLOGGING -> {
               if (NativeFluidloggingValidation.tick(mc, evidence.getParent(), nonce)) enter(Phase.GENERIC_FLUID);
            }
            case GENERIC_FLUID -> {
               if (NativeGenericFluidValidation.tick(mc, evidence.getParent(), nonce)) enter(Phase.SUBMERGED_CATWALK);
            }
            case SUBMERGED_CATWALK -> {
               if (NativeSubmergedCatwalkValidation.tick(mc, evidence.getParent(), nonce)) {
                  if(Boolean.getBoolean("iedctr.flowDistanceOnly")) finish(mc,null); else enter(Phase.CATALOGUE);
               }
            }
            case CATALOGUE -> {
               if (NativeCatalogueValidation.tick(mc, evidence.getParent(), nonce)) enter(Phase.WATER_CANAL);
            }
            case WATER_CANAL -> {
               if (NativeWaterCanalValidation.tick(mc, evidence.getParent(), nonce)) finish(mc, null);
            }
            case DOOR_SENSOR -> { if (NativeDoorSensorValidation.tick(mc, evidence.getParent(), nonce)) finish(mc,null); }
            case PANZER_GLASS -> { if (NativePanzerGlassValidation.tick(mc, evidence.getParent(), nonce)) finish(mc,null); }
            case LAVA_SURFACE -> { if (NativeLavaSurfaceValidation.tick(mc, evidence.getParent(), nonce)) finish(mc,null); }
            case WATER_DISTANCE -> {
               if (NativeWaterFlowDistanceValidation.tick(mc, evidence.getParent(), nonce)) enter(Phase.GENERIC_FLUID);
            }
            default -> throw new IllegalStateException("Unexpected fixture phase " + phase);
         }
      } catch (Throwable failure) {
         finish(mc, failure);
      }
   }

   private static void boot(Minecraft mc) throws Exception {
      nonce = System.getProperty("iedctr.clientGameplayNonce", "");
      require(nonce.matches("[a-f0-9]{32}"), "A fresh CI nonce is required");
      Path game = mc.gameDirectory.toPath().toRealPath();
      Path owned = Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath();
      require(game.equals(owned), "Client game directory must equal the script-owned directory");
      require(Files.readString(game.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Missing matching isolation marker");
      Path saves = game.resolve("saves");
      if (Files.exists(saves)) {
         try (var entries = Files.list(saves)) {
            require(entries.findAny().isEmpty(), "Refusing to use a directory with existing saves");
         }
      }
      require(mc.level == null && mc.getSingleplayerServer() == null, "Refusing to replace a loaded world");
      if (Boolean.getBoolean("iedctr.requireLithiumFluidBfs")) {
         var lithium = net.neoforged.fml.ModList.get().getModContainerById("lithium").orElseThrow();
         require(lithium.getModInfo().getVersion().toString().equals("0.15.4+mc1.21.1"), "Exact Lithium version required");
         require(java.util.Arrays.stream(net.minecraft.world.level.material.FlowingFluid.class.getDeclaredMethods())
            .anyMatch(m -> m.getName().contains("calculateComplexFluidFlowDirections")), "Lithium fluid BFS must remain enabled");
         EngineersDecorReforged.LOGGER.info("IEDCTR_LITHIUM_COMPAT version=0.15.4+mc1.21.1 bfs=true nonce={}", nonce);
      }
      evidence = game.resolve("client-gameplay-result.json");
      write("running", null);
      mc.options.pauseOnLostFocus = false;
      mc.options.autoJump().set(true);
      String name = "iedctr-ci-" + nonce;
      LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL,
         true, new GameRules(), WorldDataConfiguration.DEFAULT);
      enter(Phase.WORLD);
      mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false),
         registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
            .value().createWorldDimensions(), new TitleScreen());
   }

   private static void setup(Minecraft mc) {
      mc.options.keyAttack.setDown(false);
      mc.gameMode.stopDestroyBlock();
      Direction direction = APPROACHES.get(caseIndex % APPROACHES.size());
      Vec3 offset = direction == Direction.DOWN ? new Vec3(.5, .1, .5)
         : new Vec3(.5 + direction.getStepX() * 1.7, .2, .5 + direction.getStepZ() * 1.7);
      eye = Vec3.atLowerCornerOf(POS).add(offset);
      lampTarget = Vec3.atLowerCornerOf(POS).add(.5, 14.8 / 16, .5);
      clientLampRemovals = 0;
      survivalLampHoldChecks = 0;
      targetMismatchHolds = 0;
      maxNativeDestroyStage = -1;
      prediction = null;
      result = new JsonObject();
      result.addProperty("case", mode().getName() + "-" + direction.getName());
      result.addProperty("mode", mode().getName());
      result.addProperty("approach", direction.getName());
      result.addProperty("temporary_lit_rollback_observed", false);
      pending = mc.getSingleplayerServer().submit(() -> {
         ServerPlayer player = serverPlayer(mc);
         require(player != (Object)mc.player && player.getId() == mc.player.getId()
            && player.equals(mc.player) && player.hashCode() == mc.player.hashCode(),
            "Fixture must exercise distinct equal-ID client/server players sharing Entity equality");
         var level = player.serverLevel();
         level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12)).forEach(ItemEntity::discard);
         for (BlockPos pos : BlockPos.betweenClosed(POS.offset(-4, -4, -4), POS.offset(4, 4, 4))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
         }
         // ItemEntity keeps ordinary gravity. Catch legitimate loot below the
         // floating player's feet so a sustained hold cannot drop it into the void.
         for (BlockPos floor : BlockPos.betweenClosed(POS.offset(-4, -4, -4), POS.offset(4, -4, 4))) {
            level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
         }
         level.setBlock(POS, ModBlocks.STEEL_CATWALK_TA.get().defaultBlockState()
            .setValue(RaisedCatwalkBlock.VARIANT, 4).setValue(RaisedCatwalkBlock.PAID_LIGHT, true), 3);
         player.setGameMode(mode());
         player.getInventory().clearContent();
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
         player.setNoGravity(true);
         player.setDeltaMovement(Vec3.ZERO);
         player.connection.teleport(eye.x, eye.y - player.getEyeHeight(), eye.z, 0, 0);
         player.containerMenu.broadcastChanges();
      });
      enter(Phase.SETUP);
   }

   private static void look(Minecraft mc, Vec3 target) {
      Vec3 delta = target.subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
      updateHit(mc);
   }

   private static void updateHit(Minecraft mc) {
      Vec3 start = mc.player.getEyePosition();
      mc.hitResult = mc.level.clip(new ClipContext(start, start.add(mc.player.getViewVector(1).scale(mc.player.blockInteractionRange())),
         ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
   }

   private static PortedBlocks.RailingPart selected(Minecraft mc) {
      Vec3 start = mc.player.getEyePosition();
      return PortedBlocks.selectRailingPart(mc.level, POS, start,
         start.add(mc.player.getViewVector(1).scale(mc.player.blockInteractionRange())), mc.player);
   }

   private static void attack(Minecraft mc, boolean start, boolean canceled) {
      require(mc.options.keyAttack.isDown(),
         "Scripted held attack must be an active mapping, including its NeoForge conflict context: "
            + mc.options.keyAttack.getKeyConflictContext());
      result.addProperty("active_attack_binding", true);
      updateHit(mc);
      require(mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
         && hit.getBlockPos().equals(POS), "Native eye ray must hit the test owner");
      var input = ClientHooks.onClickInput(0, mc.options.keyAttack, InteractionHand.MAIN_HAND);
      require(input.isCanceled() == canceled,
         (canceled ? "Continuous held attack was not suppressed" : "Fresh native attack was unexpectedly suppressed")
            + "; client removal notifications=" + clientLampRemovals
            + "; " + ClientInsetLightMiningEvents.validationState());
      if (canceled) {
         require(!input.shouldSwingHand(), "Canceled held attack must suppress its swing");
         canceledHolds++;
         return;
      }
      BlockState before = mc.level.getBlockState(POS);
      boolean matchesBefore = RailingMining.matchesCurrentTarget(mc.player, POS);
      if (!start && !matchesBefore) targetMismatchHolds++;
      recordMiningDiagnostic(mc, matchesBefore, "before native attack");
      if (phase == Phase.MINE_LAMP && mode() == GameType.SURVIVAL) {
         require(matchesBefore, "Stable survival lamp HOLD lost its client-side intent; " + result.get("native_mining_diagnostic"));
         require(mc.gameMode.isDestroying(), "Survival native mining was stopped between held attacks; " + result.get("native_mining_diagnostic"));
         survivalLampHoldChecks++;
      }
      if (phase == Phase.STALE_POSE || phase == Phase.MINE_LAMP) {
         require(isLit(before), "A prior server update cannot qualify as native client lamp prediction");
      }
      if (phase == Phase.RELEASE || phase == Phase.MINE_DECK) {
         assertUnlit(before, "Native deck prediction must begin with the retained client deck");
      }
      int removalsBefore = clientLampRemovals;
      BlockHitResult hit = (BlockHitResult)mc.hitResult;
      if (start) mc.gameMode.startDestroyBlock(POS, hit.getDirection());
      else mc.gameMode.continueDestroyBlock(POS, hit.getDirection());
      prediction = new Prediction(before, mc.level.getBlockState(POS), clientLampRemovals - removalsBefore);
      recordMiningDiagnostic(mc, matchesBefore, "after native attack");
   }

   private static void recordMiningDiagnostic(Minecraft mc, boolean matchesBefore, String point) {
      maxNativeDestroyStage = Math.max(maxNativeDestroyStage, mc.gameMode.getDestroyStage());
      JsonObject diagnostic = new JsonObject();
      diagnostic.addProperty("point", point);
      diagnostic.addProperty("ticks", ticks);
      diagnostic.addProperty("start_packets", probe.startPackets - caseStartPackets);
      diagnostic.addProperty("abort_packets", probe.abortPackets - caseAbortPackets);
      diagnostic.addProperty("stop_packets", probe.stopPackets - caseStopPackets);
      diagnostic.addProperty("target_matched_before", matchesBefore);
      diagnostic.addProperty("target_matches_now", RailingMining.matchesCurrentTarget(mc.player, POS));
      diagnostic.addProperty("native_is_destroying", mc.gameMode.isDestroying());
      diagnostic.addProperty("native_destroy_stage", mc.gameMode.getDestroyStage());
      diagnostic.addProperty("max_native_destroy_stage", maxNativeDestroyStage);
      diagnostic.addProperty("target_mismatch_holds", targetMismatchHolds);
      diagnostic.addProperty("survival_lamp_hold_checks", survivalLampHoldChecks);
      // One rolling snapshot stays bounded even if a later run fails to progress.
      result.add("native_mining_diagnostic", diagnostic);
   }

   private static void observeLampPrediction(Minecraft mc) {
      BlockState state = mc.level.getBlockState(POS);
      require(state.is(ModBlocks.STEEL_CATWALK_TA.get()), "Native client prediction destroyed the catwalk while mining its lamp");
      if (!isLit(state)) {
         require(prediction != null && isLit(prediction.before()) && !isLit(prediction.after())
            && prediction.lampNotifications() == 1,
            "Lamp-only prediction must change lit to unlit and emit its completion event inside the native attack call");
         assertUnlit(state, "Immediate native ClientLevel prediction");
         result.addProperty("synchronous_lamp_completion_event", true);
         result.addProperty("immediate_client_prediction_lamp_only", true);
         result.addProperty("native_mining_ticks", ticks);
         result.addProperty("survival_lamp_hold_checks", survivalLampHoldChecks);
         enter(Phase.LAMP_ACK);
      }
   }

   private static void observeDeckPrediction(Minecraft mc) {
      if (mc.level.getBlockState(POS).isAir()) {
         require(prediction != null && !prediction.before().isAir() && prediction.after().isAir()
            && prediction.lampNotifications() == 0,
            "Deck prediction must remove the block synchronously inside its native attack call");
         result.addProperty("immediate_client_prediction_deck_removed", true);
         mc.options.keyAttack.setDown(false);
         enter(Phase.DECK_ACK);
      }
   }

   private static CompletableFuture<ServerEvidence> inspect(Minecraft mc) {
      return mc.getSingleplayerServer().submit(() -> {
         ServerPlayer player = serverPlayer(mc);
         return new ServerEvidence(player.level().getBlockState(POS), count(player, ModBlocks.IRON_INSET_LIGHT.get().asItem()),
            count(player, ModBlocks.STEEL_CATWALK_TA.get().asItem()), player.getMainHandItem().getDamageValue());
      });
   }

   private static int count(ServerPlayer player, Item item) {
      int total = player.getInventory().countItem(item);
      for (ItemEntity entity : player.level().getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12))) {
         if (entity.getItem().is(item)) total += entity.getItem().getCount();
      }
      return total;
   }

   private static ServerPlayer serverPlayer(Minecraft mc) {
      ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
      require(player != null, "Integrated server lacks the connected real player");
      return player;
   }

   private static boolean awaitPending() {
      if (!pending.isDone()) return false;
      pending.join();
      pending = null;
      return true;
   }

   private static ServerboundPlayerActionPacket.Action completionAction() {
      return mode() == GameType.CREATIVE ? ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
         : ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK;
   }

   private static boolean completionAcknowledged() {
      // The Netty observer is asynchronous. An already-ACKed survival START is
      // never evidence that the later STOP/completion has even been written yet.
      if (completionSequence < 0) {
         int observed = mode() == GameType.CREATIVE ? probe.latestStartSequence : probe.latestStopSequence;
         if (observed > baselineSequence) completionSequence = observed;
      }
      return completionSequence >= 0 && probe.ackApplied >= completionSequence;
   }

   private static GameType mode() { return caseIndex < APPROACHES.size() ? GameType.CREATIVE : GameType.SURVIVAL; }
   private static boolean isLit(BlockState state) { return state.is(ModBlocks.STEEL_CATWALK_TA.get()) && state.getValue(RaisedCatwalkBlock.VARIANT) == 4; }
   private static void assertUnlit(BlockState state, String context) {
      require(state.is(ModBlocks.STEEL_CATWALK_TA.get()) && state.getValue(RaisedCatwalkBlock.VARIANT) == 0
         && !state.getValue(RaisedCatwalkBlock.PAID_LIGHT), context + " must preserve precisely the unlit unpaid deck: " + state);
   }
   private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
   private static void enter(Phase next) { phase = next; stageTicks = 0; }

   private static void write(String status, Throwable error) throws Exception {
      JsonObject proof = new JsonObject();
      proof.addProperty("suite", Boolean.getBoolean("iedctr.panzerGlassOnly") ? "panzer-glass-session-v1" : Boolean.getBoolean("iedctr.doorSensorOnly") ? "door-sensor-session-v1" : Boolean.getBoolean("iedctr.lavaSurfaceOnly") ? "lava-surface-session-v1" : Boolean.getBoolean("iedctr.flowDistanceOnly") ? "native-water-flow-session-v1" : SUITE);
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", status);
      proof.addProperty("phase", phase.name());
      proof.addProperty("expected_cases", Boolean.getBoolean("iedctr.panzerGlassOnly") || Boolean.getBoolean("iedctr.doorSensorOnly") || Boolean.getBoolean("iedctr.lavaSurfaceOnly") ? 0 : 10);
      proof.addProperty("actual_integrated_world", true);
      proof.addProperty("fixture_attack_context_verified", fixtureAttackContextVerified);
      proof.addProperty("scope", Boolean.getBoolean("iedctr.panzerGlassOnly") ? "Isolated Panzer Glass scene captures and face visibility checks; no mining qualification" : Boolean.getBoolean("iedctr.doorSensorOnly") ? "Isolated native Door Sensor placement and captures; no mining qualification" : Boolean.getBoolean("iedctr.lavaSurfaceOnly") ? "Isolated lava height and mesh diagnostic; no mining qualification" : Boolean.getBoolean("iedctr.flowDistanceOnly") ? "Natural water flow observation only; no lamp mining qualification" : "Native gameMode and input-gate gameplay; no physical OS input or visual rendering assertions");
      proof.add("cases", RESULTS.deepCopy());
      if (error != null) {
         proof.addProperty("error", error.toString());
         proof.addProperty("hold_diagnostic", ClientInsetLightMiningEvents.validationState());
         proof.addProperty("client_lamp_removal_notifications", clientLampRemovals);
         if (result != null) proof.add("in_progress_case", result.deepCopy());
      }
      Files.writeString(evidence, new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
   }

   private static void finish(Minecraft mc, Throwable error) {
      complete = true;
      mc.options.keyAttack.setDown(false);
      if (originalAttackContext != null) mc.options.keyAttack.setKeyConflictContext(originalAttackContext);
      try {
         if (evidence != null) write(error == null ? "passed" : "failed", error);
      } catch (Exception writeError) {
         EngineersDecorReforged.LOGGER.error("IEDCTR_CLIENT_GAMEPLAY cannot write evidence", writeError);
      }
      if (error == null && Boolean.getBoolean("iedctr.panzerGlassOnly"))
         EngineersDecorReforged.LOGGER.info("IEDCTR_PANZER_GLASS_SESSION PASSED nonce={}", nonce);
      else if (error == null && Boolean.getBoolean("iedctr.doorSensorOnly"))
         EngineersDecorReforged.LOGGER.info("IEDCTR_DOOR_SENSOR_SESSION PASSED nonce={}", nonce);
      else if (error == null && Boolean.getBoolean("iedctr.lavaSurfaceOnly"))
         EngineersDecorReforged.LOGGER.info("IEDCTR_LAVA_SURFACE PASSED nonce={}", nonce);
      else if (error == null && Boolean.getBoolean("iedctr.flowDistanceOnly"))
         EngineersDecorReforged.LOGGER.info("IEDCTR_WATER_FLOW_SESSION PASSED nonce={}", nonce);
      else if (error == null) EngineersDecorReforged.LOGGER.info("IEDCTR_CLIENT_GAMEPLAY PASSED nonce={} cases={}", nonce, RESULTS.size());
      else EngineersDecorReforged.LOGGER.error("IEDCTR_CLIENT_GAMEPLAY FAILED at " + phase, error);
      mc.stop();
   }

   private record Prediction(BlockState before, BlockState after, int lampNotifications) {}

   private record ServerEvidence(BlockState state, int lamps, int decks, int toolDamage) {}

   private static final class FixtureScreen extends Screen {
      private FixtureScreen() { super(Component.literal("Isolated native client gameplay validation")); }
      @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }
      @Override public boolean isPauseScreen() { return false; }
      @Override public boolean shouldCloseOnEsc() { return false; }
   }

   /** Passive observation only: every packet continues untouched through vanilla handlers. */
   private static final class Probe extends ChannelDuplexHandler {
      private final Minecraft minecraft;
      private volatile int latestSequence = -1;
      private volatile int latestStartSequence = -1;
      private volatile int latestStopSequence = -1;
      private volatile int ackApplied = -1;
      private volatile int miningPackets;
      private volatile int startPackets;
      private volatile int abortPackets;
      private volatile int stopPackets;
      private Probe(Minecraft minecraft) { this.minecraft = minecraft; }
      @Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
         if (message instanceof ServerboundPlayerActionPacket action && action.getPos().equals(POS)) {
            switch (action.getAction()) {
               case START_DESTROY_BLOCK -> startPackets++;
               case ABORT_DESTROY_BLOCK -> abortPackets++;
               case STOP_DESTROY_BLOCK -> stopPackets++;
               default -> { }
            }
         }
         if (message instanceof ServerboundPlayerActionPacket action && action.getPos().equals(POS)
            && (action.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
               || action.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK)) {
            miningPackets++;
            latestSequence = Math.max(latestSequence, action.getSequence());
            // Publish the per-action completion sequence after its packet count.
            if (action.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
               latestStartSequence = action.getSequence();
            } else {
               latestStopSequence = action.getSequence();
            }
         }
         super.write(context, message, promise);
      }
      @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
         int sequence = ackSequence(message);
         // Connection schedules native packet handling first. This later main-thread
         // task proves the ACK handler has run before any reconciliation assertions.
         super.channelRead(context, message);
         if (sequence >= 0) minecraft.execute(() -> ackApplied = Math.max(ackApplied, sequence));
      }
      private static int ackSequence(Object message) {
         if (message instanceof ClientboundBlockChangedAckPacket ack) return ack.sequence();
         int sequence = -1;
         if (message instanceof BundlePacket<?> bundle) {
            for (Object packet : bundle.subPackets()) sequence = Math.max(sequence, ackSequence(packet));
         }
         return sequence;
      }
   }
}
