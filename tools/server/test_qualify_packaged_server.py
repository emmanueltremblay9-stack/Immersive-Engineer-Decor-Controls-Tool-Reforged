"""Offline tests only: no downloads, Minecraft installation, or EULA acceptance."""
import hashlib
from pathlib import Path
import queue
import re
import tempfile
import unittest
from unittest.mock import Mock, patch
import zipfile

import qualify_packaged_server as q


class GuardTests(unittest.TestCase):
    def test_ack_is_exact_and_has_no_default_acceptance(self):
        for value in ("", "true", "yes", q.EULA_ACK.lower(), q.EULA_ACK + " "):
            with self.subTest(value=value), self.assertRaises(q.QualificationError):
                q.require_ack(value)
        q.require_ack(q.EULA_ACK)

    def test_missing_consent_has_no_side_effects(self):
        with patch.object(q, "check_source") as source, patch.object(q, "install_server") as install, \
                patch.object(q, "write_configuration") as config, patch.object(q, "qualify") as launch:
            with self.assertRaises(q.QualificationError):
                q.main(["--source-root", "/does/not/exist"])
            for action in (source, install, config, launch):
                action.assert_not_called()

    def test_ack_only_is_inert(self):
        with patch.object(q, "check_source") as source, patch.object(q, "install_server") as install:
            self.assertEqual(0, q.main(["--check-ack-only", "--eula-acknowledgment", q.EULA_ACK]))
            source.assert_not_called()
            install.assert_not_called()

    def test_exact_acceptance_write_is_guarded(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with self.assertRaises(q.QualificationError):
                q.write_configuration(root, "")
            self.assertEqual([], list(root.iterdir()))

    def test_server_security_configuration(self):
        # Unit test intercepts all writes: no eula.txt is actually created here.
        with patch.object(Path, "write_text") as writer:
            q.write_configuration(Path("/not-a-real-server"), q.EULA_ACK)
        config = writer.call_args_list[-1].args[0]
        for line in ("server-ip=127.0.0.1", "online-mode=true", "enable-rcon=false",
                     "enable-query=false", "white-list=true", "enforce-whitelist=true"):
            self.assertIn(line + "\n", config)
        self.assertNotIn("online-mode=false", config)

    def test_arbitrary_download_rejected_before_network(self):
        with patch.object(q.urllib.request, "build_opener") as opener:
            with self.assertRaises(q.QualificationError):
                q.download("https://example.com/file.jar", Path("unused"), 10)
            opener.assert_not_called()

    def test_download_redirects_fail_closed(self):
        with self.assertRaises(q.QualificationError):
            q.NoRedirect().redirect_request(None, None, 302, "", {}, "https://elsewhere.invalid/")


class RuntimeTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name) / "fixture.jar"

    def make_jar(self, mod=q.MOD_ID, version=q.VERSION, manifest=None):
        with zipfile.ZipFile(self.path, "w") as jar:
            jar.writestr("META-INF/neoforge.mods.toml", f'[[mods]]\nmodId="{mod}"\nversion="{version}"\n')
            if manifest:
                jar.writestr("META-INF/MANIFEST.MF", manifest)
        return hashlib.sha256(self.path.read_bytes()).hexdigest()

    def test_exact_runtime_metadata_and_digest(self):
        expected = self.make_jar()
        self.assertEqual(expected, q.check_runtime(self.path, expected))

    def test_wrong_hash_fails_before_zip_is_opened(self):
        self.path.write_bytes(b"not a zip")
        with self.assertRaisesRegex(q.QualificationError, "SHA-256 mismatch"):
            q.check_runtime(self.path)

    def test_wrong_mod_or_version_fails(self):
        for mod, version in (("another_mod", q.VERSION), (q.MOD_ID, "1.1.57-reconstructed")):
            expected = self.make_jar(mod, version)
            with self.assertRaisesRegex(q.QualificationError, "mod ID/version"):
                q.check_runtime(self.path, expected)

    def test_jar_manifest_version_resolution(self):
        self.make_jar("immersiveengineering", "${file.jarVersion}",
                      "Manifest-Version: 1.0\r\nImplementation-Version: 1.21.1-12.4.2-194\r\n")
        self.assertEqual(q.IE_VERSION, q.jar_mods(self.path)[0]["version"])

    def test_packaged_notices_match_source(self):
        root = Path(self.tmp.name)
        with zipfile.ZipFile(self.path, "w") as jar:
            for name in ("LICENSE", "NOTICE.md", "CREDITS.md"):
                (root / name).write_text("Attribution " + name)
                jar.writestr("META-INF/" + name, "Attribution " + name)
        self.assertEqual(3, len(q.check_notices(self.path, root)))
        (root / "LICENSE").write_text("changed")
        with self.assertRaisesRegex(q.QualificationError, "attribution differs"):
            q.check_notices(self.path, root)

    def test_missing_notice_fails(self):
        root = Path(self.tmp.name)
        (root / "LICENSE").write_text("license")
        self.make_jar()
        with self.assertRaises(KeyError):
            q.check_notices(self.path, root)

    def test_dependency_hash_uses_exact_original_coordinate(self):
        root = Path(self.tmp.name)
        coordinate = root / "caches/modules-2/files-2.1/blusunrize.immersiveengineering/ImmersiveEngineering" / q.IE_VERSION
        with self.assertRaises(q.QualificationError):
            q.resolved_ie_hash(root)
        path = coordinate / "contenthash" / f"ImmersiveEngineering-{q.IE_VERSION}.jar"
        path.parent.mkdir(parents=True)
        path.write_bytes(b"original dependency bytes")
        self.assertEqual(q.digest(path), q.resolved_ie_hash(root))
        other = coordinate / "ambiguous" / path.name
        other.parent.mkdir()
        other.write_bytes(b"different")
        with self.assertRaises(q.QualificationError):
            q.resolved_ie_hash(root)

    def test_dependency_mismatch_prevents_copying_runtime(self):
        root = Path(self.tmp.name)
        def fake_download(url, path, maximum):
            if url.endswith(".sha256"):
                path.write_text(hashlib.sha256(b"installer").hexdigest())
            elif url == q.NEOFORGE_URL:
                path.write_bytes(b"installer")
            else:
                path.write_bytes(b"unexpected dependency")
        (root / "run.sh").write_text("unused")
        with patch.object(q, "download", side_effect=fake_download), patch.object(q.subprocess, "run"), \
                patch.object(q.shutil, "copyfile") as copy:
            with self.assertRaisesRegex(q.QualificationError, "differs from the dependency"):
                q.install_server(root, self.path, "0" * 64)
            copy.assert_not_called()

    def test_source_commit_mismatch_stops(self):
        with patch.object(q.subprocess, "check_output", return_value="0" * 40):
            with self.assertRaisesRegex(q.QualificationError, "commit"):
                q.check_source(Path(self.tmp.name))

    def test_dirty_source_stops(self):
        with patch.object(q.subprocess, "check_output", side_effect=[q.SOURCE_COMMIT, " M build.gradle"]):
            with self.assertRaisesRegex(q.QualificationError, "not clean"):
                q.check_source(Path(self.tmp.name))

    def test_hash_mismatch_blocks_install_and_launch(self):
        with patch.object(q, "check_source"), patch.object(q, "check_runtime", side_effect=q.QualificationError("bad hash")), \
                patch.object(q, "install_server") as install, patch.object(q, "qualify") as launch:
            with self.assertRaises(q.QualificationError):
                q.main(["--source-root", self.tmp.name, "--eula-acknowledgment", q.EULA_ACK])
            install.assert_not_called()
            launch.assert_not_called()


