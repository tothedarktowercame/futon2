import importlib.util
import datetime
import hashlib
import os
import json
import contextlib
import io
import subprocess
import tempfile
import unittest
from unittest import mock
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LOOP = ROOT / "scripts" / "wm_click_repair_loop.py"
DEBUGGER = ROOT / "scripts" / "wm_click_debugger.bb"

import wm_click_hermetic  # noqa: E402 (needs the test dir on sys.path first)


# The supervisor's state paths are absolute (~/code/futon2/data/wm-click-loop),
# so a test that reaches log()/record() without patching them writes the
# production loop.log or ledger from ANY checkout, worktrees included. That
# happened on 2026-11-05 (WRITE-REGISTER-5's new log line: 15 fixture lines in
# the real loop.log). Every loaded module therefore starts with its state in a
# throwaway directory; tests that need a specific STATE still patch it.
_TEST_STATE = tempfile.TemporaryDirectory(prefix="wm-click-loop-test-state-")


def load_loop():
    spec = importlib.util.spec_from_file_location("wm_click_repair_loop", LOOP)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    state = Path(_TEST_STATE.name)
    module.STATE = state
    module.LEDGER = state / "ledger.jsonl"
    module.LOGFILE = state / "loop.log"
    module.STOP_FILE = state / "STOP"
    module.ALERTS = state / "alerts.jsonl"
    module.ACKNOWLEDGED = state / "acknowledged.json"
    module.DEBUGGER = state / "debugger"
    # Hermeticity: jvm_eval/get_json/post_json/ensure_seat and the Agency
    # preview / debugger seams refuse loudly; a test needing a value patches
    # in its own fake.
    return wm_click_hermetic.harden(module)


class RepairLoopRootTest(unittest.TestCase):
    def test_isolated_code_root_drives_all_campaign_paths(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td) / "campaign"
            env = dict(os.environ, WM_CODE_ROOT=str(root),
                       BASE="http://127.0.0.1:17070")
            program = (
                "import importlib.util, json; "
                f"s=importlib.util.spec_from_file_location('isolated_loop',{str(LOOP)!r}); "
                "m=importlib.util.module_from_spec(s); s.loader.exec_module(m); "
                "print(json.dumps({'code':str(m.CODE),'f2':str(m.F2),"
                "'f3c':str(m.F3C),'agency':m.AGENCY,'state':str(m.STATE)}))"
            )
            result = subprocess.run(["python3", "-c", program], env=env,
                                    text=True, capture_output=True, check=True)
            observed = json.loads(result.stdout)
            self.assertEqual(str(root), observed["code"])
            self.assertEqual(str(root / "futon2"), observed["f2"])
            self.assertEqual(str(root / "futon3c"), observed["f3c"])
            self.assertEqual("http://127.0.0.1:17070", observed["agency"])
            self.assertEqual(str(root / "futon2" / "data" / "wm-click-loop"),
                             observed["state"])


