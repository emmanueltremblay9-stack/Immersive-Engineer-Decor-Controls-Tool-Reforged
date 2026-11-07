package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.FactoryPlacerV2ReferenceScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FactoryPlacerV2ReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.FactoryPlacerV2ReferenceGeometry.Control;
import com.oblixorprime.engineersdecorreforged.utility.FactoryPlacerV2ReferenceGeometry.Rect;
import com.oblixorprime.engineersdecorreforged.utility.FactoryPlacerV2ReferenceGeometry.ViewTransform;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.util.Arrays;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** Opt-in test source set only. All mutation is confined to a fresh nonce-owned world. */
@EventBusSubscriber(modid = "iedctr_factory_placer_validation", value = Dist.CLIENT)
public final class NativeFactoryPlacerV2ReferenceValidation {
   private static final String SUITE = "native-factory-placer-v2-reference-v1";
   private static final BlockPos POS = new BlockPos(0, 80, 0), TARGET = POS.south();
   private static final Rect CONTROL_CROP = new Rect(240, 55, 125, 120);
   private enum Stage { BOOT, WORLD, SETUP, SLOT_READY, PICKED, RETURNED, NORMAL, CONTROL,
      MANUAL_LIT, MANUAL_DARK, PLACE_READY, PLACE_WAIT, PLACED_CAPTURE, CLOSE, WORLD_CAPTURE,
      REOPEN, RELOAD_WAIT, RELOAD_CAPTURE, ADVANCED_SETUP, ADVANCED, TOOLTIPS, SELECTIONS, DONE }
   private record ControlStep(Control control, int logic, int selected, String capture) {}
   private static final ControlStep[] STEPS = {
      new ControlStep(Control.ROW_MIDDLE, 0, 6, "row-middle"), new ControlStep(Control.ROW_BOTTOM, 0, 12, "row-bottom"),
      new ControlStep(Control.ROW_TOP, 0, 0, null), new ControlStep(Control.ROW_MIDDLE, 0, 6, null),
      new ControlStep(Control.ROW_BOTTOM, 0, 12, null), new ControlStep(Control.ROW_TOP, 0, 0, null),
      new ControlStep(Control.INVERSION, 1, 0, "inverted"), new ControlStep(Control.INVERSION, 4, 0, null),
      new ControlStep(Control.INVERSION, 0, 0, null), new ControlStep(Control.INVERSION, 1, 0, null),
      new ControlStep(Control.INVERSION, 4, 0, null), new ControlStep(Control.INVERSION, 0, 0, null),
      new ControlStep(Control.TRIGGER_MODE, 2, 0, "continuous"), new ControlStep(Control.TRIGGER_MODE, 0, 0, null),
      new ControlStep(Control.TRIGGER_MODE, 2, 0, null), new ControlStep(Control.TRIGGER_MODE, 0, 0, null)
   };
   private static Stage stage = Stage.BOOT;
   private static Path directory;
   private static String nonce;
   private static UUID playerId;
   private static long deadline, lastSlotClickNanos;
   private static int ticks, slotIndex, controlIndex, serverMenuId, oldMenuId;
   private static int pickupAcks, returnAcks, manualAcks, initialTotal, finalTotal;
   private static boolean complete;
   private static MachineBlockEntity machine;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<Boolean> inspection;
   private static final JsonArray ROUTES = new JsonArray(), CAPTURES = new JsonArray(), CASES = new JsonArray(), CONTROLS = new JsonArray();
   private static JsonArray hiddenBefore, hiddenAfter;
   private static JsonObject placement, worldCapture, packagedRuntime;
   private static final JsonArray ADVANCED = new JsonArray(), TOOLTIPS = new JsonArray(), SELECTIONS = new JsonArray();
   private static final String[] GESTURE_NAMES = {"drag_left_split", "drag_right_split", "shift_click", "double_click_collect",
      "right_control_noop", "middle_control_noop", "held_control_noop", "outside_carried_drop"};
   private static int gestureIndex, gestureStep, hoverIndex, selectionIndex, tooltipFrames;
   private static int pointerX, pointerY;
   private static boolean actionSent, shiftDown, tooltipInFrame;
   private static NativeLibrary x11, xtest;
   private static Pointer display;
   private static JsonObject gesture, observedServer, tooltipObservation, renderedTooltip;
   private static CompletableFuture<JsonObject> snapshotFuture;
   private static final int[] expectedSlots = new int[54];
   private static int expectedCarried, expectedDrops;
   private NativeFactoryPlacerV2ReferenceValidation() {}

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      if (!Boolean.getBoolean("iedctr.factoryPlacerV2ReferenceNative") || complete) return;
      Minecraft mc = Minecraft.getInstance();
      try {
         if (deadline == 0) deadline = System.nanoTime() + 900_000_000_000L;
         require(System.nanoTime() < deadline && ++ticks < 1800, "Native Factory Placer timeout at " + stage);
         if (stage == Stage.BOOT) {
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) boot(mc);
            return;
         }
         if (stage == Stage.WORLD) {
            if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
            require(mc.getSingleplayerServer() != null && mc.player.connection.getConnection().isMemoryConnection(), "Only isolated integrated worlds permitted");
            playerId = mc.player.getUUID();
            pending = mc.getSingleplayerServer().submit(() -> setup(mc));
            enter(Stage.SETUP); return;
         }
         require(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null, "World disconnected");
         if (!ready()) return;
         if (stage == Stage.CLOSE) {
            if (mc.screen != null || !serverMatches(mc, p -> p.containerMenu == p.inventoryMenu && preservedAfterPlacement(p))) return;
            CASES.add("close_reaches_server"); enter(Stage.WORLD_CAPTURE); return;
         }
         if (stage == Stage.WORLD_CAPTURE) {
            if (ticks < 20 || mc.screen != null || !mc.level.getBlockState(TARGET).is(Blocks.STONE)) return;
            if (!serverMatches(mc, NativeFactoryPlacerV2ReferenceValidation::preservedAfterPlacement)) return;
            captureWorld(mc);
            pending = mc.getSingleplayerServer().submit(() -> { player(mc).openMenu(machine); serverMenuId = player(mc).containerMenu.containerId; });
            enter(Stage.REOPEN); return;
         }
         if (!(mc.screen instanceof FactoryPlacerV2ReferenceScreen screen)
            || !(mc.player.containerMenu instanceof MachineMenu menu) || menu.containerId != serverMenuId || mc.getOverlay() != null) return;
         switch (stage) {
            case SETUP -> { verifyGeometry(screen, menu); prepareSlot(mc); }
            case SLOT_READY -> {
               if (ticks < 8 || !stone(menu.getSlot(slotIndex).getItem(), 1) || !menu.getCarried().isEmpty()) return;
               if (!serverMatches(mc, p -> total(p) == 46 && sentinelsIntact())) return;
               verifyGeometry(screen, menu); clickSlot(screen, slotIndex); enter(Stage.PICKED);
            }
            case PICKED -> {
               // Vanilla treats a same-slot click inside250ms as PICKUP_ALL.
               // This case intentionally tests ordinary pickup/return, not double-click.
               if (ticks < 6 || System.nanoTime() - lastSlotClickNanos < 300_000_000L
                  || !menu.getSlot(slotIndex).getItem().isEmpty() || !stone(menu.getCarried(), 1)
                  || !serverMatches(mc, p -> p.containerMenu.getSlot(slotIndex).getItem().isEmpty()
                     && stone(p.containerMenu.getCarried(), 1) && total(p) == 46 && sentinelsIntact())) return;
               pickupAcks++; clickSlot(screen, slotIndex); enter(Stage.RETURNED);
            }
            case RETURNED -> {
               if (!menu.getCarried().isEmpty() || !stone(menu.getSlot(slotIndex).getItem(), 1)
                  || !serverMatches(mc, p -> p.containerMenu.getCarried().isEmpty()
                     && stone(p.containerMenu.getSlot(slotIndex).getItem(), 1) && total(p) == 46 && sentinelsIntact())) return;
               returnAcks++; ROUTES.add(slotIndex);
               if (++slotIndex < 54) prepareSlot(mc);
               else {
                  CASES.add("all_54_pickup_return_server_confirmed");
                  pending = mc.getSingleplayerServer().submit(() -> {
                     finalTotal = total(player(mc)); clearVisible(player(mc));
                     machine.dataAccessForTests().set(0, 0); machine.dataAccessForTests().set(2, 0);
                     player(mc).containerMenu.broadcastChanges();
                  });
                  parkCursor(mc); enter(Stage.NORMAL);
               }
            }
            case NORMAL -> {
               if (ticks < 15 || !clientState(menu, 0, 0) || !serverMatches(mc, p -> controlState(p, 0, 0))) return;
               capture(mc, menu, "normal"); beginControl(screen);
            }
            case CONTROL -> {
               var step = STEPS[controlIndex];
               if (ticks < 12 || !clientState(menu, step.logic(), step.selected())
                  || !serverMatches(mc, p -> controlState(p, step.logic(), step.selected()))) return;
               var result = new JsonObject(); result.addProperty("control", step.control().name());
               result.addProperty("logic", menu.field(0)); result.addProperty("selected_slot", menu.field(2));
               result.addProperty("server_confirmed", true); CONTROLS.add(result);
               if (step.capture() != null) capture(mc, menu, step.capture());
               if (++controlIndex < STEPS.length) beginControl(screen);
               else {
                  CASES.add("repeated_row_selection_server_confirmed"); CASES.add("repeated_inversion_and_trigger_mode_server_confirmed");
                  clickControl(screen, Control.MANUAL_TRIGGER); enter(Stage.MANUAL_LIT);
               }
            }
            case MANUAL_LIT -> {
               if (menu.field(1) != 1 || !serverMatches(mc, p -> machine.dataAccessForTests().get(1) == 1 && total(p) == 45 && sentinelsIntact())) return;
               manualAcks++; enter(Stage.MANUAL_DARK);
            }
            case MANUAL_DARK -> {
               if (ticks < 15 || menu.field(1) != 0 || !serverMatches(mc, p -> controlState(p, 0, 0))) return;
               if (manualAcks < 3) { clickControl(screen, Control.MANUAL_TRIGGER); enter(Stage.MANUAL_LIT); }
               else {
                  CASES.add("repeated_manual_trigger_server_confirmed");
                  pending = mc.getSingleplayerServer().submit(() -> {
                     require(controlState(player(mc), 0, 0), "Placement setup lost idle state");
                     require(player(mc).serverLevel().getBlockState(TARGET).isAir(), "Placement target not empty");
                     machine.setItem(0, new ItemStack(Items.STONE, 2)); player(mc).containerMenu.broadcastChanges();
                     require(total(player(mc)) == 47, "Placement seed item count differs");
                  }); enter(Stage.PLACE_READY);
               }
            }
            case PLACE_READY -> {
               if (ticks < 8 || !stone(menu.getSlot(0).getItem(), 2) || !clientState(menu, 0, 0)) return;
               if (!serverMatches(mc, p -> total(p) == 47 && stone(machine.getItem(0), 2) && p.serverLevel().getBlockState(TARGET).isAir())) return;
               clickControl(screen, Control.MANUAL_TRIGGER); enter(Stage.PLACE_WAIT);
            }
            case PLACE_WAIT -> {
               if (!stone(menu.getSlot(0).getItem(), 1) || menu.field(2) != 1 || !mc.level.getBlockState(TARGET).is(Blocks.STONE)
                  || !serverMatches(mc, NativeFactoryPlacerV2ReferenceValidation::preservedAfterPlacement)) return;
               placement = new JsonObject(); placement.addProperty("block", "minecraft:stone");
               placement.addProperty("before_count", 2); placement.addProperty("after_count", machine.getItem(0).getCount());
               placement.addProperty("server_confirmed", true); placement.add("target", targetJson());
               placement.addProperty("item_total_before", 47); placement.addProperty("item_total_after", 46); placement.addProperty("world_drop_count", 0);
               CASES.add("one_block_placement_consumes_one_server_item"); enter(Stage.PLACED_CAPTURE);
            }
            case PLACED_CAPTURE -> {
               if (ticks < 15 || !clientState(menu, 0, 1) || !serverMatches(mc, NativeFactoryPlacerV2ReferenceValidation::preservedAfterPlacement)) return;
               verifyGeometry(screen, menu); capture(mc, menu, "placed"); oldMenuId = menu.containerId;
               screen.onClose(); enter(Stage.CLOSE);
            }
            case REOPEN -> {
               if (ticks < 15 || menu.containerId == oldMenuId || !clientState(menu, 0, 1) || !stone(menu.getSlot(0).getItem(), 1)
                  || !serverMatches(mc, NativeFactoryPlacerV2ReferenceValidation::preservedAfterPlacement)) return;
               verifyGeometry(screen, menu); capture(mc, menu, "reopened"); CASES.add("reopen_preserves_inventory_and_state");
               pending = mc.reloadResourcePacks(); enter(Stage.RELOAD_WAIT);
            }
            // Wait a fresh render interval after the reload future and overlay finish.
            // Ticks spent loading resources cannot count as settled native frames.
            case RELOAD_WAIT -> enter(Stage.RELOAD_CAPTURE);
            case RELOAD_CAPTURE -> {
               if (ticks < 15 || !clientState(menu, 0, 1) || !stone(menu.getSlot(0).getItem(), 1)
                  || !serverMatches(mc, p -> { if (!preservedAfterPlacement(p)) return false; hiddenAfter = hiddenSnapshot(); return true; })) return;
               verifyGeometry(screen, menu); capture(mc, menu, "reloaded");
               CASES.add("resource_reload_preserves_inventory_and_state"); CASES.add("hidden_9_saved_slots_preserved");
               initNativeInput(mc);
               pending = mc.getSingleplayerServer().submit(() -> {
                  var p = player(mc); require(p.containerMenu.getCarried().isEmpty(), "Cannot seed over carried items");
                  clearVisible(p); machine.dataAccessForTests().set(0, 0); machine.dataAccessForTests().set(2, 0);
                  p.containerMenu.getSlot(0).set(new ItemStack(Items.STONE, 12));
                  p.containerMenu.getSlot(1).set(new ItemStack(Items.STONE, 4));
                  p.containerMenu.getSlot(18).set(new ItemStack(Items.STONE, 8)); p.containerMenu.broadcastChanges();
               });
               expectedSlots[0] = 12; expectedSlots[1] = 4; expectedSlots[18] = 8; enter(Stage.ADVANCED_SETUP);
            }
            case ADVANCED_SETUP -> {
               if (ticks < 10 || !advancedAgrees(mc, menu)) return;
               startGesture(); enter(Stage.ADVANCED);
            }
            case ADVANCED -> advancedTick(mc, screen, menu);
            case TOOLTIPS -> tooltipTick(mc, screen, menu);
            case SELECTIONS -> selectionTick(mc, screen, menu);
            case DONE -> finish(mc, null);
            default -> throw new IllegalStateException("Unexpected stage " + stage);
         }
      } catch (Throwable error) { finish(mc, error); }
   }

   private static void initNativeInput(Minecraft mc) throws Exception {
      String nativeDisplay = System.getenv("DISPLAY");
      require(nativeDisplay != null && nativeDisplay.matches(":\\d+(\\.\\d+)?")
         && Files.readString(directory.resolve(".iedctr-placer-xvfb")).equals(nonce + "\n" + nativeDisplay), "Native input requires runner-owned Xvfb");
      x11 = NativeLibrary.getInstance("X11"); xtest = NativeLibrary.getInstance("Xtst");
      display = x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null}); require(display != null, "X11 unavailable");
      GLFW.glfwFocusWindow(mc.getWindow().getWindow());
   }
   private static void nativeShift(boolean down) {
      int code = x11.getFunction("XKeysymToKeycode").invokeInt(new Object[]{display, new NativeLong(0xffe1)});
      require(code > 0 && xtest.getFunction("XTestFakeKeyEvent").invokeInt(new Object[]{display, code, down ? 1 : 0, new NativeLong(0)}) != 0, "Native Shift failed");
      shiftDown = down; x11.getFunction("XFlush").invokeInt(new Object[]{display});
   }
   private static int dropItems(ServerPlayer p) {
      int n = 0; for (var entity : p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12))) n += entity.getItem().getCount(); return n;
   }
   private static JsonObject serverSnapshot(ServerPlayer p) {
      require(p.containerMenu instanceof MachineMenu && p.containerMenu.containerId == serverMenuId, "Authoritative menu changed");
      var o = new JsonObject(); o.addProperty("menu_id", p.containerMenu.containerId);
      o.addProperty("logic", machine.dataAccessForTests().get(0)); o.addProperty("selected_slot", machine.dataAccessForTests().get(2));
      o.addProperty("signal", machine.dataAccessForTests().get(1)); o.addProperty("carried_count", p.containerMenu.getCarried().getCount());
      var slots = new JsonObject(); for (int i = 0; i < 54; i++) slots.addProperty(Integer.toString(i), p.containerMenu.getSlot(i).getItem().getCount());
      o.add("slot_counts", slots); o.addProperty("world_drop_items", dropItems(p)); o.addProperty("item_total", total(p) + dropItems(p));
      o.addProperty("hidden_sentinels_intact", sentinelsIntact()); return o;
   }
   private static boolean advancedAgrees(Minecraft mc, MachineMenu menu) {
      if (menu.getCarried().getCount() != expectedCarried || !clientState(menu, 0, 0)) return false;
      for (int i = 0; i < 54; i++) if (menu.getSlot(i).getItem().getCount() != expectedSlots[i]
         || (expectedSlots[i] > 0 && !menu.getSlot(i).getItem().is(Items.STONE))) return false;
      if (snapshotFuture == null) { snapshotFuture = mc.getSingleplayerServer().submit(() -> serverSnapshot(player(mc))); return false; }
      if (!snapshotFuture.isDone()) return false;
      observedServer = snapshotFuture.join(); snapshotFuture = null;
      if (observedServer.get("carried_count").getAsInt() != expectedCarried || observedServer.get("world_drop_items").getAsInt() != expectedDrops
         || observedServer.get("item_total").getAsInt() != 69 || !observedServer.get("hidden_sentinels_intact").getAsBoolean()
         || observedServer.get("logic").getAsInt() != 0 || observedServer.get("signal").getAsInt() != 0 || observedServer.get("selected_slot").getAsInt() != 0) return false;
      for (int i = 0; i < 54; i++) if (observedServer.getAsJsonObject("slot_counts").get(Integer.toString(i)).getAsInt() != expectedSlots[i]) return false;
      return true;
   }
   private static void startGesture() {
      gesture = new JsonObject(); gesture.addProperty("case", GESTURE_NAMES[gestureIndex]);
      gesture.addProperty("input_method", gestureIndex == 2 ? "Screen.mouseClicked/mouseReleased; physical XTest Shift observed through GLFW" : "Screen.mouseClicked/mouseDragged/mouseReleased; vanilla packets, not physical pointer clicks");
      gesture.add("before", observedServer.deepCopy()); gesture.add("steps", new JsonArray()); gesture.add("control_observations", new JsonArray()); gestureStep = 0; actionSent = false;
   }
   private static void advancedTick(Minecraft mc, FactoryPlacerV2ReferenceScreen screen, MachineMenu menu) {
      if (!actionSent) {
         // Avoid accidental PICKUP_ALL between unrelated cases. The explicit double-click below is same-frame.
         if (ticks < 8) return;
         if (gestureIndex == 2) {
            if (!shiftDown) { nativeShift(true); return; }
            if (GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) != GLFW.GLFW_PRESS) return;
            gesture.addProperty("native_shift_observed", true);
         }
         performAdvanced(screen); actionSent = true; ticks = 0; snapshotFuture = null; return;
      }
      if (gestureIndex == 2 && shiftDown) nativeShift(false);
      if (ticks < 4 || !advancedAgrees(mc, menu)) return;
      if (gestureIndex == 2 && GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) != GLFW.GLFW_RELEASE) return;
      var step = observedServer.deepCopy(); step.addProperty("step", gestureStep); step.addProperty("server_ack", true);
      if (gestureIndex == 4 || gestureIndex == 5 || (gestureIndex == 6 && gestureStep >= 1 && gestureStep <= 6)) {
         step.addProperty("control", Control.values()[gestureIndex == 6 ? gestureStep - 1 : gestureStep].name());
         var observation = step.deepCopy(); observation.addProperty("server_observed_unchanged", true);
         observation.addProperty("causal_packet_receipt_proven", false); gesture.getAsJsonArray("control_observations").add(observation);
      }
      gesture.getAsJsonArray("steps").add(step);
      int count = switch (gestureIndex) { case 0 -> 2; case 1 -> 3; case 2 -> 1; case 3 -> 2; case 4, 5 -> 6; case 6 -> 8; default -> 2; };
      if (++gestureStep < count) { actionSent = false; ticks = 0; return; }
      gesture.add("after", observedServer.deepCopy()); gesture.addProperty("server_confirmed", true); gesture.addProperty("server_ack", true);
      gesture.addProperty("ack_scope", gestureIndex >= 4 && gestureIndex <= 6 ? "independent authoritative unchanged-state observation; does not prove receipt of a rejected packet" : "independent authoritative inventory transition");
      gesture.addProperty("item_total_before", 69); gesture.addProperty("item_total_after", observedServer.get("item_total").getAsInt());
      gesture.addProperty("world_drop_items", expectedDrops); gesture.addProperty("carried_count", expectedCarried); gesture.add("slot_counts", observedServer.get("slot_counts").deepCopy());
      ADVANCED.add(gesture);
      if (++gestureIndex < GESTURE_NAMES.length) { startGesture(); ticks = 0; return; }
      // All 24 stone are now an actual world drop; no carried stack is discarded or synthetic gesture success seeded.
      hoverIndex = 0; enter(Stage.TOOLTIPS); hoverTarget(mc, screen);
   }
   private static void performAdvanced(FactoryPlacerV2ReferenceScreen screen) {
      switch (gestureIndex) {
         case 0 -> {
            if (gestureStep == 0) { clickSlot(screen, 0); expectedSlots[0] = 0; expectedCarried = 12; }
            else { dragSlots(screen, 0, 3, 4, 5); expectedSlots[3] = expectedSlots[4] = expectedSlots[5] = 4; expectedCarried = 0; }
         }
         case 1 -> {
            if (gestureStep == 0) { clickSlot(screen, 18); expectedSlots[18] = 0; expectedCarried = 8; }
            else if (gestureStep == 1) { dragSlots(screen, 1, 6, 7, 8); expectedSlots[6] = expectedSlots[7] = expectedSlots[8] = 1; expectedCarried = 5; }
            else { clickSlot(screen, 18); expectedSlots[18] = 5; expectedCarried = 0; }
         }
         case 2 -> { clickSlot(screen, 18); expectedSlots[18] = 0; expectedSlots[0] = 5; }
         case 3 -> {
            if (gestureStep == 0) { clickSlot(screen, 3); clickSlot(screen, 3); Arrays.fill(expectedSlots, 0); expectedCarried = 24; }
            else { clickSlot(screen, 18); expectedSlots[18] = 24; expectedCarried = 0; }
         }
         case 4, 5 -> controlClick(screen, Control.values()[gestureStep], gestureIndex == 4 ? 1 : 2);
         case 6 -> {
            if (gestureStep == 0) { clickSlot(screen, 18); expectedSlots[18] = 0; expectedCarried = 24; }
            else if (gestureStep <= 6) controlClick(screen, Control.values()[gestureStep - 1], 0);
            else { clickSlot(screen, 18); expectedSlots[18] = 24; expectedCarried = 0; }
         }
         case 7 -> {
            if (gestureStep == 0) { clickSlot(screen, 18); expectedSlots[18] = 0; expectedCarried = 24; }
            else { click(screen, -10, -10); expectedCarried = 0; expectedDrops = 24; }
         }
         default -> throw new IllegalStateException("Unexpected gesture");
      }
   }
   private static void controlClick(FactoryPlacerV2ReferenceScreen screen, Control control, int button) {
      var r = FactoryPlacerV2ReferenceGeometry.controlRect(control); var v = screen.validationViewTransform();
      double x = v.screenX(r.x() + r.width() / 2.0), y = v.screenY(r.y() + r.height() / 2.0);
      screen.mouseClicked(x, y, button); screen.mouseReleased(x, y, button);
   }
   private static void dragSlots(FactoryPlacerV2ReferenceScreen screen, int button, int... slots) {
      var v = screen.validationViewTransform(); var first = FactoryPlacerV2ReferenceGeometry.anchor(slots[0]);
      double previousX = v.screenX(first.x() + 8), previousY = v.screenY(first.y() + 8);
      screen.mouseClicked(previousX, previousY, button);
      for (int slot : slots) {
         var a = FactoryPlacerV2ReferenceGeometry.anchor(slot); double x = v.screenX(a.x() + 8), y = v.screenY(a.y() + 8);
         screen.mouseDragged(x, y, button, x - previousX, y - previousY); previousX = x; previousY = y;
      }
      screen.mouseReleased(previousX, previousY, button);
   }
   private static void hoverTarget(Minecraft mc, FactoryPlacerV2ReferenceScreen screen) {
      Rect r = hoverIndex == 0 ? FactoryPlacerV2ReferenceGeometry.HEADER : hoverIndex == 1
         ? new Rect(FactoryPlacerV2ReferenceGeometry.anchor(0).x(), FactoryPlacerV2ReferenceGeometry.anchor(0).y(), 16, 16)
         : FactoryPlacerV2ReferenceGeometry.controlRect(Control.values()[hoverIndex - 2]);
      var v = screen.validationViewTransform(); pointerX = (int)Math.round(v.screenX(r.x() + r.width() / 2.0)); pointerY = (int)Math.round(v.screenY(r.y() + r.height() / 2.0));
      renderedTooltip = null; tooltipObservation = null; tooltipFrames = 0; moveNativePointer(mc, screen);
   }
   private static void moveNativePointer(Minecraft mc, FactoryPlacerV2ReferenceScreen screen) {
      long window = mc.getWindow().getWindow();
      if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) { GLFW.glfwFocusWindow(window); return; }
      int[] x = {0}, y = {0}; GLFW.glfwGetWindowPos(window, x, y);
      int targetX = x[0] + (int)Math.round(pointerX * mc.getWindow().getScreenWidth() / (double)screen.width);
      int targetY = y[0] + (int)Math.round(pointerY * mc.getWindow().getScreenHeight() / (double)screen.height);
      require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{display, -1, targetX, targetY, new NativeLong(0)}) != 0, "Native pointer move failed");
      x11.getFunction("XFlush").invokeInt(new Object[]{display});
   }
   private static boolean atTooltipTarget(Minecraft mc, FactoryPlacerV2ReferenceScreen screen) {
      return Math.abs(mc.mouseHandler.xpos() * screen.width / mc.getWindow().getScreenWidth() - pointerX) < 1
         && Math.abs(mc.mouseHandler.ypos() * screen.height / mc.getWindow().getScreenHeight() - pointerY) < 1
         && GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(), GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
   }
   @SubscribeEvent public static void beforeRender(ScreenEvent.Render.Pre event) { tooltipInFrame = false; }
   @SubscribeEvent public static void observeTooltip(RenderTooltipEvent.Color event) {
      var mc = Minecraft.getInstance();
      if (complete || stage != Stage.TOOLTIPS || !(mc.screen instanceof FactoryPlacerV2ReferenceScreen screen) || !atTooltipTarget(mc, screen)) return;
      try {
         var record = new JsonObject(); var lines = new JsonArray(); var widths = new JsonArray(); var text = new StringBuilder();
         int width = 0, height = event.getComponents().size() == 1 ? -2 : 0;
         for (var component : event.getComponents()) {
            // Observe the actual native text component, not a separately reconstructed translation.
            FormattedCharSequence sequence = null;
            for (var field : component.getClass().getDeclaredFields()) if (FormattedCharSequence.class.isAssignableFrom(field.getType())) {
               field.setAccessible(true); sequence = (FormattedCharSequence)field.get(component); break;
            }
            require(sequence != null, "Tooltip is not an observable native text component");
            var line = new StringBuilder(); sequence.accept((index, style, codepoint) -> { line.appendCodePoint(codepoint); return true; });
            lines.add(line.toString()); if (!text.isEmpty()) text.append('\n'); text.append(line);
            int measured = component.getWidth(event.getFont()); widths.add(measured); width = Math.max(width, measured); height += component.getHeight();
         }
         record.addProperty("text", text.toString()); record.add("lines", lines); record.add("line_widths", widths);
         record.addProperty("tooltip_x", event.getX()); record.addProperty("tooltip_y", event.getY()); record.addProperty("tooltip_width", width); record.addProperty("tooltip_height", height);
         record.addProperty("in_bounds", event.getX() - 4 >= 0 && event.getY() - 4 >= 0 && event.getX() + width + 4 <= screen.width && event.getY() + height + 4 <= screen.height);
         tooltipObservation = record; tooltipInFrame = true;
      } catch (Throwable error) { finish(mc, error); }
   }
   @SubscribeEvent public static void afterRender(ScreenEvent.Render.Post event) {
      if (tooltipInFrame && stage == Stage.TOOLTIPS && event.getScreen() == Minecraft.getInstance().screen) {
         renderedTooltip = tooltipObservation.deepCopy(); tooltipFrames++;
      }
      tooltipInFrame = false;
   }
   private static void tooltipTick(Minecraft mc, FactoryPlacerV2ReferenceScreen screen, MachineMenu menu) throws Exception {
      if (!atTooltipTarget(mc, screen)) { moveNativePointer(mc, screen); return; }
      if (ticks < 12 || tooltipFrames < 3 || renderedTooltip == null || !menu.getCarried().isEmpty()) return;
      var record = renderedTooltip.deepCopy();
      record.addProperty("target", hoverIndex == 0 ? "HEADER" : hoverIndex == 1 ? "SLOT_0" : Control.values()[hoverIndex - 2].name());
      record.addProperty("selected_language", mc.getLanguageManager().getSelected()); record.addProperty("pointer_x", pointerX); record.addProperty("pointer_y", pointerY);
      record.addProperty("tooltip_render_events", tooltipFrames); record.addProperty("tooltip_event_class", "RenderTooltipEvent.Color+ScreenEvent.Render.Post");
      record.addProperty("input_method", "physical XTest pointer through GLFW/Minecraft, native tooltip render event");
      record.addProperty("text_source", "actual ClientTextTooltip FormattedCharSequence; newline separates rendered wrapped lines");
      saveExtraFrame(mc, "tooltip-" + hoverIndex, record); TOOLTIPS.add(record);
      if (++hoverIndex < 8) { ticks = 0; hoverTarget(mc, screen); return; }
      parkCursor(mc); selectionIndex = 0; seedSelection(mc); enter(Stage.SELECTIONS);
   }
   private static void seedSelection(Minecraft mc) {
      final int selected = selectionIndex;
      pending = mc.getSingleplayerServer().submit(() -> { machine.dataAccessForTests().set(2, selected); player(mc).containerMenu.broadcastChanges(); });
   }
   private static void selectionTick(Minecraft mc, FactoryPlacerV2ReferenceScreen screen, MachineMenu menu) throws Exception {
      if (ticks < 12 || menu.field(2) != selectionIndex || !serverMatches(mc, p -> machine.dataAccessForTests().get(2) == selectionIndex && sentinelsIntact())) return;
      var record = new JsonObject(); record.addProperty("selected_slot", selectionIndex); record.addProperty("server_confirmed", true);
      record.addProperty("state_source", "server-seeded selection render case; not a user selection gesture");
      var a = FactoryPlacerV2ReferenceGeometry.anchor(selectionIndex); var v = screen.validationViewTransform(); double scale = mc.getWindow().getGuiScale();
      var edges = new JsonObject(); var missing = new JsonArray();
      Rect[] strips = {new Rect(a.x() - 1, a.y() - 1, 18, 1), new Rect(a.x() + 16, a.y(), 1, 16), new Rect(a.x() - 1, a.y() + 16, 18, 1), new Rect(a.x() - 1, a.y(), 1, 16)};
      String[] names = {"top", "right", "bottom", "left"};
      try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         for (int edge = 0; edge < strips.length; edge++) {
            Rect r = strips[edge]; double x0 = v.screenX(r.x()) * scale, y0 = v.screenY(r.y()) * scale;
            double x1 = v.screenX(r.x() + r.width()) * scale, y1 = v.screenY(r.y() + r.height()) * scale;
            int expected = 0, matched = 0; var missingPixels = new JsonArray();
            for (int y = (int)Math.ceil(y0 - .5); y < Math.ceil(y1 - .5); y++) for (int x = (int)Math.ceil(x0 - .5); x < Math.ceil(x1 - .5); x++) {
               expected++; int rgb = frame.getPixelRGBA(x, y) & 0xffffff;
               int argb = com.oblixorprime.engineersdecorreforged.utility.FactoryPlacerV2ReferenceState.SELECTED_COLOR;
               int abgr = ((argb & 255) << 16) | (argb & 0xff00) | ((argb >>> 16) & 255);
               if (rgb == abgr) matched++; else { var pixel = new JsonArray(); pixel.add(x); pixel.add(y); missingPixels.add(pixel); }
            }
            var e = new JsonObject(); e.addProperty("expected_pixels", expected); e.addProperty("matched_pixels", matched); e.add("missing_pixels", missingPixels);
            e.addProperty("x0", x0); e.addProperty("y0", y0); e.addProperty("x1", x1); e.addProperty("y1", y1); edges.add(names[edge], e);
            if (expected == 0 || matched != expected) missing.add(names[edge]);
         }
         saveExtraFrame(mc, "selection-" + selectionIndex, record, frame);
      }
      record.add("edges", edges); record.add("missing_edges", missing); record.addProperty("full_outline_visible", missing.isEmpty());
      record.addProperty("raster_criterion", "pixel centers inside four transformed one-logical-pixel outline strips; zero-pixel edges fail"); SELECTIONS.add(record);
      if (++selectionIndex < 18) { seedSelection(mc); enter(Stage.SELECTIONS); }
      else enter(Stage.DONE);
   }
   private static void saveExtraFrame(Minecraft mc, String label, JsonObject record) throws Exception {
      try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { saveExtraFrame(mc, label, record, frame); }
   }
   private static void saveExtraFrame(Minecraft mc, String label, JsonObject record, NativeImage frame) throws Exception {
      Path file = directory.resolve("factory-placer-v2-reference-screenshots").resolve(label + ".png"); require(!Files.exists(file), "Stale additional capture");
      frame.writeToFile(file); addViewport(record, mc); record.addProperty("name", file.getFileName().toString()); record.addProperty("sha256", hash(file));
      record.addProperty("width", frame.getWidth()); record.addProperty("height", frame.getHeight());
      if (mc.screen instanceof FactoryPlacerV2ReferenceScreen screen) {
         var v = screen.validationViewTransform(); record.addProperty("view_scale", v.scale());
         record.addProperty("view_origin_x", v.originX()); record.addProperty("view_origin_y", v.originY());
         record.addProperty("menu_id", screen.getMenu().containerId);
      }
   }

   private static void boot(Minecraft mc) throws Exception {
      require(!Boolean.getBoolean("iedctr.clientGameplay") && Boolean.getBoolean("iedctr.dev.factoryPlacerV2Reference"), "Dedicated fixture only; V2 must be enabled");
      nonce = System.getProperty("iedctr.clientGameplayNonce", "");
      require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Missing nonce or exact revision");
      directory = mc.gameDirectory.toPath().toRealPath();
      require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
         && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Isolation marker mismatch");
      require(mc.level == null && mc.getSingleplayerServer() == null && !Files.exists(directory.resolve("factory-placer-v2-reference-screenshots")), "Refusing existing world/captures");
      verifyViewport(mc);
      packagedRuntime = PackagedRuntimeProof.verify(mc);
      if (Files.exists(directory.resolve("saves"))) try (var entries = Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(), "Existing saves forbidden"); }
      String name = "iedctr-factory-placer-v2-" + nonce;
      mc.options.pauseOnLostFocus = false; enter(Stage.WORLD);
      mc.createWorldOpenFlows().createFreshLevel(name,
         new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
         new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
   }

   private static void setup(Minecraft mc) {
      var p = player(mc); p.setGameMode(GameType.SURVIVAL); var level = p.serverLevel();
      level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, mc.getSingleplayerServer()); level.setDayTime(6000);
      for (int x = -2; x <= 2; x++) for (int z = -1; z <= 6; z++) level.setBlock(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState(), 3);
      p.connection.teleport(.5, 80, 4.5, 180, 18);
      level.setBlock(TARGET, Blocks.AIR.defaultBlockState(), 3);
      level.setBlock(POS, ModBlocks.FACTORY_PLACER.get().defaultBlockState().setValue(MachineBlocks.FACING, Direction.SOUTH), 3);
      require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Missing Factory Placer entity");
      machine = (MachineBlockEntity)level.getBlockEntity(POS);
      require(machine.getContainerSize() == 27, "Saved storage size changed");
      // Default logic6 is automatic. Disable it before inserting any fixture stack.
      machine.dataAccessForTests().set(0, 0); machine.dataAccessForTests().set(2, 0); machine.clearContent();
      for (int i = 18; i < 27; i++) machine.setItem(i, new ItemStack(Items.GOLD_BLOCK, i - 17));
      hiddenBefore = hiddenSnapshot(); p.getInventory().clearContent(); p.openMenu(machine);
      require(p.containerMenu instanceof MachineMenu m && !m.factoryPlacerV2ReferencePresentation() && m.slots.size() == 54 && m.visibleMachineSlots() == 18, "Server projection changed");
      serverMenuId = p.containerMenu.containerId;
   }

   private static void prepareSlot(Minecraft mc) {
      pending = mc.getSingleplayerServer().submit(() -> {
         var p = player(mc); clearVisible(p); machine.dataAccessForTests().set(0, 0);
         p.containerMenu.getSlot(slotIndex).set(new ItemStack(Items.STONE)); p.containerMenu.broadcastChanges();
         require(total(p) == 46 && sentinelsIntact(), "Route fixture conservation differs");
         if (slotIndex == 0) initialTotal = total(p);
      }); enter(Stage.SLOT_READY);
   }
   private static void clearVisible(ServerPlayer p) {
      p.getInventory().clearContent(); for (int i = 0; i < 18; i++) machine.setItem(i, ItemStack.EMPTY);
      require(p.containerMenu.getCarried().isEmpty(), "Refusing to discard carried items"); require(sentinelsIntact(), "Hidden saved slots changed");
   }
   private static boolean stone(ItemStack stack, int count) { return stack.is(Items.STONE) && stack.getCount() == count; }
   private static int total(ServerPlayer p) {
      int n = p.containerMenu.getCarried().getCount();
      // Count backing inventories once, rather than double-counting visible menu slots.
      for (int i = 0; i < machine.getContainerSize(); i++) n += machine.getItem(i).getCount();
      for (int i = 0; i < p.getInventory().getContainerSize(); i++) n += p.getInventory().getItem(i).getCount();
      return n;
   }
   private static boolean sentinelsIntact() {
      for (int i = 18; i < 27; i++) if (!machine.getItem(i).is(Items.GOLD_BLOCK) || machine.getItem(i).getCount() != i - 17) return false;
      return true;
   }
   private static JsonArray hiddenSnapshot() {
      require(sentinelsIntact(), "Hidden saved storage was not preserved"); var result = new JsonArray();
      for (int i = 18; i < 27; i++) { var row = new JsonObject(); row.addProperty("slot", i); row.addProperty("item", "minecraft:gold_block"); row.addProperty("count", machine.getItem(i).getCount()); result.add(row); }
      return result;
   }
   private static int drops(ServerPlayer p) { return p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12)).size(); }
   private static boolean clientState(MachineMenu menu, int logic, int selected) { return menu.field(0) == logic && menu.field(1) == 0 && menu.field(2) == selected; }
   private static boolean controlState(ServerPlayer p, int logic, int selected) {
      return machine.dataAccessForTests().get(0) == logic && machine.dataAccessForTests().get(1) == 0
         && machine.dataAccessForTests().get(2) == selected && total(p) == 45 && sentinelsIntact() && drops(p) == 0;
   }
   private static boolean preservedAfterPlacement(ServerPlayer p) {
      return machine.dataAccessForTests().get(0) == 0 && machine.dataAccessForTests().get(2) == 1 && stone(machine.getItem(0), 1)
         && p.serverLevel().getBlockState(TARGET).is(Blocks.STONE) && total(p) == 46 && sentinelsIntact() && drops(p) == 0;
   }
   private static void beginControl(FactoryPlacerV2ReferenceScreen screen) { clickControl(screen, STEPS[controlIndex].control()); enter(Stage.CONTROL); }
   private static void clickSlot(FactoryPlacerV2ReferenceScreen screen, int index) {
      var a = FactoryPlacerV2ReferenceGeometry.anchor(index); click(screen, a.x() + 8, a.y() + 8); lastSlotClickNanos = System.nanoTime();
   }
   private static void clickControl(FactoryPlacerV2ReferenceScreen screen, Control control) {
      var r = FactoryPlacerV2ReferenceGeometry.controlRect(control); click(screen, r.x() + r.width() / 2.0, r.y() + r.height() / 2.0);
   }
   private static void click(FactoryPlacerV2ReferenceScreen screen, double x, double y) {
      var v = screen.validationViewTransform(); screen.mouseClicked(v.screenX(x), v.screenY(y), 0); screen.mouseReleased(v.screenX(x), v.screenY(y), 0);
   }
   private static void parkCursor(Minecraft mc) { GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), mc.getWindow().getScreenWidth() - 2, mc.getWindow().getScreenHeight() - 2); }
   private static void verifyGeometry(FactoryPlacerV2ReferenceScreen screen, MachineMenu menu) {
      verifyViewport(Minecraft.getInstance());
      require(menu.factoryPlacerV2ReferencePresentation() && menu.kind() == MachineKind.FACTORY_PLACER && menu.slots.size() == 54 && menu.visibleMachineSlots() == 18, "Wrong native menu");
      var v = screen.validationViewTransform();
      require(screen.getGuiLeft() == v.guiLeft() && screen.getGuiTop() == v.guiTop() && screen.getXSize() == v.guiWidth() && screen.getYSize() == v.guiHeight(), "Public transformed bounds differ");
      for (var a : FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS) {
         var s = menu.getSlot(a.menuIndex()); int index = a.menuIndex() < 18 ? a.menuIndex() : a.menuIndex() < 45 ? a.menuIndex() - 9 : a.menuIndex() - 45;
         require(s.getSlotIndex() == index && s.x == a.x() && s.y == a.y(), "Slot backing mapping differs");
         require(Math.abs(v.localX(v.screenX(a.x() + 8)) - a.x() - 8) < .00001 && Math.abs(v.localY(v.screenY(a.y() + 8)) - a.y() - 8) < .00001, "Pointer inverse differs");
         require(screen.validationSlotHoveredAt(a.menuIndex(), v.screenX(a.x() + 8), v.screenY(a.y() + 8)), "Slot centre not hoverable");
         require(!screen.validationSlotHoveredAt(a.menuIndex(), v.screenX(a.hoverLeft() - .01), v.screenY(a.y() + 8))
            && !screen.validationSlotHoveredAt(a.menuIndex(), v.screenX(a.hoverRight() + .01), v.screenY(a.y() + 8))
            && !screen.validationSlotHoveredAt(a.menuIndex(), v.screenX(a.x() + 8), v.screenY(a.hoverTop() - .01))
            && !screen.validationSlotHoveredAt(a.menuIndex(), v.screenX(a.x() + 8), v.screenY(a.hoverBottom() + .01)), "Slot boundary escaped");
      }
   }
   private static void verifyViewport(Minecraft mc) {
      int requested = Integer.getInteger("iedctr.requestedGuiScale", 0); var w = mc.getWindow(); double actual = w.getGuiScale();
      require(requested >= 1 && requested <= 3 && mc.options.guiScale().get() == requested && actual == requested, "Requested GUI scale was not applied");
      require(w.getWidth() == Integer.getInteger("iedctr.requestedWidth", 0) && w.getHeight() == Integer.getInteger("iedctr.requestedHeight", 0), "Requested framebuffer size was not applied");
      require(w.getGuiScaledWidth() == (int)Math.ceil(w.getWidth() / actual) && w.getGuiScaledHeight() == (int)Math.ceil(w.getHeight() / actual), "Scaled GUI dimensions differ");
   }
   private static void addViewport(JsonObject proof, Minecraft mc) {
      var w = mc.getWindow(); proof.addProperty("requested_gui_scale", Integer.getInteger("iedctr.requestedGuiScale", 0)); proof.addProperty("actual_gui_scale", w.getGuiScale());
      proof.addProperty("gui_width", w.getGuiScaledWidth()); proof.addProperty("gui_height", w.getGuiScaledHeight()); proof.addProperty("framebuffer_width", w.getWidth()); proof.addProperty("framebuffer_height", w.getHeight());
   }
   private static void capture(Minecraft mc, MachineMenu menu, String label) throws Exception {
      var screen = (FactoryPlacerV2ReferenceScreen)mc.screen; var v = screen.validationViewTransform(); double scale = mc.getWindow().getGuiScale();
      Path folder = directory.resolve("factory-placer-v2-reference-screenshots"); Files.createDirectories(folder); Path file = folder.resolve(label + ".png"); require(!Files.exists(file), "Stale capture");
      var c = new JsonObject(); addViewport(c, mc);
      c.addProperty("presentation_gui_left", screen.getGuiLeft()); c.addProperty("presentation_gui_top", screen.getGuiTop()); c.addProperty("presentation_gui_width", screen.getXSize()); c.addProperty("presentation_gui_height", screen.getYSize());
      try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         frame.writeToFile(file); c.addProperty("width", frame.getWidth()); c.addProperty("height", frame.getHeight());
         require(frame.getWidth() == mc.getWindow().getWidth() && frame.getHeight() == mc.getWindow().getHeight(), "Native framebuffer differs");
         int x0 = (int)Math.ceil(v.screenX(CONTROL_CROP.x()) * scale), y0 = (int)Math.ceil(v.screenY(CONTROL_CROP.y()) * scale);
         int x1 = (int)Math.floor(v.screenX(CONTROL_CROP.x() + CONTROL_CROP.width()) * scale), y1 = (int)Math.floor(v.screenY(CONTROL_CROP.y() + CONTROL_CROP.height()) * scale);
         Path cropFile = folder.resolve(label + "-controls.png");
         try (var crop = new NativeImage(x1 - x0, y1 - y0, false)) { for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) crop.setPixelRGBA(x - x0, y - y0, frame.getPixelRGBA(x, y)); crop.writeToFile(cropFile); }
         c.addProperty("controls_name", cropFile.getFileName().toString()); c.addProperty("controls_sha256", hash(cropFile));
         c.addProperty("controls_x", x0); c.addProperty("controls_y", y0); c.addProperty("controls_width", x1 - x0); c.addProperty("controls_height", y1 - y0);
         var digest = MessageDigest.getInstance("SHA-256");
         double artWidth = FactoryPlacerV2ReferenceGeometry.SOURCE_WIDTH * FactoryPlacerV2ReferenceGeometry.SOURCE_SCALE;
         double artHeight = FactoryPlacerV2ReferenceGeometry.SOURCE_HEIGHT * FactoryPlacerV2ReferenceGeometry.SOURCE_SCALE;
         for (int y = (int)Math.ceil(v.screenY(0) * scale); y < (int)Math.floor(v.screenY(artHeight) * scale); y++)
            for (int x = (int)Math.ceil(v.screenX(0) * scale); x < (int)Math.floor(v.screenX(artWidth) * scale); x++) {
               if (excludedPixel(x, y, v, scale)) continue; int pixel = frame.getPixelRGBA(x, y); for (int b = 0; b < 4; b++) digest.update((byte)(pixel >>> (8 * b)));
            }
         c.addProperty("frame_exterior_sha256", HexFormat.of().formatHex(digest.digest()));
      }
      c.addProperty("name", file.getFileName().toString()); c.addProperty("sha256", hash(file)); c.addProperty("logic", menu.field(0)); c.addProperty("signal", menu.field(1)); c.addProperty("selected_slot", menu.field(2));
      c.addProperty("menu_id", menu.containerId); c.addProperty("screen_class", screen.getClass().getName()); c.addProperty("view_scale", v.scale()); CAPTURES.add(c);
   }
   private static boolean excludedPixel(int x, int y, ViewTransform v, double scale) {
      for (var a : FactoryPlacerV2ReferenceGeometry.SLOT_ANCHORS) if (inRect(x, y, v, scale, new Rect(a.x() - 1, a.y() - 1, 18, 18))) return true;
      for (int row = 0; row < 3; row++) if (inRect(x, y, v, scale, FactoryPlacerV2ReferenceGeometry.rowIndicator(row))) return true;
      return inRect(x, y, v, scale, FactoryPlacerV2ReferenceGeometry.SIGNAL_INDICATOR)
         || inRect(x, y, v, scale, FactoryPlacerV2ReferenceGeometry.INVERSION_INDICATOR)
         || inRect(x, y, v, scale, FactoryPlacerV2ReferenceGeometry.TRIGGER_INDICATOR);
   }
   private static boolean inRect(int x, int y, ViewTransform v, double scale, Rect r) {
      return x >= Math.floor(v.screenX(r.x()) * scale) - 2 && x <= Math.ceil(v.screenX(r.x() + r.width()) * scale) + 2
         && y >= Math.floor(v.screenY(r.y()) * scale) - 2 && y <= Math.ceil(v.screenY(r.y() + r.height()) * scale) + 2;
   }
   private static JsonArray targetJson() { var a = new JsonArray(); a.add(TARGET.getX()); a.add(TARGET.getY()); a.add(TARGET.getZ()); return a; }
   private static void captureWorld(Minecraft mc) throws Exception {
      Path file = directory.resolve("factory-placer-v2-reference-screenshots/placement-world.png"); require(!Files.exists(file) && mc.screen == null, "World capture not current");
      worldCapture = new JsonObject(); addViewport(worldCapture, mc);
      try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { frame.writeToFile(file); worldCapture.addProperty("width", frame.getWidth()); worldCapture.addProperty("height", frame.getHeight()); }
      worldCapture.addProperty("name", file.getFileName().toString()); worldCapture.addProperty("sha256", hash(file)); worldCapture.addProperty("screen_class", "none");
      worldCapture.addProperty("target_block", "minecraft:stone"); worldCapture.add("target", targetJson()); worldCapture.addProperty("server_confirmed", true);
   }
   private static String hash(Path file) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); }
   private static void enter(Stage next) { stage = next; ticks = 0; inspection = null; }
   private static boolean ready() { if (pending == null) return true; if (!pending.isDone()) return false; pending.join(); pending = null; return true; }
   private static ServerPlayer player(Minecraft mc) { var p = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId); require(p != null, "Player vanished"); return p; }
   private static boolean serverMatches(Minecraft mc, Predicate<ServerPlayer> test) {
      if (inspection == null) { inspection = mc.getSingleplayerServer().submit(() -> test.test(player(mc))); return false; }
      if (!inspection.isDone()) return false; boolean result = inspection.join(); inspection = null; return result;
   }
   private static void finish(Minecraft mc, Throwable error) {
      complete = true;
      if (display != null && shiftDown) nativeShift(false);
      if (display != null) { x11.getFunction("XCloseDisplay").invokeInt(new Object[]{display}); display = null; }
      try {
         if (directory != null) {
            var p = new JsonObject(); p.addProperty("suite", SUITE); p.addProperty("qualification_suite", "native-factory-placer-delivery-v1"); p.addProperty("status", error == null ? "passed_bounded_smoke" : "failed");
            p.add("packaged_runtime", packagedRuntime);
            p.addProperty("nonce", nonce); p.addProperty("executed_source_sha", System.getProperty("iedctr.executedSourceSha"));
            p.addProperty("selected_language", mc.getLanguageManager().getSelected()); p.addProperty("source_art_sha256", FactoryPlacerV2ReferenceGeometry.SOURCE_SHA256); addViewport(p, mc);
            p.addProperty("actual_integrated_world", mc.getSingleplayerServer() != null); p.addProperty("native_visual_acceptance", false);
            p.addProperty("menu_slot_count", 54); p.addProperty("machine_saved_slot_count", 27); p.addProperty("machine_visible_slot_count", 18);
            p.addProperty("input_method", "Base routes: Screen.mouseClicked/mouseReleased with vanilla packets and independent server observation, not physical OS clicks. Additional cases disclose XTest modifier/hover separately.");
            p.addProperty("route_pickup_server_acks", pickupAcks); p.addProperty("route_return_server_acks", returnAcks); p.addProperty("manual_trigger_server_acks", manualAcks);
            p.addProperty("initial_item_total", initialTotal); p.addProperty("final_item_total", finalTotal);
            p.add("hidden_slot_sentinels_before", hiddenBefore); p.add("hidden_slot_sentinels_after", hiddenAfter);
            p.add("advanced_gestures", ADVANCED); p.add("tooltip_captures", TOOLTIPS); p.add("selection_captures", SELECTIONS);
            p.add("slot_routes", ROUTES); p.add("control_routes", CONTROLS); p.add("captures", CAPTURES); p.add("cases", CASES); p.add("placement", placement); p.add("world_capture", worldCapture);
            if (error != null) p.addProperty("error", error.toString());
            Files.writeString(directory.resolve("client-factory-placer-v2-reference-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(p) + "\n");
         }
      } catch (Exception writeError) { EngineersDecorReforged.LOGGER.error("Factory Placer V2 proof write failed", writeError); }
      if (error == null) EngineersDecorReforged.LOGGER.info("IEDCTR_FACTORY_PLACER_V2_REFERENCE PASSED nonce={}", nonce);
      else EngineersDecorReforged.LOGGER.error("IEDCTR_FACTORY_PLACER_V2_REFERENCE FAILED at " + stage, error);
      mc.stop();
   }
   private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
