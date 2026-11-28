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
      "immersive_engineer_decor_controls_tool_reforged", "textures/gui/gui11_v5c/18.png");
   private static final int PANEL_WIDTH = 368;
   private static final int PANEL_HEIGHT = 207;
   private static final int SOURCE_WIDTH = 1672;
   private static final int SOURCE_HEIGHT = 941;
   private int panelLeft, panelTop, panelWidth, panelHeight;
   private float panelScale;

   private int panelX(float x) { return panelLeft + Math.round(x * panelScale); }
   private int panelY(float y) { return panelTop + Math.round(y * panelScale); }
   private int panelDimension(float value) { return Math.max(1, Math.round(value * panelScale)); }

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
      panelScale = Math.min(1.0F, Math.min(Math.max(1, this.width - 24) / (float)PANEL_WIDTH,
         Math.max(1, this.height - 24) / (float)PANEL_HEIGHT));
      panelWidth = Math.max(1, Math.round(PANEL_WIDTH * panelScale));
      panelHeight = Math.max(1, Math.round(PANEL_HEIGHT * panelScale));
      panelLeft = (this.width - panelWidth) / 2;
      panelTop = (this.height - panelHeight) / 2;
      int boxWidth = panelDimension(239.8F);
      int left = panelX(64.24F);
      this.firstLine = new EditBox(this.font, left, panelY(75.68F), boxWidth, Math.max(12, panelDimension(17.38F)),
         Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label.line1"));
      this.secondLine = new EditBox(this.font, left, panelY(111.76F), boxWidth, Math.max(12, panelDimension(17.6F)),
         Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label.line2"));
      this.firstLine.setBordered(false);
      this.secondLine.setBordered(false);
      this.firstLine.setMaxLength(MachineBlockEntity.LABEL_MAX_UTF16_UNITS);
      this.secondLine.setMaxLength(MachineBlockEntity.LABEL_MAX_UTF16_UNITS);
      this.firstLine.setValue(line0);
      this.secondLine.setValue(line1);
      this.addRenderableWidget(this.firstLine);
      this.addRenderableWidget(this.secondLine);
      this.addRenderableWidget(new PlaqueButton(panelX(76.56F), panelY(146.3F), panelDimension(102.74F), panelDimension(31.68F),
         Component.translatable("gui.done"), button -> this.saveAndClose()));
      this.addRenderableWidget(new PlaqueButton(panelX(188.32F), panelY(146.3F), panelDimension(103.62F), panelDimension(31.68F),
         Component.translatable("gui.cancel"), button -> this.onClose()));
      this.setInitialFocus(this.firstLine);
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      // One background pass, then retained faceplate, then native widgets and foreground labels.
      super.renderBackground(graphics, mouseX, mouseY, partialTick);
      graphics.blit(FACEPLATE, panelLeft, panelTop, panelWidth, panelHeight, 0, 0,
         SOURCE_WIDTH, SOURCE_HEIGHT, SOURCE_WIDTH, SOURCE_HEIGHT);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(this.font, this.title, panelX(PANEL_WIDTH / 2.0F), panelY(24), 16777215);
      if (mouseX >= panelX(64.24F) && mouseX < panelX(64.24F + 239.8F)
         && ((mouseY >= panelY(75.68F) && mouseY < panelY(75.68F + 17.38F))
            || (mouseY >= panelY(111.76F) && mouseY < panelY(111.76F + 17.6F)))) {
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
