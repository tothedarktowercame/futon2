# SPEC-F independent review — codex-10

2026-09-24. Author of reviewed packet: codex-13; reviewer: codex-10.
Verdict: **ACCEPT-WITH-AMENDMENTS**, as a specification only, not a proof or
live-supply witness. Required amendments are enumerated in §6.

## 1. Record and scope check

`git show b03017a5 --stat` shows exactly one changed path:
`holes/labs/wm-contract/proof2/packets/SPEC-F.md`, 351 insertions. PASS.
Reviewed that committed packet; inspected SPEC-N at `634877af`. Governing
ASSUME/THEOREM/STRATEGY and CERT-S were read in main at review HEAD
`0ce9ca96a1955f9ebc84be57d9a5f76a4dad978b`.
Only this review is changed by this packet. No build, click, shared-JVM
operation, or data write was performed.

## 2. Independently opened pinned sources and record

Each code check below used `git show REV:PATH` followed by numbered source
inspection, not an inference from the packet's printed line numbers.
Futon2 REV = `93531d415a115c7b426f37e06d7d5542ecb5dae4`;
mathlib4 REV = `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`.

| Checked source | Actual finding | Result |
|---|---|---|
| `src/futon2/aif/policy_prefix_evidence.clj:27–53` | Nonempty steps, distinct occurrence ids, consecutive tau and transition distributions checked; posterior feeds next step, line 49 adds `(:f result)` to total, line 53 emits total. Synthetic fixed-model scope is explicit. | Holds |
| Same file, `:56–70` | `production-ranked` **literally calls `(dissoc :f)` at line 63**, then installs a `:f-prefix` map with `:status :not-supplied`, `:reason :no-admitted-policy-prefix`. | Holds; answers question (b) |
| `scripts/futon2/report/war_machine.clj:6473–6481` | `policy/select-action-cascades` receives `policy-prefix/production-ranked ranked` with conditioning evidence, not the unmodified ranked list. | Holds |
| `src/futon2/aif/cascade_selection.clj:109–129` | Score is log habit minus F minus G/beta; missing F contributes literal 0.0 at line 112; action tie rule is name-ascending. | Holds; missing is not a measured zero |
| `src/futon2/aif/observation_model.clj:301–313` | Weights use observed-event probability; zero sum returns contradiction; otherwise divide by evidence to get posterior and compute surprisal. | Holds |
| `DarkTower/WarMachine/PolicyVariationalFreeEnergy.lean:43–45,159–163,271–274,338–343` | EReal support-sensitive VFE, finite-branch KL decomposition, lower bound and equality characterization all exist with the stated meanings. | Holds; equality theorem starts at 338, not 344 |
| `DarkTower/WarMachine/ExactBeliefTrajectory.lean:47–59,65–73,99–105` | `predictedState`, `observationProbability`, `exactUpdate`, predictive normalization/nonnegativity and `exactUpdate_minimises_vfe` exist. Last theorem is an iff under exactly the per-step normalization/positivity hypotheses used in SPEC-F. | Holds; theorem starts at 99, not 103 |
| `DarkTower/WarMachine/PolicySelection.lean:24–31` | `selectionPosterior t habit F G` takes real F and extended-real G and returns ENNReal; changing F does not erase G. | Holds |

Question (c): yes, the proposed prefix obligations use these existing
declarations. The step hypotheses suffice to invoke the step bound and iff;
the finite sum, equality-of-total/no-cancellation proof, and finite-real
consumed-value bridge remain new obligations. The displayed block is explicitly
a signature specification, not compiled Lean. Historical continuity and
normalized full observation rows remain additional admission obligations; the
algebraic theorem alone does not prove them or smoothed trajectory inference.

Record check used `bb` with `clojure.edn/read-string` and
`{:default tagged-literal}` on
`data/wm-runs/tick-run-record-2026-09-23-1790131591.edn`.
Raw file SHA256:
`7314951f0ad6d339d561a9f7ec4f5dc14042873e4602873dddb590701ba61f25`.
Under `[:decision :selection-certificate]`, the actual extracted values were:

```edn
{:candidates [{:f nil :f-status :not-supplied
               :f-prefix {:status :not-supplied
                          :reason :no-admitted-policy-prefix}}]
 :token-belief-input/observation-updates []
 :token-belief-stage/observation-updates []
 :model-inputs {:status :missing :reason :key-absent}}
```

