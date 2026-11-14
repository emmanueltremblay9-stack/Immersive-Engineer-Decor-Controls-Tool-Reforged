# Crate editor keyboard cancellation

Baseline aa746049 intercepts both Enter keys before focused widgets receive them.
Consequently Enter on the focused Cancel button calls saveAndClose instead of
Cancel. The extracted, unmodified production method executed with recording
widget doubles fails `Cancel saved on key 257`. This is a source-level RED,
not a native Minecraft observation.

The correction delegates to the existing screen/widget handler whenever a Button
is focused. Enter from an edit field still saves. No label payload, sanitation,
art, layout, mouse behavior, or machine gameplay changes.

The same executable test passes after correction and detects mutations removing
the focus check or the edit-field save. It covers Enter/keypad on Cancel and Done,
edit-field Enter/keypad, and the existing Space route. Doubles model standard
button activation; native Minecraft remains required to verify actual semantics.

The existing exact-JAR crate fixture preserves its resize, Unicode, blank-text,
Enter-save and mouse-cancel cases. It adds Tab navigation to real Done/Cancel
widgets, both Enter keys on each, and keypad Enter in a field. Each case waits
for three server snapshots after ten ticks; cancellation must retain the prior
label, saving must persist the new distinct text. These are screen key events,
not physical OS input. Every new case is required by the proof validator.

Native CI and mapped compilation pending; no claim of native GREEN or final
visual acceptance is made by the local tests.
