package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.Test;

public final class FreezerReferenceLiveStateTest {
   @Test public void preservesModeIsolationAndRawFE() { FreezerReferenceLiveStateChecks.checkModesAndEnergy(); }
   @Test public void preservesAuthoritativeProgressAndStaticFrostGate() { FreezerReferenceLiveStateChecks.checkFrostSnapshots(); }
   @Test public void boundsSourcePatchesToConservativeWell() { FreezerReferenceLiveStateChecks.checkPatchGeometry(); }
}
