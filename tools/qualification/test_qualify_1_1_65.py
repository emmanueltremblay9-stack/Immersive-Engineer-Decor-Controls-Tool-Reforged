"""Synthetic unit tests. These never build Minecraft or qualify a real distribution JAR."""
from __future__ import annotations
import json
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch
import warnings
import zipfile
import qualify_1_1_65 as q


class QualificationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        for name in q.NOTICES:
            (self.root / name).write_bytes(("synthetic " + name).encode())
        self.jar = self.root / q.JAR_NAME
        self.entries = {
            "META-INF/MANIFEST.MF": ("Manifest-Version: 1.0\r\nImplementation-Version: " + q.VERSION +
                "\r\nSpecification-Version: " + q.VERSION + "\r\n\r\n").encode(),
            "META-INF/neoforge.mods.toml": self.metadata().encode(),
            "main.mixins.json": b"{}", "fluid.mixins.json": b"{}",
            "com/example/Runtime.class": b"\xca\xfe\xba\xbe" + struct.pack(">HH", 0, 65),
        }
        for name in q.NOTICES:
            self.entries["META-INF/" + name] = (self.root / name).read_bytes()

    def metadata(self, version=q.VERSION, mod_id=q.MOD_ID):
        text = (f'modLoader="javafml"\nlicense="MIT"\n[[mods]]\nmodId="{mod_id}"\nversion="{version}"\n')
        for mod, (kind, version_range) in q.DEPENDENCIES.items():
            text += (f'[[dependencies.{q.MOD_ID}]]\nmodId="{mod}"\ntype="{kind}"\n'
                     f'versionRange="{version_range}"\nside="BOTH"\n')
        return text + ('[[mixins]]\nconfig="main.mixins.json"\n'
                       '[[mixins]]\nconfig="fluid.mixins.json"\n')

    def archive(self, entries=None, extra=None):
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(self.jar, "w") as jar:
                for name, content in (self.entries if entries is None else entries).items():
                    jar.writestr(name, content)
                if extra:
                    jar.writestr(*extra)
        return q.digest(self.jar)

    def inspect(self):
        sha = self.archive()
        with patch.object(q, "EXPECTED_SHA256", sha):
            return q.inspect_archive(self.jar, self.root)

    def assert_bad_archive(self):
        with self.assertRaises((q.QualificationError, KeyError, ValueError, zipfile.BadZipFile)):
            self.inspect()

    def ci(self):
        repo = {"id": q.REPOSITORY_ID, "full_name": q.REPOSITORY}
        return {"id": q.BUILD_RUN_ID, "run_attempt": q.BUILD_ATTEMPT, "head_sha": q.SOURCE_COMMIT,
                "event": "push", "status": "completed", "conclusion": "success",
                "path": ".github/workflows/build.yml", "name": "Build",
                "repository": repo.copy(), "head_repository": repo.copy()}

    def test_valid_synthetic_archive(self):
        result = self.inspect()
        self.assertEqual(result["version"], q.VERSION)
        self.assertEqual(result["assetSize"], self.jar.stat().st_size)
        self.assertEqual(result["classCount"], 1)
        self.assertEqual(len(result["noticesSha256"]), 3)

    def test_wrong_hash(self):
        self.archive()
        with self.assertRaises(q.QualificationError):
            q.inspect_archive(self.jar, self.root)

    def test_wrong_mod_version(self):
        self.entries["META-INF/neoforge.mods.toml"] = self.metadata(version="1.1.58-reconstructed").encode()
        self.assert_bad_archive()

    def test_wrong_mod_id(self):
        self.entries["META-INF/neoforge.mods.toml"] = self.metadata(mod_id="other").encode()
        self.assert_bad_archive()

    def test_wrong_dependency_type(self):
        self.entries["META-INF/neoforge.mods.toml"] = self.metadata().replace('type="required"', 'type="optional"').encode()
        self.assert_bad_archive()

    def test_wrong_dependency_range(self):
        self.entries["META-INF/neoforge.mods.toml"] = self.metadata().replace('[21.1.230,)', '[21.1.1,)').encode()
        self.assert_bad_archive()

    def test_duplicate_dependency(self):
        self.entries["META-INF/neoforge.mods.toml"] = self.metadata().replace('modId="jei"', 'modId="neoforge"').encode()
        self.assert_bad_archive()

    def test_wrong_side(self):
        self.entries["META-INF/neoforge.mods.toml"] = self.metadata().replace('side="BOTH"', 'side="CLIENT"').encode()
        self.assert_bad_archive()

    def test_wrong_license(self):
        self.entries["META-INF/neoforge.mods.toml"] = self.metadata().replace('license="MIT"', 'license="other"').encode()
        self.assert_bad_archive()

    def test_extra_mod(self):
        self.entries["META-INF/neoforge.mods.toml"] += b'\n[[mods]]\nmodId="other"\nversion="1"\n'
        self.assert_bad_archive()

    def test_manifest_version_mismatch(self):
        self.entries["META-INF/MANIFEST.MF"] = self.entries["META-INF/MANIFEST.MF"].replace(q.VERSION.encode(), b"wrong")
        self.assert_bad_archive()

    def test_manifest_continuation(self):
        self.assertEqual(q.read_manifest(b"Implementation-Version: 1.1.65-\r\n reconstructed\r\n\r\n")
                         ["Implementation-Version"], q.VERSION)

    def test_manifest_duplicate(self):
        with self.assertRaises(q.QualificationError):
            q.read_manifest(b"A: one\nA: two\n\n")

    def test_manifest_orphan_continuation(self):
        with self.assertRaises(q.QualificationError):
            q.read_manifest(b" bad\n")

    def test_notice_mismatch(self):
        self.entries["META-INF/NOTICE.md"] = b"wrong"
        self.assert_bad_archive()

    def test_empty_notice(self):
        self.entries["META-INF/NOTICE.md"] = b""
        self.assert_bad_archive()

    def test_missing_notice(self):
        del self.entries["META-INF/CREDITS.md"]
        self.assert_bad_archive()

    def test_missing_mixin(self):
        del self.entries["main.mixins.json"]
        self.assert_bad_archive()

    def test_malformed_mixin(self):
        self.entries["main.mixins.json"] = b"[]"
        self.assert_bad_archive()

    def test_duplicate_zip_member(self):
        sha = self.archive(extra=("main.mixins.json", b"{}"))
        with patch.object(q, "EXPECTED_SHA256", sha), self.assertRaises(q.QualificationError):
            q.inspect_archive(self.jar, self.root)

    def test_unsafe_zip_paths(self):
        for name in ("../bad", "/absolute", "a\\b", "C:/drive"):
            with self.subTest(path=name):
                self.entries[name] = b"x"
                self.assert_bad_archive()
                del self.entries[name]

    def test_zip_symlink(self):
        info = zipfile.ZipInfo("link")
        info.create_system = 3
        info.external_attr = 0o120777 << 16
        sha = self.archive(extra=(info, b"target"))
        with patch.object(q, "EXPECTED_SHA256", sha), self.assertRaises(q.QualificationError):
            q.inspect_archive(self.jar, self.root)

    def test_file_symlink(self):
        self.archive()
        link = self.root / "link.jar"
        link.symlink_to(self.jar)
        with self.assertRaises(q.QualificationError):
            q.inspect_archive(link, self.root)

    def test_newer_java_class(self):
        self.entries["com/example/Runtime.class"] = b"\xca\xfe\xba\xbe" + struct.pack(">HH", 0, 66)
        self.assert_bad_archive()

    def test_preview_class(self):
        self.entries["com/example/Runtime.class"] = b"\xca\xfe\xba\xbe" + struct.pack(">HH", 65535, 65)
        self.assert_bad_archive()

    def test_invalid_class(self):
        self.entries["com/example/Runtime.class"] = b"not a class"
        self.assert_bad_archive()

    def test_no_classes(self):
        del self.entries["com/example/Runtime.class"]
        self.assert_bad_archive()

    def test_exact_ci(self):
        self.assertEqual(q.validate_ci(self.ci())["status"], "EXACT_SOURCE_CI_VERIFIED")

    def test_ci_all_identity_and_status_fields(self):
        for field, wrong in {"id": 1, "run_attempt": 2, "head_sha": "0"*40,
                             "event": "pull_request", "status": "in_progress", "conclusion": "failure",
                             "path": ".github/workflows/other.yml", "name": "Other"}.items():
            with self.subTest(field=field), self.assertRaises(q.QualificationError):
                data = self.ci()
                data[field] = wrong
                q.validate_ci(data)

    def test_ci_repository_and_head_repository(self):
        for field in ("repository", "head_repository"):
            for key, wrong in (("id", 1), ("full_name", "other/repo")):
                with self.subTest(field=field, key=key), self.assertRaises(q.QualificationError):
                    data = self.ci()
                    data[field][key] = wrong
                    q.validate_ci(data)

    def test_ci_redirect_refused(self):
        with self.assertRaises(q.QualificationError):
            q.NoRedirect().redirect_request(None, None, 302, "", {}, "https://other.invalid")

    def test_wrong_source_commit(self):
        with patch.object(q, "git", return_value="wrong"), self.assertRaises(q.QualificationError):
            q.check_source(self.root)

    def test_wrong_source_tree(self):
        with patch.object(q, "git", side_effect=[q.SOURCE_COMMIT, "wrong"]), self.assertRaises(q.QualificationError):
            q.check_source(self.root)

    def test_dirty_source(self):
        with patch.object(q, "git", side_effect=[q.SOURCE_COMMIT, q.SOURCE_TREE, "?? unexpected"]), self.assertRaises(q.QualificationError):
            q.check_source(self.root)

    def test_source_properties(self):
        props = self.root / "gradle.properties"
        props.write_text("\n".join(f"{k}={v}" for k,v in q.PROPERTIES.items()))
        with patch.object(q, "git", side_effect=[q.SOURCE_COMMIT, q.SOURCE_TREE, ""]):
            self.assertEqual(q.check_source(self.root), self.root)
        props.write_text("mod_version=wrong")
        with patch.object(q, "git", side_effect=[q.SOURCE_COMMIT, q.SOURCE_TREE, ""]), self.assertRaises(q.QualificationError):
            q.check_source(self.root)

    def test_compare_rejects_same_directory(self):
        with self.assertRaises(q.QualificationError):
            q.compare_sources(self.root, self.root)

    def test_compare_rejects_different_bytes(self):
        with patch.object(q, "inspect_source", side_effect=[{"assetSha256":"a"}, {"assetSha256":"b"}]), self.assertRaises(q.QualificationError):
            q.compare_sources(self.root / "a", self.root / "b")

    def test_compare_does_not_promote_runtime_or_publication(self):
        with patch.object(q, "inspect_source", return_value={"assetSha256": "same"}):
            report = q.compare_sources(self.root / "a", self.root / "b")
        self.assertFalse(report["publicationReady"])
        self.assertEqual(report["exactJarClient"], "NOT_RUN")
        self.assertEqual(report["packagedServer"], "NOT_RUN")

    def test_server_consent_before_any_operations(self):
        with patch.object(q, "inspect_source") as inspect:
            for value in ("", "true", "yes", q.EULA_ACK + " "):
                with self.subTest(value=value), self.assertRaises(q.QualificationError):
                    q.server_smoke(self.root, self.root, value)
            inspect.assert_not_called()

    def test_cli_server_no_consent_no_report(self):
        report = self.root / "must-not-exist.json"
        with self.assertRaises(q.QualificationError):
            q.main(["server", "--report", str(report)])
        self.assertFalse(report.exists())

    def test_reports_are_utf8_and_no_overwrite(self):
        output = self.root / "report.json"
        q.write_report(output, {"note": "vérifié", "publicationReady": False})
        original = output.read_bytes()
        self.assertEqual(json.loads(original)["note"], "vérifié")
        with self.assertRaises(FileExistsError):
            q.write_report(output, {"replacement": True})
        self.assertEqual(original, output.read_bytes())

    def test_failed_operation_stays_failed_and_redacts_provider_details(self):
        report = self.root / "failed.json"
        with patch.object(q, "check_ci", side_effect=RuntimeError("DO_NOT_PRINT_SECRET")):
            self.assertEqual(q.main(["ci", "--report", str(report)]), 1)
        data = report.read_text()
        self.assertNotIn("DO_NOT_PRINT_SECRET", data)
        self.assertEqual(json.loads(data)["status"], "FAILED")
        self.assertFalse(json.loads(data)["publicationReady"])


