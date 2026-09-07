#!/usr/bin/env bash
set -euo pipefail
repo=/home/joe/code/futon2
checker="$repo/holes/labs/wm-contract/f10_live_run_check.bb"
decl="$repo/src/futon2/aif/ruled_outcome_c.clj"
trace="$repo/data/wm-trace/wm-trace-2026-09-07.edn"
artifact="$repo/holes/labs/wm-contract/runs/F10-outcome-domain/03-live-run.edn"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

run_copy() {
  local name=$1 old=$2 new=$3 expected=$4
  cp "$decl" "$tmp/$name.clj"
  python3 - "$tmp/$name.clj" "$old" "$new" <<'PY'
import pathlib,sys
p=pathlib.Path(sys.argv[1]); old=sys.argv[2]; new=sys.argv[3]; s=p.read_text(); before=s.count(old)
assert before > 0
p.write_text(s.replace(old,new,1))
t=p.read_text(); assert new in t and t.count(old) == before-1
PY
  set +e
  out=$(F10LR_DECL="$tmp/$name.clj" F10LR_ARTIFACT="$tmp/out.edn" bb "$checker" 2>&1); rc=$?
  set -e
  [ "$rc" -eq 1 ] || { echo "CONTROL $name FAILED rc=$rc"; exit 1; }
  moved=$(printf '%s\n' "$out" | sed -n 's/^FAILED-CHECKS: //p' | tail -1)
  [ "$moved" = "$expected" ] || { echo "CONTROL $name WRONG [$moved]"; exit 1; }
  echo "CONTROL $name REJECTED moved=[$moved]"
}

before=$(sha256sum "$artifact" | cut -d' ' -f1)
F10LR_ARTIFACT="$tmp/null.edn" bb "$checker" >/dev/null
cmp -s "$artifact" "$tmp/null.edn"
echo "CONTROL null ACCEPTED artifact-sha256=$before"

run_copy ruled-folded ':folded? false
    :in-ruled-sum :yes' ':folded? true
    :in-ruled-sum :yes' ':ruled-outcome-c-absent-live'
run_copy c-int-unfolded ':folded? true
    :in-ruled-sum :no' ':folded? false
    :in-ruled-sum :no' ':c-int-floor-attested'
run_copy changed-mass ':grounded-change 1/2' ':grounded-change 2/5' ':seeded-c-live-valid'

# Trace plant: insert a ruled layer into the first tick's first live vector.
cp "$trace" "$tmp/injected.edn"
python3 - "$tmp/injected.edn" <<'PY'
import pathlib,sys
p=pathlib.Path(sys.argv[1]); s=p.read_text(); old=':value [{:layer/id :floor'; new=':value [{:layer/id :ruled-outcome-c :folded? true} {:layer/id :floor'; before=s.count(old)
assert before > 0; p.write_text(s.replace(old,new,1)); t=p.read_text(); assert new in t and t.count(old) == before-1
PY
ish=$(sha256sum "$tmp/injected.edn" | cut -d' ' -f1)
set +e
out=$(F10LR_TRACE="$tmp/injected.edn" F10LR_TRACE_SHA="$ish" F10LR_ARTIFACT="$tmp/out.edn" bb "$checker" 2>&1); rc=$?
set -e
[ "$rc" -eq 1 ] && [[ "$out" == *'FAILED-CHECKS: :ruled-outcome-c-absent-live'* ]]
echo 'CONTROL injected-live-layer REJECTED moved=[:ruled-outcome-c-absent-live]'

# Trace plant, added in review: remove the floor layer from the trace itself.
# Before the `seq` repair in the checker this copy PASSED -- the artifact
# carried :floor #{} and :c-int-floor-attested stayed true, so the check's one
# positive claim about the live run was satisfied by its own absence. The
# declaration-copy control above cannot reach this: it moves the same conjunct
# through the :folded? flag and never touches the trace.
cp "$trace" "$tmp/no-floor.edn"
python3 - "$tmp/no-floor.edn" <<'PY'
import pathlib,sys
p=pathlib.Path(sys.argv[1]); s=p.read_text(); old=':layer/id :floor'; before=s.count(old)
assert before > 0; p.write_text(s.replace(old, ':layer/id :floor-renamed'))
t=p.read_text(); assert ':layer/id :floor-renamed' in t and t.count(old+',') == 0
PY
nfsh=$(sha256sum "$tmp/no-floor.edn" | cut -d' ' -f1)
set +e
out=$(F10LR_TRACE="$tmp/no-floor.edn" F10LR_TRACE_SHA="$nfsh" F10LR_ARTIFACT="$tmp/out.edn" bb "$checker" 2>&1); rc=$?
set -e
[ "$rc" -eq 1 ] && [[ "$out" == *'FAILED-CHECKS: :c-int-floor-attested'* ]]
echo 'CONTROL absent-floor-layer REJECTED moved=[:c-int-floor-attested]'

set +e
out=$(F10LR_TRACE="$repo/data/wm-trace/wm-trace-2026-09-04.edn" F10LR_ARTIFACT="$tmp/out.edn" bb "$checker" 2>&1); rc=$?
set -e
# Two checks move here, not one, and the second is the repair showing its work:
# the 2026-09-04 trace selects none of these run ids, so the tick list is empty,
# and an empty tick list can no longer satisfy :c-int-floor-attested vacuously.
# Before the repair this control moved :run-identity alone.
[ "$rc" -eq 1 ] && [[ "$out" == *'FAILED-CHECKS: :c-int-floor-attested :run-identity'* ]]
echo 'CONTROL prior-trace REJECTED moved=[:c-int-floor-attested :run-identity]'

after=$(sha256sum "$artifact" | cut -d' ' -f1)
[ "$before" = "$after" ]
echo "ALL CONTROLS PASS artifact-restored-sha256=$after"
