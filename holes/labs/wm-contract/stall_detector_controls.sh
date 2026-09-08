#!/usr/bin/env bash
# Commission wm-build-loop's stall detector against a scratch ledger.  The
# live loop, ledger, log, and Agency notify route are never invoked.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

cp "$HERE/build_step.bb" "$TMP/build_step.bb"
sed -n '/^stall_check()/,/^}/p' "$HERE/wm-build-loop.sh" > "$TMP/stall_check.fn"

write_ledger() {
  local refused="$1"
  cat > "$TMP/worklist.edn" <<EOF
{:items [{:id :FIXTURE :class :I :status :open :owner :any
          :depends-on [] :refused $refused}]}
EOF
}

write_runner() {
  cat > "$TMP/run.sh" <<'EOF'
#!/usr/bin/env bash
set -u
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NOTIFIED=0
log() { printf '%s\n' "$*" >> "$HERE/detector.log"; }
notify() { NOTIFIED=1; printf 'notify:%s\n' "$1" >> "$HERE/notify.log"; }
STALL_ID=""; STALL_N=0
source "$HERE/stall_check.fn"
check() { stall_check "$(bb "$HERE/build_step.bb" stall-key)"; }
case "$1" in
  stall) check; check; check ;;
  health)
    check; check
    sed -i 's/:refused false/:refused true/' "$HERE/worklist.edn"
    check; check
    ;;
esac
EOF
  chmod +x "$TMP/run.sh"
}

capture() {
  local label="$1" mode="$2"
  rm -f "$TMP/detector.log" "$TMP/notify.log"
  set +e
  "$TMP/run.sh" "$mode" > "$TMP/$label.stdout" 2> "$TMP/$label.stderr"
  local rc=$?
  set -e
  printf '%s=%s\n' "$label" "$rc"
  printf '%s-log=%s\n' "$label" "$(test ! -f "$TMP/detector.log" || tr '\n' '|' < "$TMP/detector.log")"
  printf '%s-notify=%s\n' "$label" "$(test ! -f "$TMP/notify.log" || tr '\n' '|' < "$TMP/notify.log")"
  CAPTURE_RC="$rc"
}

write_runner
write_ledger false
fixture_before="$(sha256sum "$TMP/worklist.edn" | cut -d' ' -f1)"
capture induced-stall stall
stall_rc="$CAPTURE_RC"
test "$stall_rc" -eq 3
grep -q '^notify:stalled on row FIXTURE:' "$TMP/notify.log"

write_ledger false
fixture_health_before="$(sha256sum "$TMP/worklist.edn" | cut -d' ' -f1)"
capture induced-health health
health_rc="$CAPTURE_RC"
test "$health_rc" -eq 0
test ! -s "$TMP/notify.log"
fixture_health_after="$(sha256sum "$TMP/worklist.edn" | cut -d' ' -f1)"
test "$fixture_health_before" != "$fixture_health_after"

# Restore the pre-e84c114e projection in the harness copy.  In that version
# :refused was outside the selected fields, so the same healthy edit must be
# misdiagnosed as a stall; this is the mutation that the whole-row hash kills.
perl -0pi -e 's/\(hash i\)/\(hash \(select-keys i [:progress :evidence :slice-b2a :slice-b2b]\)\)/' "$TMP/build_step.bb"
grep -q 'select-keys i \[:progress :evidence :slice-b2a :slice-b2b\]' "$TMP/build_step.bb"
mutant_sha="$(sha256sum "$TMP/build_step.bb" | cut -d' ' -f1)"
write_ledger false
capture mutation-health health
mutation_rc="$CAPTURE_RC"
test "$mutation_rc" -eq 3
grep -q '^notify:stalled on row FIXTURE:' "$TMP/notify.log"

printf 'fixture-stall-sha256=%s\n' "$fixture_before"
printf 'fixture-health-before-sha256=%s\n' "$fixture_health_before"
printf 'fixture-health-after-sha256=%s\n' "$fixture_health_after"
printf 'mechanism-loop-sha256=%s\n' "$(sha256sum "$HERE/wm-build-loop.sh" | cut -d' ' -f1)"
printf 'mechanism-key-sha256=%s\n' "$(sha256sum "$HERE/build_step.bb" | cut -d' ' -f1)"
printf 'mutant-key-sha256=%s\n' "$mutant_sha"
printf 'PASS -- stall fired with notify; :refused reset stayed silent; old projection mutation fired\n'
