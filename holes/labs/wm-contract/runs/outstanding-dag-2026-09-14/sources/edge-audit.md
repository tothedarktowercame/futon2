# Row 22 declaration-level audit: six class-(c) edges and beta/gamma

Date: 2026-09-13. Discovery only. No implementation, configuration, figure,
registry, frozen module, or tracker change; this is not approval of future work.

## Authorities and byte pins

The edge ledger is `p4ng/empirics-futon/control-map-edges.edn` SHA-256
`0c7ee7579701a7bf4d413b54351deef527c7991644a62af801e43ac453b1595f`.
The equation/choice authority is `aif-equations.edn` SHA-256
`2fed9f7c5d4a3c375807dab5e0e3f24c82852bbbd9cef949a2fac8d7c22479da`;
its `:temperature-update :ruling` at lines 298-304 retires drawn `R7->R14`,
calls selection gain/score spread shortcuts, and requires Friston's
`gamma=1/beta` with beta updated by eq. 2.7. The firing audit is
`TN-row22-firing-audit-2026-09-12.md` SHA-256
`e28f70c178c4700c140664e56de9a98afea0a8f5454f5d6ef2425e8085a719e8`.
The node audit is `VERIFY-r-nodes.edn` SHA-256
`b1046da1ba557d6a5125e6c13678e2cc2b80b2004d8be616dff1ec4d8109721e`.

## Endpoint/disposition table

| drawn identity | exact producer/value | proposed consumer | source/evidence today | declaration-level verdict and next packet |
|---|---|---|---|---|
| R6→R11 | R6 has two distinct declarations: candidate set `machinePolicySet` and posterior `softmaxWithFPi` (`aif-equations.edn:158-178`). The only plausible R11 input is ranked proposal fields, normalized by `hierarchical-budget-adapter/select-ranked-proposal-fields` (`:45-96`). | `hierarchical-budget/arbitrate` returns a portfolio, selected IDs, budget witness and `:selection-boundary :hierarchical-shared-budget` (`:157-212`). | Adapter/arbiter tests and V7 R11 receipt exist, but namespace search finds no production caller; no retained arbitration input/delivery. | **Underlying capability exists; edge identity underspecified.** It is candidate/ranked-field→budget-arbitration, not posterior→arbiter. Packet A must declare the adapter input mapping from real R6 candidate identities, costs and utilities, with typed missing-cost/identity/support refusals and retained replay. |
| R11→R16 | Producer is the R11 arbitration portfolio above. | R16's declared `machineAction` is the policy action law (`aif-equations.edn:179-186`); actual `enact!` instead gates ranked actions and takes a first passing construction (`enact.clj`), so declared action and enactment are already distinct carriers. | Ledger schema itself says no arbitration delivery; R11 has no production caller and no retained node-linked run. | **Not yet a well-typed arrow.** Packet B follows A and must choose explicitly between (1) R11 portfolio constrains the R6 candidate domain before `machineAction`, or (2) an R11-approved exact action gates R16 enactment. It must not call an arbitrary portfolio “u”. Require selected-ID subset/equality, budget proof, chosen/enacted correspondence or typed divergence, and negative controls for unapproved enactment and identity mismatch. |
| R9→R16 | Ledger declares an independence verdict record `{claim,witness{id,producer,layer},verdict}`. | Proposed R16 constraint: act only on `:independent`; `:self` and `:unknown` do not authorize. | `control-map-edges.edn` records this as a specification and explicitly says zero consumers in `enact.clj`/close-loop; no retained delivery. | **Well-motivated contract, unimplemented.** Packet C builds a narrow pre-enact verifier consuming a pinned R9 verdict for the exact candidate/construction identity, refusing missing/mismatched, `:self`, and `:unknown`, with file-absent controls and an independent positive receipt. Do not reinterpret the witness-registry review as this verdict. |
| R10→R8 | R10 is scheduler/liveness plumbing; no equation/value declaration exists. A scheduled entry starts a tick. | R8's live declaration is policy free energy `machinePolicyFreeEnergy`, which consumes observation and previous prediction; retired scalar F has no producer. | No retained scheduler→F_pi value; row-22 correctly says a scheduled run starting a tick is not evidence that scheduler is an R8 input. | **Drawn identity is causal sequencing, not a declared data edge.** Packet D first specifies the intended value. If the intent is merely schedule→tick→R2→R8, mark the direct stroke as a decomposed route, not F_pi input. If liveness metadata is truly meant to condition F_pi, that is new mathematics and needs a declaration and witness before code. Never target retired F. |
| R15→R13 | `temporal-hierarchy/hierarchical-rollout` consumes a slow state to reshape fast priors; R13 `machineDepth` is horizon T. The module explicitly says hierarchy changes what the fast loop wants, not how far it looks (`temporal_hierarchy.clj:1-43`). | R13 depth selects rollout horizon/multi-horizon prediction. | R15 module and tests exist but no production caller, no slow keys in the retained corpus, and no R15 hop. | **Specification error as drawn unless a new depth-control law is declared.** The existing R15 value should connect to a prior/preference consumer, not R13 depth. Packet E specifies the correct slow-state→fast-prior connection and integrates it with retained two-timescale identity; keep depth independently configured. Any adaptive-depth replacement is separate mathematics. |
| R15→R16 | `advance-slow-state` produces next `:slow/mode`, `:slow/intrinsics`, previous mode and feedback from a witnessed fast outcome (`temporal_hierarchy.clj:190-237`). | R16 is selected action/enactment. | Built/test-only R15; zero production callers and no retained tactical-outcome→actuation delivery. | **Direction/grain is ambiguous.** Existing feedback is R16/outcome→next R15 state; the forward influence is R15 slow state→fast priors→R6/R16, not a direct action value. Packet F declares and captures both temporal directions with tick/run identity, then only retains a direct R15→R16 edge if the selected action demonstrably consumes the shaped prior. Refuse missing predecessor/outcome and cross-run joins. |

