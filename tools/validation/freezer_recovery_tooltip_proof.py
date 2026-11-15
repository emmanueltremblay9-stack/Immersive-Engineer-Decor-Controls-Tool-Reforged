import hashlib
import struct
from client_mixin_smoke import SmokeFailure

EXPECTED = {'en_us': 'Recover legacy slot 1 to your inventory. Ctrl+Shift+R. Full inventory: items stay stored.', 'fr_fr': 'Récupérer l’ancien emplacement 1 dans votre inventaire. Ctrl+Maj+R. Inventaire plein : les objets restent stockés.'}

def validate_tooltip(proof,game,nonce,sha,language):
    required_ints=('frames','x','y','tooltip_width','tooltip_height','viewport_width','viewport_height')
    if any(type(proof.get(k)) is not int for k in required_ints) or proof.get('viewport_width')!=320 or proof.get('viewport_height')!=260 or proof.get('tooltip_width',0)<=0 or proof.get('tooltip_height',0)<=0:
        raise SmokeFailure('Invalid native Freezer recovery tooltip measurements')
    if not isinstance(proof.get('expected_text'),str) or not proof['expected_text'].strip() or not isinstance(proof.get('lines'),list) or not all(isinstance(line,str) for line in proof['lines']):
        raise SmokeFailure('Missing native Freezer recovery tooltip content')
    if language not in EXPECTED or proof['expected_text'] != EXPECTED[language]:
        raise SmokeFailure('Recovery tooltip is not the retained localized instruction')
    raw=(game/'freezer-recovery-tooltip.png').read_bytes()
    if raw[:8]!=b'\x89PNG\r\n\x1a\n' or struct.unpack('>II',raw[16:24])!=(320,260):
        raise SmokeFailure('Native Freezer recovery tooltip framebuffer differs')
    if proof.get('nonce')!=nonce or proof.get('source_sha')!=sha or proof.get('language')!=language or proof.get('frames',0)<3:
        raise SmokeFailure('Invalid native Freezer recovery tooltip identity/frames')
    if hashlib.sha256((game/'freezer-recovery-tooltip.png').read_bytes()).hexdigest()!=proof.get('capture_sha256'):
        raise SmokeFailure('Freezer recovery tooltip screenshot mismatch')
    if ''.join(''.join(proof.get('lines',[])).split())!=''.join(proof.get('expected_text','').split()) or not proof.get('styles'):
        raise SmokeFailure('Freezer recovery tooltip text/style missing')
    w,h=proof['tooltip_width'],proof['tooltip_height'];x,y=proof['x'],proof['y']
    if not (x-4>=0 and y-4>=0 and x+w+4<=proof['viewport_width'] and y+h+4<=proof['viewport_height']) or proof.get('in_bounds') is not True:
        raise SmokeFailure(f'Native Freezer recovery tooltip clipped: ({x},{y},{w},{h}) in {proof["viewport_width"]}x{proof["viewport_height"]}')
    return True
