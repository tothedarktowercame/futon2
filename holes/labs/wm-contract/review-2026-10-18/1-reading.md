# Review 1 — target-reading stage (outer target chosen -> published reading -> published occurrence squares)

Reviewed at futon2 HEAD `0975c7df7` (futon3c validator at `67b3b095b`). Read-only. One throwaway tooling JVM
(`clojure -M -e`) was used to confirm decoder behaviour and the NPE below; no test suites run.

Status: complete. Findings F1-F8, a rule comparison, what was not checked, and a verdict.

## F1. First-attempt offset-resolution failure now throws NPE instead of asking for a correction (regression from 3037b082f)

- `src/futon2/aif/selected_target_reading.clj:366-401, 411-421`.
- `validate` returns `{:exit 1 ...}` early when `resolve-analysis-offsets` refuses (quote absent, quote ambiguous,
  unknown sentence id) and never sets `last-output`. `cascade-failures` then runs `(.isFile @last-output)` on `nil`
  (line 414). Confirmed in a tooling JVM: `(.isFile @(atom nil))` throws
  `NullPointerException "Cannot invoke \"Object.getClass()\" because \"target\" is null"`.
- Before 3037b082f this case went to the correction round with offset diagnostics. Now the exception escapes
  `repair!`; `full_loop_runner.clj:2088-2093` catches it as `:selected-target-reading-repair-threw` and the click
  abstains with no correction contact.
- Input that triggers it: any first Zai answer in which one fragment `text` or one display-cue `text` is not found
  exactly once in its sentence/fragment (a dropped trailing period, curly vs straight quote, an elided "...",
  a repeated phrase not at fragment start), or a sentence id the request does not have.
- How likely: the five recorded real answers in `storage/wm-target-reading-requests/*/analysis-input-attempt-1.json`
  all resolved, so it has not happened yet; but exact quoting is the thing the prompt warns about at length, and
  this is precisely the case the correction round existed for.
- Test adequacy: the 3037b082f regression plants only a zero-`pattern_refs` answer that resolves cleanly; no test
  exercises offset refusal on attempt 1 after the change.
- Severity: blocks-next-click if the first answer misquotes (otherwise likely-later). Confidence: confirmed by
  reading the path end to end plus the JVM probe.

## F2. Offset diagnostics are now sent only when they are useless (inverted condition, 3037b082f)

- `selected_target_reading.clj:433-436`: `offset-diagnostics` is appended `(when (zero? (:exit validated0)))`,
  i.e. only when the Python validator accepted the answer. Before 3037b082f the diagnostics were sent on validator
  failure, which is the only case where they name something wrong. A Python offset/text refusal now reaches the
  author with the bare one-line Python message and no per-fragment expected/submitted slices.
- Severity: minor (see correction below). Confidence: confirmed by reading.

Probe for F1 (confirmed, not just read): a tooling JVM loading `selected-target-reading-test` helpers and calling
`runner/selected-target-reading-step` with `sut/repair!` and a first answer whose fragment text is `"OPENED"`
(absent from the sentence) returned after ONE contact:
`{:status :refused, :kind :selected-target-reading-repair-threw, :message "Cannot invoke \"Object.getClass()\" because \"target\" is null"}`.
Probe script: `/tmp/rev1/probe.clj` (throwaway; temp dirs only).

Correction to F2's severity: Python offset/text errors are rare once Clojure has re-derived offsets from quotes, so
F2 is **minor**.

## F3. The first prompt never names the field `pattern_refs`; Zai-2's citation-free answer was predictable and will recur

- What the author sees: `selected_target_reading.clj:235-258` sends the prompt prose, the `:acceptance_contract`
  extracted from `session_turn_analysis.py template` (line 304-315: only that key is kept), and the request JSON.
  The template's `fragment_shape` example, which is the only place `pattern_refs` is spelled out, is dropped.
- The contract (`futon3c/scripts/session_turn_analysis.py:84-89`) calls the rule `pattern_references`
  and never says the JSON key is `pattern_refs`, never says a citation is required, and never says where in a
  fragment it goes. Confirmed: `"pattern_refs" in json.dumps(acceptance_contract)` is False on the click-5 request,
  and the request file contains no `pattern_refs` either.
- Evidence from storage: every published reading before click 5 has `labeller` `wm-author` (a **Codex** seat on the
  roster) and cites a pattern on every fragment; Codex reads `session_turn_analysis.py`, as the prompt invites.
  The only Zai reading ever decoded (click 5, `b13efa40…/analysis-input-attempt-1.json`) omits the
  `pattern_refs` key entirely on all 9 fragments. The other contract keys whose JSON names match (`display_cues`,
  `pattern_rejections`, `no_surface_cue`, `unresolved_reason`, `labeller`, ...) were all used correctly by Zai.
