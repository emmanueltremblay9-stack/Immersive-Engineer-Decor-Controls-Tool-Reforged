package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkBlock;
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

   private NativeClientMiningValidation() {}

   private enum Phase { BOOT, WORLD, PROBE, SETUP, READY, STALE_POSE, MINE_LAMP,
      LAMP_ACK, HOLD, INSPECT_LAMP, RELEASE, MINE_DECK, DECK_ACK, INSPECT_DECK }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      if (!ENABLED || complete) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      try {
         if (phase == Phase.BOOT) {
            if (deadline == 0) {
               deadline = System.nanoTime() + 360_000_000_000L;
            }
            require(System.nanoTime() < deadline, "Timed out before the title screen");
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
               boot(mc);
            }
            return;
         }
         require(System.nanoTime() < deadline, "Gameplay suite deadline exceeded at " + phase);
         require(++stageTicks < 2400, "Gameplay phase made no progress: " + phase);
         if (phase == Phase.WORLD) {
            if (mc.level != null && mc.player != null && mc.gameMode != null && mc.screen == null) {
               require(mc.getSingleplayerServer() != null, "Fixture must use its own integrated server");
               Connection connection = mc.player.connection.getConnection();
               require(connection.isMemoryConnection(), "Fixture must never connect to a remote server");
               mc.setScreen(new FixtureScreen());
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
               if (awaitPending()) enter(Phase.READY);
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
               canceledHolds = 0;
               ticks = 0;
               mc.options.keyAttack.setDown(true);
               attack(mc, true, false);
               if (mode() == GameType.SURVIVAL) {
                  require(isLit(mc.level.getBlockState(POS)), "Survival START must retain the lamp until native completion");
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
               assertUnlit(mc.level.getBlockState(POS), "Client while awaiting lamp ACK");
               if (probe.latestSequence > baselineSequence && probe.ackApplied >= probe.latestSequence) {
                  lampSequence = probe.latestSequence;
                  heldPacketCount = probe.miningPackets;
                  result.addProperty("lamp_sequence", lampSequence);
                  result.addProperty("lamp_ack_applied", probe.ackApplied);
                  ticks = 0;
                  holdTicks = miningTicks + 12;
                  enter(Phase.HOLD);
               }
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
               if (probe.latestSequence > baselineSequence && probe.ackApplied >= probe.latestSequence) {
                  require(mc.level.getBlockState(POS).isAir(), "Deck ACK restored an unexpected client state");
                  result.addProperty("deck_sequence", probe.latestSequence);
                  result.addProperty("deck_ack_applied", probe.ackApplied);
                  inspection = inspect(mc);
                  enter(Phase.INSPECT_DECK);
               }
            }
            case INSPECT_DECK -> {
               if (!inspection.isDone()) return;
               ServerEvidence state = inspection.join();
               require(state.state().isAir(), "Released input must remove the authoritative deck");
               require(state.decks() == (mode() == GameType.SURVIVAL ? 1 : 0), "Deck native loot differs from game mode");
               result.addProperty("release_allows_native_deck_mining", true);
               result.addProperty("authoritative_deck_removed", true);
               RESULTS.add(result);
               write("running", null);
               EngineersDecorReforged.LOGGER.info("IEDCTR_CLIENT_GAMEPLAY case {}/10 passed: {}", caseIndex + 1, result);
               mc.options.keyAttack.setDown(false);
               if (++caseIndex == 10) finish(mc, null);
               else setup(mc);
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
      result = new JsonObject();
      result.addProperty("case", mode().getName() + "-" + direction.getName());
      result.addProperty("mode", mode().getName());
      result.addProperty("approach", direction.getName());
      pending = mc.getSingleplayerServer().submit(() -> {
         ServerPlayer player = serverPlayer(mc);
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
      updateHit(mc);
      require(mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
         && hit.getBlockPos().equals(POS), "Native eye ray must hit the test owner");
      var input = ClientHooks.onClickInput(0, mc.options.keyAttack, InteractionHand.MAIN_HAND);
      require(input.isCanceled() == canceled, canceled ? "Continuous held attack was not suppressed" : "Fresh native attack was unexpectedly suppressed");
      if (canceled) {
         require(!input.shouldSwingHand(), "Canceled held attack must suppress its swing");
         canceledHolds++;
         return;
      }
      BlockHitResult hit = (BlockHitResult)mc.hitResult;
      if (start) mc.gameMode.startDestroyBlock(POS, hit.getDirection());
      else mc.gameMode.continueDestroyBlock(POS, hit.getDirection());
   }

   private static void observeLampPrediction(Minecraft mc) {
      BlockState state = mc.level.getBlockState(POS);
      require(state.is(ModBlocks.STEEL_CATWALK_TA.get()), "Native client prediction destroyed the catwalk while mining its lamp");
      if (!isLit(state)) {
         assertUnlit(state, "Immediate native ClientLevel prediction");
         result.addProperty("immediate_client_prediction_lamp_only", true);
         result.addProperty("native_mining_ticks", ticks);
         enter(Phase.LAMP_ACK);
      }
   }

   private static void observeDeckPrediction(Minecraft mc) {
      if (mc.level.getBlockState(POS).isAir()) {
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
      proof.addProperty("suite", SUITE);
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", status);
      proof.addProperty("phase", phase.name());
      proof.addProperty("expected_cases", 10);
      proof.addProperty("actual_integrated_world", true);
      proof.addProperty("scope", "Native gameMode and input-gate gameplay; no physical OS input or visual rendering assertions");
      proof.add("cases", RESULTS.deepCopy());
      if (error != null) proof.addProperty("error", error.toString());
      Files.writeString(evidence, new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
   }

   private static void finish(Minecraft mc, Throwable error) {
      complete = true;
      mc.options.keyAttack.setDown(false);
      try {
         if (evidence != null) write(error == null ? "passed" : "failed", error);
      } catch (Exception writeError) {
         EngineersDecorReforged.LOGGER.error("IEDCTR_CLIENT_GAMEPLAY cannot write evidence", writeError);
      }
      if (error == null) EngineersDecorReforged.LOGGER.info("IEDCTR_CLIENT_GAMEPLAY PASSED nonce={} cases={}", nonce, RESULTS.size());
      else EngineersDecorReforged.LOGGER.error("IEDCTR_CLIENT_GAMEPLAY FAILED at " + phase, error);
      mc.stop();
   }

   private record ServerEvidence(BlockState state, int lamps, int decks, int toolDamage) {}

   private static final class FixtureScreen extends Screen {
      private FixtureScreen() { super(Component.literal("Isolated native client gameplay validation")); }
      @Override public boolean isPauseScreen() { return false; }
      @Override public boolean shouldCloseOnEsc() { return false; }
   }

   /** Passive observation only: every packet continues untouched through vanilla handlers. */
   private static final class Probe extends ChannelDuplexHandler {
      private final Minecraft minecraft;
      private volatile int latestSequence = -1;
      private volatile int ackApplied = -1;
      private volatile int miningPackets;
      private Probe(Minecraft minecraft) { this.minecraft = minecraft; }
      @Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
         if (message instanceof ServerboundPlayerActionPacket action && action.getPos().equals(POS)
            && (action.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
               || action.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK)) {
            latestSequence = Math.max(latestSequence, action.getSequence());
            miningPackets++;
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
