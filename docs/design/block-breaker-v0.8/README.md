# Block Breaker machine GUI integration

This branch connects the owner-approved v0.8 concept to the existing Block Breaker menu and block entity. It is a work in progress until the mandatory FE balance values are selected and the resulting server-side energy path is implemented and tested.

## Runtime behavior in this branch

- The screen uses the original 209 x 235 concept background. Labels, target item, hardness, progress, redstone signal, stored energy, and status are rendered from Minecraft/runtime data. The target block's registry ID is sent in three 15-bit menu-data components so remote clients do not depend on a client-side block entity reference or a single truncated value. The static sample-state image and label overlay are not used.
- The menu exposes one vanilla output slot and the normal 27 player inventory slots plus the 9-slot hotbar.
- Existing Block Breaker block entities persist 27 item stacks. Container index 0 is now the visible output; indices 1–26 remain saved as a pending output queue. The queue advances into the visible slot as it is emptied. No legacy stack is discarded or rewritten during load/save.
- The item capability is exposed only at the rear and allows extraction from the output. Automatic pushing is not enabled.
- Existing redstone behavior is shown as live state: a high signal stops and resets work; a low signal permits it.
- FE being mandatory is owner-confirmed. The underside input face is the planned connection. The per-tick/hardness cost, storage capacity, and transfer limit remain unresolved, so the energy capability and paid breaking behavior must not be treated as complete until the user selects those values.

## Artwork provenance

- `source-provenance.json` records the user-supplied/generated concept as unverified Engineer's Decor original texture data.
- The runtime background is a nearest-neighbor 209 x 235 reduction of the approved `background.png` layer (source SHA-256 `0D39C6131234F3D9B63F8FB9954B11D53395981BBDC900D988C64A4D75B87FA7`). The runtime texture SHA-256 is `C7C2911D7E176B4D60A9B7E6B8DDD6BD695F5FA612759D31205DEEFB231F8255`.
- The original 1254 x 1410 composite contains sample values and is intentionally not a runtime texture. The labels overlay includes example-state text (including an OFF label) and is intentionally not rendered; Minecraft draws the live labels and item icons.
- `layout.json` and `layers.json` retain the visual layout and pipeline metadata. They document the asset only; Java menu slots are defined in the runtime source.
