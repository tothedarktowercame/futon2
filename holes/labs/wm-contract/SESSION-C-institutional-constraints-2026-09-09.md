# C and institutional constraints — 2026-09-09

Status: proposed refinement of DERIVE/ARGUE for discussion with Joe, not a
ruling adopting an institutional schema or enabling implementation.

## Joe, verbatim

> So I was also thinking about. These preferences, and I was thinking, Well, I was talking with my colleagues and friends, Rob and Charlie, and they were saying that maybe... Preferences. Could be thought of as constraints in the optimization problem. So a little bit like how in the definition of a peripheral that we've used before, it gives a constrained execution envelope for an agenda. Process. Possibly the preferences could be thought of as similar. So the naive way to get that in there is in a system prompt. Like, don't talk to me this way, or do talk to me that way. And that's all well and good if what you're working with is an LLM. But if we want to constrain up. Behavior in this more general hybrid model that's not just an LLM. But is... LLM plus a good amount of classical code. Then... Maybe it would be useful to think about patterns. As having more to do with Ostrom style institutions, because Ostrom style institutions... Include action arenas with norms in them. So, I know you were saying that the… Snatch game is different from Effie, and that's true, but if we had an Effie, EFE. Way of thinking about. Institutions, then I think the IAD formalism could be useful in terms of considering institutions as constraints on behavior and ultimately thinking about those constraints on behavior as embodying preferences. So this might be a little bit of a rethink of what we just wrote down. But maybe it's compatible with what we wrote down.

“Effie” is explicitly corrected to EFE in the same turn; no substantive ruling
is inferred from the transcription.

## Compatibility and the proposed refinement

The existing design treats soft preferences as distributions over declared
outcomes and has a future hard-support type. Joe's proposal gives a richer
interpretation of that extension: a preference can motivate an institutional
arrangement that shapes who acts, what they can do, what they can observe,
and who may judge or change the rules. Not every such arrangement belongs in
C. Some belong in the action domain, transition/observation model, or authority
rules. Their justification can still refer to the same preference registry.

Ostrom's own account separates the action situation from its actors and their
valuations. The former includes participants, positions, information,
action–outcome links and control; the latter includes valuations and selection
processes. This supports keeping institutional structure and preference
valuation connected but distinct. Source: Elinor Ostrom, *The Elements of an
Action Situation* (1983), abstract reproduced by Indiana University's Digital
Library of the Commons:
https://dlc.dlib.indiana.edu/dlc/items/b65c61f5-ab48-4ff3-86ab-a6396ad80f95
(accessed 2026-09-09). This is a bounded primary-source check, not a full
institutional literature survey.

A useful proposed classification is:

| Institutional expression | Computational location | Diagnostic question |
|---|---|---|
| Preferred outcome or manner of proceeding | C_i or process-indexed C_i,tau | Is the outcome or process condition satisfied? |
| Prohibited action or mandatory precondition | Admissibility relation / execution boundary | Was the attempted action permitted for this role and state? |
| Obligation with a deadline | Trace/state monitor, fulfillment condition and authorised response | Was it fulfilled, violated, or still pending? |
| Information or participation rule | Observation access, roles and decision procedure | Did the right participant receive evidence and have the specified voice? |
| Sanction or repair | Declared response and transition/payoff model | What response actually followed a violation? |
| Rule amendment | Separate authorised collective-choice process | Who may change this arrangement, with what evidence and review? |

These are proposed implementation locations, not an assertion that every norm
is a hard constraint or that compliance is guaranteed. A social rule can be
violated; the model may need to predict that violation and its response. A
software boundary can block some actions. Record declared rule, enforcement
mechanism, observed compliance and underlying preference separately. Constraints
also encode feasibility, capacity and authority; their existence alone does
not reveal whose preference they embody or whether anyone endorses them.

## Computation and its limits

An eventual constrained selector could have the interface

    choose among pi in Pi_I(s, role), using G_I(pi; C, model).

Here Pi_I is the policy set admitted by the institution's declared rules and
state; I also affects available observations and action–outcome predictions.
This is a design decomposition, not a new implementation or claim that the
current symbolic family supplies a scalar G. If no policy is admissible,
return the conflict to an authorised resolution process; do not soften the
constraint to make selection succeed. Unknown admissibility is not permission.

A hard zero in outcome C and an execution prohibition have different effects:
positive predicted probability of a zero-preference outcome gives infinite KL,
but does not itself stop an actuator. Conversely, an actuator filter may block
an action without predicting its consequences. An obligation across time is
also not fully represented by a one-step permission check. Shared declarative
rules therefore need both diagnostic semantics and the appropriate enforcement
or monitoring points across LLM and classical-code paths. A prompt is one
consumer of the rule, not its authoritative implementation.

## Worked institutional reading of our current pilot

The preference is for warranted claims. An institutional arrangement can give
the author permission to propose a paper correction, require a distinct
reviewer to inspect its evidence, and allow publication only after the required
review. Information rules make the source revision and evidence available to
that reviewer; monitoring records whose review occurred and what was accepted.
A process preference concerns obtaining that review effectively. An outcome
preference concerns the resulting claim being warranted. Neither a review
label nor mere compliance establishes the ultimate usefulness of the paper.

The existing diagnostic can assess parts of this arrangement without becoming
an enforcement engine. For example, the evidence requirement can be satisfied,
unknown, or inadmissible. A future rule monitor must additionally identify the
actor, applicable rule version, authorisation and execution receipt. The
feedback probe then becomes an information-rule question: did the participant
actually receive feedback? Its existing string matcher cannot answer that.

This connects Snatch and EFE at the level of model structure: a game supplies
an explicit action situation; an institutional model can supply another.
Transferring the decomposition does not transfer Snatch's probabilities,
utilities, or evidence of execution to this institution.

## Existing local foundations

- `futon2/holes/M-aif4iad.md:1-106` is explicitly an IDENTIFY proposal for
  institutions with AIF heads: roles/rules/channels plus prediction, monitoring
  and topology revision. Its existence is not evidence of a finished bridge.
- `futon2/holes/M-G-over-cascades.md:66-111` already records Joe's June 23
  connection between cascades, institutional design, ADICO and editable links.
  Its strong correspondences are earlier design claims, not independently
  verified equivalences in this sitting.
- `futon3c/src/futon3c/peripheral/runner.clj:1-12` defines a peripheral as a
  constrained situation of action. `peripheral/tools.clj:27-102` checks tool
  membership and scope before backend dispatch. This is a concrete local
  enforcement point, not a whole-system security or coverage audit.

The proposed change to DERIVE is an InstitutionSpec/RuleSpec linked to
PreferenceSpec, with actor/role, modality, condition, action/outcome, monitoring,
response, amendment authority and version. It must distinguish declared rules
from observations of rules in use. The earlier immutable evidence, unknown
states, semantic probes and upgrade discipline remain applicable.

This also distinguishes two recursive operations: improve our *model* of an
institution, and change the institution's *rules*. A model revision does not
authorise a rule change. Serendipity can motivate either proposal, but the
appropriate review and authority must decide which change, if any, to make.

No main design rewrite, preference adoption, code change or build is made by
this discussion note. The proposed institutional refinement is for Joe's
consideration before amending the accepted solution shape.