Relevant implementation hashes: `hierarchical_budget.clj`
`a0581c1d7135324b1190795ba56dfc9fa834869474a297155678d88d71e5216b`,
`hierarchical_budget_adapter.clj`
`525bcb8aa4aad49c9cd02c22dfeabc7406aced8aacbb5731317e182eddb691e1`,
`temporal_hierarchy.clj`
`e3e532ae1b0b123730299bd7caa1105b074b7d27912c5508d29c395f21d34eef`,
`enact.clj` `13801b26cb67ae73c1a070a08e7bb3c2d257e4eeeab3cb6bb847e90c8a6f0c3e`.

## The correct beta/gamma connection

It is not an R-number relabel of `R7->R14`. R7 produces observation-channel
precision `Pi_k`; no value from it belongs in commitment temperature. The
existing theory-aligned chain is declaration-level:

1. `policy-precision/converge-beta` solves
   `beta = beta_prior + (pi(beta)-pi_0(beta))·G`, constructing
   `gamma=1/beta` inside the distributions (`policy_precision.clj:33-61,95-180`).
2. `carry-beta` retains the solved-or-typed-held beta and source
   (`policy_precision.clj:497-560`).
3. `policy/effective-temperature` in `:variational-beta-gamma` consumes that
   beta as `tau=beta`, refusing missing/nonpositive/nonfinite values
   (`policy.clj:77-146`).
4. `selection-scores` consumes tau as `-G/tau`, hence exactly `-gamma*G`, with
   F_pi and ln E at their separately ruled placements (`policy.clj:157-219`).

Thus the correct connection identity is **policy-precision beta posterior →
MachineTemperature's variational arm → softmaxWithFPi score**, not R7→R14.
Source hashes: `policy_precision.clj`
`2e7164ecf965fe9553822a537fb543d212ec1641daeca6652b05a47a62ad094b`,
`policy.clj` `b0dc9a1a440c18ec1464ee96abb9c76401a02295e8e3e7f0840b105242236628`,
and `policy_free_energy.clj`
`05a53b3fe2f6455f0deca83850b8d4a550065995b05ac38724d1b848eaa2af1d`.
RUN7 retains a 20-tick dark beta/gamma series; RUN8 retains only one variational
consumption tick. That proves reachability at pins, not a qualifying full run.

## R20 interoception versus the beta/gamma law

Commissioning the row-18 commitment link only on `:spread` or
`:selection-gain-only` because the fixed multiplier changes engineering gain
would demonstrate an engineering controller response, but it would not satisfy
the temperature ruling: those are explicitly shortcuts and neither has
`gamma=1/beta`. Conversely, multiplying selection gain while running
`:variational-beta-gamma` is correctly typed inapplicable because that mode
does not consume gain. Therefore an engineering-only R20 commissioning cannot
be promoted to the theory-aligned required connection.

The actual model seam, if interoception is to influence decisiveness, must be
specified at the beta dynamics rather than smuggled into tau. The narrow
candidate seam is a declared interoceptive input to the **beta prior/evidence
update** before `converge-beta`; it may not multiply beta, gamma, tau, G, or
F_pi ad hoc. The current fixed `m(k)` law supplies no theorem explaining such
an update, so implementation must wait for a strict specification packet:

* declare the interoceptive evidence carrier, authority, time index and its
  mapping into beta prior/update;
* state the extended fixed-point equation and prove that absent interoceptive
  evidence reduces exactly to eq. 2.7 and that `gamma=1/beta` remains true;
* preserve F_pi, G and habit-prior placements and the solved/held distinction;
* refuse unknown/unreadable, stale/cross-run, saturated, nonpositive and
  nonfinite inputs; never fall back to engineering gain;
* commission open/closed trip controls on the same real candidate field, with
  every beta, gamma, tau, score, posterior, chosen action and enactment joined;
* add a Lean statement and independent production-match witness before any
  qualifying-run credit.

If no reviewed equation justifies how a trip changes beta evidence, row 18 may
ship only as a separately named engineering safety controller and must remain
non-qualifying for the theory-aligned commitment-temperature obligation.

## Dependency-ordered packets

1. Specification packet G: name the policy-precision→temperature→posterior
   declarations and the interoceptive beta equation; Lean law and negative
   signature first. No runtime edit.
2. Packets A then B: real R6 ranked fields into R11, then explicit
   R11 portfolio/action into the chosen R16 boundary.
3. Packet C: exact R9 verdict pre-enact gate.
4. Packet D: disposition R10→R8 as decomposed sequencing or declare a real
   value; no implementation while it is untyped.
5. Packets E then F: production two-timescale prior shaping and joined feedback;
   repair the R15→R13 stroke unless a separately declared adaptive-depth law is
   chosen.
6. Only after G: implement and commission the interoceptive beta seam, then
   update the edge specification and qualifying-run checker from reviewed
   evidence. Discovery authorship supplies no independent approval.

