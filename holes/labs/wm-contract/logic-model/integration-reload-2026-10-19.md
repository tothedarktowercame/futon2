# Integration reload readiness — source inventory (2026-10-19)

## Decision boundary and scope

This is a **read-only, source-based readiness inventory**, not a reload
certificate.  It was read from canonical futon2 commit
`6d5a3bf7de638d60c4445fcc367d5c8af95ea178` on 2026-10-19.  It covers the
reload-relevant runtime changes named by the P00–P29/P33 review ledger, the
idle reload controller, and the P08 cross-repository publisher handoff.  It
does not enumerate unrelated application namespaces, inspect a running JVM,
or establish that any listed source is loaded.

That boundary matters under the frontier ruling: an absent measurement must be
recorded as absent rather than defaulted to a numeric success, and a serving
JVM—not a fresh test JVM—is the only place a route can be measured
([frontier ruling](../RULING-frontier-is-measured-not-pass-fail-2026-10-19.md),
lines 28–45).  No serving API/eval, reload, launch, click, test, or ledger
operation was performed for this report.

## Checkout and historical evidence

* **Canonical checkout:** `git status --short` reported one untracked file,
  `holes/labs/wm-contract/wm-click-resource-20261015T165800.receipt.json`.
  Therefore the checkout is not pristine.  This observation does not say that
  a runtime source is dirty, and it does not say what the serving classpath has
  loaded.
* **Historical, not current-live evidence:** the review ledger records merge
  `57ae36409` returning `:ok true` for runner and loop-node namespaces from
  main, with the explicit limit that later commits and other consumers were
  not proved ([PACKETS](PACKETS-2026-10-19.md), lines 54–66).  It also records
  P09 as approved at `452a37c26`/`7ea8150f4` and P08 as approved in futon2 at
  `01a688786`/`bcea02d28`/`8ea0026ed` before the futon3c miner work
  `cd8701d93`/`3c89fdc9` (lines 838–846).  Neither statement is evidence that
  commit `6d5a3bf7` is loaded or that a current run succeeds.

## What the reload controller can actually do

`scripts/wm_click_repair_loop.py` pins its source roots to futon2 `src` and
futon3c `src/futon3c/wm` (lines 60–87).  `changed_click_files` asks each
repository for committed Clojure changes since the JVM start epoch (lines
186–194); it is not limited to the source-identity inventory.  For each
candidate it:

1. excludes a file with uncommitted changes in its owning checkout (lines
   262–264 and 307–311);
2. orders only the changed candidates by direct namespace requirements (lines
   267–286); and
3. evaluates `(require ... :reload)` one namespace at a time through the
   configured futon3c `proof-eval.sh` bridge (lines 67–69 and 312–325).

Thus P09's inventory is useful attribution, not a dependency graph or an
eligibility veto: `reload_plan` returns every changed source file and merely
adds declared identity names for paths it recognizes (lines 226–259).  This
matches the packet's stated requirement to retain changed family-selection
reload even when the runner did not change ([PACKETS](PACKETS-2026-10-19.md),
lines 309–324).

The no-mid-flight rule is real but qualified.  On a running click,
`reload_changed` returns `(None, "click-in-flight", same-epoch)` and makes no
reload call (lines 296–305); `reload_when_idle` waits until it receives a
non-`None` result (lines 328–334).  The retained exception is a **named
debugger-held run**: reload is permitted when `debugger_stop(held_run_id)`
confirms that stop (lines 296–300, 355–373), after which a successful reload
may retry the held phase (lines 407–423).  It would be incorrect to claim that
all active statuses prohibit reload.  In normal startup, `run` obtains the
JVM epoch and completes `reload_when_idle` before it can enter `fire` (lines
837–861); that is source control flow, not evidence that it occurred today.

## P00–P29/P33 reload-relevant namespace census

The list below is the changed runtime surface identified from the packet's
landed commits and their current direct requirements.  “Declared” means present
in `load-identity/required-sources`; it means the runner can report a
loaded-versus-disk digest if the namespace has registered, not that it is
present in the JVM.  The declaration itself warns that unregistered entries
remain `:unregistered` rather than silently disappearing
([load_identity.clj](../../../../src/futon2/aif/load_identity.clj), lines
10–17, 45–103).