class RepairLoopCastTest(unittest.TestCase):
    def test_click_write_manifest_applies_writer_stamps_and_skips_malformed(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            root = base / "repo"
            stamps = root / "wm-write-stamps"
            root.mkdir()
            named = root / "named.edn"
            unnamed = root / "unnamed.edn"

            def fake_fire(n, args, since):
                named.write_text("named")
                unnamed.write_text("unnamed")
                stamps.mkdir()
                stamp_file = stamps / "wm-click-stamped.jsonl"
                stamp_file.write_text(
                    json.dumps({"path": str(named.resolve()),
                                "writer": "fixture/writer", "run-id": "run-stamped",
                                "click-id": "wm-click-stamped", "at": "now"})
                    + "\n{malformed\n")
                return {"n": n, "click-id": "wm-click-stamped",
                        "run-id": "run-stamped", "outcome": "ok",
                        "since-epoch": since}

            with mock.patch.object(loop, "STATE", base / "state"), \
                 mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop, "WRITE_STAMP_ROOT", stamps), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire):
                click = loop.fire_with_write_manifest(1, object(), 123)

            manifest = json.loads(Path(click["write_manifest"]["path"]).read_text())
            entries = {Path(entry["path"]).name: entry for entry in manifest["entries"]}
            self.assertEqual("stamped", entries["named.edn"]["attribution"])
            self.assertEqual("fixture/writer", entries["named.edn"]["writer"])
            self.assertEqual("run-stamped", entries["named.edn"]["run-id"])
            self.assertEqual("click-window/unattributed",
                             entries["unnamed.edn"]["attribution"])
            self.assertEqual("stamped", entries["wm-click-stamped.jsonl"]["attribution"])
            self.assertEqual("write-stamp", entries["wm-click-stamped.jsonl"]["writer"])
            self.assertEqual(2, manifest["stamped_count"])
            self.assertEqual(1, manifest["unattributed_count"])
            self.assertEqual(2, click["write_manifest"]["stamped"])
            self.assertEqual(1, click["write_manifest"]["unattributed"])
            self.assertEqual(1, manifest["stamp_lines_unreadable"])

    def test_click_write_manifest_prunes_and_skips_large_hash(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            root = base / "repo"
            root.mkdir()
            (root / ".git").mkdir()
            (root / "target").mkdir()
            outside = base / "outside"
            outside.mkdir()
            linked = root / "linked"
            linked.symlink_to(outside, target_is_directory=True)
            normal = root / "normal.txt"
            normal.write_text("before")

            def fake_fire(n, args, since):
                (root / ".git" / "state").write_text("git")
                (root / "target" / "build").write_text("build")
                (outside / "escaped").write_text("outside")
                normal.write_text("after")
                with (root / "large.bin").open("wb") as output:
                    output.truncate(loop.WRITE_HASH_LIMIT + 1)
                return {"n": n, "click-id": "wm-click-wide-roots",
                        "outcome": "ok", "since-epoch": since}

            with mock.patch.object(loop, "STATE", base / "state"), \
                 mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire), \
                 mock.patch.object(loop, "hash_file", wraps=loop.hash_file) as hashed:
                click = loop.fire_with_write_manifest(1, object(), 123)

            manifest = json.loads(Path(click["write_manifest"]["path"]).read_text())
            entries = {Path(entry["path"]).name: entry
                       for entry in manifest["entries"]}
            self.assertEqual({"normal.txt", "large.bin"}, set(entries))
            self.assertEqual("skipped-large", entries["large.bin"]["hash"])
            self.assertNotIn("sha256", entries["large.bin"])
            self.assertNotIn(root / "large.bin",
                             [call.args[0] for call in hashed.call_args_list])
            self.assertGreaterEqual(manifest["snapshot_seconds"]["before"], 0)
            self.assertGreaterEqual(manifest["snapshot_seconds"]["after"], 0)

    def test_snapshot_write_roots_skips_unreadable_directory(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            normal = root / "normal.txt"
            normal.write_text("visible")

            def unreadable_walk(top, topdown, onerror, followlinks):
                onerror(PermissionError("unreadable"))
                yield str(root), [], [normal.name]

            with mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop.os, "walk", side_effect=unreadable_walk):
                snapshot, seconds = loop.snapshot_write_roots()
            self.assertIn(str(normal.resolve()), snapshot)
            self.assertGreaterEqual(seconds, 0)

    def test_snapshot_write_roots_real_bad_cases(self):
        # claude-6 review: real filesystem cases, no mocked walk.
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            root = Path(td) / "repo"
            nested = root / "data"
            nested.mkdir(parents=True)
            (nested / "row.edn").write_text("x")
            (root / "loop").symlink_to(root, target_is_directory=True)
            locked = root / "locked"
            locked.mkdir()
            (locked / "hidden.txt").write_text("h")
            (root / ".git").mkdir()
            (root / ".git" / "HEAD").write_text("ref")
            later = Path(td) / "created-later"
            os.chmod(locked, 0)
            try:
                # nested root listed before its parent, a root that does not
                # exist yet, a symlink loop and an unreadable directory
                with mock.patch.object(loop, "WRITE_ROOTS",
                                       [nested, root, later]), \
                     mock.patch.object(loop.os, "walk",
                                       wraps=loop.os.walk) as walked:
                    snapshot, _ = loop.snapshot_write_roots()
                    later.mkdir()
                    (later / "new.txt").write_text("n")
                    again, _ = loop.snapshot_write_roots()
            finally:
                os.chmod(locked, 0o700)
            names = sorted(Path(p).relative_to(td).as_posix() for p in snapshot)
            self.assertEqual(["repo/data/row.edn"], names)
            self.assertIn(str(later / "new.txt"), again)
            # root once, nested skipped; then root and later
            self.assertEqual(3, walked.call_count)

    def test_stamp_outside_window_or_other_click_adds_nothing(self):
        # claude-6 review: a stamp only labels a path the census saw change.
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            root = base / "repo"
            stamps = base / "stamps"
            root.mkdir()
            stamps.mkdir()
            unchanged = root / "unchanged.edn"
            unchanged.write_text("same")
            changed = root / "changed.edn"

            def fake_fire(n, args, since):
                changed.write_text("new")
                rows = [{"path": str(unchanged.resolve()), "writer": "w",
                         "click-id": "wm-click-a"},
                        {"path": str(base / "nowhere.edn"), "writer": "w",
                         "click-id": "wm-click-a"},
                        {"path": str(changed.resolve()), "writer": "w",
                         "click-id": "wm-click-other"}]
                (stamps / "wm-click-a.jsonl").write_text(
                    "".join(json.dumps(r) + "\n" for r in rows))
                return {"n": n, "click-id": "wm-click-a", "outcome": "ok",
                        "since-epoch": since}

            with mock.patch.object(loop, "STATE", base / "state"), \
                 mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop, "WRITE_STAMP_ROOT", stamps), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire):
                click = loop.fire_with_write_manifest(1, object(), 123)

            manifest = json.loads(Path(click["write_manifest"]["path"]).read_text())
            self.assertEqual([str(changed.resolve())],
                             [e["path"] for e in manifest["entries"]])
            self.assertEqual("click-window/unattributed",
                             manifest["entries"][0]["attribution"])
            self.assertEqual(0, manifest["stamped_count"])
            self.assertEqual(1, manifest["stamp_lines_unreadable"])

    def test_click_write_manifest_registers_window_changes_and_ledger_pin(self):
        loop = load_loop()
        # the data root the supervisor really writes to, not this checkout's
        real_data = loop.F2 / "data"
        real_mark = max((p.stat().st_mtime_ns for p in real_data.rglob("*")
                         if p.is_file()), default=0)
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            state = base / "state"
            roots = [base / "f2-data", base / "f2-evidence", base / "f3-data"]
            for root in roots:
                root.mkdir()
            untouched = roots[0] / "untouched.edn"
            modified = roots[0] / "modified.edn"
            removed = roots[1] / "removed.edn"
            untouched.write_text("same")
            modified.write_text("before")
            removed.write_text("gone")

            def fake_fire(n, args, since):
                (roots[0] / "new.edn").write_text("new bytes")
                modified.write_text("after")
                removed.unlink()
                return {"n": n, "click-id": "wm-click-write-1", "run-id": "run-1",
                        "outcome": "ok", "since-epoch": since}

            ledger = state / "ledger.jsonl"
            with mock.patch.object(loop, "STATE", state), \
                 mock.patch.object(loop, "LEDGER", ledger), \
                 mock.patch.object(loop, "WRITE_ROOTS", roots), \
                 mock.patch.object(loop, "WRITE_STAMP_ROOT", base / "stamps"), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire):
                click = loop.fire_with_write_manifest(1, object(), 123)
                loop.record({"event": "click", **click})

            pin = click["write_manifest"]
            manifest_path = Path(pin["path"])
            manifest_bytes = manifest_path.read_bytes()
            self.assertEqual(hashlib.sha256(manifest_bytes).hexdigest(), pin["sha256"])
            manifest = json.loads(manifest_bytes)
            entries = {entry["path"]: entry for entry in manifest["entries"]}
            self.assertEqual("new", entries[str(roots[0] / "new.edn")]["change"])
            self.assertEqual(hashlib.sha256(b"new bytes").hexdigest(),
                             entries[str(roots[0] / "new.edn")]["sha256"])
            self.assertEqual("modified", entries[str(modified)]["change"])
            self.assertEqual("removed", entries[str(removed)]["change"])
            self.assertNotIn(str(untouched), entries)
            self.assertTrue(all(entry.get("attribution") ==
                                "click-window/unattributed"
                                for entry in entries.values()))
            self.assertEqual(3, pin["count"])
            self.assertEqual(0, pin["stamped"])
            self.assertEqual(3, pin["unattributed"])
            row = json.loads(ledger.read_text().splitlines()[-1])
            self.assertEqual(pin, row["write_manifest"])
        self.assertEqual(real_mark, max((p.stat().st_mtime_ns for p in real_data.rglob("*")
                                        if p.is_file()), default=0))

    def test_click_write_manifest_sees_same_size_rewrite_and_live_appends(self):
        # review (claude-6): a same-size rewrite is caught by mtime; a file
        # still being appended after the click is listed as unstable, not a
        # reason to lose the whole manifest
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            root = base / "data"
            root.mkdir()
            same = root / "same-size.edn"
            same.write_text("aaaa")
            os.utime(same, ns=(1_000_000_000, 1_000_000_000))
            live = root / "live.log"
            live.write_text("x")
            os.utime(live, ns=(1_000_000_000, 1_000_000_000))
            real_hash = loop.hash_file

            def fake_fire(n, args, since):
                same.write_text("bbbb")
                live.write_text("xy")
                return {"n": n, "click-id": "wm-click-same-size", "outcome": "ok",
                        "since-epoch": since}

            def appending_hash(path):
                digest = real_hash(path)
                if path == live:
                    with live.open("a") as fh:
                        fh.write("z")
                return digest

            with mock.patch.object(loop, "STATE", base / "state"), \
                 mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop, "WRITE_STAMP_ROOT", base / "stamps"), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire), \
                 mock.patch.object(loop, "hash_file", side_effect=appending_hash):
                click = loop.fire_with_write_manifest(1, object(), 123)
            manifest = json.loads(Path(click["write_manifest"]["path"]).read_text())
            entries = {Path(e["path"]).name: e for e in manifest["entries"]}
            self.assertEqual("modified", entries["same-size.edn"]["change"])
            self.assertEqual(hashlib.sha256(b"bbbb").hexdigest(),
                             entries["same-size.edn"]["sha256"])
            self.assertNotIn("unstable", entries["same-size.edn"])
            self.assertTrue(entries["live.log"]["unstable"])
            self.assertEqual(2, click["write_manifest"]["count"])
            manifest_dir = base / "state" / "write-manifests"
            self.assertNotIn(str(manifest_dir), " ".join(entries))

    def test_click_write_manifest_hash_failure_is_typed_and_preserves_result(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            root = base / "data"
            root.mkdir()

            def fake_fire(n, args, since):
                (root / "new.edn").write_text("bytes")
                return {"n": n, "click-id": "wm-click-write-failed",
                        "outcome": "incomplete", "since-epoch": since}

            with mock.patch.object(loop, "STATE", base / "state"), \
                 mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop, "WRITE_STAMP_ROOT", base / "stamps"), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire), \
                 mock.patch.object(loop, "hash_file", side_effect=OSError("hash refused")):
                click = loop.fire_with_write_manifest(1, object(), 123)
            self.assertEqual("incomplete", click["outcome"])
            self.assertEqual("failed", click["write_manifest"]["status"])
            self.assertIn("hash refused", click["write_manifest"]["error"])
            self.assertNotIn("stamped", click["write_manifest"])
            self.assertNotIn("unattributed", click["write_manifest"])

    def test_click_write_manifest_logs_only_unattributed_writes(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            root = base / "data"
            root.mkdir()
            first = root / "first.edn"
            second = root / "second.edn"
            claimed = root / "claimed.edn"
            before_listing = set(root.iterdir())
            click = {"click-id": "wm-click-log", "n": 1, "run-id": "run-log"}
            with mock.patch.object(loop, "STATE", base / "state"), \
                 mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop, "read_write_stamps",
                                   return_value=({str(claimed.resolve()):
                                                  {"writer": "fixture", "run-id": "run-log"}},
                                                 0, None)), \
                 mock.patch.object(loop, "log") as logged:
                first.write_text("one")
                second.write_text("two")
                claimed.write_text("claimed")
                after, _ = loop.snapshot_write_roots()
                summary = loop.write_click_manifest(
                    click, "start", "end", {}, after,
                    {"before": 0.0, "after": 0.0})
            manifest = json.loads(Path(summary["path"]).read_text())
            self.assertEqual(2, manifest["unattributed_count"])
            self.assertEqual(1, manifest["stamped_count"])
            self.assertEqual(2, summary["unattributed"])
            self.assertEqual(1, summary["stamped"])
            logged.assert_called_once()
            message = logged.call_args.args[0]
            self.assertIn("wm-click-log", message)
            self.assertIn("2", message)
            self.assertIn(summary["path"], message)
            self.assertEqual(before_listing | {first, second, claimed}, set(root.iterdir()))

            stamped_after = {str(first.resolve()): (3, first.stat().st_mtime_ns)}
            with mock.patch.object(loop, "STATE", base / "other-state"), \
                 mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop, "read_write_stamps",
                                   return_value=({str(first.resolve()):
                                                  {"writer": "fixture", "run-id": "run-log"}},
                                                 0, None)), \
                 mock.patch.object(loop, "log") as no_log:
                zero = loop.write_click_manifest(
                    {"click-id": "wm-click-zero", "n": 2}, "start", "end", {},
                    stamped_after, {"before": 0.0, "after": 0.0})
            self.assertEqual(0, zero["unattributed"])
            no_log.assert_not_called()

    def test_repair_kondo_judgment_does_not_use_stale_project_cache(self):
        loop = load_loop()
        with mock.patch.object(loop, "git", return_value="scripts/example.clj\n"), \
             mock.patch.object(Path, "exists", return_value=True), \
             mock.patch.object(loop, "sh",
                               return_value=subprocess.CompletedProcess([], 0, "", "")) as run:
            self.assertEqual("", loop.kondo_errors([(loop.F2, "abc123")]))
        self.assertEqual(
            ("clj-kondo", "--cache", "false", "--lint",
             str(loop.F2 / "scripts/example.clj")),
            run.call_args.args)

    def test_changed_identity_dependency_reloads_when_runner_is_unchanged(self):
        loop = load_loop()
        family = loop.F2 / "src/futon2/aif/wm/family_selection.clj"
        inventory = ROOT / "src/futon2/aif/load_identity.clj"
        unrelated = "\n(def unrelated {'outside.inventory \"/tmp/outside.clj\"})\n"
        with tempfile.NamedTemporaryFile("w") as source:
            source.write(inventory.read_text() + unrelated)
            source.flush()
            with mock.patch.object(loop, "SOURCE_IDENTITY_INVENTORY", Path(source.name)), \
             mock.patch.object(loop, "click_running", return_value=False), \
             mock.patch.object(loop, "changed_click_files", return_value=[family]), \
             mock.patch.object(loop, "dirty", return_value=False), \
             mock.patch.object(loop, "jvm_eval", side_effect=lambda form, timeout=600: {
                 "rc": 0, "out": ":standing-reset" if "reset-context!" in form else ":reloaded", "err": ""}) as reload, \
             mock.patch.object(loop, "record") as record, \
             mock.patch.object(loop, "log"):
                identities = loop.source_identity_inventory()
                self.assertIn("futon2.aif.observation-checks", identities)
                self.assertIn("futon2.aif.wm.family-selection", identities)
                self.assertNotIn("outside.inventory", identities)
                ok, text, _ = loop.reload_changed(123)
        self.assertTrue(ok)
        self.assertEqual("", text)
        self.assertTrue(any("futon2.aif.wm.family-selection" in c.args[0] for c in reload.call_args_list))
        self.assertIn("reset-context!", reload.call_args.args[0], "the standing context reset is the last eval")
        event = record.call_args.args[0]
        self.assertEqual(["futon2.aif.wm.family-selection"], event["source-identity-dependencies"])
        self.assertNotIn("futon2.aif.full-loop-runner", event["source-identity-dependencies"])

    def test_dependency_closure_follows_canonical_requires(self):
        loop = load_loop()
        selection = loop.F2 / "src/futon2/aif/wm/family_selection.clj"
        closure = loop.dependency_closure([selection])
        namespaces = {loop.ns_of(path)[0] for path in closure}
        self.assertIn("futon2.aif.wm.family-selection", namespaces)
        self.assertIn("futon2.aif.wm.family-canonical-g", namespaces)

    def test_reload_when_idle_retries_deferred_result_before_success(self):
        loop = load_loop()
        with mock.patch.object(loop, "reload_changed", side_effect=[
                (None, "click-in-flight", 123), (True, "", 456)]) as reload, \
             mock.patch.object(loop.time, "sleep") as sleep:
            result = loop.reload_when_idle(123)
        self.assertEqual((True, "", 456), result)
        self.assertEqual(2, reload.call_count)
        sleep.assert_called_once_with(60)

    def test_active_click_defers_with_zero_reload_and_no_launch(self):
        loop = load_loop()
        with mock.patch.object(loop, "click_running", return_value=True), \
             mock.patch.object(loop, "debugger_stop", return_value=None), \
             mock.patch.object(loop, "jvm_eval") as reload, \
             mock.patch.object(loop.subprocess, "Popen") as launch, \
             mock.patch.object(loop, "record") as record, \
             mock.patch.object(loop, "log"):
            result = loop.reload_changed(123)
        self.assertEqual((None, "click-in-flight", 123), result)
        reload.assert_not_called()
        launch.assert_not_called()
        self.assertEqual("click-in-flight", record.call_args.args[0]["reason"])

    def test_cast_has_no_stale_or_implicit_identity(self):
        loop = load_loop()
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                loop.parse_args(["run", "--clicks", "1"])
        args = loop.parse_args([
            "run", "--clicks", "1", "--owner", "codex-68",
            "--issuing-caller", "codex-68", "--author", "wm-author",
            "--reviewer", "wm-reviewer", "--repair-reviewer", "wm-repair-reviewer",
        ])
        self.assertEqual("wm-repair-reviewer", args.repair_seat)
        self.assertNotIn("proof", repr(vars(args)))

    def test_observation_only_is_explicit_and_defaults_off(self):
        loop = load_loop()
        base = ["run", "--clicks", "3", "--owner", "codex-68",
                "--issuing-caller", "codex-68", "--author", "wm-author",
                "--reviewer", "wm-reviewer",
                "--repair-reviewer", "wm-repair-reviewer"]
        self.assertFalse(loop.parse_args(base).observe_only)
        self.assertTrue(loop.parse_args(base + ["--observe-only"]).observe_only)

    def test_direct_task_is_explicit_and_defaults_absent(self):
        loop = load_loop()
        base = ["run", "--clicks", "1", "--owner", "codex-68",
                "--issuing-caller", "joe", "--author", "wm-author",
                "--reviewer", "wm-reviewer",
                "--repair-reviewer", "wm-repair-reviewer"]
        self.assertIsNone(loop.parse_args(base).task)
        self.assertEqual("T-ladder-01-loop-turns-over",
                         loop.parse_args(base + ["--task", "T-ladder-01-loop-turns-over"]).task)

    def test_repair_dispatch_uses_the_loop_agency_base(self):
        loop = load_loop()
        completed = subprocess.CompletedProcess([], 0, '{"job-id":"job-1"}', "")
        with mock.patch.object(loop, "sh", return_value=completed) as send:
            self.assertEqual("job-1", loop.dispatch("wm-repair-reviewer", "repair"))
        self.assertIn("--base", send.call_args.args)
        self.assertIn(loop.AGENCY, send.call_args.args)

    def test_observation_only_debugger_stop_never_dispatches_repair(self):
        loop = load_loop()
        args = loop.parse_args([
            "run", "--clicks", "3", "--observe-only", "--owner", "codex-68",
            "--issuing-caller", "codex-68", "--author", "wm-author",
            "--reviewer", "wm-reviewer",
            "--repair-reviewer", "wm-repair-reviewer",
        ])
        stop = {"run-id": "run-observed", "phase": "selection",
                "condition-kind": "wm/selection-terminal-abstention"}
        with tempfile.TemporaryDirectory() as td, \
             mock.patch.object(loop, "abort_held_run",
                               return_value={"out": "{:delivered true}"}) as abort, \
             mock.patch.object(loop, "repair") as repair, \
             mock.patch.object(loop, "record") as record, \
             mock.patch.object(loop, "log"):
            since = loop.handle_held_stop(
                args, stop, [], Path(td) / "held.md", 123)
        self.assertEqual(123, since)
        abort.assert_called_once_with("run-observed")
        repair.assert_not_called()
        events = [call.args[0]["event"] for call in record.call_args_list]
        self.assertEqual(["external-repair-skipped", "debugger-resumed"], events)

    def test_no_defect_held_stop_retries_once_then_aborts(self):
        # Click c31a0166: a typed :clock/store-busy hold, repair verdict
        # no-defect, and the loop aborted instead of the designed :retry.
        loop = load_loop()
        args = loop.parse_args([
            "run", "--clicks", "3", "--owner", "codex-68",
            "--issuing-caller", "codex-68", "--author", "wm-author",
            "--reviewer", "wm-reviewer",
            "--repair-reviewer", "wm-repair-reviewer",
        ])
        stop = {"run-id": "run-busy", "phase": "cascade-resume",
                "condition-kind": "clock/store-busy"}
        with tempfile.TemporaryDirectory() as td, \
             mock.patch.object(loop, "repair",
                               return_value=("no-defect", "detail")), \
             mock.patch.object(loop, "retry_held_run",
                               return_value={"out": "{:delivered true}"}) as retry, \
             mock.patch.object(loop, "abort_held_run",
                               return_value={"out": "{:delivered true}"}) as abort, \
             mock.patch.object(loop, "record"), \
             mock.patch.object(loop, "log"):
            loop.handle_held_stop(args, stop, [], Path(td) / "held.md", 1)
            retry.assert_called_once_with("run-busy")
            abort.assert_not_called()
            loop.handle_held_stop(args, stop, [], Path(td) / "held.md", 1)
            retry.assert_called_once_with("run-busy")
            abort.assert_called_once_with("run-busy")

    def test_repaired_held_stop_that_re_holds_is_aborted_not_repaired_again(self):
        # Run 6d57248a: a "repaired" verdict, hot-load and retry, then the
        # same phase held again and the loop skipped it as already handled.
        loop = load_loop()
        args = loop.parse_args([
            "run", "--clicks", "3", "--owner", "codex-68",
            "--issuing-caller", "codex-68", "--author", "wm-author",
            "--reviewer", "wm-reviewer",
            "--repair-reviewer", "wm-repair-reviewer",
        ])
        stop = {"run-id": "run-rehold", "phase": "close-refusal",
                "condition-kind": "illegal-refusal"}
        with tempfile.TemporaryDirectory() as td, \
             mock.patch.object(loop, "repair",
                               return_value=("repaired", "detail")) as rep, \
             mock.patch.object(loop, "reload_when_idle",
                               return_value=(True, "", 1)), \
             mock.patch.object(loop, "retry_held_run",
                               return_value={"out": "{:delivered true}"}) as retry, \
             mock.patch.object(loop, "abort_held_run",
                               return_value={"out": "{:delivered true}"}) as abort, \
             mock.patch.object(loop, "record"), \
             mock.patch.object(loop, "log"):
            loop.handle_held_stop(args, stop, [], Path(td) / "held.md", 1)
            retry.assert_called_once_with("run-rehold")
            abort.assert_not_called()
            loop.handle_held_stop(args, stop, [], Path(td) / "held.md", 1)
            rep.assert_called_once()
            retry.assert_called_once_with("run-rehold")
            abort.assert_called_once_with("run-rehold")

    def test_each_click_resets_the_explicit_role_cast(self):
        loop = load_loop()
        args = loop.parse_args([
            "run", "--clicks", "1", "--owner", "codex-68",
            "--issuing-caller", "codex-68", "--author", "wm-author",
            "--reviewer", "wm-reviewer",
            "--repair-reviewer", "wm-repair-reviewer",
        ])
        rows = {seat: {"status": "idle", "invoke-ready?": True}
                for seat in (args.author, args.reviewer, args.repair_reviewer)}
        with mock.patch.object(loop, "get_json", return_value={"agents": rows}), \
             mock.patch.object(loop, "post_json",
                               return_value={"ok": True,
                                             "old-session-id": "old"}) as post, \
             mock.patch.object(loop, "record") as record, \
             mock.patch.object(loop, "log"):
            loop.reset_cast_sessions(args)
        self.assertEqual(3, post.call_count)
        self.assertEqual("cast-reset", record.call_args.args[0]["event"])

    def test_each_click_can_reset_a_restored_role_cast(self):
        loop = load_loop()
        args = loop.parse_args([
            "run", "--clicks", "1", "--owner", "codex-1",
            "--issuing-caller", "codex-1", "--author", "wm-author",
            "--reviewer", "wm-reviewer",
            "--repair-reviewer", "wm-repair-reviewer",
        ])
        rows = {seat: {"status": "restored", "invoke-ready?": True}
                for seat in (args.author, args.reviewer, args.repair_reviewer)}
        with mock.patch.object(loop, "get_json", return_value={"agents": rows}), \
             mock.patch.object(loop, "post_json",
                               return_value={"ok": True,
                                             "old-session-id": None}) as post, \
             mock.patch.object(loop, "record"), \
             mock.patch.object(loop, "log"):
            loop.reset_cast_sessions(args)
        self.assertEqual(3, post.call_count)

    def test_cast_reset_fails_closed_when_a_role_is_busy(self):
        loop = load_loop()
        args = loop.parse_args([
            "run", "--clicks", "1", "--owner", "codex-68",
            "--issuing-caller", "codex-68", "--author", "wm-author",
            "--reviewer", "wm-reviewer",
            "--repair-reviewer", "wm-repair-reviewer",
        ])
        rows = {seat: {"status": "idle", "invoke-ready?": True}
                for seat in (args.author, args.reviewer, args.repair_reviewer)}
        rows["wm-reviewer"]["status"] = "invoking"
        with mock.patch.object(loop, "get_json", return_value={"agents": rows}), \
             mock.patch.object(loop, "post_json") as post:
            with self.assertRaisesRegex(RuntimeError, "wm-reviewer"):
                loop.reset_cast_sessions(args)
        post.assert_not_called()

    def test_reading_refusal_is_not_reported_as_no_policy_family(self):
        record = '''
{:selected-target-reading
 {:schema :wm/selected-target-reading-request-v1
  :status :refused
  :kind :selected-target-reading-validation-refused
  :published false
  :target "M-one"
  :detail {:stderr "display cue offsets do not match"}}
 :decision {:abstention {:status :abstained
                         :targets [{:target "M-one"
                                    :kind :no-policy-family
                                    :declines []}]}}}
'''
        with tempfile.NamedTemporaryFile("w", suffix=".edn") as fh:
            fh.write(record)
            fh.flush()
            result = subprocess.run(
                ["bb", str(DEBUGGER), fh.name], cwd=ROOT,
                text=True, capture_output=True, check=True)
        self.assertIn("selected-target-reading-validation-refused/before-cascade-construction",
                      result.stdout)
        self.assertNotIn("no-policy-family/unknown", result.stdout)
        self.assertIn('"phase":"before-cascade-construction"', result.stdout)

    def test_lifecycle_formatter_qualifies_sixty_non_actionable_findings(self):
        loop = load_loop()
        census = {
            "schema": "wm/repair-obligation-lifecycle-census-v1",
            "finding-count": 60,
            "reconciled?": True,
            "actionable-open": {"count": 0, "ids": []},
            "awaiting-validation": {"count": 20,
                                    "ids": [f"awaiting-{n:02d}" for n in range(20)]},
            "environmental-hold": {"count": 20,
                                   "ids": [f"environment-{n:02d}" for n in range(20)]},
            "closed": {"count": 20, "ids": [f"closed-{n:02d}" for n in range(20)]},
        }
        rendered = "\n".join(loop.lifecycle_display(census))
        self.assertIn("repair lifecycle authority: wm/repair-obligation-lifecycle-census-v1",
                      rendered)
        self.assertIn("actionable-open: count=0 ids=[]", rendered)
        self.assertIn("awaiting-validation: count=20", rendered)
        self.assertIn("environmental-hold: count=20", rendered)
        self.assertIn("closed: count=20", rendered)
        self.assertNotIn("open stop-lines", rendered)

    def test_lifecycle_formatter_marks_old_aggregate_records_unavailable(self):
        loop = load_loop()
        rendered = "\n".join(loop.lifecycle_display(None))
        self.assertIn("unavailable", rendered)
        self.assertIn("legacy", rendered)

    def test_repair_packet_bounds_large_shared_artifacts(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            page = root / "held.md"
            log = root / "click.log"
            page.write_text("held evidence " + "x" * 200_000)
            log.write_text("click evidence " + "y" * 200_000)
            click = {
                "log": str(log),
                "last-result": {
                    "click-id": "wm-click-test",
                    "outcome": "abstained",
                    "run-record": "/shared/full-run-record.edn",
                    "enormous-field": "z" * 200_000,
                },
            }
            packet = loop.repair_packet(
                "wm-click-loop-repair-test", "selection stopped", click,
                ["prior " + "p" * 200_000], page)
        self.assertLessEqual(len(packet), loop.REPAIR_PROMPT_MAX_CHARS)
        self.assertLessEqual(len(packet), 1_800)
        self.assertIn(str(page), packet)
        self.assertIn("/shared/full-run-record.edn", packet)
        self.assertNotIn("enormous-field", packet)
        self.assertIn("VERDICT: repaired", packet)



class ContinuousModeTest(unittest.TestCase):
    """--continuous: the Lean verdict is read, alerts are queued, and only a
    repeated signature pauses the loop."""

    TWO_RUN_ALERT = ('{":run-record" "R" ":conforms" false ":runs" [":work" ":selection"] '
                     '":work-status" ":not-run" ":selection-status" ":not-exercised" ":verdicts" {} '
                     '":selection-verdicts" {":Q1" ":unverifiable" ":Q6" ":unverifiable"}}')
    TWO_RUN_MIXED = ('{":run-record" "R" ":conforms" false ":work-status" ":evaluated" '
                     '":selection-status" ":exercised" ":verdicts" {":Q1" ":holds" ":Q7" ":violated" '
                     '":Q9" ":violated"} ":selection-verdicts" {":Q1" ":holds" ":Q11" ":holds"}}')
    TWO_RUN_CONFORMS = ('{":run-record" "R" ":conforms" true ":work-status" ":evaluated" '
                        '":selection-status" ":exercised" ":verdicts" {":Q1" ":holds"} '
                        '":selection-verdicts" {":Q1" ":holds"}}')

    def verdict_of(self, loop, text):
        with tempfile.TemporaryDirectory() as td:
            record = Path(td) / "tick-run-record.edn"
            record.write_text("{}")
            Path(str(record) + ".requirements.edn").write_text(text)
            return loop.read_verdict(str(record))

    def test_verdict_is_read_by_pattern_and_signed(self):
        loop = load_loop()
        v = self.verdict_of(loop, self.TWO_RUN_ALERT)
        self.assertFalse(v["conforms"])
        self.assertEqual({"Q1": "unverifiable", "Q6": "unverifiable"}, v["selection"])
        self.assertEqual(["selection Q1 unverifiable", "selection Q6 unverifiable",
                          "selection not-exercised", "work not-run"], loop.click_alert(v))
        mixed = self.verdict_of(loop, self.TWO_RUN_MIXED)
        self.assertEqual(["work Q7 violated", "work Q9 violated"], loop.click_alert(mixed))
        self.assertIsNone(loop.click_alert(self.verdict_of(loop, self.TWO_RUN_CONFORMS)))

    def test_refused_selection_record_is_found_by_click_id(self):
        """Clicks 1 and 2 of 2026-11-01 17:28Z: an outer refusal writes a selection
        record and no tick; last-result names no run record; the verdict beside the
        selection record must still be read."""
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td, mock.patch.object(loop, "F2", Path(td)):
            runs = Path(td) / "data" / "wm-runs"; runs.mkdir(parents=True)
            rec = runs / "selection-record-2026-11-01-wm-click-abc.edn"; rec.write_text("{}")
            Path(str(rec) + ".requirements.edn").write_text(self.TWO_RUN_ALERT)
            self.assertEqual(str(rec), loop.click_record({"outcome": "unknown"}, "wm-click-abc"))
            self.assertEqual("work not-run", loop.click_alert(loop.read_verdict(
                loop.click_record({}, "wm-click-abc")))[-1])
            self.assertIsNone(loop.click_record({"outcome": "unknown"}, "wm-click-none"))
            tick = runs / "tick-run-record-run-7.edn"; tick.write_text("{}")
            self.assertEqual(str(tick), loop.click_record({"run-id-observation": {"value": "run-7"}}, "wm-click-abc"))

    def test_a_click_that_never_launched_owns_no_earlier_result(self):
        """18:02:46Z: the relaunch hit the single-flight hold (exit 3, no click id)
        and the loop read click 5's last-result as its own, counting a tick."""
        loop = load_loop()
        args = loop.parse_args([
            "run", "--clicks", "1", "--continuous", "--owner", "claude-2",
            "--issuing-caller", "claude-2", "--author", "wm-author",
            "--reviewer", "wm-reviewer", "--repair-reviewer", "wm-repair-reviewer"])
        earlier = {"click-id": "wm-click-earlier", "outcome": "grounded-progress",
                   "run-record": "/nowhere/tick.edn",
                   "run-id-observation": {"status": "present", "value": "run-earlier"}}
        proc = mock.Mock(); proc.poll.return_value = 3; proc.returncode = 3
        with tempfile.TemporaryDirectory() as td, \
             mock.patch.object(loop, "STATE", Path(td)), \
             mock.patch.object(loop, "reset_cast_sessions"), \
             mock.patch.object(loop, "budget_lines", return_value=0), \
             mock.patch.object(loop.subprocess, "Popen", return_value=proc), \
             mock.patch.object(loop, "get_json", return_value={"last-result": earlier}), \
             mock.patch.object(loop, "carried", side_effect=lambda run_id: run_id is not None), \
             mock.patch.object(loop, "log"):
            c = loop.fire(1, args, 123)
        self.assertIsNone(c["click-id"])
        self.assertEqual(3, c["rc"])
        self.assertEqual("not-launched", c["outcome"])
        self.assertIsNone(c["run-id"])
        self.assertFalse(c["carried"])
        self.assertIsNone(c["run-record"])

    def test_reload_resets_the_standing_authority_context(self):
        """19:32Z: after reloading the medic namespaces the serving JVM's memoised
        standing verifier was stale and the gate read the ledger as tampered."""
        loop = load_loop()
        evals = []
        def jvm_eval(form, timeout=600):
            evals.append(form)
            return {"rc": 0, "out": ":reloaded" if "require" in form else ":standing-reset", "err": ""}
        with tempfile.TemporaryDirectory() as td, \
             mock.patch.object(loop, "click_running", return_value=False), \
             mock.patch.object(loop, "reload_plan", return_value=([Path(td) / "a.clj"], [])), \
             mock.patch.object(loop, "dirty", return_value=False), \
             mock.patch.object(loop, "reload_order", return_value=[("futon2.aif.a", Path(td) / "a.clj")]), \
             mock.patch.object(loop, "jvm_eval", side_effect=jvm_eval), \
             mock.patch.object(loop, "record"), mock.patch.object(loop, "log"):
            ok, _, _ = loop.reload_changed(123)
            self.assertTrue(ok)
            self.assertTrue(any("reset-context!" in f for f in evals), evals)
            self.assertLess([i for i, f in enumerate(evals) if "require" in f][-1],
                            [i for i, f in enumerate(evals) if "reset-context!" in f][0],
                            "the reset follows the reloads")
            evals.clear()
        with mock.patch.object(loop, "click_running", return_value=False), \
             mock.patch.object(loop, "reload_plan", return_value=([], [])), \
             mock.patch.object(loop, "jvm_eval", side_effect=jvm_eval), \
             mock.patch.object(loop, "record"), mock.patch.object(loop, "log"):
            loop.reload_changed(123)
            self.assertEqual([], evals, "nothing reloaded, nothing reset")

    def test_nothing_beats_stopping_waits_once_then_pauses_on_an_unchanged_field(self):
        """clicks 12-14 of 2026-11-01: :pick-not-below-abstention three times on one
        input digest; the loop re-fired because the projector's signatures differed."""
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            rec = Path(td) / "selection-record-2026-11-01-wm-click-x.edn"
            rec.write_text('{:selection-outcome {:status :refused, :kind :pick-not-below-abstention, :detail {}} '
                           ':outer/selection {:choice-input-digest {:sha256 "' + "b" * 64 + '"}}}')
            self.assertEqual("b" * 64, loop.nothing_beats_stopping(str(rec)))
            chosen = Path(td) / "selection-record-2026-11-01-wm-click-y.edn"
            chosen.write_text('{:selection-outcome {:status :chosen, :pick "M-x"}}')
            self.assertIsNone(loop.nothing_beats_stopping(str(chosen)))
            self.assertIsNone(loop.nothing_beats_stopping(None))
            # run() records a giraffe pin before its first click (WM hardening
            # package 1): give it a minimal missions-field run under the temp
            # tree, never the real /home/joe/code/storage/giraffe.
            giraffe = Path(td) / "giraffe"
            fixture_run = giraffe / "20260101T000000.000Z-fixture"
            fixture_run.mkdir(parents=True)
            (fixture_run / "manifest.edn").write_text(
                "{:invocation {:edges \"/fixture/holes/missions/edges.json\"\n"
                "  :only-ids [\"T-fixture\"]}\n"
                " :sources {:edges {:path \"/fixture/holes/missions/edges.json\"}}}\n")
            args = loop.parse_args([
                "run", "--clicks", "3", "--continuous", "--owner", "claude-2",
                "--issuing-caller", "claude-2", "--author", "wm-author",
                "--reviewer", "wm-reviewer", "--repair-reviewer", "wm-repair-reviewer"])
            def fire(n, a, since):
                return {"n": n, "rc": 1, "click-id": f"click-{n}", "spent": True, "outcome": "unknown",
                        "run-id": None, "carried": False, "last-result": {}, "log": None,
                        "run-record": str(rec), "verdict": {"file": "V"},
                        "alert": [f"selection Q11 {'violated' if n == 1 else 'unverifiable'}"],
                        "since-epoch": since, "in-flight-repairs": []}
            with mock.patch.object(loop, "STATE", Path(td)), \
                 mock.patch.object(loop, "WRITE_ROOTS", [Path(td) / "f2-data",
                                                          Path(td) / "f2-evidence",
                                                          Path(td) / "f3-data"]), \
                 mock.patch.object(loop, "LEDGER", Path(td) / "ledger.jsonl"), \
                 mock.patch.object(loop, "LOGFILE", Path(td) / "loop.log"), \
                 mock.patch.object(loop, "STOP_FILE", Path(td) / "STOP"), \
                 mock.patch.object(loop, "ALERTS", Path(td) / "alerts.jsonl"), \
                 mock.patch.object(loop, "ACKNOWLEDGED", Path(td) / "ack.json"), \
                 mock.patch.object(loop, "DEBUGGER", Path(td) / "debugger"), \
                 mock.patch.object(loop, "jvm_eval",
                                   side_effect=wm_click_hermetic.healthy_jvm_eval), \
                 mock.patch.object(loop, "GIRAFFE_PIN", Path(td) / "giraffe-pin.edn"), \
                 mock.patch.dict(os.environ, {"GIRAFFE_ROOT": str(giraffe)}), \
                 mock.patch.object(loop, "jvm_start_epoch", return_value=123), \
                 mock.patch.object(loop, "reload_when_idle", return_value=(True, "", 123)), \
                 mock.patch.object(loop, "fire", side_effect=fire), \
                 mock.patch.object(loop.time, "sleep") as sleep, \
                 mock.patch.object(loop, "notify") as notify, \
                 mock.patch.object(loop, "notify_alert"), \
                 mock.patch.object(loop, "repair") as repair, \
                 mock.patch.object(loop, "log"):
                code = loop.run(args)
            self.assertEqual(1, code)
            self.assertIn("no compared policy's G is below the abstention's, twice",
                          notify.call_args[0][3])
            self.assertEqual(1, sum(1 for c in sleep.call_args_list if c.args == (loop.WAIT_SECONDS,)),
                             "one wait for the field to change, then the pause")
            self.assertEqual(0, repair.call_count)

    def test_absent_verdict_is_an_alert_not_conformance(self):
        loop = load_loop()
        self.assertIsNone(loop.read_verdict(None))
        with tempfile.TemporaryDirectory() as td:
            self.assertIsNone(loop.read_verdict(str(Path(td) / "no-such-record.edn")))
        self.assertEqual(["verdict absent"], loop.click_alert(None))

    def run_continuous(self, clicks, acknowledged=None):
        loop = load_loop()
        args = loop.parse_args([
            "run", "--clicks", str(len(clicks)), "--continuous", "--owner", "claude-2",
            "--issuing-caller", "claude-2", "--author", "wm-author",
            "--reviewer", "wm-reviewer", "--repair-reviewer", "wm-repair-reviewer"])
        fired = iter(clicks)

        def fire(n, a, since):
            c = next(fired)
            return {"n": n, "rc": 1 if c["alert"] else 0, "click-id": f"click-{n}", "spent": True,
                    "outcome": c.get("outcome", "ok"), "run-id": f"run-{n}", "carried": c.get("carried", True),
                    "last-result": {}, "log": None, "run-record": "R", "verdict": {"file": "V"},
                    "alert": c["alert"], "why": c.get("why"),
                    "since-epoch": since, "in-flight-repairs": []}

        with tempfile.TemporaryDirectory() as td, \
             mock.patch.object(loop, "STATE", Path(td)), \
             mock.patch.object(loop, "WRITE_ROOTS", [Path(td) / "f2-data",
                                                      Path(td) / "f2-evidence",
                                                      Path(td) / "f3-data"]), \
             mock.patch.object(loop, "LEDGER", Path(td) / "ledger.jsonl"), \
             mock.patch.object(loop, "LOGFILE", Path(td) / "loop.log"), \
             mock.patch.object(loop, "STOP_FILE", Path(td) / "STOP"), \
             mock.patch.object(loop, "ALERTS", Path(td) / "alerts.jsonl"), \
             mock.patch.object(loop, "ACKNOWLEDGED", Path(td) / "acknowledged.json"), \
             mock.patch.object(loop, "DEBUGGER", Path(td) / "debugger"), \
             mock.patch.object(loop, "jvm_eval",
                               side_effect=wm_click_hermetic.healthy_jvm_eval), \
             mock.patch.object(loop, "jvm_start_epoch", return_value=123), \
             mock.patch.object(loop, "reload_when_idle", return_value=(True, "", 123)) as reload, \
             mock.patch.object(loop, "fire", side_effect=fire), \
             mock.patch.object(loop, "notify") as notify, \
             mock.patch.object(loop, "notify_alert") as notify_alert, \
             mock.patch.object(loop, "repair") as repair, \
             mock.patch.object(loop, "log"):
            if acknowledged is not None:
                (Path(td) / "acknowledged.json").write_text(json.dumps(acknowledged))
            code = loop.run(args)
            alerts = [l for l in (Path(td) / "alerts.jsonl").read_text().splitlines()] \
                if (Path(td) / "alerts.jsonl").exists() else []
            rows = [__import__("json").loads(l) for l in (Path(td) / "ledger.jsonl").read_text().splitlines()]
            events = [row["event"] for row in rows]
        return {"code": code, "stop": notify.call_args[0][3], "alerts": alerts,
                "belled": notify_alert.call_count, "repairs": repair.call_count, "events": events,
                "rows": rows, "reloads": reload.call_count}

    def test_click_ledger_carries_selection_why(self):
        why = {"classes": ["class one", "class two"], "abstention": "stopping",
               "decision": "decision"}
        result = self.run_continuous([{"alert": None, "why": why}])
        click = next(row for row in result["rows"] if row["event"] == "click")
        self.assertEqual(why, click["why"])

    def test_cost_receipt_written_once_at_click_close_with_typed_absence(self):
        loop = load_loop()
        run_record = '''
{:registered-run/timing
 {:phase-timings-ms {:selection 17 :author-wait 23}
  :debugger-stopped-ms 5
  :debugger-stopped-by-phase-ms {:selection 5}
  :debugger-dwell-status :complete}
 :registered-run/model-usage
 {:status :complete :source :wm-run-local-agency-ledger :unit :tokens
  :input-tokens 100 :output-tokens 20 :total-tokens 120
  :jobs [{:job-id "job-1" :role {:agent "wm-author"}
          :phase :author-wait :status :complete :unit :tokens
          :input-tokens 100 :output-tokens 20 :total-tokens 120}]}}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            record_path = root / "tick-run-record-run-1.edn"
            record_path.write_text(run_record)
            click_log = root / "click.log"
            click_log.write_text(
                'ordinary output\n'
                'WM_COST_PHASE {"phase":"agency-budget-preflight","wall-ms":11,'
                '"source":"launcher-monotonic"}\n'
                'WM_COST_PHASE {"phase":"source-giraffe","wall-ms":29,'
                '"source":"launcher-monotonic"}\n'
                'WM_COST_PHASE {"phase":"server-execution","wall-ms":80,'
                '"source":"launcher-monotonic"}\n')
            ledger = root / "ledger.jsonl"
            state = root / "state"
            state.mkdir()
            (state / "seat-calls.jsonl").write_text(
                json.dumps({"schema": "wm/click-seat-call-v1",
                            "click-id": "wm-click-1", "run-id": "run-1",
                            "role": "meta-diagnosis", "phase": "meta-self-heal",
                            "job-id": "job-meta", "seat": "zai-1",
                            "session-id": "session-1", "at": "2026-11-05T00:00:00Z"}) + "\n" +
                json.dumps({"schema": "wm/click-seat-call-v1",
                            "click-id": "wm-click-1", "role": "meta-acceptance-review",
                            "phase": "acceptance-review", "job-id": "job-no-usage",
                            "seat": "wm-reviewer", "session-id": "session-2"}) + "\n" +
                json.dumps({"schema": "wm/click-seat-call-v1",
                            "click-id": "wm-click-1", "role": "meta-repair-author",
                            "phase": "repair-author", "job-id": "job-running",
                            "seat": "wm-author", "session-id": "session-3"}) + "\n" +
                json.dumps({"schema": "wm/click-seat-call-v1",
                            "click-id": "some-other-click",
                            "role": "readiness-author", "phase": "ticket-readiness",
                            "job-id": {"status": "typed-missing",
                                       "reason": "job-id-not-retained"}}) + "\n")
            ordinary = {
                "n": 1, "click-id": "wm-click-1", "run-id": "run-1",
                "run-record": str(record_path), "log": str(click_log),
                "reset-wall-ms": 7,
            }
            not_launched = {
                "n": 2, "click-id": None, "run-id": None, "run-record": None,
            }
            jobs = {
                "job-meta": {"job": {"state": "done", "usage": {
                    "input_tokens": 30, "output_tokens": 10, "total_tokens": 40,
                    "cached_input_tokens": 5, "source": "codex"}}},
                "job-no-usage": {"job": {"state": "done"}},
                "job-running": {"job": {"state": "running"}},
            }
            fetch = lambda path, timeout=5: jobs[path.rsplit("/", 1)[-1]]
            with mock.patch.object(loop, "LEDGER", ledger), \
                 mock.patch.object(loop, "STATE", state), \
                 mock.patch.object(loop, "F2", ROOT), \
                 mock.patch.object(loop, "get_json", side_effect=fetch):
                loop.record_cost_receipt(ordinary, 321)
                loop.record({"event": "click", "click-id": "wm-click-1"})
                loop.record_cost_receipt(ordinary, 999)
                loop.record_cost_receipt(not_launched, 45)
                loop.record({"event": "click", "click-id": None})

            rows = [json.loads(line) for line in ledger.read_text().splitlines()]
            receipts = [row for row in rows if row["event"] == "cost-receipt"]
            self.assertEqual(2, len(receipts))
            self.assertEqual(["cost-receipt", "click", "cost-receipt", "click"],
                             [row["event"] for row in rows])
            receipt = receipts[0]
            self.assertEqual(321, receipt["wall"]["total-ms"])
            phases = {phase["phase"]: phase for phase in receipt["wall"]["phases"]}
            self.assertEqual(17, phases["selection"]["wall-ms"])
            self.assertEqual(23, phases["author-wait"]["wall-ms"])
            self.assertEqual("server-execution", phases["selection"]["parent"])
            self.assertEqual("server-execution", phases["author-wait"]["parent"])
            self.assertEqual(7, phases["reset"]["wall-ms"])
            self.assertEqual(11, phases["agency-budget-preflight"]["wall-ms"])
            self.assertEqual(29, phases["source-giraffe"]["wall-ms"])
            self.assertEqual(80, phases["server-execution"]["wall-ms"])
            self.assertEqual("launcher-monotonic", phases["server-execution"]["source"])
            self.assertEqual("typed-missing", phases["child-wait"]["status"])
            self.assertEqual("not-reached", phases["child-wait"]["reason"])
            self.assertNotIn("wall-ms", phases["child-wait"])
            self.assertEqual(5, receipt["wall"]["debugger"]["debugger-stopped-ms"])
            self.assertEqual(160, receipt["model-usage"]["totals"]["total-tokens"])
            self.assertEqual(130, receipt["model-usage"]["totals"]["input-tokens"])
            self.assertEqual(30, receipt["model-usage"]["totals"]["output-tokens"])
            self.assertEqual("partial", receipt["model-usage"]["totals"]["status"])
            self.assertEqual(2, receipt["model-usage"]["totals"]["missing-call-count"])
            job = next(call for call in receipt["model-usage"]["calls"]
                       if call.get("job-id") == "job-1")
            self.assertEqual(120, job["total-tokens"])
            direct = [call for call in receipt["model-usage"]["calls"]
                      if call.get("role") == "meta-diagnosis"]
            self.assertEqual(1, len(direct))
            self.assertEqual("job-meta", direct[0]["job-id"])
            self.assertEqual("complete", direct[0]["status"])
            self.assertEqual(40, direct[0]["total-tokens"])
            self.assertEqual(5, direct[0]["cached-input-tokens"])
            missing_reasons = {call.get("reason") for call in
                               receipt["model-usage"]["calls"]}
            self.assertIn("provider-usage-absent", missing_reasons)
            self.assertIn("job-not-terminal", missing_reasons)
            self.assertFalse(any(call.get("role") == "readiness-author"
                                 for call in receipt["model-usage"]["calls"]))
            self.assertEqual("not-applicable",
                             next(call for call in receipt["model-usage"]["calls"]
                                  if call["role"] == "selection")["status"])
            absent = receipts[1]
            self.assertEqual("typed-missing", absent["run-id"]["status"])
            self.assertEqual(45, absent["wall"]["total-ms"])
            self.assertEqual("typed-missing", absent["wall"]["phases"][0]["status"])
            self.assertNotIn("wall-ms", absent["wall"]["phases"][0])
            self.assertFalse(any(call.get("role", "").startswith("meta-") or
                                 call.get("role") == "readiness-author"
                                 for call in absent["model-usage"]["calls"]))

    def test_repeated_signature_pauses_the_loop_once_the_fix_is_due(self):
        r = self.run_continuous([{"alert": ["work Q7 violated"]},
                                 {"alert": ["work Q7 violated"]},
                                 {"alert": None}])
        self.assertEqual(1, r["code"])
        self.assertIn("same requirements failed on two consecutive clicks", r["stop"])
        self.assertIn("work Q7 violated", r["stop"])
        self.assertEqual(2, len(r["alerts"]), "both failing clicks are queued as work")
        self.assertEqual(1, r["belled"], "the owner is belled once; the repeat is the pause")
        self.assertEqual(0, r["repairs"])

    def test_different_alerts_and_a_conforming_click_keep_clicking(self):
        r = self.run_continuous([{"alert": ["work Q7 violated"]},
                                 {"alert": ["selection Q9 violated"]},
                                 {"alert": None, "outcome": "abstained", "carried": False},
                                 {"alert": ["work Q7 violated"]}])
        self.assertEqual(0, r["code"])
        self.assertTrue(r["stop"].startswith("done: 4 clicks spent"))
        self.assertEqual(3, len(r["alerts"]))
        self.assertEqual(3, r["belled"])
        self.assertEqual(0, r["repairs"], "a lawful abstention and alerts dispatch no repair seat")
        self.assertEqual(4, r["events"].count("verdict"))
        self.assertEqual(1 + 3, r["reloads"], "one reload at start, then one before each later click")

    def test_acknowledged_rows_are_recorded_but_never_pause(self):
        ack = [{"row": "work Q9 violated", "why": "factorized-preference producer absent from serving",
                "ref": "packets/L5-7-D-q9-factorized-preference-into-serving.md"}]
        r = self.run_continuous([{"alert": ["work Q9 violated"]},
                                 {"alert": ["work Q9 violated"]},
                                 {"alert": ["work Q9 violated"]}], acknowledged=ack)
        self.assertEqual(0, r["code"])
        self.assertTrue(r["stop"].startswith("done: 3 clicks spent"))
        self.assertEqual(3, len(r["alerts"]), "every failing verdict is still queued")
        self.assertEqual(0, r["belled"], "an acknowledged standing row does not bell the owner")
        unacked = self.run_continuous([{"alert": ["work Q7 violated", "work Q9 violated"]},
                                       {"alert": ["work Q7 violated", "work Q9 violated"]}], acknowledged=ack)
        self.assertEqual(1, unacked["code"])
        self.assertIn("work Q7 violated", unacked["stop"])
        self.assertNotIn("Q9", unacked["stop"], "the pause names only the open rows")
        malformed = self.run_continuous([{"alert": ["work Q9 violated"]},
                                         {"alert": ["work Q9 violated"]}], acknowledged={"row": "x"})
        self.assertEqual(1, malformed["code"], "a malformed acknowledgement file acknowledges nothing")


    def test_injury_limit_allows_one_and_refuses_two_or_unreadable(self):
        # Joe, 2026-11-05: "maximum one injury allowed at a time".
        loop = load_loop()
        cases = [
            ({"rc": 0, "out": '{:ok true, :value "ACTIVE-INJURIES 0 []"}', "err": ""}, None),
            ({"rc": 0, "out": '{:ok true, :value "ACTIVE-INJURIES 1 [\\"injury/a\\"]"}', "err": ""}, None),
            ({"rc": 0, "out": '{:ok true, :value "ACTIVE-INJURIES 2 [\\"injury/a\\" \\"injury/b\\"]"}', "err": ""},
             "2 injuries open at once"),
            ({"rc": 0, "out": '{:ok true, :value "ACTIVE-INJURIES-UNREADABLE :ledger/malformed"}', "err": ""},
             "unreadable"),
            ({"rc": 1, "out": "", "err": "connection refused"}, "unreadable"),
        ]
        for reply, expected in cases:
            loop.jvm_eval = lambda form, timeout=600, reply=reply: reply
            breach = loop.injury_limit_breach()
            if expected is None:
                self.assertIsNone(breach, reply["out"])
            else:
                self.assertIn(expected, breach or "", reply)

class LoopOwnWriteStampTest(unittest.TestCase):
    def test_loop_own_writes_stamp_themselves_foreign_writes_do_not(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            state = base / "state"
            state.mkdir()
            root = base / "repo"
            root.mkdir()
            stamps = base / "stamps"
            stamps.mkdir()
            foreign = root / "foreign.txt"
            loop.STATE = state
            loop.LOGFILE = state / "loop.log"
            loop.LEDGER = state / "ledger.jsonl"
            loop.ALERTS = state / "alerts.jsonl"

            def fake_fire(n, args, since):
                # every write surface the loop itself uses during a click
                loop.log(f"click {n}: firing")
                loop.record({"event": "cost-receipt", "click-id": "wm-click-self",
                             "click-number": n})
                loop.queue_alert({"click-id": "wm-click-self", "run-id": "run-x",
                                  "alert": ("work Q9 violated",)}, False)
                # someone else's write inside the same window
                foreign.write_text("not the loop")
                return {"n": n, "click-id": "wm-click-self", "run-id": "run-x",
                        "outcome": "ok", "since-epoch": since}

            with mock.patch.object(loop, "WRITE_ROOTS", [root, state]), \
                 mock.patch.object(loop, "WRITE_STAMP_ROOT", stamps), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire):
                click = loop.fire_with_write_manifest(1, object(), 123)

            manifest = json.loads(Path(click["write_manifest"]["path"]).read_text())
            entries = {Path(entry["path"]).name: entry for entry in manifest["entries"]}
            for name in ("loop.log", "ledger.jsonl", "alerts.jsonl"):
                self.assertEqual("stamped", entries[name]["attribution"], name)
                self.assertEqual("wm-click-loop", entries[name]["writer"], name)
            self.assertEqual("click-window/unattributed",
                             entries["foreign.txt"]["attribution"])
            self.assertEqual(3, manifest["stamped_count"])
            self.assertEqual(1, manifest["unattributed_count"])
            # the stamp rows are the format read_write_stamps accepts
            rows = [json.loads(line) for line
                    in (stamps / "wm-click-self.jsonl").read_text().splitlines()]
            self.assertEqual({"loop.log", "ledger.jsonl", "alerts.jsonl"},
                             {Path(row["path"]).name for row in rows})
            self.assertTrue(all(row["writer"] == "wm-click-loop"
                                and row["click-id"] == "wm-click-self" for row in rows))

    def test_stamp_failure_is_counted_not_raised(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            state = base / "state"
            state.mkdir()
            # a regular file where the stamp directory belongs: mkdir fails
            blocker = base / "stamps"
            blocker.write_text("in the way")
            loop.STATE = state
            loop.LOGFILE = state / "loop.log"
            loop.LEDGER = state / "ledger.jsonl"
            loop.ALERTS = state / "alerts.jsonl"
            loop.OWN_WRITE_PATHS.clear()

            def fake_fire(n, args, since):
                loop.log("still writes")
                return {"n": n, "click-id": "wm-click-blocked",
                        "outcome": "ok", "since-epoch": since}

            with mock.patch.object(loop, "WRITE_ROOTS", [state]), \
                 mock.patch.object(loop, "WRITE_STAMP_ROOT", blocker), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire):
                click = loop.fire_with_write_manifest(1, object(), 123)
            # the click completed and produced a manifest despite stamp failure
            self.assertIn("path", click["write_manifest"])
            self.assertGreaterEqual(loop.OWN_WRITE_STAMP_FAILURES, 1)
            manifest = json.loads(Path(click["write_manifest"]["path"]).read_text())
            entries = {Path(entry["path"]).name: entry for entry in manifest["entries"]}
            self.assertEqual("click-window/unattributed",
                             entries["loop.log"]["attribution"])



class JobPreviewAttributionTest(unittest.TestCase):
    def test_previews_attribute_paths_and_respect_boundaries(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            root = base / "repo"
            root.mkdir()
            stamps = base / "stamps"
            stamps.mkdir()
            edited = root / "edited.txt"
            shelled = root / "shelled.txt"
            untouched = root / "untouched.txt"
            truncated = root / "truncated-path-name.txt"
            read_only = root / "read-only.txt"

            def fake_fire(n, args, since):
                edited.write_text("a")
                shelled.write_text("b")
                untouched.write_text("c")
                truncated.write_text("d")
                read_only.write_text("e")
                return {"n": n, "click-id": "wm-click-preview", "outcome": "ok",
                        "since-epoch": since}

            def ts(delta_seconds):
                stamp = (datetime.datetime.now(datetime.timezone.utc)
                         + datetime.timedelta(seconds=delta_seconds))
                return stamp.strftime("%Y-%m-%dT%H:%M:%S.100000000Z")

            jobs = [
                {"job-id": "invoke-fix-editor", "agent-id": "claude-9",
                 "started-at": ts(-3600), "finished-at": ts(60),
                 "events": [{"type": "tool_use", "tools": ["edit_file"],
                             "previews": [f"edit_file {edited.resolve()}"]}]},
                {"job-id": "invoke-fix-shell", "agent-id": "zai-3",
                 "started-at": ts(-300), "finished-at": None,
                 "events": [{"type": "tool_use", "tools": ["run_shell"],
                             "previews": [f"run_shell cat {shelled.resolve()} > /dev/null"]}]},
                {"job-id": "invoke-fix-truncated", "agent-id": "codex-2",
                 "started-at": ts(-2400), "finished-at": ts(60),
                 # the preview cut the last character off the path
                 "events": [{"type": "tool_use", "tools": ["edit_file"],
                             "previews": [f"edit_file {str(truncated.resolve())[:-1]}"]}]},
                {"job-id": "invoke-fix-reader", "agent-id": "zai-11",
                 "started-at": ts(-600), "finished-at": ts(60),
                 # reading a file the window wrote is not evidence of writing it
                 "events": [{"type": "tool_use", "tools": ["read_file"],
                             "previews": [f"read_file {read_only.resolve()}"]}]},
                {"job-id": "invoke-fix-late", "agent-id": "claude-9",
                 "started-at": ts(86400), "finished-at": ts(86460),
                 "events": [{"type": "tool_use", "tools": ["edit_file"],
                             "previews": [f"edit_file {untouched.resolve()}"]}]},
            ]
            loop.fetch_agency_jobs_in_window = lambda started, ended: (
                [j for j in jobs if loop.job_overlaps_window(j, started, ended)], None)

            with mock.patch.object(loop, "STATE", base / "state"), \
                 mock.patch.object(loop, "WRITE_ROOTS", [root]), \
                 mock.patch.object(loop, "fire", side_effect=fake_fire):
                click = loop.fire_with_write_manifest(1, object(), 123)

            manifest = json.loads(Path(click["write_manifest"]["path"]).read_text())
            self.assertEqual({"status": "checked", "jobs-in-window": 4},
                             manifest["job_preview_check"])
            self.assertEqual(2, manifest["attributed_count"])
            entries = {Path(e["path"]).name: e for e in manifest["entries"]}
            self.assertEqual("attributed", entries["edited.txt"]["attribution"])
            self.assertEqual("claude-9", entries["edited.txt"]["agent-id"])
            self.assertEqual("invoke-fix-editor", entries["edited.txt"]["job-id"])
            self.assertEqual("edit_file", entries["edited.txt"]["tool"])
            self.assertNotIn("heuristic", entries["edited.txt"])
            # a shell preview still names the writer, marked heuristic
            self.assertEqual("attributed", entries["shelled.txt"]["attribution"])
            self.assertIs(True, entries["shelled.txt"]["heuristic"])
            self.assertEqual("zai-3", entries["shelled.txt"]["agent-id"])
            # no preview named it inside the window
            self.assertEqual("click-window/unattributed",
                             entries["untouched.txt"]["attribution"])
            self.assertEqual("click-window/unattributed",
                             entries["read-only.txt"]["attribution"])
            # a truncated preview must not match the longer real path
            self.assertEqual("click-window/unattributed",
                             entries["truncated-path-name.txt"]["attribution"])

    def test_fetch_failure_records_not_checked_and_keeps_entries(self):
        loop = load_loop()

        def broken_fetch(started, ended):
            raise OSError("agency unreachable")

        entries = [{"path": "/tmp/x/one.txt", "change": "new",
                    "attribution": "click-window/unattributed"},
                   {"path": "/tmp/x/two.txt", "change": "new",
                    "attribution": "stamped", "writer": "w"}]
        loop.fetch_agency_jobs_in_window = broken_fetch
        result, check = loop.apply_job_preview_attribution(entries, "2026-11-07T06:00:00Z",
                                                           "2026-11-07T07:00:00Z")
        self.assertEqual("not-checked", check["status"])
        self.assertIn("agency unreachable", check["reason"])
        self.assertEqual(entries, result)

    def test_fetch_none_reason_is_reported(self):
        loop = load_loop()
        loop.fetch_agency_jobs_in_window = lambda s, e: (None, "URLError: refused")
        result, check = loop.apply_job_preview_attribution(
            [{"path": "/tmp/x/one.txt", "attribution": "click-window/unattributed"}],
            "2026-11-07T06:00:00Z", "2026-11-07T07:00:00Z")
        self.assertEqual("not-checked", check["status"])
        self.assertEqual("URLError: refused", check["reason"])
        self.assertEqual("click-window/unattributed", result[0]["attribution"])


class ProviderUnavailableStopTest(unittest.TestCase):
    """PROVIDER-FAIL-4c, Joe's ruling 2026-11-07: a provider failure retried
    once (PROVIDER-FAIL-4a) ends the click typed -- never as an injury or a
    self-heal route. The run record is parsed by the real bb (the script's
    existing EDN reader); the loop itself never touches the live debugger or
    Agency (load_loop's stubs)."""

    def _write_record(self, td, text):
        path = Path(td) / "tick-run-record-run-pf4c.edn"
        path.write_text(text)
        return path

    def test_provider_unavailable_reads_the_429_run_record(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            path = self._write_record(td, (
                '{:run/id "run-pf4c" '
                ':failure {:kind :open-cascade-provider-unavailable '
                ':stage :cascade-resume :seat :wm-author '
                ':provider-failure {:class :usage-limit :http-status 429 '
                ':provider-code "1308" :evidence "HTTP 429"} '
                ':provider-retry {:first-job "job-1"} :retry-job "job-2"}}'))
            row = loop.provider_unavailable({"last-result": {"run-record": str(path)},
                                             "run-record": str(path)})
            self.assertEqual({"kind": "open-cascade-provider-unavailable",
                              "seat": "wm-author",
                              "class": "usage-limit",
                              "http-status": 429,
                              "provider-code": "1308",
                              "evidence": "HTTP 429",
                              "retry-job": "job-2",
                              "run-id": "run-pf4c"}, row)

    def test_contact_failed_without_a_provider_stop_is_not_a_provider_stop(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            path = self._write_record(td, (
                '{:run/id "run-cf" :failure {:kind :open-cascade-author-contact-failed '
                ':stage :cascade-resume}}'))
            self.assertIsNone(loop.provider_unavailable(
                {"run-record": str(path)}))

    def test_reads_the_shape_the_runner_writes(self):
        # The :failure map as run-record-failure writes a held provider stop
        # (printed by provider-fail-class-test, PROVIDER-FAIL-4c review): the
        # seat and provider detail sit under :detail, the seat a string.
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            path = self._write_record(td, (
                '{:run/id "run-real" :failure {:kind :open-cascade-provider-unavailable, '
                ':stage :cascade-resume, :error "held", '
                ':cause {:kind :open-cascade-provider-unavailable}, '
                ':detail {:status :error, :kind :open-cascade-provider-unavailable, '
                ':seat "wm-author", :provider-failure {:class :usage-limit, '
                ':http-status 429, :provider-code "1308", '
                ':evidence "HTTP 429 Usage limit reached for 5 hour"}, '
                ':provider-retry {:retried true, :first-class :usage-limit}, '
                ':retry-job "invoke-retry-2"}}}'))
            row = loop.provider_unavailable({"run-record": str(path)})
        self.assertEqual("usage-limit", row["class"])
        self.assertEqual("wm-author", row["seat"])
        self.assertEqual(429, row["http-status"])
        self.assertEqual("invoke-retry-2", row["retry-job"])

    def test_unreadable_record_returns_none_without_raising(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td, \
                mock.patch.object(loop, "log") as logged:
            path = self._write_record(td, "{:failure {:kind :not-edn")
            self.assertIsNone(loop.provider_unavailable({"run-record": str(path)}))
            self.assertIsNone(loop.provider_unavailable({"run-record": str(path)}))
            self.assertEqual(1, logged.call_count, "the read failure is logged once")

    def test_run_stops_typed_on_a_provider_unavailable_click(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            path = self._write_record(td, (
                '{:run/id "run-pf4c" '
                ':failure {:kind :open-cascade-provider-unavailable '
                ':stage :cascade-resume :seat :wm-author '
                ':provider-failure {:class :usage-limit :http-status 429 '
                ':provider-code "1308" :evidence "HTTP 429"} '
                ':retry-job "job-2"}}'))
            args = loop.parse_args([
                "run", "--clicks", "1", "--continuous", "--owner", "test-owner",
                "--issuing-caller", "claude-2", "--author", "wm-author",
                "--reviewer", "wm-reviewer", "--repair-reviewer", "wm-repair-reviewer"])

            # run() records a giraffe pin before its first click (WM hardening
            # package 1): give it a minimal missions-field run under the temp
            # tree, never the real /home/joe/code/storage/giraffe.
            giraffe = Path(td) / "giraffe"
            fixture_run = giraffe / "20260101T000000.000Z-fixture"
            fixture_run.mkdir(parents=True)
            (fixture_run / "manifest.edn").write_text(
                "{:invocation {:edges \"/fixture/holes/missions/edges.json\"\n"
                "  :only-ids [\"T-fixture\"]}\n"
                " :sources {:edges {:path \"/fixture/holes/missions/edges.json\"}}}\n")

            def fire(n, a, since):
                return {"n": n, "rc": 1, "click-id": f"click-{n}", "spent": True,
                        "outcome": "unknown", "run-id": "run-pf4c", "carried": False,
                        "last-result": {"run-record": str(path)}, "log": None,
                        "run-record": str(path), "verdict": {"file": "V"},
                        "alert": None, "since-epoch": since, "in-flight-repairs": []}
            with mock.patch.object(loop, "STATE", Path(td)), \
                 mock.patch.object(loop, "WRITE_ROOTS", [Path(td) / "f2-data",
                                                          Path(td) / "f2-evidence",
                                                          Path(td) / "f3-data"]), \
                 mock.patch.object(loop, "LEDGER", Path(td) / "ledger.jsonl"), \
                 mock.patch.object(loop, "LOGFILE", Path(td) / "loop.log"), \
                 mock.patch.object(loop, "STOP_FILE", Path(td) / "STOP"), \
                 mock.patch.object(loop, "ALERTS", Path(td) / "alerts.jsonl"), \
                 mock.patch.object(loop, "ACKNOWLEDGED", Path(td) / "ack.json"), \
                 mock.patch.object(loop, "DEBUGGER", Path(td) / "debugger"), \
                 mock.patch.object(loop, "jvm_eval",
                                   side_effect=wm_click_hermetic.healthy_jvm_eval), \
                 mock.patch.object(loop, "GIRAFFE_PIN", Path(td) / "giraffe-pin.edn"), \
                 mock.patch.dict(os.environ, {"GIRAFFE_ROOT": str(giraffe)}), \
                 mock.patch.object(loop, "jvm_start_epoch", return_value=123), \
                 mock.patch.object(loop, "reload_when_idle", return_value=(True, "", 123)), \
                 mock.patch.object(loop, "fire", side_effect=fire), \
                 mock.patch.object(loop, "notify"), \
                 mock.patch.object(loop, "notify_alert"), \
                 mock.patch.object(loop, "repair") as repair, \
                 mock.patch.object(loop, "log"):
                code = loop.run(args)
            self.assertEqual(1, code)
            self.assertEqual(0, repair.call_count)
            rows = [json.loads(line) for line in
                    (Path(td) / "ledger.jsonl").read_text().splitlines() if line.strip()]
            evidence = [r for r in rows if r.get("event") == "stop-evidence"
                        and r.get("kind") == "provider-unavailable"]
            self.assertEqual(1, len(evidence))
            self.assertEqual("usage-limit", evidence[0]["class"])
            self.assertEqual("wm-author", evidence[0]["seat"])
            stop_row = [r for r in rows if r.get("event") == "stop"][0]
            self.assertTrue(stop_row["reason"].startswith(
                "provider-unavailable: usage-limit on wm-author"),
                            stop_row["reason"])
            self.assertIn("waiting for Joe", stop_row["reason"])


class RepairProtectedPathTest(unittest.TestCase):
    """WM hardening package 3 (handoff 2026-11-13 section 3): a repair commit
    that touches a protected path is not hot-loaded; the loop stops instead."""

    # The scope-widening repair commit named in the handoff; in this repo's
    # history (reverted by 08fb7d3c0).
    BAD_SHA = "8a0cb58c2a5709318612fe6bc96b26bcc782906f"

    def test_protected_patterns_cover_the_handoff_list(self):
        loop = load_loop()
        self.assertEqual(
            ["DarkTower/WarMachine/Requirements.lean",
             "DarkTower/WarMachine/RequirementsQ12.lean"],
            loop.protected_paths_in(["DarkTower/WarMachine/Requirements.lean",
                                     "DarkTower/WarMachine/RequirementsQ12.lean",
                                     "DarkTower/WarMachine/Other.lean"]))
        self.assertEqual(
            ["scripts/wm_check_run_facts.clj", "scripts/wm_click_repair_loop.py",
             "scripts/wm_run_facts.clj", "src/futon2/aif/full_loop_runtime.clj",
             "src/futon3c/giraffe/run.clj"],
            loop.protected_paths_in(["scripts/wm_run_facts.clj",
                                     "scripts/wm_check_run_facts.clj",
                                     "scripts/wm_click_repair_loop.py",
                                     "src/futon2/aif/full_loop_runtime.clj",
                                     "src/futon3c/giraffe/run.clj"]))
        self.assertEqual([], loop.protected_paths_in(
            ["src/futon2/aif/wm/family_selection.clj",
             "test/futon2/aif/full_loop_giraffe_scoped_run_test.clj",
             "scripts/futon2/other.clj"]))

    def test_bad_case_repair_8a0cb58c2_is_not_loaded_and_stops_the_loop(self):
        loop = load_loop()
        loop.F2 = ROOT
        # Treat 8a0cb58c2 as the repair commit: its committed paths enter the
        # reload plan exactly as changed_click_files would serve them.
        rels = loop.commit_paths(ROOT, self.BAD_SHA)
        self.assertIn("src/futon2/aif/full_loop_runtime.clj", rels)
        files = [ROOT / rel for rel in rels if rel.endswith((".clj", ".cljc"))]

        def no_jvm(form, timeout=600):
            raise AssertionError("nothing may be hot-loaded when a protected path changed")

        # Realistic epoch: git does not parse bare small numbers as dates.
        since = int(loop.git(ROOT, "show", "-s", "--format=%ct", self.BAD_SHA).strip()) - 60
        with mock.patch.object(loop, "click_running", return_value=False), \
             mock.patch.object(loop, "changed_click_files", return_value=files), \
             mock.patch.object(loop, "PROTECTED_SCAN_REPOS", (ROOT,)), \
             mock.patch.object(loop, "jvm_eval", side_effect=no_jvm), \
             mock.patch.object(loop, "record") as record, \
             mock.patch.object(loop, "log"):
            with self.assertRaises(loop.ProtectedPathError) as raised:
                loop.reload_changed(since)
        self.assertIn("src/futon2/aif/full_loop_runtime.clj", raised.exception.paths)
        self.assertEqual("reload-refused-protected", record.call_args.args[0]["event"])
        self.assertIn("src/futon2/aif/full_loop_runtime.clj",
                      record.call_args.args[0]["protected-paths"])
        # The same commit judged as a tagged repair is refused, not "repaired".
        self.assertEqual(["src/futon2/aif/full_loop_runtime.clj"],
                         loop.repair_commit_protection([(ROOT, self.BAD_SHA)]))

    def test_a_repair_touching_only_unprotected_files_still_loads(self):
        loop = load_loop()
        family = loop.F2 / "src/futon2/aif/wm/family_selection.clj"
        with mock.patch.object(loop, "click_running", return_value=False), \
             mock.patch.object(loop, "changed_click_files", return_value=[family]), \
             mock.patch.object(loop, "dirty", return_value=False), \
             mock.patch.object(loop, "jvm_eval", side_effect=lambda form, timeout=600: {
                 "rc": 0, "out": ":standing-reset" if "reset-context!" in form else ":reloaded",
                 "err": ""}) as reload, \
             mock.patch.object(loop, "record"), \
             mock.patch.object(loop, "log"):
            ok, text, _ = loop.reload_changed(123)
        self.assertTrue(ok)
        self.assertEqual("", text)
        self.assertTrue(any(c.args[0].startswith("(try (require")
                            for c in reload.call_args_list))
        self.assertIn("reset-context!", reload.call_args.args[0], "the standing context reset still runs")

    def test_protected_path_stop_is_legal_only_with_evidence(self):
        loop = load_loop()
        args = mock.Mock(clicks=3, ticks=3)
        self.assertEqual(("protected-path", True), loop._validated_stop(
            args, "protected-path", {"protected-paths": ["src/futon2/aif/full_loop_runtime.clj"]}))
        self.assertEqual(("debugger-error", False), loop._validated_stop(
            args, "protected-path", {"protected-paths": []}))
        self.assertEqual(("debugger-error", False), loop._validated_stop(
            args, "protected-path", {}))

    @staticmethod
    def _repo_with_commit(td: str, rel: str, message: str) -> Path:
        """A throwaway git repo with one commit touching REL."""
        repo = Path(td) / ("repo-" + rel.replace("/", "_"))
        repo.mkdir(parents=True)
        subprocess.run(["git", "-C", repo, "init", "-q"], check=True)
        subprocess.run(["git", "-C", repo, "config", "user.email", "t@t"], check=True)
        subprocess.run(["git", "-C", repo, "config", "user.name", "t"], check=True)
        target = repo / rel
        target.parent.mkdir(parents=True)
        target.write_text("-- fixture\n")
        subprocess.run(["git", "-C", repo, "add", rel], check=True)
        subprocess.run(["git", "-C", repo, "commit", "-q", "-m", message], check=True)
        return repo

    def test_mathlib4_requirements_change_in_the_window_is_refused(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            repo = self._repo_with_commit(
                td, "DarkTower/WarMachine/Requirements.lean", "widen requirements")

            def no_jvm(form, timeout=600):
                raise AssertionError("nothing may be hot-loaded when a protected path changed")

            since = int(loop.time.time()) - 3600  # bare small numbers are not dates
            # Scan only the throwaway repo: the real futon2/futon3c may hold
            # protected commits inside the window (a merge did, 2026-11-13).
            with mock.patch.object(loop, "click_running", return_value=False), \
                 mock.patch.object(loop, "changed_click_files", return_value=[]), \
                 mock.patch.object(loop, "PROTECTED_SCAN_REPOS", (repo,), create=True), \
                 mock.patch.object(loop, "jvm_eval", side_effect=no_jvm), \
                 mock.patch.object(loop, "record") as record, \
                 mock.patch.object(loop, "log"):
                with self.assertRaises(loop.ProtectedPathError) as raised:
                    loop.reload_changed(since)
            self.assertEqual(["DarkTower/WarMachine/Requirements.lean"],
                             raised.exception.paths)
            self.assertEqual("reload-refused-protected", record.call_args.args[0]["event"])
            # The same repo's tagged repair commits are found and judged too.
            sha = loop.git(repo, "rev-parse", "HEAD").strip()
            loop.MATHLIB4 = repo
            self.assertIn((repo, sha), loop.tagged_commits("widen requirements"))
            self.assertEqual(["DarkTower/WarMachine/Requirements.lean"],
                             loop.repair_commit_protection([(repo, sha)]))

    def test_change_to_giraffe_run_verification_is_refused(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            repo = self._repo_with_commit(
                td, "src/futon3c/giraffe/run.clj", "weaken verify-run")
            with mock.patch.object(loop, "click_running", return_value=False), \
                 mock.patch.object(loop, "changed_click_files", return_value=[]), \
                 mock.patch.object(loop, "PROTECTED_SCAN_REPOS", (repo,), create=True), \
                 mock.patch.object(loop, "jvm_eval",
                                   side_effect=AssertionError(
                                       "nothing may be hot-loaded")), \
                 mock.patch.object(loop, "record"), \
                 mock.patch.object(loop, "log"):
                with self.assertRaises(loop.ProtectedPathError) as raised:
                    loop.reload_changed(int(loop.time.time()) - 3600)
            self.assertEqual(["src/futon3c/giraffe/run.clj"], raised.exception.paths)

    def test_repair_judgment_refuses_a_tagged_protected_commit(self):
        loop = load_loop()
        args = mock.Mock(repair_seat="wm-repair", seat_model=None, repair_timeout=1)
        with tempfile.TemporaryDirectory() as td:
            # Package 4's dirty-checkout guard reads the live F2/F3C/MATHLIB4;
            # this test is about protected paths, so point them at stand-ins
            # that do not exist (no .git -> nothing dirty, nothing folded).
            clean = [Path(td) / n for n in ("no-f2", "no-f3c", "no-mathlib4")]
            with mock.patch.object(loop, "F2", clean[0]), \
                 mock.patch.object(loop, "F3C", clean[1]), \
                 mock.patch.object(loop, "MATHLIB4", clean[2]), \
                 mock.patch.object(loop, "ensure_seat"), \
                 mock.patch.object(loop, "dispatch", return_value="job-1"), \
                 mock.patch.object(loop, "wait_job",
                                   return_value={"state": "done",
                                                 "result": "VERDICT: repaired"}), \
                 mock.patch.object(loop, "tagged_commits",
                                   return_value=[(ROOT, self.BAD_SHA)]), \
                 mock.patch.object(loop, "record"), \
                 mock.patch.object(loop, "log"):
                verdict, detail = loop.repair(args, "held run stopped", None, [])
        self.assertEqual("protected-path", verdict)
        self.assertIn("src/futon2/aif/full_loop_runtime.clj", detail)
        self.assertIn("refused", detail)


if __name__ == "__main__":
    unittest.main()
