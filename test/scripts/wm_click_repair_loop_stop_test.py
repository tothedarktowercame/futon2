import importlib.util
import json
import os
import tempfile
import unittest
import warnings
from pathlib import Path
from types import SimpleNamespace
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]

import wm_click_hermetic  # noqa: E402 (needs the test dir on sys.path first)


def load_loop():
    path = ROOT / "scripts" / "wm_click_repair_loop.py"
    spec = importlib.util.spec_from_file_location("wm_click_repair_loop_under_test", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return wm_click_hermetic.harden(module)


class SelfHealStopTest(unittest.TestCase):
    def setUp(self):
        warnings.simplefilter("ignore", ResourceWarning)
        self.loop = load_loop()
        # run() reads the injury ledger and debugger state from the serving
        # JVM; the hardened module refuses that, so supply the explicit fake.
        self.loop.jvm_eval = wm_click_hermetic.healthy_jvm_eval
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        canonical_state = self.loop.STATE
        self.loop.STATE = root / "state"
        self.loop.STATE.mkdir()
        self.loop.LEDGER = self.loop.STATE / "ledger.jsonl"
        self.loop.LOGFILE = self.loop.STATE / "loop.log"
        self.loop.STOP_FILE = self.loop.STATE / "STOP"
        self.loop.DEBUGGER = self.loop.STATE / "debugger"
        # Every module path under the canonical state directory moves with
        # STATE, so a newly reachable writer (ALERTS, DEBUGGER, ...) cannot
        # reach production data (2026-11-05: DEBUGGER was missed and a fixture
        # page landed in data/wm-click-loop/debugger/).
        for name, value in list(vars(self.loop).items()):
            if isinstance(value, Path) and value.is_relative_to(canonical_state):
                setattr(self.loop, name,
                        self.loop.STATE / value.relative_to(canonical_state))
        assert not any(isinstance(v, Path) and v.is_relative_to(canonical_state)
                       for v in vars(self.loop).values())
        self.loop.META_INJURY_ROOT = root / "injuries"
        # A launch records a giraffe pin before its first click (WM hardening
        # package 1), so run() needs a pinnable missions-field run.  Point
        # GIRAFFE_ROOT at a minimal fixture under the temp tree: the tests must
        # never read /home/joe/code/storage/giraffe, and a missing pin would
        # stop every test typed as no-giraffe-pin before its clicks.
        giraffe_run = root / "giraffe" / "20260101T000000.000Z-fixture"
        giraffe_run.mkdir(parents=True)
        (giraffe_run / "manifest.edn").write_text(
            "{:invocation {:edges \"/fixture/holes/missions/edges.json\"\n"
            "  :only-ids [\"T-fixture\"]}\n"
            " :sources {:edges {:path \"/fixture/holes/missions/edges.json\"}}}\n")
        giraffe_env = mock.patch.dict(os.environ,
                                      {"GIRAFFE_ROOT": str(root / "giraffe")})
        giraffe_env.start()
        self.addCleanup(giraffe_env.stop)
        self.args = SimpleNamespace(giraffe_run=None,
            clicks=2, ticks=99, stall=3, max_repairs=3,
            repair_seat="repair", continuous=True, observe_only=False,
            owner="owner", issuing_caller="caller", author="author",
            reviewer="reviewer", repair_reviewer="repair-reviewer")

    def click(self, disposition, carried=False, click_id="click-1",
              outcome="unknown", run_record=None):
        return {
            "n": 1, "rc": 1, "click-id": click_id, "spent": True,
            "outcome": outcome, "run-id": None, "carried": carried,
            "last-result": {"runtime-disposition": disposition},
            "log": "unused", "run-record": run_record, "verdict": None,
            "alert": None, "why": None, "since-epoch": 0,
            "in-flight-repairs": [],
        }

    def run_with(self, clicks):
        with (mock.patch.object(self.loop, "jvm_start_epoch", return_value=0),
              mock.patch.object(self.loop, "reload_when_idle", return_value=(True, "", 0)),
              mock.patch.object(self.loop, "debugger_stops", return_value=[]),
              mock.patch.object(self.loop, "debugger_attached", return_value=True),
              mock.patch.object(self.loop, "fire", side_effect=clicks) as fire,
              mock.patch.object(self.loop, "notify")):
            rc = self.loop.run(self.args)
        rows = [json.loads(line) for line in self.loop.LEDGER.read_text().splitlines()]
        return rc, fire.call_count, rows

    def run_with_debugger_pages(self, clicks, signatures):
        pages = [(self.loop.STATE / f"debugger-{i}.md",
                  {"page": str(self.loop.STATE / f"debugger-{i}.md"),
                   "signature": signature})
                 for i, signature in enumerate(signatures)]
        with (mock.patch.object(self.loop, "jvm_start_epoch", return_value=0),
              mock.patch.object(self.loop, "reload_when_idle", return_value=(True, "", 0)),
              mock.patch.object(self.loop, "debugger_stops", return_value=[]),
              mock.patch.object(self.loop, "debugger_attached", return_value=True),
              mock.patch.object(self.loop, "fire", side_effect=clicks) as fire,
              mock.patch.object(self.loop, "debugger_page", side_effect=pages) as debugger_page,
              mock.patch.object(self.loop, "notify")):
            rc = self.loop.run(self.args)
        rows = [json.loads(line) for line in self.loop.LEDGER.read_text().splitlines()]
        return rc, fire.call_count, debugger_page.call_count, rows

    def stop_row(self, case, evidence, reason="fixture stop"):
        with mock.patch.object(self.loop, "notify"):
            self.loop.stop(self.args, case, reason, 1, evidence)
        return json.loads(self.loop.LEDGER.read_text().splitlines()[-1])

    def test_stop_file_operator_override_is_legal(self):
        self.loop.STOP_FILE.touch()
        row = self.stop_row("operator-override",
                            {"operator-signal": "stop-file-observed"})
        self.assertEqual("operator-override", row["case"])
        self.assertTrue(row["legal"])

    def test_cli_budget_operator_override_is_legal(self):
        row = self.stop_row(
            "operator-override",
            {"operator-signal": "cli-budget-spent", "spent": self.args.clicks,
             "click-budget": self.args.clicks, "ticks": 0,
             "tick-budget": self.args.ticks})
        self.assertEqual("operator-override", row["case"])
        self.assertTrue(row["legal"])

    def test_work_exhausted_requires_present_empty_admitted_field(self):
        record = self.loop.STATE / "run.edn"
        for body in (
                "{:absent :no-task-selected :outer/selection {}}",
                # nil is "carrier absent", not empty: a text scan read the
                # next [] after the key as the admitted field
                "{:absent :no-task-selected :outer/selection "
                "{:outer-field/admitted nil :outer-field/excluded []}}",
                # only the top-level retained field counts, not a nested
                # previous selection's
                "{:absent :no-task-selected :outer/selection "
                "{:reading-reselection {:previous-selection "
                "{:outer-field/admitted []}}}}",
                # an empty field without the no-task-selected outcome
                "{:outer/selection {:outer-field/admitted []} "
                ":note \":absent :no-task-selected\"}",
                '{:absent :no-task-selected :outer/selection '
                '{:outer-field/admitted ["still-live"]}}'):
            record.write_text(body)
            row = self.stop_row("work-exhausted", {"run-record": str(record)})
            self.assertEqual("debugger-error", row["case"])
            self.assertEqual("work-exhausted", row["claimed-case"])
            self.assertFalse(row["legal"])

    def test_work_exhausted_with_retained_empty_field_is_legal(self):
        record = self.loop.STATE / "run.edn"
        record.write_text(
            "{:absent :no-task-selected "
            ":outer/selection {:outer-field/admitted []}}")
        row = self.stop_row("work-exhausted", {"run-record": str(record)})
        self.assertEqual("work-exhausted", row["case"])
        self.assertTrue(row["legal"])

    def test_work_exhausted_reads_the_selection_record_shape(self):
        # selection_record/write-decision! shape: what the supervisor sees
        record = self.loop.STATE / "selection-record.edn"
        record.write_text(
            "{:schema :x :selection-outcome {:status :abstained :case :empty-field} "
            ":outer/selection {:outer-field/admitted []}}")
        row = self.stop_row("work-exhausted", {"run-record": str(record)})
        self.assertEqual("work-exhausted", row["case"])
        self.assertTrue(row["legal"])
        record.write_text(
            "{:schema :x :selection-outcome {:status :abstained :case :empty-field} "
            ":outer/selection {:outer-field/admitted [\"M-live\"]}}")
        row = self.stop_row("work-exhausted", {"run-record": str(record)})
        self.assertEqual("debugger-error", row["case"])
        record.write_text(
            "{:schema :x :selection-outcome {:status :chosen :pick \"M-a\"} "
            ":outer/selection {:outer-field/admitted []}}")
        row = self.stop_row("work-exhausted", {"run-record": str(record)})
        self.assertEqual("debugger-error", row["case"])

    def test_g_not_below_abstention_is_debugger_error(self):
        row = self.stop_row(
            "debugger-error", {"input-digest": "abc"},
            "no compared policy's G is below the abstention")
        self.assertEqual("debugger-error", row["case"])
        self.assertFalse(row["legal"])

    def test_debugger_error_notification_is_outside_repair(self):
        sent = {}

        def fake_sh(*_args, **kwargs):
            sent["body"] = kwargs["input"]
            return SimpleNamespace(stdout="sent")

        with mock.patch.object(self.loop, "sh", side_effect=fake_sh):
            self.loop.notify(self.args, "debugger-error", False, "broken")
        self.assertIn("STOPPED [debugger-error/outside-repair]", sent["body"])

    def outer_abstention_record(self, admitted, disposition):
        record = self.loop.STATE / f"selection-{len(list(self.loop.STATE.iterdir()))}.edn"
        record.write_text(
            "{:selection-outcome {:status :abstained} "
            ":outer/selection {:outer-field/admitted " + admitted + " "
            ":refusal/disposition {:disposition :" + disposition + "}}}")
        return str(record)

    def test_retained_empty_work_exhaustion_stops_legally(self):
        record = self.outer_abstention_record("[]", "loop-stop")
        rc, calls, rows = self.run_with([
            self.click({}, outcome="abstained", run_record=record)])
        row = [r for r in rows if r.get("event") == "stop"][-1]
        self.assertEqual(0, rc)
        self.assertEqual(1, calls)
        self.assertEqual("work-exhausted", row["case"])
        self.assertTrue(row["legal"])

    def test_work_exhausted_click_dispatches_no_repair_seat(self):
        # H-wm-ladder-lucy-handoff-2026-11-13 package 2 (click 5): a typed
        # :work-exhausted stop pauses and alerts; the repair seat whose scope
        # the Lucy run saw widened must never be dispatched for it.
        record = self.outer_abstention_record("[]", "loop-stop")
        with mock.patch.object(self.loop, "repair") as repair_mock:
            rc, calls, rows = self.run_with([
                self.click({}, outcome="abstained", run_record=record)])
        row = [r for r in rows if r.get("event") == "stop"][-1]
        self.assertEqual(0, rc)
        self.assertEqual("work-exhausted", row["case"])
        self.assertTrue(row["legal"])
        self.assertEqual(0, repair_mock.call_count)
        # run_with patches notify itself; the alert is the stop row's bell.
        self.assertFalse(any(r.get("event") == "repair-dispatched" for r in rows))

    def test_false_work_exhaustion_claim_is_a_debugger_error(self):
        record = self.outer_abstention_record('["still-live"]', "loop-stop")
        rc, _, rows = self.run_with([
            self.click({}, outcome="abstained", run_record=record)])
        row = [r for r in rows if r.get("event") == "stop"][-1]
        self.assertEqual(1, rc)
        self.assertEqual("debugger-error", row["case"])
        self.assertEqual("work-exhausted", row["claimed-case"])
        self.assertFalse(row["legal"])

    def test_debugger_disposition_falls_through_existing_path(self):
        self.args.clicks = 1
        record = self.outer_abstention_record('["still-live"]', "debugger-error")
        rc, _, rows = self.run_with([
            self.click({}, outcome="abstained", run_record=record)])
        stops = [r for r in rows if r.get("event") == "stop"]
        self.assertEqual(0, rc)
        self.assertEqual("operator-override", stops[-1]["case"])
        self.assertFalse(any(r.get("claimed-case") == "work-exhausted" for r in stops))

    def test_first_continuous_debugger_error_gets_a_page_not_quiet(self):
        record = self.outer_abstention_record('["still-live"]', "debugger-error")
        first = self.click({}, outcome="abstained", run_record=record)
        second = self.click({}, carried=True, click_id="click-2", outcome="ok")
        rc, calls, pages, rows = self.run_with_debugger_pages(
            [first, second], [{"counts": {}, "named": [["M-a", "refused"]]}])
        self.assertEqual(0, rc)
        self.assertEqual(2, calls)
        self.assertEqual(1, pages)
        self.assertEqual(1, sum(r.get("event") == "debugger" for r in rows))
        self.assertFalse(any("wall repeats" in r.get("reason", "") for r in rows))

    def test_unchanged_continuous_debugger_page_stops(self):
        record = self.outer_abstention_record('["still-live"]', "debugger-error")
        signature = {"counts": {"refused": 1}, "named": [["M-a", "refused"]]}
        rc, calls, pages, rows = self.run_with_debugger_pages(
            [self.click({}, outcome="abstained", run_record=record),
             self.click({}, click_id="click-2", outcome="abstained", run_record=record)],
            [signature, signature])
        stop = [r for r in rows if r.get("event") == "stop"][-1]
        self.assertEqual(1, rc)
        self.assertEqual(2, calls)
        self.assertEqual(2, pages)
        self.assertEqual("debugger-error", stop["case"])
        self.assertIn("debugger-0.md", stop["reason"])
        self.assertIn("debugger-1.md", stop["reason"])

    def test_moved_continuous_debugger_page_continues(self):
        self.args.clicks = 3
        record = self.outer_abstention_record('["still-live"]', "debugger-error")
        first = {"counts": {"refused": 1}, "named": [["M-a", "refused"]]}
        second = {"counts": {"refused": 1}, "named": [["M-b", "refused"]]}
        rc, calls, pages, rows = self.run_with_debugger_pages(
            [self.click({}, outcome="abstained", run_record=record),
             self.click({}, click_id="click-2", outcome="abstained", run_record=record),
             self.click({}, carried=True, click_id="click-3", outcome="ok")],
            [first, second])
        self.assertEqual(0, rc)
        self.assertEqual(3, calls)
        self.assertEqual(2, pages)
        self.assertFalse(any(r.get("case") == "debugger-error" and
                             "debugger page" in r.get("reason", "") for r in rows))

    def test_moved_then_unmoved_debugger_page_stops(self):
        self.args.clicks = 3
        record = self.outer_abstention_record('["still-live"]', "debugger-error")
        first = {"counts": {"refused": 1}, "named": [["M-a", "refused"]]}
        second = {"counts": {"refused": 1}, "named": [["M-b", "refused"]]}
        rc, calls, pages, rows = self.run_with_debugger_pages(
            [self.click({}, outcome="abstained", run_record=record),
             self.click({}, click_id="click-2", outcome="abstained", run_record=record),
             self.click({}, click_id="click-3", outcome="abstained", run_record=record)],
            [first, second, second])
        stop = [r for r in rows if r.get("event") == "stop"][-1]
        self.assertEqual(1, rc)
        self.assertEqual(3, calls)
        self.assertEqual("debugger-error", stop["case"])
        self.assertIn("debugger-1.md", stop["reason"])
        self.assertIn("debugger-2.md", stop["reason"])

    def test_legal_work_exhaustion_stops_before_the_debugger_page(self):
        record = self.outer_abstention_record("[]", "loop-stop")
        rc, calls, pages, rows = self.run_with_debugger_pages(
            [self.click({}, outcome="abstained", run_record=record)], [])
        stop = [r for r in rows if r.get("event") == "stop"][-1]
        self.assertEqual(0, rc)
        self.assertEqual(0, pages)
        self.assertEqual("work-exhausted", stop["case"])
        self.assertTrue(stop["legal"])

    def assert_typed_stop(self, disposition):
        rc, calls, rows = self.run_with([self.click(disposition)])
        self.assertEqual(1, rc)
        self.assertEqual(1, calls)
        typed = [row for row in rows
                 if row.get("event") == "stop-evidence" and
                 row.get("kind") == "machine-self-heal-failed"]
        self.assertEqual(1, len(typed))
        self.assertEqual("click-1", typed[0]["click-id"])
        self.assertIn("no next iteration started", self.loop.LOGFILE.read_text())

    def test_dedicated_self_heal_refusal_stops(self):
        self.assert_typed_stop({
            "route": "dedicated-meta-self-heal", "status": "refused",
            "ordinary-selection-called?": False,
            "reason": "meta-self-heal/acceptance-run-refused",
            "injury-id": "injury/one", "attempt": 1,
        })

    def test_open_out_of_band_held_stops(self):
        # The live shape (meta_self_heal_production open-out-of-band-route!):
        # no :route key, only :phase :open-out-of-band.
        self.assert_typed_stop({
            "status": "held",
            "reason": "out-of-band/historical-cause-mismatch",
            "phase": "open-out-of-band", "injury-id": "injury/two",
        })

    def test_binding_reads_the_disposition_status_not_a_nested_one(self):
        text = ('{:click/id "click-x" :runtime-disposition '
                '{:medic-write {:status :refused :entry {:status :open}} '
                ':reason :out-of-band/medic-append-refused :status :held '
                ':phase :open-out-of-band :injury-id "injury/five"}}')
        body = self.loop._top_level_map(text, text.find("{", text.find(":runtime-disposition")))
        self.assertEqual("held", self.loop._edn_field(body, "status"))
        self.assertEqual("open-out-of-band", self.loop._edn_field(body, "phase"))
        self.assertEqual("injury/five", self.loop._edn_field(body, "injury-id"))

    def test_abandoned_latest_transition_stops(self):
        transition = (self.loop.META_INJURY_ROOT / "meta-injury-ledger" /
                      "injury" / "three")
        transition.mkdir(parents=True)
        (transition / "9.edn").write_text(
            '{:state :abandoned, :reason :attempt-limit-exhausted, '
            ':injury-id "injury/three", :attempt 3}')
        self.assert_typed_stop({
            "route": "dedicated-meta-self-heal", "status": "discharged",
            "ordinary-selection-called?": False, "injury-id": "injury/three",
        })

    def test_ordinary_carried_run_still_reaches_second_iteration(self):
        ordinary = self.click({
            "route": "ordinary", "status": "complete",
            "ordinary-selection-called?": True,
        }, carried=True, click_id="click-1")
        second = self.click({
            "route": "ordinary", "status": "complete",
            "ordinary-selection-called?": True,
        }, carried=True, click_id="click-2")
        rc, calls, rows = self.run_with([ordinary, second])
        self.assertEqual(0, rc)
        self.assertEqual(2, calls)
        self.assertFalse(any(row.get("kind") == "machine-self-heal-failed" for row in rows))

    def test_medic_discharge_does_not_stop(self):
        first = self.click({
            "route": "open-out-of-band-medic", "status": "discharged",
            "ordinary-selection-called?": False, "injury-id": "injury/four",
        }, click_id="click-1")
        second = self.click({
            "route": "ordinary", "status": "complete",
            "ordinary-selection-called?": True,
        }, carried=True, click_id="click-2")
        rc, calls, rows = self.run_with([first, second])
        self.assertEqual(0, rc)
        self.assertEqual(2, calls)
        self.assertFalse(any(row.get("kind") == "machine-self-heal-failed" for row in rows))


if __name__ == "__main__":
    unittest.main()
