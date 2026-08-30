# BUILD — Tech-lead charter for the R-node build (2026-08-30)

Joe (2026-08-30): "line up a build process whereby we push out the R numbers to associated agents and get
them to help us build the entire compliance system, in parallel as much as possible, with overlap points
moderated by the specification" — and "bell out a tech-lead role to an Opus agent who will oversee the
build while we continue to manage the overall project from this session."

## Roles
- **Operator:** Joe. Decides scope, ratifies records, owns the spine's design decisions.
- **Owner / project manager:** claude-15 (this session). Owns every `P-R<n>.md` record's S1, the edge
  schemas in `control-map-edges.edn`, the spine (`P-validated-R5`, Snatch, Markov spec), the lifecycle
  document, and the final review gate on every closed packet.
- **Tech lead:** the Opus seat this charter is belled to. Owns the *build ledger*, the dispatch of node
  packets to Codex seats, first-line review of each packet, the control-map lint as a gate, and the daily
  report. Does not own S1 fields, edge schemas, or the spine.
- **Builders:** Codex seats from the roster (`GET :7070/api/alpha/agents`), **excluding `codex-22`** (the
  owner's WM seat), `codex-20` (the spider fleet) and `codex-18` (reserved for Joe — added 2026-08-30 after claude-20 inferred it). Zai seats are for the spider only.
- **Second reviewer (semantic half):** `claude-13` (idle; holds the R5 material) on the owner's request.

## What the tech lead does
1. Reads, in this order: `futon4/holes/delivery-lifecycle.md` (v2, all of it, including the validation log
   rows 1–14 — they are the build's case law); `P-validated-R5.md` §3e; `PREREG-war-machine.md` §1, §2, §4;
   `P-R9.md`, `P-R2.md`, `P-R8.md`, `P-control-map-lint.md`; `AGENTS.md` and the CLAUDE.md dispatch
   protocol (bells, parks, `--mode work`, quoted heredocs, explicit `git add` paths, never push).
2. Dispatches **CML-D1** first (the linter), then lanes **R9-D1**, **R2-D1**, **R8-D1** — discovery packets,
   no code, refusal permitted — to three distinct idle Codex seats, each with `--from <tech-lead-id>
   --mode work --park --park-deadline 2700`, and records job-id + park-id in the ledger at dispatch time.
3. **Reviews each returned packet as a real gate** (lifecycle §1): reads the diff / the note; re-runs the
   verify step; spot-checks three pointers against the source; states what was checked. Small findings:
   fix directly and say so. Substantial: re-dispatch as a new small packet.
4. **Before dispatching any packet that carries an acceptance or a count** (D1 discovery packets included —
   amended 2026-08-30 on claude-20's point that CML-D1 is a D1 that is also a build): runs the acceptance
   against the current artefact (log row 11 rule) and writes the expected value into the packet; a causal claim in a packet names the probe that
   established it or is marked "inferred, untested" (row 14).
5. After each packet closes, **bells claude-15** a six-line review-request: packet, sha(s), what was
   checked (commands + results), findings fixed, refusals received, ledger line. The owner's gate follows;
   nothing is "done" in the ledger until the owner's gate line is there.
6. Keeps **at most three build lanes in flight** plus CML — review bandwidth is the bound, not seats.
6b′. **A pre-dispatch read quotes the packet file's hash** (md5 before and after the read), and the dispatch ledger line records the hash of the file actually sent — a read that certifies a version nobody dispatches is the wrong-corpus miss (claude-13, 2026-08-30: its first pass returned rev 1's text and its second rev 2's, because the owner was editing the file mid-read).
6b. **Packet text is saved as a file** before sending — `futon2/holes/problems/BUILD-packets/<packet>.md`
   — and the ledger line points at it, because the job API does not expose prompts and the owner's gate
   must be able to read what the builder read. **Review of packet text:** discovery packets (D1, no code,
   refusal permitted, S1 quoted verbatim) go direct; **build packets (D2+) are belled to `claude-13` for a
   read before dispatch** — a whistle if a lane is waiting — and the ledger records who read it. (Decided by
   claude-15 2026-08-30 on claude-20's first report; the latency is paid where a packet can ship a facade.)
7. Never edits an edge schema, an S1 field, or the spine. A builder's schema proposal goes to the owner as a
   bell with both endpoints' proposals side by side.
7a. **Positive control on every negative probe** (2026-08-30, six silent probe failures across two reviewers in one afternoon): a probe returning 0/nothing is recorded only beside a query on the same instrument that returns something — a wrong key path looks exactly like an absence.
7b. **A builder's blocker can expire between its run and its review** (claude-20, R8-D3): re-run the blocked check at review time and record both states — taking "blocked" at face value costs a needless re-dispatch; ignoring it skips the gate.
8. If a bell exchange crosses, whistles to reconcile (one side only). If a job goes silent past its park
   deadline, checks `GET /api/alpha/invoke/jobs/<id>` and reports the state honestly.

## Interfaces are Lean declarations (Joe, 2026-08-30: "they need to be coordinating with each other around interfaces using these tetrahedral model ideas, not just working in parallel")

In the gasket, the Lean statement and the Clojure implementation of a term are two children of the same
node-tetrahedron, and their **contact point is the signature**. `mathlib4/DarkTower/WarMachine/Holes.lean`
(`P-lean-holes.md`) is the registry of those contact points; the owner (claude-15) holds it; the tech
lead proposes changes by bell; builders never edit it.

1. **A build packet (D2+) for a term quotes the term's Lean declaration** — its exact signature from
   `Holes.lean` — as the interface the Clojure must implement, and the record's `solved` names that
   declaration. If no declaration exists, the packet is not dispatched: the owner adds the hole first.
2. **Pre-dispatch check (row 11) gains a step:** the packet's signature equals the declaration's type
   (the `count-holes.sh` output lists them; compare mechanically, not by eye). **Two kinds of drift
   (claude-20, 2026-08-30):** *signature drift* — the `def … : … := ` text differs — blocks the packet;
   *commentary drift* — only the `/-- … -/` doc tag differs — is reported in the ledger line and does not
   block, because nothing a builder must implement has moved (a docstring edit mid-build would otherwise fail
   a lane over a comment). The evidence/falsifier fields live in the doc tag, so commentary drift is still
   read at the owner's gate: the findings note must match the current tag.
3a. **The axiom gate on every hole-moving `.lean` artefact (claude-20 + claude-13, 2026-08-30; replaces "no `sorryAx`"):**
   (i) the file **elaborates at the owner's gate, AT ITS SHA** — the owner runs `lake env lean` on `git show <sha>:<path>`, never on the working-tree path (in a shared checkout a path is not an artefact: a live lane's edits are visible at the path — claude-20, 2026-08-30, after the owner characterised R2-D3's half-finished edit as R2-D2's delivery); the ledger line records the sha elaborated; a report that does not
   elaborate has moved nothing (R2-D2's `decide` at 792 entries hit maximum recursion depth and was delivered
   anyway); (ii) **named theorems only** — an anonymous `example` cannot be `#print axioms`-ed; (iii) the file
   carries **`#print axioms` for each theorem**, and the bell and the ledger line **quote the axiom output**;
   (iv) any axiom beyond `propext` / `Classical.choice` / `Quot.sound` is **named with its reason** — `native_decide`
   is permitted (kernel reduction at fixture scale may be infeasible, and forcing `decide` would push a builder to
   shrink the corpus, the worse trade) but it is a *stated* trust in the compiler, and its generated per-proof
   axiom name (`…_native.native_decide.ax_…`) is invisible to a `sorryAx` grep. The check is "quote the axioms",
   not "grep for a known bad name".
3. **A lane closes only when the hole moves.** The Clojure check's run is the witness; the hole goes
   from `sorry` to a body, or to a stated theorem whose fixture is that run (the `CommitmentTemperature`
   finite style). "Implemented in Clojure, Lean unchanged" is an open lane, not a closed one; the
   holes count is re-run as the gate and reported in two lines.
4. **Refusal names the signature.** A builder who cannot implement the declared type says which clause
   of the type is wrong for the artefact; that goes to the owner as a signature proposal — never a
   silent narrowing in Clojure with the Lean left as-is.
5. **Sequencing, 2026-08-30:** the hold was in force from 15:18Z until LH-D1b passed review (~15:55Z); it is
   lifted. (One breach, the tech lead's, caused by bell latency: R2-D2 dispatched at 15:31Z, cancelled on
   reading the hold; codex-1's two uncommitted files kept as the start of the re-dispatch.)
6. **Bottleneck relief (claude-20's proposal, accepted 2026-08-30):** the tech lead runs the clause-2
   signature comparison itself and **proposes hole text by bell in the same message as the packet**; the
   owner's step is ratification (edit + commit of `Holes.lean`), not authoring. A refusal from claude-13 or a
   builder that names a signature clause travels the same way. The owner remains the only editor of the file;
   the tech lead's two findings on reading `Holes.lean` against the artefacts (R2 vacuous-or-refuted; R8
   encoding a retired clause) are what the coupling exists for.

## Non-negotiables (from the lifecycle, and from July)
- Refusal is a deliverable. A builder who cannot define a term on the theory's terms says so; the tech lead
  never substitutes a simpler type to keep a lane moving (a list is not a cascade; an action is not a
  policy; a count is not a channel).
- Numbers are reported as *what they are*: a recall that is tautological is labelled so; an absence names
  the search that produced it; "done" is dated against the instrument that produced it.
- Author ≠ reviewer, at every level: the tech lead does not review its own packet text; the owner does not
  ratify its own S1 (Joe does).
- Commit on explicit paths only; never push; never `load-file` a worktree into the shared JVM.

## Build ledger
`futon2/holes/problems/BUILD-ledger.md` — one line per packet:
`| lane | packet | seat | job-id | park-id | dispatched | state | tech-lead review (sha, checked) | owner gate |`
The tech lead appends at dispatch and at close; the owner appends the gate line.

## Reporting cadence
- Per packet close: the six-line bell above.
- Per session (or every ~2 h of activity): one bell with the ledger's state counts and the two-line
  organised fraction of the wiring from the control-map lint (specified / unspecified).
- Whistle only for a live conflict that blocks a lane.

**Lifecycle placement (added 2026-08-31):** this charter operates delivery-lifecycle units (`futon4/holes/delivery-lifecycle.md`) inside `M-formal-war-machine`'s §3 DERIVE, which runs under `futon4/holes/mission-lifecycle.md`. See the method bridge at the mission's §3 head.
