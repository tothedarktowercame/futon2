# LP-A-D — likelihood domains, tempering, and arena absence handling

2026-09-26. Author codex-1; review claude-8. Discovery only.
Read futon2 `e7bb57ea6cdd50054a73a2f8df9362ed0585448e`; Lean read at
mathlib4 `4d565382b2934d6b96f5cfe6a0a5ad9ee668fb9c`.
Clojure paths below are under `src/futon2/aif/`, WM is
`scripts/futon2/report/war_machine.clj`. No registry/runtime edits.

## 1. Tempering and what reaches G

`likelihood_precision.clj:31-102` validates nonnegative stochastic rows,
not strictly positive rows. It admits support-preserving tempering at zero
precision and refuses negative precision. `temper-bernoulli` (120-140)
retains exact rationals for integer nonnegative zeta; `tempered-rates`
(142-182) applies this to false-positive and the complement of false-negative.
These operations do not evaluate ln A. A supplied zero probability is not
by itself missing data.

The premise about all-zero rates needs correction. The guard at
`cascade_model_manifest.clj:828-830` bypasses the tempering call, BUT the
following condition at 840-844 returns `:zeta-with-identity-rates` if zeta is
not 1. It does not score an unmarked unchanged kernel in that case.
Malformed rates likewise bypass tempering only to be refused at 835-837.

Executed probes in a short-lived `clojure -M /tmp/lp-a-probe.clj` JVM used:

```clojure
(require '[futon2.aif.likelihood-precision :as lp]
         '[futon2.aif.cascade-model-manifest :as m]
         '[futon2.aif.belief :as b])
(def base {:q0 {#{} 1} :precedence-fn (constantly []) :horizon 1
           :universe #{:t}
           :spec (m/preference-spec
                  {:want #{:t} :evidence #{} :lam 1 :mu 0 :zeroed #{}})})
(lp/tempered-rates rates zeta)
(m/horizon-g-sparse (assoc base :rates rates :zeta zeta))
```

Rates below are `[false-neg false-pos]` for :t. G numbers are doubles;
effective integer-zeta rates are exact rationals. This fixture's initial
state lacks :t and B is identity, so false-negative does not affect its G.

| Input | Direct tempering / what reaches G | Record/provenance |
| --- | --- | --- |
| `[0 1/5]`, zeta=2 | `[0 1/17]`; G=1.254438158106458 | Certificate identifies nonzero-rate path, zeta=2, tempered=true; consumed A contains effective rates. Zero is retained, no positive-domain refusal. |
| `[1 1/5]`, zeta=2 | `[1 1/17]`; same G in this empty-state fixture | Same marked path. Endpoint 1 remains 1. |
| `[0 0]`, zeta=2 | Direct tempered-rates returns zeros; G refuses `:zeta-with-identity-rates` | Typed G refusal, certificate nil. |
| `[0 0]`, zeta=1 | Untempered identity rates; G=1.3132616875182228 | `:evaluation :identity-A-zero-rates`, `:rates-all-zero true`, `:zeta-status :declared-fixed-vacuous`. |
| `[1/10 1/5]`, zeta=0 | `[1/2 1/2]`; G=0.8132616875182228 | Tempered path marked. Endpoints at zero zeta remain endpoints, by the same support rule (120-140). |
| `[1/10 1/5]`, zeta=-1 | `:missing :negative-zeta`, token :t, branch :false-pos, both direct and through G | Typed G refusal, certificate nil. All-zero rates with negative zeta hit the earlier identity-rates refusal instead. |
| `[1/10 2]`, zeta=2 | Direct: `:row-not-stochastic`; G: `:invalid-adjudication-rate` | Typed refusal rather than scoring. `temper-row [1/2 1/4] 2` separately returns row-not-stochastic, row-sum 3/4. |
| `[1/10 1/5]`, explicit zeta=nil | Direct: `:invalid-zeta`; sparse G substitutes zeta=1 and yields 1.1132616875182229 | Certificate uses raw `(get m :zeta 1)`, so explicit nil remains nil there and tempered? can be true, inconsistent with execution. |

The certificate is not silent about normal paths: manifest 1198-1220 records
`:evaluation`, original `:rates`, `:rates-all-zero`, `:zeta`,
`:zeta-tempered?`, `:zeta-status`; 1148 records effective rates under
`:consumed-g :A`. `horizon-g-sparse-cert:1142-1145` gives nil certificate for
refused G. EFE attaches it to its scored candidate at `efe.clj:1286-1292`;
`g_term_decomposition.clj:105-112` reads consumed A into the recorded term
census (runner persistence at `full_loop_runner.clj:734`). These are scoring
certificate and record paths, not a newly observed live tick in this packet.
No per-zero-cell positivity/absence classification is added by temper-row.
The explicit-nil discrepancy is read directly from 818 versus 1206-1208.

