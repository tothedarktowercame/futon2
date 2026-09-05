#!/usr/bin/env bash
# U55 -- THE STEPPER. One tick at a time, from a pin, into a sandbox, never
# into live data/.
#
#   bash holes/labs/wm-contract/wm_step.sh init  <work-dir> [agent]
#   bash holes/labs/wm-contract/wm_step.sh reset <work-dir>
#   bash holes/labs/wm-contract/wm_step.sh step  <work-dir> [label] [--allow-pin-drift]
#   bash holes/labs/wm-contract/wm_step.sh accept <work-dir> <step-dir> [run-id]
#   bash holes/labs/wm-contract/wm_step.sh determinism <work-dir>
#   bash holes/labs/wm-contract/wm_step.sh compare <work-dir> <step-dir-a> <step-dir-b>
#   bash holes/labs/wm-contract/wm_step.sh plant <work-dir> mu-uniform
#   bash holes/labs/wm-contract/wm_step.sh status <work-dir>
#
# Joe, 2026-09-04 (EPIC-run-era.md, the step-era ruling): "get a version of the
# machine set up that is stepable and resetable ... run it forward step by step,
# fix defects, and only then move on to the next step and eventual continuous
# running." Continuous running is the degenerate case of accepting every step
# without inspection, which is why it comes last.
#
# THE CHECKPOINT IS THE ONE C509 SPECIFIED, not one invented here.
#   * The pin is the WHOLE TRACE DIRECTORY, not the last record: three readers
#     reach past it -- recent-trace-records 12 (war_machine.clj:5959,6262-6263),
#     the cold-start habit prior, which folds every wm-trace-*.edn (:79-80,
#     :5984-5986), and the ladder's case-history index (:2732-2734).
#   * .run-lock is EXCLUDED: replaying a pinned pid/pid-start-ms/token
#     re-asserts a dead holder (C509 W7).
#   * .lane-futility-index.edn is EXCLUDED because it is DERIVED -- validated
#     against a (name, length, mtime) fingerprint per trace file and rebuilt
#     when stale (lane_futility.clj:82-86). Restoring the corpus restores it by
#     construction; it is the one item in the census where that is true.
#   * The sandbox seam is `FUTON_WM_TRACE_DIR` + `FUTON_WM_RECEIPT_DIR`
#     (run_tick_once.clj `sandbox`), added for this row: the trace, the RE4
#     rationale store (which follows :trace-dir, war_machine.clj:2273-2282) and
#     the receipt all land under the sandbox, so a step never writes live data/.
#   * The determinism exclusion list is C509 handoff item 4 and nothing more:
#     :run/id, :timestamp, :startedAt, and the per-hop route :at stamps.
#
# THE RUN LOCK IS TAKEN, NOT AVOIDED (RUN12). A sandboxed step writes nothing
# live, but it is still one machine and one runner: `init` holds the live lock
# while it copies the corpus, so it cannot pin a file mid-append, and `step`
# holds it across the tick. The tick nests inside it via
# FUTON_WM_RUN_LOCK_TOKEN exactly as wm_run.sh's ticks do.
#
# ONE JVM PER STEP, deliberately (as wm_run.sh:32-34). Every `defonce` resets
# between steps, so the eight in-memory atoms C509 censused (M1-M8) are not
# inter-tick state HERE. A later in-JVM stepper reintroduces three of them --
# the scan caches, the habit-seed delay, and case-history-index-memo, which is
# keyed by trace directory and so does NOT escape a redirected trace dir.
#
# Run the pre-flight (r6_zero_post_preflight.clj) before the first step of a
# session, as every run in this lab does.
set -uo pipefail

LAB="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$LAB/../../.." && pwd)"
LIVE_TRACE="$ROOT/data/wm-trace"
RECORDS="$LAB/wm_step_records.bb"
EVIDENCE="$LAB/wm_step_evidence.bb"
EV_PORT="${FUTON_WM_STEP_PORT:-7099}"
DAYS="${FUTON_WM_STEP_DAYS:-14}"
AGENT_DEFAULT="${FUTON_WM_AGENT:-wm-step}"

# Progress goes to stderr so stdout carries only data (the step directory a
# caller captures). A tool whose progress and whose result share a stream makes
# every caller parse.
say() { echo "[wm_step $(date -u '+%H:%M:%S')] $*" >&2; }
die() { say "ERROR: $*"; exit "${2:-2}"; }

