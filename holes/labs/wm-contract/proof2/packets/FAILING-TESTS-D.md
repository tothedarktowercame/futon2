# FAILING-TESTS-D — four pre-existing failures near the click's path

Date: 2026-09-25. Author: claude-3. Read-only: no code change, no click, no
flight, no write under `data/`, no shared-JVM load. Runs are in futon2's own JVM
at HEAD `b321efab` (17:48:38Z); every bisect run is in a throwaway sibling
worktree, never the shared checkout's working tree, and no stash. claude-10
found these at wiring row 9 (futon2 `f498f3f3`), identical with and without that
change: three test vars in two namespaces, the byte pin asserting twice.

| # | test | first failing commit | what moved |
|---|---|---|---|
| 1 | `ticket-queue-test/no-entries-preserves-frozen-decision-bytes` (×2) | `b1979ce2` 09-23 04:40 | the decision gained `:enacted-steps`; the frozen fixture predates it |
| 2 | `ticket-queue-test/ordinary-refusal-does-not-block-admitted-candidates` (ERROR) | `84f81cb4` 09-22 18:14 | the class observation model refuses a target whose relation is `:unknown` |
| 3 | `decision-gate-test/guard-locators-discriminate-by-production-observation-class` | `9d5525ee` 09-24 22:29 | `:C8` joined `observation-checks/checks`; the test's locator fixture has C3-C6 |

## 1. Frozen decision bytes (2 failures)

**(a) At HEAD.** `ticket_queue_test.clj:127` and `:128`, each
`(= expected (pr-str …))` against `test/fixtures/ticket-queue/before.edn`. The
two strings agree for 1,837 characters and then diverge:

```
expected  … 3.3535013046647805E-4}, :excluded-non-actions {:count 0, …
actual    … 3.3535013046647805E-4}, :enacted-steps #:p{:mission nil, :docs nil}, :excluded-non-actions …
```

13,679 expected bytes against 13,814 actual: one added key, nothing else.
**(b) Since when.** Passes at `31437d11` (09-23 04:27, 8 tests / 43 assertions,
0 failures); fails at `b1979ce2` (09-23 04:40), "Proof 1.6: the record names the
action the machine would actually take", git identity Joseph Corneli. That is
also the oldest commit `git log -S':enacted-steps'` reports. The fixture's own
last commit predates it.

**(c) Pin or code.** The pin. `:enacted-steps` is a deliberate addition — the
record naming the action the machine would take — and a byte-identity fixture
moves whenever the decision gains a field. The test did its job once already:
`fad94c89` (09-24) is titled "enacted-steps recorded {nil nil}; the byte-identity
test caught it". Worth noting before anyone regenerates the fixture: at HEAD the
value is still `#:p{:mission nil, :docs nil}`, so regenerating now freezes two
nils, and `fad94c89` touched `policy.clj` and `selection_certificate_test.clj`
without regenerating this fixture.

**(d) On the first flight's path.** The code under it is
`policy/select-action-cascades`, the selection stage of
REFUSAL-REGISTER-D §D2.2 — reached by any click that gets past assembly, but not
by M-autoclock-in's first click, which abstains at assembly (§D2.4). The failure
itself is a stale fixture, not a refusal, so it stops no flight.

**(e) Row and size.** Row 9 (selection law) touches this record; the fixture is
the ticket-queue component's. Smallest change: regenerate
`test/fixtures/ticket-queue/before.edn` from the current decision — one file,
no source change — **after** deciding whether `:enacted-steps` of `{nil nil}` is
the value to freeze. That second question is `fad94c89`'s, not this fixture's.

## 2. The class model's refusal (1 error) — the one to settle first

**(a) At HEAD.** Uncaught, not in an assertion, at
`scripts/futon2/report/war_machine.clj:6611` via
`cascade-decision-admitted` ← `cascade-decision` ← `ticket_queue_test.clj:111`:

```
clojure.lang.ExceptionInfo: cascade decision refused
{:kind :class-unknown-no-scalar-g, :target "M-main", :status :missing,
 :target-class {"M-main" :unknown},
 :possible-costs {:focused 0.5978…, :related 1.0498…, :unrelated 2.9957…}}
```

**(b) Since when.** Passes at `3b071260` (09-22 18:04, 43 assertions, 0
failures); errors at `84f81cb4` (09-22 18:14), "Proof 1.3: shared relation
producer (classify-target) for scoring and close (zai-1; committed by claude-5
after the job died before commit)". Same commit that `git log -S` names as
introducing the keyword. The assertion count drops 43 → 37 because the throw
aborts the var before its last six assertions.

