# C2 wiring gap: cited author questions cannot become legal terminals

Date: 2026-11-06. This is a read-only discovery against futon2 main
`01849cc69` and the retained run
`2026-11-06-a9fa3407-56f7-419f-aee7-126ad2a95c0b`.

## Conclusion

The C2 pieces exist but are islands. `c2-question/question-carrier` can pin
and revalidate an exact mission span, and `morning-brief/queue-c2-question!`
can durably publish it, but neither function has a production caller. The
author wire offers only `REFUSE <typed reason>` and reduces that reply to a
free-text reason. Close legality has no C2 branch, and durable exclusion
publication/consumption handles C1 and C3 only. Therefore even an author whose
prose identifies a real ambiguity reaches `:refusal :other`, hence the
debugger, never Lean's `legitimateQuestion` terminal.

The Lean requires all three facts. `caseChecked` requires both
`ambiguityCited` and `questionInBrief`, and `clickLegal` additionally requires
`exclusionRecorded` (mathlib4
`DarkTower/WarMachine/RefusalLegality.lean:67-89`). The individual failure
theorems are explicit: an unexcluded terminal is illegal, as is a question
without a brief receipt or cited ambiguity (ibid. `:95-108`).

## 1. Existing C2 callers

A bounded search of every `.clj` file under `src/futon2` finds only the two
definitions, and no callers:

* `question-carrier` is defined at
  `src/futon2/aif/c2_question.clj:26-54`. It checks the retained source SHA,
  delegates exact-span validation to `mission-reading/question-reasons`, and
  emits `:wm/c2-question-v1` with source pin, span, alternatives, reading
  receipt, and canonical question digest. `ambiguity-cited?` rechecks all of
  that at `:56-69`.
* `queue-c2-question!` is defined at
  `src/futon2/aif/morning_brief.clj:223-251`. It writes a
  `:c2-question-pending` item and returns its path/SHA/question-digest receipt;
  `c2-receipt-valid?` checks the durable item at `:253-265`.

Consequently neither is reached from author dispatch, refusal classification,
nor close.

## 2. What the author can return, and what survives

The author parser recognizes exactly `DONE`, `REFUSE`, and `REFUSE-HARM`.
Ordinary refusal becomes only `{:verdict :refuse :reason <trimmed string>}`
(`src/futon2/aif/full_loop_runner.clj:3995-4015`). The primary prompt tells an
author to end with `FULL_LOOP_AUTHOR: REFUSE <typed reason>`
(`:4900-4918`), and the one correction round asks it to repeat
`REFUSE <reason>` (`:5004-5019`). Neither contract offers a question object,
source span, exact quote, alternatives, or C2 schema.

There is already a suitable question grammar elsewhere, but only for mission
reading: `{:question, :span {:lines, :quote}, :alternatives}` is prompted at
`src/futon2/aif/mission_reading.clj:95-117`; validation demands an exact
resolving quote and at least two nonblank readings at `:126-139`, and a locator
reading can return checked `:questions` at `:173-186`.

For an ordinary reason without a proof SHA,
`author-refusal-classification` emits only
`{:outcome :guardrail-refusal :refusal-reason reason}`
(`full_loop_runner.clj:4297-4316`). `throw-if-author-refused!` then keeps the
outcome, failure kind/stage, reason, target, and whole author job in ex-data
(`:4318-4368`), but mints neither a checked question carrier nor a Morning
Brief receipt. Thus prose in the retained job is not evidence for either C2
boolean.

## 3. Exclusion transaction and what C2 can reuse

The Clojure legality mirror already knows the abstract C2 rule:
`:legitimate-question` checks `ambiguityCited && questionInBrief`, and every
click terminal additionally checks `exclusionRecorded`
(`src/futon2/aif/refusal_legality.clj:28-46`). But
`evidence-for-close` has concrete branches only for C1, C3, L1, and L2; all
other outcomes become `{:refusal :other}` (`:75-120`).
`resolve-close-refusal` permits the intermediate `:pending-exclusion` only for
C1/C3 after their case check and mission-source pin succeed (`:138-157`).

