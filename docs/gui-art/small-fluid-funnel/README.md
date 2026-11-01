# Small Fluid Funnel GUI reference and integration proof

## Source and Toolkit reproduction

- Owner-supplied canonical PNG: small fluid funnel.png, 941×1672 RGBA, SHA-256 6ec267533396619f4c64657354a5597659a3a83259686d86e76c5247f0c3c08f.
- Visible panel crop: [278,616)-[663,1055), 385×439 RGBA.
- Pixel Art Skill Toolkit source: emmanueltremblay9-stack/pixel-art-skill-toolkit@98eecf77190d5716820ec659f8aeb790513c8e93.
- Canonical crop RGBA and native Toolkit render RGBA SHA-256: 92ca1330ce4d16204ce1fef5bb2ab4e6e34d8823ec85e39ef3a4cdb25bd17323; zero differing pixels/channels at native 385×439.
- The pinned Toolkit CLI was used to validate the JSON and render at scale 1. The reference and generated render are included side by side in the PR. These are source/render images, not in-game screenshots.

## Runtime mapping

- The crop is drawn into a 209×238 logical screen. X scale is 209/385 and Y scale is 238/439; this is a near-uniform fit chosen to preserve the 18-pixel Minecraft slot pitch, with about 0.13% relative aspect difference.
- The existing Small Fluid Funnel machine menu remains authoritative: machine slot 0 is the empty-bucket input, slot 1 is the filled-bucket output, player inventory remains 27 slots plus 9 hotbar slots, fluid amount is the existing field 2 and capacity is 3000 mB.
- The existing server fluid type (water, lava, or milk) is sent in unused funnel-only field 11 as a bounded code. It controls the displayed fluid label and tank fill tint. No new tank, fluid support, temperature, redstone behavior, recipe, or gameplay rule is added.
- The illustrative bucket icons, French text and 1 500 / 3 000 mB sample are masked in the runtime screen; current item stacks, localized labels, amount, capacity and fill are drawn over the reference.
- UI asset provenance is recorded in both credits files. The source PR describes the image as owner-provided but supplies no separate artwork license statement; this PR records provenance and does not claim a distinct artwork license.

## Validation state

- Toolkit exact-render validation: PASS at native crop size (zero RGBA differences).
- Unit tests added: layout mapping, server fluid-type code mapping, and client code decoding.
- Local Gradle build is deferred while the shared build slot is reserved for another GUI branch; CI on this branch is the compile/test gate.
- No Minecraft client was launched. Runtime visual QA and owner visual approval remain pending. Please review the reference/runtime presentation before merging this draft PR.
