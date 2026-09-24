# H-EXITS-D — how the two test missions state their completion criteria

Packet for PROOF-2a hole H-exits (flight-loop unit 4b): *take the mission's
completion criteria as the flight's wants* (`PROOF-2a-THEOREM-draft-2026-09-24.md`,
"A flight" rule 1: "The flight's wants are the mission's completion criteria (for a
mission document, its phase exits), read from the mission text. The flight ends when
every criterion is met with checked evidence."). Discovery only: no code, click, or
data write. Author: kimi-6, 2026-09-24. Anchors: futon2
`610498a508beac84efec51b430325e23ee1d8865`, futon3c
`bfe16c2a60104819f06c14217a5f6c2605d3bdda`.

## 0. What reads wants today

`futon2.aif.mission-registry/open-holes` (`src/futon2/aif/mission_registry.clj:172–200`)
retains four kinds of line from a live mission document: `:unchecked-task`
(`- [ ] …`), `:work-marker` (TODO/FIXME/TBD/"open hole:"/sorry), `:pending-lifecycle`
(a lifecycle token on the same `.;/|`-free span as pending/next/open/blocked/
in-progress/remaining/not-yet/needed), and `:open-section-item` (list items under an
"open questions / remaining work / next steps" heading). `futon2.aif.mission-hole-wants`
(`src/futon2/aif/mission_hole_wants.clj`) then projects **only** `:unchecked-task`
items into flight wants, each with a C4 locator whose `:decl` is the same line with
the box ticked (`closed-form`, `hole-locator`); `observation-checks/check-decl-in-file`
(`src/futon2/aif/observation_checks.clj:52–70`) witnesses closure by
`decl-present?` — the decl must start a line at the resolved commit. Everything else
is retained and reported, never projected, because "no check can witness closure: the
item carries no checkbox to flip" (namespace docstring).

`futon4/holes/mission-lifecycle.md` defines the completion-criteria vocabulary a
mission is *supposed* to use: an IDENTIFY checklist item "Completion criteria:
Testable conditions — how will we know it's done?", a per-phase **Exit criterion**
sentence, INSTANTIATE's exit "Every completion criterion has a concrete
demonstration", DOCUMENT's exit about docbook discoverability. None of those forms is
a `- [ ]` line in the mission body, so today's reader sees none of them.

## 1. Mission M-aif-policy-conditioned-eig

File: `futon2/holes/missions/M-aif-policy-conditioned-eig.md`. Status line (line 4):
"**Status:** **IDENTIFY agreed in principle; pure kernel instantiated; generative
contract open**" → `classify-status` lead token IDENTIFY → `:identify`, live.

### 1.1 The eleven completion criteria (lines 77–104, section "### Completion criteria")

All eleven are stated the same way: **(a)** prose bullets of the form
`- **C<n> — <name>:** <claim about the world>` — a "completion-criteria section",
not checkbox lines, not phase exits, not acceptance evidence.

**(b) Exact spans** (first line of each bullet; each runs 2 lines):

- L79: `- **C1 — Parameter identity:** `θ` has a versioned, machine-readable domain and`
- L81: `- **C2 — Observation closure:** every policy in the epistemic candidate set has`
- L84: `- **C3 — Normalisation:** priors, predicted observations, and posteriors are`
- L86: `- **C4 — Bayesian coherence:** `Σ_o Q(o|π)Q(θ|o,π)=Q(θ|π)` within declared`
- L88: `- **C5 — Shared updater:** simulated and realised posterior updates call the`
- L90: `- **C6 — Policy conditioning:** at least two policies induce demonstrably`
- L92: `- **C7 — Reduction tests:** an uninformative policy has EIG zero; a perfectly`
- L95: `- **C8 — Provenance:** traces carry model identity, prior entropy, expected`
- L97: `- **C9 — Prospective calibration:** held-out log loss/Brier score evaluates`
- L99: `- **C10 — Replacement discipline:** `model-uncertainty-bonus` is not removed`
- L102: `- **C11 — Honest R18 update:** Box 1 changes only after live provenance and`

**(c) What `mission_hole_wants` reads today: none of the eleven — and worse, none is
even *retained*.** The bullets carry no `[ ]` (not `:unchecked-task`), no TODO/TBD
marker, no lifecycle token, and "### Completion criteria" is not an
`open-section-heading-pattern` heading, so `open-holes` does not match these lines at
all. The 11 criteria are invisible to the retained-holes count as well as to the
projection: the flight's want set for this mission is empty of the mission's own
definition of done.

