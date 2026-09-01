import datetime as dt
import json
from pathlib import Path
import tempfile
import unittest

from scripts import wm_quiet_run_state as sut
from scripts import writer_fence_restore as restore_sut
from checks import writer_fence_evidence as fence_authority


class QuietRunStateTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.root = Path(self.tmp.name)
        self.ledger = self.root / "state.jsonl"; self.fence_id = "fixture-fence"
        self.assertEqual(0, sut.main(["init", "--ledger", str(self.ledger),
                                      "--fence-id", self.fence_id]))

    def tearDown(self): self.tmp.cleanup()

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

    def reach_fence(self):
        quiet = self.write("quiet.json", {"verdict": "QUIESCENT"})
        self.assertEqual(0, self.advance("quiescence", quiet))
        fence, att = self.fence_pair()
        self.assertEqual(0, self.advance("fence-held", fence,
                                         "--attestations", att))
        return fence, att

    def reach_tested(self):
        fence, att = self.reach_fence()
        gate = self.bounded("gate.json")
        suites = [self.bounded("futon2.json"), self.bounded("futon3.json")]
        self.assertEqual(0, self.advance("tested-commit", gate,
            "--fence-evidence", fence, "--attestations", att,
            "--suite-receipt", suites[0], "--suite-receipt", suites[1]))

    def restoration_artifacts(self, incomplete=False):
        key = self.root / "restore.key"; key.write_bytes(b"fixture secret at least thirty two bytes"); key.chmod(0o600)
        body = {"schema": restore_sut.SCHEMA, "fence-id": self.fence_id,
                "captured-at": "fixture", "targets": {
                    restore_sut.TERMINAL_ID: {"kind": "coordinator", "class": "terminal-watchdog",
                        "pre-state": {"durable-status": "complete",
                                      "runtime-scheduler-present?": False,
                                      "watchdog-scheduler-present?": True}},
                    restore_sut.UNITS[0]: {"kind": "unit", "class": "systemd-unit",
                        "pre-state": {"ActiveState": "active"}},
                    restore_sut.UNITS[1]: {"kind": "unit", "class": "systemd-unit",
                        "pre-state": {"ActiveState": "inactive"}}}}
        manifest_value = dict(body, **{"manifest-hmac-sha256":
                                       restore_sut.authenticate(body, key.read_bytes())})
        manifest = self.write("manifest.json", manifest_value)
        specs = [("rearm-terminal-coordinator", restore_sut.TERMINAL_ID),
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
        issued = self.write("issued.json", {"click-id": "click-1",
                                             "started-at": dt.datetime.now(dt.timezone.utc).isoformat()})
        self.assertEqual(0, self.advance("click-issued", issued))
        terminal = self.write("terminal.json", {"schema": "wm-click-resource-v1",
            "click-id": "click-1", "run-id": "run-1", "terminal-outcome": "grounded-no-change",
            "resource-status": "clean"})
        self.assertEqual(0, self.advance("click-terminal", terminal))
        cert = self.root / "cert.edn"; cert.write_text('{:verdict :pass :run/id "run-1"}\n')
        self.assertEqual(0, self.advance("certified", str(cert)))
        result, manifest, journal, outcomes, key = self.restoration_artifacts()
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
        self.assertEqual(1, self.advance("fence-held", fence, "--attestations", att))

    def test_fence_that_ages_before_gate_start_is_refused(self):
        fence, att = self.reach_fence()
        old = dt.datetime.now(dt.timezone.utc) - dt.timedelta(seconds=301)
        self.write("fence.json", {"verdict": "FENCE-VERIFIABLE",
            "fence-id": self.fence_id,
            "observation-interval": {"started-at": old.isoformat(),
                                     "finished-at": old.isoformat()}})
        gate = self.bounded("gate.json")
        suites = [self.bounded("futon2.json"), self.bounded("futon3.json")]
        self.assertEqual(1, self.advance("tested-commit", gate,
            "--fence-evidence", fence, "--attestations", att,
            "--suite-receipt", suites[0], "--suite-receipt", suites[1]))

    def test_restoration_requires_every_changed_target(self):
        self.reach_tested()
        ready = self.write("ready.json", {"readiness": "READY", "checks": [
            {"name": "serving-runner-code", "pass": True}]})
        self.advance("reload-recorded", ready)
        issued = self.write("issued.json", {"click-id": "c", "started-at": dt.datetime.now(dt.timezone.utc).isoformat()})
        self.advance("click-issued", issued)
        terminal = self.write("terminal.json", {"schema": "wm-click-resource-v1", "click-id": "c",
            "run-id": "r", "terminal-outcome": "grounded-no-change", "resource-status": "clean"})
        self.advance("click-terminal", terminal)
        cert = self.root / "cert.edn"; cert.write_text('{:verdict :pass :run/id "r"}\n')
        self.advance("certified", str(cert))
        result, manifest, journal, outcomes, key = self.restoration_artifacts(incomplete=True)
        self.assertEqual(1, self.advance("restored", result, "--manifest", manifest,
            "--journal", journal, "--outcomes", outcomes, "--key-file", key))


if __name__ == "__main__": unittest.main()
