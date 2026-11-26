package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.network.MachineActionPayload;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry.Control;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry.Rect;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyState;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyState.Action;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Approved original artwork with native slots, synchronized overlays and accessible controls. */
public final class FactoryStudyScreen extends AbstractContainerScreen<MachineMenu> {
   private static final String MOD = "immersive_engineer_decor_controls_tool_reforged";
   private static final String PREFIX = "gui." + MOD + ".factory_study.";
   private final MachineKind kind;
   private final ResourceLocation faceplate;
   private final EnumMap<Control, StudyControl> controls = new EnumMap<>(Control.class);
   private FactoryStudyGeometry.ViewTransform view = FactoryStudyGeometry.fit(316, 312);
   private boolean renderingCanvas;
   private double localPointerX, localPointerY;
   private StudyControl controlGesture;

   public FactoryStudyScreen(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      if (!menu.factoryStudyPresentation()) throw new IllegalArgumentException("Factory Study requires a supported client presentation");
      kind = menu.kind();
      faceplate = ResourceLocation.fromNamespaceAndPath(MOD, FactoryStudyGeometry.texturePath(kind));
      imageWidth = FactoryStudyGeometry.CANVAS_WIDTH;
      imageHeight = FactoryStudyGeometry.CANVAS_HEIGHT;
   }

   @Override protected void init() {
      super.init();
      leftPos = 0;
      topPos = 0;
      view = FactoryStudyGeometry.fit(width, height);
      controls.clear();
      controlGesture = null;
      for (Control control : FactoryStudyGeometry.controls(kind)) {
         StudyControl widget = new StudyControl(control);
         controls.put(control, widget);
         addRenderableWidget(widget);
      }
   }

   @Override public int getGuiLeft() { return view.guiLeft(); }
   @Override public int getGuiTop() { return view.guiTop(); }
   @Override public int getXSize() { return view.guiWidth(); }
   @Override public int getYSize() { return view.guiHeight(); }

