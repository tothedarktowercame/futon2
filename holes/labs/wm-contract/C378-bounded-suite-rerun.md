# C378 — bounded futon2 and futon3 suite rerun

Date: 2026-09-01. The suites were submitted separately through
`futon3c/scripts/bg.py launch-test`, each with an explicit `--dir`.

## futon2

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch-test \
  'clojure -T:build ci' --agent wm-verbs --label c378-futon2 \
  --dir /home/joe/code/futon2 --window measurement
```

- job: `bounded-1788229029232-c378-futon2`
- 1,077 tests / 6,369 assertions
- one failure, zero errors
- inner exit 1; outer exit 125
- outer reason `repository-basis-changed`
- resource status `clean`; `pids.events:max` delta 0; peak 1,129

The repository moved from `2521a026…` to `fcce1faa…` during the run, so the
suite is not attributable to one futon2 basis. The single failed assertion was
`preemptive_repair_gate_test/build-gate-consumes-preemptive-repair-lints`.

## futon3

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch-test \
  'clojure -X:test' --agent wm-verbs --label c378-futon3 \
  --dir /home/joe/code/futon3 --window measurement
```

- job: `bounded-1788229139859-c378-futon3`
- 248 tests / 1,518 assertions
- one failure, zero errors
- inner exit 1; outer exit 125
- outer reason `test-failure`
- resource status `clean`; `pids.events:max` delta 0; peak 1,125
- futon3 basis stable at `d77b7a2c…`

The same shared `preemptive_repair_gate_test` assertion failed. Futon3's own
basis was stable, but that test scans the live futon2 corpus, which was moving
and is outside the single-repository receipt's basis.

## Attribution on the final observed corpus

A focused `gate-result` after both jobs returned a persistent, specific hard
finding:

```edn
{:repo :futon2
 :path "holes/labs/wm-contract/C284-verifiable-format-proof-and-gate.md"
 :line 42
 :finding :nonzero-finding-zero-exit
 :excerpt "the direct census was red while the historical report-only branch returned a clean exit"}
```

Thus this is not resource exhaustion and not an unexplained ordering flake.
The acceptance lint is interpreting C284's recorded contrasting exit examples
as a live nonzero-findings/zero-exit implementation. That artifact belongs to
the evidence/format-proof work; C378 reports it and does not modify or absorb
it.

No second full rerun was attempted: futon2 was still moving, and each run is a
heavyweight bounded suite. The attributed quiet-window run remains outstanding.

Delivery inventory:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
