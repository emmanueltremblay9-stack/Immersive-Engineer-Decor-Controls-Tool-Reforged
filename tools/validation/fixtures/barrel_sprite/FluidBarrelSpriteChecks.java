package com.oblixorprime.engineersdecorreforged.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Executes production code against recording doubles; not a Minecraft/GPU proof. */
public final class FluidBarrelSpriteChecks {
   private static int checks;
   private static final ResourceLocation WATER = new ResourceLocation("minecraft:block/water_still");
   private static void check(boolean condition,String message) {
      checks++;
      if (!condition) throw new AssertionError(message);
   }
   private static void close(float actual,float expected,String message) {
      check(Math.abs(actual-expected)<0.000001F,message+": "+actual+" != "+expected);
   }
   private static void register(Fluid fluid, ResourceLocation texture, int color) {
      IClientFluidTypeExtensions.VALUES.put(fluid,new IClientFluidTypeExtensions(texture,color));
      if (texture!=null) Minecraft.getInstance().sprites.put(texture,
         new TextureAtlasSprite(texture,.25F,.375F,.5F,.625F));
   }
   private static void checkCoverage(GuiGraphics graphics,int x,int y,int width,int height) {
      int[][] coverage=new int[height][width];
      for (var draw:graphics.draws) {
         check(draw.width()==16 && draw.height()==16,"Partial tile squeezed instead of cropped");
         var r=draw.visible();
         check(r.x()>=x && r.y()>=y && r.right()<=x+width && r.bottom()<=y+height,"Tile escaped tank mask");
         for (int py=r.y();py<r.bottom();py++) {
            for (int px=r.x();px<r.right();px++) {
               coverage[py-y][px-x]++;
               // The clipped texture sample still has 1/16 of the sprite per GUI pixel.
               close((px-draw.x())/(float)draw.width(),Math.floorMod(px-x,16)/16.0F,"Right-edge U clipping");
               close((py-draw.y())/(float)draw.height(),Math.floorMod(py-(y+height),16)/16.0F,"Top-edge V clipping / bottom anchor");
            }
         }
      }
      for (var row:coverage) for (int value:row) check(value==1,"Missing or overlapping tank pixel");
   }
   private static void geometry() {
      register(Fluids.WATER,WATER,0xFF3F76E4);
      for (int width:new int[]{1,5,16,21,32}) for (int height=1;height<=70;height++) {
         var graphics=new GuiGraphics();
         check(FluidBarrelSpriteRenderer.draw(graphics,new FluidStack(Fluids.WATER,32000),94,107-height,width,height),"Visible water not drawn");
         check(graphics.draws.size()==((width+15)/16)*((height+15)/16),"Tile count");
         checkCoverage(graphics,94,107-height,width,height);
         check(graphics.clips.isEmpty(),"Scissor leaked");
      }
      var shifted=new GuiGraphics();
      FluidBarrelSpriteRenderer.draw(shifted,new FluidStack(Fluids.WATER,10),-47,123,21,17);
      checkCoverage(shifted,-47,123,21,17);
   }
   private static void colorsAndReload() {
      Fluid milk=new Fluid("minecraft:milk"), modded=new Fluid("example:colored");
      Fluid[] fluids={Fluids.WATER,Fluids.LAVA,milk,modded};
      int[] colors={0xFF3F76E4,0xFFFFFFFF,0xFFFFFFFF,0x7F1234AB};
      for (int i=0;i<fluids.length;i++) {
         var texture=new ResourceLocation(fluids[i].id()+"_still");
         register(fluids[i],texture,colors[i]);
         var stack=new FluidStack(fluids[i],17500);
         var graphics=new GuiGraphics();
         check(FluidBarrelSpriteRenderer.draw(graphics,stack,94,72,21,35),"Fluid extension not drawn");
         var extension=IClientFluidTypeExtensions.of(fluids[i]);
         check(extension.receivedTextureStack==stack && extension.receivedTintStack==stack,"Stack-aware extension arguments lost");
         for (var draw:graphics.draws) {
            check(draw.sprite().name().equals(texture),"Wrong fluid sprite");
            close(draw.red(),(colors[i]>>>16&255)/255.0F,"Red");
            close(draw.green(),(colors[i]>>>8&255)/255.0F,"Green");
            close(draw.blue(),(colors[i]&255)/255.0F,"Blue");
            close(draw.alpha(),(colors[i]>>>24&255)/255.0F,"Alpha");
         }
         var replacement=new TextureAtlasSprite(texture,.75F,.875F,.125F,.25F);
         Minecraft.getInstance().sprites.put(texture,replacement);
         var after=new GuiGraphics();
         FluidBarrelSpriteRenderer.draw(after,stack,94,72,21,35);
         check(after.draws.get(0).sprite()==replacement,"Stale cached sprite survived atlas reload");
      }
   }
   private static void emptyMissingAndCleanup() {
      register(Fluids.WATER,WATER,-1);
      int lookups=Minecraft.getInstance().lookups;
      for (var stack:new FluidStack[]{new FluidStack(Fluids.EMPTY,100),new FluidStack(Fluids.WATER,0),new FluidStack(Fluids.WATER,-1)}) {
         var graphics=new GuiGraphics();
         check(!FluidBarrelSpriteRenderer.draw(graphics,stack,94,37,21,70),"Empty/negative fluid drew");
         check(graphics.events.isEmpty(),"Empty fluid changed render state");
      }
      for (int[] bounds:new int[][]{{0,70},{21,0},{-1,70},{21,-1}}) {
         var graphics=new GuiGraphics();
         check(!FluidBarrelSpriteRenderer.draw(graphics,new FluidStack(Fluids.WATER,1),94,37,bounds[0],bounds[1]),"Invalid size drew");
         check(graphics.events.isEmpty(),"Invalid size changed render state");
      }
      check(Minecraft.getInstance().lookups==lookups,"Empty geometry looked up sprite");
      Fluid unknown=new Fluid("unknown");
      register(unknown,null,-1);
      var graphics=new GuiGraphics();
      check(!FluidBarrelSpriteRenderer.draw(graphics,new FluidStack(unknown,100),94,37,21,70),"Null texture drew");
      check(graphics.events.isEmpty(),"Null texture changed render state");
      ResourceLocation missing=new ResourceLocation("example:unstitched");
      register(unknown,missing,-1);
      Minecraft.getInstance().sprites.put(missing,new TextureAtlasSprite(MissingTextureAtlasSprite.getLocation(),0,1,0,1));
      check(!FluidBarrelSpriteRenderer.draw(graphics,new FluidStack(unknown,100),94,37,21,70),"Missing atlas texture drew");
      Minecraft.getInstance().sprites.remove(missing);
      check(!FluidBarrelSpriteRenderer.draw(graphics,new FluidStack(unknown,100),94,37,21,70),"Null sprite drew");
      graphics.enableScissor(100,80,110,100);
      var outer=graphics.clips.peek();
      FluidBarrelSpriteRenderer.draw(graphics,new FluidStack(Fluids.WATER,100),94,37,21,70);
      check(graphics.clips.size()==1 && graphics.clips.peek().equals(outer),"Outer clip was not restored");
      for (var draw:graphics.draws) check(draw.visible().x()>=100 && draw.visible().right()<=110,"Outer clip escaped");
      graphics.throwOnBlit=true;
      try {
         FluidBarrelSpriteRenderer.draw(graphics,new FluidStack(Fluids.WATER,100),94,37,21,70);
         throw new AssertionError("Injected failure did not propagate");
      } catch (IllegalStateException expected) {
         check(graphics.clips.size()==1 && graphics.clips.peek().equals(outer),"Scissor leaked on failed blit");
      }
   }
   private static void screenIntegration() {
      register(Fluids.WATER,WATER,0xFF3F76E4);
      var screen=new BarrelScreenProbe();
      for (int amount:new int[]{-1,0,1,457,458,1000,15999,16000,17500,31999,32000,64000,Integer.MAX_VALUE}) {
         int expected=Math.max(0,Math.min(70,(int)((long)amount*70/32000)));
         var graphics=new GuiGraphics();
         screen.draw(graphics,Fluids.WATER,amount,32000,13,19);
         check(graphics.fills.get(0).equals(new GuiGraphics.Fill(new GuiGraphics.Rect(105,54,130,128),0xFF142028)),"Tank backing changed");
         int fillCount=expected>0?5:4;
         check(graphics.fills.size()==fillCount,"Flat fluid rectangle returned or overlay count changed");
         if (expected>0) {
            check(!graphics.draws.isEmpty(),"Actual Barrel screen still uses flat fill");
            checkCoverage(graphics,107,126-expected,21,expected);
            int pop=graphics.events.lastIndexOf("clip-");
            check(pop>graphics.events.lastIndexOf("sprite"),"Scissor not restored after sprite");
            check(graphics.events.subList(pop+1,graphics.events.size()).equals(java.util.List.of("fill","fill","fill","fill")),"Meniscus/glass draw order changed");
         } else check(graphics.draws.isEmpty(),"Subpixel fluid must preserve floor-to-zero semantics");
         var edge=graphics.fills.get(fillCount-3);
         check(edge.equals(new GuiGraphics.Fill(new GuiGraphics.Rect(106,56,107,126),0xFF77858B)),"Left glass edge changed");
         check(graphics.fills.get(fillCount-2).equals(new GuiGraphics.Fill(new GuiGraphics.Rect(128,56,129,126),0xFF526067)),"Right glass edge changed");
         check(graphics.fills.get(fillCount-1).equals(new GuiGraphics.Fill(new GuiGraphics.Rect(108,56,110,124),0x66FFFFFF)),"Glass highlight changed");
      }
      // All authoritative mB values retain the previous floor-based meter semantics.
      for (int amount=0;amount<=32000;amount++) {
         var graphics=new GuiGraphics();
         screen.draw(graphics,Fluids.WATER,amount,32000,0,0);
         int fill=amount*70/32000;
         if (fill==0) check(graphics.draws.isEmpty(),"Floor boundary regression");
         else {
            check(graphics.draws.size()==2*((fill+15)/16),"Live fill tile count differs");
            check(graphics.draws.get(0).visible().bottom()==107,"Live bottom moved");
            int minY=graphics.draws.stream().mapToInt(d->d.visible().y()).min().orElseThrow();
            check(minY==107-fill,"Live top/fill amount changed");
         }
      }
      for (int capacity:new int[]{0,-1,1}) {
         var graphics=new GuiGraphics();
         screen.draw(graphics,Fluids.WATER,1,capacity,0,0);
         checkCoverage(graphics,94,37,21,70);
      }
      var empty=new GuiGraphics();
      screen.draw(empty,Fluids.EMPTY,32000,32000,0,0);
      check(empty.draws.isEmpty() && empty.fills.size()==4,"Unknown/empty type created a false surface");
      Fluid unknown=new Fluid("unknown");
      register(unknown,null,-1);
      var missing=new GuiGraphics();
      screen.draw(missing,unknown,32000,32000,0,0);
      check(missing.draws.isEmpty() && missing.fills.size()==4,"Missing sprite created a false meniscus");
   }
   private static void nativeCropOrigin() {
      for (int guiWidth:new int[]{426,427,1280,1281}) for (int scale:new int[]{1,2,3}) {
         int left=(guiWidth-201)/2;
         int crop=BarrelScreenProbe.captureOrigin(guiWidth,201,scale);
         check(crop==left*scale,"Odd-width Barrel crop shifted from integer screen origin");
         int sampleFirst=crop+99*scale, sampleLast=crop+115*scale-1;
         check(sampleFirst==(left+99)*scale,"Native ROI shifted relative to fluid");
         check(sampleLast<(left+115)*scale,"Native ROI includes the right glass edge");
      }
      for (int guiHeight:new int[]{260,261,720,721}) for (int scale:new int[]{1,2,3}) {
         int top=(guiHeight-226)/2;
         check(BarrelScreenProbe.captureOrigin(guiHeight,226,scale)==top*scale,
            "Barrel crop Y shifted from integer screen origin");
      }
   }
   public static void main(String[] args) {
      geometry(); colorsAndReload(); emptyMissingAndCleanup(); screenIntegration(); nativeCropOrigin();
      System.out.println("PASS: "+checks+" Barrel sprite assertions; recording doubles only, no mapped compile/GPU.");
   }
}
