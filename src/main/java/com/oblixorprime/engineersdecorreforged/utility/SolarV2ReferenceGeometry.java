package com.oblixorprime.engineersdecorreforged.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free geometry for the opt-in retained-reference Solar V2 experiment.
 * Source coordinates describe the unmodified 1254-by-1254 V2 reference, not the
 * legacy Solar texture. This class changes no inventory or machine semantics.
 */
public final class SolarV2ReferenceGeometry {
   public static final int SOURCE_WIDTH = 1254;
   public static final int SOURCE_HEIGHT = 1254;
   public static final int CANVAS_WIDTH = 418;
   public static final int CANVAS_HEIGHT = 418;
   public static final int VIEWPORT_MARGIN = 6;
   public static final int ITEM_SIZE = 16;
   public static final int MENU_SLOT_COUNT = 37;
   public static final int VISIBLE_SLOT_COUNT = 37;
   public static final String SOURCE_SHA256 = "d702051b09e09cd7c9ebd7ea874356ba82324db07bcb0657cf8ac66bdf54c796";
   public static final String SOURCE_FILENAME = "small-solar-panel-v2.png";
   public static final List<SlotAnchor> SLOT_ANCHORS = createAnchors();
   private static final SlotAnchor[] ANCHORS_BY_MENU_INDEX = indexAnchors();

   private SolarV2ReferenceGeometry() {
   }

   /** All 37 original menu indices have a visible anchor. */
   public static SlotAnchor anchor(int menuIndex) {
      if (menuIndex < 0 || menuIndex >= MENU_SLOT_COUNT) {
         throw new IllegalArgumentException("No visible Solar V2 anchor for menu index " + menuIndex);
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
      anchors.add(centeredAnchor(0, 979, 312, 156, 165));
      anchors.add(centeredAnchor(1, 164, 762, 82, 67));
      anchors.add(centeredAnchor(2, 270, 762, 81, 67));
      anchors.add(centeredAnchor(3, 376, 762, 80, 67));
      anchors.add(centeredAnchor(4, 482, 762, 81, 67));
      anchors.add(centeredAnchor(5, 587, 762, 81, 67));
      anchors.add(centeredAnchor(6, 694, 762, 81, 67));
      anchors.add(centeredAnchor(7, 799, 762, 81, 67));
      anchors.add(centeredAnchor(8, 906, 762, 81, 67));
      anchors.add(centeredAnchor(9, 1012, 762, 81, 67));
      anchors.add(centeredAnchor(10, 164, 856, 82, 68));
      anchors.add(centeredAnchor(11, 270, 856, 81, 68));
      anchors.add(centeredAnchor(12, 376, 856, 80, 68));
      anchors.add(centeredAnchor(13, 482, 856, 81, 68));
      anchors.add(centeredAnchor(14, 587, 856, 81, 68));
      anchors.add(centeredAnchor(15, 694, 856, 81, 68));
      anchors.add(centeredAnchor(16, 799, 856, 81, 68));
      anchors.add(centeredAnchor(17, 906, 856, 81, 68));
      anchors.add(centeredAnchor(18, 1012, 856, 81, 68));
      anchors.add(centeredAnchor(19, 164, 951, 82, 71));
      anchors.add(centeredAnchor(20, 270, 951, 81, 71));
      anchors.add(centeredAnchor(21, 376, 951, 80, 71));
      anchors.add(centeredAnchor(22, 482, 951, 81, 71));
      anchors.add(centeredAnchor(23, 587, 951, 81, 71));
      anchors.add(centeredAnchor(24, 694, 951, 81, 71));
      anchors.add(centeredAnchor(25, 799, 951, 81, 71));
      anchors.add(centeredAnchor(26, 906, 951, 81, 71));
      anchors.add(centeredAnchor(27, 1012, 951, 81, 71));
      anchors.add(centeredAnchor(28, 164, 1084, 82, 71));
      anchors.add(centeredAnchor(29, 270, 1084, 81, 71));
      anchors.add(centeredAnchor(30, 376, 1084, 80, 71));
      anchors.add(centeredAnchor(31, 482, 1084, 81, 71));
      anchors.add(centeredAnchor(32, 587, 1084, 81, 71));
      anchors.add(centeredAnchor(33, 694, 1084, 81, 71));
      anchors.add(centeredAnchor(34, 799, 1084, 81, 71));
      anchors.add(centeredAnchor(35, 906, 1084, 81, 71));
      anchors.add(centeredAnchor(36, 1012, 1084, 81, 71));
      return List.copyOf(anchors);
   }

   private static SlotAnchor centeredAnchor(int menuIndex, int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
      int x = (int)Math.round((sourceX + sourceWidth / 2.0D) * CANVAS_WIDTH / SOURCE_WIDTH - ITEM_SIZE / 2.0D);
      int y = (int)Math.round((sourceY + sourceHeight / 2.0D) * CANVAS_HEIGHT / SOURCE_HEIGHT - ITEM_SIZE / 2.0D);
      return new SlotAnchor(menuIndex, sourceX, sourceY, sourceWidth, sourceHeight, x, y);
   }

   private static SlotAnchor[] indexAnchors() {
      SlotAnchor[] byIndex = new SlotAnchor[MENU_SLOT_COUNT];
      for (SlotAnchor anchor : SLOT_ANCHORS) {
         byIndex[anchor.menuIndex()] = anchor;
      }
      return byIndex;
   }

   /**
    * Integer item origin plus its measured continuous source interior.
    * Hover bounds intersect vanilla's [x-1,x+17) by [y-1,y+17) envelope with
    * that interior. This opt-in hitbox adjustment is separate from item drawing:
    * conservative V2 interiors bound padding on all four player inventory rows.
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
