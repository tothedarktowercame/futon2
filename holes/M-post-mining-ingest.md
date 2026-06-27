# M-post-mining-ingest — govern the post-mining material so it is neither forgotten nor muddled

**Status:** IDENTIFY ratified (Joe, 2026-06-26) → **MAP in progress.** (Lifecycle: `futon4/holes/mission-lifecycle.md`; MAP = survey/catalogue the artifacts + consumers, facts not design.)
**Owner:** Joe + claude-1 · **Spawned:** 2026-06-26, from [[M-points-de-fuite]]'s VERIFY — when we moved to *reconstruct the past from the mine*, we hit the question of which mine, applied how.

## HEAD (the one line)
The **mining run** (the State apparatus: M-operational-vocabulary + M-goals-and-holes) produced an **expensive, one-off, multi-artifact snapshot** that now fans out to many downstream consumers. **Govern it**: one **canonical, tracked** source with a **consumer contract**, so what we paid to learn is **consistently applied** — neither *neglected* (forgotten, left to rot) nor *muddled* (consumers reading divergent versions / applying it inconsistently).

## 1. IDENTIFY — shape-first

### The gap — stated as evidence · the two failure modes · shape
- **Evidence (the muddle is already real, not hypothetical):**
  - **Version sprawl.** On disk: `joint-memes.openai.json` *and* `.pre-instr-fix.json`, `.v2primed.json`; `c-entries.openai.json` *and* `.pre-instr-fix.json`, `.v3-early-fail.json`, `.salvaged.json`, `.stub.json`, `.verify-smoke.json`. **No marked canonical.**
  - **Divergent consumer reads (proven).** A grep over current consumers: most read `…openai.json`, but some read **`c-entries.openai.pre-instr-fix.json`** and **`c-entries.salvaged.json`**. So *the same downstream question gets different answers depending on which script you ask.*
  - **Wide fan-out.** The mine feeds: the embeddings comb, the **move-basins recogniser** (the reproducible ▶◀✎ tags), the **session-overview** mined overlay, the **C-vector / belly**, the per-turn **sigils**, and (proposed) the **historical reconstruction** ([[M-points-de-fuite]] "mine the past"). Each is a consumer with no shared contract.
- **The two failure modes (Joe, 2026-06-26):**
  1. **Neglect** — we *forget what is in the mine* and leave it to decay; the paid investment (and the structure it recovered) is never consistently ingested, so it rots unused.
  2. **Muddle** — inconsistent downstream application; consumers diverge (above), so "what we learned" is applied incoherently and results contradict.
- **Shape (the hypothesis):** a **post-mining registry + consumer contract** — (i) a **canonical pin** (which artifact, which version, with provenance/checksum) for each mined product; (ii) ideally **ingest the canonical mine into substrate-2** so consumers read **one source**, not scattered JSON; (iii) a **consumer manifest** (who reads what, applied how) with a **consistency check** that fails when a consumer drifts off-canonical.

### The tension
The mine is a **one-off paid snapshot** (re-running is expensive — the State apparatus). That very fact makes governance *more* urgent than for regenerable data: you cannot casually re-derive it, so a forgotten or muddled mine is a **lost, costly asset**. Yet today it lives as a heap of sibling JSON files with no canonical marker and no record of who consumes which.

### Scope
- **In:** canonicalize the mined products; track them (provenance, version, checksum); a consumer contract + consistency check; (candidate) ingest the canonical mine into substrate-2 as the single read-source.
- **Out:** **re-mining** (that is M-operational-vocabulary / M-goals-and-holes — the *production* side); the **in-flow weaving** ([[M-points-de-fuite]] — the *declare-the-future* sibling). This mission is the **consumption/governance** layer between them.

### Sibling instances (where this sits)
- **M-operational-vocabulary · M-goals-and-holes** — *produced* the mine (forward memes · backward C-entries). This mission makes their output **durably usable**.
- **[[M-points-de-fuite]]** — *declare the future* (real-time, verified basic-pass on claude-11). This mission underwrites its **"mine the past"** half: a historical reconstruction is only trustworthy if it reads a **canonical, contracted** mine.
- **`mining_recogniser` → `move-basins.json`** — already the right pattern in miniature (distil the mine into a reproducible artifact); generalize that discipline to *all* mined products.

### IDENTIFY exit (for Joe)
**The gap is named:** the paid, one-off mine fans out to many consumers with **no canonical pin and proven divergent reads** — risking *neglect* (rot) or *muddle* (inconsistent application). **The shape** is a registry + consumer contract (+ candidate substrate-2 ingest) so the mine is one canonical, tracked, consistently-applied source. **✓ Ratified by Joe (2026-06-26) → MAP.**

