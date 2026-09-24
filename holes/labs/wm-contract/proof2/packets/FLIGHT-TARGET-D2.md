# FLIGHT-TARGET-D2 — the first flight target, chosen by what the reader can see

Discovery packet, read-only. Author: kimi-6, 2026-09-24. Reader under test:
`futon2.aif.mission-criteria` (`criteria`, two args) at futon2
`8e1aa1b1a317b9a79accc54981b00a0a9ead7c0e`; verdict classes per
`verdict-class` (`:met` only on exactly `**Met.**`; partial / not-met /
not-started / not-met-retained / unrecognised / not-stated otherwise). A
criterion with no verdict carries no locator and makes assembly refuse the
target (`:tokens-without-checkable-locator`). Checkbox `- [ ]` wants come from
`mission-hole-wants` via `flight.clj`'s `:checkbox` source; criteria wants via
the `:criteria` source. No clicks, no shared-JVM loads, no edits to any mission.

## 1. Repos surveyed

`find /home/joe/code -path '*/holes/missions/*.md'`, worktrees/copies excluded
(`-old-copy`, `-provenance-recovery`, `-library-loop-*`, `-baseline*`,
`-codex10-*`, `-frame18-control`, `-v4-*`, `-index-check`, `-node-evaluation-trace`,
`-store-warning-hold`, `-L8/L17/L18-review-codex`, `-a-small-model-route-*`,
`wt-bisect`): **futon2, futon3c, futon0, futon3, futon3a, futon3b, futon4,
futon5, futon5a, futon6, futon7** — 11 repos. The 09-24 scan's triage counted
missions in futon3c (98), ticket-registry (44), futon3 (41), futon5 (27),
futon4 (26), futon6 (23), futon3a (3), futon3b (1) and futon2; futon0, futon5a
and futon7 missions are outside the scan universe. M-futon-seams excluded per
the dispatch (and now moot: its Status reads COMPLETE at futon3c `071dee27`,
all six exits `**Met.**`).

Method: `mission-criteria/criteria` over every mission file; per mission:
criteria count, verdict-class histogram, not-stated count, located count
(verdict stated), open count (located and not `:met`), unchecked `- [ ]`
count, Status line. Raw run: script `/tmp/d2_survey.clj`, output summarized in
§2 and §3.

## 2. The headline: the reader can see almost nothing

Across all 11 repos, **exactly one mission has any located criterion:
M-futon-seams** (6 criteria, all `:met`). Every other mission that states
completion criteria at all states them **without verdicts** — 100% of their
criteria are `:verdict-not-stated`, which under the reader's rule means no
locator and assembly refusal. Missions with stated criteria (form: C =
completion-criterion bullets under Acceptance/Success criteria/Exit criteria
headings, P = phase-exit paragraphs):

| repo | mission | criteria (form) | not-stated | located | open | `- [ ]` | status (line, trimmed) |
|---|---|---|---|---|---|---|---|
| futon2 | M-aif-policy-conditioned-eig | 11 C | 11 | 0 | 0 | 1 | IDENTIFY agreed in principle; generative contract open |
| futon2 | M-aif-a-matrix-faithfulness | 12 C | 12 | 0 | 0 | 0 | INSTANTIATE (Stage 2 pending) |
| futon2 | M-aif-gap-epistemic-affordance | 12 C | 12 | 0 | 0 | 0 | IDENTIFY — dependent on M-aif-policy-conditioned-eig |
| futon2 | M-f11-find-production-successor | 6 C | 6 | 0 | 0 | 1 | OPEN — first continuous-backlog item |
| futon3c | M-war-machine-pilot | 8 C | 8 | 0 | 0 | 0 | v0 mission-complete in substance; awaiting operator acceptance bell |
| futon3c | M-omni-wm-runner | 3 C | 3 | 0 | 0 | 0 | (no Status line; chartered 2026-07-26, owner claude-3) |
| futon3c | M-apm-demonstration | 1 P | 1 | 0 | 0 | 0 | HEAD complete; IDENTIFY pending operator acceptance |
| futon3c | E-ticks-firing-ratio-likelihood | 4 C | 4 | 0 | 0 | 0 | EXECUTED |
| futon3c | E-campaign-spec-grounding | 5 C | 5 | 0 | 0 | 0 | SCOPED for handoff to codex-5 |
| futon3c | M-typed-holes | 2 C | 2 | 0 | 0 | 0 | CLOSED 2026-06-15 |
| futon3c | M-social-exotype | 2 C | 2 | 0 | 0 | 8 | archived |
| futon4 | M-essays-edit-cycle | 26 C | 26 | 0 | 0 | 0 | ACTIVE |
| futon4 | M-essays-retraction-visibility | 4 C | 4 | 0 | 0 | 0 | READY TO EXECUTE |
| futon4 | M-essays-diachronic-model | 5 C | 5 | 0 | 0 | 0 | SPECIFIED, NOT YET IMPLEMENTED (draft) |
| futon6 | M-live-efe-map | 4 C | 4 | 0 | 0 | 0 | VERIFY complete 2026-07-04 |
| futon3a | E-fold-engine | 4 C | 4 | 0 | 0 | 0 | DERIVE; fold live as Car-3 |
| futon3 | M-futon1a-rebuild(-scoping-review) | 5+3 C | all | 0 | 0 | 5 | archived |
| futon5a | M-self-improvement-loop | 4 C | 4 | 0 | 0 | 0 | IDENTIFY + DERIVE |
| futon7 | M-becoming-nomad | 1 P | 1 | 0 | 0 | 0 | IDENTIFY; operator-personal content |

