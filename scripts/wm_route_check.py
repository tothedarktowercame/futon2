#!/usr/bin/env python3
"""Certify one click's self-heal route against the Lean L1 table (unit L2a).

Reads a click binding (and the run record it names), projects the recorded
fields onto `DarkTower.WarMachine.SelfHealRoute.RouteState`, has Lean decide
`conformsPath` / `conformsSuffix`, and writes `<binding>.route.edn`.  A state
the record does not establish is a typed gap, never a guess; a Lean run that
prints no verdict is `:not-checked`, never a default verdict."""
import argparse, datetime, hashlib, json, os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

LEAN_MODULE = "DarkTower/WarMachine/SelfHealRoute.lean"
ROUTE_ONLY = {"acceptance-test", "diagnosis-evidence"}        # only route! emits these
READINESS_ONLY = {"diagnosis-port", "acceptance-proposal-port", "acceptance-review-port"}
BINDING_MISMATCH = {"meta-self-heal/diagnosis-transaction-mismatch", "meta-self-heal/acceptance-transaction-mismatch"}

# Acceptance observation -> recorded prefix.  Source field:
# record [:meta-self-heal/readiness :acceptance-observation]; reasons from
# meta_self_heal_production.clj `prepare-acceptance` / `admit-acceptance!`.
# Any other observation leaves the prefix :not-recorded.
# `:acceptance-review-refused` carries `:review-status` from the review invoke's
# response (meta_self_heal_production.clj P:628 at futon2 9b97bb99c).  `:refused` is the
# reviewer's own answer; `:failed` is a transport failure (`:producer-invoke-failed`,
# adapters A:233), so there is no reviewer answer and the state is admissionRefused
# ("refused before or without a reviewer answer"), per SelfHealRoute rev 4
# `BoundaryDefect.state` (mathlib4 2e33839f88, `checkerPlacement_isViolation`).  Any
# other `:review-status` is left as a gap, never guessed.
ACCEPTANCE_PREFIX = [
  (lambda o: o.get("status") == "refused" and o.get("reason") == "meta-self-heal/acceptance-review-refused"
             and o.get("review-status") == "refused",
   ["activeInjury", "diagnosisPrepared", "admissionRun", "reviewRefused"],
   "the reviewer answered :refused (:review-status :refused), which exists only after admit-acceptance!, which runs only on an :available diagnosis"),
  (lambda o: o.get("status") == "refused" and o.get("reason") == "meta-self-heal/acceptance-review-refused"
             and o.get("review-status") == "failed",
   ["activeInjury", "diagnosisPrepared", "admissionRun", "admissionRefused"],
   "the review invoke failed in transport (:review-status :failed), so there is no reviewer answer: admissionRefused, not reviewRefused (SelfHealRoute rev 4, AdmissionDefect.noReviewerAnswer)"),
  (lambda o: o.get("status") == "accepted",
   ["activeInjury", "diagnosisPrepared", "admissionRun", "reviewAccepted"],
   "an :accepted observation is returned only by admit-acceptance!"),
  (lambda o: o.get("reason") == "meta-self-heal/acceptance-diagnosis-unavailable",
   ["activeInjury", "diagnosisRefused", "acceptanceUnavailable"],
   "prepare-acceptance returns this reason only when the diagnosis is not :available"),
  (lambda o: o.get("reason") == "meta-self-heal/acceptance-unavailable",
   ["activeInjury", "diagnosisPrepared", "proposalRefused"],
   "produce-acceptance! did not return :proposed (Lean click-4 witness)"),
  # M5 (2026-10-20): the proposal and run refusals the prefix lacked.  Each has a
  # ProposalDefect / AdmissionDefect constructor in SelfHealRoute rev 4 (L381-422).
  (lambda o: o.get("reason") in ("meta-self-heal/acceptance-prose-or-malformed",
                                 "meta-self-heal/acceptance-proposal-source-mismatch",
                                 "meta-self-heal/acceptance-request-function-unresolvable"),
   ["activeInjury", "diagnosisPrepared", "proposalRefused"],
   "produce-acceptance! refused the proposal itself (P:467, P:470, P:526): proposalRefused"),
  (lambda o: o.get("reason") in ("meta-self-heal/acceptance-run-refused",
                                 "meta-self-heal/acceptance-test-passes-before-repair"),
   ["activeInjury", "diagnosisPrepared", "admissionRun", "admissionRefused"],
   "admit-acceptance! refused before any reviewer invoke (P:618, P:622): admissionRefused"),
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

# M3a (2026-10-20): a binding that names no run record still has an acceptance
# observation, in the click-state record runner_service.clj `result-summary` writes and
# `persist-click-state!` completes (futon3c data/wm-click-acceptances, gitignored).  Only
# these observation fields are read; the record's :request (the test source) and
# :review-detail never reach a sidecar.
ACCEPTANCE_DIR = "/home/joe/code/futon3c/data/wm-click-acceptances"
OBSERVATION_KEYS = ["status", "reason", "review-status", "review-reason",
                    "acceptance-status", "acceptance-reason", "diagnosis-status", "acceptance-result"]
CLICK_STATE_SOURCE = [":wm/click-state", ":result", ":runtime-disposition", ":acceptance-observation"]
# The states the table places after every acceptance observation (SelfHealRoute
# `transitions`, "readiness after every acceptance observation").  A suffix that starts
# elsewhere leaves readiness unrecorded between the observation and the suffix.
AFTER_OBSERVATION = {"readinessReady", "readinessRefused", "ledgerUnreadable"}

def acceptance_file(directory, click_id):
  """runner_service.clj `acceptance-file`: click-<id>.edn, id sanitised."""
  return Path(directory)/("click-" + re.sub(r"[^A-Za-z0-9._-]", "_", str(click_id)) + ".edn")

def read_click_state(path):
  """{:exists? false}, or the record's top-level :status and its observation's selected
  keys (None when the record has no observation at that path)."""
  path = Path(path)
  if not path.is_file(): return {"exists": False, "path": str(path)}
  ks = " ".join(":" + k for k in OBSERVATION_KEYS)
  form = ("(require '[cheshire.core :as j])"
          f"(let [d (clojure.edn/read-string {{:default (fn [_ v] v)}} (slurp {json.dumps(str(path))}))"
          " o (get-in d [:result :runtime-disposition :acceptance-observation])"
          # M7L (2026-10-20): the bounded run of a review refusal sits under the review
          # request admit-acceptance! attaches (:request :result); three keys of it are
          # read, never the request (it carries :acceptance-test).
          " r (get-in o [:request :result])"
          f" obs (when (map? o) (cond-> (select-keys o [{ks}]) (map? r) (assoc :acceptance-run (select-keys r [:status :outcome :exit]))))]"
          " (println (j/generate-string {:status (:status d) :observation obs})))")
  got = json.loads(subprocess.run(["bb", "-e", form], text=True, capture_output=True, check=True).stdout)
  return {"exists": True, "path": str(path.resolve()), "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
          "status": got.get("status"), "observation": got.get("observation")}

def acceptance_prefix(state):
  """(prefix, gap, note) for a click-state read.  Exactly one of prefix or gap is set."""
  if state is None: return None, {"position": 0, "status": "not-recorded"}, None
  if not state["exists"]: return None, {"position": 0, "status": "acceptance-record-absent"}, None
  if state.get("status") != "completed":
    return None, {"position": 0, "status": "acceptance-record-not-completed", "click-status": state.get("status")}, None
  obs = state.get("observation")
  if obs is None: return None, {"position": 0, "status": "not-recorded"}, None
  rule = next((r for r in ACCEPTANCE_PREFIX if r[0](obs)), None)
  if rule is None: return None, {"position": 0, "status": "acceptance-observation-not-projectable"}, None
  return rule[1], None, {"position": 0, "source": CLICK_STATE_SOURCE, "note": rule[2]}

# M5 (2026-10-20): the defect beside the state, from the observation's sub-reason fields
# (meta_self_heal_production.clj P:460 :acceptance-status/:acceptance-reason, P:628
# :review-status/:review-reason, at futon2 c0b9a794b; the producer's three transport
# answers are futon3c meta_self_heal_adapters.clj L227-240).  This table is read
# independently of ACCEPTANCE_PREFIX, never derived from it: the kernel's `placed`
# (SelfHealRoute rev 4, BoundaryObservation.placed) is only a check if the state and
# the defect are two readings.  An unknown sub-reason is a gap, never guessed; note the
# asymmetry on purpose: the state rule for :review-status :failed takes any
# :review-reason (M2b), the defect rule does not.
TRANSPORT = ("meta-self-heal/producer-invoke-failed", "meta-self-heal/producer-seat-unavailable")
NO_DEFECT_BY_CONSTRUCTION = ("meta-self-heal/acceptance-diagnosis-unavailable",)

def boundary_observation(obs, prefix):
  """(state, defect, run, gap): the boundary record for the kernel.  STATE is the last
  state of the placed PREFIX; DEFECT a Lean term or None; RUN a RunOutcome term or None;
  GAP set (and defect None) when the sub-reason is not in the rev 4 vocabulary."""
  reason, ar, rr = obs.get("reason"), obs.get("acceptance-reason"), obs.get("review-reason")
  state = prefix[-1]
  defect = None
  if reason == "meta-self-heal/acceptance-unavailable" and obs.get("acceptance-status") == "failed":
    defect = ".proposal .transport" if ar in TRANSPORT else (".proposal .unreadableAnswer" if ar == "meta-self-heal/producer-response-malformed" else None)
  elif reason == "meta-self-heal/acceptance-unavailable" and obs.get("acceptance-status") == "refused": defect = ".proposal .authorRefused"
  elif reason == "meta-self-heal/acceptance-prose-or-malformed": defect = ".proposal .prose"
  elif reason == "meta-self-heal/acceptance-proposal-source-mismatch": defect = ".proposal .sourceMismatch"
  elif reason == "meta-self-heal/acceptance-request-function-unresolvable": defect = ".proposal .unresolvableFunction"
  elif reason == "meta-self-heal/acceptance-run-refused": defect = ".admission .runRefused"
  elif reason == "meta-self-heal/acceptance-test-passes-before-repair": defect = ".admission .passesBeforeRepair"
  elif reason == "meta-self-heal/acceptance-review-refused" and obs.get("review-status") == "failed":
    defect = ".admission .noReviewerAnswer" if rr in TRANSPORT + ("meta-self-heal/producer-response-malformed", "meta-self-heal/acceptance-review-threw") else None
  elif reason == "meta-self-heal/acceptance-review-refused" and obs.get("review-status") == "refused": defect = ".admission .reviewerRefused"
  # :acceptance-result (run refusals, P:618/622), or the review request's run (M7L; the
  # click-state reader lifts it to :acceptance-run, a run record lifts it here).
  runv = obs.get("acceptance-result") or obs.get("acceptance-run") or ((obs.get("request") or {}).get("result")) or {}
  run = "." + runv["outcome"] if runv.get("status") == "ran" and runv.get("outcome") in ("passed", "failed", "errored") else None
  if defect is None and not (obs.get("status") == "accepted" or reason in NO_DEFECT_BY_CONSTRUCTION):
    gap = {"position": 0, "status": "defect-not-projectable", "reason": reason}
    gap.update({k: obs[k] for k in ("acceptance-status", "acceptance-reason", "review-status", "review-reason") if obs.get(k) is not None})
    return state, None, run, gap
  return state, defect, run, None

def project(binding, record, click_state=None):
  """Return (path, gaps, notes, reason).  reason is set when no path is projectable.
  CLICK_STATE is consulted only when RECORD is None (see `join_acceptance`)."""
  if record is None and click_state is not None:
    return join_acceptance(project(binding, None), click_state)
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
  # SelfHealRoute rev 6 (mathlib4 494d0f8e6a): the route starts at the one ledger read, and an
  # uncomputable active set ends the click there (M6a makes run-opportunity! return this reason).
  # The read is the start, so this is a full path, not a suffix.
  if d.get("reason") == "meta-self-heal/active-set-unavailable": return ["ledgerRead", "activeSetUnavailable", "terminalRecord"], [], notes, None
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

def join_acceptance(projected, click_state):
  """Prepend a recorded observation's prefix to a disposition suffix, or type the gap."""
  path, gaps, notes, reason = projected
  # Pre-F7 bindings carry no disposition: still record that the observation was read.
  if gaps != [{"position": 0, "status": "not-recorded"}] and reason != "no-durable-disposition": return projected
  prefix, gap, note = acceptance_prefix(click_state)
  notes = ([note] if note else []) + notes
  if prefix is None: return path, [gap], notes, reason
  if reason or not path: return path, [], notes, reason
  if path[0] in AFTER_OBSERVATION: return prefix + path, [], notes, None
  return path, [{"position": 0, "status": "readiness-not-recorded"}], notes, None

def lean_source(path, suffix, boundary=None):
  lst = "[" + ", ".join(path) + "]"
  lines = ["import DarkTower.WarMachine.SelfHealRoute", "open DarkTower.WarMachine.SelfHealRoute RouteState",
           f'#eval IO.println s!"VERDICT {{{"conformsSuffix" if suffix else "conformsPath"} {lst}}}"']
  lines += [f'#eval IO.println s!"EDGE {i} {{edge {a} {b}}}"' for i, (a, b) in enumerate(zip(path, path[1:]))]
  if boundary:
    state, defect, run = boundary
    lines += [f'#eval IO.println s!"PLACED {{({{ state := {state}, defect := {"some (" + defect + ")" if defect else "none"}, run := {"some " + run if run else "none"} }} : BoundaryObservation).placed}}"']
  return "\n".join(lines) + "\n"

def judge(path, suffix, stdout, returncode, placement=False):
  if returncode != 0: return {"verdict": "not-checked", "reason": f"lean-exit-{returncode}"}
  found = re.findall(r"^VERDICT (true|false)$", stdout, re.M)
  if len(found) != 1: return {"verdict": "not-checked", "reason": "lean-printed-no-verdict"}
  placed = re.findall(r"^PLACED (true|false)$", stdout, re.M)
  placed = (placed[0] == "true") if len(placed) == 1 else None
  if found[0] == "true":
    if placement and placed is None: return {"verdict": "not-checked", "reason": "lean-printed-no-placement"}
    if placement and placed is False: return {"verdict": "violates", "reason": "placement", "placed": False}
    return {"verdict": "conforms-suffix" if suffix else "conforms", **({"placed": placed} if placement else {})}
  edges = dict((int(i), v) for i, v in re.findall(r"^EDGE (\d+) (true|false)$", stdout, re.M))
  out = {"verdict": "violates", **({"placed": placed} if placement and placed is not None else {})}
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
  p.add_argument("--acceptance-dir", default=ACCEPTANCE_DIR, help="click-state records (click-<id>.edn)")
  a = p.parse_args(argv); binding_path = Path(a.binding); ml = Path(a.mathlib_root)
  out_path = Path(str(binding_path) + ".route.edn")
  started = datetime.datetime.now(datetime.timezone.utc).isoformat()
  binding = read_edn(binding_path, [":click/id", ":run-record", ":runtime-disposition"])
  record = read_edn(binding["run-record"], [":meta-self-heal/readiness", ":failure"]) if binding.get("run-record") else None
  click_state = None if record is not None else read_click_state(acceptance_file(a.acceptance_dir, binding.get("click/id")))
  path, gaps, notes, reason = project(binding, record, click_state)
  boundary = None
  obs = click_state.get("observation") if click_state else ((record or {}).get("meta-self-heal/readiness") or {}).get("acceptance-observation")
  prefix = next((r[1] for r in ACCEPTANCE_PREFIX if r[0](obs)), None) if obs else None
  if prefix and path and path[:len(prefix)] == prefix:
    state, defect, run, boundary_gap = boundary_observation(obs, prefix)
    if boundary_gap: gaps.append(boundary_gap)
    else: boundary = (state, defect, run)
  suffix = any(g["position"] == 0 for g in gaps)
  if reason or not path:
    result = {"verdict": "not-checked", "reason": reason or "empty-path"}
  elif a.lean_output is not None:
    result = judge(path, suffix, Path(a.lean_output).read_text(), 0, bool(boundary))
  else:
    with tempfile.TemporaryDirectory(prefix="wm-route-") as td:
      lean = Path(td)/"RouteCheck.lean"; lean.write_text(lean_source(path, suffix, boundary))
      try: ran = subprocess.run([lake_executable(), "env", "lean", str(lean)], cwd=ml, text=True, capture_output=True)
      except Exception as e: ran = None; result = {"verdict": "not-checked", "reason": f"lean-unavailable: {type(e).__name__}"}
      if ran is not None: result = judge(path, suffix, ran.stdout, ran.returncode, bool(boundary))
  try: shas = {":mathlib-head": git(ml, "rev-parse", "HEAD"), ":self-heal-route": git(ml, "hash-object", LEAN_MODULE)}
  except Exception as e: shas = {":unavailable": type(e).__name__}
  out = {":click/id": binding.get("click/id"), ":path": [kw(s) for s in path],
         ":gaps": [{kw(k): (kw(v) if k in ("status", "click-status") and isinstance(v, str) else v) for k, v in g.items()} for g in gaps],
         ":notes": [{kw(k): v for k, v in n.items()} for n in notes],
         ":verdict": kw(result["verdict"]), ":checked-at": started, ":module-shas": shas,
         ":producer-sha": git(Path(__file__).resolve().parent, "hash-object", Path(__file__).resolve())}
  if click_state and click_state["exists"] and any(n.get("source") == CLICK_STATE_SOURCE for n in notes):
    out[":acceptance-record"] = {":path": click_state["path"], ":sha256": click_state["sha256"]}
  if "first-missing-edge" in result: out[":first-missing-edge"] = [kw(s) for s in result["first-missing-edge"]]
  if "reason" in result: out[":reason"] = result["reason"]
  if boundary:
    state, defect, run = boundary
    out[":boundary-observation"] = {":state": kw(state), ":defect": defect, ":run": run}
    placed = result.get("placed")
    out[":placement"] = kw("placed" if placed is True else "violates-placement" if placed is False else "not-checked")
    if placed is False: out[":placement-violation"] = {":state": kw(state), ":defect": defect}
  else:
    out[":placement"] = kw("not-evaluated")
  fd, tmp = tempfile.mkstemp(prefix=f".{out_path.name}.", suffix=".tmp", dir=out_path.parent); os.close(fd)
  Path(tmp).write_text(edn(out) + "\n"); os.replace(tmp, out_path)
  print(edn(out)); return 0 if result["verdict"] in ("conforms", "conforms-suffix") else 1

if __name__ == "__main__":
  sys.exit(main())
