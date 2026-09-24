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

## Revision 2 — 2026-09-24 (codex-12; specification, not implementation)

This appendix supersedes conflicting prescriptions in §§1–8 without rewriting
that history. It addresses all five corrections in `reviews/GEN-D-codex-21.md`
(7c334441), the B population/encoding objections in
`reviews/B-D-codex-20.md` (15ecfb2d), and SPEC-N review amendment 3. None of
those reviews supplies an empirical premise: the reasons below are record
contents, source definitions, and the distinction between a recorded operand
and a calculated result. This remains subject to independent re-review.

Revision pins for this appendix: **R2** = futon2
`d1b011ab9dc54ee0740309752bfc110174bc554f`; **L2** = mathlib4
`41a3691f4b65a06b2a6a1d52b759b6690b583c48`.
Unprefixed source paths are futon2-relative; bare
`learning_trial_ledger.clj` and `full_loop_runner.clj` abbreviate files under
`src/futon2/aif/`. `WM/` abbreviates mathlib4
`DarkTower/WarMachine/`. SC always means the full path
`[:decision :selection-certificate]`, including for `:certificate-schema`.
The original R/L pins above remain the citations for the original text.

### R2.1 Independent source-path/presence/value verification

**Proposed GEN-E contract, not an existing checker:** an independently
implemented verifier receives (i) a preregistered clause path-schema and its
hash, (ii) the immutable source-record identities fixed independently of the
extractor, and (iii) the proposed extract. It does not accept the extractor's
`:agrees`, `:present`, path list or hashes as its verification result.

1. Reopen each source by its registered raw SHA-256, check those raw bytes,
   parse with the declared single-form/ledger-to-EOF rule, and reject malformed
   or extra forms. Whitespace/comments followed by EOF are not extra forms.
   Obtain the schema at SC `:certificate-schema`; undeclared or unsupported
   schema yields a diagnostic extract, never positive schema standing.
2. Instantiate the **verifier's** fixed path-schema over every recorded
   candidate id and every required family/trial row. The schema declares
   record role, physical envelope path, expected value type, required/optional
   status, permitted absence variants, content-reference rule and identity
   joins. Do not instantiate from the extractor's list. Missing sections do
   not reduce the obligations: retain the missing section as a failed required
   obligation. Require exact equality between this obligation set and the
   extract's source entries (no omission, duplication, alias substitution or
   unrecognized source entry). Optional paths still receive presence checks.
3. Resolve paths component by component: maps use membership plus lookup;
   vectors use checked integer bounds; a non-container intermediate component
   is `:invalid-path-carrier`. Distinguish **key absent**, **present nil**,
   **present typed absence**, and **present value**. Missing is not nil and
   nil is not zero. Classify absence by the registered value schema, not any
   arbitrary nested `:status`. Preserve the complete source absence form.
4. Resolve content refs only through the registered immutable content store.
   Check both the canonical hash of the recorded ref map and the resolved
   object's canonical hash against the ref. Retain its complete resolution
   chain, refuse missing content/cycles/hash disagreement, and never search
   a live ledger for an alternative value. An unresolved reference produces
   a verifier finding alongside the unchanged source ref, not an invented
   source absence map.
5. Independently encode the resolved typed source value by CERT-S §3 and
   compare its bytes with the proposed extract's source value. Numeric `=`
   is insufficient: ratio versus double, set versus vector, signed zero,
   nil versus missing must remain distinguishable. Check presence first,
   then type/value. A lawful double-to-tag representation is the declared
   codec, not a new production number. Validate candidate small ids at
   SC `:candidates i :id :id` plus recomputed payload hashes across sections;
   validate producer/consumer value-hash and `:consumed-at` identity joins.
6. Output an independent verdict bound to source hashes, path-schema hash,
   extract hash and verifier loaded-source identity. Example findings:
   `:source-presence-mismatch`, `:source-value-mismatch`,
   `:source-type-mismatch`, `:required-path-omitted`, `:content-unavailable`,
   `:hash-disagrees`, `:carrier-malformed`, `:carrier-incompatible`.
   An honestly extracted missing value can pass transcription fidelity but
   fails the positive clause's completeness requirement. These are separate
   verdicts. An extractor filling it cannot pass even transcription fidelity.

The first B path-schema must enumerate the physical B entry specified in
R2.3 and **each** of its carrier, prior/posterior, ordered trial, acceptance,
commit-point, dedup, version and normalization fields; the selected family
cannot be inferred by omitting other families. Its consumed-side paths are
SC `:model-inputs <id> :B`, SC `:B-read :predecessor-chain`, and SC
`:candidates i :id :precedence j :theta` with sibling `:theta-source` and
recorded producer/consumer joins. A scalar theta is not a replacement for
missing concentration/trial paths. Full clause-5 standing additionally needs
those read/temporal joins; an arithmetic-only extract labels its narrower scope.

