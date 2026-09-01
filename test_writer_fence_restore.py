import copy
import tempfile
import unittest
from pathlib import Path

from scripts import writer_fence_restore as sut


def coordinator(status, enabled, runtime, watchdog, witness=None):
    return {"present?": True, "enabled?": enabled, "durable-status": status,
            "tick-claim": None, "runtime-scheduler-present?": runtime,
            "watchdog-scheduler-present?": watchdog,
            "quiescence-witness": witness}


class FakeBackend:
    def __init__(self, states): self.states, self.executed = states, []
    def observe(self, identity, _entry): return copy.deepcopy(self.states[identity])
    def execute(self, action, identity):
        self.executed.append((action, identity)); return {"ok": True}


class RestoreTest(unittest.TestCase):
    def manifest(self):
        body = {"schema": sut.SCHEMA, "fence-id": "fixture", "captured-at": "now",
                "targets": {
                    sut.TERMINAL_ID: {"kind": "coordinator", "class": "terminal-watchdog",
                        "pre-state": coordinator("complete", True, False, True)},
                    sut.RUNNING_IDS[0]: {"kind": "coordinator", "class": "running-coordinator",
                        "pre-state": coordinator("running", True, True, True)},
                    "fixture.timer": {"kind": "unit", "class": "systemd-unit",
                        "pre-state": {"ActiveState": "active", "UnitFileState": "enabled"}},
                }}
        return dict(body, **{"manifest-sha256": sut.digest(body)})

    def rows(self, manifest, specs):
        return [{"schema": sut.SCHEMA, "manifest-sha256": manifest["manifest-sha256"],
                 "ordinal": i, "action": action, "target": target, "recorded-at": "now"}
                for i, (action, target) in enumerate(specs, 1)]

    def parked_states(self):
        return {sut.TERMINAL_ID: coordinator("complete", True, False, False),
                sut.RUNNING_IDS[0]: coordinator("stopped", False, False, False,
                                                {"state/type": "durable-quiescence-witness"}),
                "fixture.timer": {"ActiveState": "inactive"}}

    def test_claimed_action_that_did_not_happen_is_rejected(self):
        manifest = self.manifest(); states = self.parked_states()
        states[sut.RUNNING_IDS[0]] = manifest["targets"][sut.RUNNING_IDS[0]]["pre-state"]
        rows = self.rows(manifest, [("resume-coordinator", sut.RUNNING_IDS[0])])
        with self.assertRaisesRegex(ValueError, "journal-action-not-observed"):
            sut.restore(manifest, rows, FakeBackend(states))

    def test_swapped_verb_and_contradictory_current_state_refuse(self):
        manifest = self.manifest(); states = self.parked_states()
        wrong = self.rows(manifest, [("resume-coordinator", sut.TERMINAL_ID)])
        with self.assertRaisesRegex(ValueError, "journal-row-invalid"):
            sut.restore(manifest, wrong, FakeBackend(states))
        states[sut.TERMINAL_ID]["durable-status"] = "stopped"
        right = self.rows(manifest, [("rearm-terminal-coordinator", sut.TERMINAL_ID)])
        with self.assertRaisesRegex(ValueError, "journal-action-not-observed"):
            sut.restore(manifest, right, FakeBackend(states))

    def test_partial_prefix_restores_only_prefix_in_reverse(self):
        manifest = self.manifest(); backend = FakeBackend(self.parked_states())
        specs = [("rearm-terminal-coordinator", sut.TERMINAL_ID),
                 ("resume-coordinator", sut.RUNNING_IDS[0])]
        outcomes = sut.restore(manifest, self.rows(manifest, specs), backend)
        self.assertEqual(list(reversed(specs)), backend.executed)
        self.assertEqual(2, len(outcomes))
        self.assertNotIn(("start-unit", "fixture.timer"), backend.executed)

    def test_record_appends_only_after_park_is_observed(self):
        manifest = self.manifest()
        with tempfile.TemporaryDirectory() as directory:
            journal = str(Path(directory) / "journal.jsonl")
            live = FakeBackend({sut.TERMINAL_ID:
                                manifest["targets"][sut.TERMINAL_ID]["pre-state"]})
            with self.assertRaisesRegex(ValueError, "park-not-observed"):
                sut.record(manifest, journal, "rearm-terminal-coordinator",
                           sut.TERMINAL_ID, live)
            self.assertFalse(Path(journal).exists())
            parked = FakeBackend({sut.TERMINAL_ID: self.parked_states()[sut.TERMINAL_ID]})
            sut.record(manifest, journal, "rearm-terminal-coordinator",
                       sut.TERMINAL_ID, parked)
            self.assertEqual(1, len(sut.load_journal(journal)))


if __name__ == "__main__": unittest.main()
