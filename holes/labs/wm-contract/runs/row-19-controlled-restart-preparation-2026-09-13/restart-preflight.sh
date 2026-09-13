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
[[ -n "$source_acceptance" && -f "$source_acceptance" ]] || refuse ":restart/full-tree-acceptance-absent"
[[ -n "$ingress_acceptance" && -f "$ingress_acceptance" ]] || refuse ":restart/ingress-fence-acceptance-absent"

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

# A dirty broad classpath checkout cannot be certified by the HTTP-file pin.
if [[ -n "$(git -C /home/joe/code/futon3c status --porcelain --untracked-files=no)" ]]; then
  refuse ":restart/source-worktree-dirty"
fi

printf '{:schema :wm/row19-restart-preflight-v1\n'
printf ' :pid %s\n' "$pid"
printf ' :cwd %q\n' "$(readlink "/proc/$pid/cwd" 2>/dev/null || true)"
printf ' :cmdline-sha256 %q\n' "$(sha256sum "/proc/$pid/cmdline" 2>/dev/null | cut -d' ' -f1 || true)"
printf ' :service :futon3c-zone.service\n'
printf ' :ports [7070 6768]\n'
printf ' :hot-ledger {:path %q :sha256 %q}\n' /tmp/futon3c-invoke-jobs.edn "$(sha256sum /tmp/futon3c-invoke-jobs.edn 2>/dev/null | cut -d' ' -f1 || true)"
printf ' :status %s\n' "$([[ ${#refusals[@]} -eq 0 ]] && printf ':ready-for-independent-review' || printf ':refused')"
printf ' :refusals ['
printf ' %s' "${refusals[@]:-}"
printf ']}'
printf '\n'
[[ ${#refusals[@]} -eq 0 ]]
