# The AIF-generality / formal-specification gap — and two paper consequences

Joe, dictated 2026-09-03 late evening, while the ruling-execution rows build.
Three connected threads, verbatim anchors preserved.

## 1. Futon 2026: the supplement move and the thesis sentence

> "if we wanted to kind of move some of that material into a supplement, we
> could cut a lot of it out and just replace it with... a simple statement
> saying you shouldn't expect to build a system that actually works
> according to the design you have in mind unless you have something
> approaching a formal specification, then you build to that."

The repair corpus moves to supplementary material, which then functions as
a **capability demonstration** relevant to Part 3's business considerations:

> "all this work we've put in to make the war machine work... will be a kind
> of demonstration of a capability that we have. Which is available to
> anyone who knows how to use Clojure and Lean together. So that's...
> potentially a trainable skill. But it's still perhaps a little bit
> special, and it's something that I didn't start out knowing two weeks ago."

## 2. PLoP 2026: the paper described a system that didn't hold water

> "I had left off thinking I had built a working system that was all
> validated according to the informal specification... and then only later
> when I did the audit associated with Futon 2026 and this concept of
> closing the machine over the operator, did I realize that the PLoP 2026
> paper was a description of a system that didn't really hold water. And so
> I think we need to go back and think about how to revise the PLoP 2026
> paper so that we don't just send in exactly the same design patterns and
> say, yep, it works now. Because that would be leaving out a considerable
> amount of intellectual effort, and also maybe leaving out some crucial
> design patterns that would be aligned with the way we've gone about
> building this system."

## 3. The tension (minted to the ledger from this note)

> "if we were to think about all of that using the active inference
> framework itself, we might realize that we had a missing R-node, or that
> R20 actually needs to be put in terms of some kind of formal
> specification. But that's really weird, because formal specifications
> don't exist in nature. An active inference is supposed to be a model of
> how any intelligent system works. So there's a gap between the active
> inference framework's claims to generality and our observations that
> without a formal specification you're not able to use AIF in
> computational work."

Ledger id :programme/T5, :born-of :operator-dictation, statement verbatim
from the blockquote above.

## 4. Rob's case: the rewrite problem needs TWO specifications (Joe, same evening)

> "Folks will have a working system... written in Forth... or a banking
> system written in COBOL. And someone will come along and say... let's
> rewrite it in Rust... they could take the old code as a form of
> specification and say, well, our new rewrite should at least be as
> performant as the old code is in this set of circumstances. And that
> becomes a translation problem. And what Rob has noticed is that doing
> something like translating the Lean system, which is written in C, into
> Python, using LLMs is possible, but challenging... you would have thought
> LLMs... they're just like an even better version of Google Translate...
> Rob has found that's not the case. So you need the original code as a
> specification, but you also need a specification of the transcompilation
> process. And I think that's sort of what we've developed with our
> tetrahedron model, albeit an informal specification rather than a formal
> one... that rewrite-code-or-improve-code problem is one that comes up in
> business settings quite a lot."

Two-spec structure: (1) the old system as EXTENSIONAL spec (which recorded
behaviours must survive, over a declared envelope of circumstances); (2) the
PROCESS spec (what maps to what, what counts as equivalent, how conformance
is checked and staged) — the tetrahedron's role. The campaign's machinery
(live pins, negative controls, dated-additive windows, kept-vs-ruled-changed
verdicts, the fence) is a working instance of (2). Feeds Part 3.

## 5. The retrodiction: zaif was already a paradigm rewrite (Joe, same evening)

> "we've kind of tried something already a little bit like this idea with
> our zaif work with claude-2, where we said, here's a specification of AIF
> that we're using in the war machine. Now let's rewrite it for agents as a
> harness... because the war machine itself is described as a supplemental
> harness for off-the-shelf coding agents. It just didn't anticipate that
> we'd be getting a raw API like we get. So it is kind of a rewrite. Just
> not from one programming language to another, but from one programming
> paradigm to another."

Checked against the record, the mapping holds piece by piece: the WM's AIF
spec played "old system as extensional spec"; the changed envelope was raw
API access (unanticipated by the original description); SPEC-zaif-harness-v1's
lifecycle-to-R-node mapping table WAS the process spec, with the
anti-glibness rule ("a mapping row without a unit test is decoration and
does not ship") as its conformance discipline; the U10 per-node matrix is
the conformance audit; the per-node rows (13/13) are the per-behaviour
verdicts; the boards are the ledger. Carried tension T3 ("formal claim or
fertile analogy") is exactly the two-spec question -- is the mapping a
conformance contract or an inspiration -- and the campaign has been cashing
it toward the contract side without saying so: per-node tests green,
the U11 shared-schema identity condition as a cross-paradigm conformance
clause. Consequence for Part 3: language-to-language rewrite is the SPECIAL
case; paradigm rewrite (monolith->services, batch->streaming, tick-loop->
agent-harness) is the general and commercially commoner one, and the case
study for it already exists in this repo's history.

## 6. What even IS a formal specification (Joe, same evening, continuing)

> "I think a formal specification is really one where it says, with this
> level of generality, this is what's true. So... it wouldn't really have
> mattered from the point of view of the Lean specification if we had
> written the war machine in Python. Or C. Or C#. Or Lean itself used as a
> programming language. Because the formal specification doesn't say
> anything about that. And I think in the same way, there's probably a kind
> of formal specification of what this kind of agentic work is or does,
> that would be true at the level of the mission lifecycle and true at the
> level of an AIF lifecycle. And that probably has to do with... the model
> of the system itself basically being held by the operator. The operator
> is someone who's got a sense of where this is all going, and they've got
> a sense of how it relates to their livelihood or their sustenance. And
> that's perhaps important if we think about some of the parts of this
> build which have looked underdetermined. Well, maybe they're
> underdetermined in a technical way because we haven't done that closure
> operation."

Sharpened: a formal spec at level L = the statement of what is INVARIANT
under every implementation choice below L (formality = quantification over
implementations; the spec is the quotient of the system by
"doesn't-matter"). Choosing L -- choosing what to forget -- is itself a
preference statement, which is why the tower tops out in the operator: the
highest level (purpose, livelihood, sustenance = the viability envelope,
AIF's C at organism grain) is held informally. UNDERDETERMINATION =
UN-CLOSED-NESS: a build part looks underdetermined exactly where the tower
has a gap between the written levels and the operator-held level. The
closure operation = eliciting and writing the next level up. CONCRETE
CLAIM, checkable against the record: the J-row mechanism is a formal
DETECTOR of un-closed spec -- the :bar condition (a), "not decidable from
sources, code, or a prior ruling", is literally the machine reporting it
has hit the boundary of the written tower; the twelve J-rows ruled this
campaign are twelve closure operations performed piecewise, and the zaif
paradigm rewrite transferred cleanly along exactly the already-written
invariant levels (per-node semantics, refusal/evidence disciplines) while
requiring new decisions exactly at the unwritten ones (channel adapters,
zero-support behaviour -- which then needed Joe).