**Mandatory absent-to-recount negative, before positives:** use a
schema-declared synthetic close fixture at the physical B path with prior
`[[1/2] [1/2]]`, posterior `{:status :missing :reason :not-recorded}`, and an
unrelated field containing `3/2`. A separate one-success ledger permits a
recount `[[3/2] [1/2]]`. Give the verifier a corrupt extract claiming that
recount is a present posterior, with otherwise correct raw/ref/extract hashes.
Step 3 independently reads typed absence: require
`:source-presence-mismatch` at the exact posterior path. Repeat with the
posterior key omitted (same finding), present nil (invalid required value,
not absence), and posterior omitted from the extract
(`:required-path-omitted`). A correct absent extract passes fidelity and fails
completeness. An unrelated `3/2` cannot rescue any case. These are specified
acceptance tests, **not executed results from a nonexistent verifier**.

Replace §2's “every number already occurs” rule: production operands must
come from their **designated source locations**, not numerical membership in
any record. Bookkeeping (form count, enumeration index) lives under
`:generated-metadata`; proof calculations (bounds, sums, proof terms) under
`:proof-witnesses`. Neither namespace may satisfy a production source path.
The verifier may recalculate the right side to check an equality; it may not
use that result as the missing recorded left side. Separate implementation
and independently reopening registered bytes prevents self-attestation; it
still leaves parser/hash/codec correctness in the explicit extraction trust
boundary, not a filesystem theorem inferred from Lean.

### R2.2 The exemplar has incompatible recorded carriers

Read-only reinspection used `bb`, `clojure.edn/read` with
`{:default tagged-literal :eof ::eof}`, PushbackReader, and a second read
requiring EOF. Raw SHA-256 for
`data/wm-runs/tick-run-record-2026-09-23-1790131591.edn` again equals
`7314951f0ad6d339d561a9f7ec4f5dc14042873e4602873dddb590701ba61f25`.
Let T be the string
`T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade`.

| Exact path after SC | Observed carrier; diagnostic cardinality |
|---|---|
| `[:token-belief-input :carry-admission :current-universe]` | Six `[T token]` pairs: `:admission/task-stated`, `:repair/obstruction-observed-cleared`, `:repair/split-declared-valid`, `:repair/held-out-observations-collected`, `:repair/calibration-evidence-present`, `:restoration-accepted`. Literal powerset size 64. |
| `[:scoring 0 :rates-provenance :model :universe]` | Seven set members: the same pairs **except** obstruction-cleared, plus bare string T and bare keyword `:admission/task-stated`. Literal powerset size 128; malformed as a target-qualified fact carrier. |
| `[:candidates 0 :computed-f :model :universe]` | Same seven members as the preceding row; same malformed carrier. A computed-F diagnostic is not evidence of selection consumption. |
| `[:token-belief-stage :prospective-prior :universe]` | Three pairs under `M-f11-find-production-successor`: `:admission/task-stated`, `:hole/h9ab212b3281d`, `:hole/h2045faa0e7cc`; literal powerset size 8. The role/join to the other carriers is not established by this count. |
| `[:token-belief-stage :initialization :value]` | Exactly `{#{[T :admission/task-stated]} 1}`. This establishes support size one at this path, not a uniquely identified model universe. |

The six-member measurement in §6 is therefore path-specific, **not** a
measurement of the uniquely consumed carrier. Do not zip the bare string and
keyword into a pair, insert the missing fact, or discard the M-f11 carrier by
name. Preserve all source values and report malformed/incompatible carriers.
No claim that 64 states suffice for this record's full witness survives.
Theoretical 4096/16384 matrix-cell counts for 64/128 states are only generated
metadata; enumeration size does not establish semantic validity. Any sparse
proof must cover the declared full carrier through a separately proved
embedding/equivalence with its support-closure hypotheses, not assume update
or rollout stays inside a chosen sparse subset.

### R2.3 Proposed shared B encoding and commit-point amendment

This is a **proposed amendment to CERT-S §1 B, A13/A14 and THEOREM W5/P5**,
not a claim that the rejected B-D already defines this schema or B-C implements
it. Use a successor certificate/extract schema, proposed
`:wm/proof2-certificate-v2` / `:wm/proof2-extract-v2`; old records stay unchanged.

