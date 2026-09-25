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

---

# Correction (2026-09-25, after attempting the two test-side fixes)

TEST-FIXES-I asked for both fixes test-side. One landed; the other cannot,
and the sizing in §1 and §3 above was wrong in two ways. Read-only again:
nothing outside `test/futon2/aif/ticket_queue_test.clj` was changed.

**§2 was two keys, not one** (landed, futon2 `f574a16c`). Injecting
`:focus-inputs` with a relation row for `M-main` is necessary but not
sufficient: the test pins `:focus-as-of early`, and `early` is the discovery
window's own start instant. `focus-receipt/discover` credits only commits
at-or-before the decision time, so at a window's first moment no focus is
established, `classify-target` returns `:unknown` whatever relation row it
finds, and the model refuses exactly as before. The decision time has to move
inside the window (noon of the same day establishes focus `WM`). Both keys are
the test's own inputs; no source, no fixture file, no facets file touched.
`futon2.aif.ticket-queue-test` goes 37 assertions / 2 failures / 1 error →
43 assertions / 2 failures / 0 errors, the six recovered assertions being the
ones the error used to abort.

**§3 is not a test-side fix at all.** `decision_gate.clj:72-77` keeps its **own**
class → required-fields table:

```clojure
(case (:class locator)
  :C3 [:repo :sha :path]      :C4 [:repo :sha :path :decl]
  :C5 [:repo :sha :bundle-path :entry]   :C6 [:repo :sha :path]
  nil)
```

Any other class falls to the `nil` branch and the gate returns
`{:kind :no-mechanical-check :class …}`, so **no `:C8` guard locator can pass
`gate/emit!`**, whatever the test's fixture says. Adding the fixture entry
makes the failure worse, not better: the good locator then fails
`(= decision (gate/emit! decision))`, and three per-field assertions fail
because the gate's `:locator-refusals` carries no entry for a class it does
not know.

The fixture half is right and was verified separately:
`{:class :C8 :repo "futon2" :namespace "futon2.aif.nonexistent-locator-test-ns"}`
returns `{:observed false}` from `check-registered-run` under
`*registry-latest*` bound to `:absent` — the seam the check declares for this
("Bound by tests, so no test needs the lookup endpoint to be live"). Note the
fields: `:config` is **optional** at HEAD, and a locator carrying it falls back
to the namespace lookup when `:config` is removed rather than refusing, so a
three-field C8 locator breaks the test loop's own every-field-is-required rule.
`:repo` plus exactly one of `:namespace`/`:command` is the required set
(`check-registered-run`, rule `:exactly-one-of-namespace-or-command`). The
packet's field list, and my §3 above, both came from the contract's
`:inputs {:repo :namespace :config}` and from the pre-`:command` shape of the
code.

So what the failing assertion is reporting is a **source-side gap, not a stale
fixture**: when C8 joined `observation-checks/checks` at `9d5525ee`, the gate's
duplicate table was not extended, and the gate cannot admit a C8 guard locator.
The test named it two minutes after C8 landed. Sizing, for the owner, no
proposal beyond it: one `case` arm in `decision_gate.clj`, plus the test
fixture entry and the seam binding — but the arm cannot be a plain field list,
because the gate would then have to express "exactly one of `:namespace` or
`:command`", which is the rule `check-registered-run` already holds. The two
tables are the same rule written twice, which is the disagreement
`fetch-latest-for-namespace`'s own docstring warns about ("two implementations
of that rule would disagree the first time one of them was wrong"). Whether the
gate gets an arm or learns to ask the check is the decision-gate owner's.

`futon2.aif.decision-gate-test` is therefore left exactly as it was: 16 tests,
408 assertions, 1 failure, and no warrant, since the namespace is not green.

---

# D2 — three more, at HEAD `de36a7d9` (read-only, same method and bar)

From claude-10's 58-namespace run at `27d6072d`; each quoted from a run at HEAD in futon2's own JVM, every bisect in a throwaway sibling worktree removed after, never the shared tree, no stash. Nothing changed; the first failing commits all carry the git identity Joseph Corneli.

