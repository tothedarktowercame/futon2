# E-kimi-task-40 — operator round turn-c12-ptcrime: seat B, carry out the cascade with warrants

**Requisition:** in-progress — dispatched 2026-09-25T19:13:54Z to kimi-32 as invoke-1790363634577-24286-9ed2e5f8

Clocked in by claude-12 for kimi-32 on 2026-09-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Seat B — carry out a pattern cascade, with a warrant for every change

You are seat B of an operator round. The bell's CASCADE is seat A's
translation of Joe's turn turn-c12-ptcrime; Joe's own words are below as context. The
cascade is what to do; the words are there so you can check your reading of
it, not a second set of instructions.

## Reading the cascade
- Each `(join Fn-...)` is one move. Read every flexiarg the join cites
  (/home/joe/code/futon3/library/<id>.flexiarg): its IF / HOWEVER / THEN /
  BECAUSE is how the move is to be made.
- A HOLE is a move the library cannot name. Its `:in-hand` span is Joe's
  words; you may act on those words, and the warrant is the span, not the
  `:candidate` (a candidate is not a library pattern).
- If a join does not determine an action, do not improvise: record a typed
  refusal for that join (translation/route-the-untranslatable) and go on.

## The warrant rule (Joe, 2026-09-25)
Every change you make carries its warrant where the change is:
- In code: a comment or docstring line at the change,
  `Warrant: turn-c12-ptcrime Fn-join-id pattern/id[, pattern/id]` (or `HOLE-n [a,b]`),
  in the file's comment syntax, saying in one clause what the pattern asked
  for here.
- In each commit message: a `Warrant:` trailer line in the same form.
A change you cannot warrant from the cascade is not made. If you think
something unrequested is needed — a new check, guard, test harness, field,
refactor, document — list it under "Unwarranted, not done" with the reason,
and leave it. That list is expected, not a failure.

## Limits
Work only in the repositories and files Joe's turn names or the cascade's
spans point at. Explicit-path commits (`git commit -- <paths>`), never amend,
never stash. Clojure: clj-kondo 0 errors and futon4/dev/check-parens.el OK on
changed files; run the tests of any namespace you change in its own process.
Do not load code into the shared JVMs (:6768/:7070), do not run the War
Machine, do not write under any `data/` directory.

## Report (your reply)
For each join, in order:
- **Fn-join-id** — read as: <one line> · did: <what, with file:line> ·
  commits: <shas> · warrant: <ids> — or refused: <typed reason>
Then:
- **Unwarranted, not done:** <item — why you thought of it — why no warrant>
- **Holes:** <HOLE-n: acted on the span / left, and why>
- **Gates:** <commands run and their results>

## Joe's turn turn-c12-ptcrime, as spoken

```text
Real work: let's continue wherever we left off with M-the-perfect-crime, notes should be in the mission doc and I'll want a reminder about what we accomplished and what still remains.  I've viewed https://zone.hyperreal.enterprises/wip/mission-efe-field.html and I thought it would have some more information based on the Tornhill material but maybe that hasn't been integrated there yet.
```

## Cascade

```clojure
(cascade turn-c12-ptcrime
  :envelope {:illocutionary-type request :source turn-c12-ptcrime/turn.json}

  ;; F1 — mark the substantive payload (force: re-rank; "Real work:" demotes whatever preceded)
  (join F1-mark-the-real-work
    (HOLE-1 :wanted "an operator-side framing marker that re-ranks the agenda: what follows supersedes the prior topic as the real work"
            :in-hand "'Real work:' [0,10]"
            :discharge "a pattern binding the marker to its effect: previously current threads are demoted (not closed), the following span is the payload"
            :candidate operator/mark-the-real-work
            :tried "writing-coherence/hedged-lift (commit/defer/cut, but governs rhetorical moves inside prose, not the operator's agenda re-rank); inbox-zero/classify-the-dirt (classifies incoming items by judgement needed, but is triage of a queue, not a speech-act demarcation in a turn)"
            :why-failed "both operate on collections of items, not on the conversational frame; neither binds 'the demoted topic stays live but loses priority'"))

  ;; F2 — resume the named mission from its record (force: request; "let's continue" + "notes should be" = the doc is the truth)
  (join F2-resume-m-the-perfect-crime-from-the-doc
    (process-coherence/status-refresh-before-work
      :span [11,71] "let's continue wherever we left off with M-the-perfect-crime"
      :note "taking up a mission in progress: 'wherever we left off' is a cached pointer — the resume point must be re-derived from the record, not from memory")
    (process-coherence/status-refresh-before-work
      :span [73,107] "notes should be in the mission doc"
      :note "the operator names where the truth lives: the mission doc body, not the agent's recollection; re-derive status from the body per the pattern's THEN"))

  ;; F3 — brief the operator: accomplished and remaining (force: request; "I'll want" = standing expectation on resumption)
  (join F3-report-accomplished-and-remaining
    (stack-coherence/ready-blocked-triage
      :span [112,182] "I'll want a reminder about what we accomplished and what still remains"
      :note "emit the done/remaining classification with evidence; context is devmap clauses in the pattern, generalized here to mission obligations — the THEN shape (status[done|...] with evidence) is what the turn asks for")
    (process-coherence/status-refresh-before-work
      :span [112,182] "I'll want a reminder about what we accomplished and what still remains"
      :note "the reminder's content is the re-derived status from F2, not a flattering counter; 'a reminder' = the operator holds the agent's fresh read as the source"))

  ;; F4 — report an expectation gap on a viewed document, cause held as hypothesis (force: inform with tentative diagnosis; "I thought ... but maybe" carries it)
  (join F4-expectation-gap-on-mission-efe-field-page
    (HOLE-2 :wanted "operator reports an expectation gap on an artifact they have read: expected content X (Tornhill material) is absent from the viewed document"
            :in-hand "'I've viewed https://zone.hyperreal.enterprises/wip/mission-efe-field.html and I thought it would have some more information based on the Tornhill material' [185,339]"
            :discharge "a pattern for reporting a content gap between a source corpus and its expected integration surface, carrying the URL, the expected material, and the observation, as a checkable item rather than a complaint"
            :candidate operator/report-expectation-gap-on-viewed-artifact
            :tried "hygiene/settle-with-meters (settles cycles with measurements, but the gap here is expectation-vs-content, not a metered quantity); stack-coherence/readme-devmap-sync (divergence between two system artifacts, both under the same stewardship — not operator-expectation vs published page); process/spec-silence-pair (drift into silence between spec and code, but is a problem-shaped diagnosis of agent behaviour, not the operator's report act)"
            :why-failed "the library has divergence-detection patterns between artifacts it owns, but nothing that is the operator's speech act of reporting 'I looked, I expected, I did not find' as an actionable observation")
    (agent/state-is-hypothesis
      :span [340,388] "but maybe that hasn't been integrated there yet."
      :note "the proposed cause is held as a revisable hypothesis — 'maybe' is the confidence marker; the correct response is to check and update, not to defend or dismiss the diagnosis")))
```
