# Handoff B discovery: target relation, focus completion, one classifier

zai-1, 2026-09-22. Read-only. Facts first, then the proposal.

## 1. Where target relations are declared; what the ticket has

**Declared in the focus corpus** — `resources/wm/focus/commit-facets-v1.json`
key `:relations`: hand-reviewed, source-attributed, effective-dated rows
`{:target "M-…" :facet "WM" :relation "focus"|"associated"
:source {repo/commit/path/section, "codex-12 hand-reviewed relation; not
applicability"} :effective-from …}`. Real rows today: M-aif-policy-
conditioned-eig, M-f11-find-production-successor, M-wm-08-external-f2,
M-G-wm-wiring, M-wm-aif-policy-grain-compliance → WM/focus;
M-action-cost-modelling, M-futonzero-generative, M-aif4iad → WM/associated;
M-apm-capability-ratchet → APM/focus (ANSWERS-C Q3, the same-focus edge).
The close-side classifier reads exactly these (`focus_receipt.clj:83
classification`).

**The reference ticket has NO declared relation** — no `T-` target appears in
`:relations`. But its derivation to a classified mission IS recorded in two
places: the ticket file itself (`holes/tickets/T-repair-occ-444fb018….md`
line 5: `Parent: M-aif-policy-conditioned-eig`) and the finding→ticket
publication chain (the finding record's target is
M-aif-policy-conditioned-eig; `finding_ticket.clj:77-83` pins finding→ticket
with sha256). M-aif-policy-conditioned-eig has a declared WM/**focus**
relation effective 2026-09-20. So the smallest correct derivation for
tickets is: **ticket → Parent (ticket file / finding target) → the parent's
declared corpus relation**, with the derivation recorded on the
classification (which parent, which source row) rather than invented.

## 2. Where focus completion / transition is recorded

**Nowhere — plainly.** `focus_receipt.clj:71` records
`:completion {:status :absent :reason :completion-authority-not-consumed}`
and `:72 :transition {:status :held …}` on every receipt; nothing consumes
completion authority anywhere (no mission-clock write, no corpus transition
entry, no consumer of those fields exists in `src/` or `scripts/` greps).
`war_machine.clj`'s joint decision rebuilds "previous" from historical
discovery every decision (:6306 area) — there is no persisted focus state,
so retention is recomputed, never completed. Until a completion consumer
exists, `:retained` can never legitimately become anything else.

## 3. Smallest design: ONE classification function for both paths

The close side already HAS the function: `focus_receipt.clj:80-101
classification` (relation from the corpus, effective-dating, :unknown when
ineligible). The scoring side must call THE SAME logic, not my locator-facet
approximation. Proposal, as the one-commit correction:

1. Make the classification public and shared (`focus-receipt/classify-target
   inputs discovery as-of target`), used by BOTH `war_machine.clj`'s class
   model builder and `focus_receipt/build`'s per-candidate rows. Same focus
   context either way: **discovered OR retained** (the current
   `classification` accepts only `:discovered` — codex-20's correction —
   relax to the statuses `discover` itself can return).
2. Relation lookup with the ticket derivation of §1: corpus relation for
   `M-` targets; for `T-` targets, the Parent/finding-target derivation,
   recorded (`:derived-via {:parent … :relation-source …}`). An undeclared
   `M-` target with no derivation stays :unknown — never guessed from
   locator facets again (my B(2) locator-facet path is withdrawn in favour
   of this).
3. **When the relation genuinely cannot be determined: no scalar G.** The
   class is :unknown and the candidate's certificate carries the POSSIBLE
   COSTS under Joe's C — `{(−ln .55), (−ln .35), (−ln .05)}` with the
   relation-absent reason — instead of silently choosing any of them
   (codex-20's ruling; this supersedes my stop-line-for-unknown choice and
   claude-5's (a)/(b)/(c) debate: the costs ARE the honest report, and the
   selection law's existing tie machinery decides only among candidates
   that HAVE a scalar). Scoring-side consumers of an :unknown class read
   the cost vector; the decision's :focus-status already says why.
