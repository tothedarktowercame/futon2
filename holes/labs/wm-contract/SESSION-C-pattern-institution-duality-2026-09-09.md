# Patterns, institutions and interpreters — 2026-09-09

Discussion evidence for E-C-realization; no new formalism adopted or code built.

## Joe, verbatim

> Okay, it's great that we were able to find some prior work to build on here. The one thing that I think isn't quite clear yet... And we have an M Formal Patterns mission. That looks a little bit like at this. Is what actually is the difference and what actually is the similarity between an Ostrom-style institution and a design pattern? And how do we think about that? Because... I've circled for some years that these are related conceptions, but they're clearly not identical conceptions. And if we're going to use Ostrom Institutions to... Model preferences in a local first way, which is very compatible with her. Theory. Then we need to think again about what those institutions are. Possibly the reason they're similar is that maybe institutions provide a dual D-U-A-L to design patterns. So they're not identical, but they could consume design patterns. And do work. So maybe they're a kind of co-design pattern. And if we could write down how that works. We might be able to quickly make sense of what it means, for example, to have a co-production rule. But I don't know enough about logic programming to know what a co-production rule actually comes to be. But since we're thinking about design patterns as narrative production rules, A co-production rule would be a kind of interpreter for those. Systems. And the Cascade model is... Somehow, an intermediate artifact where we have chained together various design patterns or production rules. But it itself doesn't seem to have this co property because the cascade... Doesn't do anything. The cascade is just an informational object. But if we had an interpreter for Cascades, And again, we kind of have that in our Snatch game where we talked about the temperament of players. Go to the top. So it seems to me we have got a lot of the right pieces here, but we haven't yet formalized them in any way.

“Go to the top” remains in the transcript; no design instruction is inferred
from that phrase. The dual/co-design terminology is Joe's hypothesis, not an
assertion that an established mathematical construction has been identified.

## Distinctions grounded in the existing materials

`futon5/holes/M-formal-patterns.md` defines a pattern as a learned generalisation
surviving repeated enactment and review. THEN is the proposed resolution;
NEXT-STEPS are experiments testing its salience, not the resolution itself.
Thus the narrative production rule is an operational representation of part of
a pattern, not its complete meaning, warrant or learning history.

An institutional arrangement specifies roles, permissions, obligations,
information, decision procedures and responses. Its rules can be written down,
just as a pattern can. Neither document acts by itself. Actors enact the
arrangement under material conditions, and their behaviour may depart from the
rules. An institution is therefore not identical to the interpreter program;
a program can implement parts of an institution's interpretation, monitoring
and enforcement. This preserves the IAD distinction between an action
situation and the actors participating in it (primary source linked in the
preceding SESSION-C-institutional-constraints note).

Both conceptions concern repeatable situated practice, conditions, actions and
consequences. Their distinction is functional, not a clean division between
information and activity: patterns propose and justify reusable responses;
institutional arrangements establish how participants may take up, adjudicate,
coordinate and revise responses. Some institutional rules embody patterns;
some patterns concern how to build or change institutions. The relationship
is many-to-many, not one pattern paired with exactly one institution.

| Object | Role in the proposed account |
|---|---|
| Pattern description and warrant | Proposes a response to a tension, explains why, names tests of salience. |
| Cascade | Records selected patterns, authored relations, precedence and provenance; informational input to interpretation. |
| Temperament | Supplies a way of choosing or organising responses; can itself be represented by policy-grain patterns. |
| Institutional arrangement | Specifies participants, authority, observations, obligations and amendment conditions. |
| Interpreter with actors and environment | Resolves applicable rules, proposes/chooses actions, checks authority, performs or refuses effects, and produces observations. |
| Enactment/review record | Tests the response and can motivate revision of either the pattern or the institution. |

## A proposed operational pairing, before claiming duality

For a pattern p in a cascade K, start with a partial rule:

    propose(p, situation) -> candidate action, or not-applicable.

An interpretation step needs more inputs and can have several possible results:

    step(I, K, temperament, state, incoming observations)
      -> (new state, event record), or wait, refuse, unresolved.

