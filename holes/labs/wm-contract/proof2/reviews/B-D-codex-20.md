# B-D independent review

Seat: codex-20. Model: GPT-6 (no more specific build identity supplied).
Date: 2026-09-24. Author of reviewed packet: kimi-6; reviewer did not author B-D.

**Verdict: REJECT.** Fix the accepted-trial population and commit-point
specification first. The packet describes two gates as though both govern the
population consumed by the judge; they do not. An accepted-close-only carrier
cannot certify the existing reader simply by adding fields. The arithmetic
specialization is sound once its normalization and encoding are defined, but
that does not repair the population mismatch.

## 1. Record and revision check

`git show 64b091ce --stat` names exactly one changed file:
`holes/labs/wm-contract/proof2/packets/B-D.md`, 304 insertions. PASS.
All B-D line references below refer to that commit, not the moving checkout.
Code was opened with `git show 93531d41:<path>` and numbered independently.
Lean was opened at mathlib4 `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`
(the A19 pin; B-D itself omits a Lean revision). Sibling revisions inspected:
GEN-D `c7fe3001`, SPEC-N `634877af`. CERT-S was read including its correction,
last changed at `9250eaf12530f2ac7c7a25dac1ff628329042a80`.
ASSUME A1–A21, THEOREM W/P/X and review amendments, and STRATEGY's packet
contract/B rows supplied the review obligations, not evidence of execution.

No code/data changes, clicks, shared-JVM evaluations, or Lean builds were made.

## 2. Independently opened code and Lean citations

Paths below are futon2-relative except the last two.

| Opened citation at the pins above | Finding |
|---|---|
| `src/futon2/aif/learning_trial_ledger.clj:24–69`, `record!` | HOLDS: admission is `:admitted-at-attempt-grain`; append dedup uses identity, meaning and observation, in that priority order. Lines 61–63 append and sync before the close-acceptance check. |
| Same file, `producer-of`/`theta-key`, 75–114 | HOLDS: declaring pattern id is the parameter key, distinct from configuration `:family`; none/multiple producers give typed attribution results. |
| Same file, `read-trials`, 128–152 | HOLDS for shape, NOT an accepted-close filter: maps all parsed rows; no identity collapse or accepted-close lookup occurs here. `:observed` uses the top-level value when present, otherwise the increment. Several B-D line anchors point to earlier layouts. |
| Same file, `b-update`, 173–236 | HOLDS narrowly: accepted verdict required; filter by theta-key at 186; identity collapse at 190; occurrence comparison at 200–203. The argument `:occurrence-identity` is provenance, not the dedup key. This function computes a result and does not persist a new version. |
| Same file, `pattern-theta`, 262–291 | HOLDS: independent read-side identity collapse at 275, exact ratio at 280. It reads ALL attributable ledger rows, not only rows of accepted closes. It returns sorted identities at 285, not append order. |
| `scripts/futon2/report/war_machine.clj:6375–6415` | HOLDS: call at 6383, theta stamping at 6395–6406. No identity collapse occurs in this judge; it delegates to `pattern-theta`. Separate reads occur for pattern occurrences, so the resulting family map need not be one atomic ledger snapshot. An empty candidate field performs zero reads. |
| `src/futon2/aif/full_loop_runner.clj:3130–3150`, 4190–4245 | REFUTES B-D 172–174 as a commit-point description. `record!` is called at 3149 during evidence retention. Only the later `b-update` call is conditional on the written accepted close, 4208–4230. That call cannot retroactively restrict rows already visible to the judge. |
| `src/futon2/aif/cascade_model_manifest.clj:282–290` | HOLDS: an existing theta is preserved. B-D's 271–280 citation actually opens observation prediction at this pin. |
| `mathlib4/DarkTower/WarMachine/DirichletLearning.lean:19–21`, 50–66, 82–93, 98–133 | HOLDS: positive REAL concentrations, outer-product step, accumulation, append and one-hot lemmas. B-D's cited line numbers drift; the declarations themselves exist. |
| Same Lean file, 139–157, 181–185 | `total` and normalized-trial total growth exist. No `rowTheta` or concentration-to-theta definition exists. The normalized-cell theorem therefore requires an explicit definition/specialization; it is not already a named correspondence. |

