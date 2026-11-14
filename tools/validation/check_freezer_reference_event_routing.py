from pathlib import Path
import json,re
r=Path(__file__).resolve().parents[2]
s=(r/'src/main/java/com/oblixorprime/engineersdecorreforged/client/FreezerReferenceScreen.java').read_text()
checks={}
for event in ['mouseClicked','mouseReleased','mouseDragged','mouseScrolled','mouseMoved']:
 checks[event+'_inverse_pointer']=bool(re.search(r'super\.'+event+r'\(view.localX\(mouseX\), view.localY\(mouseY\)',s))
checks['drag_delta']='view.localDelta(dragX), view.localDelta(dragY)' in s
checks['wheel_units_unchanged']='view.localY(mouseY), scrollX, scrollY)' in s
checks['render_floor']='view.renderPointerX(mouseX), view.renderPointerY(mouseY)' in s
checks['raw_hover']='mouseX = renderLocalMouseX;' in s and 'mouseY = renderLocalMouseY;' in s
checks['conditional_recovery']='recoveryButton.visible = menu.getSlot(1).hasItem();' in s and 'ClickType.QUICK_MOVE' in s
assert all(checks.values()), checks
print(json.dumps({'status': 'PASS_STATIC_SOURCE_CHECKS_ONLY_NOT_MINECRAFT_EXECUTION', 'checks': checks}, indent=2))
