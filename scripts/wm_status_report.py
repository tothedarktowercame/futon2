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
P4NG = "/home/joe/code/p4ng"
BG = "/home/joe/code/futon3c/scripts/bg.py"
CONTRACT = "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"
MATHLIB4 = "/home/joe/code/mathlib4"
HOLES_MODULE = "DarkTower/WarMachine/Holes.lean"
ACCEPTANCES = ROOT + "/checks/wm-status-accepted-red.json"
CONTRACT_DISPLAY = {"conformant": "shape-conformant",
                    "refused-implementation": "implementation-refused",
                    "witnessed": "binding-passed-shape-unchecked"}
LEAN_DISPLAY = {"DELIBERATE IMPLEMENTATION REFUSAL": "deliberate-implementation-refusal",
                "PERMANENT EXTERNAL ATTESTATION": "permanent-external-attestation",
                "WITNESSED-INSTANCE OBLIGATION": "witnessed-instance-obligation"}


def run(argv, cwd=ROOT):
    return subprocess.run(argv, cwd=cwd, text=True, capture_output=True)


def contract_pin(contract):
    module_commit = run(
        ["git", "log", "-1", "--format=%H", "--", HOLES_MODULE], cwd=MATHLIB4)
    module_sha = module_commit.stdout.strip() if module_commit.returncode == 0 else None
    json_sha = contract["source"]["git-sha"]
    return {"json-sha": json_sha, "module-last-commit-sha": module_sha,
            "fresh?": bool(module_sha) and json_sha == module_sha}


def receipt_text_values(text):
    """Extract the receipt/text agreement values exercised by the focused test."""
    contract_match = re.search(r"^declarations=(\d+) closed=(\d+) holes=(\d+) ", text,
                               re.MULTILINE)
    sorry_match = re.search(r"^:=sorry-terms=(\d+) ", text, re.MULTILINE)
    overall_match = re.search(r"^(\S+) exit=(\d+) convention=", text, re.MULTILINE)
    if not contract_match or not sorry_match or not overall_match:
        raise ValueError("status text omitted required receipt agreement fields")
    active_match = re.search(r"^active=(\d+) unused-or-superseded=", text, re.MULTILINE)
    accepted_red = int(active_match.group(1)) if active_match else 0
    return {"declaration-count": int(contract_match.group(1)),
            "closed": int(contract_match.group(2)),
            "hole": int(contract_match.group(3)),
            "sorry-count": int(sorry_match.group(1)),
            "overall-verdict": overall_match.group(1),
            "overall-exit": int(overall_match.group(2)),
            "red-components": (accepted_red +
                               len(re.findall(r"^NEW component=", text, re.MULTILINE)))}


def receipt_agrees_with_text(receipt, text):
    """True iff the focused authority values in RECEIPT equal the text report."""
    values = receipt_text_values(text)
    return values == {
        "declaration-count": receipt["contract"]["declaration-count"],
        "closed": receipt["contract"]["closed"],
        "hole": receipt["contract"]["hole"],
        "sorry-count": receipt["lean-sorry"]["count"],
        "overall-verdict": receipt["overall"]["verdict"],
        "overall-exit": receipt["overall"]["exit"],
        "red-components": sum(bool(component["red"])
                              for component in receipt["components"]),
    }


def edn_json(path, expression):
    form = ("(require '[clojure.edn :as edn] '[cheshire.core :as json]) "
            f"(let [x (edn/read-string (slurp {json.dumps(path)}))] "
            f"(println (json/generate-string {expression})))")
    result = run(["bb", "-e", form])
    return json.loads(result.stdout)


def pending_brief_state():
    """Read immutable Morning Brief state through its production reader."""
    expression = (
        "(require '[futon2.aif.morning-brief :as brief] '[cheshire.core :as json]) "
        "(println (json/generate-string "
        "(mapv #(assoc (select-keys % [:attempt-id :pending-objectives]) "
        ":belief-target? (string? (get-in % [:qa-targets :achievement :entity-id]))) "
        "(brief/pending-items))))")
    result = run(["bb", "-cp", "src:.", "-e", expression])
    try:
        rows = json.loads(result.stdout)
        valid = isinstance(rows, list) and all(
            isinstance(row.get("attempt-id"), str)
            and isinstance(row.get("pending-objectives"), list)
            and isinstance(row.get("belief-target?"), bool)
            for row in rows)
    except (json.JSONDecodeError, AttributeError, TypeError):
        rows, valid = [], False
    return result, rows, valid


