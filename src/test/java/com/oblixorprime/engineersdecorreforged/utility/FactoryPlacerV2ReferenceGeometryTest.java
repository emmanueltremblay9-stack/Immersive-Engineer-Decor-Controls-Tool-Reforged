package com.oblixorprime.engineersdecorreforged.utility;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.Test;
import static org.junit.Assert.*;

public final class FactoryPlacerV2ReferenceGeometryTest {
   @Test public void retainedReferenceGeometryAndState() { FactoryPlacerV2ReferenceChecks.main(new String[0]); }
   @Test public void actualRetainedPngMatchesGeometry() throws Exception {
      try (var input = getClass().getResourceAsStream("/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_placer_v2_reference/background.png")) {
         assertNotNull(input);
         byte[] png = input.readAllBytes();
         assertEquals(FactoryPlacerV2ReferenceGeometry.SOURCE_SHA256,
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png)));
         var header = ByteBuffer.wrap(png);
         assertEquals(1199, header.getInt(16));
         assertEquals(1312, header.getInt(20));
         assertEquals(FactoryPlacerV2ReferenceGeometry.SOURCE_WIDTH, header.getInt(16));
         assertEquals(FactoryPlacerV2ReferenceGeometry.SOURCE_HEIGHT, header.getInt(20));
         assertEquals(1991965, png.length);
      }
   }
}
