# C38 — `logMultivariateBeta` discharge

Before this delivery, `P-glossary-mathematics` had **0 bound / 5 unbound**
holes.  The four carrier-family holes remain untouched.

`logMultivariateBeta` now requires a nonempty list of strictly positive real
concentrations.  Zero and negative concentrations are outside the Dirichlet
domain; making that fact a subtype obligation prevents them from being
silently assigned a numeric result.

The independent fixture uses the integer Gamma identities `Gamma(1)=1` and
`Gamma(n+1)=n!`: `B(1,1)=1` and `B(2,1)=1/2`.  Lean proves the corresponding
logarithms in `LogMultivariateBetaWitness.lean`; the Clojure check independently
recognises those two analytic cases and rejects a denominator changed from 2
to 3.

Canonical lint invocation:

```sh
AUTH=$(jq -r '.source["git-sha"]' ../mathlib4/DarkTower/WarMachine/holes-contract.json)
bb checks/contract_lint.clj --contract ../mathlib4/DarkTower/WarMachine/holes-contract.json --registry checks/witness-registry.edn --report /tmp/C38-contract-lint.edn --authority "$AUTH"
```

After registry binding, the lane is **1 bound / 4 unbound**.  “Bound” here
means an executable witness-registry binding, not merely a closed Lean body.