def brief_decision_summary(rows):
    """Separate all due QA from the subset that blocks belief learning."""
    normalized = [{"attempt-id": row["attempt-id"],
                   "pending-objectives": sorted(row["pending-objectives"]),
                   "belief-target?": bool(row.get("belief-target?"))}
                  for row in rows if row.get("pending-objectives")]
    belief_blocked = [row["attempt-id"] for row in normalized
                      if (row["belief-target?"]
                          and "substantive-achievement" in row["pending-objectives"])]
    return {"count": len(normalized), "attempts": normalized,
            "belief-blocked-count": len(belief_blocked),
            "belief-blocked-attempt-ids": belief_blocked}


def overall_verdict(new_red, accepted, health_due, brief_due):
    """Decision debt is visible but never promoted to degradation."""
    if new_red:
        return "DEGRADED-NEW", 1
    if health_due or brief_due:
        return "DECISION-DUE", 3
    if accepted:
        return "DEGRADED-AS-EXPECTED", 0
    return "OK", 0


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
            "classification": {CONTRACT_DISPLAY.get(key, "contract-" + key): value
                               for key, value in strict_data["hole-judgements"].items()},
            "shape-unchecked": strict_data["shape-unchecked"],
            "source": "contract_lint-live-report"}


def sorry_population(check_output):
    count_match = re.search(r":sorry-count\s+(\d+)", check_output)
    split_match = re.search(r":sorry-category-counts\s+\{([^}]*)\}", check_output)
    if not count_match or not split_match:
        raise ValueError("lean_sorry_category_check did not emit its census")
    split = {LEAN_DISPLAY[name]: int(count) for name, count in
             re.findall(r'"([^"]+)"\s+(\d+)', split_match.group(1))}
    return {"count": int(count_match.group(1)), "classification": split,
            "source": "lean_sorry_category_check"}


def lean_label_population(check_output):
    match = re.search(r":category-counts\s+\{([^}]*)\}", check_output)
    if not match:
        raise ValueError("lean_sorry_category_check did not emit declaration labels")
    return {LEAN_DISPLAY[name]: int(count) for name, count in
            re.findall(r'"([^"]+)"\s+(\d+)', match.group(1))}


def validate_population_sources(holes, sorries):
    return (holes.get("source") == "contract_lint-live-report" and
            sorries.get("source") == "lean_sorry_category_check")


def vocabularies_disjoint(holes, lean_labels):
    return set(holes["classification"]).isdisjoint(lean_labels)


def retirement_window(receipts, configuration_hash, minimum=30):
    """Evaluate the committed rule from terminal, current-config production receipts."""
    scoped = [r for r in receipts
              if r.get("window-kind") == "production"
              and r.get("configuration", {}).get("configuration-hash") == configuration_hash]
    result = {"runs": len(scoped),
              "passes": sum(r.get("receipt", {}).get("outer-exit") == 0 for r in scoped),
              "containment-failures": sum(r.get("receipt", {}).get("reason") ==
                                          "resource-limit-failure" for r in scoped),
              "test-failures": sum(r.get("receipt", {}).get("reason") ==
                                    "test-failure" for r in scoped)}
    result.update({"eligible": len(scoped) >= minimum,
                   "retire": (len(scoped) >= minimum and
                              result["containment-failures"] > result["test-failures"])})
    return result


def classify_red(components, acceptances, now):
    """Return accepted/new findings. Acceptance matches are deliberately exact."""
    accepted, new, used = [], [], set()
    for finding in components:
        if not finding["red"]:
            continue
        match = next((a for a in acceptances
                      if a["component"] == finding["component"]
                      and a["signature"] == finding["signature"]), None)
        if match is None:
            new.append(dict(finding, reason="no exact active acceptance"))
            continue
        try:
            expires = datetime.datetime.fromisoformat(match["review-by"].replace("Z", "+00:00"))
        except (KeyError, ValueError):
            new.append(dict(finding, reason="acceptance has invalid review-by"))
            continue
        reference = os.path.join(ROOT, match.get("reference", ""))
        if expires <= now or not os.path.isfile(reference) or not match.get("reason") or not match.get("clears-when"):
            why = "acceptance expired" if expires <= now else "acceptance metadata/reference invalid"
            new.append(dict(finding, reason=why))
            continue
        used.add(match["id"])
        accepted.append({"finding": finding, "acceptance": match})
    stale = [a for a in acceptances if a["id"] not in used]
    return accepted, new, stale


