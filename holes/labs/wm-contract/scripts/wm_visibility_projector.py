#!/usr/bin/env python3
"""Project the ACTIVE wm-contract machinery cohort into voxterm's
wm/run-visibility-v1 JSON. Producer side of the voxterm contract:
voxterm reads one producer-owned file and never interprets campaign
records; this script owns that file.

Reads (never writes): the futon3c execution-cohort binding, the bound
preregistration (casting), and the attempt cell files under the bound
data root. Writes: <out-root>/run-visibility.json, atomically.

Stdlib only. Fail-closed: on any parse problem the projection is not
written (a stale file renders "stale" in voxterm, which is honest;
a wrong file would not be).
"""
import json
import os
import re
import sys
import tempfile
from datetime import datetime, timezone
from urllib.request import urlopen

BINDING = "/home/joe/code/futon3c/holes/labs/wm-contract/cohort-execution-binding.edn"
OUT_ROOT = "/home/joe/code/futon2/data/wm-visibility"


def rfc3339(ts):
    return datetime.fromtimestamp(ts, timezone.utc).isoformat(timespec="seconds")


def read_binding():
    text = open(BINDING, encoding="utf-8").read()
    root = re.search(r':data-root\s+"([^"]+)"', text)
    cohort = re.search(r':cohort-id\s+:([A-Za-z0-9-]+)', text)
    prereg = re.search(r':preregistration\s+"([^"]+)"', text)
    if not (root and cohort and prereg):
        raise ValueError("binding missing data-root/cohort-id/preregistration")
    return root.group(1), cohort.group(1), prereg.group(1)


def read_casting(prereg_path):
    text = open(prereg_path, encoding="utf-8").read()
    m = re.search(r':casting\s*\{([^}]*)\}', text)
    if not m:
        return None, None
    body = m.group(1)
    author = re.search(r':author\s+"([^"]+)"', body)
    reviewer = re.search(r':reviewer\s+"([^"]+)"', body)
    return (author.group(1) if author else None,
            reviewer.group(1) if reviewer else None)


def project_attempt(attempt_dir):
    # updated_at is stamped by the caller with THIS observation's time:
    # voxterm's staleness gauge then means "the projector is current",
    # and the UI's own caveat (age is not worker activity) covers the rest.
    close = os.path.join(attempt_dir, "007-closed.edn")
    if os.path.isfile(close):
        text = open(close, encoding="utf-8").read()
        if re.search(r':grounded\?\s+true', text):
            return "complete", "passed", None
        reason = None
        kind = re.search(r':failure-kind\s+:([a-z-]+)', text)
        sorry = re.search(r':sorry\s+\{:kind\s+:([a-z-]+)', text)
        if kind or sorry:
            reason = (kind or sorry).group(1)
        return "failed", "failed", reason
    return "working", "pending", None


def live_click():
    """The runner's own session report; a click id alone is not work
    evidence, but the runner's phase state for a named attempt is the
    runner reporting on itself. Unreachable endpoint = no overlay."""
    try:
        with urlopen("http://127.0.0.1:7070/api/alpha/wm/click",
                     timeout=2) as response:
            doc = json.load(response)
        if (isinstance(doc, dict) and doc.get("running?") is True
                and isinstance(doc.get("attempt-id"), str)
                and doc["attempt-id"]):
            return doc["attempt-id"]
    except Exception:
        pass
    return None


def main():
    data_root, cohort, prereg = read_binding()
    author, reviewer = read_casting(prereg)
    cohort_dir = os.path.join(data_root, cohort)
    attempts = sorted(n for n in os.listdir(cohort_dir)
                      if n.startswith("attempt-")
                      and os.path.isdir(os.path.join(cohort_dir, n)))
    now = rfc3339(datetime.now(timezone.utc).timestamp())
    trials = []
    for name in attempts:
        stage, result, reason = project_attempt(os.path.join(cohort_dir, name))
        trial = {"trial_id": cohort + "/" + name, "stage": stage,
                 "result": result, "updated_at": now}
        if author:
            trial["worker"] = author
        if reviewer:
            trial["reviewer"] = reviewer
        if reason:
            trial["blocked_reason"] = reason
        trials.append(trial)
    # Lead-maintained campaign state: when nothing is executing, the top
    # of the panel should say what the campaign is DOING (paused, planned
    # next step), not just echo the last trial's terminal state.
    state_path = os.path.join(OUT_ROOT, "campaign-state.json")
    campaign = None
    if os.path.isfile(state_path):
        try:
            campaign = json.load(open(state_path, encoding="utf-8"))
        except (OSError, ValueError):
            campaign = None
    running_attempt = live_click()
    if running_attempt and running_attempt not in attempts:
        # The click session has claimed an attempt whose first cell is not
        # on disk yet (tripwire sweep / early construction): show it as
        # working rather than showing nothing.
        trial = {"trial_id": cohort + "/" + running_attempt,
                 "stage": "working", "result": "pending", "updated_at": now}
        if author:
            trial["worker"] = author
        if reviewer:
            trial["reviewer"] = reviewer
        trials.append(trial)
    if not trials:
        # Schema requires a nonempty trials array; an armed cohort with no
        # attempt yet is projected as one planned trial.
        trials = [{"trial_id": cohort + "/awaiting-first-click",
                   "stage": "planned", "result": "pending",
                   "updated_at": now}]
    if (campaign and isinstance(campaign.get("planned_next"), str)
            and campaign["planned_next"]
            and not any(t["stage"] == "working" for t in trials)):
        trials.append({"trial_id": "planned/" + campaign["planned_next"],
                       "stage": "planned", "result": "pending",
                       "updated_at": now})
    last = trials[-1]
    stage, result = last["stage"], last["result"]
    reason = None
    if (campaign and campaign.get("paused") is True
            and not any(t["stage"] == "working" for t in trials)):
        stage, result = "blocked", "blocked"
        reason = str(campaign.get("reason") or "paused by the execution lead")
    doc = {"schema": "wm/run-visibility-v1", "run_id": cohort,
           "stage": stage, "result": result,
           "updated_at": now, "trials": trials}
    if reason:
        doc["blocked_reason"] = reason
    if author:
        doc["worker"] = author
    if reviewer:
        doc["reviewer"] = reviewer
    os.makedirs(OUT_ROOT, exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=OUT_ROOT, suffix=".tmp")
    with os.fdopen(fd, "w", encoding="utf-8") as handle:
        json.dump(doc, handle, indent=1)
        handle.write("\n")
    os.replace(tmp, os.path.join(OUT_ROOT, "run-visibility.json"))
    print("projected", cohort, len(trials), "trial(s)")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # fail closed, never write a guessed record
        print("refused:", exc, file=sys.stderr)
        sys.exit(1)
