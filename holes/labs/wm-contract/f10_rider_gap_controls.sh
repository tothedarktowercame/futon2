#!/usr/bin/env bash
# Controls for f10_rider_gap_scan.bb.  Every plant states the checks it must
# move, and the run is rejected unless it moves EXACTLY those.  Nothing here
# writes to the repository: the scan plants live in a throwaway git repo under
# $tmp, the cohort plant in a copied data root, and the null control writes its
# artifact to $tmp.
set -euo pipefail
repo=/home/joe/code/futon2
checker="$repo/holes/labs/wm-contract/f10_rider_gap_scan.bb"
artifact="$repo/holes/labs/wm-contract/runs/F10-outcome-domain/04-rider-gap.edn"
data=$repo/data/wm-full-loop
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# Runs the checker over a scan root and echoes its FAILED-CHECKS line.  A run
# that PASSES echoes nothing, so "moved exactly nothing" is expressible.
run_plant () {  # run_plant <scan-root> <data-root>
  local out rc
  set +e
  out=$(F10RG_SCAN_ROOT="$1" F10RG_DATA_ROOT="$2" F10RG_ARTIFACT="$tmp/out.edn" bb "$checker" 2>&1); rc=$?
  set -e
  if [ "$rc" -eq 0 ]; then echo ""; else
    echo "$out" | sed -n 's/^FAILED-CHECKS: //p'
  fi
}
expect () {  # expect <label> <scan-root> <data-root> <expected FAILED-CHECKS>
  local got; got=$(run_plant "$2" "$3")
  if [ "$got" != "$4" ]; then
    echo "CONTROL $1 WRONG: expected moved=[$4] got=[$got]" >&2; exit 1
  fi
  if [ -z "$4" ]; then echo "CONTROL $1 ACCEPTED (moved nothing)"
  else echo "CONTROL $1 REJECTED moved=[$4]"; fi
}

before=$(sha256sum "$artifact" | cut -d' ' -f1)
F10RG_ARTIFACT="$tmp/null.edn" bb "$checker" >/dev/null
if ! cmp -s "$artifact" "$tmp/null.edn"; then
  echo "CONTROL null FAILED: re-running the scan does not reproduce the committed artifact" >&2
  diff <(head -c 4000 "$artifact") <(head -c 4000 "$tmp/null.edn") | head -20 >&2 || true
  exit 1
fi
echo "CONTROL null ACCEPTED artifact-sha256=$before"

# A miniature git work tree, so the plants run through the SAME path source as
# the real route (git grep -l over tracked files) rather than the fallback walk.
mk_repo () {
  rm -rf "$tmp/scan"; mkdir -p "$tmp/scan"
  git -C "$tmp/scan" init -q
  git -C "$tmp/scan" config user.email c@example.invalid
  git -C "$tmp/scan" config user.name controls
  mkdir -p "$tmp/scan/src/futon2/aif" "$tmp/scan/scripts" "$tmp/scan/test" \
           "$tmp/scan/checks" "$tmp/scan/holes/labs/wm-contract" "$tmp/scan/tools"
  printf '(ns futon2.aif.ruled-outcome-c)\n' > "$tmp/scan/src/futon2/aif/ruled_outcome_c.clj"
  git -C "$tmp/scan" add -A; git -C "$tmp/scan" commit -qm base
}
commit_all () { git -C "$tmp/scan" add -A; git -C "$tmp/scan" commit -qm plant; }

# 0. The declaration alone is not a caller of itself.
mk_repo
expect declaration-only "$tmp/scan" "$data" ""

# 1. A committed consumer under src/ is a production caller.
printf '(ns probe)\n(require (quote futon2.aif.ruled-outcome-c))\n' > "$tmp/scan/src/probe.clj"
commit_all
expect committed-production-caller "$tmp/scan" "$data" ":no-production-caller"

# 2. The same file UNCOMMITTED is invisible, and that is the tracked-only rule
#    the artifact's :path-source names -- stated here rather than left implicit.
mk_repo
printf '(ns probe)\n(require (quote futon2.aif.ruled-outcome-c))\n' > "$tmp/scan/src/probe.clj"
expect untracked-production-caller "$tmp/scan" "$data" ""

# 3. A scan that reads nothing is a failure, not a pass: without the
#    declaration in its own hit set the "no production caller" claim is vacuous.
mk_repo
git -C "$tmp/scan" rm -q src/futon2/aif/ruled_outcome_c.clj
commit_all
expect empty-scan "$tmp/scan" "$data" ":declaration-found"

# 4. A mention outside the five scan roots is caught rather than unseen -- this
#    is what stops "no production caller" from being scoped by the allowlist.
mk_repo
printf '(ns tools.probe)\n;; ruled-outcome-c\n' > "$tmp/scan/tools/probe.clj"
commit_all
expect mention-outside-scan-roots "$tmp/scan" "$data" ":mentions-within-scan-roots"

# 5. A newer cohort attempt directory moves the cohort pin only.
cp -a "$data" "$tmp/data"
mkdir -p "$tmp/data/wm-outer-loop-46-v1/attempt-999"
touch -d '2030-01-01T00:00:00Z' "$tmp/data/wm-outer-loop-46-v1/attempt-999"
expect newer-attempt-copy "$repo" "$tmp/data" ":newest-cohort-attempt"

after=$(sha256sum "$artifact" | cut -d' ' -f1)
[ "$before" = "$after" ]
echo "ALL CONTROLS PASS artifact-restored-sha256=$after"
