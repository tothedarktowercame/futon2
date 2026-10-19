#!/usr/bin/env python3
"""Certify one click's self-heal route against the Lean L1 table (unit L2a).

Reads a click binding (and the run record it names), projects the recorded
fields onto `DarkTower.WarMachine.SelfHealRoute.RouteState`, has Lean decide
`conformsPath` / `conformsSuffix`, and writes `<binding>.route.edn`.  A state
the record does not establish is a typed gap, never a guess; a Lean run that
prints no verdict is `:not-checked`, never a default verdict."""
import argparse, datetime, json, os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

LEAN_MODULE = "DarkTower/WarMachine/SelfHealRoute.lean"
ROUTE_ONLY = {"acceptance-test", "diagnosis-evidence"}        # only route! emits these
READINESS_ONLY = {"diagnosis-port", "acceptance-proposal-port", "acceptance-review-port"}
BINDING_MISMATCH = {"meta-self-heal/diagnosis-transaction-mismatch", "meta-self-heal/acceptance-transaction-mismatch"}

# Acceptance observation -> recorded prefix.  Source field:
# record [:meta-self-heal/readiness :acceptance-observation]; reasons from
# meta_self_heal_production.clj `prepare-acceptance` / `admit-acceptance!`.
# Any other observation leaves the prefix :not-recorded.
ACCEPTANCE_PREFIX = [
  (lambda o: o.get("status") == "refused" and o.get("reason") == "meta-self-heal/acceptance-review-refused"
             and o.get("review-status") is not None,
   ["activeInjury", "diagnosisPrepared", "admissionRun", "reviewRefused"],
   "a reviewer answer (:review-status) exists only after admit-acceptance!, which runs only on an :available diagnosis"),
  (lambda o: o.get("status") == "accepted",
   ["activeInjury", "diagnosisPrepared", "admissionRun", "reviewAccepted"],
   "an :accepted observation is returned only by admit-acceptance!"),
  (lambda o: o.get("reason") == "meta-self-heal/acceptance-diagnosis-unavailable",
   ["activeInjury", "diagnosisRefused", "acceptanceUnavailable"],
   "prepare-acceptance returns this reason only when the diagnosis is not :available"),
  (lambda o: o.get("reason") == "meta-self-heal/acceptance-unavailable",
   ["activeInjury", "diagnosisPrepared", "proposalRefused"],
   "produce-acceptance! did not return :proposed (Lean click-4 witness)"),
]

def lake_executable():
  found = shutil.which("lake")
  if found: return found
  elan = Path.home()/".elan"/"bin"/"lake"
  if elan.is_file() and os.access(elan, os.X_OK): return str(elan)
  raise RuntimeError("lake unavailable (checked PATH and ~/.elan/bin/lake)")

def git(repo, *args):
  return subprocess.check_output(["git", "-C", str(repo), *args], text=True).strip()

def read_edn(path, keys):
  """Selected top-level keys of an EDN file, as JSON (keywords become bare strings)."""
  form = ("(require '[cheshire.core :as j])"
          f"(println (j/generate-string (select-keys (clojure.edn/read-string {{:default (fn [_ v] v)}} (slurp {json.dumps(str(path))})) [{' '.join(keys)}])))")
  return json.loads(subprocess.run(["bb", "-e", form], text=True, capture_output=True, check=True).stdout)

