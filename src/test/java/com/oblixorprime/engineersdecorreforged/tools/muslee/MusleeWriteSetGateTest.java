package com.oblixorprime.engineersdecorreforged.tools.muslee;

import org.junit.Test;

/** The same assertions are invoked by local Java 21 and existing Gradle JUnit. */
public final class MusleeWriteSetGateTest {
    @Test public void completeProposedSetPreflightIsNeverAnAuthorization() {
        MusleeWriteSetGateCases.run();
    }
}
