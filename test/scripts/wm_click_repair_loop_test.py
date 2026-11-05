import importlib.util
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


def load_loop():
    spec = importlib.util.spec_from_file_location("wm_click_repair_loop", LOOP)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class RepairLoopCastTest(unittest.TestCase):
    def test_click_write_manifest_registers_window_changes_and_ledger_pin(self):
        loop = load_loop()
        real_data = ROOT / "data"
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
            self.assertTrue(all(entry.get("attribution") == "click-window"
                                for entry in entries.values()))
            self.assertEqual(3, pin["count"])
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
                 mock.patch.object(loop, "fire", side_effect=fake_fire), \
                 mock.patch.object(loop, "hash_file", side_effect=OSError("hash refused")):
                click = loop.fire_with_write_manifest(1, object(), 123)
            self.assertEqual("incomplete", click["outcome"])
            self.assertEqual("failed", click["write_manifest"]["status"])
            self.assertIn("hash refused", click["write_manifest"]["error"])

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


if __name__ == "__main__":
    unittest.main()
