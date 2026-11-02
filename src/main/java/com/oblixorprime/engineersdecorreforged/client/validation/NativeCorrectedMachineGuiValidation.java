package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.lwjgl.glfw.GLFW;

/** Native integrated-client captures for the six MachineScreen GUIs corrected in this batch. */
final class NativeCorrectedMachineGuiValidation {
   private static final String SUITE = "native-corrected-machine-gui-v1";
   private static final BlockPos POS = new BlockPos(0, 80, 0);
   private static final List<Spec> SPECS = List.of(
      new Spec(MachineKind.FLUID_BARREL, "fluid_barrel", ModBlocks.FLUID_BARREL.get(), Fluids.WATER),
      new Spec(MachineKind.SMALL_MILKING_MACHINE, "small_milking_machine", ModBlocks.SMALL_MILKING_MACHINE.get(), null),
      new Spec(MachineKind.SMALL_SOLAR_PANEL, "small_solar_panel", ModBlocks.SMALL_SOLAR_PANEL.get(), null),
      new Spec(MachineKind.SMALL_FREEZER, "small_freezer", ModBlocks.SMALL_FREEZER.get(), null),
      new Spec(MachineKind.SMALL_FLUID_FUNNEL, "small_fluid_funnel", ModBlocks.SMALL_FLUID_FUNNEL.get(), Fluids.WATER),
      new Spec(MachineKind.PASSIVE_FLUID_ACCUMULATOR, "passive_fluid_accumulator", ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get(), Fluids.WATER)
   );
   private static final String[] STATES = {"empty", "mid", "full"};

   private enum Stage { WAIT_SETUP, WAIT_MENU, WAIT_STATE, WAIT_HOVER, DONE }

   private static Stage stage;
   private static CompletableFuture<?> pending;
   private static int ticks;
   private static int specIndex;
   private static int stateIndex;
   private static MachineBlockEntity serverMachine;
   private static int serverMenuId = -1;
   private static String serverMenuIdentity = "";
   private static Path gameDirectory;
   private static Path screenshots;
   private static String nonce;
   private static String executedSourceSha;
   private static String pullRequestHeadSha;
   private static String language;
   private static final JsonArray CAPTURES = new JsonArray();
   private static final JsonArray CASES = new JsonArray();

   private NativeCorrectedMachineGuiValidation() {}

   static void begin(Minecraft mc, Path directory, String runNonce) {
      require(mc.getSingleplayerServer() != null && mc.player != null,
         "Corrected GUI fixture requires an integrated player");
      gameDirectory = directory;
      nonce = runNonce;
      executedSourceSha = System.getProperty("iedctr.executedSourceSha", "");
      pullRequestHeadSha = System.getProperty("iedctr.pullRequestHeadSha", "");
      language = System.getProperty("iedctr.machineGuiLanguage", "");
      require(executedSourceSha.matches("[0-9a-f]{40}"), "Fixture requires the executed source SHA");
      require(pullRequestHeadSha.matches("[0-9a-f]{40}"), "Fixture requires the exact PR head SHA");
      require(language.equals("en_us") || language.equals("fr_fr"), "Fixture language must be en_us or fr_fr");
      screenshots = gameDirectory.resolve("corrected-machine-gui-screenshots");
      require(!Files.exists(screenshots), "Refusing stale corrected-machine GUI screenshots");
      while (CAPTURES.size() > 0) CAPTURES.remove(CAPTURES.size() - 1);
      while (CASES.size() > 0) CASES.remove(CASES.size() - 1);
      specIndex = 0;
      stateIndex = 0;
      ticks = 0;
      serverMachine = null;
      serverMenuId = -1;
      serverMenuIdentity = "";
      scheduleMachine(mc, 0);
      stage = Stage.WAIT_SETUP;
   }

