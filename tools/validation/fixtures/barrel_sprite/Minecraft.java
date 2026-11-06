package net.minecraft.client;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
public final class Minecraft {
   private static final Minecraft INSTANCE = new Minecraft();
   public final Map<ResourceLocation, TextureAtlasSprite> sprites = new HashMap<>();
   public int lookups;
   public static Minecraft getInstance() { return INSTANCE; }
   public Function<ResourceLocation, TextureAtlasSprite> getTextureAtlas(ResourceLocation atlas) {
      if (!InventoryMenu.BLOCK_ATLAS.equals(atlas)) throw new AssertionError("Wrong atlas: " + atlas);
      return id -> { lookups++; return sprites.get(id); };
   }
}
