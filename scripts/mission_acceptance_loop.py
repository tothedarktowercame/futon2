#!/usr/bin/env python3
"""mission_acceptance_loop.py -- give every mission a readable disposition and,
if open, acceptance checkboxes the War Machine can check.

Joe, 2026-09-30: "ask Codex to read all of the missions, decide their
disposition (open or not) and write acceptance checkboxes for all of the open
ones ... then we can stop talking about 18 and start talking about 'all open'".

One job per chunk of missions (one repo per chunk), spread over idle Codex
seats. A chunk counts only when a check computed in the serving JVM passes:

  - every mission's first Status line classifies as open (lead token OPEN) or
    closed (:complete / :inactive), never :unknown or :draft;
  - every open mission yields a cascade source with >= 1 checkbox hole
    (futon2.aif.mission-hole-wants/mission-source, the function the tick uses);
  - the job's commit touches only the chunk's mission files, deletes no
    `- [ ]`/`- [x]` line, and flips no box.

Missions a job CLOSED that were live before are listed for review, not failed.

  mission_acceptance_loop.py plan            # write data/mission-acceptance/chunks.json
  mission_acceptance_loop.py run --only c001 # one chunk (the pilot)
  mission_acceptance_loop.py run --seats codex-1,codex-3,...
  mission_acceptance_loop.py status
State: futon2/data/mission-acceptance/{chunks.json,ledger.jsonl,loop.log,STOP}.
"""
import argparse
import json
import re
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import wm_click_repair_loop as w  # noqa: E402  (sh, git, get_json, jvm_eval, TERMINAL, now)

STATE = w.F2 / "data" / "mission-acceptance"
CHUNKS = STATE / "chunks.json"
LEDGER = STATE / "ledger.jsonl"
LOGFILE = STATE / "loop.log"
STOP_FILE = STATE / "STOP"
CHUNK_SIZE = 8
JOB_TIMEOUT = 90 * 60
LOCK = threading.Lock()


def log(msg: str) -> None:
    line = f"[{w.now()}] {msg}"
    with LOCK:
        print(line, flush=True)
        with LOGFILE.open("a") as fh:
            fh.write(line + "\n")


def record(row: dict) -> None:
    with LOCK, LEDGER.open("a") as fh:
        fh.write(json.dumps({"at": w.now(), **row}, ensure_ascii=False) + "\n")


def edn_json(form: str) -> object:
    """Evaluate FORM (which must return a JSON string) in the serving JVM."""
    r = w.jvm_eval(form)
    m = re.search(r':value "(.*)"\}\s*$', r["out"], re.S)
    if not m:
        raise RuntimeError(f"jvm eval failed: {r['out'][-600:]} {r['err']}")
    return json.loads(json.loads('"' + m.group(1) + '"'))


MISSIONS_FORM = r"""
(do (require '[futon2.aif.mission-registry :as reg] '[cheshire.core :as json])
    (json/generate-string
     (for [m (:missions (reg/load-missions))]
       {:id (:id m) :path (:path m) :status-line (:status-line m)
        :status-class (some-> (:status-class m) name)})))
"""

CHECK_FORM = r"""
(do (require '[futon2.aif.mission-registry :as reg] '[futon2.aif.mission-hole-wants :as mhw]
             '[cheshire.core :as json])
    (let [ids (set %s)
          ms (filter #(ids (str (:id %%))) (:missions (reg/load-missions)))]
      (json/generate-string
       (for [m ms]
         {:id (str (:id m)) :status-line (:status-line m)
          :status-class (some-> (:status-class m) name)
          :holes (count (:want (mhw/mission-source reg/default-code-root m)))}))))
"""


def repo_of(path: str) -> Path:
    return Path(re.match(r"(.*/code/[^/]+)/", path).group(1))


def plan(_args) -> int:
    STATE.mkdir(parents=True, exist_ok=True)
    ms = edn_json(MISSIONS_FORM)
    by_repo: dict[str, list] = {}
    skipped = []
    for m in sorted(ms, key=lambda m: (m["path"] or "", m["id"])):
        if not m["path"] or not re.match(r".*/code/[^/]+/", m["path"]):
            skipped.append(m["id"])
            continue
        by_repo.setdefault(str(repo_of(m["path"])), []).append(m)
    chunks, n = [], 0
    for repo, rows in sorted(by_repo.items()):
        for i in range(0, len(rows), CHUNK_SIZE):
            n += 1
            chunks.append({"chunk": f"c{n:03d}", "repo": repo, "missions": rows[i:i + CHUNK_SIZE]})
    CHUNKS.write_text(json.dumps({"made": w.now(), "skipped": skipped, "chunks": chunks}, indent=1))
    print(f"{len(ms)} missions -> {len(chunks)} chunks; skipped (no repo path): {skipped}")
    return 0


