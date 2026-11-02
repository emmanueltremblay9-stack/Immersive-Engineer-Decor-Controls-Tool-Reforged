package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.SmallMineralSmelterHybridScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

/** Opt-in native-client fixture for the real Small Mineral Smelter screen and menu. */
final class NativeSmelterGuiValidation {
   private static final BlockPos POS = new BlockPos(0, 80, 0);
   private static final String PREFIX = "gui.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter.";
   private static final String SUITE = "native-smelter-gui-v1";
   private static final int PANEL_WIDTH = 234;
   private static final int PANEL_HEIGHT = 270;

   private enum Stage {
      WAIT_SERVER, WAIT_SCREEN, WAIT_PROGRESS_ZERO, WAIT_PROGRESS_MID, WAIT_PROGRESS_FULL,
      WAIT_PROGRESS_RESTORE, WAIT_DRAWER_RENDER
   }
   private static Stage stage;
   private static CompletableFuture<?> pending;
   private static int ticks;
   private static int serverMenuId = -1;
   private static MachineBlockEntity serverMachine;
   private static boolean serverMenuContextValid;
   private static boolean outputSlotHiddenWhenClosed;
   private static String serverMenuClass;
   private static Path gameDirectory;
   private static Path screenshots;
   private static String nonce;
   private static String executedSourceSha;
   private static String pullRequestHeadSha;
   private static String language;
   private static final JsonArray CAPTURES = new JsonArray();

   private NativeSmelterGuiValidation() {}

