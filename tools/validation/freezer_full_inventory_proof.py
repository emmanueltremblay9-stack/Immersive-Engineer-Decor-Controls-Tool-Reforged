"""Additional issue153 evidence only; does not replace normal Freezer proof gates."""
import hashlib
import math
import struct
from client_mixin_smoke import SmokeFailure


def validate_full_inventory(proof, game, width, height):
    if proof.get('durability_verified') is not False or proof.get('full_inventory_input') != 'GLFW cursor positioning and native tooltip/render events; not physical OS input':
        raise SmokeFailure('Missing or overstated full inventory boundary')
    records = proof.get('full_inventory_hovers')
    if not isinstance(records, list) or len(records) != 36:
        raise SmokeFailure('Expected 36 native hover records')
    folder = game / 'freezer-full-inventory-screenshots'
    overview = proof.get('full_inventory_overview', {})
    for record, name in [(overview, 'inventory-full.png')] + [(r, f'inventory-slot-{i}.png') for i, r in enumerate(records, 3)]:
        if record.get('name') != name or record.get('width') != width or record.get('height') != height:
            raise SmokeFailure('Full inventory capture identity mismatch')
        data = (folder / name).read_bytes()
        if data[:8] != b'\x89PNG\r\n\x1a\n' or struct.unpack('>II', data[16:24]) != (width, height) or hashlib.sha256(data).hexdigest() != record.get('sha256'):
            raise SmokeFailure('Full inventory capture bytes mismatch')
    positions = set()
    for i, record in enumerate(records, 3):
        if type(record.get('menu_index')) is not int or record['menu_index'] != i or record.get('container_index') != (i + 6 if i < 30 else i - 30):
            raise SmokeFailure('Native hovered slot mapping mismatch')
        if record.get('item') != 'minecraft:stone' or type(record.get('count')) is not int or record['count'] != 64 or type(record.get('rendered_frames')) is not int or record['rendered_frames'] < 3:
            raise SmokeFailure('Missing rendered stack/stable frames')
        xy = tuple(record.get(k) for k in ('pointer_x', 'pointer_y'))
        if any(type(v) not in (int, float) or not math.isfinite(v) for v in xy) or not (0 <= xy[0] < width and 0 <= xy[1] < height) or xy in positions:
            raise SmokeFailure('Invalid or repeated native cursor position')
        positions.add(xy)
    return True