   @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBg(graphics, partialTick, mouseX, mouseY);
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, width, height, 0xB0000000);
      localPointerX = view.localX(mouseX);
      localPointerY = view.localY(mouseY);
      for (StudyControl control : controls.values()) control.update();
      graphics.pose().pushPose();
      try {
         graphics.pose().translate(view.originX(), view.originY(), 0);
         graphics.pose().scale(view.scale(), view.scale(), 1.0F);
         renderingCanvas = true;
         super.render(graphics, view.renderPointerX(mouseX), view.renderPointerY(mouseY), partialTick);
      } finally {
         renderingCanvas = false;
         graphics.pose().popPose();
      }
      // Tooltips use native screen-space font sizing, independent of canvas fit.
      renderTooltip(graphics, mouseX, mouseY);
      if (hoveredSlot != null && hoveredSlot.hasItem()) return;
      Component tooltip = null;
      if (hoveredSlot != null && hoveredSlot.index < FactoryStudyGeometry.machineSlotCount(kind)) {
         tooltip = emptySlotTooltip(hoveredSlot.index);
      } else {
         Control hovered = FactoryStudyGeometry.controlAt(kind, localPointerX, localPointerY);
         if (hovered != null) tooltip = controls.get(hovered).description();
         else if (FactoryStudyGeometry.header(kind).contains(localPointerX, localPointerY)) tooltip = title;
         else tooltip = observationTooltip(localPointerX, localPointerY);
         if (tooltip == null && focusedControl() != null && minecraft.getLastInputType().isKeyboard())
            tooltip = focusedControl().description();
      }
      if (tooltip != null) graphics.renderTooltip(font,
         font.split(tooltip, FactoryStudyGeometry.tooltipWidth(width)), mouseX, mouseY);
   }

   private StudyControl focusedControl() {
      return getFocused() instanceof StudyControl widget ? widget : null;
   }

   @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.pose().pushPose();
      try {
         graphics.pose().scale(FactoryStudyGeometry.SOURCE_SCALE, FactoryStudyGeometry.SOURCE_SCALE, 1.0F);
         graphics.blit(faceplate, 0, 0, 0, 0,
            FactoryStudyGeometry.SOURCE_WIDTH, FactoryStudyGeometry.SOURCE_HEIGHT,
            FactoryStudyGeometry.SOURCE_WIDTH, FactoryStudyGeometry.SOURCE_HEIGHT);
      } finally {
         graphics.pose().popPose();
      }
      if (kind == MachineKind.SMALL_LAB_FURNACE) {
         renderLabState(graphics);
      } else {
         int cursor = FactoryStudyState.clamp(menu.field(kind == MachineKind.FACTORY_HOPPER ? 6 : 15),
            0, kind == MachineKind.FACTORY_HOPPER ? 17 : 11);
         var slot = FactoryStudyGeometry.anchor(kind, cursor);
         graphics.renderOutline(slot.x() - 1, slot.y() - 1, 18, 18, FactoryStudyState.SELECTED);
         for (Control control : FactoryStudyGeometry.controls(kind)) renderControlState(graphics, control);
         if (kind == MachineKind.FACTORY_DROPPER) {
            for (int filter = 0; filter < 3; filter++) {
               int status = FactoryStudyState.clamp(menu.field(12 + filter), 0, 2);
               for (int mark = 0; mark < 3; mark++) fill(graphics, FactoryStudyGeometry.filterIndicator(filter, mark),
                  mark != status ? FactoryStudyState.INACTIVE : status == 2 ? FactoryStudyState.ACTIVE
                     : status == 1 ? FactoryStudyState.SELECTED : 0xFF87909B);
            }
         }
      }
   }

   private void renderLabState(GuiGraphics graphics) {
      List<Rect> flame = FactoryStudyGeometry.flameMask();
      int minY = flame.stream().mapToInt(Rect::y).min().orElse(0);
      int bottom = flame.stream().mapToInt(Rect::bottom).max().orElse(0);
      int lit = menu.field(4) == 0 ? 0 : FactoryStudyState.fractionPixels(menu.field(0), menu.field(1), bottom - minY);
      for (Rect pixel : flame) {
         int top = Math.max(pixel.y(), bottom - lit);
         if (top < pixel.bottom()) graphics.fill(pixel.x(), top, pixel.right(), pixel.bottom(), 0xFFEAA34E);
      }
      List<Rect> progress = FactoryStudyGeometry.progressMask();
      int left = progress.stream().mapToInt(Rect::x).min().orElse(0);
      int right = progress.stream().mapToInt(Rect::right).max().orElse(0);
      int filled = FactoryStudyState.fractionPixels(menu.field(2), menu.field(3), right - left);
      for (Rect pixel : progress) {
         int end = Math.min(pixel.right(), left + filled);
         if (pixel.x() < end) graphics.fill(pixel.x(), pixel.y(), end, pixel.bottom(), FactoryStudyState.ACTIVE);
      }
      Rect energy = FactoryStudyGeometry.energy();
      int energyHeight = FactoryStudyState.fractionPixels(menu.energyStored(), menu.energyCapacity(), energy.height());
      if (energyHeight > 0) graphics.fill(energy.x(), energy.bottom() - energyHeight,
         energy.right(), energy.bottom(), 0xFFD9AC55);
   }

   private void renderControlState(GuiGraphics graphics, Control control) {
      Rect track = FactoryStudyGeometry.track(kind, control);
      int logic = menu.field(kind == MachineKind.FACTORY_HOPPER ? 2 : 5);
      if (control == Control.DIRECTION) {
         int x = track.x() + FactoryStudyState.fractionPixels(menu.field(1) + 100, 200, track.width() - 1);
         int y = track.y() + FactoryStudyState.fractionPixels(100 - menu.field(2), 200, track.height() - 1);
         graphics.fill(Math.max(track.x(), x - 2), y, Math.min(track.right(), x + 3), y + 1, FactoryStudyState.ACTIVE);
         graphics.fill(x, Math.max(track.y(), y - 2), x + 1, Math.min(track.bottom(), y + 3), FactoryStudyState.ACTIVE);
      } else if (control == Control.VELOCITY) {
         int y = track.bottom() - 1 - FactoryStudyState.fractionPixels(menu.field(0), 100, track.height() - 1);
         graphics.fill(track.x(), y, track.right(), y + 1, FactoryStudyState.ACTIVE);
      } else if (FactoryStudyState.adjustable(control)) {
         int min = control == Control.COUNT ? 1 : 0;
         int max = control == Control.RANGE ? 4 : control == Control.COUNT ? 32 : 100;
         int value = menu.field(FactoryStudyState.fieldIndex(kind, control));
         int x = track.x() + FactoryStudyState.fractionPixels(value - min, max - min, track.width() - 1);
         int y = track.y() + track.height() / 2;
         graphics.fill(Math.max(track.x(), x - 1), Math.max(track.y(), y - 2),
            Math.min(track.right(), x + 2), Math.min(track.bottom(), y + 3), FactoryStudyState.ACTIVE);
      } else if (control == Control.MANUAL) {
         if (menu.field(kind == MachineKind.FACTORY_HOPPER ? 5 : 11) != 0) {
            int x = track.x() + track.width() / 2, y = track.y() + track.height() / 2;
            graphics.fill(x - 2, y - 1, x + 2, y + 1, FactoryStudyState.SIGNAL);
            graphics.fill(x - 1, y - 2, x + 1, y + 2, FactoryStudyState.SIGNAL);
         }
      } else {
         int total = control == Control.REDSTONE ? 3 : 2;
         int selected = switch (control) {
            case REDSTONE -> kind == MachineKind.FACTORY_HOPPER
               ? (logic & 4) != 0 ? 2 : (logic & 1) != 0 ? 1 : 0
               : (logic & 32) != 0 ? 1 : (logic & 2) != 0 ? 2 : 0;
            case TRIGGER -> (logic & (kind == MachineKind.FACTORY_HOPPER ? 2 : 16)) == 0 ? 0 : 1;
            case FILTER_GATE -> logic & 1;
            default -> 0;
         };
         int y = FactoryStudyGeometry.controlBounds(kind, control).bottom() - 5;
         for (int mark = 0; mark < total; mark++) {
            int x = track.x() + mark * track.width() / total;
            int end = track.x() + (mark + 1) * track.width() / total - 1;
            graphics.fill(x, y, end, y + 2, mark == selected ? FactoryStudyState.ACTIVE : FactoryStudyState.INACTIVE);
         }
      }
   }

   private Component observationTooltip(double x, double y) {
      if (kind == MachineKind.SMALL_LAB_FURNACE) {
         if (FactoryStudyGeometry.flame().contains(x, y)) return text("flame", Math.max(0, menu.field(0)), Math.max(0, menu.field(1)));
         if (FactoryStudyGeometry.progress().contains(x, y)) {
            MutableComponent progress = text("progress", Math.max(0, menu.field(2)), Math.max(0, menu.field(3)), menu.progress());
            if (ModList.get().isLoaded("jei") && menu.getCarried().isEmpty()) progress.append("\n").append(text("recipes_help"));
            return progress;
         }
         if (FactoryStudyGeometry.energyBounds().contains(x, y))
            return text("energy", menu.energyStored(), menu.energyCapacity()).append("\n").append(text("lab_aux"));
      } else if (kind == MachineKind.FACTORY_DROPPER) {
         for (int filter = 0; filter < 3; filter++) for (int status = 0; status < 3; status++) {
            if (FactoryStudyGeometry.filterIndicator(filter, status).contains(x, y)) {
               String key = switch (FactoryStudyState.clamp(menu.field(filter + 12), 0, 2)) {
                  case 1 -> "filter_insufficient"; case 2 -> "filter_matched"; default -> "filter_empty";
               };
               return text("filter_status", filter + 1, text(key));
            }
         }
      }
      return null;
   }

   private Component emptySlotTooltip(int index) {
      String role;
      if (kind == MachineKind.FACTORY_HOPPER) role = "storage";
      else if (kind == MachineKind.FACTORY_DROPPER) role = index < 12 ? "input" : "filter";
      else role = switch (index) {
         case 0 -> "input"; case 1 -> "fuel"; case 2 -> "output";
         case 3, 4 -> "input_fifo"; case 5, 6 -> "fuel_fifo"; case 7, 8 -> "output_fifo"; default -> "aux";
      };
      MutableComponent result = text("slot", index + 1, Component.translatable("block." + MOD + "." + kind.registryName() + ".slots." + role));
      if (kind == MachineKind.FACTORY_DROPPER && index >= 12) result.append("\n").append(text("filter_slot"));
      if (kind == MachineKind.SMALL_LAB_FURNACE && index >= 9) result.append("\n").append(text("lab_aux"));
      if (kind != MachineKind.SMALL_LAB_FURNACE) result.append("\n").append(text("selected_slot",
         FactoryStudyState.clamp(menu.field(kind == MachineKind.FACTORY_HOPPER ? 6 : 15), 0,
            kind == MachineKind.FACTORY_HOPPER ? 17 : 11) + 1));
      return result;
   }

   private MutableComponent controlDescription(Control control) {
      int logic = menu.field(kind == MachineKind.FACTORY_HOPPER ? 2 : 5);
      return switch (control) {
         case RANGE -> text("range", menu.field(0));
         case PERIOD -> {
            int ticks = FactoryStudyState.periodTicks(menu.field(kind == MachineKind.FACTORY_HOPPER ? 3 : 6));
            yield text("period", ticks, String.format(Locale.ROOT, "%.1f", ticks / 20.0));
         }
         case COUNT -> text(kind == MachineKind.FACTORY_HOPPER ? "count" : "drop_count",
            menu.field(kind == MachineKind.FACTORY_HOPPER ? 1 : 4));
         case VELOCITY -> text("velocity", menu.field(0));
         case DIRECTION -> text("direction", menu.field(1), menu.field(2));
         case MANUAL -> text("manual", text(menu.field(kind == MachineKind.FACTORY_HOPPER ? 5 : 11) == 0 ? "signal_off" : "signal_on"));
         case REDSTONE -> kind == MachineKind.FACTORY_HOPPER
            ? text("redstone", text((logic & 4) != 0 ? "ignored" : (logic & 1) != 0 ? "inverted" : "normal"))
            : text("external_gate", text((logic & 32) != 0 ? "ignored" : (logic & 2) != 0 ? "and" : "or"));
         case TRIGGER -> text("trigger", text(kind == MachineKind.FACTORY_HOPPER && (logic & 4) != 0
            ? "ignored" : (logic & (kind == MachineKind.FACTORY_HOPPER ? 2 : 16)) != 0 ? "continuous" : "pulse"));
         case FILTER_GATE -> text("filter_gate", text((logic & 1) == 0 ? "any" : "all"));
      };
   }

   private static MutableComponent text(String suffix, Object... args) { return Component.translatable(PREFIX + suffix, args); }
   private static void fill(GuiGraphics graphics, Rect rect, int color) { graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), color); }
   private int requestedLogic() {
      int logic = menu.field(kind == MachineKind.FACTORY_HOPPER ? 2 : 5);
      for (StudyControl widget : controls.values()) {
         if (widget.pending != null && (widget.pending.action() == 3 || widget.pending.action() == 14)
            && Util.getMillis() - widget.pendingAt < 2500) {
            // Merge only this control's desired bits with the newest observed
            // field, so an unrelated synchronized mode is never overwritten by
            // an old full-field snapshot retained for another control.
            int mask = widget.logicMask();
            logic = (logic & ~mask) | (widget.pending.valueA() & mask);
         }
      }
      return logic;
   }
   @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }

   @Override protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
      if (renderingCanvas) { mouseX = localPointerX; mouseY = localPointerY; }
      if (!super.isHovering(x, y, w, h, mouseX, mouseY)) return false;
      if (w == 16 && h == 16) for (var anchor : FactoryStudyGeometry.anchors(kind)) {
         if (anchor.x() == x && anchor.y() == y) return anchor.containsHover(mouseX, mouseY);
      }
      return true;
   }

   @Override public boolean mouseClicked(double x, double y, int button) {
      return super.mouseClicked(view.localX(x), view.localY(y), button);
   }
   @Override public boolean mouseReleased(double x, double y, int button) {
      if (controlGesture != null && button == 0) {
         controlGesture.flush(); controlGesture = null; setDragging(false); return true;
      }
      return super.mouseReleased(view.localX(x), view.localY(y), button);
   }
   @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
      if (controlGesture != null && button == 0) {
         if (FactoryStudyState.adjustable(controlGesture.control)) controlGesture.point(view.localX(x), view.localY(y));
         return true;
      }
      return super.mouseDragged(view.localX(x), view.localY(y), button, view.localDelta(dx), view.localDelta(dy));
   }
   @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
      return super.mouseScrolled(view.localX(x), view.localY(y), sx, sy);
   }
   @Override public void mouseMoved(double x, double y) { super.mouseMoved(view.localX(x), view.localY(y)); }

   private final class StudyControl extends AbstractWidget {
      private final Control control;
      private Action pending, queued, lastSent;
      private long pendingAt, lastSentAt;

      StudyControl(Control control) {
         this(control, FactoryStudyGeometry.controlBounds(kind, control));
      }
      private StudyControl(Control control, Rect bounds) {
         super(bounds.x(), bounds.y(), bounds.width(), bounds.height(), controlDescription(control));
         this.control = control;
      }
      void update() {
         active = menu.getCarried().isEmpty();
         if (pending != null && matchesObservation(pending)) pending = null;
         if (queued != null && Util.getMillis() - lastSentAt >= 50) flush();
         setMessage(description());
      }
      MutableComponent description() {
         MutableComponent result = controlDescription(control);
         if (pending != null) result.append("\n").append(text(Util.getMillis() - pendingAt < 2500 ? "pending" : "unconfirmed"));
         return result.append("\n").append(text(!menu.getCarried().isEmpty() ? "carried_blocked"
            : control == Control.DIRECTION ? "direction_help" : "control_help"));
      }
      private boolean matchesObservation(Action action) {
         // These controls share one protocol field; a later change to a different
         // bit must not turn an earlier accepted intention into a false timeout.
         if (action.action() == 3 || action.action() == 14) {
            int mask = logicMask();
            return (menu.field(kind == MachineKind.FACTORY_HOPPER ? 2 : 5) & mask) == (action.valueA() & mask);
         }
         return FactoryStudyState.actionMatches(kind, action, menu::field);
      }
      private int logicMask() {
         return switch (control) {
            case REDSTONE -> kind == MachineKind.FACTORY_HOPPER ? 5 : 34;
            case TRIGGER -> kind == MachineKind.FACTORY_HOPPER ? 2 : 16;
            case FILTER_GATE -> 1;
            default -> 0;
         };
      }
      @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         if (isFocused() || isMouseOver(localPointerX, localPointerY)) {
            graphics.renderOutline(getX(), getY(), width, height, isFocused() ? FactoryStudyState.FOCUS : 0xFF909898);
         }
         if (pending != null) graphics.fill(getX() + 3, getY() + height - 2, getX() + width - 3, getY() + height - 1,
            Util.getMillis() - pendingAt < 2500 ? FactoryStudyState.PENDING : 0xFFD2815F);
      }
      @Override public boolean mouseClicked(double x, double y, int button) {
         return menu.getCarried().isEmpty() && super.mouseClicked(x, y, button);
      }
      @Override public void onClick(double x, double y, int button) {
         controlGesture = this;
         if (FactoryStudyState.adjustable(control) || pending == null || Util.getMillis() - pendingAt >= 2500) point(x, y);
      }
      void point(double x, double y) {
         Rect track = FactoryStudyGeometry.track(kind, control);
         Action action = FactoryStudyState.pointAction(kind, control,
            (x - track.x()) / Math.max(1, track.width() - 1),
            (y - track.y()) / Math.max(1, track.height() - 1), requestedLogic());
         request(action);
      }
      void request(Action action) {
         if (action == null || !menu.getCarried().isEmpty()) return;
         if (FactoryStudyState.adjustable(control) && action.equals(lastSent)
            && (FactoryStudyState.actionMatches(kind, action, menu::field)
               || pending != null && Util.getMillis() - pendingAt < 2500)) {
            queued = null;
            return;
         }
         if (FactoryStudyState.adjustable(control) && Util.getMillis() - lastSentAt < 50) { queued = action; return; }
         send(action);
      }
      private void send(Action action) {
         queued = null;
         lastSent = action;
         lastSentAt = Util.getMillis();
         // A signal that was already high cannot acknowledge a new manual request.
         // Manual controls show the observed signal only, never a confirmation claim.
         pending = control == Control.MANUAL ? null : action;
         pendingAt = lastSentAt;
         PacketDistributor.sendToServer(new MachineActionPayload(menu.containerId, action.action(), action.valueA(), action.valueB()));
      }
      void flush() {
         if (queued != null && menu.getCarried().isEmpty()) send(queued);
         else if (!menu.getCarried().isEmpty()) queued = null;
      }
      private Action desired() {
         // Coalesce successive keyboard/wheel intentions while the display continues
         // to use synchronized fields. This is not an optimistic server-state write.
         return queued != null ? queued : pending != null && Util.getMillis() - pendingAt < 2500 ? pending : null;
      }
      private int incrementBaseA() {
         Action desired = desired();
         return desired == null ? menu.field(FactoryStudyState.fieldIndex(kind, control)) : desired.valueA();
      }
      private int incrementBaseB() {
         Action desired = desired();
         return desired == null ? menu.field(2) : desired.valueB();
      }
      @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (!isFocused() || !active || !menu.getCarried().isEmpty()) return false;
         int logic = requestedLogic();
         if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (!FactoryStudyState.adjustable(control) && (pending == null || Util.getMillis() - pendingAt >= 2500))
               request(FactoryStudyState.pointAction(kind, control, 0, 0, logic));
            return true;
         }
         if (!FactoryStudyState.adjustable(control)) return false;
         if (keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END) {
            if (control == Control.DIRECTION) {
               if (keyCode == GLFW.GLFW_KEY_HOME) request(new Action(11, 0, 0));
               return true;
            }
            double fraction = keyCode == GLFW.GLFW_KEY_END ? 1 : 0;
            request(FactoryStudyState.pointAction(kind, control, fraction, 1 - fraction, logic));
            return true;
         }
         int dx = keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 0;
         int dy = keyCode == GLFW.GLFW_KEY_UP ? 1 : keyCode == GLFW.GLFW_KEY_DOWN ? -1 : 0;
         if (dx == 0 && dy == 0) return false;
         if (control != Control.DIRECTION && control != Control.VELOCITY) dx += dy;
         if (control == Control.VELOCITY) dy += dx;
         request(FactoryStudyState.incrementAction(kind, control, dx, dy,
            incrementBaseA(), incrementBaseB(), logic));
         return true;
      }
      @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
         if (!isMouseOver(x, y) || !active || !menu.getCarried().isEmpty()
            || !FactoryStudyState.adjustable(control) || control == Control.DIRECTION || sy == 0) return false;
         int delta = sy > 0 ? 1 : -1;
         request(FactoryStudyState.incrementAction(kind, control, delta, delta,
            incrementBaseA(), 0, menu.field(kind == MachineKind.FACTORY_HOPPER ? 2 : 5)));
         return true;
      }
      @Override protected void updateWidgetNarration(NarrationElementOutput output) {
         MutableComponent status = controlDescription(control);
         if (pending != null) status.append("\n").append(text(Util.getMillis() - pendingAt < 2500 ? "pending" : "unconfirmed"));
         output.add(NarratedElementType.TITLE, status);
         output.add(NarratedElementType.USAGE, text(control == Control.DIRECTION ? "direction_help" : "control_help"));
      }
   }

   /** Screen-space transform for integrations such as recipe viewers. */
   public FactoryStudyGeometry.ViewTransform viewTransform() { return view; }
   public boolean isItemHoveredAt(int index, double screenX, double screenY) {
      if (index < 0 || index >= menu.slots.size()) return false;
      var slot = menu.getSlot(index);
      return slot.isActive() && isHovering(slot.x, slot.y, 16, 16, view.localX(screenX), view.localY(screenY));
   }
   /** Read-only hooks used by the separate packaged native validation harness. */
   public FactoryStudyGeometry.ViewTransform validationViewTransform() { return viewTransform(); }
   public boolean validationSlotHoveredAt(int index, double screenX, double screenY) {
      return isItemHoveredAt(index, screenX, screenY);
   }
   public int validationControlCount() { return controls.size(); }
}
