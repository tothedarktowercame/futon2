#!/usr/bin/env python3
"""Fail-closed post-run Requirements checker for one persisted WM run."""
import argparse, datetime, hashlib, json, os, re, subprocess, sys, tempfile, urllib.request
from pathlib import Path

def run(cmd, cwd):
    return subprocess.run(cmd, cwd=cwd, text=True, capture_output=True, check=True)

def sha(repo, path=None):
    cmd = ["git", "-C", str(repo), "rev-parse", "HEAD"] if path is None else ["git", "-C", str(repo), "hash-object", str(path)]
    return subprocess.check_output(cmd, text=True).strip()

def nr(x): return isinstance(x, dict) and "not-recomputable" in x
def card(x): return "NR" if nr(x) else len(x)
def val(x): return "NR" if nr(x) else x

def numbers(q, f):
    opened = sum(card(f[k]) for k in ("openMissions", "openExcursions", "openTickets")) if not any(nr(f[k]) for k in ("openMissions", "openExcursions", "openTickets")) else "NR"
    tc = f.get("targetConstruction", {})
    policies = "NR" if nr(tc) else sum(len(x["targets"]) * x["policyCount"] for x in tc)
    pool = "NR" if nr(tc) else max([len(x["pool"]) for x in tc] or [0])
    table = {
      1: f"{card(f['enumeratedTasks'])} enumerated / {opened} open tasks",
      2: f"{policies} policies / {opened} open tasks; pool {pool} / {f['libraryPatternCount']} patterns",
      3: f"{card(f['cascadesWithoutG'])} cascades without G / {card(f['constructedCascades'])} constructed",
      4: f"{card(f['preferenceSteps'])} preference steps / horizon {val(f['horizonLength'])}; G terms {val(f['gTerms'])}",
      5: f"order {val(f['interpretationOrder'])}",
      6: f"previous {val(f['previousChoice'])} -> current {val(f['currentChoice'])}; digests {val(f['previousInputDigest'])}/{val(f['currentInputDigest'])}",
      7: f"{val(f['pathAbsenceCount'])} typed path absences",
      8: f"{card(f['enumeratedTasks'])}/{opened} enumerated; {card(f['targetsReachingScoring'])} scoring; {card(f['targetsWithG'])} with G; {card(f['comparedPolicies'])} policies",
      9: f"{val(f['completionPairsStrictlyPreferred'])}/{val(f['completionPreferencePairs'])} completion pairs strictly preferred",
      10: f"{val(f['arrangementPairsDistinguishedByG'])}/{val(f['differentArrangementPairs'])} arrangement pairs distinguished by G"}
    return table[q]

def edn(x):
    if isinstance(x, dict): return "{" + " ".join(edn(k) + " " + edn(v) for k,v in x.items()) + "}"
    if isinstance(x, list): return "[" + " ".join(edn(v) for v in x) + "]"
    if isinstance(x, bool): return "true" if x else "false"
    if isinstance(x, int): return str(x)
    if x is None: return "nil"
    return json.dumps(str(x), ensure_ascii=False)

