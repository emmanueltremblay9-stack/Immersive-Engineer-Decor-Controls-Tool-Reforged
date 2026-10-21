package com.oblixorprime.engineersdecorreforged.tools.muslee;

import org.junit.Test;

/** CI runs exactly the standalone cross-PR Cases against real production classes. */
public final class MusleeCrossPrOwnedCaptureTest {
    @Test(timeout = 60000)
    public void actualCaptureOwnershipSurvivesTargetedInvalidation() throws Exception {
        MusleeCrossPrOwnedCaptureCases.run();
    }
}
