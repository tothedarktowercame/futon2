#!/usr/bin/env python3
"""Run WM clicks one after another, repairing the apparatus between them
(Joe, 2026-09-29: "I'd like the run-repair cycles to start working reliably;
perhaps we could go back to the loop variant rather than relying on bells").

Modelled on apm-lean's solve-loop.py and topology-supervisor.sh: this process
fires each click, waits for it, judges it from files, and sends repair work as
an Agency job whose job id it waits on. No step waits on a bellback or a park.

  wm_click_repair_loop.py status
  wm_click_repair_loop.py run --clicks 18 [--ticks 20] \
    --owner OWNER --issuing-caller CALLER \
    --author AUTHOR --reviewer REVIEWER --repair-reviewer REPAIR-REVIEWER

Per click:
1. Reload into the serving JVM every click-path namespace (futon2/src,
   futon3c/src/futon3c/wm) whose file was committed after the last reload.
   The first reload uses the JVM's start time. A file with uncommitted edits is
   never reloaded: that would load someone's half-finished work.
2. Fire with scripts/wm_click.sh --run (it waits for seats and the
   single-flight boundary) and read the click's last-result.
3. The click CARRIED a tick when its run id appears in data/wm-trace. That is
   the count toward --ticks.
4. A click that selected nothing (abstained) gets a debugger page,
   data/wm-click-loop/debugger/<click-id>.md: every refused target and what it
   was refused on, and whether that changed since the previous abstention. The
   page goes to the repair seat, which debugs why nothing was selectable
   (read-only evaluation in the serving JVM allowed) and fixes the cause.
   When the named refusals change, or the unadmitted-universe count falls,
   the obstruction has moved and the repair count starts again.
   A failed click (build-failed, error, service-failed, ...) or a failed
   reload goes to the repair seat with the evidence. The repair is judged by
   the world: a commit tagged with the repair id, clj-kondo with no errors on
   the files it changed, and a clean reload. A seat that finds no apparatus
   defect says so and the loop clicks again.
5. The loop stops when the click budget or the tick target is reached, when
   --stall clicks in a row carry nothing without failing (the wall repeats),
   when --max-repairs repairs of one fault do not produce a carrying click,
   when a repair seat answers that an abstention is correct (a human decides),
   or when the stop file exists. Every stop bells --owner once.

State: data/wm-click-loop/ledger.jsonl (one line per event), loop.log beside it.
Stop file: data/wm-click-loop/STOP (touch it; the loop stops before the next click).

--continuous (Joe, 2026-11-01: "no more whole click trials, just continuous
improvement as needed as the machine runs"): every click's two-run Lean verdict
(<run record>.requirements.edn, written by wm_run_alert.py) is read and
recorded. A click that does not conform is an alert: it is appended to
data/wm-click-loop/alerts.jsonl as work for --owner, who is belled once with
the alert block, and the loop clicks again. No repair seat is dispatched. A
lawful abstention (the verdict conforms) is a decided click, not a defect.
Before every click after the first, click-path namespaces committed since the
last reload are reloaded, so a fix landed between clicks is what the next click
runs. The loop pauses, by stopping with a bell, only when the same requirements
fail on two consecutive clicks: no fix landed, and clicking on would repeat the
same alert. An absent or unreadable verdict is itself an alert.
"""
from __future__ import annotations

import argparse
import datetime
import fcntl
import hashlib
import json
import os
import re
import stat
import subprocess
import sys
import time
import urllib.request
import uuid
from pathlib import Path

CODE = Path.home() / "code"
F2 = CODE / "futon2"
F3C = CODE / "futon3c"
META_INJURY_ROOT = F2 / "data" / "wm-meta-injuries-live-2026-10-22"
STATE = F2 / "data" / "wm-click-loop"
LEDGER = STATE / "ledger.jsonl"
LOGFILE = STATE / "loop.log"
STOP_FILE = STATE / "STOP"
ALERTS = STATE / "alerts.jsonl"
# Standing signature rows the owner has acknowledged in writing, each with the
# reason and the open packet. They stay in every verdict and alert record; they
# are excluded only from the repeat comparison, so a known gap awaiting a
# decision cannot pause the loop while other defects are being found.
ACKNOWLEDGED = STATE / "acknowledged.json"
AGENCY = "http://localhost:7070"
SEND = F3C / "scripts" / "agency_send.py"
EVAL = F3C / "scripts" / "proof-eval.sh"
TERMINAL = {"done", "failed", "error", "cancelled", "timeout", "timed-out"}
# Outcomes that are the machine deciding, not the apparatus breaking. They are
# never sent to repair; a run of them without a tick is the stall stop.
DECIDED = {"ok", "grounded-change", "grounded-no-change", "artifact-only",
           "guardrail-refusal", "policy-nondiscrimination", "cohort-complete"}
# A seat or a shared service was unavailable: waiting fixes it, code does not.
# The loop pauses, and these count toward the stall stop like DECIDED ones.
WAIT = {"agent-unavailable", "substrate-unavailable"}
WAIT_SECONDS = 900
# A click that selected nothing is a defect to debug (Joe, 2026-09-29: "I would
# view abstained clicks as a problem to be fixed rather than a pattern to learn
# from ... write the trace into something like a Debugger and then debug it
# live"). The loop writes a debugger page and sends it to the repair seat.
DEBUG = {"abstained", "no-selection"}
DEBUGGER = STATE / "debugger"
# Canonical-name git checkouts currently selected by this rule:
# futon0, futon1, futon1a, futon1b, futon1bi, futon2, futon2a, futon3,
# futon3a, futon3b, futon3c, futon4, futon5, futon5a, futon6, futon7,
# and futon7a. Backups and descriptive clones do not match the name rule.
CANONICAL_FUTON_RE = re.compile(r"futon\d+[a-z]*")
CANONICAL_FUTON_ROOTS = [
    path for path in sorted(CODE.glob("futon*"))
    if path.is_dir() and (path / ".git").exists()
    and CANONICAL_FUTON_RE.fullmatch(path.name)
]
WRITE_ROOTS = list(dict.fromkeys(
    path.resolve() for path in
    [*CANONICAL_FUTON_ROOTS,
     F2 / "data", F2 / "evidence", F3C / "data",
     Path.home() / "worktrees" / "futon2" / "self-heal", Path("/tmp")]))
# No is_dir() filter here: the loop is long-running and a root created after
# start (self-heal appears with the first META injury) must still be walked.
# snapshot_write_roots skips a root that does not exist yet.
WRITE_PRUNED_DIRS = {".git", "target", ".cpcache", ".cache", "node_modules", ".lake"}
WRITE_HASH_LIMIT = 64 * 1024 * 1024
WRITE_STAMP_ROOT = F2 / "data" / "wm-write-stamps"
# The click's code: what a repair may change and what a reload must pick up.
CLICK_PATHS = [(F2, "src"), (F2, "scripts/futon2"), (F3C, "src/futon3c/wm")]
SOURCE_IDENTITY_INVENTORY = F2 / "src" / "futon2" / "aif" / "load_identity.clj"


def now() -> str:
    return datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def log(msg: str) -> None:
    line = f"[{now()}] {msg}"
    print(line, flush=True)
    with LOGFILE.open("a") as fh:
        fh.write(line + "\n")


def record(row: dict) -> None:
    with LEDGER.open("a") as fh:
        fh.write(json.dumps({"at": now(), **row}, ensure_ascii=False) + "\n")


def snapshot_write_roots() -> tuple[dict[str, tuple[int, int]], float]:
    """Metadata-only census of regular, non-symlink files in declared roots."""
    started = time.monotonic()
    snapshot = {}
    walked_roots = []
    # Shortest path first, so a parent root is walked before any root nested
    # in it, whatever order WRITE_ROOTS lists them in.
    for root in sorted(WRITE_ROOTS, key=lambda p: (len(Path(p).parts), str(p))):
        try:
            resolved_root = root.resolve()
            root_is_dir = resolved_root.is_dir()
        except (OSError, RuntimeError):
            continue
        if not root_is_dir:
            continue
        # The legacy data/evidence roots remain declared, but a canonical
        # checkout already walked their files. Avoid paying for them twice.
        if any(resolved_root == parent or resolved_root.is_relative_to(parent)
               for parent in walked_roots):
            continue
        walked_roots.append(resolved_root)
        for directory, dirnames, filenames in os.walk(
                resolved_root, topdown=True, onerror=lambda _error: None,
                followlinks=False):
            kept_dirs = []
            for name in dirnames:
                child = os.path.join(directory, name)
                try:
                    if name not in WRITE_PRUNED_DIRS and not os.path.islink(child):
                        kept_dirs.append(name)
                except (OSError, PermissionError):
                    pass
            dirnames[:] = kept_dirs
            for name in filenames:
                path = os.path.join(directory, name)
                try:
                    metadata = os.stat(path, follow_symlinks=False)
                    if not stat.S_ISREG(metadata.st_mode):
                        continue
                    # directory descends from an already resolved root;
                    # resolving every file repeats filesystem traversal and
                    # made the wider census prohibitively expensive.
                    snapshot[path] = (metadata.st_size, metadata.st_mtime_ns)
                except (FileNotFoundError, PermissionError, OSError):
                    continue
    return snapshot, time.monotonic() - started


def hash_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_write_stamps(click_id: str | None) -> tuple[dict[str, dict], int, Path | None]:
    """Read valid attribution stamps for one click, skipping malformed lines."""
    if not isinstance(click_id, str) or not click_id.strip():
        return {}, 0, None
    stamp_file = WRITE_STAMP_ROOT / f"{click_id}.jsonl"
    if not stamp_file.is_file():
        return {}, 0, stamp_file
    stamps = {}
    unreadable = 0
    try:
        lines = stamp_file.read_text().splitlines()
    except (OSError, UnicodeError):
        return {}, 1, stamp_file
    for line in lines:
        try:
            row = json.loads(line)
            if (not isinstance(row, dict)
                    or not isinstance(row.get("path"), str)
                    or not os.path.isabs(row["path"])
                    or not isinstance(row.get("writer"), str)
                    or not row["writer"].strip()
                    or row.get("click-id") != click_id):
                raise ValueError("invalid write stamp")
            stamps[row["path"]] = row
        except (json.JSONDecodeError, TypeError, ValueError):
            unreadable += 1
    return stamps, unreadable, stamp_file


