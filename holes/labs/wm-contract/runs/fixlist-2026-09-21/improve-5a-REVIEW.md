# Improve-5a — record-only parameter novelty

Branch `fix/narrative-improve-5a`, base `4c01c19e`. No merge, click, serving-JVM
load, production-model update, or learning-store write.

Stable key: `[:decision :selection-certificate :parameter-novelty]`, a vector
with one `:wm/parameter-novelty-v1` receipt per full policy identity. The normal
run-record and selection checkpoint retain the certificate unchanged. Each
receipt carries eligible distinct endpoints; target/cascade/ordered-patterns/
effect/route family and digest; prior kind and separate learned counts;
ledger snapshot path/SHA256 (or typed absence); declared observation contract
value/version/digest; prospective observation model; τ=T, multiplicity=1,
attempt grain and nats; factorization declaration or typed absence; expected
KL and explicit `:no-focus-declared`. There is no shadow κ. Existing scorer
risk/ambiguity remain under `:serving-risk` / `:serving-ambiguity`, with G under
`:serving-G`. `:terms :theta-latent` records the terminal attempt model's
marginal predictive risk, conditional ambiguity, pragmatic cost, parameter
information gain and EFE, with a numerical identity check. These are separate
from the serving score and do not change its accounting in this slice.
No re-scan, re-rollout or score change is performed to attach the receipt.

`parameter-novelty/read-inputs` reads the contract's absolute ledger path once
at the joint selection boundary. It reads a byte snapshot, hashes it, parses
all forms, and refuses malformed/truncated/duplicate ledgers. It never creates
or repairs files. A missing ledger permits a distinctly illustrative prior;
a bad ledger does not masquerade as an empty one. Counts join on the exact
family digest, contract, and supplied meaning digest. The prior is explicitly
an illustrative adaptation to attempt endpoints, not a claim that per-firing
counts parameterize attempt delivery. Matching counts remain separately named
`:illustrative-plus-learned-counts`; they never enter B or G.

## Missing production bindings are visible

The existing attempt contract establishes admission and timing, not a
prospective per-policy route or perfect endpoint likelihood. Serving currently
supplies neither; receipts therefore report `:route-unavailable`, with the
observation model separately marked missing. This is intentional slice-1
missing evidence, **not zero information gain**. No operational likelihood or
focus declaration was invented. Unsupported/noisy observation models are held.

For explicit shadow calculations, `:novelty-inputs` can carry:

```
{:contract <declared attempt contract>
 :ledger <read-ledger snapshot>
 :prior <illustrative prior>
 :models {<full action>
          {:schema :wm/attempt-endpoint-parameter-model-v1
           :authority :illustrative :source <declaration reference>
           :route <same route value used in the learning family>
           :observation {:schema :wm/perfect-attempt-endpoint-v1
                         :placement :post-build-artifact-revision}
           :meanings {<effect> <meaning-sha256>}
           :factorization {:status :declared-independent
                           :effects [<all eligible effects>]
                           :source <independence declaration reference>}}}}
```

A single effect needs no factorization assertion. Multiple effects need a
covering declared independence witness; a correlated or missing witness cannot
enter additive mode. This records a declaration, not a proof of causal
independence. A supplied `:prior {:kind :known-parameter :theta 0.5}` model is
the known-noise control: predictive entropy is positive, parameter EIG is zero.
Beta(a,b) uses the closed form from improve-5, with a recurrence/asymptotic
digamma implementation supporting positive real concentrations.

The narrative selection section adds one sentence naming each policy's nats
or absence and prior kind. `parameter-novelty` registers with load identity and
is listed in its canonical required-source map. No runner production edit;
its existing retention carries the augmented certificate.

## Checks and regression

`clojure -M:test -m cognitect.test-runner -n <namespace>`, own worktree:

| Namespace | Tests / assertions | Result |
|---|---:|---|
| futon2.aif.parameter-novelty-test | 8 / 793 | pass (accounting follow-up) |
| futon2.aif.policy-test | 8 / 15 | pass |
| futon2.aif.run-narrative-test | 25 / 121 | pass |
| futon2.aif.load-identity-test | 3 / 12 | pass |
| futon2.report.war-machine-test | 89 / 511 | pass |

The new namespace checks Beta identities; same mean/different concentration;
known-noise zero gain; T=2 counted once; duplicate produces counted once;
missing/unsupported observation and contract; invalid ledger and changed
meaning held; correlated effects refused; properly declared independent sum;
ledger read byte identity and file counts; both frozen G/posterior replay and
byte-identical decision apart from the new certificate slot; narrative output.
It counts its worktree data files before/after every test. Temporary ledger
and habit paths are isolated; no real store is written.

Negative: temporarily restored **only** the real `policy.clj` from base
`4c01c19e` in this worktree, ran the new namespace with its helper/tests retained,
and restored the changed file in a Python `finally` clause. No invariant or
source guard was disabled. Exactly two assertions failed (0 errors):

```
expected: (= (count ranked) (count (get-in enriched [:selection-certificate :parameter-novelty])))
  actual: (not (= 3 0))
  actual: (not (= 24 0))
```

The negative log precedes two additional controls; both failures exercise the
actual original certificate builder, not a stub or copied approximation.

