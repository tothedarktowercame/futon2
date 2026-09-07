#!/usr/bin/env bash
set -euo pipefail

repo=/home/joe/code/futon2
checker="$repo/holes/labs/wm-contract/f10_runtime_fold_check.bb"
source_file="$repo/src/futon2/aif/ruled_outcome_c.clj"
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT

run_control() {
  local name=$1 old=$2 new=$3 fact=$4
  local copy="$tmp_dir/$name.clj"
  cp "$source_file" "$copy"
  python3 - "$copy" "$old" "$new" <<'PY'
import pathlib, sys
p = pathlib.Path(sys.argv[1]); old = sys.argv[2]; new = sys.argv[3]
s = p.read_text()
if old not in s:
    raise SystemExit("plant did not land")
p.write_text(s.replace(old, new, 1))
PY
  if (cd "$repo" && F10RF_NS="$copy" bb "$checker") >/dev/null 2>&1; then
    echo "CONTROL $name FAILED (checker accepted mutant)"
    exit 1
  fi
  echo "CONTROL $name REJECTED fact=$fact"
}

run_control support-symbol-drift ":support cohort/outcome-kinds" ":support observed-dispositions" ":support-width"
run_control c-mis-placement-drift ":in-ruled-sum :undeclared" ":in-ruled-sum :no" ":fold-entries"
run_control missing-basis ":basis \"futon2:src/futon2/aif/preferences.clj:9-24\"" ":basis \"\"" ":fold-entries"

echo "ALL 3 RUNTIME FOLD CONTROLS REJECTED AS REQUIRED"
