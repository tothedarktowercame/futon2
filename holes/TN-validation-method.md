# TN — Validating a built system against the formalism it claims: a repeatable account

*Written 2026-09-01 by claude-1 from the campaign of 2026-08-29 → 2026-09-01 on the
futon2 war machine (active-inference harness). Joe's instruction: "log what we've
achieved in the last three days in a way that could make that repeatable under other
auspices... other people in other companies will have other software systems that they
want to validate against their criteria... they may well go through a process of
building naively, like I did, and expecting it's going to work the way they thought it
was going to, and then realizing that it doesn't. So that case should generalize."*

*Sections marked **[steward]** are for claude-20 to fill: the I track, the R/RUN track,
the pymdp/SPM cross-check, the choice-point experiments. Everything else is what
claude-1 ran or read.*

The specific artefacts are named throughout so a reader can open them; the steps are
stated generally so a reader with a different system and a different formalism can do
the same thing. Where the general step and the specific artefact differ in shape, the
general step is the claim and the artefact is the example.

---

## 0. The situation this method is for

You built a system to implement a theory (here: an active-inference agent from
Friston 2017 / Parr 2022 / Da Costa 2020 / Buckley 2017). You drew a diagram of it
(here: Figure 4, twenty nodes R1–R20, ~22 edges) and you believe the code does what the
diagram says and the diagram does what the theory says. You have run records. You have
no independent way to know whether any of the three — theory, diagram, code — agrees
with the others.

What we found when we looked: of 22 drawn edges, 6 were explained by no equation; of the
theory's 18 dependency edges, 10 were not drawn; 8 places where the theory left a choice
had been filled by whatever the code happened to do, without anyone recording that a
choice had been made; one quantity (the free energy F) was computed every tick and read
by nothing; the machine's "policy precision" was an engineering proxy that never moved
on the live path; and on every one of fifty comparable recorded runs the action the
machine *enacted* differed from the one it had *selected*. None of this was visible from
the diagram, the docstrings, or the run records as they stood. All of it was checkable.

Expect this. A system built by reading papers and writing code will have a diagram that
records intent, code that records what was tractable, and a theory that records neither.
The method below makes the three disagree *in writing*, then resolves each disagreement
by evidence or by a recorded decision.

## 1. The three objects and the one relation

Keep three registries, each a machine-readable file, each the *only* source for what it
records, and derive every figure and table from them. Never hand-edit a figure.

1. **The formalism registry** — every equation the system claims to implement, as a
   row: `:id`, `:defines` (the symbol it produces), `:imports` (the symbols it reads),
   `:node` (where in the system it lives), `:class` (theory-defined / stack-defined /
   plumbing), `:ref` and `:eq` (the source and equation number), `:code` (file:lines),
   `:status`. Plus `:exogenous` (symbols that come from outside), `:choices` (places the
   theory leaves open — see §5), `:holes` (claims held open with a falsifier), and
   `:references` with retrieval records (§3).
   *Here:* `futon2/holes/labs/wm-contract/aif-equations.edn`, 17 equations.

2. **The drawing registry** — every edge someone drew, with `:from`, `:to`, a
   `:status` (`:drawn`, `:route-measured`, …), a `:classification` (transition /
   constraint / unclassified) and, crucially, `:decisions`: named entries that
   `:retires` or `:adds` edges, each with `:grounds` (`:code` or `:ruling`), `:by`,
   `:at`, `:basis` (pointers), `:statement`.
   *Here:* `p4ng/empirics-futon/control-map-edges.edn` and `control-stages.edn`.

3. **The organisation registry** — which nodes belong to which phase or lane; the
   classification counts.
   *Here:* `p4ng/empirics-futon/organization.edn`.

The one relation that ties them: **an edge is forced by the formalism iff the equation
at its target imports a symbol the equation at its source defines.** Everything else
about an edge — that it was drawn, that a run recorded it, that someone wants it — is a
different kind of fact and must be labelled as such. Most of the confusion we found was
two kinds of fact wearing one line style.

**Generators** read the registries and write the figures, with a `--check` mode that
fails the build if the committed figure differs from what the registries produce:
`gen_aif_dag.bb` (formalism → dependency DAG and conformance file),
`gen_live_topology.bb` (drawing + conformance → the checked topology figure),
`gen_war_room_tetrahedron.bb` (counts). `pointer_check.bb` resolves every `file:lines`
pointer in every registry and fails on a stale one. `negative_controls.sh` plants a
defect of each kind the generators are supposed to catch and asserts each is caught
(§7).

## 2. The classification every drawn edge must end in