| test | first failing commit | pin/code | path | smallest change, owner's call |
|---|---|---|---|---|
| `offline-tick-record-preserves-validity-and-missing-prefix`, ERROR at `war_machine.clj:6624` (`uniform-run-record-test`) | `84f81cb4` 09-22 18:14 "Proof 1.3: shared relation producer" — §2's own commit | fixture | tick scoring | the shape landed at `f574a16c`: `:focus-inputs` with a row for its own target, decision time inside a discovery window |
| `existing-selection-decisions-unchanged` ×6 at `:194` (`g-term-decomposition-test`) | `991e27a4` 09-21 05:11 "Wire joint policy precision learning from declared task observations"; green at `d168d348` 00:52 | pin | selection | the projection its twin already applies, in that one `is` |
| `no-selection-does-not-borrow-historical-quantities` at `:140` (`uniform-run-record-test`) | `97a84770` 09-24 19:45 "E-cascade-real D8/AR-16: the abstained tick record carries its typed declines"; green at `b4fbc290` 19:41 | pin | the click record the flight reads | assert the two keys are typed absences, not absent |

**The error, settled first.** `{:kind :class-unknown-no-scalar-g … :target :wm-tick-001-observation-crash}`: the
same refusal, site and cause as §2, a synthetic target with no corpus relation, and `cascade_decision_test`
already carries a row for this very target name. Qualifier: the var was red before that commit too (14 of 67
assertions at `3b071260`, green at `792afae2` 09-21 14:49); the 288 commits between are **not** bisected, since
the error is what is red at HEAD and `84f81cb4` is where it became one. Not on M-autoclock-in's first click,
which abstains at assembly — but its precondition is the one M-autoclock-in also lacks.

**Both pins are stale, and neither code change is wrong.** `selection_certificate_test.clj:22-34` reads the
*same* fixture and is green: it adds the new precision metadata to the baseline and projects away
`:policy-comparison :action-comparison :near-tie-threshold :enacted-steps`, each with a comment naming the
commit that added it; `g_term_decomposition_test.clj:194-196` compares raw, so every field added since the 09-21
baseline breaks it (first divergence `:tau-source :declared-beta`, 4,940 expected bytes against 9,461) — fix
that `is`, not the shared fixture, which the twin needs as the historical snapshot. For the other, `:decision`
is built with `assoc` (`full_loop_runner.clj:625-632`), so `:abstention` and `:chosen` are always present and,
with no decision, are typed absences (`{:status :absent :reason :no-selection-decision-recorded}`;
`chosen-summary`: "A typed absence when nothing was chosen") computed from this result — the comment above them
reads "No historical checkpoints or trace lookup", which is the property the test's name guards. Both keys exist
because the flight reads them (`record-summary`, `flight_runner.clj:244-253`), so dropping them is not open.

---

# D3 — uniform-run-record-test's 17 older failures, per failure

Read at HEAD `aba5d260`; read-only, no code, fixture or test changed. Each failure quoted from a run at HEAD in
futon2's own JVM; every bisect run in a throwaway sibling worktree, removed after, never the shared tree, no stash.
The twelve `:present` failures are **four** terms — A, C, D, Q — across three policies; **E passes**.

| group | first failing commit (git identity Joseph Corneli) | pin or code | AR | smallest change, owner's call |
|---|---|---|---|---|
| A, C, D, Q read `:missing` (12, `:125`) | `daf2124e` 09-22 17:14 "Proof 1.3 build 2/3: class observation scorer in the live joint decision"; green at `3d25b818` 16:32 | code — the record types a real absence | **AR-24**, `:proposed` | assert the typed absence for A/C/D/Q, keep `:present` for E |
| F ×3 (`:122`) | `ad039985` 09-24 "F-ABS (PROOF-2 packet 27): record the selection law that actually ran when F is absent" | code — a deliberate vocabulary change | none | three words in the pin |
| `:derived-no-overlap` (1, `:126`) and the checker's `c-source` (1, `:132`) | `daf2124e`, with the twelve | **not the test** — a key whose meaning changed under its reader | none | one line in `wm_run_validity.bb`, or keep live-C where it reads |

