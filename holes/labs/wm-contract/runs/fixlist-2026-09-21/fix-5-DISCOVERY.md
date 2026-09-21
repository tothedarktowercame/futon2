# Fix-5 discovery: where candidate supply stops

2026-09-21, codex-12; discovery only. Base: `4354668b` on `main` (this
repository has no `master` ref). Branch: `fix/narrative-5`.

The serving path has no producer that turns retrieved patterns into admitted
interpretations and then constructed candidate orders. Mission wants supply
outcome tokens, not applicability. Retrieval supplies pinned proposals, not
applicability. The two streams meet only when somebody writes a declaration.
Increasing retrieval k would not repair this missing step.

Two corrections to the assignment's premise matter:

* The reference run's three singletons are **two** codex-1 declarations and
  **one** older claude-4 declaration. They are not three codex-1 declarations.
* The earlier runs really contain two three-pattern EoI orders. Their removal
  is not explained by deleting empty cascades: their token locators were
  separately withdrawn. A multi-pattern order has existed, but automatic
  production of a justified one from a stated want is still missing.

## Recorded funnel

Primary evidence, relative to the canonical futon2 checkout:

* `data/wm-runs/tick-run-record-2026-09-21-{1789951020,1789952479,1789964661}.edn`:
  `:decision/:selection-certificate`, `:decision/:selection-law/:posterior`.
* `data/wm-trace/wm-trace-2026-09-21.edn`, **three EDN forms**, matched by
  `:run/id`; form 3 is the reference run. Its `:cascade-problems` retains
  assembly refusals and dropped records missing from the compact run record.
  Reading only the first form answers the wrong run.
  Read posterior keys from the run record: the trace representation is not
  the same candidate-keyed map.

Counts use distinct units: targets, pattern interpretations, candidate orders,
proposal records. This is a join of two sources of candidates, not a sequence
where all proposals become interpretations.

| Stage | 1789951020 | 1789952479 | 1789964661 |
|---|---:|---:|---:|
| Assembly target identities (problems + refusals) | 241 | 241 | 283 |
| Assembly problems | 21 | 21 | 3 |
| Assembly refusals | 220 | 220 | 280 |
| Pattern interpretations in retained problems | 23 | 23 | 3 |
| Orders in retained problems, including old empty baselines | 43 | 43 | 3 |
| Nonempty orders there | 22 | 22 | 3 |
| Retained proposal supply | not recorded | not recorded | 90 |
| Proposal-linked interpretation admissions | not recorded | not recorded | 0 |
| Scored candidates / policies / posterior entries | 24 / 24 / 24 | 24 / 24 / 24 | 3 / 3 / 3 |
| Lengths of scored orders | 21 empty, 1 singleton, 2 triples | same | 3 singletons |

The earlier 22 nonempty assembled orders comprise 19 generated singleton
orders with **no interpretation receipts**, one WM-08 singleton and two EoI
triples. The older joint-family builder dropped the 19 unreceipted orders
(`:interpretation-receipts-missing`) but retained the 21 per-target empty
baselines; hence 43 assembled orders → 24 scored. That reason is established
by the old builder in `git show c155d690^:scripts/futon2/report/war_machine.clj`,
not by a nonexistent `:dropped-candidates` field in those compact certificates.

For the reference run, the target accounting closes exactly:

| Assembly result | Targets | Meaning |
|---|---:|---|
| `:universe-not-admitted`, missing `:universes` | 262 | No source universe for the identity |
| `:no-admitted-interpretation`, missing `:interpretations` | 17 | Generated wants exist, but patterns/receipts are empty |
| `:universe-not-admitted`, missing `:locators` | 1 | EoI: three tokens have no checkable locator |
| Assembled and retained through candidate admission | 3 | One receipted singleton each |
| Total | 283 | 280 refused + 3 retained |

