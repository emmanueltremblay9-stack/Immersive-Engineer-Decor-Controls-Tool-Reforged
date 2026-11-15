# Barrel native fluid matrix

This changes isolated validation fixtures only, not tank mechanics, rendering,
items, layout or artwork. It builds on PR151's native atlas-sprite correction.

The existing native client now accepts `--barrel-fluid water|lava|milk` only with
`--corrected-machine-gui-only`. It uses the real machine fluid handler, waits for
client synchronization of both amount and fluid identity, then checks the actual
block-atlas sprite/tint and framebuffer interior for empty/half/full states.
The evidence gate requires the selected fluid, matching native still texture and
non-flat occupied tank pixels, and rejects wrong-fluid evidence. Both languages
and the original corrected interfaces retain their existing checks.

The dedicated CI matrix uses six isolated runs: water/lava/milk, English 720px and
French 260px. Each checks out the exact candidate head and preserves framebuffer
captures and source-bound reports. Running the fixture does not by itself prove
resource reload, temporal animation, every GUI scale, or artwork V5 conformity.

This still tests the existing 201x226 runtime artwork. The separately retained
Barrel V5 reference is 1137x1383 and has not been integrated by this patch. Its
logical geometry and dynamic overlays need measured source work; no invented
controls or visual acceptance are implied.

Before remote execution: 33 Python evidence-gate tests passed locally. This is
synthetic schema testing, not Minecraft execution. Refer to the workflow result
and exact commit for any later native claim.
