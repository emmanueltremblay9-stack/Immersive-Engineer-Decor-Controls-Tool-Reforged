package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.network.LabeledCrateLabelPayload;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.components.Tooltip;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LabeledCrateEditScreen extends Screen {
   private final BlockPos pos;
   private final String[] initialLines = new String[]{"", ""};
   private EditBox firstLine;
   private EditBox secondLine;
   private static final ResourceLocation FACEPLATE = ResourceLocation.fromNamespaceAndPath(
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/label_editor_reference_v1/background.png");
   private int panelLeft, panelTop, panelWidth, panelHeight;

   private int sourceX(int x) { return panelLeft + Math.round(x * panelWidth / 1536.0F); }
   private int sourceY(int y) { return panelTop + Math.round(y * panelHeight / 1024.0F); }
   private int sourceWidth(int w) { return Math.max(1, Math.round(w * panelWidth / 1536.0F)); }
   private int sourceHeight(int h) { return Math.max(1, Math.round(h * panelHeight / 1024.0F)); }

   /** Native Button input/focus/narration, with the retained artwork as its visual face. */
   private static final class PlaqueButton extends Button {
      PlaqueButton(int x, int y, int width, int height, Component label, OnPress action) {
         super(x, y, width, height, label, action, DEFAULT_NARRATION);
         setTooltip(Tooltip.create(label));
      }
      @Override
      public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFD080);
      }
   }

   public LabeledCrateEditScreen(BlockPos pos, List<String> lines) {
      super(Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label"));
      this.pos = pos;

      for (int i = 0; i < Math.min(this.initialLines.length, lines.size()); i++) {
         this.initialLines[i] = MachineBlockEntity.sanitizeLabelLine(lines.get(i));
      }
   }

   protected void init() {
      // Screen reinitialization (for example a resize) must retain unsaved label text.
      String line0 = this.firstLine == null ? this.initialLines[0] : this.firstLine.getValue();
      String line1 = this.secondLine == null ? this.initialLines[1] : this.secondLine.getValue();
      float scale = Math.min(0.25F, Math.min(Math.max(1, this.width - 12) / 1536.0F,
         Math.max(1, this.height - 12) / 1024.0F));
      panelWidth = Math.max(1, (int)(1536 * scale));
      panelHeight = Math.max(1, (int)(1024 * scale));
      panelLeft = (this.width - panelWidth) / 2;
      panelTop = (this.height - panelHeight) / 2;
      int boxWidth = sourceWidth(1120);
      int left = sourceX(208);
      this.firstLine = new EditBox(this.font, left, sourceY(438) - 4, boxWidth, 12,
         Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label.line1"));
      this.secondLine = new EditBox(this.font, left, sourceY(607) - 4, boxWidth, 12,
         Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label.line2"));
      this.firstLine.setBordered(false);
      this.secondLine.setBordered(false);
      this.firstLine.setMaxLength(MachineBlockEntity.LABEL_MAX_UTF16_UNITS);
      this.secondLine.setMaxLength(MachineBlockEntity.LABEL_MAX_UTF16_UNITS);
      this.firstLine.setValue(line0);
      this.secondLine.setValue(line1);
      this.addRenderableWidget(this.firstLine);
      this.addRenderableWidget(this.secondLine);
      this.addRenderableWidget(new PlaqueButton(sourceX(320), sourceY(760), sourceWidth(405), sourceHeight(85),
         Component.translatable("gui.done"), button -> this.saveAndClose()));
      this.addRenderableWidget(new PlaqueButton(sourceX(810), sourceY(760), sourceWidth(405), sourceHeight(85),
         Component.translatable("gui.cancel"), button -> this.onClose()));
      this.setInitialFocus(this.firstLine);
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      // One background pass, then retained faceplate, then native widgets and foreground labels.
      super.renderBackground(graphics, mouseX, mouseY, partialTick);
      graphics.blit(FACEPLATE, panelLeft, panelTop, panelWidth, panelHeight, 0, 0, 1536, 1024, 1536, 1024);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, sourceY(134) - 4, 16777215);
      if (mouseY >= sourceY(270) && mouseY < sourceY(340) && mouseX >= sourceX(208) && mouseX < sourceX(1328)) {
         graphics.renderTooltip(this.font,
            Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label.hint"), mouseX, mouseY);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      // Focused buttons retain vanilla Enter/keypad/Space activation semantics.
      if (this.getFocused() instanceof Button || (keyCode != 257 && keyCode != 335)) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }

      this.saveAndClose();
      return true;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void saveAndClose() {
      String line0 = this.firstLine.getValue();
      String line1 = this.secondLine.getValue();
      if (this.minecraft != null && this.minecraft.level != null && this.minecraft.level.getBlockEntity(this.pos) instanceof MachineBlockEntity machine) {
         machine.setLabeledCrateLabel(line0, line1);
      }

      PacketDistributor.sendToServer(new LabeledCrateLabelPayload(this.pos, line0, line1), new CustomPacketPayload[0]);
      this.onClose();
   }
}
