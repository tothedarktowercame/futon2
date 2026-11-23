import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]
LOOP = ROOT / "scripts" / "wm_click_repair_loop.py"


def load_loop():
    spec = importlib.util.spec_from_file_location("wm_click_poll_under_test", LOOP)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class Process:
    def __init__(self, polls):
        self.polls = iter(polls)
        self.returncode = 0
        self.killed = False
        self.waited = False

    def poll(self):
        return next(self.polls, self.returncode)

    def kill(self):
        self.killed = True
        self.returncode = -9

    def wait(self):
        self.waited = True
        return self.returncode


class DebuggerPollTest(unittest.TestCase):
    def setUp(self):
        self.loop = load_loop()
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.loop.STATE = Path(self.temp.name) / "state"
        self.loop.STATE.mkdir()
        self.loop.DEBUGGER = self.loop.STATE / "debugger"
        self.args = SimpleNamespace(
            task=None, issuing_caller="caller", author="author",
            reviewer="reviewer", repair_reviewer="repair-reviewer")

    def fire(self, proc, debugger_reads, limit=30):
        events = []
        with (mock.patch.object(self.loop, "F2", Path(self.temp.name)),
              mock.patch.object(self.loop, "reset_cast_sessions"),
              mock.patch.object(self.loop, "budget_lines", side_effect=[0, 0]),
              mock.patch.object(self.loop.subprocess, "Popen", return_value=proc),
              mock.patch.object(self.loop, "debugger_stop", side_effect=debugger_reads),
              mock.patch.object(self.loop, "record", side_effect=events.append),
              mock.patch.object(self.loop.time, "sleep"),
              mock.patch.object(self.loop, "get_json", return_value={"last-result": None}),
              mock.patch.object(self.loop, "click_record", return_value=None),
              mock.patch.object(self.loop, "read_verdict", return_value=None),
              mock.patch.object(self.loop, "selection_why", return_value=None),
              mock.patch.object(self.loop, "carried", return_value=False),
              mock.patch.object(self.loop, "DEBUGGER_UNREADABLE_LIMIT", limit),
              mock.patch.object(self.loop, "log")):
            result = self.loop.fire(1, self.args, 123)
        return result, events

    def test_one_failed_read_then_success_continues_and_records_one_event(self):
        proc = Process([None, None, 0])
        result, events = self.fire(
            proc, [self.loop.DebuggerUnreadable("transient"), None])
        self.assertFalse(proc.killed)
        self.assertIsNone(result["debugger-unreadable"])
        unreadable = [event for event in events
                      if event.get("event") == "debugger-unreadable"]
        self.assertEqual(1, len(unreadable))
        self.assertEqual("transient", unreadable[0]["detail"])

    def test_failure_limit_kills_subprocess(self):
        proc = Process([None, None, None])
        result, events = self.fire(
            proc, [self.loop.DebuggerUnreadable("first"),
                   self.loop.DebuggerUnreadable("still unavailable")], limit=2)
        self.assertTrue(proc.killed)
        self.assertTrue(proc.waited)
        self.assertEqual("still unavailable", result["debugger-unreadable"])
        self.assertEqual(1, len([event for event in events
                                if event.get("event") == "debugger-unreadable"]))

    def test_failure_result_becomes_typed_stop_without_another_click(self):
        args = SimpleNamespace(
            clicks=1, ticks=99, stall=3, max_repairs=3, repair_seat="repair",
            continuous=False, observe_only=False, owner="owner",
            issuing_caller="caller", author="author", reviewer="reviewer",
            repair_reviewer="repair-reviewer")
        click = {"n": 1, "since-epoch": 0,
                 "debugger-unreadable": "still unavailable"}
        self.loop.LEDGER = self.loop.STATE / "ledger.jsonl"
        self.loop.LOGFILE = self.loop.STATE / "loop.log"
        self.loop.STOP_FILE = self.loop.STATE / "STOP"
        with (mock.patch.object(self.loop, "record_giraffe_pin", return_value=True),
              mock.patch.object(self.loop, "jvm_start_epoch", return_value=0),
              mock.patch.object(self.loop, "reload_when_idle", return_value=(True, "", 0)),
              mock.patch.object(self.loop, "debugger_stops", return_value=[]),
              mock.patch.object(self.loop, "injury_limit_breach", return_value=None),
              mock.patch.object(self.loop, "debugger_attached", return_value=True),
              mock.patch.object(self.loop, "fire_with_write_manifest",
                                return_value=click) as fire,
              mock.patch.object(self.loop, "notify")):
            rc = self.loop.run(args)
        rows = [json.loads(line) for line in self.loop.LEDGER.read_text().splitlines()]
        stopped = next(row for row in rows if row.get("event") == "stop")
        self.assertEqual(1, rc)
        self.assertEqual(1, fire.call_count)
        self.assertEqual("debugger-error", stopped["case"])
        self.assertEqual("debugger-unreadable: still unavailable", stopped["reason"])

    def test_reload_defers_when_debugger_is_unreadable(self):
        with (mock.patch.object(self.loop, "click_running", return_value=True),
              mock.patch.object(self.loop, "debugger_stop",
                                side_effect=self.loop.DebuggerUnreadable("hiccup")),
              mock.patch.object(self.loop, "reload_plan") as plan,
              mock.patch.object(self.loop, "record"),
              mock.patch.object(self.loop, "log")):
            result = self.loop.reload_changed(123, held_run_id="held-run")
        self.assertEqual((None, "click-in-flight", 123), result)
        plan.assert_not_called()


if __name__ == "__main__":
    unittest.main()
