# paper-05/09/11: minimum R20 → R14 closure path

Read-only discovery, codex-27, 2026-09-15 04:43–04:45 UTC; checkout heads
futon2 `9786f741`, futon3c `8d3c0a5e`. No reload, click, configuration change,
production snapshot acquisition, or test rerun. Only this note is written.

**Conclusion:** the fixed-half **snapshot constructor exists; the gain composer
and its production caller do not**. Thus paper-09's “no composer exists yet”
remains accurate for a trip→applied-gain composer. The DAG's composer
`:built-not-wired` label overstates the implementation found. These flags cannot
be closed just by changing that label or citing the sensitivity probe.

## Existing work and blockers

- **Trip source:** `13491e5a` built confidence; `7253ab33` repaired initial-history
  validation (retained 3 tests/24 assertions). `6fecd35e` added complete manifest
  capture; `2f39b95d` added controller-byte checks and no-create production locking
  (retained 17 tests/58 assertions, independently reviewed). `642ab3eb` subsequently
  repaired exception propagation/writer serialization; older receipts are not
  validation of those newer bytes. Production capture requires independent
  participation plus deployment/store locks [1–3]. Today both lock files exist,
  but `/etc/futon2/wm-interoceptive-participation.edn` does not. Controllers are
  explicitly `:not-lease-aware`/`:uncontrolled`; root-owned metadata alone cannot
  establish enforcement. Two required source pins also differ from current bytes:
  `interoceptive_store_lock.clj` and `repair_obligation.clj` [2].
- **History:** an unlocked, read-only EDN census found 173 repair-stage records,
  including 14 open→resolved pairs without an awaiting-validation stage. Example:
  `data/wm-repair-obligations/{findings,resolutions}/repair-attempt-002.edn`.
  This is a diagnostic, **not** a coherent production count. The current constructor
  rejects these transitions before computing confidence [1]. The missing
  participation/enforcement and logical history are separate blockers.
- **Composer/witness:** exhaustive symbol searches across futon2 source/scripts,
  futon3c source/dev/scripts and futon3a/3b/3 found no production consumer of
  `:machine-confidence` or implementation of the two modulation statuses.
  `judge` passes the learned task gain directly to policy [4]. The retained
  147-candidate/four-multiplier probe is sensitivity evidence, not joint production
  trip→gain evidence [5]. No qualifying engineering witness found.

The paper needs one truthful production engineering edge, not calibrated optimality,
safer enactment, or a retrospective replay of every historical repair. Nevertheless,
**the existing admitted source path requires coherent complete authority and valid
history**: discarding awkward rows, inventing validation, or declaring a lease
“enforced” would bypass it. A smaller authority protocol would need its own reviewed
contract and controls; it is not a paperwork exemption available in this packet.

## Serving mode and headroom

Read-only evaluations of already-loaded vars in PID **1942869** returned
`:selection-gain-only`, beta-dark=true, Fπ-dark=true, and snapshot namespace
**not loaded**. `FUTON_WM_TAU_MODE` is absent; the dark flags do not select the
variational law. The latest canonical trace is **2026-09-12T17:28:09.498448087Z**,
not a fresh cohort-57 observation. Applying the loaded gain fold to that trace
returns **g=1.0**. A pure loaded-policy calculation gives τ(1)=1, τ(1/2)=2:
headroom over the **policy** floor 0.01 exists; recheck on the commissioning tick.
The confidence floor 1/2 and learned-gain bounds [0.5,2] are different quantities.

No restart or mode change is needed to use this already-selected engineering mode.
Other modes are selected by process environment, with no per-click mode override
found [4,6]. Separately, live selection law is **`:controller-head`**, Fπ-posterior
env is absent: changed weights are not evidence of changed action selection [7].
Report that limitation. Stronger choice-control claims require the separately
admitted full-score path; do not silently flip that law under row 18's ruling.

## Ordered implementation packets

| Packet / files | One-line acceptance | Deployment / click |
|---|---|---|
| 1. Establish source participation: activation/store-lock; futon3c restart, proof-eval, admin and direct evaluator boundaries [2,3]. Review and update source pins only after validating their bytes. | Real competing launch/reload cannot cross capture; expiry, inode/generation changes and missing receipt refuse without creating files. | Quiescent deployment after cohort-57; bootstrap mediation may need **Joe's restart**—no validated restart-free installation exists yet. No click. |
| 2. Reconcile canonical repair history: commitment/manifest and owning repair reader/writers [1,8]. | Every counted/excluded trip joins admitted history; unsupported legacy resolution refuses, never synthetic backfill or filtered confidence. | Evidence/reader packet; no intrinsic restart or click, though fresh successor authority may require a later permitted run. |
| 3. Build pure composer beside commitment and its focused tests [1,6]. | k=0/positive/repeated/resolved identities preserve the ruled factor; variational and saturated arms are explicitly non-qualifying; original task posterior unchanged. | Offline; no restart/click. |
| 4. Wire `judge` before selection and preserve the witness in trace/run records [4]. | Real-shaped snapshot→applied gain→τ/weights is retained; source refusals survive the broad selection fallback; authority/mode/floor/selection-law are explicit. | Reviewed canonical hot-load at quiescence under packet 1's controls; no intrinsic restart/click. |
| 5. Commission one eligible production selection after **cohort-57 / wm-click-47248e0c finishes**. | Genuine durable trip IDs and admitted snapshot pin join the actual composed gain/τ/weights and decision in one run; retain same-field reference arms and non-qualifying controls, with no stop-line bypass or claim that a counterfactual arm was a live zero-trip state. | **One lead-dispatched click**, not this discovery. Keep all three flags if only refusal/saturation/inapplicability is witnessed; otherwise update their bounded claims together. |

## File:line evidence

Paths below are relative to `/home/joe/code/`; ranges indicate inspected spans.

1. `futon2/src/futon2/aif/interoceptive_commitment.clj:10–19,52–94,109–169`;
   `futon2/test/futon2/aif/interoceptive_commitment_test.clj:31–69`.
2. `futon2/src/futon2/aif/interoceptive_activation.clj:16–57,171–237,332–370`.
3. `futon2/src/futon2/aif/interoceptive_manifest.clj:158–179`;
   `futon2/holes/labs/wm-contract/LEAD-PACKET-LOG-2026-09-13.md:404–432`;
   `runs/row-18-controller-resolution-2026-09-13/test-receipt.edn:1`
   (relative to that same wm-contract directory).
4. `futon2/scripts/futon2/report/war_machine.clj:6156–6177,6473–6485,6537–6550,6833`;
   `futon2/src/futon2/aif/trace.clj:685–686,1011–1039`.
5. `futon2/holes/labs/wm-contract/LEAD-DECISIONS-2026-09-12.md:39–85`;
   `runs/row-18-lead-audit-2026-09-12/execution-receipt.json:1` (same directory).
6. `futon2/scripts/futon2/report/war_machine.clj:898–925`;
   `futon2/src/futon2/aif/policy.clj:127–146`;
   `futon2/src/futon2/aif/selection_gain.clj:79–81`.
7. `futon2/src/futon2/aif/policy.clj:560–599`;
   `futon2/scripts/futon2/report/war_machine.clj:350–398`.
8. `futon2/src/futon2/aif/repair_obligation.clj:452–485,537–552`.
Paper anchors: `p4ng/sec-catalog.tex:302–305,482–484`;
`p4ng/sec-observation-plop.tex:41–42`. DAG: `futon2/holes/labs/wm-contract/runs/outstanding-dag-2026-09-14/dag.edn:101–106`.
