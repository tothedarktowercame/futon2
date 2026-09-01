import copy
import tempfile
import unittest
from unittest import mock
from pathlib import Path

from scripts import writer_fence_restore as sut


def coordinator(status, enabled, runtime, watchdog, witness=None):
    return {"present?": True, "enabled?": enabled, "durable-status": status,
            "tick-claim": None, "runtime-scheduler-present?": runtime,
            "watchdog-scheduler-present?": watchdog,
            "quiescence-witness": witness}


class FakeBackend:
    def __init__(self, states, fail=()): self.states, self.executed, self.fail = states, [], set(fail)
    def observe(self, identity, _entry): return copy.deepcopy(self.states[identity])
    def execute(self, action, identity):
        self.executed.append((action, identity))
        if identity in self.fail: return {"ok": False}
        entry = self.states[identity]
        if action == "rearm-terminal-coordinator": entry["watchdog-scheduler-present?"] = True
        elif action == "resume-coordinator": entry.update({"durable-status": "running", "enabled?": True})
        else: entry["ActiveState"] = "active"
        return {"ok": True}


class RestoreTest(unittest.TestCase):
    KEY = b"fixture secret with at least thirty two bytes"

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
        return dict(body, **{"manifest-hmac-sha256": sut.authenticate(body, self.KEY)})

    def rows(self, manifest, specs):
        return [{"schema": sut.SCHEMA, "fence-id": manifest["fence-id"],
                 "manifest-hmac-sha256": manifest["manifest-hmac-sha256"],
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
        with tempfile.TemporaryDirectory() as d, self.assertRaisesRegex(ValueError, "restored-state-without-inverse-attempt"):
            sut.restore(manifest, rows, FakeBackend(states), Path(d) / "outcomes", self.KEY)

    def test_swapped_verb_and_contradictory_current_state_refuse(self):
        manifest = self.manifest(); states = self.parked_states()
        wrong = self.rows(manifest, [("resume-coordinator", sut.TERMINAL_ID)])
        with tempfile.TemporaryDirectory() as d, self.assertRaisesRegex(ValueError, "journal-row-invalid"):
            sut.restore(manifest, wrong, FakeBackend(states), Path(d) / "outcomes", self.KEY)
        states[sut.TERMINAL_ID]["durable-status"] = "stopped"
        right = self.rows(manifest, [("rearm-terminal-coordinator", sut.TERMINAL_ID)])
        with tempfile.TemporaryDirectory() as d, self.assertRaisesRegex(ValueError, "journal-action-not-observed"):
            sut.restore(manifest, right, FakeBackend(states), Path(d) / "outcomes", self.KEY)

    def test_partial_prefix_restores_only_prefix_in_reverse(self):
        manifest = self.manifest(); backend = FakeBackend(self.parked_states())
        specs = [("rearm-terminal-coordinator", sut.TERMINAL_ID),
                 ("resume-coordinator", sut.RUNNING_IDS[0])]
        with tempfile.TemporaryDirectory() as d:
            outcomes = sut.restore(manifest, self.rows(manifest, specs), backend, Path(d) / "outcomes", self.KEY)
        self.assertEqual(list(reversed(specs)), backend.executed)
        self.assertEqual(2, len(outcomes["outcomes"]))
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

    def test_manifest_authentication_and_fence_binding_refuse_laundering(self):
        manifest = self.manifest()
        manifest["targets"][sut.TERMINAL_ID]["class"] = "running-coordinator"
        with self.assertRaisesRegex(ValueError, "manifest-authentication-invalid"):
            sut.validate_manifest(manifest, self.KEY, "fixture")
        manifest = self.manifest()
        with self.assertRaisesRegex(ValueError, "manifest-fence-id-mismatch"):
            sut.validate_manifest(manifest, self.KEY, "other-fence")

    def test_empty_subject_is_not_success(self):
        manifest = self.manifest()
        with tempfile.TemporaryDirectory() as d, self.assertRaisesRegex(ValueError, "NOTHING-RECORDED"):
            sut.restore(manifest, [], FakeBackend(self.parked_states()), Path(d) / "outcomes", self.KEY)
        body = {"schema": sut.SCHEMA, "fence-id": "fixture", "captured-at": "now", "targets": {}}
        empty = dict(body, **{"manifest-hmac-sha256": sut.authenticate(body, self.KEY)})
        with self.assertRaisesRegex(ValueError, "NOTHING-RECORDED:manifest-zero-targets"):
            sut.validate_manifest(empty, self.KEY, "fixture")

    def test_failed_partial_restore_resumes_idempotently(self):
        manifest = self.manifest(); states = self.parked_states()
        specs = [("rearm-terminal-coordinator", sut.TERMINAL_ID),
                 ("resume-coordinator", sut.RUNNING_IDS[0])]
        rows = self.rows(manifest, specs)
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / "outcomes"
            backend = FakeBackend(states, fail={sut.TERMINAL_ID})
            with self.assertRaisesRegex(RuntimeError, "restore-action-failed"):
                sut.restore(manifest, rows, backend, path, self.KEY)
            self.assertEqual([("resume-coordinator", sut.RUNNING_IDS[0]),
                              ("rearm-terminal-coordinator", sut.TERMINAL_ID)], backend.executed)
            backend.fail.clear(); backend.executed.clear()
            sut.restore(manifest, rows, backend, path, self.KEY)
            self.assertEqual([("rearm-terminal-coordinator", sut.TERMINAL_ID)], backend.executed)

    def test_restore_reobserves_immediately_before_inverse(self):
        manifest = self.manifest(); rows = self.rows(
            manifest, [("resume-coordinator", sut.RUNNING_IDS[0])])
        backend = FakeBackend(self.parked_states())
        sut.validate_rows(manifest, rows)
        backend.states[sut.RUNNING_IDS[0]] = coordinator("stopped", True, False, False)
        with tempfile.TemporaryDirectory() as d, self.assertRaisesRegex(ValueError, "journal-action-not-observed"):
            sut.restore(manifest, rows, backend, Path(d) / "outcomes", self.KEY)
        self.assertEqual([], backend.executed)

    def test_key_must_be_owner_only(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "key"
            path.write_bytes(self.KEY)
            for mode in (0o640, 0o604, 0o644):
                with self.subTest(mode=oct(mode)):
                    path.chmod(mode)
                    with self.assertRaisesRegex(ValueError, "manifest-key-not-owner-only"):
                        sut.read_key(path)
            path.chmod(0o600)
            self.assertEqual(self.KEY, sut.read_key(path))

    def test_successful_inverse_with_lost_append_is_reconciled(self):
        manifest = self.manifest(); rows = self.rows(
            manifest, [("resume-coordinator", sut.RUNNING_IDS[0])])
        backend = FakeBackend(self.parked_states())
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "outcomes"
            real_append = sut.append_record
            calls = 0
            def fail_outcome(path_arg, row):
                nonlocal calls
                calls += 1
                if calls == 2:
                    raise OSError("fixture-append-failed")
                return real_append(path_arg, row)
            with mock.patch.object(sut, "append_record", side_effect=fail_outcome):
                with self.assertRaisesRegex(OSError, "fixture-append-failed"):
                    sut.restore(manifest, rows, backend, path, self.KEY)
            self.assertEqual([("resume-coordinator", sut.RUNNING_IDS[0])], backend.executed)
            backend.executed.clear()
            result = sut.restore(manifest, rows, backend, path, self.KEY)
            self.assertEqual([], backend.executed)
            self.assertEqual([sut.RUNNING_IDS[0]], result["reconciled-missing-outcomes"])
            recorded = sut.load_journal(path)
            self.assertEqual("observed-restored-outcome-record-missing",
                             recorded[0]["reconciliation"])
            self.assertIs(real_append, sut.append_record)

    def test_fabricated_attempt_cannot_authorize_reconciliation(self):
        manifest = self.manifest(); rows = self.rows(
            manifest, [("resume-coordinator", sut.RUNNING_IDS[0])])
        restored = {sut.RUNNING_IDS[0]: coordinator("running", True, False, False)}
        with tempfile.TemporaryDirectory() as directory:
            outcomes = Path(directory) / "outcomes"
            attempt = {"schema": sut.SCHEMA, "fence-id": manifest["fence-id"],
                       "manifest-hmac-sha256": manifest["manifest-hmac-sha256"],
                       "ordinal": 1, "target": sut.RUNNING_IDS[0],
                       "action": "resume-coordinator",
                       "status": "inverse-attempt-recorded", "recorded-at": "forged"}
            sut.append_record(str(outcomes) + ".attempts.jsonl", attempt)
            with self.assertRaisesRegex(ValueError, "restore-attempt-invalid"):
                sut.restore(manifest, rows, FakeBackend(restored), outcomes, self.KEY)

    def test_every_verdict_envelope_carries_residual_limitation(self):
        for envelope in (sut.verdict_envelope(True, value={}),
                         sut.verdict_envelope(False, reason="fixture")):
            self.assertEqual(sut.RESIDUAL_LIMITATION,
                             envelope["residual-limitation"])


if __name__ == "__main__": unittest.main()
