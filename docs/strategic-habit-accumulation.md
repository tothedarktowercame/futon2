# Forward strategic habit accumulation (Item 22)

Enable with `:accumulate-strategic-habit? true` in full-loop runner or judge
opts, or `FUTON_WM_ACCUMULATE_STRATEGIC_HABIT=1`. Explicit false overrides the
environment. Defaults remain off. This records selections, not enactments or
outcomes; it does not change the selector, its fixture E_S, or beta placement.

Persistence uses a separate `:strategic-habit-state` store (version 2), alongside
the existing, unchanged version-1 `:habit-prior-state`. Store-level separation
preserves the exact legacy categorical keys, counts and consumers. Legacy
unlabelled aggregates are not reclassified or reconstructed. Loading an absent
strategic side gives `:status :empty` with
`:empty-reason :forward-accumulation-not-started`, not an inferred uniform E.

The new store records `:grain :strategic`, alpha 1.0 (the scheduler default,
explicitly distinct from fixture alpha 0.5), counts keyed by the final recorded
`:selected-policy-id`, capture time and events keyed by the selection request's
trace ID. Events name the reason-bearing strategic boundary. Repeating an
identical event is idempotent; conflicting IDs and insufficient identity refuse.
No scheduler-action to policy mapping occurs. Existing stores carry unchanged
through disabled ticks, so toggling accumulation does not erase history.

The trace writer persists the separate store for the next judgement. No
historical reducer seeds it. `require-promotable` rejects absent, empty or
wrong-grain data; it is a prerequisite guard only, with no production selector
caller. Promotion remains a later packet even after counts exist.

Validation: focused strategic-habit, existing habit-prior and policy suites;
trace round-trip with a recorded-shape policy ID, tactical serialized identity
with accumulation off/on, explicit-false override, duplicate/conflicting events,
identity/grain refusal. These are isolated tests, not evidence of a live run.
