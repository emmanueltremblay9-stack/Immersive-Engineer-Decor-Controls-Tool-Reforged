package com.oblixorprime.engineersdecorreforged.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free geometry for the retained-reference Tree Cutter V3 client presentation.
 * Source coordinates describe the unmodified 1254-by-1254 V3 reference, not the
 * legacy Tree Cutter texture. This class changes no inventory or machine semantics.
 */
public final class TreeCutterV3ReferenceGeometry {
   public static final int SOURCE_WIDTH = 1254;
   public static final int SOURCE_HEIGHT = 1254;
   public static final int CANVAS_WIDTH = 418;
   public static final int CANVAS_HEIGHT = 418;
   /** Uniform source scale; both source dimensions divide exactly into the canvas. */
   public static final float SOURCE_SCALE = 1.0F / 3.0F;
   public static final int VIEWPORT_MARGIN = 6;
   public static final int ITEM_SIZE = 16;
   public static final int MENU_SLOT_COUNT = 37;
   public static final int VISIBLE_SLOT_COUNT = 37;
   public static final String SOURCE_SHA256 = "bdab10d19c0da44c113f84f1330513f4ff71e81dfe7d3eab602ce27202c0650d";
   public static final String SOURCE_FILENAME = "source_validated.png";
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
      // Conservative interiors from the hash-verified retained raster kit manifest.
      anchors.add(centeredAnchor(0, 895, 279, 119, 115));
      int[] left = {228, 310, 392, 475, 558, 641, 724, 807, 889};
      int[] right = {297, 379, 462, 545, 627, 710, 793, 875, 953};
      int[] top = {555, 631, 708, 813};
      int[] bottom = {618, 693, 768, 873};
      for (int row = 0; row < 4; row++) {
         for (int column = 0; column < 9; column++) {
            anchors.add(centeredAnchor(1 + row * 9 + column, left[column], top[row],
               right[column] - left[column], bottom[row] - top[row]));
         }
      }
      return List.copyOf(anchors);
   }

   private static SlotAnchor centeredAnchor(int menuIndex, int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
      int x = (int)Math.round((sourceX + sourceWidth / 2.0D) / 3.0D - ITEM_SIZE / 2.0D);
      int y = (int)Math.round((sourceY + sourceHeight / 2.0D) / 3.0D - ITEM_SIZE / 2.0D);
      return new SlotAnchor(menuIndex, sourceX, sourceY, sourceWidth, sourceHeight, x, y);
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
   public static final Rect ENERGY = new Rect(280, 57, 62, 6);
   public static final Rect HEADER = new Rect(84, 7, 224, 17);

   public static int energyFillWidth(int stored, int capacity) {
      if (stored <= 0 || capacity <= 0) return 0;
      return (int)Math.min(ENERGY.width(), (long)stored * ENERGY.width() / capacity);
   }

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
         return (double)this.sourceX / 3.0D;
      }

      public double interiorTop() {
         return (double)this.sourceY / 3.0D;
      }

      public double interiorRight() {
         return (double)(this.sourceX + this.sourceWidth) / 3.0D;
      }

      public double interiorBottom() {
         return (double)(this.sourceY + this.sourceHeight) / 3.0D;
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
