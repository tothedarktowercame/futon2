# GEN-D independent review — codex-21

2026-09-24. Reviewer: codex-21, OpenAI GPT-6/Codex; not the packet author
(kimi-2). This is one independent review, not all A20 signatures or theorem
standing. **Verdict: REJECT.** Before GEN-E is unblocked, specify an independent
source-path/value check that rejects the row falsifier, reconcile the recorded
carrier discrepancy, and resolve the carrier/numeric contracts below.

## 1. Record and revision check

`git show c7fe3001 --stat` resolves to
`c7fe300175216fd10b17307f41aaa30a2b3954ac`: exactly one changed file,
`holes/labs/wm-contract/proof2/packets/GEN-D.md`, 390 insertions. PASS.
The working packet is unchanged from that commit. All GEN-D line references
below refer to this revision.

Code was opened using `git show REV:PATH`, not inferred from packet line numbers:
R = futon2 `93531d415a115c7b426f37e06d7d5542ecb5dae4`;
L = mathlib4 `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`.
Sibling B-D was read at `64b091ce`, SPEC-N at `634877af`.
CERT-S's current bytes equal its bytes at R (SHA-256
`652b212f8a84e4ab430395c1b6b910e3ab29a3b41b284b335f2503765bb305ff`);
its last-changing commit is `9250eaf12530f2ac7c7a25dac1ff628329042a80`.
Read ASSUME A1–A21, THEOREM W0–W6 and review requirements, and STRATEGY's
packet/dependency and generator rules. No runtime namespace was loaded, no
click or Lean build was run, and no data file was changed.

## 2. Independent citation and record checks

Paths prefixed WM below mean `DarkTower/WarMachine/` in mathlib4.

| Pinned source opened | Finding |
|---|---|
| L `WM/DirichletConcentrationsNegative.lean:5–11` | **Holds.** The docstring says `error: unsolved goals` followed by `⊢ False`; line 10 is `#guard_msgs in`; line 11 tries `⟨[1, -1], by norm_num⟩`. This is the claimed semantic negative-witness source form. I verified the source, not a fresh successful build. It tests positivity, not the extraction provenance falsifier. |
| L `WM/DirichletLearning.lean:19–21,50–66,82–85,98–111` | **Substance holds; several line numbers do not.** `DirichletParams` has `conc : O → S → ℝ` and strict positivity. `step` starts at 50, `accumulate` at 60, `accumulate_conc` at 82, `accumulate_append` at 98. GEN-D's 46/58/77 are not those declarations. This file provides accumulation, not the proposed normalization. |
| L `WM/ExpectedFreeEnergyWitness.lean:15–29,44–53` | **Holds with corrected location.** Literal one-point distributions and `efeReference` are separate from the formula; `onePointFixture` is at 50, proved by `norm_num` at 53. Line 46 is a field value, not the theorem. |
| L `WM/MachineObservationWitness.lean:3–6,48–70` (and whole-file search) | **Partial/fails as precedent.** The header links a readback and the file proves counterexamples to bounded observations. It does not contain symbolic log/hex encoding definitions. GEN-D 214–216 and 379–380 misattribute that precedent; 169–170 overstates the header as provenance fields. |
| R `src/futon2/aif/learning_trial_ledger.clj:154–236,238–290` | **Holds.** The scalar updater returns status/family/theta/rule/trials/read-back, not concentration arrays or normalization receipts. `pattern-theta` computes `(/ (+ successes 1/2) (+ n 1))` at 280 after deduplication. |
| R `scripts/futon2/report/war_machine.clj:6375–6396` | **Holds.** It calls `pattern-theta` at 6383 and stamps recorded theta/source onto precedence patterns before scoring. This establishes a consumer, not a versioned continuity certificate. |
| R `src/futon2/aif/load_identity.clj:101–131` | **Holds.** `register!` captures source bytes/digest when loaded; `check` compares that capture with disk and distinguishes unavailable/unregistered/stale. A source hash computed later alone is not this load identity. |
| R `checks/lean_sorry_category_check.clj:7,51–60` | **Limited precedent only.** Its fixed source is `Holes.lean`; declaration inspection detects `:= sorry`. It is not already the general transitive-module no-sorry gate GEN-D 202–204 requires. That gate remains implementation work. |

### Record reproduction with `bb`

