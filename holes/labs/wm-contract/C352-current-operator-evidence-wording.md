# C352 — current operator evidence wording

Date: 2026-09-01. This wording supersedes C327's recommended receipt language.

## Use now, while C350 is in flight

> **WORKSPACE-GATE CONTENT PASS — EVENT CLAIM NOT YET EVIDENCE-BOUND (C345;
> C350 pending).** Enumerated checks and controls accepted on the recorded clean,
> stable repository basis; bounded resource status was clean. The C342-audited
> predicate controls are independently reason-preserving: their standalone exit
> 0 establishes a valid baseline and rejection of the named mutation, while
> baseline invalidity is exit 1 and named `BASELINE-INVALID`. Report-only
> findings: `<N>`. Manual exclusions were not executed and are not certified by
> this receipt. Mutable-input population v1 is fully classified: 61 content
> claims, six event/interval claims, one non-verdict library, and zero
> unexplained. The six event claims retain their declared qualifications:
> repository-authority fence/movement; Agency lane-registry interval and pending
> revision-token limit; composite quiescence; production-click before/after
> resource envelope; workspace-gate repository-basis interval; and writer-fence
> two-endpoint observation plus attestation. **Do not treat this gate receipt's
> current `event-free? true` or `FENCE-CONDITIONAL` field as proof that fence
> evidence was consumed.** Until C350's chokepoint lands, event freedom belongs
> only to the separately attached C321 evidence and the continuously held
> operator fence, not to the gate receipt itself.

If the separate fence evidence is absent, indeterminate, expired, or breached,
replace the final sentence with:

> **CONTENT RUN ONLY:** accepted exits were observed, but a coherent
> cross-repository/event-free interval is unverified; do not use this as
> operator-run readiness evidence.

## Final form after C350 is demonstrated

This paragraph is **conditional on work in flight**. Use it only after the
shared chokepoint's controls prove that a fabricated identifier cannot produce
event freedom and the gate consumes a receipt-validated capability:

> **WORKSPACE-GATE PASS — FENCE-VERIFIED `<FENCE_ID>`.** Enumerated checks and
> controls accepted on the recorded clean, stable repository basis; bounded
> resource status was clean; the gate consumed the validated writer-fence
> capability for the named observation interval. The C342-audited predicate
> controls are independently reason-preserving. Report-only findings: `<N>`.
> Manual exclusions were not executed and are not certified. Mutable-input
> population v1 is fully classified as 61 content claims, six qualified
> event/interval claims, one non-verdict library, and zero unexplained. This
> receipt certifies no event outside the named fence interval.

## Clauses removed or narrowed

- Removed: “Predicate negative controls are accepted only as positive+negative
  composites.” C342 repaired all 12 vulnerable wrappers across the audited
  mutation modes. Standalone negative exit 0 now establishes its own positive
  baseline; exit 1 names baseline invalidity. No three unrepaired modes are
  recorded by C342.
- Removed: “unaudited mutable-verdict programs.” C330's versioned population is
  reconciled by name at 61/6/1/0, and its gate check rejects undeclared, stale,
  or overlapping membership.
- Retained and made specific: manual exclusions remain uncertified, report-only
  findings remain visible, and the six event claims keep distinct interval
  qualifications rather than being promoted to content facts.
- Added temporarily: C345 proves the current gate and two sibling consumers can
  turn a well-formed identifier into unearned event freedom. Therefore the
  current receipt must not be called fence-verified until C350's single
  capability chokepoint lands and its negative control passes.