| Packet/change surface | Current namespace and direct route | Reload discovery / identity attribution | Source pin and limit |
|---|---|---|---|
| P00/P07/P10/P11/P14 terminal and selection retention | `futon2.aif.full-loop-runner`, plus `trace`, `assessed-policy-basis`, `selected-outer-cascade-policy`, and `terminal-publication-receipt` | Runner, trace, assessed basis and selected policy are declared; a committed change in futon2 `src` is discovered regardless. | The runner identity report compares load-time source digests with canonical bytes and expressly excludes bytecode identity, partial loads, and later Var mutation ([full_loop_runner.clj](../../../../src/futon2/aif/full_loop_runner.clj), lines 636–663). |
| P01/P02/P03–P06 outer evidence/objective path | `d-predecessor-task-authority`, `outer-policy-provider-runtime`, `outer-policy-evidence`, `outer-policy-objective`, consumed by `meta-live-outer-selector` | The selector is declared; individual outer-provider/evidence/objective modules are not listed in `required-sources` lines 18–103, so changed files remain reload candidates but are absent from the attributed identity list. | Current selector requires the score/belief, evidence, objective and family-selection route ([meta_live_outer_selector.clj](../../../../src/futon2/aif/meta_live_outer_selector.clj), lines 8–25). This is an attribution gap, not proof of missing runtime loading. |
| P08 graph publication / reader | `futon2.aif.pattern-graph-pin`, consumed by `futon2.aif.wm.family-selection` | Family selection is declared; graph pin is not. Both are under futon2 `src`, so either committed source is discoverable; only family selection appears in the P09 attribution census. | `family-selection` directly requires graph pin and calls `load-pinned` ([family_selection.clj](../../../../src/futon2/aif/wm/family_selection.clj), lines 7–16, 136–155). The ledger limits P08 to manifest-aware readers: `pattern-graph-view` still calls `load-unpinned` (line 66) and `scripts/unblocks_relation.clj` still directly reads a pin (line 179). Those raw readers are not migrated or made coherent by a JVM reload. |
| P09 deployment boundary | `load-identity` and the Python reload loop; explicit family-selection dependency | `load-identity` itself is under futon2 `src`; declared paths include runner/runtime, meta selector, family selection, pipeline selector and tripwire. | Source inventory parsing is bounded to `required-sources` (reload loop lines 197–247). It cannot prove that all declared sources registered or that consumers accept a reload. |
| P15–P17b diagnosis / proposal and P18 budget observations | `full-loop-runtime`, `meta-self-heal-production`, `meta-field-observation` | Runtime is declared; meta-field observation and meta-self-heal production are not declared in the current map. All are still eligible by changed-file discovery. | P18's landed changes include runtime and field observation in the packet history; no current serving route was invoked for this inventory. |
| P19/P20/P29 selection measurement and advisory score separation | `meta-field-observation`, `meta-pipeline-selector`, `meta-live-outer-selector`, `outer-target-score`, `outer-target-belief` | Meta selector and pipeline selector are declared. Field observation, target score, and target belief are not declared, but fall within discovery when committed. | The current selector requires field observation, pipeline selector, outer target score, and family selection (meta selector lines 8–25); target score requires target belief ([outer_target_score.clj](../../../../src/futon2/aif/outer_target_score.clj), lines 1–4). P29 review records a known broader selector test failure, so source presence is not compatibility evidence ([PACKETS](PACKETS-2026-10-19.md), lines 823–835). |
| P21/P22 incident coverage / tripwire | `futon2.aif.tripwire` | Declared and discoverable if changed. | Current tripwire requires full-loop-cohort, store lock, repair, telemetry and trace ([tripwire.clj](../../../../src/futon2/aif/tripwire.clj), lines 1–21); this inventory did not inspect delivery configuration or send anything. |
| P23–P28 documentary/model readings | no new reload requirement established here | N/A | The review ledger treats these as reconciliation/documentation packets; this report does not infer runtime work from prose. |
| P33 outer outcome observation | `outer-target-belief`, downstream of `outer-target-score` and the selector | Both belief and score are discovered if committed but are absent from current identity attribution. | P33 is approved only on synthetic producer/consumer evidence; exact L2 and serving behavior remain unverified ([PACKETS](PACKETS-2026-10-19.md), lines 849–857). |

