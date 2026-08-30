# R9-D1b — where the thirteen closed repair rows live

Packet: `holes/problems/BUILD-packets/R9-D1b.md`. Builder: **codex-5**, job
`invoke-1788102965707-4301-472672aa`, 2026-08-30. Record: `P-R9.md@e01dab9`.

**Provenance note (claude-20, tech lead).** codex-5 delivered this content in its bell reply but did not
write or commit the note the packet required. Rather than spend a round-trip on a file write, I have
transcribed its findings here and marked my own independent checks separately, so the finding is durable
and its authorship is not blurred. Sections marked **[codex-5]** are its report; **[claude-20 verified]**
are checks I ran myself. The packet-compliance gap is recorded in `BUILD-ledger.md`.

## Answer

**The thirteen are identifiable rows, not an aggregate only** — but only at one commit.

## The artefact

**[codex-5]** `p4ng/vetting/OBLIGATIONS.md` — in the **p4ng** repo, not under `futon2`. It declares
itself a row ledger, and carries R9's own rule in its own words at `OBLIGATIONS.md:6-13`: *"Each row
closes only by supplying the missing evidence, correcting the referent, or withdrawing the claim
outright"* and *"The author (claude-4) may not mark a row closed on the strength of its own re-reading."*

## Shape and vocabulary

**[codex-5]** Rows are Markdown sections headed `## O…`, each carrying an inline `**Status: …**` marker
in its body (e.g. `O1` at `:17-45`, `O20` at `:363-394`). The declared status vocabulary is at
`OBLIGATIONS.md:12-13`: `open | fixed | withdrawn | disputed`. Some rows carry no top-level `**Status:`
at all (`O21` at `:398-415`, `O22` at `:431-455`).

**[codex-5]** **`closed` is not a token of this ledger.** Closure is written `fixed` or
`fixed by withdrawal`.

**[claude-20 verified]** Confirmed at `OBLIGATIONS.md:12-13`. This is why the tech lead's earlier
word-count over the file (24 open / 6 closed / 3 withdrawn / 4 corrected) was meaningless: it counted a
word the artefact does not use as a status, which is why it was recorded as asserting nothing.

## The thirteen

**[codex-5]** At commit `6c288174` — the commit that wrote the "The author has since closed thirteen"
sentence at `OBLIGATIONS.md:404-407` — parsing the section/status shape gives
`TOTAL=22 CLOSED=13 OPEN=7 MISSING=2`, with
`CLOSED_IDS = O1, O2, O3, O5, O6, O7, O8, O9, O14, O15, O16, O17, O20`.

**[claude-20 verified]** Re-parsed independently (`git -C p4ng show 6c288174:vetting/OBLIGATIONS.md`,
sections by `^## (O…)`, status by `\*\*Status:\s*([^*\n]+)\*\*`): **22 sections; 13 matching `fixed*`
(7 `fixed`, 5 `fixed.`, 1 `fixed by withdrawal.`); 7 open; 2 with no marker (O21, O1c)**. Matches
codex-5 exactly.

## The corpus must be pinned to that sha

**[codex-5]** Parsing the **current** file finds later derivative rows `O1c` (`:417-427`) and `O1d`
(`:457-474`); current totals are `TOTAL=24 CLOSED=14 OPEN=7 MISSING=3`. The extra closed row is `O1d`,
**so the number is no longer thirteen in the current file.**

**[claude-20]** R9-D2's corpus is therefore `OBLIGATIONS.md@6c288174`, never bare `OBLIGATIONS.md`. This
is the same lesson as the record anchoring at `e01dab9`, arriving from the other direction: an unanchored
corpus quietly becomes a different corpus, and a checker run against HEAD would return fourteen and be
correct about the wrong thing.

## Who closed them — not a row field

**[codex-5]** The ledger records the rule and the accusation but **no per-row closer field**. `git blame`
on the thirteen status lines attributes them to commits `cb996a7`, `c5375a4`, `ec50d31`, `278e409`
(authored by Joseph Corneli), **but that is VCS metadata, not a row field**, and codex-5 refused to
present it as one.

**[claude-20]** This bears directly on R9-D2. `P-R9`'s `solved` (1) requires `producingPart` to be
*declared, never inferred*. Here it does not exist in the artefact at all and would have to be
constructed from git history — so the honest disposition for these rows may be `unknown` rather than
`self`, and that is the owner's call, not D2's.

## Refusals

**[codex-5]** Not treating `futon2/data/wm-repair-obligations/` as the thirteen: parsing
`resolutions/*.edn` gives 30 records all `:resolved` (with a `:reviewer` field), and `findings/*.edn`
gives 54 all `:open`. Structured repair records, but `repair-attempt-*`, not the paper's `O…` rows.
Searched and reported rather than assumed absent.

**[codex-5]** Not claiming the ledger records who closed each row; it does not.
