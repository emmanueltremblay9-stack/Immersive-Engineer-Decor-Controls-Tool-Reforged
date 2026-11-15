package net.minecraft.client.renderer.texture;
import net.minecraft.resources.ResourceLocation;
public record TextureAtlasSprite(ResourceLocation name, float u0, float u1, float v0, float v1) {
   public TextureAtlasSprite contents() { return this; }
   public float getU0() { return u0; }
   public float getU1() { return u1; }
   public float getV0() { return v0; }
   public float getV1() { return v1; }
}
