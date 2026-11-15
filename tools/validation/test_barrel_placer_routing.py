"""Execute the production selection expressions with doubles, not Minecraft gameplay."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
MENU = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java'

class RoutingTests(unittest.TestCase):
    def test_reload_qualification_uses_default_without_selector(self):
        runner = (ROOT / 'tools/validation/barrel_reference_native.py').read_text()
        native = (ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeBarrelReferenceValidation.java').read_text()
        self.assertNotIn("systemProperty 'iedctr.dev.barrelV5Reference'", runner)
        self.assertIn('System.getProperty(MachineMenu.DEV_BARREL_REFERENCE_PROPERTY) == null', native)

    def test_integrated_wiring_guards(self):
        for name in ('check_barrel_reference_wiring.py', 'check_solar_v2_reference_wiring.py'):
            result = subprocess.run([sys.executable, str(ROOT / 'tools/validation' / name)], capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_production_selection_matrix(self):
        source = MENU.read_text()
        factory = re.search(r'public static MachineMenu client\(.*?\n   }', source, re.S)[0]
        args = re.search(r'\),\s*(kind == MachineKind.SMALL_FREEZER.*?);', factory, re.S)[1][:-1]
        assignments = '\n'.join(re.findall(r'this\.(?:freezerReferencePresentation|barrelReferencePresentation|factoryPlacerV2ReferencePresentation|solarV2ReferencePresentation|treeCutterV3ReferencePresentation) = .*?;', source, re.S))
        self.assertEqual(5, assignments.count('this.'))
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'SelectionProbe.java'
            path.write_text('''
public class SelectionProbe {
 enum MachineKind { FLUID_BARREL, FACTORY_PLACER, SMALL_FREEZER, SMALL_SOLAR_PANEL, SMALL_TREE_CUTTER }
 static final String DEV_FREEZER_REFERENCE_PROPERTY = "freezer.test";
 static final String DEV_FREEZER_LIVE_ENERGY_PROPERTY = "freezer.energy.test";
 static final String DEV_BARREL_REFERENCE_PROPERTY = "barrel.test";
 static final String DEV_SOLAR_V2_REFERENCE_PROPERTY = "solar.test";
 static boolean client;
 static class Level { boolean isClientSide = client; }
 static class Player { Level level() { return new Level(); } }
 static class Inventory { Player player = new Player(); }
 boolean freezerReferencePresentation, barrelReferencePresentation, factoryPlacerV2ReferencePresentation, solarV2ReferencePresentation, treeCutterV3ReferencePresentation;
 SelectionProbe(MachineKind kind, Inventory inventory, boolean freezerReferenceRequested, boolean barrelReferenceRequested,
                boolean factoryPlacerV2ReferenceRequested, boolean solarV2ReferenceRequested) {
 ''' + assignments + '''
 }
 static SelectionProbe factory(MachineKind kind) {
   return new SelectionProbe(kind, new Inventory(), ''' + args + ''');
 }
 public static void main(String[] ignored) {
   for (boolean side : new boolean[]{false,true}) {
     client = side;
     for (String property : new String[]{"absent", "false", "true"}) {
       if (property.equals("absent")) System.clearProperty(DEV_BARREL_REFERENCE_PROPERTY);
       else System.setProperty(DEV_BARREL_REFERENCE_PROPERTY, property);
       for (String solar : new String[]{"absent", "false", "true"}) {
       if (solar.equals("absent")) System.clearProperty(DEV_SOLAR_V2_REFERENCE_PROPERTY);
       else System.setProperty(DEV_SOLAR_V2_REFERENCE_PROPERTY, solar);
       for (String freezer : new String[]{"absent", "false", "true"}) {
       if (freezer.equals("absent")) System.clearProperty(DEV_FREEZER_REFERENCE_PROPERTY);
       else System.setProperty(DEV_FREEZER_REFERENCE_PROPERTY, freezer);
       for (String energy : new String[]{"absent", "false", "true"}) {
       if (energy.equals("absent")) System.clearProperty(DEV_FREEZER_LIVE_ENERGY_PROPERTY);
       else System.setProperty(DEV_FREEZER_LIVE_ENERGY_PROPERTY, energy);
       for (MachineKind kind : MachineKind.values()) {
         var actual = factory(kind);
         boolean frozen = side && kind == MachineKind.SMALL_FREEZER && (freezer.equals("true") || !energy.equals("false"));
         if(actual.freezerReferencePresentation != frozen) throw new AssertionError("Freezer selection");
         boolean barrel = side && kind == MachineKind.FLUID_BARREL && !property.equals("false");
         boolean placer = side && kind == MachineKind.FACTORY_PLACER;
         boolean tree = side && kind == MachineKind.SMALL_TREE_CUTTER;
         if(actual.barrelReferencePresentation != barrel || actual.factoryPlacerV2ReferencePresentation != placer || actual.solarV2ReferencePresentation != (side && kind == MachineKind.SMALL_SOLAR_PANEL && !solar.equals("false")))
           throw new AssertionError(kind+" "+property+" "+side);
         if(actual.treeCutterV3ReferencePresentation != tree)
           throw new AssertionError("Tree presentation routing: " + kind + " " + side);
         var serverConstructor = new SelectionProbe(kind,new Inventory(),false,false,false,false);
         if(serverConstructor.freezerReferencePresentation || serverConstructor.barrelReferencePresentation || serverConstructor.factoryPlacerV2ReferencePresentation || serverConstructor.solarV2ReferencePresentation)
           throw new AssertionError("Public constructor leaked presentation");
         // Tree is always selected on a Tree client; all server presentations remain false.
         if(serverConstructor.treeCutterV3ReferencePresentation != tree)
           throw new AssertionError("Tree constructor isolation: " + kind + " " + side);
       }
       }
       }
     }
     }
   }
 }
}
''')
            subprocess.run(['java', str(path)], check=True, capture_output=True, text=True)
        self.assertIn('this(kind, id, inventory, container, data, false, false, false, false);', source)
        self.assertEqual(3, source.count('else if (factoryPlacerV2ReferencePresentation)'))

    def test_integrated_projection_blocks_keep_each_geometry(self):
        source = MENU.read_text()
        for flag, geometry, offset in (
            ('barrelReferencePresentation', 'BarrelReferenceGeometry', 2),
            ('factoryPlacerV2ReferencePresentation', 'FactoryPlacerV2ReferenceGeometry', 18),
            ('solarV2ReferencePresentation', 'SolarV2ReferenceGeometry', 1),
            ('treeCutterV3ReferencePresentation', 'TreeCutterV3ReferenceGeometry', 1),
        ):
            blocks = re.findall(r'if \(' + flag + r'\) \{(.*?)\n\s*}', source, re.S)
            self.assertEqual(3, len(blocks), flag)
            indexes = ('slot.containerIndex()', f'{offset} + row * 9 + column', f'{offset + 27} + column')
            for index, block in enumerate(blocks):
                assignment = ('presentationSlot = new MachineLayout.MachineSlot(slot.containerIndex(), anchor.x(), anchor.y(), slot.role(), slot.output());'
                              if index == 0 else 'x = anchor.x(); y = anchor.y();')
                expected = f'var anchor = {geometry}.anchor({indexes[index]}); {assignment}'
                self.assertEqual(expected, ' '.join(block.split()), flag)

if __name__ == '__main__':
    unittest.main()