The 17 generated targets are M-dionysus-winddown, M-daily-scan,
M-distributed-frontiermath, M-G-wm-wiring, M-kangaroo, M-chipwitz-corps,
M-wm-aif-policy-grain-compliance, M-canon-fingerprint-store,
M-self-documenting-stack, M-apm-capability-ratchet, M-usage-hacking,
M-futonzero-mvp, M-action-cost-modelling, M-aif4iad,
M-federated-agency-hardening, M-futonzero-generative, M-futon-forward-model.
They fail at interpretation **before** `:no-constructed-candidate` can apply:
`cascade-problems/assemble-one` reports the first failed condition.

The proposal stream is narrower than “retrieval for every want”:

* 48 `:retrieval-proposed` records all target **M-a-wmc-scaling**, from the
  single retained bundle
  `data/wm-cascade-proposals/8961ab24-0c3a-4e7c-adbd-7413b5e3e5fa/proposal.edn`.
* 42 `:repair-finding-proposed` records come from 43 open repair findings;
  `repair-attempt-054` produces `:repair-finding-evidence-missing` because its
  failure/discharge fields are missing.
* All 90 proposals have `:proposal-awaiting-agent-admission`, missing
  `[:interpretation-receipt :guard :produces :locators]`; admissions is `[]`.
  The 17 generated want targets additionally have `:no-evidenced-proposal`.
* The 388 dropped records are **not 388 distinct candidates**: 280 assembly
  refusals + 90 pending proposals + 17 targets lacking proposal evidence +
  one malformed repair finding. Their populations overlap.

`proposal-supply/:admissions` specifically counts receipts joined by
`:proposal-id`; zero does not mean zero independently hand-admitted patterns.
The three actually scored entries are:

| Target | Pattern | Construction receipt author |
|---|---|---|
| M-aif-policy-conditioned-eig | apparatus/one-authority-per-question | codex-1, 2026-09-21 |
| M-f11-find-production-successor | apparatus/done-is-observed-running | codex-1, 2026-09-21 |
| M-wm-08-external-f2 | cascade-construction/run-it-on-a-real-case | claude-4, 2026-09-17 |

All have local id `:C1`; identity must include target. Three constructed
orders survive candidate admission and all three appear in scoring.

Do not use `:enumeration-completeness` as the assembly denominator. All three
runs report available mission/ticket/excursion counts 137/22/158 and enumerated
counts 0/0/0. Those cannot describe the simultaneously recorded nonempty
cascade family. The census names the legacy proposer comparison; its zeroes
are not evidence that assembly received zero targets.

The fixlist's **441 retained holes / 99 checkbox wants** is context, not a
freshly established count for these records. The earlier traces' 19 generated
problems contain **116 want tokens**, and all 21 problems contain 120. The
reference trace lacks `:mission-hole-coverage`; this discovery cannot recover
441/99 as its contemporaneous coverage census. That persistence issue belongs
to fix-13. Do not equate a hole count with a target count or corpus C count.

## Why the family changed

Four relevant commits, independently inspectable with `git show`:

1. `6592c58b`: supplies the two codex-1 target-specific declarations.
2. `c155d690`: stops adding empty baselines to assembly's executable orders;
   pairs each order with its receipt; adds per-pattern receipt admission.
   The lane may retain an empty diagnostic comparison, never joint selection.
3. `19a3af6b`: removes EoI's three C6 locators. Reference-run EoI refusal names
   `:change-authored-and-bound`, `:obligation-resolved-through-the-account`,
   `:premise-refused-before-work`. Its two triple orders no longer reach scoring.
4. `65e523ef`: removes the generated
   `:aif/grounded-actuation-not-reobservation` interpretation and singleton
   candidate from `mission-hole-wants/mission-source`, leaving empty maps and
   vectors. It adds evidence-only proposal supply. A checkbox no longer
   authorizes the claim that a pattern can produce all the mission's wants.

Thus 24 → 3 is not a controlled comparison changing only empty-cascade policy:
remove 21 empties and two old EoI triples, add two new singletons, retain WM-08.
The generated 19 old singleton orders were already absent from scoring.

## Current, isolated assembly probe

