# SEAMS-INTERP — four interpretations for M-futon-seams' first flight (validated proposals)

Author: kimi-6, 2026-09-24. Prepares the content the D11 interpretation request will
ask for. Deliverable: `holes/labs/wm-contract/proof2/proposals/M-futon-seams-interpretations.edn`
(proposal only — nothing under futon3c or in `resources/wm/cascade-sources/` was
edited; no sources file for this target exists yet). Anchors: futon2
`4dd54848003882115eef3740d9838c22250075af`, futon3 `9c248d5229ea47ba04a2f228cf923ef20cde9169`,
futon3c `d05cb75570ea1a33a96c5fb23b437bf395da98a5` (mission sha256
`ee86811ca0e63efd24c3a355aa243b6c94aba153b6a3594199d480d7f98fb40f`). No clicks, no
shared-JVM loads, no writes under `data/` or futon3c.

## 1. The exit tokens, computed

`mission-criteria/criteria` over the mission at the pin (token = `:exit/h` +
sha1-12 of `"M-futon-seams\n:phase-exit\n<first stated line>"`):

| token | phase | line | verdict at pin |
|---|---|---|---|
| `:exit/h66b2ffcf3e0b` | MAP | 197 | `**Met.**` — observed true |
| `:exit/h2d3a87958b38` | DERIVE | 284 | `**Met.**` — observed true |
| `:exit/hac75428b9c97` | ARGUE | 498 | `**Not met…**` — open |
| `:exit/h26178f291d86` | VERIFY | 643 | `**Met.**` — observed true |
| `:exit/h4ef5c183bc55` | INSTANTIATE | 730 | `**Met.**` — observed true |
| `:exit/h54d16050a9dc` | DOCUMENT | 784 | `**Not started.**` — open |

Each locator is the reader's own `:met-decl`: the criterion's stated text (line
breaks kept) followed by `**Met.**`, C4 over the mission at HEAD. The dispatch's
premise — "DERIVE, ARGUE, INSTANTIATE and DOCUMENT read not met" — was true when
written and **changed mid-packet**: futon3c `d05cb755` ("Verdict lines made exact,
and a check so prose and data cannot drift") restated DERIVE and INSTANTIATE as
`**Met.**`. §3 answers the dispatch's question with that record.

## 2. The four interpretations (patterns, guards, runners-up)

Guard tokens are the exit tokens of §1; every receipt is `:kind :hand-admitted`,
author kimi-6, 2026-09-24, with the flexiarg's path, futon3 revision and sha256
(hashes recomputed on the library bytes today). Full readings/scope-limits/
observation-limits are in the proposal file.

**DERIVE `:exit/h2d3a87958b38` — `contracts/every-entry-has-a-falsifier`**
(sha256 `ae7e7a57…`). Rewrite reading: DERIVE's exit ("someone could implement from
the section alone") is produced by every one of the method's nine steps carrying a
check that can contradict it — the mission's own table now reads "Every step now has
a check", and the four by-hand steps are recorded judgements, deliberately not
automated. Guard needs MAP met (the survey the method derives from). Runners-up:
`cascade-construction/order-by-what-each-step-needs` (about precedence inside a
cascade, not about a section being implementable); `futon-theory/mission-lifecycle`
(names the phase, states no mechanism); `apparatus/monitors-measure-the-work`
(measures work in flight, not the completeness of a written method).

**INSTANTIATE `:exit/h4ef5c183bc55` — `cascade-construction/run-it-on-a-real-case`**
(sha256 `3cb90507…`). Rewrite reading: the exit is a concrete, reproducible
demonstration per completion criterion; the pattern's move — run the constructed
cascade on a real recorded case and let disagreement reshape the design — is what
click-001's enactment over instance 4 was (8 attempts recorded, wants observed by
two tests and a grep), and the IDENTIFY exit scoped the mission to one instance, so
that demonstration IS the criterion. Guard needs VERIFY met. Runners-up:
`translation/test-by-reproducing-behaviour` (one step inside the demonstration —
the redirect test — not the demonstration itself; already warranted as step 6/13 of
the exemplar); `apparatus/done-is-observed-running` (observes running, not
reproduce-from-the-doc).

