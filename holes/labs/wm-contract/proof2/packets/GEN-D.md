# GEN-D — the witness generator boundaries: extract, emit, bad-extract

Row 44 of `PROOF-2-STRATEGY-draft-2026-09-24.md` (§6 generator expansion).
Author: kimi-2, 2026-09-24. Discovery/specification only: no code, data,
bundle, or Lean file changed; no click; no JVM touched. Sources read: the
THEOREM draft (P₀–P₆, the F sentence, the Correction and Review
amendments), `PROOF-2-ASSUME-draft-2026-09-24.md` (A12–A21), CERT-S
(`proof2/packets/CERT-S.md`), the strategy's §4–§6, the witness style of
`mathlib4/DarkTower/WarMachine/MachineObservationWitness.lean` and
`ExpectedFreeEnergyWitness.lean`, the negative style of
`DirichletConcentrationsNegative.lean`, the `futon2/checks/` census, the
exemplar `data/wm-runs/tick-run-record-2026-09-23-1790131591.edn`, and
`DirichletLearning.lean`. Revisions: futon2
`93531d415a115c7b426f37e06d7d5542ecb5dae4`, mathlib4
`77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3` (which is A19's baseline
identity). All file:line anchors are at those revisions. Record key paths
are cited against the exemplar's bytes; where I could not show something
on a record, I say so.

GEN-D specifies three boundaries and their negative. It authorizes no
extractor, emitter, or Lean module; those are GEN-E/GEN-B/GEN-X5 and the
G* rows, each separately reviewed.

## 1. The extract schema — `proof2/extracts/<record-sha256>/<clause>.edn`

### 1.1 Naming and the record hash

`<record-sha256>` is the SHA-256 of the run record's raw file bytes, no
canonicalisation — the same convention the held-out observation rows
already use (`resources/wm/eig/held-out-observations.edn`, each row's
`:source {:path :sha256}`), so one hash names one byte string everywhere
in the proof. `<clause>` is one of `B`, `A`, `D`, `F`, `Q`, `Field`,
`BT` (B temporal), `Complete` — one extract per clause per record, so a
clause's reviewer diffs exactly one file.

### 1.2 The manifest (every extract carries one, at key `:manifest`)

```
{:extract-schema :wm/proof2-extract-v1
 :record {:path <repo-relative> :sha256 <64-hex>}          ; §1.1
 :forms {:rule :single-form-to-EOF|:ledger-to-EOF          ; §1.4
         :forms-read <int>}                                ; counted, never assumed
 :key-paths [{:path [:decision :selection-certificate …]
              :status :present|:absent|:typed-absence
              :absence-form <the record's own absence map, when typed>
              :value-ref {:content-ref "sha256:…"}|:inline}]
 :referenced-hashes {…every embedded :value-sha256/content-ref
                     → {:recomputed "sha256:…" :agrees true|false}…}
 :identities {:certificate-schema <record's :certificate-schema or :absent>
              :extractor {:ns futon2.checks.proof2-extract
                          :source-sha256 <64-hex>}
              :bundle-hash <A7's r12 bundle hash>
              :record-baseline <A19 tuple, when the record is a baseline member>}
 :canonical-extract-sha256 <64-hex>}
```

Rules, each stated with its reason:

- **Every referenced content hash is recomputed, never trusted.** CERT-S
  §4 BJ-3 names the self-attesting hash; the manifest's
  `:referenced-hashes … :agrees` field is the extractor's answer — it
  recomputes the canonical hash of the value it read and compares. An
  `:agrees false` entry is a refusal of the whole extract (§2), not a
  warning.
- **Presence statuses are the record's own.** A key read as a CERT-S
  typed absence (`{:status :missing …}` / `:not-supplied` / `:refused`)
  is extracted *as that absence*, status `:typed-absence`, with the
  absence map preserved verbatim. The extractor never converts absence
  to a value (that is the row falsifier, §7) and never converts a value
  to an absence.
