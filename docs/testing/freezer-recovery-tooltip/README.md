# Freezer recovery tooltip: bounded RED probe

Base: `54e265d5178f41aef1c77c5565eeab32d02f1f6e`.
Qualification-only branch: `dot/qualify-freezer-recovery-tooltip-20261114`.
No renderer, artwork, menu or gameplay change. No RED result claimed until CI runs.

The existing native fixture fills all 36 player slots and legacy reserved slot 1. Before its existing full-inventory recovery attempt, this opt-in probe moves the real pointer through XTest on the runner-owned nonce-guarded Xvfb display to the visible recovery button. It waits for at least three real render frames containing the actual tooltip, captures the unedited framebuffer, and records full localized text, per-character styles and native tooltip bounds. It then executes the unchanged full-inventory, extraction, duplicate prevention, reopen and recipe checks. Recovery clicks remain programmatic Screen calls, not OS clicks.

Both profiles are 320×260, GUI scale 1, EN/FR. The driver rejects other probe sizes; ordinary non-probe defaults remain 1280 pixels wide. Text is checked against the retained localized instructions. Bounds fail only after all existing gameplay/provenance gates have passed. The bundled opt-in fixture changes the qualification JAR, whose source/hash must be recorded; it is not a published artifact. No claim of audio narration, all viewport sizes, player visual acceptance or durability is made.

Local validation: `python -m unittest discover -s tools/validation -p 'test_freezer*.py'`.
CI workflow is push-triggered only for this exact qualification branch. Full compilation and actual client results remain pending CI; local environments lack the required populated runtime/dependencies.
