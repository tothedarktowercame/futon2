# Find-out-before-building is selected but not enacted as discovery (2026-11-06)

## Finding

The outer selector is doing what its pattern says at the *choice* boundary: it
constructs `find-out-before-building` as the executable cascade
`[{:status-gate :undetermined} :by-rank]`, chooses an undetermined target, and
retains that cascade identity.  The ordinary runner then loses the cascade's
meaning at the *enactment* boundary.  It independently constructs the target's
reading-policy cascade and sends the ordinary implementation prompt.  Thus an
outer discovery policy becomes an inner implementation action.

This is not merely missing prose in the author prompt.  It is a missing typed
join from the selected outer cascade to the kind of action the runner enacts.

## 1. Where the class is decided and how far it travels

The task-selection library is loaded and pinned by `outer-menu/load-library`;
only mapping rows marked `:executable` become menu cascades
(`src/futon2/aif/outer_menu.clj:46-85`, `:103-140`).  The mapping row supplies
the cascade ID, pattern names, and executable steps; `construct-cascade` turns
the patterns into pinned units but does not interpret their THEN text
(`outer_menu.clj:94-101`).  In particular, a status gate is a real interpreter
step, not prose (`outer_menu.clj:24-44`).

`outer-select/select` applies every menu cascade to the field, scores each
result, groups equal predictions, and chooses the unique minimum below
abstention (`src/futon2/aif/outer_select.clj:322-400`, `:405-465`).  The
explanation row retains both the chosen class and its member cascade IDs
(`outer_select.clj:103-124`, `:126-147`).  `giraffe-order`,
`find-out-before-building`, and `serves-a-way` are therefore alternative
selection arguments, not action kinds.

The selected class is carried into the ordinary run.  Runtime attaches the
Giraffe and makeup pins, focus, and `:selected-relation` to the entire retained
selection (`src/futon2/aif/full_loop_runtime.clj:1691-1719`) and passes it under
`:outer/selection` (`full_loop_runtime.clj:1503-1514` shows the same carrier on
reselection).  The real held record
`data/wm-runs/tick-run-record-2026-11-06-a9fa3407-56f7-419f-aee7-126ad2a95c0b.edn:1`
contains all of the following:

* `[:outer/selection :cascade-id] = :find-out-before-building`;
* `[:outer/selection :selection :selected :target] = "M-formal-patterns"`;
* a `:why` class whose `:cascades` is `[:find-out-before-building]`; and
* the fully pinned menu construction and argument frame.

It is also retained later in the run's repeat identity, but only through a
small projection including `:cascade-id` and `:selected-relation`
(`src/futon2/aif/full_loop_runner.clj:10247-10262`; the same real record at
line 1 has `[:outer-repeat-comparison :inputs :outer-selection :cascade-id]
= :find-out-before-building`).  So the identity is not absent from the run
record.

This was not peculiar to M-formal-patterns.  The records
`data/wm-runs/tick-run-record-2026-11-06-7c7eb934-9871-4d8b-8bdb-7c37803cc32d.edn:1`
and
`data/wm-runs/tick-run-record-2026-11-06-b5ad33e3-8f30-46ce-ae1c-28fafef8c2cf.edn:1`
both retain selected target `M-superpod-mark3` with outer `:cascade-id
:find-out-before-building`.

It does **not** become the action's execution mode.  The inner selector's
`selected-entry` carries its own action, canonical cascade, and enacted steps
(`full_loop_runner.clj:3199-3250`).  `construct-selected-action` then builds a
`:selected-cascade` construction solely from that inner `:cascade-candidate`
(`full_loop_runner.clj:3533-3575`).  The a9fa3407 record's `:decision` action is
`M-formal-patterns/policy-d6e451d8989ec8cda92b`, with precedence beginning
`pattern-coherence/scope-boundaries`; it has no outer
`:find-out-before-building` action-kind field
(`tick-run-record-...a9fa3407....edn:1`).

## 2. What reaches the author

The primary prompt is assembled by `author-prompt`
(`src/futon2/aif/full_loop_runner.clj:4855-4864`) and dispatched at
`:author-dispatch` (`full_loop_runner.clj:10120-10141`, `:10274-10285`).  It
unconditionally says:

> Implement the enacted step ... Change the actual mission/code world.

and requires committed artifacts plus `FULL_LOOP_AUTHOR: DONE <sha>`
(`full_loop_runner.clj:4884-4912`, `:4950-4971`).  Its `PATTERN CASCADE` is
rendered from `cascade-entry` (`full_loop_runner.clj:4924-4932`), which is the
inner construction above.  Neither `author-prompt`'s arguments nor its selected
construction keys include `:outer/selection`, `:cascade-id` from the outer
receipt, or the pattern's THEN text (`full_loop_runner.clj:4855-4860`,
`:4926-4932`).

The outer pattern itself is unambiguous: “puts a discovery task (read-only,
spike, or sample run) ahead of the build” and “Select the discovery as its own
task (find out and propose; no edits), review its answer, and then select the
build” (`/home/joe/code/futon3/library/task-selection/find-out-before-building.flexiarg:9-13`,
`:17-27`).  That effect/THEN is pinned as source evidence by the outer menu but
is not rendered or compiled into an author contract.

The empirical contrast is exact.  The a9fa3407 record retained the discovery
class but dispatched the implementation action, and ultimately records the
author's `:guardrail-refusal` (`tick-run-record-...a9fa3407....edn:1`).  A good
discovery output instead gives, per item, what it is, why Giraffe could not
decide, and one of `current`, `blocked`, or `likely outdated`, with technical
questions and file:line evidence, while making no target edit
(`holes/tickets/T-giraffe-undetermined-triage.md:25-33`).  Its M-superpod-mark3
row demonstrates `current` with concrete next work and no questions
(`T-giraffe-undetermined-triage.md:37-45`); its blocked rows carry explicit
decisions/options/evidence (`:47-58`, `:60-71`).

## 3. What Lean requires

`SelectionArgument.lean` formalises argument acceptance only.  `Argument` has
exactly `id` and `G`; attacks and preference-based defeats determine the
grounded extension (`mathlib4/DarkTower/WarMachine/SelectionArgument.lean:21-66`).
Its concrete frame contains the three class IDs and stopping, and proves only
that `serves-a-way` is grounded for that historical score table
(`SelectionArgument.lean:130-150`).  It contains no action, THEN, discovery,
read-only, review, or enactment field.

`R20Step.lean` is equally selection-local.  A `SelectionCascade` is merely a
list of gates and orders (`mathlib4/DarkTower/WarMachine/R20Step.lean:88-125`),
and `servesAWay` is represented as those executable steps
(`R20Step.lean:132-148`).  The module says explicitly that the candidate field
and Giraffe facts are observations and the click “records only the selection
computed from them” (`R20Step.lean:10-18`).  Its selection theorem establishes
only membership in the offered field and preservation of that field
(`R20Step.lean:1087-1096`).

Therefore no current Lean definition says that selecting an argument obliges
enactment of that pattern's THEN.  The Clojure-to-pattern semantic join Joe is
asking for has no Lean statement today.  Lean proves which selector wins and
which target it returns, not what operational mode that selector imposes.

`RefusalLegality.lean` does constrain a no-work ending.  A legitimate question
must have a cited ambiguity and be in the brief, and every click-legal terminal
must record an exclusion (`mathlib4/DarkTower/WarMachine/RefusalLegality.lean:61-99`,
`:101-108`).  With unchanged evidence the exclusion removes the target; changed
evidence restores it (`RefusalLegality.lean:146-165`).

## 4. Smallest change set, one behaviour per packet

### FOBB-A — retain an enactment mode

At outer selection close, derive a typed mode from the **chosen outer cascade
ID**, not from target status alone: `:find-out-before-building` maps to
`:discovery`; the other cascades map to the existing `:implementation`.  Pin
`{:mode :discovery :outer-cascade-id :find-out-before-building :selection-sha
...}` on `:outer/selection`, and carry it unchanged through `selected-entry` and
`construct-selected-action`.  Refuse if a chosen class has multiple member
cascades with incompatible enactment modes; do not guess from a representative.

Red tests: an outer find-out choice reaches construction as `:discovery`; a
giraffe-order/serves-a-way choice remains `:implementation`; loss or conflict
of the pinned mode is typed refusal.  The existing seam
`test/futon2/aif/outer_selection_wiring_test.clj:135-154` is the narrow starting
point.