After the immutable run record is published, `terminal-entry` converts a
pending C1 or C3 into the corresponding exclusion row and only then changes
the compact index disposition to `:click-terminal` with
`exclusionRecorded true`; unknown cases become debugger errors
(`src/futon2/aif/outer_refusal_index.clj:64-84`). Failure to publish a pending
exclusion is fatal (`:86-105`). C1 and C3 rows both bind the target to a digest
of the current mission-source SHA
(`src/futon2/aif/c1_exclusion.clj:17-50` and
`src/futon2/aif/c3_exclusion.clj:16-37`). At the next selection, runtime reads
only `:c1-exclusion` and `:c3-exclusion` rows
(`src/futon2/aif/full_loop_runtime.clj:1638-1668`), and outer selection accepts
only their two reason keywords (`src/futon2/aif/outer_select.clj:199-211`).

C2 can reuse this transaction pattern and the common unchanged-mission digest
semantics, but not the code unchanged: it needs a C2 row carrying the question
and brief pins, a C2 case/key in publication, and a C2 reason in runtime and
outer-selection validation.

## 4. Smallest change set, one behavior per packet

1. **Author question wire.** Add an explicit terminal marker with the existing
   mission-reading question shape (for example, `FULL_LOOP_AUTHOR: QUESTION
   <EDN>`), describe exact span/quote and two alternatives in both author
   prompts, parse it separately from plain `REFUSE`, and build
   `question-carrier` from the already pinned mission bytes. Red test: an
   exact cited question yields a checked carrier; a missing/mismatched span is
   typed invalid; plain refusal still yields only guardrail refusal.
2. **Brief publication.** For a checked author-question carrier, call
   `queue-c2-question!` before close and retain its validated receipt alongside
   the carrier. Red test: one valid question creates one pinned brief item;
   publication failure or a mismatched receipt never claims
   `questionInBrief`.
3. **C2 close evidence.** Add one C2 outcome branch to `evidence-for-close`
   that rechecks `ambiguity-cited?` against pinned bytes and
   `c2-receipt-valid?`, and admit C2 to `:pending-exclusion`. Red tests: both
   checks true reaches pending exclusion; carrier-without-brief,
   brief-without-citation, and ordinary `guardrail-refusal` remain debugger
   errors. This is the packet that proves a plain refusal did not become a
   legal escape hatch.
4. **C2 exclusion publication and consumption.** Define a C2 exclusion row
   pinned to mission-source SHA, question digest, and brief receipt; publish it
   through `terminal-entry`; consume it in runtime; allow only the new typed
   C2 exclusion reason in outer selection. Red test: a pending C2 record
   becomes click-terminal and excludes its target on unchanged mission
   evidence, while changed mission evidence restores it; failure to publish
   remains a debugger error.

These packets keep the three Lean premises separately observable. Folding
them into a free-text reason check would falsely treat the author's assertion
as both cited ambiguity and durable brief evidence.

## 5. Is M-formal-patterns excluded now?

No. It remains eligible to be offered again (selection is not guaranteed).
The held click was observed at `:close-refusal`, retried, re-held, and aborted
(`data/wm-click-loop/ledger.jsonl:1013-1018`). Its terminal record retains a
debugger abort and `:refusal/disposition` debugger error, not pending exclusion
(`data/wm-runs/tick-run-record-2026-11-06-a9fa3407-56f7-419f-aee7-126ad2a95c0b.edn:1`).
The compact index likewise has an ordinary indexed debugger-aborted row with
no `:c1-exclusion`, `:c3-exclusion`, or click-terminal disposition
(`data/wm-runs/outer-refusal-index.ednl:301`). Since runtime constructs the
next field exclusions solely from those C1/C3 keys, this cancelled close
records no target exclusion.

This is consistent with recent production history: M-formal-patterns appears
repeatedly in compact rows 296-301, including two earlier debugger aborts,
then grounded progress, two selection abstentions, and this abort
(`data/wm-runs/outer-refusal-index.ednl:296-301`). Exact-policy history or
other admission facts can still affect which policy wins, but the target is
not removed under the Lean C2 `nextField` rule. That rule expressly removes a
legal terminal's target only when the evidence digest is unchanged
(`DarkTower/WarMachine/RefusalLegality.lean:146-165`).

The underlying text is genuinely a candidate for the new channel rather than
proof of legality by itself: the mission is marked OPEN
(`futon5/holes/M-formal-patterns.md:1-4`) while its freeze restricts work to
eight registered items and says not to dispatch a slice
(`futon5/holes/M-formal-patterns.md:104-109`). Whether work on a registered
item counts as a forbidden slice is exactly the interpretation that must be
stated with alternatives, pinned in the brief, and excluded before C2 can
close legally.
