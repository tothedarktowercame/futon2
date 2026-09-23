# ⟨1⟩6 — third limb pre-flight: STOPPED on feasibility

zai-1, 2026-09-23. Read-only in the serving JVM. **No click fired** —
the feasibility check (part 1.3) fails, and per the handoff that means
STOP AND REPORT, not spend a click.

## 1. Queue — clean

`:front-stratum`, the reference ticket the only eligible target.

## 2. The decision under the new evidence — the machine still prefers C2's
FIRST action, and here is the honest subtlety

G(C2)=0.5978, G(C1)=2.9957; posterior {C2 0.9167, C1 0.0833}; the marginal
maximum is `:aif/declare-the-conditioning` (0.9167) — the SAME first action
as the last two clicks. Why, when `:repair/split-declared-valid` is now
observed TRUE and C2's first-action guard forbids it? Because the class
scorer's terminal emission asks only whether `[T :restoration-accepted]` is
reached at the horizon, and the ROLLOUT from today's q₀ still runs the full
chain: the first pattern whose guard holds is now the SECOND limb
(`:aif/measurement-window-hygiene`, needing split-declared-valid ✓,
forbidding held-out-observations-collected ✓) — but the runner's
selection-certificate names the candidate's FIRST DECLARED action
(the chain head `:aif/declare-the-conditioning`), and the marginal is keyed
on that name. The action the machine would ENACT is the next enabled limb;
the action the marginal NAMES is the chain head. So the reported preference
is C2, and the enacted first step would be the observations limb — with the
naming mismatch flagged here rather than assumed away.

## 3. FEASIBILITY — the limb's acceptance CANNOT be met with evidence that
exists. STOP.

The declaration's own locator and window
(`resources/wm/eig/held-out-split.edn`):

- `:held-out-set ["attempt-002" "attempt-003"]`,
  `:training-set ["attempt-001"]`,
  `:window {:minimum-observations 2
            :close-when :all-held-out-attempts-have-durable-close}`,
- `:locator {:root "data/wm-full-loop-machinery-72/…" :record
  "<attempt-id>/007-closed.edn"}`,
- `:claims {:observations-collected? false …}`.

The held-out records: **attempt-002 exists** (closed today, the second
limb's own close) — **attempt-003 DOES NOT EXIST** (verified:
`data/wm-full-loop-machinery-72/wm-contract-machinery-72-v1/` contains
attempt-001 and attempt-002 only). The window requires TWO held-out
attempts with durable closes before the observations are complete, and the
declaration's own claims field says `:observations-collected? false`.

So the observations limb's acceptance — `[T
:repair/held-out-observations-collected]` observed true — requires an
attempt-003 close that does not exist yet. A click spent on this limb now
would either deliver work whose acceptance cannot be met (click 3's
`:prospective-held-out-evidence-unavailable` failure mode, the exact trap
the rewrite of the cascade source was designed to avoid: C2's observations
guard FORBIDS held-out-observations-collected, and the acceptance for that
token cannot become true until the window closes) or wait inside the click.
**Per the handoff: STOP AND REPORT.** The limb becomes feasible when
machinery-72 attempt-003 exists with a durable close — i.e. after one more
ordinary click on a DIFFERENT limb-bearing target, or by Joe's direction.

One design observation for you and Joe, not acted on: the held-out window
needs two MORE closes (002 done, 003 pending) — meaning the repair as
declared needs at least: this observations limb (after 003 exists), then
the calibration limb, then acceptance. At one click per limb plus the
prerequisite closes, the renewal's remaining three clicks cannot complete
the repair; a further renewal or a narrower window is a Joe-level choice.