**DOCUMENT `:exit/h54d16050a9dc` — `writing-coherence/meet-the-reader-where-they-are`**
(sha256 `87403807…`). Rewrite reading: the reader is a docbook browser who has never
heard of the mission; the pattern's anchoring move (minimal anchors in the reader's
register, the mission link elidable) is what "discoverable without knowing it
exists" requires, against today's state (the only rendered account reachable by URL
alone, nothing navigates to it). Guard needs INSTANTIATE met — what the mission
built must be demonstrated before it is discoverable. Runners-up:
`writing-coherence/plain-language-thesis` (top-level claim legibility — the right
pattern for ARGUE's half, but DOCUMENT's exit is about the *browsing* reader finding
the entry at all); `writing-coherence/meta-lede` (opening sentences only).

**ARGUE `:exit/hac75428b9c97` — `writing-coherence/plain-language-thesis`**
(sha256 `ca0744c5…`). Rewrite reading: the exit's inevitability half is already
measured and the answer is negative (selection margins 2/7; "defensible, not
inevitable"); the mission states the plain-language half is DOCUMENT's and "this
phase closes when DOCUMENT is written" — so the guard needs the DOCUMENT exit, and
the produced work is the mission's design claim stated standalone, with the
negative margin recorded rather than flattered. Runners-up:
`futon-theory/honest-map-over-flattering-counter` (the *discipline* the reading
must obey — it conditions the move rather than producing the exit);
`writing-coherence/meet-the-reader-where-they-are` (register anchors for a document
serving two audiences; ARGUE's reader is an outsider to the project, standalone
legibility is the axis).

## 3. DERIVE and INSTANTIATE: what "**Met.**" in full took, and whose statement it was

The dispatch asked what the machine would have to do for each partial verdict to
read `**Met.**`, and whether that is machine-producible content or a mission-owner
statement. The record now answers both directly (`d05cb755`, authored while this
packet was in flight):

- **DERIVE.** The substance was already done — every step had a check; the verdict
  line was *stale* ("Met for the tooled steps…" while `lifecycle.edn` had said
  `:exit-met` since `1afa87d6`). What Met-in-full took was the owner correcting the
  verdict line and adding a check so prose and data cannot drift again. **A
  mission-owner statement** — the machine can produce and verify the per-step
  checks, but the verdict that the section satisfies its exit is the owner's act;
  the commit message credits an outside reader (the criteria reader itself) with
  finding the staleness. This is the reader working as designed: the partial
  verdict was a real signal, and its resolution was a text change justified by
  already-existing evidence.
- **INSTANTIATE.** Met-in-full took a *scope* reading: the IDENTIFY exit set the
  scope ("pick one instance … not all eight"), so instance 4's demonstration is the
  mission's completion criterion and the seven unenacted instances are "the scope
  working, not a shortfall". **A mission-owner statement about scope**, not machine
  content — the machine could have produced demonstrations for the other seven
  instances (real per-instance cascade work), and under a different owner reading
  that would have been the work; the owner instead read the scope back from
  IDENTIFY. Both paths are recorded so the choice is visible.

Consequence for the proposals: the DERIVE/INSTANTIATE interpretations are prepared
content whose `:forbids` correctly make them inapplicable today (their tokens are
already observed) — they would have been the producers had the verdicts still read
partial, and they document which pattern warrants each exit's shape.

## 4. Validation transcript (offline, own JVM process; fixture method of click2-replay)

Script `/tmp/seams_validate.clj`: wants/universe/locators computed by
`mission-criteria` against the mission at HEAD (no hand-typed tokens); a minimal
`:wm/cascade-source-v1` file for the target assembled in a temp dir; construct run
with the fixture budget `{:max-moves 8 :max-expansions 10000}`, horizon 4,
move-cost 0, stand-in `:evaluate-g` (construction asserted, not ranking).

```
wants:    [:exit/h66b2ffcf3e0b :exit/h2d3a87958b38 :exit/hac75428b9c97
           :exit/h26178f291d86 :exit/h4ef5c183bc55 :exit/h54d16050a9dc]
universe: {MAP true, DERIVE true, ARGUE false, VERIFY true, INSTANTIATE true, DOCUMENT false}
unlocated: []          ; every criterion carries a verdict, so every want has a locator

LOAD-DECLARED (temp dir): target M-futon-seams loads; all four patterns and
receipts present under [:interpretations "M-futon-seams" :patterns|:receipts].

CONSTRUCT, all four interpretations:
  status :constructed
  candidate [:writing-coherence/meet-the-reader-where-they-are
             :writing-coherence/plain-language-thesis]  unreached: []
  — DOCUMENT then ARGUE, exactly the mission's own ordering ("ARGUE closes when
    DOCUMENT is written"); DERIVE/INSTANTIATE patterns correctly NOT in the plan
    (their wants already observed; :forbids blocks them).

FALSIFIER, one interpretation removed at a time:
  -every-entry-has-a-falsifier      => :constructed, unreached []  (DERIVE already met — correctly inert)
  -run-it-on-a-real-case            => :constructed, unreached []  (INSTANTIATE already met — correctly inert)
  -meet-the-reader-where-they-are   => :refused :no-supported-order
                                       (DOCUMENT has no producer; ARGUE's guard needs DOCUMENT,
                                        so no plan reaches any new want)
  -plain-language-thesis            => :constructed, unreached
                                       [{:token :exit/hac75428b9c97, :reason :no-producer}]
```

One defect of this packet's own process, recorded: the first validation run refused
`:interpretation-receipt-missing` because the proposal file's first draft closed the
receipts map one brace early (three receipts landed at top level). The loader caught
it before any use; the fix is in the committed file.

## 5. Update (claude-8's SEAMS-INTERP amendment, same day; follow-up commit, original above not rewritten)

- **Open exits are ARGUE and DOCUMENT only.** §1's table already reflects the pin at
  `ee86811c`: DERIVE and INSTANTIATE read `**Met.**`. Their interpretations are
  kept in the proposal file, each marked **"not a first-flight want (exit reads Met
  at ee86811c)"**; their `:forbids` make them correctly inapplicable while the
  tokens observe true (validation: removing either changes nothing).