# --------------------------------------------------------------------------
# the live run lock (RUN12): taken by a holder process, released on exit
# --------------------------------------------------------------------------
HOLDER=""
LOCKOUT=""
lock_take() { # $1 hold-seconds  $2 agent
  LOCKOUT="$(mktemp /tmp/wm-step-lock.XXXXXX.out)"
  ( cd "$ROOT" && clojure -M -m futon2.wm-run-lock hold "$1" "$2" ) > "$LOCKOUT" 2>&1 &
  HOLDER=$!
  for _ in $(seq 1 180); do grep -q ':token' "$LOCKOUT" 2>/dev/null && break; sleep 1; done
  if ! grep -q ':token' "$LOCKOUT" 2>/dev/null; then
    say "could not take the live run lock:"; cat "$LOCKOUT"; return 3
  fi
  FUTON_WM_RUN_LOCK_TOKEN=$(sed -n 's/.*:token "\([^"]*\)".*/\1/p' "$LOCKOUT")
  export FUTON_WM_RUN_LOCK_TOKEN
  say "run lock held ($2)"
  return 0
}
lock_release() {
  [ -n "$HOLDER" ] || return 0
  kill -TERM "$HOLDER" 2>/dev/null; wait "$HOLDER" 2>/dev/null
  HOLDER=""; unset FUTON_WM_RUN_LOCK_TOKEN
  say "run lock released"
}
trap 'lock_release; ev_stop' EXIT

# --------------------------------------------------------------------------
# the evidence cassette (see wm_step_evidence.bb): the answers of a service the
# pin cannot hold. Started around the tick and stopped after it.
# --------------------------------------------------------------------------
EVPID=""
ev_start() { # $1 cassette  $2 log  $3 "" | --frozen
  bb "$EVIDENCE" serve "$1" "$EV_PORT" ${3:-} --log "$2" > "$2.out" 2>&1 &
  EVPID=$!
  # Wait on the server's own line, never on a probe request: a probe would fill
  # the cassette with a URL the tick never asks for.
  for _ in $(seq 1 40); do
    grep -q "serving" "$2.out" 2>/dev/null && break
    sleep 0.5
  done
  grep -q "serving" "$2.out" 2>/dev/null || { say "evidence cassette failed to start:"; cat "$2.out" >&2; return 3; }
  FUTON3C_EVIDENCE_BASE="http://127.0.0.1:$EV_PORT"; export FUTON3C_EVIDENCE_BASE
  say "evidence cassette serving on :$EV_PORT ($(basename "$1"))"
  return 0
}
ev_stop() {
  [ -n "$EVPID" ] || return 0
  kill -TERM "$EVPID" 2>/dev/null
  wait "$EVPID" 2>/dev/null
  EVPID=""; unset FUTON3C_EVIDENCE_BASE
}

sha_of() { sha256sum "$1" | cut -d' ' -f1; }
git_sha() { ( cd "$1" && git rev-parse HEAD 2>/dev/null || echo "" ); }
tree_dirty() { ( cd "$1" && [ -n "$(git status --porcelain 2>/dev/null)" ] && echo true || echo false ); }
# U57 closes the residual C511 section 1 named: the boolean above cannot exclude
# a dirty-then-reverted-without-a-commit path, so a later reader cannot tell
# which files the tick actually read. This records the porcelain LIST beside it.
# `prn` of a vector, not a hand-built literal: a path with a quote or a backslash
# in it would otherwise write EDN the step record cannot be read back from.
tree_dirty_files() {
  ( cd "$1" && git status --porcelain 2>/dev/null ) \
    | bb -e '(prn (vec (remove clojure.string/blank? (clojure.string/split-lines (slurp *in*)))))'
}

