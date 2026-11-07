package net.neoforged.neoforge.fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
public record FluidStack(Fluid fluid, int amount) {
   public boolean isEmpty() { return fluid == Fluids.EMPTY || amount <= 0; }
   public Fluid getFluid() { return fluid; }
   public int getAmount() { return amount; }
}
