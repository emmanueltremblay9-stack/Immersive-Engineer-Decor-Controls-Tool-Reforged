package com.oblixorprime.engineersdecorreforged.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime geometry for GUI11-R3 V5-Corrigee #10. This class changes no inventory
 * or machine semantics.
 */
public final class TreeCutterV3ReferenceGeometry {
   public static final int SOURCE_WIDTH = Gui11V5CRuntimeGeometry.sourceWidth(MachineKind.SMALL_TREE_CUTTER);
   public static final int SOURCE_HEIGHT = Gui11V5CRuntimeGeometry.sourceHeight(MachineKind.SMALL_TREE_CUTTER);
   public static final int CANVAS_WIDTH = Gui11V5CRuntimeGeometry.width(MachineKind.SMALL_TREE_CUTTER);
   public static final int CANVAS_HEIGHT = Gui11V5CRuntimeGeometry.height(MachineKind.SMALL_TREE_CUTTER);
   public static final float SOURCE_SCALE = (float)CANVAS_WIDTH / SOURCE_WIDTH;
   public static final int VIEWPORT_MARGIN = 6;
   public static final int ITEM_SIZE = 16;
   public static final int MENU_SLOT_COUNT = 37;
   public static final int VISIBLE_SLOT_COUNT = 37;
   public static final String SOURCE_SHA256 = "c81ada9ba030d42eda8dac4eafba5b305ecb83cffed2ab7c77025aa34bb7b3ef";
   public static final String SOURCE_FILENAME = "10-V5-Corrigee.png";
   public static final List<SlotAnchor> SLOT_ANCHORS = createAnchors();
   private static final SlotAnchor[] ANCHORS_BY_MENU_INDEX = indexAnchors();

   private TreeCutterV3ReferenceGeometry() {
   }

   /** All 37 menu indices have a visible anchor. */
   public static SlotAnchor anchor(int menuIndex) {
      if (menuIndex < 0 || menuIndex >= MENU_SLOT_COUNT) {
         throw new IllegalArgumentException("No visible Tree Cutter V3 anchor for menu index " + menuIndex);
      }
      return ANCHORS_BY_MENU_INDEX[menuIndex];
   }

   /**
    * Uniformly fits the full canvas inside six-pixel margins, never upscaling.
    * For positive viewports shorter than 13 pixels, margins alone relax enough
    * to leave one usable pixel. The scale has no arbitrary lower clamp.
    */
   public static ViewTransform fit(int viewportWidth, int viewportHeight) {
      if (viewportWidth <= 0 || viewportHeight <= 0) {
         throw new IllegalArgumentException("Viewport dimensions must be positive");
      }
      double shortEdge = Math.min(viewportWidth, viewportHeight);
      double margin = Math.min(VIEWPORT_MARGIN, (shortEdge - 1.0D) / 2.0D);
      double availableWidth = viewportWidth - 2.0D * margin;
      double availableHeight = viewportHeight - 2.0D * margin;
      float scale = (float)Math.min(1.0D, Math.min(availableWidth / CANVAS_WIDTH, availableHeight / CANVAS_HEIGHT));
      if ((double)scale * CANVAS_WIDTH > availableWidth || (double)scale * CANVAS_HEIGHT > availableHeight) {
         scale = Math.nextDown(scale);
      }
      float originX = (float)((viewportWidth - (double)CANVAS_WIDTH * scale) / 2.0D);
      float originY = (float)((viewportHeight - (double)CANVAS_HEIGHT * scale) / 2.0D);
      return new ViewTransform(scale, originX, originY);
   }

   private static List<SlotAnchor> createAnchors() {
      List<SlotAnchor> anchors = new ArrayList<>(VISIBLE_SLOT_COUNT);
      for (int index = 0; index < MENU_SLOT_COUNT; index++) {
         Gui11V5CRuntimeGeometry.Point point = Gui11V5CRuntimeGeometry.slot(MachineKind.SMALL_TREE_CUTTER, index);
         if (point == null) throw new IllegalStateException("Missing Tree Cutter GUI11 slot " + index);
         var source = Gui11V5CRuntimeGeometry.sourceItemBounds(MachineKind.SMALL_TREE_CUTTER, point);
         anchors.add(new SlotAnchor(index, source.x(), source.y(), source.width(), source.height(), point.x(), point.y()));
      }
      return List.copyOf(anchors);
   }


