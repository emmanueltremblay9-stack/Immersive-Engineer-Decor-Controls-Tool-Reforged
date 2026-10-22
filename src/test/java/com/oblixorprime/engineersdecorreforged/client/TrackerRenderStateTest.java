package com.oblixorprime.engineersdecorreforged.client;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrackerRenderStateTest {
   private static TrackerRenderState.Angles angles(double x, double y, double z, double lx, double ly, double lz) {
      return TrackerRenderState.angles(x, y, z, lx, ly, lz).orElseThrow();
   }

   @Test public void alignedAndOppositeTargetsKeepUpstreamYaw() {
      assertEquals(new TrackerRenderState.Angles(0, 0), angles(0, 0, -10, 0, 0, -1));
      assertEquals(new TrackerRenderState.Angles(0, 180), angles(0, 0, -10, 0, 0, 1));
   }

   @Test public void cardinalTurnsKeepUpstreamWrapping() {
      assertEquals(new TrackerRenderState.Angles(0, -90), angles(10, 0, 0, 0, 0, -1));
      assertEquals(new TrackerRenderState.Angles(0, -90), angles(0, 0, -10, -1, 0, 0));
      assertEquals(new TrackerRenderState.Angles(0, -270), angles(-10, 0, 0, 0, 0, -1));
   }

   @Test public void verticalTargetsTiltOnlyInsideUpstreamCone() {
      assertEquals(90, angles(0, 10, 0, 0, 0, -1).pitch());
      assertEquals(-90, angles(0, -10, 0, 0, 0, -1).pitch());
      assertEquals(0, angles(10, 4, 0, 0, 0, -1).pitch());
      assertEquals(0, angles(1, 0.1, 0, 0, 0, -1).pitch());
   }

   @Test public void nearTargetRetainsPreviousDisplayInsteadOfProducingNan() {
      assertTrue(TrackerRenderState.angles(0, 0, 0, 0, 0, -1).isEmpty());
      assertTrue(TrackerRenderState.angles(0.5, 0, 0, 0, 0, -1).isEmpty());
      assertTrue(TrackerRenderState.angles(0.6, 0, 0, 0, 0, -1).isPresent());
   }

   @Test public void invalidInputsCannotReachPoseRotations() {
      assertTrue(TrackerRenderState.angles(Double.NaN, 0, 0, 0, 0, -1).isEmpty());
      assertTrue(TrackerRenderState.angles(10, 0, 0, 0, Double.NaN, -1).isEmpty());
   }
}
