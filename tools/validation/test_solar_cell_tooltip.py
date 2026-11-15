"""Regression of translated empty-cell hint; synthetic proof tests are not native evidence."""
import copy
import json
from pathlib import Path
import unittest
from client_mixin_smoke import SmokeFailure
from solar_tooltip_proof import validate_tooltip
from test_solar_tooltip_proof import SolarTooltipProofTest
ROOT=Path(__file__).resolve().parents[2]
KEY='gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.cell_tooltip'
class CellTooltipTest(SolarTooltipProofTest):
 def test_native_cell_proof_rejects_wrong_language_content(self):
  (self.game/'solar-cell-tooltip.png').write_bytes((self.game/'solar-fe-tooltip.png').read_bytes())
  p=copy.deepcopy(self.p)
  p['expected_text']='Emplacement pour objet à réserve d’énergie FE.'
  p['lines']=[p['expected_text']]
  self.assertTrue(validate_tooltip(p,self.game,'n','s','fr_fr',capture_name='solar-cell-tooltip.png',expected_text=p['expected_text']))
  for bad in ['Cell',KEY,'Energy item slot']:
   q=copy.deepcopy(p);q['expected_text']=bad;q['lines']=[bad]
   with self.assertRaises(SmokeFailure):validate_tooltip(q,self.game,'n','s','fr_fr',capture_name='solar-cell-tooltip.png',expected_text=p['expected_text'])
 def test_languages_and_empty_only_render(self):
  langs={l:json.loads((ROOT/f'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/{l}.json').read_text())[KEY] for l in ['en_us','fr_fr','fr_ca']}
  self.assertEqual(langs['fr_fr'],langs['fr_ca']);self.assertNotEqual(langs['en_us'],langs['fr_fr'])
  self.assertTrue(all('FE' in s and len(s)>30 for s in langs.values()))
  s=(ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/SolarV2ReferenceScreen.java').read_text()
  self.assertIn('hoveredSlot.index == 0 && !hoveredSlot.hasItem()',s)
  self.assertIn('Component.translatable(PREFIX + "cell_tooltip")',s)
  self.assertIn('font.split(cellTooltip, wrapWidth)',s)
  self.assertNotIn('Component.literal(menu.layout()',s)
  self.assertIn('renderTooltip(graphics, mouseX, mouseY);',s)
 def test_probe_is_real_hover_with_empty_item_guard(self):
  s=(ROOT/'src/solarPackagedValidation/java/com/oblixorprime/iedctrtest/solar/NativeSolarV2ReferenceValidation.java').read_text()
  for term in ['case TOOLTIP, CELL_TOOLTIP','menu.getSlot(0).getItem().isEmpty() && menu.getCarried().isEmpty()', 'hovered.index!=0 || hovered.hasItem()', 'XTestFakeMotionEvent', 'tooltipFrames<3', 'Screenshot.takeScreenshot', '"solar-cell-tooltip.json"']:
   self.assertIn(term,s)

class CellWrapExecutionTest(unittest.TestCase):
 def test_exact_body_and_mutations(self):
  import subprocess,tempfile
  s=(ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/SolarV2ReferenceScreen.java').read_text()
  body=s[s.index('         Component cellTooltip ='):s.index('\n      }',s.index('         Component cellTooltip ='))]
  from test_solar_tooltip_wrap import FIXTURE
  fixture=FIXTURE.replace('static Component translatable(String key,int a,int b,int c)', 'static Component translatable(String key,int a,int b,int c)').replace('record Component(String key,int a,int b,int c){','record Component(String key,int a,int b,int c){static Component translatable(String key){return new Component(key,0,0,0);}')
  fixture=fixture.replace('d.text.a()!=32000||d.text.c()!=100','!d.text.key().equals("solar.cell_tooltip")')
  def run(code):
   with tempfile.TemporaryDirectory() as d:
    p=Path(d)/'Probe.java';p.write_text(fixture.replace('BODY',code))
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main',str(p)],check=True,capture_output=True)
    return subprocess.run(['java','-cp',d,'Probe'],capture_output=True).returncode
  self.assertEqual(0,run(body))
  for code in [body.replace('font.split(cellTooltip, wrapWidth)','cellTooltip'),body.replace('width - 16','width + 16'),body.replace('"cell_tooltip"','"energy_tooltip"')]:
   self.assertNotEqual(0,run(code))

class AdditiveHistoryTest(unittest.TestCase):
 def test_exact_projection_and_mutations(self):
  import hashlib
  from solar_cell_integration import FILES, project_history, current_hash
  for path,row in FILES.items():
   data=(ROOT/path).read_bytes()
   self.assertEqual(row['baseline_sha256'],hashlib.sha256(project_history(path,data)).hexdigest())
   self.assertEqual(row['current_sha256'],current_hash(path,row['baseline_sha256']))
   for changed in [data+b' ',data.replace(b'FE',b'RF',1),data.replace(b'cell_tooltip',b'cell_tooltip_bad')]:
    with self.assertRaises(AssertionError):project_history(path,changed)
   with self.assertRaises(AssertionError):current_hash(path,'0'*64)