Ran `clojure -M /tmp/fix5-dry.clj` in `/home/joe/code/futon2-fix-5`, exit 0,
using the real `load-declared`, `mission-sources`/merge, `assemble` and
`record-supply` functions. **Not a live tick or authoritative substrate census.**
Inputs were an explicit current filesystem mission scan, declarations in this
worktree, and the already-recorded form-3 proposal supply. Tickets were not
separately scanned. Observation checks only read Git objects. Mutable proposal,
repair, habit and runner stores were never opened for updating or invoked;
no selection, scoring, dispatch, retrieval subprocess or shared JVM was used.

Fresh output (fields reordered for readability):

```edn
{:scope :current-file-missions-plus-recorded-proposals
 :targets 260 :source-targets 31 :declared-targets 4
 :interpretations 6 :source-candidates 5 :problems 3 :constructed 3
 :coverage {:live-missions 217 :holes-retained 634
            :missions-projected 29 :holes-projected 165
            :holes-not-projected 469 :targets-added 27
            :targets-deferred-to-declaration
            ["M-aif-policy-conditioned-eig" "M-f11-find-production-successor"]
            :not-projected-by-kind
            {:pending-lifecycle 226 :open-section-item 102 :work-marker 141}}
 :refusals {[:no-admitted-interpretation :interpretations] 27
            [:universe-not-admitted :universes] 229
            [:universe-not-admitted :locators] 1}
 :drops {[:proposal-supply :repair-finding-evidence-missing] 1
         [:proposal-supply :no-evidenced-proposal] 26
         [:proposal-supply :proposal-awaiting-agent-admission] 90}}
```

The four declaration targets contribute six interpreted patterns and five
orders: EoI's three patterns/two orders fail its locator condition; the other
three targets remain. The generated 27 targets contribute zero interpretations
and zero orders. One already has retained retrieval proposals, explaining
26 rather than 27 `:no-evidenced-proposal` records. Downstream admission/scoring
was not executed in this probe; those counts come from the recorded run above.
The different file-scan counts must not be presented as substrate drift.

## What hand-admission actually supplies

Worked source: `resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn`.
The agent declares four facts, three wants, one interpreted pattern, and one
order. The initial task-stated C4 locator points to `cb2045b8…`; the three
completion locators point to checked declaration heads at `HEAD`.

Judgment and policy choices:

* `:reading`: the shared A4a/BMR updater is to become the authority for both
  hypothetical and observed posterior changes, with equality/provenance and
  unknown-outcome acceptance work.
* `:scope-limit`: only the shared-updater task, not the model, typed-Q task or
  calibration task. `:observation-limit` explicitly limits C4 to observing a
  checked declaration, not verifying its supporting evidence.
* Which pattern applies; which token it produces; why task-stated and not-yet-
  completed are its guard; which order to propose. These do not follow from a
  hash, retrieval rank, or a checkbox. Beta/scales/schedule are declared
  policy inputs, not values an interpreter may silently choose.

Mechanical work after those choices:

* Preserve pattern path/revision/SHA256 and target path/revision/SHA256/hole-id/
  line. The loader checks pattern source bytes against the supplied hash.
  Do not assume it validates every target-source field: `read-receipt-source`
  specifically validates `:source`, while `:target-source` is carried metadata.
* `observe-facts` invokes actual C3–C6 observations to produce true/false/unknown.
  Assembly checks universe, pattern coverage, wants, locator classes, nonempty
  receipt-bearing orders and beta. The runner checks a receipt for every
  pattern in each candidate before scoring.
* Target-qualify tokens, carry receipts and compute guards/model transitions.
  A `:construction-receipt {:kind :hand-admitted :moves [] ...}` is provenance
  of a hand-authored order, not evidence that a constructor searched a family.

The current admission checks are necessary but do not mechanically prove the
semantic reading. C3 proves path presence; C4 proves declaration presence.
Neither proves that an arbitrary effect assigned by an interpreter is warranted.

## Proposed follow-up: interpretation and construction before selection

Implement a bounded preselection producer with its own request identity and
immutable evidence directory. It may use an automated interpreter job, but
humans would no longer hand-write each cascade-source EDN. Interpretation
remains attributed judgment, visibly distinct from mechanically observed facts.

