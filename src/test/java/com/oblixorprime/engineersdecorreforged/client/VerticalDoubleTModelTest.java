package com.oblixorprime.engineersdecorreforged.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
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

   @Test public void resource_chain_covers_every_wall_mount_facing_and_connection()throws Exception {
      var parts=read("blockstates/vertical_steel_double_t_support.json").getAsJsonArray("multipart");assertEquals(28,parts.size());
      for(boolean across:new boolean[]{false,true})for(String face:new String[]{"north","east","south","west"})for(boolean negative:new boolean[]{false,true})for(boolean positive:new boolean[]{false,true}) {
         var values=Map.of("wall_horizontal",Boolean.toString(across),"facing",face,"join_negative",Boolean.toString(negative),"join_positive",Boolean.toString(positive));
         int count=0;
         for(var element:parts) {
            var part=element.getAsJsonObject();boolean match=true;
            for(var entry:part.getAsJsonObject("when").entrySet())match&=entry.getValue().getAsString().equals(values.get(entry.getKey()));
            if(!match)continue;count++;
            var apply=part.getAsJsonObject("apply");String model=apply.get("model").getAsString();
            assertTrue(model.startsWith("immersive_engineer_decor_controls_tool_reforged:block/hsupport/"));
            var resource=read("models/"+model.split(":",2)[1]+".json");assertEquals(4,resource.getAsJsonArray("elements").size());
            assertEquals(read(BASE).get("textures"),resource.get("textures"));
            assertEquals(Map.of("north",0,"east",90,"south",180,"west",270).get(face).intValue(),apply.get("y").getAsInt());
         }
         assertEquals("one core plus the selected existing half-cross arms",1+(negative?1:0)+(positive?1:0),count);
      }
      assertEquals("immersive_engineer_decor_controls_tool_reforged:block/hsupport/vertical_steel_double_t_support_model",read("models/item/vertical_steel_double_t_support.json").get("parent").getAsString());
      assertFalse(Files.exists(Path.of("src/main/resources/data/immersive_engineer_decor_controls_tool_reforged/recipe/vertical_steel_double_t_support.json")));
   }

   @Test public void ceiling_join_adds_only_the_aligned_upright_terminal_piece()throws Exception {
      var parts=read("blockstates/vertical_steel_double_t_support.json").getAsJsonArray("multipart");
      var expected=Map.of("north",0,"east",90,"south",180,"west",270);var seen=new boolean[4];int count=0;
      String modelName="immersive_engineer_decor_controls_tool_reforged:block/hsupport/wall_vertical_double_t_ceiling_end";
      for(var element:parts) {
         var part=element.getAsJsonObject();var when=part.getAsJsonObject("when");
         if(!when.has("ceiling_join"))continue;
         assertEquals("true",when.get("ceiling_join").getAsString());
         assertEquals("false",when.get("wall_horizontal").getAsString());
         String facing=when.get("facing").getAsString();int index=List.of("north","east","south","west").indexOf(facing);
         assertTrue("one terminal piece for each horizontal facing",index>=0&&!seen[index]);seen[index]=true;count++;
         var apply=part.getAsJsonObject("apply");assertEquals(modelName,apply.get("model").getAsString());
         assertEquals(expected.get(facing).intValue(),apply.get("y").getAsInt());
      }
      assertEquals(4,count);for(boolean present:seen)assertTrue(present);
      var model=read("models/block/hsupport/wall_vertical_double_t_ceiling_end.json");
      assertEquals(read(BASE).get("textures"),model.get("textures"));var elements=model.getAsJsonArray("elements");assertEquals(4,elements.size());
      for(var element:elements) {
         var box=element.getAsJsonObject();assertEquals(16,box.getAsJsonArray("from").get(1).getAsInt());
         assertEquals(27,box.getAsJsonArray("to").get(1).getAsInt());
      }
   }

   @Test public void horizontal_model_is_the_same_upright_model_turned_in_wall_with_uvs()throws Exception {
      var source=read(VERTICAL).getAsJsonArray("elements");var target=read("models/block/hsupport/wall_horizontal_steel_double_t_model.json").getAsJsonArray("elements");
      var normals=Map.of("north","north","south","south","up","east","down","west","east","down","west","up");
      for(int i=0;i<source.size();i++) {
         var a=source.get(i).getAsJsonObject();var b=target.get(i).getAsJsonObject();
         double[] lo=vector(a.getAsJsonArray("from")),hi=vector(a.getAsJsonArray("to"));
         double[] nl=vector(b.getAsJsonArray("from")),nh=vector(b.getAsJsonArray("to"));
         assertArrayEquals(new double[]{lo[1],16-hi[0],lo[2]},nl,1e-9);assertArrayEquals(new double[]{hi[1],16-lo[0],hi[2]},nh,1e-9);
         for(var entry:a.getAsJsonObject("faces").entrySet()) {
            String face=entry.getKey(),next=normals.get(face);var af=entry.getValue().getAsJsonObject();var bf=b.getAsJsonObject("faces").getAsJsonObject(next);assertEquals(af.get("texture"),bf.get("texture"));
            for(int corner=0;corner<8;corner++) {
               double[] p={((corner&1)==0?lo:hi)[0],((corner&2)==0?lo:hi)[1],((corner&4)==0?lo:hi)[2]};
               int axis=face.equals("east")||face.equals("west")?0:face.equals("up")||face.equals("down")?1:2;
               boolean positive=face.equals("east")||face.equals("up")||face.equals("south");if(p[axis]!=(positive?hi:lo)[axis])continue;
               assertArrayEquals("same texture coordinate at every rotated physical corner",uv(af,face,p,lo,hi),uv(bf,next,new double[]{p[1],16-p[0],p[2]},nl,nh),1e-9);
            }
         }
      }
   }
   @Test public void connector_cuboids_are_not_stretched_by_a_pole_specific_clip()throws Exception {
      var old=read("models/block/hsupport/steel_double_t_support_xconnect_model.json").getAsJsonArray("elements");
      var next=read("models/block/hsupport/wall_vertical_double_t_positive.json").getAsJsonArray("elements");
      var normals=Map.of("north","up","south","down","up","south","down","north","east","east","west","west");
      for(int i=0;i<old.size();i++) {
         var a=old.get(i).getAsJsonObject();var b=next.get(i).getAsJsonObject();double[] lo=vector(a.getAsJsonArray("from")),hi=vector(a.getAsJsonArray("to"));
         double[] nl=vector(b.getAsJsonArray("from")),nh=vector(b.getAsJsonArray("to"));
         assertArrayEquals(new double[]{lo[0],16-hi[2],lo[1]},nl,1e-9);assertArrayEquals(new double[]{hi[0],16-lo[2],hi[1]},nh,1e-9);
         for(var entry:a.getAsJsonObject("faces").entrySet()) {
            String face=entry.getKey(),target=normals.get(face);var af=entry.getValue().getAsJsonObject();var bf=b.getAsJsonObject("faces").getAsJsonObject(target);
            for(int corner=0;corner<8;corner++) {
               double[] point={((corner&1)==0?lo:hi)[0],((corner&2)==0?lo:hi)[1],((corner&4)==0?lo:hi)[2]};
               int axis=face.equals("east")||face.equals("west")?0:face.equals("up")||face.equals("down")?1:2;
               boolean positive=face.equals("east")||face.equals("up")||face.equals("south");if(point[axis]!=(positive?hi:lo)[axis])continue;
               assertArrayEquals("connector UV at the exact rotated physical corner",uv(af,face,point,lo,hi),uv(bf,target,turn(point),nl,nh),1e-9);
            }
         }
      }
   }

}