### Answers to the three specific questions

(a) The mechanisms are distinct: append admission/dedup, post-close occurrence
recognition, and counting-time identity collapse. The last mechanism exists in
both `b-update` and `pattern-theta`, not in `read-trials` or the judge itself.
They are not three independent protections for every call: an occurrence
missing from the ledger is not remembered by the non-writing `b-update`.
Two distinct effect identities from one occurrence also survive read-side
identity collapse. State whether the trial grain is occurrence/effect or just
occurrence; do not call both exactly-once over the same grain.

(b) The arithmetic reduces to `accumulate_conc`/`accumulate_onehot` plus the
normalization definition. For each family separately, take `O = Fin 2`,
`S = Fin 1`, prior 1/2 in both cells, and n normalized one-hot pairs with s
successes. The concentrations are `(s+1/2, n-s+1/2)`; their sum is `n+1 > 0`.
Define theta as `a.conc achieved 0 / sum_o a.conc o 0`; then it is
`(s+1/2)/(n+1)`. Lean's array is over reals, so rational record values need an
exact rational-to-real interpretation and the corresponding equality proof.
No floating tolerance is needed. A generic array with multiple family-state
cells must normalize at a fixed state, not by the total of the entire array.
The packet alternates between “one state per family” and a separate singleton
array per family; the latter is the stated specialization and must be explicit.

(c) Confirmed in the bounded corpus, not “any record” universally: recursive
EDN inspection with `bb` of all 14 `007-closed.edn` files (attempts 001/002 in
machinery-70 through 76) found no keyword `:B-read`, `:read-at`, `:b-update`,
or exact `:version` anywhere. All 14 do have `:event/schema-version 1`.
Thus absence of a B version holds; absence of every kind of version key is
false. The trace `2026-09-23-1790199409` also lacks certificate `:B-read` and
`:model-inputs`; its C1 precedence carries 3/4 and 1/4, and C2's final pattern
carries 1/16. Absence here is a finding, not a value supplied to any witness.

## 3. Actual record-key checks with bb

Executed read-only `bb /dev/stdin` with `clojure.edn/read-string` and
`{:default tagged-literal}` for closes; the ledger was read using
`PushbackReader` and `edn/read` repeatedly through EOF. The close survey
recursed through maps and sequential values rather than inspecting only roots.
These specific paths were checked in machinery-71/attempt-002:

- `[:payload :judgment :accepted-increment :accepted?]` = `:refused`;
  `:reason` = `:predicate-evaluation-failed`.
- `[:payload :judgment :token-outcome-comparison :learning-trial-receipt
  :trials 0 :status]` = `:admitted-at-attempt-grain`.
- At that same trial: `:counted? true`, `:after-observation true`, and
  `[:ledger :status] = :appended`.
- `[:ledger :identity]` =
  `8e7d1aaf32d9ef2ead1702b304887c414298ce7ea5b48b0e51e42995963e1403`.
  The ledger's third form has that top-level `:identity`, `:observed true`,
  and occurrence action `action-88c8dab8-7052-4f3c-a5f0-0d461fd4fe3e`, matching
  the close's occurrence. Its declared producer is
  `:apparatus/done-is-observed-running`.

Ledger census: 12 forms. Raw SHA-256:
`854011186efca6790b838ea1b6bcae79eda4335fe692141842a97269b5576854`.
Close path:
`data/wm-full-loop-machinery-71/wm-contract-machinery-71-v1/attempt-002/007-closed.edn`;
raw SHA-256:
`ea8159c858ff23ebfeac029aab2f71d34b20465c3bff54d02d9c4d61d83a149f`.
These are negative diagnostic evidence, not positive PROOF-2 standing.

## 4. Concrete falsifiers and the proposed checks

