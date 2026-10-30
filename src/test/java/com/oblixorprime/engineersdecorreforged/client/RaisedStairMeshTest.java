package com.oblixorprime.engineersdecorreforged.client;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Geometry conservation and native resource wiring, independent of renderer implementation. */
public final class RaisedStairMeshTest {
   private static final Path ASSETS=Path.of("src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged");
   private static final Path MODELS=ASSETS.resolve("models/block/raised_stair_v");
   private static double area(double[] a,double[] b,double[] c) {
      double x=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]);
      double y=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]);
      double z=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
      return Math.sqrt(x*x+y*y+z*z)/2;
   }
   private static double[] vec(String[] fields,int offset) {
      return new double[]{Double.parseDouble(fields[offset]),Double.parseDouble(fields[offset+1]),Double.parseDouble(fields[offset+2])};
   }
   private static double objArea(String name,boolean bounded) throws Exception {
      List<double[]> vertices=new ArrayList<>();double result=0;
      for(String line:Files.readAllLines(MODELS.resolve(name+".obj"))) {
         String[] f=line.split(" ");
         if(f[0].equals("v")) {
            double[] v=vec(f,1);vertices.add(v);
            if(bounded)for(double x:v)assertTrue("rendered part stays within its owner cell",x>=-1e-9&&x<=1+1e-9);
         } else if(f[0].equals("vt")) {
            for(int i=1;i<=2;i++)assertTrue("UV stays within its own atlas sprite",Double.parseDouble(f[i])>=-1e-9&&Double.parseDouble(f[i])<=1+1e-9);
         } else if(f[0].equals("f")) {
            assertEquals("triangulation remains explicit",4,f.length);
            double[][] p=new double[3][];
            for(int i=0;i<3;i++)p[i]=vertices.get(Integer.parseInt(f[i+1].split("/")[0])-1);
            double a=area(p[0],p[1],p[2]);assertTrue("no degenerate emitted triangle",a>1e-12);result+=a;
         }
      }
      assertFalse(vertices.isEmpty());return result;
   }

   @Test public void split_and_uv_tiling_preserve_the_approved_surface() throws Exception {
      var mesh=JsonParser.parseString(Files.readString(Path.of("tools/models/raised_stair_v_approved.json"))).getAsJsonObject();
      var vertices=mesh.getAsJsonArray("vertices");double area=0;
      for(var element:mesh.getAsJsonArray("triangles")) {
         var ids=element.getAsJsonArray();double[][] p=new double[3][];
         for(int i=0;i<3;i++) {
            var v=vertices.get(ids.get(i).getAsInt()).getAsJsonArray();p[i]=new double[]{v.get(0).getAsDouble()/16,v.get(1).getAsDouble()/16,v.get(2).getAsDouble()/16};
         }
         area+=area(p[0],p[1],p[2]);
      }
      // The approved source is immutable; only its stem below -4 is now adapted.
      // The closed wood surface still splits exactly between its two owner cells.
      double adapted=objArea("full",false);
      for(String facing:List.of("north","east","south","west"))
         assertEquals("split adds no caps or surface at the ownership boundary",adapted,
            objArea("upper_"+facing,true)+objArea("lower_"+facing,true),1e-9);
      assertNotEquals("stem contour is intentionally no longer the old diagonal chamfer",area,adapted,1e-8);
   }

   @Test public void metal_stair_and_existing_state_resolution_are_exact() throws Exception {
      byte[] metal=Files.readAllBytes(ASSETS.resolve("models/block/furniture/steel_catwalk_stairs_model.json"));
      assertEquals("704f5cbc2249d4418e70b6cb067ae08dce2f0ed116f2f0d844d0f652ec3cdb37",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(metal)));
      var compact=JsonParser.parseString(Files.readString(ASSETS.resolve("blockstates/steel_catwalk_stairs.json"))).getAsJsonObject().getAsJsonObject("variants");
      var raised=JsonParser.parseString(Files.readString(ASSETS.resolve("blockstates/raised_steel_catwalk_stairs.json"))).getAsJsonObject().getAsJsonObject("variants");
      for(String facing:List.of("north","east","south","west"))for(boolean left:List.of(false,true))
         for(boolean right:List.of(false,true)) {
            String base="facing="+facing+",left_railing="+left+",right_railing="+right;
            var compactVariant=compact.getAsJsonObject(base);
            var raisedVariant=raised.getAsJsonObject(base);
            assertNotNull("compact stair model mapping "+base,compactVariant);
            assertNotNull("raised stair model mapping "+base,raisedVariant);
            assertEquals("fluid schema change keeps identical geometry mapping",raisedVariant,compactVariant);
         }
   }

   @Test public void section_models_use_distinct_lower_meshes_and_preserve_upper_shape() throws Exception {
      for(String facing:List.of("north","east","south","west")) {
         String previous=null;
         for(String section:List.of("wood","thin_steel","steel")) {
            var model=JsonParser.parseString(Files.readString(MODELS.resolve("lower_"+section+"_"+facing+".json"))).getAsJsonObject();
            assertEquals("neoforge:obj",model.get("loader").getAsString());
            if(previous!=null)assertNotEquals("section must not alias another lower mesh",previous,model.get("model").getAsString());
            previous=model.get("model").getAsString();
         }
         assertEquals(objArea("upper_"+facing,true),objArea("upper_thin_steel_"+facing,true),1e-9);
         assertEquals(objArea("upper_"+facing,true),objArea("upper_steel_"+facing,true),1e-9);
      }
      assertFalse("no unapproved crafting recipe",Files.exists(Path.of("src/main/resources/data/immersive_engineer_decor_controls_tool_reforged/recipe/raised_steel_catwalk_stairs.json")));
   }
}
