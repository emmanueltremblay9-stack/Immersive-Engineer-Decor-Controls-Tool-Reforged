"""Additive Solar hint layer. Historical manifests/hashes remain untouched.
Only the exact appended locale entry may be projected away, restoring baseline bytes.
"""
import hashlib
from heavy_double_t_provenance import project as heavy_project, current_hash as heavy_hash
import json
BASELINE_COMMIT = "630cf7ba"
KEY = 'gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.cell_tooltip'
FILES = {'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/en_us.json': {'baseline_sha256': 'efdd1e32fa134196f629ca2c06bd2bf74a86e94a4be4f9bb2944b86d605b2c30', 'current_sha256': '39b48736b027d915123882143a649d1da6394eecc894bf4867d1e7d049a644db', 'added_text': 'Energy item slot: accepts items with FE energy storage. Charging depends on the item accepting energy.'}, 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/fr_fr.json': {'baseline_sha256': '4fb0a94655848377b3fe95d2ab173762e9feadc57a7da692b63f54cc47ae8734', 'current_sha256': '550f1ffbe5ad7dd1d2f070222a0d6f66c253c3848f7c523fa27ba390911c7ef9', 'added_text': 'Emplacement pour objet à réserve d’énergie FE. La recharge dépend de la capacité de l’objet à recevoir de l’énergie.'}, 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/fr_ca.json': {'baseline_sha256': 'df6c25b2a8e14d26a9a20fb169434387d2cfddaa9d597fdf3a67ba523e7c5eda', 'current_sha256': '6d4d638b67ea0b441a1e4c976d23f70d471b97a25a57e2fba8a0b941510163ba', 'added_text': 'Emplacement pour objet à réserve d’énergie FE. La recharge dépend de la capacité de l’objet à recevoir de l’énergie.'}}

def project_history(path, data):
    if path not in FILES:
        return data
    data = heavy_project(path, data)
    row = FILES[path]
    assert hashlib.sha256(data).hexdigest() == row['current_sha256'], 'Solar cell current bytes changed: '+path
    suffix = (',\n  '+json.dumps(KEY)+': '+json.dumps(row['added_text'], ensure_ascii=False)+'\n}\n').encode()
    assert data.endswith(suffix), 'Solar cell additive entry changed: '+path
    historical = data[:-len(suffix)] + b'\n}\n'
    assert hashlib.sha256(historical).hexdigest() == row['baseline_sha256'], 'Solar cell historical bytes changed: '+path
    return historical

def current_hash(path, historical_hash):
    if path not in FILES:
        return historical_hash
    assert historical_hash == FILES[path]['baseline_sha256'], 'Solar cell baseline changed: '+path
    return heavy_hash(path, FILES[path]['current_sha256'])
