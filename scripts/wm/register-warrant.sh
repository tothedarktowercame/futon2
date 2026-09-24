#!/usr/bin/env bash
# register-warrant.sh <test-namespace> — register a NARROW test warrant.
#
# Usage:
#   AUTHOR=zai-1 scripts/wm/register-warrant.sh futon2.vm.standing-enacted-policy-is-cascade-g-test
#
# WHY NARROW: a registration config that declares :code-paths ["src"] and
# :test-paths ["test"] digests the WHOLE tree, so ANY commit anywhere under
# src/ stales EVERY warrant in the repo. In the shared checkout (five agents
# committing) such a warrant lives minutes and the War Machine's C1/C2
# observation checks go :unknown about their own compliance. The guarantee
# does not need wide scope: the registry separately pins the run's LOAD
# CLOSURE (every file the run actually loaded, written by the runner in the
# run JVM), so narrowing the declared scope to the files under test loses
# nothing that the warrant actually vouches for.
#
# This script derives that narrow scope from the test namespace:
#   test-paths = the test file itself
#   code-paths = the futon2/checks source files the test namespace requires
# (override with CODE_PATHS="a.clj b.clj" if the heuristic misses).
#
# Environment: AUTHOR (required), AGENCY_URL (default http://localhost:7070),
# ARTIFACT_DIR (default /home/joe/code/storage/test-registry/artifacts).
#
# --pinned <commit>: register from a git worktree pinned at <commit> (a SIBLING
# of futon2, because deps.edn's local/root paths are ../futonN). This is not a
# convenience wrapper: since futon3c's :scope-not-committed guard (6f50e24b,
# 18ba2516), registering from the LIVE shared checkout refuses almost always,
# because git status in a tree five agents are editing is almost never empty.
# A pinned worktree is the mechanism that makes expensive registration
# possible at all — quiescent by construction, so the 22 s stable? window
# cannot be broken by another agent's commit, and the scope guard passes
# because a checked-out commit has no dirty or untracked files. The warrant
# validates on the LIVE checkout whenever the live bytes at its recorded
# repo-relative paths match the pinned commit's — :git-head is recorded but
# never compared. LAND <commit> in the live checkout before anyone checks, or
# the check correctly refuses :stale-sha.
set -euo pipefail

usage() { sed -n '2,26p' "$0"; exit 2; }
PINNED=""
case "${1:-}" in
  --pinned) [ $# -eq 3 ] || usage; PINNED="$2"; NS="$3" ;;
  *) [ $# -eq 1 ] || usage; NS="$1" ;;
esac
: "${AUTHOR:?Set AUTHOR=<agent-id> (required)}"
AGENCY_URL="${AGENCY_URL:-http://localhost:7070}"
ARTIFACT_DIR="${ARTIFACT_DIR:-/home/joe/code/storage/test-registry/artifacts}"

FUTON2="$(cd "$(dirname "$0")/../.." && pwd)"
FUTON3C="$(cd "$FUTON2/.." && pwd)/futon3c"

# --- pinned worktree (optional) --------------------------------------------
WT=""
cleanup() { [ -n "$WT" ] && git -C "$FUTON2" worktree remove --force "$WT" 2>/dev/null || true; }
trap cleanup EXIT
if [ -n "$PINNED" ]; then
  WT="$FUTON2/../wt-warrant-$(printf '%s' "$PINNED" | head -c 8)"
  git -C "$FUTON2" worktree add --detach "$WT" "$PINNED" >/dev/null
  ROOT="$WT"
  echo "--- worktree $ROOT at $PINNED"
else
  ROOT="$FUTON2"
fi

ns_path() { printf '%s' "$1" | tr '.' '/' | tr '-' '_'; }

# --- test file -------------------------------------------------------------
test_rel=""
for cand in "test/$(ns_path "$NS").clj" "test/$(ns_path "$NS").cljc"; do
  [ -f "$ROOT/$cand" ] && test_rel="$cand" && break
done
[ -n "$test_rel" ] || { echo "No test file for $NS under $ROOT/test" >&2; exit 1; }

# --- code paths: namespaces the test file requires, resolved to files ------
if [ -n "${CODE_PATHS:-}" ]; then
  # shellcheck disable=SC2206
  code_paths=($CODE_PATHS)
else
  mapfile -t reqs < <(
    grep -oE '\[(futon2|checks)[a-zA-Z0-9._-]*' "$ROOT/$test_rel" \
      | sed 's/^\[//' | sort -u)
  code_paths=()
  for r in "${reqs[@]:-}"; do
    [ -z "$r" ] && continue
    p="$(ns_path "$r")"
    for cand in "src/$p.clj" "src/$p.cljc" "$p.clj" "$(dirname "$p")/$(basename "$p").clj"; do
      if [ -f "$ROOT/$cand" ] && [ "$cand" != "$test_rel" ]; then
        code_paths+=("$cand"); break
      fi
    done
  done
fi
if [ ${#code_paths[@]} -eq 0 ]; then
  echo "Could not derive code-paths for $NS; pass CODE_PATHS=\"<repo-relative files>\"" >&2
  exit 1
fi

# --- config ----------------------------------------------------------------
edn_list() { printf '['; printf '"%s" ' "$@"; printf ']'; }
CFG="$(mktemp /tmp/warrant-XXXXXX.edn)"
{
  printf '{\n'
  printf ':agency-url "%s"\n:origin "scripts/wm/register-warrant.sh"\n' "$AGENCY_URL"
  printf ':repo-root "%s"\n' "$ROOT"
  printf ':code-paths %s\n' "$(edn_list "${code_paths[@]}")"
  printf ':test-paths %s\n' "$(edn_list "$test_rel")"
  printf ':command ["clojure" "-M:test" "-n" "%s"]\n' "$NS"
  printf ':author "%s"\n:artifact-dir "%s"\n' "$AUTHOR" "$ARTIFACT_DIR"
  printf '}\n'
} > "$CFG"

echo "--- config ($CFG)"
cat "$CFG"
echo "--- registering (this runs the namespace once, ~its suite time)"
( cd "$FUTON3C" && clojure -M -m futon3c.test-registry run "$CFG" )
