# CERT-S — the certificate join schema, P₀–P₆

Packet 4 of `PROOF-2-STRATEGY-draft-2026-09-24.md` (codex-4, f71aa3e6).
Author: kimi-5, 2026-09-24. Discovery/specification only: no code, data,
bundle, or Lean file changed. Sources read: the THEOREM draft (P₀–P₆, the F
sentence, the Correction and Review amendments), ASSUME A16/A17/A21,
`src/futon2/aif/candidate_derivations.clj` (zai-2, d98fe4c7/8f0a2036),
the judge hunk at `scripts/futon2/report/war_machine.clj:6380–6400` and
`:6494–6497`, and the exemplar record
`data/wm-runs/tick-run-record-2026-09-23-1790131591.edn` (run/id
`2026-09-23-1790131591`, click `wm-click-e3e4479c-e873-4dae-8292-44de95c317e6`,
read with an EDN reader tolerating tagged literals). All file:line anchors
below are at futon2 `d0a369dd519b492c1a1e8c73fe1a5447a9741efe`.

## 0. Name and declaration

The schema is `:wm/proof2-certificate-v1`, declared at

`[:decision :selection-certificate :certificate-schema]`

— a key on the certificate itself, not in a sidecar, so that a record
carrying no declaration is unambiguously pre-schema. Any change to a rule in
this document is a new version keyword; v1 records are never edited into
compliance (A16: no after-the-fact reconstruction).

The THEOREM draft's proposed keys are **ratified**: `:candidate-derivations`,
`:model-inputs <id> :A/:D/:F/:B`, `:Q-link`, `:B-read :predecessor-chain`,
and the close's `:b-update`. One amendment and one addition, each once,
with reasons:

- **Amendment — the candidate join key is the small id, not the payload
  map.** Today a candidate is named by the map `{:kind :cascade-candidate
  :id :C2 :target … :precedence […] …}`, and the exemplar materialises that
  payload inconsistently across sections: `:target` is a *symbol* under
  `:candidates` and a *string* under `:policies`, `:scoring`, and
  `:g-term-decomposition` (EDN print/read variance, observed directly in
  1790131591). A whole-map equality join is therefore unreliable on the
  record's own bytes. The schema's join key is the keyword at `[:id :id]`
  (e.g. `:C2`), and every section that carries the payload must additionally
  carry `:candidate-payload-sha256` (§3) of the canonical payload. Two
  occurrences name the same candidate iff id and payload hash agree.
- **Addition — every term entry carries a `:consumption` pair.** Each
  term-level map named in §1 carries `:value-sha256` and `:consumed-at`
  (§3). The THEOREM draft lists these for `:A` only; A16 requires the
  recorded/consumed distinction for every term, so the pair is uniform
  rather than per-term bespoke.

## 1. Term → key path → value form → absence form → consumed-at locus → join key

Value forms are one of: **exact rational** (EDN ratio or integer, printed
as-is — the only form that can witness a real-valued equality; the
exemplar already uses ratios, e.g. `:focused 11/20`); **machine double**,
recorded as the tagged literal `#wm/double "0x1.…p…"` carrying the
`Double/toHexString` encoding so the printed record loses no bit
(the strategy's exact-arithmetic problem §1.1: a decimal print of a double
is a silent rounding); **keyword/enum**; **finite map/vector** of the
above; or **content-addressed ref** `{:content-ref "sha256:…"}` for values
too large to inline (e.g. full q maps), fixed at selection time.

