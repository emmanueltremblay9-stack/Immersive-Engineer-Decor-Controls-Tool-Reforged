# Block Breaker machine GUI integration

This branch connects the owner-approved v0.8 concept to the existing Block Breaker menu and block entity. The owner-approved FE balance is 160 + 160 × positive block hardness FE per completed block, a 32,000 FE reserve, and a 256 FE/t input limit.

## Runtime behavior in this branch

- The screen uses the original 209 x 235 concept background. Labels, target item, hardness, progress, redstone signal, stored energy, and status are rendered from Minecraft/runtime data. The target block's registry ID is sent in three 15-bit menu-data components so remote clients do not depend on a client-side block entity reference or a single truncated value. The static sample-state image and label overlay are not used.
- The menu exposes one vanilla output slot and the normal 27 player inventory slots plus the 9-slot hotbar.
- The block entity has a 27-stack backing list, while the pre-change GUI exposed 10 machine slots: collection slots 0-8 and an I/O slot at 9. The new menu exposes only output slot 0. On upgrade, saved legacy items in slots 1-9 are drained into slot 0 in ascending slot order; newly generated drops are inserted only into slot 0 and are never stored in hidden slots. Backing slots 10-26 are preserved in NBT but remain untouched and are not treated as a collection queue.
- The item capability and sided `Container` view expose the output only at the rear; hopper pulls verify the source-to-destination side as well. Automatic pushing is not enabled.
- Existing redstone behavior is shown as live state: a high signal stops and resets work; a low signal permits it.
- FE is mandatory to break blocks. Cost is `round(160 + 160 × max(hardness, 0))` FE per block; it is apportioned across the existing hardness-based work ticks so a completed block consumes exactly that amount. Work pauses when the next work tick cannot be paid or the single output cannot accept the target's drops. No FE is spent on a blocked output or an unbreakable target.
- The breaker stores at most 32,000 FE and accepts at most 256 FE per machine tick through its underside. Other sides and unsided access expose no breaker FE capability. The GUI shows the exact stored FE and the target's FE/block cost.
- Balancing is implementation-tested, not a claim of broad modpack compatibility or full client/runtime release qualification.

## Artwork provenance

- `source-provenance.json` records the user-supplied/generated concept as unverified Engineer's Decor original texture data.
- The runtime background is a nearest-neighbor 209 x 235 reduction of the approved `background.png` layer (source SHA-256 `0D39C6131234F3D9B63F8FB9954B11D53395981BBDC900D988C64A4D75B87FA7`). The runtime texture SHA-256 is `C7C2911D7E176B4D60A9B7E6B8DDD6BD695F5FA612759D31205DEEFB231F8255`.
- The original 1254 x 1410 composite contains sample values and is intentionally not a runtime texture. The labels overlay includes example-state text (including an OFF label) and is intentionally not rendered; Minecraft draws the live labels and item icons.
- `layout.json` and `layers.json` retain the visual layout and pipeline metadata. They document the asset only; Java menu slots are defined in the runtime source.