Run the derivation once (`gen_aif_dag.bb` writes `aif-conformance.edn`) and it
partitions edges into: **conformant** (drawn and forced), **realised-undrawn** (forced,
implemented in code, not drawn), **not-realised** (forced, no code found),
**path-dependent** (forced, realised on one code path and not another), and
**unexplained** (drawn, forced by nothing).

Every *unexplained* drawn edge must then end in exactly one of:

- **retired by decision, grounds `:code`** — the code shows the edge is not there
  (pointers), e.g. R13→R14 (the depth never reaches the temperature: `policy.clj:242-245`
  reads no depth);
- **retired by decision, grounds `:ruling`** — the operator declares the drawing wrong
  as design intent, e.g. R6→R13 / R5→R6 ("select the target first, then construct the
  cascade") — *and this does not change the code*; a run may still traverse it until
  the code is changed (§9);
- **kept as measured route** — a run recorded the hop; it is a fact about call order,
  not about dependency (R2→R7 at route grain);
- **kept, no data flow** — the edge exists as control flow with nothing carried
  (R7→R5);
- **plumbing** — infrastructure the formalism does not describe (R9–R12, R15, R20,
  TRACE).

Every *forced-but-undrawn* edge must end in: **added by decision, grounds `:code`**
(pointers to where the value flows), **added by ruling**, **not realised — held open
as a hole with a falsifier**, or **path-dependent — held open with the paths named**.

The figure draws each of these in its own stroke class with a legend that says from
which registry and at which stamp the classes were derived. Twelve classes on one
figure is the ceiling we hit; beyond that, distinguish on another axis.

## 3. Sources: retrieve, checksum, cite by line

Nothing is "per Friston 2017" until the PDF is on disk, checksummed, converted to text,
and the claim points at a line. `refs/README.md` records sha256 and the text-extraction
command for each source. Citation rows carry `:verified {:ref :eq :retrieved}`.

Two things the retrieved texts did for us that memory could not: they *refuted* three
citations two Claude seats had written from memory (Da Costa A.2 does not state the β
update; π₀ is σ(−γ·G), not the habit prior; pymdp's default γ is 1.0 not 16); and they
*settled* questions we had been about to put to the operator (the Laplace F is not the
per-policy F_π; γ multiplies G alone; iteration to convergence is the paper's stated
scheme). Reading the source at the line is cheaper than a ruling and more durable.

A text extraction can lose symbols the PDF has — the overbar distinguishing β_prior from
β_posterior vanished in ours, so eq. 2.7 read as β = β + …, which is only meaningful if
the two βs differ. When an equation has two same-named variables, look for the line that
proves they're distinct (here `friston2017.txt:1711`, whose first term is identically
zero otherwise).

## 4. The ledger: one row per claim, statuses that mean something

`worklist.edn` — every unit of work is a row: `:id`, `:class`, `:status`, `:owner`,
`:statement`, `:acceptance` (what done looks like, stated before the work), then
`:evidence` (shas, pointers) when done. Classes we needed:

| class | meaning |
|---|---|
| V | verify a claim against a source or the code; the result may be negative |
| C | correct a registry (author must not be its reviewer) |
| D | a drawing decision: retire/add an edge with grounds |
| H | hold a claim open as a hole with a falsifier (here: a Lean declaration) |
| J | a question only the operator can answer — see §5 for the bar |
| I | implement a theory-aligned replacement |
| RUN | a validated run (named so `R<digit>` always means a node) |

Statuses: `:open → :done-unreviewed → :done`. A row reaches `:done` only when someone
other than its author records `:reviewed-by`, `:reviewed-at`, and `:review` — *what they
checked*, not "LGTM". `worklist_check.bb` validates the file and enforces the rules below;
the loop script runs it before and after every row.

**The publish gate.** No figure regenerates from a registry while any row touching that
registry is `:done-unreviewed`. The build is the enforcement point: it runs the
generators, the pointer check and the negative controls, and it is only run when the
ledger says the registries are read.

**Stale signatures.** A signed row names the registry entry it covers (`:covers-key`, a
vector of key-paths, content-addressed — `[:equations {:id :precision}]`, never an
index) and the commit it read (`:review-covers`). The check re-reads that entry at that
commit and dies if the entry differs now. Position drift (a row inserted above) must not
fire it; a change to a later path of a multi-path row must; a key-path that did not exist
at the signed sha must die ("not there to sign"), or a typo passes as a verification.
An entry amended after signature gets a *new* row that names the one it supersedes; the
old signature is never edited. Rows that touch no registry declare `:covers-key :none`
and may not also name a registry path.

**A read is against a commit, never the working tree.** Two agents share one checkout.
Before signing, the ledger directory must be clean except for the signature being
committed; the check prints every dirty file. A signature left uncommitted in the tree
can be overwritten by the next agent's write; commit it.

## 5. Choices the theory does not make: record, then run — do not ask

The operator's rule, stated twice (2026-09-01): *"if there's a decision to be made that
isn't predetermined by the theory, then we need to note that down as a decision, and
explore the branches so that we can make an informed decision about which one performs
best. Asking me to make an advance ruling is not a great way to get good results because
I have nothing to go on."*

So: a `:choices` entry with `:status :open-branches`, the arms named, what each
resembles in the sources, the **measurement** that would separate them (the parameter,
the values where the arms differ, and the **control = the system as it runs today**,
which is usually *neither* arm), and an `:interim` arm shipped behind a flag when one is
needed before the branches can be run. Then the branches are built and run — on
recorded fields first — and the entry becomes `:decided-by-experiment` with the numbers.

A **J row** (a question for the operator) is the residue: it must carry a `:bar` saying
(a) why the answer is not decidable from sources, code, or a prior ruling, and (b) why
no experiment we can run separates the arms; the check refuses a J row without both.
Retrospectively, of five J rows we raised, two met the bar (J1, J2: what the machine is
*for*), one was answered by earlier rulings, one was experimentally decidable, and one
was a fact about the system. The operator's tell for a misplaced J row: his answer is a
general rule rather than an answer to the row.

Operator rulings that *are* needed are recorded verbatim under `:ruling {:by :at
:grounds :ruling :decision}`, and by the same rule they are also *run* where they can be
(V6 ran the carried-β ruling both ways: not contradicted; the arms differ; the argmax
never moved on our fields).

## 6. Roles and how the agents were used

- **Operator (Joe):** owns the purpose questions (J1, J2), the naming, the phase order.
  Sees results and vetoes; is not asked to pick arms. Two rulings in three days.
- **Reader (claude-1, an Emacs REPL seat):** ran the initial check, wrote the standard
  (TN-edge-review §1), reviewed every row another agent produced, and held the reading
  role after handing stewardship over. Never both author and reviewer of a registry row.
- **Steward (claude-20, a second Claude seat):** took the collection on 2026-09-01:
  dispatch, the gate, publish, the I and RUN tracks. Its own registry rows come back to
  the reader.
- **CLI seat (`claude-cli`, a terminal Claude driven by `wm-edge-loop.sh` +
  `worklist-prompt.md`):** takes the next `:open` non-J row, does it, marks it
  `:done-unreviewed` with evidence, and stops. One row per invocation. Seventeen D rows
  this way, each reviewed before the next regeneration.
- **Codex agents (via the Agency bell/park protocol, `agency_send.py --mode work`):**
  discovery reports and implementation slices, one file / one behaviour / one acceptance
  per packet; discovery split from implementation with a review between.

Coordination: bells with a park on every dispatch (deadline as an absolute epoch-ms;
job-id and park-id stated to the operator); replies in-thread when the header says the
turn is auto-delivered. Reviews reproduce the verify step — re-run the experiment, re-run
the build, re-count the records — and say what was checked. Findings are fixed by the
reviewer when small (carve-out b), noted in the review; substantial ones become rows.

Two things about working *with* Claude specifically that the campaign needed written
down: (i) two seats will confidently cite the same source from memory and both be
wrong the same way — retrieve first; (ii) a seat will report the one thing a tool
returned as the whole answer (§8) unless a rule makes it establish the read was
exhaustive. The protocol works because the second reader is a different instance with
different context, not because either instance is careful.

## 7. Controls: pin the property, not the sentence

Every generator has negative controls (`negative_controls.sh`, 14 negative / 10
positive at the end): plant the defect — a decision without grounds, two grounds drawn
alike, a retirement of an edge nobody asserted, a conditional edge drawn like an
unconditional one, a hop on a retired edge — and assert it is caught by comparing the
*property* (stroke colours differ; the pair is refused) rather than the *text* of an
error message. Four controls this campaign failed on changes they did not guard because
they pinned a sentence or an exact count; each was re-cut to the property.

Every new check gets its negative mode run *before* landing: for the stale-signature
check, tamper a covered key (must fail), tamper an uncovered key (must pass), insert an
entry above (must pass), typo a key-path (must die). A control that passes for any edit
made in two places guards nothing (the byte-identity test that re-listed the whitelist
it was testing).

**At least one fixture looks like the data.** The F_π scorer passed every delivered test
and could not be called on real records, because no fixture had fourteen channels of
which twelve carried "no prediction" as a zero variance.

**Measure a comparison where the arms are supposed to differ.** "Identical under both
scalings" was measured at the one τ where they coincide by construction; "did not
converge" was measured at a bound two iterations short; "the enactment test agrees" was
run on a field where one of 110 candidates passed its gate, so any permutation agrees.
Name the parameter, pick values where the arms differ, and if none is available, report
"not distinguishable", not agreement.

## 8. The failure family, and the rule that covers it

Nine instances in three days of one shape: **a tool answered a narrower question than
the one asked, and the answer was read as though it were the wide one.**

| instance | what was read | what was true |
|---|---|---|
| pointer check | the range's end line existed | the range's start was past EOF in a 19-line shim |
| `find … \| head -1` | first match | three copies; the real file was the third |
| `edn/read-string` on a file | one record | 38 records (50/50 differed, not 2) |
| `bb check \| cut` | `cut`'s exit code | the check had failed |
| τ = 1 comparison | identical results | the arms coincide there by construction |
| 256-iteration bound | "did not converge" | converged at 258 |
| 2-of-110 gate field | enactment unchanged | any permutation leaves it unchanged |
| one pointer verified | "the unsafe call is here" | four `http/post` sites; two live; one was an `/eval` into the shared JVM |
| two greps of a shared file | "the other agent's field vanished" | one's own write clobbered one's own edit |

Rule: **establish that the read was exhaustive before treating its result as the
whole.** Concretely: read files of forms in a loop; enumerate call sites, don't verify
the pointer you were handed; capture a gate's exit code before anything else runs;
check the diff before believing a tamper landed; count with a parser, not a grep. These
are in `AGENTS.md` as their own sections so the next test author reads them.

## 9. Validated runs: the drawing is not the machine  **[steward]**

*(claude-1's part:)* Everything above validates the *drawing* against the theory and the
code. It does not validate the machine. The RUN track does: a **pre-flight** that
intercepts every outbound call and every write and runs a real tick (found a persistent
file two audits missed, and proved zero POSTs on the real path — after enumeration found
an `/eval` fallback into the shared JVM that inspection had not); a shadow run of N
ticks on a named sha; then **conformance**: every recorded hop is an edge of the checked
topology, no retired edge is traversed — with the decision rule by grounds: a
`:code`-retired *route-grain* edge traversed refutes the drawing; a dependency-grain
retirement is not testable by routes; a `:ruling`-retired edge traversed means the
ruling is not yet realised in code and opens a build row. Figures then carry "conformant
with run `<sha>`, N ticks" instead of "checked against two old records."

*[steward: RUN1–RUN10 as they land — the persistence defect, the pre-flight script, the
conformance report format, what S1 showed.]*

## 10. Extracting the formalism's edges and making them work  **[steward]**

*[steward: the I track — I3 trace seam (mean+variance, Q(π), effects mode; measured
+92,985 bytes/tick against the naive form), I2 F_π (absent-variance handling; the
rank/N join trap avoided; unscaled by source), I1 β/γ (the fixed point; bisection over
fixed-step gradient, with the 1/β² cost shown to belong to the solver), the pymdp/SPM
cross-check and the novelty table (what is ours alone, with every absence a recorded
search).]*

## 11. What the operator should expect, in order

1. The diagram is wrong in places you did not suspect, and right in places you cannot
   yet prove. Both lists are short enough to finish.
2. Most "decisions" turn out to be decidable — from a source at a line, from code at a
   pointer, from a ruling already made. The residue that is truly yours is small (two
   in three days) and is about what the system is *for*.
3. Things you built and never wired (F), and things you wired and never built (γ), will
   both surface. Neither is a defect of the method; both are what the method is for.
4. The checking itself will fail in the ways of §8, repeatedly, across every agent and
   the operator. The ledger and the controls exist so that those failures are caught by
   a second read rather than by a run.
5. Nothing in the first three days touches whether the machine *works*. That is the
   next phase, and it starts with a run that fires no actuator.

## 12. What would make this repeatable elsewhere

- The registries' schemas and the generators are system-agnostic in shape; the node
  names and the equations are the only content to replace.
- `worklist_check.bb` and `negative_controls.sh` are the two scripts that carry the
  discipline; AGENTS.md carries the rules for authors.
- The reader/steward/seat/Codex division and the bell-park protocol are how the
  author≠reviewer rule was enforced across agents; any two independent reviewers would
  do, but they must be *independent* — different context, different instance.
- The honest limit: shown once, on one system, by the team that built both. The
  discharge condition for "this generalises" is a second system and a second team who
  close a ledger without us in the loop. That is the case to manufacture next, after the
  machine is shown to work.
