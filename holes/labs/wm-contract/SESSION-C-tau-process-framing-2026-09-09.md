# E-C-realization: process preferences and Cascade Live

2026-09-09, Joe with codex-12, emacs-repl. Continuation of
[the first map](MAP-C-realization-2026-09-09.md). Phase remains MAP: this records
Joe's orientation and adds relevant existing objects for examination.

## Joe's framing, verbatim

> So the way I've thought about this C vector, I'm not really sure it's a vector, first of all. Um, the other aspect of it that we should take into account on the list here is we've got F12F. Sorry, F10F, C defined. We've also got U88i derived C tau. So, I think we need to also include ctau, because the last time I thought about c... I was thinking... We may have a preference for certain outcomes or for certain types of outcomes. But we've also got a process-based preference for how we get to those outcomes. Namely, we should proceed towards those outcomes effectively. And I think that it's very hard to distinguish fully the final outcomes from that process-based view of proceeding towards the outcomes. Because if you were guaranteed that you could proceed towards the outcomes effectively... Then... The outcomes would become realized. But anyway, one of the things we did to prepare for this work was to start to look at the Cascade Live page, which is served by Cady. And includes some high-level problems that we're attempting to work on here. And from a point of view of high level goals or orientation, addressing that problem and giving it an effective breakdown in terms of other problems that can be addressed. Effectively. I would state that as the main preference as to how we would assemble that view. I would say this is related to what's going on with the library mining work we've been doing, where we've developed how and why links between design patterns. Because those how and why links help to explain how and why we would make choices or actions that we would see as effective at achieving those high level. Problem Resolution Aims. And I think ultimately, like I've said before, this is related to the way in which G is computed over. Design Patterns in Cascades. And it somewhat reverses the flow. Or anyway, requires a somewhat different calculational implementation over similar objects. So with G... We've been thinking about patterns as production rules. And in terms of preferences, I think there should be a fairly standard way of thinking about how. Production rules lead to satisfaction criteria, and I think one could think of this in terms of something like a SAT solver or a logic prover. So that gives an initial framing, and in terms of the empirical data, like I said, the empirical data starts from Cascade Live

Transcription decoding, with evidence: `worklist.edn` contains `:id :F10`,
`:class :F`, statement beginning “C, defined”, and `:id :U88`, `:class :I`,
statement beginning “DERIVE C_tau FROM THE CASCADE LIVE PAGE”. Thus “F10F”
and “U88i” are read as row plus class, not newly named subrows. Joe corrected
F12 to F10 himself in the passage. “Cady” is read as Caddy, consistent with
Item 18e of today's walkthrough and the existing page pin.

## Orientation to carry into the map

This is a paraphrase for discussion, not a replacement for Joe's words:

- C need not be assumed to be one finite vector. The preferred object includes
  outcomes, types of outcomes, and effective progress toward resolving problems.
- Cτ must be examined alongside terminal preferences. Process preferences here
  concern effective contribution to problem resolution, not only the machine's
  channel-health targets. The existing first map's channel/process association
  is therefore too narrow to stand as the whole process account.
- The high-level orientation starts with Cascade Live's problem statement and
  an effective breakdown into addressable problems. The relation between that
  breakdown and actual resolution matters, not merely the existence of a tree.
- Authored how/why links and patterns as production rules are relevant existing
  material. Joe proposes examining satisfaction through something like a SAT
  solver or logic prover, with a calculational direction related to, but
  different from, evaluating G over cascades.

This does not assign band weights, choose a SAT encoding, define a probability
model, or enable a production fold. It also does not identify Cτ's time index
with depth in a problem decomposition; that connection remains to be described.

## Empirical starting point checked this turn

