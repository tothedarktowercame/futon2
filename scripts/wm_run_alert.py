#!/usr/bin/env python3
"""Fail-closed post-run Requirements checker for one persisted WM click.

A click records a work run and a selection run, each a RunFacts.  The click
conforms only when every requirement holds on both.  There is no one-run verdict.
"""
import argparse, datetime, hashlib, json, os, re, shutil, subprocess, sys, tempfile, urllib.error, urllib.request
from pathlib import Path

def requirement_numbers(ml):
    """The requirement list is Lean's constructor list, nothing held here."""
    text=(ml/"DarkTower/WarMachine/Requirements.lean").read_text()
    found=re.search(r"inductive\s+Requirement\s+where\s*((?:\|\s*q\d+\s*)+)", text)
    if not found: raise RuntimeError("could not read inductive Requirement from Requirements.lean")
    return tuple(int(q) for q in re.findall(r"q(\d+)", found.group(1)))

def run(cmd, cwd):
    return subprocess.run(cmd, cwd=cwd, text=True, capture_output=True, check=True)

def lake_executable():
    """Resolve Lean independently of an interactive shell's PATH."""
    found=shutil.which("lake")
    if found: return found
    elan=Path.home()/".elan"/"bin"/"lake"
    if elan.is_file() and os.access(elan, os.X_OK): return str(elan)
    raise RuntimeError("lake unavailable (checked PATH and ~/.elan/bin/lake)")

def sha(repo, path=None):
    cmd = ["git", "-C", str(repo), "rev-parse", "HEAD"] if path is None else ["git", "-C", str(repo), "hash-object", str(path)]
    return subprocess.check_output(cmd, text=True).strip()

def nr(x): return isinstance(x, dict) and "not-recomputable" in x
def card(x): return "NR" if nr(x) else len(x)
def val(x): return "NR" if nr(x) else x

def nr_reasons(f, *keys):
    return "; ".join(f"{k}: {f[k]['not-recomputable']}" for k in keys if nr(f.get(k)))

def construction_line(f):
    reasons=nr_reasons(f,"kind","targetConstruction","libraryPatternCount","constructorPatternCount",
                       "comparedPolicies","constructedCascades","enumeratedTasks","currentChoice")
    return reasons or (f"kind {f['kind']}; {card(f['targetConstruction'])} constructions; "
                       f"{f['constructorPatternCount']} pool patterns / {f['libraryPatternCount']} library; "
                       f"{card(f['comparedPolicies'])} compared / {card(f['constructedCascades'])} constructed")

def strict_minimum_line(f):
    reasons=nr_reasons(f,"policyG","abstentionG","policyPrediction","abstentionPrediction")
    return reasons or (f"{card(f['policyG'])} G / {card(f['comparedPolicies'])} compared; "
                       f"abstention G {f['abstentionG']}; {card(f['policyPrediction'])} prediction digests")

def provenance_name(x):
    return "observed" if isinstance(x, dict) else str(x).lower()

def path_line(f):
    """On a selection run, which part of the typed path Lean read."""
    count=f"{val(f['pathAbsenceCount'])} typed path absences"
    if f.get("kind") != "selection": return count
    path=f.get("selectionPath")
    if nr(path): return f"{count}; selectionPath: {path['not-recomputable']}"
    if not isinstance(path, dict): return f"{count}; selectionPath absent"
    parts=[f"{k} {provenance_name(path.get(k))}" for k in ("receipt","pickRelation","pickReach")]
    return f"{count}; {', '.join(parts)}, learningApplied {provenance_name(path.get('learningApplied'))}"

def numbers(q, f):
    opened = sum(card(f[k]) for k in ("openMissions", "openExcursions", "openTickets")) if not any(nr(f[k]) for k in ("openMissions", "openExcursions", "openTickets")) else "NR"
    table = {
      1: f"{card(f['enumeratedTasks'])} enumerated / {opened} open tasks",
      2: construction_line(f),
      3: f"{card(f['cascadesWithoutG'])} cascades without G / {card(f['constructedCascades'])} constructed",
      4: f"{card(f['preferenceSteps'])} preference steps / horizon {val(f['horizonLength'])}; G terms {val(f['gTerms'])}",
      5: f"order {val(f['interpretationOrder'])}",
      6: f"previous {val(f['previousChoice'])} -> current {val(f['currentChoice'])}; digests {val(f['previousInputDigest'])}/{val(f['currentInputDigest'])}",
      7: path_line(f),
      9: f"{val(f['completionPairsStrictlyPreferred'])}/{val(f['completionPreferencePairs'])} completion pairs strictly preferred",
      11: strict_minimum_line(f)}
    return table[q]

