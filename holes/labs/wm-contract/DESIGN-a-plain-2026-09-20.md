# DESIGN — A, stated plainly (2026-09-20)

Written for Joe after: "I don't understand what the *design* of A is
that you are pursuing." The bar he set: plausible, not perfect. This
document is the design-of-record; the programme doc's points 3 and 6
(WMC scaling, variance-wmc) are its PARKED APPENDIX, not its face.

A answers: when the machine looks, how much should it believe what it
sees? Two kinds of looking:

1. CHECKABLE facts (path exists at commit): believed exactly, rate
   zero — proven immune to any coupling story
   (mixtureLikelihood_checkable_marginal). All of today's observations.
2. JUDGEMENT facts (agent/adjudicator says X happened): believed at a
   declared rate < 1. First live one: the J locator on the C
   declaration, unrated until a basis is adopted.

Making A real needs exactly three things:

- CHANNEL RATES FROM THE RECORD. Per Joe's per-model channels:
  codex-intake 0.14 (9/64 counted, MEASUREMENT-dispatch-premise-rates,
  d594140c); Claude refused-handoffs and Zai in-turn channels
  measurable from the same ledger/Landscape by the same
  counted-denominator method. Declared-as-such, query-labeled,
  revisable. The Landscape is the calibration source; designed
  collections refine later (programme point 5).
- ONE COUPLING PARAMETER. A single shared-condition variable z
  ("bad day", probability p): several judgement observations can fail
  together. Justification: correlated failure is what the record
  shows (same-session correction clusters, one wedged JVM), and
  independence understates joint failure by orders of magnitude
  (469x in the enumeration run). p estimable from co-failure
  clustering in the Landscape; declared-as-such meanwhile.
- EXACT COMPUTATION AT LIVE SCALE. A live decision observes a handful
  of tokens (current declaration: 3 wants + 1 locator; run universe
  7). One binary z makes a fully specified observation two products
  (TwoProductEvaluation). No powerset, no compiler, no approximation.

Belief update is Bayes on the small observed universe; conditioning
enters the rollout (that is Q's need, downstream).

PARKED APPENDIX (do not present as the design): the 2^45 powerset
concern and BN->CNF->SDD->WMC compilation are for joint queries over
the WHOLE token vocabulary, which no live decision performs; they
activate if the per-decision universe grows. variance-wmc adds error
bars from rate uncertainty on top of that. Programme points 3 and 6,
sequenced after the reference implementation, pilots not design.