Plus ~40 further missions with **no criteria the reader recognises at all**
(only `- [ ]` tasks or nothing): e.g. futon2 M-wm-aif-policy-grain-compliance
(4 unchecked), futon3c M-forum-refactor (27), alleycat-scorecard (20),
M-peripheral-behavior (37), futon0 M-apm-capability-ratchet (23). A flight over
checkbox wants alone is possible for these, but see §4 on false closure.

## 3. Reading of the result

**Tier 1 (closeable faithfully today with 1–2 interpretation requests): EMPTY.**
Typed reason: `:all-criteria-verdict-not-stated` for every live mission with
stated criteria; the one mission with located criteria reads COMPLETE. This is
not a defect in the missions — the verdict-line convention is a day old
(claude-1's `d05cb755` introduced exact `**Met.**` verdicts, on
M-futon-seams) — but it is the truth about what the reader can see today.

**Consequence for FLIGHT-TARGET-D's ranking (correction to my own packet):**
M-f11-find-production-successor was ranked first on the strength of one open
checkbox want. Under the criteria reader its six `## Acceptance` bullets are
`:verdict-not-stated`, so assembly now **refuses the target** until the owner
verdicts them. The one-interpretation gap found there is unchanged, but it is
behind six missing verdict lines.

**Scan admission:** the 09-24 scan admitted only the four declared-source
targets (M-f11, M-aif-policy-conditioned-eig, M-wm-08-external-f2,
T-repair-occ-444fb018…); every mission above is `universe-not-admitted`, and
the E-* excursions are outside the scan universe entirely. The flight driver
takes its target explicitly, so scan admission does not gate a flight — but
the tick's own admission runs inside each click, so it is noted, not
dismissed.

## 4. False-closure check on the candidates

For each tier-2 candidate: is the mission's own done-definition covered by
what the reader would see?

- **M-omni-wm-runner**: done-definition = "one durée click runs in-process in
  the futon3c JVM; no third JVM". The three Acceptance bullets (Parcel A tests,
  Parcel B tests incl. the single-flight 409 guard and "no new JVM/process
  spawn anywhere (`pgrep…`)", gates green) cover the parcels and the
  operational core. **Covered**, minor residual: the bullets are test-shaped,
  and "runs in-process" is witnessed by a test assertion rather than by an
  operational record — acceptable for a first flight, stated here.
- **E-ticks-firing-ratio-likelihood**: four success criteria are concrete and
  checkable (canonical mapping exists; no string-matching inference; channel
  appears; sorry rotates off). Status EXECUTED, so all four would verdict Met
  on existing evidence — a flight would have **zero open wants**: a degenerate
  closure (the flight rule's "ends when every criterion is met" is satisfied at
  read time). Faithful but vacuous as a demonstration.
