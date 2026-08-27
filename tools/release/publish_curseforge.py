#!/usr/bin/env python3
"""Deterministic, fail-closed CurseForge publisher for the 1.1.54 release."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import tempfile
import time
from typing import Any
import urllib.error
import urllib.parse
import urllib.request
import zipfile


EXIT_OK = 0
EXIT_TOKEN_MISSING = 3
EXIT_PROCESSING = 4
EXIT_AUTHORIZATION = 5
EXIT_CONFLICT = 6
EXIT_VALIDATION = 7

OFFICIAL_GITHUB_API = "https://api.github.com"
OFFICIAL_CURSEFORGE_PUBLIC_API = "https://www.curseforge.com/api/v1"
OFFICIAL_CURSEFORGE_UPLOAD_API = "https://minecraft.curseforge.com"
USER_AGENT = "IEDCT-CurseForge-Publisher/1"
PUBLICATION_WORKFLOW_FILE = "publish-curseforge.yml"
PERSIST_INTENT_STEP_NAME = "Persist upload intent before any POST"

RELEASE_TYPE_NUMERIC = {"release": 1, "beta": 2, "alpha": 3}
SUPPORTED_UPLOAD_RELATIONS = {
    "embeddedLibrary",
    "incompatible",
    "optionalDependency",
    "requiredDependency",
    "tool",
}


class PublicationError(RuntimeError):
    def __init__(self, status: str, message: str, exit_code: int = EXIT_VALIDATION):
        super().__init__(message)
        self.status = status
        self.message = message
        self.exit_code = exit_code


class HttpStatusError(PublicationError):
    def __init__(self, label: str, status_code: int):
        super().__init__(
            "HTTP_REQUEST_FAILED",
            f"{label} returned HTTP {status_code}",
            EXIT_VALIDATION,
        )
        self.label = label
        self.status_code = status_code


def canonical_json(value: Any) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def safe_repo_path(repo_root: Path, relative: str) -> Path:
    candidate = (repo_root / relative).resolve()
    root = repo_root.resolve()
    try:
        candidate.relative_to(root)
    except ValueError as exc:
        raise PublicationError(
            "MANIFEST_PATH_ESCAPES_REPOSITORY",
            f"Manifest path is outside the repository: {relative}",
        ) from exc
    return candidate


def write_report(path: Path | None, report: dict[str, Any]) -> None:
    if path is None:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


class HttpClient:
    """Small urllib transport that never includes request headers in errors."""

    def __init__(self, timeout: float = 30.0, get_attempts: int = 3):
        self.timeout = timeout
        self.get_attempts = max(1, get_attempts)

    def _request(
        self,
        method: str,
        url: str,
        *,
        label: str,
        headers: dict[str, str] | None = None,
        body: bytes | None = None,
        retry_safe: bool,
    ) -> bytes:
        merged_headers = {"User-Agent": USER_AGENT}
        if headers:
            merged_headers.update(headers)
        attempts = self.get_attempts if retry_safe else 1
        for attempt in range(1, attempts + 1):
            request = urllib.request.Request(
                url,
                data=body,
                headers=merged_headers,
                method=method,
            )
            try:
                with urllib.request.urlopen(request, timeout=self.timeout) as response:
                    return response.read()
            except urllib.error.HTTPError as exc:
                status_code = exc.code
                exc.close()
                if retry_safe and status_code >= 500 and attempt < attempts:
                    time.sleep(min(attempt, 2))
                    continue
                raise HttpStatusError(label, status_code) from None
            except (urllib.error.URLError, TimeoutError, OSError):
                if retry_safe and attempt < attempts:
                    time.sleep(min(attempt, 2))
                    continue
                raise PublicationError(
                    "HTTP_TRANSPORT_FAILED",
                    f"{label} failed after {attempt} attempt(s)",
                ) from None
        raise AssertionError("unreachable")

    def get_json(
        self,
        url: str,
        *,
        label: str,
        headers: dict[str, str] | None = None,
    ) -> Any:
        raw = self._request(
            "GET",
            url,
            label=label,
            headers={"Accept": "application/json", **(headers or {})},
            retry_safe=True,
        )
        try:
            return json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise PublicationError(
                "INVALID_JSON_RESPONSE",
                f"{label} did not return valid JSON",
            ) from exc

    def download(
        self,
        url: str,
        destination: Path,
        *,
        label: str,
        headers: dict[str, str] | None = None,
    ) -> None:
        raw = self._request(
            "GET",
            url,
            label=label,
            headers=headers,
            retry_safe=True,
        )
        destination.write_bytes(raw)

    def post_json(
        self,
        url: str,
        body: bytes,
        *,
        content_type: str,
        label: str,
        headers: dict[str, str] | None = None,
    ) -> Any:
        raw = self._request(
            "POST",
            url,
            label=label,
            headers={
                "Accept": "application/json",
                "Content-Type": content_type,
                **(headers or {}),
            },
            body=body,
            retry_safe=False,
        )
        try:
            return json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise PublicationError(
                "INVALID_UPLOAD_RESPONSE",
                "CurseForge upload did not return valid JSON",
            ) from exc


def load_manifest(path: Path) -> dict[str, Any]:
    try:
        manifest = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise PublicationError("MANIFEST_INVALID", "Release manifest is unreadable") from exc
    if manifest.get("schemaVersion") != 1:
        raise PublicationError("MANIFEST_SCHEMA_UNSUPPORTED", "Expected schemaVersion 1")
    for section in ("repository", "release", "curseforge"):
        if not isinstance(manifest.get(section), dict):
            raise PublicationError("MANIFEST_INVALID", f"Missing manifest section: {section}")

    release = manifest["release"]
    curseforge = manifest["curseforge"]
    required_release = (
        "tag",
        "version",
        "modId",
        "assetName",
        "assetSize",
        "assetSha256",
        "changelogPath",
        "changelogSha256",
    )
    for key in required_release:
        if release.get(key) in (None, ""):
            raise PublicationError("MANIFEST_INVALID", f"Missing release.{key}")
    for key in ("assetSha256", "changelogSha256"):
        if not re.fullmatch(r"[0-9a-f]{64}", str(release[key])):
            raise PublicationError("MANIFEST_INVALID", f"release.{key} is not lowercase SHA-256")
    if not isinstance(release["assetSize"], int) or release["assetSize"] <= 0:
        raise PublicationError("MANIFEST_INVALID", "release.assetSize must be positive")
    if curseforge.get("releaseType") not in RELEASE_TYPE_NUMERIC:
        raise PublicationError("MANIFEST_INVALID", "Unsupported CurseForge releaseType")
    if not isinstance(curseforge.get("isMarkedForManualRelease"), bool):
        raise PublicationError(
            "MANIFEST_INVALID",
            "curseforge.isMarkedForManualRelease must be an explicit boolean",
        )
    names = curseforge.get("gameVersionNames")
    if not isinstance(names, list) or not names or len(set(names)) != len(names):
        raise PublicationError("MANIFEST_INVALID", "gameVersionNames must be unique")
    for relation in curseforge.get("uploadRelations", []):
        if relation.get("type") not in SUPPORTED_UPLOAD_RELATIONS:
            raise PublicationError(
                "MANIFEST_UNSUPPORTED_UPLOAD_RELATION",
                f"Unsupported upload relation type: {relation.get('type')}",
            )
        if not relation.get("projectID") or not relation.get("slug"):
            raise PublicationError("MANIFEST_INVALID", "Upload relation is incomplete")
    expected = curseforge.get("expectedPublicRelations")
    if not isinstance(expected, list) or not expected:
        raise PublicationError("MANIFEST_INVALID", "Expected public relations are required")
    return manifest


def build_multipart(
    metadata: dict[str, Any], asset_name: str, asset_bytes: bytes, asset_sha256: str
) -> tuple[bytes, str]:
    metadata_bytes = canonical_json(metadata)
    seed = metadata_bytes + b"\0" + asset_sha256.encode("ascii")
    boundary = "----IEDCTCurseForge" + sha256_bytes(seed)[:32]
    boundary_bytes = boundary.encode("ascii")
    if boundary_bytes in metadata_bytes or boundary_bytes in asset_bytes:
        raise PublicationError("MULTIPART_BOUNDARY_COLLISION", "Multipart boundary collision")
    crlf = b"\r\n"
    body = b"".join(
        [
            b"--" + boundary_bytes + crlf,
            b'Content-Disposition: form-data; name="metadata"' + crlf,
            b"Content-Type: application/json; charset=utf-8" + crlf + crlf,
            metadata_bytes + crlf,
            b"--" + boundary_bytes + crlf,
            (
                'Content-Disposition: form-data; name="file"; filename="'
                + asset_name
                + '"'
            ).encode("utf-8")
            + crlf,
            b"Content-Type: application/java-archive" + crlf + crlf,
            asset_bytes + crlf,
            b"--" + boundary_bytes + b"--" + crlf,
        ]
    )
    return body, f"multipart/form-data; boundary={boundary}"


class Publisher:
    def __init__(
        self,
        repo_root: Path,
        manifest: dict[str, Any],
        *,
        http: HttpClient | None = None,
        github_api: str = OFFICIAL_GITHUB_API,
        curseforge_public_api: str = OFFICIAL_CURSEFORGE_PUBLIC_API,
        curseforge_upload_api: str = OFFICIAL_CURSEFORGE_UPLOAD_API,
    ):
        self.repo_root = repo_root.resolve()
        self.manifest = manifest
        self.http = http or HttpClient()
        self.github_api = github_api.rstrip("/")
        self.public_api = curseforge_public_api.rstrip("/")
        self.upload_api = curseforge_upload_api.rstrip("/")
        self.release = manifest["release"]
        self.cf = manifest["curseforge"]
        self.repository = manifest["repository"]

    def _github_headers(self, github_token: str) -> dict[str, str]:
        headers = {
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        }
        if github_token:
            headers["Authorization"] = f"Bearer {github_token}"
        return headers

    def _github_release(self, github_token: str) -> dict[str, Any]:
        owner = urllib.parse.quote(self.repository["owner"], safe="")
        name = urllib.parse.quote(self.repository["name"], safe="")
        tag = urllib.parse.quote(self.release["tag"], safe="")
        url = f"{self.github_api}/repos/{owner}/{name}/releases/tags/{tag}"
        result = self.http.get_json(
            url,
            label="GitHub release readback",
            headers=self._github_headers(github_token),
        )
        if not isinstance(result, dict):
            raise PublicationError("GITHUB_RELEASE_INVALID", "GitHub release response is invalid")
        if result.get("tag_name") != self.release["tag"]:
            raise PublicationError("GITHUB_RELEASE_TAG_MISMATCH", "GitHub tag mismatch")
        if result.get("draft") is not False or result.get("prerelease") is not False:
            raise PublicationError(
                "GITHUB_RELEASE_NOT_PUBLIC",
                "GitHub release must be public, non-draft, and non-prerelease",
            )
        return result

    def _download_and_validate_asset(
        self, work_dir: Path, github_token: str
    ) -> tuple[Path, dict[str, Any], str]:
        release_info = self._github_release(github_token)
        candidates = [
            asset
            for asset in release_info.get("assets", [])
            if asset.get("name") == self.release["assetName"]
        ]
        if len(candidates) != 1:
            raise PublicationError(
                "GITHUB_ASSET_CARDINALITY_MISMATCH",
                "Expected exactly one canonical GitHub release asset",
            )
        asset = candidates[0]
        expected_digest = "sha256:" + self.release["assetSha256"]
        if asset.get("size") != self.release["assetSize"]:
            raise PublicationError("GITHUB_ASSET_SIZE_MISMATCH", "GitHub asset size mismatch")
        if str(asset.get("digest", "")).lower() != expected_digest:
            raise PublicationError("GITHUB_ASSET_DIGEST_MISMATCH", "GitHub asset digest mismatch")
        download_url = asset.get("browser_download_url")
        if not isinstance(download_url, str) or not download_url:
            raise PublicationError("GITHUB_ASSET_URL_MISSING", "GitHub asset URL is missing")
        destination = work_dir / self.release["assetName"]
        self.http.download(
            download_url,
            destination,
            label="GitHub release asset download",
        )
        actual_size = destination.stat().st_size
        actual_sha = sha256_file(destination)
        if actual_size != self.release["assetSize"]:
            raise PublicationError("DOWNLOADED_ASSET_SIZE_MISMATCH", "Downloaded asset size mismatch")
        if actual_sha != self.release["assetSha256"]:
            raise PublicationError("DOWNLOADED_ASSET_HASH_MISMATCH", "Downloaded asset hash mismatch")
        self._validate_jar_identity(destination)
        return destination, asset, actual_sha

    def _validate_jar_identity(self, jar_path: Path) -> None:
        try:
            with zipfile.ZipFile(jar_path) as archive:
                metadata = archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
        except (OSError, KeyError, UnicodeDecodeError, zipfile.BadZipFile) as exc:
            raise PublicationError(
                "JAR_METADATA_INVALID",
                "Runtime JAR lacks readable NeoForge metadata",
            ) from exc
        mod_ids = re.findall(r'^\s*modId\s*=\s*"([^"]+)"', metadata, re.MULTILINE)
        versions = re.findall(r'^\s*version\s*=\s*"([^"]+)"', metadata, re.MULTILINE)
        if self.release["modId"] not in mod_ids:
            raise PublicationError("JAR_MOD_ID_MISMATCH", "Runtime JAR mod ID mismatch")
        if self.release["version"] not in versions:
            raise PublicationError("JAR_VERSION_MISMATCH", "Runtime JAR version mismatch")

    def _changelog(self) -> str:
        path = safe_repo_path(self.repo_root, self.release["changelogPath"])
        if not path.is_file():
            raise PublicationError("CHANGELOG_MISSING", "Configured changelog does not exist")
        raw = path.read_bytes()
        if sha256_bytes(raw) != self.release["changelogSha256"]:
            raise PublicationError("CHANGELOG_HASH_MISMATCH", "Changelog hash mismatch")
        try:
            return raw.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise PublicationError("CHANGELOG_ENCODING_INVALID", "Changelog is not UTF-8") from exc

    def _public_file(self, file_id: int) -> dict[str, Any]:
        project = self.cf["projectId"]
        result = self.http.get_json(
            f"{self.public_api}/mods/{project}/files/{file_id}",
            label=f"CurseForge public file {file_id}",
        )
        data = result.get("data") if isinstance(result, dict) else None
        if not isinstance(data, dict):
            raise PublicationError("CURSEFORGE_PUBLIC_FILE_INVALID", "Public file response is invalid")
        return data

    def _public_relations(self, file_id: int) -> list[dict[str, Any]]:
        project = self.cf["projectId"]
        result = self.http.get_json(
            f"{self.public_api}/mods/{project}/files/{file_id}/dependencies",
            label=f"CurseForge public dependencies {file_id}",
        )
        data = result.get("data") if isinstance(result, dict) else None
        if not isinstance(data, list):
            raise PublicationError(
                "CURSEFORGE_PUBLIC_DEPENDENCIES_INVALID",
                "Public dependency response is invalid",
            )
        return data

    def _public_project_relations(self) -> list[dict[str, Any]]:
        project = self.cf["projectId"]
        result = self.http.get_json(
            f"{self.public_api}/mods/{project}/dependencies?pageIndex=0&pageSize=50",
            label="CurseForge public project dependencies",
        )
        data = result.get("data") if isinstance(result, dict) else None
        pagination = result.get("pagination") if isinstance(result, dict) else None
        if not isinstance(data, list) or not isinstance(pagination, dict):
            raise PublicationError(
                "CURSEFORGE_PROJECT_DEPENDENCIES_INVALID",
                "Public project dependency response is invalid",
            )
        if (
            pagination.get("index") != 0
            or pagination.get("totalCount") != len(data)
            or len(data) > 50
        ):
            raise PublicationError(
                "CURSEFORGE_PROJECT_DEPENDENCIES_PAGINATION_INVALID",
                "Public project dependency pagination is incomplete or inconsistent",
            )
        return data

    @staticmethod
    def _relation_key(relation: dict[str, Any], public: bool) -> tuple[int, str, str]:
        project_key = "id" if public else "projectId"
        return (
            int(relation.get(project_key, 0)),
            str(relation.get("slug", "")),
            str(relation.get("type", "")),
        )

    def _validate_expected_relations(self, file_id: int) -> list[dict[str, Any]]:
        actual = sorted(self._relation_key(item, True) for item in self._public_relations(file_id))
        expected = sorted(
            self._relation_key(item, False) for item in self.cf["expectedPublicRelations"]
        )
        if actual != expected:
            raise PublicationError(
                "CURSEFORGE_RELATION_MISMATCH",
                "CurseForge dependency relation readback differs from the approved baseline",
            )
        return [
            {"projectId": project_id, "slug": slug, "type": relation_type}
            for project_id, slug, relation_type in actual
        ]

    def _validate_expected_project_relations(self) -> list[dict[str, Any]]:
        actual = sorted(
            self._relation_key(item, True) for item in self._public_project_relations()
        )
        expected = sorted(
            self._relation_key(item, False) for item in self.cf["expectedPublicRelations"]
        )
        if actual != expected:
            raise PublicationError(
                "CURSEFORGE_PROJECT_RELATION_MISMATCH",
                "CurseForge project dependency relations differ from the approved baseline",
                EXIT_CONFLICT,
            )
        return [
            {"projectId": project_id, "slug": slug, "type": relation_type}
            for project_id, slug, relation_type in actual
        ]

    def _validate_previous_public_baseline(self) -> dict[str, Any]:
        previous_id = int(self.cf["previousPublicFileId"])
        previous = self._public_file(previous_id)
        if set(previous.get("gameVersions", [])) != set(self.cf["gameVersionNames"]):
            raise PublicationError(
                "CURSEFORGE_BASELINE_GAME_VERSIONS_DRIFTED",
                "Previous public file game-version metadata drifted",
            )
        expected_release_type = RELEASE_TYPE_NUMERIC[self.cf["releaseType"]]
        if previous.get("releaseType") != expected_release_type:
            raise PublicationError(
                "CURSEFORGE_BASELINE_RELEASE_TYPE_DRIFTED",
                "Previous public file release type drifted",
            )
        return {
            "previousFileId": previous_id,
            "previousFileRelations": self._validate_expected_relations(previous_id),
            "projectRelations": self._validate_expected_project_relations(),
        }

    def _list_public_files(self) -> list[dict[str, Any]]:
        project = self.cf["projectId"]
        collected: list[dict[str, Any]] = []
        seen_ids: set[int] = set()
        for page_index in range(0, 100):
            result = self.http.get_json(
                f"{self.public_api}/mods/{project}/files?pageIndex={page_index}&pageSize=50&sortBy=dateCreated&sortOrder=desc",
                label=f"CurseForge public file list pageIndex {page_index}",
            )
            data = result.get("data") if isinstance(result, dict) else None
            if not isinstance(data, list):
                raise PublicationError("CURSEFORGE_FILE_LIST_INVALID", "Public file list is invalid")
            for item in data:
                if not isinstance(item, dict) or not isinstance(item.get("id"), int):
                    raise PublicationError(
                        "CURSEFORGE_FILE_LIST_INVALID",
                        "Public file list contains an invalid item",
                    )
                file_id = item["id"]
                if file_id in seen_ids:
                    raise PublicationError(
                        "CURSEFORGE_FILE_LIST_PAGINATION_INVALID",
                        "Public file list repeated a file across pages",
                    )
                seen_ids.add(file_id)
                collected.append(item)
            pagination = result.get("pagination") if isinstance(result, dict) else None
            if not isinstance(pagination, dict):
                raise PublicationError(
                    "CURSEFORGE_FILE_LIST_PAGINATION_INVALID",
                    "Public file list omitted required pagination metadata",
                )
            index = pagination.get("index")
            total = pagination.get("totalCount")
            if index != page_index or not isinstance(total, int) or total < 0:
                raise PublicationError(
                    "CURSEFORGE_FILE_LIST_PAGINATION_INVALID",
                    "Public file pagination metadata did not match the request",
                )
            if len(collected) >= total:
                return collected
            if not data:
                raise PublicationError(
                    "CURSEFORGE_FILE_LIST_PAGINATION_INVALID",
                    "Public file pagination ended before totalCount",
                )
        raise PublicationError(
            "CURSEFORGE_FILE_LIST_PAGINATION_LIMIT",
            "Public file list exceeded the bounded pagination limit",
        )

    def _download_public_file(self, file_id: int, destination: Path) -> str:
        project = self.cf["projectId"]
        self.http.download(
            f"{self.public_api}/mods/{project}/files/{file_id}/download",
            destination,
            label=f"CurseForge public file download {file_id}",
        )
        return sha256_file(destination)

    def _validate_public_release(self, file_id: int, work_dir: Path) -> dict[str, Any]:
        data = self._public_file(file_id)
        expected_type = RELEASE_TYPE_NUMERIC[self.cf["releaseType"]]
        exact_fields = {
            "fileName": self.release["assetName"],
            "displayName": self.cf["displayName"],
            "fileLength": self.release["assetSize"],
            "releaseType": expected_type,
        }
        for key, expected in exact_fields.items():
            if data.get(key) != expected:
                raise PublicationError(
                    "CURSEFORGE_PUBLIC_METADATA_MISMATCH",
                    f"CurseForge public {key} readback mismatch",
                )
        if data.get("status") != 4:
            raise PublicationError(
                "CURSEFORGE_PUBLIC_STATUS_NOT_APPROVED",
                "CurseForge file is visible but not approved",
            )
        if set(data.get("gameVersions", [])) != set(self.cf["gameVersionNames"]):
            raise PublicationError(
                "CURSEFORGE_PUBLIC_GAME_VERSIONS_MISMATCH",
                "CurseForge game-version readback mismatch",
            )
        relations = self._validate_expected_relations(file_id)
        public_jar = work_dir / f"curseforge-{file_id}.jar"
        public_sha = self._download_public_file(file_id, public_jar)
        if public_jar.stat().st_size != self.release["assetSize"]:
            raise PublicationError(
                "CURSEFORGE_PUBLIC_SIZE_MISMATCH",
                "CurseForge public redownload size mismatch",
            )
        if public_sha != self.release["assetSha256"]:
            raise PublicationError(
                "CURSEFORGE_PUBLIC_HASH_MISMATCH",
                "CurseForge public redownload hash mismatch",
                EXIT_CONFLICT,
            )
        self._validate_jar_identity(public_jar)
        return {
            "fileId": file_id,
            "projectId": data.get("projectId"),
            "fileName": data.get("fileName"),
            "displayName": data.get("displayName"),
            "releaseType": data.get("releaseType"),
            "status": data.get("status"),
            "gameVersions": sorted(data.get("gameVersions", [])),
            "relations": relations,
            "size": public_jar.stat().st_size,
            "sha256": public_sha,
        }

    def _find_existing_release(self, work_dir: Path) -> dict[str, Any] | None:
        version = self.release["version"]
        candidates = []
        for item in self._list_public_files():
            names = (str(item.get("fileName", "")), str(item.get("displayName", "")))
            if self.release["assetName"] in names or any(version in name for name in names):
                candidates.append(item)
        if not candidates:
            return None
        matching: list[int] = []
        divergent: list[int] = []
        for item in candidates:
            file_id = int(item.get("id", 0))
            if file_id <= 0:
                divergent.append(file_id)
                continue
            path = work_dir / f"candidate-{file_id}.jar"
            actual_sha = self._download_public_file(file_id, path)
            if (
                item.get("fileName") == self.release["assetName"]
                and item.get("displayName") == self.cf["displayName"]
                and path.stat().st_size == self.release["assetSize"]
                and actual_sha == self.release["assetSha256"]
            ):
                matching.append(file_id)
            else:
                divergent.append(file_id)
        if divergent or len(matching) != 1:
            raise PublicationError(
                "BLOCKED_BY_REMOTE_ARTIFACT_CONFLICT",
                "A divergent or ambiguous CurseForge file already uses this release version",
                EXIT_CONFLICT,
            )
        return self._validate_public_release(matching[0], work_dir)

    def _resolve_game_version_ids(self, token: str) -> list[int]:
        try:
            result = self.http.get_json(
                f"{self.upload_api}/api/game/versions",
                label="CurseForge upload game versions",
                headers={"X-Api-Token": token},
            )
        except HttpStatusError as exc:
            if exc.status_code == 401:
                raise PublicationError(
                    "CURSEFORGE_TOKEN_REJECTED",
                    "CurseForge rejected the configured API token",
                    EXIT_AUTHORIZATION,
                ) from None
            if exc.status_code == 403:
                raise PublicationError(
                    "CURSEFORGE_TOKEN_FORBIDDEN",
                    "CurseForge denied game-version metadata access",
                    EXIT_AUTHORIZATION,
                ) from None
            raise
        values = result.get("data") if isinstance(result, dict) and "data" in result else result
        if not isinstance(values, list):
            raise PublicationError(
                "CURSEFORGE_GAME_VERSIONS_INVALID",
                "CurseForge game-version response is invalid",
            )
        resolved: list[int] = []
        for expected_name in self.cf["gameVersionNames"]:
            matches = [item for item in values if item.get("name") == expected_name]
            if len(matches) != 1 or not isinstance(matches[0].get("id"), int):
                raise PublicationError(
                    "CURSEFORGE_GAME_VERSION_UNRESOLVED",
                    f"CurseForge game version could not be resolved uniquely: {expected_name}",
                )
            resolved.append(matches[0]["id"])
        return resolved

    def _state_artifact_prefix(self) -> str:
        safe_tag = re.sub(r"[^A-Za-z0-9_.-]", "-", self.release["tag"])
        return f"iedct-cf-{safe_tag}--"

    def _prior_persisted_intent_run_keys(
        self, github_token: str, current_run_key: str
    ) -> set[str]:
        """Find prior attempts whose GitHub job proves the intent was persisted.

        Workflow-run and job metadata can remain queryable after ordinary Actions
        artifact retention. While matching metadata remains available, this prevents
        an expired intent/result artifact from silently reopening the one-POST gate.
        """
        owner = urllib.parse.quote(self.repository["owner"], safe="")
        name = urllib.parse.quote(self.repository["name"], safe="")
        workflow = urllib.parse.quote(PUBLICATION_WORKFLOW_FILE, safe="")
        workflow_runs: list[dict[str, Any]] = []
        total_count: int | None = None
        for page in range(1, 11):
            result = self.http.get_json(
                f"{self.github_api}/repos/{owner}/{name}/actions/workflows/{workflow}/runs"
                f"?per_page=100&page={page}",
                label=f"GitHub publication workflow history page {page}",
                headers=self._github_headers(github_token),
            )
            if not isinstance(result, dict) or not isinstance(
                result.get("workflow_runs"), list
            ):
                raise PublicationError(
                    "GITHUB_WORKFLOW_HISTORY_INVALID",
                    "GitHub publication workflow history response is invalid",
                )
            if not isinstance(result.get("total_count"), int):
                raise PublicationError(
                    "GITHUB_WORKFLOW_HISTORY_INVALID",
                    "GitHub publication workflow history omitted total_count",
                )
            if total_count is None:
                total_count = result["total_count"]
            elif total_count != result["total_count"]:
                raise PublicationError(
                    "GITHUB_WORKFLOW_HISTORY_DRIFTED",
                    "GitHub publication workflow history changed during pagination",
                )
            workflow_runs.extend(result["workflow_runs"])
            if len(workflow_runs) >= total_count:
                break
        if total_count is None or len(workflow_runs) < total_count:
            raise PublicationError(
                "GITHUB_WORKFLOW_HISTORY_PAGINATION_LIMIT",
                "GitHub publication workflow history exceeded the bounded pagination limit",
            )

        persisted: set[str] = set()
        for workflow_run in workflow_runs:
            if not isinstance(workflow_run, dict):
                raise PublicationError(
                    "GITHUB_WORKFLOW_HISTORY_INVALID",
                    "GitHub publication workflow history contains an invalid run",
                )
            run_id = workflow_run.get("id")
            run_attempt = workflow_run.get("run_attempt")
            if (
                not isinstance(run_id, int)
                or run_id <= 0
                or not isinstance(run_attempt, int)
                or not 1 <= run_attempt <= 100
            ):
                raise PublicationError(
                    "GITHUB_WORKFLOW_HISTORY_INVALID",
                    "GitHub publication workflow history contains an invalid run identity",
                )
            for attempt in range(1, run_attempt + 1):
                run_key = f"{run_id}-{attempt}"
                if run_key == current_run_key:
                    continue
                jobs: list[dict[str, Any]] = []
                jobs_total: int | None = None
                for page in range(1, 11):
                    jobs_result = self.http.get_json(
                        f"{self.github_api}/repos/{owner}/{name}/actions/runs/{run_id}"
                        f"/attempts/{attempt}/jobs?per_page=100&page={page}",
                        label=(
                            "GitHub publication workflow jobs "
                            f"for run {run_id} attempt {attempt} page {page}"
                        ),
                        headers=self._github_headers(github_token),
                    )
                    if not isinstance(jobs_result, dict) or not isinstance(
                        jobs_result.get("jobs"), list
                    ):
                        raise PublicationError(
                            "GITHUB_WORKFLOW_JOBS_INVALID",
                            "GitHub publication workflow jobs response is invalid",
                        )
                    if not isinstance(jobs_result.get("total_count"), int):
                        raise PublicationError(
                            "GITHUB_WORKFLOW_JOBS_INVALID",
                            "GitHub publication workflow jobs omitted total_count",
                        )
                    if jobs_total is None:
                        jobs_total = jobs_result["total_count"]
                    elif jobs_total != jobs_result["total_count"]:
                        raise PublicationError(
                            "GITHUB_WORKFLOW_JOBS_DRIFTED",
                            "GitHub publication workflow jobs changed during pagination",
                        )
                    jobs.extend(jobs_result["jobs"])
                    if len(jobs) >= jobs_total:
                        break
                if jobs_total is None or len(jobs) < jobs_total:
                    raise PublicationError(
                        "GITHUB_WORKFLOW_JOBS_PAGINATION_LIMIT",
                        "GitHub publication workflow jobs exceeded the bounded pagination limit",
                    )
                for job in jobs:
                    if not isinstance(job, dict):
                        raise PublicationError(
                            "GITHUB_WORKFLOW_JOBS_INVALID",
                            "GitHub publication workflow jobs contains an invalid job",
                        )
                    steps = job.get("steps", [])
                    if not isinstance(steps, list):
                        raise PublicationError(
                            "GITHUB_WORKFLOW_JOBS_INVALID",
                            "GitHub publication workflow job steps are invalid",
                        )
                    if any(
                        isinstance(step, dict)
                        and step.get("name") == PERSIST_INTENT_STEP_NAME
                        and step.get("conclusion") == "success"
                        for step in steps
                    ):
                        persisted.add(run_key)
        return persisted

    def _prior_durable_state_resume_id(
        self, github_token: str, current_run_key: str
    ) -> int | None:
        if not github_token:
            raise PublicationError(
                "GITHUB_STATE_TOKEN_MISSING",
                "A GitHub token with Actions read access is required for durable upload-state reconciliation",
                EXIT_AUTHORIZATION,
            )
        owner = urllib.parse.quote(self.repository["owner"], safe="")
        name = urllib.parse.quote(self.repository["name"], safe="")
        artifacts: list[dict[str, Any]] = []
        total_count: int | None = None
        for page in range(1, 11):
            result = self.http.get_json(
                f"{self.github_api}/repos/{owner}/{name}/actions/artifacts?per_page=100&page={page}",
                label=f"GitHub durable publication state page {page}",
                headers=self._github_headers(github_token),
            )
            if not isinstance(result, dict) or not isinstance(result.get("artifacts"), list):
                raise PublicationError(
                    "GITHUB_STATE_RESPONSE_INVALID",
                    "GitHub Actions artifact state response is invalid",
                )
            if not isinstance(result.get("total_count"), int):
                raise PublicationError(
                    "GITHUB_STATE_RESPONSE_INVALID",
                    "GitHub Actions artifact state omitted total_count",
                )
            if total_count is None:
                total_count = result["total_count"]
            elif total_count != result["total_count"]:
                raise PublicationError(
                    "GITHUB_STATE_RESPONSE_DRIFTED",
                    "GitHub Actions artifact state changed during pagination",
                )
            artifacts.extend(result["artifacts"])
            if len(artifacts) >= total_count:
                break
        if total_count is None or len(artifacts) < total_count:
            raise PublicationError(
                "GITHUB_STATE_PAGINATION_LIMIT",
                "GitHub Actions artifact state exceeded the bounded pagination limit",
            )

        persisted_intent_runs = self._prior_persisted_intent_run_keys(
            github_token, current_run_key
        )

        prefix = self._state_artifact_prefix()
        runs: dict[str, dict[str, list[tuple[int, list[str]]]]] = {}
        for artifact in artifacts:
            artifact_name = artifact.get("name")
            artifact_id = artifact.get("id")
            if (
                artifact.get("expired") is True
                or not isinstance(artifact_name, str)
                or not artifact_name.startswith(prefix)
                or not isinstance(artifact_id, int)
            ):
                continue
            fields = artifact_name[len(prefix) :].split("--")
            if len(fields) < 3:
                raise PublicationError(
                    "GITHUB_STATE_ARTIFACT_INVALID",
                    "A publication state artifact name is malformed",
                )
            run_key, phase, *details = fields
            if run_key == current_run_key:
                continue
            if not re.fullmatch(r"[1-9][0-9]*-[1-9][0-9]*", run_key):
                raise PublicationError(
                    "GITHUB_STATE_ARTIFACT_INVALID",
                    "A publication state artifact has an invalid run key",
                )
            if phase not in {"intent", "result"}:
                raise PublicationError(
                    "GITHUB_STATE_ARTIFACT_INVALID",
                    "A publication state artifact has an invalid phase",
                )
            runs.setdefault(run_key, {"intent": [], "result": []})[phase].append(
                (artifact_id, details)
            )

        resume_ids: set[int] = set()
        for run_key, phases in runs.items():
            intents = phases["intent"]
            results = phases["result"]
            if len(intents) > 1 or len(results) > 1:
                raise PublicationError(
                    "GITHUB_STATE_ARTIFACT_AMBIGUOUS",
                    f"Durable publication state is ambiguous for run {run_key}",
                    EXIT_CONFLICT,
                )
            if intents and not results:
                raise PublicationError(
                    "UPLOAD_OUTCOME_UNKNOWN",
                    f"Run {run_key} has a durable upload intent without a result; refusing another POST",
                    EXIT_CONFLICT,
                )
            if not results:
                continue
            details = results[0][1]
            if len(details) != 2 or not re.fullmatch(r"[A-Z0-9_]+", details[0]):
                raise PublicationError(
                    "GITHUB_STATE_ARTIFACT_INVALID",
                    f"Durable publication result is malformed for run {run_key}",
                )
            status, file_id_text = details
            if status == "UPLOAD_INTENT_READY":
                raise PublicationError(
                    "GITHUB_STATE_ARTIFACT_INVALID",
                    f"Run {run_key} recorded an upload intent as a publication result",
                    EXIT_CONFLICT,
                )
            if status == "UPLOAD_OUTCOME_UNKNOWN":
                raise PublicationError(
                    "UPLOAD_OUTCOME_UNKNOWN",
                    f"Run {run_key} recorded an ambiguous upload outcome; refusing another POST",
                    EXIT_CONFLICT,
                )
            if not re.fullmatch(r"[0-9]+", file_id_text):
                raise PublicationError(
                    "GITHUB_STATE_ARTIFACT_INVALID",
                    f"Durable publication result has an invalid file ID for run {run_key}",
                )
            file_id = int(file_id_text)
            if file_id > 0:
                resume_ids.add(file_id)
        for run_key in sorted(persisted_intent_runs):
            phases = runs.get(run_key)
            if phases is None or not phases["result"]:
                raise PublicationError(
                    "UPLOAD_OUTCOME_UNKNOWN",
                    (
                        f"Run {run_key} persisted an upload intent, but its active result "
                        "artifact is missing or expired; refusing another POST"
                    ),
                    EXIT_CONFLICT,
                )
        if len(resume_ids) > 1:
            raise PublicationError(
                "GITHUB_STATE_MULTIPLE_FILE_IDS",
                "Durable publication state contains multiple unresolved CurseForge file IDs",
                EXIT_CONFLICT,
            )
        return next(iter(resume_ids), None)

    def _metadata(self, changelog: str) -> dict[str, Any]:
        return {
            "changelog": changelog,
            "changelogType": "markdown",
            "displayName": self.cf["displayName"],
            "gameVersionNames": self.cf["gameVersionNames"],
            "isMarkedForManualRelease": self.cf["isMarkedForManualRelease"],
            "releaseType": self.cf["releaseType"],
            "relations": {"projects": self.cf["uploadRelations"]},
        }

    @staticmethod
    def _validate_run_key(run_key: str) -> None:
        if not re.fullmatch(r"[1-9][0-9]*-[1-9][0-9]*", run_key):
            raise PublicationError(
                "RUN_KEY_INVALID",
                "Publication run key must be '<GitHub run id>-<run attempt>'",
            )

    def _validate_current_intent_artifact(
        self,
        github_token: str,
        run_key: str,
        artifact_id: int,
        multipart_sha256: str,
    ) -> str:
        if not github_token:
            raise PublicationError(
                "GITHUB_STATE_TOKEN_MISSING",
                "A GitHub token with Actions read access is required to verify the durable upload intent",
                EXIT_AUTHORIZATION,
            )
        if artifact_id <= 0:
            raise PublicationError(
                "INTENT_ARTIFACT_ID_INVALID",
                "A positive durable intent artifact ID is required before upload",
            )
        owner = urllib.parse.quote(self.repository["owner"], safe="")
        name = urllib.parse.quote(self.repository["name"], safe="")
        result = self.http.get_json(
            f"{self.github_api}/repos/{owner}/{name}/actions/artifacts/{artifact_id}",
            label="GitHub durable upload intent",
            headers=self._github_headers(github_token),
        )
        expected_name = (
            f"{self._state_artifact_prefix()}{run_key}--intent--{multipart_sha256[:12]}"
        )
        if (
            not isinstance(result, dict)
            or result.get("id") != artifact_id
            or result.get("name") != expected_name
            or result.get("expired") is True
        ):
            raise PublicationError(
                "DURABLE_INTENT_NOT_VERIFIED",
                "The exact upload intent was not durably persisted before POST",
                EXIT_CONFLICT,
            )
        return expected_name

    def _validate_intent_report(
        self,
        intent_report: dict[str, Any] | None,
        *,
        run_key: str,
        intent_artifact_id: int,
        metadata_sha256: str,
        multipart_sha256: str,
        multipart_size: int,
        resolved_game_versions: dict[str, int],
    ) -> None:
        if not isinstance(intent_report, dict):
            raise PublicationError(
                "UPLOAD_INTENT_REPORT_MISSING",
                "The durable upload intent report is required before POST",
            )
        expected = {
            "schemaVersion": 1,
            "mode": "prepare-publish",
            "tag": self.release["tag"],
            "version": self.release["version"],
            "projectId": self.cf["projectId"],
            "asset": {
                "name": self.release["assetName"],
                "size": self.release["assetSize"],
                "sha256": self.release["assetSha256"],
            },
            "status": "UPLOAD_INTENT_READY",
            "verdict": "PASS",
            "runKey": run_key,
            "postRequired": True,
            "metadataSha256": metadata_sha256,
            "multipartSha256": multipart_sha256,
            "multipartSize": multipart_size,
            "resolvedGameVersions": resolved_game_versions,
        }
        for key, value in expected.items():
            if intent_report.get(key) != value:
                raise PublicationError(
                    "UPLOAD_INTENT_REPORT_MISMATCH",
                    f"Durable upload intent field does not match: {key}",
                    EXIT_CONFLICT,
                )
        if "fileId" in intent_report or intent_report.get("publicHashMatch") is not None:
            raise PublicationError(
                "UPLOAD_INTENT_REPORT_INVALID",
                "Durable upload intent contains a post-upload result",
                EXIT_CONFLICT,
            )
        if intent_artifact_id <= 0:
            raise PublicationError(
                "INTENT_ARTIFACT_ID_INVALID",
                "A positive durable intent artifact ID is required before upload",
            )

    def _upload(self, token: str, body: bytes, content_type: str) -> int:
        project = self.cf["projectId"]
        try:
            result = self.http.post_json(
                f"{self.upload_api}/api/projects/{project}/upload-file",
                body,
                content_type=content_type,
                label="CurseForge upload",
                headers={"X-Api-Token": token},
            )
        except HttpStatusError as exc:
            if exc.status_code == 401:
                raise PublicationError(
                    "CURSEFORGE_TOKEN_REJECTED",
                    "CurseForge rejected the configured API token",
                    EXIT_AUTHORIZATION,
                ) from None
            if exc.status_code == 403:
                raise PublicationError(
                    "CURSEFORGE_PROJECT_PERMISSION_DENIED",
                    "CurseForge denied upload permission for the configured project",
                    EXIT_AUTHORIZATION,
                ) from None
            if exc.status_code >= 500:
                raise PublicationError(
                    "UPLOAD_OUTCOME_UNKNOWN",
                    "CurseForge returned a server error after the non-retried upload request; refusing another POST",
                    EXIT_CONFLICT,
                ) from None
            raise
        except PublicationError as exc:
            if exc.status in {"HTTP_TRANSPORT_FAILED", "INVALID_UPLOAD_RESPONSE"}:
                raise PublicationError(
                    "UPLOAD_OUTCOME_UNKNOWN",
                    "The CurseForge upload response was not reliably received; refusing another POST",
                    EXIT_CONFLICT,
                ) from None
            raise
        file_id = result.get("id") if isinstance(result, dict) else None
        if not isinstance(file_id, int) or file_id <= 0:
            raise PublicationError(
                "UPLOAD_OUTCOME_UNKNOWN",
                "CurseForge upload returned no positive file ID; refusing another POST",
                EXIT_CONFLICT,
            )
        return file_id

    def _poll_public(
        self,
        file_id: int,
        work_dir: Path,
        attempts: int,
        interval: float,
    ) -> dict[str, Any]:
        attempts = max(1, attempts)
        for attempt in range(1, attempts + 1):
            try:
                return self._validate_public_release(file_id, work_dir)
            except HttpStatusError as exc:
                if exc.status_code != 404:
                    raise
            except PublicationError as exc:
                if exc.status not in {
                    "CURSEFORGE_PUBLIC_STATUS_NOT_APPROVED",
                    "CURSEFORGE_PUBLIC_FILE_INVALID",
                }:
                    raise
            if attempt < attempts:
                time.sleep(max(0.0, interval))
        raise PublicationError(
            "UPLOADED_PROCESSING",
            f"CurseForge accepted file {file_id}, but public hash readback is not yet available",
            EXIT_PROCESSING,
        )

    def run(
        self,
        *,
        mode: str,
        curseforge_token: str,
        github_token: str,
        resume_file_id: int | None,
        poll_attempts: int,
        poll_interval: float,
        run_key: str = "",
        intent_report: dict[str, Any] | None = None,
        intent_artifact_id: int | None = None,
    ) -> dict[str, Any]:
        report: dict[str, Any] = {
            "schemaVersion": 1,
            "mode": mode,
            "tag": self.release["tag"],
            "version": self.release["version"],
            "projectId": self.cf["projectId"],
            "asset": {
                "name": self.release["assetName"],
                "size": self.release["assetSize"],
                "sha256": self.release["assetSha256"],
            },
        }
        if mode not in {"dry-run", "prepare-publish", "publish"}:
            raise PublicationError("MODE_INVALID", "Unsupported publisher mode")
        try:
            with tempfile.TemporaryDirectory(prefix="iedct-curseforge-") as temporary:
                work_dir = Path(temporary)
                jar_path, github_asset, source_sha = self._download_and_validate_asset(
                    work_dir, github_token
                )
                changelog = self._changelog()
                report["curseForgeBaseline"] = self._validate_previous_public_baseline()
                report["githubRelease"] = {
                    "assetId": github_asset.get("id"),
                    "assetDigest": github_asset.get("digest"),
                    "sourceHashMatch": source_sha == self.release["assetSha256"],
                }
                existing = self._find_existing_release(work_dir)
                if existing is not None:
                    report.update(
                        {
                            "status": "ALREADY_PUBLISHED",
                            "verdict": "PASS",
                            "fileId": existing["fileId"],
                            "publicReadback": existing,
                            "publicSize": existing["size"],
                            "publicSha256": existing["sha256"],
                            "publicHashMatch": True,
                            "postRequired": False,
                        }
                    )
                    return report
                if resume_file_id is not None:
                    report["fileId"] = resume_file_id
                    observed = self._poll_public(
                        resume_file_id, work_dir, poll_attempts, poll_interval
                    )
                    report.update(
                        {
                            "status": "RESUMED_PUBLICATION_VERIFIED",
                            "verdict": "PASS",
                            "publicReadback": observed,
                            "publicSize": observed["size"],
                            "publicSha256": observed["sha256"],
                            "publicHashMatch": True,
                            "postRequired": False,
                        }
                    )
                    return report

                metadata = self._metadata(changelog)
                body, content_type = build_multipart(
                    metadata,
                    self.release["assetName"],
                    jar_path.read_bytes(),
                    self.release["assetSha256"],
                )
                metadata_sha256 = sha256_bytes(canonical_json(metadata))
                multipart_sha256 = sha256_bytes(body)
                report.update(
                    {
                        "metadataSha256": metadata_sha256,
                        "multipartSha256": multipart_sha256,
                        "multipartSize": len(body),
                        "publicHashMatch": None,
                    }
                )
                if mode == "dry-run":
                    report.update(
                        {
                            "status": "AUTOMATION_READY_DRY_RUN",
                            "verdict": "PASS",
                            "postRequired": False,
                        }
                    )
                    return report

                self._validate_run_key(run_key)
                report["runKey"] = run_key

                if mode == "prepare-publish":
                    prior_file_id = self._prior_durable_state_resume_id(
                        github_token, run_key
                    )
                    if prior_file_id is not None:
                        report["fileId"] = prior_file_id
                        observed = self._poll_public(
                            prior_file_id, work_dir, poll_attempts, poll_interval
                        )
                        report.update(
                            {
                                "status": "RESUMED_PUBLICATION_VERIFIED",
                                "verdict": "PASS",
                                "durableStateResume": True,
                                "publicReadback": observed,
                                "publicSize": observed["size"],
                                "publicSha256": observed["sha256"],
                                "publicHashMatch": True,
                                "postRequired": False,
                            }
                        )
                        return report
                    if not curseforge_token:
                        raise PublicationError(
                            "BLOCKED_BY_MISSING_CURSEFORGE_API_TOKEN",
                            "CURSEFORGE_API_TOKEN is not configured",
                            EXIT_TOKEN_MISSING,
                        )
                    game_version_ids = self._resolve_game_version_ids(curseforge_token)
                    resolved = dict(zip(self.cf["gameVersionNames"], game_version_ids, strict=True))
                    report.update(
                        {
                            "status": "UPLOAD_INTENT_READY",
                            "verdict": "PASS",
                            "postRequired": True,
                            "resolvedGameVersions": resolved,
                        }
                    )
                    return report

                if not curseforge_token:
                    raise PublicationError(
                        "BLOCKED_BY_MISSING_CURSEFORGE_API_TOKEN",
                        "CURSEFORGE_API_TOKEN is not configured",
                        EXIT_TOKEN_MISSING,
                    )
                if intent_artifact_id is None:
                    raise PublicationError(
                        "INTENT_ARTIFACT_ID_INVALID",
                        "A positive durable intent artifact ID is required before upload",
                    )
                intent_name = self._validate_current_intent_artifact(
                    github_token,
                    run_key,
                    intent_artifact_id,
                    multipart_sha256,
                )
                game_version_ids = self._resolve_game_version_ids(curseforge_token)
                resolved = dict(zip(self.cf["gameVersionNames"], game_version_ids, strict=True))
                self._validate_intent_report(
                    intent_report,
                    run_key=run_key,
                    intent_artifact_id=intent_artifact_id,
                    metadata_sha256=metadata_sha256,
                    multipart_sha256=multipart_sha256,
                    multipart_size=len(body),
                    resolved_game_versions=resolved,
                )
                report.update(
                    {
                        "intentArtifactId": intent_artifact_id,
                        "intentArtifactName": intent_name,
                        "resolvedGameVersions": resolved,
                    }
                )
                try:
                    file_id = self._upload(curseforge_token, body, content_type)
                    report["fileId"] = file_id
                    observed = self._poll_public(
                        file_id, work_dir, poll_attempts, poll_interval
                    )
                except PublicationError:
                    raise
                except Exception:
                    raise PublicationError(
                        "UPLOAD_OUTCOME_UNKNOWN",
                        "The publication code failed after POST began; refusing another POST",
                        EXIT_CONFLICT,
                    ) from None
                report.update(
                    {
                        "status": "PUBLISHED_VERIFIED",
                        "verdict": "PASS",
                        "publicReadback": observed,
                        "publicSize": observed["size"],
                        "publicSha256": observed["sha256"],
                        "publicHashMatch": True,
                    }
                )
                return report
        except PublicationError as exc:
            if not isinstance(getattr(exc, "report", None), dict):
                exc.report = report  # type: ignore[attr-defined]
            raise


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--dry-run", action="store_true", help="Validate without uploading")
    mode.add_argument(
        "--prepare-publish",
        action="store_true",
        help="Authenticate and emit a durable upload intent without uploading",
    )
    mode.add_argument("--publish", action="store_true", help="Upload when authorized")
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument(
        "--manifest",
        type=Path,
        default=Path("tools/release/curseforge_release_1.1.54.json"),
    )
    parser.add_argument("--tag", required=True)
    parser.add_argument("--resume-file-id", type=int)
    parser.add_argument("--run-key", default="")
    parser.add_argument("--intent-report", type=Path)
    parser.add_argument("--intent-artifact-id", type=int)
    parser.add_argument("--poll-attempts", type=int, default=30)
    parser.add_argument("--poll-interval", type=float, default=20.0)
    parser.add_argument("--report", type=Path)
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    repo_root = args.repo_root.resolve()
    manifest_path = args.manifest
    if not manifest_path.is_absolute():
        manifest_path = repo_root / manifest_path
    report: dict[str, Any] = {
        "schemaVersion": 1,
        "status": "NOT_STARTED",
        "verdict": "FAIL",
    }
    exit_code = EXIT_VALIDATION
    try:
        manifest = load_manifest(manifest_path)
        if args.tag != manifest["release"]["tag"]:
            raise PublicationError(
                "TAG_NOT_AUTHORIZED_BY_MANIFEST",
                "Requested tag does not match the reviewed release manifest",
            )
        if args.resume_file_id is not None and args.resume_file_id <= 0:
            raise PublicationError("RESUME_FILE_ID_INVALID", "Resume file ID must be positive")
        if not 1 <= args.poll_attempts <= 120:
            raise PublicationError("POLL_ATTEMPTS_INVALID", "Poll attempts must be from 1 to 120")
        if not 0 <= args.poll_interval <= 60:
            raise PublicationError("POLL_INTERVAL_INVALID", "Poll interval must be from 0 to 60")
        if args.resume_file_id is not None and not args.publish:
            raise PublicationError(
                "RESUME_MODE_INVALID",
                "--resume-file-id requires --publish",
            )
        if (args.intent_report is not None or args.intent_artifact_id is not None) and not args.publish:
            raise PublicationError(
                "INTENT_MODE_INVALID",
                "Intent report and artifact ID arguments require --publish",
            )
        intent_report = None
        if args.intent_report is not None:
            try:
                intent_report = json.loads(args.intent_report.read_text(encoding="utf-8"))
            except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
                raise PublicationError(
                    "UPLOAD_INTENT_REPORT_INVALID",
                    "The upload intent report is unreadable",
                ) from exc
        publisher = Publisher(repo_root, manifest)
        selected_mode = (
            "dry-run"
            if args.dry_run
            else "prepare-publish"
            if args.prepare_publish
            else "publish"
        )
        report = publisher.run(
            mode=selected_mode,
            curseforge_token=os.environ.get("CURSEFORGE_API_TOKEN", ""),
            github_token=os.environ.get("GITHUB_TOKEN", ""),
            resume_file_id=args.resume_file_id,
            poll_attempts=args.poll_attempts,
            poll_interval=args.poll_interval,
            run_key=args.run_key,
            intent_report=intent_report,
            intent_artifact_id=args.intent_artifact_id,
        )
        exit_code = EXIT_OK
    except PublicationError as exc:
        partial = getattr(exc, "report", None)
        if isinstance(partial, dict):
            report = partial
        report.update(
            {
                "status": exc.status,
                "verdict": "BLOCKED" if exc.exit_code in {3, 4, 5, 6} else "FAIL",
                "message": exc.message,
            }
        )
        exit_code = exc.exit_code
    except Exception:
        report.update(
            {
                "status": "UNEXPECTED_PUBLISHER_FAILURE",
                "verdict": "FAIL",
                "message": "Unexpected publisher failure; inspect the controlled CI log",
            }
        )
        exit_code = EXIT_VALIDATION
    report["exitCode"] = exit_code
    write_report(args.report, report)
    public_summary = {
        key: report.get(key)
        for key in ("status", "verdict", "fileId", "publicHashMatch", "exitCode")
        if key in report
    }
    print(json.dumps(public_summary, sort_keys=True))
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
