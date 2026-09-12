# TN: Row 14 measured-A source discovery

Date: 2026-09-12  
Scope: source discovery and count extraction only. No estimator and no
production behavior change. Source census HEAD before this packet:
`7b5cde4a215a9944bfba4dbe0f65ced28d4c2771`.

Joe's ruling is applied literally: A is an `:observed-estimate` requiring a
pinned measurement artifact (`src/futon2/aif/machine_model.clj:71-99`). The
unit counted here is one entity in one of the seven categorical R1 statuses at
one full-loop close, paired with one of the twelve current organization
dispositions. No entity averaging, smoothing, or invented mass occurs.

## 1. Where candidate pairs live

### Full-loop cohort events: disposition and candidate entity

`full_loop_cohort/event-record` writes cohort id, attempt id, ordinal, sequence,
checkpoint type, and `recorded-at` around the supplied payload
(`src/futon2/aif/full_loop_cohort.clj:317-325`). `append-checkpoint!` validates
ordering and writes each event with CREATE_NEW (`:395-426`); `close-attempt!`
requires a grounded close and validates its outcome (`:428-460`). The close
payload carries `:outcome`, grounding flags, duration/resource use, and witness,
but **no entity and no status** (`src/futon2/aif/full_loop_runner.clj:2860-2900`).
The sibling selection event carries `:selected-action`; its `:target`, when
present, is the only candidate entity identity. `:target-class` is an action
class and is not renamed into an entity. The derived ledger itself retains only
selected-mission and outcome (`full_loop_cohort.clj:462-505`), so extraction
uses the immutable events rather than the summary.

On disk under `data/wm-full-loop`: 82 physical close files, 81 distinct file
contents (one byte-identical copy), spanning 2026-07-14T20:27:28Z through
2026-07-27T10:59:53Z. Outcomes are 22 `:no-selection`, 22 `:build-failed`, 19
`:grounded-change`, 9 `:agent-unavailable`, 7 `:incomplete`, and 2
`:substrate-unavailable`. Only 41/81 distinct closes have a selection
`:target`. Attempt identity is not globally sound in the archive: multiple
`[:cohort/id :attempt/id]` groups carry different times/outcomes; the extractor
retains them as distinct records and reports the collisions rather than
arbitrarily dropping one. The later writer explicitly documents why attempt ids
must be global (`full_loop_cohort.clj:302-315`), confirming the historical
collision is real evidence, not a deduplication key.

### WM trace: per-entity belief, but not a close

`trace/trace-record` writes a timestamp plus full `:mu-pre` and `:mu-post` maps
from the one judge evaluation (`src/futon2/aif/trace.clj:577-622`); these are
maps from individual entity id to a seven-coordinate probability row, not a
population average. On disk under `data/wm-trace`: 60 matching daily files and
897 readable timestamped records, all 897 carrying nonempty `:mu-post`, spanning
2026-05-18T19:42:49Z through 2026-09-12T17:28:09Z. A trace timestamp is tick
time, not attempt-close time, and the record does not carry cohort/attempt
identity for the July cohort closes. It therefore supplies a possible prior
belief row only through an explicit entity/time derivation.

### Tick-run records and judge evidence: no second join

The wrapper writer emits run id, start time, trace-written flag, and route; only
special terminal cases add a `:terminal` projection
(`src/futon2/aif/full_loop_runner.clj:333-440`). There are 236 readable
`tick-run-record-*.edn` files beneath `holes/labs/wm-contract`, spanning
2026-08-30T20:41:31Z–2026-09-12T18:34:22Z; 36 carry `:terminal`, none carries
`:mu-post`, and only two carry an attempt identity. They postdate the July close
corpus and cannot add a status-at-close pair. Judge results have belief in
memory and cohort close data in a separate runner path, but no durable record
retains both at the close boundary. Thus neither tick-run records nor judge
evidence supplies another measured pair today.

## 2. The join and its integrity

The only presently testable join is:

1. read each distinct immutable close and its sibling selection;
2. accept only an exact `selected-action :target` as `:entity/id`;
3. find trace records at or before `:recorded-at` whose `:mu-post` contains that
   exact entity key;
4. choose the unique latest timestamp; and
5. derive a categorical status as the unique argmax of that one entity's seven
   coordinates.

This is deliberately labelled
`:latest-prior-trace-containing-exact-entity-id` and
`:status-source :derived-unique-argmax-of-mu-post` in the retained count record.
The status is **not recorded categorical truth**. Argmax is a derivation from a
belief distribution and requires estimator review; this packet does not rule
that it is an admissible observation of state. Ties/malformed rows refuse the
join. Equal latest timestamps refuse as ambiguous. There is no maximum-age
rule to invent: the sole join is 232,539 ms (3m52.539s) before close, and that
age is retained for review.

Of 81 distinct closes, 40 have no entity id, 40 have an exact entity id that
appears in no prior trace belief, and exactly one reaches a unique argmax. The
one pair is entity `:sorry/pudding-g1-arrow-witness-binding`, derived status
`:strengthened`, disposition `:grounded-change`, close
2026-07-14T20:27:28.840534153Z, trace
2026-07-14T20:23:36.301882001Z. Its source file and SHA-256 are retained in
`runs/row-14-measured-a-sources-2026-09-12/counts.edn`.

