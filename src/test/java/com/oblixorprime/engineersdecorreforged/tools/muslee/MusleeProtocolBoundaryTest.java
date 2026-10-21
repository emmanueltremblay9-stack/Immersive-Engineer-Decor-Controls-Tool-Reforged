package com.oblixorprime.engineersdecorreforged.tools.muslee;

import org.junit.Test;

public final class MusleeProtocolBoundaryTest {
    @Test public void exhaustiveWireAndReplayContract() throws Exception { MusleeProtocolBoundaryCases.run(); }
}
