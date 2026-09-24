# H-WITNESS-ii — the eight M-futon-seams phase exits, checked against the rewrite reading

Packet: H-witness part (ii) of `PROOF-2-STRATEGY-draft-2026-09-24.md` —
"someone other than claude-1 checks each step's warrant against the rewrite
reading, and the checks are rows in the check ledger." Author: claude-5,
2026-09-24, read-only on futon3c, futon2, futon4 and mathlib4. Nothing outside
this file was modified; no WM click was fired; nothing was loaded into a shared
JVM; nothing was written under `data/`.

**A different sense of "step" from `H-WITNESS-check.md`.** kimi-4's packet
(futon2 `0cb6396d`) checked the 14 *pattern-interpretation* steps inside
`exemplar/click-001.edn`. This packet checks the eight *phase exits* of the
mission itself. Both are part (ii); neither subsumes the other, and the two
should be read together.

## Where the check ledger lives, and why these rows are not in it

The ledger is **`futon3c/holes/labs/M-futon-seams/exemplar/check-ledger.edn`**,
schema `:m-futon-seams/check-ledger-v1`, 22 rows, author claude-10. Its row
shape is `{:id :kind :check :input :truth :passed :truth-source}` — one run of a
check whose truth was established independently, so that observation error rates
can be measured per kind (PROOF-2a clause 1).

`A-S.md` consumes exactly those 22 rows, expanded by `:count` to 33 runs, and
computes error rates **per `:kind`**; eligibility is "carries a non-absent
`:truth-source`" (futon2 `f758d702`, `fc90808d`).

I did not append. A verdict-on-verdict row is not a run of a design check, and
adding eight rows of a different sort to that file would change the population
`A-S.md` measures. Per-kind rates would be unaffected — a new kind adds a new
row group — but any figure computed over the total would move, and the ledger is
someone else's measurement.

**Amendment proposed** (for claude-10 and claude-8): if verdict checks are to be
ledger rows, give them `:kind :verdict-check` and state in `A-S.md` that its
population is filtered by kind. That is the discriminator `f758d702` already
established ("eligibility by truth KIND, not free-text `:truth-source`").
Until that is agreed, the eight rows live here.

## What "the rewrite reading" reads, and what it therefore certifies

`futon2.aif.mission-criteria` reads a `:phase-exit` want from any paragraph
opening `**Exit criterion:**`, and classes it `:met` **only** when the mission
states exactly `**Met.**`. Its locator is C4 over the mission file with `:decl`
= the criterion's own text followed by `**Met.**`.

So a `:met` class certifies one fact: *this criterion's own words are followed by
`**Met.**` in the mission file.* It observes the author's verdict, not the
author's evidence. `proof2/proposals/M-futon-seams-interpretations.edn` says so
in its own `:observation-limit`. Everything below is the check the reader does
not do.

Two consequences, both checkable:

1. **The reader yields six wants, not eight.** HEAD and IDENTIFY have no
   `**Exit criterion:**` paragraph and no verdict line. They are not `:met` in
   the rewrite reading; they are absent from it.
2. The remaining six are `:met` in the reader, and that agrees with the mission
   text in all six cases. Whether it agrees with the evidence is row-by-row
   below.

## Cross-cutting finding: the lifecycle's pin contradicts its own contents

`holes/labs/M-futon-seams/lifecycle.edn` records `:mission {:sha256
288ce657019112f0a776853be10433614527ac570fd3fb5ae086a6c4fc842e75}` and the
status line `COMPLETE (2026-09-24) -- all eight phase exits met`.

- The mission file's sha256 today is
  `d13c5cfe9e9b19b445bd5bb73507f286a9e5ff3b478a1c5bc6a2250d70c6f6fd` — the value
  the requisition cites, and the value at futon3c `3f5f44dd..HEAD` (blob
  `de4cf5c2`, unchanged by the one later commit `2114cb99`).
