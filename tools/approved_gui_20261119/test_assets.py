import hashlib,subprocess,unittest,tempfile
from pathlib import Path
from factory_placer_v2_reference_native import decode_png
import build_assets as b
import factory_owner_20261119 as gate
class ApprovedAssets(unittest.TestCase):
 def test_exact_source_attestations_and_private_separation(self):
  data=gate.recipe()
  self.assertEqual(data['source_attestations'],{spec[0]:spec[1] for spec in b.SPECS.values()})
  self.assertFalse((b.ROOT/'tools/approved_gui_20261119/sources').exists())
  self.assertEqual(data['private_reproduction_artifact_sha256'],'e910eda70ae7c5fd2ac74fdf90e310b03ae47b1a1a725e9cbf3186ba464ff64e')
 def test_all_item_footprints_are_empty_wells(self):
  for kind,(_,_,size,_,_) in b.SPECS.items():
   width,height,pixels=decode_png(b.OUT/(kind+'.png'));self.assertEqual((width,height),(size[0]*6,size[1]*6))
   expected=54 if kind=='hopper' else 51
   self.assertEqual(len(b.slots(kind)),expected)
   for x,y,_ in b.slots(kind):
    if kind=='hopper':x+=8;y+=16
    for row in range(y*6,(y+16)*6):
     for col in range(x*6,(x+16)*6):
      pos=(row*width+col)*4
      self.assertLess(max(pixels[pos:pos+3]),70,(kind,x,y))
      self.assertEqual(pixels[pos+3],255)
 def test_visible_well_vignette_not_flat_centre(self):
  for kind in b.SPECS:
   width,height,pixels=decode_png(b.OUT/(kind+'.png'))
   for x,y,_ in b.slots(kind):
    if kind=='hopper':x+=8;y+=16
    values=[max(pixels[(row*width+col)*4:(row*width+col)*4+3]) for row in range(y*6,(y+16)*6) for col in range(x*6,(x+16)*6)]
    self.assertGreaterEqual(max(values)-min(values),10,(kind,x,y))

 def test_native_size_well_contrast_survives_sampling(self):
  for kind in b.SPECS:
   w,h,p=decode_png(b.OUT/(kind+'.png'))
   for x,y,_ in b.slots(kind):
    if kind=='hopper':x+=8;y+=16
    values=[max(p[((y*6+row*6+3)*w+x*6+col*6+3)*4:((y*6+row*6+3)*w+x*6+col*6+3)*4+3]) for row in range(16) for col in range(16)]
    self.assertGreaterEqual(max(values)-min(values),15)
    self.assertGreater(sum(values)/len(values),18)

 def test_v4_pixels_changed_only_in_wells_and_hopper_ribbon(self):
  gate.ensure_base_object(anchor=gate.PREVIOUS_COMMIT)
  for kind in b.SPECS:
   path=b.OUT/(kind+'.png')
   previous=subprocess.check_output(['git','show',gate.PREVIOUS_COMMIT+':'+str(path.relative_to(b.ROOT))],cwd=b.ROOT)
   with tempfile.TemporaryDirectory() as td:
    old=Path(td)/'old.png';old.write_bytes(previous);ow,oh,op=decode_png(old)
   w,h,p=decode_png(path);self.assertEqual((w,h),(ow,oh))
   ox,oy=(8,16) if kind=='hopper' else (0,0)
   boxes=[((x+ox)*6,(y+oy)*6,(x+ox+16)*6,(y+oy+16)*6) for x,y,_ in b.slots(kind)]
   if kind=='hopper':boxes.append((14*6,78*6,178*6,86*6))
   for row in range(h):
    spans=sorted((x,z) for x,y,z,t in boxes if y<=row<t);end=0
    for x,z in spans:
     if x>end:self.assertEqual(p[(row*w+end)*4:(row*w+x)*4],op[(row*w+end)*4:(row*w+x)*4])
     end=max(end,z)
    self.assertEqual(p[(row*w+end)*4:(row*w+w)*4],op[(row*w+end)*4:(row*w+w)*4])

 def test_only_registered_rims_and_selection_order(self):
  screen=(b.ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java').read_text()
  hopper=(b.ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryHopperSteelArtwork.java').read_text()
  self.assertNotIn('FactorySteelFrame.slots(g,menu,x,y)',hopper)
  self.assertNotIn('FactorySteelFrame.slots(graphics,m,x,y)',screen)
  start=screen.index('   private void drawRetainedDropper(')
  end=screen.index('   private void drawFactoryDropperWidgets(',start)
  section=screen[start:end]
  self.assertLess(section.index('graphics.blit(steel'),section.index('this.drawHoverOutline('))

 def test_hopper_stripe_matches_privately_proven_source_projection(self):
  w,h,pixels=decode_png(b.OUT/'hopper.png')
  crop=b''.join(bytes(pixels[((473+y)*w+140)*4:((473+y)*w+300)*4]) for y in range(38))
  self.assertEqual(hashlib.sha256(crop).hexdigest(),gate.recipe()['stripe_projection_sha256'])
  # This is an exact attested output-region check, not a public source rebuild.
 def test_hopper_crossmember_and_round_bolts_do_not_overlap_slots_or_controls(self):
  decorations=[(0,75,14,14),(178,75,14,14),(18,78,159,8)]
  protected=[(x+7,y+15,18,18) for x,y,_ in b.slots('hopper')]
  protected += [(x+8,y+16,w,h) for x,y,w,h in [(131,13,38,7),(130,26,39,7),(132,39,37,7),(132,48,11,11),(144,48,11,11),(158,48,11,11)]]
  for x,y,w,h in decorations:
   for a,c,ww,hh in protected:
    self.assertFalse(max(x,a)<min(x+w,a+ww) and max(y,c)<min(y+h,c+hh),(x,y,w,h,a,c,ww,hh))

 def test_layout_and_actions_unchanged(self):
  for file in ['utility/MachineLayout.java','utility/RetainedDropperLayout.java','client/MachineScreen.java','menu/MachineMenu.java']:
   p='src/main/java/com/oblixorprime/engineersdecorreforged/'+file
   current=(b.ROOT/p).read_bytes()
   if file=='client/MachineScreen.java':
    start=current.index(b'   @Override\n   protected boolean hasClickedOutside(');end=current.index(b'   private void drawFactoryHopperWidgets(',start)
    current=current[:start]+current[end:]
    old=b'      FactorySteelFrame.slots(graphics,m,x,y);'
    new=b'      // Slot borders are registered once in the owner-source background.'
    self.assertEqual(current.count(new),1)
    current=current.replace(new,old)
    current=current.replace(b'FactorySteelFrame.controlSymbol(graphics,x+sx+5,y+90,n,state[n]);',b'FactorySteelFrame.controlSymbol(graphics,x+sx+5,y+90,n);')
   self.assertEqual(current,(gate.ensure_baseline()/p).read_bytes())
 def test_runtime_rim_only_new_owner_source(self):
  p=b.ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/FactorySteelFrame.java'
  self.assertIn('factory_steel_v2/owner_slot_rim.png',p.read_text());self.assertNotIn('factory_placer_v2_reference',p.read_text())
 def test_hopper_no_ghost_rails_outside_registered_controls(self):
  width,height,pixels=decode_png(b.OUT/'hopper.png')
  controls=[(131,13,38,7),(130,26,39,7),(132,39,37,7),(132,48,11,11),(144,48,11,11),(158,48,11,11)]
  # The whole reset panel must be a dark owner-source well except actual controls.
  for y in range(5*6,62*6):
   for x in range(129*6,170*6):
    if any(a*6<=x<(a+w)*6 and c*6<=y<(c+h)*6 for a,c,w,h in controls):continue
    pos=((y+16*6)*width+x+8*6)*4
    self.assertLess(max(pixels[pos:pos+3]),70,(x,y))
 def test_hopper_draw_origin_and_decorative_bounds(self):
  source=(b.ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryHopperSteelArtwork.java').read_text()
  self.assertIn('g.blit(STEEL,x-8,y-16,192,182,0F,0F,1152,1092,1152,1092)',source)
  # Static coordinate contract only: no synthetic mouse event is claimed.
  for origin in [(0,0),(72,47)]:
   for scale in [1,2,3,4]:
    for x,y,_ in b.slots('hopper'):
     self.assertEqual(((x+8-8+origin[0])*scale,(y+16-16+origin[1])*scale),((x+origin[0])*scale,(y+origin[1])*scale))
  # Decorative band is deliberately outside the unchanged container rectangle.
  for x,y in [(-4,20),(180,20),(80,-8)]:self.assertFalse(0<=x<176 and 0<=y<166)
 def test_existing_native_widget_renderer_retained(self):
  source=(b.ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java').read_text()
  self.assertIn('drawFactoryHopperWidgets',source);self.assertIn('drawRetainedDropper',source)
if __name__=='__main__':unittest.main()