**A, C, D, Q.** `:A` `:C` `:D` `:Q` at `:125`: `expected: (= :present (get-in p [:terms term :status]))`, `actual:
(not (= :present :missing))`. Bisected: seven probes spread across the 288-commit range (09-21 15:30 … 09-22 13:38)
are all green at 67 assertions, so nothing earlier contributes; the transition is one commit, `daf2124e`, where all
fourteen of the older failures appear at once. `census` (`g_term_decomposition.clj:105-106`) reads A/C/D/Q from each
ranked entry's `[:certificate :consumed-g]`, and `verdict` (`:44`, returns at `:55-63`) returns `{:status :missing :value nil :reason
:consumed-value-not-recorded}` when that value is nil — for `:C` also when any step's `:distribution` is nil
(`:57-58`), which is what this record has. `daf2124e` made war_machine's joint decision score with the class-emission
model, whose decomposition (KL of the class pushforward against 55/35/5/5, ambiguity 0) has no token-level A/C/D/Q to
consume. So the terms are absent because the machine no longer computes them on this path, and the record says so
with a reason; the `:present` pin predates the class scorer. E stays `:present` because it comes from the candidate's
`:habit`, which selection still supplies (`census:106`).

**That absence is already on the register.** AR-24 (`PROOF-2-THEOREM-draft-2026-09-24.md:316`, `:proposed`), from
Walkthrough 03 §5 over live records: "On the recorded clicks the scorer consumed no token-level A at all: the
precision-family model is `:class-emission` with `:rates {:status :absent :reason :class-emission-has-no-token-rates}`
and the G decomposition's A term is `:consumed-value-not-recorded`. Clause 1 as drafted assumes a consumed A to
compare with a measured one; on these records there is neither." This test reaches the same fact from the fixture
side. It is an absence, not a substitution: nothing stands in for the term, and the status carries its own reason.
C, D and Q have no AR of their own (AR-26 is the only other row naming the decomposition). Because AR-24 is
`:proposed`, flipping the pin records today's behaviour while the question it opens — whether the class scorer should
record its own consumed terms rather than leaving the token four absent — is still open; that is AR-24's owner's
call, and the test-side change is four lines in the shape used at `7b9690a1`.

**F.** At `:122`, `{:status :absent :value nil :reason :omitted-from-law}` against a pin of `{:status :missing :value
nil :reason :consumed-value-not-recorded}`. Confirmed from the diff, not from a run: at `ad039985` the namespace still
errors on the class model before reaching this assertion, so the evidence is `git show ad039985 --
src/futon2/aif/g_term_decomposition.clj`, which replaces exactly those two lines in `verdict`'s F branch. The reason
is in the code (`:58-61`): "an absent F is not a lost value — the selection law omits the term (cascade-selection line
112), so the record says the term was omitted from the law that ran." A deliberate change of vocabulary that makes the
KIND of absence explicit, which is the distinction `[[absence-must-not-read-as-a-value]]` (cited at AR-27) turns on.
Owner: F-ABS, PROOF-2 packet 27.

**The two C-shape failures are not a stale pin.** `:126` wants every scoring entry's `[:c :status]` to be
`:derived-no-overlap`; `:132` wants the validity checker's field map to read `"c-source" "flagged"` and gets `"bad"`.
After `daf2124e` the scoring entry's `:c` is the class scorer's step-indexed preference schedule — `{:form
:step-indexed :schedule nil :steps [{:tau 1 :distribution nil} …]}`, six of them on this record — and no `:status
:derived…` appears anywhere on the record. `scripts/wm_run_validity.bb:90-99` does `(find-field r :c)` and reads
`:status`: `:derived` → ok, `:derived-no-overlap` → flagged, anything else → **bad**, noting the status it saw (here
`nil`). So the checker is reading a key whose meaning changed under it: one name, `:c`, now carrying the preference
schedule where the checker expects live-C's provenance. Its `bad` is a verdict about C's source computed from
something that is not C's source, which is the nearest thing here to an absence read as a value, though no AR names
it. The smallest change is not in the test: either the checker reads live-C at its own path instead of by
`find-field`, or the record keeps the live-C status where the checker looks. Which of those is right is a claim about
the record's schema, so it belongs to the checker's and live-C's owners, not to this test.

**Path.** All seventeen are on the click's scoring path, not the flight's: `from-result` (`g_term_decomposition.clj:
122-131`) writes the census onto every run record, and the checker runs over any record. None is reached by
M-autoclock-in's first click, which abstains at assembly (REFUSAL-REGISTER-D §D2.4). **Not done:** no per-term
bisect was needed — all four terms move together at `daf2124e` — and the F trio's commit is established by diff
rather than by a run, for the reason given.
