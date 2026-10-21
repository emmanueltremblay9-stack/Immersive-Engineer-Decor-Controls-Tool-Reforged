package com.oblixorprime.engineersdecorreforged.tools.muslee;

import org.junit.Test;

public final class MusleeIngressQueueIntegrationTest {
    @Test public void recognitionAndQueueLifecyclesRemainSeparate() {
        MusleeIngressQueueIntegrationCases.run();
    }
}
