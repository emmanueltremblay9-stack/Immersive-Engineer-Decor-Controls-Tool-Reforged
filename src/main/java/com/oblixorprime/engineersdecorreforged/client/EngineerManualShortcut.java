package com.oblixorprime.engineersdecorreforged.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.gui.ManualScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Uses IE's own item/recipe index, including entries supplied by other addons. */
@EventBusSubscriber(modid = "immersive_engineer_decor_controls_tool_reforged", value = Dist.CLIENT,
      bus = EventBusSubscriber.Bus.GAME)
public final class EngineerManualShortcut {
   static final KeyMapping OPEN = new KeyMapping(
         "key.immersive_engineer_decor_controls_tool_reforged.engineer_manual",
         KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H,
         "key.categories.immersive_engineer_decor_controls_tool_reforged");
   private static final ManualShortcutHold<Target> HOLD = new ManualShortcutHold<>();
   private static Screen armedScreen;
   private static InputConstants.Key armedKey;
   private static int armedKeyboardCode = GLFW.GLFW_KEY_UNKNOWN;
   private static long retryManualAfter;
   private static Target hovered;
   private static ManualInstance.ManualLink hoveredLink;
   private static ManualScreen preparedScreen;

   private EngineerManualShortcut() {}

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void keyPressed(ScreenEvent.KeyPressed.Post event) {
      // Post is not fired for input consumed by a screen or a cancelled Pre event.
      InputConstants.Key key = OPEN.getKey().getType() == InputConstants.Type.SCANCODE
            ? InputConstants.Type.SCANCODE.getOrCreate(event.getScanCode())
            : InputConstants.getKey(event.getKeyCode(), event.getScanCode());
      arm(event.getScreen(), key, event.getKeyCode());
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void mousePressed(ScreenEvent.MouseButtonPressed.Post event) {
      if (!event.getClickResult()) arm(event.getScreen(), InputConstants.Type.MOUSE.getOrCreate(event.getButton()), GLFW.GLFW_KEY_UNKNOWN);
   }

   private static void arm(Screen screen, InputConstants.Key key, int keyboardCode) {
      if (armedScreen == null && eligible(screen) && OPEN.isActiveAndMatches(key)) {
         armedScreen = screen;
         armedKey = key;
         armedKeyboardCode = keyboardCode;
         HOLD.reset();
      }
   }

   @SubscribeEvent
   public static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
      InputConstants.Key key = armedKey != null && armedKey.getType() == InputConstants.Type.SCANCODE
            ? InputConstants.Type.SCANCODE.getOrCreate(event.getScanCode())
            : InputConstants.getKey(event.getKeyCode(), event.getScanCode());
      if (key.equals(armedKey)) disarm();
   }

   @SubscribeEvent
   public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
      if (InputConstants.Type.MOUSE.getOrCreate(event.getButton()).equals(armedKey)) disarm();
   }

   @SubscribeEvent
   public static void opening(ScreenEvent.Opening event) {
      disarm();
   }

   @SubscribeEvent(receiveCanceled = true)
   public static void beforeRender(ScreenEvent.Render.Pre event) {
      if (Minecraft.getInstance().screen != event.getScreen()) return;
      hovered = null;
      hoveredLink = null;
      preparedScreen = null;
      if (event.getScreen() != armedScreen || !eligible(event.getScreen())
            || armedKey == null || !OPEN.isActiveAndMatches(armedKey) || !physicallyDown()) disarm();
   }

   @SubscribeEvent
   public static void tooltip(RenderTooltipEvent.GatherComponents event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (!eligible(minecraft.screen) || OPEN.isUnbound() || event.getItemStack().isEmpty()) return;
      if (!ManualHelper.IE_MANUAL_INSTANCE.isInitialized()) return;
      ManualInstance manual = ManualHelper.getManual();
      if (manual == null) return;
      // getManualLink alone can expose an uninitialized/stale index. IE owns reloads,
      // component matching, recipe alternatives and page/anchor selection.
      if (Util.getMillis() < retryManualAfter) return;
      ManualScreen screen = manual.getGui(false);
      if (screen == null) {
         retryManualAfter = Util.getMillis() + 1000;
         return;
      }
      ManualInstance.ManualLink link = manual.getManualLink(event.getItemStack());
      if (link == null) return;
      event.getTooltipElements().add(Either.left(Component.translatable(
            "tooltip.immersive_engineer_decor_controls_tool_reforged.engineer_manual",
            OPEN.getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY)));
      hovered = new Target(link.getKey().getLocation(), link.getAnchor(), link.getOffset(),
            event.getItemStack().getItem(), event.getItemStack().copy().getComponents());
      hoveredLink = link;
      preparedScreen = screen;
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void afterRender(ScreenEvent.Render.Post event) {
      if (Minecraft.getInstance().screen != event.getScreen()) return;
      if (!HOLD.update(hovered, armedScreen == event.getScreen() && eligible(event.getScreen()), Util.getMillis())) return;
      ManualScreen screen = preparedScreen;
      ManualInstance.ManualLink link = hoveredLink;
      disarm();
      if (screen == null || link == null) return;
      // setScreen initializes the screen before changePage invokes fullInit.
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.setScreen(screen);
      if (minecraft.screen == screen) link.changePage(screen, false);
   }

   private static boolean eligible(Screen screen) {
      Minecraft minecraft = Minecraft.getInstance();
      if (screen == null || screen instanceof ManualScreen || minecraft.player == null
            || !minecraft.isWindowActive()
            || InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_F3)) return false;
      GuiEventListener focused = screen.getFocused();
      while (focused != null) {
         if (focused instanceof EditBox) return false;
         if (!(focused instanceof ContainerEventHandler container)) break;
         focused = container.getFocused();
      }
      return true;
   }

   private static boolean physicallyDown() {
      if (armedKey == null) return false;
      long window = Minecraft.getInstance().getWindow().getWindow();
      if (armedKey.getType() == InputConstants.Type.MOUSE)
         return GLFW.glfwGetMouseButton(window, armedKey.getValue()) == GLFW.GLFW_PRESS;
      // GLFW does not expose polling by scan code; ordinary keyboard events supply
      // a key code even for scan-code bindings. Release events remain authoritative.
      return armedKeyboardCode == GLFW.GLFW_KEY_UNKNOWN || InputConstants.isKeyDown(window, armedKeyboardCode);
   }

   private static void disarm() {
      armedScreen = null;
      armedKey = null;
      armedKeyboardCode = GLFW.GLFW_KEY_UNKNOWN;
      HOLD.reset();
   }

   private record Target(ResourceLocation entry, String anchor, int offset, Item item, DataComponentMap components) {}
}
