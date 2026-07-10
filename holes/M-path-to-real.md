# Path to Real — closing the loop on real `:capability/*` evidence

**Joint proposal:** claude-4 (write-side: actuator) + claude-5 (read-side: A4a/BMR). For Joe's
two arming decisions. **The whole A4a pipeline (BMR concepts → EIG → ranker) is inert for exactly
one reason: 0 real `:capability/*` edges.** Make them flow and it runs on real evidence.

## The path (one sequence, owners + gates)

| # | step | owner | gate |
|---|---|---|---|
| 1 | **`:eig-fn` convergence** — merge-safe ranker read (fixes the under-count) | claude-4 | none; runs ∥ write-flow, must beat the A1-read to merges |
| 2 | **`:clean` backfill** into `ft-learning-loop-010` (for build-match verify) | claude-4 | none; minutes |
| 3 | **scoped provisional-vocabulary extraction** → real `:capability/*` corpus (~10–15 capability-rich missions) | claude-4 | **GATE A — Joe arms (a real write)** |
| 4 | BMR consumes the corpus → **merges fire** | claude-5 | success criterion below |
| 5 | A3 proves M-learning-loop 1/2→2/2 · A4 records the discharge | claude-4 | (follows the write) |
| 6 | **A1 EIG-read on real evidence** → R13 discrimination | claude-4 | gated on (1) ∧ (4) |
| 7 | 3b γ-migration (substrate-dial → live γ) | claude-4 | **GATE B — Joe arms the live-wire (later)** |

`:eig-fn` (1) ∥ the corpus (3→4); only the A1-read (6) is gated on both. 3b (7) is orthogonal
(γ-variance) — making-it-real fills its *data* readiness for free, but its arm stays parked.

## The two arming decisions we're asking of Joe

- **GATE A (now-ish): arm the scoped extraction.** Green-light claude-4 to run a batch capability
  extraction over ~10–15 real missions and **write durable `:capability/*` edges to the LIVE
  store.** This is the first domino.
- **GATE B (later): arm the live-wire** (`*gamma-grounded-feed?*`) for 3b, once we've validated on
  real variance. Separate decision, separate time.

## Three honesty guards (baked in, so a provisional result isn't mistaken for the settled thing)

1. **Provisional-vocabulary label.** The first outputs are **real edges** but **provisional-vocabulary
   concepts** — only as real as the bootstrap vocabulary, which is itself the mission's open central
   bet. So step 6 is *"R13 discrimination on real-but-provisional evidence,"* **not** *"validated
   capability structure."* The vocabulary refinement is an explicit follow-on. Same trap we've
   guarded all session — named up front.
2. **Preregistered success criterion (falsifiable).** Success of steps 3→4 = **BMR fires ≥1 real
   merge** on the extracted corpus. If the scoped ~10–15 missions stay **inert** (no capability pair
   reaches ~20 co-occurrences), that is a *real finding* → scale the batch. Not "we wrote edges,
   therefore done." claude-5 reviews the merges on the real corpus, same reviewed-lane rigor as demo.
3. **The arm is a real write.** GATE A writes **durable `:capability/*` edges to the live store** —
   refinable, but a real substrate mutation, not a dry-run. Joe arms it knowing that.

## Status of readiness
- **read-path (claude-5):** ready — `a4a-substrate/read-corpus` queries `:capability/*`; BMR consumes
  real edges the moment they land. `eig-for-produces` reviewed-green (`64aefaf`).
- **write-path (claude-4):** steps 1–3, 5 are mine; 1–2 need no gate; 3 needs GATE A.
- **linchpin:** the corpus inhabits three separate ◐ boxes at once (R5-EIG, R12-accumulate,
  R13-signal) — one flight, three requirements advanced.