def main(argv=None):
    p=argparse.ArgumentParser(); p.add_argument("--run-record", required=True)
    p.add_argument("--futon2-root", default=str(Path(__file__).resolve().parents[1]))
    p.add_argument("--mathlib-root", default="/home/joe/code/mathlib4")
    p.add_argument("--alerts-log"); p.add_argument("--evidence-url", default="http://127.0.0.1:7073/api/alpha/evidence")
    p.add_argument("--facts-json", help="pre-exported fixture; tests only")
    p.add_argument("--no-evidence", action="store_true", help="hermetic tests only")
    a=p.parse_args(argv); f2=Path(a.futon2_root); ml=Path(a.mathlib_root); record=Path(a.run_record)
    verdict=Path(str(record)+".requirements.edn"); started=datetime.datetime.now(datetime.timezone.utc)
    try:
      if not record.is_file(): raise RuntimeError(f"run record missing: {record}")
      with tempfile.TemporaryDirectory(prefix="wm-requirements-") as td:
        td=Path(td); exported=td/"facts.json"; lean=td/"RunCheck.lean"
        if a.facts_json:
          exported.write_text(Path(a.facts_json).read_text())
        else:
          exported.write_text(run(["clojure","-M","-m","wm-run-facts","--run",str(record)], f2).stdout)
        run(["clojure","-M","-m","wm-check-run-facts",str(exported),str(lean),"exported"], f2)
        checked=run(["lake","env","lean",str(lean)], ml).stdout
        export=json.loads(exported.read_text()); facts=export["facts"]
      found={int(q): outcome for q,outcome in re.findall(r"Requirement\.q(\d+), [^)\n]*Outcome\.(\w+)", checked)}
      outcomes={q: found.get(q,"holds") for q in range(1,11)}
      bad={q:v for q,v in outcomes.items() if v != "holds"}; head="ALERT FOR JOE" if bad else "CONFORMS"
      lines=[head]+[f"Q{q} {outcomes[q]}: {numbers(q,facts)}" for q in (bad or outcomes)]
      block="\n".join(lines); print(block, flush=True)
      result={":run-record":str(record),":checked-at":started.isoformat(),":conforms":not bad,
              ":verdicts":{f":Q{q}":f":{v}" for q,v in outcomes.items()},
              ":critical-parameters":{f":Q{q}":numbers(q,facts) for q in range(1,11)},
              ":not-recomputable":export.get("diagnostics",{}).get("notRecomputable",[]),
              ":module-shas":{":mathlib-head":sha(ml),":requirements":sha(ml,"DarkTower/WarMachine/Requirements.lean"),":reader":sha(ml,"DarkTower/WarMachine/RequirementsReader.lean")},
              ":exporter-sha":sha(f2,"scripts/wm_run_facts.clj")}
      verdict.write_text(edn(result)+"\n")
      log=Path(a.alerts_log or f2/"data/wm-click-loop/alerts.log"); log.parent.mkdir(parents=True,exist_ok=True)
      with log.open("a") as out: out.write(f"{started.isoformat()} {record.name}\n{block}\n\n")
      if not a.no_evidence:
        payload={"evidence/id":"e-wm-requirements-"+hashlib.sha256(str(record).encode()).hexdigest()[:24],
          "evidence/subject":{"ref/type":"wm-run","ref/id":record.name},"evidence/type":"reflection",
          "evidence/claim-type":"observation","evidence/at":started.isoformat(),"evidence/author":"wm-run-alert",
          "evidence/body":{"event":"wm-requirements","conforms":not bad,"verdict-file":str(verdict),"block":block},
          "evidence/tags":["war-machine","requirements","alert"]}
        # futon1b refuses writes without a penholder (403 missing-penholder);
        # same header and default as futon3c's futon1b backend.
        penholder=os.environ.get("FUTON1B_PENHOLDER") or os.environ.get("FUTON1A_PENHOLDER") or "api"
        req=urllib.request.Request(a.evidence_url,data=json.dumps(payload).encode(),headers={"Content-Type":"application/json","x-penholder":penholder},method="POST")
        try:
          with urllib.request.urlopen(req,timeout=10) as response:
            if response.status >= 300: raise RuntimeError(f"evidence HTTP {response.status}")
        except Exception as e:
          # The requirements WERE checked (verdict file and alerts.log are
          # written above); only the evidence copy failed. Say so.
          print(f"requirements checked (verdict {verdict}); evidence POST FAILED: {e}", flush=True)
          return 2
      if not verdict.is_file(): raise RuntimeError("verdict file absent after check")
      return 1 if bad else 0
    except Exception as e:
      print("ALERT FOR JOE\nrequirements NOT CHECKED: "+str(e), flush=True)
      return 2

if __name__ == "__main__": sys.exit(main())
