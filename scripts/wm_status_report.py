#!/usr/bin/env python3
"""Run and render the current War Machine status from its instruments."""

import datetime
import json
import os
import re
import subprocess
import sys
import time

ROOT = "/home/joe/code/futon2"
BG = "/home/joe/code/futon3c/scripts/bg.py"
CONTRACT = "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"


def run(argv, cwd=ROOT):
    return subprocess.run(argv, cwd=cwd, text=True, capture_output=True)


def edn_json(path, expression):
    form = ("(require '[clojure.edn :as edn] '[cheshire.core :as json]) "
            f"(let [x (edn/read-string (slurp {json.dumps(path)}))] "
            f"(println (json/generate-string {expression})))")
    result = run(["bb", "-e", form])
    return json.loads(result.stdout)


def launch_suite(command, directory, label):
    result = run(["python3", BG, "launch-test", command, "--agent", "wm-status-report",
                  "--label", label, "--dir", directory, "--window", "measurement"])
    payload = json.loads(result.stdout)
    return payload["value"]["id"]


def suite_status(job_id, timeout=360):
    deadline = time.time() + timeout
    while time.time() < deadline:
        payload = json.loads(run(["python3", BG, "test-status", job_id]).stdout)
        if payload.get("receipt"):
            log = open(payload["output-file"], encoding="utf-8", errors="replace").read()
            match = re.findall(r"Ran (\d+) tests containing (\d+) assertions\.\s*\n(\d+) failures, (\d+) errors", log)
            counts = match[-1] if match else (None, None, None, None)
            return payload, counts
        time.sleep(2)
    return {"receipt": {"verdict": "timeout", "outer-exit": 124}, "id": job_id}, (None,) * 4


def hole_population(strict_data):
    return {"count": strict_data["kinds"]["hole"],
            "classification": strict_data["hole-judgements"],
            "source": "contract_lint-live-report"}


def sorry_population(check_output):
    count_match = re.search(r":sorry-count\s+(\d+)", check_output)
    split_match = re.search(r":sorry-category-counts\s+\{([^}]*)\}", check_output)
    if not count_match or not split_match:
        raise ValueError("lean_sorry_category_check did not emit its census")
    split = {name: int(count) for name, count in
             re.findall(r'"([^"]+)"\s+(\d+)', split_match.group(1))}
    return {"count": int(count_match.group(1)), "classification": split,
            "source": "lean_sorry_category_check"}


def validate_population_sources(holes, sorries):
    return (holes.get("source") == "contract_lint-live-report" and
            sorries.get("source") == "lean_sorry_category_check")


def source_control():
    holes = {"count": 11, "classification": {"conformant": 7},
             "source": "contract_lint-live-report"}
    sorries = {"count": 6, "classification": {"refusal": 3, "attestation": 3},
               "source": "lean_sorry_category_check"}
    positive = validate_population_sources(holes, sorries)
    substitution_rejected = not validate_population_sources(holes, holes)
    passed = positive and substitution_rejected
    print("wm-status-source-control:", "PASS" if passed else "FAIL",
          f"positive={positive} substituted-hole-as-sorry-rejected={substitution_rejected}",
          "exit-convention=0-pass/1-fail")
    return 0 if passed else 1