The slash labels above abbreviate two nested paths, not literal record keys.
The `:model-inputs` map is the review's typed report of an absent key, not a
map present in the record. The prefix map has no `:source` key. The nil F is
reported as found, never converted into zero. This independently confirms the
packet's limited absence finding, not the other two runs or any positive prefix.

## 3. Constructed falsifiers

I instantiated both bad cases using read-only `bb -e` arithmetic and maps.
No production checker or new Lean proof is claimed to exist.

1. S=Unit, O=Bool, identity transition, prior/posterior mass 1, observations
   true at two distinct occurrences, likelihoods 1/2 then 1/4. Complementary
   false masses normalize both rows. Step F values are log 2 and log 4;
   their sum is log 8. Replacing the total with the last step yields a gap
   log 2 > 0. Babashka printed steps `[0.6931471805599453
   1.3862943611198906]`, sum `2.0794415416798357`, substituted total
   `1.3862943611198906`, equality `false`. SPEC-F:138–168's summed theorem
   and consumed-total relation, plus :282–283's accumulation check, catch
   this. With SPEC-N's preregistered numerical budget it must also fail the
   consumed-value refinement, not just a naive double equality. The exact
   symbolic discrepancy above does not rely on those rounded printouts.
2. Two distinct actions, E=[1,1], beta=1, G=[0,2], F=[log 8,0]. Baseline
   scores `[-2.0794415416798357,-2]` select action 2; F-only ablation
   `[0,-2]` selects action 1. The bad extract also zeroes G and gives `[0,0]`.
   Comparing `(dissoc baseline :F)` with `(dissoc bad :F)` in bb returns
   false. SPEC-F:197–217's frozen-non-F rule and :319–328's frozen-G
   predicate reject it even if the tie-break selects action 1. A winner
   inequality alone would not reject it. In the proposed certificate checker,
   this must be equality of recomputed canonical input hashes, with candidate
   id/payload joins, not trust in a hash copied into the diagnostic.

Question (a): yes. SPEC-F:228–231 explicitly compares
`selectionPosterior t E Fprefix G` with `selectionPosterior t E (fun _ => 0) G`.
The same ordinary baseline certificate supplies both arms; :199–207 freezes
all non-F scorer inputs byte-identically and retains input hashes; :262–269
requires id/payload and consumption joins. The declared intervention is only
F-vector := 0; it cannot recompute D/B/G or turn absent F into an admitted
baseline. This is an adequate proposed rule, not implemented enforcement.

## 4. Exhaustive key-path cross-check against CERT-S §§1 and 3

Here SC abbreviates `[:decision :selection-certificate]`. Slash-separated
field lists expand to separate keys at the same parent, not composite keys.

| SPEC-F paths/keys | CERT-S comparison |
|---|---|
| SC `:model-inputs <id> :F`; SC `:candidates i :f`, `:policies i :f`, `:g-term-decomposition :policies i :terms :F` | Exact match to CERT-S:76. Small id + payload hash preserved. |
| SC `:candidates i :f-prefix` | Existing record path, independently verified; CERT-S:76 mentions prefix receipts but does not explicitly enumerate this candidate alias. Add the alias and its join rule in successor schema. |
| SC `:token-belief-input :observation-updates`, `:token-belief-stage :observation-updates` | Stage path explicit at CERT-S:75; input parent explicit at :75/:77 but child not enumerated. These are discovery paths, not silently substituted D or Q witnesses. |
| F `:A/:B/:sPrev/:o/:q` vs F `:f-prefix i :A/:B/:sPrev/:o/:q` | CERT-S:76 lists the former; SPEC-F:286–288 proposes moving authoritative inputs into each step and checked singleton aliases. This is a declared schema refinement, not already-v1-compatible. |
| F `:definition/:aggregation/:length/:units/:z-semantics/:policy/:admission/:f-prefix`; step `:tau/:occurrence-id/:execution-ref/:observation-ref/:prediction/:evidence/:f`; F `:total` | New detailed obligations at SPEC-F:271–283, absent as exact paths in CERT-S. All require successor-schema definitions, not inferred current-record presence. |
| F and step `:numeric-refinement` | New paths; CERT-S has no such key. Also reconcile with SPEC-N's root keyed collection (see §5). |
| `:candidate-payload-sha256`, `:value-sha256`, `:consumed-at` | Names and consumer-locus semantics agree with CERT-S:118–129. Need explicit value projection for scalar total vs structured prefix so matching scalar consumer hashes do not purport to hash the entire input receipt. |
| Absence `:status/:source/:reason`, refusal reason/step, `:zero-support`; diagnostic `:diagnostic-intervention` | Not-supplied map agrees with CERT-S:60–67/:76. The record checked lacks the source key; it is not compliant positive evidence. Zero-support and diagnostic forms are new proposals. Diagnostic baseline ref, frozen hashes, vectors and choices are specified in prose, without exact storage paths; successor schema must assign them. |

