package com.oblixorprime.engineersdecorreforged.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independent coordinate/UV chart checks, without bootstrapping Minecraft. */
public final class VerticalDoubleTModelTest {
   private static final Path ROOT=Path.of("src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged");
   private static final String BASE="models/block/hsupport/steel_double_t_support_model.json";
   private static final String VERTICAL="models/block/hsupport/vertical_steel_double_t_support_model.json";
   private static JsonObject read(String file)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve(file))).getAsJsonObject();}
   private static double[] vector(JsonArray a){return new double[]{a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble()};}
   private static double[] turn(double[] p){return new double[]{p[0],16-p[2],p[1]};}
   private static double fraction(double value,double lo,double hi){return (value-lo)/(hi-lo);}
   private static double[] uv(JsonObject face,String direction,double[] p,double[] lo,double[] hi) {
      double x=fraction(p[0],lo[0],hi[0]),y=fraction(p[1],lo[1],hi[1]),z=fraction(p[2],lo[2],hi[2]);
      double a,b;
      switch(direction) {
         case "north":a=1-x;b=1-y;break;case "south":a=x;b=1-y;break;
         case "east":a=1-z;b=1-y;break;case "west":a=z;b=1-y;break;
         case "up":a=x;b=z;break;default:a=x;b=1-z;
      }
      double u=a,v=b;
      int rotation=face.has("rotation")?face.get("rotation").getAsInt():0;
      switch(rotation){case 90:u=b;v=1-a;break;case 180:u=1-a;v=1-b;break;case 270:u=1-b;v=a;break;default:break;}
      var rect=face.getAsJsonArray("uv");return new double[]{rect.get(0).getAsDouble()+u*(rect.get(2).getAsDouble()-rect.get(0).getAsDouble()),rect.get(1).getAsDouble()+v*(rect.get(3).getAsDouble()-rect.get(1).getAsDouble())};
   }

   @Test public void preserves_source_model_and_texture_identity()throws Exception {
      assertEquals("a314580b6f2b7c3c3ca47318b514d8090ca0b4b192eab84072e3bf4c89e14863",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(ROOT.resolve(BASE)))));
      var old=read(BASE);var model=read(VERTICAL);assertEquals(old.get("textures"),model.get("textures"));
      assertEquals(4,model.getAsJsonArray("elements").size());
   }

   @Test public void rigid_rotation_preserves_every_cuboid_and_uv_corner()throws Exception {
      var old=read(BASE).getAsJsonArray("elements");var vertical=read(VERTICAL).getAsJsonArray("elements");
      var normals=Map.of("north","up","south","down","up","south","down","north","east","east","west","west");
      for(int i=0;i<old.size();i++) {
         var a=old.get(i).getAsJsonObject();var b=vertical.get(i).getAsJsonObject();
         double[] lo=vector(a.getAsJsonArray("from")),hi=vector(a.getAsJsonArray("to"));
         double[] nl=vector(b.getAsJsonArray("from")),nh=vector(b.getAsJsonArray("to"));
         assertArrayEquals(new double[]{lo[0],16-hi[2],lo[1]},nl,1e-9);assertArrayEquals(new double[]{hi[0],16-lo[2],hi[1]},nh,1e-9);
         assertEquals(a.getAsJsonObject("faces").size(),b.getAsJsonObject("faces").size());
         for(var entry:a.getAsJsonObject("faces").entrySet()) {
            String face=entry.getKey(),next=normals.get(face);var af=entry.getValue().getAsJsonObject();var bf=b.getAsJsonObject("faces").getAsJsonObject(next);
            assertEquals(af.get("texture"),bf.get("texture"));assertEquals(af.get("uv"),bf.get("uv"));
            for(int corner=0;corner<8;corner++) {
               double[] p={((corner&1)==0?lo:hi)[0],((corner&2)==0?lo:hi)[1],((corner&4)==0?lo:hi)[2]};
               int axis=face.equals("east")||face.equals("west")?0:face.equals("up")||face.equals("down")?1:2;
               boolean positive=face.equals("east")||face.equals("up")||face.equals("south");
               if(p[axis]!=(positive?hi:lo)[axis])continue;
               assertArrayEquals("UV follows the same physical corner",uv(af,face,p,lo,hi),uv(bf,next,turn(p),nl,nh),1e-9);
            }
         }
      }
   }

   @Test public void resource_chain_is_four_wall_facings_without_an_invented_recipe()throws Exception {
      var variants=read("blockstates/vertical_steel_double_t_support.json").getAsJsonObject("variants");assertEquals(4,variants.size());
      for(var entry:Map.of("north",0,"east",90,"south",180,"west",270).entrySet()) {
         var model=variants.getAsJsonObject("facing="+entry.getKey());assertEquals(entry.getValue().intValue(),model.get("y").getAsInt());
         assertEquals("immersive_engineer_decor_controls_tool_reforged:block/hsupport/vertical_steel_double_t_support_model",model.get("model").getAsString());
      }
      assertEquals("immersive_engineer_decor_controls_tool_reforged:block/hsupport/vertical_steel_double_t_support_model",read("models/item/vertical_steel_double_t_support.json").get("parent").getAsString());
      assertFalse(Files.exists(Path.of("src/main/resources/data/immersive_engineer_decor_controls_tool_reforged/recipe/vertical_steel_double_t_support.json")));
   }
}
