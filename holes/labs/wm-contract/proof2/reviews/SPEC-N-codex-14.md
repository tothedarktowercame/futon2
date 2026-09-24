# SPEC-N independent review — codex-14

2026-09-24. Reviewer: codex-14, Codex / GPT-6 (session model identification;
no more specific build identifier is available). Packet author: codex-12.
This is an independent specification review, not a compiled negative witness,
implementation acceptance, full A20 sign-off, or clause standing.

**Verdict: ACCEPT-WITH-AMENDMENTS.** The numerical distinction and both
falsifier designs are sound within their stated scope. Before the successor
schema/emitter is frozen, make the three interface amendments below. None
requires changing the mathematics of the proposed bounds.

1. Qualify SPEC-N lines 266–268: a strict policy-score gap must be a **proved
   gap between ideal scores**, for example lower(winner) > upper(other), not
   merely a positive gap between recorded scores. Injectivity alone does not
   make the latter sufficient. Lines 379–388 already supply its counterexample.
2. Specify the v2 refinement-entry grammar and hash subjects: the F container
   `SC :model-inputs <id> :F` is not itself the scalar total. Name its scalar
   `:total` value path, the separate symbolic expression/input paths, and the
   candidate-id/payload-hash joins for every alias. Make SPEC-F's nested
   `:numeric-refinement` fields references to the authoritative path-keyed
   collection, or define checked equality between the copies. For an action
   marginal, record all contributing candidate joins, not one arbitrarily
   selected owner. Hashes must be recomputed using CERT-S §3, with value and
   proof/metadata objects explicitly separated; an entry must not hash itself.
3. Resolve GEN-D AM-2's `def f_total : ℝ := …` by defining the machine value as
   the cast of `DecodeExact` of its recorded encoding, retaining that encoding
   for identity. Give the ideal F expression a separate name and prove the
   refinement relation. An unspecified symbolic real is not a decoder.

## 1. Record and revision check

Ran in `/home/joe/code/futon2`:

```sh
git show 634877af --stat --oneline
git branch --show-current
```

Result: branch `main`; commit
`634877af239fd4b5458c179909450a0a1c25a414` changes only
`holes/labs/wm-contract/proof2/packets/SPEC-N.md` (413 insertions).
Reviewed SPEC-F from `git show b03017a5:holes/labs/wm-contract/proof2/packets/SPEC-F.md`
and GEN-D from the corresponding path at `c7fe3001`. Governing ASSUME,
THEOREM, STRATEGY and CERT-S were read in the checkout at
`15ecfb2d229acdca727a41929d44e54972e917b6`; their later amendment register
is not evidence for the mathematical conclusions here. All line references
below refer to those versions or the explicitly pinned packet/source.

No shared source, data, live JVM, or runtime was changed. No click or Lean build
was run. Other lanes' untracked files were left untouched.

## 2. Independently opened source and record evidence

Source inspection used `git show REV:PATH` (via Python subprocess), with
numbered output from the returned source, not the packet's line numbers as
proof. R = `93531d415a115c7b426f37e06d7d5542ecb5dae4` in futon2;
L = `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3` in mathlib4.

| Pinned file and independently checked lines | Result |
|---|---|
| L `Mathlib/Analysis/Complex/Exponential.lean`, 513–525 | Holds: `Real.exp_bound` requires `abs x ≤ 1`, `0 < n`; error is `abs x ^ n * (n.succ / (n.factorial * n))`. At n=24 this is exactly the packet's R24. |
| L `Mathlib/Analysis/SpecialFunctions/Log/Basic.lean`, 160–166 | Holds: `log_lt_iff_lt_exp` and `lt_log_iff_exp_lt` have the needed directions and positive argument hypothesis. Argument 2 satisfies it. |
| L `DarkTower/WarMachine/ActionMarginal.lean`, 21–27, 60–90 | Holds: action mass sums all owning policies; `IsBayesAction` allows ties; `bayesAction_of_injective` requires injectivity and an actual policy maximum, not a rounded-score claim. |
| R `src/futon2/aif/cascade_selection.clj`, 109–118, 147–161 | Holds: score uses double log habit, missing-F zero branch and `G/beta`; posterior uses exp; action mass is accumulated from 0.0, sorted by action string and compared strictly. Ideal reciprocal multiplication cannot silently replace this floating division. |
| R `src/futon2/aif/cascade_free_energy.clj`, 90–103 | Holds: rational rollout evidence is accumulated, zero yields infinity, otherwise evidence is cast before `Math/log`. This supplies neither a full observed-prefix VFE witness nor exact log equality. |
| R `src/futon2/aif/action_identity.clj`, 14–63 | Holds: finite doubles use `Double/toHexString`, fixed printer flags are explicit, and the digest hashes a tagged canonical tree. That codec is not CERT-S's tagged-EDN codec. |

