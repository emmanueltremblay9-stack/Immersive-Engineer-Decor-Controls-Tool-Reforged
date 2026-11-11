package com.oblixorprime.engineersdecorreforged.utility;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.Test;
import static org.junit.Assert.*;

public final class BarrelReferenceGeometryTest {
   @Test public void retainedReferenceGeometryAndState() { BarrelReferenceChecks.main(new String[0]); }
   @Test public void actualRetainedPngMatchesGeometry() throws Exception {
      try (var input = getClass().getResourceAsStream("/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/barrel_reference_v5/background.png")) {
         assertNotNull(input);
         byte[] png = input.readAllBytes();
         assertEquals(BarrelReferenceGeometry.SOURCE_SHA256,
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png)));
         var header = ByteBuffer.wrap(png);
         assertEquals(1137, header.getInt(16));
         assertEquals(1383, header.getInt(20));
         assertEquals(BarrelReferenceGeometry.SOURCE_WIDTH, header.getInt(16));
         assertEquals(BarrelReferenceGeometry.SOURCE_HEIGHT, header.getInt(20));
         assertEquals(3 * BarrelReferenceGeometry.CANVAS_WIDTH, header.getInt(16));
         assertEquals(3 * BarrelReferenceGeometry.CANVAS_HEIGHT, header.getInt(20));
      }
   }
}
