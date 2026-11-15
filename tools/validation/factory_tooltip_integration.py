"""Exact additive Factory control-tooltip layer; historical pins remain immutable."""
import hashlib
PATH = 'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java'
BASE_SHA256 = '2b0f8cc8bfeac9f421b6948833e4d88b7c993c5c633b6f26feebc6f96f2a0e6f'
CURRENT_SHA256 = '92fc98c4a92e236c6f8237109d85eed485beda0abeb2d0060ba495622397fabf'
BEFORE = '         graphics.renderTooltip(this.font, controlTooltip, mouseX, mouseY);'
AFTER = '         MachineKind kind = ((MachineMenu)this.menu).kind();\n         if (kind == MachineKind.FACTORY_HOPPER || kind == MachineKind.FACTORY_DROPPER) {\n            // Preserve translated line breaks and styles; bound long control help to the viewport.\n            int wrapWidth = Math.max(1, Math.min(240, this.width - 16));\n            graphics.renderTooltip(this.font, this.font.split(controlTooltip, wrapWidth), mouseX, mouseY);\n         } else {\n            graphics.renderTooltip(this.font, controlTooltip, mouseX, mouseY);\n         }'

def current_hash(path, historical_hash):
    if path != PATH:
        return historical_hash
    assert historical_hash == BASE_SHA256, 'Unexpected Factory tooltip baseline'
    return CURRENT_SHA256

def assert_projection(source):
    assert hashlib.sha256(source.encode()).hexdigest() == CURRENT_SHA256
    assert source.count(AFTER) == 1
    assert hashlib.sha256(source.replace(AFTER, BEFORE, 1).encode()).hexdigest() == BASE_SHA256