def project(binding, record):
  """Return (path, gaps, notes, reason).  reason is set when no path is projectable."""
  notes = []
  if record is not None:
    readiness, failure = record.get("meta-self-heal/readiness"), record.get("failure") or {}
    path, gaps = [], []
    obs = (readiness or {}).get("acceptance-observation") or {}
    rule = next((r for r in ACCEPTANCE_PREFIX if r[0](obs)), None)
    if rule: path += rule[1]; notes.append({"position": 0, "source": [":meta-self-heal/readiness", ":acceptance-observation"], "note": rule[2]})
    else: gaps.append({"position": 0, "status": "not-recorded"})
    if readiness is not None:
      if readiness.get("status") == "absent":
        notes.append({"position": len(path), "source": [":meta-self-heal/readiness"],
                      "note": "F2a preflight observation, not a production-readiness result; readinessReady because the click spent a ration in selection, which a refused readiness prevents"})
      elif readiness.get("status") != "ready":
        return [], gaps, notes, "readiness-status-not-projectable"
      path.append("readinessReady")
    if failure.get("stage") == "selection-world" or (binding.get("runtime-disposition") or {}).get("ordinary-selection-called?") is True:
      path.append("ordinarySelection")
    else:
      return path, gaps, notes, "ordinary-selection-not-established"
    return path + ["terminalRecord"], gaps, notes, None
  d = binding.get("runtime-disposition")
  if not d: return [], [], notes, "no-durable-disposition"
  gaps = [{"position": 0, "status": "not-recorded"}]
  if d.get("ordinary-selection-called?") is True: return ["ordinarySelection", "terminalRecord"], gaps, notes, None
  if d.get("status") != "refused": return [], gaps, notes, "disposition-not-projectable"
  if d.get("reason") == "meta-self-heal/ledger-unreadable": return ["ledgerUnreadable", "terminalRecord"], gaps, notes, None
  # route! after readiness (SelfHealRoute rev 3, mathlib4 a84278e893): a refused
  # coordinator/prepare is its own refusal since futon2 df2209a33 (2026-10-19 20:38Z);
  # before that commit the same refusal was recorded as a binding mismatch (click
  # c78d0f27, jit/SelfHealPrepare.lean), so a mismatch reason is projected to
  # bindingRefused with that caveat as a note, never silently.
  if d.get("reason") == "meta-self-heal/preparation-refused": return ["readinessReady", "preparationRefused", "terminalRecord"], gaps, notes, None
  if d.get("reason") in BINDING_MISMATCH:
    notes.append({"position": 1, "source": [":runtime-disposition", ":reason"],
                  "note": "a binding mismatch is genuine only from futon2 df2209a33 (2026-10-19 20:38Z); a record with this reason from before then may be a masked preparation refusal (click c78d0f27, SelfHealPrepare.click_c78d0f27_masked)"})
    return ["readinessReady", "bindingRefused", "terminalRecord"], gaps, notes, None
  if d.get("reason") != "meta-self-heal/production-config-required": return [], gaps, notes, "disposition-not-projectable"
  terms = set(d.get("absent-terms") or [])
  route, ready = bool(terms & ROUTE_ONLY), bool(terms & READINESS_ONLY) or d.get("before-ration-spend?") is True
  if route and not ready: return ["readinessReady", "routeRefused", "terminalRecord"], gaps, notes, None
  if ready and not route: return ["readinessRefused", "terminalRecord"], gaps, notes, None
  return [], gaps, notes, "absent-terms-ambiguous-between-readiness-and-route"

def lean_source(path, suffix):
  lst = "[" + ", ".join(path) + "]"
  lines = ["import DarkTower.WarMachine.SelfHealRoute", "open DarkTower.WarMachine.SelfHealRoute RouteState",
           f'#eval IO.println s!"VERDICT {{{"conformsSuffix" if suffix else "conformsPath"} {lst}}}"']
  lines += [f'#eval IO.println s!"EDGE {i} {{edge {a} {b}}}"' for i, (a, b) in enumerate(zip(path, path[1:]))]
  return "\n".join(lines) + "\n"

def judge(path, suffix, stdout, returncode):
  if returncode != 0: return {"verdict": "not-checked", "reason": f"lean-exit-{returncode}"}
  found = re.findall(r"^VERDICT (true|false)$", stdout, re.M)
  if len(found) != 1: return {"verdict": "not-checked", "reason": "lean-printed-no-verdict"}
  if found[0] == "true": return {"verdict": "conforms-suffix" if suffix else "conforms"}
  edges = dict((int(i), v) for i, v in re.findall(r"^EDGE (\d+) (true|false)$", stdout, re.M))
  out = {"verdict": "violates"}
  missing = next((i for i in range(len(path) - 1) if edges.get(i) != "true"), None)
  if missing is not None and edges.get(missing) == "false": out["first-missing-edge"] = [path[missing], path[missing + 1]]
  elif missing is not None: out["reason"] = f"lean-printed-no-edge-{missing}"
  else: out["reason"] = "every-edge-present-path-not-complete"
  return out