### FOBB-B — discovery-specific dispatch

Branch before `author-prompt`.  A discovery prompt must say read-only/no edits
to the selected target; require a typed result such as
`FULL_LOOP_DISCOVERY: {:schema :wm/find-out-result-v1 :target ... :items
[{... :verdict :current|:blocked|:likely-outdated :evidence [...] :questions
[...]}]}`.  Its rubric should be the one already demonstrated at
`T-giraffe-undetermined-triage.md:25-33`.  It must not request
`FULL_LOOP_ARTIFACTS`, `DONE <sha>`, or “Change the actual ... world.”  The
ordinary implementation prompt remains unchanged.

Red tests: real outer discovery receipt produces the discovery prompt and no
commit contract; an implementation receipt still produces the current prompt;
malformed/missing verdict rows are typed failures; the target repo head and
write manifest are unchanged across the discovery turn.

### FOBB-C — review and retain the discovery answer

Give the reviewer a discovery rubric, not code-review instructions.  Validate
the schema, target and selection pin, evidence locators, complete item census,
and the distinction among current/blocked/outdated.  Persist a
`:wm/find-out-result-v1` receipt plus independent review in the run record.  A
`current` verdict proposes the later build but does not dispatch it in the same
click: this preserves the pattern's checkpoint between finding and code.

Red tests: an unreviewed answer cannot close; a reviewed complete answer can;
the same click cannot enter author implementation or change the target; a
later click can see the reviewed verdict as new selection evidence.

### FOBB-D — close discovery honestly

A completed investigation is **not a refusal**, so it should not be squeezed
into C2.  Add a typed successful `:discovery-complete` outcome whose grounding
is the pinned result/review receipt rather than a code commit.  Extend the
requirements checker to require: selected outer mode `:discovery`; valid
result schema; independent review; target source pin; target repo unchanged;
and no implementation dispatch.  It should expect no target artifact commit.
The result becomes observed evidence for Giraffe/selection: `current` permits a
later build candidate, `likely-outdated` requests disposition, and `blocked`
supplies the blockers.

This outcome needs a new Lean contract (owned separately): current
`RefusalLegality` enumerates only three *refusal* terminals and sends anything
else to debugger error (`RefusalLegality.lean:110-142`).  A successful
discovery should instead be modelled as productive work with a non-code
artifact/receipt, not added as a fourth refusal.

Red tests: reviewed discovery closes without a commit; absent review, target
mutation, or implementation dispatch fails requirements; it does not create a
C1/C2/C3 exclusion merely because discovery completed.

### FOBB-E — route actual ambiguity through the existing C2 wire

When a blocked discovery finds a mission-text ambiguity that requires Joe, its
question must use the existing `FULL_LOOP_AUTHOR: QUESTION` carrier with exact
span, at least two alternatives, Morning Brief receipt, and C2 exclusion.  That
is the correct legal close: Lean's `.legitimateQuestion`, not
`:discovery-complete`, because the click cannot finish its discovery verdict
without the operator decision.  The exclusion remains keyed to the unchanged
mission evidence and new bytes restore the target
(`RefusalLegality.lean:72-89`, `:146-165`).  A merely uncertain or unsupported
claim is not C2 and remains a debugger error.

The existing carrier is singular.  If a per-item discovery can produce several
blocking questions, a separate packet must either (a) define and validate one
consolidated cited decision, or (b) extend C2 publication/exclusion to a
reviewed vector.  It must not silently discard all but one.

Red tests: a blocked result with a resolvable exact cited ambiguity closes C2
and excludes; a plain blocker without citation does not; a current or outdated
verdict never masquerades as C2; changed mission bytes invalidate the citation
and restore eligibility under the existing C2 law.

## Bottom line

The WM is entitled to choose an undetermined item: the selector correctly
recognises discovery value.  The defect is that selection-class semantics stop
at the retained receipt.  The smallest coherent repair is to make that class a
typed enactment mode, dispatch and review a non-editing discovery result, close
successful discovery as productive non-code work, and reserve C2 for a cited
ambiguity that genuinely needs Joe.