def source_control():
    holes = {"count": 11, "classification": {"shape-conformant": 7},
             "source": "contract_lint-live-report"}
    sorries = {"count": 6, "classification": {"deliberate-implementation-refusal": 3,
                                                 "permanent-external-attestation": 3},
               "source": "lean_sorry_category_check"}
    positive = validate_population_sources(holes, sorries)
    substitution_rejected = not validate_population_sources(holes, holes)
    vocabulary_positive = vocabularies_disjoint(holes, sorries["classification"])
    colliding = dict(sorries["classification"], **{"shape-conformant": 1})
    collision_rejected = not vocabularies_disjoint(holes, colliding)
    now = datetime.datetime(2026, 8, 31, tzinfo=datetime.timezone.utc)
    sample = {"id": "control", "component": "accepted-control", "signature": {"count": 1},
              "reason": "control", "reference": "Makefile", "review-by": "2026-09-01T00:00:00Z",
              "clears-when": "control ends"}
    accepted, new, _ = classify_red(
        [{"component": "accepted-control", "red": True, "signature": {"count": 1}}], [sample], now)
    _, injected_new, _ = classify_red(
        [{"component": "injected-unaccepted", "red": True, "signature": {"count": 1}}], [sample], now)
    expired = dict(sample, id="expired", **{"review-by": "2026-08-30T00:00:00Z"})
    _, expired_new, _ = classify_red(
        [{"component": "accepted-control", "red": True, "signature": {"count": 1}}], [expired], now)
    policy_control = len(accepted) == 1 and not new and len(injected_new) == 1 and len(expired_new) == 1
    config = "control-configuration"
    receipts = ([{"window-kind": "production", "configuration": {"configuration-hash": config},
                  "receipt": {"outer-exit": 0}} for _ in range(27)] +
                [{"window-kind": "production", "configuration": {"configuration-hash": config},
                  "receipt": {"outer-exit": 125, "reason": "resource-limit-failure"}} for _ in range(2)] +
                [{"window-kind": "production", "configuration": {"configuration-hash": config},
                  "receipt": {"outer-exit": 125, "reason": "test-failure"}}])
    due = retirement_window(receipts, config)
    retirement_control = (due == {"runs": 30, "passes": 27, "containment-failures": 2,
                                  "test-failures": 1, "eligible": True, "retire": True})
    brief_due = brief_decision_summary([
        {"attempt-id": "pending-belief", "pending-objectives":
         ["feature-verdict", "substantive-achievement"], "belief-target?": True}])
    brief_answered = brief_decision_summary([])
    brief_control = (brief_due["count"] == 1
                     and brief_due["belief-blocked-attempt-ids"] == ["pending-belief"]
                     and brief_answered["count"] == 0
                     and brief_answered["belief-blocked-count"] == 0
                     and overall_verdict([], [], False, True) == ("DECISION-DUE", 3)
                     and overall_verdict([], [], False, False) == ("OK", 0))
    passed = (positive and substitution_rejected and vocabulary_positive and collision_rejected
              and policy_control and retirement_control and brief_control)
    print("wm-status-source-control:", "PASS" if passed else "FAIL",
          f"positive={positive} substituted-hole-as-sorry-rejected={substitution_rejected}",
          f"vocabularies-disjoint={vocabulary_positive} injected-collision-rejected={collision_rejected}",
          f"accepted-red={len(accepted)==1} injected-unaccepted-is-new={len(injected_new)==1} "
          f"expired-is-new={len(expired_new)==1}",
          f"receipt-window-eligible={due['eligible']} receipt-window-retire={due['retire']} "
          f"receipt-window-counts={due['runs']}/{due['passes']}/{due['test-failures']}/{due['containment-failures']}",
          f"brief-substantive-due={brief_due['belief-blocked-count']==1} "
          f"brief-fully-answered-clear={brief_answered['count']==0}",
          "exit-convention=0-pass/1-fail")
    return 0 if passed else 1


