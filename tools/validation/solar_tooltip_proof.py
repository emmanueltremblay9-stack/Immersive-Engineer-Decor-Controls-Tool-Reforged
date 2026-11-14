import hashlib
import struct
from client_mixin_smoke import SmokeFailure

def validate_tooltip(proof,game,nonce,sha,language):
    required_ints=('frames','x','y','tooltip_width','tooltip_height','viewport_width','viewport_height')
    if any(type(proof.get(k)) is not int for k in required_ints) or proof.get('viewport_width')!=320 or proof.get('viewport_height')!=260 or proof.get('tooltip_width',0)<=0 or proof.get('tooltip_height',0)<=0:
        raise SmokeFailure('Invalid native Solar tooltip measurements')
    if not isinstance(proof.get('expected_text'),str) or not proof['expected_text'].strip() or not isinstance(proof.get('lines'),list) or not all(isinstance(line,str) for line in proof['lines']):
        raise SmokeFailure('Missing native Solar tooltip content')
    raw=(game/'solar-fe-tooltip.png').read_bytes()
    if raw[:8]!=b'\x89PNG\r\n\x1a\n' or struct.unpack('>II',raw[16:24])!=(320,260):
        raise SmokeFailure('Native Solar tooltip framebuffer differs')
    if proof.get('nonce')!=nonce or proof.get('source_sha')!=sha or proof.get('language')!=language or proof.get('frames',0)<3:
        raise SmokeFailure('Invalid native Solar tooltip identity/frames')
    if hashlib.sha256((game/'solar-fe-tooltip.png').read_bytes()).hexdigest()!=proof.get('capture_sha256'):
        raise SmokeFailure('Solar tooltip screenshot mismatch')
    if ''.join(''.join(proof.get('lines',[])).split())!=''.join(proof.get('expected_text','').split()) or not proof.get('styles'):
        raise SmokeFailure('Solar tooltip text/style missing')
    w,h=proof['tooltip_width'],proof['tooltip_height'];x,y=proof['x'],proof['y']
    if not (x-4>=0 and y-4>=0 and x+w+4<=proof['viewport_width'] and y+h+4<=proof['viewport_height']) or proof.get('in_bounds') is not True:
        raise SmokeFailure(f'Native Solar FE tooltip clipped: ({x},{y},{w},{h}) in {proof["viewport_width"]}x{proof["viewport_height"]}')
    return True
