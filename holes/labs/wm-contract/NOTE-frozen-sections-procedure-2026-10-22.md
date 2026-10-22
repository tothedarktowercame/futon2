# Develop from the last successful recorded section

The first working slice takes the frozen `752b48a7` selection through canonical
materialisation, recorded compilation, construction, wiring, and preparation of
an author/reviewer request. It does not execute the mission. Success at this
boundary means a concrete operation reaches the build prompt, with its completion
check still unchecked. Abstention is not an acceptance criterion.

## The defect repaired

The selected reading-family action carries a co-application map. The old
`:cascade-candidate` constructor treated its entries as a sequence of pattern
names. That produced an invalid fold with a policy hole missing `free`, `why`,
and `obligation`. The diagnostic chain reproduces `:fold-output-invalid` using
the actual production constructor and wiring functions.

The selected cascade already had a certified, attributed executable refinement
in `data/wm-executable-cascades/426fa181e58c73917635a5f5868dce56e55d0d35084f549404d2e3ae0f3e142e.edn`.
The gzip fixture preserves it. Recompilation from its recorded authoring
reproduces that digest exactly. The runner now carries those compiled clauses
into construction, the author prompt, and the construction checkpoint. It
checks the structural identity against the selected cascade; selection is not
rerun or changed. Execution requests also retain the compiled node's pattern
name (previously they only looked in the older nested node representation).

For this fixture the result is one box, zero policy holes, and the operation
`:declare-mission-lifecycle-state` for `futon-theory/mission-lifecycle`. Its
completion check requires the mission opening block to record
`Status: OPEN — IDENTIFY`. Historical attribution is author `zai-2`, reviewer
`zai-3`. These names are recorded provenance, not evidence of a new exchange.

## Repeatable procedure

Run from the futon2 checkout. Choose a new directory for `ROOT`:

```sh
clojure -M scripts/wm_frozen_cascade_steps.clj init \
  holes/labs/wm-contract/runs/frozen-selection-step-2026-10-22/pass-1/selected-boundary.edn.gz \
  test/fixtures/frozen-selection-step/recorded-compiled-426fa181.edn.gz ROOT
clojure -M scripts/wm_frozen_cascade_steps.clj next ROOT
clojure -M scripts/wm_frozen_cascade_steps.clj status ROOT
```

Each `next` runs exactly one section from the saved prefix. Run it five times
after `init` to reach `dispatch-prepared`. Each process can exit between steps.
Checkpoints contain their payload, predecessor digest, content digest, and
producer revision/source hashes. A failed attempt is recorded separately and
leaves the successful prefix intact. After repairing the failing section,
`next ROOT` retries it. When a repair changes an earlier successful section,
fork at its predecessor:

```sh
clojure -M scripts/wm_frozen_cascade_steps.clj fork SOURCE compiled NEW_ROOT
clojure -M scripts/wm_frozen_cascade_steps.clj next NEW_ROOT
```

Use a single writer per root. This is an offline development driver, not a
concurrent job scheduler. Once preparation is complete, another `next` returns
the saved terminal digest and `:reused true`; it sends no messages.

## Recorded evidence

All paths below are relative to `holes/labs/wm-contract/runs/`:

* `frozen-cascade-steps-2026-10-22/`: original successful prefix and the actual
  failed wiring attempt under `attempts/`. The old construction is retained.
* `frozen-cascade-repaired-2026-10-22/`: first repair, forked at compilation.
* `frozen-cascade-verified-2026-10-22/`: final implementation, forked at the same
  compilation checkpoint; construction, wiring, and dispatch preparation each
  run in a fresh JVM. `verification.edn` compares the preserved prefix and
  records the resulting operation and pending completion check.

Focused validation passed: 14 tests, 110 assertions, zero failures/errors across
`compiled-cascade-construction-test`, `cascade-continuation-test`, and
`cascade-runner-seam-test`. Full logs are in the verified run's `tests/`
directory. `verify.clj` there reruns the prefix/result comparison. The integration
tests use isolated ports; no change was loaded into the running production JVM.

The imported selection was explicitly resealed offline in the preceding
selection experiment. Its original menu seal does not validate under current
code. Its `:import` record preserves that limitation. Neither the selection nor
this chain is authenticated authority for a new production click. The first
diagnostic checkpoints predate producer-source recording; they are retained
unchanged rather than retroactively relabelled.

## Extend toward useful work

1. **Completed:** recover selection and compiled instructions, reproduce the
   construction defect, repair it, and resume from saved compilation to a
   concrete author/reviewer handoff.
2. **Next:** bind a fresh authorised task to the current mission revision and
   dispatch the prepared operation through production author/reviewer ports.
   Save the exact request and author result before review, including a durable
   job identity so a restart reconnects instead of submitting twice. The
   current offline driver stops before this side effect.
3. Record independent review against the exact author result. Require evidence
   that the requested mission change happened; a recording-only executor or
   an abstention does not satisfy this milestone. Preserve the author boundary
   if review needs repair.
4. Feed the verified effect through the existing continuation/journal/close
   path, save close evidence, then demonstrate the next production tick
   consuming the carried state. Resume those sections without repeating the
   successful author task.

The existing runtime already has `resume-on-prod-cascade!` and
`continue-on-prod-cascade!`; extend those ports for live work rather than
creating a second production execution route. A new Zai exchange and any
resulting mission edit remain pending; this note does not claim end-to-end
mission completion.
