import importlib.util
import os
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
                self.assertTrue(loop.record_giraffe_pin(args=mock.Mock()))
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
                self.assertTrue(loop.record_giraffe_pin(args=mock.Mock()))
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
                self.assertFalse(loop.record_giraffe_pin(args=mock.Mock()))
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


if __name__ == "__main__":
    unittest.main()
