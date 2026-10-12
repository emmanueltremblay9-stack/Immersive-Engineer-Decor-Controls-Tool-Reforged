# Guarded CurseForge publication

New publications use a reviewed **local-jar manifest** and distribute the runtime JAR through **CurseForge only**. The manual workflow builds privately from an exact source commit, verifies the resulting bytes against that manifest, and never creates a GitHub Release, downloads a GitHub Release asset, or uploads a runtime Actions artifact. Only sanitized JSON upload-intent/result records are persisted in Actions.

No next-release manifest, version bump, candidate hash, or qualification result is supplied by this pipeline change. Historical manifests remain unchanged. The publisher's legacy GitHub Release reader is retained for explicitly selected old manifests; the new workflow requires local mode before checking out or building runtime sources and cannot use that reader.

## Safety model

- Local source gates require a full manifest-pinned Git commit, a clean source checkout with that exact HEAD, and the manifest-named `build/libs` JAR. Wrong paths, symlinks, missing files, modified tracked files, and untracked source files fail closed.
- The manifest pins one `Build` workflow run ID and attempt. A live GitHub read must show a completed successful `push` run for that exact SHA, the expected repository and head repository, and `.github/workflows/build.yml`. A successful PR merge check, nearby commit, different workflow, skipped run, or later run attempt is insufficient. CI is checked before building and again in each publisher phase, including before POST.
- Asset name, private-copy size/SHA-256, NeoForge mod ID, and the version belonging to that exact mod entry must match the reviewed manifest. The checked snapshot supplies the upload bytes; there is no fallback download, repackaging, renaming, or hash override.
- The canonical manifest SHA-256 and exact source/CI identity are bound into the local-mode upload intent as well as the existing multipart hashes.
- The UTF-8 changelog is hash-pinned.
- All public CurseForge file pages are checked with the service's zero-based `pageIndex` contract. An exact existing file is idempotent success; divergent or ambiguous version reuse is blocked.
- Dry-run does not receive, resolve, or transmit the CurseForge secret.
- A production run first authenticates and constructs the exact deterministic multipart request without posting. GitHub Actions must persist that upload intent as an immutable artifact. Intent and result use different files, so an interrupted publish step cannot overwrite or relabel the intent as a result.
- The publish phase reconstructs the request, verifies the persisted artifact ID/name and all request hashes, repeats public duplicate checks, then sends one non-retried POST.
- Each later production prepare reads the publication workflow history and the exact `Persist upload intent before any POST` step conclusion. If an earlier attempt reached that successful step but has no active result artifact, including after artifact expiry while matching run/job metadata remains queryable, the attempt is `UPLOAD_OUTCOME_UNKNOWN` and another POST is refused. This blocks rather than reconstructs an expired result, so a processing file ID cannot be recovered from workflow metadata alone.
- An intent without a result, a transport failure after POST, a server error after POST, invalid response JSON, a missing positive file ID, or an unexpected failure after upload code begins is `UPLOAD_OUTCOME_UNKNOWN`. A later run refuses another POST.
- A returned processing file ID is durably recorded and later polled without a CurseForge token or second POST.
- Once a positive file ID is durably recorded, resume does not let mutable project-level dependency aggregation block polling that exact accepted file. It still validates the configured source (including the local bytes and exact CI for local mode), approved baseline file, and every relation/hash/metadata field on the resumed file before passing.
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

JEI is optional in NeoForge metadata and is not submitted as a CurseForge project relation. Engineer's Decor, Engineer's Tools, and Redstone Gauges and Switches were ported and incorporated into this single mod; they are provenance projects, not runtime dependencies. Their historical `Include` relations are therefore not transmitted or required by the 1.1.56 public-readback contract. Provenance remains recorded in `CREDITS.md`, `NOTICE.md`, the source metadata, and the project description. Immersive Engineering is the sole CurseForge `RequiredDependency`, and a real publication is accepted as complete only when that relation and every other hash, version, loader, game-version, status, and file-integrity field pass live readback.

`isMarkedForManualRelease` is explicitly `false`. This is deliberate: the documented API says `true` holds an approved file for a later manual release choice, but documents no API operation for making that held file public. This is a scope-derived decision from the user-authorized autonomous, no-computer-use release mandate; it waives an external pre-publication metadata quarantine. Internal source/hash/duplicate/intent gates remain fail-closed, but public readback is necessarily post-publication verification and can detect a platform-side metadata mismatch only after the file is public.

## Reviewed local source contract

Keep `schemaVersion: 1` and the existing `repository`, `release`, and `curseforge` sections. Add a top-level `source` object with exactly these fields:

- `mode`: the literal `local-jar`
- `commit`: the full lowercase 40-character SHA of the final runtime source, including its approved version and all intended fixes
- `buildRunId`: the positive integer ID of that commit's successful `Build` push run
- `buildRunAttempt`: the positive integer attempt returned by that run's current readback

The `release.assetName`, `assetSize`, `assetSha256`, embedded version, and changelog hash must be recorded from the genuinely qualified candidate, not inferred from a previous release or substituted from a fresh build. The changelog path is relative to the reviewed manifest/publisher checkout. The publication `tag` remains a stable idempotency label; no GitHub tag or Release needs to be created.

The manifest/publisher revision and runtime source revision are intentionally separate. Build and qualify the final runtime source first, then review the manifest that pins it. This avoids requiring a manifest to contain the SHA of its own commit. Historical manifests with no `source` section select legacy behavior only outside this workflow; unknown or malformed source sections always fail.

