package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** JUnit integration for the same checks runnable with the plain Java entry point. */
public class FreezerReferenceGeometryTest {
   @Test
   public void menuIndicesAndImmutableMappingRemainStable() {
      FreezerReferenceGeometryChecks.checkMenuMapping();
   }

   @Test
   public void itemOriginsFollowMeasuredNonuniformSourceCenters() {
      FreezerReferenceGeometryChecks.checkMeasuredCenters();
   }

   @Test
   public void itemsAndClippedHoverStayInsideReferenceInteriorsWithoutOverlap() {
      FreezerReferenceGeometryChecks.checkItemAndClippedHoverContainment();
   }

   @Test
   public void clippedHoverUsesHalfOpenBounds() {
      FreezerReferenceGeometryChecks.checkHoverHalfOpenEdges();
   }

   @Test
   public void allViewportTransformsAreUniformAndInvertInputIncludingNegativePointers() {
      FreezerReferenceGeometryChecks.checkUniformTransforms();
   }

   @Test
   public void scissorBoundsFollowCanvasTransformInGuiScaledScreenCoordinates() {
      for (int[] viewport : new int[][] {{1280, 260}, {1280, 720}}) {
         var view = FreezerReferenceGeometry.fitCanvas(viewport[0], viewport[1], 347, 381, 0.78F);
         var bounds = view.screenBounds(56, 89, 77, 192);
         assertEquals((int)Math.floor(view.screenX(56)), bounds.left());
         assertEquals((int)Math.floor(view.screenY(89)), bounds.top());
         assertEquals((int)Math.ceil(view.screenX(77)), bounds.right());
         assertEquals((int)Math.ceil(view.screenY(192)), bounds.bottom());
         // The real 7-segment art sits inside the local meter bounds. The
         // projected clip must include it even when the canvas is scaled.
         assertTrue(bounds.left() <= view.screenX(62.3));
         assertTrue(bounds.right() > view.screenX(72));
         assertTrue(bounds.top() <= view.screenY(124));
         assertTrue(bounds.bottom() >= view.screenY(186));
         assertTrue("screen-space scissor must not be mistaken for canvas-local coordinates", bounds.left() > 56);
      }
   }

   @Test
   public void invalidGeometryInputsFailExplicitly() {
      FreezerReferenceGeometryChecks.checkInvalidInputs();
   }
}
