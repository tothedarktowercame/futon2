# Buffer cleaner: declaration before execution

This is constructed data, not Joe's measured revisit history. The 24 buffer
identities, 24-hour revisit outcomes, probabilities, costs, threshold and both
wirings are fixed in parameters.json before running compute.py. No fitting
follows computation. Prior arithmetic precedent: futon2 11d7a10b.

Ground-truth oracle: among wirings leaving at most 16 buffers, choose the one
with minimum REALIZED revisit recovery cost plus scan/kill fuel. A false kill
means killing a buffer named as revisited in the constructed next-24h history.
This oracle is independent of predicted G. Predicted choices minimize G+fuel
among the same eligible wirings. One recovery-cost unit equals one nat of
preference loss, by declaration, not measurement. No confidence threshold is
needed for this count threshold; the needed-later prior remains explicit.

All kills succeed, no concurrent creations occur, and all candidate buffers
are invisible, process-free, and without unsaved file edits or server clients.
The four protected entries remain untouched. Safe stream/HTTP outputs are
stipulated disposable in this construction; the real category test alone does
NOT prove this. File revisit probabilities at 2h/8h/30h are .6/.4/.05, not a
learned age model. Other probabilities and actual revisits are in parameters.
The conservative category mask is a hypothetical wiring, not a shipped option.

## Generative model and two columns

One finite-horizon cycle is scored at its endpoint (T=1), after the composed
eligibility/age/kill wiring. This is not a test of temporal length neutrality.
Hidden current snapshot is fixed; future revisit indicators are independent
Bernoulli variables by construction, not by inference from buffer categories.
For each of the SAME 24 coordinates in each policy, X_i means false kill:
P(X_i=1)=p_needed_i if killed, otherwise 0. Observation A is the joint product
of these Bernoullis; q has one state. Preferences are the product of
C_i(1)=exp(-c_i)/(1+exp(-c_i)), C_i(0)=1/(1+exp(-c_i)). Thus no zero-C issue.
Book B: KL(Q||C)+H(A), from that same joint (Parr eq 4.9/4.10 as in
CascadeEFE). The code evaluates KL and H independently and checks the derived
identity G=E[recovery cost]+sum log(1+exp(-c_i)). No entropy term is added twice.

A illustration: outcome projection removes only deterministic eligibility/hash/
process-status bookkeeping, whose conditional entropy is zero; it RETAINS
all genuine revisit outcomes. On this field the projected model is identical
to B, so A-illustration=B. This is not a formula for full Alexander A and
cannot discriminate the unresolved A/B fork. A different A score needs an
explicit rule for real revisit uncertainty; none is silently invented here.
Expected recovery loss is also reported separately, never relabelled G.

## Source limits

Pinned cleaner source: source-pin.json. Lines 130-179 classify buffers; absence
of a process does not establish zero revisit cost. Render/invoke/HTTP outputs,
Dired and temp buffers can still be wanted later. Files must be unmodified,
stale and have no waiting client; visible/process/active-agent buffers are
protected. Temp modes include help, fundamental, org and special buffers.
Lines 182-211 count candidate attempts regardless of kill return, whereas total
is before minus after. The supplied log's category counts sum differently from
its killed count; it cannot establish per-category successful kills. It retains
no identities or revisit join. A scoped search in futon0 and wm-contract/futon3c
notes found no usable revisit history; this is not an exhaustive machine audit.
Neither the supplied log nor its alleged 6502-buffer origin is used as data.

Checks: probabilities and preferences normalize; direct KL+H equals cross
entropy; exact rational expected/realized costs; all categories and counts;
zero-revisit control; both wiring results; deterministic-projection equality.
No live Emacs calls, buffer kills, runtime changes or publication.
