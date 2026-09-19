#!/usr/bin/env bash
# wm_click.sh -- preflight, then optionally run, one WM click.
#
# WHY A SCRIPT. On 2026-09-18 a click spent ~20 minutes writing five 91 MB
# files and then died on a renderer NPE without selecting anything. Everything
# that would have predicted that is now checkable in about a second, and the
# checks are easy to skip if they live in a checklist instead of a file.
#
#   scripts/wm_click.sh                 # PREFLIGHT ONLY. Default. Fires nothing.
#   scripts/wm_click.sh --run           # preflight, then fire if preflight passes
#   scripts/wm_click.sh --run --force   # fire even if a wire would halt (say why)
#   scripts/wm_click.sh --probe-seats   # also spend one turn per seat on a quota probe
#
# Casting defaults to the cohort charter's three worker seats. Override with
# --author / --reviewer / --repair-reviewer. They must be three DISTINCT seats.
set -uo pipefail

F2="$HOME/code/futon2"; F3C="$HOME/code/futon3c"
BASE="http://localhost:7070"
AUTHOR="codex-23"; REVIEWER="codex-22"; REPAIR="codex-24"
RUN=0; FORCE=0; PROBE=0

while [ $# -gt 0 ]; do
  case "$1" in
    --run) RUN=1;; --force) FORCE=1;; --probe-seats) PROBE=1;;
    --author) AUTHOR="$2"; shift;; --reviewer) REVIEWER="$2"; shift;;
    --repair-reviewer) REPAIR="$2"; shift;;
    -h|--help) sed -n '2,20p' "$0"; exit 0;;
    *) echo "wm_click: unknown argument $1" >&2; exit 2;;
  esac; shift
done

ok=1
say() { printf '  %-22s %s\n' "$1" "$2"; }
bad() { ok=0; printf '  %-22s %s\n' "$1" "$2"; }

echo "wm_click preflight"

# 1. Agency reachable.
code=$(curl -s -o /dev/null -m 10 -w '%{http_code}' "$BASE/api/alpha/agents")
[ "$code" = "200" ] && say "agency" "up" || { bad "agency" "HTTP $code -- nothing else can be checked"; echo; exit 1; }

# 2. Casting: three distinct seats, all on the roster.
if [ "$AUTHOR" = "$REVIEWER" ] || [ "$AUTHOR" = "$REPAIR" ] || [ "$REVIEWER" = "$REPAIR" ]; then
  bad "casting" "author/reviewer/repair-reviewer must be three DISTINCT seats"
else
  missing=""
  for a in "$AUTHOR" "$REVIEWER" "$REPAIR"; do
    curl -s -m 10 "$BASE/api/alpha/agents" | grep -q "\"$a\"" || missing="$missing $a"
  done
  [ -z "$missing" ] && say "casting" "$AUTHOR / $REVIEWER / $REPAIR on roster" \
                    || bad "casting" "not on roster:$missing"
fi

# 3. No click already in flight (the boundary is single-flight).
inflight=$(curl -s -m 15 "$BASE/api/alpha/wm/click" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get("running?"))' 2>/dev/null)
[ "$inflight" = "False" ] && say "in flight" "none" || bad "in flight" "a click is already running -- wait for it"

