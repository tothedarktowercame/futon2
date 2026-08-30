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
6b. **Packet text is saved as a file** before sending — `futon2/holes/problems/BUILD-packets/<packet>.md`
   — and the ledger line points at it, because the job API does not expose prompts and the owner's gate
   must be able to read what the builder read. **Review of packet text:** discovery packets (D1, no code,
   refusal permitted, S1 quoted verbatim) go direct; **build packets (D2+) are belled to `claude-13` for a
   read before dispatch** — a whistle if a lane is waiting — and the ledger records who read it. (Decided by
   claude-15 2026-08-30 on claude-20's first report; the latency is paid where a packet can ship a facade.)
7. Never edits an edge schema, an S1 field, or the spine. A builder's schema proposal goes to the owner as a
   bell with both endpoints' proposals side by side.
8. If a bell exchange crosses, whistles to reconcile (one side only). If a job goes silent past its park
   deadline, checks `GET /api/alpha/invoke/jobs/<id>` and reports the state honestly.

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