The `required-sources` snapshot also names `full-loop-runtime`, the two meta
selectors, family selection, futon3c runner service, and tripwire
([load_identity.clj](../../../../src/futon2/aif/load_identity.clj), lines
45–103).  That is the concrete loaded/disk reporting scope.  It is narrower
than the committed-source candidate set by design.

## Cross-repository and signature risks to resolve before a probe

1. **P08 publisher is not a reload target.**  `scripts/publish_pattern_graph.clj`
   is outside `CLICK_PATHS`; the futon3c miner commits cited above are in a
   different repository, and `CLICK_PATHS` includes only its
   `src/futon3c/wm` tree.  A publisher/miner deployment therefore needs its
   own checkout/commit verification before a reader probe.  This is a source
   boundary, not an assertion that either checkout is wrong.
2. **Identity attribution is incomplete for several landed dependencies.**
   `pattern-graph-pin`, `outer-target-score`, `outer-target-belief`,
   `meta-field-observation`, `meta-self-heal-production`, and the outer
   evidence/objective modules are absent from the explicit map shown above.
   They will not be represented in `source-identity-dependencies` despite
   remaining reloadable when committed.  No source shows an incompatible
   function arity at the reload call: the loop always uses Clojure
   `(require namespace :reload)` (lines 312–315).  Compatibility of a changed
   namespace with already-loaded callers is unmeasured.
3. **The runner's refusal authority is intentionally narrow.**  Only its own
   `:drift` is refusal-authoritative; broader displacement is report-only
   ([full_loop_runner.clj](../../../../src/futon2/aif/full_loop_runner.clj),
   lines 636–681).  A successful runner identity response cannot certify every
   dependency in this census.

## Coordinator review: proposed zero-click probes (do not execute from this packet)

These commands are proposed for review and must be run only against the
configured serving JVM with its debugger attached.  They are ordered to avoid
launching a click.  They are not evidence merely because they appear here.

```sh
# 1. First reject a dirty source deployment.  Review output; do not infer a
# clean tree from this packet's earlier untracked non-source receipt.
git -C /home/joe/code/futon2 status --porcelain -- src
git -C /home/joe/code/futon3c status --porcelain -- src/futon3c/wm

# 2. Confirm the exact canonical revisions that the serving-side identity
# report is expected to compare against.
git -C /home/joe/code/futon2 rev-parse HEAD
git -C /home/joe/code/futon3c rev-parse HEAD

# 3. Via the already configured proof-eval bridge, request only source-state;
# no wm_click.sh, no loop `run`, and no /click endpoint.
/home/joe/code/futon3c/scripts/proof-eval.sh \
  '(futon2.aif.full-loop-runner/runner-source-drift)'

# 4. If (and only if) the report identifies stale named namespaces, use the
# reviewed idle reload controller rather than hand-editing its ordering.  The
# coordinator must first verify no click is running; this packet does not
# authorize running the command.
python3 /home/joe/code/futon2/scripts/wm_click_repair_loop.py --help
```

The fourth line is intentionally only a CLI-surface review: `run` itself
obtains a start epoch and can eventually invoke `fire` (reload loop lines
837–875), so it is not a safe substitute for a source-only serving probe.
Before any actual reload, the coordinator should resolve the incomplete
identity-attribution list above and record the target futon3c miner revision.

## Limits and proposed model update

**Observed:** canonical checkout dirtiness, static controller behavior, current
declaration membership, direct `:require` relations, and historical ledger
claims.  **Not observed:** serving JVM start epoch, click-running state,
debugger-held state, loaded digests, dirty state of the serving checkouts,
actual reload results, graph publication, or a click.  No count of loaded
namespaces is reported because no such source-derived count would mean
“loaded.”

Proposed model/ledger wording for coordinator review (no shared table edited):

> **Reload readiness, 2026-10-19:** P09 discovers committed Clojure files
> beneath futon2 `src` and futon3c `src/futon3c/wm`, excluding dirty files,
> and reloads them only while idle except for a named debugger-held stop.  Its
> `required-sources` list attributes a bounded subset of candidates; absence
> from that list is neither zero nor an eligibility veto.  P08 publisher/miner
> deployment and raw graph readers remain outside this reload guarantee.  At
> source-read time the canonical futon2 checkout had an untracked
> documentation receipt; no serving identity or current-live reload was
> measured.
