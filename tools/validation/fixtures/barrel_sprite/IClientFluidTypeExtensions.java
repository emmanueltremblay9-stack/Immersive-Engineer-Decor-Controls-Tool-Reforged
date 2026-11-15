package net.neoforged.neoforge.client.extensions.common;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
public final class IClientFluidTypeExtensions {
   public static final Map<Fluid, IClientFluidTypeExtensions> VALUES = new HashMap<>();
   private final ResourceLocation texture;
   private final int tint;
   public FluidStack receivedTextureStack, receivedTintStack;
   public IClientFluidTypeExtensions(ResourceLocation texture, int tint) { this.texture=texture; this.tint=tint; }
   public static IClientFluidTypeExtensions of(Fluid fluid) { return VALUES.get(fluid); }
   public ResourceLocation getStillTexture(FluidStack stack) { receivedTextureStack=stack; return texture; }
   public int getTintColor(FluidStack stack) { receivedTintStack=stack; return tint; }
}