- **Identities are load identities, not names.** `:extractor
  :source-sha256` follows the house `load-identity` convention
  (`src/futon2/aif/load_identity.clj`): a git sha alone does not identify
  what ran (A19's "dirty/uncommitted code participates" falsifier).

### 1.3 Carrier ordering (finite maps and sets)

Identical to CERT-S §3's canonicalisation, by reference, so that one
canonical form serves the certificate, the extract hash, and the emitted
Lean: **maps sorted by the canonical printed form of their keys, sets
sorted likewise; ratios and integers printed exactly; doubles only as
`#wm/double "0x…p…"`; strings/keywords verbatim; no metadata, no
comments.** The extract body is canonical by construction;
`:canonical-extract-sha256` is the SHA-256 of the canonical EDN of the
body (the whole file minus the `:canonical-extract-sha256` entry itself).

Finite maps whose iteration order is semantically load-bearing get an
explicit order key: the candidate field is extracted as a vector in the
certificate's own `:candidates` order with the small id (CERT-S §0
amendment) as the join key, never as a hash-keyed map re-sorted into a
new order. Sets of facts (token states, §6) are extracted as sorted
vectors with the sort declared in the manifest entry
`:carrier-order :canonical-printed`.

### 1.4 Reading rules: all-forms-to-EOF and reject-extra-forms

Two input shapes exist in the stores: single-record files (one EDN form:
run records, findings, close checkpoints) and ledgers (many forms:
JSONL/EDN logs). The rule is disjunctive and the manifest records which
branch ran:

- **Single-record file: read exactly one form, then demand EOF.** Any
  bytes after the first form — a second form, trailing garbage — are
  `{:extract-refusal :extra-forms :after-byte <n>}`. Reason: a run record
  with trailing bytes is not a record, it is two things sharing a name;
  silently reading the first is how a stale prefix impersonates a record.
- **Ledger: read every form to EOF and count them.** `:forms-read` is
  that count. The extractor may count and index forms; it may not
  aggregate them into any production value (§2) — counting rows is
  bookkeeping, summing them into a concentration is a recount.

### 1.5 Hex-double preservation

A double is transcribed **only** as `#wm/double "0x…p…"` carrying the
`Double/toHexString` encoding (CERT-S §1/§3). The extractor refuses —
`{:extract-refusal :lossy-double :path …}` — rather than write a decimal
rendering of a machine double, because a decimal print is a silent
rounding and the strategy's §1.1 exact-arithmetic problem begins exactly
there. Ratios print as-is (`11/20` is exact); integers as-is. This rule
has one teeth-bearing consequence: on the exemplar, `:theta 1/8` and
`:focused 11/20` extract exactly, while any F total produced as a double
arrives only as its hex form and is a *machine-number* value, not a real
— the refinement obligation (§8, amendment AM-2) is where that gap is
closed, not here.

## 2. The trust boundary — what the extractor may and may not compute

Lean sees decoded records, not the filesystem or reality (strategy §6).
The extractor is the whole bridge, so its permission list is short and
everything else is a refusal:

**MAY** (bookkeeping over bytes read):

1. Read forms (§1.4), classify presence (§1.2), canonicalise and order
   (§1.3), and hash byte strings (§1.1, §1.2).
2. Recompute embedded hashes and report agreement (§1.2, BJ-3).
3. Copy values verbatim from record key paths into the extract,
   preserving the record's own absence maps.

**MAY NOT** (each refusal is a first-class extract value):

- **R1 `:would-recompute-production-value`** — any production fact:
  a concentration, a trial count-as-value, a posterior, a theta, a
  normalization, a likelihood, an F. If the record does not carry the
  value at the declared key path, the extract carries the absence.
  *This is the row falsifier as a refusal:* filling an absent
  concentration from a ledger recount is exactly R1.
- **R2 `:hash-disagrees`** — any `:agrees false` under §1.2.
- **R3 `:schema-undeclared`** — the record carries no
  `:certificate-schema` declaration (the exemplar's case, CERT-S §6.10):
  the extract is still written, marked `:certificate-schema :absent`,
  and is usable only as a negative fixture (strategy §6: old scalar-only
  records are negative fixtures; nothing is added to them retroactively).
- **R4 `:extra-forms`** / **R5 `:lossy-double`** (§1.4, §1.5).
- **R6 `:no-ledger-derivation`** — no value in any extract may be
  computed from a ledger *at all*. A ledger is readable only to *check*
  a recorded dedup/version claim's referents exist (reference-checking,
  strategy §6 "check references rather than trust hash strings"), never
  to produce the claimed value.

