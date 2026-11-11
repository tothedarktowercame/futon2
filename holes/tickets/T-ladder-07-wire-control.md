# Ladder 07: M3 wire with medium-swap control

**Status:** OPEN — DRAFT FOR REVIEW
Parent: M-futonzero-prelim-practice
Gate: awaiting Joe and Fable review; do not select or execute.

Dependencies: T-ladder-05-settling-confinement and Joe's engine ruling from T-ladder-06-layout-premise.

## Note
With the rule layer held fixed this is a known-answer ticket: speed-1 arrival and XOR superposition are properties of rule 90, and the 77 control follows from ladder 05's confinement result. It tests the layout code and the controls, not the substrate. The TN's own order puts the memory run (after ladder 08's sign-off) ahead of the wire as the informative experiment.

## Scoped task
On the ruled engine, test a rule-90 channel and controls replacing it with rule 77 and routing through rule 178.

## What would close this
A perturbation arrives at distance d at time d on the 90 channel; no corresponding arrival occurs with the channel changed to 77 or through a 178 back path; transmitted content is checked by XOR. Expected outcome: **closed / pass**.
