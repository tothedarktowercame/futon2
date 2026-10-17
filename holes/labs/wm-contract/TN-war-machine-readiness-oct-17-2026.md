# War Machine readiness campaign — 17 October 2026

## Campaign status

Prepared, not started. Joe authorized beta = 1 on 2026-10-17 as the neutral
starting precision for this five-click readiness campaign, whose purpose is to
make the system ready to tune. It is not claimed to be learned or calibrated.
The sealed declaration is
`resources/wm/reading-precision-beta/wm-readiness-2026-10-17-v1.edn`, bound to
reading-model identity
`f9c5cf02df9f933d2b554efbcb7b70ceb2ce1dddf595d35c0bd88245ae4186b2`, with
declaration digest
`72526b4b8b64dd084c67deb9903703b3290da0b494141c2e308a7dfb4ca898c0`.

Before click 1, the serving JVM must load and register that exact declaration,
then read it back by identity and verify the seal. Later clicks may carry beta
only while the reading-model identity is unchanged. An identity change pauses
the campaign for a new explicit declaration; neither the debugger nor the
runner may invent or silently migrate the value.

## Fixed Zai cast

Every click in this campaign will pass this cast explicitly; there is no roster
fallback:

- author: `zai-2`
- reviewer: `zai-6`
- repair reviewer: `zai-7`

At 2026-10-17T19:32:35Z all three Agency sessions were reset through the
`reset-session` boundary. Their prior session identities were discarded. The
post-reset roster reported all three as `idle`, `invoke-ready? true`, with no
session identity; first use will therefore create fresh sessions.

## Recording contract

For each of the five clicks append:

1. authorization and durable budget receipt;
2. exact cast and fresh session identities;
3. click, run, attempt, and selected target/cascade identities;
4. phase work time, wall time, and debugger dwell separately;
5. every held condition, repair dispatch, verdict, commit, reload, and restart;
6. run record, report card, requirement verdict, and publication paths;
7. whether a tick was carried and what grounded work changed;
8. unresolved defects and the evidence required before the next click.

The five clicks run sequentially. A debugger repair resumes the held phase of
the same click and must not spend another click or rerun outer selection. A
failure requiring operator authority pauses the campaign instead of repeating
the same predictable failure.

## Runs

### Click 1 — selection stopped by an obsolete/undeclared loop-node contract

- Authorization/load preflight: the five-click grant was recorded by raising
  the ordinary-click allocation from 81 to 86. The serving JVM reported five
  available clicks and read back the intact beta declaration for the exact
  identity above. Zai-2, Zai-6 and Zai-7 were reset immediately before issue.
- Click: `wm-click-454f8ade-720e-4ade-bec4-a0381b552aba`.
- Run: `2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492`, attempt 002.
- Selected target under construction: `M-recommendation-bindings`; one
  occurrence-keyed cascade with twelve pattern occurrences and validated
  four-map squares reached the selection gate.
- Terminal result: `build-failed`; no tick carried and no mission work was
  enacted. Run record:
  `data/wm-runs/tick-run-record-2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492.edn`.
  Report card:
  `data/wm-runs/2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492/report-card.md`.
- Timing: 563,598 ms wall time, comprising 164,084 ms active selection and
  369,049 ms debugger dwell. Within selection: meta-field ranking 76,863 ms,
  strategic judgement 54,836 ms, strategic scans 20,374 ms, G admission
  6,282 ms, declaration/interpretation loading 5,111 ms, and the remaining
  measured subphases below 0.2 s each.
- Held condition: `required-loop-node-unexercised`. R6, R13 and R14 were
  legitimately bypassed because this run had one admissible policy and one
  action group. CTAU-CLASS was refused because the current reading-policy
  scorer retains progress-preference evidence, whereas the old gate requires
  a `:class-emission` observation model from the retired class-scoring route.
- Debugger handling: Zai-7 diagnosed the held phase under job
  `invoke-1792266440527-16272-d984469f`, then confirmed the terminal record
  under `invoke-1792266841488-16282-3cc3a535`. Both returned
  `cannot-repair`; no code was committed or hot-loaded. The second dispatch
  was the loop seeing the same terminal failure after abort, not an independent
  defect.

Disposition before click 2: do not click again unchanged. Joe must choose
whether CTAU-CLASS is (a) re-declared for the reading route, (b) implemented by
adding a genuine class-emission model to that route, or (c) formally retired
from the registry, Lean contract and live gate. Independently, the scorer's
observation-model evidence is currently lost when `family-selection` replaces
the scorer certificate's `:f`; retaining that evidence is a real repair but is
not by itself sufficient to settle CTAU-CLASS.

