"""Exact reviewed Breaker additions; no regex or broad preservation exclusions."""

BREAKER_INTEGRATION = {'main_commit': 'b9fc2b44d41eab7cb0513775b27ac7c231aa6e20',
 'source_patch_sha256': '9dde1f13b615df5ae92ee3b2229bab48e5084205c68c0d7f2a0fbcab9bb85580',
 'scope': 'Separate Block Breaker lubricant layer over main. Includes the reviewed long-arithmetic '
          'correction; historical Solar, Tree and Freezer records remain unchanged. Exact source '
          'identities, not gameplay qualification.',
 'files': {'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java': {'main_sha256': '9a6678406008a817dd544df65f259eade10cc91c37a52bdca84f6ada18e88c63',
                                                                                                     'integrated_sha256': '1ab112cfb93c2817c68edae262aed72d9fc91008851cc433a9dc3693f11beb29'},
           'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java': {'main_sha256': '903cc15bbce612d9b5c7f150426fdce52858d63769eca9f7cdc16d6b43fb1ae2',
                                                                                           'integrated_sha256': '9b78022405e584f41da11e03999cad0b14d5563a9c2636d55441cee874488b14'},
           'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java': {'main_sha256': 'fdf3aba428feef82fea57881f1e4466963bd8f3dbfa3af8513f9443e8e314c10',
                                                                                               'integrated_sha256': 'cb52445a0f4373a7fabf51d12a608c5e8620890c3aceb027895abd0df9c31884'}}}
BREAKER_FILES = BREAKER_INTEGRATION['files']
BREAKER_MENU_ADDITIONS = ('         case SMALL_BLOCK_BREAKER -> MachineBlockEntity.BLOCK_BREAKER_LUBRICANT_CAPACITY;\n',
 '      if (this.kind == MachineKind.SMALL_BLOCK_BREAKER) {\n'
 '         return (this.data.get(4) & BLOCK_BREAKER_FLUID_PRESENT) != 0 && this.data.get(2) > 0\n'
 '            ? BuiltInRegistries.FLUID.get(BLOCK_BREAKER_LUBRICANT_ID) : Fluids.EMPTY;\n'
 '      }\n',
 '   public boolean blockBreakerAccelerated() {\n'
 '      return this.kind == MachineKind.SMALL_BLOCK_BREAKER && (this.data.get(4) & '
 'BLOCK_BREAKER_ACCELERATED) != 0;\n'
 '   }\n'
 '\n'
 '   public boolean blockBreakerCycleActive() {\n'
 '      return this.kind == MachineKind.SMALL_BLOCK_BREAKER && (this.data.get(4) & '
 'BLOCK_BREAKER_CYCLE_ACTIVE) != 0;\n'
 '   }\n'
 '\n'
 '   public FluidStack blockBreakerFluidStack() {\n'
 '      Fluid fluid = this.storedFluid();\n'
 '      return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, '
 'this.fluidAmount());\n'
 '   }\n'
 '\n'
 '   public Component blockBreakerFluidName() {\n'
 '      FluidStack stack = this.blockBreakerFluidStack();\n'
 '      return stack.isEmpty()\n'
 '         ? '
 'Component.translatable("gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.fluid_name")\n'
 '         : stack.getHoverName();\n'
 '   }\n'
 '\n'
 '   public int blockBreakerDurationTicks() {\n'
 '      if (this.kind != MachineKind.SMALL_BLOCK_BREAKER) return 0;\n'
 '      int synced = this.data.get(3);\n'
 '      if (synced > 0) return synced;\n'
 '      float hardness = this.blockBreakerHardness();\n'
 '      if (!Float.isFinite(hardness) || hardness < 0.0F || '
 'this.blockBreakerTargetStack().isEmpty()) return 0;\n'
 '      long normalTicks = 20L + Math.round(hardness * 20.0F);\n'
 '      int normal = (int)Math.max(20L, Math.min(200L, normalTicks));\n'
 '      return this.fluidAmount() >= MachineBlockEntity.BLOCK_BREAKER_LUBRICANT_PER_SUCCESS\n'
 '         ? Math.max(20, (normal + 1) / 2) : normal;\n'
 '   }\n'
 '\n')
def normalize_breaker_menu_tail(tail):
    for addition in BREAKER_MENU_ADDITIONS:
        assert tail.count(addition) == 1, 'Missing, changed or duplicate Breaker menu addition'
        tail = tail.replace(addition, '', 1)
    return tail