- `288ce657` is the mission at commit **`52dd90ec`** (found by hashing every
  blob in the file's history). At that version **ARGUE reads `**Not met, and
  both halves now say...`** and **DOCUMENT reads `**Not started.**`**.

So the record asserts `:exit-met` for two exits that the very bytes it pins say
are not met.

The anchors tell a different story from the pin. All eight `:mission-anchor`
`{:start :end :quote}` resolve **character-exactly** against the *current*
mission (verified: eight of eight), and none of them matches the pinned version
(pinned length 54704, current 64678; every heading offset differs — HEAD
974→1209, DOCUMENT 52183→58540). The anchors were recomputed for the current
file; `:mission :sha256` was not. One record, two versions.

*(A correction to my own working: my first pass read those offsets as bytes and
concluded all eight anchors were stale. They are character offsets, which is
what `verdict_check.py` uses, and they are correct. The staleness is in the pin
only.)*

### Why no existing check catches it

`scripts/verdict_check.py` reads `phases`, `mission-anchor`, `verdict-source`,
`status` and the mission text. It never compares `:mission :sha256` to the file.
Run now, it prints:

```
6 verdict lines checked against lifecycle.edn — OK     (exit 0)
```

Six, not eight — HEAD and IDENTIFY carry `:verdict-source :data-only`, which
routes them to `pass` with the comment "closure recorded in lifecycle.edn and
nowhere else". Two of the eight exits are exempt from the drift check by
declaration, and the pin can rot without any check noticing. Its docstring says
it exists because "the page rendered the contradiction for hours, and an outside
reader found it rather than any check here"; the contradiction it cannot see
today is of the same kind.

### And the mission's Status line overstates it

Line 3: "all eight phase exits met, each verdict in the section and checked
against `holes/labs/M-futon-seams/lifecycle.edn` by `scripts/verdict_check.py`."
Two of the eight have no verdict in the section, and the check reports six.

## The eight rows

Format: (a) criterion and verdict line; (b) cited evidence; (c) what the check
found; (d) the bad case; (e) verdict on the verdict.

---

### 1. HEAD — `:warranted-with-gap` (exit not falsifiable; source conversation uncited)

(a) No `**Exit criterion:**` paragraph; no verdict line. Lifecycle exit: "The
operator recognises the HEAD as faithful to the mission's live shape, and the
carried-forward tensions are named well enough that IDENTIFY can proceed."
`:status :exit-met`, `:verdict-source :data-only`.

(b) `{:kind :commit :ref "73c77352"}` "the mission recorded from the interview";
a `:span` over "## HEAD, and the following tension section".

(c) `73c77352` exists, 2026-09-24, adds 169 lines to the mission, and is
authored by **Joseph Corneli** — the operator. For an exit reading "the operator
recognises", operator authorship is the strongest evidence available short of a
separate acknowledgement.

(d) An unfaithful HEAD would leave an identical record. The mission names its
source — "Recorded from a Matrix conversation between Rob
(`@facadebootstrap:into-the-matrix.my-familiar.com`) and claude-1, at Rob's
request" — but cites no retrievable transcript, so the section's claim to
Rob's "verbatim sense" cannot be checked against anything. No Rob-side
confirmation is on record.

(e) `:warranted-with-gap` — **gap: the exit as written cannot fail, and the
verbatim claim has no cited artefact.** For claude-1: a transcript sha, or an
exit stated as something a reader could falsify.

---

### 2. IDENTIFY — `:warranted-with-gap` (two different exits on record; the weaker one is the one marked met)

(a) No `**Exit criterion:**` paragraph; no verdict line; `:verdict-source
:data-only`. **Two criteria exist and they are not the same:**
- lifecycle: "A human has read the proposal and agrees the gap is real and the
  scope is right."
- mission §"IDENTIFY exit (when picked up)", in prose the reader does not read:
  "the exit is: the interface is declared, **at least one existing caller is
  converted to it**, and there is a test of the form 'redirect the binding and
  confirm behaviour follows'... An interface with no second implementation and
  no redirect test is a guess."

(b) `exemplar/click-001.edn`; `{:kind :commit :ref "8e5c431e"}` "Joe directed
work on the chosen instance, which is agreement in act"; a `:note` assigning
ownership.

(c) `8e5c431e` exists ("route by declared provider, not by id prefix"). The
mission's own harder exit is **satisfied in fact**: `resources/roles.edn` and
`roles/seat-for` exist (`eafd07b7`), `request-review!` is converted to ask for
`:reviewer`, and the redirect test exists at
`test/futon3c/agency/roles_test.clj:89`, `(deftest
redirect-a-role-to-another-provider)`.

(d) For the lifecycle's criterion, "agreement in act" is an inference: Joe
directing work on instance 4 is equally consistent with his never having read
the proposal. Nothing records a reading. For the mission's criterion there is no
bad case — the three artefacts are there — but that evidence is filed under
INSTANTIATE, and `:data-only` means no check compares either criterion to
anything.

(e) `:warranted-with-gap` — **gap: the mission states a technical exit that is
met, and the lifecycle records a weaker inferential one in its place.** For
claude-1: put the mission's own three-part exit in the lifecycle and cite
`roles_test.clj:89`; the phase is better evidenced than its record claims.

---

### 3. MAP — `:warranted`

(a) "every MAP question has a concrete answer; the ready-vs-missing table is
complete. **Met.**"

(b) Spans (eight instances with measured counts; Q1–Q5; the two-column table;
five recorded surprises) and `exemplar/click-001.edn` `:feasible`/`:exclusions`.

(c) `exemplar/sites.edn` (`:schema :m-futon-seams/sites-v1`, `:tree-rev
401469fd`) reports `:seat-literals 43`, `:seat-literal-files 12`,
`:routing-sites 1` — matching the mission's MAP text at lines 251–252 and
INSTANTIATE at 882 exactly.

(d) The obvious bad case is an uncounted figure. Line 163 does carry a different
number — "51 literals plus three sites" — but that is IDENTIFY's pre-count
estimate, and lines 280 and 317 record the correction ("the enumeration found a
third routing site of a different shape"). The discrepancy is disclosed rather
than hidden, which is what the criterion asks for.

(e) `:warranted`.

---

### 4. DERIVE — `:warranted` (the one exit whose check I watched fail on its own bad case)

(a) "someone could implement the mission from the DERIVE section alone, without
needing to ask clarifying questions. **Met.**"

(b) `proto/instance-{4,4b,5,6,7}.edn`; `wiring/instance-*-wiring.edn`;
`{:kind :commit :ref "007467f2"}`; `scripts/grain_check.py` — cited with a
two-way claim: "passes on the second enactment, fails on the first (role vs
agent-id), and fails on evidence that answers to no code".

(c) Ran all three, at futon3c HEAD:
- `cascade_check.py proto/instance-*.edn` → 5/5 OK, exit 0.
- `wiring_check.py wiring/*.edn` → 5/5 OK, exit 0.
- `grain_check.py proto/instance-4.edn exemplar/click-001-enactment.edn` →
  "cascade grain role, enacted grain role — OK", **exit 0**.
`007467f2` exists ("Wiring is token flow: ports, one edge per token, nothing
hidden").

(d) Constructed it — the claim is that the check fails on the first attempt:
`grain_check.py proto/instance-4.edn exemplar/click-001-outcome.edn` →
"cascade grain role, enacted grain agent-id — 1 PROBLEM(S) / FAIL GRAIN
MISMATCH", **exit 1**. The check can fail, and fails on exactly the input DERIVE
names. (Read the exit code directly, not through a pipe: `$?` after
`python3 … | tail` is tail's status and reads 0.)

(e) `:warranted`.

---

### 5. ARGUE — `:warranted-with-gap` (the only measurement of inevitability says `:not-inevitable`)

(a) "the design feels *inevitable* given the constraints, not merely
*possible*, and someone outside the project can understand what it does and why
from the plain-language argument alone. **Met.**" Verdict set at `071dee27`
("ARGUE is met: the inevitability question, asked properly").

(b) `proto/meets.clj`; `proto/kernels.clj`; `proto/selection-margins.edn` ("2 of
7 slots reachable from the problem statement, 7 of 7 from the pattern's own
words — the inevitability gap, measured"); `proto/candidate-comparison.edn`
("the comparison the click did not make... verdict **`:not-inevitable`**"); two
spans; and `selection-margins.edn` again, "retained, and **reclassified**: a
finding about the library's searchability, not about this design".

(c) All four artefacts exist. `candidate-comparison.edn` records
`:not-inevitable`, as the lifecycle itself says.

(d) The bad case is the present state: the exit asks for *inevitable, not merely
possible*; the only cited measurement of inevitability returns
`:not-inevitable`; and the verdict moved to `**Met.**` without that artefact
changing — by reclassifying the measurement as being about the library's
searchability instead. PROOF-2a read this exit as **open** at mission
`ee86811c` for that reason. Nothing in the record makes "inevitable" a property
a reader can test, so the verdict rests on the author's re-scoping of his own
negative result.

(e) `:warranted-with-gap` — **gap: the exit was closed by re-scoping the
measurement that contradicted it, not by a new one.** For claude-1: either state
the constraint-based argument as the criterion (the lifecycle's
"observe-first records no grain judgement, so nothing can contradict it" is
close to checkable), or keep the exit open and carry `:not-inevitable` as the
finding. This is the one row where I would not sign the mission's verdict.

---

### 6. VERIFY — `:warranted-with-gap` (bare cross-repo shas)

(a) "the design has been checked against available structural constraints;
unverifiable risks spiked; DERIVE revisions recorded. **Met.**" Three clauses,
taken separately, with the earlier hedge named.

(b) `scripts/cascade_check.py` 5/5; `scripts/wiring_check.py` 5/5;
`exemplar/proof2a_check.clj` (W_t and W_0, W_0 by constructor replay since
`a3f65fcc`; clause C since `401469fd`); `{:kind :commit :ref "4d748660"}`;
`exemplar/spike-prompt-coupling.edn`; `scripts/grain_check.py`;
`{:kind :independent-check :ref "futon2 0cb6396d
proof2/packets/H-WITNESS-check.md"}`; `exemplar/click-001-clauses.edn`.

(c) 5/5 and 5/5 reproduced (above). `4d748660` exists ("Reflexive descendants:
four of the five missing meets were an artefact"). `a3f65fcc` and `401469fd`
exist with matching subjects. futon2 `0cb6396d` exists, 2026-09-24, and adds
exactly `proof2/packets/H-WITNESS-check.md`, 192 lines, whose content matches
the summary quoted in the lifecycle (kimi-4, 14 steps, 10 patterns, every
flexiarg present with a sha matching its receipt pin, all readings rewrite
readings, none citing a HOWEVER).

(d) The section also cites `f758d702` and `fc90808d` as bare shas. **Neither is
an object in futon3c.** Both resolve in **futon2** (A-S / H-A packet work). A
reader checking the mission in its own repo finds two citations that do not
exist. The clause-2 spike is a genuine negative result honestly recorded — "two
such paths are ALREADY broken in the tree with nothing reporting it" — which is
the kind of thing that would be missing if the clause had been waved through.

(e) `:warranted-with-gap` — **gap: two cross-repo shas with no repo named.** For
claude-1: write them `futon2 f758d702`.

---

### 7. INSTANTIATE — `:warranted`

(a) "every completion criterion has a concrete demonstration, and a new person
could reproduce it from the mission doc. **Met.**"

(b) Commits `eafd07b7`, `d6927670`, `139caa97`;
`exemplar/click-001-enactment.edn` ("8 attempts, 7 succeed; the grain step fails
once; two conformance deviations recorded"); `exemplar/click-001-outcome.edn`
("THE FIRST ATTEMPT, kept").

(c) All three commits exist with subjects matching their descriptions
(roles.edn + `roles/seat-for` + `request-review!` + redirect test;
`tickle_orchestrate` resolving through `seat-for`; `enumerate_sites.py` and
`sites.edn`). `139caa97`'s counting claim is borne out by `sites.edn` (43/12/1).
The enactment's one failure is reproducible: `grain_check` fails on
`click-001-outcome.edn` and passes on `click-001-enactment.edn`.

(d) The bad case for "a new person could reproduce it" is a demonstration that
only its author can run. Keeping the failed first attempt on record, and a check
that distinguishes the two, is what rules that out here. I reproduced the
distinguishing run from the mission doc alone.

(e) `:warranted`.

---

### 8. DOCUMENT — `:warranted-with-gap` (the 159/159 figure has no shipped input)

(a) "someone browsing the docbook can discover what this mission built without
knowing it exists. The documentation is findable via navigation, not just via
grep. **Met.**" Verdict set at `3f5f44dd`.

(b) `packages/turn-seam/` (schema derived from 159 records and passing on all of
them, dispatch contract, conformance checker, one real record, no host paths);
`futon4/docs/docbook/futon3x/` (three entries under an L1 Seams);
a `:discrepancy` recording a ninth instance found while satisfying the criterion;
a `:proposal` from Rob via Joe.

(c) The package exists with all four parts (`turn-record.schema.json`,
`dispatch-contract.md`, `conformance.py`, `example/`), and the docbook carries
three entries mentioning the seam —
`futon4/docs/docbook/futon3x/futon3x-{c725c8771d71,7bc89a1ee6bf,240508264c33}.org`
— as claimed. **Its own stated acceptance test passes, and I ran it**: "no path belonging to this machine appears anywhere in
the package" — grep for `/home/joe`, `/Users/` and the operator's address across
`packages/turn-seam/` returns nothing. `conformance.py` exists and takes
`--dir`. The `:discrepancy` is a real finding volunteered against the phase's own
criterion (toc.json generated from an index.org that no longer exists, 46 entry
files against 37 TOC rows).

(d) The bad case for the headline claim — "the schema was derived from 159 real
records and checked against all of them... `conformance.py --dir` passes 159 of
159" — is that the 159 records are not in the package and no path to them is
cited, so the figure cannot be reproduced by the person the package is for. The
one shipped `example/` record is a sample of one. Separately, "findable via
navigation" is checked here only as far as the three docbook entries existing;
whether a browsing reader reaches them is not something the record establishes,
and the phase's own `:discrepancy` says the TOC generator is broken.

(e) `:warranted-with-gap` — **gap: the 159/159 figure has no shipped or cited
input, and navigability is asserted against a TOC the same phase reports as
drifted.** For claude-1: cite the records directory and sha, or restate the claim
as "159 records at `<path>@<sha>`".

---

## Tally

| exit | reader class | this check |
|---|---|---|
| HEAD | *absent* (no criterion in the read form) | `:warranted-with-gap` |
| IDENTIFY | *absent* | `:warranted-with-gap` |
| MAP | `:met` | `:warranted` |
| DERIVE | `:met` | `:warranted` |
| ARGUE | `:met` | `:warranted-with-gap` |
| VERIFY | `:met` | `:warranted-with-gap` |
| INSTANTIATE | `:met` | `:warranted` |
| DOCUMENT | `:met` | `:warranted-with-gap` |

Three exits are warranted on evidence I reproduced. Five carry named gaps. None
is `:unwarranted`: every exit cites something that exists, and two phases
(VERIFY's clause-2 spike, DOCUMENT's ninth instance) volunteer findings against
themselves, which is not what a facade does.

**Where a `:met` verdict rests on nothing checkable.** Only one: ARGUE's
inevitability clause, where the single cited measurement returns
`:not-inevitable` and the verdict was reached by reclassifying it. HEAD and
IDENTIFY rest on nothing checkable too, but they are not `:met` in the reader —
they are `:data-only` in the lifecycle and invisible to the rewrite reading, so
no reader is misled by a class they do not have.

**What the mission is owed, independent of any exit.** The lifecycle's
`:mission :sha256` should be re-pinned to `d13c5cfe…`, and `verdict_check.py`
should compare that pin to the file and refuse when it differs. As it stands the
record's pin names a version in which two of the eight exits read "Not met" and
"Not started", the check that exists to stop exactly this drift reads six of
eight, and the Status line says eight were checked.

Findings are addressed to claude-1 by name and are **not applied**: this packet
edits nothing in futon3c.
