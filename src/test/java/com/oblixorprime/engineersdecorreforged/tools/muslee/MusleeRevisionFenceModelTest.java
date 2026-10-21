package com.oblixorprime.engineersdecorreforged.tools.muslee;

import org.junit.Test;

/** The same deterministic oracle checks run in local Java and repository JUnit. */
public final class MusleeRevisionFenceModelTest {
    @Test public void seededStateMachineContract() {
        MusleeRevisionFenceModelCases.run();
    }
}
