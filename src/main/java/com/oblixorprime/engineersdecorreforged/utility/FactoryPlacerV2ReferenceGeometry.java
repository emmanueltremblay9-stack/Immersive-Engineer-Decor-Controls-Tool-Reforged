package com.oblixorprime.engineersdecorreforged.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free geometry for the retained-reference Factory Placer V2 client presentation.
 * Source coordinates describe the unmodified 1199-by-1312 V2 reference, not the
 * legacy Placer texture. This class changes no inventory or machine semantics.
 */
public final class FactoryPlacerV2ReferenceGeometry {
   public static final int SOURCE_WIDTH = 1199;
   public static final int SOURCE_HEIGHT = 1312;
   public static final int CANVAS_WIDTH = 400;
   public static final int CANVAS_HEIGHT = 438;
   /** Uniform source scale; canvas dimensions round outward, never stretch the art. */
   public static final float SOURCE_SCALE = 1.0F / 3.0F;
   public static final int VIEWPORT_MARGIN = 6;
   public static final int ITEM_SIZE = 16;
   public static final int MENU_SLOT_COUNT = 54;
   public static final int VISIBLE_SLOT_COUNT = 54;
   public static final String SOURCE_SHA256 = "ff079488dd0d1c7434597a6915231d88b81732472bb23ccc8a61235eca27ec2b";
   public static final String SOURCE_FILENAME = "factory-placer-grey-steel-v2-concept.png";
   public static final List<SlotAnchor> SLOT_ANCHORS = createAnchors();
   private static final SlotAnchor[] ANCHORS_BY_MENU_INDEX = indexAnchors();

   private FactoryPlacerV2ReferenceGeometry() {
   }

   /** All 54 original menu indices have a visible anchor. */
   public static SlotAnchor anchor(int menuIndex) {
      if (menuIndex < 0 || menuIndex >= MENU_SLOT_COUNT) {
         throw new IllegalArgumentException("No visible Factory Placer V2 anchor for menu index " + menuIndex);
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
      // Measured dark interiors, excluding frame highlights and clipped corners.
      int[] machineX = {122, 218, 313, 409, 504, 599};
      int[] machineY = {222, 331, 440};
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 6; column++) {
            anchors.add(centeredAnchor(row * 6 + column, machineX[column], machineY[row], 56, 56));
         }
      }
      int[] playerX = {156, 257, 358, 458, 559, 660, 760, 861, 961};
      int[] playerY = {681, 786, 892};
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            anchors.add(centeredAnchor(18 + row * 9 + column, playerX[column], playerY[row], 80, 72));
         }
      }
      int[] hotbarX = {167, 266, 365, 465, 564, 663, 763, 862, 961};
      for (int column = 0; column < 9; column++) {
         anchors.add(centeredAnchor(45 + column, hotbarX[column], 1040, 76, 72));
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

   /**
    * Selection-only raster geometry in framebuffer pixels, ordered top/right/bottom/left.
    * At subpixel canvas scale, reserve one complete pixel outside the unchanged 16x16
    * item footprint. Outward rounding can extend this overlay beyond the old fractional
    * 18x18 border; it never changes the artwork, item transform or input geometry.
    * At >=1 physical pixel per canvas unit, preserve the original outline exactly.
    */
   public static List<PixelRect> selectionEdges(ViewTransform view, double guiScale, int slot) {
      if (!Double.isFinite(guiScale) || guiScale <= 0 || slot < 0 || slot >= 18) {
         throw new IllegalArgumentException("Selection requires a positive GUI scale and machine slot");
      }
      var a = anchor(slot);
      if (view.scale() * guiScale < 1.0D) {
         double left = Math.floor(view.screenX(a.x()) * guiScale);
         double top = Math.floor(view.screenY(a.y()) * guiScale);
         double right = Math.ceil(view.screenX(a.x() + ITEM_SIZE) * guiScale);
         double bottom = Math.ceil(view.screenY(a.y() + ITEM_SIZE) * guiScale);
         return List.of(new PixelRect(left - 1, top - 1, right + 1, top),
            new PixelRect(right, top, right + 1, bottom),
            new PixelRect(left - 1, bottom, right + 1, bottom + 1),
            new PixelRect(left - 1, top, left, bottom));
      }
      return List.of(pixelRect(view, guiScale, a.x() - 1, a.y() - 1, 18, 1),
         pixelRect(view, guiScale, a.x() + 16, a.y(), 1, 16),
         pixelRect(view, guiScale, a.x() - 1, a.y() + 16, 18, 1),
         pixelRect(view, guiScale, a.x() - 1, a.y(), 1, 16));
   }

   private static PixelRect pixelRect(ViewTransform view, double guiScale, int x, int y, int width, int height) {
      return new PixelRect(view.screenX(x) * guiScale, view.screenY(y) * guiScale,
         view.screenX(x + width) * guiScale, view.screenY(y + height) * guiScale);
   }

   public record PixelRect(double left, double top, double right, double bottom) {}

   public enum Control { ROW_TOP, ROW_MIDDLE, ROW_BOTTOM, MANUAL_TRIGGER, INVERSION, TRIGGER_MODE }

   public static Rect controlRect(Control control) {
      return switch (control) {
         case ROW_TOP -> new Rect(260, 61, 80, 18);
         case ROW_MIDDLE -> new Rect(260, 89, 80, 18);
         case ROW_BOTTOM -> new Rect(260, 118, 80, 18);
         case MANUAL_TRIGGER -> new Rect(246, 157, 11, 9);
         case INVERSION -> new Rect(280, 160, 32, 9);
         case TRIGGER_MODE -> new Rect(332, 160, 30, 9);
      };
   }

   public static Control controlAt(double localX, double localY) {
      for (Control control : Control.values()) {
         if (controlRect(control).contains(localX, localY)) return control;
      }
      return null;
   }

   /** All status paint is inside these measured flat/dark source interiors. */
   public static Rect rowIndicator(int row) {
      if (row < 0 || row > 2) throw new IllegalArgumentException("Row must be 0..2");
      return new Rect(266, new int[] {67, 95, 124}[row], 10, 4);
   }
   public static final Rect SIGNAL_INDICATOR = new Rect(249, 160, 5, 3);
   public static final Rect INVERSION_INDICATOR = new Rect(282, 171, 28, 2);
   public static final Rect TRIGGER_INDICATOR = new Rect(334, 171, 26, 2);
   public static final Rect HEADER = new Rect(90, 10, 220, 18);

   public record Rect(int x, int y, int width, int height) {
      public boolean contains(double localX, double localY) {
         return localX >= x && localX < x + width && localY >= y && localY < y + height;
      }
   }

   /**
    * Integer item origin plus its measured continuous source interior.
    * Hover bounds intersect vanilla's [x-1,x+17) by [y-1,y+17) envelope with
    * that interior. This presentation hitbox adjustment is separate from item drawing:
    * conservative V2 interiors bound padding on all four player inventory rows.
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
