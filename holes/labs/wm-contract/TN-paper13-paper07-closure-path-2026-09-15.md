# Minimum closure path: paper-13 configuration / paper-07 live depth

Read-only discovery, codex-26 for claude-20, 2026-09-15 04:42Z.
No reload, click, environment/configuration edit or application test. Only this
note is written. Leave cohort-57 / wm-click-47248e0c to finish.

## Findings and present values

The missing posterior reader is **war_machine.clj in scripts/**, not src.
The normal path is HTTP `handle-wm-click-start` → runner-service `run-click!`
→ full-loop `run-opportunity!` / `selection-judge` → `generate-war-machine`
→ `judge`. The HTTP allow-list carries no horizon/detail/FPI options; moreover,
`selection-judge` forwards only `:accumulate-strategic-habit?` plus its fixed
selection callback. Passing arbitrary judge options in a click does not work.

| Setting | Actual reader / effective behavior | Present evidence; one-click override today |
|---|---|---|
| Anticipation horizon | `policy_depth.clj:13–31`, called by `war_machine.clj:6015,6376–6381`: explicit judge `:policy-depth` wins, otherwise RUN_CONFIG file; loaded nonempty anticipation events permit requested depth (default 3), otherwise horizon nil / depth 1. | PID environment has no RUN_CONFIG; current consumer value is **not exposed**. Canonical source has zero upcoming events, so checked source predicts nil/1. No working normal-click override. |
| Previous-tick policy details | `trace.clj:67–73` load-time `*persist-policy-trace-details?*`; used at `:178,295,845`. | PID env = 1. Expected loaded value true, **not independently read from JVM**. No click override. |
| FPI dark | `war_machine.clj:187–192` load-time `*f-pi-dark?*`. | PID env = 1. Expected loaded value true, **not independently read from JVM**. No click override. |
| FPI posterior | `war_machine.clj:220–239` load-time `*f-pi-posterior?*`; `f-pi-posterior-opts:643–722` refuses flag-off or incomplete coverage. | Env absent. Cohort-57 attempt-001 selection explicitly records absent/flag-off/applied false (148/149-candidate envelopes). This verifies off at that selection, not a direct current Var read. No click override. |

`/proc/1942869/environ` was read for these keys only (also BETA_DARK=1).
Environment is not proof of loaded Vars after namespace changes. No permitted
read-only endpoint found here exposes all four current consumers. RUN4's
existing `run4-effective-environment/attest` reads loaded Vars without reloading,
but covers only details/dark/beta, not posterior or horizon; its trusted click
route validates rather than changes them. RUN4 admits `:policy-depth` in pinned
options but the normal runner judge drops it. It is not a working shortcut.

**Additional real prerequisite:** `anticipation.clj:47–61,124–147` selects only
future events within 30 days from `~/code/calendar/events.edn`. Pure EDN
readback at 04:42:48Z found four events dated May 19/26/28 and June 8; zero in
that window. An independently legitimate current anticipation record is needed;
no invented event solely to make the gate fire. Default depth is already 3
when events exist, so an explicit horizon knob is not itself necessary.

## Smallest record change and ordered packets

1. **One effective-configuration map, not a new evidence subsystem.** In WM
   `judge`, retain actual consumer values alongside its existing `:horizon-steps`
   and `:policy-depth-used` (already returned at `:6895–6908`). Suggested map
   `:effective-run-configuration`: requested depth/source, effective horizon,
   effective depth, anticipation source hash/eligible-event IDs, details/dark/
   posterior booleans, and posterior applied/coverage/refusal from the actual
   decision. Include run identity and loaded-code identity when available.
   Full-loop selection checkpoint construction (`:3820–3877`) and `close-core!`
   (`:3353`) must carry this SAME map to every close, including containment;
   pre-judge failures retain loaded configuration with evaluation `:not-reached`,
   never invented effective depth. The initial time-cell at `:3280` may retain
   loaded flags, but `arena-mode-flags` alone cannot know per-tick horizon.
   **Acceptance:** production-shaped success and failure closes preserve the
   map; absent events give nil/1, real eligible events give depth >=2; env/Var
   disagreement reports the consumer value; incomplete FPI stays unapplied.
   Files: war_machine.clj, full_loop_runner.clj, their focused tests; trace.clj
   only if the map is also carried into daily traces.
2. **One reviewed activation, after the occupied click finishes.** Under the
   existing environment-driven design, enabling missing FPI_POSTERIOR requires
   a **JVM restart with Joe's authorization** (or a separately authorized code
   reload/rebinding mechanism, which this packet neither needs nor performs).
   Exporting in another shell cannot alter this JVM. Bundle packet 1 activation
   with posterior=1; preserve details/dark and existing tau/precondition gates.
   Have the proper owner supply legitimate anticipation data. Optional explicit
   depth uses RUN_CONFIG at restart; no HTTP option-plumbing is needed on the
   minimum route. **Acceptance:** loaded-consumer readback agrees with intended
   settings and the actual judge reports horizon >=2, not merely config=2.
3. **Bounded live record, then existing witness.** Run a reviewed machinery
   click after activation; if previous-policy details do not cover the complete
   current field, retain that refusal and use only the predeclared bounded
   continuation—no zero substitution or convenient candidate filtering.
   **Paper-13:** closable on retained effective required settings, with actual
   complete FPI coverage demonstrated rather than promised. **Paper-07:** retain
   the actual `efe/compute-efe` multi-horizon branch inputs/results (`efe.clj:633–640`)
   and build/review the existing machineDepth witness against this live record;
   a configuration stamp alone is insufficient. No further restart for this
   read-only witness packet. Reuse row-15 depth proof machinery; no new checker.

## Paper corrections now

The blanket wording that the settings are all “not enabled” is too broad:
details/dark are already in the process environment, while posterior is absent.
“Never fired live” is not established by the current retention gap; say
“no admitted live depth>=2 witness” until the scoped records establish more.
Do not turn either flag green from env declarations. The two flags can close
from the same configured live acquisition; they do not require measured-A
completion or a whole qualifying certificate.

## File/line authority and byte pins

These are checked filesystem bytes, not a claim of identical loaded code.
Cohort-57 selection was read as EDN and only the relevant fields printed;
calendar was parsed read-only with dates/counts printed. No gates rerun.

| Source (repository-relative unless absolute) | Lines | SHA-256 |
|---|---|---|
| `scripts/futon2/report/war_machine.clj` | 187–239;303–333;643–722;1013;6015;6376;6895 | `50d02bb7179087597f468f361e16206d93400b1e410e27ce1c261bdde801981f` |
| `src/futon2/aif/policy_depth.clj` | 13–31 | `24fba07ef6458570ddd7e5f4dd3b4bc6ec0b49432ead8af06b51e29e0d369786` |
| `src/futon2/aif/trace.clj` | 58;67–73;711–716 | `858a4ae3a837fa4755da1c307732bd4b7b887eff3e04bd2d97e139cc70afb356` |
| `src/futon2/aif/anticipation.clj` | 24;47–61;124–147 | `98af935397bff5b5a4d6f5e547ac879681bc7acf6686149c744cbd472961c06b` |
| `src/futon2/aif/efe.clj` | 633–640 | `1dd3b83d0c78a8388889d35c32a6bde12cd9ee55eed5ce8c0b570f3005fd1b83` |
| `src/futon2/aif/full_loop_runner.clj` | 3280;3353;3592;3735–3751;3820–3877 | `7da51274e48ab5b9c5fd6a2b3051b9ac4ee8253f906bf6e9ccd7bb494401ab9c` |
| `/home/joe/code/futon3c/src/futon3c/transport/http.clj` | 8642–8710 | `7c5c3a11d6684e8a05447380e5330f8a0409f6a5483e5519e066ba47d6ff460f` |
| `/home/joe/code/futon3c/src/futon3c/wm/runner_service.clj` | 153–163;412–428 | `17210a48730ab71bb6b035d8975765ddf0e00576470ca1f6577d58dd21d54e40` |
| `/home/joe/code/futon3c/src/futon3c/wm/run4_effective_environment.clj` | 4–10;20–65 | `57e716a0b89c0a2639c1bc5ac3db3b0c4daf5d2e4979d0b6b8f019cfcf156c6c` |
| `/home/joe/code/futon3c/src/futon3c/wm/run4_pinned_run_config.clj` | 14;50–60 | `316280ba6867a9d2638227cdbed14f821d1be77274ea052aaa77ad88117d9108` |
| `/home/joe/code/futon3c/src/futon3c/wm/run4_trusted_entry.clj` | 129–174 | `f8afaa24ff93a52f9ef8a041a08cd12325dbf9183d2ad3706ac20e78bb0648cf` |
| `/home/joe/code/calendar/events.edn` | 29;78;163;218 | `aa9506d5bb83eebc016788e027946e1a9e9a1147ce5524075b05272a4d1924b8` |
| `data/wm-full-loop-machinery-57/wm-contract-machinery-57-v1/attempt-001/002-selection.edn` | 1 | `7eff45e396c7cee04576e8e1c0e7f9a9c9ee73e83da969cd980f8f6cb99e0c54` |
| `holes/labs/wm-contract/runs/row-15-depth-proof-2026-09-12/verification-receipts.edn` | 2–19 | `e33b9619a496ad478e909003206929b391d6faf370486d185184b168fe855996` |
