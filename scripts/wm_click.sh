#!/usr/bin/env bash
# wm_click.sh -- preflight, then optionally run, one WM click.
#
# WHY A SCRIPT. On 2026-09-18 a click spent ~20 minutes writing five 91 MB
# files and then died on a renderer NPE without selecting anything. Everything
# that would have predicted that is now checkable in about a second, and the
# checks are easy to skip if they live in a checklist instead of a file.
#
# FIRING POLICY (PROOF-wm-works ⟨1⟩1 ⟨2⟩1/⟨2⟩2, Joe 2026-09-22): the machine
# runs when Joe says run. Agency must be reachable (exit 1 otherwise).
# Clean-start authority, 2026-10-24: --run
# prepares canonical source composition only while idle, and refuses dirty,
# unavailable or still-stale identity before POST (exit 3). No held-run recovery.
# Informational tripwire checks PRINT their findings and never stop
# firing. Busy cast seats are waited out, not refused:
# --run polls until the cast seats are free; an active click refuses source
# preparation rather than authorizing a reload. A wait longer than 30 minutes,
# or a casting misconfiguration that
# cannot be waited out, is a terminal account and exit 3 -- reported, never a
# silent refusal and never a success. The tripwires keep acting DURING a run
# exactly as before; source readiness is distinct from their findings.
#
#   scripts/wm_click.sh                 # PREFLIGHT ONLY. Default. Fires nothing.
#                                       Prints findings, exits 0 when Agency is up.
#   scripts/wm_click.sh --run           # print findings, wait for seats/single-flight, then fire
#   scripts/wm_click.sh --run --force   # --force is a NO-OP, accepted so existing
#                                       # callers do not break (firing no longer needs it)
#   scripts/wm_click.sh --run --probe-seats # also spend one turn per seat on a quota probe
#
# Optional: --ask-seats zai-4,zai-5,zai-6 names interpretation workers.
# Optional: --g-law canonical-coapply selects the canonical law on the reading-family route.
# Identify the issuer with --issuing-caller NAME or WM_ISSUING_CALLER.
# Omission is recorded as caller-unknown and does not block a click.
#
# Casting defaults to the cohort charter's three worker seats. Override with
# --author / --reviewer / --repair-reviewer. They must be three DISTINCT seats.
set -uo pipefail

F2="${F2:-$HOME/code/futon2}"; F3C="${F3C:-$HOME/code/futon3c}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BASE="${BASE:-http://localhost:7070}"
POST_TIMEOUT_S="${WM_CLICK_POST_TIMEOUT_S:-60}"
POST_RECOVERY_MAX_S="${WM_CLICK_POST_RECOVERY_MAX_S:-600}"
POST_RECOVERY_POLL_S="${WM_CLICK_POST_RECOVERY_POLL_S:-5}"
# codex-22 was the charter's reviewer and is no longer on the roster, so the
# default casting failed preflight on every invocation (claude-4, 2026-09-19).
# wm-author / wm-reviewer / wm-repair-reviewer are Codex seats on one shared
# account; on 2026-10-19 three of four clicks were lost to its 429s. Joe,
# 2026-10-19: "my suggestion would be to use zai runners in wm roles rather
# than codex". Defaults are now Zai seats (each answered a liveness whistle
# at 21:11Z). Override with --author / --reviewer / --repair-reviewer.
AUTHOR="zai-2"; REVIEWER="zai-3"; REPAIR="zai-8"
ISSUING_CALLER="${WM_ISSUING_CALLER:-caller-unknown}"
RUN=0; PROBE=0; G_LAW=""; ASK_SEATS=""

while [ $# -gt 0 ]; do
  case "$1" in
    --run) RUN=1;;
    --ask-seats) ASK_SEATS="$2"; shift;;
    --g-law) G_LAW="${2:-}"; [ "$G_LAW" = "canonical-coapply" ] || { echo "unsupported --g-law" >&2; exit 2; }; shift;;
    --force) :;; # no-op since firing never refuses; kept for old callers
    --probe-seats) PROBE=1;;
    --issuing-caller) ISSUING_CALLER="$2"; shift;;
    --author) AUTHOR="$2"; shift;; --reviewer) REVIEWER="$2"; shift;;
    --repair-reviewer) REPAIR="$2"; shift;;
    -h|--help) sed -n '2,28p' "$0"; exit 0;;
    *) echo "wm_click: unknown argument $1" >&2; exit 2;;
  esac; shift