1. Select a stated want with its exact source span and closure observation.
   Capture it and retrieved pattern bytes using `prepare-proposal!`; retain
   rank solely as search evidence. Record all considered/rejected matches and
   their reasons. Use the actual 40/8 retriever settings as search bounds, not
   as evidence of completeness or applicability.
2. Ask the interpreter for cited readings, scope limits, guards, intended
   effects and locators. Require independent observations for initial facts;
   unknown remains unknown. Pin every proposed token's meaning, and reject
   unlocated or unsupported effects. Preserve the distinction between a
   checkable artifact's presence and a claim about that artifact's correctness.
3. Validate and freeze an attributed interpretation before construction. Reuse
   the strict source/citation checks in `interpretation-evidence`, but introduce
   a preselection identity/schema: today's `interpretation-job/run!` requires
   an already authorized attempt and flat mission/ticket action, and the runner
   calls it **after selection**. Passing a fabricated attempt or relabeling a
   cascade candidate as an authorized flat action would violate that contract.
4. Construct a family from those interpretations. Derive edges where one
   pattern produces a token needed by another, and use `construction-moves/
   order-by-need` plus the bounded `construction/construct` policy with declared
   budget, costs, horizon and G evaluator. These functions already exist.
   `cascade-policy/candidate-space` can fold and organise orders, but accepts
   interpretations as input; it does not solve their absence. Its default
   subset enumeration refuses above eight interpretations, so use bounded
   explicit orders, not an unbounded powerset of 48 retrieval results.
5. Emit nonempty orders with actual construction receipts and per-pattern
   interpretation receipts into the existing source-loader/assembly/admission
   path. Record each refusal and count at each boundary. Do not set authority
   to `:agent-authored-declaration` for a machine-produced artifact without
   extending that provenance vocabulary and its validation. Do not overwrite
   an operator declaration: add an explicit source merge/authority rule.

For a meaningful two-pattern case, interpret P as producing an independently
observable prerequisite `q`, and Q as requiring `q` and producing the stated
want `w`. Initial observation lacks both. The resulting order `[P Q]` must
actually execute both model steps and predict `w`; `[Q]` must not. This is a
fixture design, not a claim that two current library patterns have already
been justified for the updater task. Choose and cite that real pair during
implementation. Do not pad a singleton with an unrelated pattern merely to
satisfy length > 1. With initial T=2 this example fits the declared horizon.

Estimated scope for a first reviewed packet (not a commitment): one new
preselection interpretation/constructor namespace (~250–400 lines), a strict
preselection envelope/adapter in `interpretation-evidence`/`interpretation-request`
(~100–180), source and proposal admission plumbing in `cascade-sources`,
`cascade-proposals` and `war_machine` (~100–160), and tests/retained fixtures
(~250–400). Approximately 700–1,140 lines across 6–9 code/test files plus
fixtures. Reuse `construction`/`construction-moves` rather than change G or
relax existing gates. The schema conversion is substantive: current receipt
guards support Boolean expressions/effects, while cascade sources use
needs/forbids and add-only produces. Unsupported expressions must be refused
or supported by a separately reviewed semantic extension, never dropped.

First honest test: a frozen real mission want and two captured real flexiargs,
real temporary Git commits for C3/C4 observations, and a recorded automated
interpreter response. Drive proposal → validated interpretation → construction
→ real loader/assembly/admission → model fold in one isolated process. Assert
at least two distinct patterns act and only their composed effects reach `w`.
The positive test must fail on this base because proposals cannot populate
interpretations/candidates. Then replace the high-ranked match's locator with
an unresolved commit, corrupt a source hash, and remove the producer of `q`:
these must respectively refuse/retain unknown, reject evidence, and fail to
reach `w`—never manufacture a fact. Use the real observation checks, not a
stub that always returns true. Also test an unrelated rank-1 proposal staying
unadmitted. A separately captured, bounded real interpreter run is required
before claiming automatic interpretation works; replay alone tests the
plumbing and validator, not the interpreter's reliability.

## Reproduction and validation