Read `data/wm-runs/tick-run-record-2026-09-23-1790131591.edn` with
`clojure.edn/read`, `{:default tagged-literal :eof ::eof}`, a PushbackReader,
and a second read requiring EOF. EOF held. Raw SHA-256 independently matched
`7314951f0ad6d339d561a9f7ec4f5dc14042873e4602873dddb590701ba61f25`.
Let SC = `[:decision :selection-certificate]`.

* `SC :token-belief-stage :initialization :value` is exactly a mass-1 map
  on the singleton state containing `[T :admission/task-stated]`, where
  T is `T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade`.
* `SC :certificate-schema` is absent, measured using a distinct missing-key
  sentinel; no value was supplied in its place.
* `SC :candidates` has one entry; `SC :candidates 0 :id :precedence` has
  four patterns, each with one `:produces` fact and **absent** `:theta`
  (`contains?` checked separately). GEN-D's shape claim holds; nil should
  not be used as the presence classification.
* `SC :token-belief-input :carry-admission :current-universe` contains
  exactly the six fact pairs listed by GEN-D 298–305. Its powerset has 64
  elements. **64 is reproducible for this particular recorded carrier.**
* But `SC :scoring 0 :rates-provenance :model :universe` and
  `SC :candidates 0 :computed-f :model :universe` each contain **seven**
  elements: five fact pairs, the bare string T, and the bare keyword
  `:admission/task-stated`. The obstruction-cleared fact is missing from
  these sets. Their literal powerset has **128** elements; this is a
  malformed fact carrier, not permission to substitute a cleaned carrier.
* `SC :token-belief-stage :prospective-prior :universe` contains the three
  M-f11 facts. Their role cannot be settled just by filtering target names.

The decisive read-only calculation is reproducible as follows (run in futon2):

```clojure
(require '[clojure.edn :as e] '[clojure.java.io :as io])
(with-open [rd (java.io.PushbackReader.
                (io/reader "data/wm-runs/tick-run-record-2026-09-23-1790131591.edn"))]
  (let [opts {:default tagged-literal :eof ::eof}
        r (e/read opts rd)
        sc (get-in r [:decision :selection-certificate])]
    (assert (= ::eof (e/read opts rd)))
    (doseq [path [[:token-belief-input :carry-admission :current-universe]
                  [:scoring 0 :rates-provenance :model :universe]]]
      (let [u (get-in sc path ::absent)]
        (assert (set? u))
        (prn path {:members (count u)
                   :powerset (bit-shift-left 1 (count u))
                   :non-pairs (vec (remove vector? u))})))))
```

Thus GEN-D 298–318 cannot stand as a measurement of the uniquely consumed
carrier or as grounds for “nothing requires [a sparse lemma] at 64 states.”
It must name the path and retain the disagreement as a carrier finding.

## 3. Concrete falsifiers and the checks that would catch them

I constructed and evaluated this small bad case with `bb`, entirely in memory:

```clojure
{:certificate-schema :wm/proof2-certificate-v1
 :b-update {:posterior-concentrations {:status :missing :reason :not-recorded}
            :prior-concentrations [1/2 1/2]}
 :unrelated [3/2]}
```

A ledger `[{ :observed true }]` yields a recount `[3/2 1/2]`; the corrupt
extractor writes that as `:posterior-concentrations` and claims `:present`.
The same attack applies when the original concentration key is omitted.
The `:unrelated` value makes GEN-D 161–162's proposed number-membership
test pass: every output number already occurs in the record. My evaluated
results were `:all-output-numbers-already-in-record true` and
`:exact-path-value-equality false`. The synthetic root schema marker merely
abbreviates a declared-schema input; placing it at CERT-S's SC location
does not change the attack.

**Which proposed check catches this? None is specified independently of the
extractor's assertion.** R1/R6 (142–159) prohibit it but do not define a
source-to-output comparison algorithm or gate. R2 checks hashes, not origin:
the corrupt extractor can correctly hash the original bytes and its forged
output, and there need be no referenced-content disagreement. R3 is avoided
by using a declared-schema fixture, rather than letting pre-schema status
mask the defect. R4/R5 concern parsing/number representation. Correct Lean
arithmetic on the fabricated vector also cannot establish that it was recorded.
No `checks/proof2*` implementation exists at R; absence of code is expected for
a discovery packet, but absence of an operational detection rule is not enough
to unblock its implementation.

