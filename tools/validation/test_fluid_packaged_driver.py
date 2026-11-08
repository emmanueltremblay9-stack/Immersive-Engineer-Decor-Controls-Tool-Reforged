"""Static isolation guards; these are not a mapped Java or native execution claim."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'src/fluidPackagedValidation/java/com/oblixorprime/iedctrtest/fluid'

class PackagedDriverGuards(unittest.TestCase):
    def test_driver_has_no_direct_menu_or_screen_shortcut(self):
        java=(BASE/'NativeFluidGestureValidation.java').read_text()
        for name in ('.openMenu(','.handleInventoryMouseClick(','.clicked(','.quickMoveStack(','.setScreen(','.mouseClicked(','.mouseReleased('):
            self.assertNotIn(name,java)
        for name in ('XTestFakeMotionEvent','XTestFakeButtonEvent','XTestFakeKeyEvent','GLFW_MOUSE_BUTTON_RIGHT',
                     'hit.getBlockPos().equals(POS)','native_right_press_observed','native_right_release_observed',
                     'XTEST_RMB_BLOCK_HIT','mc.player.getMainHandItem().isEmpty()',
                     'native-attempt-and-state-conservation-only'):
            self.assertIn(name,java)
    def test_tank_setup_and_toast_guards_are_explicit(self):
        java=(BASE/'NativeFluidGestureValidation.java').read_text()
        for name in ('tankCapacity==(machineIndex==0?3000:4000)','row.addProperty("setup_seed",true)',
                     'case TANK_RESET','recipeToastVisible(mc)','RecipeToast.class','Toast.NO_TOKEN',
                     'o.addProperty("recipe_toast_absent",!recipeToastVisible(mc))'):
            self.assertIn(name,java)
        self.assertNotIn('getToasts().clear',java)
    def test_distinct_mod_and_jar_only_source_set(self):
        java=(BASE/'NativeFluidGestureValidation.java').read_text()
        self.assertIn('package com.oblixorprime.iedctrtest.fluid;',java)
        self.assertIn('modid = FluidValidationMod.MOD_ID',java)
        gradle=(ROOT/'gradle/fluid-packaged-validation.gradle').read_text()
        self.assertIn('sourceSets.main.runtimeClasspath - sourceSets.main.output',gradle)
        self.assertNotIn('compileClasspath += sourceSets.main.output',gradle)
        for name in ('Supplied runtime JAR differs','Harness and production JAR contain a split package',
                     "loadedMods = [neoForge.mods.getByName('iedctr_fluid_validation')]",
                     'Legacy source-runtime fluid fixture cannot accompany packaged validation'):
            self.assertIn(name,gradle)
    def test_runtime_checks_helpers_origins_and_flags(self):
        java=(BASE/'PackagedRuntimeProof.java').read_text()
        for name in ('key.startsWith("iedctr.dev.")','Class.forName("com.oblixorprime.engineersdecorreforged.client.RetainedFluidMachineArtwork")',
                     'type.getModule() != FluidValidationMod.class.getModule()',
                     'url.toURI().equals(mod.findResource(entry).toUri())','active.sourcePackId().equals(',
                     'resourceHash.equals(hash(archive.readAllBytes()))'):
            self.assertIn(name,java)

if __name__=='__main__':unittest.main()
