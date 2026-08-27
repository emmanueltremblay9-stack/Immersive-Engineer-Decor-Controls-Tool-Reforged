# Independent code review

## Initial review

`REVIEW_GATE: FAIL`

The read-only reviewer identified these blockers in the initial publisher revision:

1. Upload relations used a list instead of the documented `relations.projects` object.
2. Public-file duplicate checks and workflow concurrency did not prevent a second POST after an accepted request with an ambiguous response.
3. Manual-release behavior was implicit and post-publication verification was not distinguished from quarantine.
4. Dry-run used names while publish used IDs, so request-byte parity was not proved.
5. Public pagination used `page` instead of the live zero-based `pageIndex` contract.
6. The workflow injected the CurseForge secret into dry-run, did not retain a sanitized report, and described the Actions archive digest as if it were the runtime JAR hash.

During re-review, the reviewer found one additional workflow defect: Python correctly returned `ALREADY_PUBLISHED` or a verified automatic resume, but the prepare step accepted only `UPLOAD_INTENT_READY` and converted those terminal successes into workflow failure.

## Corrections submitted for re-review

- Metadata now uses `gameVersionNames`, explicit `isMarkedForManualRelease=false`, and `relations.projects` in every mode.
- Dry-run, prepare, and publish reconstruct identical metadata and multipart bytes; tests compare all three hashes and sizes.
- An authenticated prepare phase performs no POST and emits an exact upload-intent report.
- GitHub Actions persists the intent artifact before publish. Publish verifies the exact artifact ID/name and request hashes before one non-retried POST.
- Prior unmatched intent and post-POST transport, server, invalid-JSON, or missing-ID ambiguity become `UPLOAD_OUTCOME_UNKNOWN` and block another POST.
- Positive processing IDs are durably recorded and resumed without a CurseForge token or second upload.
- File enumeration uses bounded zero-based `pageIndex` pagination.
- Dry-run has no CurseForge secret in its environment. Sanitized intent/result reports are retained as workflow artifacts.
- Build CI records the runtime JAR SHA-256 separately from the GitHub Actions transport-archive digest.
- Prepare now emits `post_required=false` for `ALREADY_PUBLISHED` and `RESUMED_PUBLICATION_VERIFIED`; intent persistence and publish are skipped. Regression assertions cover those terminal-success reports.
- Live project-level dependencies are now a mandatory pre-upload match, in addition to baseline-file and post-upload file relations.
- The documentation explicitly states that `false` enables autonomous release, while `true` would require a later manual action for which the official documentation exposes no API endpoint. Public readback is post-publication verification.
- Hardened suite at that review point: 22/22 tests passed; actionlint exit 0; live dry-run exit 0.

## Second re-review findings and corrections

The next read-only review confirmed the relation, terminal-success, and request-parity corrections but found two critical durable-state gaps: the same path could let a stale `UPLOAD_INTENT_READY` report be persisted as a result after interruption, and 90-day artifact expiry could erase the only evidence that a prior POST might have occurred.

- The workflow now uses distinct `PREPARE_REPORT_PATH` and `RESULT_REPORT_PATH` files. Only the result path can produce a result artifact, and `UPLOAD_INTENT_READY` is rejected as a result status.
- The publisher now reads prior runs of `publish-curseforge.yml`, all attempts, their jobs, and the exact successful `Persist upload intent before any POST` step. If that proof exists without an active result artifact, including after expiry, preparation returns `UPLOAD_OUTCOME_UNKNOWN` before token validation or POST.
- Regression tests cover stale intent-as-result, missing artifacts, expired artifacts, and valid processing-state resume.

The following frozen read-only pass confirmed those two fixes and then identified one adjacent post-boundary defect: an unexpected exception after a positive upload response during public polling could lose the file ID and escape as `UNEXPECTED_PUBLISHER_FAILURE`.

