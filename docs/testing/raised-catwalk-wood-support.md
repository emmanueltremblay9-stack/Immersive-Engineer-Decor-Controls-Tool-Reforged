# Raised catwalk treated-wood connections

## Expected behavior

- Place a Raised Steel Catwalk directly above a Straight Treated Wood Pole or a Treated Wood Pole Head: it uses a wooden connection regardless of coordinates, click face, or waterlogging.
- Extend an existing raised catwalk onto a wood-supported destination: the new piece discovers its own support and keeps the same deck height.
- Add, replace, or remove the block directly below: the connection changes between thick steel, thin steel, wood, and the plain deck as appropriate. Changes on the sides do not reinterpret saved variants.
- Installed inset lights remain installed when a wooden support is added. Removing a purchased light refunds it once and discovers the wooden support underneath.
- Variant IDs 0–4 retain their existing meaning. Only this block gains variant 5; the shared legacy variant property is unchanged.

## Model and save compatibility

The new `steel_catwalk_ta5_model.json` preserves the plain deck's elements and textures. Its connection reuses the complete treated-wood pole head, including its shaft, braces and plate. The thin-steel, thick-steel and wood connection plates extend to the grating underside at Y=15; all other geometry and textures are preserved. Models 0, 3 and 4 are unchanged. See [support plate contact](raised-catwalk-support-contact.md) for the exact resource checks. Outline and collision follow the wooden structural profile.

## Automated coverage

`RaisedCatwalkGameTests` covers:

- Native item placements across all five former coordinate residues for steel/wood poles and their heads
- Wood placement using all six click faces, both dry and waterlogged; NBT reload, unrelated side updates, and support removal
- Reversible support changes, preserved waterlogging, and installed-light protection
- Native route extension in all four horizontal directions using top and underside clicks
- Survival inset-light installation, save/load, removal, and exact item accounting with either hand above both wooden supports
- Existing numeric states 0–4, including unpaid legacy/creative lights, with their previous compatibility expectations retained
- Outline, collision, NBT and codec round trips, drops, and model selectors for all six variants
- An independent resource assertion that the wood model keeps the original deck, textures, and existing wooden pole profile

Run the full repository checks with Java 21:

```sh
./gradlew compileJava processResources test runGameTestServer build --stacktrace
```

## Client visual checklist

1. Place vertical straight treated-wood poles and pole heads, then place raised catwalks above them. Inspect the join from all sides and below.
2. Compare against thick/thin steel supports and the plain, frame, and inset-light variants. Confirm there are no missing textures, unintended geometry changes, or a gap between the support plate and grating.
3. Remove and replace wooden supports, save/reload the world, and repeat with waterlogged catwalks.
4. Install a paid inset light before adding the wooden pole; remove it and confirm one refund and a wooden connection.

Client appearance is a separate visual check; passing dedicated-server GameTests alone does not establish it.
