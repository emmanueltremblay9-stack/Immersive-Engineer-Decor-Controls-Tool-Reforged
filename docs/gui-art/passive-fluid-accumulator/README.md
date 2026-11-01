# Passive Fluid Accumulator GUI

## Reference

![Owner-supplied Passive Fluid Accumulator reference](passive-fluid-accumulator.reference.png)

- Canonical owner-supplied PNG: 941×1672 RGBA; file SHA-256 bb2206d5caf1913081f66b4f22ac9cb2fa5720a0489fd28fc80ab1b96eafc5a7; decoded RGBA SHA-256 384c8f2f6c85bb4372a0761b8dee03cf45f74775be2c00bf56e676213b94612f.
- Visible alpha bounds: (278,616)-(663,1055), producing a 385×439 source crop with RGBA SHA-256 a4c8df73dc5a964871aca16da59bdd62253f154da07fb35a35488e3073fa7b9b.
- No separate artwork license statement accompanied the supplied PNG; provenance is recorded in both root and packaged CREDITS.md without asserting a distinct license.

## Runtime mapping

- The runtime texture is a byte-for-byte copy of the canonical PNG. The client blits source region (278,616,385,439) from the full 941×1672 texture onto a 209×238 logical canvas. Source and destination dimensions remain separate; tests check source bounds and mapping.
- Existing machine behavior and server synchronization are unchanged. MachineMenu already receives exact fluid amount in data field 2 and reports 4000 mB capacity. Only water is supported by the existing accumulator logic.
- Machine item slots remain container indices 0 (empty bucket input) and 1 (filled water bucket output); their positions are fitted to the two illustrated frames. Player inventory remains 27 slots plus 9 hotbar slots.
- The static sample amount and blue tank fill are covered by the server-backed value and proportional water fill. Bucket illustrations, sample labels, and inventory caption are covered by actual localized labels or the interactive slots. The passive collection/rain artwork remains explanatory and adds no state or mechanic.

## Toolkit and visual QA status

- Exact Toolkit JSON and native Toolkit render for this PNG have not been found. The available v0.2 Library candidate is marked procedural and non-pixel-identical, so it is not used as proof or as an approved exact source. No CLI validation, render, or zero-difference result is claimed for this GUI.
- No Minecraft client was launched. In-game visual QA and owner approval remain pending. The source image is shown here for review; it is not a runtime screenshot.
