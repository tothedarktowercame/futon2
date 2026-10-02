# E-wm-algorithms — make META's Algorithm class safe and selectable

**Status:** DERIVE (2026-10-02). One Algorithm is approved; Algorithms are not
yet an enabled War Machine action class and the unchecked items below remain
the excursion.

## Origin

Joe, 2026-10-01: when the War Machine is stuck it should eventually be able to
select an approved Algorithm instead of another Mission, Excursion, or Ticket,
giving the outer selector the four classes M/E/T/A (META). Candidate Algorithms
include writing a new design pattern and addressing a Tornhill-level cleanup
task. `~/code/algorithms` contains sample material, not an approved operational
registry.

An Algorithm is a reusable recovery procedure, not a predeclared task and not a
canonical cascade. It should change the stalled situation or produce evidence
that changes the next selection. Ordinary M/E/T selection then runs again over
the updated world.

## Scope

In scope: define the approved Algorithm registry and its runtime contract;
admit a deliberately small initial set; expose Algorithm candidates only when
their applicability predicates hold; retain execution and learning receipts;
and add the fourth critical census count.

Out of scope until those conditions are met: treating files currently under
`~/code/algorithms` as approved, enabling arbitrary scripts, silently widening
agent authority, or turning every failure into an Algorithm invocation.

## Work items

- [x] **Approved registry.** Define a versioned, machine-readable registry
  separate from the sample corpus. Every entry has a stable id, description,
  implementation locator and revision, owner, status, and explicit approval
  evidence. Unregistered or unapproved entries never become candidates.
  `resources/wm/approved-algorithms.edn` is the v1 registry; Joe approved
  `A-self-heal` on 2026-10-02. Approval does not waive applicability,
  resource, or launch checks.

- [ ] **Applicability contract.** Every approved Algorithm declares the
  observations under which it is eligible, including the typed stuck condition
  it addresses, required inputs, and exclusions. A negative fixture shows that
  an inapplicable Algorithm is absent from the candidate set.

- [ ] **Bounded action and authority.** Every entry declares its allowed
  repositories, mutations, resource/time budget, stop conditions, and any
  operator-gated effects. Execution outside that envelope fails closed; adding
  an Algorithm does not grant authority beyond the ordinary runner contract.

- [ ] **Completion evidence.** Every invocation defines an observable result:
  changed artifacts, a registered finding, a new or repaired library pattern,
  a measured cleanup delta, or a typed failure. Merely running the procedure is
  not success.

- [ ] **Run receipt.** Retain the Algorithm id and revision, applicability
  evidence, inputs, authority envelope, timing, token/model usage, artifacts,
  outcome, and the M/E/T selection state before and after execution. Replaying
  the receipt identifies the exact procedure that ran.

- [ ] **Learning feedback.** Feed verified results into later Algorithm and
  cascade construction without collapsing selection into a success bit. Record
  whether the procedure removed the stuck condition, enabled a later M/E/T
  advance, or exposed a missing/defective design pattern.

- [ ] **Small initial set.** Admit and test at least two distinct recovery
  shapes, provisionally: (a) construct or repair a design pattern needed by a
  stalled cascade, and (b) choose and perform one bounded Tornhill-level cleanup
  whose measured delta can be checked. Admission follows the registry contract;
  these examples are not approved merely by appearing here.

- [ ] **META enumeration.** Add `:algorithm` candidates to the outer selector
  only after the registry, applicability, authority, and receipt checks pass.
  The per-run critical parameters then record
  `:available-to-choose {:missions M :excursions E :tickets T :algorithms A}`.
  Before enablement, omission of `:algorithms` (or an explicitly disabled zero)
  must truthfully mean that no operational Algorithm action set was offered.

- [ ] **Recovery-loop acceptance.** In a controlled fixture, an ordinary M/E/T
  action reaches a typed stuck condition; exactly one applicable approved
  Algorithm is selected and executed; its receipt establishes a world change;
  the next ordinary selection observes that change and advances or chooses a
  newly warranted target. The bad cases—no applicable Algorithm, failed
  authority check, and no measured change—remain explicit and do not spin.

## Exit

This excursion is complete when the controlled recovery-loop acceptance passes,
the serving War Machine records all four selectable counts per run, and a
disabled/empty Algorithm registry cannot fabricate an A candidate. Live
enablement for general clicks is a separate operational decision based on those
receipts, not a condition for completing the implementation work.

**DERIVE exit: Not met.** The approval catalog now has its first entry, while
applicability, resource admission (including available clicks), execution,
receipts, and the controlled recovery-loop acceptance remain open.