class ConsoleTests(unittest.TestCase):
    def console(self, lines):
        server = q.ConsoleServer.__new__(q.ConsoleServer)
        server.phase = "unit"
        server.events = queue.Queue()
        server.lines = list(lines)
        server.assertions = []
        for line in lines:
            server.events.put(line)
        return server

    def test_ready_requires_real_server_ready_output(self):
        self.assertIsNone(q.READY.search("runGameTestServer BUILD SUCCESSFUL"))
        self.assertIsNotNone(q.READY.search('[Server thread/INFO]: Done (1.203s)! For help, type "help"'))

    def test_error_prevents_ready_pass(self):
        server = self.console(["[main/ERROR]: failed", 'Done (1.0s)! For help, type "help"'])
        with self.assertRaisesRegex(q.QualificationError, "server error"):
            server.wait_for(q.READY, timeout=0.1)

    def test_client_class_failures_detected(self):
        for line in ("java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft",
                     "java.lang.ClassNotFoundException: net.minecraft.client.Minecraft",
                     "Attempted to load class net/minecraft/client/Minecraft for invalid dist DEDICATED_SERVER",
                     "[Server thread/FATAL]: crash"):
            self.assertIsNotNone(q.BAD_LOG.search(line))

    def test_premature_exit_fails(self):
        server = self.console([])
        server.events.put(None)
        with self.assertRaisesRegex(q.QualificationError, "exited"):
            server.wait_for(q.READY, timeout=0.1)

    def test_missing_assertion_is_not_success(self):
        server = self.console(["say TOKEN", "execute if block 0 0 0 air run say TOKEN"])
        with self.assertRaisesRegex(q.QualificationError, "timed out"):
            server.wait_for(re.compile(r"\[Server\] TOKEN$"), timeout=0.01)

    def test_unique_marker_and_condition_are_recorded_only_after_response(self):
        server = self.console([])
        server.send = Mock()
        server.wait_for = Mock()
        with patch.object(q.uuid, "uuid4", return_value=Mock(hex="unique")):
            server.assert_block("2 80 2", q.HATCH)
        self.assertIn("execute if block 2 80 2", server.send.call_args.args[0])
        pattern = server.wait_for.call_args.args[0]
        self.assertIsNone(pattern.search("say IEDCTR_ASSERT_unique"))
        self.assertIsNotNone(pattern.search("[Server] IEDCTR_ASSERT_unique"))
        self.assertEqual(["if block 2 80 2 " + q.HATCH], server.assertions)

    def test_shutdown_scans_errors_after_stop_message(self):
        server = self.console(["[Server thread/ERROR]: late save failure"])
        server.send = Mock()
        server.wait_for = Mock()
        server.process = Mock()
        server.process.wait.return_value = 0
        server.reader = Mock()
        server.reader.is_alive.return_value = False
        with self.assertRaisesRegex(q.QualificationError, "complete server log"):
            server.stop()

    def test_newline_command_rejected(self):
        server = self.console([])
        with self.assertRaisesRegex(q.QualificationError, "newline"):
            server.send("say hi\nstop")

    def test_nonzero_shutdown_fails(self):
        server = self.console([])
        server.send = Mock()
        server.wait_for = Mock()
        server.process = Mock()
        server.process.wait.return_value = 1
        with self.assertRaisesRegex(q.QualificationError, "unclean"):
            server.stop()

    def test_loot_assertion_requires_fresh_exact_drop_and_linked_removal(self):
        server = Mock(unsafe=True)
        q.assert_gate_destroy_loot(server)
        calls = server.method_calls
        self.assertEqual("barrier", calls[0][0])
        self.assertTrue(calls[0].args[0].startswith("unless entity @e[type=minecraft:item,"))
        self.assertEqual("setblock 10 80 2 minecraft:air destroy", calls[1].args[0])
        self.assertEqual([("10 80 2", "minecraft:air"), ("10 81 2", "minecraft:air")],
                         [call.args for call in calls if call[0] == "assert_block"])
        commands = [call.args[0] for call in calls if call[0] == "send"]
        self.assertIn("scoreboard players set #gate_entities iedctr_loot 0", commands)
        self.assertTrue(any(command.startswith("execute as @e[type=minecraft:item,")
                            and command.endswith("scoreboard players add #gate_entities iedctr_loot 1")
                            for command in commands))
        conditions = [call.args[0] for call in calls if call[0] == "barrier"]
        self.assertIn("if score #gate_entities iedctr_loot matches 1", conditions)
        self.assertTrue(conditions[-1].startswith("if items entity @e[type=minecraft:item,"))
        self.assertTrue(conditions[-1].endswith("contents " + q.GATE + "[minecraft:count=1]"))
        self.assertFalse(any(command.startswith(("summon ", "give ", "loot ", "item "))
                             for command in commands))

    def test_failed_empty_loot_area_aborts_before_destroy(self):
        server = Mock(unsafe=True)
        server.barrier.side_effect = q.QualificationError("old item exists")
        with self.assertRaises(q.QualificationError):
            q.assert_gate_destroy_loot(server)
        server.send.assert_not_called()

    def test_restart_assertions_precede_any_world_mutation(self):
        servers = []
        def make_server(root, phase):
            server = Mock(unsafe=True)
            server.phase = phase
            server.lines = []
            server.assertions = []
            servers.append(server)
            return server
        with patch.object(q, "ConsoleServer", side_effect=make_server):
            result = q.qualify(Path("unused"))
        self.assertEqual(2, len(servers))
        restart_calls = servers[1].method_calls
        first_mutation = next(i for i, call in enumerate(restart_calls) if call[0] == "send" and call.args[0].startswith(("setblock ", "fill ")))
        assertions = [c for c in restart_calls[:first_mutation] if c[0] == "assert_block"]
        self.assertEqual(list(q.STATES), [c.args for c in assertions])
        self.assertFalse(result["realPlayerClicksVerified"])
        self.assertFalse(result["clientRenderingVerified"])
        self.assertFalse(result["playerClimbingVerified"])
        self.assertFalse(result["playerBreakingVerified"])
        self.assertFalse(result["otherLootVerified"])
        self.assertEqual(1, result["consoleGateLootVerified"]["stackCount"])
        self.assertEqual(2, len(result["phases"]))
        for server in servers:
            server.stop.assert_called_once()
            server.close.assert_called_once()


if __name__ == "__main__":
    unittest.main()