# 4. THE ONE THAT MATTERS: would any tripwire halt this click?
#    A witness stops the run (futon2 a8ac1615). Evaluating all 13 against a
#    real observation costs ~13s here; discovering it inside a click costs the
#    click. This also covers
#    committed-but-not-loaded, which T10 proves rather than suspects (b6da1420).
# The wires must be evaluated against the observation the runner actually
# builds, not an empty one. Evaluating {:tripwire/force? true} alone gives
# every cross-run wire (T6, T7, T8) a world with no :findings, so they report
# clear while checking nothing -- this script shipped with that bug and told
# zai-14 "13/13 clear" seconds before T8 halted its click on 2026-09-19.
# Building the real observation costs ~13s because it parses the whole repair
# store. That is the honest price of the check.
# NOTE (kept from the withdrawn --disable-wire route, reverted 2026-09-19 by
# zai-30 under zai-14's handoff; claude-4 stood the route down and is fixing
# T8 instead): if a preflight skip set is ever re-introduced here, it CANNOT
# travel by environment -- proof-eval evaluates this form inside the server
# JVM, whose env is not this shell's, so a var like WM_CLICK_SKIP_WIRES on the
# proof-eval command line never reaches the code (measured, ab5ca8fa). Inline
# any such literal into the generated form.
cat > /tmp/wm_click_wires.clj <<'CLJ'
(do (require 'futon2.aif.tripwire 'futon2.aif.repair-obligation)
    (let [cro (resolve 'futon2.aif.tripwire/cross-run-observation)
          evals @(resolve 'futon2.aif.tripwire/wire-evaluators)
          rcw (resolve 'futon2.aif.tripwire/repair-covered-witness?)
          root @(resolve 'futon2.aif.repair-obligation/default-root)
          obs (cro {:cohort? true}
                   {:phase :opportunity :transition :start
                    :opportunity-id "wm_click-preflight"
                    :trigger :duree-click-on-demand
                    :cohort? true :repair-root root})
          ;; A repair-covered witness at a pre-selection start does NOT halt
          ;; the fired click: observe! defers it so the repair can be
          ;; selected (8f7799b7). Preflight must classify with the SAME
          ;; predicate the runner will use, or it reports WOULD HALT for a
          ;; click that would run -- the mirror image of the 2026-09-19 bug
          ;; where preflight said 13/13 clear seconds before T8 halted.
          ;; Everything not repair-covered still halts.
          classified (for [[k f] evals
                           :let [w (try (f (assoc obs :tripwire/force? true))
                                        (catch Throwable e [{:kind :wire-threw}]))]
                           :when (seq w)]
                       [k (mapv :kind w)
                        (boolean (some #(rcw % obs) w))])]
      {:tripping (vec (for [[k kinds deferred?] classified :when (not deferred?)]
                        [k kinds]))
       :will-defer (vec (for [[k _ deferred?] classified :when deferred?] k))}))
CLJ
# proof-eval.sh reads its admin token from its own directory, so it must be
# invoked from there.
wires=$( (cd "$F3C" && ./scripts/proof-eval.sh -f /tmp/wm_click_wires.clj 2>&1) | tail -1)
case "$wires" in
  *":tripping []"*)
    say "tripwires" "13/13 clear";;
  *":tripping ["*)
    bad "tripwires" "WOULD HALT: $(echo "$wires" | sed 's/.*:tripping //; s/}}$//')";;
  *)
    # Third outcome, kept distinct on purpose. "The check could not run" is not
    # "a wire would halt", and reporting the two the same way is the defect
    # this script exists to keep out of clicks (guardrails.clj:118 does exactly
    # that with (catch Throwable _ false)).
    bad "tripwires" "CHECK DID NOT RUN -- wire status unknown: $wires";;
esac
# Repair-covered witnesses do not halt the click (observe! defers them so the
# repair can be selected). Report them so the operator knows the click will
# start inside a named repair obligation -- visible, never silent.
case "$wires" in
  *":will-defer []"*) :;;
  *":will-defer ["*)
    say "will defer to repair" "$(echo "$wires" | sed 's/.*:will-defer //; s/}$//') -- witness names open obligations; repair is selectable";;
  *)  :;;
esac

# 5. Seat quota (opt-in: costs one agent turn each).
if [ "$PROBE" = "1" ]; then
  for a in "$AUTHOR" "$REVIEWER" "$REPAIR"; do
    r=$(cd "$F3C" && timeout 200 python3 scripts/agency_send.py --from claude-4 --to "$a" \
          --kind whistle --timeout-ms 170000 <<'EOF' 2>&1 | tail -1
Quota probe only -- do not start any task, do not edit any file.
Reply with exactly one line: your model id, and whether you currently have
usage quota available to run a coding turn. Nothing else.
EOF
)
    case "$r" in *available*) say "quota $a" "ok";; *) bad "quota $a" "$r";; esac
  done
fi

echo
if [ "$ok" = "1" ]; then echo "preflight PASS"; else echo "preflight FAIL"; fi
if [ "$RUN" != "1" ]; then
  echo "(preflight only; pass --run to fire)"; exit $(( 1 - ok ))
fi
if [ "$ok" != "1" ] && [ "$FORCE" != "1" ]; then
  echo "refusing to fire. Fix the above, or pass --force and say in your report why."
  exit 1
fi

# ---------------------------------------------------------------- fire
RUNID="$(date -u +%Y-%m-%d)-$(uuidgen 2>/dev/null || date +%s)"
echo; echo "firing click, run-id $RUNID"
resp=$(curl -s -m 60 -X POST "$BASE/api/alpha/wm/click" -H 'Content-Type: application/json' \
  -d "{\"run-id\":\"$RUNID\",\"author\":\"$AUTHOR\",\"reviewer\":\"$REVIEWER\",\"repair-reviewer\":\"$REPAIR\",\"trigger\":\"duree-click-on-demand\"}")
echo "$resp"
clickid=$(echo "$resp" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("click-id",""))' 2>/dev/null)
[ -n "$clickid" ] || { echo "no click-id returned; not accepted"; exit 1; }

t0=$(date +%s)
while :; do
  sleep 20
  s=$(curl -s -m 20 "$BASE/api/alpha/wm/click")
  running=$(echo "$s" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("running?"))' 2>/dev/null)
  phase=$(echo "$s" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("phase"))' 2>/dev/null)
  echo "[$(( $(date +%s) - t0 ))s] running=$running phase=$phase"
  [ "$running" = "False" ] && break
  [ $(( $(date +%s) - t0 )) -gt 5400 ] && { echo "still running after 90 min; report this, do not re-fire"; exit 1; }
done

echo; echo "terminal state:"
curl -s -m 20 "$BASE/api/alpha/wm/click" | python3 -m json.tool | sed -n '1,24p'
echo; echo "newest repair finding, if the click opened one:"
ls -t "$F2"/data/wm-repair-obligations/findings/*.edn 2>/dev/null | head -1 | xargs -r ls -l