1. **Packet's token-posterior impostor:** for the retained identity above,
   attach `:state-belief {["M-f11-find-production-successor"
   :restoration-accepted] 1}` and label it the trial's state posterior.
   B-D 250–257 says B-C must refuse it, and the declared Fin 1 carrier would
   reject the key if a checker actually checks it. But B-D §4 has NO
   `:trial-vectors` carrier or vector/carrier validation predicate. An emitter
   that drops the offending vector can still satisfy §4. No specified §4
   field check catches that loss. Require recorded vector pairs, carrier ids,
   normalized singleton validation, and joins to the source identity; an
   absent vector must remain a typed absence.
2. **Duplicate success:** prior `(1/2,1/2)`, one success identity i yields
   `(3/2,1/2)` and 3/4; feeding `[i,i]` directly to `accumulate` yields
   `(5/2,1/2)` and 5/6. Exact bb ratio evaluation confirmed these numbers.
   Append `:duplicate-replay` and the two identity-map collapses named in §2
   catch an identical replay on their respective paths. Lean accumulation
   itself does not. §4's `:dedup` enum has no read-side disposition despite
   §2/§7 requiring that layer. Add it. Conflicting rows sharing i are also
   worth a negative: read-side `into {}` is last-wins, not conflict rejection;
   byte-identical duplicate invariance must not be overclaimed as protection
   against arbitrary conflicting ledger content.
3. **Real eligibility counterexample:** use machinery-71's admitted ledger
   row together with its refused close, above. `pattern-theta` includes the
   row without testing the close. Neither §2's later `b-update` guard nor
   §4's absent-update receipt catches consumption of it. B-D must define
   accepted measurement versus accepted increment, specify the actual
   population, and amend A13/W5 if those are different notions. Excluding
   unsuccessful closes silently would change the estimator, not certify it.
4. **Singleton corruption:** set the single state mass to 2 instead of 1.
   `TrialNonneg` still holds, and one success gives `(5/2,1/2)`. B-D 299–301
   calls this test inapplicable, but a serialized singleton carrier can have
   a wrong mass. Require normalization/one-hot validation (or a typed encoder
   that makes invalid mass unrepresentable and is itself checked); do not
   remove the negative without specifying that check.

## 5. Every named key group against CERT-S §§1 and 3

`SC` below means `[:decision :selection-certificate]`.

| B-D key/path group | Cross-check |
|---|---|
| `[:b-update]` on producing close | Matches CERT-S's proposed logical close root. Actual checkpoint judgments live under `[:payload :judgment]`; neither packet defines whether the new key is at file root or judgment root. Amend to an exact physical path before B-C/GEN-E implementation. It is absent at both today. |
| `:prior-concentrations`, `:posterior-concentrations` | B-D 223–225 prescribes per-family maps; CERT-S's B row prescribes vectors. Select one ordered carrier with explicit family/outcome axes by amendment, not an assertion of agreement. |
| `:trial-identities`, each `:observed`/`:theta-key` | CERT-S requires content refs. A dedup identity digest is not automatically a content hash of the ledger row. Specify the referenced object and exact `{:content-ref "sha256:..."}` encoding; added attributes need their own location. B-D's append order differs from today's sorted `pattern-theta :identities`. |
| `:dedup`, `:status`, `:layer`, held reasons | CERT-S leaves receipt internals open. B-D's three-layer requirement is not implemented by its own enum: read-side collapse is missing. Distinguish genuine `:none` from missing evidence. |
| `:version` | B-D supplies hash inputs but omits an explicit invocation of CERT-S §3 canonical EDN/UTF-8/SHA-256 and reference resolution. Version hash is not a substitute for each term's `:value-sha256`. Include family/carrier meaning in the hashed value so equal counts for different parameters do not name an indistinguishable B. |
| `:normalization`, `:conc-achieved`, `:conc-not`, theta | Necessary under P5, but these field names and the recipe are not defined in CERT-S's B row. Declare a schema amendment/addition, including output theta and declaration identity. |
| `SC :B-read :predecessor-chain`, entries `:version/:read-at/:commit-point`; `SC :model-inputs <id> :B` | Paths agree. B-D's additional phrase “root B-read with version/read-at/commit-point” needs to distinguish current read from chain entries. Small candidate-id plus payload join must follow CERT-S. A per-pattern loop is not an unconditional root read on an empty/incompatible field. |
| `:theta-source`, `:theta-provenance`, `:targets`, `:unattributed-rows` | Existing pattern provenance, compatible additions; do not replace CERT-S §3's `:value-sha256` and `:consumed-at` producer/consumer match. B-D §4 does not enumerate those required hashes/loci. |
| `{:status :missing :reason :no-eligible-outcome}` | Matches CERT-S's absence form, but the population dispute makes B-D's trigger unestablished. Unavailable read must also preserve CERT-S's typed `:defaulted/:ledger-read-failed` evidence rather than be recast as a posterior. |
| `:schema/:mode/:identity/:family/:meaning-sha256/:observed/:increment/:contract/:trial`; `:trial-grain/:does-not-establish`; `:theta-key/:produces/:id`; `[:trial :deduplication :inputs :occurrence]`; accepted-verdict `[:evidence :acceptance-token]`; `:recorded-at`; `:status/:theta/:rule/:trials-read/:read-back` | These are existing source/provenance keys, not CERT-S term paths. No schema conflict merely from naming them; source refs must name the actual envelope and file. None alone establishes B version or consumption. |