- **M-essays-retraction-visibility**: four exit criteria cover the feature
  (retractions shown, coherent treatment, ERT coverage, checkpoint appended).
  READY TO EXECUTE — real open work; a flight would not be vacuous. Coverage
  good.
- **E-campaign-spec-grounding**: five success criteria about a spec document;
  criterion 3 itself cites a library pattern
  (`structure/what-problem-is-this-actually-solving`). Coverage good; the work
  is authoring, owner codex-5.
- **M-apm-demonstration**: one phase-exit criterion ("problem 2 populates
  unaided") is a DERIVE-candidate exit, not the mission's done-definition;
  IDENTIFY itself is pending operator acceptance. **False-closure risk** if
  flown as a whole-mission target.
- **M-live-efe-map**: C2/C3 require operator decisions (opt-in streaming,
  freshness cadence) — not machine-closable faithfully.
- Checkbox-only missions (M-wm-aif-policy-grain-compliance, M-forum-refactor,
  …): the H-EXITS-D M-f11 shape in general — a checkbox is a task, and unless
  the mission says the tasks ARE the done-definition, closure on checkboxes
  alone is unfaithful. None of them says so.

Producer plausibility (the ask step decides; these are the candidates a seat
would be asked about):

- M-omni-wm-runner criteria: test-green/gate claims —
  `apparatus/done-is-observed-running` (observed running as the acceptance
  witness) and `contracts/every-entry-has-a-falsifier` (each parcel's gate set)
  are plausible; used on two prior targets with receipts.
- M-essays-retraction-visibility:
  `translation/test-by-reproducing-behaviour` (behaviour-level ERT coverage)
  plausible for criteria 1–3; criterion 4 (append the checkpoint) is a
  recording act — `coordination/par-as-obligation`-shaped.
- E-campaign-spec-grounding: criterion 3 names its own pattern; the
  IF/HOWEVER/THEN/BECAUSE criterion maps to the lifecycle's DERIVE checklist —
  plausible.
- E-ticks: producers moot (no open wants after verdicts).

## 5. Ranking

1. **(Tier 1 — empty.)** No mission is closeable by the reader today.
2. **M-omni-wm-runner** (futon3c) — first of tier 2: smallest faithful gap.
   Three verdict lines from owner **claude-3** (named in the file), acceptance
   covers the done-definition, plausible producers exist, live work with a
   real open question (are the parcels' gates green at HEAD). Two
   interpretation requests at most. Command line, once the verdicts land:

   ```
   clojure -M -m futon2.aif.flight-driver M-omni-wm-runner --seat kimi-6 \
     --repo futon3c --path holes/missions/M-omni-wm-runner.md
   ```

   (no `--lifecycle-path`: the mission carries no lifecycle.edn; `--seat` may be
   any seat not in `flight-driver`'s refused-seats — kimi-6 answered the P1/F11
   interpretation packets, so named here).
3. **M-essays-retraction-visibility** (futon4) — tier 2 with real open work
   (READY TO EXECUTE), four verdict lines needed; the file names an area
   ("futon4 / Arxana Essays notes-pane and edit-cycle UI"), not a person —
   owner statement needed on both verdicts and ownership.
4. **E-campaign-spec-grounding** (futon3c) — five verdict lines from codex-5;
   good producer plausibility; status SCOPED, so the work itself is early.
5. **M-f11-find-production-successor** (futon2) — previously my first rank;
   now blocked on six owner verdict lines before its (already proposed,
   `proposals/M-f11-interpretation.edn`) interpretation is reachable by
   assembly. Scan-admitted, unlike 2–4.
6. **E-ticks-firing-ratio-likelihood** — verdicts would close it vacuously;
   useful only as a reader smoke test, not a flight.
7. The rest, with reasons: M-apm-demonstration (false-closure risk; IDENTIFY
   unaccepted), M-live-efe-map (operator decisions inside the criteria),
   M-war-machine-pilot (awaiting operator acceptance; 8 verdicts),
   M-aif-policy-conditioned-eig (11 verdicts + wall-clock held-out evidence,
   FLIGHT-TARGET-D §2.5), M-aif-a-matrix-faithfulness / M-aif-gap-epistemic-affordance
   (12 verdicts each; the latter dependent), M-essays-edit-cycle (26 verdicts,
   ACTIVE mid-flight work), M-becoming-nomad (operator-personal, date-bound),
   checkbox-only missions (unfaithful closure unless the mission declares its
   tasks are its done-definition), archived/closed/parked (excluded).

**The meta-finding for the coordinator:** the binding constraint on the first
flight is not interpretations (D11 now asks for those) and not producers — it
is **verdict lines**. One owner pass over one mission ("each criterion gets
`**Met.**` / `**Not met.**` / `**Not started.**`", the convention M-futon-seams
landed at `d05cb755` with `scripts/verdict_check.py` keeping prose and data
from drifting) turns any tier-2 mission into a flyable target. That is a
one-paragraph ask to claude-3 for M-omni-wm-runner, and it doubles as the
mission-format amendment H-EXITS-D §5(4) proposed.

## 6. Addendum (claude-10's two ranking properties, applied; follow-up commit)

Property 1 — **rank on "all open criteria located", not on how many the reader
sees.** Applied strictly: today every tier-2 candidate has *zero* open located
criteria, because every criterion is `:verdict-not-stated`, and the reader
refuses assembly on any unlocated criterion — so property 1 reduces all of
them to the same precondition (owner verdicts; each verdict creates its
locator, so post-verdict every open criterion is located by construction).
Property 1 therefore does not discriminate among candidates *after* the
verdict pass; it only explains why none flies before it. Where it does
discriminate is M-f11 and M-aif-eig: both currently stand at "assembly refuses
naming the unlocated criterion" — M-f11's six Acceptance bullets, M-aif-eig's
eleven completion criteria — while their *checkbox* wants are located. Under
property 1 the checkbox does not rescue them: one unlocated criterion refuses
the whole target.

Property 2 — **at least one open want must be startable on the first click**:
an admitted/published interpretation produces it, or the would-be-asked
interpretation's guard is satisfiable from the target's *current* facts (else
the flight ends `:no-progress` under rule 2). Per candidate, the first want's
guard facts and whether they hold today (futon2 `8b073dca`, futon3c `071dee27`):

| candidate | first open want (after verdicts) | guard facts needed | hold today? |
|---|---|---|---|
| **M-f11-find-production-successor** | `:hole/h2045faa0e7cc` (checkbox L102, located) | proposed interpretation (F11-INTERP, `39ace063`, validated) needs `:hole/h9ab212b3281d` | **YES** — L101 reads `- [x]` at HEAD; the interpretation exists, is validated, and its guard is observed true. First-click startable. |
| **M-omni-wm-runner** | whichever of the 3 acceptance criteria verdict non-Met (unknown until claude-3 verdicts; if parcels landed, none — degenerate flight) | no interpretation exists; the plausible producers (`done-is-observed-running`, `every-entry-has-a-falsifier`) have historically guarded on `:admission/task-stated` plus already-true tokens | **Probably** — guards of that shape are satisfiable from current facts, but no reading exists to check; the ask step's answer decides. Unknown until the ask. |
| **M-essays-retraction-visibility** | all 4 (READY TO EXECUTE) | plausible producer (`test-by-reproducing-behaviour`) needs only the task-stated fact | **Probably** (same caveat), but the enactment is futon4 Elisp/ERT work — clicks can ask and select, the work's substrate sits outside the WM repos' test path; first click advances only the interpretation, not the want. |
| **E-campaign-spec-grounding** | all 5 (SCOPED) | criterion 3 names its own pattern; guards likely fact-light | **Probably**, same caveat; work is authoring, owner codex-5. |
| **E-ticks-firing-ratio-likelihood** | none (EXECUTED; verdicts would all be Met) | — | **Vacuous**: no open want, nothing to start; not a flight. |
| (reference) **M-futon-seams** | `:exit/h54d16050a9dc` (DOCUMENT) | proposed interpretation (SEAMS-INTERP rev. 3) needs `:exit/h4ef5c183bc55` | **YES** (INSTANTIATE reads Met) — the only target passing both properties *today*, and excluded by scope (worked example, not a WM target), not by mechanics. |

Revised ranking under the two properties: **M-f11 moves back to first**, one
step ahead of M-omni-wm-runner. Both need owner verdict lines to satisfy
property 1 (M-f11: six, on its Acceptance bullets; M-omni-wm-runner: three).
The tie-break is property 2, and it is not close: M-f11's first want has a
validated interpretation whose single guard fact holds at HEAD *today* —
verified against the mission file (L101 `- [x]`) — so its first click can
advance a real want; M-omni-wm-runner's first want is unknown until the
verdicts land and its interpretation does not yet exist to check a guard
against. Recommended sequence: (i) owner verdicts on M-f11's Acceptance
section (its owner per the file's authority note is the F11 lane; the verdicts
are checkable against the recorded gate runs), (ii) promote
`proposals/M-f11-interpretation.edn` into the live sources file, (iii) fly
M-f11; M-omni-wm-runner second, once claude-3 verdicts and one ask cycle
produces its readings. Command line for the revised top candidate (no
lifecycle file exists for it):

```
clojure -M -m futon2.aif.flight-driver M-f11-find-production-successor \
  --seat kimi-6 --repo futon2 --path holes/missions/M-f11-find-production-successor.md
```

## 7. Addendum 2 (Joe's correction, via claude-8: do not rank on precomputed locators)

§6's property 1 measured how much the owner pre-did, not what the machine can
do — the machine is to take any open mission and compute reasonable completion
criteria on the fly. Property 1 is therefore **retracted as a ranking gate**
and replaced by a per-mission report of what the machine must compute:

- **Extraction needed** (no criteria in any recognised form): the ~40
  checkbox-only or criteria-free missions — M-wm-aif-policy-grain-compliance,
  M-forum-refactor, M-action-cost-modelling, M-dionysus-winddown,
  M-apm-capability-ratchet, alleycat-scorecard, … The machine must extract
  criteria from the mission's prose before anything can be verdicted.
- **Verdict computation needed** (criteria present, verdicts unstated):
  M-f11-find-production-successor, M-omni-wm-runner,
  M-essays-retraction-visibility, E-campaign-spec-grounding, M-war-machine-pilot,
  M-aif-policy-conditioned-eig, M-aif-a-matrix-faithfulness,
  M-aif-gap-epistemic-affordance, M-apm-demonstration, M-live-efe-map,
  E-fold-engine, M-essays-edit-cycle, M-essays-diachronic-model,
  M-self-improvement-loop, E-ticks-firing-ratio-likelihood (EXECUTED — verdicts
  would land Met; degenerate for a flight), M-becoming-nomad
  (operator-personal).
- **Nothing to compute**: M-futon-seams (excluded by scope).

Ranking, by faithfulness (does what the reader sees, plus what extraction from
the mission's own text yields, cover the done-definition?) then property 2:

1. **M-f11-find-production-successor** — verdict computation only; done-definition
   covered (Acceptance bullets + the repair-024 disjunct; extraction below);
   property 2 holds today (validated interpretation, guard fact `:hole/h9ab212b3281d`
   true at HEAD). **First.**
2. **M-omni-wm-runner** — verdict computation plus one extraction that matters
   (the Acceptance bullets never state the mission's core criterion — "a click
   runs in-process" appears only in Scope; extraction finds it, §7.1). Property
   2 probable, unwritten readings. **Second.**
3. **M-essays-retraction-visibility** — verdict computation only (extraction
   ≈ identity); property 2 probable but enactment substrate is futon4 Elisp.
   **Third.**
4. E-campaign-spec-grounding (authoring work, owner codex-5, SCOPED);
   E-ticks (degenerate); then §5 rows 6–7 unchanged. The extraction-needed
   class ranks below all of these on faithfulness grounds alone: with no
   stated criteria, coverage of the done-definition is unverifiable until the
   extractor exists and its output is checkable against the text.

### 7.1 Extracted completion criteria, top three (hand reference for an extractor)

Each criterion cued to the sentence it comes from. "Reader-visible" = the
criteria reader already returns it; "extraction-only" = only an extractor over
the prose would yield it.

**M-f11-find-production-successor** (futon2, `holes/missions/M-f11-find-production-successor.md`):

| # | extracted criterion | cue (sentence it comes from) |
|---|---|---|
| F1 | The F2 falsifier is reconciled against current library state. | Acceptance: "Reconcile the F2 falsifier against current library state." (reader-visible bullet) |
| F2 | A conformant implementation acts at the applied `find` interface. | "Supply a conformant implementation at the applied interface." (reader-visible) |
| F3 | F1–F4 are stated in Lean, each witnessed on a real find over the committed 24-pattern library. | "State F1–F4 in Lean and witness each on a real find over the committed library." (reader-visible) |
| F4 | The `find` sorry formerly at `DarkTower/WarMachine/Holes.lean:264` is discharged or amended. | "Discharge or amend the `find` sorry." + Objective: "discharge or amend the `find` sorry formerly anchored at `DarkTower/WarMachine/Holes.lean:264`" (reader-visible bullet; the anchor location is extraction-only, from the Objective sentence) |
| F5 | F12's gates ran: focused Lean builds, negative controls, pointer checks, clj-kondo, paren checks. | "Run the same gates as F12, including focused Lean builds, negative controls, pointer checks, clj-kondo for scripts, and paren checks." (reader-visible) |
| F6 | Unrelated work preserved; residual limitations reported, not called success. | "Preserve unrelated work and report any residual limitation without calling it success." (reader-visible) |
| F7 | Repair-024 disposition published: strict successful successor link, or the typed failure retained without resolution — and only after ordinary gates produce strict durable terminal evidence. | Checkbox L102 (reader-visible) + the boundary sentence "Repair-024 is resolved only after this mission's ordinary gates produce strict durable terminal evidence." (extraction-only: it conditions F7 on F1–F6, a dependency no bullet states) |

**M-omni-wm-runner** (futon3c, `holes/missions/M-omni-wm-runner.md`):

| # | extracted criterion | cue |
|---|---|---|
| O1 | Parcel A tests green; ladder timeouts, override pinning, typed exhaustion, injectable-seam preference all covered by new tests. | Acceptance bullet 1 (reader-visible) |
| O2 | Parcel B: single-flight guard 409s a second click; endpoint contract tests; registry status transitions; **no new JVM/process spawn during a stubbed click** (`pgrep java` unchanged). | Acceptance bullet 2 (reader-visible) |
| O3 | Gates pass on both parcels: clj-kondo, check-parens, `clojure -X:test` on touched namespaces. | Acceptance bullet 3 (reader-visible) |
| O4 | **A durée click (`once` semantics) runs in-process in the serving futon3c JVM on a dedicated thread, triggered over HTTP** — the mission's actual done-definition; the Acceptance bullets test it only under stubs. | Scope: "one durée click (`once` semantics) runs in-process in the futon3c JVM on a dedicated thread; HTTP trigger + status" (extraction-only; no bullet states it) |
| O5 | Continuous mode, the futon0 scan JVM, store heap, and the turn-commits relay are out of scope — a faithful closure must not require them. | Scope: "**Out (follow-ups):** `continuous` mode; the transient futon0 scan JVM …" (extraction-only; a negative criterion — the extractor must produce scope-outs too, or a flight could be failed on work the mission declined) |

**M-essays-retraction-visibility** (futon4, `holes/missions/M-essays-retraction-visibility.md`):

| # | extracted criterion | cue |
|---|---|---|
| R1 | Opening a section with `:retracted t` manifest annotations shows them in the notes pane, not filtered away. | Exit criteria bullet 1 (reader-visible) |
| R2 | Persisted and live-pending retractions have a coherent visual treatment; the state transition is understandable. | bullet 2 (reader-visible) |
| R3 | Targeted ERT coverage lands in `test/arxana-browser-essays-test.el`. | bullet 3 (reader-visible) |
| R4 | A checkpoint with test counts and a one-line live-behavior note is appended to the parent mission. | bullet 4 (reader-visible) |

Extraction adds nothing here — which is itself the finding: this mission's
reader-visible criteria ARE its done-definition, the property that makes it
the cleanest faithfulness baseline of the three.

### 7.2 What an extractor must additionally produce (from the three tables)

The cues show the extractor's required range, beyond bullet-lifting: (i)
anchor data buried in prose (F4's file:line); (ii) cross-criteria dependencies
stated only in boundary sentences (F7 on F1–F6); (iii) core criteria that
never became bullets (O4); (iv) negative criteria from scope-out lists (O5).
An extractor tested against these three missions and producing exactly these
tables — cues included — would have demonstrated the same role for criteria
that claude-1's mission-C plays for outcomes.

Everything else in the packet stands.