Full `futon2.aif.full-loop-runner-test` was attempted and terminated after over
100 s while an existing test at line 5521 scanned canonical morning-brief
records through tripwire. Thread dump and partial log are retained; **no full
runner pass claimed**. The new test was then run alone:

```
clojure -M:test -m cognitect.test-runner -n futon2.aif.full-loop-runner-test -v futon2.aif.full-loop-runner-test/grounded-close-retains-parameter-novelty-selection-receipt
```

**1 test / 7 assertions, zero failures/errors**. It uses the existing passing
grounded feature-card fixture, reads durable `002-selection.edn` and
`007-closed.edn`, and checks receipt equality and selection-law bytes. The new
test redirects only its morning-brief filesystem port to its temporary root;
no source-authority guard is bypassed. Its grounded attempt took 384 ms.
Owner can run the whole runner namespace on canonical main.

Changed-file clj-kondo: 0 errors, 0 warnings (one pre-existing `str` info in
war_machine.clj). `futon4/dev/check-parens.el` on all seven changed/new Clojure
files: OK. `git diff --check`: clean. Java 21.0.11, Clojure 1.11.1,
CLI 1.12.5.1664. Logs/configs and registered warrant are in
`improve-5a-evidence/`; registry pre-check returned typed `:missing-entry`.

Registered warrant:
`test-registry-43385829fbc755fd1e4fa6c96997846e75c8e064bfc38a4dcd0b85a8aca75560`,
implementation commit `ccbcf210aa31bb2130954a8ea6438e0a922ee414`,
postcheck `:matched`, **5 tests / 54 assertions / 0 failures / 0 errors**.
Exact registration command, from `/home/joe/code/futon3c` in a separate CLI:

```
clojure -M -m futon3c.test-registry run /home/joe/code/futon2-narrative-improve-5a/holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-5a-evidence/registry.edn
```

The other namespace and focused runner results are fresh command logs, not
claims that this single warrant covers their executions. At that initial implementation, the completed
commands covered 131 tests / 720 assertions; the interrupted full runner command
is excluded from that total.


## Accounting follow-up: θ latent, novelty once

Implements the accounting decision recorded on main in FIXLIST §“improve-5
accounting decision”: `risk_marginal + ambiguity_conditional = pragmatic_cost
− I(theta;Y)`. Novelty's coefficient is 1; no κ appears in the receipt. The
strength question is about priors and C's strength per focus, not an extra
learning weight. No sensitivity sweep is added.

The supported illustrative model holds background terminal state fixed and
uses the declared independent endpoint channels. The predictive distribution
is retained as that fixed state plus Bernoulli means (θ marginalized).
Predictive entropy is the sum of marginal binary entropies; expected −log C
uses the scorer's actual additive log-weight preference member. Conditional
ambiguity is evaluated directly by digamma moments of Beta, not by subtracting
the receipt's information gain from predictive entropy. Risk is cost minus
predictive entropy. The receipt independently records both sides of the stated
identity, residual, tolerance and `:checked` / `:failed` status. τ=T and
multiplicity=1 remain explicit. Non-deterministic background or C that excludes
positive predictive support gives a typed absence for this finite accounting;
it is not silently treated as a finite identity or zero uncertainty.

Tests enumerate marginal outcomes independently for every one of the 27 frozen
candidates at Beta(1,1), Beta(9,1), Beta(90,10), compare direct KL and expected
cost against the factorized calculation, and compute conditional entropy using
integer harmonic sums. Each checks the identity, τ=T, multiplicity and unchanged
serving G. The known fair-coin control has H(Y|θ)=ln2 and I=0, hence EFE equals
cost; an excluded-support control remains held. Both full frozen decisions and
posteriors remain unchanged apart from receipt fields.

The preceding receipt implementation (`ccbcf210`) fails the new targeted
regression, with **2 failures / 0 errors**:

```
expected: (= :checked (get-in r [:terms :theta-latent :identity :status]))
  actual: (not (= :checked nil))
expected: (not (contains? r :shadow-kappa))
  actual: (not (not true))
```

This was tested by temporarily restoring only `parameter_novelty.clj` from
that commit in the isolated worktree, running
`clojure -M:test -m cognitect.test-runner -n futon2.aif.parameter-novelty-test
-v futon2.aif.parameter-novelty-test/theta-latent-receipt-is-required`, then
restoring the new file in `finally`. No guard or invariant was bypassed.
Fresh accounting gates: changed-file clj-kondo and check-parens pass; namespace
`parameter-novelty-test` 8 tests / 793 assertions, policy-test 8 / 15. The old
warrant check refused with `:review-diff-required`; a new warrant follows below.

Accounting warrant (supersedes the initial receipt warrant for this namespace):
`test-registry-6dfb2915396c4e14d9212d023949b6fc6a54862be041faa5597ef4ad7d9bcefa`,
commit `794f9eec457c4416ea02683a69ee66719aced767`, **8 tests / 793 assertions,
0 failures / 0 errors**, postcheck matched. Same registry command/config as
above. The focused grounded runner test was also freshly rerun after this
accounting change: **1 test / 7 assertions, zero failures/errors**.