Missing from B-D §4: explicit `:trial-vectors` (P5/GEN-D), §3 term hashes and
consumption loci, the concentration carrier's axis ordering, and a coherent
physical close path. Existing absence must never be filled by a later recount.

## 6. Sibling contradictions and amendment discipline

- **GEN-D 258–261 vs B-D 220–241:** GEN-E requires `:trial-vectors` and
  concentration vectors. B-D's purported complete B-C carrier specifies only
  trial identities/attributes and concentration maps. Implementing only that
  contract cannot supply GEN-E's input. Amend both contracts to one explicit
  vector encoding; do not generate missing vectors retrospectively.
- **GEN-D AM-1, 362–372 vs B-D 285–292:** GEN-D calls for normalization of
  row `o` with a generic `rowTheta (a : DirichletParams O S) (o : O)`.
  B-D's quantity normalizes outcomes at fixed singleton state. Literal
  normalization across the state entries of row o would be 1 for S=Fin 1,
  not 3/4 after one success. GEN-D leaves the formula undefined, so this is
  an unresolved axis ambiguity that becomes a contradiction under its literal
  row reading. Amend AM-1 to the fixed-state outcome normalization, or an
  explicitly specialized two-outcome/singleton function, then prove it.
- **SPEC-N W5 row 79 and 89–93 vs B-D 69–77, 269–273:** no contradiction
  for the rational arithmetic sub-witness: SPEC-N explicitly separates it
  from trial interpretation and the generally transcendental old/new action
  comparison. B-D's “no bridge” must stay limited to exact concentration and
  theta arithmetic. Neither packet discharges semantic attribution or full W5.

B-D §7 explicitly labels its three theorem changes as proposed amendments:
PASS on labeling. Its §4 map-versus-vector change, implicit omission of
trial vectors, and new normalization shape are not identified as CERT-S
amendments: FAIL on completeness. Extend the amendment list. Also amend the
A13 interpretation if “accepted trial” means admitted measurement rather
than accepted close; the two predicates demonstrably differ.

I found no operator ruling used as a justification in B-D itself. It quotes
code and a prior theorem correction. The latter's “carrier only” assertion
(B-D 243–246) is not proof and is refuted by the population mismatch. Names
inside quoted code comments establish no mathematical or empirical premise
in this review.

## 7. Required revision before dispatching B-C/B-N from this packet

1. Specify the population actually consumed, its visibility/commit point,
   and the distinction between eligible measurements and accepted increments.
   Name the theorem amendment or implementation gap; retain failed/missing
   cases honestly. A close-only version cannot describe pre-close reads.
2. Reconcile the exact B-C/GEN-E/CERT-S carrier, including contemporaneous
   trial vectors, hash rules, axes, physical paths, and all dedup dispositions.
3. Specify normalization and the exact rational-to-real interpretation;
   align GEN-D AM-1 and keep the singleton corruption check.
4. Correct source anchors, pin Lean, and qualify the absence and exactly-once
   claims to the checked records and identity grains.

These are specification corrections, not authorization for runtime changes.