def edn(x):
  if isinstance(x, dict): return "{" + ", ".join(edn(k) + " " + edn(v) for k, v in x.items()) + "}"
  if isinstance(x, list): return "[" + " ".join(edn(v) for v in x) + "]"
  if isinstance(x, bool): return "true" if x else "false"
  if isinstance(x, int): return str(x)
  if x is None: return "nil"
  if isinstance(x, str) and x.startswith(":"): return x
  return json.dumps(x, ensure_ascii=False)

def kw(s): return ":" + s

def main(argv=None):
  p = argparse.ArgumentParser(); p.add_argument("--binding", required=True)
  p.add_argument("--mathlib-root", default="/home/joe/code/mathlib4")
  p.add_argument("--lean-output", help="canned Lean stdout; hermetic tests only")
  a = p.parse_args(argv); binding_path = Path(a.binding); ml = Path(a.mathlib_root)
  out_path = Path(str(binding_path) + ".route.edn")
  started = datetime.datetime.now(datetime.timezone.utc).isoformat()
  binding = read_edn(binding_path, [":click/id", ":run-record", ":runtime-disposition"])
  record = read_edn(binding["run-record"], [":meta-self-heal/readiness", ":failure"]) if binding.get("run-record") else None
  path, gaps, notes, reason = project(binding, record)
  suffix = any(g.get("status") == "not-recorded" and g["position"] == 0 for g in gaps)
  if reason or not path:
    result = {"verdict": "not-checked", "reason": reason or "empty-path"}
  elif a.lean_output is not None:
    result = judge(path, suffix, Path(a.lean_output).read_text(), 0)
  else:
    with tempfile.TemporaryDirectory(prefix="wm-route-") as td:
      lean = Path(td)/"RouteCheck.lean"; lean.write_text(lean_source(path, suffix))
      try: ran = subprocess.run([lake_executable(), "env", "lean", str(lean)], cwd=ml, text=True, capture_output=True)
      except Exception as e: ran = None; result = {"verdict": "not-checked", "reason": f"lean-unavailable: {type(e).__name__}"}
      if ran is not None: result = judge(path, suffix, ran.stdout, ran.returncode)
  try: shas = {":mathlib-head": git(ml, "rev-parse", "HEAD"), ":self-heal-route": git(ml, "hash-object", LEAN_MODULE)}
  except Exception as e: shas = {":unavailable": type(e).__name__}
  out = {":click/id": binding.get("click/id"), ":path": [kw(s) for s in path],
         ":gaps": [{kw(k): (kw(v) if k == "status" else v) for k, v in g.items()} for g in gaps],
         ":notes": [{kw(k): v for k, v in n.items()} for n in notes],
         ":verdict": kw(result["verdict"]), ":checked-at": started, ":module-shas": shas,
         ":producer-sha": git(Path(__file__).resolve().parent, "hash-object", Path(__file__).resolve())}
  if "first-missing-edge" in result: out[":first-missing-edge"] = [kw(s) for s in result["first-missing-edge"]]
  if "reason" in result: out[":reason"] = result["reason"]
  fd, tmp = tempfile.mkstemp(prefix=f".{out_path.name}.", suffix=".tmp", dir=out_path.parent); os.close(fd)
  Path(tmp).write_text(edn(out) + "\n"); os.replace(tmp, out_path)
  print(edn(out)); return 0 if result["verdict"] in ("conforms", "conforms-suffix") else 1

if __name__ == "__main__":
  sys.exit(main())
