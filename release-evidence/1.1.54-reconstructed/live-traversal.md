# Collision traversal substitution

No Computer Use, keyboard/mouse control, screenshot, or visual client interaction was invoked.

The automated GameTest world verifies 33 world-space positions of a real standing Minecraft player AABB (`0.6 x 1.8` blocks) through every open `FACING x HINGE` sliding-door combination using `Level.noCollision`. It also checks the closed-door negative control and probes each retained open solid component. All 182 tests passed twice.

This is accepted as the collision gate only. It does not claim client visual, audio, manual UI, or general UX validation.

Prism client world entry remains `BLOCKED_BY_UNRESOLVED_TRANSIENT_LOOPBACK`; see `loopback-diagnostic.md`.
