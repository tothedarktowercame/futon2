#!/usr/bin/env bash
set -euo pipefail
LAB="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
T="$(mktemp -d)"
trap 'rm -rf "$T"' EXIT
mkdir -p "$T/lab" "$T/code/mathlib4/DarkTower/WarMachine"
cp "$LAB/channel-positive-receipt.edn" "$T/lab/channel-positive-receipt.edn"
cp "$HOME/code/mathlib4/DarkTower/WarMachine/ChannelWitness.lean" "$T/code/mathlib4/DarkTower/WarMachine/ChannelWitness.lean"
cp "$HOME/code/mathlib4/DarkTower/WarMachine/Holes.lean" "$T/code/mathlib4/DarkTower/WarMachine/Holes.lean"
printf '{:schema :positive-receipt-known-stale/v1 :entries []}\n' > "$T/known.edn"
bb "$LAB/positive_receipt_reattestation_check.bb" --lab "$T/lab" --code-root "$T/code" --known "$T/known.edn" >"$T/good.out"
grep -q 'PASS' "$T/good.out"
sed -i '0,/theorem declaredVocabulary/s//theorem declaredVocabulary \/- U71 planted drift -\//' "$T/code/mathlib4/DarkTower/WarMachine/ChannelWitness.lean"
if bb "$LAB/positive_receipt_reattestation_check.bb" --lab "$T/lab" --code-root "$T/code" --known "$T/known.edn" >"$T/bad.out" 2>&1; then
  echo "positive_receipt_reattestation_controls: FAIL -- planted declaration drift passed" >&2
  exit 1
fi
grep -q 'UNATTESTED-DRIFT' "$T/bad.out"
grep -q 'moved without a re-emitted receipt' "$T/bad.out"
echo "positive_receipt_reattestation_controls: PASS -- fresh receipt accepted; planted unre-attested drift rejected"
