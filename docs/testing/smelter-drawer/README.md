# Smelter output drawer accessible action

The real drawer button was initialized with the open action and retained it
while the drawer was open. The toggle now refreshes its message using the
existing localized helper. No artwork, gameplay, translations, slot positions
or input routing change.

Validation:
- `test_smelter_drawer_accessibility.py` executes the exact toggle/helper with
  recording doubles. Four toggles verify alternating open/close labels and
  unchanged menu/availability side effects. Removing setMessage must fail.
- Existing `NativeSmelterGuiValidation` locates the real button via `children()`
  and reads `getMessage()` closed, opened, resized-open, restored-open,
  closed-again via Escape (including focus restoration) and reopened via click.
  It checks translated EN/FR text and real slot
  visibility, using native screen callbacks and `resize` directly.
- `validate_smelter_gui_proof` requires all six observations and explicitly
  disallows an audio-narration or OS-input claim. Tests reject missing, stale,
  reordered and mistyped observations.

Boundary: a real Minecraft client fixture, but programmatic screen callbacks,
not XTest or physical keyboard/mouse, and no audible screen-reader proof.
Resize calls exercise screen lifecycle, not an operating-system window change.
Existing progress fixtures and their gameplay scope are unchanged. New native
execution remains required before claiming the strengthened qualification passes.
