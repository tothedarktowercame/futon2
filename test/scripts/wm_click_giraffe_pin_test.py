import importlib.util
import json
import os
import types
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

MANIFEST_SCOPED = """{:files
 [{:file "input.edn", :sha256 "aa"}],
 :git-sha "0000000000000000000000000000000000000000",
 :invocation
 {:edges "/fixture/holes/missions/giraffe-mission-edges.json",
  :only-ids
  ["T-ladder-08-memory-preregistration"
   "T-ladder-09-the-mirror"]},
 :sources
 {:edges
  {:path "/fixture/holes/missions/giraffe-mission-edges.json"}}}
"""

MANIFEST_WIDE = """{:files
 [{:file "input.edn", :sha256 "bb"}],
 :git-sha "0000000000000000000000000000000000000000",
 :invocation
 {:edges "/fixture/holes/missions/giraffe-mission-edges.json",
  :only-ids nil},
 :sources
 {:edges
  {:path "/fixture/holes/missions/giraffe-mission-edges.json"}}}
"""

MANIFEST_TICKETS_ONLY = """{:files
 [{:file "input.edn", :sha256 "cc"}],
 :sources
 {:edges
  {:path "/fixture/holes/tickets/ticket-edges.json"}
  :tickets-index {:path "/fixture/tickets.json"}}}
"""