GUIDE = """MISSION DISPOSITION AND ACCEPTANCE CHECKBOXES

Joe (2026-09-30): the War Machine should have every open mission to choose
from. It can only work on a mission whose file (a) says clearly whether it
is open, and (b) states acceptance as checkboxes it can check. Your job, for
EACH mission file listed below:

1. READ the whole mission file (and, briefly, what it cites if you need to
   judge whether its work is done).

2. DISPOSITION. Joe's rule: a mission is OPEN unless its file says it is
   finished, closed, archived, cancelled, superseded or abandoned, or every
   acceptance condition it states is demonstrably met (cite the commit or
   file). Parked, deferred, draft, IDENTIFY, "in flight": all OPEN. When
   unsure, OPEN.
   Write the disposition as the FIRST line in the file that starts with a
   Status field, at the START of a line, in exactly this form:
     **Status:** OPEN — <the old status text, kept verbatim>
     **Status:** CLOSED — <why: the old status text, or the evidence>
   (SUPERSEDED or ABANDONED instead of CLOSED where that is the fact.)
   If the file already has a Status line, rewrite that line in place (it
   must be the first Status line in the file); keep its old text after the
   dash, nothing lost. If it has none, add one right under the title.
   A Status written mid-line (`**Date:** ... · **Status:** ...`) does not
   count: split it so Status starts its own line.

3. ACCEPTANCE (OPEN missions only). Add a section
     ## Acceptance checklist (2026-09-30)
   with 2 to 6 items, each one line:
     - [ ] <one observable condition>
   Derive them from the mission's own stated goals, completion criteria and
   open work; do not invent new scope. Each item must be a condition a
   reviewer can check by looking, not an activity: a named file exists and
   contains X; a named test or command passes/prints Y; a record/endpoint
   shows Z; a named document section is written. BAD: "- [ ] Investigate
   caching". GOOD: "- [ ] `futon3c/test/.../cache_test.clj` exists and passes
   under `clojure -X:test`". If the file already has good unchecked
   checkboxes, you may keep them instead and add only what is missing.
   A condition that already holds: write it as `- [x] ... (evidence: <sha or
   path>)` so the machine is not sent to do finished work.
   Never delete or flip an existing `- [ ]` or `- [x]` line.

4. Touch ONLY the listed files. No code changes.

5. Commit, in the repo shown, with explicit paths (the checkout is shared;
   never `git commit -a`, never amend):
     git add <files>; git commit -m "missions: disposition + acceptance (<TAG>)" -- <files>
   The commit message must contain the tag <TAG>.

Your final message (it is read by a script and by claude-1): one line per
mission: `<id> OPEN n-items` or `<id> CLOSED <one-phrase reason>`, then the
commit sha. Do not bell anyone.
"""


def packet(chunk: dict) -> str:
    tag = f"mission-accept-{chunk['chunk']}"
    lines = [f"- {m['id']}: {m['path']}  (current status: {m['status-line']!r})" for m in chunk["missions"]]
    return (GUIDE.replace("<TAG>", tag)
            + f"\nREPO: {chunk['repo']}\nTAG: {tag}\nMISSIONS ({len(lines)}):\n" + "\n".join(lines) + "\n")


def dispatch(seat: str, prompt: str) -> str:
    r = w.sh(sys.executable, w.SEND, "--from", "mission-accept-loop", "--to", seat, "--kind", "bell",
             "--mode", "work", input=prompt, timeout=120)
    m = re.search(r'"job-id"\s*:\s*"([^"]+)"', r.stdout)
    if not m:
        raise RuntimeError(f"dispatch to {seat} failed: {r.stdout[-400:]} {r.stderr[-400:]}")
    return m.group(1)


def wait_job(job: str) -> dict:
    start = time.time()
    while time.time() - start < JOB_TIMEOUT:
        try:
            j = w.get_json(f"/api/alpha/invoke/jobs/{job}").get("job", {})
            if j.get("state") in w.TERMINAL:
                return j
        except Exception as e:
            log(f"job {job} status: {e}")
        time.sleep(45)
    return {"state": "timeout"}


def box_lines(text: str) -> list[str]:
    return [l.strip() for l in text.splitlines() if re.match(r"^\s*[-*]\s+\[[ xX]\]\s", l)]


