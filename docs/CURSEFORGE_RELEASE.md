# Guarded CurseForge publication

The repository can publish only the reviewed `v1.1.54-reconstructed` GitHub release asset described by `tools/release/curseforge_release_1.1.54.json`. The publisher downloads that asset and never builds, rewrites, renames, or replaces the JAR.

## Safety model

- GitHub source gates require a public, non-draft, non-prerelease release and exactly one manifest-named asset.
- Asset name, size, GitHub API digest, downloaded SHA-256, NeoForge mod ID, and embedded version must match the hash-pinned manifest.
- The UTF-8 changelog is hash-pinned.
- All public CurseForge file pages are checked with the service's zero-based `pageIndex` contract. An exact existing file is idempotent success; divergent or ambiguous version reuse is blocked.
- Dry-run does not receive, resolve, or transmit the CurseForge secret.
- A production run first authenticates and constructs the exact deterministic multipart request without posting. GitHub Actions must persist that upload intent as an immutable artifact. Intent and result use different files, so an interrupted publish step cannot overwrite or relabel the intent as a result.
- The publish phase reconstructs the request, verifies the persisted artifact ID/name and all request hashes, repeats public duplicate checks, then sends one non-retried POST.
- Each later production prepare reads the publication workflow history and the exact `Persist upload intent before any POST` step conclusion. If an earlier attempt reached that successful step but has no active result artifact, including after artifact expiry while matching run/job metadata remains queryable, the attempt is `UPLOAD_OUTCOME_UNKNOWN` and another POST is refused. This blocks rather than reconstructs an expired result, so a processing file ID cannot be recovered from workflow metadata alone.
- An intent without a result, a transport failure after POST, a server error after POST, invalid response JSON, a missing positive file ID, or an unexpected failure after upload code begins is `UPLOAD_OUTCOME_UNKNOWN`. A later run refuses another POST.
- A returned processing file ID is durably recorded and later polled without a CurseForge token or second POST.
- Once a positive file ID is durably recorded, resume does not let mutable project-level dependency aggregation block polling that exact accepted file. It still validates the immutable GitHub source, approved baseline file, and every relation/hash/metadata field on the resumed file before passing.
- Publication passes only after public file metadata, approved status, game-version labels, dependencies, size, SHA-256, mod ID, and version all match.

## Upload contract and residual platform gate

The request uses the documented `gameVersionNames` field and the documented nested relation shape:

```json
{
  "relations": {
    "projects": [
      {
        "projectID": 231951,
        "slug": "immersive-engineering",
        "type": "requiredDependency"
      }
    ]
  }
}
```

`gameVersionNames` is the complete upload-label contract (`Client`, `Server`, `1.21.1`, and `NeoForge`). The authenticated `/api/game/versions` preflight has a narrower purpose: it proves that the configured catalog-backed Minecraft version exists. The manifest therefore keeps `gameVersionLookupNames` as an explicit non-empty subset of the upload labels; for this release it resolves only `1.21.1`. A name can legitimately have records under multiple game-version types, so the publisher validates every matching positive numeric ID and binds the sorted unique ID set into both prepare and publish intent checks. Loader and environment labels remain in the upload request and final public readback, but are not incorrectly required to appear as records in the game-version catalog.

JEI is optional in NeoForge metadata and is not added to CurseForge metadata. The previous approved CurseForge file also exposes three `Include` relations for Engineers Decor, Engineers Tools, and Redstone Gauges and Switches. `Include` is not an accepted upload relation type, so those entries are not transmitted. Before upload, the publisher requires the live project-level dependency endpoint and the approved baseline file to expose all four expected relations. Historical live readback also found the same four relations on each of the four newest files. A real publication is accepted as complete only if all four appear on the new file; unit tests and historical inheritance cannot replace that post-upload proof.

`isMarkedForManualRelease` is explicitly `false`. This is deliberate: the documented API says `true` holds an approved file for a later manual release choice, but documents no API operation for making that held file public. This is a scope-derived decision from the user-authorized autonomous, no-computer-use release mandate; it waives an external pre-publication metadata quarantine. Internal source/hash/duplicate/intent gates remain fail-closed, but public readback is necessarily post-publication verification and can detect a platform-side metadata mismatch only after the file is public.

## GitHub Actions workflow

The `Publish CurseForge` workflow is manual-only, tag-scoped by a non-cancelling concurrency lock, and uses read-only `contents` and `actions` permissions. Third-party actions are pinned to full commit SHAs. Its run title distinguishes dry-run, publish, and resume attempts. The repository secret is injected only into prepare and one-POST publish steps; repository-administrator control of that secret, administrative deletion of workflow history, and GitHub's lack of an indefinite run/job-metadata retention guarantee remain external trust boundaries.

1. Run with `dry_run=true`. No CurseForge secret is injected.
2. For a real publication, the repository secret must be named `CURSEFORGE_API_TOKEN`; never paste its value into an issue, log, prompt, or workflow input.
3. Run with `dry_run=false`. The workflow persists an intent artifact before the upload phase.
4. If the result is `UPLOADED_PROCESSING`, rerun with that positive `resume_file_id`; this route receives no CurseForge token and cannot post.
5. If the result is `UPLOAD_OUTCOME_UNKNOWN`, investigate the existing CurseForge state. Do not bypass the durable-state block or upload manually.

## Local verification

```powershell
python -W error::ResourceWarning -m unittest discover -s tools/release -p 'test_*.py' -v
python tools/release/publish_curseforge.py --dry-run --tag v1.1.54-reconstructed --report curseforge-publication-report.json
```

The second command performs live public readbacks but does not upload. Production prepare/publish is intentionally orchestrated by GitHub Actions so the secret and durable state never enter a local command line.

## Proof levels

- `MOCKED`: local HTTP tests prove program logic and controlled failure handling only.
- `LIVE_DRY_RUN`: public GitHub and CurseForge preflight passed; no upload occurred.
- `UPLOAD_ACCEPTED`: CurseForge returned a positive file ID; public state may still be processing.
- `PUBLICATION_VERIFIED`: the public metadata, dependencies, approved status, redownload size/hash, mod ID, and version all passed.

## Controlled statuses

- `AUTOMATION_READY_DRY_RUN`: live public preflight passed; no upload occurred.
- `UPLOAD_INTENT_READY`: token and game-version access passed and the exact request is ready to persist; no upload occurred.
- `ALREADY_PUBLISHED`: one exact public file already matches; no upload occurred.
- `PUBLISHED_VERIFIED`: the one POST returned an ID and complete public readback passed.
- `RESUMED_PUBLICATION_VERIFIED`: a prior file ID became public and passed without another POST.
- `UPLOADED_PROCESSING`: CurseForge returned an ID but public proof is not yet available.
- `UPLOAD_OUTCOME_UNKNOWN`: the POST outcome cannot be proven; later POST attempts are blocked by active artifacts and retained workflow-run/job metadata.
- `BLOCKED_BY_MISSING_CURSEFORGE_API_TOKEN`: no authorized publication token is configured.
- `BLOCKED_BY_REMOTE_ARTIFACT_CONFLICT`: a public file for this version differs or is ambiguous.
- `CURSEFORGE_TOKEN_REJECTED`, `CURSEFORGE_TOKEN_FORBIDDEN`, or `CURSEFORGE_PROJECT_PERMISSION_DENIED`: configured authority was not accepted.
