#!/usr/bin/env bash
set -euo pipefail

# Read-only preflight. This file never stops, reloads, starts, copies, or writes.
# A later operator must supply independently reviewed records for both controls.
pid="${1:-1942869}"
source_acceptance="${2:-}"
ingress_acceptance="${3:-}"
refusals=()

refuse() { refusals+=("$1"); }
[[ "$pid" =~ ^[0-9]+$ && -r "/proc/$pid/cmdline" ]] || refuse ":restart/process-unavailable"
# Paths are discovery inputs only. This script has no authority resolver and
# therefore never promotes their existence or contents to acceptance.
[[ -n "$source_acceptance" && -f "$source_acceptance" ]] || refuse ":restart/full-tree-acceptance-absent"
[[ -n "$ingress_acceptance" && -f "$ingress_acceptance" ]] || refuse ":restart/ingress-fence-acceptance-absent"
refuse ":restart/acceptance-authority-unverified"

if [[ -r "/proc/$pid/cgroup" ]]; then
  grep -qx '0::/user.slice/user-1000.slice/user@1000.service/futon.slice/futon-services.slice/futon3c-zone.service' "/proc/$pid/cgroup" \
    || refuse ":restart/service-identity-mismatch"
fi
[[ "$(readlink "/proc/$pid/cwd" 2>/dev/null || true)" == "/home/joe/code/futon3c" ]] \
  || refuse ":restart/cwd-mismatch"
[[ "$(sha256sum /home/joe/code/futon3c/src/futon3c/transport/http.clj | cut -d' ' -f1)" == \
   "defb1ed3b0deeb16ba15857f8efe5ac4e53ed6c442723dfae8808b38406f65fb" ]] \
  || refuse ":restart/retention-source-mismatch"
[[ -f /tmp/futon3c-invoke-jobs.edn ]] || refuse ":restart/hot-ledger-absent"
[[ -d /tmp/futon3c-invoke-jobs.edn.commissions ]] || refuse ":restart/archive-store-not-provisioned"

cwd="$(readlink "/proc/$pid/cwd" 2>/dev/null || true)"
cmd_sha="$(sha256sum "/proc/$pid/cmdline" 2>/dev/null | cut -d' ' -f1 || true)"
ledger_sha="$(sha256sum /tmp/futon3c-invoke-jobs.edn 2>/dev/null | cut -d' ' -f1 || true)"
source_path_status="$([[ -n "$source_acceptance" && -f "$source_acceptance" ]] && echo present-unverified || echo absent)"
ingress_path_status="$([[ -n "$ingress_acceptance" && -f "$ingress_acceptance" ]] && echo present-unverified || echo absent)"
printf '%s\n' "${refusals[@]}" | jq -Rsc \
  --argjson pid "$pid" --arg cwd "$cwd" --arg cmd "$cmd_sha" --arg ledger "$ledger_sha" \
  --arg source "$source_path_status" --arg ingress "$ingress_path_status" \
  '{schema:"wm/row19-restart-discovery-v2",status:"discovery-unverified",
    pid:$pid,cwd:$cwd,cmdline_sha256:$cmd,service:"futon3c-zone.service",ports:[7070,6768],
    hot_ledger:{path:"/tmp/futon3c-invoke-jobs.edn",sha256:$ledger},
    supplied_paths:{source_acceptance:$source,ingress_acceptance:$ingress},
    refusals:(split("\n")|map(select(length>0)))}'
exit 1