**Population and visibility.** At R2, `learning_trial_ledger.clj:24–69`
appends admitted measurement rows, writing/syncing at 61–63;
`full_loop_runner.clj:3161` invokes that append while retaining comparison
evidence. `learning_trial_ledger.clj:173–179` separately guards the later
`b-update` with accepted-close status. The reader at `:273–285` filters by
pattern key, collapses identities and computes theta; it does not filter by
accepted close. Thus the B visibility/commit point is **rows appended at
comparison**, not accepted close. Record append sequence/content identity and
read snapshot boundary, not merely a later close timestamp. Accepted
measurement, counted ledger row and accepted increment are distinct fields.
No filtering out refused closes is allowed as an extraction “repair.”

**Physical encoding proposal.** The canonical close envelope address is
`[:payload :judgment :b-update]` (logical `[:b-update]` in CERT-S). It contains
the following value or a content ref fixed by the close, with the update's
earlier comparison/append identity preserved. Resolved content is the B value
itself, not another envelope. Both physical and logical paths must appear in
the path-schema. A retained file may hold that content under
`<attempt>/retained/`; its filename alone is not the immutable join.
At R2 `full_loop_runner.clj:4247–4279` writes a post-close retained
`b-update.edn`, using `:carrier` or typed absence. That is **not** proof of the
proposed close-bound reference or comparison-time carrier: the binding and
producer order remain B-C/B-R obligations. Extraction cannot add them later.

For each parameter, use `:families {<pattern-id> <family-value>}`; pattern id
is the theta key, distinct from a trial-configuration digest. A family value
has these mandatory fields, with exact field schemas rather than “any shape”:

- `:carrier {:outcomes [:achieved :not-achieved] :states [:attempt]
  :shape [2 1] :grain :occurrence-effect :meaning-sha256 <hash>}`.
  Arrays are **outcome-major matrices**, two vectors each of length one.
  Family keys are not another state axis; each family has its own Fin 2 × Fin 1.
- `:prior-concentrations` and `:posterior-concentrations`: positive exact
  rational matrices in those axes. This replaces B-D's named-cell maps and
  CERT-S's unindexed vectors by an explicit versioned encoding. No extractor
  converts an old map to this producer carrier and calls it recorded.
- `:trial-identities`: ordered vector of **content refs to complete immutable
  counted ledger rows**, in identity order (ascending dedup-identity string).
  Each row's dedup identity is separate from its recomputed content hash.
- `:trial-vectors`: equally long vector; entry j contains `:identity`,
  `:source-row-ref`, `:outcome` (`[1 0]` or `[0 1]`), `:state-belief [1]`,
  `:carrier-sha256`, and the recorded acceptance/attribution reference. Entry j
  must join identity/ref j exactly. Boolean true maps to achieved; false to
  not-achieved. Missing/non-Boolean observations refuse, never map to false.
  Check exact one-hotness, normalized singleton, lengths and every carrier
  key. `[2]` or a token-state map cannot be an admitted state vector.
- `:row-dispositions`: full ordered source-row census with ledger position,
  row ref, dedup identity, admission status, close acceptance (true/refused/
  typed unavailable), attribution and counted/excluded reason. It preserves
  rows excluded from `:trial-identities` and joins the immutable snapshot.
  `:dedup` explicitly records append admission and read-side collapse; identical
  repeats contribute once, conflicting same-identity rows are a provenance
  failure, not silently merged. Existing `into {}` at ledger `:275` is
  last-wins and does not establish this conflict check.
- `:normalization {:outcome :achieved :state :attempt
  :numerator <recorded-rational> :denominator <recorded-rational>
  :theta <recorded-rational> :rule <versioned-definition-id>}`; verify the
  numerator equals posterior[0][0], denominator equals the sum over outcomes
  at state 0, denominator > 0, theta equals their quotient. These are recorded
  operands checked against a formula, never filled by GEN-E.
- `:commit-point`, `:snapshot-ref`, `:version`, `:value-sha256`, producer
  identity and consumption joins: bind the family, carrier meaning, prior,
  ordered rows/vectors, dispositions and normalization. Version hashes a
  specified semantic payload excluding its own hash fields; term hashes
  likewise name their value subject explicitly. Temporal successor claims
  additionally require the independent `:B-read` joins, not the close alone.

B-C must produce vectors/arrays contemporaneously from the actual population;
GEN-E only verifies and transcribes them. If the writer cannot retain per-row
close acceptance when appending, it records typed unavailable then; a later
close record can link its acceptance without rewriting the earlier row.
No missing field is inferred from a later recount. A future read needs an
immutable snapshot with the consumed row population; a mutable ledger path
and timestamp alone do not define a version.

