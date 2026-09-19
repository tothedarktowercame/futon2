#!/usr/bin/env bash
# :F10 slice 4 controls, AGAINST THE CHECKER.  Each plant is made in a COPY of
# the declaration the checker reads, pointed at through the checker's own
# `F10RF_NS`, and each must move EXACTLY the named checks -- not merely produce
# a nonzero exit.
#
# THE NULL CONTROL IS THE FIRST ONE AND IT IS WHY THE REST MEAN ANYTHING.  In
# the first version of this script (review, 2026-09-07) an UNMUTATED copy was
# also rejected: the checker excluded the declaration file from its caller scan
# by PATH, so with `F10RF_NS` on a copy the real
# `src/futon2/aif/ruled_outcome_c.clj` counted as a caller and every control
# passed for that reason instead of for its plant.
set -euo pipefail

repo=/home/joe/code/futon2
checker="$repo/holes/labs/wm-contract/f10_runtime_fold_check.bb"
artifact="$repo/holes/labs/wm-contract/runs/F10-outcome-domain/02-runtime-fold.edn"
source_file="$repo/src/futon2/aif/ruled_outcome_c.clj"
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT

before=$(sha256sum "$artifact" | cut -d' ' -f1)

# 0. NULL CONTROL: an unmutated copy through the same route must be ACCEPTED.
cp "$source_file" "$tmp_dir/null.clj"
if ! (cd "$repo" && F10RF_NS="$tmp_dir/null.clj" bb "$checker") >/dev/null 2>&1; then
  echo "NULL CONTROL FAILED: the checker rejects an unmutated copy, so no control below discriminates" >&2
  exit 1
fi
after=$(sha256sum "$artifact" | cut -d' ' -f1)
if [ "$before" != "$after" ]; then
  echo "NULL CONTROL FAILED: the artifact moved ($before -> $after)" >&2
  exit 1
fi
echo "CONTROL null ACCEPTED (artifact unchanged, sha256 $after)"

run_control() {
  local name=$1 old=$2 new=$3 expected=$4
  local copy="$tmp_dir/$name.clj" out moved
  cp "$source_file" "$copy"
  python3 - "$copy" "$old" "$new" <<'PY'
import pathlib, sys
p = pathlib.Path(sys.argv[1]); old = sys.argv[2]; new = sys.argv[3]
s = p.read_text()
if old not in s:
    raise SystemExit("plant did not land")
p.write_text(s.replace(old, new, 1))
PY
  if out=$( (cd "$repo" && F10RF_NS="$copy" bb "$checker") 2>&1 ); then
    echo "CONTROL $name FAILED (checker accepted mutant)" >&2
    exit 1
  fi
  moved=$(echo "$out" | grep -m1 '^FAILED-CHECKS:' | sed 's/^FAILED-CHECKS: //')
  if [ "$moved" != "$expected" ]; then
    echo "CONTROL $name MOVED [$moved], EXPECTED [$expected]" >&2
    exit 1
  fi
  echo "CONTROL $name REJECTED moved=[$moved]"
}

run_control support-symbol-drift \
  ":support disposition-outcomes" ":support observed-dispositions" \
  ":support-is-disposition-authority :support-width"

run_control c-mis-placement-drift \
  ":folded? false
    :in-ruled-sum :no
    :site \"futon2:src/futon2/aif/mission_c.clj\"" \
  ":folded? false
    :in-ruled-sum :yes
    :site \"futon2:src/futon2/aif/mission_c.clj\"" \
  ":c-mis-outside"

run_control missing-basis \
  ":basis \"futon2:src/futon2/aif/preferences.clj:9-24\"" ":basis \"\"" \
  ":fold-bases"

run_control retype-the-named-zeros \
  "(def ruled-vertices" \
  "(def ^:private retyped-vocabulary
  #{:grounded-no-change :artifact-only :abstained :guardrail-refusal
    :dispatch-failed :substrate-unavailable :cancelled})

(def ruled-vertices" \
  ":no-retyped-zeros"

run_control declare-it-unfolded \
  ":folded? true
    :in-ruled-sum :yes
    :site \"futon2:src/futon2/aif/efe.clj\"" \
  ":folded? false
    :in-ruled-sum :yes
    :site \"futon2:src/futon2/aif/efe.clj\"" \
  ":folded-claim-matches-callers"

echo "ALL 5 RUNTIME FOLD CONTROLS REJECTED AS REQUIRED, AND THE NULL CONTROL WAS ACCEPTED"
