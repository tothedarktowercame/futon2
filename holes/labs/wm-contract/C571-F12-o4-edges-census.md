# C571 — F12 O4-and-edges census

## 1. Bounded measurement

The checker reads the ten named cascade records and the three separately named
gate records at the bounded corpus declaration
`futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:8-22`.  Futon3 inputs are
read from pin `cdb5e8a56fd907beb6a99f8b88af9de50ff93126`, recorded in the artifact at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:27-29`.

No one cascade record carries both a comparable O4 precedence pair and a real
cascade edge; the aggregate measurement is at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:1-12`, and
the ten per-record results are at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:135-496`.
The three gate records are reported separately at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:30-42`.

## 2. The two halves do occur, separately

The ants record is the only cascade record with precedence before and after over
the same members (`futon3:checks/ants-cascade.edn:93-102`), while its repository
reports zero authored why/how edges (`futon3:checks/ants-cascade.edn:3`).  Its
two actual run edge counts are both zero in the artifact at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:205-214`.

Conversely, retrodiction carries a real cascade edge at
`futon3:checks/retrodiction-cascade.edn:6742-6744`, but its constructor records
O4 as not exercised at `futon3:checks/retrodiction-cascade.edn:34090`.  The
mining exemplar also carries two recorded edges at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:33-43`, but its
O4 block supplies no before/after precedence pair; its measured row is
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:477-496`.

## 3. The slice-1 disagreement and causes

Slice 1 called construct's O4 absent at
`futon2:holes/labs/wm-contract/runs/F12-organise/00-census.edn:96`.  The pinned
record actually has per-run refusal values at
`futon3:checks/construct-cascade.edn:187` and
`futon3:checks/construct-cascade.edn:266`; the disagreement is recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:14-26`.
The apparently similar snatch reading is not a disagreement: its O4 key was not
found, as recorded under run-record id `:snatch` at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:238-249`.

The zaif refusal is measured: the gate checks how many members carry a
play-grain rule and refuses a pair exchange below two members at
`futon3c/scripts/zaif_cascade_gate.clj:549-554`.  The other refusal families are
constructor-set without play: their emitter pointers and unconditional
classification are recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:43-55`.

## 4. What a recorded exemplar would need

The two record representations show the required conjunction: a real edge in a
run's cascade, shaped as at `futon3:checks/retrodiction-cascade.edn:6742-6744`,
and an O4 before/after precedence pair over the same members, shaped as at
`futon3:checks/ants-cascade.edn:93-102`.  This measured description, including
the measured-gate pointer, is stored at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:516-523`.

One limit on that description is recorded with it at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:524-532`:
the pair this census recognises is two maps over the same key set
(`futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:53-57`), which is the
ants shape, while the one gate that measures O4 rather than declaring it refused
emits the pair as two vectors
(`futon3c/scripts/zaif_cascade_gate.clj:555-558`).  A cascade exercised through
that gate would be read here as carrying no pair until the recognised shape is
widened.

## 5. Controls

The checker keeps actual cascade counts separate from the explicitly
counterfactual prior-relation counts; construct shows both representations at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:150-167`.
Eight mutations reverse an O4 reading, move a per-run O4 value, zero a nested
edge count, substitute a counterfactual count, drop a corpus record, shift a
pointer, zero a count-mode edge count, and flip which reading of the slice-1
disagreement is correct.  Every plant landed, every planted verdict is false,
and each one is refused by the validity predicate on its own -- the
`:caught-by-validity-predicate?` column at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:56-104`.
That column is reported separately because `:verdict` alone cannot carry the
plants: its second conjunct compares the planted report with the report the run
derived, which no mutation whatever can satisfy
(`futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:222-228`).  The predicate
refuses a plant on measured grounds instead: every recorded pointer is resolved
in its own source file and the cited line must still carry the key it is
recorded under (66 pointers,
`futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:122-134`), and every
recorded edge count is recomputed from the record's own bytes
(`futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:135-141`).
The unmutated verdict is true at
`futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:515`.