All explicit paths and table keys named in SPEC-F are covered above; runtime
parameter keys `:q0/:rates/:observed/:theta` are descriptions of the old
producer API, not proposed certificate paths. The `:f` removal is a source
operation, not an absence encoding. CERT-S §3's canonical hashing and
content-ref resolution apply to new paths too.

## 5. Siblings and governing-text contradictions

- **A12 conflict is real and explicitly amended:** ASSUME:102–109 defines
  one-step VFE and asks for habit-alone comparison; SPEC-F:173–191 requires
  the prefix sum and freezes G. The two definitions cannot both describe a
  general multi-step F or an F-only diagnostic. Adopt the proposed amendment;
  habit-alone removes G and cannot identify F's effect.
- **A17 conflict is real and explicitly amended:** ASSUME:142–148 requires
  both values to be historical and rejects fabricated alternates;
  SPEC-F:199–216 exempts only declared zero-F ablation. Both cannot be applied
  literally to that arm. The exemption must be adopted before L; B's historical
  comparison remains unchanged.
- **W3 conflict is real and explicitly amended:** THEOREM:64 equates consumed
  F to a single-step VFE; SPEC-F:220–234 changes it to a numerically related
  prefix total and an action-level comparison. W3 already retains G in its
  zero-F arm; SPEC-F preserves that part and sharpens marginalization/ties.
- **F sentence:** THEOREM:12–21's code and reduced-law observation agrees
  with SPEC-F:304–312. The sentence's subsequent operator-ruling rationale
  is not used here. The record and score formula themselves establish missing
  supply and the reduced law. No contradiction on those facts.
- **SPEC-N §4.2:** SPEC-N:217–222 and SPEC-F:282–299 agree that every step
  contributes and floating accumulation needs its own bound. No contradiction.
- **CERT-S numeric correction:** SPEC-N:306–315 and SPEC-F:294–302 both
  reject an exact-rational mathematical log total and require versioned symbolic
  expression/refinement. Both conflict with unamended CERT-S:76; both label
  the correction an amendment. No disagreement between the packets.
- **Refinement layout:** SPEC-N:287–298 puts authoritative entries at SC
  `:numeric-refinement` indexed by full path; SPEC-F:282–283 places entries
  under F and its steps. These are not logically incompatible if local entries
  are checked refs, but no rule currently says that. This is an unresolved
  schema ambiguity, not a reason to invent two independent authorities.

## 6. Required amendments and verdict

**ACCEPT-WITH-AMENDMENTS:**

1. In the successor CERT-S schema, make SPEC-F's local `:numeric-refinement`
   entries references to SPEC-N's full-path-keyed collection (or explicitly
   require equality with it). Define authoritative paths for diagnostic receipts
   and the candidate prefix alias. No silently divergent duplicate receipts.
2. Define the hash projections: F's `:value-sha256` joined to consumer totals
   hashes the consumed scalar value in its declared encoding; separately bind
   the ordered prefix/input-expression receipt by a recomputed content hash.
   State where that second binding lives. Otherwise a structured F input map
   and a scalar consumer cannot literally have the same canonical value hash.
3. Adopt the already-proposed A12/A17/W3 and CERT-S value/layout changes
   together before preregistration; retain the absence-vs-ablation distinction.
   These changes are not satisfied by accepting this review.

The packet consistently labels its changes as proposals/amendments (§§3–5),
including its single-step contract and concluding ASSUME restatement changes.
No justification in SPEC-F appeals to an operator ruling or speaker authority.
Its mathematical claims rest on the displayed definitions and pinned code;
its production claims stop at typed absence. The new Lean module, admission
producer, numeric proofs and diagnostic checker remain owed. This verdict
awards no clause standing.

Validation: `git diff --check` and staged whitespace check passed. Only this
Markdown review is staged and committed; unrelated shared-checkout files were
left untouched.
