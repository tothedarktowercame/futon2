import datetime as dt
import json
from pathlib import Path
import tempfile
import unittest
from unittest import mock
import contextlib
import io

from scripts import wm_quiet_run_state as sut
from scripts import writer_fence_restore as restore_sut
from checks import writer_fence_evidence as fence_authority


class QuietRunStateTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.root = Path(self.tmp.name)
        self.ledger = self.root / "state.jsonl"; self.fence_id = "fixture-fence"
        self.quiet_patch = mock.patch.object(
            sut, "observe_quiescence", return_value={"verdict": "QUIESCENT"})
        self.fence_patch = mock.patch.object(sut, "observe_fence",
                                             side_effect=self.observed_fence)
        self.click_status = None
        self.click_patch = mock.patch.object(
            sut, "serving_click_status", side_effect=lambda: self.click_status)
        self.certificate_patch = mock.patch.object(
            sut, "produce_certificate",
            side_effect=lambda args, context: (args.evidence,
                                                sut.edn_file(args.evidence)))
        self.quiet_patch.start(); self.fence_patch.start()
        self.click_patch.start(); self.certificate_patch.start()
        self.assertEqual(0, sut.main(["init", "--ledger", str(self.ledger),
                                      "--fence-id", self.fence_id]))

    def tearDown(self):
        self.certificate_patch.stop(); self.click_patch.stop()
        self.fence_patch.stop(); self.quiet_patch.stop(); self.tmp.cleanup()

    def observed_fence(self, fence_id, attestations):
        current = dt.datetime.now(dt.timezone.utc)
        return {"verdict": "FENCE-VERIFIABLE", "fence-id": fence_id,
                "observation-interval": {"started-at": current.isoformat(),
                                         "finished-at": current.isoformat()}}

    def write(self, name, value):
        path = self.root / name
        path.write_text(json.dumps(value) + "\n")
        return str(path)

    def advance(self, state, evidence=None, *extra):
        argv = ["advance", "--ledger", str(self.ledger), "--to", state]
        if evidence: argv += ["--evidence", evidence]
        argv += list(extra)
        return sut.main(argv)

    def fence_pair(self, observed=None):
        current = dt.datetime.now(dt.timezone.utc)
        observed = observed or current
        fence = self.write("fence.json", {"verdict": "FENCE-VERIFIABLE",
            "fence-id": self.fence_id,
            "observation-interval": {"started-at": observed.isoformat(),
                                     "finished-at": observed.isoformat()}})
        att = self.write("att.json", {"fence-id": self.fence_id,
            "expires-at": (current + dt.timedelta(minutes=30)).isoformat()})
        return fence, att

    def bounded(self, name, started=None, finished=None):
        current = dt.datetime.now(dt.timezone.utc)
        basis = {"head": "tested", "dirty": False}
        command = ("make workspace-gate" if "gate" in name else
                   "clojure -T:build ci" if "futon2" in name else "clojure -X:test")
        return self.write(name, {"outer-exit": 0, "verdict": "pass", "command": command,
            "resource-status": "clean", "repository-basis-stable": True,
            "repository-basis-start": basis, "repository-basis-finish": basis,
            "started-at": (started or current).isoformat(),
            "finished-at": (finished or current).isoformat()})

    def bounded_records(self, heads=None, agent=None):
        heads = heads or {"gate": "tested", "futon2": "tested", "futon3": "futon3-tested"}
        records = {}
        for name in ("gate", "futon2", "futon3"):
            path = self.bounded(name + ".json")
            receipt = json.loads(Path(path).read_text())
            receipt["repository-basis-start"]["head"] = heads[name]
            receipt["repository-basis-finish"]["head"] = heads[name]
            Path(path).write_text(json.dumps(receipt) + "\n")
            job_id = "job-" + name
            records[job_id] = {"id": job_id, "unit": job_id + ".service",
                "agent-id": agent or self.fence_id, "receipt-file": path,
                "receipt": receipt, "systemd": {"ActiveState": "inactive",
                    "ExecMainStartTimestampMonotonic": "12345"}}
        return records

    def terminal_receipt(self, click_id, run_id, outcome="grounded-no-change"):
        run = self.root / (run_id + ".edn")
        run.write_text('{:run/id "' + run_id + '" :click/id "' + click_id + '"}\n')
        binding = self.root / (click_id + "-binding.edn")
        binding.write_text('{:click/id "' + click_id +
                           '" :run-id-observation {:status :present :value "' +
                           run_id + '"}}\n')
        self.click_status = {"running?": False, "click-id": click_id,
            "last-result": {"click-id": click_id, "outcome": outcome,
                "binding-status": "verified", "run-record-status": "present",
                "run-id-observation": {"status": "present", "value": run_id},
                "run-record": str(run), "run-binding": str(binding)}}
        return self.write("terminal.json", {"schema": "wm-click-resource-v1",
            "click-id": click_id, "run-id": run_id, "terminal-outcome": outcome,
            "resource-status": "clean",
            "started-at": dt.datetime.now(dt.timezone.utc).isoformat()})

    def reach_fence(self):
        quiet = self.write("quiet.json", {"verdict": "QUIESCENT"})
        self.assertEqual(0, self.advance("quiescence", quiet))
        fence, att = self.fence_pair()
        self.assertEqual(0, self.advance("fence-held", fence,
                                         "--attestations", att))
        return fence, att

    def reach_tested(self):
        fence, att = self.reach_fence()
        records = self.bounded_records()
        with mock.patch.object(sut, "bounded_job", side_effect=records.__getitem__):
            self.assertEqual(0, self.advance("tested-commit", None,
                "--fence-evidence", fence, "--attestations", att,
                "--job-id", "job-gate", "--job-id", "job-futon2",
                "--job-id", "job-futon3"))

    def restoration_artifacts(self, incomplete=False):
        key = self.root / "restore.key"; key.write_bytes(b"fixture secret at least thirty two bytes"); key.chmod(0o600)
        targets = {
            restore_sut.TERMINAL_ID: {"kind": "coordinator", "class": "terminal-watchdog",
                "pre-state": {"durable-status": "complete",
                              "runtime-scheduler-present?": False,
                              "watchdog-scheduler-present?": True}}}
        for identity in restore_sut.RUNNING_IDS:
            targets[identity] = {"kind": "coordinator", "class": "running-coordinator",
                "pre-state": {"durable-status": "running", "enabled?": True}}
        for index, identity in enumerate(restore_sut.UNITS):
            targets[identity] = {"kind": "unit", "class": "systemd-unit",
                "pre-state": {"ActiveState": "active" if index == 0 else "inactive"}}
        body = {"schema": restore_sut.SCHEMA, "fence-id": self.fence_id,
                "captured-at": "fixture", "targets": targets}
        manifest_value = dict(body, **{"manifest-hmac-sha256":
                                       restore_sut.authenticate(body, key.read_bytes())})
        manifest = self.write("manifest.json", manifest_value)
        specs = [("rearm-terminal-coordinator", restore_sut.TERMINAL_ID)] + [
            ("resume-coordinator", identity) for identity in restore_sut.RUNNING_IDS] + [
            ("start-unit", restore_sut.UNITS[0])]
        if incomplete: specs = specs[:1]
        rows = [{"schema": restore_sut.SCHEMA, "fence-id": self.fence_id,
                 "manifest-hmac-sha256": manifest_value["manifest-hmac-sha256"],
                 "ordinal": i, "action": action, "target": target}
                for i, (action, target) in enumerate(specs, 1)]
        journal = self.root / "journal.jsonl"
        journal.write_text("".join(json.dumps(x) + "\n" for x in rows))
        outcome_rows = [dict(row, status="restored") for row in reversed(rows)]
        outcomes = self.root / "outcomes.jsonl"
        outcomes.write_text("".join(json.dumps(x) + "\n" for x in outcome_rows))
        result = self.write("restore.json", {"ok": True})
        return result, manifest, str(journal), str(outcomes), str(key)

    def test_full_chain_and_release_only_after_restoration(self):
        self.reach_tested()
        ready = self.write("ready.json", {"readiness": "READY", "checks": [
            {"name": "serving-runner-code", "pass": True}]})
        self.assertEqual(0, self.advance("reload-recorded", ready))
        terminal = self.terminal_receipt("click-1", "run-1")
        self.assertEqual(0, self.advance("click-issued", terminal))
        self.assertEqual(0, self.advance("click-terminal", terminal))
        cert = self.root / "cert.edn"; cert.write_text(
            '{:verdict :pass :run {:id "run-1"}}\n')
        self.assertEqual(0, self.advance("certified", str(cert)))
        result, manifest, journal, outcomes, key = self.restoration_artifacts()
        backend = mock.Mock()
        backend.observe.side_effect = lambda identity, entry: entry["pre-state"]
        with mock.patch.object(restore_sut, "LiveBackend", return_value=backend):
            self.assertEqual(0, self.advance("restored", result, "--manifest", manifest,
                "--journal", journal, "--outcomes", outcomes, "--key-file", key))
        self.assertEqual(0, self.advance("released"))
        self.assertEqual("released", sut.load_ledger(self.ledger)[-1]["state"])

    def test_skip_and_early_release_refuse(self):
        evidence = self.write("quiet.json", {"verdict": "QUIESCENT"})
        self.assertEqual(1, self.advance("fence-held", evidence, "--attestations", evidence))
        self.assertEqual(1, self.advance("released"))

    def test_parking_request_is_derived_from_enforced_population(self):
        request = sut.parking_request(self.fence_id)
        spec = request["specification"]
        ids = {row["id"] for row in spec["writers"]}
        self.assertEqual(set(fence_authority.COORDINATORS + fence_authority.UNITS), ids)
        self.assertEqual(set(restore_sut.COORDINATORS + restore_sut.UNITS), ids)
        self.assertEqual(3, spec["coordinator-count"])
        self.assertEqual(8, spec["systemd-unit-count"])
        self.assertNotIn("five background units", request["request"])
        initial = sut.load_ledger(self.ledger)[0]
        self.assertEqual(sut.digest(spec),
                         initial["facts"]["parking-specification-sha256"])

    def test_stale_fence_receipt_cannot_gate_test(self):
        old = dt.datetime.now(dt.timezone.utc) - dt.timedelta(seconds=301)
        quiet = self.write("quiet.json", {"verdict": "QUIESCENT"})
        self.assertEqual(0, self.advance("quiescence", quiet))
        # The initial fence transition itself refuses the stale receipt.
        fence, att = self.fence_pair(old)
        stale = {"verdict": "FENCE-VERIFIABLE", "fence-id": self.fence_id,
                 "observation-interval": {"started-at": old.isoformat(),
                                          "finished-at": old.isoformat()}}
        with mock.patch.object(sut, "observe_fence", return_value=stale):
            self.assertEqual(1, self.advance("fence-held", fence, "--attestations", att))

    def test_fence_that_ages_before_gate_start_is_refused(self):
        fence, att = self.reach_fence()
        records = self.bounded_records()
        future = dt.datetime.now(dt.timezone.utc) + dt.timedelta(seconds=301)
        with mock.patch.object(sut, "bounded_job", side_effect=records.__getitem__), \
             mock.patch.object(sut, "now", return_value=future):
            self.assertEqual(1, self.advance("tested-commit", None,
                "--fence-evidence", fence, "--attestations", att,
                "--job-id", "job-gate", "--job-id", "job-futon2",
                "--job-id", "job-futon3"))

    def test_restoration_requires_every_changed_target(self):
        self.reach_tested()
        ready = self.write("ready.json", {"readiness": "READY", "checks": [
            {"name": "serving-runner-code", "pass": True}]})
        self.advance("reload-recorded", ready)
        terminal = self.terminal_receipt("c", "r")
        self.advance("click-issued", terminal)
        self.advance("click-terminal", terminal)
        cert = self.root / "cert.edn"; cert.write_text(
            '{:verdict :pass :run {:id "r"}}\n')
        self.advance("certified", str(cert))
        result, manifest, journal, outcomes, key = self.restoration_artifacts(incomplete=True)
        self.assertEqual(1, self.advance("restored", result, "--manifest", manifest,
            "--journal", journal, "--outcomes", outcomes, "--key-file", key))

    def test_synthesized_midchain_ledger_refuses(self):
        body = {"schema": sut.SCHEMA, "previous-receipt-sha256": None,
                "state": "click-issued", "fence-id": self.fence_id,
                "transitioned-at": sut.now().isoformat(), "evidence": [],
                "facts": {"click-id": "forged"}}
        self.ledger.write_text(json.dumps(dict(body, **{"receipt-sha256": sut.digest(body)})) + "\n")
        with self.assertRaisesRegex(ValueError, "state-ledger-history-invalid"):
            sut.load_ledger(self.ledger)

    def test_recorded_evidence_hash_is_revalidated(self):
        evidence = self.write("quiet.json", {"verdict": "QUIESCENT"})
        self.assertEqual(0, self.advance("quiescence", evidence))
        recorded = sut.load_ledger(self.ledger)[-1]["evidence"][0]["path"]
        Path(recorded).write_text('{"verdict":"NOT-QUIESCENT"}\n')
        with self.assertRaisesRegex(ValueError, "state-ledger-evidence-changed"):
            sut.load_ledger(self.ledger)

    def test_changed_parking_specification_requires_reinitialization(self):
        changed = dict(sut.parking_specification(), writers=[])
        with mock.patch.object(sut, "parking_specification", return_value=changed):
            with self.assertRaisesRegex(
                    ValueError,
                    "parking-specification-changed:.*abort-or-re-initialize"):
                sut.load_ledger(self.ledger)

    def test_authority_limits_are_persisted_and_emitted_on_refusal(self):
        initial = sut.load_ledger(self.ledger)[0]
        authority = initial["facts"]["evidence-authority"]
        self.assertEqual("self-owned", authority["authority/type"])
        self.assertFalse(authority["independent-canonical-head?"])
        self.assertEqual(3, len(authority["history-limitations"]))
        attempt_limit = authority["cross-producer-limitations"][0]
        self.assertEqual("bounded-test-and-serving-run-share-one-attempt",
                         attempt_limit["claim"])
        self.assertEqual("unprovable", attempt_limit["status"])
        self.assertTrue(attempt_limit["label-equality-enforced?"])
        self.assertEqual("consistency-not-independent-identity",
                         attempt_limit["label-equality-establishes"])
        self.assertEqual("independently-issued-verify-at-use-attempt-capability",
                         attempt_limit["clearing-condition"]["required"])
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(1, sut.main(["advance", "--ledger", str(self.ledger),
                                          "--to", "released"]))
        refused = json.loads(output.getvalue())
        self.assertEqual("authority-limit-not-pending-local-repair",
                         refused["evidence-authority"]["classification"])
        self.assertEqual("independent-canonical-head",
                         refused["evidence-authority"]["clearing-condition"]["required"])

    def test_handwritten_bounded_receipt_is_not_a_producer(self):
        fence, att = self.reach_fence()
        with mock.patch.object(sut, "bounded_job", side_effect=ValueError(
                "bounded-job-not-in-producer-registry")):
            self.assertEqual(1, self.advance("tested-commit", None,
                "--fence-evidence", fence, "--attestations", att,
                "--job-id", "made-up", "--job-id", "made-up-2",
                "--job-id", "made-up-3"))

    def test_stale_fence_cannot_be_freshened_by_gate_timestamp(self):
        fence, att = self.reach_fence()
        records = self.bounded_records()
        for record in records.values():
            record["receipt"]["started-at"] = dt.datetime.now(dt.timezone.utc).isoformat()
            Path(record["receipt-file"]).write_text(json.dumps(record["receipt"]) + "\n")
        future = dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=1)
        with mock.patch.object(sut, "bounded_job", side_effect=records.__getitem__), \
             mock.patch.object(sut, "now", return_value=future):
            self.assertEqual(1, self.advance("tested-commit", None,
                "--fence-evidence", fence, "--attestations", att,
                "--job-id", "job-gate", "--job-id", "job-futon2",
                "--job-id", "job-futon3"))

    def test_systemd_monotonic_presence_is_not_a_freshness_claim(self):
        fence, att = self.reach_fence()
        records = self.bounded_records()
        for record in records.values():
            record["systemd"].pop("ExecMainStartTimestampMonotonic", None)
        with mock.patch.object(sut, "bounded_job", side_effect=records.__getitem__):
            self.assertEqual(0, self.advance("tested-commit", None,
                "--fence-evidence", fence, "--attestations", att,
                "--job-id", "job-gate", "--job-id", "job-futon2",
                "--job-id", "job-futon3"))

    def test_handwritten_quiescence_evidence_is_not_consumed(self):
        fake = self.write("fake-quiet.json", {"verdict": "QUIESCENT"})
        with mock.patch.object(sut, "observe_quiescence",
                               side_effect=ValueError("quiescence-not-proven")):
            self.assertEqual(1, self.advance("quiescence", fake))

    def test_mismatched_gate_and_suite_commit_refuses(self):
        fence, att = self.reach_fence()
        records = self.bounded_records({"gate": "A", "futon2": "B", "futon3": "C"})
        with mock.patch.object(sut, "bounded_job", side_effect=records.__getitem__):
            self.assertEqual(1, self.advance("tested-commit", None,
                "--fence-evidence", fence, "--attestations", att,
                "--job-id", "job-gate", "--job-id", "job-futon2",
                "--job-id", "job-futon3"))

    def test_handwritten_post_test_chain_cannot_reach_certified(self):
        self.reach_tested()
        ready = self.write("ready.json", {"readiness": "READY", "checks": [
            {"name": "serving-runner-code", "pass": True}]})
        self.assertEqual(0, self.advance("reload-recorded", ready))
        fake = self.write("fake-click.json", {"schema": "wm-click-resource-v1",
            "click-id": "never-clicked", "run-id": "never-ran",
            "terminal-outcome": "grounded-no-change", "resource-status": "clean",
            "started-at": dt.datetime.now(dt.timezone.utc).isoformat()})
        self.click_status = {"running?": False, "click-id": None, "last-result": None}
        self.assertEqual(1, self.advance("click-issued", fake))
        self.assertEqual("reload-recorded", sut.load_ledger(self.ledger)[-1]["state"])

    def test_inactive_single_target_manifest_cannot_restore_vacuously(self):
        key = self.root / "empty.key"
        key.write_bytes(b"fixture secret at least thirty two bytes"); key.chmod(0o600)
        body = {"schema": restore_sut.SCHEMA, "fence-id": self.fence_id,
                "captured-at": "fixture", "targets": {
                    restore_sut.UNITS[0]: {"kind": "unit", "class": "systemd-unit",
                        "pre-state": {"ActiveState": "inactive"}}}}
        manifest_value = dict(body, **{"manifest-hmac-sha256":
                                       restore_sut.authenticate(body, key.read_bytes())})
        manifest = self.write("inactive-manifest.json", manifest_value)
        journal = self.write("empty-journal.jsonl", {})
        Path(journal).write_text("")
        outcomes = self.write("empty-outcomes.jsonl", {})
        Path(outcomes).write_text("")
        result = self.write("empty-restore.json", {"ok": True})
        args = type("Args", (), {"evidence": result, "manifest": manifest,
            "journal": journal, "outcomes": outcomes, "key_file": str(key)})()
        with self.assertRaisesRegex(ValueError,
                                    "restoration-manifest-population-incomplete"):
            sut.evidence_restored(args, {"fence-id": self.fence_id})

    def test_complete_manifest_with_no_active_writers_is_not_restoration(self):
        key = self.root / "none.key"
        key.write_bytes(b"fixture secret at least thirty two bytes"); key.chmod(0o600)
        targets = {restore_sut.TERMINAL_ID: {"kind": "coordinator",
            "class": "terminal-watchdog", "pre-state": {
                "durable-status": "complete", "runtime-scheduler-present?": False,
                "watchdog-scheduler-present?": False}}}
        for identity in restore_sut.RUNNING_IDS:
            targets[identity] = {"kind": "coordinator", "class": "running-coordinator",
                "pre-state": {"durable-status": "stopped", "enabled?": False}}
        for identity in restore_sut.UNITS:
            targets[identity] = {"kind": "unit", "class": "systemd-unit",
                "pre-state": {"ActiveState": "inactive"}}
        body = {"schema": restore_sut.SCHEMA, "fence-id": self.fence_id,
                "captured-at": "fixture", "targets": targets}
        manifest_value = dict(body, **{"manifest-hmac-sha256":
                                       restore_sut.authenticate(body, key.read_bytes())})
        manifest = self.write("none-manifest.json", manifest_value)
        journal = self.root / "none-journal.jsonl"; journal.write_text("")
        outcomes = self.root / "none-outcomes.jsonl"; outcomes.write_text("")
        result = self.write("none-restore.json", {"ok": True})
        args = type("Args", (), {"evidence": result, "manifest": manifest,
            "journal": str(journal), "outcomes": str(outcomes), "key_file": str(key)})()
        with self.assertRaisesRegex(ValueError,
                                    "restoration-not-required:no-active-writers-recorded"):
            sut.evidence_restored(args, {"fence-id": self.fence_id})


if __name__ == "__main__": unittest.main()