def write_click_manifest(click: dict, started: str, ended: str,
                         before: dict[str, tuple[int, int]],
                         after: dict[str, tuple[int, int]],
                         snapshot_seconds: dict[str, float]) -> dict:
    stamps, stamp_lines_unreadable, stamp_file = read_write_stamps(click.get("click-id"))
    stamp_path = str(stamp_file.resolve()) if stamp_file and stamp_file.exists() else None
    entries = []
    for raw_path in sorted(set(before) | set(after)):
        old, new = before.get(raw_path), after.get(raw_path)
        if old == new:
            continue
        stamp = stamps.get(raw_path)
        stamped = stamp is not None or raw_path == stamp_path
        attribution = ({"attribution": "stamped",
                        "writer": stamp["writer"] if stamp else "write-stamp",
                        "run-id": stamp.get("run-id") if stamp else click.get("run-id")}
                       if stamped else {"attribution": "click-window/unattributed"})
        if new is None:
            entries.append({"path": raw_path, "change": "removed", **attribution})
            continue
        path = Path(raw_path)
        entry = {"path": raw_path, "size": new[0],
                 "change": "new" if old is None else "modified",
                 **attribution}
        try:
            if new[0] > WRITE_HASH_LIMIT:
                entry["hash"] = "skipped-large"
            else:
                entry["sha256"] = hash_file(path)
            current = path.stat()
            if (current.st_size, current.st_mtime_ns) != new:
                # a live writer is still appending: keep the entry, say so
                entry["unstable"] = True
        except FileNotFoundError:
            entry["change"] = "removed-after-click"
        entries.append(entry)
    stamped_count = sum(entry["attribution"] == "stamped" for entry in entries)
    unattributed_count = sum(
        entry["attribution"] == "click-window/unattributed" for entry in entries)
    manifest = {
        "schema": "wm-click-write-manifest-v1",
        "click": {"id": click.get("click-id"), "number": click.get("n"),
                  "run-id": click.get("run-id")},
        "started": started, "ended": ended,
        "roots": [str(root.resolve()) for root in WRITE_ROOTS],
        "snapshot_seconds": snapshot_seconds,
        "entries": entries,
        "stamped_count": stamped_count,
        "unattributed_count": unattributed_count,
        "stamp_lines_unreadable": stamp_lines_unreadable,
        "attribution_note": ("click-window/unattributed means the path changed while this "
                             "click ran and no writer claimed it; it does not prove that the "
                             "click was the writer. Never delete or move a listed path on "
                             "this evidence. stamped means a writer "
                             "claimed the path in the click's stamp file; any process that can "
                             "write that file can make the claim, so it names a writer, it "
                             "does not prove one."),
    }
    safe_id = re.sub(r"[^A-Za-z0-9._-]", "_",
                     str(click.get("click-id") or f"click-unknown-{click.get('n', 'unknown')}"))
    target = STATE / "write-manifests" / f"{safe_id}.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    encoded = (json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode()
    with target.open("xb") as output:
        output.write(encoded)
    summary = {"path": str(target.resolve()),
               "sha256": hashlib.sha256(encoded).hexdigest(),
               "count": len(entries), "stamped": stamped_count,
               "unattributed": unattributed_count}
    if unattributed_count > 0:
        log(f"click {click.get('click-id')} has {unattributed_count} unattributed "
            f"write(s) in {summary['path']}")
    return summary


def fire_with_write_manifest(n: int, args, since_epoch: int) -> dict:
    """Run one click unchanged, registering every declared-root window change."""
    started = now()
    try:
        before, before_seconds = snapshot_write_roots()
        snapshot_error = None
    except Exception as error:
        before, snapshot_error = {}, error
    click = fire(n, args, since_epoch)
    ended = now()
    try:
        if snapshot_error is not None:
            raise snapshot_error
        after, after_seconds = snapshot_write_roots()
        click["write_manifest"] = write_click_manifest(
            click, started, ended, before, after,
            {"before": before_seconds, "after": after_seconds})
    except Exception as error:
        click["write_manifest"] = {
            "status": "failed", "error": f"{type(error).__name__}: {error}"}
    return click


RUN_COST_PROJECTION = r'''
(require '[clojure.edn :as edn] '[cheshire.core :as json])
(let [path (System/getenv "WM_COST_RECORD_PATH")
      record (edn/read-string (slurp path))]
  (println (json/generate-string
            (select-keys record
                         [:registered-run/timing :registered-run/model-usage]))))
'''

USAGE_PROJECTION = r'''
(require '[cheshire.core :as json]
         '[futon2.aif.registered-run-telemetry :as telemetry])
(let [rows (json/parse-string (slurp *in*) true)]
  (println (json/generate-string
            (mapv #(telemetry/normalize-provider-usage (:usage %)) rows))))
'''

CLICK_ENVELOPE_PHASES = [
    "reset", "agency-budget-preflight", "source-giraffe", "heap-gc",
    "tripwire-preflight", "cast-single-flight-wait", "post-admission",
    "server-execution", "child-wait", "ownership-verification",
    "requirements-report-card",
]
PHASE_BOUNDARY_PREFIX = "WM_COST_PHASE "

def run_cost_projection(run_record: str | None) -> dict:
    """Read only the two cost-bearing run fields through Clojure's EDN reader.

    Live run records are large, single-line EDN. Python deliberately never
    reads or regex-parses that text; the subprocess returns one bounded JSON
    projection. An absent/unreadable record is typed by the receipt builder.
    """
    if not run_record or not Path(run_record).is_file():
        return {}
    env = os.environ.copy()
    env["WM_COST_RECORD_PATH"] = str(Path(run_record).resolve())
    result = subprocess.run(
        ["clojure", "-M", "-e", RUN_COST_PROJECTION], cwd=F2, env=env,
        capture_output=True, text=True, timeout=120)
    if result.returncode != 0:
        return {"projection-error": "run-record-cost-projection-failed"}
    try:
        value = json.loads(result.stdout)
        return value if isinstance(value, dict) else {
            "projection-error": "run-record-cost-projection-invalid"}
    except (TypeError, json.JSONDecodeError):
        return {"projection-error": "run-record-cost-projection-invalid"}


def _typed_missing(reason: str) -> dict:
    return {"status": "typed-missing", "reason": reason}


def indexed_seat_calls(click_id: str | None) -> list[dict]:
    """Project only durable seat identities belonging to this click."""
    if not click_id:
        return []
    path = STATE / "seat-calls.jsonl"
    if not path.is_file():
        return []
    calls = []
    for line in path.read_text(errors="replace").splitlines():
        try:
            row = json.loads(line)
        except json.JSONDecodeError:
            continue
        if not isinstance(row, dict) or row.get("click-id") != click_id:
            continue
        job_id = row.get("job-id")
        call = {key: row[key] for key in
                ("role", "phase", "seat", "session-id", "run-id", "parent-click-id")
                if key in row}
        if isinstance(job_id, str) and job_id:
            call.update({"job-id": job_id, "status": "identified",
                         "reason": "usage-not-yet-joined"})
        elif isinstance(job_id, dict) and job_id.get("status") == "typed-missing":
            call.update(_typed_missing(job_id.get("reason", "job-id-not-retained")))
        else:
            call.update(_typed_missing("job-id-not-retained"))
        calls.append(call)
    return calls


