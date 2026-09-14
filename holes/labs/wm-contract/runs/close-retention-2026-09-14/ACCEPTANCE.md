# Independent review acceptance — close-retention v1 contract

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 0103c396
(spec + mechanism + controls), 0db7de6c (deterministic minting +
refusal ordering), 5f4ab866 (receipts). Verdict: ACCEPTED.

Checked:

- File scope: spec + one new namespace + one test namespace + receipts.
  full_loop_runner.clj, full_loop_cohort.clj, and
  measured_a_annotation.clj untouched. Zero-mass holds.
- All four design anchors implemented as decided: (1) mint-occurrence
  demands injected now/uuid-fn, mints fresh prefixed occurrence UUIDs,
  byte-pins the action value (pr-str sha256, drift refuses), exact
  input/output key sets; (2) the state port is :observed (method must
  be :independent-categorical-observation; selection-belief, argmax,
  and disposition refuse BY NAME) or typed :absent with a keyword
  reason and no fabricated time; (3) the model port refuses any source
  other than :declared-machine-model as :model-revision-coercion, and
  absence must carry exactly :declared-model-identity-unthreaded;
  (4) cutoff must EQUAL closed-at (:cutoff-not-closed-at), the
  observation freeze must be strictly earlier (equality refuses as
  :collapsed-evidence-freezes), and an observed state's evidence id
  must be in the admitted set.
- Refusal ordering verified in source: freeze-vs-cutoff checks precede
  the observed-at/state-at check, so the named freeze refusals cannot
  be shadowed — this was exactly the second failed attempt's finding,
  and the fix is in the final code, not just the receipts.
- Controls: happy paths (mint with deterministic ids; typed absence;
  an observed state with earlier freeze and admitted evidence), all
  five named coercions, occurrence drift, and the four temporal
  refusals. Receipts honest (alias misfire + 5-failure attempt
  retained with findings), finals kondo 0/0, full-driver parens,
  fresh-JVM 4 tests / 16 assertions exit 0.

Notes for the wiring packets (not defects):

1. :state-evidence-not-admitted and :admitted-evidence-invalid have no
   direct test — add controls when the writer-side wiring lands.
2. The minted-id check is shape-only: "action-" + a foreign UUID would
   pass format validation. The real guarantee is the single mint call
   site; wiring review must confirm no other construction of these
   ids exists.
3. observed-at <= state-at is stricter than the annotation spec's law
   (which only bounds evidence by the cutoff). Fail-closed and
   anti-retrospective; keep unless a-labels shows a legitimate
   observation flow it wrongly refuses.
4. The action-value digest uses pr-str; the wiring should bind printer
   settings (or use a canonical printer) at both mint and revalidation
   sites. Drift from printer variance refuses (availability risk, not
   a licensing hole).
5. Ordering subtlety for the writer wiring: closed-at is minted INSIDE
   the cohort writer, but the retention block travels in the submitted
   cell — so the WRITER must complete the block with its own
   :recorded-at at event-record time and validate it there. Additive
   only: cells without retention inputs must close exactly as today.
