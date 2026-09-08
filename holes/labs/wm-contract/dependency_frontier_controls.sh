#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/futon2/holes/labs"
cp -a "$HERE" "$TMP/futon2/holes/labs/wm-contract"
COPY="$TMP/futon2/holes/labs/wm-contract"
export WM_PATTERNS_DIR="/home/joe/code/futon3/library/apparatus"

# No-fourth-state commissioning: deleting one authoritative assurance must name
# the uncovered pattern/signature pair.
perl -0pi -e 's/\n  :assures \[\{:pattern "apparatus\/default-to-the-cheap-error".*?\}\]//' "$COPY/worklist.edn"
if bb "$COPY/gen_dependency_frontier.bb" >"$TMP/missing.out" 2>&1; then
  echo "dependency_frontier_controls: missing assurance was accepted" >&2
  exit 1
fi
grep -q 'uncovered or multiply-covered pattern/signature pair' "$TMP/missing.out"

# Staleness commissioning, both ways. A moved witnessed mechanism degrades;
# re-witnessing with the new blob hash holds witnessed.
cp "$HERE/worklist.edn" "$COPY/worklist.edn"
MECH="$COPY/positive_receipt_reattestation_check.bb"
printf '\n;; commissioned drift\n' >> "$MECH"
bb "$COPY/gen_dependency_frontier.bb" >/dev/null
grep -q '`apparatus/pin-moves-with-the-population` | `:ticketed` | `:U71`' "$COPY/dependency-frontier.md"
NEW_SHA="$(sha256sum "$MECH" | cut -d' ' -f1)"
sed -i "s/:mechanism-sha \"[0-9a-f]*\"/:mechanism-sha \"$NEW_SHA\"/" "$COPY/worklist.edn"
bb "$COPY/gen_dependency_frontier.bb" >/dev/null
grep -q '`apparatus/pin-moves-with-the-population` | `:witnessed` | `:U71`' "$COPY/dependency-frontier.md"
echo "dependency_frontier_controls: PASS -- missing pair named; stale witness degraded; re-witness held"