def edn(x):
    if isinstance(x, dict): return "{" + " ".join(edn(k) + " " + edn(v) for k,v in x.items()) + "}"
    if isinstance(x, list): return "[" + " ".join(edn(v) for v in x) + "]"
    if isinstance(x, bool): return "true" if x else "false"
    if isinstance(x, int): return str(x)
    if x is None: return "nil"
    return json.dumps(str(x), ensure_ascii=False)

def publication_failure(e):
    """A typed receipt for an evidence transport failure, never a Q verdict."""
    receipt={":status":":failed", ":error-class":type(e).__name__}
    if isinstance(e, urllib.error.HTTPError):
      receipt.update({":kind":":evidence-http-status", ":http-status":e.code})
    else:
      receipt[":kind"]=":evidence-publication-error"
    return receipt

def post_check_failure(stage, e):
    """Report a failure after evaluation without recasting Qs as uncomputed."""
    print(f"POST-CHECK {stage} FAILED: {type(e).__name__}", flush=True)
    return 3

def publication_diagnostic(e):
    """Keep transport diagnostics typed; arbitrary exception text can hold secrets."""
    if isinstance(e, urllib.error.HTTPError):
      return f"HTTPError status {e.code}"
    return type(e).__name__

def write_verdict(path, result):
    """Atomically replace a verdict only after the complete new bytes exist."""
    fd,tmp_name=tempfile.mkstemp(prefix=f".{path.name}.",suffix=".tmp",dir=path.parent)
    tmp=Path(tmp_name)
    try:
      os.close(fd)
      tmp.write_text(edn(result)+"\n")
      os.replace(tmp,path)
    except Exception:
      try: tmp.unlink()
      except FileNotFoundError: pass
      raise

def evaluate_run(kind, source, requirements, td, f2, ml, reader, record):
    """One run's export, Lean check and outcomes.  Any failure raises: a click is
    checked on both runs or not at all."""
    title=kind.capitalize(); exported=td/f"facts-{kind}.json"; lean=td/f"RunCheck{title}.lean"
    if source:
      exported.write_text(Path(source).read_text())
    else:
      export_cmd=["clojure","-M","-m","wm-run-facts","--run",str(record)]
      if kind != "work": export_cmd+=["--kind",kind]
      exported.write_text(run(export_cmd, f2).stdout)
    run(["clojure","-M","-m","wm-check-run-facts",str(exported),str(lean),kind,str(reader)], f2)
    checked=run([lake_executable(),"env","lean",str(lean)], ml).stdout
    export=json.loads(exported.read_text()); facts=export["facts"]
    if facts.get("kind") != kind:
      raise RuntimeError(f"{kind} export is labelled {facts.get('kind')!r}")
    found={int(q): outcome for q,outcome in re.findall(r"Requirement\.q(\d+),\s+[^)]*?Outcome\.(\w+)", checked)}
    outcomes={q: found.get(q,"not-checked") for q in requirements}
    return {"facts":facts,"outcomes":outcomes, "diagnostics":export.get("diagnostics",{}),
            "bad":{q:v for q,v in outcomes.items() if v != "holds"},
            "verdicts":{f":Q{q}":f":{v}" for q,v in outcomes.items()},
            "parameters":{f":Q{q}":numbers(q,facts) for q in requirements},
            "not-recomputable":export.get("diagnostics",{}).get("notRecomputable",[])}

def run_lines(title, checked):
    shown=checked["bad"] or checked["outcomes"]
    return [f"{title} run:"]+[f"Q{q} {checked['outcomes'][q]}: {numbers(q,checked['facts'])}" for q in shown]