done

say() { printf '  %-22s %s\n' "$1" "$2"; }
# A preflight finding: printed, never blocks firing (see FIRING POLICY above).
finding() { printf '  %-22s FINDING: %s\n' "$1" "$2"; }
# A terminal account: this launch cannot proceed and waiting cannot fix it.
# Reported as "cannot launch" (exit 3) -- not a refusal of a healthy click,
# not a success.
cannot() { printf '  %-22s CANNOT LAUNCH: %s\n' "$1" "$2"; }

echo "wm_click preflight"

# 1. Agency reachable. The one condition that stops everything.
code=$(curl -s -o /dev/null -m 10 -w '%{http_code}' "$BASE/api/alpha/agents")
[ "$code" = "200" ] && say "agency" "up" || { cannot "agency" "HTTP $code -- Agency unreachable, nothing else can be checked"; echo; exit 1; }

# The durable ration is knowable before a worker or readiness wake exists.
# Treat absence/exhaustion as a terminal launch account, not as a live run.
budget=$(curl -s -m 10 "$BASE/api/alpha/wm/click" | python3 -c '
import json,sys
d=json.load(sys.stdin).get("ordinary-click-budget") or {}
print(d.get("available", "unknown"))
' 2>/dev/null)
if [ "$budget" = "unknown" ]; then
  cannot "click budget" "serving process did not expose a durable availability receipt"
  echo; echo "cannot launch (exit 3)"; exit 3
elif [ "$budget" -le 0 ] 2>/dev/null; then
  cannot "click budget" "ordinary-click allocation exhausted before cast wake"
  echo; echo "cannot launch (exit 3)"; exit 3
else
  say "click budget" "$budget available"
fi

# Source preparation is not an informational tripwire. No campaign/abort loop.
# Check before preflight JVM reads and again immediately before each POST.
prepare_sources() {
  if ! python3 "$SCRIPT_DIR/wm_click_prepare.py"; then
    cannot "source composition" "idle canonical preparation did not verify; no click posted"
    return 3
  fi
}
if [ "$RUN" = "1" ]; then
  prepare_sources || exit 3
fi

# 2. Casting: three distinct seats, all on the roster AND idle.
# Misconfiguration (non-distinct seats) cannot be waited out: terminal now.
# Busy seats CAN be waited out: when firing, we poll (see the wait loop below).
#
# ON THE ROSTER IS NOT ENOUGH. full_loop_runner/available? (:827) requires
# :invoke-ready? true AND status "idle"; the author is checked at :3786 and
# throws :agent-unavailable before selection is ever reached. On 2026-09-19
# grants 2 and 3 of the five-click allocation were both spent this way: the
# click was issued through codex-23, which made codex-23 status "invoking",
# while codex-23 was also the configured AUTHOR. :failure-detail :busy, twice,
# for the same reason, and neither run reached a cascade selection. Checking
# roster membership alone did not see it. Check the status field.
#
# "restored" COUNTS AS AVAILABLE (claude-4, 2026-09-19, after a server restart).
# After futon3c-zone.service restarts, the Agency restores its roster and every
# seat reads status "restored" until it is next observed working -- 58 of 73 on
# the 21:33:37Z restart, including all three cast seats, with the only "idle"
# seats being off-site oxf-*/ams-* that write to another host. Rejecting
# "restored" therefore blocked every click after any restart, which is a
# stricter rule than the runner's own. Checked before loosening it: codex-15 was
# belled while reading "restored" and went accepted -> running -> prompt -> text
# in nine seconds. The state that actually costs a click is "invoking", which
# this still treats as busy.
misconfigured=0
if [ "$AUTHOR" = "$REVIEWER" ] || [ "$AUTHOR" = "$REPAIR" ] || [ "$REVIEWER" = "$REPAIR" ]; then
  cannot "casting" "author/reviewer/repair-reviewer must be three DISTINCT seats (got $AUTHOR / $REVIEWER / $REPAIR)"
  misconfigured=1
fi

# 2b. The issuing caller must not be one of the three cast seats.
# Issuing through a seat marks it "invoking"; if that seat is also the author,
# the runner's own readiness check fails it. This is how two clicks were lost.
# A misconfiguration cannot be waited out: terminal now.
case "$ISSUING_CALLER" in
  "$AUTHOR"|"$REVIEWER"|"$REPAIR")
    cannot "issuing caller" "$ISSUING_CALLER is also a cast seat -- issuing marks it busy and the author check will fail"
    misconfigured=1;;
  *) say "issuing caller" "$ISSUING_CALLER (not a cast seat)";;
