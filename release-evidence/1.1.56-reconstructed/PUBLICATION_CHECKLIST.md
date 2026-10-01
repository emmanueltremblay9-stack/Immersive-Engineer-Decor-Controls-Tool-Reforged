# Future publication prerequisites for 1.1.56-reconstructed

Publication is NOT_PERFORMED in this preparation task.

- Review the preparation PR and its exact-head CI; merging requires separate authorization.
- Bind a separately authorized immutable tag/release to the qualified source commit and JAR. Read back name, version, bytes and SHA-256 before any downstream upload.
- Adapt the existing hash-pinned CurseForge publication configuration in a later authorized task. It currently targets only 1.1.55. Bind the new artifact, approved changelog and fresh public previous-file baseline; never reuse historical payload hashes.
- CurseForge project: 1555214. The only required upload dependency is Immersive Engineering project 231951. Incorporated source-project attribution remains CREDITS.md, NOTICE.md and metadata; no Include relation is required by the established contract. JEI is optional.
- Preserve duplicate detection, immutable intent, unknown-outcome stop and tokenless receipt recovery guards; inspect fresh public metadata and downloaded bytes after any separately authorized upload.
- Modrinth: BLOCKED_BY_MISSING_CONFIGURATION. Obtain verified project/dependency configuration and publication authorization before preparing a submission. Historical guessed IDs are not operational configuration.
- Retain linguistic and visual limitations. Do not describe resource parity, build or GameTests as exhaustive client rendering proof.
