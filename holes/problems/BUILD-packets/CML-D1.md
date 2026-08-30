Packet **CML-D1** for the R-node build. From claude-20 (Opus tech lead) on behalf of
claude-15 (owner). Record: `/home/joe/code/futon2/holes/problems/P-control-map-lint.md`.
Governing process: `/home/joe/code/futon4/holes/delivery-lifecycle.md` (v2) — read §1, §2 and the
validation log rows 9-14 before you start; they are this build's case law.

This is a BUILD packet: one file plus one fixture test. Everything below between the rule marks is
the record **verbatim**. Do not rewrite it; if it is wrong, refuse and say where.

--------------------------------------------------------------------------
## S1

**problem.** The R-node build fans out to several builders at once (`BUILD-tech-lead-charter.md`). The
overlap points are the Figure-4 edges, lifted to data in `p4ng/empirics-futon/control-map-edges.edn`
(21 drawn, 1 unresolved `R5→R5`, 1 chartered, 26 derived-undrawn with `:by`). Nothing checks that an edge
has a payload schema, a fixture, and two endpoint records that agree on it. Without that, two builders can
each satisfy their own record and disagree at the edge — which is how R8's five individually-defensible
deliveries composed into silence.

**now.** `checks/library_graph_lint.clj` (futon3) is the precedent: baseline, attestations, fails closed,
report EDN, fixture tests. The `Delivery` type is in `futon4/holes/delivery-lifecycle.md` §0.6.

**solved.** A bb linter `futon2/checks/control_map_lint.clj` over the EDN such that: every `:drawn` edge
carries `:schema` (a map or the typed absence `:unspecified`), `:fixture` (one example payload, or
`:unspecified`), and `:endpoints-agree?` computed from the two `P-R<n>.md` records referencing the edge;
every `:derived-undrawn` edge carries `:by`; per node, in/out counts and how many edges are fully specified;
the report fails closed on a drawn edge with no entry at all. **Baseline, honestly:** the first run reports
21 drawn edges with `:schema :unspecified` — that number is the build's starting organised-fraction for the
wiring, and it is reported in two lines (specified vs unspecified), never as a percentage of "done".
**Falsifier:** a fixture test with a drawn edge missing from the EDN must fail; a fixture with a `:schema`
that the endpoint record does not mention must report `:endpoints-agree? false`.

**facades:** a schema copied from a code signature and presented as agreed by both endpoints; a fixture that
is the only payload the edge has ever carried (a fixture is an example, and the record must say so);
"specified" counted from the presence of a key rather than a non-`:unspecified` value.

**status.** open.

--------------------------------------------------------------------------
- **CML-D1 — build (one file, one fixture test).** As above. Gates: kondo, check-parens, `bb -cp . test/…`
  green, run twice deterministic, `git diff --check`. Commit explicit paths in futon2. No push.

--------------------------------------------------------------------------

## Pre-dispatch checks I ran against the current artefact (lifecycle log row 11/13)
I parsed `/home/joe/code/p4ng/empirics-futon/control-map-edges.edn` with babashka before sending
this. **Write these expected values into your test as the baseline; if your run disagrees with any
of them, that disagreement is the finding and you should report it rather than adjust the number.**

    (:edges d)            => 22 entries; :status frequencies {:drawn 21, :unresolved 1}
                             :kind frequencies {:control 10, :support 12}
    the :unresolved edge  => {:from :R5 :to :R5 :kind :support :status :unresolved :note "start point 98px..."}
    (:derived-undrawn d)  => 26 entries, **all 26 carry :by**; :status frequencies {nil 25, :chartered 1}
    edges carrying :schema   => 0
    edges carrying :fixture  => 0
    edge key set          => #{:from :kind :label :note :status :to}

So the baseline the record predicts — **21 drawn edges with `:schema :unspecified`** — is correct,
and every one of the 26 derived-undrawn edges already satisfies the `:by` requirement.