Every typed absence is zai-2's form, ratified: `{:status :missing :reason
<keyword> …}` for a value never produced, `{:status :not-supplied :source
… :reason …}` for a value the law positionally expects but no producer
supplied (the F case), and the whole-carrier refusal `{:status :refused
:kind :candidate-id-mismatch :reason …}` from
`candidate_derivations.clj:133–143`. **A bare `0`, `0.0`, `nil`, or
omitted key is never a lawful absence** — this is the F sentence's defect
written as schema.

| Term / clause | Key path(s) (`[:decision :selection-certificate …]` unless noted) | Value form | Typed-absence form | `:consumed-at` locus (current revision) | Join key to candidate |
|---|---|---|---|---|---|
| Field & scores (P₀) | `:candidates`, `:policies`, `:scoring` | vector of candidate maps; `:scoring` entries maps with `:id`, `:c`, `:rates-provenance` | `:status :missing` per entry; whole-field refusal as above | scorer assembly `war_machine.clj:6494`; scoring consumption `cascade_selection.clj:110–116` | `[:id :id]` + `:candidate-payload-sha256` in all three sections |
| Derivations (P₀) | `:candidate-derivations <id>` | per-candidate map of the 14 `p0-fields` (`candidate_derivations.clj:17–20`) | per-field `:missing` with reason; per-candidate `:status :inadmissible` with `:offending-path`/`:offending-kind` | judge assembly `war_machine.clj:6494–6497`; admissibility verdict `cascade_equivalence.clj` (`admissible-provenance?`) | map key **is** the small id |
| Selected action (P₀/P₃) | `[:decision :selection-law :per-policy-argmax :action]`, `:action-marginal`, `:posterior` | candidate payload map / action map; posterior finite map id→probability | `:refused` `:no-admissible-candidate` (`cascade_selection.clj:104–108`) | argmax/posterior `cascade_selection.clj:51–116` | payload hash; **note**: `:action-marginal` is today keyed by *precedence-pattern* maps, not candidate ids — the marginal's candidate attribution must go through the pattern→candidate owner key (§4, bad join BJ-3) |
| A (P₁) | `:model-inputs <id> :A` | `:rates` exact-rational map, `:estimator` keyword+version, `:population`/`:window`, `:counts` integers, `:source-row-identities` vector of content refs, `:value-sha256`, `:consumed-at` | `:missing` with reason; identity-default is a *value*, and its provenance must say `:estimator :identity-default` so X₁ can refuse it | likelihood consumption `cascade_model_manifest.clj:200` (`token-likelihood`), row production `:174`, predicted outcome `:217` | `:model-inputs` map key; cross-check `[:decision :selection-certificate :g-term-decomposition :policies <i> :terms :A]` via payload hash |
| D (P₂) | `:model-inputs <id> :D` | `:sPrev` finite token-state map (exact rational masses), `:o` observation set, `:A`/`:B` version ids + content refs, `:q` exact-rational map or content ref, `:value-sha256`, `:consumed-at` | `:missing` `:conditioning-not-admitted`; refused conditioning carries the admission refusal map | exact-update consumption `cascade_model_manifest.clj:620,648`; certificate anchors `:token-belief-input`, `:token-belief-stage :observation-updates` | `:model-inputs` key; D is field-level, so the same value must appear under every `<id>` — the schema says so explicitly (§4, BJ-4) |
| F (P₃) | `:candidates <i> :f`, `:policies <i> :f`, `:g-term-decomposition :policies <i> :terms :F`; inputs at `:model-inputs <id> :F` (`:A`,`:B`,`:sPrev`,`:o`,`:q`, per-step `:f-prefix` receipts, total) | F total: machine double **and** exact-rational prefix-sum dual-carrier (the numerical refinement the strategy §1.1 owes); prefix: vector of per-step receipt maps | `:not-supplied :source :policy-prefix-evidence :reason :no-admitted-policy-prefix` (the form `efe.clj:1265–1272` and `policy_prefix_evidence.clj:56–70` already emit); never the silent `0.0` of `cascade_selection.clj:112` | consumed at the softmax score `cascade_selection.clj:112`; computed at `cascade_free_energy.clj:105`, attached `efe.clj:1126`; decomposition verdict `g_term_decomposition.clj:101` | all three occurrences must carry the same small id and equal `:value-sha256` — this is the X₃ "logged F ≠ consumed F" check's data |
| Q (P₄) | `:Q-link` | vector of link maps: `:predecessor-click-id`, `:action-sha256`, `:outcome-row-ref`, `:A-version`/`:B-version`, `:prior-sha256`/`:posterior-sha256`, `:consumed-at`; first click of L carries `:boundary :first-click-of-L` with the declared boundary state | `:missing :reason :no-predecessor-in-L` (genuine first click) or `:refused :observation-admission-refused`; never a `:closed-loop true` flag (A15 stub risk) | predecessor admission `token_belief_predecessor`; next-click consumption at `:token-belief-input` assembly | the chain joins by click id and content hashes, **not** by candidate — Q is a temporal, field-level term |
| B (P₅) | close record's `:b-update`; certificate `:model-inputs <id> :B` (consumed θ); root `:B-read :predecessor-chain` | `:b-update`: exact-rational prior/posterior concentration vectors, `:trial-identities` content refs, `:dedup` receipt, `:version` hash; θ exact rational `(s+1/2)/(t+1)`; `:B-read`: vector of read maps `:version`/`:read-at`/`:commit-point` | `:missing :reason :no-eligible-outcome` (B is updated only when eligible); `:defaulted :reason :ledger-read-failed` (judge's existing form, ratified) | write `learning_trial_ledger.clj:154` (`b-update`); rule/normalization `:238` (`pattern-theta`); unconditional pre-selection read `war_machine.clj:6380–6393` (the judge's per-family read before scoring) | θ is per *pattern family*, stamped onto precedence patterns (`:theta-source :recorded-trials`); candidate attribution is via the candidate's precedence pattern ids |
| E, C, γ/β, tie rule (P₆) | `:candidates <i> :habit` (+`:habit-provenance`/`:habit-status`), C under `:scoring <i> :c`, `[:decision :selection-law] :beta/:gamma/:tie-break-rule` | exact rational where produced as one; C steps with per-step `:tau` and distribution | `:habit-status` enums as today; C `:form :step-indexed` with explicit nil distributions is a **recorded absent** and must say why | softmax consumption `cascade_selection.clj:110–116`; G consumption `cascade_selection.clj:113`; decomposition verdict `g_term_decomposition.clj:101` | small id + payload hash |
| Enumeration (P₀) | `[:decision :enumeration-completeness]` | exclusion receipt: counts by reason, phantom list, version keyword | absences here are findings, not term absences | discovery census, `war_machine.clj:6005–6013` (named in the receipt's `:independent-of`) | n/a (field-level) |
| Initial belief (P₂ boundary) | `:token-belief-stage :initialization :value`, `:initial-belief-receipt` | point mass: finite map with one state at mass 1; else mixture map + the `:initial-state-not-a-point-mass` absence when s₀ is demanded (`candidate_derivations.clj:22–31`) | `:missing`/typed origin map as today | assembly of continuation belief; read by `s0-of` | n/a |

## 2. The id bijection rule

One candidate id — the keyword at `[:id :id]` — across:
`:candidate-derivations` (map keys), `:candidates`, `:policies`, `:scoring`
(each entry's `[:id :id]`), `:model-inputs` (map keys),
`:g-term-decomposition :policies <i> :id`, and the action marginal /
`per-policy-argmax` selection (via payload hash).

**Rule:** the id sets of these six sections are equal, and payload hashes
agree per id. **Violations and refusal forms** (ratifying zai-2's carrier,
`candidate_derivations.clj:133–143`, extended to the same shape in every
section):

- `{:status :refused :kind :candidate-id-mismatch :reason :candidate-ids-absent-or-not-unique :ids […]}` — nil or duplicate id in any section;
- `{:status :refused :kind :candidate-id-mismatch :reason :action-id-not-in-candidates :unmatched […]}` — a selected/argmax action id outside the field;
- `{:status :refused :kind :candidate-id-mismatch :reason :section-id-sets-differ :only-in {…}}` — new: the six-way bijection fails;
- `{:status :refused :kind :candidate-payload-drift :id <id> :hashes {…}}` — new: same id, differing `:candidate-payload-sha256` across sections (catches the symbol/string materialisation trying to hide).

A partial map is never emitted: the refusal replaces the whole carrier, so
no consumer can read half a field.

## 3. Content-hash and `:consumed-at` semantics

**What is hashed.** The canonical EDN of the value: maps sorted by the
canonical printed form of their keys, sets sorted likewise, ratios and
integers printed exactly, doubles printed as `#wm/double "0x…p…"`, strings
and keywords verbatim, no metadata, no comments, UTF-8 bytes. The hash is
`sha256` of those bytes, written `"sha256:<hex>"`. Large values (full q
maps, ledger vectors) may be replaced by `{:content-ref "sha256:<hex>"}`
pointing at a store fixed at selection time; the in-record hash then covers
the ref map, and the ref must resolve to content whose own canonical hash
equals the ref — a reader **recomputes**, never trusts a hash string
embedded in the object it would verify (GEN-D's rule, restated here as
schema).