The exact normalization definition owed to B-N is, for finite O at fixed s,
`theta(a,o,s) = a.conc o s / (sum o' : O, a.conc o' s)`.
For O=Fin 2, S=Fin 1, achieved=0, state=0, Jeffreys prior in both cells and
n normalized one-hot trials with s successes, this yields
`(s+1/2)/(n+1)`. It does **not** divide a fixed outcome across states (which
would give 1 in Fin 1), nor sum all families. R2's scalar formula is at ledger
`:280`; L2 `WM/DirichletLearning.lean:50–66,82–85` supplies accumulation,
not that interpretation of whole-attempt observations as token posteriors.
Amend A13/W5 to name the admitted occurrence/effect measurement and explicit
singleton mapping. Until that amendment is reviewed, arithmetic specialization
is not full W5 compliance. Duplicate [i,i] must fail the dedup/identity check;
Lean accumulation correctly adds both if actually given both.

### R2.4 Typed hashing, enumeration and a defined machine-F operand

Source-value hash and enumeration are different objects. Preserve sets as sets
and maps as maps under CERT-S §3's canonical UTF-8 codec. Record a separate
`:generated-metadata :enumerations` entry with source path/hash, canonical
ordering rule, ordered elements, and bijection check. A vector representing
an enumeration is never hashed as though it were the source set. Preserve
candidate and trial vector order; do not reorder semantic vectors. Hash an
extract payload without its own digest/verifier receipt; hash verification and
proof receipts separately. Recompute ref-map and resolved-value hashes as
R2.1 requires. The source entry's hash never covers itself or proof metadata.

Adopt SPEC-N §3, replacing original §1.5/AM-2. Finite binary64 encoding decodes
to an exact rational, preserving original raw signed-zero/hex identity. A
round-tripping decimal printer need not lose bits; the error is interpreting
its displayed decimal as an exact decimal rational or treating an approximation
as the ideal real. The declared output codec remains `#wm/double "<hex>"`.

**Proposed amendment to CERT-S F and THEOREM W3/W6:** name the scalar path
SC `:model-inputs <id> :F :total`; name the ordered symbolic-input path SC
`:model-inputs <id> :F :f-prefix`, and the expression declaration/version at
SC `:model-inputs <id> :F :ideal-expression`. The latter identifies the
mathematical definition, not an independently asserted numerical value.
These are proposed paths, not observed exemplar fields. On successful verified
finite decoding, emit the following mathematical construction (API names owed
to NUM-R/L; no undefined real or axiom):

```
h := the independently checked hex string at the scalar source path
r : ℚ := the literal rational returned by DecodeExact(h)
prove decode_binding : DecodeExact(h) = some r
machineFTotal : ℝ := (r : ℝ)
idealFTotal : EReal := the declared prefix-VFE expression on recorded inputs
```

For finite ideal F, prove SPEC-N's interval relation between `machineFTotal`
and the real expression. Do not define machineFTotal from idealFTotal.
If decoding fails or F is absent, emit the typed diagnostic and no inhabited
machine-F operand; do not select zero, an arbitrary real or a default branch.
Mathematical top is a separate support case, not a nonfinite hex literal.
A rational sum of decoded doubles is not an exact mathematical prefix F.

The authoritative proposed numeric entries remain SC `:numeric-refinement`,
keyed by full scalar path. Each entry explicitly names source value hash,
candidate small id **and** payload hash, codec/decoding, expression/input refs,
proof-artifact hash and budget identity. Candidate/policy/decomposition aliases
must independently resolve to the same scalar value/hash with their candidate
joins checked; nested prefix refinement fields are refs to this authoritative
collection, not unchecked copies. Action-marginal entries name **all** owning
candidate joins. This is an extension of CERT-S, requiring version review,
not permission to silently change v1. A strict policy-score gap means a proved
ideal-score gap, not a gap between rounded point estimates even with injective
policy-to-action mapping (SPEC-N review amendment 1).

### R2.5 Emitter independence and corrected witness precedents

Replace the “both sides reduce to the same def” rule. Literal finite equalities
can legitimately close by reduction, `rfl` or `norm_num`. The forbidden step is
**constructing the recorded operand from the model formula under test**.

Proposed independently checked emitter grammar: each source-operand declaration
is generated solely from the verified source entry, using literals, finite
constructors/indexing and the reviewed exact numeric decoder/cast. Bind every
cell/leaf to a source path plus index and source hash. A separate checker walks
the emitted operand definitions and their transitive dependencies, checks this
restricted grammar, and compares the decoded literal carrier to the verified
extract. It must not trust the emitter's self-reported dependency list. Recorded
posterior definitions may not call `accumulate`, normalize priors/trials, invoke
the scorer, or depend on the asserted model output. The theorem's other operand
names the reviewed model definition applied to separately sourced inputs.
Proof terms may evaluate arithmetic and unfold both sides; this does not change
operand provenance. Carrier positivity proofs can depend on verified literals,
but cannot introduce assumed empirical equalities. Enforce no extra source
operand axioms, kernel proof checking and transitive axiom audit separately.

