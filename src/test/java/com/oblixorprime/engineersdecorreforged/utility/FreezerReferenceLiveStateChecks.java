package com.oblixorprime.engineersdecorreforged.utility;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceLiveState.PreviewMode;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceLiveState.TexturePatch;

/** Offline production-model checks and SOURCE-RESOLUTION CPU previews, not Minecraft execution. */
public final class FreezerReferenceLiveStateChecks {
   private static int checks;
   private FreezerReferenceLiveStateChecks() { }

   public static void main(String[] args) throws Exception {
      checkModesAndEnergy();
      checkFrostSnapshots();
      checkPatchGeometry();
      if (args.length != 2) throw new IllegalArgumentException("Usage: Checks source.png output-directory");
      checkSourceAndRender(Path.of(args[0]), Path.of(args[1]));
      System.out.println("PASS: " + checks + " offline model/pixel checks. Mapped compilation, Minecraft and GPU capture NOT RUN.");
   }

   public static void checkModesAndEnergy() {
      check(FreezerReferenceLiveState.previewMode(false, false) == PreviewMode.LIVE_ENERGY, "default production");
      check(FreezerReferenceLiveState.previewMode(true, false) == PreviewMode.EXACT_SNAPSHOT, "exact independent");
      check(FreezerReferenceLiveState.previewMode(false, true) == PreviewMode.LIVE_ENERGY, "live independent");
      check(FreezerReferenceLiveState.previewMode(true, true) == PreviewMode.EXACT_SNAPSHOT, "exact wins");
      for (PreviewMode mode : List.of(PreviewMode.PRODUCTION, PreviewMode.EXACT_SNAPSHOT)) {
         for (int energy : new int[] {0, 1, 8000, 12345, 16000}) {
            check(FreezerReferenceLiveState.energyPatches(mode, FreezerReferenceLiveState.energy(energy, 16000)).isEmpty(),
               "no patches for legacy production enum or historical exact snapshot");
         }
      }
      equal(0, FreezerReferenceLiveState.energy(0, 16000).litCells(), "empty data");
      equal(0, FreezerReferenceLiveState.energy(100, 0).stored(), "zero capacity");
      equal(0, FreezerReferenceLiveState.energy(Integer.MIN_VALUE, -1).litCells(), "invalid data bounded");
      equal(0, FreezerReferenceLiveState.energy(-1, 16000).stored(), "negative FE clamped");
      equal(16000, FreezerReferenceLiveState.energy(Integer.MAX_VALUE, 16000).stored(), "overflow FE clamped");
      equal(11, FreezerReferenceLiveState.energy(Integer.MAX_VALUE, Integer.MAX_VALUE).litCells(), "long multiplication");
      equal(12345, FreezerReferenceLiveState.energy(12345, 16000).stored(), "raw FE no 100FE rounding");
      equal(8, FreezerReferenceLiveState.energy(12345, 16000).litCells(), "raw FE quantized only in artwork");
      equal(5, FreezerReferenceLiveState.energy(8000, 16000).litCells(), "half charge five complete segments");
      equal(10, FreezerReferenceLiveState.energy(15999, 16000).litCells(), "not full early");
      equal(11, FreezerReferenceLiveState.energy(16000, 16000).litCells(), "full");
      int previous = -1;
      for (int energy = 0; energy <= 16000; energy++) {
         var state = FreezerReferenceLiveState.energy(energy, 16000);
         equal(energy, state.stored(), "exact FE for every valid value");
         check(state.litCells() >= previous, "monotonic quantization");
         check(state.litCells() >= 0 && state.litCells() <= 11, "bounded count");
         previous = state.litCells();
      }
      expectInvalid(() -> new FreezerReferenceLiveState.Rect(-1, 0, 2, 2));
      expectInvalid(() -> new FreezerReferenceLiveState.Rect(1253, 0, 2, 2));
   }

