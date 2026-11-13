import importlib.util
import json
import os
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest import mock


ROOT = Path(os.environ.get("FUTON2_UNDER_TEST", Path(__file__).resolve().parents[2]))

import wm_click_hermetic  # noqa: E402 (needs the test dir on sys.path first)


def load_loop():
    path = ROOT / "scripts" / "wm_click_repair_loop.py"
    spec = importlib.util.spec_from_file_location("wm_click_attach_under_test", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return wm_click_hermetic.harden(module)


class DebuggerAttachTest(unittest.TestCase):
    def setUp(self):
        self.loop = load_loop()
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        canonical_state = self.loop.STATE
        self.loop.STATE = root / "state"
        self.loop.STATE.mkdir()
        self.loop.LEDGER = self.loop.STATE / "ledger.jsonl"
        self.loop.LOGFILE = self.loop.STATE / "loop.log"
        self.loop.STOP_FILE = self.loop.STATE / "STOP"
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
        self.args = SimpleNamespace(
            clicks=1, ticks=99, stall=3, max_repairs=3,
            repair_seat="repair", continuous=False, observe_only=False,
            owner="owner", issuing_caller="caller", author="author",
            reviewer="reviewer", repair_reviewer="repair-reviewer")
        self.click = {
            "n": 1, "rc": 0, "click-id": "click-1", "spent": True,
            "outcome": "built", "run-id": "run-1", "carried": True,
            "last-result": {"runtime-disposition": {"route": "ordinary",
                            "status": "complete", "ordinary-selection-called?": True}},
            "log": "", "run-record": None, "verdict": None, "alert": None,
            "why": None, "since-epoch": 0, "in-flight-repairs": []}

    def run_case(self, replies):
        calls = []

        def jvm_eval(form, timeout=600):
            calls.append(form)
            values = replies.get(form)
            value = values.pop(0) if isinstance(values, list) else values
            return value or {"rc": 0, "out": "{:value nil}", "err": ""}

        with (mock.patch.object(self.loop, "jvm_start_epoch", return_value=0),
              mock.patch.object(self.loop, "reload_when_idle", return_value=(True, "", 0)),
              mock.patch.object(self.loop, "jvm_eval", side_effect=jvm_eval),
              mock.patch.object(self.loop, "fire", return_value=dict(self.click)) as fire,
              mock.patch.object(self.loop, "notify")):
            rc = self.loop.run(self.args)
        rows = [json.loads(line) for line in self.loop.LEDGER.read_text().splitlines()]
        return rc, fire.call_count, calls, rows

    def test_detached_zero_stops_attaches_then_fires(self):
        rc, fires, calls, rows = self.run_case({
            "(futon2.aif.wm.debugger/stopped)": [
                {"rc": 0, "out": "{:value []}", "err": ""},
                {"rc": 0, "out": "{:value []}", "err": ""}],
            "(futon2.aif.wm.debugger/attached?)": [
                {"rc": 0, "out": "{:ok true, :value false}", "err": ""},
                {"rc": 0, "out": "{:value true}", "err": ""}],
            "(futon2.aif.wm.debugger/attach!)": {"rc": 0, "out": "{:value :attached}", "err": ""}})
        self.assertEqual((0, 1), (rc, fires))
        self.assertIn("(futon2.aif.wm.debugger/attach!)", calls)
        self.assertTrue(any(row.get("event") == "debugger-attached" for row in rows))

    def test_attach_does_not_take_stops_without_fire(self):
        rc, fires, _, rows = self.run_case({
            "(futon2.aif.wm.debugger/stopped)": {"rc": 0, "out": "{:value []}", "err": ""},
            "(futon2.aif.wm.debugger/attached?)": {"rc": 0, "out": "{:ok true, :value false}", "err": ""},
            "(futon2.aif.wm.debugger/attach!)": {"rc": 0, "out": "{:value nil}", "err": ""}})
        self.assertEqual((1, 0), (rc, fires))
        self.assertTrue(any(row.get("kind") == "debugger-not-attached" for row in rows))

    def test_two_stops_stop_without_fire(self):
        stopped = ('{:value [{:run-id "one" :phase :author :condition {:kind :a}} '
                   '{:run-id "two" :phase :review :condition {:kind :b}}]}')
        rc, fires, _, rows = self.run_case({
            "(futon2.aif.wm.debugger/stopped)": {"rc": 0, "out": stopped, "err": ""}})
        self.assertEqual((1, 0), (rc, fires))
        self.assertTrue(any(row.get("kind") == "self-heal-debugger-frames-stacked"
                            for row in rows))

    def test_already_attached_does_not_attach_and_fires(self):
        rc, fires, calls, _ = self.run_case({
            "(futon2.aif.wm.debugger/stopped)": {"rc": 0, "out": "{:value []}", "err": ""},
            "(futon2.aif.wm.debugger/attached?)": {"rc": 0, "out": "{:value true}", "err": ""}})
        self.assertEqual((0, 1), (rc, fires))
        self.assertNotIn("(futon2.aif.wm.debugger/attach!)", calls)


if __name__ == "__main__":
    unittest.main()
