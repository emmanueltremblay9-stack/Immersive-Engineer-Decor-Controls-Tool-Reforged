package com.oblixorprime.engineersdecorreforged.compat.jei;

import com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.renderer.Rect2i;

/** JEI 19.32 integration uses the same transform as the actual items and clicks. */
public final class FactoryStudyJeiHandler implements IGuiContainerHandler<FactoryStudyScreen> {
   @Override
   public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
      IClickableIngredientFactory factory, FactoryStudyScreen screen, double mouseX, double mouseY) {
      var menu = screen.getMenu();
      if (!menu.getCarried().isEmpty()) return Optional.empty();
      for (var anchor : FactoryStudyGeometry.anchors(menu.kind())) {
         var slot = menu.getSlot(anchor.menuIndex());
         if (slot.hasItem() && screen.isItemHoveredAt(anchor.menuIndex(), mouseX, mouseY)) {
            var area = absoluteArea(screen.viewTransform(),
               new FactoryStudyGeometry.Rect(anchor.x(), anchor.y(), 16, 16));
            return factory.createBuilder(slot.getItem()).buildWithArea(area);
         }
      }
      return Optional.empty();
   }

   @Override
   public Collection<IGuiClickableArea> getGuiClickableAreas(FactoryStudyScreen screen, double guiMouseX, double guiMouseY) {
      // Shared screen class: only the Lab furnace has the smelting shortcut.
      if (screen.getMenu().kind() != MachineKind.SMALL_LAB_FURNACE || !screen.getMenu().getCarried().isEmpty())
         return List.of();
      var view = screen.viewTransform();
      double x = view.localX(guiMouseX + screen.getGuiLeft());
      double y = view.localY(guiMouseY + screen.getGuiTop());
      if (!FactoryStudyGeometry.progress().contains(x, y)) return List.of();
      var absolute = absoluteArea(view, FactoryStudyGeometry.progress());
      var relative = new Rect2i(absolute.getX() - screen.getGuiLeft(), absolute.getY() - screen.getGuiTop(),
         absolute.getWidth(), absolute.getHeight());
      return List.of(new IGuiClickableArea() {
         @Override public Rect2i getArea() { return relative; }
         // The screen's localized progress tooltip includes the JEI hint. Avoid a second tooltip on top of it.
         @Override public boolean isTooltipEnabled() { return false; }
         @Override public void onClick(IFocusFactory focusFactory, IRecipesGui recipesGui) {
            recipesGui.showTypes(List.of(RecipeTypes.SMELTING));
         }
      });
   }

   static Rect2i absoluteArea(FactoryStudyGeometry.ViewTransform view, FactoryStudyGeometry.Rect rect) {
      int x = (int)Math.floor(view.screenX(rect.x())), y = (int)Math.floor(view.screenY(rect.y()));
      return new Rect2i(x, y, (int)Math.ceil(view.screenX(rect.right())) - x,
         (int)Math.ceil(view.screenY(rect.bottom())) - y);
   }
}
