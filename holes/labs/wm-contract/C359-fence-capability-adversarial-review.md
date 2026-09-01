# C359 — adversarial review of the shared fence capability

Date: 2026-09-01. Reviewer: `wm-evidence`. Assessment only; no capability,
consumer, wrapper, or receipt changed.

## Verdict

The production subprocess path re-observes the live world and therefore
rejects an unfenced world. However, the capability is not an unforgeable
in-process authority, and its prior-receipt validation accepts an observation-
free shell. Because every consumer now trusts this one value, these are shared
failures rather than isolated caller defects.

## Findings

### 1. A caller can recover the private token

Clojure privacy is advisory, not a security boundary. This caller-authored map
was accepted by `observed-held?`:

```clojure
(let [token (var-get
             (ns-resolve 'writer-fence-capability 'capability-token))]
  (writer-fence-capability/observed-held?
   {:schema :writer-fence-capability/v1
    :verified? true
    :status :observed-held
    :id "forged"
    :writer-fence-capability/token token}))
;; => true
```

Thus “caller-authored `:observed-held` maps no longer certify anything” holds
for ordinary data construction but not for an in-process caller. Once forged,
the same value satisfies workspace gate, contract authority, and mutable
read-set because all correctly delegate to `observed-held?`.

### 2. The public dynamic runner is a second minting route

`*run-evidence*` is public and dynamic. A caller can bind it to return exit 0
and a fabricated `FENCE-VERIFIABLE` report, call `verify`, and receive the real
private token from inside the namespace. This is the exact mechanism used by
the tests, and it is equally available to application code.

With that binding, a minimal prior receipt containing only verdict, fence IDs,
an arbitrary non-nil observation interval, and an attestation shell produced
`:status :observed-held`, no problems, and a capability accepted by every
consumer. No live-world observation was needed.

### 3. Prior receipt structure does not prove prior observation

The verifier does not require `classification.observed`, an observed writer
population, or a findings field. A receipt with no observations and no
findings was accepted when the evidence runner returned verifiable. Its prior
observation interval was `1900-01-01`, but no relationship between that
interval and the attestation interval or current observation was checked.

In the ordinary production path this does not create a false current fence,
because the subprocess receives the embedded attestation and independently
observes the live world. It does mean the “prior receipt” contributes no
authenticated historical observation: it is a transport envelope for the
attestation, not evidence in its own right.

### 4. Replay is bounded only by fresh re-observation and attestation expiry

An earlier receipt can be replayed while its embedded attestation remains
current. The capability ignores the age of the receipt observation but runs a
fresh world check; consequently replay cannot buy a pass after the world is
unfenced, and it cannot pass after attestation expiry because the live checker
revalidates issued/expiry bounds. This behavior is defensible if stated as
“fresh verification using a replayed attestation,” but it is not validation of
the old receipt's observation interval.

### 5. Wrapper input states are not fully distinguished

`run_workspace_gate_bounded.py` behaves as follows:

| Input | Result before submission |
|---|---|
| ID with missing evidence variable | exit 125, pair required |
| ID with empty evidence path | exit 125, same pair-required result |
| evidence without ID | exit 125, pair required |
| ID plus nonempty nonexistent/unparseable path | accepted for submission; inner capability must reject |

The wrapper is intentionally transport-only, so the last behavior does not
mint an event claim. It does mean “receipt present” is only truthiness at this
layer, and missing and explicitly empty are collapsed. An invalid nonempty path
consumes bounded admission before the inner process can classify it.

## Empty-input and unfenced controls

- A zero-byte or malformed JSON receipt returns unavailable and cannot mint a
  capability.
- A parsed receipt with no observation/finding population can mint one through
  the dynamic runner seam.
- With the real evidence subprocess, a syntactically convincing receipt in the
  unfenced world remains `FENCE-BREACH`; observed findings still dominate.
- With a rebound runner, `FENCE-VERIFIABLE` is reachable in an unfenced world.
  Therefore the production executable is fail-closed, while the in-process API
  is not.

## What held

- Ordinary caller-authored status maps lacking the actual token are rejected.
- All four named consumers route through the shared predicate; no parallel
  ID-to-held branch was found.
- The bounded wrapper rejects lone ID/evidence members and quotes both values
  when transporting them.
- A real current breach or expired attestation prevents the normal subprocess
  path from producing an observed capability.

## Inventory

The delivery inventory command is:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`. The
existing focused suite remains green: 10 tests, 46 assertions.
