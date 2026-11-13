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
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LabeledCrateEditScreen extends Screen {
   private final BlockPos pos;
   private final String[] initialLines = new String[]{"", ""};
   private EditBox firstLine;
   private EditBox secondLine;

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
      int boxWidth = Mth.clamp(this.width - 48, 120, 180);
      int left = (this.width - boxWidth) / 2;
      int top = Mth.clamp(this.height / 2 - 58, 18, Math.max(18, this.height - 126));
      this.firstLine = new EditBox(this.font, left, top + 30, boxWidth, 20, Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label.line1"));
      this.secondLine = new EditBox(
         this.font, left, top + 56, boxWidth, 20, Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label.line2")
      );
      this.firstLine.setMaxLength(MachineBlockEntity.LABEL_MAX_UTF16_UNITS);
      this.secondLine.setMaxLength(MachineBlockEntity.LABEL_MAX_UTF16_UNITS);
      this.firstLine.setValue(line0);
      this.secondLine.setValue(line1);
      this.addRenderableWidget(this.firstLine);
      this.addRenderableWidget(this.secondLine);
      int buttonWidth = Math.max(58, (boxWidth - 8) / 2);
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), button -> this.saveAndClose()).bounds(left, top + 90, buttonWidth, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
            .bounds(left + boxWidth - buttonWidth, top + 90, buttonWidth, 20)
            .build()
      );
      this.setInitialFocus(this.firstLine);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      int top = Mth.clamp(this.height / 2 - 58, 18, Math.max(18, this.height - 126));
      graphics.drawCenteredString(this.font, this.title, this.width / 2, top, 16777215);
      graphics.drawCenteredString(
         this.font, Component.translatable("screen.immersive_engineer_decor_controls_tool_reforged.labeled_crate_label.hint"), this.width / 2, top + 17, 10526880
      );
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 257 && keyCode != 335) {
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