### Click 1 repair disposition — CTAU-CLASS implemented, not waived

Joe chose option (b): the reading-policy route must carry a genuine
class-emission model. The repair makes the two observation channels explicit:

- the progress model supplies progress risk, ambiguity and transition
  information gain;
- the class-emission model emits `ending/not-yet-evaluated` before the horizon
  and, at the horizon, emits `focused` when the already-selected outer target's
  acceptance tokens were reached or `stop-the-line` otherwise;
- its terminal class risk and ambiguity are added to G. Transition information
  is consumed once from the shared rollout rather than counted twice;
- the model, its score certificate, and `consumed-in-g?` receipt survive the
  scorer → family selector → policy selector boundary as `:class-emission`.
  Xiang reading fit remains the distinct `:computed-f` carrier.

The launch gate now accepts CTAU-CLASS only when every compared candidate has
that explicit model, a numeric class risk, and `consumed-in-g? true`. Merely
placing a class-shaped map in a fit or certificate field does not pass.

The pre-click scanner now invokes the loaded production scorer's bounded
`class-emission-readiness` probe. A missing function, malformed class model,
or class receipt that does not contribute to G is reported as the predictable
`cannot-repair` blocker `reading-policy-class-emission`; it therefore stops
before a click or agent dispatch. Its negative control for an unconsumed class
receipt is part of the focused gates.

This changes the reading scorer from
`:wm/cascade-shape-g-score-policy-v1` to
`:wm/cascade-shape-g-score-policy-v2-class-emission`. Consequently the beta
declaration recorded above is not silently migrated: click 2 remains blocked
until an offline production-family replay yields the new exact reading-model
identity and a new sealed beta=1 campaign declaration is registered for it.

Offline repair evidence, commit `48e8ae2d0`:

- the scorer/preference/family/selector/CTAU focused gate passed 51 tests and
  268 assertions with no failures or errors;
- the readiness negative control rejected a class-shaped receipt whose cost
  was not consumed by G;
- changed-file lint had no new warnings (the sole warning in
  `full_loop_runner_test.clj:7109` is also present at the untouched base),
  Python compilation and `git diff --check` passed;
- the serving JVM was hot-loaded and its production scorer probe returned
  `:ready`, scorer v2, model kind `:class-emission`, `consumed-in-g? true`,
  with recorded G exactly equal to the independently recomposed G;
- the old all-of-`full-loop-runner-test` comparison was stopped after it
  reproduced the same ambient `:no-selection` failures at untouched base. It
  is intentionally not a readiness gate: it is slow, state-sensitive, and
  did not discriminate this repair. The bounded cross-layer gate above does.

The upgraded scanner therefore clears the former predictable
`cannot-repair` condition in the serving JVM. It still reports NOT-READY for
independent reasons: the latest recorded workspace/CI receipts do not name
this commit, the runner code receipt names an older tested commit, and the
reviewer seat was unavailable at the instant of the scan. These are launch
preconditions, not reasons to spend click 2.

### Click 2 launch cleanup

The launch bookkeeping was repaired without a broad suite rerun:

- readiness now consumes test-registry warrants for the seven namespaces in
  the changed execution closure. Four warrants remained fresh and were reused;
  only `full-loop-runtime`, `reading-policy-precision`, and
  `reading-family-decision` reran and minted new warrants;
- the old all-repository “everything conforms” receipts were removed from the
  click-launch predicate. They remain maintenance evidence, but unrelated
  repository commits and the pre-existing untracked resource receipt no
  longer invalidate an unchanged tested closure;
- serving-code readiness compares the loaded runner bytes with the current
  disk bytes. The serving JVM reports them equal and current;
- the WM campaign cast is now Zai-2 as author, Zai-6 as reviewer, and Zai-7 as
  repair reviewer. All three sessions were reset before assignment and the
  readiness scan requires all three to be idle and invoke-ready;
- Joe's beta=1 authorization is installed as sealed campaign authority
  `3387e9ed…f4464`, limited to four distinct reading-model identities—the four
  clicks remaining after click 1. Each use still creates and records an exact
  identity-bound declaration; there is no silent carry across model changes.

Final non-clicking scan: READY. Contract and trace-schema checks, all seven
warrants, serving runner bytes, CTAU class emission, Zai cast availability,
bounded service capacity, and the run-certification command all passed. No
click or agent dispatch was performed by this cleanup.
