"""Execute exact production FE tooltip body using a recording Font; not native qualification."""
from pathlib import Path
import subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/SolarV2ReferenceScreen.java'
FIXTURE='''import java.util.*;
public class Probe {
 static final String PREFIX="solar.";
 record Component(String key,int a,int b,int c){static Component translatable(String key,int a,int b,int c){return new Component(key,a,b,c);}}
 static class Font{int size;Component original;List<Component> split(Component text,int size){this.size=size;original=text;return List.of(text);}}
 static class Menu{int energyStored(){return 32000;}int energyCapacity(){return 32000;}int energyDisplayGranularity(){return 100;}}
 static class Draw{int calls,x,y;Component text;void renderTooltip(Font f,List<Component> lines,int x,int y){calls++;text=lines.get(0);this.x=x;this.y=y;}void renderTooltip(Font f,Component text,int x,int y){calls++;this.text=text;}}
 Font font=new Font();Menu menu=new Menu();int width;
 void render(Draw graphics,int mouseX,int mouseY){BODY}
 public static void main(String[]args){for(int w:new int[]{1,24,100,240,320,1280}){Probe p=new Probe();p.width=w;Draw d=new Draw();p.render(d,160,125);
 if(p.font.size!=Math.max(1,Math.min(240,w-16))||p.font.original!=d.text||d.calls!=1||d.x!=160||d.y!=125||d.text.a()!=32000||d.text.c()!=100)throw new AssertionError("wrap/content/pointer changed");}}
}'''

def body():
 s=SOURCE.read_text();a=s.index('         Component energyTooltip =');b=s.index('\n      } else if',a);return s[a:b]
def run(code):
 with tempfile.TemporaryDirectory() as tmp:
  p=Path(tmp)/'Probe.java';p.write_text(FIXTURE.replace('BODY',code))
  subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main',str(p)],check=True,capture_output=True)
  return subprocess.run(['java','-cp',tmp,'Probe'],capture_output=True).returncode
class SolarWrapTest(unittest.TestCase):
 def test_body(self):self.assertEqual(0,run(body()))
 def test_mutations(self):
  for code in [body().replace('font.split(energyTooltip, wrapWidth)','energyTooltip'),body().replace('width - 16','width + 16'),body().replace('Math.min(240, width - 16)','Math.max(240, width - 16)')]:self.assertNotEqual(0,run(code))
