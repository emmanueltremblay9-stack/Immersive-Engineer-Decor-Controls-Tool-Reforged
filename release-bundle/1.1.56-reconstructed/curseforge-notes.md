# 1.1.56-reconstructed

Localization expands from 12 to 120 locales, with 898 language keys and 223 manual pages per locale. Deterministic validation checks UTF-8, key and placeholder parity, manual coverage and selected semantic expectations. Tool descriptions and manual headings were corrected, including material-box usage, Japanese Muslee Bar naming, GUI indexes/subtitles and Arabic wrapping that preserves words.

Stimpack and sleeping-bag recipes now accept all 16 wool-carpet colors. The required GameTest suite increased from 188 to 189: **189/189 passing**. **31/31 mocked publisher tests** passed during artifact qualification; these tests validate publisher guards and do not constitute a live upload. Independent qualified builds produced byte-identical JARs, and post-merge CI passed on the release commit.

Compatibility: Minecraft **1.21.1**, Java **21**, NeoForge minimum **21.1.230**. Immersive Engineering **1.21.1-12.4.2-194** is required; JEI **19.32.0.359** is optional. Engineer's Decor, Engineer's Tools and Redstone Gauges and Switches are incorporated-source provenance, not runtime dependencies to install.

Native fluency for all locales and exhaustive visual/manual/resolution/UI-scale coverage are unverified. Earlier sampled client QA retained shared Hindi, Arabic and Thai rendering limitations whose exact upstream component attribution remains unverified. No new packaged client/dedicated-server runtime qualification is claimed; GameTests and builds do not replace it.

The file is the exact already public GitHub release JAR, unchanged:
`immersive_engineer_decor_controls_tool_reforged-1.1.56-reconstructed.jar` — **19,568,379 bytes**.

SHA-256: `fa7bde923433658a78dcf3792c9fc6af2e0b5212a7897e1b21c3323e7af809a4`.