### Authority and absence versus an actual zero

Git identifies the zero-support/zero-zeta declaration with
`fe55a1a0b09a34d4d3c748a391e216b5f7047d86`, author Joseph Corneli,
2026-09-17. Both source docstring (36-42) and commit message declare it;
this is evidence of a committed numerical choice, not an inferred intent.
The all-zero/non-unit refusal is `021f129b48bda1e7dd889a0d031685be5e40f51b`,
author Joseph Corneli, 2026-09-18, citing the zai-55/zai-30 ruling in both
message and source (838-844). Certificate vocabulary is from `e8744c985`
(Joseph Corneli), with the claude-4 2026-09-18 ruling explicitly cited at
manifest 1209-1215 / likelihood_precision 224ff.

Thus a supplied measured zero is NOT an absence converted to a value by
this exponentiation. Nor is the identity path unmarked. Unmeasured rates
may arrive as the declared zero-kernel default upstream; the reader and
rates provenance distinguish that (`observation_label_reader.clj:65-74`,
WM 5943-5949, `observation_rates/sourced-rates:218-260`). That default is a
different question from treating a supplied boundary probability as missing.
Jeffreys on included measured cells avoids exact endpoints, but does not
turn excluded classes into measured ones. The row's positive-domain theorem
and code's support-preserving extension differ; documenting the scope is
necessary, banning actual zeros as “absence” is not justified by this finding.

## 2. No beta-zeta update in code

Searches for beta-zeta, zeta-post, likelihood-precision and precision-update
in aif and WM found no implementation of
`beta_post = beta_prior + sum (o_zeta-o) . ln A s`.
`likelihood_precision.clj:16-23` explicitly declines a zeta prior/update law;
`:prior` and `:update :none` in its declaration (195-205) make the absence
named. Policy precision is a different quantity; adaptive channel precision
(`precision.clj:212-233`) multiplies scalar residuals, not ln A messages.

Lean `Proof2/LikelihoodPrecisionUpdate.lean:135-142` demonstrates zero-log
vanishing and negative-log absolute-value semantics; its guarded version
154-158 refuses negative zeta and nonpositive A. There is no Clojure
betaPosterior site on which to patch those behaviours. LP-A's obligation
here is a `:code` sentence saying update not realised / fixed zeta only,
not a new posterior computation or a guard on an absent computation.

## 3. Arena reachability and consumers

The actual shipped A is dense and positive (`belief.clj:127-143,199-205`),
B is identity (214-226). `update-step:316-323` raises A to
kappa=ln(1+w)/ln2, multiplies the predicted mass, then calls `normalise`.
The latter returns uniform at exactly zero total (51-60).

For the request's w in (0,0.1], kappa is in (0,0.13750352374993502].
The real model probe produced:

| Event | min A | max A | min A^kappa at w=0.1 |
| --- | --- | --- | --- |
| :strengthened | 0.0909090909090909 | 0.2597402597402597 | 0.7191257437234808 |
| :foreclosed | 0.0875 | 0.2597402597402597 | 0.7153562554282686 |

For a valid q and identity B, sum q(s) A(o|s)^kappa is at least the listed
positive minimum. Thus zero posterior mass is unreachable under these
inputs; no missing A lookup occurs in this dense model. This is an algebraic
bound, not merely observing one successful sample. The floor is dormant
on that domain, not evidence of live posterior replacement.

**Weight correction:** WM 7303-7304 bounds the BASE event weight by 0.1;
7317-7332 redistributes it by entity inconsistency with mean preserved.
An individual weight is not necessarily <=0.1; it can be as large as
0.1*n entities. For any finite such w, kappa is finite and the exact product
remains positive. Floating underflow would require implausibly large weights
for this A (roughly kappa>306 at the smallest displayed entry); do not claim
a universal machine-float non-underflow theorem from the base bound alone.

There are nevertheless real API bad cases outside the shipped dense A:

```clojure
(def sparse (into {} (for [o b/status-set] [o {o 1.0}])))
(b/valid-observation-model? sparse) ; true
(b/categorical-filter-step {:addressed 1.0} {:type :strengthened}
                          sparse b/transition-model-v1 {:weight 0.1})
;; uniform over all seven statuses: each 0.14285714285714285
```

