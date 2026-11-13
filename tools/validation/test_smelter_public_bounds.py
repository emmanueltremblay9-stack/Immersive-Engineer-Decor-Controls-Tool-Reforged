"""Execute the actual public projection methods; not a native Minecraft/JEI test."""
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/SmallMineralSmelterHybridScreen.java'


def public_methods(source):
    methods = []
    for name in ('getGuiLeft', 'getGuiTop', 'getXSize', 'getYSize'):
        matches = re.findall(r'public int ' + name + r'\(\)\s*\{[^{}]*\}', source)
        if len(matches) != 1:
            raise AssertionError('Missing/ambiguous public bounds: ' + name)
        methods.append(matches[0])
    return '\n'.join(methods)


class SmelterPublicBoundsTest(unittest.TestCase):
    def probe(self, methods):
        source = SOURCE.read_text()
        projection = source.split('      viewScale = Math.min(', 1)[1].split('\n\n', 1)[0]
        projection = 'viewScale = Math.min(' + projection
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'BoundsProbe.java'
            path.write_text('''class BoundsProbe {
 int imageWidth=234, imageHeight=270, leftPos=0, topPos=0;
 float originX, originY, viewScale;
 ''' + methods + '''
 void init(int width,int height) { ''' + projection + ''' }
 void check() {
   int l=getGuiLeft(),t=getGuiTop(),w=getXSize(),h=getYSize();
   float right=originX+imageWidth*viewScale,bottom=originY+imageHeight*viewScale;
   if(l>originX || originX-l>=1 || t>originY || originY-t>=1 ||
      l+w<right || l+w-right>=1 || t+h<bottom || t+h-bottom>=1 || w<=0 || h<=0)
      throw new AssertionError("Projected enclosure differs");
   if(leftPos!=0 || topPos!=0 || imageWidth!=234 || imageHeight!=270)
      throw new AssertionError("Logical slot coordinate system changed");
 }
 public static void main(String[] args) {
   BoundsProbe p=new BoundsProbe();
   int[][] sizes={{1280,720},{1920,1080},{960,540},{640,360},{427,240},
      {1280,260},{321,181},{235,271},{233,269},{30,30},{10,10},{1281,721}};
   for(int[] size:sizes) {p.init(size[0],size[1]);p.check();}
   // Explicit fractional and negative origins catch truncation and ceil(width)-only errors.
   for(float s:new float[]{0.1f,0.33333334f,0.725f,1f})
      for(float x:new float[]{-3.75f,-0.25f,0f,0.25f,11.75f}) {
         p.originX=x;p.originY=x+0.125f;p.viewScale=s;p.check();
      }
 }
}''')
            return subprocess.run(['java', str(path)], capture_output=True, text=True, timeout=30)

    def test_production_methods_enclose_scaled_canvas(self):
        result = self.probe(public_methods(SOURCE.read_text()))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_old_inherited_logical_bounds_fail(self):
        result = self.probe('''public int getGuiLeft(){return leftPos;}
 public int getGuiTop(){return topPos;}
 public int getXSize(){return imageWidth;}
 public int getYSize(){return imageHeight;}''')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Projected enclosure differs', result.stderr)

    def test_removing_overrides_is_rejected(self):
        source = SOURCE.read_text()
        for name in ('getGuiLeft', 'getGuiTop', 'getXSize', 'getYSize'):
            with self.subTest(name=name), self.assertRaises(AssertionError):
                public_methods(re.sub(r'public int ' + name + r'\(\)\s*\{[^{}]*\}', '', source))

    def test_inward_rounding_is_rejected(self):
        result = self.probe(public_methods(SOURCE.read_text()).replace('Math.floor', 'Math.ceil'))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Projected enclosure differs', result.stderr)


if __name__ == '__main__':
    unittest.main()
