"""Execute the production selection expressions with doubles, not Minecraft gameplay."""
from pathlib import Path
import re
import subprocess
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

    def test_production_selection_matrix(self):
        source = MENU.read_text()
        factory = re.search(r'public static MachineMenu client\(.*?\n   }', source, re.S)[0]
        args = re.search(r'\),\s*(kind == MachineKind.FLUID_BARREL.*?);', factory, re.S)[1].rstrip(')')
        assignments = '\n'.join(re.findall(r'this\.(?:barrelReferencePresentation|factoryPlacerV2ReferencePresentation) = .*?;', source, re.S))
        self.assertEqual(2, assignments.count('this.'))
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'SelectionProbe.java'
            path.write_text('''
public class SelectionProbe {
 enum MachineKind { FLUID_BARREL, FACTORY_PLACER, SMALL_FREEZER }
 static final String DEV_BARREL_REFERENCE_PROPERTY = "barrel.test";
 static boolean client;
 static class Level { boolean isClientSide = client; }
 static class Player { Level level() { return new Level(); } }
 static class Inventory { Player player = new Player(); }
 boolean barrelReferencePresentation, factoryPlacerV2ReferencePresentation;
 SelectionProbe(MachineKind kind, Inventory inventory, boolean barrelReferenceRequested,
                boolean factoryPlacerV2ReferenceRequested) {
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
       for (MachineKind kind : MachineKind.values()) {
         var actual = factory(kind);
         boolean barrel = side && kind == MachineKind.FLUID_BARREL && !property.equals("false");
         boolean placer = side && kind == MachineKind.FACTORY_PLACER;
         if(actual.barrelReferencePresentation != barrel || actual.factoryPlacerV2ReferencePresentation != placer)
           throw new AssertionError(kind+" "+property+" "+side);
         var serverConstructor = new SelectionProbe(kind,new Inventory(),false,false);
         if(serverConstructor.barrelReferencePresentation || serverConstructor.factoryPlacerV2ReferencePresentation)
           throw new AssertionError("Public constructor leaked presentation");
       }
     }
   }
 }
}
''')
            subprocess.run(['java', str(path)], check=True, capture_output=True, text=True)
        self.assertIn('this(kind, id, inventory, container, data, false, false);', source)
        self.assertEqual(3, source.count('else if (factoryPlacerV2ReferencePresentation)'))

if __name__ == '__main__':
    unittest.main()
