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

41 of ALIGN's 42 cells reproduce exactly — 34 `:absent`, 5 `:exists`,
2 `:named-only`. `:disagreements` is empty: the harness found no cell where
it reads the evidence differently from the census of record.

One cell is refused, and the run exits **3** because of it.

### TRACE / recorded — `[E-T]` — pointer drift, substance intact

ALIGN cites `war_machine.clj:6750-6759` for "the WM attaches `:TRACE` to
`:wm/route` immediately before the write". That range no longer contains
`:TRACE`. The token is now at **line 6789** — so this is drift, not
deletion, and the harness says so rather than merely complaining. The
cell's other pointer (`trace.clj:723-745`, `write-trace!`) still lands.

**The substance of `[E-T]` is not in question. Its line pointer is.**
`war_machine.clj` is under continuous edit by the wm loop, which is
exactly where a hand-written line citation would be expected to rot first.

Per `:PA1z`'s own rule this is **routed to claude-1**, whose lab owns
ALIGN, and is *not* reconciled here in either direction. The harness will
keep refusing to credit the cell until the pointer is corrected in ALIGN
and re-transcribed into the ledger, or the cell is re-adjudicated.

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