   static void begin(Minecraft mc, Path directory, String runNonce) {
      require(mc.getSingleplayerServer() != null && mc.player != null, "Native Smelter fixture requires an integrated player");
      gameDirectory = directory;
      nonce = runNonce;
      executedSourceSha = System.getProperty("iedctr.executedSourceSha", "");
      pullRequestHeadSha = System.getProperty("iedctr.pullRequestHeadSha", "");
      language = System.getProperty("iedctr.smelterLanguage", "");
      require(executedSourceSha.matches("[0-9a-f]{40}"), "Fixture requires the executed source SHA");
      require(pullRequestHeadSha.matches("[0-9a-f]{40}"), "Fixture requires the exact PR head SHA");
      require(language.equals("en_us") || language.equals("fr_fr"), "Fixture language must be en_us or fr_fr");
      screenshots = gameDirectory.resolve("smelter-gui-screenshots");
      require(!Files.exists(screenshots), "Refusing stale Smelter screenshots");
      while (CAPTURES.size() > 0) {
         CAPTURES.remove(CAPTURES.size() - 1);
      }
      ticks = 0;
      serverMenuId = -1;
      serverMachine = null;
      serverMenuContextValid = false;
      outputSlotHiddenWhenClosed = false;
      serverMenuClass = "";
      UUID playerId = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null, "Integrated server player disappeared before Smelter fixture setup");
         var level = player.serverLevel();
         player.setGameMode(GameType.CREATIVE);
         player.getInventory().clearContent();
         player.connection.teleport(POS.getX() + 0.5, POS.getY() + 1.0, POS.getZ() + 3.5, 180.0F, 0.0F);
         level.setBlock(POS.below(), Blocks.STONE.defaultBlockState(), 3);
         level.setBlock(POS, ModBlocks.SMALL_MINERAL_SMELTER.get().defaultBlockState(), 3);
         if (!(level.getBlockEntity(POS) instanceof MachineBlockEntity machine)
            || machine.kind() == null || !machine.getBlockState().is(ModBlocks.SMALL_MINERAL_SMELTER.get())) {
            throw new IllegalStateException("Fixture did not create the Small Mineral Smelter block entity");
         }
         serverMachine = machine;
         ItemStack input = new ItemStack(Items.COBBLESTONE);
         require(machine.canPlaceItem(0, input), "Fixture mineral input is not accepted");
         machine.setItem(0, input);
         machine.setItem(2, new ItemStack(Items.MAGMA_BLOCK, 2));
         require(machine.fluidHandler(Direction.UP).fill(new FluidStack(Fluids.LAVA, 2500), FluidAction.EXECUTE) == 2500,
            "Fixture lava did not fill the top input");
         require(machine.energyStorage(null).receiveEnergy(12000, false) == 12000,
            "Fixture FE did not fill the real energy storage");
         player.openMenu(machine);
         if (!(player.containerMenu instanceof MachineMenu menu)
            || !menu.isMineralSmelterMenuFor(machine)
            || !menu.kind().name().equals("SMALL_MINERAL_SMELTER")
            || menu.visibleMachineSlots() != 2 || menu.slots.size() != 38
            || menu.getSlot(0).getSlotIndex() != 0 || menu.getSlot(1).getSlotIndex() != 2
            || !menu.stillValid(player) || !menu.getSlot(1).isActive()
            || !menu.getSlot(0).getItem().is(Items.COBBLESTONE)
            || !menu.getSlot(1).getItem().is(Items.MAGMA_BLOCK)) {
            throw new IllegalStateException("Server did not open the real Smelter menu with the expected slots");
         }
         serverMenuId = menu.containerId;
         serverMenuContextValid = true;
         serverMenuClass = menu.getClass().getName();
         menu.broadcastChanges();
      });
      stage = Stage.WAIT_SERVER;
   }

   static boolean tick(Minecraft mc, Path directory, String runNonce) throws Exception {
      if (stage == Stage.WAIT_SERVER) {
         if (++ticks > 300) throw new IllegalStateException("Integrated server did not prepare the Smelter menu");
         if (!pending.isDone()) return false;
         pending.join();
         ticks = 0;
         stage = Stage.WAIT_SCREEN;
         return false;
      }
      if (stage == Stage.WAIT_SCREEN) {
         if (++ticks > 300) throw new IllegalStateException("Real Smelter screen did not open or synchronize");
         if (!(mc.screen instanceof SmallMineralSmelterHybridScreen screen)) return false;
         if (!(mc.player.containerMenu instanceof MachineMenu menu)) return false;
         if (screen.validationOutputDrawerOpen()) {
            throw new IllegalStateException("Smelter output drawer must start closed");
         }
         if (!menu.kind().name().equals("SMALL_MINERAL_SMELTER")
            || menu.containerId != serverMenuId || !serverMenuContextValid
            || menu.visibleMachineSlots() != 2 || menu.slots.size() != 38
            || menu.getSlot(0).getSlotIndex() != 0 || menu.getSlot(1).getSlotIndex() != 2) {
            return false;
         }
         if ((menu.field(MachineMenu.SMS_UI_FLAGS) & 3) != 3
            || menu.field(MachineMenu.SMS_UI_SELECTED_MODE) != 0
            || menu.field(MachineMenu.SMS_UI_BATCH_MODE) != 0
            || menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS) <= 0
            || menu.field(MachineMenu.SMS_UI_THERMAL) <= 0
            || menu.field(MachineMenu.SMS_UI_TANK_AMOUNT) <= 0
            || menu.field(MachineMenu.SMS_UI_ENERGY) <= 0
            || menu.field(MachineMenu.SMS_UI_FE_RATE) != 16
            || menu.field(MachineMenu.SMS_UI_STATUS) != 1
            || !menu.getSlot(0).getItem().is(Items.COBBLESTONE)
            || !menu.getSlot(1).getItem().is(Items.MAGMA_BLOCK)
            || menu.getSlot(1).getItem().getCount() != 2) {
            return false;
         }
         if (ticks < 30) return false;
         moveCursorOutsidePanel(mc);
         scheduleProgressFixture(mc, 0, 0, Stage.WAIT_PROGRESS_ZERO);
         return false;
      }
      if (stage == Stage.WAIT_PROGRESS_ZERO) {
         if (++ticks > 120) {
            MachineMenu menu = currentFixtureMenu(mc);
            String state = menu == null ? "menu=missing" : "menuProgress=" + menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS)
               + ", menuEnergy=" + menu.field(MachineMenu.SMS_UI_ENERGY)
               + ", menuStatus=" + menu.field(MachineMenu.SMS_UI_STATUS);
            throw new IllegalStateException("Smelter zero-progress fixture did not synchronize: " + state);
         }
         if (!pending.isDone()) return false;
         pending.join();
         MachineMenu menu = currentFixtureMenu(mc);
         if (menu == null || menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS) != 0
            || menu.field(MachineMenu.SMS_UI_ENERGY) != 0 || menu.field(MachineMenu.SMS_UI_STATUS) != 3) return false;
         moveCursorOutsidePanel(mc);
         capture(mc, menu, "progress-empty.png");
         scheduleProgressFixture(mc, 50, 0, Stage.WAIT_PROGRESS_MID);
         return false;
      }
      if (stage == Stage.WAIT_PROGRESS_MID) {
         if (++ticks > 120) throw new IllegalStateException("Smelter intermediate-progress fixture did not synchronize");
         if (!pending.isDone()) return false;
         pending.join();
         MachineMenu menu = currentFixtureMenu(mc);
         if (menu == null || menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS) != 50
            || menu.field(MachineMenu.SMS_UI_ENERGY) != 0 || menu.field(MachineMenu.SMS_UI_STATUS) != 3) return false;
         capture(mc, menu, "progress-mid.png");
         scheduleProgressFixture(mc, 90, 0, Stage.WAIT_PROGRESS_FULL);
         return false;
      }
      if (stage == Stage.WAIT_PROGRESS_FULL) {
         if (++ticks > 120) throw new IllegalStateException("Smelter full-progress fixture did not synchronize");
         if (!pending.isDone()) return false;
         pending.join();
         MachineMenu menu = currentFixtureMenu(mc);
         if (menu == null || menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS) != 90
            || menu.field(MachineMenu.SMS_UI_ENERGY) != 0 || menu.field(MachineMenu.SMS_UI_STATUS) != 3) return false;
         capture(mc, menu, "progress-full.png");
         scheduleProgressFixture(mc, 50, 12000, Stage.WAIT_PROGRESS_RESTORE);
         return false;
      }
      if (stage == Stage.WAIT_PROGRESS_RESTORE) {
         if (++ticks > 180) throw new IllegalStateException("Smelter active fixture did not resume after progress captures");
         if (!pending.isDone()) return false;
         pending.join();
         if (!(mc.screen instanceof SmallMineralSmelterHybridScreen screen)) return false;
         MachineMenu menu = currentFixtureMenu(mc);
         if (menu == null || menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS) <= 0
            || menu.field(MachineMenu.SMS_UI_ENERGY) <= 0
            || menu.field(MachineMenu.SMS_UI_FE_RATE) != 16 || menu.field(MachineMenu.SMS_UI_STATUS) != 1) return false;
         if (ticks < 30) return false;
         outputSlotHiddenWhenClosed = !menu.getSlot(1).isActive();
         require(outputSlotHiddenWhenClosed, "Output item slot is not hidden while the drawer is closed");
         mc.options.hideGui = false;
         moveCursorOutsidePanel(mc);
         capture(mc, menu, "screen-closed.png");
         double scale = screen.validationViewScale();
         double guiWidth = mc.getWindow().getGuiScaledWidth();
         double guiHeight = mc.getWindow().getGuiScaledHeight();
         double originX = (guiWidth - PANEL_WIDTH * scale) / 2.0;
         double originY = (guiHeight - PANEL_HEIGHT * scale) / 2.0;
         double drawerButtonX = originX + 189.0 * scale;
         double drawerButtonY = originY + 39.0 * scale;
         require(screen.mouseClicked(drawerButtonX, drawerButtonY, 0), "Output drawer button did not receive the click");
         screen.mouseReleased(drawerButtonX, drawerButtonY, 0);
         require(screen.validationOutputDrawerOpen() && menu.getSlot(1).isActive(),
            "The native output drawer did not reveal the real output slot");
         ticks = 0;
         stage = Stage.WAIT_DRAWER_RENDER;
         return false;
      }
      if (stage == Stage.WAIT_DRAWER_RENDER) {
         if (++ticks < 20) return false;
         if (!(mc.screen instanceof SmallMineralSmelterHybridScreen screen)
            || !(mc.player.containerMenu instanceof MachineMenu menu)
            || !screen.validationOutputDrawerOpen() || !menu.getSlot(1).isActive()
            || !menu.getSlot(1).getItem().is(Items.MAGMA_BLOCK)
            || menu.getSlot(1).getItem().getCount() != 2) {
            throw new IllegalStateException("Output drawer lost its real item or visible slot before capture");
         }
         capture(mc, menu, "drawer-open.png");
         writeProof(mc, screen, menu, runNonce);
         return true;
      }
      throw new IllegalStateException("Unexpected Smelter GUI fixture stage: " + stage);
   }

   private static MachineMenu currentFixtureMenu(Minecraft mc) {
      if (mc.player == null || !(mc.player.containerMenu instanceof MachineMenu menu)
         || !menu.kind().name().equals("SMALL_MINERAL_SMELTER") || menu.containerId != serverMenuId) return null;
      return menu;
   }

   private static void scheduleProgressFixture(Minecraft mc, int progress, int energy, Stage waitStage) {
      UUID playerId = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null && serverMachine != null, "Smelter fixture lost its live machine");
         serverMachine.dataAccessForTests().set(1, progress);
         serverMachine.dataAccessForTests().set(6, energy);
         serverMachine.setChanged();
         if (player.containerMenu instanceof MachineMenu menu) menu.broadcastChanges();
      });
      stage = waitStage;
      ticks = 0;
   }

   private static void moveCursorOutsidePanel(Minecraft mc) {
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), mc.getWindow().getWidth() - 3, mc.getWindow().getHeight() - 3);
   }

   private static void capture(Minecraft mc, MachineMenu menu, String name) throws Exception {
      Files.createDirectories(screenshots);
      Path output = screenshots.resolve(name);
      if (Files.exists(output)) throw new IllegalStateException("Refusing to overwrite a Smelter capture");
      try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         image.writeToFile(output);
      }
      JsonObject capture = new JsonObject();
      capture.addProperty("name", name);
      capture.addProperty("sha256", HexFormat.of().formatHex(
         MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(output))));
      capture.addProperty("width", mc.getWindow().getWidth());
      capture.addProperty("height", mc.getWindow().getHeight());
      capture.addProperty("batch_progress", menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS));
      capture.addProperty("energy", menu.field(MachineMenu.SMS_UI_ENERGY));
      capture.addProperty("status", menu.field(MachineMenu.SMS_UI_STATUS));
      CAPTURES.add(capture);
   }

   private static void writeProof(Minecraft mc, SmallMineralSmelterHybridScreen screen,
                                  MachineMenu menu, String runNonce) throws Exception {
      require(nonce.equals(runNonce), "Native Smelter fixture nonce changed during the run");
      JsonObject proof = new JsonObject();
      proof.addProperty("suite", SUITE);
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", "passed");
      proof.addProperty("actual_integrated_world", mc.getSingleplayerServer() != null
         && mc.player.connection.getConnection().isMemoryConnection());
      proof.addProperty("executed_source_sha", executedSourceSha);
      proof.addProperty("pull_request_head_sha", pullRequestHeadSha);
      proof.addProperty("language", language);
      proof.addProperty("translated_lava_mode", Component.translatable(PREFIX + "mode.lava").getString());
      proof.addProperty("screen_identity", mc.screen.getClass().getName());
      proof.addProperty("client_menu_identity", menu.getClass().getName());
      proof.addProperty("server_menu_identity", serverMenuClass);
      proof.addProperty("machine_kind", menu.kind().name());
      proof.addProperty("client_menu_id", menu.containerId);
      proof.addProperty("server_menu_id", serverMenuId);
      proof.addProperty("server_menu_context_valid", serverMenuContextValid);
      proof.addProperty("visible_machine_slots", menu.visibleMachineSlots());
      proof.addProperty("total_slots", menu.slots.size());
      JsonArray backingSlots = new JsonArray();
      backingSlots.add(menu.getSlot(0).getSlotIndex());
      backingSlots.add(menu.getSlot(1).getSlotIndex());
      proof.add("machine_slot_backing_indices", backingSlots);
      proof.addProperty("data_fields", MachineMenu.SMALL_MINERAL_SMELTER_V05_DATA_COUNT);

      JsonObject state = new JsonObject();
      state.addProperty("selected_mode", menu.field(MachineMenu.SMS_UI_SELECTED_MODE));
      state.addProperty("batch_mode", menu.field(MachineMenu.SMS_UI_BATCH_MODE));
      state.addProperty("batch_progress", menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS));
      state.addProperty("thermal", menu.field(MachineMenu.SMS_UI_THERMAL));
      state.addProperty("input_item", "minecraft:cobblestone");
      state.addProperty("output_item", "minecraft:magma_block");
      state.addProperty("output_count", menu.getSlot(1).getItem().getCount());
      state.addProperty("tank_amount", menu.field(MachineMenu.SMS_UI_TANK_AMOUNT));
      state.addProperty("tank_capacity", menu.field(MachineMenu.SMS_UI_TANK_CAPACITY));
      state.addProperty("energy", menu.field(MachineMenu.SMS_UI_ENERGY));
      state.addProperty("energy_capacity", menu.field(MachineMenu.SMS_UI_ENERGY_CAPACITY));
      state.addProperty("fe_rate", menu.field(MachineMenu.SMS_UI_FE_RATE));
      state.addProperty("status", menu.field(MachineMenu.SMS_UI_STATUS));
      proof.add("live_state", state);

      JsonObject geometry = new JsonObject();
      geometry.addProperty("gui_width", mc.getWindow().getGuiScaledWidth());
      geometry.addProperty("gui_height", mc.getWindow().getGuiScaledHeight());
      geometry.addProperty("panel_width", menu.layout().imageWidth());
      geometry.addProperty("panel_height", menu.layout().imageHeight());
      geometry.addProperty("view_scale", screen.validationViewScale());
      geometry.addProperty("drawer_closed_capture", hasCapture("screen-closed.png"));
      geometry.addProperty("drawer_open_capture", screen.validationOutputDrawerOpen());
      geometry.addProperty("output_slot_hidden_when_closed", outputSlotHiddenWhenClosed);
      geometry.addProperty("output_slot_visible_when_open", menu.getSlot(1).isActive()
         && menu.getSlot(1).getItem().is(Items.MAGMA_BLOCK));
      proof.addProperty("window_width", mc.getWindow().getWidth());
      proof.addProperty("window_height", mc.getWindow().getHeight());
      proof.add("screen", geometry);
      proof.add("screenshots", CAPTURES.deepCopy());
      proof.addProperty("screenshot_method", "Minecraft Screenshot.takeScreenshot(getMainRenderTarget())");

      Files.writeString(gameDirectory.resolve("client-smelter-gui-result.json"),
         new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
   }

   private static boolean hasCapture(String name) {
      for (int index = 0; index < CAPTURES.size(); index++) {
         if (CAPTURES.get(index).getAsJsonObject().get("name").getAsString().equals(name)) return true;
      }
      return false;
   }

   private static void require(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
   }
}
