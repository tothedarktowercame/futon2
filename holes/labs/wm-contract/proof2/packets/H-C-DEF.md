# H-C-DEF — a definition of "outcome" that the extractor's E4/E5/E6 defects need

Author: kimi-5, 2026-09-25. Packet H-C-DEF for PROOF-2, answering discovery
packet H-C. Read-only throughout: no mission edited, no click, no write under
`data/`, no load into a shared JVM. The extractor was run in a private JVM
(`clojure -M scripts/wm/extract-outcomes.clj`, futon2 `fafbb933`, mission
sha256 `d13c5cfe…f6fd`).

Inputs: `futon4/holes/mission-lifecycle.md` (read in full),
`futon3c/holes/missions/M-futon-seams.md` (1002 lines),
`futon3c/holes/labs/M-futon-seams/item6/mission-C.edn` (the owner's reference,
6 outcomes, 14 served-by mappings), `proof2/packets/H-C-D.md` (defect classes
E1–E6, D1–D2), and the extractor's current output (16 outcomes).

## 1. What the lifecycle document says — and the typed absence it carries

`futon4/holes/mission-lifecycle.md` never defines "outcome" in the sense C
needs. The word occurs only in the PSR/PUR boilerplate, where it means the
recorded result of applying a pattern:

> "Pattern Use Record per application — outcome, prediction error, surprises"
> (VERIFY checklist); "For each pattern selected via PSR … write a Pattern Use
> Record after the pattern is in place: outcome (success / partial / failure)"
> (INSTANTIATE checklist).

That is a **typed absence**: `{:absent :lifecycle-never-defines-outcome}`.
The definitional gap H-C-D §4 recorded as D1 is real, and it originates
upstream of A9 — the lifecycle document itself has no word for the object.
What it has is four anchors from which the definition can be built:

1. **The mission's purpose is a state change of the world, not of the
   mission.** Opening paragraph: "A futonic mission is a scoped unit of work
   that moves the stack from a known state to a better one." The object C
   ranges over is a property of the "better one" — a state of the stack/world.

2. **IDENTIFY names the discrepancy an outcome closes.** "**Motivation:** What
   discrepancy between ideal and actual prompted this?" An outcome is the
   ideal-side of one such discrepancy, stated as obtainable.

3. **IDENTIFY separates completion criteria from the gap.** "**Completion
   criteria:** Testable conditions — how will we know it's done?" — and
   INSTANTIATE makes the separation operational: "Go through each criterion
   from IDENTIFY and confirm it's met with evidence (not assertion)."
   Criteria are testable conditions *about the mission being done*; they are
   mission-internal and are checked against demonstrations the mission itself
   stages. They are not states of the world the mission wants to obtain.

4. **MAP states what an evidence sentence is.** "The MAP phase is research.
   It produces facts, not decisions." A fact about the present or past —
   what Rob had to do, what the Python splitter reimplements — is an input to
   the derivation, not an output the mission wants.

No phase's exit criterion speaks of outcomes of the world; every exit
criterion speaks of the mission's own state (a human agrees the gap is real;
someone could implement from DERIVE alone; every completion criterion has a
concrete demonstration; the docbook is navigable). That silence is exactly
why the reference, the hand table, and the extractor diverged on whether the
mission's statements about its own method are outcomes (H-C-D §3.1–3.2).

## 2. Proposed definition

Grounded in those four anchors, and in A9's requirement that C be "the
prospective preference distribution over **outcomes for each candidate
universe**" (`PROOF-2-ASSUME-draft-2026-09-24.md` L78) — i.e. over states of
a universe, not over properties of mission artefacts:

