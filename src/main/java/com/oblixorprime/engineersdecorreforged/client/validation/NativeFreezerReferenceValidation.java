package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.FreezerReferenceScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceLiveState;
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
@net.neoforged.fml.common.EventBusSubscriber(modid = "immersive_engineer_decor_controls_tool_reforged", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class NativeFreezerReferenceValidation {
   private static final String SUITE = "native-freezer-reference-v1";
   private static final BlockPos POS = new BlockPos(0, 80, 0);
   private static final Component RESERVED_NAME = Component.literal("Freezer reserved recovery fixture");
   private enum Stage {
      SETUP, ENERGY, EMPTY_FOR_INPUT, PICKUP, RIGHT_INPUT, REJECT_OUTPUT, RETURN_INPUT,
      FULL_SETUP, FULL_HOVERS, FULL_RECOVERY, ROOM_SETUP, RECOVER, REPEAT_RECOVERY, CLOSE, REOPEN, PROCESS, DONE
   }
   private static final JsonArray FULL_HOVERS = new JsonArray();
   private static JsonObject fullOverview;
   private static int hoverIndex, hoverFrames, fullFrames;
   private static boolean tooltipInFrame;
   private static double pointerX, pointerY;
   private static Stage stage;
   private static int ticks;
   private static int energyIndex;
   private static int stableFrames;
   private static int previousFrostPhase;
   private static int frostStableFrames;
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
      while (FULL_HOVERS.size() > 0) FULL_HOVERS.remove(FULL_HOVERS.size() - 1);
      fullFrames = 0;
      directory = gameDirectory;
      nonce = runNonce;
      playerId = mc.player.getUUID();
      while (CAPTURES.size() > 0) CAPTURES.remove(CAPTURES.size() - 1);
      while (CASES.size() > 0) CASES.remove(CASES.size() - 1);
      while (FROST_CAPTURES.size() > 0) FROST_CAPTURES.remove(FROST_CAPTURES.size() - 1);
      java.util.Arrays.fill(FROST_SEEN, false);
      previousFrostPhase = -1;
      frostStableFrames = 0;
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
      require(directory.equals(gameDirectory) && nonce.equals(runNonce), "Fixture identity changed");
      require(++ticks < 360, "Freezer fixture timed out at " + stage);
      if (stage == Stage.SETUP) {
         if (!ready()) return false;
         if (!(mc.screen instanceof FreezerReferenceScreen) || current(mc) == null) return false;
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
      if (menu == null || !(mc.screen instanceof FreezerReferenceScreen screen)) return false;
      switch (stage) {
         case ENERGY -> {
            int expected = new int[]{0, 8000, 16000}[energyIndex];
            if (menu.energyStored() != expected) { stableFrames = 0; return false; }
            // Wait for rendered frames after the synchronized state, not since submission.
            if (++stableFrames < 8) return false;
            require(menu.getSlot(0).getItem().isEmpty() && menu.freezerProgressTicks() == 0, "Energy frames must not fake processing");
            capture(mc, "energy-" + expected, menu);
            if (energyIndex == 0) {
               require(menu.freezerPhase() == 0, "Idle phase must be server zero");
               capture(mc, "frost-phase-0", menu);
               FROST_SEEN[0] = true;
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
            if (Boolean.getBoolean("iedctr.freezerFullInventory")) {
               if (fullFrames < 3) return false;
               fullOverview = fullCapture(mc, "inventory-full");
               hoverIndex = 3;
               enter(Stage.FULL_HOVERS);
               aim(mc, screen);
            } else {
               clickRecovery(screen);
               enter(Stage.FULL_RECOVERY);
            }
         }
         case FULL_HOVERS -> {
            if (hoverFrames < 3) return false;
            require(menu.getCarried().isEmpty(), "Hover changed carried stack");
            for (int i = 3; i < 39; i++) require(is(menu.getSlot(i).getItem(), Items.STONE, 64), "Hover changed inventory");
            var slot = screen.getSlotUnderMouse();
            require(slot == menu.getSlot(hoverIndex) && is(slot.getItem(), Items.STONE, 64), "Actual hovered slot differs");
            JsonObject record = fullCapture(mc, "inventory-slot-" + hoverIndex);
            record.addProperty("menu_index", hoverIndex);
            record.addProperty("container_index", slot.getSlotIndex());
            record.addProperty("count", slot.getItem().getCount());
            record.addProperty("item", "minecraft:stone");
            record.addProperty("rendered_frames", hoverFrames);
            record.addProperty("pointer_x", pointerX); record.addProperty("pointer_y", pointerY);
            FULL_HOVERS.add(record);
            if (++hoverIndex < 39) { ticks = 0; aim(mc, screen); }
            else { moveCursorAway(mc); clickRecovery(screen); enter(Stage.FULL_RECOVERY); }
         }
         case FULL_RECOVERY -> {
            if (ticks < 8 || !reserved(menu.getSlot(1).getItem()) || !serverMatches(mc, p -> reserved(machine.getItem(1)) && countDirt(p) == 0
                  && p.containerMenu.getCarried().isEmpty() && droppedDirt(p) == 0
                  && java.util.stream.IntStream.range(0, 36).allMatch(i -> is(p.getInventory().getItem(i), Items.STONE, 64)))) return false;
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
               clickRecovery(screen); // Same coordinate after disappearance must not duplicate/drop anything.
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
            int phase = menu.freezerPhase();
            if (phase != previousFrostPhase) { previousFrostPhase = phase; frostStableFrames = 0; }
            if (phase > 0 && phase < 4 && !FROST_SEEN[phase] && ++frostStableFrames >= 3) {
               require(menu.freezerProgressTicks() >= phase * 45
                  && menu.freezerProgressTicks() < (phase + 1) * 45, "Phase/progress disagree");
               capture(mc, "frost-phase-" + phase, menu);
               FROST_SEEN[phase] = true;
            }
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
   private static void clickSlot(FreezerReferenceScreen screen, int index, int button) {
      var anchor = FreezerReferenceGeometry.anchor(index);
      click(screen, anchor.x() + 8, anchor.y() + 8, button);
   }
   private static void clickRecovery(FreezerReferenceScreen screen) { click(screen, 157, 93, 0); }
   private static void click(FreezerReferenceScreen screen, double x, double y, int button) {
      var view = screen.validationViewTransform();
      // Exercise inverse-coordinate routing and vanilla click/release, not menu.clicked.
      screen.mouseClicked(view.screenX(x), view.screenY(y), button);
      screen.mouseReleased(view.screenX(x), view.screenY(y), button);
   }
   private static void verifyGeometry(Minecraft mc) {
      var screen = (FreezerReferenceScreen)mc.screen;
      var menu = current(mc);
      require(menu != null && menu.freezerReferencePresentation() && menu.slots.size() == 39
         && menu.visibleMachineSlots() == 3 && !menu.getSlot(1).isActive(), "Actual client must retain all 39 protocol slots and hide only reserved slot 1");
      var view = screen.validationViewTransform();
      for (var anchor : FreezerReferenceGeometry.SLOT_ANCHORS) {
         var slot = menu.getSlot(anchor.menuIndex());
         int expectedContainerIndex = anchor.menuIndex() < 3 ? anchor.menuIndex()
            : anchor.menuIndex() < 30 ? anchor.menuIndex() + 6 : anchor.menuIndex() - 30;
         require(slot.getSlotIndex() == expectedContainerIndex && slot.x == anchor.x() && slot.y == anchor.y(), "Slot mapping changed at " + anchor.menuIndex());
         require(screen.validationSlotHoveredAt(anchor.menuIndex(), view.screenX(slot.x + 8), view.screenY(slot.y + 8)), "Rendered slot center cannot be hovered");
      }
   }
   private static void aim(Minecraft mc, FreezerReferenceScreen screen) {
      hoverFrames = 0; tooltipInFrame = false;
      var slot = current(mc).getSlot(hoverIndex); var view = screen.validationViewTransform();
      pointerX = view.screenX(slot.x + 8); pointerY = view.screenY(slot.y + 8);
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), pointerX * mc.getWindow().getScreenWidth() / screen.width,
         pointerY * mc.getWindow().getScreenHeight() / screen.height);
   }
   @net.neoforged.bus.api.SubscribeEvent
   public static void beforeFullRender(net.neoforged.neoforge.client.event.ScreenEvent.Render.Pre event) { tooltipInFrame = false; }
   @net.neoforged.bus.api.SubscribeEvent
   public static void fullTooltip(net.neoforged.neoforge.client.event.RenderTooltipEvent.Color event) {
      var mc = Minecraft.getInstance();
      if (stage != Stage.FULL_HOVERS || !(mc.screen instanceof FreezerReferenceScreen screen)) return;
      var menu = current(mc);
      if (menu == null || screen.getSlotUnderMouse() != menu.getSlot(hoverIndex)) return;
      if (Math.abs(mc.mouseHandler.xpos() * screen.width / mc.getWindow().getScreenWidth() - pointerX) >= 1
         || Math.abs(mc.mouseHandler.ypos() * screen.height / mc.getWindow().getScreenHeight() - pointerY) >= 1) return;
      // Item tooltip events must concern the actual synchronized stack, not a control tooltip.
      tooltipInFrame = is(event.getItemStack(), Items.STONE, 64) && !event.getComponents().isEmpty();
   }
   @net.neoforged.bus.api.SubscribeEvent
   public static void afterFullRender(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
      if (stage == Stage.FULL_SETUP) {
         var mc = Minecraft.getInstance(); var menu = current(mc);
         if (event.getScreen() == mc.screen && mc.screen instanceof FreezerReferenceScreen && menu != null
            && java.util.stream.IntStream.range(3,39).allMatch(i -> is(menu.getSlot(i).getItem(), Items.STONE,64))) fullFrames++;
         else fullFrames = 0;
      }
      if (stage != Stage.FULL_HOVERS) return;
      if (tooltipInFrame && event.getScreen() == Minecraft.getInstance().screen) hoverFrames++;
      else hoverFrames = 0;
      tooltipInFrame = false;
   }
   private static JsonObject fullCapture(Minecraft mc, String name) throws Exception {
      var folder = directory.resolve("freezer-full-inventory-screenshots"); Files.createDirectories(folder);
      var file = folder.resolve(name + ".png"); require(!Files.exists(file), "Stale full-inventory capture");
      var record = new JsonObject();
      try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         image.writeToFile(file); record.addProperty("width", image.getWidth()); record.addProperty("height", image.getHeight());
      }
      record.addProperty("name", file.getFileName().toString());
      record.addProperty("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
      return record;
   }
   private static void moveCursorAway(Minecraft mc) {
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), mc.getWindow().getScreenWidth() - 2, mc.getWindow().getScreenHeight() - 2);
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
         // Hash only native meter pixels too: an animated world backdrop must not
         // make a frozen/static gauge pass merely because full screenshots differ.
         var view = ((FreezerReferenceScreen)mc.screen).validationViewTransform();
         var meter = FreezerReferenceLiveState.ENERGY_HIT_REGION;
         double factor = (double)FreezerReferenceGeometry.CANVAS_WIDTH / FreezerReferenceGeometry.SOURCE_WIDTH;
         double pixelX = (double)frame.getWidth() / mc.getWindow().getGuiScaledWidth();
         double pixelY = (double)frame.getHeight() / mc.getWindow().getGuiScaledHeight();
         int x0 = (int)Math.floor(view.screenX(meter.x() * factor) * pixelX);
         int y0 = (int)Math.floor(view.screenY(meter.y() * factor) * pixelY);
         int x1 = (int)Math.ceil(view.screenX(meter.right() * factor) * pixelX);
         int y1 = (int)Math.ceil(view.screenY(meter.bottom() * factor) * pixelY);
         Path meterFile = folder.resolve(label + "-meter.png");
         require(!Files.exists(meterFile), "Refusing to overwrite native meter crop");
         try (var crop = new NativeImage(x1 - x0, y1 - y0, false)) {
            for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
               crop.setPixelRGBA(x - x0, y - y0, frame.getPixelRGBA(x, y));
            }
            crop.writeToFile(meterFile);
         }
         capture.addProperty("meter_name", meterFile.getFileName().toString());
         capture.addProperty("meter_sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(meterFile))));
         capture.addProperty("meter_width", x1 - x0);
         capture.addProperty("meter_height", y1 - y0);
         // Conservative interior glass crop only, excluding animated world and FE.
         int fx0 = (int)Math.floor(view.screenX(460 * factor) * pixelX);
         int fy0 = (int)Math.floor(view.screenY(390 * factor) * pixelY);
         int fx1 = (int)Math.ceil(view.screenX(795 * factor) * pixelX);
         int fy1 = (int)Math.ceil(view.screenY(555 * factor) * pixelY);
         Path frostFile = folder.resolve(label + "-frost.png");
         require(!Files.exists(frostFile), "Refusing stale frost crop");
         try (var crop = new NativeImage(fx1 - fx0, fy1 - fy0, false)) {
            for (int y = fy0; y < fy1; y++) for (int x = fx0; x < fx1; x++)
               crop.setPixelRGBA(x - fx0, y - fy0, frame.getPixelRGBA(x, y));
            crop.writeToFile(frostFile);
         }
         capture.addProperty("frost_name", frostFile.getFileName().toString());
         capture.addProperty("frost_sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(frostFile))));
         capture.addProperty("frost_width", fx1 - fx0);
         capture.addProperty("frost_height", fy1 - fy0);
      }
      capture.addProperty("name", file.getFileName().toString());
      capture.addProperty("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
      capture.addProperty("energy", menu.energyStored());
      capture.addProperty("lit_cells", FreezerReferenceLiveState.energy(menu.energyStored(), menu.energyCapacity()).litCells());
      capture.addProperty("menu_id", menu.containerId);
      capture.addProperty("screen_class", mc.screen.getClass().getName());
      capture.addProperty("view_scale", ((FreezerReferenceScreen)mc.screen).validationViewTransform().scale());
      capture.addProperty("phase", menu.freezerPhase());
      capture.addProperty("progress_ticks", menu.freezerProgressTicks());
      if (label.startsWith("frost-phase-")) FROST_CAPTURES.add(capture); else CAPTURES.add(capture);
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
      proof.addProperty("source_art_sha256", FreezerReferenceGeometry.SOURCE_SHA256);
      proof.addProperty("frost_art_static", false);
      proof.addProperty("presentation_mode", "normal-dynamic");
      proof.addProperty("dev_selectors_absent", true);
      proof.addProperty("decorative_rim_frost_static", true);
      proof.addProperty("phase_source", "server_menu_phase_natural_recipe");
      proof.addProperty("phase4_native_observed", false);
      proof.addProperty("menu_slot_count", current(mc).slots.size());
      proof.addProperty("native_visual_acceptance", false);
      proof.addProperty("input_method", "real Screen.mouseClicked/mouseReleased, vanilla packets, server observation; no physical OS input");
      if (Boolean.getBoolean("iedctr.freezerFullInventory")) {
         proof.add("full_inventory_overview", fullOverview);
         proof.add("full_inventory_hovers", FULL_HOVERS.deepCopy());
         proof.addProperty("full_inventory_input", "GLFW cursor positioning and native tooltip/render events; not physical OS input");
         proof.addProperty("durability_verified", false);
      }
      proof.add("cases", CASES.deepCopy());
      proof.add("captures", CAPTURES.deepCopy());
      proof.add("frost_captures", FROST_CAPTURES.deepCopy());
      Files.writeString(directory.resolve("client-freezer-reference-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
   }
   private static void require(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
   }
}