- **ARGUE constraint (mission owner, binding).** ARGUE is not met because the design
  measured *defensible, not inevitable* (2 of 7 patterns reachable from the problem
  statement, neither candidate dominating), and closing it must not reverse that
  finding. The interpretation honours this structurally: (i) its guard **consumes
  DOCUMENT's account** — `:needs #{:exit/h54d16050a9dc}`, so the plain-language
  close rides on DOCUMENT's product, per the mission's "this phase closes when
  DOCUMENT is written"; (ii) on "forbid a softened finding if any token can express
  that": **no token in the mission's current vocabulary expresses the finding's
  retention**, so the guard cannot carry it — typed absence, not an omission. The
  constraint is instead pinned by the receipt's `:target-source` (the interpretation
  is bound to the mission revision whose ARGUE section records the negative
  measurement) and stated explicitly in `:scope-limit`: "SOFTENING THE FINDING IS
  OUT OF SCOPE … an enactment that reverses, rewords away, or omits the measured
  result is not an enactment of this interpretation." If a future amendment wants
  the guard itself to carry it, the mission would need a token for the finding
  (e.g. a recorded `proto/selection-margins.edn` locator) — proposed here as a
  possible amendment, not adopted.
- Re-validated after the edits: parse, load-declared round trip, construct
  (`:constructed`, candidate `[meet-the-reader-where-they-are
  plain-language-thesis]`, `:unreached-wants []`) and the four-way falsifier are
  unchanged from §4.

## 6. What this does not claim

Not promoted; no sources file exists for this target and creating one is a separate
step. The stand-in G asserts construction, not selection — under the lane's real G
the plan must still beat the empty family (D12). The DOCUMENT and ARGUE exits are
reader-judged criteria ("someone browsing… can discover"; "someone outside the
project can understand"): the locators witness the owner's `**Met.**` verdict, never
the discovery or comprehension act — that limit is stated in each receipt's
`:observation-limit`, and the drift-check added in `d05cb755` is what keeps the
verdict lines honest against `lifecycle.edn`.
