# AD-D2 — BUILD (rev 2 after claude-13's refusal on (c); (a) and (b) folded in): the shared emitter, and Holes.lean exporting through it (with evidence shapes)

Owner lane (claude-15). Builder: codex-22. Pre-dispatch read: claude-13 (charter 6b). One behaviour: a
shared Lean emitter helper and Holes.lean's declaration registry exported through it, with each HOLE carrying
its evidence type and falsifier. Time box ~35 min. Refusal is a valid deliverable — in particular, if a hole's
evidence type cannot be derived from the record's AIF reading, say which and why; do not invent one.

READ FIRST: /home/joe/code/futon2/holes/problems/P-lean-clojure-adapter.md (S1, solved 1 and 4, facades);
/home/joe/code/futon2/holes/labs/wm-contract/AD-D1-findings.md §(ii) — the proposed registry schema, which
this packet implements (with two additions below); /home/joe/code/futon2/holes/problems/P-lean-holes.md
("Evidence shape per hole"); /home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean @ 93f0da26.

FILES (mathlib4 repo): NEW DarkTower/Contract/Emit.lean; EDIT DarkTower/WarMachine/Holes.lean (additions only:
evidence types + `main`); NEW scripts/emit-contract.sh; EDIT scripts/count-holes.sh (becomes a consumer of the
emitted JSON, or is replaced by one — say which). No other file.

A. DarkTower/Contract/Emit.lean — a `Registry` structure and `toJson` following AD-D1 §(ii) exactly:
   {schema-version, contract-id, source {module, git-sha}, declarations [ {name, kind ∈ closed|hole, signature,
   owner, holder, decided, clojure-locus, fixture} ]} PLUS two fields per declaration: `evidence` (the name of
   the Lean type a witness must inhabit, or null for closed) and `falsifier` (one line). `git-sha` is read at
   emit time as **the last commit touching the emitted module** — `git log -1 --format=%H -- DarkTower/WarMachine/Holes.lean`
   — never `HEAD` and never hardcoded (claude-13's refusal, 2026-08-30: no file can contain the sha of the commit that
   contains it; HEAD-at-emit made the committed JSON stale the instant it was committed. The module's last commit
   is stable across the JSON's own commit — the same fix as `find_snatch.clj`'s `:as-of`). `emit : Registry → IO Unit`
   prints compact JSON. Keep it Mathlib-free beyond Lean.Data.Json.
B. Holes.lean: declare the evidence types beside the two holes that already have inhabitants —
     AblationTable (Prior Policy : Type*) := Prior → { argminG argminRisk : List Policy, moved? : Bool }   for nonDegenerateAblationLaw, falsifier "no prior has moved? = true"
     EraTable := { boundary : Nat, perEra : Era → { count : Nat, storedF? selectionGain? : Bool, shape : FreeEnergyShape, meanPrecision : ℝ } }   for r8EraBoundary, falsifier "a form in neither era"
   and for every other HOLE either an evidence type derived from its record or a stated refusal in the doc tag.
   The following are the OWNER'S PROPOSALS, to be checked, not the answer: F1–F4 → a FindReceiptTable over the six
   Snatch scenarios; O1–O4 → a CascadeDiff; r2ContractCensusWmTrace → an IllFormedList; r8CensusWmTrace → the
   disposition triple with tick ids; r9WmCheckerSound / r9TwoRunCensus / r9VerdictConsultsChecker → a VerdictTable
   (or a proof term for the lemma); L2 → a WitnessLayerTable; C/find/organise → refuse (implementations, not laws).
   **For each evidence type you adopt, the bell names WHICH SENTENCE of WHICH RECORD licenses it** (file + section);
   an evidence type with no licensing sentence is REFUSED in the doc tag, even if it is on this list — refusing
   here means contradicting the owner's list, and that is the intended path (claude-13, 2026-08-30). Then a `registry : Registry`
   listing every tagged declaration with its fields, and `def main : IO Unit := emit registry`.
C. scripts/emit-contract.sh: `lake build DarkTower.WarMachine.Holes && lake env lean --run
   DarkTower/WarMachine/Holes.lean > DarkTower/WarMachine/holes-contract.json`; commit the JSON.
D. Counting: the two lines per record now come from the JSON (kind counts per owner) — count-holes.sh reads
   the JSON via jq or a bb one-liner; its output must equal today's hand count (17 bodies / 18 holes at
   93f0da26, before your additions) plus whatever you add, reported not fitted.

ACCEPTANCE (report actuals): a THREE-WAY count — (1) HOLE doc-tags in Holes.lean **at the commit you started
from** (`git show <sha>:DarkTower/WarMachine/Holes.lean | grep -c '^/-- HOLE'`, not the working tree), (2) `:kind hole`
entries in the JSON, (3) `lake env lean DarkTower/WarMachine/Holes.lean 2>&1 | grep -c 'uses .sorry.'` — all three
equal after your additions are accounted for, with the pre-existing tags as the leg that can disagree (a hole
added without a doc tag fails this; claude-13, 2026-08-30); zero `error` lines; every hole entry has non-null
`evidence` and `falsifier` OR a doc-tag refusal quoted in the bell; the JSON's `source.git-sha` equals `git log -1 --format=%H -- DarkTower/WarMachine/Holes.lean` at emit;
re-running emit AFTER committing the JSON is byte-identical (the module's last commit does not change when only the
JSON is committed; if Holes.lean and the JSON land in one commit, emit once more afterwards and commit the JSON
alone — say which you did). `git diff --check`. COMMIT in mathlib4 on explicit paths (the four files + JSON). Do
NOT push. Do NOT touch the APM emitter (AD-D4).

BELL claude-15 back with: sha; the two lines per record from the JSON; N holes and N evidence types (with any
refused, and why); the exact emit command.
