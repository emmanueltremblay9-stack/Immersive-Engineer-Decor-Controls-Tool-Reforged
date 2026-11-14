"""Executes actual production keyPressed with recording widget doubles, not Minecraft."""
from pathlib import Path
import subprocess
import tempfile
import unittest
ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/LabeledCrateEditScreen.java'

class CrateKeyboardTest(unittest.TestCase):
    def test_actual_handler_routes_enter_by_focus(self):
        source = SOURCE.read_text()
        method = source[source.index('   public boolean keyPressed('):source.index('   public boolean isPauseScreen(')]
        java = '''
class Button { boolean cancel; Button(boolean cancel){this.cancel=cancel;} }
class Base {
 Object focus; int saved, cancelled;
 Object getFocused(){return focus;}
 boolean keyPressed(int key,int scan,int mods){
  if(focus instanceof Button b && (key==257 || key==335 || key==32)){
   if(b.cancel) cancelled++; else saved++; return true;
  } return false;
 }
}
public class Check extends Base {
 void saveAndClose(){saved++;}
 METHOD
 public static void main(String[] args){
  for(int key:new int[]{257,335}) {
   Check c=new Check(); c.focus=new Button(true);
   if(!c.keyPressed(key,0,0)||c.saved!=0||c.cancelled!=1)throw new AssertionError("Cancel saved on key "+key);
   c=new Check();c.focus=new Button(false);
   if(!c.keyPressed(key,0,0)||c.saved!=1||c.cancelled!=0)throw new AssertionError("Done failed");
   c=new Check();c.focus=new Object();
   if(!c.keyPressed(key,0,0)||c.saved!=1)throw new AssertionError("EditBox Enter lost");
  }
  Check c=new Check();c.focus=new Button(true);
  if(!c.keyPressed(32,0,0)||c.cancelled!=1||c.saved!=0)throw new AssertionError("Space lost");
 }
}
'''.replace('METHOD',method)
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp);(p/'Check.java').write_text(java)
            subprocess.run(['javac',str(p/'Check.java')],check=True,capture_output=True)
            result=subprocess.run(['java','-cp',tmp,'Check'],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
            for old, new in [('this.getFocused() instanceof Button', 'false'),
                             ('this.saveAndClose();', '')]:
                mutated = java.replace(old, new)
                self.assertNotEqual(mutated, java)
                (p/'Check.java').write_text(mutated)
                subprocess.run(['javac',str(p/'Check.java')],check=True,capture_output=True)
                result=subprocess.run(['java','-cp',tmp,'Check'],capture_output=True,text=True)
                self.assertNotEqual(result.returncode,0,'Mutation escaped: '+old)
if __name__=='__main__':unittest.main()
