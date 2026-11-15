"""Execute exact production toggle/helper with recording doubles, not native narration."""
from pathlib import Path
import re
import subprocess
import tempfile
import unittest
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/SmallMineralSmelterHybridScreen.java'
def probe(source):
    methods=[]
    for name in ('toggleOutputDrawer','drawerAccessibleComponent'):
        match=re.search(r'   private (?:void|Component) '+name+r'\(\) \{[^{}]*\}',source)
        assert match,name
        methods.append(match.group())
    fixture='''class DrawerProbe {
 static final String PREFIX="gui.smelter.";
 record Component(String key) {static Component translatable(String key){return new Component(key);}}
 static class Button {Component message;void setMessage(Component value){message=value;}}
 static class Menu {boolean open;int calls;void setSmelterOutputDrawerOpen(boolean value){open=value;calls++;}}
 boolean outputDrawerOpen; final Menu menu=new Menu(); final Button outputDrawerButton=new Button(); int updates;
 void updateModeButtonAvailability(){updates++;}
 METHODS
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  DrawerProbe p=new DrawerProbe();p.outputDrawerButton.setMessage(p.drawerAccessibleComponent());
  for(int i=1;i<=4;i++){
   p.toggleOutputDrawer(); boolean open=i%2==1;
   check(p.outputDrawerOpen==open && p.menu.open==open,"Drawer state changed unexpectedly");
   check(p.menu.calls==i && p.updates==i,"Existing side effects differ");
   check(p.outputDrawerButton.message.key().equals(PREFIX+(open?"drawer.close_accessible":"drawer.open_accessible")),"Stale accessible drawer message");
  }
 }
}'''.replace('METHODS','\n'.join(methods))
    with tempfile.TemporaryDirectory() as d:
        file=Path(d)/'DrawerProbe.java';file.write_text(fixture)
        return subprocess.run(['java',str(file)],capture_output=True,text=True)
class DrawerTests(unittest.TestCase):
    def test_actual_toggle_updates_accessible_message_and_preserves_effects(self):
        result=probe(SOURCE.read_text());self.assertEqual(result.returncode,0,result.stderr)
    def test_removed_update_mutant_fails(self):
        result=probe(SOURCE.read_text().replace('      outputDrawerButton.setMessage(drawerAccessibleComponent());\n',''))
        self.assertNotEqual(result.returncode,0);self.assertIn('Stale accessible drawer message',result.stderr)
if __name__=='__main__':unittest.main()