   public static void checkFrostSnapshots() {
      var defaults = FreezerReferenceLiveState.frost(0, 180, 0, 0, false);
      equal(0, defaults.percent(), "default zero progress");
      check(!defaults.artworkIsStatic() && defaults.cleanChamberAssetAvailable(), "candidate layer availability");
      for (int status = 0; status <= 6; status++) {
         for (int phase = 0; phase <= 4; phase++) {
            var state = FreezerReferenceLiveState.frost(1, 180, phase, status, status == 3);
            equal(1, state.ticks(), "progress not inferred/reset by status");
            equal(phase, state.phase(), "authoritative phase not inferred from time");
            equal(status, state.status(), "authoritative status");
            check(state.redstonePowered() == (status == 3), "redstone independently retained");
            check(FreezerReferenceLiveState.frostPatches(state).isEmpty(), "no fake clear/progressive frost");
         }
      }
      equal(1, FreezerReferenceLiveState.frost(1, 180, 1, 4, false).ticks(), "last8FE transient waiting/progress1");
      equal(0, FreezerReferenceLiveState.frost(0, 180, 0, 4, false).ticks(), "next tick server reset");
      equal(99, FreezerReferenceLiveState.frost(179, 180, 4, 2, false).percent(), "pre-completion");
      // A synthetic full snapshot is representable but never latched or manufactured.
      equal(100, FreezerReferenceLiveState.frost(180, 180, 4, 2, false).percent(), "pure full snapshot");
      equal(0, FreezerReferenceLiveState.frost(0, 180, 0, 6, false).percent(), "water bucket completes to invalid input");
      equal(0, FreezerReferenceLiveState.frost(0, 180, 0, 0, false).percent(), "single ice completes idle");
      equal(0, FreezerReferenceLiveState.frost(0, 180, 0, 1, false).percent(), "remaining valid stack ready");
      equal(0, FreezerReferenceLiveState.frost(-100, 180, -2, 0, false).ticks(), "negative progress");
      equal(180, FreezerReferenceLiveState.frost(Integer.MAX_VALUE, 180, 9, 0, false).ticks(), "progress bounded");
      equal(4, FreezerReferenceLiveState.frost(1, 180, 9, 0, false).phase(), "phase bounded");
      equal(0, FreezerReferenceLiveState.frost(12, 0, 0, 0, false).percent(), "zero duration safe");
   }

   public static void checkPatchGeometry() {
      for (int cells = 0; cells <= 11; cells++) {
         int energy = (cells * 16000 + 10) / 11;
         var state = FreezerReferenceLiveState.energy(energy, 16000);
         equal(cells, state.litCells(), "all cell states reachable");
         var patches = FreezerReferenceLiveState.energyPatches(state);
         check(patches.size() < 40, "bounded texture calls");
         if (cells == 8) check(patches.isEmpty(), "retained 8/11 exact snapshot has no substitutions");
         boolean[][] touched = new boolean[1254][1254];
         for (var patch : patches) {
            var donor = patch.lit() ? FreezerReferenceLiveState.LIT_DONOR : FreezerReferenceLiveState.DARK_DONOR;
            var source = patch.source();
            var target = patch.destination();
            check(donor.contains(source.x(), source.y()) && donor.contains(source.right() - 1, source.bottom() - 1), "source inside donor");
            for (int y = target.y(); y < target.bottom(); y++) {
               for (int x = target.x(); x < target.right(); x++) {
                  check(FreezerReferenceLiveState.allowsEnergyPixel(x, y), "no housing or frost patch");
                  check(!touched[y][x], "no double-patched pixels");
                  touched[y][x] = true;
               }
            }
         }
      }
   }

