# Independent review — retention/delivery-failure-evidence (zai-5)

Reviewed: futon2 `df2eb94e63eeb3d03ed1959d7fbe178bbad404ae` on
`retention/delivery-failure-evidence` (base `b2dd77218`, ancestor-verified),
in a read-only worktree. Includes the claude-1 commit `df2eb94e6` (the
runner close-path wiring) with the same scrutiny as zai-4's `637969d00` /
`adc696715`. No click, reload, merge, ledger write or source edit performed.
No suites rerun (per dispatch; the prior targeted-results summary is
consistent with everything I read).

## Verdict: APPROVE `df2eb94e63eeb3d03ed1959d7fbe178bbad404ae`

with two accuracy corrections to the narrative layer (docstring/report
wording, non-blocking) and one real, unmentioned-but-benign behavior change
in `:causes` (§2) that integrators should know.

## 1. Pin reaching the receiver — VERIFIED, single route

- `check-delivery` has exactly ONE production caller:
  `full_loop_runtime.clj:533`; retention is wired into that same
  `(not= :delivered …)` branch at `full_loop_runtime.clj:577-593`.
- The refusal map (with `:delivery-evidence`) becomes
  `persisted-cascade-execution` → thrown ex-data with
  `::held-open-cascade-resume`/`:downstream-refusal`
  (`full_loop_runner.clj:8345-8356`) → the close catch writes
  `:data (held-resume-data …)` (`full_loop_runner.clj:9547+`, the df2eb94e6
  refactor of the previously inline map — content-identical apart from the
  new conditional `:error-data`).
- `held-resume-data` puts `{:delivery-evidence … :author-job … :reason …}`
  into `:error-data` (`full_loop_runner.clj:1086-1107`); the run record
  write projects `:failure (run-record-failure result …)`
  (`full_loop_runner.clj:1519`); `:detail (failure-detail (:error-data d))`
  and the new `:delivery-evidence` map case in `failure-detail`'s bounded
  projection (`full_loop_runner.clj:996-1000`) carry the pin through. Path
  `[:failure :detail :delivery-evidence :pin]` is real, and the test
  composition mirrors the production call chain exactly (both `#'`-private
  fns called the way the close path calls them).
- Bypass check: the other `:downstream-refusal` sites are the no-selection
  sorry cell (`8252`, `8354` — the same persisted-cascade-execution carrier,
  same close catch) and the abstention/Q6 reads at `1362/1386`, which read
  `:answer-delivery` from checkpoints, not a delivery refusal close. No
  second delivery-refusal close route exists. The `product-check` branch
  (`:accepted`) is a different failure class, intentionally not retained.

## 2. Record-shape change for other injuries — one real, benign change

`held-resume-data` adds `:error-data` ONLY when `:delivery-evidence` is
present; the negative test (`a-held-refusal-without-retention…`) locks
`:detail {:absent :no-error-data}` for plain refusals. For failures that
DO carry evidence:

- `:detail` gains `:delivery-evidence`/`:author-job`/`:reason` — additive.
- **`:causes` changes, and the report does not mention it.**
  `refusal-cause-candidates` (`full_loop_runner.clj:1037-1061`) walks
  `error-data` as a container and projects `:reason`; at BASE the held-resume
  close passed `:error-data nil`, so its `:causes` was empty — the refusal's
  own reason never reached `:causes`. On the branch,
  `:error-data {:reason :abbreviated-commit-descriptor …}` yields one new
  candidate `{:reason …}`. The `:delivery-evidence` map itself projects no
  cause keys (`:status`/`:pin` only), `:author-job` doesn't project, and
  `distinct` dedupes. Net: `:causes` goes from `[]` to one truthful entry
  for evidence-carrying held resumes. This is an accuracy improvement, but
  any classifier or consumer keying on `:causes` emptiness/count for this
  close class will observe it. Repair classification keys I read
  (`failure-kind`, `kind`, `stage`) are unchanged by the addition.

## 3. Report/docstring accuracy — two corrections owed

