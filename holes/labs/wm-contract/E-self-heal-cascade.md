# E-self-heal-cascade — repair chosen by G over repair cascades

**Opened:** 2026-10-28, Joe to claude-11 ("let's start on the self heal
cascade, which should presumably include a pattern-based
intake/analysis/extension for any new findings").
**Owner:** claude-11 (criteria, review). Implementation by codex-N packets.
**Status:** OPEN. Criteria stated; nothing built. Proposed as row
E4-SELF-HEAL-CASCADE of `criteria-completeness-deficits.tsv` (codex-11's file).
**Related:** `futon3/library/cascades/heal-by-repair-cascades.flexiarg`
(`751d7c4`); `futon3/library/self-heal/` (`8376f57`, drafts awaiting Joe);
`futon3/library/meta/meta-outer-policy-cascade.edn` (the `:self-heal` arm);
`DISCOVERY-meta-cascade-g-2026-10-28.md`; `A-self-heal` (`holes/labs/A-self-heal.md`).

## Scope ruling (Joe, 2026-10-28)

"we should exercise our normal G-over-cascades mechanism rather than set up a
fixed pipeline in E4". So E4 adds no scorer, no repair-specific model with
its own G, and no fixed order of steps. Repair cascades are candidate
policies, and the G that already scores filled META policies chooses among
them. Intake, analysis and extension are patterns that cascades contain and
that G sees, not a tail appended to every repair by code.

## What exists

- The META cascade's `:self-heal` arm (`meta-outer-policy-cascade.edn:49-61`)
  admits task kind `:algorithm` with next move `:repair` or `:run-algorithm`
  while an injury is active. Today it is filled by one policy, run
  `A-self-heal`; `:meta/minimise-g-over-filled-meta-policies` then has one
  self-heal candidate to score.
- `meta_self_heal_production.clj` runs diagnose → acceptance test → claim →
  author → review → enact → retry for every injury. Outcomes outside that
  order (check correct and input wrong, premise unproved, never an injury)
  are reached only by hand (`library/self-heal/README.md`, "first reading").
- Six repair cascades are in the record (README, "Repair cascades in the
  record"), each a composition of the fourteen draft patterns.

## Criteria

- **S1 (patterns accepted).** Joe has read and corrected the
  `library/self-heal/` drafts. Observed by: a futon3 commit recording his
  corrections, or his word that they stand.
- **S2 (reading checked).** A discovery checks the README's reading of the
  present pipeline against the code, line by line. Observed by: a
  `DISCOVERY-self-heal-pipeline-*.md` with citations, reviewed.
- **S3 (Lean).** A repair cascade is an instance of the existing filled-META-
  policy G, filling the `:self-heal` arm, with no new scoring definition. The
  Lean file imports the existing G and proves, on a two-cascade example for
  one injury, that the argmin flips on one observation (for example, the
  reproduction says "check correct" versus "check wrong"), with a nonzero
  epistemic term. Observed by: the Lean file building, reviewed.
- **S4 (Clojure follows Lean).** The `:self-heal` arm is filled with one
  policy per applicable repair cascade, and the live selector scores them
  with the same function it uses for ordinary-work policies. Observed by:
  a test that the two calls resolve to one function, and the Lean
  definition → runtime function → call site trace.
- **S5 (steps are the patterns' steps).** Each executed step is a pattern of
  the selected cascade, and after each step its `@effect` is compared with
  the state (`a-step-that-did-nothing-is-an-observation`). Observed by: the
  repair receipt naming the cascade, each step's pattern and each
  before/after check.
- **S6 (closing patterns run inside the machine).** When the selected
  cascade contains intake, analysis or extension, the machine records the
  findings, matches them against the library and, where warranted, writes
  the formal introduction citing the repair's receipts. Observed by: those
  records written by the machine, with no hand-assembled step.
- **S7 (live).** One live injury is repaired through the cascade G selected,
  and the selection with its per-cascade G is retained in the click record.
  Observed by: the click record and the effective ledger.

Closes when S1–S7 are observed. S3–S7 wait for S1 and S2.

## Order and constraints

- Lean first, then Clojure aligned to it (codex-N writes, claude-11 reviews).
- One packet per criterion; discovery separate from implementation.
- Until S1 and S2, no self-heal work proceeds except what row B5 needs to
  release run001 (the 2aef recovery authority and the live recovery steps).
- No hard-coded rates; preferences learned as in the ordinary loop.