esac
[ "$misconfigured" = "0" ] || { echo; echo "cannot launch (exit 3)"; exit 3; }

# Prefer three idle Zai seats outside the cast. When fewer are idle, report
# their actual state below rather than adding an admission guard.
default_ask_seats() {
  python3 -c '
import json,sys
roster=json.load(sys.stdin); roster=roster.get("agents",roster)
excluded=set(sys.argv[1:])
preferred=["zai-4","zai-5","zai-6"]
names=[s for s in preferred + sorted(roster) if s.startswith("zai-") and s in roster and s not in excluded]
names=list(dict.fromkeys(names))
def idle(s):
    r=roster.get(s,{})
    return r.get("status") in ("idle","restored") and r.get("invoke-ready?") is True
names.sort(key=lambda s: not idle(s))
print(",".join(names[:3]))
' "$AUTHOR" "$REVIEWER" "$REPAIR" "$ISSUING_CALLER"
}

# cast_report: print the casting/single-flight state once; return 0 when all
# three seats are idle-or-restored + invoke-ready AND no click is in flight.
cast_report() {
  local roster notready="" a st inf
  roster=$(curl -s -m 10 "$BASE/api/alpha/agents")
  if [ -z "$ASK_SEATS" ]; then
    ASK_SEATS=$(printf '%s' "$roster" | default_ask_seats)
  fi
  for a in "$AUTHOR" "$REVIEWER" "$REPAIR"; do
    st=$(printf '%s' "$roster" | AGENT="$a" python3 -c '
import sys, json, os
a = os.environ["AGENT"]
d = json.load(sys.stdin)
ag = d.get("agents", d)
r = ag.get(a)
if not isinstance(r, dict):
    print("absent")
elif r.get("status") in ("idle", "restored") and r.get("invoke-ready?") is True:
    print("idle")
else:
    print(str(r.get("status")) + ("" if r.get("invoke-ready?") else "/not-invoke-ready"))
' 2>/dev/null)
    [ "$st" = "idle" ] || notready="$notready $a($st)"
  done
  # Ask-seat observations never veto a click or change cast waiting.
  printf '%s' "$roster" | python3 -c '
import json,sys
roster=json.load(sys.stdin); roster=roster.get("agents",roster)
for seat in sys.argv[1].split(","):
    row=roster.get(seat)
    status="absent" if not isinstance(row,dict) else str(row.get("status")) + ("" if row.get("invoke-ready?") else "/not-invoke-ready")
    overlap=" (also cast)" if seat in sys.argv[2:] else ""
    print("  %-22s %s%s" % ("ask " + seat,status,overlap))
' "$ASK_SEATS" "$AUTHOR" "$REVIEWER" "$REPAIR"
  # 3. No click already in flight (the boundary is single-flight). An
  # unreadable endpoint counts as busy: we do not fire into an unknown state.
  inf=$(curl -s -m 15 "$BASE/api/alpha/wm/click" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get("running?"))' 2>/dev/null)
  if [ -z "$notready" ]; then
    say "casting" "$AUTHOR / $REVIEWER / $REPAIR idle and invoke-ready"
  else
    say "casting" "busy (waiting):$notready -- a busy AUTHOR would spend the click without selecting"
  fi
  if [ "$inf" = "False" ]; then
    say "in flight" "none"
  else
    say "in flight" "click running or unavailable ($inf)"
    if [ "$RUN" = "1" ]; then
      cannot "source composition" "not idle; no preparation or launch"
      exit 3
    fi
  fi
  [ -z "$notready" ] && [ "$inf" = "False" ]
}

# 4. THE ONE THAT MATTERS: would any tripwire halt this click?
#    (Reported as a finding since the firing-policy change: the tripwires act
#    during the run; preflight no longer vetoes.) A witness stops the run
#    (futon2 a8ac1615). Evaluating all 13 against a real observation costs
#    ~13s here; discovering it inside a click costs the click. This also covers
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
# travel by environment--proof-eval evaluates this form inside the server
# JVM, whose env is not this shell's, so a var like WM_CLICK_SKIP_WIRES on the
# proof-eval command line never reaches the code (measured, ab5ca8fa). Inline
# any such literal into the generated form.
if [ "$RUN" = "1" ]; then
wires_file=$(mktemp "${TMPDIR:-/tmp}/wm-click-wires.XXXXXX.clj")
cat > "$wires_file" <<'CLJ'
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
                                        (catch Throwable _ [{:kind :wire-threw}]))]
                           :when (seq w)]
                       [k (mapv :kind w)
                        (boolean (some #(rcw % obs) w))])]
      {:tripping (vec (for [[k kinds deferred?] classified :when (not deferred?)]
                        [k kinds]))
       :will-defer (vec (for [[k _ deferred?] classified :when deferred?] k))
       ;; What actually decides the branch. full-loop-runner takes
       ;;   stop-line = (first open, non-environmental-hold obligation)
       ;; and when one exists the entry is repair-entry, NOT ordinary
       ;; selection -- so there is no controller-decision and no selection
       ;; certificate, and the run record cannot score above 1/5. A deferred
       ;; tripwire witness is a SYMPTOM of the same backlog, not its cause;
       ;; this counts the cause.
       :stop-lines-queued
       (count (filter #(and (= :open (:repair/status %))
                            (not= :environmental-hold (:repair/class %)))
                      ((resolve 'futon2.aif.repair-obligation/open-obligations))))})
CLJ
# proof-eval.sh reads its admin token from its own directory, so it must be
# invoked from there.
wires=$( (cd "$F3C" && ./scripts/proof-eval.sh -f "$wires_file" 2>&1) | tail -1)
rm -f -- "$wires_file"
else
  wires="not evaluated in dry-run (no JVM evaluation or namespace load)"
fi
case "$wires" in
  *":tripping []"*)
    say "tripwires" "13/13 clear";;
  *":tripping ["*)
    # Finding, not a veto: the wire halts the RUN if it trips live, and the
    # run records that honestly. Preflight reports it so the operator knows.
    finding "tripwires" "WOULD HALT if tripped live: $(echo "$wires" | sed 's/.*:tripping //; s/}}$//')";;
  *)
    # Third outcome, kept distinct on purpose. "The check could not run" is not
    # "a wire would halt", and reporting the two the same way is the defect
    # this script exists to keep out of clicks (guardrails.clj:118 does exactly
    # that with (catch Throwable _ false)).
    finding "tripwires" "CHECK DID NOT RUN -- wire status unknown: $wires";;
esac
# Repair-covered witnesses do not halt the click (observe! defers them so the
# repair can be selected). Report them so the operator knows the click will
# start inside a named repair obligation -- visible, never silent.
case "$wires" in
  *":will-defer []"*) :;;
  *":will-defer ["*)
    say "will defer to repair" "$(echo "$wires" | sed 's/.*:will-defer \(\[[^]]*\]\).*/\1/') -- witness names open obligations"
    :;;
  *)  :;;
esac

# 4b. THE ONE THAT DECIDES WHAT KIND OF RUN THIS IS.
# An open stop-line obligation pre-empts ordinary selection: the runner takes
# repair-entry (injected at -Inf, :selection-source :stop-the-line) instead of
# the cascade's choice, so the run has no controller-decision and no selection
# certificate and its record cannot score above 1/5 under wm_run_validity.bb.
# Measured on click 1 of 5 (wm-click-599b9255, 2026-09-19): 20 queued, the
# click enacted :repair-machine-failure, record INVALID 0/5.
# An earlier version of this check blamed the deferred tripwire witness. That
# was the wrong cause: the witness defers BECAUSE the backlog exists, and both
# are downstream of the queue. Count the queue. Reported as evidence either
# way; selection proceeds regardless (RULING-selection-precedence-2026-09-19,
# futon2 8b6827da).
queued=$(echo "$wires" | sed -n 's/.*:stop-lines-queued \([0-9]*\).*/\1/p')
case "$queued" in
  0) say "stop-line queue" "empty";;
  "") finding "stop-line queue" "COULD NOT BE READ -- do not assume it is empty";;
  *)  say "stop-line queue" "$queued open obligation(s) -- recorded as evidence in the run record; selection proceeds regardless (RULING-selection-precedence-2026-09-19, futon2 8b6827da)";;