> **An outcome is a sentence of the mission that**
>
> 1. **states a property of the world after the mission that does not hold
>    now** — a feature of the "better one" the mission moves the stack to —
>    whether stated positively ("so a VS Code implementation could reuse the
>    core functionality"), as the cost of its absence ("every later
>    implementation must impersonate the first"), or as a capability contrast
>    ("With hardcoded code you can grep for the literal; with a hardcoded
>    prompt you must match natural language at runtime");
> 2. **closes a named discrepancy between ideal and actual for an
>    attributable party** — Rob, Joe, the system, or whoever brings the next
>    implementation (the IDENTIFY Motivation anchor);
> 3. **is checkable by a world-check** — evidence gathered outside the
>    mission document (Rob runs the stack with no shim; grep finds the
>    coupling; a second client produces a byte-identical record), in the
>    manner of the reference's `:checkable-by` fields — and *not* by
>    inspecting the mission's own artefacts; and
> 4. **is not discharged by the mission completing.** A mission can reach
>    COMPLETE — every completion criterion demonstrated — while an outcome
>    fails in the world, and an outcome can hold while the mission is still
>    in flight. Completion criteria belong to the lifecycle's exit lines;
>    outcomes belong to C.

The three confusable kinds, under this definition:

- **Method sentence** — an imperative or rule addressed to the mission's own
  process or artefacts ("the exit is: the interface is declared…", "**Do
  not** mint the abstraction…", "a judgement must be recorded in a form
  something else can contradict"). Its subject is the mission or its method;
  it is checkable by reading the mission and its artefacts; it is discharged
  by enactment. These are wants *at mission grain* (H-C-D §4 D1's proposed
  amendment), and the lifecycle licenses them as exit criteria and DERIVE
  constraints, not as outcomes.
- **Evidence sentence** — a past- or present-tense report of what is or was
  ("Rob had to add configurable room and agent names…", "The Python one
  already had to reimplement sentence splitting"). MAP "produces facts, not
  decisions." Evidence *motivates* an outcome; deleting it loses a reason,
  not a want. Test: the sentence is true before the mission starts and stays
  true after it completes.
- **Completion criterion** — a testable condition for a phase exit or for
  the mission being done ("how will we know it's done"). Mission-internal by
  the INSTANTIATE anchor; belongs to the exit lines, not to C.

Proposed amendment to PROOF-2a item 6's finding (superseding D1's draft,
same direction): *"each outcome a state of the world the mission wants to
obtain, attributable to a party and checkable outside the mission document,
distinguished from the mission's constraints on its own method and artefacts
(which are wants at mission grain) and from the facts that motivate it."*

## 3. The 10 extras, classified under the definition

The extractor at `fafbb933` emits 16 outcomes. 6 match the reference
(`:o-1`↔`:vs-code-implementation-possible`, `:o-2`↔`:second-implementation-
is-cheap`, `:o-3`↔`:joe-can-use-robs-work`, `:o-4`↔`:rob-can-run-the-stack`,
`:o-5`↔`:no-drifting-forks`, `:o-8`↔`:coupling-visible-to-tooling` — E2 and
E3 are fixed). The 10 extras, with ids from the current output:

| id | cue | quote (abbreviated) | class under §2 | why |
|---|---|---|---|---|
| `:o-6` | L93 | "With roles that is a property of the binding, not a string comparison in the dispatcher." | **other — finer-grain facet of a reference outcome, evidence-voiced** | It states a post-outcome world property, but it is the tail of the sentence the mission itself introduces with "**Evidence it is needed:**" — it is instance 4's facet of `:rob-can-run-the-stack`, which the reference already carries. Not a new outcome; an E1-residual consolidation candidate. |
| `:o-7` | L97 | "Rob had to add configurable room and agent names, then an option *not to start an IRC server* … and finally ran a `matrix-ircd` adapter to mimic IRC" | **evidence** (E5) | Past-tense report of what Rob did; true before the mission started. It is the evidence *for* `:rob-can-run-the-stack`. |
| `:o-9` | L128 | "The intended end state is the abstract path absorbing the hardcoded one and the flag disappearing." | **method** (E4) | A rule about how the mission's own enactment of instance 6 must end — checkable by inspecting the artefact (is the flag gone), discharged by enactment. Its world-content (no two drifting sources) is already the reference's `:no-drifting-forks`; what remains is a mission-grain constraint. |
| `:o-10` | L138 | "The Python one already had to reimplement sentence splitting to match the elisp." | **evidence** (E5) | Measured drift, already happened; the mission cites it as the reason for instance 7. Motivates `:no-drifting-forks`. |
| `:o-11` | L167 | "the exit is: the interface is declared, **at least one existing caller is converted to it**, and there is a test of the form…" | **method / completion criterion** (E4) | The mission's own exit rule for whichever instance is picked — "how will we know it's done" in the lifecycle's sense. Checkable by reading the mission's artefacts. |
| `:o-12` | L169 | "**Do not** mint the abstraction and leave the hardcoded path alive indefinitely." | **method** (E4) | An imperative constraining every instance's enactment. Discharged by the mission behaving, not by the world changing. |
| `:o-13` | L289–290 | "What this mission develops is a **capability**, not a list of instances: find / an undeclared seam, choose how to declare it, do it, and check what was done." | **method** (E4) | A description of the mission's own method (the DERIVE loop). Its subject is the mission. |
| `:o-14` | L300–301 | "**In:** a person who could not do something — Rob had only Codex and the code / was hardcoded to talk to Claude." | **evidence / restatement** (E5) | DERIVE step 1's input specification, restating `:o-4`'s cue as a method input. True before the mission. |
| `:o-15` | L708–710 | "a judgement must be recorded in a form something else can / contradict" | **method** (E4) | A constraint on the form of the mission's artefacts, applied to the mission's own candidates two lines later ("Apply it to the two candidates:"). |
| `:o-16` | L778–779 | "`exemplar/check-ledger.edn` (claude-10) — every check run recorded with what / was true, so the checks themselves can be scored." | **evidence / artefact property** (E5) | A property of an artefact that already exists; the `so … can` is about scoring checks, a mission-internal capability, not a state of the world for any named party. |

**Zero of the 10 extras is an outcome the reference missed.** Under the §2
definition every extra fails on clause 1 (states a past/current fact), clause
3 (checkable only inside the mission's artefacts), or clause 4 (discharged by
the mission completing). The reference's 6 are exactly the sentences that
pass all four clauses. The extractor's E4/E5 precision defects are real, and
they are fixable against this definition: a cue hit whose sentence is
past-tense first-person-plural narrative, an imperative, an "exit is" rule,
or a description of the mission's own method is not an outcome row.

## 4. E6: a served-by rule that does not use section containment

Containment fails in both directions on this mission, and the failures are
visible in the current output:

- **False negative:** `:o-5` (`:no-drifting-forks`) is cued at L22 in "## The
  tension", mission-grain prose outside every instance section, so
  containment emits `:unlinked`. The reference gives it **5 of its 14
  mappings**, across three instances: `5/:adapter-conformance-test`,
  `6/:one-authority`, `6/:flag-retired`, `7/:writers-converted`,
  `7/:divergence-test`. The mission's own prose supports each link without
  any containment: instance 6 names `mfuton_prompt_override.clj` "a second
  source of truth that must be kept in sync with a dependency map by hand"
  (L127–128) and instance 7 names the drift concretely — "The Python one
  already had to reimplement sentence splitting to match the elisp" (L138),
  the `>>>` block-quote convention (L136, L991) — the very drift the
  outcome's cue prices.
- **False positive:** containment links `:o-9` and `:o-10` to the wants of
  instances 6 and 7 purely because the sentences sit inside those sections.
  Under §2 neither is an outcome at all, so the links vanish for the right
  reason — but a containment rule has no way to say so.

**Proposed rule — served-by by shared named artefact.** A want `w` of
instance `i` serves outcome `o` iff:

1. `o`'s cue span(s) name or pronominally carry an artefact — the coupling
   whose hardcoding the outcome states as the obstacle (a file, a protocol,
   a convention, a flag, a provider string); and
2. instance `i`'s section names the same artefact as the thing `w` declares,
   converts, or retires; and
3. the direction matches: `w`'s enactment removes the obstacle `o`'s cue
   states (`:how` in the reference is exactly this argument, written out).

An outcome whose artefact no instance section names emits
`{:unlinked :no-shared-artefact}`; an instance with no cascade keeps
`{:absent :no-cascade}` (instance 8's prospective link to
`:joe-can-use-robs-work` is preserved as a typed absence, as all three
readings already do).

Worked example, containment vs artefact rule on `:o-5`: containment says
*unlinked* because L22 sits in "## The tension". The artefact rule says
`o`'s cue ("Two implementations that have already drifted cost more to
unify…") carries the artefact class *second implementations kept in sync by
convention*; instances 5 (adapters must impersonate IRC), 6
(`mfuton_prompt_override.clj` kept in sync by hand, L127–128) and 7 (the
`>>>` parser convention, reimplemented in Python, L136–138) each name an
instance of that class and a want that retires it — so the rule derives the reference's 5 links from the mission's
own words, and the reference's `:how` paragraphs become checkable arguments
rather than judgements.

## 5. Summary for the record

- The lifecycle never defines "outcome" — a typed absence, quoted above; the
  definition in §2 is built from its four nearest anchors plus A9's
  "outcomes of candidate universes".
- Under that definition, the reference's 6 are exactly right, and all 10 of
  the extractor's extras classify as method (5: `:o-9` `:o-11` `:o-12` `:o-13`
  `:o-15`), evidence (4: `:o-7` `:o-10` `:o-14` `:o-16`), or a finer-grain
  facet of an admitted outcome (1: `:o-6`). None is an outcome the reference
  missed.
- E6's fix is the shared-named-artefact rule of §4, with `:o-5` as the case
  where containment gives the wrong answer (0 links vs the reference's 5)
  and `:o-9`/`:o-10` as the cases where it gives wrong answers in the other
  direction.
