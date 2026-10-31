#!/usr/bin/env bash
# Recover the adf04df0 delivery-failure evidence from the Agency invoke-jobs
# journal into content-addressed storage. The journal defaults to /tmp and does
# not survive a reboot; the committed record does. Fresh process; no session,
# no notes, no Agency state. Read-only on every input; the only writes are NEW
# append-only files in the delivery-failure evidence store and the report dir.
set -euo pipefail
cd "$(dirname "$0")/.."
exec clojure -M -e '(load-file "scripts/recover_adf04df0_evidence.clj")'