esac

# 5. Seat quota (opt-in: costs one agent turn each).
if [ "$PROBE" = "1" ] && [ "$RUN" != "1" ]; then
  say "quota probe" "not sent in dry-run; use --run --probe-seats"
fi
if [ "$PROBE" = "1" ] && [ "$RUN" = "1" ]; then
  for a in "$AUTHOR" "$REVIEWER" "$REPAIR"; do
    r=$(cd "$F3C" && timeout 200 python3 scripts/agency_send.py --from "$ISSUING_CALLER" --to "$a" \
          --kind whistle --timeout-ms 170000 <<'EOF' 2>&1 | tail -1
Quota probe only -- do not start any task, do not edit any file.
Reply with exactly one line: your model id, and whether you currently have
usage quota available to run a coding turn. Nothing else.
EOF
)
    case "$r" in *available*) say "quota $a" "ok";; *) finding "quota $a" "$r";; esac
  done
fi

echo
if [ "$RUN" != "1" ]; then
  # Print the casting/single-flight state once, informationally. Findings only;
  # preflight never refuses, so busy seats here are a report, not a block.
  cast_report || true
  echo
  echo "preflight complete (findings above are informational; preflight never refuses; exit 0)"
  exit 0
fi

# Casting wait (⟨1⟩1 ⟨2⟩2): never launch into an occupied seat and never start
# a second active click. WAIT for seats only: poll every 20 s, printing
# each poll, until cast_report clears. After 30 minutes, print a terminal
# account and exit 3 (cannot launch -- reported, not a refusal, not a success).
waited_since=$(date -u +%Y-%m-%dT%H:%M:%SZ)
t0=$(date +%s)
echo "waiting for cast seats and single-flight boundary (since $waited_since; max 30 min)"
clickid=""
while [ -z "$clickid" ]; do
  while :; do
    if cast_report; then break; fi
    if [ $(( $(date +%s) - t0 )) -gt 1800 ]; then
      echo
      cannot "casting wait" "30 minutes elapsed since $waited_since; seat/click state above is the terminal account"
      echo "cannot launch (exit 3)"
      exit 3
    fi
    sleep 20
  done

  # ---------------------------------------------------------------- fire
  prepare_sources || exit 3
  # The kernel's UUID source, not uuidgen: zone 1.1 (2026-10-09) shipped
  # without uuid-runtime, and the old `|| date +%s` fallback minted an id the
  # endpoint refuses as :wm-click-run-id-malformed.
  RUNID="$(date -u +%Y-%m-%d)-$(cat /proc/sys/kernel/random/uuid)"
  echo; echo "firing click, run-id $RUNID"
  payload=$(python3 - "$RUNID" "$AUTHOR" "$REVIEWER" "$REPAIR" "$ISSUING_CALLER" "$G_LAW" "$ASK_SEATS" <<'PYJSON'
import json, sys
payload = dict(zip(
    ["run-id", "author", "reviewer", "repair-reviewer", "issuing-caller"],
    sys.argv[1:6]), trigger="duree-click-on-demand")
if sys.argv[6]:
    payload["g-law"] = sys.argv[6]
payload["interpretation-seats"] = sys.argv[7].split(",")
print(json.dumps(payload))
PYJSON
)
  post_left=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  resp=$(curl -s -m "$POST_TIMEOUT_S" -X POST "$BASE/api/alpha/wm/click" -H 'Content-Type: application/json' \
    -d "$payload")
  post_rc=$?
  echo "$resp"
  if [ "$post_rc" -eq 28 ] || [ -z "$resp" ]; then
    echo "admission answer not received (curl exit $post_rc; POST left $post_left)"
    recovery_started=$(date +%s)
    while [ -z "$clickid" ]; do
      recovery_record="$F2/data/wm-runs/tick-run-record-$RUNID.edn"
      if [ -f "$recovery_record" ]; then
        clickid=$(python3 - "$recovery_record" <<'PY'
import re, sys
text = open(sys.argv[1], encoding="utf-8").read()
match = re.search(r':click/id\s+"([^"]+)"', text)
print(match.group(1) if match else "")
PY
)
      fi
      if [ -z "$clickid" ]; then
        recovery_status=$(curl -s -m 20 "$BASE/api/alpha/wm/click")
        recovery_running=$(echo "$recovery_status" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("running?"))' 2>/dev/null)
        recovery_click=$(echo "$recovery_status" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("click-id") or "")' 2>/dev/null)
        if [ "$recovery_running" = "True" ] && [ -n "$recovery_click" ]; then
          clickid="$recovery_click"
        fi
      fi
      [ -n "$clickid" ] && break
      if [ $(( $(date +%s) - recovery_started )) -ge "$POST_RECOVERY_MAX_S" ]; then
        echo "ALERT FOR JOE"
        echo "requirements NOT CHECKED: admission answer absent and click was not recovered"
        exit 1
      fi
      sleep "$POST_RECOVERY_POLL_S"
    done
    echo "recovered admitted click: $clickid"
  fi
  # The server answers {:rejected :already-running :click-id <THE OTHER CLICK>}
  # when another caller started a click between our wait and this POST
  # (runner_service.clj click!). That id is not ours: stop this launch.
  rejected=$(echo "$resp" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("rejected") or "")' 2>/dev/null)
  if [ -n "$rejected" ]; then
    cannot "launch" "$rejected; no automatic launch after another active click"
    exit 3
  fi
  if [ -z "$clickid" ]; then
    clickid=$(echo "$resp" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("click-id",""))' 2>/dev/null)
  fi
  [ -n "$clickid" ] || { echo "no click-id returned; not accepted"; exit 1; }
