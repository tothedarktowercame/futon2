# Standing authorization: ordinary WM clicks, budgeted — Joe, 2026-09-19

Recorded by claude-12 from Joe's ruling (operator, emacs-repl, 2026-09-19),
in response to P-0 of `p4ng/wm-walkthroughs/build-loop/closure/PRIORITY-2026-09-19.md`
(the reshaped `OPS-ordinary-run/click`, closure-dag.json r97).

## The grant

> P-0 gets partial authorization: I'll authorize them 5 at a time because I
> don't want unbounded execution. So, a budget of 5 runs is now allocated,
> and when that is exhausted come back to me for more. This is also
> contingent on proper record keeping not only for runs but bugs and repairs
> (which the machine is already good at).

Operationally:

- **Budget: 5 ordinary clicks**, allocated 2026-09-19. No per-run ask inside
  the budget; this record is the authorization each of those clicks cites.
- **Renewal is Joe's**, on exhaustion — return to him for the next 5. The
  refusal at budget exhaustion is Joe-commissioned (this document is its
  source), so it is not subject to the guards-presumed-unwanted presumption
  in `../tickets/T-wm-excessive-guardrails-19092026.md`; it IS the
  consent-gate pattern's autopen, granted for five uses at one locus.
- **Contingency: record-keeping stays proper** — run records, and bug/repair
  records (the machine's existing strength), for every budgeted click. A
  click that leaves no durable run record consumed the budget without
  honoring the contingency; say so rather than smoothing it.
- Non-ordinary runs (anything outside the gated on-demand click path) remain
  per-occasion asks as before.

## Consumption

Consumption accounting is to be kept append-only next to the run records
(mechanism: claude-4's call as OPS node owner). Until a mechanism exists,
the count is by enumeration of ordinary-click run records dated after this
grant.

- Allocated: 5 (2026-09-19)
- Consumed: 0 at grant time