Required amendment: define a verifier that independently reopens the immutable
record identified by its raw hash; resolves every declared source path/reference;
compares exact typed value and presence status with the emitted value; rejects
missing-path-to-present and typed-absence-to-value transitions; and checks
completeness against a fixed clause path schema rather than the extractor's
self-selected path list. Keep generated metadata/proof arithmetic separately
typed. Preregister this absent-to-recount mutation against that verifier before
positive evaluation. This would catch the case; it is **owed**, not a check I
claim to have run in production.

The second bad case at GEN-D 343–348 is concrete too:
`def postConc := accumulate priorConc trialVec h; theorem ok : postConc =
accumulate priorConc trialVec h := rfl`. Section 3's proposed structural
independence check explicitly rejects it. However, “both sides reduce to the
same def” (194–196) needs a precise source/provenance dependency test: valid
literal finite equalities can also be definitionally equal after computation.
Reject computing the recorded operand from the asserted formula, not correct
equalities simply because reduction proves them. The no-sorry and axiom audits
do not detect this circular construction by themselves.

## 4. Complete key-path/schema cross-check

CERT-S references here are sections 1 and 3 (lines 50–81,104–133); SC is as above.

| GEN-D key/path family | Comparison and mismatch |
|---|---|
| `:manifest`, `:extract-schema`, `:record {:path :sha256}`, `:forms {:rule :forms-read}`, `:key-paths` with `:path/:status/:absence-form/:value-ref`, `:referenced-hashes` with `:recomputed/:agrees`, `:identities` with certificate/extractor/source/bundle/baseline identities, `:canonical-extract-sha256`, `:carrier-order` | New **extract-local** fields, not certificate paths; lawful to specify separately. Their presence must not be represented as source-record presence. `:forms-read` and tool metadata also contradict the unqualified every-number rule at 161–162. Define its scope. |
| `:certificate-schema` | Correct certificate declaration is **SC `:certificate-schema`** (CERT-S 17–19), not arbitrary record root. GEN-D 49,149–151 leaves the abbreviated lookup ambiguous; specify it fully. |
| `:content-ref`, `:value-sha256`, `:consumed-at` | Names align (CERT-S 55–59,106–129). A resolved value's hash and the ref-map hash are separate obligations under CERT-S 111–114; GEN-D must preserve both. The consumed-at map requires function/file/line/code-identity and a matching consumer hash, not only copying a pair of field names. |
| `:candidates`, small `<id>` join, candidate order | Align in principle with CERT-S 71,85–99. GEN-D 89–92 should spell out `SC :candidates <i> :id :id` and retain `:candidate-payload-sha256`. Vector order must be preserved. |
| Close `:b-update :prior-concentrations/:posterior-concentrations/:trial-identities/:dedup/:version` | Align with B row (78), although CERT-S says vectors and B-D proposes named maps; see section 5. |
| Close `:b-update :trial-vectors/:normalization` | **Not named as nested keys in CERT-S's B row.** THEOREM P5 requires their meanings; B-D names normalization but not `:trial-vectors`. GEN-D 256–259 silently attributes both names to CERT-S. Propose an explicit shared schema amendment, including shapes and trial ordering. |
| SC `:model-inputs <id> :B`, SC `:B-read :predecessor-chain` | Align with CERT-S B row. Root means certificate root, not run-record root. Preserve read-map `:version/:read-at/:commit-point` and pattern-family attribution. |
| Precedence `:theta/:theta-source :recorded-trials`; `:token-outcome-comparison` | B row supports precedence attribution, but GEN-D 263–268 does not give the full concrete precedence path or the close comparison path. `:token-outcome-comparison` is auxiliary evidence, not a CERT-S replacement for consumed B. Specify source paths and qualify the unverified close example. |
| SC `:token-belief-stage :initialization :value` | Exact match to CERT-S initial-belief row (81); verified above. |
| `:focused 11/20` and `:theta 1/8` (120–122) | Values are cited without exact paths. `:focused` is not a CERT-S term path; the four candidate precedence theta keys on this exemplar are absent. Do not treat either snippet as a consumed-value locator. |
| Held-out `:source {:path :sha256}` (29–31) | External precedent for raw-file identity, not a certificate key-path change. |
| `:extract-refusal` reasons and `:after-byte/:path`, `:negative-invalid :syntax-only` | New diagnostic fields, not certified production values. Keep them separate from preserved source absence maps. |

