package net.minecraft.client.gui;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Recording double. Models axis-aligned GUI-space clipping, not the real GPU. */
public final class GuiGraphics {
   public record Rect(int x,int y,int right,int bottom) {
      Rect intersect(Rect b) { return new Rect(Math.max(x,b.x),Math.max(y,b.y),Math.min(right,b.right),Math.min(bottom,b.bottom)); }
   }
   public record Draw(Rect visible,int x,int y,int width,int height,TextureAtlasSprite sprite,
                      float red,float green,float blue,float alpha) {}
   public record Fill(Rect bounds,int color) {}
   public final Deque<Rect> clips = new ArrayDeque<>();
   public final List<Draw> draws = new ArrayList<>();
   public final List<Fill> fills = new ArrayList<>();
   public final List<String> events = new ArrayList<>();
   public boolean throwOnBlit;
   public void enableScissor(int x,int y,int right,int bottom) {
      var clip=new Rect(x,y,right,bottom);
      clips.push(clips.isEmpty() ? clip : clip.intersect(clips.peek()));
      events.add("clip+");
   }
   public void disableScissor() { clips.pop(); events.add("clip-"); }
   public void fill(int x,int y,int right,int bottom,int color) {
      fills.add(new Fill(new Rect(x,y,right,bottom),color)); events.add("fill");
   }
   public void blit(int x,int y,int z,int width,int height,TextureAtlasSprite sprite,
                    float red,float green,float blue,float alpha) {
      if (throwOnBlit) throw new IllegalStateException("Injected draw failure");
      if (z != 0) throw new AssertionError("Unexpected depth");
      Rect bounds=new Rect(x,y,x+width,y+height);
      draws.add(new Draw(clips.isEmpty() ? bounds : bounds.intersect(clips.peek()),
         x,y,width,height,sprite,red,green,blue,alpha));
      events.add("sprite");
   }
}
