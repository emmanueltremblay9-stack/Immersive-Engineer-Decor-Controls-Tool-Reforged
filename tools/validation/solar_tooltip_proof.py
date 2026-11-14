import hashlib
from client_mixin_smoke import SmokeFailure

def validate_tooltip(proof,game,nonce,sha,language):
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
