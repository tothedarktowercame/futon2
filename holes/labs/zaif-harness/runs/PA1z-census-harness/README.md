# PA1z — the Box 12 census harness, and its negative control

Row `:PA1z` on the zaif-harness board. Track: Box 12 process assurance
(Joe's ruling of 2026-09-06, `EPIC-run-era.md`; commissioned to this lane
by claude-1).

## What is here

| file | what it is |
|---|---|
| `negative-control-2026-09-06.edn` | machine-readable verdicts for all 42 censused cells |
| `negative-control-2026-09-06.txt` | the same run, human-readable |

The instrument is `../../process_census.bb`; the data is
`../../census-ledger.edn`. Read the ledger's header first — it explains
why absence is mechanised and presence is pinned, which is the one design
decision in this row.

## Reproducing

    cd /home/joe/code/futon2/holes/labs/zaif-harness
    ./process_census.bb            # readable
    ./process_census.bb --edn      # the artifact
    ./process_census_test.bb       # acceptance, 22 checks

The harness resolves every path from its own location, so cwd does not
matter; this was verified from `/tmp` and from `/`.

**A census is only reproducible against pinned trees.** This run read
three repositories:

| repo | HEAD at run time |
|---|---|
| futon2 | `e21d41ac` |
| futon3c | `058226e0` |
| p4ng | `7f38451` |

Other seats commit to futon2 continuously, so a later run may legitimately
differ. That is the point of the harness rather than a problem with it:
the difference will be *reported* instead of quietly absorbed.

## The result

**42 of 42 cells reproduce ALIGN exactly** — 34 `:absent`, 6 `:exists`,
2 `:named-only`. `:disagreements` is empty and the run exits **0**.

It did not start that way, and the history is the point.

### The first run caught a real citation rotting — `[E-T]`

On the harness's first run, TRACE/`recorded` was **refused** (exit 3). ALIGN
cited `war_machine.clj:6750-6759` for the `:TRACE` attachment; that range no
longer contained the token, which the harness located at **line 6789** and
reported as drift rather than as a bare complaint. The cell's other pointer
(`trace.clj:723-745`) still landed, and the *verdict* was never in doubt — only
the line citation had moved, under the wm loop's continuous edits to the
most-edited file in the declared scope.

Per this row's rule the finding was **routed to claude-1**, who authors ALIGN.
They re-read the token at HEAD and appended a dated correction to the `[E-T]`
cell, keeping the original range as history (futon2 `4c400a62`); this ledger
then re-lifted the corrected pointer from ALIGN. The document led, the ledger
followed — at no point did the harness silently re-point a stale citation,
which is how a census would launder a stale reading into a fresh-looking one.

**The instrument paid for itself on its first run**, and the acceptance test now
pins the *corrected* pointer so a re-drift is caught again rather than absorbed
as normal. Expect a re-drift: nothing about the repair makes `war_machine.clj`
stop moving.

## What this row deliberately did not do

- It made **no new census claims**. It re-derives ALIGN's six nodes and
  stops. The 14 uncensused nodes are `PA2z`/`PA3z`/`PA4z`.
- It did **not** edit ALIGN. That file is in claude-1's lab.
- It did **not** move the drifted pointer. Silently re-pointing a stale
  citation is how a census launders a stale reading into a fresh-looking
  one.

## One thing that went wrong, recorded because it is instructive

The first run reported **three** stale cells. Two were false alarms: the
pointers were fine and my `:expect` tokens were wrong — I had guessed
`"phase"` and `"R16"` for ranges where ALIGN never claimed either. A
harness with a high false-alarm rate teaches its readers to skip it, which
is precisely the failure mode `N-process-trap-recording-conventions.md` §0
is about. The rule that came out of it is now recorded in the ledger
header: **an `:expect` token is lifted from what the reading itself
claims, never guessed from surrounding code** — and a stale report gets
checked by hand before it is believed. Only `[E-T]` survived that check.
