package com.oblixorprime.engineersdecorreforged.client;

import static org.junit.Assert.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Test;

/** Contact/UV/coverage checks against the unchanged original bulb and beam resources. */
public class IronBulbSupportModelTest {
   private static final String ROOT = "/assets/immersive_engineer_decor_controls_tool_reforged/";

   @Test public void attachedBulbPreservesEveryOriginalFaceUvTextureAndCageElement() throws Exception {
      JsonObject original = resource("models/block/light/bulb_light_model.json");
      JsonObject mounted = resource("models/block/light/bulb_under_double_t_model.json");
      assertEquals(original.get("parent"), mounted.get("parent"));
      assertEquals(original.get("textures"), mounted.get("textures"));
      var actual = mounted.getAsJsonArray("elements").deepCopy();
      for (var entry : actual) {
         var e = entry.getAsJsonObject();
         for (String vector : new String[]{"from", "to"}) {
            var xyz = e.getAsJsonArray(vector); xyz.set(2, new JsonPrimitive(xyz.get(2).getAsDouble()-5));
         }
         var origin = e.getAsJsonObject("rotation").getAsJsonArray("origin");
         origin.set(2, new JsonPrimitive(origin.get(2).getAsDouble()-5));
      }
      assertEquals("only a uniform local translation; all UV/rotation/texture/face data retained", original.get("elements"), actual);
   }

   @Test public void plateTouchesActualBeamUndersideAndEntireBulbStaysInOwnerCell() throws Exception {
      var beam = resource("models/block/hsupport/steel_double_t_support_model.json");
      double underside = Double.POSITIVE_INFINITY;
      for (var entry : beam.getAsJsonArray("elements")) {
         underside = Math.min(underside, entry.getAsJsonObject().getAsJsonArray("from").get(1).getAsDouble());
      }
      assertEquals(11, underside, 0);
      var mounted = resource("models/block/light/bulb_under_double_t_model.json");
      double minY=16, maxY=0, minX=16, maxX=0, minZ=16, maxZ=0;
      for (var entry : mounted.getAsJsonArray("elements")) {
         var e=entry.getAsJsonObject(); var from=e.getAsJsonArray("from"); var to=e.getAsJsonArray("to");
         assertEquals(0,e.getAsJsonObject("rotation").get("angle").getAsDouble(),0);
         // Existing ceiling rotation x270: x'=x, y'=16-z, z'=y.
         minY=Math.min(minY,16-to.get(2).getAsDouble());maxY=Math.max(maxY,16-from.get(2).getAsDouble());
         minX=Math.min(minX,from.get(0).getAsDouble());maxX=Math.max(maxX,to.get(0).getAsDouble());
         minZ=Math.min(minZ,from.get(1).getAsDouble());maxZ=Math.max(maxZ,to.get(1).getAsDouble());
      }
      assertEquals("no gap or penetration",underside,maxY,0);assertEquals(6.75,minY,0);
      assertEquals(6,minX,0);assertEquals(10,maxX,0);assertEquals(6,minZ,0);assertEquals(10,maxZ,0);
   }

   @Test public void allOwnershipAndConnectionStatesResolveExistingBeamPlusOnlyTheMountedBulb() throws Exception {
      var parts=resource("blockstates/steel_double_t_support.json").getAsJsonArray("multipart");
      assertEquals(9,parts.size());
      for (boolean ew : new boolean[]{false,true}) for(boolean left:new boolean[]{false,true})
         for(boolean right:new boolean[]{false,true}) for(int down=0;down<=2;down++)
            for(String bulb:new String[]{"none","creative","paid"}) {
               var state=Map.of("eastwest",""+ew,"leftbeam",""+left,"rightbeam",""+right,"downconnect",""+down,"bulb_light",bulb);
               int lampCount=0,baseCount=0;
               for(var entry:parts){
                  var p=entry.getAsJsonObject();boolean matches=true;
                  for(var condition:p.getAsJsonObject("when").entrySet()) {
                     var allowed=java.util.List.of(condition.getValue().getAsString().split("\\|"));
                     matches &= allowed.contains(state.get(condition.getKey()));
                  }
                  if(!matches)continue;
                  var apply=p.getAsJsonObject("apply");String model=apply.get("model").getAsString();
                  assertNotNull(model,IronBulbSupportModelTest.class.getResourceAsStream("/assets/"+model.replace(":","/models/")+".json"));
                  if(model.endsWith("bulb_under_double_t_model")){lampCount++;assertEquals(270,apply.get("x").getAsInt());}
                  if(model.endsWith("steel_double_t_support_model"))baseCount++;
               }
               assertEquals("exactly one existing base beam",1,baseCount);
               assertEquals("empty state has no bulb; creative and paid share one model",bulb.equals("none")?0:1,lampCount);
            }
   }

   private static JsonObject resource(String path) throws Exception {
      try(var input=IronBulbSupportModelTest.class.getResourceAsStream(ROOT+path)) {
         assertNotNull(path,input);return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
      }
   }
}
