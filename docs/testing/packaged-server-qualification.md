# IEDCTR 1.1.58 packaged dedicated-server qualification

## Status and limits

This is a production-server smoke harness, not a GameTest server and not a claim
that an authenticated player has tested the game. Unit tests do not install or
start Minecraft. A server verdict exists only after the guarded workflow finishes.

The candidate is immutable:

- Runtime source: `19c6bc7f8322002fd1d4710885d13f4482b4cb45`
- Version: `1.1.58-reconstructed`
- Runtime SHA-256: `bc77067f9f6e0c03f987123b490812d32aa9171ad388fcd87ecdf50777883fdb`
- Minecraft 1.21.1, Java 21, NeoForge **21.1.230** (the source's pinned baseline)
- Immersive Engineering `1.21.1-12.4.2-194`; JEI is optional and intentionally absent

The runtime is rebuilt in a separate exact-commit checkout; the qualification
harness can change without changing the candidate. The build must reproduce the
known runtime SHA-256. A mismatch stops before downloading/installing the server.
The script verifies embedded mod ID/version and byte-for-byte equality of packaged
LICENSE, NOTICE.md and CREDITS.md with the pinned source. The byte size, attribution
hashes and runtime digest are measured from the actual JAR and written to the
workflow summary. No byte-size value is invented from a previous release.

## Permission and trigger

Running Minecraft accepts the [Minecraft EULA](https://www.minecraft.net/en-us/eula)
and its incorporated [Microsoft Services Agreement](https://www.microsoft.com/servicesagreement).
The operator explicitly approved this isolated 1.1.58 server test after these
terms were disclosed. The temporary push trigger is limited to the exact branch
`dot/qualify-server-1.1.58`, in the original repository, and changes to the harness
or its workflow. It allows the test to run on a draft PR without merging it.
This is scoped consent for the pinned candidate, not blanket acceptance of future
versions or other services. No pull-request, schedule, or general-branch server
trigger exists. Remove the temporary branch trigger when the test is retired.

The manual path requires the exact acknowledgement string shown in the dispatch
form. It has no default acceptance. Inputs are passed as environment variables,
never interpolated into shell code. Empty/wrong acknowledgement fails before
runtime checkout/build, dependency downloads, or server setup. GitHub requires a
workflow-dispatch definition on the default branch before using that UI; the
scoped push path avoids requiring a merge just to run this qualification.

## Isolation and dependency integrity

The job uses a standard `ubuntu-24.04` GitHub-hosted runner. This repository is
public; standard public-repository runners are free under
[GitHub's billing documentation](https://docs.github.com/en/billing/concepts/product-billing/github-actions).
It does not select a paid larger runner or buy a service. Permissions are
`contents: read`; no secrets, persistent credentials, access changes, runtime
artifact uploads, world uploads, or Actions caches are requested. Gradle uses an
isolated temporary cache with Actions cache saving disabled.

The script downloads the NeoForge installer from the official Maven endpoint,
checks its SHA-256 against the official HTTPS sidecar, and uses the vendor's
`--installServer` plus `run.sh nogui` production route. See the
[official server installation guide](https://docs.neoforged.net/user/docs/server/).
The installer checksum is a vendor-integrity check collected during the run,
not a previously reviewed immutable dependency hash. The summary records it.

Immersive Engineering is downloaded from the project's existing Maven source
(`maven.blamejared.com`), at the exact coordinate already used by the pinned
build. Its bytes must match the original dependency resolved by that build in
the isolated Gradle cache. Embedded mod metadata is checked and its SHA-256 is
recorded. Redirects or unfamiliar download URLs fail closed. Mojang/NeoForge
runtime downloads are left to the official installer.

A fresh temporary directory contains the server and disposable world. The server
binds only `127.0.0.1`; online authentication stays enabled; RCON/query are disabled;
the whitelist is enforced and empty. No real player or external connection is
needed. Commands use the child process's stdin. No firewall/security settings are
changed. On completion/failure the newly created processes and temporary server
files are cleaned up; no existing server/world is touched.

## Reproducible checks and evidence

| Check | Required evidence |
| --- | --- |
| Packaged candidate | Exact source, runtime digest, embedded version, size, packaged attribution |
| Real server startup | Official installed production `run.sh` emits server-ready message |
| Runtime registration | Namespaced blocks can be placed and read by console conditions |
| Hatch redstone | Add/remove real redstone source; native neighbor changes produce open/powered states |
| Persistent fixtures | Five explicit hatch, ladder and two-segment gate states assert before saving |
| Save and stop | `save-all flush` completes, stop is observed, process exits zero |
| Same-world restart | Loaded query succeeds; all five saved states assert before any replacement |
| Loaded native behavior | Removing redstone closes saved hatch; removing lower gate removes linked upper segment |
| Log health | Entire output scanned, including shutdown, for ERROR/FATAL, missing classes and client-on-server failures |

Chunk loading is awaited before the fixture is created and again after restart.
Assertions use unique `say` markers emitted only after `execute if block` succeeds.
A missing marker, echoed input, early EOF, timeout, error, or nonzero exit fails.
The summary includes successful assertion conditions and complete console-log
hashes per phase. Complete successful phase logs are retained as Actions text output
so those hashes and markers can be audited. On failure the last console lines are
emitted for diagnosis. A pass is recorded only after disposable-directory cleanup.
Only text logs and summaries are retained; no runtime binaries are published.

Still unverified by this harness: real-player clicks and protection hooks,
client/server multiplayer synchronization, rendering, climbing/collision feel,
loot/drop behavior, and behavior with NeoForge versions other than 21.1.230.
A successful console smoke run must never close those client/manual gates or be
presented as a full release qualification.

## Offline validation

Run from the repository root:

```sh
python3 -W error::ResourceWarning -m unittest discover -s tools/server -p 'test_*.py' -v
python3 -m py_compile tools/server/qualify_packaged_server.py tools/server/test_qualify_packaged_server.py
```

These use the Python standard library only. Network/server actions are mocked;
fixture ZIPs are invented test data, not redistributed Minecraft binaries. The
ordinary Build workflow should run these alongside its existing release-tool unit
tests. Passing them proves safeguards and controller logic, not a server result.
