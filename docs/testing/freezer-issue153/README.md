# Issue 153: filled player inventory, qualification only

This branch extends the explicitly enabled bundled native fixture, not the renderer,
menu, assets or gameplay. Consequently the qualification JAR has a **new hash**;
it is not a release and must not borrow the identity of the historical JAR.
`production-baseline.json` records the eight unchanged class entries and eight
Freezer textures already covered by runtime provenance (excluding the edited
NativeFreezerReferenceValidation fixture). The workflow compares these exact bytes,
not every class in the repository: unrelated changes since the baseline are allowed.


The existing FULL_SETUP fixture already supplies 36 server-synchronized STONE ×64
stacks. Before recovery, the extension captures the full GUI and every player-slot
center after at least three consecutive native rendered tooltip frames. It verifies
`getSlotUnderMouse()` identity, container index, stable transformed cursor position,
actual tooltip ItemStack, client counts/carried stack and later server counts.
The method is publicly provided in the official NeoForge 1.21.1
`AbstractContainerScreen.java.patch`. The real tooltip is observed through
`RenderTooltipEvent.Color` and completed frames through `ScreenEvent.Render.Post`.

This is GLFW cursor positioning, not physical OS input. Stone has no durability;
no durability or audible narration claim is made. Screenshots still require human
visual inspection to assess icon/count clipping; a passing mapping gate alone is
not visual acceptance. No user world or installed Minecraft profile is used.

Run the existing Freezer packaged workflow on this exact qualification branch.
EN 1280×720 and FR 1280×260 each produce 37 additional framebuffer PNGs with hashes.
`freezer_full_inventory_proof.py` supplements, never bypasses, all existing gates.
