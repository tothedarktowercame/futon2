#!/usr/bin/env bash
set -euo pipefail
repo=/home/joe/code/futon2
checker="$repo/holes/labs/wm-contract/f10_rider_gap_scan.bb"
artifact="$repo/holes/labs/wm-contract/runs/F10-outcome-domain/04-rider-gap.edn"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

before=$(sha256sum "$artifact" | cut -d' ' -f1)
F10RG_ARTIFACT="$tmp/null.edn" bb "$checker" >/dev/null
cmp -s "$artifact" "$tmp/null.edn"
echo "CONTROL null ACCEPTED artifact-sha256=$before"

mkdir -p "$tmp/scan/src/probe" "$tmp/scan/scripts" "$tmp/scan/test" "$tmp/scan/checks" "$tmp/scan/holes/labs/wm-contract"
printf '(ns probe)\n(require '\''futon2.aif.ruled-outcome-c)\n' > "$tmp/scan/src/probe/fake.clj"
grep -q 'ruled-outcome-c' "$tmp/scan/src/probe/fake.clj"
set +e
out=$(F10RG_SCAN_ROOT="$tmp/scan" F10RG_DATA_ROOT="$repo/data/wm-full-loop" F10RG_ARTIFACT="$tmp/out.edn" bb "$checker" 2>&1); rc=$?
set -e
[ "$rc" -eq 1 ] && [[ "$out" == *'FAILED-CHECKS: :no-production-caller'* ]]
echo 'CONTROL fake-production-caller REJECTED moved=[:no-production-caller]'

cp -a "$repo/data/wm-full-loop" "$tmp/data"
mkdir -p "$tmp/data/wm-outer-loop-46-v1/attempt-999"
touch -d '2030-01-01T00:00:00Z' "$tmp/data/wm-outer-loop-46-v1/attempt-999"
[ -d "$tmp/data/wm-outer-loop-46-v1/attempt-999" ]
set +e
out=$(F10RG_SCAN_ROOT="$repo" F10RG_DATA_ROOT="$tmp/data" F10RG_ARTIFACT="$tmp/out.edn" bb "$checker" 2>&1); rc=$?
set -e
[ "$rc" -eq 1 ] && [[ "$out" == *'FAILED-CHECKS: :newest-cohort-attempt'* ]]
echo 'CONTROL newer-attempt-copy REJECTED moved=[:newest-cohort-attempt]'

after=$(sha256sum "$artifact" | cut -d' ' -f1)
[ "$before" = "$after" ]
echo "ALL CONTROLS PASS artifact-restored-sha256=$after"