   static boolean tick(Minecraft mc, Path directory, String runNonce) throws Exception {
      require(nonce.equals(runNonce) && directory.equals(gameDirectory), "Corrected GUI fixture identity changed");
      switch (stage) {
         case WAIT_SETUP -> {
            if (++ticks > 300) throw new IllegalStateException("Integrated server did not prepare a corrected GUI");
            if (!pending.isDone()) return false;
            pending.join();
            ticks = 0;
            stage = Stage.WAIT_MENU;
            return false;
         }
         case WAIT_MENU -> {
            if (++ticks > 300) throw new IllegalStateException("Real corrected machine screen did not open");
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != SPECS.get(specIndex).kind()
               || menu.containerId != serverMenuId
               || !menu.getClass().getName().equals(serverMenuIdentity)) return false;
            verifyMenuGeometry(screen, menu, SPECS.get(specIndex));
            if (ticks < 15) return false;
            stateIndex = 0;
            scheduleState(mc, 0);
            stage = Stage.WAIT_STATE;
            ticks = 0;
            return false;
         }
         case WAIT_STATE -> {
            if (++ticks > 300) throw new IllegalStateException("Corrected GUI live state did not synchronize");
            if (!pending.isDone()) return false;
            pending.join();
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != SPECS.get(specIndex).kind()
               || menu.containerId != serverMenuId) return false;
            if (!stateIsVisible(menu, SPECS.get(specIndex), stateIndex)) return false;
            if (SPECS.get(specIndex).kind() == MachineKind.SMALL_FREEZER && stateIndex == 1
               && menu.freezerProgressTicks() < 30) return false;
            if (ticks < 6) return false;
            if (stateIndex == 2) {
               moveCursorToFirstSlot(mc, menu);
               stage = Stage.WAIT_HOVER;
               ticks = 0;
               return false;
            }
            capture(mc, screen, menu, STATES[stateIndex], null);
            stateIndex++;
            scheduleState(mc, stateIndex);
            stage = Stage.WAIT_STATE;
            ticks = 0;
            return false;
         }
         case WAIT_HOVER -> {
            if (++ticks < 6) return false;
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || !stateIsVisible(menu, SPECS.get(specIndex), 2)) {
               throw new IllegalStateException("Corrected GUI changed before its hover capture");
            }
            MachineLayout.MachineSlot target = menu.layout().machineSlots().get(0);
            JsonObject hover = new JsonObject();
            hover.addProperty("slot_index", target.containerIndex());
            hover.addProperty("slot_local_x", target.x());
            hover.addProperty("slot_local_y", target.y());
            double centerX = (mc.getWindow().getGuiScaledWidth() - menu.layout().imageWidth()) / 2.0 + target.x() + 8;
            double centerY = (mc.getWindow().getGuiScaledHeight() - menu.layout().imageHeight()) / 2.0 + target.y() + 8;
            hover.addProperty("slot_hitbox_center_verified", screen.isMachineSlotHoveredAt(0, centerX, centerY));
            require(hover.get("slot_hitbox_center_verified").getAsBoolean(), "First machine slot does not hover at its center");
            capture(mc, screen, menu, "full-hover", hover);
            JsonObject result = caseProof(mc, screen, menu, SPECS.get(specIndex));
            CASES.add(result);
            specIndex++;
            if (specIndex >= SPECS.size()) {
               writeProof(mc, runNonce);
               stage = Stage.DONE;
               return true;
            }
            scheduleMachine(mc, specIndex);
            stage = Stage.WAIT_SETUP;
            ticks = 0;
            return false;
         }
         case DONE -> { return true; }
      }
      throw new IllegalStateException("Unexpected corrected GUI fixture stage: " + stage);
   }

   private static void scheduleMachine(Minecraft mc, int index) {
      UUID playerId = mc.player.getUUID();
      Spec spec = SPECS.get(index);
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null, "Integrated server player disappeared before GUI setup");
         player.closeContainer();
         var level = player.serverLevel();
         player.setGameMode(GameType.CREATIVE);
         player.getInventory().clearContent();
         player.getInventory().setItem(0, new ItemStack(Items.BUCKET));
         player.connection.teleport(POS.getX() + 0.5, POS.getY() + 1.0, POS.getZ() + 3.5, 180.0F, 0.0F);
         level.setBlock(POS.below(), Blocks.STONE.defaultBlockState(), 3);
         level.setBlock(POS, Blocks.AIR.defaultBlockState(), 3);
         level.setBlock(POS, spec.block().defaultBlockState(), 3);
         if (!(level.getBlockEntity(POS) instanceof MachineBlockEntity machine)
            || machine.kind() != spec.kind()) {
            throw new IllegalStateException("Fixture failed to place " + spec.kind());
         }
         if (spec.kind() == MachineKind.SMALL_SOLAR_PANEL) level.setDayTime(18000L);
         serverMachine = machine;
         setState(machine, spec, 0);
         if (player.openMenu(machine).isEmpty()) throw new IllegalStateException("Server refused " + spec.kind() + " menu");
         if (!(player.containerMenu instanceof MachineMenu menu)
            || menu.kind() != spec.kind() || menu.visibleMachineSlots() != menu.layout().machineSlots().size()
            || menu.slots.size() != menu.visibleMachineSlots() + 36
            || !menu.stillValid(player)) {
            throw new IllegalStateException("Server opened the wrong menu or slot lattice for " + spec.kind());
         }
         serverMenuId = menu.containerId;
         serverMenuIdentity = menu.getClass().getName();
         menu.broadcastChanges();
      });
   }

   private static void scheduleState(Minecraft mc, int nextState) {
      UUID playerId = mc.player.getUUID();
      Spec spec = SPECS.get(specIndex);
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null && serverMachine != null, "Server fixture lost its live machine");
         setState(serverMachine, spec, nextState);
         if (spec.kind() == MachineKind.SMALL_FREEZER && nextState == 1) {
            require(serverMachine.canPlaceItem(0, new ItemStack(Items.WATER_BUCKET)),
               "Freezer fixture water bucket is not a valid recipe input");
            serverMachine.setItem(0, new ItemStack(Items.WATER_BUCKET));
         }
         if (spec.kind() == MachineKind.SMALL_FREEZER && nextState == 2) {
            serverMachine.setItem(0, ItemStack.EMPTY);
            serverMachine.dataAccessForTests().set(1, 0);
         }
         if (player.containerMenu instanceof MachineMenu menu) menu.broadcastChanges();
      });
   }

   private static void setState(MachineBlockEntity machine, Spec spec, int state) {
      int fraction = switch (state) { case 0 -> 0; case 1 -> 1; case 2 -> 2; default -> throw new IllegalArgumentException("Unknown GUI state"); };
      if (spec.fluid() != null || spec.kind() == MachineKind.SMALL_MILKING_MACHINE) {
         var handler = machine.fluidHandler(Direction.UP);
         require(handler != null && handler.getTanks() > 0, "Fixture has no fluid tank for " + spec.kind());
         handler.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
         Fluid fluid = spec.fluid() != null ? spec.fluid() : milkFluid();
         int capacity = handler.getTankCapacity(0);
         int amount = fraction == 0 ? 0 : fraction == 1 ? capacity / 2 : capacity;
         if (amount > 0) require(handler.fill(new FluidStack(fluid, amount), FluidAction.EXECUTE) == amount,
            "Fixture could not set " + spec.kind() + " tank to state " + state);
      } else if (spec.kind() == MachineKind.SMALL_SOLAR_PANEL) {
         int amount = fraction * 64000 / 2;
         machine.dataAccessForTests().set(3, amount / 100);
      } else if (spec.kind() == MachineKind.SMALL_FREEZER) {
         int amount = fraction * 16000 / 2;
         machine.dataAccessForTests().set(3, amount);
      }
      machine.setChanged();
   }

   private static Fluid milkFluid() {
      require(NeoForgeMod.MILK.isBound(), "Milk fluid is not registered in the native GUI fixture");
      return NeoForgeMod.MILK.value();
   }

   private static boolean stateIsVisible(MachineMenu menu, Spec spec, int state) {
      int expected = state == 0 ? 0 : state == 1 ? (spec.fluid() != null || spec.kind() == MachineKind.SMALL_MILKING_MACHINE
         ? menu.fluidCapacity() / 2 : menu.energyCapacity() / 2) : (spec.fluid() != null || spec.kind() == MachineKind.SMALL_MILKING_MACHINE
         ? menu.fluidCapacity() : menu.energyCapacity());
      int visible = spec.fluid() != null || spec.kind() == MachineKind.SMALL_MILKING_MACHINE
         ? menu.fluidAmount() : menu.energyStored();
      if (spec.kind() == MachineKind.SMALL_FREEZER && state == 1) return visible > 0 && menu.freezerProgressTicks() >= 30;
      return visible == expected;
   }

   private static void verifyMenuGeometry(MachineScreen screen, MachineMenu menu, Spec spec) {
      MachineLayout layout = menu.layout();
      require(screen.getClass().getName().contains("MachineScreen$"), "Fixture did not open the registered MachineScreen");
      require(menu.visibleMachineSlots() == layout.machineSlots().size(), "Visible machine slot count differs from layout");
      for (int i = 0; i < layout.machineSlots().size(); i++) {
         MachineLayout.MachineSlot expected = layout.machineSlots().get(i);
         var actual = menu.getSlot(i);
         require(actual.getSlotIndex() == expected.containerIndex() && actual.x == expected.x() && actual.y == expected.y(),
            "Live slot geometry differs from artwork layout for " + spec.id() + " index " + i);
         require(actual.x >= 0 && actual.y >= 0 && actual.x + 16 <= layout.imageWidth()
            && actual.y + 16 <= layout.imageHeight(), "Machine slot hitbox falls outside its panel: " + spec.id());
      }
      int base = layout.machineSlots().size();
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            var actual = menu.getSlot(base + row * 9 + column);
            require(actual.x == layout.playerSlotX(column) && actual.y == layout.playerSlotY(row),
               "Live player inventory lattice differs from layout for " + spec.id());
         }
      }
      for (int column = 0; column < 9; column++) {
         var actual = menu.getSlot(base + 27 + column);
         require(actual.x == layout.hotbarSlotX(column) && actual.y == layout.hotbarY(),
            "Live hotbar lattice differs from layout for " + spec.id());
      }
      if (spec.kind() == MachineKind.SMALL_FREEZER) {
         require(!menu.getSlot(1).mayPlace(new ItemStack(Items.STONE)),
            "Reserved Freezer Cold slot must reject insertion");
      }
   }

   private static void moveCursorToFirstSlot(Minecraft mc, MachineMenu menu) {
      MachineLayout layout = menu.layout();
      int guiWidth = mc.getWindow().getGuiScaledWidth();
      int guiHeight = mc.getWindow().getGuiScaledHeight();
      MachineLayout.MachineSlot target = layout.machineSlots().get(0);
      double scaleX = mc.getWindow().getWidth() / (double)guiWidth;
      double scaleY = mc.getWindow().getHeight() / (double)guiHeight;
      double x = ((guiWidth - layout.imageWidth()) / 2.0 + target.x() + 8) * scaleX;
      double y = ((guiHeight - layout.imageHeight()) / 2.0 + target.y() + 8) * scaleY;
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x, y);
   }

   private static void capture(Minecraft mc, MachineScreen screen, MachineMenu menu, String state,
                               JsonObject hover) throws Exception {
      MachineLayout layout = menu.layout();
      int guiWidth = mc.getWindow().getGuiScaledWidth();
      int guiHeight = mc.getWindow().getGuiScaledHeight();
      double scaleX = mc.getWindow().getWidth() / (double)guiWidth;
      double scaleY = mc.getWindow().getHeight() / (double)guiHeight;
      int cropX = (int)Math.round((guiWidth - layout.imageWidth()) / 2.0 * scaleX);
      int cropY = (int)Math.round((guiHeight - layout.imageHeight()) / 2.0 * scaleY);
      int cropWidth = (int)Math.round(layout.imageWidth() * scaleX);
      int cropHeight = (int)Math.round(layout.imageHeight() * scaleY);
      String name = SPECS.get(specIndex).id() + "-" + state + ".png";
      Files.createDirectories(screenshots);
      Path output = screenshots.resolve(name);
      require(!Files.exists(output), "Refusing to overwrite a corrected GUI capture");
      String tooltipName = hover == null ? null : SPECS.get(specIndex).id() + "-" + state + "-window.png";
      Path tooltipOutput = tooltipName == null ? null : screenshots.resolve(tooltipName);
      if (tooltipOutput != null) {
         require(!Files.exists(tooltipOutput), "Refusing to overwrite a corrected GUI tooltip capture");
      }
      try (NativeImage full = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         require(cropX >= 0 && cropY >= 0 && cropX + cropWidth <= full.getWidth()
            && cropY + cropHeight <= full.getHeight(), "GUI-only screenshot crop exceeds the live framebuffer");
         try (NativeImage crop = new NativeImage(cropWidth, cropHeight, false)) {
            for (int py = 0; py < cropHeight; py++) {
               for (int px = 0; px < cropWidth; px++) {
                  crop.setPixelRGBA(px, py, full.getPixelRGBA(cropX + px, cropY + py));
               }
            }
            crop.writeToFile(output);
         }
         if (tooltipOutput != null) {
            full.writeToFile(tooltipOutput);
         }
      }
      JsonObject capture = new JsonObject();
      capture.addProperty("name", name);
      capture.addProperty("state", state);
      capture.addProperty("capture_mode", "gui_panel");
      capture.addProperty("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(output))));
      capture.addProperty("width", cropWidth);
      capture.addProperty("height", cropHeight);
      if (tooltipOutput != null) {
         capture.addProperty("tooltip_name", tooltipName);
         capture.addProperty("tooltip_capture_mode", "full_window_tooltip");
         capture.addProperty("tooltip_sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tooltipOutput))));
         capture.addProperty("tooltip_width", mc.getWindow().getWidth());
         capture.addProperty("tooltip_height", mc.getWindow().getHeight());
      }
      capture.addProperty("kind", SPECS.get(specIndex).kind().name());
      capture.addProperty("screen_identity", screen.getClass().getName());
      capture.addProperty("menu_id", menu.containerId);
      capture.addProperty("server_menu_id", serverMenuId);
      capture.addProperty("fluid_amount", menu.fluidAmount());
      capture.addProperty("energy", menu.energyStored());
      capture.addProperty("progress", menu.progress());
      if (hover != null) capture.add("hover", hover);
      CAPTURES.add(capture);
   }

   private static JsonObject caseProof(Minecraft mc, MachineScreen screen, MachineMenu menu, Spec spec) {
      JsonObject result = new JsonObject();
      result.addProperty("machine_id", spec.id());
      result.addProperty("machine_kind", spec.kind().name());
      result.addProperty("screen_identity", screen.getClass().getName());
      result.addProperty("client_menu_identity", menu.getClass().getName());
      result.addProperty("server_menu_identity", serverMenuIdentity);
      result.addProperty("client_menu_id", menu.containerId);
      result.addProperty("server_menu_id", serverMenuId);
      result.addProperty("menu_context_matches", menu.containerId == serverMenuId
         && menu.getClass().getName().equals(serverMenuIdentity));
      result.addProperty("visible_machine_slots", menu.visibleMachineSlots());
      result.addProperty("total_slots", menu.slots.size());
      result.addProperty("panel_width", menu.layout().imageWidth());
      result.addProperty("panel_height", menu.layout().imageHeight());
      result.addProperty("gui_width", mc.getWindow().getGuiScaledWidth());
      result.addProperty("gui_height", mc.getWindow().getGuiScaledHeight());
      result.addProperty("geometry_matches_layout", true);
      result.addProperty("fluid_amount", menu.fluidAmount());
      result.addProperty("fluid_capacity", menu.fluidCapacity());
      result.addProperty("energy", menu.energyStored());
      result.addProperty("energy_capacity", menu.energyCapacity());
      result.addProperty("progress", menu.progress());
      result.addProperty("freezer_progress_ticks", menu.freezerProgressTicks());
      result.addProperty("language", language);
      JsonArray stateNames = new JsonArray();
      stateNames.add("empty");
      stateNames.add("mid");
      stateNames.add("full-hover");
      result.add("captured_states", stateNames);
      JsonArray slots = new JsonArray();
      for (int index = 0; index < menu.visibleMachineSlots(); index++) {
         MachineLayout.MachineSlot layoutSlot = menu.layout().machineSlots().get(index);
         var liveSlot = menu.getSlot(index);
         JsonObject slot = new JsonObject();
         slot.addProperty("container_index", liveSlot.getSlotIndex());
         slot.addProperty("x", liveSlot.x);
         slot.addProperty("y", liveSlot.y);
         slot.addProperty("layout_x", layoutSlot.x());
         slot.addProperty("layout_y", layoutSlot.y());
         slot.addProperty("active", liveSlot.isActive());
       slot.addProperty("insertion_allowed", liveSlot.mayPlace(new ItemStack(Items.STONE)));
         double centerX = (mc.getWindow().getGuiScaledWidth() - menu.layout().imageWidth()) / 2.0 + liveSlot.x + 8;
         double centerY = (mc.getWindow().getGuiScaledHeight() - menu.layout().imageHeight()) / 2.0 + liveSlot.y + 8;
         slot.addProperty("center_hit_test", screen.isMachineSlotHoveredAt(index, centerX, centerY));
         slots.add(slot);
      }
      result.add("machine_slots", slots);
      return result;
   }

   private static void writeProof(Minecraft mc, String runNonce) throws Exception {
      require(nonce.equals(runNonce) && CASES.size() == SPECS.size() && CAPTURES.size() == SPECS.size() * STATES.length,
         "Corrected GUI capture matrix is incomplete");
      String selectedLanguage = mc.getLanguageManager().getSelected();
      require(language.equals(selectedLanguage), "Minecraft did not load the requested GUI language");
      JsonObject proof = new JsonObject();
      proof.addProperty("suite", SUITE);
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", "passed");
      proof.addProperty("actual_integrated_world", mc.getSingleplayerServer() != null
         && mc.player.connection.getConnection().isMemoryConnection());
      proof.addProperty("executed_source_sha", executedSourceSha);
      proof.addProperty("pull_request_head_sha", pullRequestHeadSha);
      proof.addProperty("language", language);
      proof.addProperty("selected_language", selectedLanguage);
      proof.addProperty("window_width", mc.getWindow().getWidth());
      proof.addProperty("window_height", mc.getWindow().getHeight());
      proof.addProperty("screenshot_method", "Native Minecraft panel crops for every state plus full-window hover captures to retain tooltips");
      proof.addProperty("fixture_state_source", "Live MachineBlockEntity fluid handlers and test data synced through the registered MachineMenu");
      proof.add("cases", CASES.deepCopy());
      proof.add("screenshots", CAPTURES.deepCopy());
      Files.writeString(gameDirectory.resolve("client-corrected-machine-gui-result.json"),
         new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
   }

   private static void require(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
   }

   private record Spec(MachineKind kind, String id, Block block, Fluid fluid) {}
}