**(d) Locators.** No C4 locator can decide any of C1–C11, for a typed reason per
class:

- *World-claim, evidence-named-elsewhere (C5, C7, C9, C10):* the claim is that some
  code/behaviour exists ("simulated and realised posterior updates call the same
  implementation"). C4 reads text presence in one file; the criterion is decidable
  only by running a named test or reading a named evidence artifact. The mission
  does not name the artifact per criterion, so today there is no locator; the
  missing definition is *which record discharges the criterion*. **H-exits
  instance.** (Partial exception: C5's work is recorded done in the `- [x]` item at
  L195 — "Completed 2026-09-21: `a4a/update-posterior` …" — but that is a prose
  discharge note, not a checkable locator.)
- *Operator-judgement criterion (C11, and C10's "Joe decides" half in §3 Stage 4):*
  "Box 1 changes only after live provenance and prospective evidence distinguish
  'kernel exists' from 'epistemic value is load-bearing'" — the criterion is a
  disciplined *act*, not a state; a locator can witness that Box 1 changed, never
  that the evidence preceded it. Typed reason: `:criterion-is-a-discipline-not-a-state`.
  **H-exits instance** (no decidable want), bordering **H-C** (the outcome must be
  read from run provenance at click time, which the mission does not yet state as
  data).
- *Calibration criterion (C9):* requires held-out runs that do not yet exist; any
  locator would read a future artifact. Same typed absence as above.

### 1.2 The machine-proposed instantiation items (section at L183+)

**(a)** Checkbox lines — the only completion-shaped lines in the file.

**(b)** L195 `- [x] **Mint the shared posterior updater, then unify the two paths through it.**`;
L214 `- [x] **Give the risk consumer a typed `Q(o|pi)` boundary …`; L245
`- [ ] **Collect a default-off EIG shadow and calibration packet.**` (span continues
to L253; an "Implementation advance 2026-09-22" note inside it states the item
"remains open until a preregistered held-out run supplies the persisted empirical
packet and replay artifacts").

**(c)** The two `- [x]` lines are not retained (the pattern requires an unchecked
box). The single `- [ ]` at L245 **is** the one criterion of this mission the flight
reads today: kind `:unchecked-task`, observable, projected with locator
`{:class :C4 :repo "futon2" :sha "HEAD"
:path "holes/missions/M-aif-policy-conditioned-eig.md"
:decl "- [x] **Collect a default-off EIG shadow and calibration packet.**"}`.

**(d)** That locator is *decidable but not faithful*: the checkbox flips when someone
ticks it, while the criterion's own text conditions closure on a held-out run that
the box does not witness. See the falsifier in §4. The faithful locator would need
the artifact the note names (the persisted empirical packet), which the mission does
not pin to a path — typed absence `:evidence-artifact-unnamed`.

### 1.3 VERIFY/INSTANTIATE plan (numbered list, ~L172–180)

Five numbered plan items ("Preregister the first θ …", "Add generative-model
validators …", …). **(a)** numbered list items; **(c)** invisible — numbered items
match `list-item-pattern` only under an open-work heading, and "VERIFY / INSTANTIATE
plan" is not one; **(d)** these are plan steps, not stated completion criteria, so
their invisibility is correct, noted here only so the census is complete.

## 2. Mission M-futon-seams

File: `futon3c/holes/missions/M-futon-seams.md`. Status line (L3): "Status: HEAD
complete; IDENTIFY complete; INSTANTIATE-1 complete for instance 4 … MAP/DERIVE/ARGUE
in progress; VERIFY met in substance". Lead token HEAD → `:open`, live. The file
contains **zero** `- [ ]` lines.

### 2.1 IDENTIFY exit — the instance-4 completion criteria, in prose (L159–171)

**(a)** Phase-exit prose under its own heading "## IDENTIFY exit (when picked up)".
**(b)** L169: "For whichever is chosen, the exit is: the interface is declared, **at
least one existing caller is converted to it**, and there is a test of the form
\"redirect the binding and confirm behaviour follows\" (instance 1's method)."
Three conjuncts: interface-declared, caller-converted, redirect-test.

**(c)** Missed by projection (no checkbox). The conjuncts *are* machine-checkable
and were in fact checked — but as **want tokens in a hand-written click artifact**,
`holes/labs/M-futon-seams/exemplar/click-001.edn:121`
`:want #{:caller-converted :redirect-test :prefix-routing-retired}`, not as anything
read from this prose. This is the H-exits hole in its purest observed form: the
mission states three decidable criteria in prose, and the flight's wants existed only
because a human transcribed them into an EDN.

**(d)** Locators are constructible for all three conjuncts because the checks exist
and leave named artifacts:

- caller-converted: `{:class :C4 :repo "futon3c" :sha "HEAD"
  :path "holes/labs/M-futon-seams/exemplar/click-001-enactment.edn" :decl …}` over a
  verdict line, **provided** the artifact is amended to carry one line-start verdict
  token per want (today the wants' verdicts are embedded in table prose in the
  mission and in nested EDN — see falsifier §4 for why mid-line tokens do not work).
  Absent that amendment: typed reason `:verdict-not-line-anchored`.
- redirect-test: same shape; the check is `futon3c.agency.roles-test` (run, not text).
  A C4 over the mission file can witness the *claim* "passed —" (L727), never the
  test. Typed reason: `:criterion-is-a-test-run-not-a-text-state` — this is an
  H-C-shaped criterion (read outcome at click time from a record), satisfied today
  by a human-written record.
- prefix-routing-retired: genuinely C4-like in the negative (absence of
  `str/starts-with?` provider branches across ~12 files) — i.e. it is a grep check
  over the tree, which is a different check class than decl-presence in one file; no
  single-file C4 locator decides it. Typed reason `:criterion-is-tree-wide-absence`.

### 2.2 The phase exits, stated as "**Exit criterion:** … **Met/Not met**" lines

**(a)** Phase-exit prose with an inline verdict token — the lifecycle's form.
**(b)** exact spans:

- L197: `**Exit criterion:** every MAP question has a concrete answer; the ready-vs-missing table is complete. **Met.**`
- L284: `**Exit criterion:** someone could implement the mission from the DERIVE section alone … **Met for the tooled steps; not met for four steps that are done by hand**`
- L498: `**Exit criterion:** the design feels *inevitable* given the constraints, not merely *possible*, and someone outside the project can understand … **Not met, and both halves now say why rather than promising more work.**`
- L643: `**Exit criterion:** the design has been checked against available structural constraints; unverifiable risks spiked; DERIVE revisions recorded. **Met.**`
- L674: `**Exit criterion:** every completion criterion has a concrete demonstration, and a new person could reproduce it from the mission doc. **Met for instance 4** … Not met for the mission, which has seven other instances.`
- L773: `**Exit criterion:** someone browsing the docbook can discover what this mission built without knowing it exists. **Not started.**`
- L780: `### Proposed additional exit criterion (Rob, via Joe, 2026-09-24)` — DOCUMENT to be discharged by building a seam for the annotation-of-user-inputs feature; proposed, not adopted.

(The HEAD exit exists only in `futon4/holes/mission-lifecycle.md` and is restated as
data in `holes/labs/M-futon-seams/lifecycle.edn` (`:phases` rows, each with an `:exit`
string and a `:status` keyword, e.g. `:exit-met`); the mission file does not restate it.)

**(c)** All missed by projection (no checkboxes). Two are *retained* as holes: the
Status line matches `:pending-lifecycle` ("MAP/DERIVE/ARGUE in progress"), which is
kept and reported but never projected — correctly, since flipping it is not
witnessable. The exit lines themselves match no pattern ("Not met" / "Not started"
are not in the pending-word list; "Met for instance 4 … Not met for the mission"
carries no listed pending word adjacent to a lifecycle token), so five of six phase
exits are wholly invisible.

**(d)** A C4 locator over the criterion text itself is the canonical *bad* locator
(§4) — it reads true from the day the section is written, in both the Met and the
Not-met state, because `decl-present?` anchors at line start and the line carries the
same criterion sentence under either verdict. The discriminating content — `**Met.**`
vs `**Not met**` — is mid-line, which `decl-present?` cannot anchor, and is identical
text across phases, so even a mid-line match would not identify *which* exit is met.
Typed reason for all six: `:verdict-token-not-locatable-by-C4` — the criterion is
stated, its verdict is stated, and neither is in a position the check can read. These
are H-exits instances for MAP/DERIVE/VERIFY/INSTANTIATE/DOCUMENT and (once the
outcomes must be read from records at click time) H-C instances for the four whose
evidence is test runs rather than document state. DOCUMENT's exit ("someone browsing
the docbook can discover…") is additionally an operator/outsider judgement:
`:criterion-requires-a-reader`.

## 3. Census

| mission | stated completion criteria | forms used | read today | missed today |
|---|---|---|---|---|
| M-aif-policy-conditioned-eig | 11 (C1–C11) + 3 checkbox items (2 done) | completion-criteria prose section; checkboxes | 1 (the `- [ ]` shadow item, unfaithfully — §1.2d) | 11 prose criteria invisible even to retention; 2 checked items correctly absent |
| M-futon-seams | 3 IDENTIFY-exit conjuncts + 6 phase exits + 1 proposed | phase-exit prose with inline verdict; prose conjuncts transcribed by hand into click-001.edn | 0 | all 9 (3 conjuncts exist as wants only via hand transcription) |

## 4. Falsifier: the bad case

A locator that reads true **before** the criterion is met:

```clojure
{:class :C4 :repo "futon3c" :sha "HEAD"
 :path "holes/missions/M-futon-seams.md"
 :decl "**Exit criterion:** someone browsing the docbook can discover what this mission"}
```

`decl-present?` anchors the decl at line start and finds it at L773 today — while the
same line ends "**Not started.**" The check reports the DOCUMENT exit *met* on the
day the section was written, and stays "met" forever, because the criterion sentence
is invariant under the verdict. Same bad case for the other five exits and for any
`C<n>` criterion sentence in M-aif-policy-conditioned-eig (e.g. `:decl "- **C5 —
Shared updater:**"` reads true from 2026-07-13 onward). Heading-matching variants
(`:decl "### Completion criteria"`, `:decl "## DOCUMENT"`) are worse of the same
kind. The general shape: **C4 witnesses that a criterion was *stated*, not that it
was *met*** — stated and met are different text spans, and today only the stated span
is line-anchored. (The one projected want, §1.2, has the mirror defect: its met-span
—the ticked box—is locatable but unfaithful, because the item's own text conditions
closure on an artifact the box does not witness.)

## 5. Proposed amendment (to how the flight reads wants)

**Amendment A-exits.** Extend `mission-hole-wants` beyond `:unchecked-task` as
follows, keeping the namespace's existing discipline (retain everything, project only
what a check can witness, hand-written declarations always win):

1. *Recognise two more stated forms as criteria:* (i) bullets under a heading named
   `completion criteria` (any level); (ii) lines matching `**Exit criterion:**` —
   each with the phase taken from its enclosing `## <PHASE>` heading. These become
   retained holes of kinds `:completion-criterion` and `:phase-exit`, so the 20
   currently invisible criteria above enter the coverage account (`retained-by-kind`)
   even where they cannot be projected.
2. *Project one only when it carries a decidable met-span at line start.* A criterion
   is projectable iff the mission names, on the criterion or in the mission's
   completion section, (a) an evidence artifact path and (b) a line-anchored verdict
   token (e.g. a per-criterion status line `- [ ] C5 — …` / `- [x] C5 — …`, or an EDN
   record whose verdict value starts a line). The locator is C4 over that artifact
   with the verdict token as `:decl`. Criteria without both are retained with a typed
   reason (`:verdict-not-line-anchored`, `:evidence-artifact-unnamed`,
   `:criterion-is-a-test-run-not-a-text-state`, `:criterion-is-tree-wide-absence`,
   `:criterion-is-a-discipline-not-a-state`, `:criterion-requires-a-reader`) — never
   projected, never defaulted to false.
3. *Consequence for the two missions as written:* under the amendment,
   M-aif-policy-conditioned-eig projects its existing `- [ ]` item (unchanged) and
   retains C1–C11 with typed reasons; M-futon-seams retains 9 criteria and projects
   none until the missions name verdict artifacts — the honest reading, since the
   three instance-4 wants that *were* discharged were discharged into
   `click-001-enactment.edn`, which the mission does not cite as its
   completion-of-record for the phase exits.
4. *Mission-authoring corollary (for the mission format, not the reader):* a
   completion criterion that the machine is to read should be written as a checkbox
   line whose text names its discharging artifact, e.g.
   `- [ ] **C5 — Shared updater:** witnessed by a4a-test shared-updater fixture`.
   This makes stated-span and met-span the same line and keeps the falsifier of §4
   out by construction.

This amendment is the unit-4b half of H-exits; it does not touch H-C (reading the
mission's *outcomes* at click time), which several criteria above still require.
