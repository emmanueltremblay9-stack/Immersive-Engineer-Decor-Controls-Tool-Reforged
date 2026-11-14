# Freezer recovery tooltip: bounded RED probe

Initial preparation base: `54e265d5178f41aef1c77c5565eeab32d02f1f6e`. The actual launched probe was rebased on main `97cdb48c394e268bbafaa467c87266b233ea85d7`.
Qualification-only branch: `dot/qualify-freezer-recovery-tooltip-20261114`.
The RED probe did not change renderer, artwork, menu or gameplay. The subsequent fix wraps only the recovery-help Component using Font.split with max(1, min(240, width - 16)); translation, fallback, styles and coordinates remain intact. No recovery action or inventory logic changes.

The existing native fixture fills all 36 player slots and legacy reserved slot 1. Before its existing full-inventory recovery attempt, this opt-in probe moves the real pointer through XTest on the runner-owned nonce-guarded Xvfb display to the visible recovery button. It waits for at least three real render frames containing the actual tooltip, captures the unedited framebuffer, and records full localized text, per-character styles and native tooltip bounds. It then executes the unchanged full-inventory, extraction, duplicate prevention, reopen and recipe checks. Recovery clicks remain programmatic Screen calls, not OS clicks.

Both profiles are 320×260, GUI scale 1, EN/FR. The driver rejects other probe sizes; ordinary non-probe defaults remain 1280 pixels wide. Text is checked against the retained localized instructions. Bounds fail only after all existing gameplay/provenance gates have passed. The bundled opt-in fixture changes the qualification JAR, whose source/hash must be recorded; it is not a published artifact. No claim of audio narration, all viewport sizes, player visual acceptance or durability is made.

Local validation: `python -m unittest discover -s tools/validation -p 'test_freezer*.py'`.
CI workflow covers relevant pull-request paths and pushes only to this exact qualification branch. RED compilation and actual client evidence are verified below; GREEN compilation and native evidence for the correction remain pending exact-commit CI. Local environments lack the required populated runtime/dependencies.

## Verified RED, exact source

Run `43626715309`, commit `3e3e8073fc3eb6a00e6a014ac204a0f9486d5cc1`.
JAR SHA-256: `f4adcea83aed15874284153399031c792c1e1b4496474869fea849f5afd1a17c`.
Both native profiles completed all prior gameplay/recovery and artifact-provenance gates. Only the new bounds gate failed. Offline replay repeated those gates against the archived JAR/logs; the PNGs decoded at 320×260. Both observed 5 actual render frames.

- EN native tooltip x/y/width/height: `(4,67,444,8)`; artifact 14126042567, ZIP SHA-256 `e0259482f2d1a990954005eee81be3f5960148a21ca6796fd43360e4297c7551`.
- FR: `(4,67,581,8)`; artifact 14127180248, ZIP SHA-256 `5a544f842ed2f83513a8e3631e45aafa80781241bc9064ebd34485be4ae6c6d6`.

GREEN evidence for the correction is pending its exact-commit CI. Local recording-font execution checks the actual production body and rejects unwrapped, wrong-margin and unbounded mutations; it is not a native rendering claim.