Negative controls: a forged `postConc := accumulate prior trials h` fails the
operand grammar even if its theorem proves by rfl. Changing a source trial cell
while leaving the emitted literal unchanged fails source/operand equality;
changing it in both places but keeping an inconsistent recorded posterior must
fail the arithmetic proposition. Parser/import failures are invalid negatives.
The absent-to-recount case fails the earlier provenance verifier even if its
forged arithmetic theorem compiles. No implementation of these gates is claimed.

Corrected source precedents (opened at L2):

| Source | What it supplies, and does not |
|---|---|
| `WM/DirichletLearning.lean:19–21,50–66,82–85,98–104` | Positive concentration structure, step, accumulate, pointwise equation, append theorem. Corrects original 46/58/77 anchors. Does not supply source extraction or the new normalization schema. |
| `WM/ExpectedFreeEnergyWitness.lean:15–29,44–53` | Literal Q/C and reference value; `onePointFixture` at 50 proved by norm_num at 53. A finite fixture precedent, not immutable-record provenance. |
| `WM/DirichletConcentrationsNegative.lean:5–11` | Expected unsolved `False` goal under `#guard_msgs`; semantic positivity negative. Does not test absent-source reconstruction. No fresh build claimed here. |
| `WM/MachineObservationWitness.lean:3–6` | Readback cross-reference in a header. No structured provenance-manifest or symbolic hex/log codec established by that header. |
| `WM/MachinePolicyFreeEnergyWitness.lean:6–8,18,55` | The actual symbolic-log precedent, with separate floating deltas; not a binary64 decoder or certified floating-error bound. |

At R2 `checks/lean_sorry_category_check.clj:7,51–60` is a fixed-Holes source
check, not the proposed general transitive import audit. Source/hash identities
alone also do not establish execution: `src/futon2/aif/load_identity.clj:101–131`
records loaded bytes and checks them against disk; an extractor/verifier receipt
must bind what actually ran. Broader emitter/axiom/absence gates remain owed.

### R2.6 Amendment register, falsifiers and next work

All changes to external contracts in this appendix are **proposed amendments**:

1. **CERT-S / GEN-E:** successor version, independent fixed-path fidelity and
   completeness checks; physical close envelope/ref; typed source hashing
   separate from enumeration/metadata; explicit B arrays/axes/ordered vectors,
   statuses, dedup and commit/read snapshot fields (R2.1–R2.4).
2. **ASSUME A13/A14 and THEOREM W5/P5:** distinguish comparison-time admitted
   measurements from accepted increments; name actual append visibility and
   snapshot consumption; fixed-state outcome normalization and explicit
   singleton mapping. Neither an accepted-close filter nor a token-posterior
   interpretation may be inserted by the extractor (R2.3).
3. **CERT-S F and THEOREM W3/W6:** decoded machine scalar versus ideal prefix
   expression/refinement, precise scalar path and alias joins (R2.4), following
   SPEC-N; no exact-rational mathematical log total.
4. **THEOREM extraction/emission wording, A16 verification contract:** replace
   number-membership/self-reported independence with independent path/value
   verification and operand dependency checking. This operationalizes A16;
   it does not relax missingness or permit reconstructed production facts.

**Exact row falsifier:** a required posterior is omitted or explicitly missing,
but the extract contains `[[3/2] [1/2]]` obtained from a ledger recount. Correct
hashes and correct Lean arithmetic do not save it: R2.1 requires independent
`:source-presence-mismatch`. **Emitter falsifier:** a posterior defined by
`accumulate` rather than transcribed source cells fails R2.5 before proof credit,
even if its equality compiles. Both negative checks require implementation and
independent registration/execution before any positive evaluation.

**Unblocks after re-review:** GEN-E's independent transcription verifier and
negative fixtures; a B-C/B-N shared carrier contract and GEN-B's operand gate.
It does not establish those implementations, a valid exemplar carrier, read
continuity, W5 standing or theorem success. Missing definitions are precisely
the proposed schema, fixed-state normalization, decoder/refinement interface
and independently checked operand grammar above, not questions deferred to
an operator. Validation here was read-only source inspection, exemplar
EOF/path inspection and raw SHA-256 reproduction; no Lean build, click,
shared-JVM evaluation or data write occurred.
