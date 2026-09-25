# WM-CUE-D — the five `:cue-not-in-criterion` locator rejections (discovery, read-only)

claude-10, 2026-09-25, for claude-8 (packet WM-CUE-D). Read at futon2
6bbaf419. Records read:

- futon3c `holes/labs/M-wm-wiring/spike/flight-ffcd772b.edn` (the second
  flight);
- the five request records it names, under futon2
  `data/wm-interpretations/requests/` (read, not written).

No code, no test, no flight.

## 1. The check as written

`validate-locator` (`mission_reading.clj:147-176`) rejects a locator reading
with `{:reason :cue-not-in-criterion :quote …}` when the cue's quote is blank,
or when `(words stated)` does not contain `(words (:quote cue))` as a string
(`:169-170`).

`words` (`:141-145`) normalises before comparing: it removes backticks, turns
every whitespace run into one space, and trims. So the cue must be one
contiguous span of the criterion, up to whitespace and backticks.

`stated` is `(get-in issued [:criterion :stated])` (`:167`). For an extracted
criterion that is the criteria reading's own cue quote:
`validate-criteria` publishes each criterion with `:stated (:quote cue)`
(`:208-210`), after checking that the quote is exactly the mission's text at
its cued lines (`:196-201`). So the cue is matched against a verbatim
multi-line span of the mission file, markdown included. The five spans below
run from 1 to 17 lines.

## 2. The five rejections

Each quote was split on `...`/`…` after the same `words` normalisation. Every
piece is a verbatim substring of `words(stated)`, and the pieces occur in
order (character offsets increasing):

| want | request | pieces (words each) | offsets in stated | kind |
|---|---|---|---|---|
| `:exit/h5c6933e5e2fc` | request-ee877276e5bbd6f3 | 4 (18, 4, 8, 9) | 0, 180, 291, 510 | ellipsis-joined, each piece verbatim, in order |
| `:exit/h7afd70f40a29` | request-7a22ebcc5af3c2ae | 2 (5, 16) | 10, 151 | same |
| `:exit/h64ddcd0e8811` | request-a8b9fb06e9e0dba1 | 3 (11, 13, 15) | 413, 579, 1014 | same |
| `:exit/h0c55648e57e5` | request-eac34688cf1b8e75 | 3 (13, 5, 8) | 36, 182, 326 | same |
| `:exit/h59adca2cd730` | request-da53dffe6dad625d | 3 (8, 8, 45) | 48, 196, 352 | same |

**What the seat quoted.**
- None is a paraphrase, and none quotes from elsewhere in the mission.
- In each, the seat quoted the parts of a long criterion that the locator
  decides, and elided the rest with `...`.
- For example, for h7afd70f40a29 the stated text is
  `- *Risk:* auto-clocking on a fuzzy mention (the exact anti-pattern … — *explicit, not fuzzy*) would mislabel turns and corrupt …`,
  and the quote is
  `auto-clocking on a fuzzy mention ... would mislabel turns and corrupt `turn→mission` ground truth. A wrong auto-clock is worse than `[no mission]`.`
- The first flight's one cue rejection (h59adca2cd730, flight-d00574c8) has
  the same shape.

**Verdict:** a reader who accepts "the criterion's own words" would have
accepted all five.

## 3. The grammar the seat was given

The locator REPLY GRAMMAR (`mission_reading.clj:109-112`) asks for
`:cue {:quote "words of the criterion this locator decides"}`. That does not
say verbatim, contiguous, or exact.

Every other reading kind in the same `prompt` says `"exact text"`,
`"exact mission text"` or `"exact text of those lines"`: constraints `:102`,
coverage `:105-107`, criteria `:114`, questions' `:span`. The locator cue is
the only one that asks for "words of the criterion", and the only one that
must be the part this locator decides.

`validate-locator`'s docstring states the intent: "its cue quotes the
criterion's own words" (`:150`). The mission-reading ns docstring asks for
exact cues only for CRITERIA ("every cue must be the mission's text at HEAD",
`:17-18`), not for locators.

So the grammar and the docstring agree with each other, and they disagree
with the check. The defect is on the check's side: it enforces one
contiguous span, which the grammar never asked for and the stated intent does
not require. The check has been this way since it was written, 81ff1315
(2026-09-24); no ruling was found for contiguity.

## 4. Cost of each repair under IDENTIFY 4

- **(a) The check accepts an ellipsis-joined quote** when every piece, after
  `words`, is a non-blank substring of `words(stated)`, in order. This
  **removes red tape**: it adds no contract a flight must satisfy, and it
  stops refusing quotes the grammar invited. It keeps what the check exists
  for: every word quoted is the criterion's, in the criterion's order, so a
  cue cannot bring in words from elsewhere or reorder them.
- **(b) The grammar states a contiguous-substring rule.** This is a **new
  contract**. The seat must quote one unbroken span even when the locator
  decides only parts of a long criterion. That forces longer quotes that
  carry the parts the locator does not decide, or a refusal. It adds a
  requirement without an AIF-validity case, since the cue's job, tying the
  locator to the criterion's words, is met by in-order pieces.
- **(c) Both.** This adds the contract of (b) for no gain once (a) holds.

**Recommendation: (a) alone.** If anything is added to the grammar, it
should describe what (a) accepts ("verbatim pieces in order, joined by `...`
where you elide"). That documents the rule; it does not narrow it.

## 5. Recommendation, size, falsifier

**Build:** one function, one test namespace, no map rows (the check is inside
the verifier the map treats as a component, and no field moves).

- `mission_reading.clj` `validate-locator`: replace the `str/includes?` test
  at `:169` with a check that the cue's pieces are a subsequence of the
  criterion. Split `words(quote)` on `...` or `…`, require every piece
  non-blank, and require each piece to occur in `words(stated)` after the end
  of the previous one. The reason keeps its name
  (`:cue-not-in-criterion`) and its `:quote`.
- **Test box** `futon2.aif.locator-cue-test`, with the five quotes and their
  criteria's `:stated` pinned verbatim from the record: request ids above,
  quotes from flight-ffcd772b's `:reasons`, stated text from the five request
  records (sha in the fixture header).
  - All five accepted by the cue check.
  - Bad cases still refused:
    - a quote whose pieces are verbatim but out of order (h7afd's two pieces
      swapped);
    - a piece that is not in the criterion (h0c55's second piece replaced by
      words from another criterion);
    - a blank piece (`"a ... ... b"`);
    - a blank quote.
  - The unchanged contiguous case still accepted (h7afd's second piece alone).

**Falsifier (a fourth flight of M-autoclock-in):**
- No locator reading is rejected `:cue-not-in-criterion` when its quote's
  pieces are the criterion's words in order.
- The five wants above either publish a locator, or are rejected only for
  another reason on the record (most likely `:check-refused`, whose timeout
  WM-SPIKE-FIX-II D now names, or a class or field reason).
- A reading rejected for the cue must show on its record a piece that is not
  in the criterion, or pieces out of order.
- If a faithful in-order quote is still refused, the fix was wrong.

**Not addressed here:** the other two of the seven unlocated wants
(h7934b12d0a3a, h0f3acdf222d7) were `:check-refused`, the C8 registry read
timing out at 5 s against a 5.66 s endpoint. The cue fix does not move them.