The missing [:strengthened :addressed] cell is read as 0.0 in BOTH validator
(`belief.clj:254`) and update (319). This sparse identity matrix passes the
real validator because missing cells have already become zero. A dense
identity matrix with explicit zeros gives the same zero-mass/floor outcome;
explicit impossible evidence and missing likelihood are different causes
which the current API conflates. A direct `update-step` with a fully zero
matrix likewise returned uniform. Shipped A with a cell deleted will
normally fail its column sum instead; it is not correct to say EVERY missing
cell is caught by C9.

C9 is the existing fail-closed model contract in
`holes/missions/M-aif-a-matrix-faithfulness.md:187-190`; source throws at
`belief.clj:339-342`, introduced by `863dea72cfa593dbba22b31f270d65333d4182b7`
(Joseph Corneli, 2026-07-13). This packet does not weaken or remove it.

A prospective honest entity result would be
`{eid {:status :absent :reason :zero-posterior-mass :event ... :model ...}}`,
or `:missing-likelihood-entry` naming [observed status] for an absent cell.
It must remain that absence in `:belief` and trace `:mu-post`
(`trace.clj:444-445`), not be replaced by its previous distribution.
But simply returning that map is NOT a safe small change:

- `entity-expected-health` (belief 625-638), used at WM 7322 on the next
  microstep and by prediction (belief 650-655), casts every map value to a
  double. The real probe with the proposed absence threw ClassCastException.
- `update-belief` (423-432), the next filter application (399-420), and
  other status-distribution consumers require an explicit absence branch;
  missing entity alone currently initialises a uniform prior.
- Arena provenance records mode and model manifest (WM 1049-1050), not an
  update-outcome classification. It cannot represent this occurrence's
  failure just by changing the model hash.

Any absence implementation must therefore change the reader contract too:
retain entity identity with typed absence, omit its unavailable numeric
contribution with an explicit omission receipt, and propagate it into later
filter/carry reads rather than invent health 0, health 1/2, uniform or an old
posterior. No claim that this propagation already exists is made here.

## 4. Recommended implementation split

- **Tempering endpoints / zeta=0 / negative zeta: (a), leave arithmetic.**
  Name fe55a1a0b0 and 021f129b4 and distinguish support-preserving tempering
  from the positive-A beta-update theorem. A zero supplied probability is a
  value. Existing typed refusals and certificate fields already identify
  the paths; no new positive-A gate has an absence-validity case here.
  Regression cases should retain `tempered-rates {:t {:false-neg 0
  :false-pos 1/5}} 2 => [0,1/17]`, negative-zeta refusal, nonstochastic-row
  refusal and all-zero/non-unit refusal through real G.
- **Beta update: (c), registry code sentence only.** No code consumer/law
  exists; inventing one would exceed the packet.
- **Explicit nil zeta: (b), a separate small consistency repair has a case.**
  It currently becomes numeric 1 inside sparse G while the certificate can
  describe nil/tempered. Distinguish an absent key (declared default) from an
  explicitly unavailable value. Return the existing `:invalid-zeta` as G's
  typed result/candidate missing reason, certificate nil, with the runner
  retaining the refusal rather than halting. Test sparse G AND certificate
  against direct tempered-rates for explicit nil versus omitted key. Check
  the ranker/run-record refusal path before implementing; do not just throw.
- **Arena zero/missing likelihood: (b) is justified at the public API but is
  NOT an isolated normalise edit.** The sparse-valid example is an actual
  missing-as-zero witness; the dense impossible-observation example is an
  actual zero-to-uniform witness. Implement only as a separate reader-complete
  packet, changing entity update outcome, health/prediction aggregation and
  carry/provenance together. Tests must use real validator/filter, then feed
  the absent entity through the next reader and trace round trip, asserting
  no uniform, previous-belief substitution or fabricated numeric health.
  Shipped-A healthy output must remain identical. Preserve C9's existing
  malformed-model throw contract; missing-cell classification and its place
  relative to that contract need explicit agreement, not bypassing the check.
  Until that complete packet is scoped, leave the dormant shipped-path floor
  in place and document it, rather than injecting a map that crashes the run.

No epsilon/beta/positive-A receipt is proposed merely to add documentation.
The recommended new typed outcomes have concrete missing-as-value or
wrong-certificate witnesses; the other sites get accurate code provenance.

## Unowned findings, unchanged

The zeta namespace/declaration still says no production caller sources real
rates (lines 13-14 and declaration/gaps), stale since A-LABELS-CONSUME-I.
Explicit nil zeta has the execution/certificate discrepancy above. The
validator's missing-cell default and the per-entity weight bound correction
are additional findings. No claim of a live zero-sum arena event was found
or manufactured. Nothing here changes C9, the runtime, registry or map.
