# Discovery: T3 injury `a59c85a6`

Date: 2026-10-28
Scope: read-only reconstruction of repair iteration 2. No serving-JVM evaluation, click, reload, ledger edit, or source edit was performed.

## Evidence pins

- Historical run: `data/wm-runs/tick-run-record-2026-10-27-c9d132d8-a2be-42b3-8369-fd23e9b9924f.edn`, SHA-256 `5888cb3876182a6655083ec9114c2799e719393eeeb0352e1a6f391f6ed7a900`.
- Durable trip: `data/wm-tripwires/trips/trip-8c194159-fcd5-44fa-9098-5656814dcb8e.edn`, SHA-256 `92899c36126accbcecd6aece4940a6473df5516717e72d0282204be615903b7c`.
- Repair-iteration receipt: `/home/joe/notes/wm-next/criteria-completeness/repair-iteration-2/receipt.md`, SHA-256 `9f035ca7c45d0f14b861788b75e82cec5fedd8e8e6ef0505caaa23e19955e49a`.
- Diagnosis: `/home/joe/notes/wm-next/criteria-completeness/repair-iteration-2/diagnosis.edn`, SHA-256 `fdcb2e7e5636b0be2f31c52dbcb21049607415250070b4d61b4ce9a7285775d8`.
- Proposed acceptance: `/home/joe/notes/wm-next/criteria-completeness/repair-iteration-2/acceptance-proposal.edn`, SHA-256 `5f0ee69aa7303fcf1ed77b0f98e9f57df970317e02e39e5a89a511839d1922f6`.

## 1. What T3 tripped on

T3 is registered as **“exit and stop-line completeness”** at `src/futon2/aif/tripwire.clj:95`. Its predicate is `t3` at `tripwire.clj:216-238`, and phase events reach it through `full_loop_runner.clj:370-378` (`emit-phase!` calls `tripwire/observe!`). For a cohort opportunity-end observation, T3:

1. treats every outcome other than `:grounded-change`, `:cohort-complete`, or `:historical-verification-awaiting-validation` as zero achievement (`tripwire.clj:221-227`);
2. rereads the repair store for the durable attempt id (`tripwire.clj:228-231`); and
3. emits `:missing-durable-stop-line` when the effective statuses contain neither `:open` nor `:superseded` (`tripwire.clj:235-238`). Effective status is computed from the immutable repair directories at `tripwire.clj:144-175`.

The retained trip's actual observation was:

```clojure
{:phase :opportunity
 :transition :end
 :outcome :incomplete
 :attempt-id "attempt-002"
 :external-attempt-id
 "ea1-65af74f037b64872be918c41c0925b043846507f8ab294945c53707dc54dbe9f--attempt-002"
 :cohort? true
 :repair-root "/home/joe/code/futon2/data/wm-repair-obligations"}
```

The repair lookup returned status set `#{}`. T3 therefore produced:

```clojure
{:kind :missing-durable-stop-line
 :attempt-id "ea1-65af74f037b64872be918c41c0925b043846507f8ab294945c53707dc54dbe9f--attempt-002"
 :statuses #{}}
```

The run record retains that witness under `[:failure :detail :tripwire/witness]`, records `{:absent :close-without-exception}` as the close-time cause, and has no `:repair-obligation`. The later run failure's outcome is `:tripwire-tripped`; that must not be confused with T3's input outcome, which the trip artifact proves was `:incomplete`.

The producer is the held-open-cascade-resume catch at `src/futon2/aif/full_loop_runner.clj:9013-9033`. It explicitly says no repair obligation is recorded (`:9014-9020`), then emits `:opportunity/:end/:incomplete` (`:9021-9026`). Those two choices jointly create exactly the T3 violation. The branch was introduced by commit `d9fb5ffecebf70bb9d392f393bc261b261998df1` (“injury 94091b05: cascade-resume typed :error holds the phase, never abstention”, 2026-10-23) and remains in today's source.

## 2. Reproduction against today's code

I loaded the retained trip with a separate `clojure -M -e` process and evaluated today's public predicate, without invoking `check!`, `observe!`, or any writer:

```clojure
(tripwire/evaluate-wire :T3 (:trip/observation retained-trip))
;; => [{:kind :missing-durable-stop-line,
;;      :attempt-id "ea1-65af...--attempt-002",
;;      :statuses #{}}]
```

Thus the condition is still producible by today's implementation, on the retained real inputs. This is not merely a stale historical classification. The current unit test at `test/futon2/aif/tripwire_test.clj:105-121` also states the right predicate-level contract: a zero-achievement end without an on-disk finding is red, and the same observation clears only after an open finding is reread. The historical branch supplies the red shape while deliberately omitting the finding.

No later commit corrected this producer ordering. In particular, ordinary close does record a failure before its opportunity-end event (`full_loop_runner.clj:6387-6434`, then `:7083-7091`), but the held-resume branch at `:9013-9033` is a separate early return and does not pass through that sequence.

## 3. Does the proposed acceptance test exercise T3?

No. The proposed namespace contains five tests and 33 assertions:

1. it verifies the pinned record and reads the already-retained witness;
2. it calls `cascade-decision/select-and-record-cascade!` with and without a task;
3. it calls `loop-node-exercise/require-complete!`;
4. it projects the historical failure through `terminal-receipt`; and
5. it projects a hand-built selected record through `terminal-receipt`.

It neither requires `futon2.aif.tripwire` nor calls `evaluate-wire`. Its alleged repaired value does not contain the T3 inputs (`:phase`, `:transition`, durable attempt identity, or repair root), does not create or reread an open/superseded finding, and does not exercise the held-open-cascade-resume branch. Consequently it passes unchanged before any repair, exactly as the repair-iteration receipt reports. Reading the old witness proves the incident existed; constructing an action receipt proves terminal projection works; neither proves the producer now establishes a durable stop line before the end observation.

The retained failing case above is sufficient to expose the gap: feed its exact observation to `evaluate-wire` against a hermetic repair root with no matching finding. The proposed test never does so and therefore cannot catch the defect.

## 4. Conclusion and discriminating acceptance

**Conclusion (a): the defect is still present, and the diagnosis's test is not discriminating.** T3 is behaving correctly. The inconsistency is in the runner: `full_loop_runner.clj:9013-9033` announces a zero-achievement opportunity end while intentionally leaving no durable `:open` or `:superseded` stop line.

A smallest discriminating regression should use the real held-resume producer path in the existing hermetic runner fixture:

1. cause the cascade-resume handler to return the typed error that becomes `::held-open-cascade-resume`;
2. retain the emitted opportunity-end observation and the hermetic repair store;
3. before a repair, assert the exact red result `[:missing-durable-stop-line]` from `tripwire/evaluate-wire :T3` (or assert the runner trips T3); and
4. after the repair, require that the same path has durably recorded an `:open` or `:superseded` finding for the qualified external attempt id *before* emitting the end event, then assert `evaluate-wire` returns `[]` after rereading that store.

The repair must reconcile the held/resumable state with T3's durable-stop-line contract. Disabling T3, exempting `:incomplete`, changing the observation to avoid the check, or merely testing terminal receipt projection would weaken the invariant and would not close this injury.

Because the predicate still fails on today's code, the “already corrected” ledger route is not applicable here.