   private static void checkSourceAndRender(Path sourcePath, Path directory) throws Exception {
      String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(sourcePath)));
      check(FreezerReferenceGeometry.SOURCE_SHA256.equals(hash), "original PNG exact bytes");
      BufferedImage source = ImageIO.read(sourcePath.toFile());
      equal(1254, source.getWidth(), "source width");
      equal(1254, source.getHeight(), "source height");
      Files.createDirectories(directory);
      StringBuilder report = new StringBuilder("{\n  \"kind\": \"SOURCE_RESOLUTION_CPU_PRODUCTION_DRAW_PLAN_NOT_MINECRAFT\",\n  \"source_sha256\": \"" + hash + "\",\n  \"states\": [\n");
      StringBuilder plans = new StringBuilder("{\n  \"source_sha256\": \"" + hash + "\",\n  \"plans\": [\n");
      int previousAmber = -1;
      for (int cells = 0; cells <= 11; cells++) {
         int energy = (cells * 16000 + 10) / 11;
         var state = FreezerReferenceLiveState.energy(energy, 16000);
         var patches = FreezerReferenceLiveState.energyPatches(state);
         BufferedImage rendered = render(source, patches);
         int different = 0, outside = 0, amber = 0;
         for (int y = 0; y < 1254; y++) {
            for (int x = 0; x < 1254; x++) {
               int color = rendered.getRGB(x, y);
               if (color != source.getRGB(x, y)) {
                  different++;
                  if (!FreezerReferenceLiveState.allowsEnergyPixel(x, y)) outside++;
               }
               if (FreezerReferenceLiveState.allowsEnergyPixel(x, y)) {
                  int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
                  if (red > 150 && green > 80 && red > blue * 1.8D) amber++;
               }
            }
         }
         equal(0, outside, "every source pixel outside conservative energy polygon unchanged");
         if (cells == 8) equal(0, different, "source snapshot exact across entire panel");
         if (cells == 0) equal(0, amber, "no energized yellow pixels inside zero-charge mask");
         check(amber > previousAmber, "strictly monotonic lit amber pixel count");
         previousAmber = amber;
         ImageIO.write(rendered, "PNG", directory.resolve("source-cells-" + cells + ".png").toFile());
         report.append("    {\"energy\":" + energy + ",\"cells\":" + cells + ",\"patches\":" + patches.size()
            + ",\"different_pixels\":" + different + ",\"outside_mask_differences\":" + outside + ",\"amber_pixels\":" + amber + "}")
            .append(cells == 11 ? "\n" : ",\n");
         plans.append("    {\"energy\":" + energy + ",\"cells\":" + cells + ",\"patches\":[");
         for (int i = 0; i < patches.size(); i++) {
            var patch = patches.get(i);
            if (i > 0) plans.append(',');
            plans.append("{\"cell\":" + patch.cell() + ",\"lit\":" + patch.lit() + ",\"source\":" + rectJson(patch.source())
               + ",\"destination\":" + rectJson(patch.destination()) + "}");
         }
         plans.append("]}").append(cells == 11 ? "\n" : ",\n");
      }
      report.append("  ],\n  \"frost_draw_calls\":0,\n  \"mapped_compile\":\"NOT_RUN\",\n  \"minecraft_capture\":\"NOT_RUN\"\n}\n");
      plans.append("  ]\n}\n");
      Files.writeString(directory.resolve("cpu-pixel-proof.json"), report);
      Files.writeString(directory.resolve("production-draw-plans.json"), plans);
      renderComparison(source, directory);
   }

   /** Integer source-to-source nearest-center blits. No AWT resize/library guesses. */
   private static BufferedImage render(BufferedImage original, List<TexturePatch> patches) {
      BufferedImage result = new BufferedImage(1254, 1254, BufferedImage.TYPE_INT_RGB);
      result.setRGB(0, 0, 1254, 1254, original.getRGB(0, 0, 1254, 1254, null, 0, 1254), 0, 1254);
      for (var patch : patches) {
         var s = patch.source();
         var d = patch.destination();
         for (int y = 0; y < d.height(); y++) {
            int sy = s.y() + (int)Math.floor((y + 0.5D) * s.height() / d.height());
            for (int x = 0; x < d.width(); x++) {
               int sx = s.x() + (int)Math.floor((x + 0.5D) * s.width() / d.width());
               result.setRGB(d.x() + x, d.y() + y, original.getRGB(sx, sy));
            }
         }
      }
      return result;
   }

   private static void renderComparison(BufferedImage source, Path directory) throws Exception {
      int[] values = {0, 8000, 16000};
      BufferedImage sheet = new BufferedImage(990, 498, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = sheet.createGraphics();
      g.setColor(new Color(24, 27, 32)); g.fillRect(0, 0, 990, 498);
      g.setColor(Color.WHITE); g.setFont(new Font("Dialog", Font.BOLD, 14));
      g.drawString("CPU SOURCE-BLIT PREVIEW ONLY. NOT MINECRAFT / NOT GPU ACCEPTANCE.", 12, 22);
      for (int i = 0; i < values.length; i++) {
         var energy = FreezerReferenceLiveState.energy(values[i], 16000);
         var result = render(source, FreezerReferenceLiveState.energyPatches(energy));
         ImageIO.write(result, "PNG", directory.resolve("source-fe-" + values[i] + ".png").toFile());
         BufferedImage thumb = new BufferedImage(314, 314, BufferedImage.TYPE_INT_RGB);
         for (int y = 0; y < 314; y++) for (int x = 0; x < 314; x++) {
            thumb.setRGB(x, y, result.getRGB((int)((x + 0.5D) * 1254 / 314), (int)((y + 0.5D) * 1254 / 314)));
         }
         g.drawImage(thumb, 8 + i * 330, 60, null);
         g.setColor(Color.WHITE);
         g.drawString(values[i] + " / 16000 FE | " + energy.litCells() + "/11 cells", 12 + i * 330, 48);
         // Enlarged source crop intentionally includes preserved warm housing reflections.
         BufferedImage crop = result.getSubimage(495, 662, 305, 44);
         g.drawImage(crop, 12 + i * 330, 382, null);
         g.drawString("Energy-only CPU preview; frost not rendered here", 12 + i * 330, 451);
      }
      g.setFont(new Font("Dialog", Font.PLAIN, 12));
      g.drawString("Source-derived cells; metallic reflection retained. Full proof: outside rounded energy mask = 0 changed pixels.", 12, 480);
      g.dispose();
      ImageIO.write(sheet, "PNG", directory.resolve("CPU-NOT-GAME-energy-comparison.png").toFile());
   }

   private static String rectJson(FreezerReferenceLiveState.Rect r) {
      return "[" + r.x() + "," + r.y() + "," + r.width() + "," + r.height() + "]";
   }
   private static void equal(int expected, int actual, String message) { check(expected == actual, message + ": " + actual + " != " + expected); }
   private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
   private static void expectInvalid(Runnable action) {
      try { action.run(); throw new AssertionError("expected invalid rectangle"); }
      catch (IllegalArgumentException expected) { checks++; }
   }
}
