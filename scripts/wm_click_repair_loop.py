#!/usr/bin/env python3
"""Run WM clicks one after another, repairing the apparatus between them
(Joe, 2026-09-29: "I'd like the run-repair cycles to start working reliably;
perhaps we could go back to the loop variant rather than relying on bells").

Modelled on apm-lean's solve-loop.py and topology-supervisor.sh: this process
fires each click, waits for it, judges it from files, and sends repair work as
an Agency job whose job id it waits on. No step waits on a bellback or a park.

  wm_click_repair_loop.py status
  wm_click_repair_loop.py run --clicks 18 [--ticks 20] [--repair-seat wmfix-1]

Per click:
1. Reload into the serving JVM every click-path namespace (futon2/src,
   futon3c/src/futon3c/wm) whose file was committed after the last reload.
   The first reload uses the JVM's start time. A file with uncommitted edits is
   never reloaded: that would load someone's half-finished work.
2. Fire with scripts/wm_click.sh --run (it waits for seats and the
   single-flight boundary) and read the click's last-result.
3. The click CARRIED a tick when its run id appears in data/wm-trace. That is
   the count toward --ticks.
4. A failed click (build-failed, error, service-failed, ...) or a failed
   reload goes to the repair seat with the evidence. The repair is judged by
   the world: a commit tagged with the repair id, clj-kondo with no errors on
   the files it changed, and a clean reload. A seat that finds no apparatus
   defect says so and the loop clicks again.
5. The loop stops when the click budget or the tick target is reached, when
   --stall clicks in a row carry nothing without failing (the wall repeats),
   when --max-repairs repairs of one fault do not produce a carrying click,
   or when the stop file exists. Every stop bells --owner once.

State: data/wm-click-loop/ledger.jsonl (one line per event), loop.log beside it.
Stop file: data/wm-click-loop/STOP (touch it; the loop stops before the next click).
"""
from __future__ import annotations

import argparse
import datetime
import fcntl
import json
import os
import re
import subprocess
import sys
import time
import urllib.request
import uuid
from pathlib import Path

CODE = Path.home() / "code"
F2 = CODE / "futon2"
F3C = CODE / "futon3c"
STATE = F2 / "data" / "wm-click-loop"
LEDGER = STATE / "ledger.jsonl"
LOGFILE = STATE / "loop.log"
STOP_FILE = STATE / "STOP"
AGENCY = "http://localhost:7070"
SEND = F3C / "scripts" / "agency_send.py"
EVAL = F3C / "scripts" / "proof-eval.sh"
TERMINAL = {"done", "failed", "error", "cancelled", "timeout", "timed-out"}
# Outcomes that are the machine deciding, not the apparatus breaking. They are
# never sent to repair; a run of them without a tick is the stall stop.
DECIDED = {"abstained", "ok", "grounded-change", "grounded-no-change", "artifact-only",
           "guardrail-refusal", "policy-nondiscrimination", "cohort-complete"}
# A seat or a shared service was unavailable: waiting fixes it, code does not.
# The loop pauses, and these count toward the stall stop like DECIDED ones.
WAIT = {"agent-unavailable", "substrate-unavailable"}
WAIT_SECONDS = 900
# The click's code: what a repair may change and what a reload must pick up.
CLICK_PATHS = [(F2, "src"), (F3C, "src/futon3c/wm")]


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


def sh(*args, cwd=None, timeout=None, input=None) -> subprocess.CompletedProcess:
    return subprocess.run(list(map(str, args)), cwd=cwd, capture_output=True, text=True,
                          timeout=timeout, input=input)


def git(repo: Path, *args: str) -> str:
    return sh("git", "-C", repo, *args).stdout


def get_json(path: str, timeout: int = 30) -> dict:
    with urllib.request.urlopen(f"{AGENCY}{path}", timeout=timeout) as fh:
        return json.load(fh)


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
        out = git(repo, "log", f"--since=@{since_epoch}", "--name-only", "--format=", "--", sub)
        for rel in out.split():
            p = repo / rel
            if p.suffix in (".clj", ".cljc") and p.exists():
                files.add(p)
    return sorted(files)


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


