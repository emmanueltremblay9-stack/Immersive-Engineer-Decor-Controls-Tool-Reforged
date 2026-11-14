# Solar FE tooltip wrapping

## Observed regression before the fix

Qualification run **43622848906**, exact source
`f88bed08b53a4e3464e81a07b76966631660fcf1`, ordinary JAR SHA-256
`8e5fc9116a71e5b319bf682d702ecf02ed27d8109285a18c6c0f3d91c356cba7`.

Both native clients used a **320×260 framebuffer, GUI scale 1**. All existing Solar
validation gates (including 37 inventory routes, server observations, FE captures,
reopen and ordinary-JAR provenance) passed and were replayed offline. The added
native tooltip gate alone failed:

| Locale | Native tooltip rectangle x,y,w,h | Result |
| --- | --- | --- |
| en_us | 4,114,339,8 | Right edge clipped |
| fr_fr | 4,114,392,8 | Right edge clipped |

Each observation followed five completed render frames. The screenshots were decoded
at 320×260 and visually inspected; they showed the clipped text. Artifact ZIP hashes:

- EN `14125285731`: `39cfb50a3faf5b4b49ece0d2027b562ecf138c721a0e6f366f63f9fdf83cf0e0`
- FR `14124748331`: `8543396607873c64251fec8a64d122c380d57b9b5df78f5e117cd4f42102c42c`

## Correction and evidence gates

Only the energy tooltip in `SolarV2ReferenceScreen` is changed. The original
translated Component, including its styles and synchronized FE arguments, goes to
`Font.split` with `max(1, min(240, width - 16))`. No conversion to a plain string,
translation edit, artwork change, gameplay change or other tooltip routing occurs.

The recording test executes the exact production tooltip body and rejects removal
of wrapping and incorrect bounds. It is not native visual proof. Native qualification
reuses the existing isolated ordinary-JAR Solar harness, adds an XTest hover over the
FE meter after its normal checks, and records the actual Color tooltip components,
text styles, dimensions, coordinates, completed frames and screenshot hash.

`--tooltip-probe` is explicitly limited to 320×260 and GUI scale 1. Other existing
Solar profiles are unchanged. An XTest pointer inside the isolated runner is not a
claim of manual user testing. Opening/reopening remain programmatic. This test does
not qualify every viewport, third-party font/resource pack or screen-reader audio.
The native gate requires the complete expected text and viewport containment; no
historical gate is bypassed. Fresh green native results on the corrected exact SHA
are still required. The failed baseline is preserved, not relabeled as passing.

The workflow supports relevant pull requests and the exact qualification branch.
No merge, publication or release is implied by this qualification.
