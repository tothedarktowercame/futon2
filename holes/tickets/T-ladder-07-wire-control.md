# Ladder 07: M3 wire with medium-swap control

**Status:** ABANDONED (WONTFIX) — superseded: folded into T-ladder-08-memory-preregistration as its control arm (claude-4 review, Joe 2026-11-13)
Parent: M-futonzero-prelim-practice

Dependencies: T-ladder-05-settling-confinement and Joe's engine ruling from T-ladder-06-layout-premise.

Joe's engine ruling (2026-11-13): **propagator-only** (rule layer held fixed), on the recommendation of reviewer claude-4, whose probe ran `genotype-step` with the correct even8 on the 77|90|178 layout: the 90 and 178 blocks dissolve within about 10-20 steps and by t = 80 the whole rule field is 77. With propagator-only steps the rule layer is exactly frozen, so this is an experiment on a fixed heterogeneous elementary CA, not on a meta-CA; say so in the result.

## Note
With the rule layer held fixed this is a known-answer ticket: speed-1 arrival and XOR superposition are properties of rule 90, and the 77 control follows from ladder 05's confinement result. It tests the layout code and the controls, not the substrate. The TN's own order puts the memory run (after ladder 08's sign-off) ahead of the wire as the informative experiment.

## Scoped task
On the ruled engine, test a rule-90 channel and controls replacing it with rule 77 and routing through rule 178.

## Acceptance criteria

- [ ] A perturbation arrives at distance d at time d on the 90 channel; no corresponding arrival occurs with the channel changed to 77 or through a 178 back path; transmitted content is checked by XOR. Expected outcome: **closed / pass**.
