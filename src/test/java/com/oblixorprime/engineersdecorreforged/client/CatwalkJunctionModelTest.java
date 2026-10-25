package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.Test;

/** Endpoint continuity and a frozen semantic guard for the shipped railing resources. */
public class CatwalkJunctionModelTest {
   private static final String ROOT = "/assets/immersive_engineer_decor_controls_tool_reforged/";
   private static final String[] SUFFIX = {"lr", "rr", "br"};
   // low cap, high cap, low post, sloped handrail, high post. BR retains both posts/sides.
   private static final int[][][] PARTS = {{{11,13,16,17,18}}, {{9,11,16,17,18}}, {{9,11,18,19,20},{13,15,21,22,23}}};
   private static final String[] BEFORE = {"06e5cb5b37d145f11de970217dca91413b98df523133bcfaf2b43b83e40cdf27", "3fad95c23dd3dbfcfcf8d1fc192aa139a72b0c44c6cee49209d6f01b45df5295", "47529ff2f8b4bbad46aa66402fa7f2e61fefd3b3df56481b0482ee8af26bb43e"};

   @Test public void upperCapsMeetBothUnchangedLandingTypes() throws Exception {
      double flat = topCap(resource("models/block/furniture/steel_catwalk_n_model.json"));
      double standalone = topCap(resource("models/block/furniture/steel_railing_n_model.json"));
      assertEquals(16, flat, 0); assertEquals(16, standalone, 0);
      for (int variant=0; variant<SUFFIX.length; variant++) {
         var elements = model(variant).getAsJsonArray("elements");
         for (int[] parts : PARTS[variant]) {
            double top = coordinate(elements.get(parts[1]).getAsJsonObject(), "to", 1);
            assertEquals("standard landing: stair cell is one lower", flat, top-16, 0);
            assertEquals("raised landing: standalone railing is one higher", standalone+16, top, 0);
         }
      }
   }

   @Test public void lowerCapsAndChainedSegmentsHaveTheSameSixteenPixelPitch() throws Exception {
      for (int variant=0; variant<SUFFIX.length; variant++) {
         var elements = model(variant).getAsJsonArray("elements");
         for (int[] p : PARTS[variant]) {
            double low=coordinate(elements.get(p[0]).getAsJsonObject(), "to", 1);
            double high=coordinate(elements.get(p[1]).getAsJsonObject(), "to", 1);
            assertEquals(16, low, 0); assertEquals("one-cell chain pitch",16,high-low,0);
            var slope=elements.get(p[3]).getAsJsonObject();
            var rotation=slope.getAsJsonObject("rotation");
            assertEquals("x",rotation.get("axis").getAsString());assertEquals(45,rotation.get("angle").getAsDouble(),0);
            double cy=rotation.getAsJsonArray("origin").get(1).getAsDouble();
            double cz=rotation.getAsJsonArray("origin").get(2).getAsDouble();
            double intercept=cy+cz+Math.sqrt(2)*(coordinate(slope,"to",1)-cy);
            assertEquals("rotated handrail top follows y+z=32",32,intercept,1e-9);
         }
      }
   }

   @Test public void onlyReviewedRailingCoordinatesChange() throws Exception {
      for (int variant=0;variant<SUFFIX.length;variant++) {
         JsonObject copy=model(variant).deepCopy();var elements=copy.getAsJsonArray("elements");
         for (int[] p : PARTS[variant]) {
            var low=elements.get(p[0]).getAsJsonObject();var high=elements.get(p[1]).getAsJsonObject();
            assertEquals(14.5,coordinate(low,"from",1),0);assertEquals(16,coordinate(low,"to",1),0);
            assertEquals(30.5,coordinate(high,"from",1),0);assertEquals(32,coordinate(high,"to",1),0);
            assertEquals(14.5,coordinate(elements.get(p[2]).getAsJsonObject(),"to",1),0);
            assertEquals(30.5,coordinate(elements.get(p[4]).getAsJsonObject(),"to",1),0);
            set(low,"from",1,14);set(low,"to",1,15.5);set(high,"from",1,29);set(high,"to",1,30.5);
            set(elements.get(p[2]).getAsJsonObject(),"to",1,14);set(elements.get(p[4]).getAsJsonObject(),"to",1,29);
            var slope=elements.get(p[3]).getAsJsonObject();set(slope,"from",1,23);set(slope,"to",1,24);
            slope.getAsJsonObject("rotation").getAsJsonArray("origin").set(1,new JsonPrimitive(16));
         }
         assertEquals("all other geometry, poles, UVs, textures and metadata stay unchanged",BEFORE[variant],fingerprint(copy));
      }
   }

   @Test public void everyRotationSideAndWaterVariantKeepsItsExistingModel() throws Exception {
      var variants=resource("blockstates/steel_catwalk_stairs.json").getAsJsonObject("variants");
      assertEquals(32,variants.size());
      for(String facing:new String[]{"north","east","south","west"})for(boolean l:new boolean[]{false,true})
         for(boolean right:new boolean[]{false,true})for(boolean wet:new boolean[]{false,true}) {
            String key="facing="+facing+",left_railing="+l+",right_railing="+right+",waterlogged="+wet;
            var mapping=variants.getAsJsonObject(key);assertNotNull(key,mapping);
            String suffix=l?(right?"_br":"_lr"):(right?"_rr":"");
            assertEquals("immersive_engineer_decor_controls_tool_reforged:block/furniture/steel_catwalk_stairs"+suffix+"_model",mapping.get("model").getAsString());
            int expected=switch(facing){case "east"->90;case "south"->180;case "west"->270;default->0;};
            assertEquals(expected,mapping.has("y")?mapping.get("y").getAsInt():0);
         }
   }

   private static JsonObject model(int variant)throws Exception{return resource("models/block/furniture/steel_catwalk_stairs_"+SUFFIX[variant]+"_model.json");}
   private static JsonObject resource(String path)throws Exception{
      try(var input=CatwalkJunctionModelTest.class.getResourceAsStream(ROOT+path)){
         assertNotNull(path,input);return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
      }
   }
   private static double topCap(JsonObject model){
      double top=Double.NEGATIVE_INFINITY;
      for(var e:model.getAsJsonArray("elements")){var b=e.getAsJsonObject();if(coordinate(b,"from",1)==15&&coordinate(b,"to",1)==16)top=Math.max(top,coordinate(b,"to",1));}
      return top;
   }
   private static double coordinate(JsonObject e,String v,int axis){return e.getAsJsonArray(v).get(axis).getAsDouble();}
   private static void set(JsonObject e,String v,int axis,double value){e.getAsJsonArray(v).set(axis,new JsonPrimitive(value));}
   private static String fingerprint(JsonElement value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes(StandardCharsets.UTF_8)));}
   private static String canonical(JsonElement value){
      if(value.isJsonObject()){var o=value.getAsJsonObject();return "{"+String.join(",",o.keySet().stream().sorted().map(k->new JsonPrimitive(k)+":"+canonical(o.get(k))).toList())+"}";}
      if(value.isJsonArray()){StringBuilder s=new StringBuilder("[");for(var e:value.getAsJsonArray()){if(s.length()>1)s.append(',');s.append(canonical(e));}return s.append(']').toString();}
      if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isNumber())return value.getAsBigDecimal().stripTrailingZeros().toPlainString();
      return value.toString();
   }
}
