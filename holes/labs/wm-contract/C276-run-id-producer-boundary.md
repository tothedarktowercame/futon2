# C276 — run-id producer boundary

Date: 2026-09-01

## Finding

The apparent producer collision is not on the production HTTP path and is not
a race.

`futon2.aif.full-loop-runner/run-opportunity!` chooses:

```clojure
(or (:run-id raw-opts) (str (UUID/randomUUID)))
```

The first arm is an explicit direct-runner override. It is used by diagnostic
and pinned tests to name a run deterministically. The second arm is the normal
producer and creates a new occurrence UUID at run start.

The production `POST /api/alpha/wm/click` adapter constructs a closed option
map containing only author, reviewer, repair reviewer, and trigger. It does not
accept or forward a caller `:run-id`. The runner service adds click identity
and selection machinery but does not add a run id. Therefore two identical
production HTTP clicks reach the UUID arm and receive distinct run ids.

C267/C272's duplicate was constructed through the direct service test seam,
whose stub runner deliberately returned one fixed id twice. That remains a
useful control for the binding: direct/internal callers can supply or return a
stable id, so the binding must classify a repeated observation as `:duplicate`
and retain both click identities. It is not evidence that production UUID
generation collides.

## Disposition

No producer change is recommended. Removing or randomising the explicit
override would break deterministic diagnostic and pinned-run joins without
repairing a production defect. Production occurrence identity is already
fresh; direct override identity is intentionally caller-controlled, and C272
makes repetition honest at the binding boundary.

## Control and focused verification

The existing hermetic runner test now invokes `run-opportunity!` twice with
identical production-shaped options and asserts distinct generated ids. It
also retains the explicit `run-assigned` case, proving the override remains
stable rather than being silently randomised.

Canonical invocation:

```sh
clojure -M:test -m cognitect.test-runner \
  -n futon2.aif.full-loop-runner-test
```

The Futon3c C272 control remains the other direction: two sequential service
clicks whose test runner explicitly returns `run-duplicate` share the raw
observation, while the second binding is authoritative `:duplicate` and names
the first click.

No production click ran and no source behavior changed. Only the producer
boundary control and this diagnosis were added; full suites and workspace gate
were skipped per focused scope.

Focused result: 122 tests / 576 assertions, exit 0.
