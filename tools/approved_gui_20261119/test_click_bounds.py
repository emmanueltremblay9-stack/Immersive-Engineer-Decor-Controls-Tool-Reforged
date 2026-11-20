"""Compile and execute actual extracted input method with minimal menu/base doubles.
This proves branching/arithmetic, not a real mouse packet or Minecraft client.
"""
import subprocess,tempfile,unittest
from pathlib import Path
import build_assets as b
class ClickBounds(unittest.TestCase):
 def test_actual_method_boundaries_and_other_machine_delegation(self):
  source=(b.ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java').read_text()
  start=source.index('   @Override\n   protected boolean hasClickedOutside(');end=source.index('   private void drawFactoryHopperWidgets(',start)
  method=source[start:end]
  code='''class Base { protected boolean hasClickedOutside(double x,double y,int l,int t,int button){ return x<l || x>=l+228 || y<t || y>=t+250; } }
class MachineKind {static final int FACTORY_HOPPER=1;}
class MachineMenu { int kind; int kind(){return kind;} }
class Bounds extends Base {MachineMenu menu=new MachineMenu();
'''+method+'''
 public static void main(String[] args){Bounds s=new Bounds();int checks=0;
 for(int kind:new int[]{0,1,2})for(int left:new int[]{0,72,-5})for(int top:new int[]{0,47,-7})for(int button:new int[]{0,1,2}){
 s.menu.kind=kind;
 for(double dx:new double[]{-9,-8.001,-8,-7.999,-1,0,8,175.999,176,183.999,184,184.001,227.999,228})
 for(double dy:new double[]{-17,-16.001,-16,-15.999,-1,0,8,165.999,166,166.001,249.999,250}){
 double x=left+dx,y=top+dy;
 boolean expected=kind==1 ? x<left-8 || x>=left+184 || y<top-16 || y>=top+166 : x<left || x>=left+228 || y<top || y>=top+250;
 if(s.hasClickedOutside(x,y,left,top,button)!=expected)throw new AssertionError(kind+":"+dx+":"+dy);
 checks++;
 }}System.out.println("PASS "+checks+" extracted-method boundary cases; not native input");}}
'''
  with tempfile.TemporaryDirectory() as td:
   p=Path(td)/'Bounds.java';p.write_text(code)
   compiled=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',td,str(p)],capture_output=True,text=True)
   self.assertEqual(compiled.returncode,0,compiled.stdout+compiled.stderr)
   result=subprocess.run(['java','-cp',td,'Bounds'],capture_output=True,text=True)
   self.assertEqual(result.returncode,0,result.stdout+result.stderr);print(result.stdout.strip())
if __name__=='__main__':unittest.main()