def normalize_provider_usage(rows: list[dict]) -> list[dict | None]:
    """Use the registered-run telemetry normalizer; provider rules stay in Clojure."""
    if not rows:
        return []
    result = subprocess.run(
        ["clojure", "-M", "-e", USAGE_PROJECTION], cwd=F2,
        input=json.dumps(rows), capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        return [None] * len(rows)
    try:
        values = json.loads(result.stdout)
        return values if isinstance(values, list) and len(values) == len(rows) else [None] * len(rows)
    except (TypeError, json.JSONDecodeError):
        return [None] * len(rows)


def join_indexed_usage(calls: list[dict]) -> list[dict]:
    """Fetch each indexed terminal job once, then normalize all usage in one batch."""
    terminal, positions = [], []
    joined = [dict(call) for call in calls]
    for index, call in enumerate(joined):
        job_id = call.get("job-id")
        if not isinstance(job_id, str) or not job_id:
            call.update(_typed_missing(call.get("reason", "job-id-not-retained")))
            continue
        try:
            response = get_json(f"/api/alpha/invoke/jobs/{job_id}", timeout=5)
            job = response.get("job") if isinstance(response, dict) else None
        except Exception:
            call.update(_typed_missing("fetch-failed"))
            continue
        if not isinstance(job, dict):
            call.update(_typed_missing("fetch-failed"))
            continue
        if job.get("state") not in {"done", "failed", "cancelled", "timed-out"}:
            call.update(_typed_missing("job-not-terminal"))
            continue
        terminal.append({"usage": job.get("usage")})
        positions.append(index)
    for index, usage in zip(positions, normalize_provider_usage(terminal)):
        call = joined[index]
        if isinstance(usage, dict):
            call.update({"status": "complete", "unit": "tokens"})
            call.pop("reason", None)
            call.update(usage)
        else:
            call.update(_typed_missing("provider-usage-absent"))
    return joined


def launcher_phase_timings(click: dict) -> dict[str, int]:
    """Read the launcher's fixed-prefix JSON boundaries from this click's log."""
    phases = {}
    log_path = click.get("log")
    if log_path and Path(log_path).is_file():
        for line in Path(log_path).read_text(errors="replace").splitlines():
            if not line.startswith(PHASE_BOUNDARY_PREFIX):
                continue
            try:
                boundary = json.loads(line[len(PHASE_BOUNDARY_PREFIX):])
            except json.JSONDecodeError:
                continue
            phase, duration = boundary.get("phase"), boundary.get("wall-ms")
            if (phase in CLICK_ENVELOPE_PHASES and isinstance(duration, int)
                    and duration >= 0 and phase not in phases):
                phases[phase] = duration
    reset_ms = click.get("reset-wall-ms")
    if isinstance(reset_ms, int) and reset_ms >= 0:
        phases["reset"] = reset_ms
    return phases


def cost_receipt(click: dict, total_ms: int) -> dict:
    projection = run_cost_projection(click.get("run-record"))
    timing = projection.get("registered-run/timing")
    usage = projection.get("registered-run/model-usage")
    timing = timing if isinstance(timing, dict) else {}
    usage = usage if isinstance(usage, dict) else {}

    phases = []
    phase_timings = timing.get("phase-timings-ms")
    if isinstance(phase_timings, dict):
        phases.extend({"phase": phase, "status": "complete", "wall-ms": duration,
                       "source": "run-record", "parent": "server-execution"}
                      for phase, duration in phase_timings.items()
                      if isinstance(duration, int) and duration >= 0)
    elif click.get("run-record"):
        phases.append({"phase": "ordinary-run", **_typed_missing(
            "run-record-phase-timings-unavailable")})
    else:
        phases.append({"phase": "meta-self-heal", **_typed_missing(
            "phase-duration-not-retrievable")})
    launcher = launcher_phase_timings(click)
    phases.extend(({"phase": phase, "status": "complete", "wall-ms": launcher[phase],
                    "source": "launcher-monotonic"}
                   if phase in launcher else
                   {"phase": phase, **_typed_missing("not-reached")})
                  for phase in CLICK_ENVELOPE_PHASES)

    debugger = {key: timing[key] for key in
                ("debugger-stopped-ms", "debugger-stopped-by-phase-ms",
                 "debugger-dwell-status", "debugger-dwell-errors")
                if key in timing}
    if not debugger:
        debugger = _typed_missing("run-record-debugger-dwell-unavailable")

    calls = []
    for job in usage.get("jobs", []) if isinstance(usage.get("jobs"), list) else []:
        if isinstance(job, dict):
            calls.append(job)
    run_calls = list(calls)
    calls.append({"role": "selection", "status": "not-applicable",
                  "reason": "in-process-non-model-selection"})
    direct_calls = join_indexed_usage(indexed_seat_calls(click.get("click-id")))
    calls.extend(direct_calls)

    numeric_totals = {key: usage[key] for key in
                      ("input-tokens", "output-tokens", "total-tokens",
                       "cached-input-tokens", "uncached-input-tokens")
                      if isinstance(usage.get(key), int) and usage[key] >= 0}
    for call in direct_calls:
        if call.get("status") == "complete":
            for key in ("input-tokens", "output-tokens", "total-tokens",
                        "cached-input-tokens", "uncached-input-tokens"):
                if isinstance(call.get(key), int):
                    numeric_totals[key] = numeric_totals.get(key, 0) + call[key]
    unresolved = (sum(call.get("status") != "complete" for call in direct_calls) +
                  sum(call.get("status") != "complete" for call in run_calls))
    cost_calls = len(direct_calls) + len(run_calls)
    all_complete = cost_calls > 0 and unresolved == 0
    totals = {"status": "complete" if all_complete else "partial", **numeric_totals}
    if unresolved:
        totals["missing-call-count"] = unresolved
    usage_status = usage.get("status")
    model_usage = {
        "status": "complete" if all_complete else "partial",
        "source-status": usage_status if isinstance(usage_status, str) else "typed-missing",
        "unit": "tokens", "calls": calls, "totals": totals,
    }
    if not usage:
        model_usage["source-reason"] = (projection.get("projection-error") or
                                        "run-record-model-usage-unavailable")
    elif usage.get("reason"):
        model_usage["source-reason"] = usage["reason"]

    run_id = click.get("run-id")
    return {
        "schema": "wm/click-cost-receipt-v1", "event": "cost-receipt",
        "click-id": click.get("click-id"), "click-number": click.get("n"),
        "run-id": ({"status": "present", "value": run_id} if run_id else
                   _typed_missing("run-id-unavailable")),
        "wall": {"status": "complete", "unit": "ms", "total-ms": total_ms,
                 "phases": phases, "debugger": debugger},
        "model-usage": model_usage,
    }


def cost_receipt_recorded(click: dict) -> bool:
    """Idempotence by click id, or by loop ordinal before admission minted one."""
    if not LEDGER.is_file():
        return False
    for line in LEDGER.read_text(errors="replace").splitlines():
        try:
            row = json.loads(line)
        except json.JSONDecodeError:
            continue
        if row.get("event") != "cost-receipt":
            continue
        if click.get("click-id") is not None:
            if row.get("click-id") == click["click-id"]:
                return True
        elif row.get("click-id") is None and row.get("click-number") == click.get("n"):
            return True
    return False


def record_cost_receipt(click: dict, total_ms: int) -> None:
    if not cost_receipt_recorded(click):
        record(cost_receipt(click, total_ms))


def _plain(value):
    """Normalize JSON strings and EDN-like keyword strings for comparison."""
    return value[1:] if isinstance(value, str) and value.startswith(":") else value


def _runtime_disposition(value):
    """Find a retained runtime disposition in a JSON last-result."""
    if not isinstance(value, dict):
        return None
    for key in ("runtime-disposition", "runtime_disposition", "disposition"):
        if isinstance(value.get(key), dict):
            return value[key]
    if any(key in value for key in ("route", "ordinary-selection-called?", "ordinary_selection_called")):
        return value
    for child in value.values():
        found = _runtime_disposition(child)
        if found:
            return found
    return None


def _edn_field(text: str, key: str):
    match = re.search(rf':{re.escape(key)}\s+(?:"([^"]*)"|:([^\s,}}\]]+)|(true|false)|(-?\d+))', text)
    if not match:
        return None
    quoted, keyword, boolean, integer = match.groups()
    if quoted is not None:
        return quoted
    if keyword is not None:
        return keyword
    if boolean is not None:
        return boolean == "true"
    return int(integer)


def _binding_disposition(click_id: str | None):
    if not click_id:
        return None
    safe = re.sub(r"[^A-Za-z0-9._-]", "_", click_id)
    path = F3C / "data" / "wm-click-run-bindings" / f"click-run-binding-{safe}.edn"
    try:
        text = path.read_text()
    except (OSError, UnicodeError):
        return None
    start = text.find(":runtime-disposition")
    if start < 0:
        return None
    body = _top_level_map(text, text.find("{", start))
    if body is None:
        return None
    return {key: _edn_field(body, key) for key in
            ("route", "status", "reason", "phase", "injury-id", "attempt",
             "ordinary-selection-called?")}


def _top_level_map(text: str, open_at: int):
    """The disposition map's own entries, with nested maps, vectors and strings
    blanked out, so a nested :status (e.g. :medic-write's) is never read as
    the disposition's.  None when the map is not closed."""
    if open_at < 0:
        return None
    out, depth, i, in_str = [], 0, open_at, False
    while i < len(text):
        ch = text[i]
        if in_str:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                in_str = False
            if depth == 1:
                out.append(ch)
        elif ch == '"':
            in_str = True
            if depth == 1:
                out.append(ch)
        elif ch in "{[(":
            depth += 1
            if depth == 1:
                out.append(ch)
            elif depth == 2:
                out.append(" nested ")
        elif ch in "}])":
            depth -= 1
            if depth == 0:
                out.append(ch)
                return "".join(out)
        elif depth == 1:
            out.append(ch)
        i += 1
    return None


def _latest_injury_transition(injury_id: str | None):
    if not injury_id:
        return None
    injury_dir = META_INJURY_ROOT / "meta-injury-ledger" / "injury" / injury_id.rsplit("/", 1)[-1]
    try:
        files = sorted(injury_dir.glob("*.edn"),
                       key=lambda path: int(path.stem) if path.stem.isdigit() else -1)
        text = files[-1].read_text() if files else ""
    except (OSError, UnicodeError):
        return None
    if not text:
        return None
    return {key: _edn_field(text, key) for key in
            ("state", "reason", "injury-id", "attempt")}


def machine_self_heal_failure(click: dict):
    """Return the retained failing disposition, or None without guessing."""
    disposition = (_runtime_disposition(click.get("last-result")) or
                   _binding_disposition(click.get("click-id")))
    if not disposition:
        return None
    disposition = {key: _plain(value) for key, value in disposition.items()}
    route = disposition.get("route")
    status = disposition.get("status")
    ordinary = disposition.get("ordinary-selection-called?",
                               disposition.get("ordinary_selection_called"))
    route_failed = (route == "dedicated-meta-self-heal" and ordinary is False and
                    status in {"refused", "held", "threw"})
    # A held out-of-band result carries :phase :open-out-of-band and no :route
    # (meta_self_heal_production open-out-of-band-route!).
    out_of_band_failed = status == "held" and "open-out-of-band" in (
        route, disposition.get("phase"))
    latest = _latest_injury_transition(disposition.get("injury-id"))
    abandoned = latest and _plain(latest.get("state")) == "abandoned"
    if not (route_failed or out_of_band_failed or abandoned):
        return None
    if abandoned:
        disposition.setdefault("reason", _plain(latest.get("reason")))
        disposition.setdefault("attempt", latest.get("attempt"))
        disposition["state"] = "abandoned"
    return disposition


def sh(*args, cwd=None, timeout=None, input=None) -> subprocess.CompletedProcess:
    return subprocess.run(list(map(str, args)), cwd=cwd, capture_output=True, text=True,
                          timeout=timeout, input=input)


def git(repo: Path, *args: str) -> str:
    return sh("git", "-C", repo, *args).stdout


def get_json(path: str, timeout: int = 30) -> dict:
    with urllib.request.urlopen(f"{AGENCY}{path}", timeout=timeout) as fh:
        return json.load(fh)


def post_json(path: str, timeout: int = 30) -> dict:
    req = urllib.request.Request(f"{AGENCY}{path}", method="POST", data=b"")
    with urllib.request.urlopen(req, timeout=timeout) as fh:
        return json.load(fh)


def reset_cast_sessions(args) -> None:
    """Start every click with fresh conversations for the explicit role cast.

    Stable wm-* identities preserve role/provenance while reset-session clears
    model conversation state.  Refuse to proceed when a seat is busy or cannot
    be reset: silently retaining an old context defeats this boundary.
    """
    seats = [args.author, args.reviewer, args.repair_reviewer]
    if len(set(seats)) != 3:
        raise RuntimeError(f"cast identities must be distinct: {seats}")
    agents = get_json("/api/alpha/agents").get("agents", {})
    for seat in seats:
        row = agents.get(seat)
        if not row:
            raise RuntimeError(f"cast seat is not registered: {seat}")
        if row.get("status") not in ("idle", "restored") or not row.get("invoke-ready?"):
            raise RuntimeError(f"cast seat is not idle and invoke-ready: {seat}")
    resets = {}
    for seat in seats:
        result = post_json(f"/api/alpha/agents/{seat}/reset-session")
        if not result.get("ok"):
            raise RuntimeError(f"cast session reset failed: {seat}: {result}")
        resets[seat] = result.get("old-session-id")
    record({"event": "cast-reset", "cast": seats,
            "old-session-ids": resets})
    log(f"cast reset: {' / '.join(seats)} now have fresh conversations")


def jvm_eval(form: str, timeout: int = 600) -> dict:
    r = sh(EVAL, "-", input=form, timeout=timeout, cwd=F3C)  # it reads futon3c/.admintoken
    out = r.stdout.strip().splitlines()
    return {"rc": r.returncode, "out": out[-1] if out else "", "err": r.stderr[-800:]}


# ---------------------------------------------------------------- reload

def jvm_start_epoch() -> int:
    r = jvm_eval("(quot (.getStartTime (java.lang.management.ManagementFactory/getRuntimeMXBean)) 1000)")
    m = re.search(r":value (\d+)", r["out"])
    if not m:
        raise RuntimeError(f"cannot read JVM start time: {r}")
    return int(m.group(1))


def ns_of(path: Path) -> tuple[str | None, set[str]]:
    text = path.read_text(errors="replace")
    m = re.search(r"\(ns\s+([^\s()]+)", text)
    if not m:
        return None, set()
    depth, end = 0, len(text)
    for j in range(m.start(), len(text)):
        depth += text[j] == "("
        depth -= text[j] == ")"
        if depth == 0:
            end = j
            break
    head = text[m.start():end]
    return m.group(1), set(re.findall(r"[\[\s(]([a-z][\w.\-]*\.[\w.\-]+)", head))


def changed_click_files(since_epoch: int) -> list[Path]:
    files: set[Path] = set()
    for repo, sub in CLICK_PATHS:
        # --first-parent -m: a merge commit lists the files it brought onto main.
        # Without it, a seat's commit authored before SINCE and merged after it
        # is invisible, and a dependent namespace reloads before its dependency
        # (21:45Z 2026-11-01: outer-select reloaded against the old outer-score).
        out = git(repo, "log", "--first-parent", "-m", f"--since=@{since_epoch}",
                  "--name-only", "--format=", "--", sub)
        for rel in out.split():
            p = repo / rel
            if p.suffix in (".clj", ".cljc") and p.exists():
                files.add(p)
    return sorted(files)


def required_sources_declaration(text: str) -> str:
    """Extract only the map literal assigned to load_identity/required-sources."""
    declaration = re.search(r"\(def\s+required-sources\b", text)
    if not declaration:
        return ""
    start = text.find("{", declaration.end())
    if start < 0:
        return ""
    depth, quoted, escaped = 0, False, False
    for end in range(start, len(text)):
        char = text[end]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[start:end + 1]
    return ""


def source_identity_inventory() -> dict[str, Path]:
    """Read the runner's declared source-identity inventory without loading Clojure.

    The inventory is the explicit click-path census maintained by
    load_identity/required-sources. Reload planning uses it for attribution,
    not as a guessed dependency graph: changed-click-file discovery remains
    the set eligible to reload.
    """
    try:
        text = SOURCE_IDENTITY_INVENTORY.read_text(errors="replace")
    except OSError:
        return {}
    entries = {}
    declaration = required_sources_declaration(text)
    for namespace, raw_path in re.findall(r"'([\w.\-]+)\s+\"([^\"]+\.clj)\"", declaration):
        path = Path(raw_path)
        if raw_path.startswith("/home/joe/code/futon2/"):
            path = F2 / raw_path.removeprefix("/home/joe/code/futon2/")
        elif raw_path.startswith("/home/joe/code/futon3c/"):
            path = F3C / raw_path.removeprefix("/home/joe/code/futon3c/")
        entries[namespace] = path
    return entries


def reload_plan(since_epoch: int) -> tuple[list[Path], list[str]]:
    """Return landed reload candidates and their declared identity names.

    A changed dependency remains reloadable even when full-loop-runner itself
    did not change. The second result is an attribution census; absence from
    it is deliberately not treated as a numeric zero or an eligibility veto.
    """
    files = changed_click_files(since_epoch)
    by_path = {path: namespace for namespace, path in source_identity_inventory().items()}
    return files, sorted(by_path[path] for path in files if path in by_path)


def dirty(path: Path) -> bool:
    repo = F2 if F2 in path.parents else F3C
    return bool(git(repo, "status", "--porcelain", "--", str(path.relative_to(repo))).strip())


def reload_order(files: list[Path]) -> list[tuple[str, Path]]:
    info = {}
    for f in files:
        ns, reqs = ns_of(f)
        if ns:
            info[ns] = (f, reqs)
    done, order = set(), []

    def visit(ns: str, stack: set[str]) -> None:
        if ns in done or ns in stack:
            return
        for dep in info[ns][1]:
            if dep in info and dep != ns:
                visit(dep, stack | {ns})
        done.add(ns)
        order.append((ns, info[ns][0]))

    for ns in sorted(info):
        visit(ns, set())
    return order


def click_running() -> bool:
    try:
        return bool(get_json("/api/alpha/wm/click").get("running?"))
    except Exception:
        return True


def reload_namespace(ns: str) -> dict:
    """Load a planned canonical namespace; callers own idle/dirty checks."""
    return jvm_eval(f"(try (require (quote {ns}) :reload) :reloaded "
                    f"(catch Throwable t (loop [t t acc []] (if t (recur (.getCause t) "
                    f"(conj acc (str (.getName (class t)) \": \" (.getMessage t)))) (str \"RELOAD-FAILED \" acc)))))")


def reload_changed(since_epoch: int, held_run_id: str | None = None) -> tuple[bool | None, str, int]:
    """Reload every click-path namespace committed since since_epoch. Returns
    (ok, failure-text, new-since). Reload only between clicks, or while the
    named run is demonstrably stopped at the restart debugger."""
    if click_running() and not (held_run_id and debugger_stop(held_run_id)):
        log("reload: deferred; a click is in flight")
        record({"event": "reload", "ok": None, "since": since_epoch,
                "reason": "click-in-flight", "namespaces": [],
                "source-identity-dependencies": []})
        return None, "click-in-flight", since_epoch
    started = int(time.time())
    files, identity_dependencies = reload_plan(since_epoch)
    skipped = [f for f in files if dirty(f)]
    order = [(ns, f) for ns, f in reload_order([f for f in files if f not in skipped])]
    for f in skipped:
        log(f"reload: SKIP {f} -- uncommitted edits in the working tree")
    for ns, f in order:
        r = reload_namespace(ns)
        if r["rc"] != 0 or ":reloaded" not in r["out"]:
            text = f"reload of {ns} ({f}) failed: {r['out'][:1500]} {r['err'][:500]}"
            log(text)
            record({"event": "reload", "ok": False, "ns": ns, "detail": text})
            return False, text, since_epoch
    if order:
        # The standing authority context is memoised in the serving JVM; a
        # reload of the medic/authority namespaces leaves the old verifier in
        # it, and the pre-ration gate then reads a valid ledger as tampered
        # (19:32Z: "self-heal is not serviceable" after the environmental
        # disposition landed). Reset it after every reload.
        r = jvm_eval("(do (futon2.aif.standing-authority/reset-context!) :standing-reset)")
        if ":standing-reset" not in r["out"]:
            text = f"standing authority context reset failed after reload: {r['out'][:500]} {r['err'][:300]}"
            log(text)
            record({"event": "reload", "ok": False, "detail": text})
            return False, text, since_epoch
    log(f"reload: {len(order)} namespace(s) since @{since_epoch}: {' '.join(ns for ns, _ in order) or '-'}")
    record({"event": "reload", "ok": True, "since": since_epoch,
            "namespaces": [ns for ns, _ in order], "skipped-dirty": [str(f) for f in skipped],
            "source-identity-dependencies": identity_dependencies})
    return True, "", started


def reload_when_idle(since_epoch: int, held_run_id: str | None = None) -> tuple[bool, str, int]:
    """Wait between clicks; never turn an active click into a reload attempt."""
    while True:
        ok, text, updated = reload_changed(since_epoch, held_run_id)
        if ok is not None:
            return ok, text, updated
        time.sleep(60)


# ---------------------------------------------------------------- clicks

def budget_lines() -> int:
    p = F2 / "data" / "wm-ordinary-clicks" / "consumption.jsonl"
    return sum(1 for _ in p.open()) if p.exists() else 0


def carried(run_id: str | None) -> bool:
    if not run_id:
        return False
    day = datetime.datetime.now(datetime.timezone.utc).date()
    for d in (day, day - datetime.timedelta(days=1)):
        f = F2 / "data" / "wm-trace" / f"wm-trace-{d.isoformat()}.edn"
        if f.exists() and sh("grep", "-qF", f'"{run_id}"', f).returncode == 0:
            return True
    return False


# ---------------------------------------------------------------- verdict

def read_verdict(run_record: str | None) -> dict | None:
    """The click's two-run Lean verdict as wm_run_alert.py wrote it beside the
    run record: string-keyed EDN, read by pattern and never defaulted. None
    when the record or the verdict file is absent; the caller treats that as
    an alert, not as conformance."""
    if not run_record:
        return None
    f = Path(str(run_record) + ".requirements.edn")
    if not f.is_file():
        return None
    text = f.read_text(errors="replace")

    def scalar(key):
        m = re.search(r'"%s"\s+(\S+)' % re.escape(key), text)
        return m.group(1).strip('"') if m else None  # keywords are written quoted

    def outcomes(key):
        m = re.search(r'"%s"\s+\{([^}]*)\}' % re.escape(key), text)
        return dict(re.findall(r'":(Q\d+)"\s+":([a-z-]+)"', m.group(1))) if m else {}

    return {"file": str(f), "conforms": scalar(":conforms") == "true",
            "work-status": scalar(":work-status"), "selection-status": scalar(":selection-status"),
            "work": outcomes(":verdicts"), "selection": outcomes(":selection-verdicts")}


def alert_signature(verdict: dict | None) -> list[str]:
    """Every requirement that did not hold, as 'run Qn outcome', sorted, plus a
    run that did not happen. Two clicks with the same signature failed the same
    way."""
    if verdict is None:
        return ["verdict absent"]
    rows = [f"{run} {q} {outcome}" for run in ("work", "selection")
            for q, outcome in verdict[run].items() if outcome != "holds"]
    if verdict["work-status"] == ":not-run":
        rows.append("work not-run")
    if verdict["selection-status"] == ":not-exercised":
        rows.append("selection not-exercised")
    return sorted(rows)


def click_record(lr: dict, click_id: str | None) -> str | None:
    """The record wm_click.sh checked: the run record named by last-result, or,
    for an outer refusal or abstention that wrote no tick, the one selection
    record carrying the click id (the same rule wm_click.sh uses for ownership).
    None when neither exists; the verdict is then absent, which is an alert."""
    rr = lr.get("run-record")
    if rr and Path(rr).is_file():
        return rr
    run_id = (lr.get("run-id-observation") or {}).get("value")
    if run_id and (F2 / "data" / "wm-runs" / f"tick-run-record-{run_id}.edn").is_file():
        return str(F2 / "data" / "wm-runs" / f"tick-run-record-{run_id}.edn")
    if click_id:
        found = sorted((F2 / "data" / "wm-runs").glob(f"selection-record-????-??-??-{click_id}.edn"))
        if len(found) == 1:
            return str(found[0])
    return rr


def selection_why(record_path: str | None) -> dict | None:
    """Rendered :why lines from the typed selection carrier.

    The loop deliberately carries only the deterministic rendered strings; the
    EDN record remains the authority for all typed fields.
    """
    if not record_path or not Path(record_path).is_file():
        return None
    text = Path(record_path).read_text(errors="replace")
    start = text.find(":why {:schema :wm/selection-why-v1")
    if start < 0:
        return None
    rendered = [json.loads('"' + value + '"') for value in
                re.findall(r':rendered "((?:\\.|[^"\\])*)"', text[start:])]
    if len(rendered) < 2:
        return None
    return {"classes": rendered[:-2], "abstention": rendered[-2],
            "decision": rendered[-1]}


def nothing_beats_stopping(record_path: str | None) -> str | None:
    """The outer selection refused because no compared policy's G is below the
    abstention's (:pick-not-below-abstention). Returns the selection's input
    digest, so an unchanged field can be recognised: re-firing on the same
    input is the repeated identical-input refusal Q6 forbids (clicks 12-14 of
    2026-11-01). None for any other record."""
    if not record_path or not Path(record_path).is_file():
        return None
    text = Path(record_path).read_text(errors="replace")
    if ":selection-outcome {:status :refused, :kind :pick-not-below-abstention" not in text:
        return None
    m = re.search(r':choice-input-digest \{:sha256 "([0-9a-f]{64})"', text)
    return m.group(1) if m else "digest-unretained"


def click_alert(verdict: dict | None) -> list[str] | None:
    """None when the click conforms; otherwise its alert signature."""
    return None if verdict and verdict["conforms"] else alert_signature(verdict)


def launch_failure_text(log_path) -> str:
    """Name the preflight's own reason for an exit-3 launch failure when the click
    log carries one (wm_click_prepare prints a launch-unavailable JSON line);
    otherwise the seat/single-flight wait is the only remaining cause."""
    generic = "wm_click.sh could not launch (exit 3: seats or single-flight held 30 min)"
    if not log_path:
        return generic
    try:
        for line in Path(log_path).read_text().splitlines():
            line = line.strip()
            if line.startswith("{") and '"launch-unavailable"' in line:
                try:
                    reason = json.loads(line).get("reason")
                except json.JSONDecodeError:
                    reason = None
                if reason:
                    return f"wm_click.sh could not launch (exit 3: preflight launch-unavailable: {reason})"
    except OSError:
        pass
    return generic


def acknowledged_rows() -> dict[str, dict]:
    """row -> {why, ref} from ACKNOWLEDGED; read at every click so an entry
    added while the loop runs counts from the next click. Malformed file: none
    acknowledged (the loop then pauses sooner, never later)."""
    try:
        rows = json.loads(ACKNOWLEDGED.read_text()) if ACKNOWLEDGED.is_file() else []
        return {r["row"]: r for r in rows if isinstance(r, dict) and r.get("row") and r.get("why")}
    except (ValueError, TypeError, KeyError):
        return {}


def unacknowledged(signature: list[str] | None, acknowledged: dict[str, dict]) -> list[str] | None:
    """The part of a signature that is not acknowledged; None when nothing is left."""
    if signature is None:
        return None
    rest = [row for row in signature if row not in acknowledged]
    return rest or None


def alert_block(log_path: str | None) -> str:
    """The ALERT FOR JOE / CONFORMS block wm_click.sh printed last, from the click log."""
    if not log_path or not Path(log_path).is_file():
        return "(click log absent)"
    lines = Path(log_path).read_text(errors="replace").splitlines()
    starts = [i for i, l in enumerate(lines) if l.startswith(("ALERT FOR JOE", "CONFORMS"))]
    return "\n".join(lines[starts[-1]:starts[-1] + 40]) if starts else "(no alert block in the click log)"


def queue_alert(c: dict, repeated: bool) -> None:
    row = {"at": now(), "click-id": c["click-id"], "run-id": c["run-id"], "run-record": c.get("run-record"),
           "signature": c["alert"], "verdict": c.get("verdict"), "repeated": repeated, "log": c.get("log")}
    with ALERTS.open("a") as fh:
        fh.write(json.dumps(row, ensure_ascii=False) + "\n")
    record({"event": "alert", "click-id": c["click-id"], "run-id": c["run-id"],
            "signature": c["alert"], "repeated": repeated})


def notify_alert(args, c: dict) -> None:
    """Bell the owner once per alert; the loop does not wait for an answer."""
    body = "\n".join([f"From wm_click_repair_loop.py, {now()}. ALERT on click {c['click-id']} "
                      f"(run {c['run-id']}); the loop continues.",
                      "", "Signature: " + "; ".join(c["alert"]), "",
                      "Selection why:", *((c.get("why") or {}).get("classes") or []),
                      ((c.get("why") or {}).get("decision") or "selection explanation absent"), "",
                      alert_block(c.get("log")), "",
                      f"Verdict file: {(c.get('verdict') or {}).get('file')}",
                      f"Queue: {ALERTS}",
                      "The loop pauses if the same signature repeats on the next click."])
    r = sh(sys.executable, SEND, "--from", "wm-click-loop", "--to", args.owner, "--kind", "bell",
           input=body, timeout=120)
    log(f"alert belled to {args.owner}: {r.stdout.strip()[-200:]}")


# Joe, 2026-11-05: "maximum one injury allowed at a time". The ledger itself
# tolerates several open injuries (meta_injury_ledger other-open-injury), so the
# supervisor enforces the limit: it reads the same effective projection the
# self-heal reads and fires no click while more than one injury is open.
MAX_ACTIVE_INJURIES = 1
_ACTIVE_INJURY_FORM = (
    "(let [d (futon2.aif.meta-injury-store/load-durable-ledger "
    "futon2.aif.meta-self-heal-production/default-ledger-path)] "
    "(if (map? (:source-pin d)) "
    "(let [a (futon2.aif.meta-injury-ledger/effective-active-injuries-from-validated-ledger (:ledger d))] "
    "(if (= :computed (:status a)) "
    "(str \"ACTIVE-INJURIES \" (count (:injuries a)) \" \" (pr-str (vec (sort (keys (:injuries a)))))) "
    "(str \"ACTIVE-INJURIES-UNREADABLE \" (pr-str (:status a))))) "
    "(str \"ACTIVE-INJURIES-UNREADABLE \" (pr-str (:reason d)))))")


def active_injuries() -> tuple[int | None, str]:
    """Return (count, detail) of effective active META injuries, or (None,
    reason) when the ledger cannot be read; an unreadable count is not zero."""
    r = jvm_eval(_ACTIVE_INJURY_FORM, timeout=120)
    m = re.search(r'ACTIVE-INJURIES (\d+) (.*?)"?\}?$', r["out"])
    if r["rc"] == 0 and m:
        return int(m.group(1)), m.group(2).replace('\\"', '"')
    return None, (r["out"] or r["err"])[-300:]


def injury_limit_breach() -> str | None:
    count, detail = active_injuries()
    if count is None:
        return f"active-injury count unreadable ({detail}); no click fired"
    if count > MAX_ACTIVE_INJURIES:
        return (f"{count} injuries open at once (limit {MAX_ACTIVE_INJURIES}): {detail}; "
                "no click fired")
    return None


def debugger_stops() -> list[dict]:
    """Read the serving debugger, not the click's coarse public phase.

    The value is deliberately reduced to identifiers safe for the loop.  The
    full stop remains in the JVM and is copied into the live repair packet.
    """
    r = jvm_eval("(futon2.aif.wm.debugger/stopped)", timeout=30)
    if r["rc"] != 0:
        return None
    ids = re.findall(r':run-id\s+"([^"]+)"', r["out"])
    phases = re.findall(r':phase\s+:([\w.-]+)', r["out"])
    kinds = re.findall(r':condition\s+\{:kind\s+:([\w./-]+)', r["out"])
    stamps = re.findall(r':stopped-at\s+"([^"]+)"', r["out"])
    return [{"run-id": rid,
             "phase": phases[i] if i < len(phases) else "unknown",
             "condition-kind": kinds[i] if i < len(kinds) else "unknown",
             "stopped-at": stamps[i] if i < len(stamps) else "unknown",
             "raw": r["out"]}
            for i, rid in enumerate(ids)]


def debugger_stop(run_id: str | None = None) -> dict | None:
    for stop in debugger_stops():
        if run_id is None or stop["run-id"] == run_id:
            return stop
    return None


def debugger_attached() -> bool:
    r = jvm_eval("(futon2.aif.wm.debugger/attached?)", timeout=30)
    return r["rc"] == 0 and bool(re.search(r":value\s+true\b", r["out"]))


def attach_debugger() -> dict:
    return jvm_eval("(futon2.aif.wm.debugger/attach!)", timeout=30)


def retry_held_run(run_id: str) -> dict:
    safe = run_id.replace("\\", "\\\\").replace('"', '\\"')
    return jvm_eval(f'(futon2.aif.wm.debugger/continue! "{safe}" :retry)', timeout=30)


def abort_held_run(run_id: str) -> dict:
    safe = run_id.replace("\\", "\\\\").replace('"', '\\"')
    return jvm_eval(f'(futon2.aif.wm.debugger/continue! "{safe}" :abort)', timeout=30)


def handle_held_stop(args, stop: dict, in_flight_repairs: list,
                     live_page: Path, since_epoch: int) -> int:
    """Dispose one debugger stop according to the campaign's repair policy.

    Observation-only means no repair agent may be invoked, including from the
    debugger-held path.  Abort releases the single-flight boundary while
    preserving the stop as the run's observed terminal evidence.
    """
    reason = (f"held run {stop['run-id']} stopped at {stop['phase']} "
              f"({stop['condition-kind']})")
    if (stop["run-id"], stop["phase"]) in HELD_RETRIED:
        # The retry re-held at the same phase: the repair did not remove the
        # cause.  Do not repair again inside this run; abort so the click
        # ends and the next round's self-heal sees it.  (2026-11-05: run
        # 6d57248a re-held at close-refusal after a "repaired" retry and sat
        # unattended for 20 minutes, because the repeat matched a handled
        # signature and was skipped.)
        result = abort_held_run(stop["run-id"])
        record({"event": "debugger-resumed", "run-id": stop["run-id"],
                "phase": stop["phase"], "choice": "abort-repeat-hold",
                "result": result["out"][-500:]})
        log(f"{reason} again after a retry; aborting the held phase")
        return since_epoch
    if args.observe_only:
        result = abort_held_run(stop["run-id"])
        record({"event": "external-repair-skipped", "run-id": stop["run-id"],
                "phase": stop["phase"], "condition-kind": stop["condition-kind"],
                "reason": "META self-heal observation trial"})
        record({"event": "debugger-resumed", "run-id": stop["run-id"],
                "phase": stop["phase"], "choice": "abort-observation-only",
                "result": result["out"][-500:]})
        log(f"observation-only: {reason}; external repair skipped and held phase aborted")
        return since_epoch

    verdict, detail = repair(args, reason, None, in_flight_repairs, live_page)
    in_flight_repairs.append(detail)
    if verdict == "repaired":
        ok, reload_text, since_epoch = reload_when_idle(
            since_epoch, held_run_id=stop["run-id"])
        if ok:
            HELD_RETRIED.add((stop["run-id"], stop["phase"]))
            resumed = retry_held_run(stop["run-id"])
            record({"event": "debugger-resumed", "run-id": stop["run-id"],
                    "phase": stop["phase"], "choice": "retry",
                    "result": resumed["out"][-500:]})
            log("repaired, hot-loaded and retried the held phase")
        else:
            abort_held_run(stop["run-id"])
            log(f"reload failed while held: {reload_text[:300]}")
    elif verdict == "no-defect" and not _retried_no_defect(stop):
        # The repair seat found the machine behaving to contract: the stop is
        # environmental (e.g. a typed :clock/store-busy refusal), and the
        # designed recovery is the debugger's :retry.  One retry per held
        # (run, phase); a second hold there is aborted below.
        HELD_NO_DEFECT_RETRIES.add((stop["run-id"], stop["phase"]))
        HELD_RETRIED.add((stop["run-id"], stop["phase"]))
        resumed = retry_held_run(stop["run-id"])
        record({"event": "debugger-resumed", "run-id": stop["run-id"],
                "phase": stop["phase"], "choice": "retry-no-defect",
                "result": resumed["out"][-500:]})
        log("held repair ended no-defect; retried the held phase once")
    else:
        abort_held_run(stop["run-id"])
        log(f"held repair ended {verdict}; aborting the held phase")
    return since_epoch


# (run-id, phase) pairs already retried after a no-defect verdict.
HELD_NO_DEFECT_RETRIES: set = set()

# (run-id, phase) pairs already retried after any repair verdict; a second
# hold at one of them is aborted, not repaired again.
HELD_RETRIED: set = set()


def _retried_no_defect(stop: dict) -> bool:
    return (stop["run-id"], stop["phase"]) in HELD_NO_DEFECT_RETRIES


def fire(n: int, args, since_epoch: int) -> dict:
    reset_started_ns = time.monotonic_ns()
    reset_cast_sessions(args)
    reset_wall_ms = max(0, (time.monotonic_ns() - reset_started_ns) // 1_000_000)
    out = STATE / f"click-{n:03d}-{int(time.time())}.log"
    before = budget_lines()
    cmd = [F2 / "scripts" / "wm_click.sh", "--run", "--issuing-caller", args.issuing_caller,
           "--author", args.author, "--reviewer", args.reviewer, "--repair-reviewer", args.repair_reviewer]
    log(f"click {n}: firing ({out.name})")
    in_flight_repairs = []
    handled_stops = set()
    # w+ so the log can be read back through this descriptor: on 2026-11-05
    # click 5's log was unlinked mid-click (directory changed 04:57:14Z) and
    # out.read_text() then crashed the loop with FileNotFoundError.
    with out.open("w+", errors="replace") as fh:
        proc = subprocess.Popen(list(map(str, cmd)), cwd=F2, stdout=fh,
                                stderr=subprocess.STDOUT)
        deadline = time.time() + 3 * 3600
        while proc.poll() is None:
            if time.time() >= deadline:
                proc.kill()
                raise subprocess.TimeoutExpired(cmd, 3 * 3600)
            stop = debugger_stop()
            if stop:
                signature = (stop["run-id"], stop["phase"], stop["condition-kind"],
                             stop.get("stopped-at"))
                if signature not in handled_stops:
                    handled_stops.add(signature)
                    record({"event": "debugger-held", **{k: stop[k] for k in
                            ("run-id", "phase", "condition-kind")}})
                    log(f"click {n}: debugger held {stop['run-id']} at {stop['phase']} "
                        f"({stop['condition-kind']})")
                    live_page = DEBUGGER / f"held-{stop['run-id']}.md"
                    DEBUGGER.mkdir(parents=True, exist_ok=True)
                    live_page.write_text("# Live held phase\n\n" + stop["raw"] + "\n")
                    since_epoch = handle_held_stop(
                        args, stop, in_flight_repairs, live_page, since_epoch)
            time.sleep(10)
        rc = proc.returncode
        fh.flush()
        fh.seek(0)
        text = fh.read()
    if not out.exists():
        log(f"click {n}: log {out.name} was removed during the click; "
            "restored from the open descriptor")
        record({"event": "click-log-restored", "n": n, "log": str(out)})
        out.write_text(text)
    m = re.search(r"tracking our click: (\S+)", text)
    click_id = m.group(1) if m else None
    spent = budget_lines() > before
    lr = {}
    try:
        lr = get_json("/api/alpha/wm/click").get("last-result") or {}
    except Exception as e:
        log(f"click {n}: status read failed: {e}")
    if not click_id:
        # The script exited before admission (exit 3: seats or single-flight
        # held); last-result belongs to an earlier click and is not ours.
        lr = {"outcome": "not-launched", "note": f"no click id; last-result is for {lr.get('click-id')}"}
    elif lr.get("click-id") != click_id:
        lr = {"outcome": "unknown", "note": f"last-result is for {lr.get('click-id')}, not {click_id}"}
    run_id = (lr.get("run-id-observation") or {}).get("value")
    record_path = click_record(lr, click_id)
    verdict = read_verdict(record_path)
    why = selection_why(record_path)
    return {"n": n, "rc": rc, "click-id": click_id, "spent": spent, "outcome": lr.get("outcome", "unknown"),
            "run-id": run_id, "carried": carried(run_id), "last-result": lr, "log": str(out),
            "run-record": record_path, "verdict": verdict, "alert": click_alert(verdict), "why": why,
            "since-epoch": since_epoch, "in-flight-repairs": in_flight_repairs,
            "reset-wall-ms": reset_wall_ms}


# ---------------------------------------------------------------- debugger

UNIVERSES = "universe-not-admitted/universes"


def moved(before: dict, after: dict) -> bool:
    """The obstruction moved: a different set of targets refused for something
    other than an unadmitted universe, or at least 5 fewer unadmitted-universe
    refusals. The field gains or loses a few missions between clicks by
    itself (286 -> 289 today), so a small drift in that count is not movement."""
    if before["named"] != after["named"]:
        return True
    return after["counts"].get(UNIVERSES, 0) <= before["counts"].get(UNIVERSES, 0) - 5


def lifecycle_display(census: object) -> list[str]:
    """Render retained lifecycle evidence without treating old aggregates as actionable."""
    if not isinstance(census, dict):
        return ["repair lifecycle: unavailable (legacy record has no lifecycle census)"]
    schema = census.get("schema")
    if schema != "wm/repair-obligation-lifecycle-census-v1":
        status = census.get("status", "unavailable")
        reason = census.get("reason", "unknown")
        return [f"repair lifecycle: unavailable ({status}/{reason}; authority={schema!r})"]
    lines = [f"repair lifecycle authority: {schema}",
             f"repair lifecycle reconciled: {json.dumps(census.get('reconciled?'))}",
             f"repair lifecycle findings: {json.dumps(census.get('finding-count'))}"]
    for bucket in ("actionable-open", "awaiting-validation", "environmental-hold", "closed"):
        receipt = census.get(bucket)
        if not isinstance(receipt, dict):
            lines.append(f"{bucket}: unavailable (missing bucket)")
        else:
            lines.append(f"{bucket}: count={json.dumps(receipt.get('count'))} "
                         f"ids={json.dumps(receipt.get('ids'))}")
    return lines


def debugger_page(c: dict, previous: dict | None) -> tuple[Path, dict]:
    """Write the click's debugger page; return (path, summary). The summary's
    signature is what must change for an abstention to count as moved."""
    DEBUGGER.mkdir(parents=True, exist_ok=True)
    rr = (c.get("last-result") or {}).get("run-record")
    r = sh("bb", F2 / "scripts" / "wm_click_debugger.bb", rr, timeout=600) if rr else None
    try:
        d = json.loads(r.stdout) if r and r.returncode == 0 else {}
    except ValueError:
        d = {}
    named = sorted((x["target"], x["refusal"]) for x in d.get("targets", [])
                   if not x["refusal"].startswith("universe-not-admitted/universes"))
    sig = {"counts": d.get("counts", {}), "named": named}
    lines = [f"# Debugger: {c['click-id']} ({c['outcome']})", "",
             f"run record: {rr}", f"click log: {c['log']}", f"run id: {c.get('run-id')}", "",
             f"failure: {json.dumps(d.get('failure'))}",
             f"chosen: {json.dumps(d.get('chosen'))}",
             *lifecycle_display(d.get("repair-lifecycle-census")), "",
             "## Refusals by kind", *[f"- {k}: {v}" for k, v in sorted(sig["counts"].items())], "",
             "## Targets refused for something other than an unadmitted universe",
             *[f"- {x['target']}  {x['refusal']}  declines={x['declines']}"
               for x in d.get("targets", []) if not x["refusal"].startswith("universe-not-admitted/universes")],
             "", "## First 15 targets refused as universe-not-admitted/universes",
             *[f"- {x['target']}" for x in d.get("targets", [])
               if x["refusal"].startswith("universe-not-admitted/universes")][:15], ""]
    if not d:
        lines += ["(the run record could not be read by wm_click_debugger.bb:",
                  (r.stderr[-1500:] if r else "no run record path") + ")", ""]
    if previous:
        lines += ["## Compared with the previous abstention",
                  f"previous page: {previous['page']}",
                  "CHANGED -- the obstruction moved." if moved(previous["signature"], sig) else
                  "UNCHANGED -- same refusal counts and the same named targets.", ""]
    page = DEBUGGER / f"{c['click-id']}.md"
    page.write_text("\n".join(lines))
    return page, {"page": str(page), "signature": sig}


# ---------------------------------------------------------------- repair

def seat_registered(seat: str) -> bool:
    try:
        agents = get_json("/api/alpha/agents")
    except Exception:
        return False
    agents = agents.get("agents", agents)
    items = agents.values() if isinstance(agents, dict) else agents
    return any((a.get("id") or {}).get("id/value") == seat for a in items if isinstance(a, dict))


def ensure_seat(seat: str, model: str) -> None:
    if seat_registered(seat):
        return
    body = json.dumps({"agent-id": seat, "type": "codex", "cwd": str(CODE) + "/", "model": model}).encode()
    req = urllib.request.Request(f"{AGENCY}/api/alpha/agents/restore", data=body,
                                 headers={"Content-Type": "application/json"}, method="POST")
    urllib.request.urlopen(req, timeout=30).read()
    log(f"registered repair seat {seat} ({model})")


def phase_tail(n: int = 40) -> str:
    p = F2 / "data" / "wm-full-loop-phases.edn.log"
    return "".join(p.read_text(errors="replace").splitlines(True)[-n:]) if p.exists() else "(no phase log)"


GUIDE = """HOW TO REPAIR

You are the repair seat of wm_click_repair_loop.py (futon2/scripts). The loop
fires WM clicks one after another. It stopped because the apparatus failed,
not because the machine decided something. Your job is to find the defect in
the click's code path and fix it so the next click does not fail the same way.

Diagnose from the evidence, not from a guess. Start from the click log and the
last-result, then the run record, the attempt directory and the phase log.
The runner is futon2/src/futon2/aif/full_loop_runner.clj; the service is
futon3c/src/futon3c/wm/runner_service.clj.

NOT DEFECTS -- report VERDICT: no-defect and change nothing:
  - the machine opening a repair obligation (an abstention IS a defect to
    debug; see HOW TO DEBUG above when there is a debugger page);
  - an author's build legitimately failing review or its own tests (that is
    the machine's work, and its repair obligations handle it);
  - a provider quota or capacity refusal (waiting fixes it, code does not).

RULES
  - Change the smallest thing that removes the cause, in futon2/src,
    futon2/test, futon3c/src/futon3c/wm or futon3c/test. Add or adjust a test
    that fails without your fix (plant the bad case and watch it fail).
  - Gates: clj-kondo with no errors on changed files; futon4/dev/check-parens.el
    on them; the targeted tests. Do NOT run the full suite.
  - The worktrees are shared. Stage explicit paths only; never `git commit -a`,
    never amend a commit that is not yours, never touch another agent's
    uncommitted edits.
  - COMMIT, and put this exact tag in the commit message: {tag}
    The loop finds your fix by that tag. An uncommitted fix does not exist.
  - Do NOT fire a click, and do NOT reload anything into the running JVM.
    The loop reloads what you committed and clicks again.
  - If an orphaned Agency job from the failed click is still running, say so;
    cancel it only if it is plainly the failed click's own author job.

Finish with exactly one of these lines as the LAST line of your reply:
  VERDICT: repaired        (you committed a fix carrying the tag)
  VERDICT: no-defect       (the apparatus is fine; say why)
  VERDICT: cannot-repair   (say what a human or Claude seat must decide)
Your final message is the reply; do not bell anyone.
"""


DEBUG_GUIDE = """HOW TO DEBUG THIS ABSTENTION

You are the repair seat of wm_click_repair_loop.py (futon2/scripts). The click
above selected nothing. Joe (2026-09-29): an abstained click is a problem to be
fixed, not a pattern to learn from. The debugger page above lists every target
the judge refused and what it was refused on. Your job is to find why the
machine had nothing it could select, and fix that cause in the code.

Start with the targets refused for something other than an unadmitted
universe (e.g. no-constructed-candidate with empty declines: the record does
not even say why no candidate was built). Follow the refusal to the code that
produces it (grep the refusal kind under futon2/src/futon2/aif: cascade_problems,
wm/cascade_decision, target_field, decision_gate) and to the inputs it read.

COVERAGE IS A REQUIREMENT (Joe, 2026-09-29): the War Machine ranges over ALL
unfinished Missions, Excursions and Tickets in FUTON. A target field in which
only a handful of targets are admitted is a placeholder, not a complete field,
and is a DEFECT however correct each refusal is under today's declarations.
So a universe-not-admitted refusal on an unfinished mission is never
VERDICT: no-defect. If admitting it needs more than code (a declaration only
Joe can make), answer VERDICT: cannot-repair and say exactly what is missing.

DEBUG LIVE: you may evaluate READ-ONLY forms in the serving JVM to watch the
judge's functions on today's inputs: write the form to a file and run
  cd /home/joe/code/futon3c && scripts/proof-eval.sh -f /tmp/<form>.clj
Wrap forms in try and print the cause chain. Never call anything that fires a
click, dispatches a job, or writes stores (runner entry points write real trip
reports and read the real repair store); never reload a namespace.

A fix is code, not data surgery: do not edit mission files, ledgers or stores to
make a refusal disappear. If the right fix is that a target genuinely has no
admissible action and some OTHER target should be constructible, fix whatever
stops that one. VERDICT: no-defect on an abstention is allowed only when every unfinished
Mission, Excursion and Ticket was admitted to the field and each admitted one
was refused for a reason that is true of the task itself (its wants already
hold, say). It stops the loop for a human decision.
"""


# Agency seats add their own system/context envelope. Keep the user payload
# comfortably below the provider's effective request ceiling; a 14.6k packet
# was rejected live with HTTP 400 / code 1261 on 2026-10-18.
REPAIR_PROMPT_MAX_CHARS = 1_800
REPAIR_EVIDENCE_EXCERPT_CHARS = 4_000


def evidence_excerpt(text: str, limit: int = REPAIR_EVIDENCE_EXCERPT_CHARS) -> str:
    """Keep repair dispatches bounded while leaving the full artifact addressable.

    Live run records can contain the whole target field on one EDN line.  Sending
    that line verbatim makes Agency reject the dispatch before the repair seat
    runs.  The repair seat shares this filesystem, so the prompt needs an
    informative excerpt and the artifact path, not a second copy of the store.
    """
    if len(text) <= limit:
        return text
    head = limit * 2 // 3
    tail = limit - head
    return (text[:head] +
            f"\n\n... [{len(text) - limit} chars omitted; read the artifact path above] ...\n\n" +
            text[-tail:])


def repair_packet(tag: str, reason: str, click: dict | None, prior: list[str],
                  page: Path | None = None) -> str:
    parts = [f"REPAIR REQUEST {tag}", "", f"FAILURE: {reason}", ""]
    if page:
        parts += ["== debugger page " + str(page) + " ==",
                  "Full evidence is at the path above; read it locally when the excerpt is truncated.",
                  evidence_excerpt(page.read_text(errors="replace")), "", DEBUG_GUIDE, ""]
    if click:
        last_result = click.get("last-result") or {}
        compact_result = {k: last_result.get(k) for k in
                          ("click-id", "attempt-id", "outcome", "run-record",
                           "run-id-observation", "binding-status", "report-card")
                          if k in last_result}
        parts += ["== last-result summary (full record is at :run-record) ==",
                  evidence_excerpt(json.dumps(compact_result, indent=1)), "",
                  f"click log: {click.get('log')}", "== click log tail ==",
                  evidence_excerpt("".join(Path(click["log"]).read_text(errors="replace").splitlines(True)[-60:])), ""]
    parts += ["== phase log tail (full file: futon2/data/wm-full-loop-phases.edn.log) ==",
              evidence_excerpt(phase_tail()), ""]
    if prior:
        parts += ["== earlier repair attempts on this fault ==",
                  evidence_excerpt("\n".join(prior)), ""]
    parts += [GUIDE.replace("{tag}", tag)]
    packet = "\n".join(parts)
    if len(packet) > REPAIR_PROMPT_MAX_CHARS:
        # Preserve addressability, not an arbitrary prefix: the repair seat
        # shares the filesystem and can read the complete artifacts there.
        guide = ("Read the addressed artifacts, diagnose the smallest apparatus defect, "
                 "add a planted regression test, run focused gates, and commit explicit "
                 f"paths with tag {tag} in the commit message. Do not click or reload. "
                 "End with exactly VERDICT: repaired, VERDICT: no-defect, or "
                 "VERDICT: cannot-repair.")
        anchors = [f"REPAIR REQUEST {tag}", "", f"FAILURE: {reason}", ""]
        if page:
            anchors += [f"debugger-page: {page}"]
        if click:
            last_result = click.get("last-result") or {}
            anchors += [f"run-record: {last_result.get('run-record')}",
                        f"click-log: {click.get('log')}"]
        anchors += ["phase-log: futon2/data/wm-full-loop-phases.edn.log", "",
                    "Evidence was omitted to keep Agency dispatch bounded; read the paths above.",
                    "", guide]
        packet = "\n".join(anchors)
    return packet


def dispatch(seat: str, prompt: str) -> str:
    r = sh(sys.executable, SEND, "--from", "wm-click-loop", "--to", seat, "--kind", "bell",
           "--mode", "work", input=prompt, timeout=120)
    m = re.search(r'"job-id"\s*:\s*"([^"]+)"', r.stdout)
    if not m:
        raise RuntimeError(f"dispatch to {seat} failed: {r.stdout[-400:]} {r.stderr[-400:]}")
    return m.group(1)


def wait_job(job: str, timeout: int) -> dict:
    start = time.time()
    while time.time() - start < timeout:
        try:
            j = get_json(f"/api/alpha/invoke/jobs/{job}").get("job", {})
            if j.get("state") in TERMINAL:
                return j
        except Exception as e:  # a transient Agency hiccup is a wait, not a failure
            log(f"job {job} status: {e}")
        time.sleep(60)
    return {"state": "timeout"}


def tagged_commits(tag: str) -> list[tuple[Path, str]]:
    out = []
    for repo in (F2, F3C):
        for sha in git(repo, "log", "--since=1 day ago", "-F", f"--grep={tag}", "--format=%H").split():
            out.append((repo, sha))
    return out


def kondo_errors(commits: list[tuple[Path, str]]) -> str:
    files = set()
    for repo, sha in commits:
        for rel in git(repo, "show", "--name-only", "--format=", sha).split():
            p = repo / rel
            if p.suffix in (".clj", ".cljc") and p.exists():
                files.add(str(p))
    if not files:
        return ""
    # This is a changed-file judgment, not a project analysis pass.  A project
    # cache may hold dependency metadata from before a helper's arities changed
    # and falsely reject a valid call in the touched file.  Read the current
    # files and dependencies afresh for every repair judgment.
    r = sh("clj-kondo", "--cache", "false", "--lint", *sorted(files), timeout=600)
    return r.stdout[-2000:] if r.returncode >= 3 else ""


def repair(args, reason: str, click: dict | None, prior: list[str],
           page: Path | None = None) -> tuple[str, str]:
    """Send one repair job and judge it. Returns (verdict, detail)."""
    tag = f"wm-click-loop-repair-{uuid.uuid4().hex[:8]}"
    ensure_seat(args.repair_seat, args.seat_model)
    job = dispatch(args.repair_seat, repair_packet(tag, reason, click, prior, page))
    log(f"repair {tag}: job {job} to {args.repair_seat}: {reason[:160]}")
    record({"event": "repair-dispatched", "tag": tag, "job": job, "seat": args.repair_seat, "reason": reason})
    j = wait_job(job, args.repair_timeout)
    result = str(j.get("result") or j.get("error") or "")
    (STATE / f"{tag}.result.txt").write_text(result)
    m = re.findall(r"VERDICT:\s*(repaired|no-defect|cannot-repair)", result)
    verdict = m[-1] if m else f"no-verdict (job {j.get('state')})"
    commits = tagged_commits(tag)
    detail = f"job {job} {j.get('state')}; verdict {verdict}; commits {[s[:9] for _, s in commits]}"
    if verdict == "repaired" and not commits:
        verdict, detail = "unlanded", detail + "; claimed repaired but no commit carries the tag"
    elif commits:
        errs = kondo_errors(commits)
        if errs:
            verdict, detail = "kondo-errors", detail + "; clj-kondo errors:\n" + errs
    log(f"repair {tag}: {detail[:400]}")
    record({"event": "repair-judged", "tag": tag, "job": job, "verdict": verdict, "detail": detail,
            "commits": [f"{r.name}:{s}" for r, s in commits]})
    return verdict, detail


# ---------------------------------------------------------------- stop

def notify(args, case: str, legal: bool, reason: str) -> None:
    rows = LEDGER.read_text().splitlines()[-12:] if LEDGER.exists() else []
    disposition = f"{case}/{'legal-stop' if legal else 'outside-repair'}"
    body = "\n".join([f"From wm_click_repair_loop.py, {now()}. "
                      f"STOPPED [{disposition}]: {reason}", "",
                      "Last ledger rows (futon2/data/wm-click-loop/ledger.jsonl):", *rows, "",
                      "Restart only with the same explicit cast recorded in the start row; "
                      "no cast identity has a default."])
    r = sh(sys.executable, SEND, "--from", "wm-click-loop", "--to", args.owner, "--kind", "bell",
           input=body, timeout=120)
    log(f"notified {args.owner}: {r.stdout.strip()[-200:]}")


# Reads the retained run record as EDN (babashka), never by text search: a
# scan for the next "[" after the key read `:outer-field/admitted nil` as the
# following `[]`, and took a nested previous-selection's empty field for a
# missing top-level one; both made "work exhausted" legal with no field.
# The outer no-task decision reaches the supervisor as the selection record
# (`click_record` fallback), which carries `:selection-outcome {:status
# :abstained}` beside `:outer/selection`, not the runtime's `:absent` map.
_WORK_EXHAUSTED_EDN = (
    "(let [r (clojure.edn/read-string {:default tagged-literal}"
    " (slurp (first *command-line-args*)))"
    " a (get-in r [:outer/selection :outer-field/admitted] ::missing)]"
    " (println (boolean (and (map? r)"
    " (or (= :no-task-selected (:absent r))"
    " (= :abstained (get-in r [:selection-outcome :status])))"
    " (vector? a) (empty? a)))))")

_CLAIMS_WORK_EXHAUSTED_EDN = (
    "(let [r (clojure.edn/read-string {:default tagged-literal}"
    " (slurp (first *command-line-args*)))]"
    " (println (= :loop-stop"
    " (get-in r [:outer/selection :refusal/disposition :disposition]))))")


def _work_exhausted_checked(evidence: dict) -> bool:
    """L1 (RefusalLegality.loopStopLegal .workExhausted): the outer selector
    returned no task AND its retained admitted field is present and empty."""
    path = evidence.get("run-record") if isinstance(evidence, dict) else None
    if not path or not Path(path).is_file():
        return False
    try:
        r = subprocess.run(["bb", "-e", _WORK_EXHAUSTED_EDN, str(path)],
                           capture_output=True, text=True, timeout=120)
    except (OSError, subprocess.TimeoutExpired):
        return False
    return r.returncode == 0 and r.stdout.strip() == "true"


def _record_claims_work_exhausted(record_path: str | None) -> bool:
    """Read only the outer validator's retained claim. Stop legality is
    independently recomputed by `_work_exhausted_checked`."""
    if not record_path or not Path(record_path).is_file():
        return False
    try:
        r = subprocess.run(["bb", "-e", _CLAIMS_WORK_EXHAUSTED_EDN, record_path],
                           capture_output=True, text=True, timeout=120)
    except (OSError, subprocess.TimeoutExpired):
        return False
    return r.returncode == 0 and r.stdout.strip() == "true"


def _validated_stop(args, claimed_case: str, evidence: dict) -> tuple[str, bool]:
    if claimed_case == "operator-override":
        signal = evidence.get("operator-signal") if isinstance(evidence, dict) else None
        if signal == "stop-file-observed":
            legal = STOP_FILE.exists()
        elif signal == "cli-budget-spent":
            legal = ((evidence.get("spent") == evidence.get("click-budget") == args.clicks) or
                     (evidence.get("ticks") == evidence.get("tick-budget") == args.ticks))
        else:
            legal = False
    elif claimed_case == "work-exhausted":
        legal = _work_exhausted_checked(evidence)
    else:
        legal = claimed_case == "debugger-error"
    return (claimed_case, True) if legal and claimed_case != "debugger-error" else ("debugger-error", False)


def stop(args, case: str, reason: str, code: int, evidence: dict) -> int:
    effective_case, legal = _validated_stop(args, case, evidence)
    log(f"STOP: {reason}")
    row = {"event": "stop", "case": effective_case, "legal": legal,
           "evidence": evidence, "reason": reason}
    if effective_case != case:
        row["claimed-case"] = case
    record(row)
    notify(args, effective_case, legal, reason)
    # A claimed legal stop that failed validation is a debugger error; it
    # never exits as success, whatever code the claiming site passed.
    return 1 if effective_case != case else code


# ---------------------------------------------------------------- main

def run(args) -> int:
    lock = (STATE / "loop.lock").open("w")
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        print("another wm_click_repair_loop holds the lock; exiting")
        return 0
    STOP_FILE.unlink(missing_ok=True)
    log(f"=== start: clicks={args.clicks} ticks={args.ticks} stall={args.stall} "
        f"max-repairs={args.max_repairs} repair-seat={args.repair_seat} ===")
    record({"event": "start", "args": vars(args)})
    since = jvm_start_epoch()
    initial_reload_started_ns = time.monotonic_ns()
    ok, text, since = reload_when_idle(since)
    initial_reload_ms = max(0, (time.monotonic_ns() - initial_reload_started_ns) // 1_000_000)
    fault_repairs: list[str] = []
    while not ok:  # a namespace that will not load is a repair before any click
        if len(fault_repairs) >= args.max_repairs:
            return stop(args, "debugger-error", f"reload still failing after {len(fault_repairs)} repairs: {text[:300]}", 1, {})
        verdict, detail = repair(args, text, None, fault_repairs)
        fault_repairs.append(detail)
        if verdict in ("cannot-repair", "no-defect"):
            return stop(args, "debugger-error", f"reload failing and the repair seat answered {verdict}: {detail[:300]}", 1, {})
        ok, text, since = reload_when_idle(since)

    spent = ticks = quiet = 0
    last_abstention: dict | None = None
    previous_alert: list[str] | None = None
    stopping_digest: str | None = None  # the field on which nothing beat stopping
    initial_reload_pending = True
    while spent < args.clicks and ticks < args.ticks:
        reload_ms = initial_reload_ms if initial_reload_pending else 0
        if STOP_FILE.exists():
            return stop(args, "operator-override",
                        f"stop file present after {spent} clicks, {ticks} carrying", 0,
                        {"operator-signal": "stop-file-observed"})
        if args.continuous and spent:
            # A fix landed between clicks is loaded before the next one; that is
            # how an alert's signature stops repeating without a pause.
            reload_started_ns = time.monotonic_ns()
            ok, text, since = reload_when_idle(since)
            reload_ms = max(0, (time.monotonic_ns() - reload_started_ns) // 1_000_000)
            if not ok:
                return stop(args, "debugger-error", f"paused: a namespace committed since the last click will not load: {text[:300]}", 1, {})
        stops = debugger_stops()
        if len(stops) > 1:
            reason = (f"self-heal-debugger-frames-stacked: {len(stops)} debugger stops; "
                      "no click fired")
            record({"event": "stop-evidence", "kind": "self-heal-debugger-frames-stacked",
                    "stop-count": len(stops),
                    "run-ids": [held["run-id"] for held in stops]})
            return stop(args, "debugger-error", reason, 1, {})
        if len(stops) == 1:
            held = stops[0]
            record({"event": "debugger-held", **{key: held[key] for key in
                    ("run-id", "phase", "condition-kind")}})
            live_page = DEBUGGER / f"held-{held['run-id']}.md"
            DEBUGGER.mkdir(parents=True, exist_ok=True)
            live_page.write_text("# Live held phase\n\n" + held["raw"] + "\n")
            since = handle_held_stop(args, held, [], live_page, since)
            continue
        breach = injury_limit_breach()
        if breach:
            record({"event": "stop-evidence", "kind": "injury-limit", "detail": breach})
            return stop(args, "debugger-error", f"injury-limit: {breach}", 1, {})
        attached = debugger_attached()
        if not attached:
            result = attach_debugger()
            record({"event": "debugger-attached", "click-number": spent + 1,
                    "result": result.get("out", "")[-500:]})
        attached = debugger_attached()
        stops_after_attach = debugger_stops()
        if not attached or stops_after_attach:
            reason = ("debugger-not-attached: verification failed "
                      f"(attached={attached}, stops={len(stops_after_attach)}); no click fired")
            record({"event": "stop-evidence", "kind": "debugger-not-attached",
                    "attached": attached, "stop-count": len(stops_after_attach)})
            return stop(args, "debugger-error", reason, 1, {})
        log(f"click {spent + 1}: debugger attached={attached}")
        fire_started_ns = time.monotonic_ns()
        c = fire_with_write_manifest(spent + 1, args, since)
        fire_wall_ms = max(0, (time.monotonic_ns() - fire_started_ns) // 1_000_000)
        c["reset-wall-ms"] = c.get("reset-wall-ms", 0) + reload_ms
        initial_reload_pending = False
        since = c.pop("since-epoch")
        spent += 1 if c["spent"] else 0
        if c["carried"]:
            ticks += 1
        log(f"click {c['n']}: {c['click-id']} outcome={c['outcome']} run={c['run-id']} "
            f"carried={c['carried']} rc={c['rc']}  [spent {spent}/{args.clicks}, carrying {ticks}/{args.ticks}]")
        record_cost_receipt(c, fire_wall_ms)
        record({"event": "click", **{k: v for k, v in c.items() if k != "last-result"},
                "last-result": c["last-result"], "spent-total": spent, "carrying-total": ticks})
        failure = machine_self_heal_failure(c)
        if failure:
            fields = {key: failure.get(key) for key in
                      ("route", "status", "reason", "phase", "injury-id", "attempt")
                      if failure.get(key) is not None}
            record({"event": "stop-evidence", "kind": "machine-self-heal-failed",
                    "click-id": c["click-id"], **fields})
            detail = ", ".join(f"{key} {value}" for key, value in fields.items())
            reason = (f"machine-self-heal-failed: click {c['click-id']}"
                      f"{', ' if detail else ''}{detail}; no next iteration started")
            return stop(args, "debugger-error", reason, 1, {})
        if (_runtime_disposition(c.get("last-result")) is None and
                _binding_disposition(c.get("click-id")) is None):
            log(f"click {c['click-id']}: retained runtime disposition unreadable; "
                "keeping current loop behaviour")
        if c["outcome"] == "preflight-refused":
            return stop(args, "debugger-error", c["last-result"]["reason"], 1, {})
        if c["rc"] == 3 and not c["spent"]:
            return stop(args, "debugger-error", launch_failure_text(c.get("log")), 1, {})
        if args.continuous:
            acknowledged = acknowledged_rows()
            open_rows = unacknowledged(c["alert"], acknowledged)
            record({"event": "verdict", "click-id": c["click-id"], "run-id": c["run-id"],
                    "conforms": c["alert"] is None, "signature": c["alert"],
                    "acknowledged": [r for r in (c["alert"] or []) if r in acknowledged],
                    "open": open_rows})
            if c["alert"] is not None:
                repeated = open_rows is not None and previous_alert == open_rows
                queue_alert(c, repeated)
                if repeated:
                    return stop(args, "debugger-error",
                                "paused: the same requirements failed on two consecutive clicks "
                                f"({'; '.join(open_rows)}); fix, then restart. Queue: {ALERTS}", 1, {})
                if open_rows is not None:
                    notify_alert(args, c)
                else:
                    log(f"click {c['n']}: only acknowledged rows failed ({'; '.join(c['alert'])}); no bell")
            previous_alert = open_rows
        if (c["outcome"] in DEBUG and c.get("run-record") and
                _record_claims_work_exhausted(c["run-record"])):
            return stop(args, "work-exhausted",
                        f"outer selector exhausted work in {c['run-record']}", 0,
                        {"run-record": c["run-record"]})
        if c["carried"]:
            quiet, fault_repairs = 0, []
            stopping_digest = None
            continue
        if args.continuous:
            digest = nothing_beats_stopping(c.get("run-record"))
            if digest:
                record({"event": "nothing-beats-stopping", "click-id": c["click-id"], "input-digest": digest,
                        "repeated": digest == stopping_digest})
                if digest == stopping_digest:
                    return stop(args, "debugger-error",
                                "paused: on an unchanged field no compared policy's G is below the "
                                f"abstention's, twice (input digest {digest[:12]}); the field must change "
                                "or the model must; re-firing would be the repeated identical-input "
                                "refusal Q6 forbids", 1, {"input-digest": digest})
                stopping_digest = digest
                log(f"click {c['n']}: nothing beats stopping on input {digest[:12]}; waiting {WAIT_SECONDS}s "
                    "for the field to change before the next click")
                time.sleep(WAIT_SECONDS)
                continue
        if c["outcome"] in DECIDED or c["outcome"] in WAIT:
            quiet += 1
            if quiet >= args.stall:
                return stop(args, "debugger-error",
                            f"{quiet} clicks in a row carried no tick without failing "
                            f"(last outcome {c['outcome']}); the wall repeats", 0, {})
            if c["outcome"] in WAIT:
                log(f"click {c['n']}: {c['outcome']}; waiting {WAIT_SECONDS}s before the next click")
                time.sleep(WAIT_SECONDS)
            continue
        page = None
        if c["outcome"] in DEBUG:
            page, summary = debugger_page(c, last_abstention)
            previous_page = last_abstention
            page_moved = (previous_page and
                          moved(previous_page["signature"], summary["signature"]))
            record({"event": "debugger", "click-id": c["click-id"], "page": str(page),
                    "counts": summary["signature"]["counts"],
                    "moved": page_moved if previous_page else None})
            log(f"click {c['n']}: debugger page {page}")
            if args.continuous and previous_page and not page_moved:
                return stop(args, "debugger-error",
                            f"continuous debugger page did not move: {previous_page['page']} "
                            f"then {summary['page']}", 1,
                            {"previous-page": previous_page["page"],
                             "current-page": summary["page"]})
            if page_moved:
                log(f"click {c['n']}: the abstention moved since {last_abstention['page']}; repair count reset")
                fault_repairs = []
            last_abstention = summary
        if args.observe_only or args.continuous:
            record({"event": "external-repair-skipped",
                    "click-id": c["click-id"],
                    "reason": ("continuous: alert queued for the owner" if args.continuous
                               else "META self-heal observation trial")})
            log(f"click {c['n']}: no external repair dispatched "
                f"({'continuous' if args.continuous else 'observation-only'})")
            continue
        # The apparatus failed, or the machine had nothing to select. Repair,
        # reload, click again.
        if len(fault_repairs) >= args.max_repairs:
            return stop(args, "debugger-error",
                        f"{len(fault_repairs)} repairs did not produce a carrying click or move "
                        f"the obstruction; last outcome {c['outcome']}", 1, {})
        reason = f"click {c['click-id']} ended {c['outcome']} (wm_click.sh rc={c['rc']})"
        verdict, detail = repair(args, reason, c, fault_repairs, page)
        fault_repairs.append(f"{reason} -> {detail}")
        if verdict == "cannot-repair" or (page and verdict == "no-defect"):
            return stop(args, "debugger-error", f"repair seat answered {verdict}: {detail[:300]}", 1, {})
        ok, text, since = reload_when_idle(since)
        while not ok:
            if len(fault_repairs) >= args.max_repairs:
                return stop(args, "debugger-error", f"reload failing after repairs: {text[:300]}", 1, {})
            verdict, detail = repair(args, text, None, fault_repairs)
            fault_repairs.append(f"{text[:200]} -> {detail}")
            if verdict in ("cannot-repair", "no-defect"):
                return stop(args, "debugger-error", f"reload failing; repair seat answered {verdict}", 1, {})
            ok, text, since = reload_when_idle(since)
    return stop(args, "operator-override", f"done: {spent} clicks spent, {ticks} carried a tick", 0,
                {"operator-signal": "cli-budget-spent", "spent": spent,
                 "click-budget": args.clicks, "ticks": ticks, "tick-budget": args.ticks})


def status() -> int:
    rows = [json.loads(l) for l in LEDGER.read_text().splitlines()] if LEDGER.exists() else []
    clicks = [r for r in rows if r.get("event") == "click"]
    for r in rows[-15:]:
        print(json.dumps({k: r[k] for k in r if k not in ("last-result", "args")})[:300])
    print(f"clicks recorded {len(clicks)}, carrying {sum(1 for r in clicks if r.get('carried'))}")
    return 0


def parse_args(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("status")
    r = sub.add_parser("run")
    r.add_argument("--clicks", type=int, required=True, help="clicks this run may spend")
    r.add_argument("--ticks", type=int, default=10**6, help="stop after this many carrying clicks")
    r.add_argument("--stall", type=int, default=3)
    r.add_argument("--max-repairs", type=int, default=3)
    r.add_argument("--observe-only", action="store_true",
                   help="capture debugger evidence but never dispatch external repairs")
    r.add_argument("--continuous", action="store_true",
                   help="record every click's Lean verdict, queue alerts for --owner, dispatch no "
                        "repair seat, pause only when the same requirements fail on consecutive clicks")
    r.add_argument("--repair-seat", help="repair worker; defaults to the explicit repair reviewer")
    r.add_argument("--seat-model", default="gpt-5.6-sol")
    r.add_argument("--repair-timeout", type=int, default=5400)
    r.add_argument("--owner", required=True)
    r.add_argument("--issuing-caller", required=True)
    r.add_argument("--author", required=True)
    r.add_argument("--reviewer", required=True)
    r.add_argument("--repair-reviewer", required=True)
    args = ap.parse_args(argv)
    if args.cmd == "run" and not args.repair_seat:
        args.repair_seat = args.repair_reviewer
    return args


def main() -> int:
    args = parse_args()
    STATE.mkdir(parents=True, exist_ok=True)
    if args.cmd == "status":
        return status()
    try:
        return run(args)
    except Exception as e:  # a crash is a stop like any other: the owner hears of it
        import traceback
        log(traceback.format_exc())
        return stop(args, "debugger-error", f"loop crashed: {type(e).__name__}: {e}", 2, {})


if __name__ == "__main__":
    sys.exit(main())
