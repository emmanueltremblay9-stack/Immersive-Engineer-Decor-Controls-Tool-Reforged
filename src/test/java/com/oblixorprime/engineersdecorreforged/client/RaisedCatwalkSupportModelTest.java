package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import org.junit.Test;

/** Checks the shipped model resources without bootstrapping Minecraft or a renderer. */
public class RaisedCatwalkSupportModelTest {
   private static final String MOD_ID = "immersive_engineer_decor_controls_tool_reforged";
   private static final String ASSETS = "assets/" + MOD_ID + "/";
   private static final String MODEL = "block/furniture/steel_catwalk_ta";

   // Semantic snapshots of the pre-fix models. Only each support plate's upper Y is exempted.
   // Key order, whitespace and equivalent numeric spelling do not affect these fingerprints.
   private static final String[] ORIGINAL_MODELS = {
      "aa52eef83c9553a0cca2e8eb57731aff32416da8aeab17d6f2a6ebf424f04803",
      "1a1c48ce18dcf8b1636e3d701449aed897245cb72ea3d4df8c0b0bf3784551e1",
      "50e0cdfa21304ff6446b19a6f0e97df66f5a509be7a481751b6d541b405e6800",
      "99dbc7bc288055e2fb2dc3799599792ab612374b414b3db4c196a8cd851a7aa6",
      "150ebb07477f7794ded4b4a8083be144ae406eae8a6bf69a0c22e7cfee1191a0",
      "fb883aadab9ee7e231e18389e12f14e1a163a0398af45e78d4a56303f2a7d39a"
   };

   @Test
   public void thickSteelPlateTouchesTheGratingWithoutMovingTheSupport() throws IOException {
      assertPlateContact(1, 10, 3, 13);
   }

   @Test
   public void thinSteelPlateTouchesTheGratingWithoutMovingTheSupport() throws IOException {
      assertPlateContact(2, 10, 4, 12);
   }

   @Test
   public void treatedWoodPlateTouchesTheGratingWithoutMovingTheSupport() throws IOException {
      assertPlateContact(5, 9, 3, 13);
   }

   @Test
   public void everyVariantKeepsTheSameDeckAndOuterRim() throws IOException {
      JsonArray deck = model(0).getAsJsonArray("elements");
      assertEquals(6, deck.size());
      for (int variant = 1; variant <= 5; variant++) {
         JsonArray elements = model(variant).getAsJsonArray("elements");
         for (int index = 0; index < deck.size(); index++) {
            assertEquals("variant " + variant + ", deck element " + index,
                  deck.get(index), elements.get(index));
         }
      }
   }

   @Test
   public void unsupportedScaffoldAndLampModelsStayUnchanged() throws IOException {
      for (int variant : new int[]{0, 3, 4}) {
         assertEquals("unrelated variant " + variant + " must stay unchanged",
               ORIGINAL_MODELS[variant], fingerprint(model(variant)));
      }
   }

   @Test
   public void allSixBlockStatesStillSelectTheirUnrotatedModels() throws IOException {
      JsonObject state = resource("blockstates/steel_catwalk_ta.json");
      assertEquals(Set.of("variants"), state.keySet());
      JsonObject variants = state.getAsJsonObject("variants");
      assertEquals(6, variants.size());
      for (int variant = 0; variant <= 5; variant++) {
         JsonObject expected = new JsonObject();
         expected.addProperty("model", MOD_ID + ":" + MODEL + variant + "_model");
         assertEquals("variant=" + variant, expected, variants.get("variant=" + variant));
      }
   }

   private static void assertPlateContact(int variant, int plateIndex, double min, double max)
         throws IOException {
      JsonObject model = model(variant);
      JsonArray elements = model.getAsJsonArray("elements");
      JsonObject underside = elements.get(5).getAsJsonObject();
      JsonObject plate = elements.get(plateIndex).getAsJsonObject();
      assertEquals("the central grating must not be lowered", 15, coordinate(underside, "from", 1), 0);
      assertEquals("the central grating remains a horizontal plane", 15, coordinate(underside, "to", 1), 0);
      assertEquals("plate must touch the actual underside with neither gap nor penetration",
            coordinate(underside, "from", 1), coordinate(plate, "to", 1), 0);
      assertFalse("the plate must remain horizontal", plate.has("rotation"));
      assertEquals("plate underside and shaft join must not move", 12, coordinate(plate, "from", 1), 0);
      for (int axis : new int[]{0, 2}) {
         assertEquals("plate footprint minimum", min, coordinate(plate, "from", axis), 0);
         assertEquals("plate footprint maximum", max, coordinate(plate, "to", axis), 0);
         assertTrue("plate must lie under the central grating",
               coordinate(plate, "from", axis) >= coordinate(underside, "from", axis)
                     && coordinate(plate, "to", axis) <= coordinate(underside, "to", axis));
      }
      assertTrue("plate retains its visible side faces", plate.getAsJsonObject("faces").keySet()
            .containsAll(Set.of("north", "east", "south", "west")));

      // Restore just the permitted coordinate in a copy; everything else must match the original.
      // This protects shafts, braces, rotations, plate underside, UVs, textures and item transforms.
      JsonObject originalGeometry = model.deepCopy();
      originalGeometry.getAsJsonArray("elements").get(plateIndex).getAsJsonObject()
            .getAsJsonArray("to").set(1, new JsonPrimitive(14));
      assertEquals("only plate.to[1] may change in support variant " + variant,
            ORIGINAL_MODELS[variant], fingerprint(originalGeometry));
   }

   private static double coordinate(JsonObject element, String bound, int axis) {
      return element.getAsJsonArray(bound).get(axis).getAsDouble();
   }

   private static JsonObject model(int variant) throws IOException {
      return resource("models/" + MODEL + variant + "_model.json");
   }

   private static JsonObject resource(String path) throws IOException {
      try (InputStream stream = RaisedCatwalkSupportModelTest.class.getClassLoader()
            .getResourceAsStream(ASSETS + path)) {
         assertNotNull("packaged resource missing: " + path, stream);
         return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
      }
   }

   private static String fingerprint(JsonElement value) {
      try {
         byte[] bytes = canonical(value).getBytes(StandardCharsets.UTF_8);
         return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      } catch (NoSuchAlgorithmException impossible) {
         throw new AssertionError("Java must provide SHA-256", impossible);
      }
   }

   private static String canonical(JsonElement value) {
      if (value.isJsonObject()) {
         JsonObject object = value.getAsJsonObject();
         return "{" + String.join(",", object.keySet().stream().sorted()
               .map(key -> new JsonPrimitive(key) + ":" + canonical(object.get(key))).toList()) + "}";
      }
      if (value.isJsonArray()) {
         StringBuilder result = new StringBuilder("[");
         for (JsonElement element : value.getAsJsonArray()) {
            if (result.length() > 1) result.append(',');
            result.append(canonical(element));
         }
         return result.append(']').toString();
      }
      if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
         return value.getAsBigDecimal().stripTrailingZeros().toPlainString();
      }
      return value.toString();
   }
}