The boundary's test is one sentence: **every number in an extract is a
number that was already in the record, or a hash of bytes that were.**

## 3. The emitter contract — extract → `Proof2/<Clause>Witness_<hash>.lean`

`<hash>` is the `:canonical-extract-sha256`, so the module name and the
extract are locked together; the manifest link is the module's header
comment carrying record sha256, extract hash, key paths, and tool
identities (the MachineObservationWitness style of a provenance header,
`MachineObservationWitness.lean:1–9`, tightened from prose to fields).

**The independence rule (the second row falsifier).** The emitter
transcribes numerals and finite maps from the extract as *literals*, and
asserts the model formula over them as a *separate* proposition. For the
B backend the shape is:

```
def priorConc : Fin 2 → Fin 6 → ℝ := !{…literal from extract…}
def trialVec  : …                  := !{…literal from extract…}
def postConc  : Fin 2 → Fin 6 → ℝ := !{…literal from extract…}
theorem posterior_is_accumulate :
    postConc = DirichletLearning.accumulate priorConc trialVec nonneg := by
  decide / norm_num / finite extensionality
```

Defining `postConc := DirichletLearning.accumulate priorConc …` and then
"proving" `postConc = accumulate priorConc …` is `rfl` — both sides from
one function is not a correspondence, and the emitter must structurally
refuse it: the right-hand side is evaluated from the *recorded* left
side, never the other way, and the emitted literal map is the only
source of the asserted values. The check is mechanical: the emitter's
own receipt lists, per theorem, which defs the proof term mentions, and
any asserted equality whose two sides reduce to the same def is rejected
before `lake build`.

**Build gates (per module, recorded in the module receipt):**