## 2. MAP — the artifact catalogue + consumer manifest (claude-1, 2026-06-26)

MAP's object: **survey what the mine actually contains and who actually reads it** — facts, so DERIVE can pin a canonical + write a contract on solid ground. (Audit run over `futon6/data/{meme-mine,c-vector}` + a grep of all consumers across the repos.)

### A. The mined products — ~25 files in two dirs, four kinds
| kind | files | note |
|---|---|---|
| **canonical (the live products)** | `meme-mine/joint-memes.openai.json` · `resolved-memes.openai.json` · `concept-index.json` · `action-cert.json` · `c-vector/c-entries.openai.json` · `move-basins.json` | the unsuffixed `.openai.json` is canonical **by convention only — no marker** |
| **vocab / derived** | `c-vector/spot-vocab.json` · `typology.json` | distilled lexicons (move-basins also belongs here — the reproducible recogniser) |
| **golden / test fixtures** | `golden-backward.json` · `golden-reach-candidates.json` · `*.stub.json` · `c-entries.verify-smoke.json` · `meme-mine/sample.json` | regression/smoke inputs — must NOT be read as data |
| **stale / superseded versions** | `*.pre-instr-fix.json` · `c-entries.openai.v3-early-fail.json` · `*.v2primed.json` · early `memes.json` · `resolved-memes.json` | experiment/repair lineage; safe to read only by the salvage path |

> **MAP finding — mtime is a trap.** `joint-memes.openai.v2primed.json` (06-25 22:54) is **newer** than the canonical `joint-memes.openai.json` (06-25 22:52). A consumer that picks "the newest" gets the *wrong* file. Canonical cannot be inferred from the filesystem today.

### B. The consumer manifest — who reads what
| consumer | reads | canonical? |
|---|---|---|
| `session_scope_view.py` (live overview) | `c-entries.openai.json`, `joint-memes.openai.json`, `move-basins.json` | ✓ |
| `mining_recogniser.py` (builds move-basins) | `c-entries.openai.json`, `joint-memes.openai.json`, `move-basins.json` | ✓ |
| `session_typology.py` | `c-entries.openai.json`, `joint-memes.openai.json`, `spot-vocab.json`, `typology.json` | ✓ |
| `check_meme_mine_gates.py` · `check_goals_holes_gates.py` (gates) | `c-entries.openai.json`, `joint-memes.openai.json` | ✓ |
| `promote_to_proof_layer.bb` | `resolved-memes.openai.json` | ✓ |
| `session-mode.el` | `spot-vocab.json`, `typology.json` | ✓ (vocab) |
| `salvage_c_entries.py` | `c-entries.openai.pre-instr-fix.json` → `c-entries.salvaged.json` | — *legitimate*: the repair path, reads the broken one ON PURPOSE |
| **`cas_checks.py` · `clean_box_typing.py` · `c_vector.bb` · `hotspot-dashboard.py`** | **non-canonical** (`pre-instr-fix` / `v2primed` / `stub` / `verify-smoke`) | **✗ the muddle** — divergent reads of stale/test data as if canonical |

### C. MAP findings (facts for DERIVE)
1. **There IS a de-facto canonical** (`*.openai.json`), honoured by the core live consumers — but it is **convention, not contract**: nothing marks it, nothing stops drift.
2. **The muddle is a small, identifiable set** (4 scripts) reading stale/test versions — not pervasive, but exactly the inconsistency the mission names; the **salvage** path must be distinguished from a **bug**.
3. **Three product classes need different governance:** *data* (memes/c-entries — pin + checksum), *derived vocab* (move-basins/spot-vocab/typology — regenerable from canonical data, so pin the recipe not the file), *fixtures* (golden/stub — never read as data; a contract should make that a hard error).
4. **No provenance/version metadata travels with any file** — session coverage, mine date, model, gate-pass status all live only in the filename suffix and our memory. (`mining_recogniser → move-basins` is the one example of the right discipline: a derived artifact regenerable from canonical inputs.)
5. **Substrate-2 ingest is the candidate single-source** (DERIVE question): would consumers reading one substrate query beat a canonical-pinned file? — defer to DERIVE.

**← MAP cataloguing the SHAPE of the problem is done (artifacts + consumers + the divergence set). Remaining MAP-if-wanted: confirm the `.openai.json` lineage is the gate-passed one, and whether the 4 muddle-consumers are live or dead code. Then → DERIVE (pin a canonical + the consumer contract + the consistency check).**