Reproducible record query actually run with **bb**:

```clojure
(require '[clojure.edn :as edn])
(let [r (edn/read-string
          (slurp "data/wm-runs/tick-run-record-2026-09-23-1790199409.edn"))
      sc (get-in r [:decision :selection-certificate])]
  (prn {:schema (get sc :certificate-schema :review/absent)
        :model-inputs (get sc :model-inputs :review/absent)
        :candidates
        (mapv (fn [c] {:id (get-in c [:id :id]) :g (:g c)
                       :f (:f c) :f-status (:f-status c)
                       :theta (mapv :theta (get-in c [:id :precedence]))})
              (:candidates sc))
        :posterior
        (mapv (fn [[k v]] {:id (:id k)
                           :hex (Double/toHexString (double v))})
              (get-in r [:decision :selection-law :posterior]))}))
```

Result: schema and model-inputs are `:review/absent` (a query sentinel, not
invented record content). C1 theta vector `[3/4 1/4]`; C2 has `1/16` at
precedence index 3. G values are `0.7324151971422708` and
`1.9138870492238644`. Both F values are nil with **recorded**
`:f-status :not-supplied`; neither is zero or a positive F witness.
The posterior is keyed by payload maps with ids C1/C2 and has the two hex
values SPEC-N lists. Exact Python `Fraction.from_float(float.fromhex(h))`
addition gives residual `7/36028797018963968` above one.

Ran `sha256sum` on both cited records. Results match SPEC-N lines 25–26:
`7314951f0ad6d339d561a9f7ec4f5dc14042873e4602873dddb590701ba61f25`
and `7b3c56df1633bbdf1a0e32527f5778bbe45f2f883b6bb439647c693b813bd957`.

## 3. Concrete bad cases and checks that catch them

### Rounded log asserted equal to its ideal value

Construct the bad claim `d = Real.log 2`, with d decoded from
`0x1.62e42fefa39efp-1`. Independently ran this exact rational computation:

```python
from fractions import Fraction as F
from math import factorial
l = F(69314718055994530, 10**17)
u = F(69314718055994532, 10**17)
d = F(6243314768165359, 9007199254740992)
def p(x):
    return sum((x**m / factorial(m) for m in range(24)), F(0))
def r(x):
    return abs(x)**24 * F(25, factorial(24)*24)
assert F.from_float(float.fromhex('0x1.62e42fefa39efp-1')) == d
assert all(v > F(1, 10**17)
           for v in [l-d, 2-p(l)-r(l), p(u)-r(u)-2])
```

All checks hold. The three positive margins are approximately
`1.3773236017004819e-17`, `1.883446424291314e-17`, and
`2.1165535756578862e-17`; the conclusions above use fractions, not those displays.
Since 0 < l < u < 1 and n=24>0, the checked Lean theorem gives
`exp l ≤ P24(l)+R24(l) < 2` and
`2 < P24(u)-R24(u) ≤ exp u`. The checked strict log iff lemmas therefore
prove **d < l < log 2 < u**. The falsifier design is sound.

Exactly what rejects: a claimed literal equality cannot coexist with this
strict inequality. If the equality branch is represented as refinement with
zero error, SPEC-N lines 155–156 demand `u ≤ d`, which fails (and no sound
interval containing log 2 can satisfy zero error around d). A nonzero budget
may correctly accept the approximation; that must not be labelled equality.
NUM-T-L still owes the compiled negative witness: the reviewed packet supplies
no executable checker whose observed refusal this review can claim.

