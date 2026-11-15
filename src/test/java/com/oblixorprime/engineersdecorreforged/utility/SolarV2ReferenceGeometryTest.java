package com.oblixorprime.engineersdecorreforged.utility;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SolarV2ReferenceGeometryTest {
   @Test public void retainedReferenceGeometryAndState() { SolarV2ReferenceChecks.main(new String[0]); }
   @Test public void actualRetainedPngMatchesGeometry() throws Exception {
      try (var input = getClass().getResourceAsStream("/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/solar_v2_reference/background.png")) {
         assertNotNull(input);
         byte[] png = input.readAllBytes();
         assertEquals(SolarV2ReferenceGeometry.SOURCE_SHA256,
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png)));
         var header = ByteBuffer.wrap(png);
         assertEquals(1254, header.getInt(16));
         assertEquals(1254, header.getInt(20));
         assertEquals(SolarV2ReferenceGeometry.SOURCE_WIDTH, header.getInt(16));
         assertEquals(SolarV2ReferenceGeometry.SOURCE_HEIGHT, header.getInt(20));
         assertEquals(3 * SolarV2ReferenceGeometry.CANVAS_WIDTH, header.getInt(16));
         assertEquals(3 * SolarV2ReferenceGeometry.CANVAS_HEIGHT, header.getInt(20));
      }
   }
}