# The evidence store's watermark: C509's :store-basis, the only cursor-shaped
# thing anywhere on the tick path (run_tick_once.clj:126-135) -- and nothing
# consumes it. The stepper does: a step records the count and the newest
# :evidence/at it saw, so a divergence between two steps can be attributed to a
# store that moved rather than left as a mystery. Base resolution copies
# pattern-registry/configured-evidence-base (FUTON3C_EVIDENCE_BASE, then
# FUTON3C_SERVER, then 127.0.0.1:FUTON3C_PORT, default 7070).
store_basis() {
  local base="${FUTON3C_EVIDENCE_BASE:-${FUTON3C_SERVER:-http://127.0.0.1:${FUTON3C_PORT:-7070}}}"
  local c m
  c="$(curl -s -m 10 "$base/api/alpha/evidence/count" 2>/dev/null | grep -o '"count":[0-9]*' | head -1 | cut -d: -f2)"
  m="$(curl -s -m 10 "$base/api/alpha/evidence?limit=1" 2>/dev/null | grep -o '"evidence/at":"[^"]*"' | head -1 | cut -d'"' -f4)"
  printf '{:base "%s" :count %s :max-at "%s" :at "%s"}' "$base" "${c:-nil}" "${m:-}" "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
}

# --------------------------------------------------------------------------
cmd_init() {
  local work="$1" agent="${2:-$AGENT_DEFAULT}"
  [ -e "$work/pin" ] && die "$work/pin already exists -- init refuses to overwrite a pin; remove it or choose another work dir"
  mkdir -p "$work/pin/wm-trace" "$work/steps" || die "cannot create $work"
  lock_take 900 "$agent" || die "run lock" 3
  local n=0
  for f in "$LIVE_TRACE"/wm-trace-????-??-??.edn; do
    [ -e "$f" ] || continue
    cp -p "$f" "$work/pin/wm-trace/" && n=$((n+1))
  done
  local basis; basis="$(store_basis)"
  lock_release
  [ "$n" -gt 0 ] || die "no wm-trace-YYYY-MM-DD.edn under $LIVE_TRACE"
  bb "$RECORDS" manifest "$work/pin/wm-trace" "$work/pin/manifest.edn" || die "manifest"
  bb "$RECORDS" world "$work/pin/world.edn" || die "world"
  cat > "$work/pin/pin.edn" <<EOF
{:pin/schema :wm/step-pin-v1
 :pin/at "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
 :pin/generation 0
 :pin/agent "$agent"
 :pin/source "$LIVE_TRACE"
 :pin/files $n
 :pin/futon2-sha "$(git_sha "$ROOT")"
 :pin/futon2-tree-dirty? $(tree_dirty "$ROOT")
 :pin/store-basis $basis
 :pin/manifest "pin/manifest.edn"
 :pin/world "pin/world.edn"
 :pin/accepted-steps []
 :pin/held-under-run-lock true
 :pin/excluded ".run-lock (holder identity, C509 W7); .lane-futility-index.edn (derived, C509 W2); data/wm-rationale (write target, recreated under the sandbox)"
 :pin/note "the WHOLE corpus, not the last record: the habit prior folds every file (war_machine.clj:79-80,5984-5986) and the ladder's case-history index does the same (:2732-2734)"}
EOF
  say "init: pinned $n trace file(s) from $LIVE_TRACE into $work/pin"
  cmd_reset "$work"
}

# --------------------------------------------------------------------------
cmd_reset() {
  local work="$1"
  [ -d "$work/pin/wm-trace" ] || die "no pin at $work/pin -- run init first"
  rm -rf "$work/sandbox"
  mkdir -p "$work/sandbox/wm-trace" "$work/sandbox/receipts"
  cp -p "$work/pin/wm-trace"/*.edn "$work/sandbox/wm-trace/" || die "restore"
  # The restore is VERIFIED BY CONTENT HASH against the PIN AS IT STANDS, not by
  # trusting cp -- that is what "reset restores the pin exactly" has to mean.
  # Deliberately not against pin/manifest.edn: that manifest answers a different
  # question (has an input moved under the pin since it was taken?), which
  # `step` asks before it runs.
  bb "$RECORDS" verify-dirs "$work/pin/wm-trace" "$work/sandbox/wm-trace"
  local rc=$?
  if [ $rc -ne 0 ]; then say "reset: RESTORED STATE DOES NOT MATCH THE PIN"; return 4; fi
  say "reset: sandbox restored and hash-verified against the pin"
  return 0
}

# --------------------------------------------------------------------------
cmd_step() {
  local work="$1"; shift
  local label="step" allow_drift=0 evidence="pinned-replay" ev_flag=""
  # `arg` and not `a`: bash locals are dynamically scoped, so a loop variable
  # named `a` here assigns the CALLER's `a` -- which is how the determinism
  # control first compared a flag string against a step directory.
  local arg
  for arg in "$@"; do
    case "$arg" in
      --allow-pin-drift) allow_drift=1 ;;
      --live-evidence) evidence="live" ;;
      --frozen-evidence) evidence="pinned-frozen"; ev_flag="--frozen" ;;
      --*) die "unknown flag $arg" ;;
      *) label="$arg" ;;
    esac
  done
  [ -d "$work/pin/wm-trace" ] || die "no pin at $work/pin -- run init first"

  # 1. The pin is verified BEFORE the step. A changed input is a refusal, not a
  #    note: this is the arm that makes "a planted divergent input is DETECTED,
  #    not absorbed" a property of the tool rather than of the reader.
  local pinverify="ok"
  if ! bb "$RECORDS" verify "$work/pin/wm-trace" "$work/pin/manifest.edn"; then
    pinverify="drift"
    if [ "$allow_drift" = "0" ]; then
      say "step: REFUSING -- the pinned corpus differs from pin/manifest.edn."
      say "step: an input moved under the pin. Re-init, restore it, or pass --allow-pin-drift to step deliberately."
      return 4
    fi
    say "step: pin drift ACCEPTED by --allow-pin-drift; the step record says so"
  fi

  local n idx dir
  n=$(( $(find "$work/steps" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | wc -l) + 1 ))
  idx=$(printf '%03d' "$n")
  dir="$work/steps/$idx-$label"
  mkdir -p "$dir/rationale" || die "cannot create $dir"

  # A step that cannot restore its own starting state is not a step: remove the
  # directory rather than leave an empty numbered one that reads like a run.
  cmd_reset "$work" || { rm -rf "$dir"; return 4; }

  bb "$RECORDS" world "$dir/world-before.edn" || die "world-before"
  local basis_before; basis_before="$(store_basis)"

  local cassette="$work/pin/evidence-cassette.edn"
  local t0 t1 rc
  t0=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
  if [ "$evidence" != "live" ]; then
    ev_start "$cassette" "$dir/cassette.log" "$ev_flag" || die "evidence cassette" 3
  fi
  lock_take 1800 "$AGENT_DEFAULT" || die "run lock" 3
  ( cd "$ROOT" \
    && FUTON_WM_TRACE_DIR="$work/sandbox/wm-trace" \
       FUTON_WM_RECEIPT_DIR="$work/sandbox/receipts" \
       clojure -M -m futon2.run-tick-once "$DAYS" ) > "$dir/tick.out" 2>&1
  rc=$?
  lock_release
  ev_stop "$cassette"
  t1=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
  say "step $idx-$label: tick exit=$rc ($(wc -l < "$dir/tick.out") lines in $dir/tick.out)"

  bb "$RECORDS" world "$dir/world-after.edn" || true
  local basis_after; basis_after="$(store_basis)"

  # 2. What the step produced: the appended records (selected by run id), the
  #    receipt, and the rationale the RE4 seam wrote beside the sandbox trace.
  bb "$RECORDS" delta "$work/pin/wm-trace" "$work/sandbox/wm-trace" "$dir/delta.edn" || die "delta"
  local run_id
  run_id="$(bb -e '(println (or (first (:delta/run-ids (clojure.edn/read-string (slurp (first *command-line-args*))))) ""))' "$dir/delta.edn")"
  cp -p "$work/sandbox/receipts"/tick-run-record-*.edn "$dir/" 2>/dev/null
  if [ -d "$work/sandbox/wm-trace/rationale" ]; then
    cp -p "$work/sandbox/wm-trace/rationale"/*.edn "$dir/rationale/" 2>/dev/null
  fi
  # The decision record the determinism control compares: the trace record(s)
  # the step appended. One file, one form per record.
  bb -e '(let [d (clojure.edn/read-string {:default (fn [t v] {:unread-tag t :value v})} (slurp (first *command-line-args*)))]
           (with-open [w (clojure.java.io/writer (second *command-line-args*))]
             (doseq [r (:delta/records d)] (.write w (pr-str r)) (.write w "\n"))))' \
     "$dir/delta.edn" "$dir/decision-records.edn" || die "decision-records"
  bb "$RECORDS" normalize "$dir/decision-records.edn" "$dir/normalized.edn" || die "normalize"
  local norm_sha
  norm_sha="$(bb -e '(println (:normalized/sha256 (clojure.edn/read-string {:default (fn [t v] nil)} (slurp (first *command-line-args*)))))' "$dir/normalized.edn")"

  cat > "$dir/step.edn" <<EOF
{:step/schema :wm/step-record-v1
 :step/id "$idx-$label"
 :step/work "$work"
 :step/started-at "$t0"
 :step/ended-at "$t1"
 :step/tick-exit $rc
 :step/days $DAYS
 :step/run-id "$run_id"
 :step/pin-verify :$pinverify
 :step/pin-generation $(bb -e '(println (:pin/generation (clojure.edn/read-string (slurp (first *command-line-args*)))))' "$work/pin/pin.edn")
 :step/futon2-sha "$(git_sha "$ROOT")"
 :step/futon2-tree-dirty? $(tree_dirty "$ROOT")
 :step/futon2-tree-dirty-files $(tree_dirty_files "$ROOT")
 :step/sandbox {:trace-dir "$work/sandbox/wm-trace"
                :receipt-dir "$work/sandbox/receipts"
                :rationale-dir "$work/sandbox/wm-trace/rationale"}
 :step/live-data-written false
 :step/run-lock :taken-and-released
 :step/normalized-sha256 "$norm_sha"
 :step/store-basis {:before $basis_before :after $basis_after}
 :step/evidence {:mode :$evidence
                 :cassette "pin/evidence-cassette.edn"
                 :cassette-sha256 "$( [ -f "$cassette" ] && sha_of "$cassette" || echo "" )"
                 :cassette-entries $(bb -e '(println (or (:cassette/entry-count (clojure.edn/read-string {:default (fn [t v] nil)} (slurp (first *command-line-args*)))) 0))' "$cassette" 2>/dev/null || echo 0)
                 :log "cassette.log"
                 :note "the evidence store is the one input C509 called unpinnable; the cassette pins its ANSWERS, reached through the existing FUTON3C_EVIDENCE_BASE seam (pattern_registry.clj:33-38)"}
 :step/world {:before "world-before.edn" :after "world-after.edn"}
 :step/note "the sandbox is restored from the pin before the tick, so two steps of the same pin are steps from the same state"}
EOF
  say "step $idx-$label: run-id $run_id, normalized sha $norm_sha"
  say "step $idx-$label: $dir"
  echo "$dir" > "$work/.last-step"
  echo "$dir"
  return $rc
}

# --------------------------------------------------------------------------
cmd_compare() {
  local work="$1" a="$2" b="$3"
  [ -f "$a/normalized.edn" ] || die "no normalized.edn in $a"
  [ -f "$b/normalized.edn" ] || die "no normalized.edn in $b"
  local out="$work/compare-$(basename "$a")-vs-$(basename "$b").edn"
  bb "$RECORDS" compare "$a/normalized.edn" "$b/normalized.edn" "$out"
  local rc=$?
  # The world census beside the verdict: a divergence a moving input explains is
  # a different fact from a divergence the machine produced by itself.
  bb -e '
(let [[wa wb out] *command-line-args*
      rd #(clojure.edn/read-string {:default (fn [t v] nil)} (slurp %))
      a (rd wa) b (rd wb)
      idx (fn [w] (into {} (map (juxt :id identity)) (:world/files w)))
      ia (idx a) ib (idx b)
      moved (vec (for [k (sort (keys ia))
                       :let [x (get ia k) y (get ib k)]
                       :when (not= (:sha256 x) (:sha256 y))]
                   {:id k :path (:path x) :a (:sha256 x) :b (:sha256 y)}))
      ca (:world/commit-census a) cb (:world/commit-census b)
      census-moved (vec (for [k (sort-by str (distinct (concat (keys (:by-workstream ca)) (keys (:by-workstream cb)))))
                              :when (not= (get (:by-workstream ca) k) (get (:by-workstream cb) k))]
                          {:workstream k :a (get (:by-workstream ca) k) :b (get (:by-workstream cb) k)}))
      r {:world-drift/between [wa wb]
         :world-drift/moved moved
         :world-drift/count (count moved)
         :world-drift/mana-age [(get-in a [:world/mana-age :age-min]) (get-in b [:world/mana-age :age-min])]
         :world-drift/mana-stale [(get-in a [:world/mana-age :stale?]) (get-in b [:world/mana-age :stale?])]
         :world-drift/commit-census {:total [(:total ca) (:total cb)]
                                     :moved census-moved
                                     :note "the four :*-pct observation channels are this census divided by its total (war_machine.clj:3828-3836); a census that moves moves the observation"}
         :world-drift/note "a step cannot redirect these paths (C509 R7/R6), so it hashes them; the mana snapshot decides mode by AGE, not content (C509 T5)"}]
  (spit out (with-out-str (clojure.pprint/pprint r)))
  (println (format "world-drift: %d of %d hashed input(s) moved between the two steps%s"
                   (count moved) (count ia)
                   (if (seq moved) (str " -- " (clojure.string/join ", " (map (comp name :id) moved))) "")))
  (println (format "world-drift: commit census total %s -> %s%s"
                   (:total ca) (:total cb)
                   (if (seq census-moved)
                     (str " -- moved: " (clojure.string/join ", " (map #(str (name (:workstream %)) " " (:a %) "->" (:b %)) census-moved)))
                     " (unchanged)")))
  (println (format "world-drift: mana age %.2f -> %.2f min, stale? %s -> %s"
                   (double (or (get-in a [:world/mana-age :age-min]) -1.0))
                   (double (or (get-in b [:world/mana-age :age-min]) -1.0))
                   (get-in a [:world/mana-age :stale?]) (get-in b [:world/mana-age :stale?]))))' \
     "$a/world-before.edn" "$b/world-before.edn" "$work/world-drift-$(basename "$a")-vs-$(basename "$b").edn"
  return $rc
}

# --------------------------------------------------------------------------
cmd_determinism() {
  local work="$1"; shift
  local a b
  cmd_step "$work" det-a "$@" > /dev/null || { say "determinism: step A failed"; return 6; }
  a="$(cat "$work/.last-step")"
  cmd_step "$work" det-b "$@" > /dev/null || { say "determinism: step B failed"; return 6; }
  b="$(cat "$work/.last-step")"
  say "determinism: comparing $a and $b"
  cmd_compare "$work" "$a" "$b"
  local rc=$?
  if [ $rc -eq 0 ]; then say "determinism: IDENTICAL -- two steps from one pin agree on every decision field outside the exclusion list"
  else say "determinism: DIVERGENT -- see the differing paths above and the world-drift census beside them"; fi
  return $rc
}

# --------------------------------------------------------------------------
cmd_plant() {
  local work="$1" what="${2:-mu-uniform}"
  [ -d "$work/pin/wm-trace" ] || die "no pin at $work/pin"
  bb "$RECORDS" plant "$work/pin/wm-trace" "$what" | tee "$work/plant-$what.edn"
  say "plant: the pin now differs from its manifest; step refuses until --allow-pin-drift"
}

# --------------------------------------------------------------------------
# The catalogued check battery, run against ONE accepted step. All EIGHT checks
# in run-era-ledger.edn's :ledger/check-catalogue have a `--deposit <run-id>`
# path (RE3/RE6/RE7 built them); this runs all eight, plus RUN3, which is not a
# catalogued check but the measurement that writes the conformance verdict the
# run-conformance transcription reads and cites.
#
# TWO PASSES, AND THAT IS THE LEDGER'S RULE NOT A LIMITATION HERE: append-row!
# refuses a row whose artifact is untracked or dirty ("a ledger row may only
# point at a committed, unmodified file"). So the first pass produces the
# artifacts and its deposits are refused; the operator commits the run store;
# `wm_step.sh deposit` re-runs the same battery and the rows land. Each check is
# written to rewrite its receipt byte-identically, so the second pass is a
# replay and not a second measurement.
#
# TWO OF THE NINE NEED THE REPO ROOT AS CWD, and it is a classpath fact, not a
# preference: bb.edn declares :paths ["."], so `checks/contract_authority_current.clj`
# (which requires writer-fence-capability from the root) resolves only from there,
# and `u37_enumeration_replay.clj` needs src/ on the classpath, which only
# `clojure -M:test` gives it. Run from the lab under bb they fail to LOAD, and a
# check that failed to load reads exactly like a check that had nothing to say.
# --------------------------------------------------------------------------
cmd_battery() {
  local work="$1" run_id="$2"
  local store="$LAB/runs/$run_id"
  [ -d "$store" ] || die "no run store at $store"
  local date_str; date_str="$(basename "$(ls "$store"/wm-trace-*.edn | head -1)" .edn | sed 's/^wm-trace-//')"
  local battery="$store/battery.log"
  : > "$battery"
  # Lean identifier fragments: a slug that starts with a digit or carries a dash
  # is not a Lean name, so the run id cannot be the slug.
  local U49_SLUG RE7_SLUG
  U49_SLUG="step$(echo "$run_id" | tr -cd '[:alnum:]')"
  RE7_SLUG="$U49_SLUG"
  run_check() { # $1 label, $2 cwd, rest: command
    local label="$1" cwd="$2"; shift 2
    echo "=== $label (cwd $cwd): $* " >> "$battery"
    ( cd "$cwd" && "$@" ) >> "$battery" 2>&1
    local rc=$?
    echo "=== $label exit=$rc" >> "$battery"
    say "battery: $label exit=$rc"
  }
  run_check contract-pin "$ROOT" bb "$ROOT/checks/contract_authority_current.clj" --deposit "$run_id"
  run_check flip-readiness "$LAB" bb "$LAB/flip_readiness_check.bb" --deposit "$run_id"
  # RUN3 is the MEASUREMENT, not one of the eight catalogued checks: it writes
  # the run's conformance.edn, which u49 then transcribes and cites by
  # :checked-at. Re-running it on a second pass restamps that timestamp, and the
  # ledger then refuses u49's row as divergent for an existing
  # (run-id, check-id) -- correctly, because the row would name a different
  # measurement. So it runs once per run and a later pass reuses the verdict.
  if [ -f "$store/conformance.edn" ]; then
    say "battery: run3-conformance already measured (conformance.edn present); not re-measuring"
    echo "=== run3-conformance SKIPPED: conformance.edn already written" >> "$battery"
  else
    run_check run3-conformance "$LAB" env FUTON_WM_TRACE_DIR="$store" bb "$LAB/run3_conformance.bb" "runs/$run_id"
  fi
  run_check run-conformance "$LAB" env FUTON_WM_TRACE_DIR="$store" U49_RUN_DIR="runs/$run_id" U49_SLUG="$U49_SLUG" \
    U49_TRACE_FILE="runs/$run_id/wm-trace-$date_str.edn" U49_EMIT_TABLES=0 \
    bb "$LAB/u49_route_transcribe.bb" --deposit "$run_id" "runs/$run_id/u49"
  run_check per-node-runtime-validation "$LAB" bb "$LAB/runtime_validation_check.bb" --deposit "$run_id"
  # `clojure -M:test`, not bb: the replay requires futon2.aif.enumeration-completeness
  # from src/, which bb.edn's :paths ["."] does not carry. This is how its own
  # controls run it (u37_enumeration_controls.sh:31).
  run_check enumeration-completeness "$ROOT" env FUTON_WM_TRACE_DIR="$store" clojure -M:test holes/labs/wm-contract/u37_enumeration_replay.clj --deposit "$run_id"
  run_check rationale-regret "$LAB" bb "$LAB/u39_selection_retrospective.bb" --deposit "$run_id"
  run_check tensions-cashed "$LAB" bb "$LAB/u41_tension_ledger.bb" --deposit "$run_id"
  run_check selection-discrimination "$LAB" env RE7_RUN_ID="$run_id" RE7_RATIONALE_DIR="runs/$run_id/rationale" \
    RE7_TRACE="runs/$run_id/wm-trace-$date_str.edn" RE7_SLUG="$RE7_SLUG" \
    bb "$LAB/re7_selection_discrimination.bb" --deposit "$run_id" "runs/RE7-selection-discrimination/$run_id"
  ( cd "$LAB" && bb run_era_ledger.bb --check > /dev/null ) \
    && say "battery: run-era ledger --check green" \
    || say "battery: RUN-ERA LEDGER --check FAILED"
  local deposited
  deposited=$(grep -c "exit=0$" "$battery")
  say "battery: $deposited of the battery's commands exited 0 -- eight catalogued checks plus RUN3 (log: $battery)"
  say "battery: rows the ledger refused as :artifact-untracked are the FIRST pass;"
  say "battery: commit runs/$run_id (and the RE3/RE6/RE7 receipts) then: wm_step.sh deposit $work $run_id"
}

# --------------------------------------------------------------------------
# accept: advance the pin, deposit the step as a run, run the check battery.
# --------------------------------------------------------------------------
cmd_accept() {
  local work="$1" dir="$2" run_id="${3:-}"
  [ -f "$dir/step.edn" ] || die "no step.edn in $dir"
  # ACCEPT ADVANCES THE PIN FROM THE SANDBOX THAT STEP LEFT, and every `step`
  # resets the sandbox first, so only the most recent step is acceptable. The
  # alternative -- reconstructing an older step's post-state by replaying its
  # records into the pin -- would be a second, unverified way of building the
  # state the machine actually had. Re-step to accept an earlier one.
  local last=""; [ -f "$work/.last-step" ] && last="$(cat "$work/.last-step")"
  [ "$last" = "$dir" ] || die "the sandbox holds $last, not $dir -- accept takes the most recent step; re-step to accept another"
  local tick_rc step_run_id date_str
  tick_rc="$(bb -e '(println (:step/tick-exit (clojure.edn/read-string (slurp (first *command-line-args*)))))' "$dir/step.edn")"
  [ "$tick_rc" = "0" ] || die "step $dir exited $tick_rc -- accept refuses a step whose tick did not complete"
  step_run_id="$(bb -e '(println (:step/run-id (clojure.edn/read-string (slurp (first *command-line-args*)))))' "$dir/step.edn")"
  [ -n "$step_run_id" ] || die "step $dir appended no record carrying a :run/id"
  date_str="$(date -u '+%Y-%m-%d')"
  [ -n "$run_id" ] || run_id="$date_str-$(basename "$dir")"

  local store="$LAB/runs/$run_id"
  [ -e "$store" ] && die "run store runs/$run_id already exists"
  mkdir -p "$store/rationale"
  # The run store is SELF-CONTAINED: the extraction is named wm-trace-<date>.edn
  # so that by-id selection (run3_conformance) finds it with the store itself as
  # the trace dir, and RE7's run-store trace regex matches it. Nothing in the
  # battery then depends on the sandbox surviving.
  cp -p "$dir/decision-records.edn" "$store/wm-trace-$date_str.edn"
  cp -p "$dir"/tick-run-record-*.edn "$store/" 2>/dev/null
  cp -p "$dir/rationale"/*.edn "$store/rationale/" 2>/dev/null
  cp -p "$dir/step.edn" "$dir/world-before.edn" "$dir/world-after.edn" "$dir/delta.edn" "$dir/normalized.edn" "$store/" 2>/dev/null

  cat > "$store/README.md" <<EOF
# $run_id — one accepted step (worklist \`:U55\`, \`EPIC-run-era.md\`)

One tick, run by \`wm_step.sh step\` from the pin at \`$work/pin\` into the
sandbox \`$work/sandbox\`, at futon2 sha \`$(git_sha "$ROOT")\`. The live
\`data/wm-trace\` was not written: the tick's trace, its RE4 rationale and its
receipt were redirected by \`FUTON_WM_TRACE_DIR\` / \`FUTON_WM_RECEIPT_DIR\`
(\`scripts/futon2/run_tick_once.clj\`, \`sandbox\`), and the receipt carries
\`:stepSandbox\` saying so. The live run lock (RUN12) was held across the tick.

Run id \`$step_run_id\`. Accepted from \`$dir\`.
\`wm-trace-$date_str.edn\` is this step's appended record(s), selected from the
sandbox corpus by \`:run/id\`.
EOF

  say "accept: deposited $store"

  cmd_battery "$work" "$run_id"

  # ---- advance the pin ------------------------------------------------------
  # A red row is a VERDICT, not a tool failure, so it does not block the
  # advance; a step whose tick did not complete never got this far.
  local gen
  gen="$(bb -e '(println (:pin/generation (clojure.edn/read-string (slurp (first *command-line-args*)))))' "$work/pin/pin.edn")"
  rm -rf "$work/pin/wm-trace"
  mkdir -p "$work/pin/wm-trace"
  cp -p "$work/sandbox/wm-trace"/*.edn "$work/pin/wm-trace/"
  bb "$RECORDS" manifest "$work/pin/wm-trace" "$work/pin/manifest.edn" || die "manifest"
  bb "$RECORDS" world "$work/pin/world.edn" || true
  bb -e '
(let [[pin-path gen step-id run-id store] *command-line-args*
      p (clojure.edn/read-string (slurp pin-path))
      p (assoc p :pin/generation (inc (Long/parseLong gen))
                 :pin/advanced-at (str (java.time.Instant/now))
                 :pin/accepted-steps (conj (vec (:pin/accepted-steps p))
                                           {:step step-id :run-id run-id :store store}))]
  (spit pin-path (with-out-str (clojure.pprint/pprint p))))' \
     "$work/pin/pin.edn" "$gen" "$(basename "$dir")" "$run_id" "holes/labs/wm-contract/runs/$run_id"
  say "accept: pin advanced to generation $((gen+1)); the accepted step's records are now the state the next step reads"
  cmd_reset "$work"
}

# --------------------------------------------------------------------------
# check: run a catalogued check against ONE step without accepting it. This is
# the "observe" half of the fix-defect loop -- step, look, fix, reset, re-step,
# look again -- and it deposits NOTHING: a run-era ledger row is a claim about
# an accepted run, and a step under inspection is not one yet.
# --------------------------------------------------------------------------
cmd_check() {
  local work="$1" dir="$2" which="${3:-selection-discrimination}"
  [ -f "$dir/step.edn" ] || die "no step.edn in $dir"
  local abs; abs="$(cd "$dir" && pwd)"
  case "$which" in
    selection-discrimination)
      ( cd "$LAB" && env RE7_RUN_ID="$(basename "$dir")" \
          RE7_RATIONALE_DIR="$abs/rationale" \
          RE7_TRACE="$abs/decision-records.edn" \
          RE7_SLUG="chk$(basename "$dir" | tr -cd '[:alnum:]')" \
          bb "$LAB/re7_selection_discrimination.bb" "$abs/re7" )
      ;;
    *) die "unknown check $which" ;;
  esac
}

# --------------------------------------------------------------------------
cmd_status() {
  local work="$1"
  [ -d "$work/pin" ] || die "no pin at $work/pin"
  cat "$work/pin/pin.edn"
  echo "steps:"
  for d in "$work"/steps/*/; do
    [ -d "$d" ] || continue
    echo "  $(basename "$d")  $(bb -e '(let [s (clojure.edn/read-string (slurp (first *command-line-args*)))] (println (:step/run-id s) (:step/tick-exit s) (:step/normalized-sha256 s)))' "$d/step.edn" 2>/dev/null)"
  done
}

# --------------------------------------------------------------------------
CMD="${1:-}"; shift || true
case "$CMD" in
  init) cmd_init "$@" ;;
  reset) cmd_reset "$@" ;;
  step) cmd_step "$@" ;;
  accept) cmd_accept "$@" ;;
  deposit) cmd_battery "$@" ;;
  compare) cmd_compare "$@" ;;
  check) cmd_check "$@" ;;
  determinism) cmd_determinism "$@" ;;
  plant) cmd_plant "$@" ;;
  status) cmd_status "$@" ;;
  *) sed -n '1,20p' "${BASH_SOURCE[0]}"; exit 2 ;;
esac