def reload_changed(since_epoch: int) -> tuple[bool, str, int]:
    """Reload every click-path namespace committed since since_epoch. Returns
    (ok, failure-text, new-since). Never reloads while a click is in flight."""
    while click_running():
        log("reload: a click is in flight; waiting")
        time.sleep(60)
    started = int(time.time())
    files = changed_click_files(since_epoch)
    skipped = [f for f in files if dirty(f)]
    order = [(ns, f) for ns, f in reload_order([f for f in files if f not in skipped])]
    for f in skipped:
        log(f"reload: SKIP {f} -- uncommitted edits in the working tree")
    for ns, f in order:
        r = jvm_eval(f"(try (require (quote {ns}) :reload) :reloaded "
                     f"(catch Throwable t (loop [t t acc []] (if t (recur (.getCause t) "
                     f"(conj acc (str (.getName (class t)) \": \" (.getMessage t)))) (str \"RELOAD-FAILED \" acc)))))")
        if r["rc"] != 0 or ":reloaded" not in r["out"]:
            text = f"reload of {ns} ({f}) failed: {r['out'][:1500]} {r['err'][:500]}"
            log(text)
            record({"event": "reload", "ok": False, "ns": ns, "detail": text})
            return False, text, since_epoch
    log(f"reload: {len(order)} namespace(s) since @{since_epoch}: {' '.join(ns for ns, _ in order) or '-'}")
    record({"event": "reload", "ok": True, "since": since_epoch,
            "namespaces": [ns for ns, _ in order], "skipped-dirty": [str(f) for f in skipped]})
    return True, "", started


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


def fire(n: int, args) -> dict:
    out = STATE / f"click-{n:03d}-{int(time.time())}.log"
    before = budget_lines()
    cmd = [F2 / "scripts" / "wm_click.sh", "--run", "--issuing-caller", args.issuing_caller,
           "--author", args.author, "--reviewer", args.reviewer, "--repair-reviewer", args.repair_reviewer]
    log(f"click {n}: firing ({out.name})")
    with out.open("w") as fh:
        rc = subprocess.run(list(map(str, cmd)), cwd=F2, stdout=fh, stderr=subprocess.STDOUT,
                            timeout=3 * 3600).returncode
    text = out.read_text(errors="replace")
    m = re.search(r"tracking our click: (\S+)", text)
    click_id = m.group(1) if m else None
    spent = budget_lines() > before
    lr = {}
    try:
        lr = get_json("/api/alpha/wm/click").get("last-result") or {}
    except Exception as e:
        log(f"click {n}: status read failed: {e}")
    if click_id and lr.get("click-id") != click_id:
        lr = {"outcome": "unknown", "note": f"last-result is for {lr.get('click-id')}, not {click_id}"}
    run_id = (lr.get("run-id-observation") or {}).get("value")
    return {"n": n, "rc": rc, "click-id": click_id, "spent": spent, "outcome": lr.get("outcome", "unknown"),
            "run-id": run_id, "carried": carried(run_id), "last-result": lr, "log": str(out)}


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
  - the machine abstaining, refusing a target, or opening a repair obligation;
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


def repair_packet(tag: str, reason: str, click: dict | None, prior: list[str]) -> str:
    parts = [f"REPAIR REQUEST {tag}", "", f"FAILURE: {reason}", ""]
    if click:
        parts += ["== last-result ==", json.dumps(click.get("last-result"), indent=1), "",
                  f"click log: {click.get('log')}", "== click log tail ==",
                  "".join(Path(click["log"]).read_text(errors="replace").splitlines(True)[-60:]), ""]
    parts += ["== phase log tail (futon2/data/wm-full-loop-phases.edn.log) ==", phase_tail(), ""]
    if prior:
        parts += ["== earlier repair attempts on this fault ==", *prior, ""]
    parts += [GUIDE.replace("{tag}", tag)]
    return "\n".join(parts)


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
    r = sh("clj-kondo", "--lint", *sorted(files), timeout=600)
    return r.stdout[-2000:] if r.returncode >= 3 else ""


def repair(args, reason: str, click: dict | None, prior: list[str]) -> tuple[str, str]:
    """Send one repair job and judge it. Returns (verdict, detail)."""
    tag = f"wm-click-loop-repair-{uuid.uuid4().hex[:8]}"
    ensure_seat(args.repair_seat, args.seat_model)
    job = dispatch(args.repair_seat, repair_packet(tag, reason, click, prior))
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

def notify(args, reason: str) -> None:
    rows = LEDGER.read_text().splitlines()[-12:] if LEDGER.exists() else []
    body = "\n".join([f"From wm_click_repair_loop.py, {now()}. STOPPED: {reason}", "",
                      "Last ledger rows (futon2/data/wm-click-loop/ledger.jsonl):", *rows, "",
                      "Restart: systemd-run --user --unit=wm-click-loop --collect "
                      "python3 /home/joe/code/futon2/scripts/wm_click_repair_loop.py run --clicks N"])
    r = sh(sys.executable, SEND, "--from", "wm-click-loop", "--to", args.owner, "--kind", "bell",
           input=body, timeout=120)
    log(f"notified {args.owner}: {r.stdout.strip()[-200:]}")


