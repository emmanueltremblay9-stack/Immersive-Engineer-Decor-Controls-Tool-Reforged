"""Exact additive Freezer V5 reconciliation. Historical main pins are not rewritten."""
import hashlib
import json
from factory_tooltip_integration import current_hash as factory_current_hash

LAYER = 'freezer_v5_reconciliation'
RECORD_SHA256 = 'b4c91dcd9553c8d1af5af02846299c6dda5505622d9ed0caa956ea1e597c980b'
FILES = {'src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java': {'main_sha256': '3f32ffeccd725fe8d54b95f5a54ae11acb8a31bfb124172bee60bbf420977723',
                                                                                       'freezer_v5_sha256': 'ae2230498ad5567b02e5e9873d0d9e01e24129e0956f20349798e98db28dd04b',
                                                                                       'integrated_sha256': 'bf327e4fa3ff6b32afa1ec80c03c23b2c372ff8470a9181e86351cf29feba29c'},
 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java': {'main_sha256': 'a8a3d691e191bf4dc5a9725e7c9e4e9b0e19b8647a5a6c1408e2f2a92dcc9fdd',
                                                                                 'freezer_v5_sha256': '1f9233a7be9d9a0a284625322a426eb2ae0eb8f39f00016ef4326a00f53193b9',
                                                                                 'integrated_sha256': '2396df69a54927bc1dc818d887ac5a417765ceb8e8e5fbff0f3ee4695d43faf0'},
 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/en_us.json': {'main_sha256': 'b6ca6454118504f44a918a2b248fff649ee35ff7f2352389302f0d51a366c500',
                                                                                               'freezer_v5_sha256': 'b1692c3d2221068955de4b07929c97ba0cc00d465bbdf0ffc3ee035b0e34e08b',
                                                                                               'integrated_sha256': 'efdd1e32fa134196f629ca2c06bd2bf74a86e94a4be4f9bb2944b86d605b2c30'},
 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/fr_fr.json': {'main_sha256': '5f737152b4a5e1c372b28c21324fa3287c2896bf7a594146abc1e0541d8e482b',
                                                                                               'freezer_v5_sha256': '3094194ae387faba243ba02fef97a8180cfba3b65e43796c46ba520de6f8aecb',
                                                                                               'integrated_sha256': '4fb0a94655848377b3fe95d2ab173762e9feadc57a7da692b63f54cc47ae8734'},
 'tools/validation/check_barrel_reference_wiring.py': {'main_sha256': '89128c598d2e698f4f670b0551b8f918661fca3b836100890873853ec76bc7c3',
                                                       'freezer_v5_sha256': None,
                                                       'integrated_sha256': '108015af3c2e33c3b7953a50f867b1506a85cee8d6dca0f1f15528402c45fa37'}}


ACTIVATION_PATH = 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java'
ACTIVATION_SHA256 = '626ff2fecd86da026e16e9fee4b4a76f840508c3029f4c2fdb2809d72062713f'
ACTIVATION_RECORD_SHA256 = '3976fe30c82a8e7c95e9074a7cfe234fed114fb9f1743d833d3fa95c97c0ac82'

DIAGNOSTIC_SHA256 = '74f95f7e8ab5af051ae6ce9ff435070a95cc12ea199f11c775e35fd14dfeb3de'
DIAGNOSTIC_RECORD_SHA256 = 'f9c49d5d56a2d58a9af31f13eb5531a90193ab67cd307319675cdcee1b931004'

def assert_freezer_v5_reconciliation(root, manifest):
    record = manifest.get(LAYER)
    digest = hashlib.sha256(json.dumps(record, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
    assert digest == RECORD_SHA256, 'Freezer V5 reconciliation provenance changed'
    activation = manifest.get('freezer_v5_normal_activation')
    assert hashlib.sha256(json.dumps(activation, sort_keys=True, separators=(',', ':')).encode()).hexdigest() == ACTIVATION_RECORD_SHA256, 'Normal activation record changed'
    diagnostic = manifest.get('freezer_v5_legacy_diagnostic')
    assert hashlib.sha256(json.dumps(diagnostic, sort_keys=True, separators=(',', ':')).encode()).hexdigest() == DIAGNOSTIC_RECORD_SHA256, 'Legacy diagnostic record changed'
    for path, hashes in FILES.items():
        assert record['files'][path] == hashes, path
        data = (root / path).read_bytes()
        if path == ACTIVATION_PATH:
            assert hashlib.sha256(data).hexdigest() == DIAGNOSTIC_SHA256, path
            text = data.decode()
            assert text.count(diagnostic['replace_to']) == 1, 'Ambiguous diagnostic projection'
            data = text.replace(diagnostic['replace_to'], diagnostic['replace_from']).encode()
            assert hashlib.sha256(data).hexdigest() == ACTIVATION_SHA256, path
            text = data.decode()
            assert text.count(activation['replace_to']) == 1, 'Ambiguous default projection'
            data = text.replace(activation['replace_to'], activation['replace_from']).encode()
        assert hashlib.sha256(data).hexdigest() == hashes['integrated_sha256'], path


def current_hash(path, historical_hash):
    if path not in FILES:
        return factory_current_hash(path, historical_hash)
    assert historical_hash == FILES[path]['main_sha256'], 'Unexpected Freezer V5 baseline: ' + path
    return DIAGNOSTIC_SHA256 if path == ACTIVATION_PATH else FILES[path]['integrated_sha256']