Read `/var/www/zone.hyperreal.enterprises/wip/pipeline-pattern-cascade.html`,
the local file identified by the existing Caddy-page record. Extracted its
embedded `apex-thesis.json` using a JSON decoder and compared the parsed object
with `runs/U88-cascade-live/apex-thesis-pinned.json`: **equal**.
The HTML SHA256 remains
`a93ec6fb56be548b632f137ee01544b6b3dfdfd9adca2b6ea943a437fa4be47d`.
This verifies the local served-page artifact, not a fresh HTTP delivery.

The apex asks:

> Can a community of humans and machines doing open-ended work leave an account of itself structured enough that the work can steer itself -- so that activity becomes inference rather than motion?

| Cluster | Name | Pinned members |
|---|---|---:|
| A | Records carry warrant | 6 |
| B | One queryable self-account | 4 |
| C | Feedback reaches every participant | 4 |
| D | Accountable next-action choice, including goal formation | 5 |

These members are the recorded induction base. The pin does not itself show
that satisfying all members is sufficient for the apex, or that each is a
necessary condition. Those logical relations cannot be inferred from membership
alone. The pin also preserves the caveat that A–D stand independently if the
apex is rejected.

## Additional existing calculations relevant to Joe's framing

| Material | What is implemented | What it does not establish |
|---|---|---|
| `futon3/checks/find_organise.clj:167`, `read-repository` | Reads authored edges; defaults to `@why`, accepts explicit edge-kind selection; records out-of-repository targets and cycles. Builds the `:stands-on` relation over read patterns. | An authored how/why relation alone is not an executable implication proving that a lower-level outcome satisfies a higher-level criterion. |
| `futon3/checks/construct_cascade.clj`, `antecedent-holds?`, `cascade-of`, `library-correspondence` | Existing pattern applicability, cascade construction, and correspondence checks over library records. | Construction and correspondence do not alone supply success probabilities or a proof that the apex is realized. |
| `futon3/checks/how_kernel_snatch.clj:32`, `supporto` | A core.logic relation identifies possible bindings for a particular how-edge, with a negative mirror case. | This is a domain-specific witness, not an encoding of Cascade Live's problem hierarchy. |
| Same file, `kernel:49` | Assigns and normalizes weights on those bindings from declared Beta priors and success/failure evidence. Separates logical support from probability mass. | Its Beta(1,1) prior is explicitly stipulated. It is not authority to add mass to the excursion's pinned zeros or a fitted WM observation model. |
| `futon3/checks/derive_q_snatch.clj` and `ablate_g_snatch.clj` | Derive outcome distributions from policy playouts and declared counterpart priors, and evaluate risk/information quantities. | Their game outcomes are not automatically the satisfaction conditions of the Cascade Live apex and clusters. |
| `mathlib4/DarkTower/WarMachine/PreferenceLadderDraft.lean:34`, `PreferenceFamily` | A time-indexed family over observation/terminal alternatives, with terminal support constraints. | It provides neither instantiated problem satisfaction predicates nor a logical/probabilistic connection from authored pattern edges to those predicates. |

## Questions exposed by this framing, for subsequent mapping

1. For each pinned problem, where is its satisfaction criterion recorded, and
   what observation or witness would establish it?
2. Which how/why links state contributions, alternatives, prerequisites, or
   sufficient conditions? Which have executable production-rule semantics?
3. Given a desired resolution, what existing calculation can trace the required
   lower-level conditions and applicable rules? What happens when no rule or
   witness supports the connection?
4. Where do rule execution evidence and observations supply probabilities of
   satisfying those conditions? Logical possibility and empirical likelihood
   are separate inputs even when they concern the same cascade.
5. Which intermediate achievements constitute effective progress, how are they
   indexed in time, and how do they relate to terminal satisfaction without
   counting the same benefit repeatedly?

These are MAP questions generated from Joe's framing, not decisions or requests
to settle every question now. No registry/worklist entries changed. Validation:
parsed worklist row identities; compared page/pin JSON and HTML digest; inspected
the listed source definitions; checked Markdown whitespace and local links.