- **"Durable" journal is wrong across a reboot.** The module docstring
  (`delivery_failure_evidence.clj:6-13`) correctly calls the
  `/tmp/futon3c-invoke-jobs.edn.journal` volatile; the script header and
  the report call it "the durable Agency invoke-jobs journal"
  (`recover-adf04df0-evidence.sh:2`, report §1). The journal lives in
  `/tmp` — a reboot (or tmpfiles cleanup) removes it, and the report's
  own premise ("terminal transcripts compact to tombstones after a day")
  says the store is time-limited anyway. The script header's "no /tmp
  notes" is also misleading phrasing: the script READS a /tmp artifact as
  its primary input. Correct claim: the recovery was reproducible while
  the journal file survives; the retained record + proposal are now
  content-addressed in git (committed under the report dir), which is the
  part that IS reboot-durable. "No sessions, no notes" as a process claim
  survives; "no /tmp dependency" does not.
- **"Input retained nowhere" overstates.** What was actually missing from
  the source record: a carrier reachable from the pinned tick record under
  the existing cause-entry contract. The exact check input was NOT
  retained nowhere — repair finding `repair-ea1-19929445…--attempt-002`
  independently holds the abbreviated AND full commit, the job id, the
  requisition and both digest levels, and git durably holds the authored
  blob (verified in the script's linkage assertions). The module
  docstring's own P4-2 claim is precise ("no retained carrier exists" —
  i.e. none at/linked-from the source record); the report's looser
  phrasing should match it.

## 4. `-Dfuton2.test-run` gate — acceptable, fail-safe, two notes

The gate (`full_loop_runtime.clj:586-591`) reads the property set by the
`:test` alias (futon2 `deps.edn:38`, same prop in futon3c's `:test` alias),
falling back to `nil` root under test, and `retain-delivery-failure!` with a
nil root returns `{:status :disabled :reason :no-store-root}` — a typed
absence that never throws into the failure path
(`delivery_failure_evidence.clj:152-155`). So the failure mode of a missing
property in a test JVM is a visible `:disabled` marker, not a canonical
write. Test paths through `full_loop_runtime`'s delivery branch without the
`:test` alias: none found — the only tests reaching that branch
(`delivery_seam_retention_test`) pass their own
`:delivery-failure-evidence-root`, and `:vm-test`/`:test-slow` are overlays
combined with `:test`. Notes: (a) a production JVM that accidentally carries
the property silently disables retention (the `:disabled` marker does land
in the record, so it is auditable, but nothing alarms); (b) any non-alias
execution driving a live tick (REPL, `clojure -M -e`) writes the canonical
store by design — intended, but worth knowing when replaying old scenarios.

## 5. Supplemental carrier — nothing admitted; smallest supported route

`cause_scope.clj` is untouched by the branch; the proposal
(`:wm/supplemental-carrier-proposal-v1`, committed alongside the retained
record) is proposal-only, and `evidence-readiness` stamps
`:carrier-admission-requires-review` / `:readiness-is-not-diagnosis`.
Nothing on the branch admits it. For adf04df0's historical record (which
predates retention and will never carry the pin in-source): the smallest
supported route is exactly the review-gated one — admit a NARROW contract
amendment to `cause-scope` (seal/assert-repair) allowing `input.record-pin`
to be the source-record pin PLUS one linked supplemental
`:wm/delivery-failure-evidence-v1` record whose linkage (job id, cascade,
requisition) matches the source record, with the boundary exercised on the
supplemental `:check-input`; then re-release the diagnosis under that
admitted contract. Anything broader (auto-substitution, silent fallback) is
correctly refused by the current `:cause-scope/input-pin-not-source-record`
rule. Future failures need none of this — the pin rides in-source.

## Test adequacy

Good for the claims made: the consumer test composes the two private fns
exactly as the close path calls them and asserts pin round-trip through
`read-pin`; the seam test drives the REAL handler's delivery-error branch
with an adf04df0-shaped answer (abbreviated descriptor retained verbatim,
readiness `:usable`); the index test locks declaration-to-constant
agreement; the negative test locks the no-evidence shape. What the tests do
NOT witness (correctly, and the report says so): an end-to-end live tick
whose persisted run record carries the pin at
`[:failure :detail :delivery-evidence :pin]` — the composition is faithful
but composed; and no diagnosis has yet consumed such a pin. Both belong to
re-release, not this branch.

**Ready for integration** once the two narrative corrections (§3) land as
docstring/report wording; no code change required.
