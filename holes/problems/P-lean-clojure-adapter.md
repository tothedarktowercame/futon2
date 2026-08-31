# P-lean-clojure-adapter — One adapter from Lean declarations to Clojure witnesses, standardised from the APM one

Problem record (delivery-lifecycle v2). Opened 2026-08-30 by claude-15 on Joe's remark: *"It is probably
worth taking the Lean-to-Clojure adapter we developed for the APM work and standardizing it, so that we can
reuse it here — a bit of infrastructure that builds on top of the Lean work and makes the next steps nicer."*
Owner: claude-15. **holder.** claude-15 → codex-22 (AD-D1). **parent.** P-lean-holes (the holes are the
contact points; this is the wire across them).

## S1

**problem.** Two adapters exist and neither is the general thing. *APM:* `mathlib4-apm-validation/DarkTower/
APMCycleContractEmitter.lean` builds `contractJson : Json` from the canonical cycle machine and prints it
(`lake env lean --run … > apm-cycle-contract-v4.json`, `futon3c/scripts/regenerate-apm-contract.sh`);
`futon3c.apm.generated-contract` reads it (`read-contract`), validates it against **hand-restated
expectations** (`required-bounds`, `required-dispatch-policy`, `required-*` — ~330 lines that duplicate what
Lean emitted) and checks a `validate-round-trip` on phase order; `campaign_trace` and `countdown_control`
consume it. *WM:* `mathlib4/DarkTower/WarMachine/ContractEmitter.lean` emits predicate **families** —
`{id, name, lean-predicate, clauses, clojure-locus}` — naming, per Lean predicate, the Clojure file that is
supposed to witness it; **nothing on the Clojure side reads it** (H3a never built). Meanwhile the build's
contact points now live in `Holes.lean` (17 bodies / 18 holes at `93f0da26`) with the rule "a lane closes
when its hole moves", checked today by hand: a reviewer reads the Clojure run, reads the Lean, and writes
a ledger line. That is the adapter's job, and it is being done by people.

**now.** The pieces are all present once: Lean → JSON emission (APM, WM); a Clojure validation boundary
(APM); a per-predicate `clojure-locus` (WM); a two-line count script over doc tags (`count-holes.sh`);
receipts of Clojure runs with fixtures (`checks/*-snatch.edn`, `harvest-verify-*.edn`). What is missing is
the loop closing in one direction: **the Lean file is the source of truth, the Clojure side registers which
declaration each check witnesses, and a lint reports per declaration `witnessed / unwitnessed / stale`.**
The APM consumer's ~330 lines of restated expectations are the anti-pattern: the contract is checked against
a copy of itself.

**solved.**
1. **Emitter convention (Lean):** any module can export a *declaration registry* — for each tagged
   declaration `{name, kind ∈ {closed, hole}, signature (pretty-printed), owner, holder, decided,
   clojure-locus (expected witness), fixture?}` plus `{schema-version, contract-id, source {module, git-sha}}`
   — via one shared `DarkTower/Contract/Emit.lean` helper, so `Holes.lean`, `ContractEmitter.lean` and the
   APM emitter all print the same shape. `count-holes.sh` becomes a consumer of that JSON, not a grep.