### Near-tie with a strict recorded-score gap

Use the packet's dyadic recorded scores `za=-4093/4096`, `zb=-4095/4096`,
each with error `2/4096`. The raw strict gap is `2/4096 > 0`.
Permitted ideal values `za*=-4095/4096`, `zb*=-4093/4096` reverse it.
The score intervals are respectively `[-4095,-4091]/4096` and
`[-4097,-4093]/4096`. They overlap; lower(a) < upper(b).

This **does defeat a checker using only the strict recorded policy-score gap**,
even with an injective action map. It does **not** defeat a checker proving a
strict ideal-score gap from certified bounds. Nor can it pass SPEC-N's proposed
`U_b < L_recordedAction` at lines 256–262: sound exponential bounds must admit
the reversed ideal values. That exact check catches it; name-order tie handling
cannot rescue the failed proof. This is the reason for amendment 1.

Scope: this constructs the numerical stability counterexample, not a complete
live prefix-F provenance record. Positive F alone does not prove its origin as
VFE. That broader provenance is deliberately not claimed by this specification.

An additional concrete aggregation check: three policies with E=(3/7,2/7,2/7),
F=G=0, map the first to action a and the other two to b. The strict ideal policy
winner has mass 3/7, but action b has 4/7. A policy-only checker fails here even
without rounding; the packet's all-policy aggregation catches it.

## 4. Exhaustive named-path comparison against CERT-S §§1 and 3

Here SC has the packet's full prefix `[:decision :selection-certificate]`.
Generic subterm names below expand under the containing paths, not at a new
root. Historical vector positions are addresses only, never candidate identity.

| SPEC-N paths/fields (lines) | CERT-S comparison and disposition |
|---|---|
| SC `:certificate-schema`, `:model-inputs` (28–32) | Schema declaration specified in §0; model-inputs children in §1 rows A/D/F/B. Historical absence correctly reported. |
| SC `:candidates i :id :precedence j :theta`, sibling `:theta-source`; `:id :id` (43–46, 74, 79) | §1 field/B rows, lines 71/78, support these candidate/pattern joins. Not a replacement for concentration/provenance records. |
| SC `:candidates i :g/:f/:f-status/:habit` (47–59, 77/80) | F/habit match lines 76/79; G is the existing candidate score under line 71's candidate maps, not a newly specified model-inputs G root. Legacy nil is diagnostic only; v2 must retain typed absence. |
| SC `:candidate-derivations <id>` and normalized slots/emitted rows (74) | Matches line 72. |
| SC `:model-inputs <id> :A`; decomposition `:g-term-decomposition :policies i :terms :A`; predicted-outcome rows (75) | Matches line 74. “Predicted-outcome rows” names a required value, not an exact schema subpath: final emitter schema must name it. |
| SC `:model-inputs <id> :D` with `:sPrev/:o/:A/:B/:q`; token-belief input/stage (76) | Matches lines 75/81. D is field-level and must agree across candidates. |
| SC `:model-inputs <id> :F`, `:f-prefix`; candidates/policies `i :f`; decomposition `:terms :F` (77, 299–301) | Root paths match line 76. F container versus scalar-total address is underspecified; amendment 2. **Value-form conflict:** CERT-S calls total exact-rational; SPEC-N explicitly amends it. |
| SC `:Q-link`, prior/posterior hashes, joined D (78) | Matches line 77's temporal joins, not a candidate bijection. |
| Close `[:b-update]`, SC `:model-inputs <id> :B`, SC `:B-read :predecessor-chain` (79) | Matches line 78, reading “root” under table's SC default and CERT-S §0's explicit anchor; no move to run-record root. |
| SC `:scoring i :c`; candidate habit; model inputs/decomposition (80) | Matches lines 71/79 and term-specific rows. |
| `[:decision :selection-law]` beta/gamma/posterior/action-marginal (50–58, 77/80, 302–304) | Matches intended selection-law location. **Existing schema mismatch:** line 73 says posterior id→probability; record and SPEC-N use payload→probability. Line 73's abbreviated marginal/posterior roots are also ambiguous under its SC table default. v2 must spell full paths and key forms explicitly. |
| SC `:numeric-refinement` keyed by full value path; per-entry id/payload hash, source value hash, consumed-at, expression/leaf/proof identities (287–304) | **New path**, explicitly an amendment, not an existing §1 path. Compatible extension in principle; exact entry field names and references remain to be specified. CERT-S §3's recomputed value/payload hashes and consumption identity still apply. |
| `:candidate-payload-sha256`, `:value-sha256`, `:consumed-at`, content hashes/refs (31–32, 287–304) | Compatible with §1 joins and §3 lines 106–121. Full paths locate values; they do not authenticate candidate identity. No permission to use action_identity's different hash codec (SPEC-N 120–124). |