class WorkflowContractTests(unittest.TestCase):
    def setUp(self):
        self.root = Path(__file__).resolve().parents[2]
        self.workflow = (self.root / ".github/workflows/qualify-exact-jar-1.1.65.yml").read_text(encoding="utf-8")

    def test_source_is_pinned_twice(self):
        self.assertEqual(self.workflow.count("ref: " + q.SOURCE_COMMIT), 2)
        self.assertIn("path: runtime-a", self.workflow)
        self.assertIn("path: runtime-b", self.workflow)

    def test_independent_build_caches(self):
        for value in ("iedctr-gradle-a", "iedctr-gradle-b", "cache-disabled: true"):
            self.assertIn(value, self.workflow)
        self.assertEqual(self.workflow.count("clean build --no-build-cache --rerun-tasks"), 2)

    def test_no_publication_secret_or_privileged_trigger(self):
        for value in ("secrets.", "pull_request_target", "contents: write", "--publish", "--prepare-publish"):
            self.assertNotIn(value, self.workflow)
        self.assertIn("contents: read", self.workflow)
        self.assertIn("actions: read", self.workflow)

    def test_server_requires_dispatch_and_explicit_consent(self):
        self.assertEqual(self.workflow.count("github.event_name == 'workflow_dispatch' && inputs.run_server"), 2)
        self.assertIn("default: false", self.workflow)
        self.assertIn(q.EULA_ACK, self.workflow)
        self.assertNotIn("eula=true", self.workflow)

    def test_only_text_evidence_uploaded(self):
        self.assertIn("${{ env.QUALIFICATION_REPORTS }}/*.json", self.workflow)
        self.assertIn("${{ env.QUALIFICATION_REPORTS }}/unit-tests.txt", self.workflow)
        self.assertNotIn("build/libs", self.workflow)

    def test_legacy_harness_not_retargeted(self):
        source = (self.root / "tools/qualification/qualify_1_1_65.py").read_text(encoding="utf-8")
        for call in ("q.main(", "q.check_source(", "q.check_runtime(", "q.install_server("):
            self.assertNotIn(call, source)
        self.assertIn("q.qualify(root)", source)


if __name__ == "__main__":
    unittest.main()