**Where the hash lives.** At the term entry itself as `:value-sha256`;
payload hashes at every section that inlines a candidate payload as
`:candidate-payload-sha256`; the record's own identity is the existing raw
file SHA-256 (A19 lists the exemplar's).

**Recorded vs consumed (A16, as two independent predicates).**
- `recorded(term)` ⟺ the entry carries a lawful value form or a lawful
  typed absence, plus `:value-sha256`, plus producer provenance.
- `consumed(term)` ⟺ the entry carries `:consumed-at {:function <symbol>
  :file <path> :line <int> :code-identity <sha256-of-source-file>}` naming
  the exact reader locus (§1 column 5), and the consumer's section carries
  the same `:value-sha256` it read.
- The predicates are independent by construction: `:computed-f` on the
  exemplar is recorded-not-consumed; `cascade_selection.clj:112`'s `0.0`
  fallback is consumed-not-recorded. Both fail clause 6; the schema makes
  each visible separately.

## 4. The bad joins CERT-C must refuse

- **BJ-1: correct number, wrong candidate.** A `:model-inputs` or `:f`
  value that satisfies a Lean instance but sits under the wrong id, or is
  joined positionally. Detectable from the record alone *because* the join
  is by id + payload hash and the per-section hashes must agree;
  `g_term_decomposition.clj:101`'s current by-position join is exactly the
  hazard this schema replaces, and a positional-only record fails BJ-1 by
  construction (no hashes to compare).
- **BJ-2: post-selection source presented as input.** A term whose
  producer provenance or `:consumed-at` locus postdates selection — e.g. a
  prediction or a diagnostic like `:computed-f` (produced for evidence,
  never read at `cascade_selection.clj:112`) presented as the consumed F.
  Detectable: the `:consumed-at` locus names *who read it*; a value whose
  only reader is a diagnostic fails `consumed`, and the law receipt must
  name the law that ran (`σ(log E − γG)` when `:f-status :not-supplied` —
  the F sentence's owed record).
- **BJ-3: self-attesting hash.** A `:value-sha256` or content ref whose
  bytes are supplied inside the same unverified object. Refused by
  recomputation: CERT-C hashes the canonical value itself and compares.
- **BJ-4: singleton masquerading as per-candidate.** A field-level term
  (D, the A universe) copied under one `<id>` while other candidates carry
  absences, read as if per-candidate. The schema requires the same
  field-level value under every `<id>` with the same hash — the sameness
  is then checkable, and a divergence is BJ-1 in per-candidate clothing.
- **BJ-5: identity-default A with measured-A clothing.** Rates equal to
  the identity default whose `:estimator` field claims a measured
  estimator; refused by cross-checking `:source-row-identities` (a measured
  estimator names its rows; the default names none).

## 5. Version

`:wm/proof2-certificate-v1`, declared at
`[:decision :selection-certificate :certificate-schema]` (§0). Amendments
that change a value form, an absence keyword, a join rule, or a hash
canonicalisation are a new version; clarifications that change none of
those bump only this document.

## 6. What the exemplar fails, field by field

Run `2026-09-23-1790131591` against v1 (this is clause 6's first honest
reading; the THEOREM draft's per-clause failure sentences agree):

1. **Field cardinality.** One candidate (`:C2`). P₀ needs ≥ 2 — fail.
2. **`:candidate-derivations`.** Key absent entirely — fail by absence
   (the carrier was only built later, by zai-2's 2b).
3. **Provenance kinds.** `:construction-receipt :kind :hand-admitted` on
   the candidate, and every `:interpretation-receipts` entry
   `:hand-admitted` — X₀(b)'s failing condition, present in the positive
   record: `:status` would be `:inadmissible`, `:offending-kind
   :hand-admitted`.
4. **F.** `:f nil`, `:f-status :not-supplied`; the consumed score used
   `0.0` at `cascade_selection.clj:112` with no record that the law that
   ran was `σ(log E − γG)`; no q anywhere; `:computed-f` is
   recorded-not-consumed with `:provenance :status :synthetic`
   ("unmeasured -> stop-the-line", the stipulated 55/35/5/5 — also EC-D's
   falsifier for C). Fails P₃ recorded, consumed, and provenance
   predicates separately.
5. **A.** No `:model-inputs`; `:rates-provenance :source
   :observation-model/query` with the synthetic model; g-term
   decomposition `:status :missing`. Fails P₁; the identity-default
   question (X₁) is moot because no estimator claim exists to check.
6. **D.** `:token-belief-stage :initialization` is the point mass
   `:target-qualified-true-facts-point-mass-v1`;
   `:conditioning-status :awaiting-observation-admission`;
   `:observation-initialization` policy `:enabled false`. Fails W₂'s
   nondegeneracy and P₂'s admission.
7. **Q.** No `:Q-link`; the only predecessor evidence is
   `:token-belief-input :inspection :identity` naming the previous run —
   a co-occurrence, not a consumed chain (A15). Fail.
8. **B.** No `:b-update`, no `:model-inputs <id> :B`, no `:B-read`;
   precedence patterns carry **no** `:theta` at all (the judge's
   `pattern-theta` read at `war_machine.clj:6380` landed only in later
   runs — cf. the Correction's 1790199409 evidence). Fail by absence.
9. **Join key hygiene.** `:scoring` is keyed by ordinal (`{0 {:id …}}`)
   with the candidate named by the payload map, and the payload
   materialises `:target` as symbol under `:candidates` but string
   elsewhere — v1's §0 amendment exists because of this record. No
   `:candidate-payload-sha256` anywhere.
10. **Schema declaration.** No `:certificate-schema` key — correctly
    unreadable as a v1 record; it stands as a pre-schema negative
    fixture, exactly its assigned role.

**Falsifier check (mandatory, from the strategy row).** BJ-1 is detectable
from the record alone *only because* v1 requires per-section payload hashes
and id-keyed sections; on the exemplar's positional/hashless layout BJ-1
is *not* detectable, which this schema records as a failure of the record
layout (§6.9), not a softened requirement. BJ-2 is detectable via the
`:consumed-at` locus + law receipt; absent those keys the record fails
`consumed` outright rather than passing silently. Both requirements stand
as written.

## Discrepancy notes for zai-2 (2b/2c carriers)

No code changed here; these become slices, not rewrites:

- `candidate_derivations.clj` emits `:source-id` as the small id but the
  map is keyed by it; v1 additionally wants `:candidate-payload-sha256`
  per entry so the derivations section joins the payload-carrying
  sections.
- The judge hunk (`war_machine.clj:6380–6393`) stamps `:theta`/`:theta-source`
  onto patterns but writes no `:B-read` record with version/commit-point;
  B-R's producer seam is that hunk plus the ledger root it reads.
- `cascade_selection.clj:112`'s fallback is the known F-sentence defect;
  v1's `:not-supplied` form plus the law receipt is F-ABS's target shape,
  already emitted by `efe.clj:1265–1272` and `policy_prefix_evidence.clj:56–70`.