1. `lake build` of the module and its imports green.
2. **Axiom audit:** `#print axioms` on every emitted theorem; the receipt
   records the list. Only the standard mathlib axioms (`propext`,
   `Classical.choice`, `Quot.sound`) may appear; any click-local or
   module-local `axiom` is a failure (the theorem draft's "an axiom
   introduced for the click" clause).
3. **No `sorry`:** source-level scan plus the existing
   `checks/lean_sorry_category_check.clj` discipline; a `sorry` anywhere
   in the transitive module is a failure.
4. **No `x = x`:** the receipt's correspondence check above.

These follow the existing witness discipline —
`ExpectedFreeEnergyWitness.lean` proves by `norm_num` over literal
carriers (`onePointFixture`, `:46`), `MachineObservationWitness.lean`
keeps logs symbolic and states measured counterexamples as separate
theorems — and tighten it where the strategy demands more (numerals from
records, not fixtures).

## 4. The bad-extract contract — `proof2/negative/<spec-hash>/`

`<spec-hash>` is the hash of the *registered mutation description* (§
below), so negative artifacts cannot be mistaken for live positives and
the registration itself is content-addressed (strategy §6: "their
namespace/file names cannot be mistaken for live positives").

**Order of operations (A18, made mechanical):**

1. **The reviewer registers the semantic mutation first.** Against a
   valid-shaped fixture extract (R3-marked pre-schema records are the
   lawful fixture source, §2), the registration names: the fixture's
   canonical hash, the exact mutation (one value changed, one identity
   edge broken — X₀–X₆ in the theorem draft are the menu), the premises
   deliberately preserved, and the proposition expected to fail. A18's
   "bad case chosen by the implementer alone" and "run after the
   reviewer sees the positive outcome" are both refused by the
   registration existing before GEN-B runs.
2. **The harness checks the same proposition** — the emitted module's
   checker, not a second reading of it — and records: the expected
   semantic failure, the inner diagnostic verbatim, the wrapper exit,
   the module hash, and the checker identity.
3. **A module that fails only by syntax is not a negative.** The existing
   style is exactly right here and is ratified:
   `DirichletConcentrationsNegative.lean:10` wraps the bad def in
   `#guard_msgs` with the *expected error in the docstring*
   (`error: unsolved goals ⊢ False`) — the wrapper compiles green
   precisely because the wrong assertion fails for the registered
   semantic reason. A parse error, an unknown identifier, or a failed
   import proves nothing about the proposition and is recorded as
   `:negative-invalid :syntax-only`, not as a negative result.

## 5. The first backend: B accumulation/normalization

Strategy §6 names it: B is already computed (`learning_trial_ledger.clj:
154`, `b-update`) and consumed (`scripts/futon2/report/war_machine.clj:
6383` at this revision — the `pattern-theta` call inside the judge's
scoring hunk; CERT-S cited `:6380` at `d0a369dd`, the hunk has drifted
three lines, which is why §1.2 requires per-revision anchors).

**Extract keys (clause `B`, per record):**

- From the producing close record (key paths per CERT-S §1 B row):
  `:b-update` — `:prior-concentrations`, `:posterior-concentrations`
  (exact-rational vectors), `:trial-vectors`, `:trial-identities`
  (content refs), `:dedup` receipt, `:version`, `:normalization`
  receipt. **None of these exist on today's records**: today's
  `b-update` (`learning_trial_ledger.clj:154`) records the scalar
  two-cell form and the row; the concentration carrier is B-C's build.
- From the run record's certificate: `[:decision :selection-certificate
  :model-inputs <id> :B]` (consumed θ with `:value-sha256`/
  `:consumed-at`), root `[:decision :selection-certificate :B-read
  :predecessor-chain]`, and the θ stamped at the candidate's precedence
  patterns (`:theta-source :recorded-trials` — visible on the close
  records of 2026-09-23, e.g. `…1790184736`'s
  `:token-outcome-comparison`, as a scalar with provenance, not a
  concentration row).

**Lean declarations:** `DirichletLearning.DirichletParams`
(`DirichletLearning.lean:20`), `step` (`:46`), `accumulate` (`:58`),
`accumulate_conc` (`:77`), `accumulate_append`; for the consumed-θ half,
the owed normalization declaration (§8, AM-1) and
`CascadeTransition.patternKernel`/`cascadeKernel` for the substitution
instance; `selectionPosterior`/`IsBayesAction` belong to the G5T
temporal backend, not this one.

**Landing order:** **B-C first.** Extraction (GEN-E, row 45) reads
recorded values without recomputing them; with no recorded concentration
carrier there is nothing lawful to read — a GEN-E against today's
scalar-only records would either produce absences (useless as a positive
backend) or recount the ledger (the row falsifier). **B-N before GEN-B**
(row 46): the emitted witness must assert `theta = normalize posterior`
against a *declared* `normalize`, and B-N is that declaration plus its
Jeffreys-specialization lemma. So: B-C → GEN-E, with B-N in parallel →
GEN-B → GEN-X5. The W₅ arithmetic sub-witness is the only claim GEN-B
may make (row 46's own boundary); read continuity and the first
compatible pair are G5T's, later.

## 6. Cardinality risk, measured on the exemplar

Run `2026-09-23-1790131591`, measured 2026-09-24 on its bytes:

- **Token-state carrier: 64 states.** The ticket's fact vocabulary is
  six `[target token]` pairs — `:admission/task-stated`,
  `:repair/obstruction-observed-cleared`, `:repair/split-declared-valid`,
  `:repair/held-out-observations-collected`,
  `:repair/calibration-evidence-present`, `:restoration-accepted` (all
  under `T-repair-occ-444fb018…a2ade`; enumerated by walking the record's
  fact vectors). A token state is a set of facts, so the carrier is
  2⁶ = 64 states. (The record also names three facts of a different
  mission, `M-f11-find-production-successor`; those are outside this
  click's field universe.)
- **Recorded beliefs on it:** the initial belief
  `[:decision :selection-certificate :token-belief-stage :initialization
  :value]` is a point mass on **one** of the 64 states (the singleton
  set `{:admission/task-stated}`, mass 1) — support of size 1 out of 64.
- **Candidate-side maps:** 1 candidate, precedence of 4 patterns, each
  `:produces` a singleton set; no `:theta` on any (measured: all nil).

A full 64×64 transition carrier or a 64-support q map is emittable today
(64² = 4096 literals is mechanical), but the strategy's risk is real one
vocabulary over: the next ticket's vocabulary is unbounded by anything
in the schema. **What a sparse-support lemma would have to say** (as a
separate reviewed Lean packet, per §6 of the strategy — recorded here as
the exact obligation): for beliefs whose support is a declared subset
σ of the state carrier, (a) extension by zero embeds `σ → ℝ` into
`State → ℝ`; (b) `exactUpdate`, `accumulate`, and the softmax/
`selectionPosterior` maps commute with that embedding — computed on the
support and lifted, they equal computation on the full carrier; (c)
nonnegativity and normalization are preserved by the embedding. Without
(b), a sparse witness proves a small model, which the strategy forbids
as a substitute. No such lemma exists today; nothing in GEN-E/GEN-B
requires it at 64 states.

## 7. The falsifier, restated as concrete bad cases

1. **The extractor fills an absent concentration from a ledger recount.**
   Concretely: a close record whose `:b-update` carries only today's
   scalar row (no `:posterior-concentrations`); GEN-E, asked for `B.edn`,
   walks `data/wm-repair-obligations`' ledger, recounts the family's
   trials, and writes the recounted vector under
   `:posterior-concentrations`. Under §2 R1/R6 this must refuse with
   `:would-recompute-production-value` and the extract must carry the
   absence instead. A harness that emits the recounted vector — however
   correct the arithmetic — has produced a value the record never
   contained, and the whole proof's "recorded, not recomputed" spine
   (A16) is void.
2. **An emitted witness whose both sides come from the same function.**
   Concretely: `def postConc := DirichletLearning.accumulate priorConc
   trialVec h` followed by `theorem ok : postConc = accumulate priorConc
   trialVec h := rfl`. Under §3's independence rule the emitter's
   correspondence check rejects this before `lake build`: the asserted
   equality reduces both sides to one def, so it witnesses the
   definition, not the record.

## 8. What this packet unblocks, and the amendments the theorem needs

**Unblocks:** GEN-E (row 45) directly — its `checks/proof2_extract.clj`
implements §1–§2 against the first backend's keys (§5). GEN-B/GEN-X5
(rows 46–47) inherit §3–§4 unchanged. The G* backend rows (§6 table)
inherit the schema per clause with only the key-path table of §1.2
specialised.

**Proposed amendments (definitions the theorem needs, stated as
amendments, not questions):**

- **AM-1 (clause 5, to B-N):** the theorem's W₅ says "the extracted
  theta equals a separately recorded normalization of the relevant
  posterior concentration row," but no Lean declaration of that
  normalization exists — `pattern-theta` (`learning_trial_ledger.clj:
  238`) is the runtime rule, and `DirichletLearning` has no
  concentration→theta map. Proposed definition for B-N:
  `DirichletLearning.rowTheta (a : DirichletParams O S) (o : O) : ℝ`
  as the declared normalization of row `o`, together with the lemma
  that on the two-cell Jeffreys-specialized carrier it equals the
  recorded `(successes + 1/2)/(trials + 1)` rule. W₅'s theta equality
  then names a declaration, not a docstring.
- **AM-2 (strategy §1.1, to SPEC-N):** CERT-S fixes `#wm/double` as the
  lossless *record* form, but neither the theorem nor CERT-S says what
  a hex-double *is in Lean*. Proposed: hex-double extracts are emitted
  as symbolic named reals carrying their recorded encoding
  (`def f_total_hex : String := "0x1.…p…"`, `def f_total : ℝ := …` with
  the refinement proposition stated separately), so a machine-number
  value can never silently stand for a real equality — the
  MachineObservationWitness style of keeping encodings symbolic,
  written as an amendment so the emitter has a rule to implement rather
  than a taste to guess.

**What I did not establish:** the `M-f11` facts' model role on the
exemplar (they sit outside the selected field; whether the carrier
should include them is a FIELD-D question, not settled here); the exact
form B-C will give `:trial-vectors` (the extract schema accepts it
whatever its shape within §1.3's value forms); and whether 64 is the
largest carrier L will see (§6 records the risk and the lemma
obligation, not a forecast).
