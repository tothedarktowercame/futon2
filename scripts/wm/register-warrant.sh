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
set -euo pipefail

usage() { sed -n '2,20p' "$0"; exit 2; }
[ $# -eq 1 ] || usage
NS="$1"
: "${AUTHOR:?Set AUTHOR=<agent-id> (required)}"
AGENCY_URL="${AGENCY_URL:-http://localhost:7070}"
ARTIFACT_DIR="${ARTIFACT_DIR:-/home/joe/code/storage/test-registry/artifacts}"

FUTON2="$(cd "$(dirname "$0")/../.." && pwd)"
FUTON3C="$(cd "$FUTON2/.." && pwd)/futon3c"

ns_path() { printf '%s' "$1" | tr '.' '/' | tr '-' '_'; }

# --- test file -------------------------------------------------------------
test_rel=""
for cand in "test/$(ns_path "$NS").clj" "test/$(ns_path "$NS").cljc"; do
  [ -f "$FUTON2/$cand" ] && test_rel="$cand" && break
done
[ -n "$test_rel" ] || { echo "No test file for $NS under $FUTON2/test" >&2; exit 1; }

# --- code paths: namespaces the test file requires, resolved to files ------
if [ -n "${CODE_PATHS:-}" ]; then
  # shellcheck disable=SC2206
  code_paths=($CODE_PATHS)
else
  mapfile -t reqs < <(
    grep -oE '\[(futon2|checks)[a-zA-Z0-9._-]*' "$FUTON2/$test_rel" \
      | sed 's/^\[//' | sort -u)
  code_paths=()
  for r in "${reqs[@]:-}"; do
    [ -z "$r" ] && continue
    p="$(ns_path "$r")"
    for cand in "src/$p.clj" "src/$p.cljc" "$p.clj" "$(dirname "$p")/$(basename "$p").clj"; do
      if [ -f "$FUTON2/$cand" ] && [ "$cand" != "$test_rel" ]; then
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
  printf ':repo-root "%s"\n' "$FUTON2"
  printf ':code-paths %s\n' "$(edn_list "${code_paths[@]}")"
  printf ':test-paths %s\n' "$(edn_list "$test_rel")"
  printf ':command ["clojure" "-M:test" "-n" "%s"]\n' "$NS"
  printf ':test-environment {"LC_ALL" "C.UTF-8" "LANG" "C.UTF-8" "TZ" "UTC"}\n'
  printf ':author "%s"\n:artifact-dir "%s"\n' "$AUTHOR" "$ARTIFACT_DIR"
  printf '}\n'
} > "$CFG"

echo "--- config ($CFG)"
cat "$CFG"
echo "--- registering (this runs the namespace once, ~its suite time)"
( cd "$FUTON3C" && clojure -M -m futon3c.test-registry run "$CFG" )