Environment: Babashka 1.13.219; OpenJDK 21.0.11; Clojure 1.11.1 from `deps.edn`.
No production or test code changed, so code gates/test warrants are not claimed.
The read-only census and isolated assembly completed with exit 0; `git diff
--check` is the document gate. The following reproduces the main record counts
without running WM code. Save as `/tmp/fix5-records.clj`, then
`bb /tmp/fix5-records.clj`:

```clojure
(require '[clojure.edn :as e] '[clojure.java.io :as io])
(with-open [r (java.io.PushbackReader.
               (io/reader "/home/joe/code/futon2/data/wm-trace/wm-trace-2026-09-21.edn"))]
  (loop []
    (when-let [x (e/read {:eof nil} r)]
      (let [a (:cascade-problems x)
            ps (:problems a)
            orders (mapcat #(get-in % [:cascade-problem :precedences]) ps)
            supply (:proposal-supply a)
            run-record (e/read-string (slurp (str "/home/joe/code/futon2/data/wm-runs/tick-run-record-"
                                                 (:run/id x) ".edn")))
            posterior (keys (get-in run-record [:decision :selection-law :posterior]))]
        (prn {:run (:run/id x)
              :targets (+ (count ps) (count (:refusals a)))
              :problems (count ps)
              :refusals (frequencies (map (juxt :kind :missing) (:refusals a)))
              :interpretations (reduce + (map #(count (get-in % [:cascade-problem :interpretations])) ps))
              :orders (frequencies (map count orders))
              :scored (count posterior)
              :lengths (frequencies (map #(count (:precedence %)) posterior))
              :proposals (frequencies (map :origin (:proposals supply)))
              :proposal-admissions (when supply (count (:admissions supply)))
              :drops (frequencies (map (juxt :stage :reason) (:dropped-candidates a)))}))
      (recur))))
```

To reproduce the limited current-file assembly probe, save the following as
`/tmp/fix5-dry.clj` and run `clojure -M /tmp/fix5-dry.clj` from this worktree.
All supply is immutable in-memory EDN, no mutable store is called. Current file
inputs and Git HEAD observations can change; these are not historical replay.

```clojure
(require '[clojure.edn :as e] '[clojure.java.io :as io]
         '[futon2.aif.mission-registry :as registry]
         '[futon2.aif.mission-hole-wants :as wants]
         '[futon2.aif.cascade-sources :as sources]
         '[futon2.aif.cascade-problems :as problems]
         '[futon2.aif.cascade-proposals :as proposals])
(let [trace (with-open [r (java.io.PushbackReader.
                           (io/reader "/home/joe/code/futon2/data/wm-trace/wm-trace-2026-09-21.edn"))]
              (e/read r) (e/read r) (e/read r))
      _ (assert (= "2026-09-21-1789964661" (:run/id trace)))
      supply (get-in trace [:cascade-problems :proposal-supply])
      missions (:missions (registry/load-missions-from-files "/home/joe/code"))
      declared (sources/load-declared "resources/wm/cascade-sources")
      merged (sources/with-context-fn
               (wants/merge-into-sources declared "/home/joe/code" missions :WM))
      targets (vec (distinct (concat
                 (map :id (registry/open-missions {:missions missions}))
                 (keys (:universes merged)) (map :target (:proposals supply)))))
      a (proposals/record-supply
          (problems/assemble {:targets targets :sources (assoc merged :horizon-steps 2)})
          merged supply)]
  (prn {:targets (count targets) :coverage (:mission-hole-coverage merged)
        :source-targets (count (:universes merged))
        :interpretations (reduce + (map #(count (:patterns %)) (vals (:interpretations merged))))
        :source-candidates (reduce + (map count (vals (:candidates merged))))
        :problems (count (:problems a))
        :constructed (reduce + (map #(count (:constructed-candidates %)) (:problems a)))
        :refusals (frequencies (map (juxt :kind :missing) (:refusals a)))
        :drops (frequencies (map (juxt :stage :reason) (:dropped-candidates a)))}))
(shutdown-agents)
```