def check(chunk: dict, before_class: dict) -> dict:
    repo = Path(chunk["repo"])
    tag = f"mission-accept-{chunk['chunk']}"
    shas = w.git(repo, "log", "--since=1 day ago", "-F", f"--grep={tag}", "--format=%H").split()
    problems, closed_live = [], []
    if not shas:
        return {"ok": False, "problems": ["no commit carries the tag"], "shas": []}
    paths = {str(Path(m["path"]).relative_to(repo)) for m in chunk["missions"]}
    for sha in shas:
        touched = set(w.git(repo, "show", "--name-only", "--format=", sha).split())
        if touched - paths:
            problems.append(f"{sha[:9]} touches files outside the chunk: {sorted(touched - paths)}")
    base = shas[-1] + "^"
    for p in sorted(paths):
        old = w.git(repo, "show", f"{base}:{p}")
        new = (repo / p).read_text(errors="replace")
        old_boxes, new_boxes = box_lines(old), set(box_lines(new))
        lost = [b for b in old_boxes if b not in new_boxes]
        if lost:
            problems.append(f"{p}: {len(lost)} existing checkbox line(s) removed or flipped, e.g. {lost[0][:90]!r}")
    ids = [m["id"] for m in chunk["missions"]]
    rows = edn_json(CHECK_FORM % ("[" + " ".join(json.dumps(i) for i in ids) + "]"))
    seen = {r["id"]: r for r in rows}
    for i in ids:
        r = seen.get(i)
        if not r:
            problems.append(f"{i}: no longer enumerated as a mission")
            continue
        cls = r["status-class"]
        if cls == "open":
            if r["holes"] < 1:
                problems.append(f"{i}: OPEN but yields 0 checkbox holes")
        elif cls in ("complete", "inactive"):
            if before_class.get(i) not in ("complete", "inactive"):
                closed_live.append(f"{i}: {r['status-line'][:120]}")
        else:
            problems.append(f"{i}: status classifies {cls!r} (line: {str(r['status-line'])[:90]!r})")
    return {"ok": not problems, "problems": problems, "closed-live": closed_live,
            "shas": shas, "open": sum(1 for r in rows if r["status-class"] == "open"),
            "holes": sum(r["holes"] for r in rows if r["status-class"] == "open")}


def done_chunks() -> set[str]:
    if not LEDGER.exists():
        return set()
    return {json.loads(l)["chunk"] for l in LEDGER.read_text().splitlines()
            if l.strip() and json.loads(l).get("verdict") == "pass"}


def do_chunk(seat: str, chunk: dict) -> None:
    before = {m["id"]: m["status-class"] for m in chunk["missions"]}
    prompt = packet(chunk)
    for attempt in (1, 2):
        job = dispatch(seat, prompt)
        log(f"{chunk['chunk']} -> {seat} job {job} (attempt {attempt}, {len(chunk['missions'])} missions)")
        j = wait_job(job)
        try:
            res = check(chunk, before)
        except Exception as e:
            res = {"ok": False, "problems": [f"checker error: {e}"], "shas": []}
        verdict = "pass" if res["ok"] else "fail"
        record({"chunk": chunk["chunk"], "seat": seat, "job": job, "attempt": attempt,
                "job-state": j.get("state"), "verdict": verdict, **res})
        log(f"{chunk['chunk']} {verdict}: open={res.get('open')} holes={res.get('holes')} "
            f"closed-live={len(res.get('closed-live', []))} problems={res['problems'][:3]}")
        if res["ok"]:
            return
        prompt = (packet(chunk) + "\nA CHECK FAILED ON YOUR PREVIOUS ATTEMPT. Fix exactly these, "
                  "in a new commit carrying the same tag:\n" + "\n".join(f"- {p}" for p in res["problems"]))


def run(args) -> int:
    STATE.mkdir(parents=True, exist_ok=True)
    chunks = json.loads(CHUNKS.read_text())["chunks"]
    todo = [c for c in chunks if c["chunk"] not in done_chunks()
            and (not args.only or c["chunk"] in args.only.split(","))]
    seats = args.seats.split(",")
    log(f"run: {len(todo)} chunks over {seats}")
    lock = threading.Lock()

    def worker(seat: str) -> None:
        while not STOP_FILE.exists():
            with lock:
                if not todo:
                    return
                c = todo.pop(0)
            try:
                do_chunk(seat, c)
            except Exception as e:
                log(f"{c['chunk']} on {seat}: crashed: {e}")
                record({"chunk": c["chunk"], "seat": seat, "verdict": "crash", "error": str(e)})

    ts = [threading.Thread(target=worker, args=(s,)) for s in seats]
    for t in ts:
        t.start()
    for t in ts:
        t.join()
    log("run: finished" + (" (STOP file)" if STOP_FILE.exists() else ""))
    return 0


def status(_args) -> int:
    chunks = json.loads(CHUNKS.read_text())["chunks"] if CHUNKS.exists() else []
    rows = [json.loads(l) for l in LEDGER.read_text().splitlines()] if LEDGER.exists() else []
    last = {r["chunk"]: r for r in rows}
    passed = [r for r in last.values() if r.get("verdict") == "pass"]
    print(f"chunks {len(chunks)}  passed {len(passed)}  failed/crashed "
          f"{sum(1 for r in last.values() if r.get('verdict') != 'pass')}  "
          f"open missions {sum(r.get('open', 0) for r in passed)}  "
          f"holes {sum(r.get('holes', 0) for r in passed)}")
    for r in last.values():
        if r.get("verdict") != "pass" or r.get("closed-live"):
            print(r["chunk"], r.get("verdict"), r.get("problems", [])[:2], r.get("closed-live", [])[:3])
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("plan")
    r = sub.add_parser("run")
    r.add_argument("--seats", default="codex-1")
    r.add_argument("--only", default="")
    sub.add_parser("status")
    a = ap.parse_args()
    return {"plan": plan, "run": run, "status": status}[a.cmd](a)


if __name__ == "__main__":
    sys.exit(main())
