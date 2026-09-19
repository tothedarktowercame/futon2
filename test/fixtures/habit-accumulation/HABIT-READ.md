WIRE-habit-read intentionally changes selection authority: the learned joint
policy prior now enters the live posterior. This is a change from the earlier
record-only posture, not a fix to an accidentally omitted diagnostic field.

`cascade-prior/habit-masses` converts ln E to E. The mandatory attachment is
inside `policy/select-action-cascades`, after scoring and before constructing
the consumed candidates. The joint War Machine call forwards the same habit
path used by accumulation. Each read takes one state snapshot; each successful
selection is counted afterwards. The ordinary softmax/log-score functions and
positive-habit/temperature guards are unchanged.

The former mixed-mission guard is removed, with the four reasons recorded at
its source site. Targets remain part of policy identity. Missing or mixed-type
policy identities still throw in the prior; the live seam catches those typed
identity failures and records a whole-menu `:neutral-fallback`, its reason,
and `:declared-neutral` status. Malformed persisted state still refuses.
Valid menus have `:habit-status :attached` for every consumed candidate.

The permanent discriminator uses the same two candidate maps in both arms,
with beta 2, G [0, 0.2], F [0.2, 0.7], and counts [0, 3] (alpha 1):

| Quantity | Empty | Work (different target) |
| --- | ---: | ---: |
| Neutral E | 1 | 1 |
| Learned E | 0.2 | 0.8 |
| Neutral posterior | 0.6456563062257954 | 0.35434369377420455 |
| Learned posterior | 0.3129648952316626 | 0.6870351047683373 |

The argmax AND selected action change from empty to work. This is learned
non-degeneracy: the unequal masses trace to unequal counts. Both E census
entries report `:non-degenerate` / `:non-unit-habit`, but that verdict alone
cannot prove learning. The prior test separately pins the expected probability
masses and their sum of 1 across targets. The live scorer-to-selector test
uses the production fixture's candidate shape and asserts distinct attached
masses without calling an attachment helper.

`habit-read-structural-diff.edn` walks all six old decisions from 9265a89b
against a sequential replay that reads and accumulates habit. It reports every
changed path and value, without dropping keys or normalizing the comparison.
Only E, E provenance/status/input records, E census, posterior/softmax weights,
and chosen-action mass change. All six selected actions remain unchanged;
G, F, beta, scoring certificates and every other field remain equal.
The first tick's E is uniform: its census non-degeneracy is ONLY normalization.
Later ticks have unequal accumulated counts and therefore learned asymmetry.
The diff was committed in 97cafa3e BEFORE replacing before.edn.
The `(= before after)` assertion remains intact.

Reproduce the structural report from futon2 with:

```
clojure -Sdeps '{:paths ["src" "resources" "scripts" "test"]}' -M test/fixtures/habit-accumulation/verify-habit-read.clj
```

That writes a proposed fixture under /tmp, not over the committed fixture.
The earlier verify-regeneration.clj is historical evidence for the scoring-only
packet and is not the regeneration command for this behavioural change.

Validation is offline, using temporary habit stores: no click was run and no
production habit file was edited. Narrow namespaces: cascade-prior-test (7/26),
cascade-habit-read-test (3/14), cascade-habit-accumulation-test (5/32), and
selection-certificate-test (4/58), all green. The certificate test also checks
two positive and three negative Lean controls. Clj-kondo has zero errors and
warnings on every touched Clojure file; futon4 check-parens passes.