- The entire upload-plus-public-poll block now maps unexpected exceptions to `UPLOAD_OUTCOME_UNKNOWN`.
- The positive file ID is written into the partial report before polling begins.
- A dedicated regression injects an unexpected polling failure and proves one POST, preserved file ID, conflict exit, and no exception-text disclosure.
- Durability wording is now conditional on matching GitHub run/job metadata remaining queryable; administrative history deletion is an external trust boundary, and expired result metadata cannot recover a processing file ID.
- Current suite: 27/27 tests passed; actionlint exit 0; live dry-run exit 0; gitleaks found no leaks.

## Historical pre-publication read-only verdict

No blocking finding remained. The reviewer independently matched the manifest hashes to the current publisher, test module, and workflow; reran 27/27 tests and the live dry run without `CURSEFORGE_API_TOKEN`; confirmed file-ID preservation and `UPLOAD_OUTCOME_UNKNOWN` after an unexpected post-ID polling failure; and directly verified the separate intent/result orchestration.

Residual nonblocking gaps are the conditional GitHub metadata horizon, absence of a dedicated static unit test for embedded workflow scripts, and the still-unperformed real upload/public readback.

`PRE_PUBLICATION_FINAL_REVIEW_GATE: PASS`

## Catalog-lookup correction re-review

After merged run 33041227322 stopped before intent persistence or POST, the reviewer inspected the four-file correction that separates the complete upload labels from the authenticated catalog lookup. The reviewer confirmed that `gameVersionLookupNames` is a validated non-empty subset, prepare and publish bind the same resolved map into durable intent validation, multipart metadata and final public readback retain all four labels, and unresolved catalog records still fail before POST. The reviewer reran 28/28 tests, a live dry run without `CURSEFORGE_API_TOKEN`, and `git diff --check`.

`PREFLIGHT_FIX_REVIEW_GATE: PASS`

## Multi-record catalog correction re-review

The live diagnostic established an ambiguous exact-name result without crossing the intent-persistence boundary. The reviewer confirmed that the correction accepts multiple records only when every ID is a positive non-boolean integer, deduplicates and sorts IDs deterministically, persists that mapping in prepare, recomputes and compares it in publish, and preserves the single-POST/token-secrecy controls. The 28-test suite covers sorted multi-record results, missing records, invalid IDs, zero POST on failures, and the verified one-POST success path.

`MULTI_RECORD_FIX_REVIEW_GATE: PASS`

## Accepted-file resume correction review

After file 8744461 became public, CurseForge-token-free resume first encountered mutable project-level dependency drift. The reviewer required accepted-file verification to avoid that aggregate without weakening any immutable or exact-file gate. An initial correction covered explicit resume but missed automatic durable resume; a second ordering briefly moved durable-history reconciliation too early and could have preempted definitive `ALREADY_PUBLISHED` proof.

The final reviewed ordering is:

1. explicit positive resume validates that exact file directly and cannot POST;
2. otherwise, one exact public duplicate is authoritative before durable-history reconciliation;
3. a durable processing file ID skips only mutable project aggregation;
4. approved baseline-file relations and exact target filename/display name, status, labels, relations, size, hash, mod ID, and version remain mandatory;
5. project aggregation remains mandatory before every path on which a new upload can still occur.

The final 29-test suite includes explicit no-list resume, project-drift durable resume, exact-public precedence over stale history, and zero-POST assertions. Python compilation and `git diff --check` passed. No update-file endpoint or repair workflow remains.

`ACCEPTED_FILE_RESUME_REVIEW_GATE: PASS`

## Rejected relation-repair review

The reviewer compared the proposed `Include` to `embeddedLibrary` conversion against current official CurseForge contracts. The public API enum defines `EmbeddedLibrary` and `Include` as distinct values, while the documented upload/update write enum accepts `embeddedLibrary` but not `Include`. Therefore the proposed metadata update could not safely guarantee the approved public `Include` postcondition.

The reviewer required a fail-closed stop before POST. The prototype, tests, workflow, and documentation claims were removed. No relation update and no second upload were attempted.

`RELATION_REPAIR_REVIEW_GATE: BLOCKED_BY_UNPROVEN_RELATION_TRANSLATION`

`FINAL_REVIEW_GATE: PASS_FOR_RESUME_FIX`

`PUBLICATION_COMPLETION_REVIEW_GATE: BLOCKED_PUBLIC_RELATION_MISMATCH`
