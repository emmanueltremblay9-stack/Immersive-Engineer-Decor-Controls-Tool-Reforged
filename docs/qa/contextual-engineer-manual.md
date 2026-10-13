# Contextual Engineer Manual shortcut

## Scope and implementation

Hold H for 500 ms over an item tooltip to open its linked Engineer Manual page.
The key is configurable under Minecraft Controls > IEDCTR, and the tooltip shows
its current translated binding, including modifier/mouse bindings. Unbound keys
hide the hint. Input already consumed by a screen or an earlier event handler
is not stolen; focused edit fields and F3 combinations are excluded.

Uses the existing IE 12.4.2-194 `ManualHelper` and `ManualInstance` APIs. IE's
`getGui(false)` performs lazy initialization/reload before `getManualLink(stack)`.
No item lists, namespace filters, synthetic mappings, default-stack fallbacks,
or separately cached item/page indexes are introduced. Consequently this covers
IE and addon entries only when IE actually indexes that exact item and its
components. Recipe alternatives, matching and ambiguous-entry precedence remain
IE's responsibility. Unmapped items, and books other than Engineer Manual, are
outside the scope. Lookup follows resource reloads through IE's lifecycle.

New code is an original implementation; no AE2/GuideME/IE implementation or
assets are copied. AE2's guide interaction inspired the hold gesture only.

English, French (France), and French (Canada) labels are provided. Other locales
use Minecraft's standard English fallback for these three new keys; the
localization validator explicitly permits this bounded fallback while retaining
strict parity for all older keys.

## Automated checks

`./gradlew test` includes `validateManualShortcutHold`: 16 standalone assertions
cover threshold timing, one-shot triggering, release/rearm, changing the hovered
target, leaving the tooltip, interrupted input, and unmapped items. It has no
Minecraft runtime dependency. `compileJava`, resource checks, dedicated
GameTests and aggregate `build` are also required in CI.

## Client validation still required before release

- Fresh world/client: first hover initializes the manual without first opening it
- Hover linked IE, IEDCTR and another addon item; each opens the correct anchor
- Hover unmapped and component-modified items; obey IE's exact matching
- Hold for less than 500 ms, release, change item, move away, change screen,
  alt-tab, and repeat; no accidental opening or accumulated stale time
- Remap H, modifier+key, and an unused mouse button; tooltip matches and opens
- Unbind; hint and activation disappear
- Type in creative, machine, JEI and other search fields; input remains text
- F3+H remains advanced-tooltip toggle, never opens the manual
- Reload resources/language and reconnect; no stale links or old translations
- Escape the manual; normal IE lifecycle and normal inventory/container state
- Test inventory and JEI rendered tooltips. Other UI providers must emit the
  NeoForge tooltip/screen events; compatibility is not assumed without testing
- Dedicated server starts without loading client shortcut classes

No merge, publication, desktop installation, or in-game qualification is implied
by creating the draft PR.