def main():
    if "--source-control" in sys.argv[1:]:
        return source_control()
    timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
    stamp = str(int(time.time()))
    futon2_job = launch_suite("clojure -T:build ci", ROOT, f"status-{stamp}-futon2")
    futon3_job = launch_suite("clojure -X:test", "/home/joe/code/futon3", f"status-{stamp}-futon3")

    gate = run(["bb", "-cp", ".", "checks/wm_workspace_gate.clj"])
    gate_summary = next((line for line in gate.stdout.splitlines()
                         if line.startswith("wm-workspace-gate: SUMMARY")), "SUMMARY absent")
    gate_provenance = next((line for line in gate.stdout.splitlines()
                            if line.startswith("wm-workspace-gate: PROVENANCE")), "PROVENANCE absent")

    contract = json.load(open(CONTRACT, encoding="utf-8"))
    authority = contract["source"]["git-sha"]
    strict_path = f"/tmp/wm-status-strict-{stamp}.edn"
    strict = run(["bb", "-cp", ".", "checks/contract_lint.clj", "--strict",
                  "--contract", CONTRACT, "--registry", "checks/witness-registry.edn",
                  "--report", strict_path, "--authority", authority])
    strict_data = edn_json(strict_path,
        "{:summary (:summary x) :declaration-count (count (:declarations x)) "
        ":kinds (frequencies (map :kind (:declarations x))) "
        ":hole-judgements (frequencies (map :judgement (filter #(= \"hole\" (:kind %)) (:declarations x))))}")
    sorry_check = run(["bb", "-cp", ".", "checks/lean_sorry_category_check.clj"])
    holes = hole_population(strict_data)
    sorries = sorry_population(sorry_check.stdout)
    if not validate_population_sources(holes, sorries):
        raise RuntimeError("contract-hole and Lean-sorry populations lost source independence")

    absence = run(["bb", "-cp", ".", "checks/preemptive_absence_coercion_lint.clj"])
    absence_line = next((line for line in absence.stdout.splitlines()
                         if line.startswith("absence lint:")), "absence lint summary absent")
    dispositions = edn_json("checks/absence-coercion-dispositions.edn", "(:summary x)")

    obligations_path = f"/tmp/wm-status-obligations-{stamp}.edn"
    obligations = run(["bb", "checks/obligation_ledger_reconciliation_check.clj",
                       "--report", obligations_path])
    obligation_data = edn_json(obligations_path, "(:summary x)")

    lanes = run(["bb", "checks/lane_registry_check.clj"])
    lane_rows = [line for line in lanes.stdout.splitlines() if line.startswith("{:lane ")]
    lane_summary = lanes.stdout.splitlines()[-1] if lanes.stdout.splitlines() else "lane summary absent"

    futon2, f2_counts = suite_status(futon2_job)
    futon3, f3_counts = suite_status(futon3_job)

    qualification = strict_data["summary"]["qualification"]
    strict_counts = strict_data["summary"]["counts"]

    print(f"WAR MACHINE STATUS — {timestamp}")
    print("\nWORKSPACE GATE")
    print(f"verdict={'PASS' if gate.returncode == 0 else 'FAIL'} exit={gate.returncode}")
    print(gate_summary)
    print(gate_provenance)
    print("\nCONTRACT")
    print(f"declarations={strict_data['declaration-count']} closed={strict_data['kinds']['closed']} "
          f"holes={holes['count']} source={holes['source']}")
    print(f"hole-classification={json.dumps(holes['classification'], sort_keys=True)} source={holes['source']}")
    print(f":=sorry-terms={sorries['count']} sorry-classification={json.dumps(sorries['classification'], sort_keys=True)} "
          f"source={sorries['source']}")
    print(f"all-classifications={json.dumps(strict_counts, sort_keys=True)}")
    print("\nSTRICT LINT")
    print(f"verdict={'PASS' if strict.returncode == 0 else 'FAIL'} exit={strict.returncode} "
          f"stale={len(qualification['stale-declarations'])} "
          f"uninspectable={len(qualification['uninspectable-declarations'])} "
          f"bound-to-false={strict_counts.get('bound-to-false', 0)} "
          f"conformant={strict_counts.get('conformant', 0)}")
    print("\nABSENCE LINT")
    print(f"verdict={'PASS' if absence.returncode == 0 else 'FAIL'} exit={absence.returncode} {absence_line}")
    print(f"dispositions={json.dumps(dispositions, sort_keys=True)} "
          "source=HAND-MAINTAINED checks/absence-coercion-dispositions.edn; validated by running lint")
    print("\nSUITES (bounded substrate receipts)")
    for name, payload, counts in (("futon2", futon2, f2_counts), ("futon3", futon3, f3_counts)):
        receipt = payload["receipt"]
        print(f"{name}: tests={counts[0]} assertions={counts[1]} failures={counts[2]} errors={counts[3]} "
              f"resource-verdict={receipt.get('verdict')} outer-exit={receipt.get('outer-exit')} "
              f"pids-peak={receipt.get('pids-peak')} receipt={payload.get('receipt-file')}")
    print("\nOBLIGATIONS")
    print(f"verdict={'PASS' if obligations.returncode == 0 else 'FAIL'} exit={obligations.returncode} "
          f"current-table={json.dumps(obligation_data, sort_keys=True)}")
    print("\nLANES")
    print(f"verdict={'PASS' if lanes.returncode == 0 else 'FAIL'} exit={lanes.returncode} {lane_summary}")
    for row in lane_rows:
        print(row)

    component_red = [gate.returncode, strict.returncode, sorry_check.returncode, absence.returncode,
                     obligations.returncode, lanes.returncode,
                     futon2["receipt"].get("outer-exit", 1), futon3["receipt"].get("outer-exit", 1)]
    print("\nOVERALL")
    print("DEGRADED" if any(component_red) else "PASS")
    return 1 if any(component_red) else 0


if __name__ == "__main__":
    sys.exit(main())
