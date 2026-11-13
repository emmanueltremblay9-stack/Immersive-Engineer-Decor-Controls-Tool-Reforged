"""Adversarial evidence gates, not a substitute for executing native Minecraft."""
from pathlib import Path
import unittest
from unittest.mock import patch
import block_breaker_packaged_native as subject


class BreakerPackagedEvidenceTests(unittest.TestCase):
    def proof(self):
        return dict(suite='native-block-breaker-packaged-v1',status='passed',nonce='a'*32,executed_source_sha='b'*40,
                    presentation_selector_absent=True,opened_via_native_block_interaction=1,pickup_server_acks=1,
                    return_server_acks=1,energy_seeded=240,energy_final=0,width=1280,height=720,selected_language='en_us',
                    work=dict(target_before='minecraft:dirt',target_after='minecraft:air',energy_seeded=240,energy_after=0,
                              output_item='minecraft:dirt',output_count=1,world_drop_count=0,server_confirmed=True),
                    final_conservation=dict(destination='player_inventory',destination_menu_slot=1,machine_item_count=0,
                                            player_item='minecraft:dirt',player_item_count=1,carried_item_count=0,
                                            world_drop_count=0,output_accepts_insertion=False,server_confirmed=True),
                    packaged_runtime=dict(jar_path='/tmp/test.jar',jar_sha256='c'*64,fixture_free_jar=True,
                                          separate_harness_module=True,active_texture_sha256=subject.ART_SHA,
                                          active_power_texture_sha256="23ff15c60420b8951117f748d0b6122e174c915f87e91466930af80fbdc12123",
                                          active_texture_pack='mod/immersive_engineer_decor_controls_tool_reforged'),
                    captures=[dict(name=n,file='breaker-'+n+'.png',sha256='d'*64,render_frames=3,
                                   tooltip=dict(in_bounds=True,text='Small Block Breaker',line_count=4,width=240)) for n in subject.NAMES])

    def check(self,p):
        with patch.object(subject,'png'):
            subject.validate(p,'a'*32,'b'*40,Path('/tmp/test.jar'),'c'*64,Path('/tmp'),1280,720,'en_us')

    def test_valid_shape(self): self.check(self.proof())

    def test_identity_and_gameplay_corruption_rejected(self):
        for key in ['status','nonce','executed_source_sha','presentation_selector_absent',
                    'opened_via_native_block_interaction','pickup_server_acks','return_server_acks',
                    'energy_seeded','energy_final','work','width','height','selected_language']:
            with self.subTest(key=key):
                p=self.proof();p[key]=None
                with self.assertRaises(subject.SmokeFailure):self.check(p)

    def test_single_line_and_overwide_fluid_tooltips_are_rejected(self):
        for key, value in [('line_count', 1), ('line_count', True), ('line_count', None),
                           ('width', 847), ('width', 241), ('width', 0), ('width', True), ('width', None)]:
            p = self.proof(); p['captures'][3]['tooltip'][key] = value
            with self.subTest(key=key, value=value), self.assertRaises(subject.SmokeFailure): self.check(p)

    def test_production_tooltip_uses_bounded_font_split(self):
        import re
        source = (Path(__file__).resolve().parents[2] / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java').read_text()
        pattern = r'renderTooltip\(this.font,\s*this.font.split\(blockBreakerTooltip, milkingTooltipWidth\(this.width\)\), mouseX, mouseY\)'
        self.assertRegex(source, pattern)
        old = re.sub(pattern, 'renderTooltip(this.font, blockBreakerTooltip, mouseX, mouseY)', source)
        self.assertNotRegex(old, pattern)

    def test_final_conservation_corruption_rejected(self):
        for key in self.proof()['final_conservation']:
            with self.subTest(key=key):
                p=self.proof();p['final_conservation'][key]=None
                with self.assertRaises(subject.SmokeFailure):self.check(p)
        for destination in [0,37,True]:
            p=self.proof();p['final_conservation']['destination_menu_slot']=destination
            with self.assertRaises(subject.SmokeFailure):self.check(p)
        p=self.proof();p['final_conservation']['destination']='machine_output'
        with self.assertRaises(subject.SmokeFailure):self.check(p)
        p=self.proof();p['final_conservation']['output_accepts_insertion']=True
        with self.assertRaises(subject.SmokeFailure):self.check(p)

    def test_missing_coverage_and_wrong_title_rejected(self):
        for change in [lambda p:p['captures'].pop(),lambda p:p['captures'][1]['tooltip'].update(text='title.key'),
                       lambda p:p['captures'][1]['tooltip'].update(in_bounds=False),
                       lambda p:p['captures'][1].update(render_frames=0),
                       lambda p:p['captures'][1].update(file='../unexpected.png')]:
            p=self.proof();change(p)
            with self.assertRaises(subject.SmokeFailure):self.check(p)

    def test_wrong_package_or_art_rejected(self):
        for key in ['jar_path','jar_sha256','fixture_free_jar','separate_harness_module','active_texture_sha256','active_texture_pack','active_power_texture_sha256']:
            p=self.proof();p['packaged_runtime'][key]=None
            with self.assertRaises(subject.SmokeFailure):self.check(p)


if __name__=='__main__':unittest.main()