I names a versioned institutional arrangement. State includes participants and
roles, outstanding obligations, domain state and relevant history. An event
records the pattern/rule invoked, actor, proposed action, authority assessment,
actual response and observed effect. This is a candidate type/interface, not
yet an implemented generic machine or a probability kernel. Human choice or
an uncertain environment can make the step relational or stochastic.

The complementary operation Joe is pointing to can provisionally be written:

    receive proposal -> establish applicability and authority
      -> enact / decline / request resolution
      -> observe -> review the claimed resolution -> return evidence.

That is a concrete meaning to discuss for “co-production rule”: it supplies
the conditions and response through which a proposal becomes a situated event
and returns evidence. It does not automatically prove the proposal effective.
Local participants' rules and valuations must come from the actual arrangement;
this interface supplies no universal preference ordering or automatic consent.

Calling this a *duality* would require more: specify the two sorts of objects,
their maps or pairing, and laws saying what is preserved under the correspondence.
An evaluator taking a program as input is not, for that reason alone, its
mathematical dual. For now, interpreter/proposal pairing states the operational
claim without presupposing such laws. Candidate laws to examine later are
traceability to an invoked rule, refusal of unauthorised effects, and return of
adequate evidence for the pattern's declared validation question.

Nor does “co-production rule” here select coinductive logic programming.
Coinductive interpretations and coclauses have particular fixed-point semantics;
they do not simply mean consuming ordinary rules. Dagnino, Ancona and Zucca,
*Flexible coinductive logic programming* (2020), distinguishes those semantics:
https://arxiv.org/abs/2008.02140 (bounded abstract check, 2026-09-09).
A persistent interaction can eventually warrant coinductive treatment, but
iteration, interpretation and institutional organisation alone do not establish it.

## The existing Snatch instance

`futon3/checks/find_organise.clj:405-479` already implements `fires?`, `fire`,
`apply-edit` and `construct`. `fire` checks IF and HOWEVER, orders eligible
rules and takes the first THEN that emits a value. `construct` uses that same
operation on cascade state and records edits. In
`futon3/checks/playout_snatch.clj:200-274`, a temperament is a policy-grain
cascade whose execution derives play-grain precedence; `pattern-policy` uses
the firing loop to choose a play action. Thus there is actual interpretation,
not only a graph on display. No new replay or broad correctness audit was run
in this discussion.

`futon3/holes/labs/library-contract/LA1c-restatement.md` is an earlier candidate
formal account of these types and grains. Its September 2 “nothing executes a
policy-grain rule” is historical, superseded at these concrete code sites.
It also states an important institutional consequence: Snatch treatments can
change the observation space itself. An institutional interpreter therefore
needs declared observation domains and cross-domain mappings, not merely a
new weight on a fixed C vector.

Temperament need not be fused into the interpreter: the existing shared loop
consumes ordering data produced by temperament rules. Further policy levels
can use the same loop, but an actual run still needs a chosen initial context,
semantics and authority. Moving authorship up a level does not abolish it.

## Relation to M-formal-patterns and C

M-formal-patterns supplies the enactment/review account and a proposed signed
validation graph at two scales. It does not establish the institution/pattern
duality here. Its own honest bounds leave the real-pattern-to-graph modelling
claim unverified and label the G bridge speculative. In particular, satisfying
every signed constraint is different from merely having a minimum of a scalar
objective: a finite nonempty frustrated system can still have a minimum while
failing to satisfy every bond. No balance/G equivalence is imported here.
The mission is frozen against new slices; this is a read of prior work in the
authorised C sitting, not a new propagator experiment or mission dispatch.

The C registry can assess both the result and how the interaction proceeded:
was a warranted claim produced; was an appropriate review obtained; did the
participant receive feedback? The institutional description supplies the local
subjects and conditions for those questions. Rule compliance and actual
benefit remain distinct. Prediction requires a justified model of this
interaction, not just a cascade and its annotations.

A small next formalisation candidate is the author/reviewer arrangement:
pattern proposes evidence-backed correction; cascade arranges correction,
review and publication; the institutional interpretation assigns roles,
requires the review, records its result and refuses premature publication.
NEXT-STEPS asks whether that process actually yielded warranted, useful work.
This is a proposed example for discussion, not an authorised implementation.
