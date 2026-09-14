# Residual questions for Joe — 2026-09-12

Everything else on WORK-REMAINING.md is either done, accruing live
evidence, or owned by another lane. These are the open rulings, in
the order the queued packets want them. Each section says what is
being asked, the enumerated options (none chosen for you), and
exactly which packets dispatch when you rule.

Source of record for each: the `:blocker` entries in
WORK-REMAINING.md rows 18, 19, 22, 24 (and the row-14/row-16
at-review notes). This document collects them; the tracker rows
remain authoritative.

---

## 1. Row 18 — the R20→R14 trip decay law (model content)

An unresolved GENUINE trip on the commitment link lowers R14's
applied selection gain until discharge restores it. The charter
fixes only: monotone direction, factor 1 at zero trips, restoration
on discharge. The v1 factor law itself is model content and needs
your ruling:

- **(i) Law shape** for k distinct open genuine trips. Candidates:
  - `m = base^k` (the TN-War-Machine-Restart design sketch; base
    and floor to be fixed),
  - a linear step,
  - a fixed single factor `m0` while any trip is open.
- **(ii) The floor** (minimum multiplier).
- **(iii) Counting**: distinct-trip vs distinct-wire.
- **(iv) Optional**: whether restoration requires an
  operator/grounding gate. The grounding-confirmation gate is a
  SEPARATE proposed behavior the TN excludes from this edge — you
  may leave (iv) unruled without blocking anything.

**Unblocks:** row-18 packets 2–5 (strict order; packet 3 cannot
pick a law packet 1 has not supplied).

---

## 2. Row 19 — the R9 bootstrap anchor (genesis authority)

The no-self-certification checker is built and refuses its own
admission with `:r9/anchor-missing` until you select the genesis
authority for certifying the checker itself. "Author was Codex,
reviewer was Claude" is not a rule — model-family names are not
authenticated identities. Options:

1. **Operator-signed anchor** over exact source/test hashes; later
   versions admitted by the previous anchored version + a
   distinct-agent review.
2. **Cross-agent genesis**: unequal agent-ids + immutable ledger
   pins + an operator-owned branch rule.
3. **Threshold genesis**: operator + two distinct agent reviews.

**Unblocks:** row-19 packets 3–7.

**Related retention gap (for awareness, not a ruling):** review
commissions (or their digest preimages) must be durably retained
for reviewer jobs, or the review-request-digest join can never pass
on real data — the real-pair run refused honestly on exactly this.

---

## 3. Row 24 — the §2C reading of "FULL scope" (certificate semantics)

The state algebra (CertificateStates.lean) carries all three
readings as separate definitions; a bytes-pinned QualifyingRuling
value selects one, and with no ruling nothing is certifiable. How
does a FULL-scope certificate treat typed partial states?

1. **Attest typed state as-is** — census-complete honesty; "full"
   does not mean all-positive.
2. **Require positive closure** — today un-certifiable, correctly.
3. **Two-level** — always attest the complete typed census, PLUS a
   separate QualifyingRun predicate requiring positive closure
   except for items you have ruled non-load-bearing in writing.
   (The TN argues this has the fewest semantic traps; it does not
   choose for you.)

**Unblocks:** row-24 packets 2+ (with F11-lane coordination on
derive_certificate.bb, which row 24 extends and never forks).

---

## 4. Row 24 — the 17-item negative-scope inventory (written rule-outs by exact bytes)

Each item in TN-row24-scoping-2026-09-12.md §3 must either close
before row 28 or receive your written rule-out **by exact bytes**.
Existing negative-scope prose does NOT count as a ruling. Two items
to flag specifically:

- **Item 11**: confirming the J2 R8-free-energy retirement as
  permitted negative scope (the producer was deleted under your J2
  ruling; row 16 marked it `:retired-no-production-object`).
- **Item 16**: the R11/R15 0/7 honest-absence census rows.
- **Item 17** is the qualifying-run configuration — expanded in
  section 5 below.

---

## 5. Rows 22 + 24 — the qualifying-run configuration (item 17's concrete content)

The firing audit (32 base edges: 20 fired at a declared grain, 12
never fired) needs your run configuration:

- **(i) Effective horizon policy**: enable real anticipation giving
  horizon ≥ 2 (which arms the conditional R13→R4 depth edge), or
  rule the depth arm out. The depth capture showed horizon nil in
  the wild.
- **(ii) The three F_π / detail flags**: previous-tick policy
  details, `FUTON_WM_FPI_DARK`, and `FUTON_WM_FPI_POSTERIOR` with
  complete coverage. RUN9 having fired R8→R6 once does not
  silently set the future configuration.
- **(iii) Aspirational marks**: which of the seven class-(c)
  candidates get the aspirational mark on the figure —
  R11→R16, R6→R11, R7→R14, R9→R16, R10→R8, R15→R13, R15→R16.

Not a question, but a consequence of the row's own default ("all
that the full loop traverses must fire"): FOUR continuation edges
are mandatory and currently unevidenced — **R6→R13, R13→R14,
R14→R16, R16→R2**. The qualifying run must retain joined evidence
for each; no ruling needed, just noting the bar.

**Unblocks:** the row-22 marking packet (evidence-grain `:firing`
annotations on all 32 edges + `:aspirational` per your (iii)
ruling + generator refusal of unclassified base edges).

---

## 6. Smaller questions that will surface at review time (no packet blocked today)

- **Row 14 packet 1b (measured A)**: whether
  `:derived-unique-argmax-of-mu-post` is an acceptable categorical
  status authority — the one observed pair's argmax is 0.183 over
  a near-uniform belief row, so the estimator review will put this
  to you rather than decide it. Also gated on data: the retention
  stack is deployed, but the corpus currently licenses no positive
  A construction (one candidate pair; six of twelve dispositions
  never observed).
- **Row 16 / R5 estimator build**: the bridge module proved there
  is no general bridge between production's Gaussian ambiguity and
  the categorical Lean law (one isolated agreement at
  v = 1/(2πe)). The run NOTES record a recommendation (categorical
  estimator over row-9 Q + row-6 A, Gaussian lane registered
  separately); whether and when to build it is a later decision.
- **R8 free-energy reopening**: permanently closed for row 16
  under J2; reopening would be an operator decision, not capture
  work. Only relevant if you want the R8→R6 free-energy lane back.

---

## Not waiting on you (for completeness)

- **Live-record accrual**: the capture stack is deployed on the
  serving JVM; the next scheduled tick / build-phase cohort
  attempt produces rows 13/14/15/23 evidence with no ruling
  needed.
- **Other lanes**: R10's production caller, R17's capability-typed
  substrate data, F11's derive_certificate.bb extensions, the
  hand-maintained SVG owner edits, and the three standing p4ng
  reds (declaration-rung assertion, stale control-stages.edn pin,
  tetrahedron diagnostic).