**(c) Pin or code.** The code is doing what the record says, and the pin is
stale. `observation_model.clj:210-221` carries the ruling in its own comment:
"a target whose relation genuinely cannot be resolved gets NO scalar G — no
stop-the-line scoring, no worst case, no averaging, no uniform, no exclusion.
The refusal carries the possible terminal costs under Joe's C so the
certificate states them; selection proceeds only over candidates that have a
scalar" (codex-20 ruling, handoff B). Note the branch below it:
`(if c {c 1} {:stop-the-line 1})` — a target with *no* class entry scores
stop-the-line; only an **explicitly `:unknown`** class refuses, and
`focus_receipt.clj:104-106` says unresolvable relations are "`:unknown` with a
reason — distinct from an unmeasured outcome, and never guessed". The fixture
target `M-main` is synthetic and has no corpus relation, so it classifies
`:unknown`. The test is about ticket-queue strata; the class model now
intercepts before it reaches its subject.

**(d) On the first flight's path.** Yes, and not only as a fixture problem.
The production classifier reads `resources/wm/focus/commit-facets-v1.json`
(`focus_receipt.clj:16-19`, last written `c7a68fa9` 09-21). That file carries
**nine** relation rows: M-aif-policy-conditioned-eig, M-f11-find-production-successor,
M-wm-08-external-f2, M-G-wm-wiring, M-wm-aif-policy-grain-compliance (focus);
M-apm-capability-ratchet, M-action-cost-modelling, M-futonzero-generative,
M-aif4iad (associated). **M-autoclock-in is not among them**, so it classifies
exactly as `M-main` does. A first click abstains at assembly and never reaches
scoring (REFUSAL-REGISTER-D §D2.4), but the click *after* an interpretation
publishes does reach it, and on the record it would refuse
`:class-unknown-no-scalar-g` at `war_machine.clj:6611` — the rethrow boundary
already listed in §D2.2's judge rows. The failing test is the live path's
behaviour on the first flight's own target, seen early.

**(e) Row and size.** Not a wiring row: the class model is PROOF-wm-works 1.3
(zai-1, committed by claude-5) and the corpus is the focus receipt's. Two
distinct smallest changes, and they are not the same decision:
- for the **test**: give the fixture a resolvable relation or an explicit class
  in the test's inputs — one key in `with-inputs`' opts, no source change;
- for the **flight**: one relation row for M-autoclock-in in
  `commit-facets-v1.json` — one JSON object with a target, a relation and an
  `effective-from` — which is a classification judgment about that mission, not
  a mechanical edit, and belongs to whoever owns the focus corpus.

## 3. C8 missing from the gate test's locator fixture

**(a) At HEAD.** `decision_gate_test.clj:253`:
`expected: (= (set (keys observations/checks)) (set (keys class-locators)))`,
`actual: (not (= #{:C5 :C6 :C3 :C8 :C4} #{:C5 :C6 :C3 :C4}))`. 16 tests / 408
assertions, 1 failure.

**(b) Since when.** Passes at `48d49816` (09-24 22:27); fails at `9d5525ee`
(09-24 22:29), "OBS-C8 (AR-41): a class that observes tests passed AT THE
CURRENT CONTENT". The test file's own last commit is `7c872c55` (09-21), three
days earlier.

**(c) Pin or code.** The pin, and the test behaved as designed: a completeness
assertion that fires when a class joins `observation-checks/checks` without a
locator fixture beside it. It caught C8 two minutes after C8 landed. Nothing
about the gate is wrong.

**(d) On the first flight's path.** The gate (`decision_gate`) is on the tick's
path, §D2.2's gate rows, reached only after a chosen action — so not on
M-autoclock-in's first click. The failure is in the test's fixture, not in the
gate, so it stops nothing.

**(e) Row and size.** OBS-C8's own row (AR-41), not a wiring row. Smallest
change: one `:C8` entry in `class-locators` at `decision_gate_test.clj:245-250`
carrying the fields `check-registered-run` reads (`:repo :namespace :config`,
per `observation_checks.clj:281`), so the per-class loop below line 254 can
exercise it as it does the other four. One map entry plus whatever that loop
needs for a class whose inputs are not a repo path.

## Method and limits

Each namespace was run once at HEAD, then at each candidate commit and its
parent, in `git worktree add --detach` siblings of futon2 (so `deps.edn`'s
`../futonN` roots resolve), removed afterwards; `git worktree list` is clean.
Candidates came from `git log -S` on the moved keys and were confirmed by
running, not by reading. No other namespace was run, so this says nothing about
failures elsewhere. No code, fixture or test was changed.
