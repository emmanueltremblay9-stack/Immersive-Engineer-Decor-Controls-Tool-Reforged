package com.oblixorprime.engineersdecorreforged.kx;
import org.junit.Test;
public class KXIntegrationTest {
    @Test public void recoveredCore() { KXMechanicsChecks.main(new String[0]); }
    @Test public void recoveredCoordinator() { KxMk2PlacementCoordinatorChecks.main(new String[0]); }
    @Test public void runtimeSeam() { KXRuntimePlacementChecks.main(new String[0]); }
}
