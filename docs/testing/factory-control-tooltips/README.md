# Factory Hopper / Dropper control-tooltip wrapping

Scope: only the original control-tooltips of Factory Hopper and Factory Dropper.
The Component is passed unchanged to Minecraft Font.split with width
`max(1, min(240, screen.width - 16))`. Other machines, item tooltips,
translations, controls, hitboxes, artwork and gameplay are unchanged.
This preserves explicit newlines and style while bounding long EN/FR help.
No change to narration or component content is introduced.

## Validation layers

- `test_accumulator_tooltip_wrap.py`: executes the extracted production dispatcher
  with recording doubles. Viewport widths 1,24,100,240,320,400,1280; original
  Component identity, pointer, one tooltip, and unchanged neighboring routes.
- `test_factory_tooltip_wrap.py`: source mutation rejection for no split,
  unbounded wrapping, or global routing; exact additive projection to the
  immutable historical MachineScreen pin. These are not native rendering.
- `test_factory_control_tooltip_native.py`: malformed proof rejection only.
- `factory_control_tooltip_native.py`: clean committed checkout, builds ordinary
  JAR and isolated validation mod, checks runtime class origins and JAR hashes,
  launches isolated Xvfb/native integrated client, right-clicks the placed
  machine and hovers actual GUI controls. No hand-constructed production screen.
  RenderTooltip events observe full localized text, gray style, line bounds;
  screenshots are real framebuffer captures with SHA256 and >=3 rendered frames.
  Proof schema v2 retains the inversion-to-ignored example, then physically clicks
  inversion again to leave ignored mode before testing pulse versus continuous.
  Three server acknowledgements, changed visible state text and full expected
  semantic text are required. Historical v1 evidence remains bounded to the
  original run and is not reclassified as pulse/continuous coverage.
  Hopper inversion/trigger are physically clicked; changed synchronized menu
  state must match the server before capturing the changed text.

CI matrix: Hopper and Dropper, EN1280x720 and FR320x260. Direct commands:

```
python tools/validation/factory_control_tooltip_native.py --machine hopper --language fr_fr --width 320 --height 260
python tools/validation/factory_control_tooltip_native.py --machine dropper --language en_us --width 1280 --height 720
```

Native success is bounded tooltip qualification, not owner visual acceptance,
complete machine gameplay qualification, or a release verdict. No native run
has been claimed by the implementation itself. CI must compile and execute the
exact candidate before a passed status is reported. Initial local compilation
could not start because no writable populated Gradle cache was available.
