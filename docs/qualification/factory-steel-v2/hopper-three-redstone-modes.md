# Hopper three-mode tooltip and icon qualification

Test-driver-only extension of PR196. No production behavior, artwork, hitboxes or design changes.

The original native sequence captured inverted and ignored redstone tooltips, then clicked to normal solely to prepare the continuous/pulse test. Its clean screen named `normal` was not a normal-mode tooltip. This extension adds the missing normal-mode hover and requires exact synchronized states 3 (inverted+continuous), 6 (ignored+continuous), 2 (normal+continuous), then 0 (normal+pulse). All three existing control clicks remain real XTest actions, with exact server/client acknowledgement. No redstone state is seeded by the test driver.

Each redstone tooltip is followed by a separate unhovered screenshot at unchanged state, three screen render callbacks and server confirmation. The pointer returns to the actual control before clicking again. No production tooltip suppression or direct mouse-handler mutation is used. The validator independently pins EN/FR state descriptions and compares every 9x9 icon RGB pixel against the sprite from the exact ordinary JAR, alpha-composited over its opaque original underlay. RGB tolerance 2 allows GPU rounding, not icon permutation. Mutation tests reject swapped, duplicated and damaged icons. Actual native execution must validate this blend assumption; source tests alone do not.

The three historical sprites are retained; verifying their mapping does not claim the user finds their symbolism intuitive. This covers indication and switching, not transferred item counts under powered/unpowered conditions, every pulse combination, the reporter's modpack or issue22 as a whole. The issue remains open, with distribution and reporter retest separate. No merge or publication is authorized by this work.

Status: local development and verification; no new native success claimed until exact-head CI artifacts have been independently checked.
