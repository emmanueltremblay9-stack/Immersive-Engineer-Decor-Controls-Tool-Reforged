package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import com.oblixorprime.engineersdecorreforged.client.ThermalVerticalScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.ThermalVerticalGeometry;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

/**
 * Opt-in native fixture. Only the nonce-guarded fresh-world harness may invoke it.
 * Mutations prepare fixtures on the server; inventory actions enter the real screen
 * with physical GUI coordinates and use vanilla packets. Captures are framebuffer
 * evidence, never CPU previews. A pass is a bounded smoke test, not full acceptance.
 */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class NativeFreezerReferenceValidation {
   private static final String SUITE = "native-freezer-reference-v1";
   private static final String NEUTRAL_SHA256 = "48d9d8a9eea6ffb477453bc7b84516b9f03be20132bff3b873d4d7b66e68b806";
   private static final String EFFECT_SHA256 = "046988bf2783a5396c43c6cbe949a32b3088a4c6cc9f59be0347fdd9611af957";
   private static final String ENERGY_SHA256 = "73403246b8e9389955a9cb5ff23ce8f478cbdb06af633c0feda7445dac355638";
   private static final double RASTER_PER_CANVAS_PIXEL = 3.0D;
   // Verified alpha bounds of the exact 1041x1143 Freezer energy/effect layers,
   // projected onto the 347x381 ThermalVerticalScreen canvas.
   private static final double ENERGY_LEFT = 187.0D / RASTER_PER_CANVAS_PIXEL;
   private static final double ENERGY_TOP = 371.0D / RASTER_PER_CANVAS_PIXEL;
   private static final double ENERGY_RIGHT = 216.0D / RASTER_PER_CANVAS_PIXEL;
   private static final double ENERGY_BOTTOM = 559.0D / RASTER_PER_CANVAS_PIXEL;
   private static final double EFFECT_LEFT = 331.0D / RASTER_PER_CANVAS_PIXEL;
   private static final double EFFECT_TOP = 324.0D / RASTER_PER_CANVAS_PIXEL;
   private static final double EFFECT_RIGHT = 720.0D / RASTER_PER_CANVAS_PIXEL;
   private static final double EFFECT_BOTTOM = 564.0D / RASTER_PER_CANVAS_PIXEL;
   private static final BlockPos POS = new BlockPos(0, 80, 0);
   private static final Component RESERVED_NAME = Component.literal("Freezer reserved recovery fixture");
   private enum Stage {
      SETUP, ENERGY, EMPTY_FOR_INPUT, PICKUP, RIGHT_INPUT, REJECT_OUTPUT, RETURN_INPUT,
      FULL_SETUP, RECOVERY_TOOLTIP, FULL_RECOVERY, ROOM_SETUP, RECOVER, REPEAT_RECOVERY, CLOSE, REOPEN, PROCESS, DONE
   }
   private static Stage stage;
   private static com.sun.jna.NativeLibrary x11, xtest;
   private static com.sun.jna.Pointer display;
   private static double pointerX, pointerY;
   private static JsonObject tooltipObservation;
   private static int tooltipFrames;
   private static boolean tooltipInFrame;
   private static final String RECOVERY_KEY = ThermalVerticalScreen.FREEZER_RECOVERY_HELP_KEY;
   private static int ticks;
   private static int energyIndex;
   private static int stableFrames;
   private static int previousFrostPhase;
   private static int frostStableFrames;
   private static MachineMenu renderingMenu;
   private static int renderingPhase;
   private static int renderingProgress;
   private static Exception renderFailure;
   private static final boolean[] FROST_SEEN = new boolean[4];
   private static int serverMenuId;
   private static int oldMenuId;
   private static Path directory;
   private static String nonce;
   private static UUID playerId;
   private static MachineBlockEntity machine;
   private static CompletableFuture<?> setup;
   private static CompletableFuture<Boolean> inspection;
   private static final JsonArray CAPTURES = new JsonArray();
   private static final JsonArray FROST_CAPTURES = new JsonArray();
   private static final JsonArray CASES = new JsonArray();

   private NativeFreezerReferenceValidation() { }

   static void begin(Minecraft mc, Path gameDirectory, String runNonce) throws Exception {
      require(System.getProperty(MachineMenu.DEV_FREEZER_LIVE_ENERGY_PROPERTY) == null
         && System.getProperty(MachineMenu.DEV_FREEZER_REFERENCE_PROPERTY) == null, "Fixture requires ordinary selection without dev selectors");
      require(mc.getSingleplayerServer() != null && mc.player != null
         && mc.player.connection.getConnection().isMemoryConnection(), "Only an isolated integrated server is permitted");
      require(gameDirectory.toRealPath().equals(mc.gameDirectory.toPath().toRealPath())
         && Files.readString(gameDirectory.resolve(".iedctr-client-gameplay-nonce")).equals(runNonce), "Fixture nonce/directory mismatch");
      require(System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Exact source revision is required");
      require(!Files.exists(gameDirectory.resolve("freezer-reference-screenshots")), "Refusing stale native captures");
      directory = gameDirectory;
      nonce = runNonce;
      playerId = mc.player.getUUID();
      tooltipFrames = 0; tooltipObservation = null; tooltipInFrame = false;
      while (CAPTURES.size() > 0) CAPTURES.remove(CAPTURES.size() - 1);
      while (CASES.size() > 0) CASES.remove(CASES.size() - 1);
      while (FROST_CAPTURES.size() > 0) FROST_CAPTURES.remove(FROST_CAPTURES.size() - 1);
      java.util.Arrays.fill(FROST_SEEN, false);
      previousFrostPhase = -1;
      frostStableFrames = 0;
      renderingMenu = null;
      renderFailure = null;
      energyIndex = 0;
      serverMenuId = -1;
      setup = mc.getSingleplayerServer().submit(() -> {
         ServerPlayer player = player(mc);
         player.setGameMode(GameType.SURVIVAL);
         player.getInventory().clearContent();
         player.getInventory().setItem(0, new ItemStack(Items.ICE, 2));
         var level = player.serverLevel();
         // Survival fixture support must exist on the server, not only client no-gravity.
         for (int x = -1; x <= 1; x++) for (int z = 2; z <= 4; z++) {
            level.setBlock(new BlockPos(x, 80, z), Blocks.STONE.defaultBlockState(), 3);
         }
         player.connection.teleport(0.5, 81, 3.5, 180, 0);
         level.setBlock(POS.below(), Blocks.STONE.defaultBlockState(), 3);
         level.setBlock(POS, ModBlocks.SMALL_FREEZER.get().defaultBlockState(), 3);
         require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Missing actual Freezer entity");
         machine = (MachineBlockEntity)level.getBlockEntity(POS);
         player.openMenu(machine);
         require(player.containerMenu instanceof MachineMenu menu && menu.kind() == MachineKind.SMALL_FREEZER
            && !menu.freezerReferencePresentation(), "Server must retain authoritative legacy presentation");
         serverMenuId = player.containerMenu.containerId;
         player.containerMenu.broadcastChanges();
      });
      enter(Stage.SETUP);
   }

   static boolean tick(Minecraft mc, Path gameDirectory, String runNonce) throws Exception {
      if (renderFailure != null) throw renderFailure;
      require(directory.equals(gameDirectory) && nonce.equals(runNonce), "Fixture identity changed");
      require(++ticks < 360, "Freezer fixture timed out at " + stage);
      if (stage == Stage.SETUP) {
         if (!ready()) return false;
         if (!(mc.screen instanceof ThermalVerticalScreen) || current(mc) == null) return false;
         verifyGeometry(mc);
         moveCursorAway(mc);
         enter(Stage.ENERGY);
         return false;
      }
      if (stage == Stage.CLOSE) {
         if (!serverMatches(mc, p -> p.containerMenu == p.inventoryMenu)) return false;
         casePassed("close_reaches_server");
         setup = mc.getSingleplayerServer().submit(() -> {
            ServerPlayer p = player(mc);
            p.openMenu(machine);
            serverMenuId = p.containerMenu.containerId;
            require(serverMenuId != oldMenuId, "Reopen must create a fresh menu identity");
         });
         enter(Stage.REOPEN);
         return false;
      }
      if (!ready()) return false;
      MachineMenu menu = current(mc);
      if (menu == null || !(mc.screen instanceof ThermalVerticalScreen screen)) return false;
      switch (stage) {
         case ENERGY -> {
            int expected = new int[]{0, 8000, 16000}[energyIndex];
            if (menu.energyStored() != expected) { stableFrames = 0; return false; }
            // Tick settling remains separate from the actual render-frame frost gate.
            if (++stableFrames < 8 || (energyIndex == 0 && !FROST_SEEN[0])) return false;
            require(menu.getSlot(0).getItem().isEmpty() && menu.freezerProgressTicks() == 0, "Energy frames must not fake processing");
            capture(mc, "energy-" + expected, menu);
            if (energyIndex == 0) {
               require(menu.freezerPhase() == 0, "Idle phase must be server zero");
               // Phase zero was captured by the coherent Render.Post gate.
            }
            if (++energyIndex < 3) {
               setup = mc.getSingleplayerServer().submit(() -> {
                  require(machine.energyStorage(null).receiveEnergy(8000, false) == 8000, "Real FE storage rejected fixture energy");
                  player(mc).containerMenu.broadcastChanges();
               });
               enter(Stage.ENERGY);
            } else {
               setup = mc.getSingleplayerServer().submit(() -> {
                  // Explicit fixture reset, not a client animation or a processing claim.
                  machine.dataAccessForTests().set(3, 0);
                  player(mc).containerMenu.broadcastChanges();
               });
               enter(Stage.EMPTY_FOR_INPUT);
            }
         }
         case EMPTY_FOR_INPUT -> {
            if (menu.energyStored() != 0 || ticks < 6) return false;
            clickSlot(screen, 30, 0);
            enter(Stage.PICKUP);
         }
         case PICKUP -> {
            if (!is(menu.getCarried(), Items.ICE, 2) || !menu.getSlot(30).getItem().isEmpty()
               || !serverMatches(mc, p -> is(p.containerMenu.getCarried(), Items.ICE, 2) && p.getInventory().getItem(0).isEmpty())) return false;
            casePassed("pickup_from_hotbar");
            clickSlot(screen, 0, 1);
            enter(Stage.RIGHT_INPUT);
         }
         case RIGHT_INPUT -> {
            if (!is(menu.getCarried(), Items.ICE, 1) || !is(menu.getSlot(0).getItem(), Items.ICE, 1)
               || !serverMatches(mc, p -> is(machine.getItem(0), Items.ICE, 1) && is(p.containerMenu.getCarried(), Items.ICE, 1))) return false;
            casePassed("right_click_places_one_input");
            clickSlot(screen, 2, 0);
            enter(Stage.REJECT_OUTPUT);
         }
         case REJECT_OUTPUT -> {
            if (ticks < 8 || !is(menu.getCarried(), Items.ICE, 1) || !menu.getSlot(2).getItem().isEmpty()
               || !serverMatches(mc, p -> is(p.containerMenu.getCarried(), Items.ICE, 1) && machine.getItem(2).isEmpty())) return false;
            casePassed("output_rejects_insertion");
            clickSlot(screen, 30, 0);
            enter(Stage.RETURN_INPUT);
         }
         case RETURN_INPUT -> {
            if (!menu.getCarried().isEmpty() || !is(menu.getSlot(30).getItem(), Items.ICE, 1)
               || !serverMatches(mc, p -> p.containerMenu.getCarried().isEmpty() && is(p.getInventory().getItem(0), Items.ICE, 1))) return false;
            casePassed("return_carried_stack");
            setup = mc.getSingleplayerServer().submit(() -> {
               ServerPlayer p = player(mc);
               for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
               ItemStack reserved = new ItemStack(Items.DIRT, 5);
               reserved.set(DataComponents.CUSTOM_NAME, RESERVED_NAME);
               machine.setItem(1, reserved);
               p.containerMenu.broadcastChanges();
            });
            enter(Stage.FULL_SETUP);
         }
         case FULL_SETUP -> {
            if (ticks < 8 || !screen.validationRecoveryVisible() || !reserved(menu.getSlot(1).getItem())) return false;
            for (int i = 3; i < 39; i++) if (!is(menu.getSlot(i).getItem(), Items.STONE, 64)) return false;
            if (Boolean.getBoolean("iedctr.freezerRecoveryTooltipProbe")) {
               require(screen.width == 320 && screen.height == 260 && mc.getWindow().getGuiScale() == 1, "Recovery probe requires 320x260 GUI scale 1");
               initNativeInput(mc);
               var view = screen.validationViewTransform();
               pointerX = view.screenX(screen.validationRecoveryButtonX() + screen.validationRecoveryButtonWidth() / 2.0D);
               pointerY = view.screenY(screen.validationRecoveryButtonY() + screen.validationRecoveryButtonHeight() / 2.0D);
               moveNativePointer(mc, screen);
               enter(Stage.RECOVERY_TOOLTIP);
            } else { clickRecovery(screen); enter(Stage.FULL_RECOVERY); }
         }
         case RECOVERY_TOOLTIP -> {
            require(screen.validationRecoveryVisible() && reserved(menu.getSlot(1).getItem()) && menu.getCarried().isEmpty(), "Recovery fixture changed during hover");
            if (ticks % 5 == 1) moveNativePointer(mc, screen);
            if (tooltipFrames < 3) return false;
            var proof = tooltipObservation.deepCopy();
            proof.addProperty("nonce", nonce); proof.addProperty("source_sha", System.getProperty("iedctr.executedSourceSha"));
            proof.addProperty("frames", tooltipFrames); proof.addProperty("language", mc.getLanguageManager().getSelected());
            proof.addProperty("expected_text", Component.translatable(RECOVERY_KEY).getString());
            var image = directory.resolve("freezer-recovery-tooltip.png");
            require(!Files.exists(image), "Refusing stale recovery capture");
            try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { frame.writeToFile(image); }
            proof.addProperty("capture_sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image))));
            Files.writeString(directory.resolve("freezer-recovery-tooltip.json"), new GsonBuilder().setPrettyPrinting().create().toJson(proof));
            closeNativeInput(); moveCursorAway(mc);
            clickRecovery(screen); enter(Stage.FULL_RECOVERY);
         }
         case FULL_RECOVERY -> {
            if (ticks < 8 || !reserved(menu.getSlot(1).getItem()) || !serverMatches(mc, p -> reserved(machine.getItem(1)) && countDirt(p) == 0
                  && p.containerMenu.getCarried().isEmpty() && droppedDirt(p) == 0)) return false;
            casePassed("full_inventory_preserves_reserved");
            setup = mc.getSingleplayerServer().submit(() -> {
               player(mc).getInventory().setItem(0, ItemStack.EMPTY);
               player(mc).containerMenu.broadcastChanges();
            });
            enter(Stage.ROOM_SETUP);
         }
         case ROOM_SETUP -> {
            if (ticks < 6 || !menu.getSlot(30).getItem().isEmpty()) return false;
            clickRecovery(screen);
            enter(Stage.RECOVER);
         }
         case RECOVER -> {
            if (!menu.getSlot(1).getItem().isEmpty() || !reserved(menu.getSlot(30).getItem())
               || !serverMatches(mc, p -> machine.getItem(1).isEmpty() && reserved(p.getInventory().getItem(0)) && countDirt(p) == 5
                  && p.containerMenu.getCarried().isEmpty() && droppedDirt(p) == 0)) return false;
            casePassed("recovery_preserves_count_and_components");
            enter(Stage.REPEAT_RECOVERY);
         }
         case REPEAT_RECOVERY -> {
            if (ticks == 3) {
               require(!screen.validationRecoveryVisible(), "Recovery control must disappear after extraction");
               clickRecoveryButtonCenter(screen); // Same coordinate after disappearance must not duplicate/drop anything.
            }
            if (ticks < 8 || !menu.getCarried().isEmpty()
               || !serverMatches(mc, p -> machine.getItem(1).isEmpty() && countDirt(p) == 5 && p.containerMenu.getCarried().isEmpty() && droppedDirt(p) == 0)) return false;
            casePassed("repeated_recovery_no_duplicate");
            oldMenuId = menu.containerId;
            screen.onClose();
            enter(Stage.CLOSE);
         }
         case REOPEN -> {
            if (ticks < 8 || menu.containerId == oldMenuId) return false;
            verifyGeometry(mc);
            require(is(menu.getSlot(0).getItem(), Items.ICE, 1) && menu.getSlot(1).getItem().isEmpty()
               && reserved(menu.getSlot(30).getItem()) && !screen.validationRecoveryVisible(), "Reopened inventory state was lost");
            casePassed("reopen_preserves_inventory_and_geometry");
            setup = mc.getSingleplayerServer().submit(() -> {
               require(machine.energyStorage(null).receiveEnergy(1440, false) == 1440, "Processing fixture requires exactly 1440 FE");
               player(mc).containerMenu.broadcastChanges();
            });
            enter(Stage.PROCESS);
         }
         case PROCESS -> {
            // Natural recipe captures only phases 1..3. The server sets phase 4 at
            // completion then resets it in the same tick: never invent a hold/frame.
            if (!menu.getSlot(0).getItem().isEmpty() || !is(menu.getSlot(2).getItem(), Items.PACKED_ICE, 1)
               || menu.energyStored() != 0 || menu.freezerProgressTicks() != 0
               || !serverMatches(mc, p -> machine.getItem(0).isEmpty() && is(machine.getItem(2), Items.PACKED_ICE, 1)
                  && machine.energyStorage(null).getEnergyStored() == 0 && countDirt(p) == 5)) return false;
            casePassed("real_recipe_consumes_1440_fe");
            for (boolean seen : FROST_SEEN) require(seen, "Natural recipe missed a frost phase capture");
            require(menu.freezerPhase() == 0, "Completed recipe must return to server phase zero");
            casePassed("natural_frost_phases_0_to_3_and_completion_reset");
            moveCursorAway(mc);
            enter(Stage.DONE);
         }
         case DONE -> {
            if (ticks < 8) return false;
            capture(mc, "completed-recipe", menu);
            writeProof(mc);
            return true;
         }
         default -> throw new IllegalStateException("Unexpected Freezer stage " + stage);
      }
      return false;
   }

   // Capture the frame that actually rendered this menu state, never a previous
   // framebuffer sampled during a burst of catch-up client ticks.
   @SubscribeEvent
   public static void beforeFrostRender(ScreenEvent.Render.Pre event) {
      renderingMenu = null;
      Minecraft mc = Minecraft.getInstance();
      if (directory == null || renderFailure != null || mc.getOverlay() != null
         || (stage != Stage.PROCESS && !(stage == Stage.ENERGY && energyIndex == 0))
         || !(event.getScreen() instanceof ThermalVerticalScreen) || event.getScreen() != mc.screen
         || setup != null) return;
      MachineMenu menu = current(mc);
      if (menu == null) return;
      renderingMenu = menu;
      renderingPhase = menu.freezerPhase();
      renderingProgress = menu.freezerProgressTicks();
   }

   @SubscribeEvent
   public static void afterFrostRender(ScreenEvent.Render.Post event) {
      Minecraft mc = Minecraft.getInstance();
      MachineMenu menu = renderingMenu;
      renderingMenu = null; // a Post without its matching Pre cannot count
      if (menu == null || event.getScreen() != mc.screen || mc.getOverlay() != null
         || current(mc) != menu || menu.freezerPhase() != renderingPhase
         || menu.freezerProgressTicks() != renderingProgress) {
         frostStableFrames = 0;
         return;
      }
      int phase = renderingPhase;
      boolean eligible = phase >= 0 && phase < 4
         && (stage == Stage.PROCESS ? phase > 0
            : stage == Stage.ENERGY && energyIndex == 0 && phase == 0 && menu.energyStored() == 0);
      if (!eligible) { frostStableFrames = 0; return; }
      if (phase != previousFrostPhase) { previousFrostPhase = phase; frostStableFrames = 0; }
      if (++frostStableFrames < 3 || FROST_SEEN[phase]) return;
      try {
         require(phase == 0 ? renderingProgress == 0
            : renderingProgress >= phase * 45 && renderingProgress < (phase + 1) * 45,
            "Rendered phase/progress disagree");
         // GuiGraphics batches draws; flush before reading the native target.
         event.getGuiGraphics().flush();
         capture(mc, "frost-phase-" + phase, menu);
         FROST_SEEN[phase] = true;
      } catch (Exception error) {
         renderFailure = error; // existing tick failure/reporting path remains authoritative
      }
   }

   private static void enter(Stage next) { stage = next; ticks = 0; stableFrames = 0; inspection = null; }
   private static boolean ready() {
      if (setup == null) return true;
      if (!setup.isDone()) return false;
      setup.join();
      setup = null;
      return true;
   }
   private static ServerPlayer player(Minecraft mc) {
      ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
      require(player != null, "Integrated player disappeared");
      return player;
   }
   private static MachineMenu current(Minecraft mc) {
      return mc.player != null && mc.player.containerMenu instanceof MachineMenu menu
         && menu.kind() == MachineKind.SMALL_FREEZER && menu.containerId == serverMenuId ? menu : null;
   }
   private static boolean serverMatches(Minecraft mc, Predicate<ServerPlayer> condition) {
      if (inspection == null) {
         inspection = mc.getSingleplayerServer().submit(() -> {
            ServerPlayer p = player(mc);
            // A transient duplicate must fail permanently, not become ready after falling away.
            require(droppedDirt(p) == 0, "Reserved items were duplicated or dropped into the world");
            return condition.test(p);
         });
         return false;
      }
      if (!inspection.isDone()) return false;
      boolean matches = inspection.join();
      inspection = null;
      return matches;
   }
   private static boolean is(ItemStack stack, net.minecraft.world.item.Item item, int count) {
      return stack.is(item) && stack.getCount() == count;
   }
   private static boolean reserved(ItemStack stack) {
      return is(stack, Items.DIRT, 5) && RESERVED_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
   }
   private static int countDirt(ServerPlayer player) {
      int count = 0;
      for (int i = 0; i < 36; i++) if (player.getInventory().getItem(i).is(Items.DIRT)) count += player.getInventory().getItem(i).getCount();
      return count;
   }
   private static int droppedDirt(ServerPlayer player) {
      int count = 0;
      for (ItemEntity item : player.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(8))) {
         if (item.getItem().is(Items.DIRT)) count += item.getItem().getCount();
      }
      return count;
   }
   private static void clickSlot(ThermalVerticalScreen screen, int index, int button) {
      require(index == 0 || index == 2 || index >= 3 && index <= 38,
         "Only visible Thermal Freezer slots may be clicked: " + index);
      var anchor = ThermalVerticalGeometry.slot(index, MachineKind.SMALL_FREEZER);
         click(screen, anchor.x() + 8, anchor.y() + 8, button);
   }
   private static void clickRecovery(ThermalVerticalScreen screen) {
      require(screen.validationRecoveryVisible(), "Reserved-item recovery control is not visible");
      clickRecoveryButtonCenter(screen);
   }
   private static void clickRecoveryButtonCenter(ThermalVerticalScreen screen) {
      click(screen, screen.validationRecoveryButtonX() + screen.validationRecoveryButtonWidth() / 2.0D,
         screen.validationRecoveryButtonY() + screen.validationRecoveryButtonHeight() / 2.0D, 0);
   }
   private static void click(ThermalVerticalScreen screen, double x, double y, int button) {
      var view = screen.validationViewTransform();
      // Exercise inverse-coordinate routing and vanilla click/release, not menu.clicked.
      screen.mouseClicked(view.screenX(x), view.screenY(y), button);
      screen.mouseReleased(view.screenX(x), view.screenY(y), button);
   }
   private static void verifyGeometry(Minecraft mc) {
      var screen = (ThermalVerticalScreen)mc.screen;
      var menu = current(mc);
      require(menu != null && menu.thermalVerticalPresentation() && menu.slots.size() == 39
         && menu.visibleMachineSlots() == 3 && !menu.getSlot(1).isActive()
         && menu.getSlot(0).isActive() && menu.getSlot(2).isActive(),
         "Thermal Freezer must retain 39 protocol slots, expose two machine slots, and hide only reserved index 1");
      ThermalVerticalGeometry.validate(MachineKind.SMALL_FREEZER);
      var view = screen.validationViewTransform();
      require(!screen.validationSlotHoveredAt(1, view.screenX(238), view.screenY(207)),
         "Reserved protocol slot 1 must not have a visible hover target");
      int visible = 0;
      for (int menuIndex = 0; menuIndex < menu.slots.size(); menuIndex++) {
         var slot = menu.getSlot(menuIndex);
         boolean shouldBeVisible = menuIndex != 1;
         require(slot.isActive() == shouldBeVisible, "Unexpected active state at menu slot " + menuIndex);
         if (!shouldBeVisible) continue;
         var anchor = ThermalVerticalGeometry.slot(menuIndex, MachineKind.SMALL_FREEZER);
         int expectedContainerIndex = menuIndex < 3 ? menuIndex
            : menuIndex < 30 ? menuIndex + 6 : menuIndex - 30;
         require(slot.getSlotIndex() == expectedContainerIndex && slot.x == anchor.x() && slot.y == anchor.y(),
            "Thermal slot mapping changed at menu index " + menuIndex);
         require(screen.validationSlotHoveredAt(menuIndex, view.screenX(slot.x + 8), view.screenY(slot.y + 8)),
            "Rendered Thermal slot center cannot be hovered at menu index " + menuIndex);
         visible++;
      }
      require(visible == ThermalVerticalGeometry.VISIBLE_SLOTS,
         "Thermal canvas must expose exactly 38 slots (2 machine + 36 inventory), got " + visible);
   }
   private static void initNativeInput(Minecraft mc) throws Exception {
      String nativeDisplay = System.getenv("DISPLAY");
      require(nativeDisplay != null && nativeDisplay.matches(":\\d+(\\.\\d+)?")
         && Files.readString(directory.resolve(".iedctr-freezer-xvfb")).equals(nonce + "\n" + nativeDisplay), "Native input requires runner-owned Xvfb");
      x11 = com.sun.jna.NativeLibrary.getInstance("X11"); xtest = com.sun.jna.NativeLibrary.getInstance("Xtst");
      display = x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null});
      require(display != null, "X11 unavailable"); GLFW.glfwFocusWindow(mc.getWindow().getWindow());
   }
   private static void closeNativeInput() {
      if (display != null) { x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display}); display = null; }
   }
   private static void moveNativePointer(Minecraft mc, ThermalVerticalScreen screen) {
      long window = mc.getWindow().getWindow();
      if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) { GLFW.glfwFocusWindow(window); return; }
      int[] x = {0}, y = {0}; GLFW.glfwGetWindowPos(window, x, y);
      int targetX = x[0] + (int)Math.round(pointerX * mc.getWindow().getScreenWidth() / (double)screen.width);
      int targetY = y[0] + (int)Math.round(pointerY * mc.getWindow().getScreenHeight() / (double)screen.height);
      require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{display, -1, targetX, targetY, new com.sun.jna.NativeLong(0)}) != 0, "Native pointer move failed");
      x11.getFunction("XFlush").invokeInt(new Object[]{display});
   }
    @net.neoforged.bus.api.SubscribeEvent public static void beforeTooltipRender(net.neoforged.neoforge.client.event.ScreenEvent.Render.Pre event){tooltipInFrame=false;}
    @net.neoforged.bus.api.SubscribeEvent public static void observeRecoveryTooltip(net.neoforged.neoforge.client.event.RenderTooltipEvent.Color event){
        if(stage!=Stage.RECOVERY_TOOLTIP)return;
        var mc=Minecraft.getInstance();if(!(mc.screen instanceof ThermalVerticalScreen screen)||screen.getSlotUnderMouse()!=null)return;
        var v=screen.validationViewTransform();
        double mx=mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth(),my=mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight();
        if (Math.abs(mx-pointerX)>1 || Math.abs(my-pointerY)>1 || !screen.validationRecoveryButtonHovered()) return;
        try{
            var record=new JsonObject();var lines=new JsonArray();var styles=new JsonArray();int width=0,height=event.getComponents().size()==1?-2:0;
            for(var component:event.getComponents()){
                net.minecraft.util.FormattedCharSequence seq=null;
                for(var field:component.getClass().getDeclaredFields())if(net.minecraft.util.FormattedCharSequence.class.isAssignableFrom(field.getType())){field.setAccessible(true);seq=(net.minecraft.util.FormattedCharSequence)field.get(component);break;}
                require(seq!=null,"Unobservable native tooltip component");var text=new StringBuilder();
                seq.accept((i,style,codepoint)->{text.appendCodePoint(codepoint);styles.add(style.toString());return true;});lines.add(text.toString());
                width=Math.max(width,component.getWidth(event.getFont()));height+=component.getHeight();
            }
            record.add("lines",lines);record.add("styles",styles);record.addProperty("tooltip_width",width);record.addProperty("tooltip_height",height);
            record.addProperty("x",event.getX());record.addProperty("y",event.getY());record.addProperty("viewport_width",screen.width);record.addProperty("viewport_height",screen.height);
            record.addProperty("in_bounds",event.getX()-4>=0&&event.getY()-4>=0&&event.getX()+width+4<=screen.width&&event.getY()+height+4<=screen.height);
            record.addProperty("translation_key", RECOVERY_KEY);
            record.addProperty("hovered_control", "freezer_recovery_button");
            record.addProperty("button_canvas_x", screen.validationRecoveryButtonX());
            record.addProperty("button_canvas_y", screen.validationRecoveryButtonY());
            record.addProperty("button_canvas_width", screen.validationRecoveryButtonWidth());
            record.addProperty("button_canvas_height", screen.validationRecoveryButtonHeight());
            record.addProperty("pointer_screen_x", mx); record.addProperty("pointer_screen_y", my);
            record.addProperty("pointer_canvas_x", v.localX(mx)); record.addProperty("pointer_canvas_y", v.localY(my));
            tooltipObservation=record;tooltipInFrame=true;
        }catch(Throwable error){throw new IllegalStateException("Recovery tooltip observation failed",error);}
    }
    @net.neoforged.bus.api.SubscribeEvent public static void afterTooltipRender(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        if (stage != Stage.RECOVERY_TOOLTIP) return;
        var mc = Minecraft.getInstance();
        if (tooltipInFrame && event.getScreen() == mc.screen && mc.getOverlay() == null) tooltipFrames++; else tooltipFrames = 0;
    }
   private static void moveCursorAway(Minecraft mc) {
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), mc.getWindow().getScreenWidth() - 2, mc.getWindow().getScreenHeight() - 2);
   }
   private record CropResult(int width, int height, String sha256) { }

   private static CropResult captureCanvasCrop(Minecraft mc, NativeImage frame, Path path,
                                               double left, double top, double right, double bottom) throws Exception {
      var screen = (ThermalVerticalScreen)mc.screen;
      var view = screen.validationViewTransform();
      double pixelX = (double)frame.getWidth() / mc.getWindow().getGuiScaledWidth();
      double pixelY = (double)frame.getHeight() / mc.getWindow().getGuiScaledHeight();
      int x0 = (int)Math.floor(view.screenX(left) * pixelX);
      int y0 = (int)Math.floor(view.screenY(top) * pixelY);
      int x1 = (int)Math.ceil(view.screenX(right) * pixelX);
      int y1 = (int)Math.ceil(view.screenY(bottom) * pixelY);
      require(x0 >= 0 && y0 >= 0 && x1 <= frame.getWidth() && y1 <= frame.getHeight() && x1 > x0 && y1 > y0,
         "Thermal artwork crop is outside the native framebuffer: " + path.getFileName());
      require(!Files.exists(path), "Refusing stale Thermal artwork crop: " + path.getFileName());
      try (var crop = new NativeImage(x1 - x0, y1 - y0, false)) {
         for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++)
            crop.setPixelRGBA(x - x0, y - y0, frame.getPixelRGBA(x, y));
         crop.writeToFile(path);
      }
      return new CropResult(x1 - x0, y1 - y0,
         HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
   }

   private static void capture(Minecraft mc, String label, MachineMenu menu) throws Exception {
      Path folder = directory.resolve("freezer-reference-screenshots");
      Files.createDirectories(folder);
      Path file = folder.resolve(label + ".png");
      require(!Files.exists(file), "Refusing to overwrite capture");
      JsonObject capture = new JsonObject();
      try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         frame.writeToFile(file);
         capture.addProperty("width", frame.getWidth());
         capture.addProperty("height", frame.getHeight());
         // Hash only exact active artwork bounds, so an animated world backdrop
         // cannot make a frozen gauge/effect pass merely because the full frame differs.
         Path energyFile = folder.resolve(label + "-energy.png");
         CropResult energyCrop = captureCanvasCrop(mc, frame, energyFile,
            ENERGY_LEFT, ENERGY_TOP, ENERGY_RIGHT, ENERGY_BOTTOM);
         capture.addProperty("energy_crop_name", energyFile.getFileName().toString());
         capture.addProperty("energy_crop_sha256", energyCrop.sha256());
         capture.addProperty("energy_crop_width", energyCrop.width());
         capture.addProperty("energy_crop_height", energyCrop.height());
         Path effectFile = folder.resolve(label + "-effect.png");
         CropResult effectCrop = captureCanvasCrop(mc, frame, effectFile,
            EFFECT_LEFT, EFFECT_TOP, EFFECT_RIGHT, EFFECT_BOTTOM);
         capture.addProperty("effect_crop_name", effectFile.getFileName().toString());
         capture.addProperty("effect_crop_sha256", effectCrop.sha256());
         capture.addProperty("effect_crop_width", effectCrop.width());
         capture.addProperty("effect_crop_height", effectCrop.height());
      }
      capture.addProperty("name", file.getFileName().toString());
      capture.addProperty("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
      capture.addProperty("energy", menu.energyStored());
      capture.addProperty("energy_capacity", menu.energyCapacity());
      capture.addProperty("energy_clip_top", ThermalVerticalGeometry.energyClipTop(89, 192, menu.energyStored(), menu.energyCapacity()));
      capture.addProperty("menu_id", menu.containerId);
      capture.addProperty("screen_class", mc.screen.getClass().getName());
      capture.addProperty("view_scale", ((ThermalVerticalScreen)mc.screen).validationViewTransform().scale());
      capture.addProperty("phase", menu.freezerPhase());
      capture.addProperty("progress_ticks", menu.freezerProgressTicks());
      if (label.startsWith("frost-phase-")) {
         capture.addProperty("rendered_frames", frostStableFrames);
         capture.addProperty("capture_event", "ScreenEvent.Render.Post");
         FROST_CAPTURES.add(capture);
      } else CAPTURES.add(capture);
   }
   private static void casePassed(String name) { CASES.add(name); }
   private static void writeProof(Minecraft mc) throws Exception {
      JsonObject proof = new JsonObject();
      proof.addProperty("suite", SUITE);
      proof.addProperty("selected_language", mc.getLanguageManager().getSelected());
      proof.addProperty("status", "passed_bounded_smoke");
      proof.addProperty("nonce", nonce);
      proof.addProperty("executed_source_sha", System.getProperty("iedctr.executedSourceSha"));
      proof.addProperty("actual_integrated_world", true);
      proof.addProperty("screen_class", ThermalVerticalScreen.class.getName());
      proof.addProperty("source_art_sha256", NEUTRAL_SHA256);
      proof.addProperty("effect_art_sha256", EFFECT_SHA256);
      proof.addProperty("energy_art_sha256", ENERGY_SHA256);
      proof.addProperty("canvas_width", ThermalVerticalGeometry.CANVAS_WIDTH);
      proof.addProperty("canvas_height", ThermalVerticalGeometry.CANVAS_HEIGHT);
      proof.addProperty("visible_machine_slots", 2);
      proof.addProperty("visible_slot_count", ThermalVerticalGeometry.VISIBLE_SLOTS);
      proof.addProperty("reserved_menu_index", 1);
      proof.addProperty("frost_art_static", false);
      proof.addProperty("presentation_mode", "normal-dynamic");
      proof.addProperty("dev_selectors_absent", true);
      proof.addProperty("effect_visibility_rule", "server_phase_greater_than_zero_single_effect_texture");
      proof.addProperty("phase_source", "server_menu_phase_natural_recipe");
      proof.addProperty("phase4_native_observed", false);
      proof.addProperty("menu_slot_count", current(mc).slots.size());
      proof.addProperty("native_visual_acceptance", false);
      proof.addProperty("input_method", "real Screen.mouseClicked/mouseReleased, vanilla packets, server observation; no physical OS input");
      proof.add("cases", CASES.deepCopy());
      proof.add("captures", CAPTURES.deepCopy());
      proof.add("frost_captures", FROST_CAPTURES.deepCopy());
      Files.writeString(directory.resolve("client-freezer-reference-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
   }
   private static void require(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
   }
}