def main():
    if "--source-control" in sys.argv[1:]:
        return source_control()
    args = sys.argv[1:]
    receipt_path = None
    if args:
        if len(args) == 2 and args[0] == "--receipt":
            receipt_path = args[1]
        else:
            print("usage: wm_status_report.py [--receipt PATH]", file=sys.stderr)
            return 2
    now = datetime.datetime.now(datetime.timezone.utc)
    timestamp = now.isoformat()
    stamp = str(int(time.time()))
    futon2_job = launch_suite("clojure -T:build ci", ROOT, f"status-{stamp}-futon2")
    futon3_job = launch_suite("clojure -X:test", "/home/joe/code/futon3", f"status-{stamp}-futon3")

    gate = run(["bb", "-cp", ".", "checks/wm_workspace_gate.clj"])
    gate_summary = next((line for line in gate.stdout.splitlines()
                         if line.startswith("wm-workspace-gate: SUMMARY")), "SUMMARY absent")
    gate_provenance = next((line for line in gate.stdout.splitlines()
                            if line.startswith("wm-workspace-gate: PROVENANCE")), "PROVENANCE absent")

    contract = json.load(open(CONTRACT, encoding="utf-8"))
    pin = contract_pin(contract)
    authority = contract["source"]["git-sha"]
    strict_path = f"/tmp/wm-status-strict-{stamp}.edn"
    strict = run(["bb", "-cp", ".", "checks/contract_lint.clj", "--strict",
                  "--contract", CONTRACT, "--registry", "checks/witness-registry.edn",
                  "--report", strict_path, "--authority", authority])
    strict_data = edn_json(strict_path,
        "{:summary (:summary x) :declaration-count (count (:declarations x)) "
        ":kinds (frequencies (map :kind (:declarations x))) "
        ":shape-unchecked (mapv :name (filter #(= :witnessed (:judgement %)) (:declarations x))) "
        ":hole-judgements (frequencies (map :judgement (filter #(= \"hole\" (:kind %)) (:declarations x))))}")
    sorry_check = run(["bb", "-cp", ".", "checks/lean_sorry_category_check.clj"])
    holes = hole_population(strict_data)
    sorries = sorry_population(sorry_check.stdout)
    lean_labels = lean_label_population(sorry_check.stdout)
    if not validate_population_sources(holes, sorries):
        raise RuntimeError("contract-hole and Lean-sorry populations lost source independence")
    if not vocabularies_disjoint(holes, lean_labels):
        raise RuntimeError("contract and Lean display vocabularies collide")

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
    lane_error_kinds = sorted(re.findall(r':error\s+(:[\w-]+)', lanes.stdout + lanes.stderr))

    drift = run(["python3", "detect_drift.py"], cwd=P4NG)
    drift_count = sum(int(n) for n in re.findall(r'^(?:DRIFTED|VANISHED|NEW|NO LONGER CITED) \((\d+)\)',
                                                  drift.stdout, re.MULTILINE))

    health_result = run(["python3", BG, "test-health"])
    try:
        health = json.loads(health_result.stdout)
        health_window = health["current-window"]
        health_valid = all(key in health_window for key in
                           ("runs", "passes", "test-failures", "containment-failures", "eligible", "retire"))
    except (json.JSONDecodeError, KeyError, TypeError):
        health, health_window, health_valid = {}, {}, False

    brief_result, brief_rows, brief_valid = pending_brief_state()
    brief_summary = brief_decision_summary(brief_rows) if brief_valid else {
        "count": 0, "attempts": [], "belief-blocked-count": 0,
        "belief-blocked-attempt-ids": []}

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
    print(f"contract-hole-judgement={json.dumps(holes['classification'], sort_keys=True)} source={holes['source']}")
    print(f"binding-passed-shape-unchecked-declarations={json.dumps(holes['shape-unchecked'])}")
    print(f":=sorry-terms={sorries['count']} sorry-classification={json.dumps(sorries['classification'], sort_keys=True)} "
          f"source={sorries['source']}")
    print(f"lean-declaration-labels={json.dumps(lean_labels, sort_keys=True)} source={sorries['source']}")
    print(f"all-classifications={json.dumps(strict_counts, sort_keys=True)}")
    print(f"contract-pin={json.dumps(pin, sort_keys=True)}")
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

    print("\nREFERENT DRIFT")
    print(f"verdict={'PASS' if drift.returncode == 0 else 'FAIL'} exit={drift.returncode} findings={drift_count} "
          "source=p4ng/detect_drift.py")

    print("\nBOUNDED TESTING RETIREMENT WINDOW")
    if health_valid:
        print(f"runs={health_window['runs']} passes={health_window['passes']} "
              f"test-failures={health_window['test-failures']} "
              f"containment-failures={health_window['containment-failures']} "
              f"eligible={str(health_window['eligible']).lower()} retire={str(health_window['retire']).lower()} "
              f"configuration={health.get('current-configuration', {}).get('configuration-hash')} "
              "source=bg.py/test-health")
        print("clears-when=Joe records keep/retire and begins a new evaluation window or changes configuration")
    else:
        print(f"UNREADABLE exit={health_result.returncode} source=bg.py/test-health")

    print("\nMORNING BRIEF DECISIONS")
    if brief_valid:
        brief_state = "DECISION-DUE" if brief_summary["count"] else "CLEAR"
        print(f"state={brief_state} pending-attempts={brief_summary['count']} "
              f"belief-blocked={brief_summary['belief-blocked-count']} "
              "source=data/wm-morning-brief/items+reviews")
        for row in brief_summary["attempts"]:
            cost = ("belief-learning-blocked"
                    if (row["belief-target?"]
                        and "substantive-achievement" in row["pending-objectives"])
                    else "audit-only")
            print(f"attempt={row['attempt-id']} outstanding={json.dumps(row['pending-objectives'])} "
                  f"cost={cost}")
    else:
        print(f"UNREADABLE exit={brief_result.returncode} "
              "source=data/wm-morning-brief/items+reviews")

    absence_findings = int(re.search(r'findings=\s*(\d+)', absence_line).group(1))
    absence_dispositions = sorted(set(re.findall(r':disposition\s+:([\w-]+)', absence.stdout)))
    components = [
        {"component": "workspace-gate", "red": gate.returncode != 0, "signature": {"exit": gate.returncode}},
        {"component": "strict-lint", "red": strict.returncode != 0, "signature": {"exit": strict.returncode}},
        {"component": "sorry-category", "red": sorry_check.returncode != 0, "signature": {"exit": sorry_check.returncode}},
        {"component": "absence-lint", "red": absence.returncode != 0,
         "signature": {"exit": absence.returncode, "findings": absence_findings,
                       "dispositions": absence_dispositions}},
        {"component": "obligations", "red": obligations.returncode != 0, "signature": {"exit": obligations.returncode}},
        {"component": "lane-registry", "red": lanes.returncode != 0,
         "signature": {"exit": lanes.returncode, "errors": lane_error_kinds}},
        {"component": "referent-drift", "red": drift.returncode != 0,
         "signature": {"exit": drift.returncode, "findings": drift_count}},
        {"component": "bounded-test-health", "red": health_result.returncode != 0 or not health_valid,
         "signature": {"exit": health_result.returncode, "readable": health_valid}},
        {"component": "morning-brief-state", "red": not brief_valid,
         "signature": {"exit": brief_result.returncode, "readable": brief_valid}},
        {"component": "futon2-suite", "red": futon2["receipt"].get("outer-exit", 1) != 0,
         "signature": {"exit": futon2["receipt"].get("outer-exit", 1)}},
        {"component": "futon3-suite", "red": futon3["receipt"].get("outer-exit", 1) != 0,
         "signature": {"exit": futon3["receipt"].get("outer-exit", 1)}}]
    acceptances = json.load(open(ACCEPTANCES, encoding="utf-8"))["acceptances"]
    accepted, new_red, stale_acceptances = classify_red(components, acceptances, now)

    print("\nACCEPTED RED (enumerated; exact signatures only)")
    for item in accepted:
        a = item["acceptance"]
        print(f"{a['id']}: component={a['component']} signature={json.dumps(a['signature'], sort_keys=True)} "
              f"reason={a['reason']} reference={a['reference']} review-by={a['review-by']} "
              f"clears-when={a['clears-when']}")
    print(f"active={len(accepted)} unused-or-superseded={len(stale_acceptances)}")
    for acceptance in stale_acceptances:
        print(f"UNUSED id={acceptance['id']} component={acceptance['component']} "
              f"review-by={acceptance['review-by']} clears-when={acceptance['clears-when']}")
    for finding in new_red:
        print(f"NEW component={finding['component']} signature={json.dumps(finding['signature'], sort_keys=True)} "
              f"reason={finding['reason']}")

    print("\nOVERALL")
    verdict, exit_code = overall_verdict(
        new_red, accepted,
        health_valid and health_window["eligible"],
        brief_valid and brief_summary["count"] > 0)
    print(f"{verdict} exit={exit_code} "
          "convention=OK-0/DEGRADED-AS-EXPECTED-0/DEGRADED-NEW-1/DECISION-DUE-3")
    if receipt_path:
        component_classification = {
            item["finding"]["component"]: "accepted" for item in accepted}
        component_classification.update(
            {item["component"]: "new-red" for item in new_red})
        annotated_components = [
            dict(component,
                 classification=(component_classification.get(component["component"], "green")))
            for component in components]
        suites = []
        for name, payload, counts in (("futon2", futon2, f2_counts),
                                      ("futon3", futon3, f3_counts)):
            suite_receipt = payload["receipt"]
            suites.append({"name": name, "tests": counts[0], "assertions": counts[1],
                           "failures": counts[2], "errors": counts[3],
                           "receipt": payload.get("receipt-file"),
                           "verdict": suite_receipt.get("verdict"),
                           "exit": suite_receipt.get("outer-exit"),
                           "pids-peak": suite_receipt.get("pids-peak")})
        receipt = {
            "timestamp": timestamp,
            "overall": {"verdict": verdict, "exit": exit_code},
            "components": annotated_components,
            "accepted-red": {"accepted": accepted,
                             "stale-acceptances": stale_acceptances,
                             "new-red": new_red},
            "contract": {"declaration-count": strict_data["declaration-count"],
                         "closed": strict_data["kinds"]["closed"],
                         "hole": holes["count"],
                         "hole-population": holes,
                         "all-classifications": strict_counts},
            "contract-pin": pin,
            "lean-sorry": dict(sorries, **{"declaration-labels": lean_labels}),
            "workspace-gate": {"verdict": "PASS" if gate.returncode == 0 else "FAIL",
                               "exit": gate.returncode, "summary": gate_summary,
                               "provenance": gate_provenance},
            "strict-lint": {"verdict": "PASS" if strict.returncode == 0 else "FAIL",
                            "exit": strict.returncode,
                            "stale": len(qualification["stale-declarations"]),
                            "uninspectable": len(qualification["uninspectable-declarations"]),
                            "bound-to-false": strict_counts.get("bound-to-false", 0),
                            "conformant": strict_counts.get("conformant", 0)},
            "absence-lint": {"verdict": "PASS" if absence.returncode == 0 else "FAIL",
                             "exit": absence.returncode, "summary": absence_line,
                             "findings": absence_findings,
                             "dispositions": dispositions},
            "obligations": {"verdict": "PASS" if obligations.returncode == 0 else "FAIL",
                            "exit": obligations.returncode, "current-table": obligation_data},
            "lanes": {"verdict": "PASS" if lanes.returncode == 0 else "FAIL",
                      "exit": lanes.returncode, "summary": lane_summary,
                      "rows": lane_rows, "errors": lane_error_kinds},
            "referent-drift": {"verdict": "PASS" if drift.returncode == 0 else "FAIL",
                               "exit": drift.returncode, "findings": drift_count,
                               "source": "p4ng/detect_drift.py"},
            "suites": suites,
            "retirement-window": (health_window if health_valid else
                                  {"unreadable": True, "exit": health_result.returncode}),
            "morning-brief": (brief_summary if brief_valid else
                              {"unreadable": True, "exit": brief_result.returncode}),
        }
        with open(receipt_path, "w", encoding="utf-8") as output:
            json.dump(receipt, output, sort_keys=True, indent=2)
            output.write("\n")
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
