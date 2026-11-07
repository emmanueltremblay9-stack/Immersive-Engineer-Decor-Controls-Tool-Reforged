package com.oblixorprime.engineersdecorreforged.client.validation;

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
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class NativeFactoryPlacerV2ReferenceValidation {
   private static final String SUITE = "native-factory-placer-v2-reference-v1";
   private static final BlockPos POS = new BlockPos(0, 80, 0), TARGET = POS.south();
   private static final Rect CONTROL_CROP = new Rect(240, 55, 125, 120);
   private enum Stage { BOOT, WORLD, SETUP, SLOT_READY, PICKED, RETURNED, NORMAL, CONTROL,
      MANUAL_LIT, MANUAL_DARK, PLACE_READY, PLACE_WAIT, PLACED_CAPTURE, CLOSE, WORLD_CAPTURE,
      REOPEN, RELOAD_WAIT, RELOAD_CAPTURE, DONE }
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
   private static JsonObject placement, worldCapture;
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
               CASES.add("resource_reload_preserves_inventory_and_state"); CASES.add("hidden_9_saved_slots_preserved"); enter(Stage.DONE);
            }
            case DONE -> finish(mc, null);
            default -> throw new IllegalStateException("Unexpected stage " + stage);
         }
      } catch (Throwable error) { finish(mc, error); }
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
      p.containerMenu.setCarried(ItemStack.EMPTY); require(sentinelsIntact(), "Hidden saved slots changed");
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
      try {
         if (directory != null) {
            var p = new JsonObject(); p.addProperty("suite", SUITE); p.addProperty("status", error == null ? "passed_bounded_smoke" : "failed");
            p.addProperty("nonce", nonce); p.addProperty("executed_source_sha", System.getProperty("iedctr.executedSourceSha"));
            p.addProperty("selected_language", mc.getLanguageManager().getSelected()); p.addProperty("source_art_sha256", FactoryPlacerV2ReferenceGeometry.SOURCE_SHA256); addViewport(p, mc);
            p.addProperty("actual_integrated_world", mc.getSingleplayerServer() != null); p.addProperty("native_visual_acceptance", false);
            p.addProperty("menu_slot_count", 54); p.addProperty("machine_saved_slot_count", 27); p.addProperty("machine_visible_slot_count", 18);
            p.addProperty("input_method", "Screen.mouseClicked/mouseReleased with vanilla packets and independent server observation; GLFW cursor parking only, not physical OS input");
            p.addProperty("route_pickup_server_acks", pickupAcks); p.addProperty("route_return_server_acks", returnAcks); p.addProperty("manual_trigger_server_acks", manualAcks);
            p.addProperty("initial_item_total", initialTotal); p.addProperty("final_item_total", finalTotal);
            p.add("hidden_slot_sentinels_before", hiddenBefore); p.add("hidden_slot_sentinels_after", hiddenAfter);
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