Canonical-value mismatch: GEN-D 80–85 repeats CERT-S's map/set ordering, but
95–97 converts sets to sorted vectors while claiming one canonical form serves
certificate and extract hashes. A set and a vector have different canonical
EDN bytes even in the same order. Preserve original typed values for their
CERT-S hashes and separately record the emitted enumeration, or amend the
schema explicitly. Also specify UTF-8 for the extract digest (CERT-S 109).

## 5. Sibling contradictions and unresolved joins

1. **B-D carrier names/shapes:** B-D 223–225 requires per-family named maps
   `{achieved r, not r}`; GEN-D 255–259 calls them exact-rational vectors.
   Both cannot specify the same serialized field without a defined encoding.
   B-D 226–238 records ordered trial identities/cells and normalization;
   GEN-D additionally requires `:trial-vectors` but disclaims its shape at
   386–388. That is an incomplete producer/consumer join, not an existing
   exact agreement. Agree one amended schema before B-C/GEN-E.
2. **B normalization indices:** B-D 65–78 fixes division across the two
   outcome cells for a selected family/state (`O=Fin 2`, `S=Fin 1` per
   family). GEN-D AM-1 368–371 proposes `rowTheta a (o : O)` with no state
   argument or finite-outcome sum. For general `O,S`, it does not identify
   the family column needed for `a.conc achieved s / Σ o, a.conc o s`.
   Specify that definition or explicitly restrict to the singleton-state
   specialization. The illustrative `Fin 2 → Fin 6` at 180–182 is not a
   measured family count and cannot select B-D's carrier by itself.
3. **SPEC-N decoding versus AM-2:** SPEC-N 104–118 defines finite binary64
   as an exact dyadic rational, retaining raw sign/encoding identity;
   130–146 requires an independently recorded operand; 150–170 separates
   decoded result from ideal-real refinement. GEN-D 373–381 instead leaves
   `def f_total : ℝ := …` symbolic/undefined. Without a required equality
   to the decoder, these instructions do not determine the same operand.
   Amend GEN-D to use SPEC-N's decoded value and reserve symbolic expressions
   for the ideal formula. SPEC-N 125–128 also explicitly contradicts
   GEN-D 114–118's claim that any decimal printing necessarily loses bits;
   the actual issue is decoding semantics, not decimal typography alone.
4. **CERT-S ordering:** CERT-S 106–114 and GEN-D 80–85 agree on map/set
   ordering. The contradiction arises at GEN-D 95–97's set-to-vector
   transformation under a shared canonical-value claim, as detailed above.
   Candidate/trial vector order is semantic and must not be sorted as a set.

SPEC-N 167–170 permits generating proof endpoints from immutable inputs.
GEN-D's every-number rule (161–162), if applied literally to all extract
fields, prohibits this and even its own form counts. Distinguish production
values from bookkeeping and mathematical witnesses; do not relax the
recorded-value requirement to accommodate either.

## 6. Amendment discipline and final disposition

AM-1 and AM-2 are explicitly labelled amendments. No GEN-D justification
appeals to an operator ruling or to the identity of someone issuing one.
The failures here follow from the pinned definitions and records.
However, the extra B keys/shape and set-to-vector hash semantics are presented
as already following CERT-S rather than as changes requiring an amendment.
Those additions need explicit, versioned schema proposals. Neither the absent
normalization definition nor the undefined hex-real operand can be supplied
by a prose claim that they already exist.

**REJECT until revised:** (1) define the independent path/presence/value
verifier and its absent-to-recount negative; (2) report the six-member and
seven-member universes by exact path, leaving malformed/incompatible carriers
as failures; (3) reconcile B-D/CERT-S serialization and normalization indices;
(4) adopt SPEC-N decoding and separate source-value hashing from enumeration;
(5) correct witness citations and make the emitter independence rule precise.
These are specification corrections, not a demand to implement GEN-E inside
GEN-D. The guard-messages precedent itself passes the source check.

Validation performed: pinned git source reads, packet-only stat check,
raw-record SHA-256, bb EOF/key/carrier inspection, and the in-memory adversarial
number-membership comparison. No claim of executing a nonexistent GEN-D gate
or of re-running the Lean wrapper is made. Only this Markdown review is to be
committed; other lanes' files and all records remain untouched.