def main(argv=None):
    p=argparse.ArgumentParser(); p.add_argument("--run-record", required=True)
    p.add_argument("--futon2-root", default=str(Path(__file__).resolve().parents[1]))
    p.add_argument("--mathlib-root", default="/home/joe/code/mathlib4")
    p.add_argument("--alerts-log"); p.add_argument("--evidence-url", default="http://127.0.0.1:7073/api/alpha/evidence")
    p.add_argument("--facts-json", help="pre-exported work fixture; tests only")
    p.add_argument("--selection-facts-json", help="pre-exported selection fixture; tests only")
    p.add_argument("--no-evidence", action="store_true", help="hermetic tests only")
    a=p.parse_args(argv); f2=Path(a.futon2_root); ml=Path(a.mathlib_root); record=Path(a.run_record)
    verdict=Path(str(record)+".requirements.edn"); started=datetime.datetime.now(datetime.timezone.utc)
    try:
      if not record.is_file(): raise RuntimeError(f"run record missing: {record}")
      requirements=requirement_numbers(ml)
      if bool(a.facts_json) != bool(a.selection_facts_json):
        raise RuntimeError("one fixture given: a click is checked on both runs")
      reader=ml/"DarkTower/WarMachine/RequirementsReader.lean"
      with tempfile.TemporaryDirectory(prefix="wm-requirements-") as td:
        td=Path(td)
        selection=evaluate_run("selection",a.selection_facts_json,requirements,td,f2,ml,reader,record)
        work_not_run=selection["diagnostics"].get("workRunStatus") == "not-run"
        work=({"facts":{},"bad":{},"verdicts":{},"parameters":{},"not-recomputable":[]}
              if work_not_run else evaluate_run("work",a.facts_json,requirements,td,f2,ml,reader,
                                Path(selection["diagnostics"].get("workRecord") or record)))
      work_scale=work["facts"].get("policyGScale"); selection_scale=selection["facts"].get("policyGScale")
      scale_differs=not work_not_run and work_scale != selection_scale
      conforms=not (work_not_run or work["bad"] or selection["bad"] or scale_differs)
      lines=["CONFORMS" if conforms else "ALERT FOR JOE"]+(["work run: not-run"] if work_not_run else run_lines("work",work))+run_lines("selection",selection)
      if scale_differs: lines.append(f"policyGScale differs: work {work_scale} / selection {selection_scale}")
      block="\n".join(lines); print(block, flush=True)
      result={":run-record":str(record),":checked-at":started.isoformat(),":conforms":conforms,
              ":runs":[":work",":selection"],
              ":work-status":":not-run" if work_not_run else ":evaluated",
              ":verdicts":work["verdicts"],
              ":critical-parameters":work["parameters"],
              ":not-recomputable":work["not-recomputable"],
              ":selection-verdicts":selection["verdicts"],
              ":selection-critical-parameters":selection["parameters"],
              ":selection-not-recomputable":selection["not-recomputable"],
              ":policy-g-scale":{":work":work_scale,":selection":selection_scale} if scale_differs else work_scale,
              ":module-shas":{":mathlib-head":sha(ml),":requirements":sha(ml,"DarkTower/WarMachine/Requirements.lean"),":reader":sha(ml,"DarkTower/WarMachine/RequirementsReader.lean")},
              ":exporter-sha":sha(f2,"scripts/wm_run_facts.clj")}
    except Exception as e:
      print("ALERT FOR JOE\nrequirements NOT CHECKED: "+str(e), flush=True)
      return 2

    result[":evidence-publication"]=( {":status":":skipped", ":reason":":no-evidence"}
                                      if a.no_evidence else {":status":":pending"})
    try:
      write_verdict(verdict,result)
      log=Path(a.alerts_log or f2/"data/wm-click-loop/alerts.log"); log.parent.mkdir(parents=True,exist_ok=True)
      with log.open("a") as out: out.write(f"{started.isoformat()} {record.name}\n{block}\n\n")
    except Exception as e:
      return post_check_failure("VERDICT-OR-ALERT-LOG", e)

    if not a.no_evidence:
      payload={"evidence/id":"e-wm-requirements-"+hashlib.sha256(str(record).encode()).hexdigest()[:24],
          "evidence/subject":{"ref/type":"wm-run","ref/id":record.name},"evidence/type":"reflection",
          "evidence/claim-type":"observation","evidence/at":started.isoformat(),"evidence/author":"wm-run-alert",
          "evidence/body":{"event":"wm-requirements","conforms":conforms,"verdict-file":str(verdict),"block":block},
          "evidence/tags":["war-machine","requirements","alert"]}
      req=urllib.request.Request(a.evidence_url,data=json.dumps(payload).encode(),
          headers={"Content-Type":"application/json","X-Penholder":"api"},method="POST")
      try:
        with urllib.request.urlopen(req,timeout=10) as response:
          if response.status >= 300: raise RuntimeError(f"evidence HTTP {response.status}")
        result[":evidence-publication"]={":status":":published"}
      except Exception as e:
        result[":evidence-publication"]=publication_failure(e)
        print("EVIDENCE PUBLICATION FAILED: "+publication_diagnostic(e), flush=True)
        try:
          write_verdict(verdict,result)
        except Exception as write_error:
          return post_check_failure("PUBLICATION-RECEIPT-PERSISTENCE", write_error)
        return 3
      try:
        write_verdict(verdict,result)
      except Exception as e:
        return post_check_failure("PUBLICATION-RECEIPT-PERSISTENCE", e)
    if not verdict.is_file():
      return post_check_failure("VERDICT-PERSISTENCE", RuntimeError())
    return 0 if conforms else 1

if __name__ == "__main__": sys.exit(main())
