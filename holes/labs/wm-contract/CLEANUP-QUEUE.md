
## C391 — the adapter check pins occurrence, not correspondence (my fix, owner review of C379)

**Verified C379 (`9e3da2f`) independently.** What I checked, and what it establishes.

**No Lean file is touched in the commit.** The diff is the validator, 27 receipts, one test file and a
report — so *"rebuilding restored elaboration without weakening any theorem"* is supported: no theorem was
restated. The receipts' apparent 20–40 line shrink is a pretty-print collapse, not content loss.

**Compared all 27 receipts as EDN data across the commit** rather than reading two by eye. Every difference is
one of three kinds:

1. `:result {:axioms nil, :exit 1}` → `{:axioms ["propext" "Classical.choice" "Quot.sound"], :exit 0}` on the
   eleven that C376 caught. **Consistent with the stale-`.olean` diagnosis.**
2. `:dependency-closure {:mode :author-declared-source-slices, :machine-complete false, :reason
   :lean-transitive-closure-not-content-pinned}` added to **every** receipt — the lane recorded C379's residual
   *in the receipts themselves*, which is what C386 was going to ask for.
3. **Adapter `:lean-field` strings rewritten** on six receipts, from prose labels to literal source fragments:
   `"Policy constructors"` → `"inductive Policy"`, `"controlled support rows"` → `"| (s, .stay) => [s]"`,
   `"posterior support/mass rows"` → `"support := fun _ => [.cautious]"`.

**(3) is the finding.** The same commit made "Lean-field strings must occur in retained declaration slices" a
requirement **and** rewrote the strings so they occur. That is defensible — the old strings were prose that
could never appear in Lean source, so they asserted nothing checkable — but it narrows what the check proves.

**The strings are correctly scoped.** I checked the one that looked worst: `"support := fun _ => [.cautious]"`
also appears in `ParameterPosteriorKernelPriorNegative.lean`, but the receipt's basis is
`ParameterPosteriorKernelWitness.lean` and it occurs there at line 19, inside the pinned slice. **Not a
cross-file match.**

**What it does not establish.** The mapping `[:rows] -> "support := fun _ => [.cautious]"` carries an
`:expected` of four rows, each with a support list and a mass map. **The pinned string is one line covering the
`support` half of one constructor arm.** So its occurrence does not establish that the four-row table
corresponds to the Lean definition. Contrast `channelCount` expected `14` — a field name against a scalar,
where occurrence and correspondence nearly coincide.

**So the check catches what C376 mutated** — deleted adapters, empty adapters, `unrelatedField` — and that was
its purpose. **It does not check correspondence between `:expected` and Lean content**, and for the six
rewritten receipts the gap between the two is widest.

**Not a defect in C379 and 31/31 stands.** It is a limit of the same shape as C379's stated residual: the pin
is load-bearing without being complete. **Route to `wm-nouns` after C386 returns** — same file, same question,
and two lanes in one file is how a repair gets lost in a merge.

## C394 — owner review of C383/C392, and the file moved under me (my fix, owner review)

**Gated C383 (`8d4bd99`) because the parking request rests on it.** What I checked.

**Read the evidence acceptance paths, not the summary.** `evidence_quiescent` and `evidence_fence` invoke their
producers directly — there is no caller-supplied path into either. `evidence_tested` resolves three bounded job
IDs through the Futon3c registry, requires the terminal systemd unit and on-disk receipt to agree, and takes
the gate start instant from **systemd's `ExecMainStartTimestampMonotonic`**, so a presenter cannot freshen a
day-old fence by editing a receipt's `started-at`.

**`evidence_reload` and `evidence_click_issued` DO accept a caller-supplied JSON path** and check only its
contents. **This is a documented boundary, not a gap.** C371: *"The attestation claim deliberately ends at
`tested-commit`."* The code agrees — those transitions carry `attestation-coverage:
not-claimed-after-tested-phase`. C383's *"Caller substitutions are not accepted"* is the closing sentence of
its quiescence-and-fence paragraph, scoped by it.

**So two of the eight states rest on caller-supplied JSON by design**, because reload and click are Joe's
operations with unbounded latency and no bounded producer. The machine enforces their identity, terminal,
certificate, restoration and ordering predicates; **it does not claim the attestation covered them.** That is
the honest limit the operational certificate has to live with, and it is stated in the right place.

**Ran the controls rather than trusting the count: 13 green** (the report said 12).

### The file moved under me

**I grepped for a `parking-specification-sha256` comparison, found none, and dispatched C392 to add one.**
Between that grep and the test run, **C392 landed as `386b0e9` at 02:38** — and `load_ledger` now raises
`parking-specification-changed:recorded-request-differs-from-current;abort-or-re-initialize`, distinguishing
changed operator authority from ledger corruption.

**I noticed because a test name did not match what I had just told a lane**, not because I saw the commit. This
is the read-side of the hazard I flagged one hour earlier when `wm-organization` closed `wm-evidence`'s census
finding four minutes after it committed. **Holding a dispatch prevents two lanes writing one file; it does not
prevent reading one mid-write.** C393 is the durable answer for censuses; for review, the rule is to pin the
commit under review and read at that commit, not at `HEAD`.

**Both C385 exceptions are now closed.** With C389 (`fe698f3`) amending C284's prose, **both full suites are
green**: Futon2 CI exit 0, Futon3 248 tests / 1,518 assertions, zero failures.

## C397 — owner review of C390; fixed the guard's unresolvable-path crash (my fix)

**Gated C390 (`c9f71ff`) with my own fixtures rather than its self-test**, because a guard that cannot fail is
the defect class this campaign has been auditing others for.

**Its self-test is real.** `--negative-control` injects a Make target invoking a report-only command and
requires rejection; it exits 2 if detection silently stops working. **Shipped positive: 0 findings, exit 0.
Control: rejects, exit 0.**

**Fixtures I wrote, against synthetic file text (no shared file mutated):**

| exposure | result |
|---|---|
| clean Makefile | not caught (correct) |
| direct `python3 <path>` in a recipe | **CAUGHT** |
| indirect via Make variable `P=<path>` | **CAUGHT** — the literal path is in the file text |
| indirect via a wrapper `.sh` | **not caught** |

**The wrapper gap is real and not live.** Both report-only commands
(`live_artifact_format_boundary_lint.py`, `empty_subject_acceptance_lint.py`) are invoked **only** from
`checks/wm_workspace_gate.clj`, which is exactly where exit 3 is understood. C390's own claim is *"direct
exposure"*, so behaviour matches the claim.

**`:report-only-set-empty` deserves noting**: the guard reports a finding when the report-only set is empty, so
it cannot pass by having nothing to check. **That is class 1 addressed in the design**, not bolted on.

### What I fixed

**A report-only command whose argv names no `.py`/`.clj` script made `command-path` return nil, which reached
`str/includes?` and threw a bare `NullPointerException`.** Not hypothetical: this repo has `.bb` checks. It
failed *closed*, which is the right direction, but the error named the guard rather than the command it could
not resolve.

**Repaired directly** (owner carve-out — I held the full context and re-belling would have cost a round trip
for six lines): nil paths are now collected and reported as
`:report-only-command-path-unresolvable :command <name>`. **A crossing check that cannot locate a command's
script has not cleared it**, so it stays a finding.

**Verified after the fix:** the `.bb` fixture now yields that finding instead of an NPE; shipped positive still
exit 0; negative control still rejects; all four Make fixtures unchanged; clj-kondo 0 errors 0 warnings.