## GitHub Actions workflow

The `Publish CurseForge` workflow is manual-only, label-scoped by a non-cancelling concurrency lock, and uses read-only `contents` and `actions` permissions. Third-party actions are pinned to full commit SHAs. Its run title distinguishes dry-run, publish, and resume attempts. The repository secret is injected only into prepare and one-POST publish steps; repository-administrator control of that secret, administrative deletion of workflow history, and GitHub's lack of an indefinite run/job-metadata retention guarantee remain external trust boundaries.

1. Select the reviewed workflow/manifest revision and provide its repository-relative `manifest` path under `tools/release/` and the matching `tag`. Neither input has a release-specific default. The workflow rejects another repository, a legacy manifest, or unverified exact-source CI before building.
2. The workflow checks out the pinned runtime SHA separately into `runtime-source`, disables credential persistence and Gradle output caching for this job, and runs `bash ./gradlew clean build --no-build-cache`. No CurseForge token is available to this build step. Only a clean checkout's exact, manifest-matching JAR can proceed.
3. Run first with `dry_run=true`. This still rebuilds/validates private local bytes and live public inputs, but receives no CurseForge secret. Rebuilding must reproduce the reviewed hash and size; any drift blocks rather than silently replacing the manifest. This pipeline does not establish reproducibility by itself.
4. Before a real publication, complete the qualification gates below. The existing repository secret must be named `CURSEFORGE_API_TOKEN`; never paste its value into an issue, log, prompt, or workflow input.
5. Run with `dry_run=false`. The workflow rebuilds and verifies the same candidate, persists a JSON intent before the one-POST phase, then validates public CurseForge readback. The runtime JAR remains ephemeral on the runner.
6. If the result is `UPLOADED_PROCESSING`, rerun the same reviewed manifest with that positive `resume_file_id`; this route rebuilds/revalidates source and receives no CurseForge token, so it cannot post.
7. If the result is `UPLOAD_OUTCOME_UNKNOWN`, investigate the existing CurseForge state. Do not bypass the durable-state block or upload manually.

## Qualification still required before publication

Passing publisher tests or a dry-run does not authorize or prove release readiness. In particular:

- Review the distribution license and redistribution rights for the combined mod and bundled assets. Resolve the repository-MIT versus CurseForge-All-Rights-Reserved discrepancy tracked in [issue #34](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/issues/34), and preserve `LICENSE`, `NOTICE.md`, and upstream credits. This publisher does not resolve or waive that review.
- Qualify the **exact final version/commit/JAR**, with successful exact-head Build CI, dedicated-server testing, and the applicable in-game client/visual checks. Headless GameTests do not prove client rendering, placement feedback, or the user's full gameplay checklist.
- Privately identify the candidate bytes by SHA-256 and size, confirm its embedded NeoForge identity, prepare final release notes, and demonstrate that the workflow rebuild matches the reviewed bytes.
- Verify authority for the existing CurseForge project and retain the approved dependency/platform metadata contract. No GitHub release, merge, license change, or access change is authorized by this tooling.

See [the next-patch qualification checklist](releases/next-patch-qualification.md) for the current source evidence and remaining release gates. Do not copy an older runtime hash or reuse its published version as proof for the next patch.

## Local verification

Run the dependency-free tests with Python 3.11 or newer:

```sh
python -W error::ResourceWarning -m unittest discover -s tools/release -p 'test_*.py' -v
```

For an already reviewed manifest and private candidate, a local dry-run uses explicit arguments (replace the paths and label with reviewed values):

```sh
python tools/release/publish_curseforge.py --dry-run \
  --repo-root /path/to/manifest-checkout \
  --manifest tools/release/reviewed-manifest.json \
  --tag REVIEWED_PUBLICATION_LABEL --require-local-source \
  --source-root /path/to/clean-pinned-source \
  --local-jar /path/to/clean-pinned-source/build/libs/REVIEWED_RUNTIME_NAME.jar \
  --report /private/path/curseforge-publication-report.json
```

This performs read-only GitHub Actions/CurseForge checks without uploading. The local checkout's actual Git HEAD, not a CLI SHA claim, supplies provenance. Production prepare/publish remains orchestrated by GitHub Actions so credentials and durable state are handled there. Tests exercise both the legacy route and the new local route, including duplicate/intent/resume failures and explicit rejection of every GitHub Release request in local-mode fixtures.

## Proof levels

- `MOCKED`: local HTTP tests prove program logic and controlled failure handling only.
- `LIVE_DRY_RUN`: configured source/byte validation, exact CI where required, and live CurseForge preflight passed; no upload occurred.
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

Local-source validation additionally fails with controlled statuses including `LOCAL_SOURCE_REQUIRED`, `MANIFEST_SOURCE_INVALID`, `LOCAL_SOURCE_COMMIT_MISMATCH`, `LOCAL_SOURCE_CHECKOUT_DIRTY`, `LOCAL_JAR_MISSING`, `LOCAL_JAR_PATH_INVALID`, `LOCAL_JAR_SIZE_MISMATCH`, `LOCAL_JAR_HASH_MISMATCH`, and `SOURCE_BUILD_CI_NOT_VERIFIED`. None permits a fallback to a GitHub Release or an unreviewed JAR.