   private static SlotAnchor[] indexAnchors() {
      SlotAnchor[] byIndex = new SlotAnchor[MENU_SLOT_COUNT];
      for (SlotAnchor anchor : SLOT_ANCHORS) {
         byIndex[anchor.menuIndex()] = anchor;
      }
      return byIndex;
   }

   /** Tooltip content width in unscaled screen pixels, with room for native padding. */
   public static int tooltipWidth(int viewportWidth) {
      return (int)Math.max(1L, Math.min(240L, (long)viewportWidth - 16L));
   }

   /** Dynamic energy paint is confined to the manifest's safe dark interior. */
   public static final Rect HEADER = new Rect(84, 7, 224, 17);


   public record Rect(int x, int y, int width, int height) {
      public boolean contains(double localX, double localY) {
         return localX >= x && localX < x + width && localY >= y && localY < y + height;
      }
   }

   /**
    * Integer item origin plus its measured continuous source interior.
    * Hover bounds intersect vanilla's [x-1,x+17) by [y-1,y+17) envelope with
    * that interior. This presentation hitbox adjustment is separate from item drawing:
    * conservative V3 interiors bound padding on all four player inventory rows.
    */
   public record SlotAnchor(int menuIndex, int sourceX, int sourceY, int sourceWidth, int sourceHeight, int x, int y) {
      public double interiorLeft() {
         return (double)this.sourceX * CANVAS_WIDTH / SOURCE_WIDTH;
      }

      public double interiorTop() {
         return (double)this.sourceY * CANVAS_HEIGHT / SOURCE_HEIGHT;
      }

      public double interiorRight() {
         return (double)(this.sourceX + this.sourceWidth) * CANVAS_WIDTH / SOURCE_WIDTH;
      }

      public double interiorBottom() {
         return (double)(this.sourceY + this.sourceHeight) * CANVAS_HEIGHT / SOURCE_HEIGHT;
      }

      public double hoverLeft() {
         return Math.max(this.x - 1.0D, this.interiorLeft());
      }

      public double hoverTop() {
         return Math.max(this.y - 1.0D, this.interiorTop());
      }

      public double hoverRight() {
         return Math.min(this.x + ITEM_SIZE + 1.0D, this.interiorRight());
      }

      public double hoverBottom() {
         return Math.min(this.y + ITEM_SIZE + 1.0D, this.interiorBottom());
      }

      public boolean containsHover(double localX, double localY) {
         return localX >= this.hoverLeft() && localX < this.hoverRight()
            && localY >= this.hoverTop() && localY < this.hoverBottom();
      }
   }

   /** One transform must be shared by rendering, absolute input, and drag deltas. */
   public record ViewTransform(float scale, float originX, float originY) {
      public ViewTransform {
         if (!Float.isFinite(scale) || scale <= 0.0F || scale > 1.0F
            || !Float.isFinite(originX) || !Float.isFinite(originY)) {
            throw new IllegalArgumentException("Transform must be finite with scale in (0,1]");
         }
      }

      public double localX(double screenX) {
         return (screenX - this.originX) / this.scale;
      }

      public double localY(double screenY) {
         return (screenY - this.originY) / this.scale;
      }

      public double screenX(double localX) {
         return this.originX + localX * this.scale;
      }

      public double screenY(double localY) {
         return this.originY + localY * this.scale;
      }

      /** Public container bounds for screen-space consumers such as JEI. */
      public int guiLeft() {
         return (int)Math.floor(this.screenX(0));
      }

      public int guiTop() {
         return (int)Math.floor(this.screenY(0));
      }

      /** Round outwards so an external overlay cannot cover a fractional edge. */
      public int guiWidth() {
         return (int)Math.ceil(this.screenX(CANVAS_WIDTH)) - this.guiLeft();
      }

      public int guiHeight() {
         return (int)Math.ceil(this.screenY(CANVAS_HEIGHT)) - this.guiTop();
      }

      public double localDelta(double screenDelta) {
         return screenDelta / this.scale;
      }

      public double screenDelta(double localDelta) {
         return localDelta * this.scale;
      }

      /** Floor rather than truncate: a pointer just outside the left edge stays outside. */
      public int renderPointerX(double screenX) {
         return (int)Math.floor(this.localX(screenX));
      }

      /** Floor rather than truncate: a pointer just outside the top edge stays outside. */
      public int renderPointerY(double screenY) {
         return (int)Math.floor(this.localY(screenY));
      }
   }
}