## Two defects in the record I found before dispatch — do not silently fix either
1. **The inventory line double-counts.** The record's opening says "21 drawn, 1 unresolved R5->R5,
   1 chartered, 26 derived-undrawn with `:by`". The chartered edge (`{:from :R20 :to :R14 ...
   :status :chartered}`) is the 26th entry **inside** `:derived-undrawn`, not a fourth category:
   the file holds 22 + 26 = 48 edge entries, not 49. Your linter should report the categories as
   the file actually shapes them. Flag this in your bell-back; the owner (claude-15) will amend the
   record. You may not edit S1.
2. **`:endpoints-agree?` is not computable for any drawn edge today, and must not be reported as
   `false`.** The record defines it as "computed from the two `P-R<n>.md` records referencing the
   edge". Only three such records exist (`P-R2.md`, `P-R8.md`, `P-R9.md`). Measured: of the 21 drawn
   edges, **0 have a record at both endpoints**, 6 have exactly one, 15 have neither. A boolean here
   would report `false` for all 21, which reads as "the endpoints disagree" when the truth is "there
   is no record to compare" — the empty-vs-didn't-run confusion the lifecycle exists to prevent
   (P-validated-R5 §3e law F1; R6's "empty vs didn't run").
   **Required:** on real data emit a **typed absence** — `:endpoints-agree? :no-endpoint-record`
   (or `:one-endpoint-record` where exactly one exists) — never `false`. Reserve `false` for a real
   disagreement, and exercise that branch in the **fixture** test, where you can synthesise two
   records that disagree. The record's stated falsifier ("a fixture with a `:schema` that the
   endpoint record does not mention must report `:endpoints-agree? false`") is then satisfiable on
   fixtures, which is where it belongs.

## Output path and gates
- File: `/home/joe/code/futon2/checks/control_map_lint.clj` (a bb script; `futon2/checks/` does not
  exist yet — create it). Precedent to follow for structure, baseline, fail-closed behaviour and
  report shape: `/home/joe/code/futon3/checks/library_graph_lint.clj` — read it first.
- Fixture test alongside it (the record says `bb -cp . test/...`; put it where the futon3 precedent
  puts its fixtures and say in your bell-back where that was).
- The report is EDN. It **fails closed** on a drawn edge with no entry at all.
- Report the organised fraction **in two lines, specified vs unspecified — never as a percentage of
  "done"** (the record is explicit about this).
- Gates, all of which must be green and whose output you quote back:
  `clj-kondo --lint` on the new files; `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el
  --eval "(arxana-check-parens-cli)" <files>`; the fixture test; **run the linter twice and show the
  output is byte-identical** (determinism); `git diff --check`.
- Commit in `futon2` on explicit paths only.

## Protocol (CLAUDE.md / AGENTS.md — non-negotiable)
- Commit on **explicit paths only** (`git add <path>`), **never `git add -a`**, **never push**.
- Never `load-file` a worktree copy into a shared JVM (:6768 / :7073). Test in your own process.
- **Refusal is a deliverable.** If a term in this packet has no definition you can state on the
  record's own terms, or an acceptance condition contradicts the artefact, STOP, change no files,
  and bell back naming the contradiction with file:line. Three packets in this build's case law
  (delivery-lifecycle log rows 9, 10, 11) were wrong on the commissioner's side and the builder's
  refusal was the correct delivery. Do not substitute a simpler type to keep the lane moving.
- **A causal claim names its probe** (command + result) **or is marked "inferred, untested"** (row 14).
- **A negative/absence names the instrument and its limits** before it is written down (row 6):
  "grep X over Y found 0" — not "X does not exist".
- Do not edit any `P-R<n>.md` S1 field, any edge schema, or the spine. Propose; do not amend.

## Reporting
Bell **claude-20** back with: (1) a summary, (2) commit sha(s) if any, (3) the gate outputs,
(4) every refusal, (5) anything in this packet you found to be wrong about the artefact.
Time box: **40 minutes.** If you pass it, bell back with what you have rather than running on.
