"""WM hardening package 4: one writer per checkout.

Handoff 2026-11-13 section 3, "Shared-tree races": during one hold the repair
seat committed the coordinator's uncommitted patch as its own work and
reported the coordinator moving it out as "another writer resetting it".

Bad case (acceptance): a throwaway git repo standing in for F2 with an
uncommitted change to a tracked file when a repair would be dispatched -- the
loop stops typed "dirty-checkout", dispatch is never called, and no lease file
is left behind.  Plus the lease lifecycle and the post-repair foreign-dirt
alert.
"""

import argparse
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
LOOP = ROOT / "scripts" / "wm_click_repair_loop.py"

import wm_click_hermetic  # noqa: E402 (needs the test dir on sys.path first)

_TEST_STATE = tempfile.TemporaryDirectory(prefix="wm-click-p4-test-state-")


def load_loop():
    spec = importlib.util.spec_from_file_location("wm_click_repair_loop_p4", LOOP)
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
    module.REPAIR_LEASE = state / "repair-lease.json"
    return wm_click_hermetic.harden(module)


def args_for(loop):
    return argparse.Namespace(
        repair_seat="repair-seat", seat_model="m", repair_timeout=60,
        owner="owner")


class DirtyCheckoutStopTest(unittest.TestCase):
    def test_dirty_checkout_stops_typed_before_dispatch(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            base = Path(td)
            f2 = base / "futon2"
            f2.mkdir()
            (f2 / "src.txt").write_text("base\n")
            import subprocess
            subprocess.run(["git", "-C", str(f2), "init", "-q"], check=True,
                           capture_output=True)
            subprocess.run(["git", "-C", str(f2), "add", "src.txt"], check=True,
                           capture_output=True)
            subprocess.run(["git", "-C", str(f2), "commit", "-qm", "base"],
                           check=True, capture_output=True,
                           env={"GIT_AUTHOR_NAME": "t", "GIT_AUTHOR_EMAIL": "t@t",
                                "GIT_COMMITTER_NAME": "t",
                                "GIT_COMMITTER_EMAIL": "t@t",
                                "PATH": "/usr/bin:/bin:/usr/local/bin",
                                "HOME": str(base)})
            # The bad case: a tracked file with an uncommitted change.
            (f2 / "src.txt").write_text("base\ncoordinator patch\n")
            loop.F2 = f2
            loop.F3C = base / "no-futon3c"
            loop.MATHLIB4 = base / "no-mathlib4"
            dispatch = mock.Mock()
            loop.dispatch = dispatch
            loop.ensure_seat = mock.Mock()
            with self.assertRaises(loop.DirtyCheckoutError) as caught:
                loop.repair(args_for(loop), "click failed", None, [])
            self.assertEqual([{"repo": str(f2), "paths": ["src.txt"]}],
                             caught.exception.dirty)
            dispatch.assert_not_called()
            # The lease file is not left behind (it was never written).
            self.assertFalse(loop.REPAIR_LEASE.exists())

    def test_dirty_checkout_stop_is_legal_only_with_paths(self):
        loop = load_loop()
        ev = [{"repo": "/x/futon2", "paths": ["src/foo.clj"]}]
        self.assertEqual(("dirty-checkout", True),
                         loop._validated_stop(args_for(loop), "dirty-checkout",
                                              {"dirty-checkouts": ev}))
        for bad in ({}, {"dirty-checkouts": []}, {"dirty-checkouts": "src"},
                    {"dirty-checkouts": [{"repo": "/x", "paths": []}]}):
            self.assertEqual(
                ("debugger-error", False),
                loop._validated_stop(args_for(loop), "dirty-checkout", bad),
                f"should be illegal: {bad!r}")


class RepairLeaseTest(unittest.TestCase):
    def setUp(self):
        import subprocess
        self.tmp = tempfile.TemporaryDirectory()
        base = Path(self.tmp.name)
        self.f2 = base / "futon2"
        self.f2.mkdir()
        (self.f2 / "src.txt").write_text("base\n")
        # Production futon2 ignores data/*; the loop's state lives there.
        (self.f2 / ".gitignore").write_text("data/\n")
        env = {"GIT_AUTHOR_NAME": "t", "GIT_AUTHOR_EMAIL": "t@t",
               "GIT_COMMITTER_NAME": "t", "GIT_COMMITTER_EMAIL": "t@t",
               "PATH": "/usr/bin:/bin:/usr/local/bin", "HOME": str(base)}
        for cmd in (["init", "-q"], ["add", "src.txt", ".gitignore"],
                    ["commit", "-qm", "base"]):
            subprocess.run(["git", "-C", str(self.f2), *cmd], check=True,
                           capture_output=True, env=env)
        self.loop = load_loop()
        self.loop.F2 = self.f2
        self.loop.F3C = base / "no-futon3c"
        self.loop.MATHLIB4 = base / "no-mathlib4"
        self.loop.ensure_seat = mock.Mock()
        self.loop.dispatch = mock.Mock(return_value="job-1")

    def tearDown(self):
        self.tmp.cleanup()

    def test_clean_checkout_dispatches_with_lease_and_clears_it(self):
        loop = self.loop
        seen = {}

        def fake_wait(job, timeout):
            seen["lease_exists_during_repair"] = loop.REPAIR_LEASE.exists()
            seen["lease"] = json.loads(loop.REPAIR_LEASE.read_text())
            return {"state": "done", "result": "VERDICT: no-defect"}

        loop.wait_job = fake_wait
        verdict, detail = loop.repair(args_for(loop), "click failed", None, [])
        self.assertEqual("no-defect", verdict)
        loop.dispatch.assert_called_once()
        # The packet tells the seat the lease exists and where other writers go.
        packet = loop.dispatch.call_args[0][1]
        self.assertIn(str(loop.REPAIR_LEASE), packet)
        self.assertIn("~/worktrees/<repo>/<name>", packet)
        # The lease carried seat, job, repos and a start time while held...
        self.assertTrue(seen["lease_exists_during_repair"])
        self.assertEqual("repair-seat", seen["lease"]["seat"])
        self.assertEqual("job-1", seen["lease"]["job"])
        self.assertEqual([str(self.f2)], seen["lease"]["repos"])
        self.assertIn("started-at", seen["lease"])
        # ...and is cleared when the repair ends.
        self.assertFalse(loop.REPAIR_LEASE.exists())

    def test_lease_cleared_even_when_judging_crashes(self):
        loop = self.loop

        def boom(job, timeout):
            raise RuntimeError("judge crash")

        loop.wait_job = boom
        with self.assertRaises(RuntimeError):
            loop.repair(args_for(loop), "click failed", None, [])
        self.assertFalse(loop.REPAIR_LEASE.exists())

    def test_dirt_that_appeared_during_repair_is_alerted_not_folded(self):
        loop = self.loop

        def fake_wait(job, timeout):
            # Another writer dirties a tracked path mid-repair, on a path the
            # seat's commits do not cover.
            (self.f2 / "src.txt").write_text("base\nsomeone else\n")
            (self.f2 / "untracked.txt").write_text("new\n")
            return {"state": "done", "result": "VERDICT: repaired"}

        loop.wait_job = fake_wait
        # No tagged commits in the throwaway repo: everything dirty is foreign.
        verdict, detail = loop.repair(args_for(loop), "click failed", None, [])
        self.assertIn("alert: checkout dirty on paths the repair did not commit",
                      detail)
        rows = [json.loads(l) for l in loop.ALERTS.read_text().splitlines()]
        self.assertEqual(1, len(rows))
        self.assertEqual("foreign-dirty-paths", rows[0]["event"])
        paths = set()
        for r in rows[0]["dirty"]:
            paths.update(r["paths"])
        self.assertEqual({"src.txt", "untracked.txt"}, paths)
        self.assertFalse(loop.REPAIR_LEASE.exists())

    def test_loop_state_writes_do_not_count_as_dirty(self):
        loop = self.loop
        # Loop-own writes under its (gitignored) state dir must not stop a
        # dispatch even if git were shown them.
        (self.f2 / "data").mkdir()
        (self.f2 / "data" / "wm-click-loop").mkdir()
        (self.f2 / "data" / "wm-click-loop" / "ledger.jsonl").write_text("{}\n")
        self.assertEqual([], loop.dirty_checkout_changes())


if __name__ == "__main__":
    unittest.main()
