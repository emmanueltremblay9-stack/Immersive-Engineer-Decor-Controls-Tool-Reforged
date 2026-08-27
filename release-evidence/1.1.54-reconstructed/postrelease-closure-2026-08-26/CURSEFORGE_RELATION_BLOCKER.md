# CurseForge relation blocker

## Confirmed public state

CurseForge accepted exactly one upload POST in workflow run 33043440815 and returned file ID 8744461. The file is public and approved (`status=4`) with the authorized filename/display name, release type, and labels. A fresh public redownload is 2,678,440 bytes with SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`; its NeoForge metadata contains mod ID `immersive_engineer_decor_controls_tool_reforged` and version `1.1.54-reconstructed`.

The public file and project dependency endpoints expose only:

- project 231951 / `immersive-engineering` / `RequiredDependency`

They do not expose the approved `Include` relations for:

- project 296686 / `redstone-gauges-and-switches`
- project 313866 / `engineers-decor`
- project 319716 / `engineers-tools`

## Why no automated repair was submitted

The official CurseForge upload/update contract accepts `embeddedLibrary`, `incompatible`, `optionalDependency`, `requiredDependency`, and `tool`. It does not accept `Include`. The official public API enum separately defines `EmbeddedLibrary` and `Include`, so replacing an intended public `Include` with `embeddedLibrary` is not an authoritative translation.

A metadata-only `update-file` implementation was prototyped locally against a fake endpoint, then rejected before commit or POST after independent review confirmed that its public postcondition was unsupported. Post-write readback would be too late because the production relation metadata might already be wrong.

No second upload POST and no relation-update POST were issued. The new replacement GitHub Actions secret was not consumed for this rejected repair path. Computer Use remains prohibited by the executable task mandate, so the legacy/dashboard relation editor was not used.

## Resolution gate

Closure requires one of these new authoritative capabilities, neither of which exists in the current mandate:

1. a documented CurseForge API operation or written CurseForge confirmation that can set public relation type `Include` on existing file 8744461; or
2. a future user mandate that explicitly supersedes the current Computer Use prohibition and authorizes the exact dashboard relation edit.

After either path, all four file relations must be read back publicly and the same file ID/JAR hash must still match. Do not upload another JAR to work around this metadata gap.

Official sources:

- [CurseForge Upload API](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-api)
- [CurseForge for Studios API — FileRelationType](https://docs.curseforge.com/rest-api/)

`CURSEFORGE_AUTOMATION_VERDICT: PASS`

`CURSEFORGE_PUBLICATION_VERDICT: BLOCKED_PUBLIC_RELATION_MISMATCH`

`BLOCKER: BLOCKED_BY_CURSEFORGE_INCLUDE_RELATION_API_GAP`
