"""Filled profile evidence mutations; these do not replace a native execution."""
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch, MagicMock
import unittest
import block_breaker_packaged_native as subject
import test_block_breaker_packaged_native as historical


class FilledBreakerEvidenceTests(unittest.TestCase):
    def proof(self):
        p = historical.BreakerPackagedEvidenceTests().proof()
        p['lubricant_profile'] = dict(ip_sha256=subject.IP_SHA, fluid_id='immersivepetroleum:lubricant',
            setup=subject.IP_SETUP, server_initial_amount=1000, client_initial_amount=1000, capacity=4000,
            client_fluid_id='immersivepetroleum:lubricant', client_preview_ticks=20,
            server_final_amount=990, client_final_amount=990, accelerated_cycle_observed=True,
            client_fluid_name='Lubricant')
        p['captures'][3]['tooltip']['text'] = 'Lubricant: 1000 / 4000 mB Mode: Accelerated (next cycle); duration: 20 ticks.'
        return p

    def check(self, p, lubricant=True):
        with patch.object(subject, 'png'):
            subject.validate(p, 'a'*32, 'b'*40, Path('/tmp/test.jar'), 'c'*64, Path('/tmp'), 1280, 720, 'en_us', lubricant)

    def test_filled_profile(self):
        self.check(self.proof())

    def test_missing_wrong_or_type_confused_profile_rejected(self):
        p = self.proof(); del p['lubricant_profile']
        with self.assertRaises(subject.SmokeFailure): self.check(p)
        for key, value in self.proof()['lubricant_profile'].items():
            for mutation in [None, 'wrong', False, 1]:
                with self.subTest(key=key, mutation=mutation):
                    p = self.proof(); p['lubricant_profile'][key] = mutation
                    with self.assertRaises(subject.SmokeFailure): self.check(p)
        for key in self.proof()['lubricant_profile']:
            p = self.proof(); del p['lubricant_profile'][key]
            with self.assertRaises(subject.SmokeFailure): self.check(p)

    def test_wrong_tooltip_amount_mode_duration_or_name_rejected(self):
        for old,new in [('1000','990'), ('4000','1000'), ('Accelerated','Normal'), ('20 ticks','30 ticks'), ('Lubricant','Water')]:
            p=self.proof();p['captures'][3]['tooltip']['text']=p['captures'][3]['tooltip']['text'].replace(old,new)
            with self.assertRaises(subject.SmokeFailure):self.check(p)
        p=self.proof();p['lubricant_profile']['client_fluid_name']='fluid.immersivepetroleum.lubricant'
        with self.assertRaises(subject.SmokeFailure):self.check(p)

    def test_french_uses_pinned_ip_english_name_and_french_mode(self):
        p=self.proof();p['selected_language']='fr_fr'
        p['captures'][1]['tooltip']['text']='Petit brise-bloc'
        p['captures'][3]['tooltip']['text']='Lubricant : 1000 / 4000 mB Mode : Accéléré (prochain cycle) ; durée : 20 ticks.'
        with patch.object(subject,'png'):
            subject.validate(p,'a'*32,'b'*40,Path('/tmp/test.jar'),'c'*64,Path('/tmp'),1280,720,'fr_fr',True)

    def test_profile_cannot_masquerade_as_empty(self):
        with self.assertRaises(subject.SmokeFailure):self.check(self.proof(),False)
        with self.assertRaises(subject.SmokeFailure):self.check(historical.BreakerPackagedEvidenceTests().proof())

    def test_wrong_download_hash_never_writes_mod(self):
        response=MagicMock();response.__enter__.return_value.read.return_value=b'not the IP jar'
        with TemporaryDirectory() as tmp,patch.object(subject.urllib.request,'urlopen',return_value=response) as download:
            with self.assertRaises(subject.SmokeFailure):subject.install_pinned_ip(Path(tmp))
            self.assertFalse((Path(tmp)/'mods').exists())
            download.assert_called_once_with(subject.IP_URL,timeout=90)

    def test_runtime_wiring_and_real_observation_gates(self):
        root=Path(__file__).resolve().parents[2]
        source=(root/'src/factoryPlacerNativeValidation/java/com/oblixorprime/iedctrtest/factoryplacer/BlockBreakerPackagedValidation.java').read_text()
        for token in ['level.getCapability(Capabilities.FluidHandler.BLOCK, POS, Direction.WEST)',
                      'ModList.get().getModFileById("immersivepetroleum")',
                      'IP_SHA.equals(ipHash)', 'menu.fluidAmount() == 1000', 'menu.fluidAmount() == 990',
                      'ServerTickEvent.Post', 'machine.dataAccessForTests().get(3) == 20',
                      'acceleratedCycleObserved', 'port.getFluidInTank(0).getAmount() == 990']:
            self.assertIn(token,source)
        workflow=(root/'.github/workflows/block-breaker-packaged.yml').read_text()
        self.assertIn('native-default-breaker:',workflow)
        self.assertIn('native-lubricant-breaker:',workflow)
        self.assertEqual(workflow.count(' --lubricant --language'),1)
        self.assertIn('iedctr.blockBreakerLubricantNative',Path(subject.__file__).read_text())


if __name__=='__main__':unittest.main()