- 3037b082f does not change the first prompt. It only adds the field name and shape in the correction feedback
  (line 432). So every Zai reading will spend its one correction round on this, leaving no second round for
  anything else wrong with the corrected answer (see F4).
- Severity: likely to fire on the next click (it costs the correction round, not the click by itself).
  Confidence: confirmed from the prompt construction, the contract bytes and the stored answers.

## F4. The correction round is single-shot and its failure is terminal for the click; what Zai's corrected answer must survive

Does a corrected answer get revalidated by all layers? Yes, mostly:
- Clojure offset resolution and Python `complete` run again (`validate corrected-analysis`, line 440-442), on a
  fresh attempt-scoped request copy (so the os.link collision 3037b082f fixed is really gone).
- The cascade check is *not* re-run before publication by `cascade-failures`, but `registry/publish!`
  (`target_reading_registry.clj:121-128, 155-162`) runs the same `analysis->cascades {:mode :overlap}` check, so a
  correction that still has no citation is refused at publication. Same function, so no drift between those two.
- The occurrence-square layer (pattern must have IF/HOWEVER/BECAUSE/THEN) is not applied at reading time at all;
  see F6.

What happens if the correction also fails: there is no second round. The record is a refusal, and
`full_loop_runner.clj:2087-2105` is not wrapped in `run-phase!`, so nothing is held for the debugger to repair and
`:retry`: the click ends abstained (as clicks 3-5 did). Specific outcomes:
- Corrected answer fails offsets or Python -> `:selected-target-reading-validation-refused` with that stderr.
- **The most likely next failure**: Python stops on the first bad citation (`session_turn_analysis.py:253-267`,
  `unknown canonical pattern` / `pattern declaration does not match`). Zai, newly told to cite a pattern on every
  fragment out of 1,441 library files, must get every id exactly right; one wrong id in nine refuses the whole
  answer and the click. Zai seats do have `read_file`/`list_files`/`search` tools (`futon3c/src/futon3c/agents/zai_api.clj:72, 701`),
  so this is avoidable, but nothing checks ids before the single round is spent.
- Correction contact itself fails (job not done, undecodable reply): `corrected-analysis` is nil and the code
  silently falls back to the *first* answer (line 438-443). In the click-5 shape (Python passed, no citations)
  this goes on to publish the uncorrected answer and reports `:selected-target-reading-publication-refused`,
  hiding that the real failure was the correction exchange; the correction's `:reason`/`:job-id` are not kept.
  Severity minor (diagnostic), confirmed by reading.
- Stated-vs-enforced mismatch inside the feedback: line 432 tells the author "Every fragment must cite at least one
  pattern", but `analysis->cascades` (`analysis_cascade.clj:122-132`) and the registry accept an analysis where
  *any one* fragment cites; the correction round also fires only when *zero* fragments cite. Uncited fragments are
  silently left out of the cascade. Not a failure; the TN's description ("required ... on every fragment") is also
  inaccurate on this point.
- Test adequacy: `pattern-ref-less-answer-gets-one-correction-round` runs the real Python validator and the real
  registry, which is good. It covers only the success of the correction. No test covers: offset refusal on attempt 1
  (F1), a correction that still fails, a correction exchange that fails, or a corrected answer with one unknown id.

## F5. Occurrence-square authoring has never been done by Zai live, has no correction round, and the recorded flight bypassed its digest check

- All four live square records under `storage/wm-occurrence-squares/*/` have `:author "wm-author"`, which the
  roster reports as `type codex`. With the campaign cast (zai-2 as author) the square handoff
  (`full_loop_runtime.clj:216-224` -> `occurrence_square_authoring.clj:59-152`) has never met Zai output.
- No correction round: any one of these ends the click. `full_loop_runner.clj:6383-6392` throws
  "Occurrence-square authoring failed before G", outside `run-phase!`, so there is nothing for the debugger to hold:
  - reply not one decodable JSON object -> `:occurrence-square-answer-invalid` (line 106-108);
  - `schema` not `wm/occurrence-square-maps-v1`, `target` not exact, `analysis_digest` not the exact 64-hex value,
    or the entry id set not exactly the non-void set -> `:occurrence-square-answer-unbound` (line 110-115);
  - a role key not exactly `I1`/`I2`/`G`/`B`, or a role value that is a string instead of `{element, source}` ->
    `:situation-maps-incomplete` inside the square -> `:occurrence-square-construction-invalid` (line 130-136;
    `three_halves_square.clj:58-66`).
