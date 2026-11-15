# Retained Crate V1 candidate

Artwork retained byte-for-byte from `labeled-crate-v1-static-exact.official-render.png` (1122x1402), SHA256 `9658f359d5675b13bc46ba5af12120f2ddcc7ef7f2b1bf563da4234f16132ea2`. Design direction: https://chatgpt.com/c/6ae94ffc-b25c-83e9-ac03-182f96d22aa9?messageId=134952a0-57ab-4691-beaf-4e30c395fcb0 . Concept approval is not runtime acceptance.

Preserve 54 storage, 1 frame-hint, 27 player, 9 hotbar, slot indices and server layout. Client factory alone chooses measured source anchors; server construction stays original. The screen follows existing transformed container routing, including drag deltas, unscaled wheel amounts and public JEI bounds. The special Ctrl+Shift transfer payload remains unchanged. Label editor deliberately unchanged, no replacement reference established.

Validation: 3 source/asset tests PASS. Source interior coordinates estimated conservatively from the viewed reference must receive independent pixel/anchor inspection. `CrateGeometryCheck.java` is a standalone dependency-free executable geometry check; NOT EXECUTED here because only a Java runtime, no javac, is available. Full mapped compilation and native qualification are PENDING. Do not mark ready on these source tests.

Required native gates on exact combined commit/JAR: actual ordinary crate open, every 91 slot pickup/return, frame limit1, forbidden nesting, repeated standard and Ctrl+Shift transfers, drag, number keys, carried stack and drop outside, reopen, resize/scales, EN/FR, public overlay bounds, unchanged storage/drop/persistence. Capture real latest JAR after implementation; old original-GUI captures do not qualify this change.
