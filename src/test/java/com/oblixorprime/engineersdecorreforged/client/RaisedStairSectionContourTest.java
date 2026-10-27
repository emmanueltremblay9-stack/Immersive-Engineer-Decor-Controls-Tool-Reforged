package com.oblixorprime.engineersdecorreforged.client;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.*;

/** Compare complete join boundaries with the existing cuboid union, not just area. */
public final class RaisedStairSectionContourTest {
   private static final Path ROOT=Path.of("src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/models/block");
   private record Point(double x,double z) {}
   private record Edge(Point a,Point b) {}
   private record Rect(double x0,double z0,double x1,double z1) {
      boolean contains(double x,double z){return x>x0&&x<x1&&z>z0&&z<z1;}
   }
   private static List<Rect> profile(String model)throws Exception {
      var elements=JsonParser.parseString(Files.readString(ROOT.resolve("pole/"+model+".json"))).getAsJsonObject().getAsJsonArray("elements");
      List<Rect> result=new ArrayList<>();
      for(var e:elements){var obj=e.getAsJsonObject();var a=obj.getAsJsonArray("from");var b=obj.getAsJsonArray("to");result.add(new Rect(a.get(0).getAsDouble(),a.get(1).getAsDouble(),b.get(0).getAsDouble(),b.get(1).getAsDouble()));}
      return result;
   }
   private static boolean occupied(List<Rect> r,double x,double z){return r.stream().anyMatch(a->a.contains(x,z));}
   private static Point unturn(double x,double z,int facing){return switch(facing){case 1->new Point(z,16-x);case 2->new Point(16-x,16-z);case 3->new Point(16-z,x);default->new Point(x,z);};}
   private static List<Edge> boundary(String name,int facing)throws Exception {
      List<double[]> vertices=new ArrayList<>();List<Edge> edges=new ArrayList<>();
      for(String line:Files.readAllLines(ROOT.resolve("raised_stair_v/"+name+".obj"))) {
         var f=line.split(" ");if(f[0].equals("v"))vertices.add(new double[]{Double.parseDouble(f[1])*16,Double.parseDouble(f[2])*16,Double.parseDouble(f[3])*16});
         if(!f[0].equals("f"))continue;
         double[][] v=new double[3][];for(int i=0;i<3;i++)v[i]=vertices.get(Integer.parseInt(f[i+1].split("/")[0])-1);
         assertFalse("matching neighbor must not receive a coincident bottom cap",Math.abs(v[0][1])+Math.abs(v[1][1])+Math.abs(v[2][1])<1e-8);
         for(int i=0;i<3;i++) {var a=v[i];var b=v[(i+1)%3];if(Math.abs(a[1])+Math.abs(b[1])<1e-8)edges.add(new Edge(unturn(a[0],a[2],facing),unturn(b[0],b[2],facing)));}
      }
      return edges;
   }
   private static boolean covers(List<Edge> edges,Edge expected) {
      double dx=expected.b.x-expected.a.x,dz=expected.b.z-expected.a.z,length=Math.hypot(dx,dz);
      List<double[]> intervals=new ArrayList<>();
      for(var e:edges) {
         if(Math.abs((e.a.x-expected.a.x)*dz-(e.a.z-expected.a.z)*dx)/length>1e-8
               ||Math.abs((e.b.x-expected.a.x)*dz-(e.b.z-expected.a.z)*dx)/length>1e-8)continue;
         double a=((e.a.x-expected.a.x)*dx+(e.a.z-expected.a.z)*dz)/(length*length);
         double b=((e.b.x-expected.a.x)*dx+(e.b.z-expected.a.z)*dz)/(length*length);
         intervals.add(new double[]{Math.min(a,b),Math.max(a,b)});
      }
      intervals.sort(java.util.Comparator.comparingDouble(a->a[0]));double covered=0;
      for(var interval:intervals) {if(interval[1]<covered)continue;if(interval[0]>covered+1e-8)return false;covered=Math.max(covered,interval[1]);if(covered>=1-1e-8)return true;}
      return false;
   }
   @Test public void every_join_edge_matches_the_real_pole_contour_all_sections_and_rotations()throws Exception {
      String[] sections={"wood","thin_steel","steel"};String[] models={"straight_pole_model","straight_thin_metal_pole_model","straight_thick_metal_pole_model"};String[] facings={"north","east","south","west"};
      for(int kind=0;kind<3;kind++)for(int rotation=0;rotation<4;rotation++) {
         var rects=profile(models[kind]);var edges=boundary("lower_open_"+sections[kind]+"_"+facings[rotation],rotation);assertFalse(edges.isEmpty());
         // Each emitted edge must be on the actual source union's boundary. This
         // catches diagonal corner cuts, filled steel holes, offsets and scaling.
         for(var e:edges) {
            double dx=e.b.x-e.a.x,dz=e.b.z-e.a.z,length=Math.hypot(dx,dz);assertTrue(length>1e-8);
            for(double t:new double[]{.1,.5,.9}) {
               double x=e.a.x+dx*t,z=e.a.z+dz*t,nx=-dz/length*1e-6,nz=dx/length*1e-6;
               assertNotEquals("no surplus join edge: "+sections[kind],occupied(rects,x+nx,z+nz),occupied(rects,x-nx,z-nz));
            }
         }
         // And every source boundary interval must be covered, including holes
         // and the wood's small right-angle corner steps (area alone misses them).
         var xs=new TreeSet<Double>();var zs=new TreeSet<Double>();
         for(var r:rects){xs.add(r.x0);xs.add(r.x1);zs.add(r.z0);zs.add(r.z1);}
         var xx=new ArrayList<>(xs);var zz=new ArrayList<>(zs);
         for(double x:xx)for(int i=0;i<zz.size()-1;i++) {
            double z=(zz.get(i)+zz.get(i+1))/2;
            if(occupied(rects,x-1e-6,z)!=occupied(rects,x+1e-6,z))assertTrue("missing vertical source edge",covers(edges,new Edge(new Point(x,zz.get(i)),new Point(x,zz.get(i+1)))));
         }
         for(double z:zz)for(int i=0;i<xx.size()-1;i++) {
            double x=(xx.get(i)+xx.get(i+1))/2;
            if(occupied(rects,x,z-1e-6)!=occupied(rects,x,z+1e-6))assertTrue("missing horizontal source edge",covers(edges,new Edge(new Point(xx.get(i),z),new Point(xx.get(i+1),z))));
         }
      }
   }
}
