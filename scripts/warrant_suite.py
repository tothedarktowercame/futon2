#!/usr/bin/env python3
"""Keep one test-registry warrant per futon2 test namespace.

Joe, 2026-09-21: mint warrants "so we never need to rerun all the tests when
files are not changing". The registry already records, per run, the load
closure the test JVM actually loaded, and `check` re-hashes that closure
without running anything. So a namespace needs a rerun only when a file in
its own recorded closure (or the environment) has changed.

For each namespace under test/:
  - index has a warrant and `check` says :warrant? true  -> fresh, skip;
  - otherwise                                           -> `run` it, which
    registers a new record (a warrant if the tests pass and inputs held).

Usage:
  scripts/warrant_suite.py              # check all, rerun only stale ones
  scripts/warrant_suite.py --dry-run    # report fresh/stale, run nothing
  scripts/warrant_suite.py -j 3 NS...   # limit to named namespaces

Index: data/test-warrants/index.json  {ns: {"entry-id", "git-head", ...}}.
Registry commands run from futon3c in their own JVMs (never the serving JVM).
"""
import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

REPO = Path("/home/joe/code/futon2")
FUTON3C = Path("/home/joe/code/futon3c")
INDEX = REPO / "data/test-warrants/index.json"
ARTIFACTS = Path("/home/joe/code/storage/test-registry/futon2-suite")
AUTHOR = os.environ.get("WARRANT_AUTHOR", "claude-3")


def namespaces():
    out = {}
    for f in sorted((REPO / "test").rglob("*_test.clj")):
        m = re.search(r"\(ns\s+([^\s()]+)", f.read_text(errors="replace"))
        if m:
            out[m.group(1)] = f.relative_to(REPO).as_posix()
    return out


def code_paths(test_path):
    # The declared code scope is exact-match checked, so keep it to the
    # namespace's own source file; everything else it loads is covered by
    # the recorded load closure.
    src = "src/" + test_path[len("test/"):].replace("_test.clj", ".clj")
    return [src] if (REPO / src).is_file() else [test_path]


def edn_str(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def edn_vec(xs):
    return "[" + " ".join(edn_str(x) for x in xs) + "]"


def registry(op, spec_edn):
    with tempfile.NamedTemporaryFile("w", suffix=".edn", delete=False) as fh:
        fh.write(spec_edn)
        path = fh.name
    try:
        p = subprocess.run(["clojure", "-M", "-m", "futon3c.test-registry", op, path],
                           cwd=FUTON3C, capture_output=True, text=True)
    finally:
        os.unlink(path)
    lines = [l for l in p.stdout.splitlines() if l.startswith("{")]
    try:
        return json.loads(lines[-1]) if lines else {"error": p.stderr[-2000:]}
    except json.JSONDecodeError:
        return {"error": lines[-1][-2000:]}


def git(*args):
    return subprocess.run(["git", *args], cwd=REPO, capture_output=True,
                          text=True).stdout.strip()


def check(entry):
    changed = [p for p in git("diff", "--name-only", entry["git-head"]).splitlines() if p]
    spec = ("{:entry-id %s :repo-root %s :changed-paths %s :output :json}"
            % (edn_str(entry["entry-id"]), edn_str(str(REPO)), edn_vec(changed)))
    return registry("check", spec)


def run(ns, test_path):
    stamp = time.strftime("%Y%m%dT%H%M%SZ", time.gmtime())
    spec = ("{:repo-root %s :code-paths %s :test-paths %s :command %s "
            ":author %s :subject-id %s :artifact-dir %s :output :json}"
            % (edn_str(str(REPO)), edn_vec(code_paths(test_path)), edn_vec([test_path]),
               edn_vec(["clojure", "-M:test", "-n", ns]), edn_str(AUTHOR),
               edn_str("futon2-suite/" + ns), edn_str(str(ARTIFACTS / ns / stamp))))
    head = git("rev-parse", "HEAD")
    result = registry("run", spec)
    entry_id = result.get("evidence/id") or result.get("id")
    payload = result.get("payload") or {}
    return {"entry-id": entry_id, "git-head": head,
            "warrant?": payload.get("warrant?"), "results": payload.get("results"),
            "error": result.get("error") or result.get("reason")}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-j", type=int, default=3)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("ns", nargs="*")
    a = ap.parse_args()
    nss = namespaces()
    if a.ns:
        nss = {k: v for k, v in nss.items() if k in a.ns}
    index = json.loads(INDEX.read_text()) if INDEX.exists() else {}

    def one(ns):
        entry = index.get(ns)
        if entry and entry.get("entry-id"):
            c = check(entry)
            if c.get("warrant?") is True:
                return ns, "fresh", entry
            if a.dry_run:
                return ns, "stale:" + str(c.get("reason")), entry
        elif a.dry_run:
            return ns, "stale:no-warrant", entry
        new = run(ns, nss[ns])
        return ns, ("minted" if new["warrant?"] else "failed"), new

    tally = {}
    with ThreadPoolExecutor(max_workers=a.j) as pool:
        for fut in as_completed([pool.submit(one, ns) for ns in nss]):
            ns, status, entry = fut.result()
            tally[status.split(":")[0]] = tally.get(status.split(":")[0], 0) + 1
            if status in ("minted", "failed"):
                index[ns] = entry
                INDEX.parent.mkdir(parents=True, exist_ok=True)
                INDEX.write_text(json.dumps(index, indent=1, sort_keys=True))
            print(status, ns, entry.get("entry-id") if entry else "",
                  entry.get("results") if entry and status == "failed" else "",
                  flush=True)
    print(json.dumps(tally), flush=True)
    return 1 if tally.get("failed") else 0


if __name__ == "__main__":
    sys.exit(main())
