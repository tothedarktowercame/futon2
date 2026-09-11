# U88 trial-1 packet — activated mission / exact task and series pins

This revision supersedes the original proposal at `50192c5e`; those bytes and
their pins remain in git history. It declares the three serving-JVM
requirements and the RUN4-scoped single-level model. The recording flag is a
requirement on the separate `wm_step_observe.bb` process; this packet does not
claim that the series invokes or attests that process.

2026-09-11 activation update: Joe explicitly directed OPEN activation and exact pin freeze. The canonical mission is now in ordinary discovery; dispatch and serving remain unperformed, and validation still grants no launch permission. Coordinator corrections at
futon2 `db99f729` (identical replay no-op; conflicting event-ID refusal;
registry departure is not inbox-unavailable proof) are already folded into
the mission draft by that commit — its post-correction bytes
(sha256 `8fecf3da…`) are preserved as the historical basis. This packet pins the
activated canonical bytes (sha256 `8212ec99…`), so it cannot silently
drift from the reviewed fixture semantics. The original round-1 mission bytes
remain in git history; the revision record is `db99f729` itself.

## Files (this directory)

- `run-config.edn` — `:wm/run4-pinned-run-config-v1`, sha256
  `fcc70191f2fe0ecbdc345632a32f87b4e1287f7dff947545ad3ced9814358c56`.
- `task-pin.edn` — `:wm/run4-task-pin-v1`, sha256
  `9cf34ffcff3a78bbe2b60e5c8cbfa674887ff3c34de5a41c49ea57315b63b717`.
- `series-pin.edn` — `:wm/run4-series-pin-v1` (frozen, one trial, stop-rule
  `:attempt-each-once-even-after-fail-or-block`), pins the task pin.

## Ruled-flag → consumer mapping (authority: RUN4-config-2026-09-09.edn, ruled flags; U88-PREPARATION-BASIS.json)

| Ruled flag (authority line) | Sheet field | Consumer |
|---|---|---|
| C fold ON (`:ruled-outcome-c-enabled? true`, Item 23 A3) | `:c-fold {:enabled? true :seed … :kernel …}` | `futon2.aif.c-fold-config/resolve-opts` → `:ruled-outcome-c-enabled?`, `:seeded-c`, `:disposition-kernel`, `:c-fold-provenance` (futon3c.wm.run4-pinned-run-config materialized-fold keys) |
| beta habit both (`:beta-habit-in-both? true`, SESSION-model-choices §1 / Item 25) | `:runner-options :beta-habit-in-both?` | full-loop runner options (futon3c.wm.run4-pinned-run-config/runner-option-keys) |
| forward/strategic habit accumulation (`:accumulate-strategic-habit? true`, Item 22) | `:runner-options :accumulate-strategic-habit?` | same |
| policy depth anticipation 3 / cascade 5 (Item ruling 2026-09-09 §6) | `:runner-options :policy-depth` | same; **gap G3** below re effective tracing |
| hierarchy single-level RUN4-scoped (§2, 4bcda58e) | **not representable** — see gap G2 | config-only disposition in futon2 |
| recording contract `:wm/realized-recording-v1` (A4, env-gated) | **not representable** — see gap G1 | run-environment env flag |
| accept-red-guard refuse-on-red (A7) | **not representable** — see gap G1 | `wm_step.sh` env |
| strategic prior fixture E_S stands (Items 21a+22) | non-flag warrant; nothing to set | strategic selector keeps fixture E_S |
| `:FUTON_WM_MISSION_C :unset` (Item 19a) | correctly absent | flag-gated default |

## Concrete gaps (NOT dropped to fit the loader)

- **G1 — caller integration remains.** The strict loader now carries
  `:run4/serving-declaration` as pinned data. The trusted caller must compare
  its three requirements with both current environment and already-loaded
  consumer values before click. Codex10 separately owns that integration and
  the post-accept observer adapter; this proposal claims neither is complete.
  Historically, the
  (`futon3c.wm.run4-pinned-run-config`) admits only
  `#{:cohort? :window-days :accumulate-strategic-habit? :beta-habit-in-both?
  :policy-depth}` plus `:c-fold`. The ruled recording contract
  (`FUTON_WM_RECORDING_CONTRACT=1` + the three beta-habit evidence env flags),
  the accept-red guard (`FUTON_WM_VERDICT_CAPTURE`), and the RUN4-scoped
  hierarchy disposition are consumed by the run environment, not by any sheet
  field. Until the loader (or the runbook) carries them as pinned inputs,
  they are launch-wiring prerequisites, and the beta-habit flag's OWN required
  environment is not established by this sheet alone.
- **G2 — strict-loader enabled-c-fold double-resolution (defect found by this
  packet's tests).** `load!` wraps the injected read-text with a second
  parent-relative normalization while `c-fold-config/resolve-opts` already
  resolves seed/kernel refs relative to the config path's parent; with a
  config pin whose path has a directory component the enabled path
  double-resolves and refuses `:c-fold-materialization-refused
  {:cause :unreadable-source}`. The same sheet materializes all ruled C-fold
  flags through `resolve-opts` directly, so the flag is consumer-supported;
  the loader's reader-wrapping is the defect. Encoded as
  `futon3c.wm.u88-trial-packet-test/strict-loader-enabled-c-fold-gap-is-typed-not-silent`
  — the packet keeps C fold ON and does not bend the sheet to hide this.
  Fix belongs to the C-wiring/launch owner, not this packet.
- **G3 — policy-depth effective-consumer tracing.** The ruling itself notes
  `cascade_lane.clj:381` passes `:depth 5` and defaults are not effective
  values; the sheet pins 3/5 but verification that consumers actually read
  the pinned depth is launch wiring ("Wiring/test packet routed"), not done
  here.
- **G4 — `:cohort?` / `:window-days`** deliberately absent (no ruling seen for
  this trial); absent keys stay absent per the loader contract.

## Mission eligibility

The canonical mission at `holes/missions/M-u88-contextual-preferences.md` is
OPEN and discovered by the production mission registry. Its activation metadata
records Joe's 2026-09-11 instruction and the reviewed `db99f729` draft basis.
The task-pin validator and serving guardrail accept only the exact action
`{:type :advance-mission :target "M-u88-contextual-preferences"}`; alias-shaped
targets refuse. Validation remains non-dispatching and returns
`:launch {:permitted? false}`. The historical draft stays in the preparation
root and still parses as `:draft` in isolated parser tests.

## Staffing (proposed) + read-only availability check 2026-09-10 (~18:1xZ)

| Role | Agent | Registry status |
|---|---|---|
| author | zai-2 | invoking (active, this packet) |
| reviewer | codex-12 | idle — available |
| repair-reviewer / coordinator | codex-17 | idle — available |
| (excluded) | codex-16 | idle but excluded (busy with Joe's maths per direction) |
| (excluded) | codex-10 | invoking; terminal-reader work excluded |

Casting in the pins matches this table. No worker dispatched.

## Gates run

futon2: `clojure -M:test -m cognitect.test-runner -n
futon2.aif.u88-trial-packet-test` — 3 tests / 17 assertions / 0 failures;
`futon2.aif.u88-draft-mission-test` still green. futon3c: `clojure -M:test -r
futon3c.wm.u88-trial-packet-test` — 4 tests / 9 assertions / 0 failures.
clj-kondo 0/0 both files; check-parens OK both; git diff --check clean.
No credentials, no output/runtime stores, no live/service/ledger/registry
writes anywhere in this packet.
