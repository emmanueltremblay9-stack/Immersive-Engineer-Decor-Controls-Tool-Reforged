package com.oblixorprime.engineersdecorreforged.utility;

import org.junit.jupiter.api.Test;

public final class FreezerReferenceLiveStateTest {
   @Test void preservesModeIsolationAndRawFE() { FreezerReferenceLiveStateChecks.checkModesAndEnergy(); }
   @Test void preservesAuthoritativeProgressAndStaticFrostGate() { FreezerReferenceLiveStateChecks.checkFrostSnapshots(); }
   @Test void boundsSourcePatchesToConservativeWell() { FreezerReferenceLiveStateChecks.checkPatchGeometry(); }
}