done
echo "tracking our click: $clickid"

t0=$(date +%s)
while :; do
  sleep 20
  s=$(curl -s -m 20 "$BASE/api/alpha/wm/click")
  running=$(echo "$s" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("running?"))' 2>/dev/null)
  cur=$(echo "$s" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("click-id") or "")' 2>/dev/null)
  if [ -n "$cur" ] && [ "$cur" != "$clickid" ]; then
    echo "status now shows a different click ($cur); ours ($clickid) has finished"; break
  fi
  phase=$(echo "$s" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("phase"))' 2>/dev/null)
  echo "[$(( $(date +%s) - t0 ))s] running=$running phase=$phase"
  [ "$running" = "False" ] && break
  [ $(( $(date +%s) - t0 )) -gt 5400 ] && { echo "still running after 90 min; report this, do not re-fire"; exit 1; }
done

if ! python3 "$F2/scripts/wm_click_wait_child.py" --base "$BASE" --click-id "$clickid"; then
  echo "ALERT FOR JOE"
  echo "requirements NOT CHECKED: self-heal child did not complete"
  exit 1
fi

echo; echo "terminal state:"
curl -s -m 20 "$BASE/api/alpha/wm/click" | python3 -m json.tool | sed -n '1,24p'
echo; echo "newest repair finding, if the click opened one:"
# Avoid ls | head under pipefail: SIGPIPE used to turn a completed click into
# shell exit 141 when the finding directory outgrew the pipe buffer.
python3 - "$F2/data/wm-repair-obligations/findings" <<'PY'
from pathlib import Path
import subprocess
import sys