def stop(args, reason: str, code: int = 0) -> int:
    log(f"STOP: {reason}")
    record({"event": "stop", "reason": reason})
    notify(args, reason)
    return code


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
    ok, text, since = reload_changed(since)
    fault_repairs: list[str] = []
    while not ok:  # a namespace that will not load is a repair before any click
        if len(fault_repairs) >= args.max_repairs:
            return stop(args, f"reload still failing after {len(fault_repairs)} repairs: {text[:300]}", 1)
        verdict, detail = repair(args, text, None, fault_repairs)
        fault_repairs.append(detail)
        if verdict in ("cannot-repair", "no-defect"):
            return stop(args, f"reload failing and the repair seat answered {verdict}: {detail[:300]}", 1)
        ok, text, since = reload_changed(since)

    spent = ticks = quiet = 0
    while spent < args.clicks and ticks < args.ticks:
        if STOP_FILE.exists():
            return stop(args, f"stop file present after {spent} clicks, {ticks} carrying")
        c = fire(spent + 1, args)
        spent += 1 if c["spent"] else 0
        if c["carried"]:
            ticks += 1
        log(f"click {c['n']}: {c['click-id']} outcome={c['outcome']} run={c['run-id']} "
            f"carried={c['carried']} rc={c['rc']}  [spent {spent}/{args.clicks}, carrying {ticks}/{args.ticks}]")
        record({"event": "click", **{k: v for k, v in c.items() if k != "last-result"},
                "last-result": c["last-result"], "spent-total": spent, "carrying-total": ticks})
        if c["rc"] == 3 and not c["spent"]:
            return stop(args, "wm_click.sh could not launch (exit 3: seats or single-flight held 30 min)", 1)
        if c["carried"]:
            quiet, fault_repairs = 0, []
            continue
        if c["outcome"] in DECIDED or c["outcome"] in WAIT:
            quiet += 1
            if quiet >= args.stall:
                return stop(args, f"{quiet} clicks in a row carried no tick without failing "
                                  f"(last outcome {c['outcome']}); the wall repeats")
            if c["outcome"] in WAIT:
                log(f"click {c['n']}: {c['outcome']}; waiting {WAIT_SECONDS}s before the next click")
                time.sleep(WAIT_SECONDS)
            continue
        # The apparatus failed. Repair, reload, click again.
        if len(fault_repairs) >= args.max_repairs:
            return stop(args, f"{len(fault_repairs)} repairs did not produce a carrying click; "
                              f"last outcome {c['outcome']}", 1)
        reason = f"click {c['click-id']} ended {c['outcome']} (wm_click.sh rc={c['rc']})"
        verdict, detail = repair(args, reason, c, fault_repairs)
        fault_repairs.append(f"{reason} -> {detail}")
        if verdict == "cannot-repair":
            return stop(args, f"repair seat cannot repair: {detail[:300]}", 1)
        ok, text, since = reload_changed(since)
        while not ok:
            if len(fault_repairs) >= args.max_repairs:
                return stop(args, f"reload failing after repairs: {text[:300]}", 1)
            verdict, detail = repair(args, text, None, fault_repairs)
            fault_repairs.append(f"{text[:200]} -> {detail}")
            if verdict in ("cannot-repair", "no-defect"):
                return stop(args, f"reload failing; repair seat answered {verdict}", 1)
            ok, text, since = reload_changed(since)
    return stop(args, f"done: {spent} clicks spent, {ticks} carried a tick")


def status() -> int:
    rows = [json.loads(l) for l in LEDGER.read_text().splitlines()] if LEDGER.exists() else []
    clicks = [r for r in rows if r.get("event") == "click"]
    for r in rows[-15:]:
        print(json.dumps({k: r[k] for k in r if k not in ("last-result", "args")})[:300])
    print(f"clicks recorded {len(clicks)}, carrying {sum(1 for r in clicks if r.get('carried'))}")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("status")
    r = sub.add_parser("run")
    r.add_argument("--clicks", type=int, required=True, help="clicks this run may spend")
    r.add_argument("--ticks", type=int, default=10**6, help="stop after this many carrying clicks")
    r.add_argument("--stall", type=int, default=3)
    r.add_argument("--max-repairs", type=int, default=3)
    r.add_argument("--repair-seat", default="wmfix-1")
    r.add_argument("--seat-model", default="gpt-5.6-sol")
    r.add_argument("--repair-timeout", type=int, default=5400)
    r.add_argument("--owner", default="claude-1")
    r.add_argument("--issuing-caller", default="claude-1")
    r.add_argument("--author", default="codex-proof2d")
    r.add_argument("--reviewer", default="codex-proof2e")
    r.add_argument("--repair-reviewer", default="codex-2")
    args = ap.parse_args()
    STATE.mkdir(parents=True, exist_ok=True)
    if args.cmd == "status":
        return status()
    try:
        return run(args)
    except Exception as e:  # a crash is a stop like any other: the owner hears of it
        import traceback
        log(traceback.format_exc())
        return stop(args, f"loop crashed: {type(e).__name__}: {e}", 2)


if __name__ == "__main__":
    sys.exit(main())
