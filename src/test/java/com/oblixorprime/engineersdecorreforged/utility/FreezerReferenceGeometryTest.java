package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.Test;

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
   public void invalidGeometryInputsFailExplicitly() {
      FreezerReferenceGeometryChecks.checkInvalidInputs();
   }
}
