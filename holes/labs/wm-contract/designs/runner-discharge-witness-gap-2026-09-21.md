# Discharge implementation stop: the finding-specific witness producer

Author: codex-8. Owner: claude-12.
Request: invoke-1789966046653-22916-a702739f.
Accepted design: 12abf794.

Implementation is not complete. No application source, live store, or serving
JVM was changed by this investigation. This is a gap in my accepted design,
not a retraction of its two-phase lifecycle or derived-receipt decisions.

## Missing authority

The design requires an explicit requirement-to-evidence table and a
finding-specific grounded witness. It does not specify the producer or the
executable acceptance predicate for that witness. The current evidence
machinery cannot supply that predicate by composition alone:

- `src/futon2/aif/full_loop_runner.clj:2512` produces witness booleans from
  implementation entity insertion/visibility. That is not observation that the
  selected finding's failure was repaired.
- `src/futon2/aif/limb_evidence.clj:221` determines coverage by the set of
  declared `:limb` keywords. Companion-output checks establish byte integrity,
  not whether the command tested the failure, discriminated against its bad
  case, or ran through a production-shaped path.
- `validate-standing-decision` checks shape, named independence, explanation
  length, and nonempty evidence references. Those references are not themselves
  an executed repair validator.
- The ruling in
  `RULING-repair-subject-positive-status-2026-09-14.md:74-79` explicitly says a
  reviewer assertion or projection label cannot trigger resolution. Its section
  5 also distinguishes approval of implementation from standing of the full
  finding, including the successor limb.

Consequently I cannot use a covered limb bundle, an approving Agency job, and
a distinct grounded close as sufficient proof of the named repair. The close
proves that work occurred; the missing observation is that this particular
failure was repaired. Requiring only more receipt fields would preserve the
same defect.

## Fresh tooling JVM control

Executed on 2026-09-21 in canonical futon2, exit 0, without loading a shared
JVM. This is a control on the existing validators, not an application test
suite or a successful repair demonstration:

```clojure
(require '[futon2.aif.limb-evidence :as limb])
(let [receipt {:schema :wm/limb-receipt-v1
               :repair/id "unrelated-failure"
               :limb :grounded-repair :command "true" :exit 0
               :stdout-sha256 (apply str (repeat 64 "0"))
               :stderr-sha256 (apply str (repeat 64 "0"))
               :recorded-at "2026-09-21T06:00:00Z"}
      standing {:schema :wm/target-standing-decision-v1
                :entity/id "unrelated-failure" :decision :resolved
                :decided-by "reviewer" :implementation-author "author"
                :decided-at "2026-09-21T06:00:01Z"
                :evidence ["unverified-receipt"]
                :explanation (apply str (repeat 100 "x"))}]
  (prn {:probe :shape-validation-is-not-repair-proof
        :bundle (select-keys
                 (limb/validate-limb-bundle
                  {:requires [:grounded-repair]} [receipt])
                 [:status :covered :absent])
        :standing-admitted?
        (= standing (limb/validate-standing-decision standing))
        :actual-repair-evidence-present? false}))
```

Output:

```edn
{:probe :shape-validation-is-not-repair-proof,
 :bundle {:status :covered, :covered [:grounded-repair], :absent []},
 :standing-admitted? true,
 :actual-repair-evidence-present? false}
```

This is expected behavior for these validators; it is not a claim that they
are defective. No shell command in the receipt was executed. No store writer
was invoked. A production manifest adds file-integrity checks, but it does
not add the missing finding-specific semantic check.

## Proposed bounded addition for review

Specify a reviewed, revision-pinned validation contract per finding, separate
from the existing finite discharge requirements. It names the exact failure
and a registered evaluator capable of observing it through the production
path. The evaluator, not author-supplied witness booleans, supplies the result.
Retain its inputs, outputs, finding-byte pin, evaluator revision, tested repair
revision, and actual attempt/run identities. Unknown evaluators and absent
contracts yield `:evidence-unavailable` with no discharge writes.

For implementation A, require the evaluator's discriminating bad-case control
and repaired-case observation plus executed independent review. For successor
B, require the same finding/contract binding and an actual later
production-shaped observation, then the existing successor/store conjunction.
Do not manufacture success by accepting an arbitrary zero-exit command.

The first evaluator should cover a real retained finding (for example the
history-poison repair's non-identity exclusion and identity-refusal controls),
with a constructed irrelevant-success control that must refuse. The contract
must distinguish this integration fixture from a live successor.

Review requested on this witness authority before adding the store-writing
stage. The previously approved two phases, authoritative resolution store,
regenerable receipt, rejection semantics, and narrow publisher remain the
intended implementation. No claim is made that a refused-only stage would
satisfy Joe's request for a functional repair loop.
