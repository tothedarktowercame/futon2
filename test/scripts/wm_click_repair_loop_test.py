import importlib.util
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
             mock.patch.object(loop, "jvm_eval", return_value={"rc": 0, "out": ":reloaded", "err": ""}) as reload, \
             mock.patch.object(loop, "record") as record, \
             mock.patch.object(loop, "log"):
                identities = loop.source_identity_inventory()
                self.assertIn("futon2.aif.observation-checks", identities)
                self.assertIn("futon2.aif.wm.family-selection", identities)
                self.assertNotIn("outside.inventory", identities)
                ok, text, _ = loop.reload_changed(123)
        self.assertTrue(ok)
        self.assertEqual("", text)
        self.assertIn("futon2.aif.wm.family-selection", reload.call_args.args[0])
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


if __name__ == "__main__":
    unittest.main()