newest = max(Path(sys.argv[1]).glob("*.edn"),
             key=lambda path: path.stat().st_mtime_ns, default=None)
if newest is not None:
    subprocess.run(["ls", "-l", str(newest)], check=True)
PY

# Ownership check (R19): after a recovered or ordinary click, only retained
# server evidence establishes that the finished click is ours. It runs after
# the terminal-state prints so a refusal still shows what the server holds.
# only retained server evidence establishes ownership. Prefer the run record;
# a click binding can establish the same pair while the run record is still
# unavailable. Never run the requirements checker for a mismatched pair.
run_record="$F2/data/wm-runs/tick-run-record-$RUNID.edn"
binding_id=$(printf '%s' "$clickid" | sed 's/[^A-Za-z0-9._-]/_/g')
binding_file="$F3C/data/wm-click-run-bindings/click-run-binding-$binding_id.edn"
if ! ownership=$(python3 - "$run_record" "$binding_file" "$RUNID" "$clickid" <<'PY'
import pathlib, re, sys
record_path, binding_path, expected_run, expected_click = sys.argv[1:]

def value(text, key):
    match = re.search(re.escape(key) + r'\s+"([^"]+)"', text)
    return match.group(1) if match else None

record = pathlib.Path(record_path)
if record.exists():
    text = record.read_text(encoding="utf-8")
    actual_run = value(text, ":run/id")
    actual_click = value(text, ":click/id")
    if (actual_run, actual_click) == (expected_run, expected_click):
        print("run-record")
        raise SystemExit(0)
    print(f"run record ownership mismatch: expected run={expected_run} click={expected_click}; "
          f"actual run={actual_run} click={actual_click}")
    raise SystemExit(1)

binding = pathlib.Path(binding_path)
if binding.exists():
    text = binding.read_text(encoding="utf-8")
    actual_click = value(text, ":click/id")
    observed = re.search(r':run-id-observation\s+\{[^}]*:value\s+"([^"]+)"', text)
    actual_run = observed.group(1) if observed else value(text, ":run/id")
    if (actual_run, actual_click) == (expected_run, expected_click):
        print("click-binding")
        raise SystemExit(0)
    print(f"click binding ownership mismatch: expected run={expected_run} click={expected_click}; "
          f"actual run={actual_run} click={actual_click}")
    raise SystemExit(1)

print(f"ownership evidence absent: run={expected_run} click={expected_click}; "
      f"no run record {record_path} and no click binding {binding_path}")
raise SystemExit(1)
PY
); then
  echo "ALERT FOR JOE"
  echo "$ownership"
  echo "requirements NOT CHECKED: recovered click ownership not established"
  exit 1
fi
echo "click ownership confirmed by $ownership"

# A written run is not a successful click until the independently evaluated
# Q1--Q10 verdict exists.  This is deliberately unconditional: no environment
# variable can disable the post-run check.
verdict_file="$run_record.requirements.edn"
if [ ! -f "$run_record" ]; then
  echo "ALERT FOR JOE"
  echo "requirements NOT CHECKED: run record missing after completed click: $run_record"
  exit 1
fi
python3 "$F2/scripts/wm_run_alert.py" --run-record "$run_record"
requirements_status=$?
if [ ! -f "$verdict_file" ]; then
  echo "ALERT FOR JOE"
  echo "requirements NOT CHECKED: verdict file absent after post-run check: $verdict_file"
  exit 1
fi
# R11: when a self-heal child ran, its verdict is carried into the parent's
# durable click state (the service refuses a file naming another run record).
# An unrecorded child verdict is not a passed click.
if ! python3 "$F2/scripts/wm_click_record_child_verdict.py" --base "$BASE" --click-id "$clickid" --verdict-file "$verdict_file"; then
  exit 1
fi
exit "$requirements_status"