2. **Consumer (Clojure), generic:** `futon2.contract` (or `futon3c.contract`) — `read`, `validate-schema`,
   and a **witness registry**: a Clojure check declares `{:witnesses "DarkTower.WarMachine.Holes.r8Census"
   :fixture "checks/r8-census.edn" :run-sha …}`; `lint` joins registry to contract and prints, per
   declaration, `witnessed` (check exists, ran, fixture recorded, contract sha matches) / `unwitnessed` /
   `stale` (fixture older than the declaration's commit) — **two lines per record, never a percentage**.
   No restated expectations: the only Clojure-side constants are the witness bindings.
3. **Round trip that can fail:** for a `closed` declaration with a fixture, the Clojure check re-runs the
   fixture and compares to the Lean-stated expected value (e.g. `r2ContractCensus` illFormed = 2;
   `r8Census` 755/32/5); a mismatch is `stale-or-refuted`, reported with both numbers.
4. **Evidence shape, stated on the Lean side before any run (Joe, 2026-08-30):** *"if we specify in the
   Lean side what the evidence should look like, then we can run the Clojure code and see if we really do
   gather evidence with that right shape — and even in the absence of evidence we should be able to derive
   what the evidence should look like, in light of our theorisation of active inference."* So every
   registry entry carries an **`evidence` type** — the shape a witness must inhabit — and a **`falsifier`**
   over that type (the zero-mass outcome), i.e. the lifecycle's `EvidenceContract` (§0.6: subject, claim,
   artefact KIND, domain, corpus, method, falsifier, not-evidence) with its `artefact` and `falsifier`
   fields as Lean types rather than prose. The consumer's lint then has a third judgement beside
   `witnessed/unwitnessed/stale`: **`conformant`** — the fixture parses as an inhabitant of the declared
   evidence type — versus **`wrong-shape`** (the artefact exists and is not the evidence the theory said to
   gather: the apex's "wrong evidence", made mechanical). Two witnesses already inhabit their types:
   `nonDegenerateAblationLaw`'s evidence is an ablation table `Prior → {argminG, argminRisk, moved?}` —
   `checks/ablation-snatch.edn` is one; `r8EraBoundary`'s is `{boundary, perEra : Era → {count, storedF?,
   selectionGain?, shape, meanPrecision}}` with the falsifier "a form in neither era". **Derivable without
   evidence:** the evidence type for a law is the observation the AIF reading predicts if the law holds —
   the generative model of the run — so a hole with no witness yet still states what would count, and a
   witness that cannot be typed against it is the refusal, not a pass.
5. **Falsifier:** a contract with a declaration nobody witnesses must show `unwitnessed`, not disappear;
   a Clojure check claiming to witness a declaration that is not in the contract must fail the lint
   (the inferred-edge facade); regenerating from a different Lean sha must change `source.git-sha` and
   flip fixtures to `stale` until re-run.

**facades:** two emitters for one contract with no declared authority (the AD-D1 finding: `apm-lean` vs `mathlib4-apm-validation`); restating the contract in Clojure and checking equality with itself (the APM `required-*`
pattern); a `witnessed` computed from the presence of a file rather than a recorded run; percentages;
an adapter that reads the JSON but never fails; a `clojure-locus` string nobody resolves.

**status.** open.

## deliveries
- **AD-D1 — discovery, no code** (codex-22, the Lean seat). Read both emitters, the APM consumer, the WM
  families, `Holes.lean`, `count-holes.sh`, and two Clojure check receipts. Produce: (i) a side-by-side of
  the two emitted shapes (fields, who consumes each field, which fields are restated on the Clojure side);
  (ii) the proposed common registry schema (EDN and JSON, one example entry per kind); (iii) the witness
  registry shape and where a check declares it (metadata map in the ns? a sidecar EDN?) with one worked
  example (`find_snatch.clj` ↔ F1–F4); (iv) which of the APM consumer's `required-*` blocks would survive as
  witness bindings and which are pure restatement; (v) refusals: anything in the APM adapter that cannot
  be generalised without changing APM behaviour. ≤ 200 lines, file:line throughout. Refusal permitted.
- **AD-D2 — build (after review):** `DarkTower/Contract/Emit.lean` + `Holes.lean` exporting through it +
  the JSON committed — **each HOLE carrying its `evidence` type and `falsifier` (solved 4)**, starting with the
  two that already have inhabitants (ablation table; era table) so the round trip has a green and a red
  case on day one; **AD-D3:** the Clojure consumer + lint with the falsifier tests; **AD-D4:** APM's
  emitter moved onto the shared helper with its behaviour unchanged (round-trip test as acceptance).
  One behaviour each; each past claude-13.

## log
- 2026-08-30 record written (claude-15) from the survey; AD-D1 dispatched (status line below).
- 2026-08-30 **AD-D1 reviewed by claude-15 — passes** (futon2 `863f33ac`, 171 lines). Checked: three
  pointers at source; `generated_contract.clj:424–425` is `(not= required-dispatch-policy (:dispatch-policy
  contract))` — whole-map equality against a hand-kept copy, so RESTATEMENT is confirmed for that block and
  the note's 11/0 tally stands (my own regex comparison gave 65 of 81 keys equal — nested maps, my
  instrument); the witness registry binds `{check, fixture, run-sha, contract-sha, result}` — `witnessed` is
  a recorded run. **Refusal upheld, against my packet:** the regeneration authority is
  `/home/joe/code/apm-lean` (`regenerate-apm-contract.sh:5`; its emitter changed 2026-08-30), not the
  `mathlib4-apm-validation` copy I named (08-26). Added to *solved* 5 and the facades: **the adapter refuses
  ambiguous authority** — a contract whose `source` does not match the configured regeneration root is
  `wrong-authority`, never silently chosen. The proposed hole entry predates the evidence-shape requirement
  (solved 4); AD-D2 adds `evidence`/`falsifier` to it.

**STATUS 2026-08-30 15:47Z:** AD-D1 dispatched to codex-22 — job `invoke-1788104829154-4334-88a69f02`, park `park-e661282a-0530-48c5-a1a7-3f86a302a399` (deadline +45 min).

**STATUS 2026-08-30 15:59Z:** AD-D1 passed; AD-D2 refused once by claude-13 (fixed-point on HEAD-at-emit), passed on rev 2, dispatched to codex-22 — job `invoke-1788105584404-4352-b758986c`, park `park-7da0a7d3-5311-4c22-b42a-c5d13dda104f` (deadline +45 min). AD-D3 (consumer + lint) written after AD-D2's gate.
- 2026-08-30 16:13Z **AD-D2 reviewed by claude-15 — passes** (mathlib4 `25d1771d` emitter/registry + `4e17e37e`
  JSON; then the owner's `e3f65c5c` + `5e7b4c2a`). Checked: scope = `Contract/Emit.lean`, `Holes.lean`
  (+91), `count-holes.sh` (now `jq` over the JSON), `emit-contract.sh`, the JSON; **the equation computed
  here from the pinned tag count: 18 + 5 = 23 = 23**; zero errors; re-emit byte-identical; `source.git-sha`
  = `git log -1 -- Holes.lean` (the fixed point claude-13 traced converges: module commit, then JSON commit
  alone); 20 holes typed with nine evidence shapes, three refused in the doc tag (`C`, `find`, `organise` —
  implementations); the three licensing sentences opened at their cited lines (F1–F4 `P-validated-R5:485–`,
  `P-R9:33/35`, `P-R2:26`); `AblationTable` and `EraTable` declared. Registry keys: name, kind, signature,
  owner, holder, decided, clojure-locus, fixture, evidence, falsifier. **The contract exists.** AD-D3 (the
  Clojure consumer) is under claude-13's read (`BUILD-packets/AD-D3.md`).
- 2026-08-30 16:23Z **AD-D3 dispatched** to codex-8 (job `invoke-1788107007642-4387-9d4144c4`) after claude-13's REFUSE (judgements never read
  `:result` — the self-report facade rebuilt inside the tool that exists to refuse it) and PASS on rev 2 (md5
  `6f718dee`). Registered before the run: 5 bound (ablation `:conformant`, four F-laws `:witnessed`), 3
  `:refused-implementation`, 15 `:unwitnessed` (forced arithmetic), 0 `:stale` (today-tautological; the
  drift detector for the next emit). **Honest floor of this design (claude-13):** `:result` is builder-written,
  so a false `:passed` is an affirmative false statement with a timestamp, not an omission — an improvement,
  not verification. **AD-D5 (queued):** the lint invokes `:check` and derives `:result` from the run.
- 2026-08-30 16:28Z **AD-D3 reviewed by claude-15 — passes with two fixes made directly** (codex-8 futon2
  `7e4f3253`; review fixes `3a4344a`, `c9f17be`). Checked: scope (lint, registry, test, two fixtures); tests
  3/8/0 re-run; lint run twice with the contract's authority — byte-identical report and console; **live
  counts 1 `:conformant` (the ablation law — `AblationTable` parsed from `ablation-snatch.edn`) / 4
  `:witnessed` (F1–F4, `:shape-check-not-implemented`) / 3 `:refused-implementation` / 16 `:unwitnessed` /
  0 `:stale` / 24 `:closed-by-record`** — 16 not 15 because the contract has 24 holes at `32b92969`
  (`r9WmPerRowDeclarations` landed after the packet was written); codex-8 reported the actual and did not
  force the old arithmetic. Seed bindings verified against futon3 git (`5941270`, `6364964`) with
  `:result`/`:recorded-at`; a registry entry naming a non-contract declaration exits 1 (run); the judgement
  path reads `:result` (`:witness-failed` on `:failed`). **Fixes, mine:** (1) a stray `:result` value (e.g.
  `:unknown`) fell through to `:conformant` — now malformed, exit 1; (2) a wrong-authority contract judged
  every hole `:wrong-authority` and still exited 0 — `pass?` now fails closed, as *solved* 5 says.
  **The hole-moved judgement is now a lint line.** AD-D5 (queued): the lint invokes `:check` and derives
  `:result` from the run, so a false `:passed` cannot be declared. AD-D4 (APM onto the shared emitter)
  waits on the authority decision (`apm-lean` vs `mathlib4-apm-validation`) — Joe's.
- 2026-08-30 16:44Z **First `wrong-shape`** — `r8EraBoundary` (R8-D3's report vs the declared `EraTable`): right numbers, wrong evidence shape; the lint said so without anyone arguing it. R8-D4 conforms the generator. Also: the axiom gate (charter 3a) found `native_decide` under three theorems across two lanes with no artefact saying so; artefacts now carry `#print axioms`.
- 2026-08-30 16:53Z **Standard for generated Lean artefacts (from the R8-D3/R2-D3 gates):** the *generator* emits a
  named theorem and `#print axioms` for it — never a hand edit of a generated file (the owner's hand-added lines on
  R8-D3 broke its own `regenerate && git diff --exit-code` gate and were reverted, `58b55c0`). The registry
  binding carries the axiom set until then. Lint after R2-D3: witnessed 8 / wrong-shape 1 (R8's `EraTable`) /
  stale 5 (pre-G-D3 bindings, re-run pending) / unwitnessed 13 / refused 5. Every node lane that ran today has its
  run bound; three holes discharged by stated `native_decide`, four by kernel `decide`.
- 2026-08-30 16:56Z `EraTable`'s evidence type rewritten so a non-uniform era is representable — `:conformant` for `r8EraBoundary` now means something (before, the type could not have been otherwise). The lint's shape check for `EraTable` must follow the new fields (R8-D4 / an AD-D3 follow-up: `era-table?` reads `shapes`, `storedFCount`, `precisionRecords`).
- 2026-08-30 17:16Z **AD-D3b (queued):** `contract_lint.clj`'s `era-table?` reads the pre-rewrite `EraSummary` fields; the R8-D4 report conforms to the new type and is judged `:wrong-shape` by an outdated check — a lint that lags its contract. Fix: shape checks generated from (or validated against) the contract's evidence types, not hand-written per type. The generator now emits `#print axioms` (3a(iii)); first lane to do so.
- 2026-08-30 17:17Z **Staleness is too coarse (actual, after R8-D4's re-bind):** lint now reads witnessed 2 /
  wrong-shape 1 / **stale 11** / unwitnessed 13 / refused 5 — the contract's source sha moved with the owner's
  `EraSummary` commits, so R9's four and R2's two bindings (made against `1b09974a`) are marked stale although
  **their declarations did not change**. Stale-by-contract-sha is honest but coarse: every unrelated Lean commit
  invalidates every binding. **AD-D5 refinement:** staleness per binding = *the text of the bound declarations*
  (signature + evidence type) differs between the binding's contract sha and the current one, or the fixture's
  last commit moved — not "the contract moved somewhere." Until then the number is reported as it is, and a
  re-run is what re-binds.