No other literal record root is introduced. `DecodeExact`, `RefinesWithin`,
encoding/proof identities and error budgets are proposed semantic/schema fields,
not claims that these keys already exist in the historical records.

**Answer to question (c): compatible, with an explicit v2 extension.** A map
keyed by value path may contain several entries for the same candidate. It is
not a seventh candidate section required to have exactly one row per id. Each
candidate-bearing entry must resolve to the existing six-section bijection and
payload hash; global/temporal entries must explicitly identify their different
scope. Substituting a C2 payload hash into a C1 F entry must fail CERT-S's
recomputed id/payload join even if every interval proof remains numerically valid.
The packet requires those joins; amendment 2 makes their serialization unambiguous.

## 5. Sibling contradictions and unresolved interfaces

- **CERT-S versus both numeric packets:** CERT-S line 76's exact-rational
  mathematical F total cannot generally coexist with SPEC-N 305–313's symbolic
  logarithmic total, or SPEC-F 295–303's exact symbolic Real/EReal sum. This is
  a genuine definition conflict in unamended v1; both packets explicitly propose
  the same amendment and a successor version. Rational inputs/endpoints do not
  cure irrational summands.
- **SPEC-F versus SPEC-N aggregation:** SPEC-F 15–32 requires the whole ordered
  prefix sum, with EReal support handling. SPEC-N 77, 206–233 and 401–402 require
  per-step bounds and total refinement, separately typing mathematical top
  (116–118). No contradiction: finite refinement is not an assertion that every
  EReal value is finite. Both reject treating the last summand as the total.
- **SPEC-F versus SPEC-N storage:** SPEC-F 282–283 describes per-step and total
  `:numeric-refinement`; SPEC-N 287–304 describes a top-level path-keyed
  collection. These can both be followed using checked references, so are not
  inherently contradictory. Their alias/reference relationship is missing;
  amendment 2 supplies the required interface rather than silently choosing
  one independent source of numerical truth.
- **GEN-D AM-2 versus SPEC-N §3:** GEN-D 373–382 leaves a named real's definition
  as an ellipsis; SPEC-N 104–118 gives exact binary64 decoding. An unconstrained
  symbolic real would violate that definition, but GEN-D does not actually
  require it to be unconstrained. There is under-specification, not an unavoidable
  contradiction: amendment 3 instantiates the ellipsis without changing the
  bit-preserving record or pretending the ideal expression equals the double.
- **F ablation:** SPEC-F 176–219/223–235 and SPEC-N 401–405 agree to retain G,
  explicitly amend A12/A17, and check both arms' action choice. No sibling
  conflict. Neither silently treats F=0 as historically observed.

## 6. Amendment and authority audit; limits of acceptance

SPEC-N 18–19 declares amendments rather than silent reinterpretation;
285–314 explicitly changes CERT-S with v2, and 394–413 enumerates theorem and
assumption amendments. Decode/refinement, prefix sum, F-only intervention,
strict stability, exact ties, unresolved status, top/overflow and normalization
residuals are all explicitly proposed changes. Rational-subterm equalities and
A16's consumed-value/provenance obligation are preserved. I found no
justification in SPEC-N that rests on an operator ruling or who said it.

Acceptance is of this specification with the three listed amendments, not of a
working decoder, generated Lean proof, live certificate, full theorem or completed
A18 CHECK. NUM-R/L, NUM-T-L, composition, NUM-S-L and extraction still owe code,
compiled proofs and observed negative-case rejection. The absent F/model-input
carriers remain absent; no offline numerical calculation supplies them.
