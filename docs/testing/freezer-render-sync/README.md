# Freezer natural-phase framebuffer synchronization

Qualification-only harness change based on PR187 head `0e261b42`.
Run `43630145841` recorded menu phases 0/1/2/3 at progress 0/47/92/137,
but the phase-1 chamber crop was byte-identical to idle phase 0. The old
`frostStableFrames` counted client ticks, not rendered frames. Catch-up ticks
could therefore label the previous framebuffer with the new menu state.

Frost captures now require matching ScreenEvent.Render.Pre/Post on the actual
Freezer screen/menu, unchanged phase/progress during the render, and at least
three coherent frames in that phase. GuiGraphics is flushed before reading the
framebuffer in Post. Idle phase zero follows the same render gate. Errors return
to the existing tick failure path. Missing natural phases still fail; no recipe
hold, game-rule change, production-renderer change, or image-threshold relaxation.

Proof records require `rendered_frames >= 3` (integer, not boolean) and
`capture_event = ScreenEvent.Render.Post`. Schema mutations and structural tests
are not native proof. Exact-commit mapped compilation and native CI remain
required before claiming this fixes the observed run.

PR186 also edits NativeFreezerReferenceValidation for a recovery tooltip probe.
It has deliberately not been included here. Future reconciliation must preserve
both independent event handlers and the recovery probe, sharing the client
subscriber declaration rather than duplicating it.