def load_loop():
    spec = importlib.util.spec_from_file_location(
        "wm_click_repair_loop", ROOT / "scripts" / "wm_click_repair_loop.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class GiraffePinTest(unittest.TestCase):
    def pin_state(self, loop, td):
        """Bind the loop's state globals to a temp tree; return the pin path."""
        state = Path(td)
        state.mkdir(parents=True, exist_ok=True)
        pin = state / "giraffe-pin.edn"
        patcher_state = mock.patch.multiple(
            loop, GIRAFFE_PIN=pin, LEDGER=state / "ledger.jsonl",
            LOGFILE=state / "loop.log", record=mock.DEFAULT, log=mock.DEFAULT)
        patcher_state.start()
        self.addCleanup(patcher_state.stop)
        (state / "ledger.jsonl").write_text("")
        return pin

    def test_launch_pins_the_newest_missions_run_with_its_only_scope(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            giraffe = Path(td) / "giraffe"
            (giraffe / "20260101T000000.000Z-scoped").mkdir(parents=True)
            (giraffe / "20260101T000000.000Z-scoped" / "manifest.edn").write_text(
                MANIFEST_SCOPED)
            (giraffe / "20260202T000000.000Z-wide").mkdir()
            (giraffe / "20260202T000000.000Z-wide" / "manifest.edn").write_text(
                MANIFEST_WIDE)
            pin = self.pin_state(loop, Path(td) / "state")
            with mock.patch.dict(os.environ, {"GIRAFFE_ROOT": str(giraffe)}):
                self.assertTrue(loop.record_giraffe_pin(mock.Mock(giraffe_run=None)))
            text = pin.read_text()
            self.assertIn(':run-id "20260202T000000.000Z-wide"', text)
            self.assertIn(":only-ids []", text)

    def test_launch_pins_a_scoped_run_when_it_is_the_newest(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            giraffe = Path(td) / "giraffe"
            (giraffe / "20260101T000000.000Z-wide").mkdir(parents=True)
            (giraffe / "20260101T000000.000Z-wide" / "manifest.edn").write_text(
                MANIFEST_WIDE)
            (giraffe / "20260202T000000.000Z-scoped").mkdir()
            (giraffe / "20260202T000000.000Z-scoped" / "manifest.edn").write_text(
                MANIFEST_SCOPED)
            pin = self.pin_state(loop, Path(td) / "state")
            with mock.patch.dict(os.environ, {"GIRAFFE_ROOT": str(giraffe)}):
                self.assertTrue(loop.record_giraffe_pin(mock.Mock(giraffe_run=None)))
            text = pin.read_text()
            self.assertIn(':run-id "20260202T000000.000Z-scoped"', text)
            self.assertIn("T-ladder-08-memory-preregistration", text)
            self.assertIn("T-ladder-09-the-mirror", text)

    def test_tickets_only_runs_are_not_pinned(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            giraffe = Path(td) / "giraffe"
            (giraffe / "20260303T000000.000Z-tickets").mkdir(parents=True)
            (giraffe / "20260303T000000.000Z-tickets" / "manifest.edn").write_text(
                MANIFEST_TICKETS_ONLY)
            pin = self.pin_state(loop, Path(td) / "state")
            with mock.patch.dict(os.environ, {"GIRAFFE_ROOT": str(giraffe)}):
                self.assertFalse(loop.record_giraffe_pin(mock.Mock(giraffe_run=None)))
            self.assertFalse(pin.exists())

    def test_stop_clears_the_pin(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            pin = self.pin_state(loop, Path(td))
            pin.write_text('{:run-id "x" :only-ids []}\n')
            with mock.patch.object(loop, "notify", lambda *a, **k: None), \
                 mock.patch.object(loop, "_validated_stop",
                                   lambda args, case, evidence: (case, True)):
                loop.stop(mock.Mock(), "operator-override", "test", 0, {})
            self.assertFalse(pin.exists())

    def test_operator_names_the_run_over_the_newest(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            giraffe = Path(td) / "giraffe"
            (giraffe / "20260101T000000.000Z-older").mkdir(parents=True)
            (giraffe / "20260101T000000.000Z-older" / "manifest.edn").write_text(
                MANIFEST_SCOPED)
            (giraffe / "20260202T000000.000Z-newer").mkdir()
            (giraffe / "20260202T000000.000Z-newer" / "manifest.edn").write_text(
                MANIFEST_WIDE)
            pin = self.pin_state(loop, Path(td) / "state")
            args = mock.Mock(giraffe_run="20260101T000000.000Z-older")
            with mock.patch.dict(os.environ, {"GIRAFFE_ROOT": str(giraffe)}):
                self.assertTrue(loop.record_giraffe_pin(args))
            text = pin.read_text()
            self.assertIn(':run-id "20260101T000000.000Z-older"', text)
            self.assertIn("T-ladder-08-memory-preregistration", text)

    def test_operator_named_run_must_exist_and_be_a_missions_run(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            giraffe = Path(td) / "giraffe"
            giraffe.mkdir(parents=True)
            (giraffe / "20260303T000000.000Z-tickets").mkdir()
            (giraffe / "20260303T000000.000Z-tickets" / "manifest.edn").write_text(
                MANIFEST_TICKETS_ONLY)
            pin = self.pin_state(loop, Path(td) / "state")
            with mock.patch.dict(os.environ, {"GIRAFFE_ROOT": str(giraffe)}):
                missing = mock.Mock(giraffe_run="99991231T000000.000Z-none")
                self.assertFalse(loop.record_giraffe_pin(missing))
                tickets = mock.Mock(giraffe_run="20260303T000000.000Z-tickets")
                self.assertFalse(loop.record_giraffe_pin(tickets))
            self.assertFalse(pin.exists())

    def test_run_without_a_pin_stops_before_any_click(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            state = Path(td)
            pin = state / "giraffe-pin.edn"
            empty_root = Path(td) / "giraffe-empty"
            empty_root.mkdir()
            patcher = mock.patch.multiple(
                loop, STATE=state, GIRAFFE_PIN=pin, LEDGER=state / "ledger.jsonl",
                LOGFILE=state / "loop.log", STOP_FILE=state / "STOP",
                record=loop.record, log=mock.DEFAULT, notify=mock.DEFAULT,
                jvm_start_epoch=mock.DEFAULT, reload_when_idle=mock.DEFAULT,
                fire=mock.DEFAULT, repair=mock.DEFAULT, debugger_stops=mock.DEFAULT)
            patcher.start()
            self.addCleanup(patcher.stop)
            (state / "ledger.jsonl").write_text("")
            loop.jvm_start_epoch.return_value = 0
            loop.reload_when_idle.return_value = (True, "", 0)
            loop.debugger_stops.return_value = []
            # On a base loop that ignores the pin failure this makes the
            # "should never happen" click loud and terminating instead of a
            # silent hang: the fired click raises out of run().
            loop.fire.side_effect = RuntimeError(
                "unpinned loop fired a click")
            args = types.SimpleNamespace(
                clicks=3, ticks=10**6, stall=3, max_repairs=3,
                repair_seat=None, repair_reviewer="r",
                observe_only=False, continuous=False,
                giraffe_run=None)
            with mock.patch.dict(os.environ, {"GIRAFFE_ROOT": str(empty_root)}):
                code = loop.run(args)
            self.assertEqual(1, code)
            loop.fire.assert_not_called()
            loop.repair.assert_not_called()
            rows = [json.loads(line) for line
                    in (state / "ledger.jsonl").read_text().splitlines() if line]
            stops = [r for r in rows if r.get("event") == "stop"]
            self.assertEqual(1, len(stops))
            self.assertEqual("no-giraffe-pin", stops[0]["case"])
            self.assertTrue(stops[0]["legal"])


if __name__ == "__main__":
    unittest.main()