- Two specific traps in the prompt (`occurrence_square_authoring.clj:39-51`):
  - the reply must use `occurrence_id`, but the occurrence list rendered right below it is keyed `"occurrence-id"`
    (it is the internal map, `json/generate-string`'d, with `pattern-parts`, `missing-parts`, `void?` alongside).
    A model copying from the data rather than the prose sends `occurrence-id`; `(comp vec :occurrence_id)` then
    yields `[]` for every entry and the answer is unbound;
  - the author must transcribe `analysis_digest` exactly. The recorded flight does not test this: it overwrites the
    recorded answer's `:analysis_digest` with the machine value before calling `repair!`
    (`wm/recorded_flight.clj:82-91`), and every unit test in `occurrence_square_authoring_test.clj` does the same
    (`assoc answer :analysis_digest ...` at lines 63, 77, 96, 110). The check at line 112 is therefore exercised
    against real author output only by the four Codex records.
- Severity: likely to be the first failure after the reading publishes, if F1/F4 do not stop the click first.
  Confidence: the code path is confirmed by reading; whether Zai actually trips on `occurrence-id` or the digest is
  plausible, not observed.

## F6. The void-square case: the live path is the recorded flight's path and will not refuse

- Question asked: is a cited pattern lacking the IF/HOWEVER/BECAUSE/THEN carrier handled live as in the flight?
  Yes. The flight calls the same `square-authoring/repair!` (`wm/recorded_flight.clj:109-116`) with the same
  production defaults. In `repair!` each cited pattern is parsed with `square/pattern-parts`; any blank part marks
  it void (line 79-94); voids are left out of the author's request and published under `:voids`;
  `occurrence_square_registry.clj:43-48` accepts a void only when its missing parts are non-empty and it has no
  square entry; `target_policy_family.clj:53-67, 104-105, 129-130` removes void citations before cascade
  construction and records a counted `:void-pattern-citation` failure that does not fail the family.
- Live evidence: two of the four Codex-authored live records (`3eed1f2c…`, `de4b1e3b…`) were published with a void
  (`system-coherence/present-graph-topology-not-adjacency-lists`).
- How often it matters: 184 of 1,441 library `.flexiarg` files lack at least one of the four headers (a lower
  bound; blank parts also count as void in code), so a nine-citation reading includes a void more often than not.
- Residual risks (minor): (a) the author returning an entry for a void occurrence is terminal
  (`:occurrence-square-answer-unbound`), and in the flight the first offline answer did exactly that and was redone
  offline — live there is no redo; it is unlikely only because the void is not shown to the author. (b) If every
  citation is void, the author is still contacted with an empty occurrence list, and the family ends with no reading
  policies. (c) No unit test in `occurrence_square_authoring_test.clj` covers void classification (only the registry
  and square namespaces test voids).

## F7. Two definitions of `fragment-index`

- `analysis_cascade.clj:31-36` numbers fragments after sorting by `(start, end, sentence-id)`; cascade nodes carry
  that index. `occurrence_square_authoring.clj:13-25` (square occurrence ids) and
  `target_policy_family.clj:53-67` (void removal) number fragments in the order the analysis lists them.
  `attach-squares` (`target_policy_family.clj:41-51`) looks squares up by `[pattern (:fragment-index node)]`.
- They agree only when the published analysis lists sentences and fragments in source order. Python preserves the
  author's order and checks only the set of sentence ids (`session_turn_analysis.py:197-199`), not their order.
- Failure scenario: an author lists s3 before s2, or lists a fragment that starts earlier after one that starts
  later in the same sentence. Squares are then published under one index and looked up under another; nodes lack
  `:blend-square` and appear as unread citations downstream, and a void removal can hit the wrong fragment's ref.
- All five stored readings are in source order, so this has not happened. Severity likely-later; confidence: the
  mismatch is confirmed by reading, the consequence downstream of `attach-squares` is plausible (not traced).

## F8. Shared decoder: actual behaviour differs from its docstring

Probed in a tooling JVM against `handoff_contract.clj:9-32`:
- `"{...}\n\nDone: analysis complete."` decodes (cheshire reads the first value and ignores trailing text), and so
  does `"{a} {b}"` (first object taken, second dropped). The docstring says unfenced prose is refused.
- `"Here it is:\n{...}"` (prose *before* an unfenced object) is refused -> `:selected-target-reading-answer-invalid`,
  click lost. This is the mirror image of click 4's shape and the most plausible next representation failure.
- A fenced object followed by a second fenced block (e.g. a fenced note or a `<think>` block with its own fence)
  is refused (two fences).
- Severity: likely-later. Confidence: confirmed by probe.

## Rule comparison: what the reading author is told vs what is enforced

Sources: the contract the author sees (`session_turn_analysis.py:43-113`, `acceptance_contract` only) plus the
prompt prose (`selected_target_reading.clj:239-249`); enforcement in Clojure projection
(`selected_target_reading.clj:115-159`), Python `validate` (`session_turn_analysis.py:184-388`), and registry
publication (`target_reading_registry.clj:121-162` via `analysis_cascade.clj`), plus square authoring.

Enforced but not stated to the author:
1. The JSON key is `pattern_refs` (contract says `pattern_references`). F3. This caused click 5.
2. At least one fragment must carry a validated citation, or publication refuses (registry). Not in contract or
   first prompt; stated only in the correction feedback, and there overstated as "every fragment".
3. A citation counts toward squares only if the pattern has all four IF/HOWEVER/BECAUSE/THEN parts; otherwise it is
   silently voided. Not stated; harmless unless all citations are void.
4. Pattern id must match `[\w-]+(?:/[\w'-]+)+` (Python line 256-258). Not stated; all 1,441 library ids match it,
   so harmless.
5. `labeller` must be a non-empty string; contract says only "required".
6. Sentence/fragment order must be source order for squares to bind (F7). Not stated, not validated.
7. Square reply key `occurrence_id` and exact echo of `analysis_digest`/`target`/`schema` (F5). Stated in the
   square prompt prose but contradicted by the `occurrence-id` key in the data shown.

Stated but not enforced (or enforced more loosely):
1. `reusable_cues` and `rnode_cues` are listed as required; Python defaults both to `[]`.
2. `display_cues.max_covered_words`: the Clojure projection silently drops cues to fit the budget before Python
   sees them (`apply-cue-budget`, line 98-113), so the author never fails this rule. Its word count splits on Java
   `\s+` (ASCII only) while Python's `split()` also splits on Unicode spaces such as U+00A0; a sentence with a
   non-breaking space can pass the Clojure budget and then fail Python. Minor.
3. `relations` "from relation_vocabulary": Clojure maps four aliases and silently drops unknown roles; Python then
   refuses only if none remain.
4. Correction feedback "every fragment must cite": enforced as "some fragment" (F4).

Agreements checked and found consistent: offsets in Unicode codepoints on both sides (`codepoint-slice`); the
display-cue budget formula `max(floor(words/2), 2)` above 8 words; `validated-ref?` in `analysis_cascade.clj:25-29`
needs exactly the `status`/`source_sha256`/`rationale` that Python stamps; void detection uses the same
`pattern-parts`/blank test in authoring and in square construction.

## Not checked / not reached

- `want_interpretation.clj` and `interpretation_job.clj`: only skimmed. They serve the interpretation-ask path,
  which fires only for `:no-admitted-interpretation` refusals; none of the four named commits touched them.
- The Q5 selected-reading phase (`selected_reading.clj`, `full_loop_runner.clj:1983-2040`): read; it uses the shared
  decoder and also has no correction round, but I did not establish whether the reading route's selected policy
  will owe debt (and so contact Zai) on the next click.
- Downstream of `attach-squares` (what an unsquared node does to G and the gates): not traced.
- Whether a refused reading should be a held, retryable phase under ground rule 4 is a question of the written
  procedure; I note only that today it is not (`full_loop_runner.clj:2087-2105`, `6383-6392`).

## Readiness verdict for my area

Not ready. A Zai author will predictably omit `pattern_refs` again (F3), because the contract it sees does not
name that field; the correction round added in 3037b082f should recover that once. But that round is the only one,
and its failure ends the click with nothing held for the debugger (F4). The most likely next stop is a single
unknown pattern id in the corrected answer. A first answer with one misquoted fragment now crashes with an NPE
instead of being corrected (F1, a regression from 3037b082f, confirmed by probe). Beyond the reading, square
authoring has never been done by Zai live, has no correction round, and has two traps in its prompt that the
recorded flight and unit tests stub out (F5). The void case itself is handled live exactly as in the flight (F6).
Small fixes would clear the first two stops: guard `last-output` for nil, name `pattern_refs` and the
at-least-one-citation rule in the first prompt or contract, and fix the `occurrence-id` key shown in the square
prompt. Which of these to build is codex-68's call.
