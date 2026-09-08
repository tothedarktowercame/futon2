#!/usr/bin/env bash
set -euo pipefail
repo=/home/joe/code/futon2
lab="$repo/holes/labs/wm-contract"
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT

# Positive control: the live manifest includes the named F10 expected gap.
bb "$lab/production_caller_check.bb" >/dev/null

# Negative control: a claimed declaration that exists but has no production
# caller must be a first-class red naming that declaration.
cp "$lab/production-caller-manifest.edn" "$tmp_dir/manifest.edn"
bb -e "(require '[clojure.edn :as e]) (let [p \"$tmp_dir/manifest.edn\" d (e/read-string (slurp p)) c {:id :planted-consumed-without-caller :needles [\"def planted-consumed-without-caller\"] :declaration-path \"holes/labs/wm-contract/production_caller_controls.sh\" :basis \"holes/labs/wm-contract/production_caller_controls.sh:12\"}] (spit p (pr-str (update d :claims conj c))))"
if WMPC_MANIFEST="$tmp_dir/manifest.edn" bb "$lab/production_caller_check.bb" >"$tmp_dir/out" 2>&1; then
  echo "production_caller_controls: planted missing caller was accepted" >&2
  exit 1
fi
grep -q 'RED planted-consumed-without-caller missing-caller' "$tmp_dir/out"
echo "production_caller_controls: PASS positive expected-gap and negative missing-caller"
