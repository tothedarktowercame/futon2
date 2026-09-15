# Retained real-packet computation

Declaration commit 4d712077. This executes the actual pure classifier against
138 retained real buffer rows and reads the library category, wiring, kill and
yield arguments. Flexiargs are retained by byte hash as their authored semantics;
this does not claim to execute free-form prose. No new Emacs capture or kills.

| Wiring | Proposed kills | Remaining | Risk | Ambiguity | B G = illustrative A G | Fuel | Threshold 16 |
|---|---:|---:|---:|---:|---:|---:|---|
| aggressive | 3 | 135 | .366235 | .959366 | 1.325600 | 1.68 | not reached |
| conservative | 0 | 138 | 1.100600 | 0 | 1.100600 | 1.38 | not reached |

**No admissible choice; ground-truth verdict unavailable.** Revisit data have not
been observed. Priors, costs and threshold remain declared constructions, even
though the buffer snapshot is real. A retains genuine revisit uncertainty and
projects only deterministic bookkeeping away, so it is indistinguishable from B
on this field. Full Alexander A remains undefined; this is not its validation.
This is a T=1 candidate-loss projection, not the general Lean cascade interpreter.

Critical source finding: the packet contains modified="autosaved" on one buffer.
Emacs treats this non-nil value as modified; the adapter truthy? helper accepts
only true/"true" and would allow it. Our consumer explicitly preserves autosaved,
normalizes explicit modified=null (Emacs nil) to false, and refuses missing or
otherwise malformed protection fields. Therefore its three proposals differ from
the older runner's four. Raw source packet and normalization are both pinned.
The two earlier strict-validation failures are retained; neither was scored.

No sharp disposable kill occurs in the selected candidate union. Conservative's
kept outcomes are deterministic and show zero ambiguity. This shows the formula
can represent a sharp channel; it does not establish a certain-safe real kill.
A sharp outcome can still have positive KL under a soft preference. No entropy
is fabricated for process/eligibility observations. See per-coordinate Q/C in
results.json and the likelihood/independence assumptions in PLAN.md.

Ages are retained as a distribution in results.json. Age is not revisit truth;
P(needed later|age) cannot be read off this marginal. The single file's age and
its preservation status do not validate the library age-prior table. Classification
controls retain protected rows and reject missing fields. Independent product-joint
enumeration checks marginal KL/entropy sums; exact rational costs are retained,
while logarithms are double precision with numerical checks.

Reproduce from the adapter directory:

    bb --classpath src scoring/export.bb > scoring/adapter-output.json
    python3 scoring/score.py > scoring/score.log

Kondo and explicit-path parens passed for export.bb. No adapter source edited.
The consumer is a bounded diagnostic, not an acceptance of the adapter's general
input validation or preservation semantics. The autosaved finding must be fixed
upstream before its own four-kill receipts can be relied upon.