No fallback joins selected-mission text to a differently typed entity, no
nearest entity is substituted, no later trace is used, and no belief rows are
averaged. Historical cohort/attempt collisions make that pair an insufficient
join key by itself; exact source artifact plus cohort, attempt, close timestamp,
entity, and trace timestamp is the minimum identity of this derived pair.

## 3. Actual 7×12 count census

Extraction source:
`holes/labs/wm-contract/row14_measured_a_census.clj`. Retained output:
`holes/labs/wm-contract/runs/row-14-measured-a-sources-2026-09-12/counts.edn`
(SHA-256 `2075b7b5defb96d45e8c23cef5f6d1220fe3edf28f3177e13cf4a8cb31a05fd4`).

The state support is `belief/status-set` (`belief.clj:37-43`). The disposition
support is `full_loop_cohort/outcome-kinds` (`full_loop_cohort.clj:26-40`) minus
the two named historical verification labels, yielding twelve.

Result: **1 total derived candidate pair, 1/84 nonzero cells, 83/84 zero
cells**. The only nonzero cell is:

| state | disposition | count |
|---|---|---:|
| strengthened | grounded-change | 1 |

Every other state/disposition cell is measured count zero in this source
census. That does **not** make a zero-data row a probability row, and it does
not establish that an unobserved transition has zero real probability. It says
only that this pinned corpus supplies no counted instance.

## 4. Retention gaps and follow-on row-14 packets

1. **Categorical status at close (highest priority).** Add to the immutable
   close judgment a versioned record containing exact `:entity/id`, the seven
   coordinate belief row actually in force, a separately named categorical
   status if the authority rules one, status method, trace/run identity, and
   measurement time. Refuse close-record attribution when entity/status
   identity is missing or mismatched. This removes temporal nearest-trace and
   argmax from the measurement authority without changing selection.
2. **Entity identity on every close.** Persist a typed
   `:outcome-entity {:status :present ...}` or an explicit absence/refusal on
   every outcome path, including no-selection and agent/substrate failures.
   Never turn `:target-class` into an entity. Negative controls are missing,
   ambiguous, and action-target/record-entity mismatch.
3. **Cross-ledger identity.** Carry the WM `:run/id`/trace record identity into
   the selection and close events and carry cohort/attempt identity into the
   associated trace. Refuse joins on historical reused attempt ids or multiple
   trace matches. This replaces temporal proximity with literal identity.
4. **Outcome-class retention coverage.** Continue additive capture across all
   twelve current dispositions. The present close corpus exercises only six;
   the absent six (`:abstained`, `:artifact-only`, `:cancelled`,
   `:dispatch-failed`, `:grounded-no-change`, `:guardrail-refusal`) need real
   close records, not synthetic mass. A coverage report should list zero-count
   cells and schedule retention opportunities; it must not induce failures in
   production merely to fill A.
5. **Status coverage.** The one join observes only `:strengthened`. Each close
   record must retain whatever single-entity categorical authority is ruled,
   so naturally occurring examples of the other six statuses accumulate.
   Sparse cells remain visible follow-on retention actions.

Each packet cites WORK-REMAINING row 14 and is additive capture only; the
estimator follows after independent review of the categorical-state authority.

## 5. Packet 1b estimator sketch

The reviewed estimator consumes a pinned count record, not live mutable
directories. For each state `s`, let `n[s,o]` be the twelve retained counts and
`N[s] = sum_o n[s,o]`.

- If `N[s] > 0`, emit `A[s,o] = n[s,o] / N[s]` as exact ratios. Measured zero
  remains exactly zero. No pseudocount, epsilon, or renormalization of rounded
  floats is permitted.
- If `N[s] = 0`, emit a typed absence for the whole state row, e.g.
  `{:status :absent :reason :zero-measured-state-support :state s :count 0}`.
  It is not a uniform distribution and cannot enter the machine model as a
  completed kernel row.
- Persist every cell count, row total, zero-cell list, join/exclusion counts,
  corpus date range, extractor identity, input-manifest digest, state/outcome
  supports, and derivation ruling. Changing any pin creates a new measurement
  revision; it does not overwrite the old estimate.

The artifact admitted by `machine_model/measurement!` must be a single EDN map
of the required shape (`machine_model.clj:71-99`), conceptually:

```clojure
{:schema :wm/kernel-measurement-v1
 :kernel/name "production-measured-A-v1"
 :outcome :measured
 :rows {<seven complete measured rows only after retention>}
 :counts {:cells {s {o n}} :row-totals {s N}}
 :source {:census-schema :wm/measured-a-source-census-v1
          :path ".../counts.edn" :sha256 "..."
          :input-manifest-sha256 "..."
          :date-range {...}}
 :derivation {:status-authority <reviewed ruling>
              :entity-policy :single-entity}}
```

The machine-model A entry then uses `:authority :observed-estimate` and a
`:measurement {:path ... :sha256 ...}` pointer. On today's 1-pair census six
state rows are typed absent, so no positive full A construction is licensed.
That is the sparse-data retention obligation Joe's proviso requires, not an
excuse to reuse the positional placeholder.
