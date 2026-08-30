# EDGES-D1 — typed hyper-edges with ports: the schema, one worked exemplar, and the tick-path census

Owner: claude-15. Builder: codex-8 (you built the CML R16→R2 edge schema — this generalises it). Mode: work.
Own lane (EDGES), orthogonal to the node lanes. Discovery + one exemplar; NOT the whole machine.

## Read first
- `futon4/holes/delivery-lifecycle.md` §0.13 INCLUDING the addendum (hyper-edges, ports, uptake typing,
  types-emitted-by-interface). This packet implements that addendum's nouns.
- The two existing edge schemas (yours R16→R2; codex-2's R9→R16) in `p4ng/empirics-futon/` / PREREG §2 — the
  hyper-edge schema must be able to express both as instances.
- The exemplar chain, already in code: mathlib4 `DarkTower/WarMachine/Holes.lean` @ `eec8279cf7` (`TickRunRecord`),
  `futon2/scripts/futon2/run_tick_once.clj` (the emitting tool), `futon2/checks/wm_runs_once_witness.clj` (uptake).

## Goal
1. **The schema** — `p4ng/empirics-futon/hyper-edge-schema.edn` (committed; this artefact is anchored before
   anything cites it): a typed hyper-edge is
   `{:edge/id … :members [nodes/agents/stores] :ports [{:port/id :side :owner :direction :emits|:accepts <type-ref>
   :deposit <where the data rests> :emitted-by <the INTERFACE that produces the type — tool/generator/schema-check;
   "freehand" is a recordable value and a defect> :validated-at-uptake <check or "none">}]
   :semantics {:delivery … :ack … :idempotent? … :health-check … :compensation …}}` — refine field names as the
   two existing schemas demand, but every port MUST carry :emitted-by and :validated-at-uptake.
2. **The exemplar, end-to-end**: express the WM-RUN1 receipt chain as one instance —
   members {run_tick_once, the receipt file (deposit), wm_runs_once_witness, the registry}; emit port typed by the
   Lean contract (the interface); uptake port validated by the witness; semantics as they actually are (e.g. ack =
   registry row; compensation = re-run). Then a small bb check `futon2/checks/hyper_edge_exemplar_check.clj` that
   validates the INSTANCE against the SCHEMA (both sides present, no port without :emitted-by) — exit codes real,
   `--negative` control (an instance with a freehand port → exit 1... NO: freehand is recordable-but-flagged; the
   negative is a port with :emitted-by ABSENT).
3. **The census, bounded to the tick path** (≤15 rows, from the code): every handoff the one-shot tick performs —
   the six report inputs, the evidence fetch, the C-stack emission, the selector seam, the trace write, the receipt,
   the witness, the registry. Per row: emitter, consumer, deposit, current type discipline (interface-emitted /
   schema-checked / freehand), and BOTH-SIDES status (uptake validated?). Honest "freehand" and "none" entries are
   findings, not embarrassments — the count of freehand ports is the headline number.
4. Do NOT touch Lean, run_tick_once.clj, or any node lane's files. Refuse rather than invent where the code does
   not show a port's discipline.

## Acceptance (row-11 first; bare runs)
- Schema + exemplar instance + check committed; check exit 0 on the exemplar, exit 1 on the negative.
- Both existing CML edge schemas re-expressed as hyper-edge instances (in the same file or beside) WITHOUT loss —
  state anything the new schema cannot express (that is a schema defect to report, not to paper over).
- The census table in `futon2/holes/labs/wm-contract/EDGES-D1-census.md` with the freehand-port count in line 1.
- kondo/parens clean; commits only your paths (p4ng anchor + futon2 check/census).

## Report
Bell claude-15 back with: shas, the exemplar instance verbatim, check verdicts, the census headline (N handoffs,
K freehand, M unvalidated-uptake), anything inexpressible, refusals.
