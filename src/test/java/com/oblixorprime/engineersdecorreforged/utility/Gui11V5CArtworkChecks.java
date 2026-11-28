package com.oblixorprime.engineersdecorreforged.utility;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import static org.junit.Assert.*;

/** Independent byte and pixel oracle for the selected runtime faceplates. */
final class Gui11V5CArtworkChecks {
   private static final String ROOT = "/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/";

   static BufferedImage panel(String path, String sha256, int width, int height) throws Exception {
      try (var input = Gui11V5CArtworkChecks.class.getResourceAsStream(ROOT + path)) {
         assertNotNull(path, input);
         byte[] bytes = input.readAllBytes();
         assertEquals(path, sha256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
         var image = ImageIO.read(new ByteArrayInputStream(bytes));
         assertNotNull(path, image);
         assertEquals(width, image.getWidth());
         assertEquals(height, image.getHeight());
         assertTrue("Exterior transparency in " + path, image.getColorModel().hasAlpha());
         return image;
      }
   }

   static void darkInterior(BufferedImage image, int canvasWidth, int canvasHeight,
                            int x, int y, int width, int height) {
      int left = (int) Math.floor(x * (double) image.getWidth() / canvasWidth);
      int top = (int) Math.floor(y * (double) image.getHeight() / canvasHeight);
      int right = (int) Math.ceil((x + width) * (double) image.getWidth() / canvasWidth);
      int bottom = (int) Math.ceil((y + height) * (double) image.getHeight() / canvasHeight);
      for (int sy = top; sy < bottom; sy++) {
         for (int sx = left; sx < right; sx++) {
            int argb = image.getRGB(sx, sy);
            int brightest = Math.max((argb >>> 16) & 255, Math.max((argb >>> 8) & 255, argb & 255));
            assertTrue("Expected dark glass/slot interior at " + sx + "," + sy,
               (argb >>> 24) >= 252 && brightest < 70);
         }
      }
   }
}
