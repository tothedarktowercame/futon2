# C491 — C_mis v1: the criteria→C ingest and the clocked-mission risk binding

Row U11 (`holes/labs/wm-contract/worklist.edn:675`), DESIGN-c-vector.md §2–§3.
Built by claude-cli (wm-edge loop, any-lane), 2026-09-02. Registry untouched
(that is U13); nothing here is a ruling.

## 1. What was built

`src/futon2/aif/mission_c.clj` (new, 376 lines) and its binding at
`scripts/futon2/report/war_machine.clj:85-98` (the flag) and `:1526-1621`, behind `FUTON_WM_MISSION_C`,
default OFF.

- **(a) the criteria reader.** `read-criteria` dispatches on extension:
  `.edn` → an IDENTIFY ingest's `:preferences/c`
  (`mission_c.clj:141-171`), anything else → a mission doc
  (`mission_c.clj:223-244`; `read-criteria` at `:246-264`). Every row carries a `file:line` pointer.
- **(b) C_mis.** `c-mis` (`mission_c.clj:270-315`) builds
  `{observable → (pref/c-distribution spec)}` with the SAME constructor
  `futon2.aif.preferences/c-distribution`, and `log-c-mis`
  (`mission_c.clj:317-336`) is the weighted log-sum Σ_k w_k·ln C_k(o_k).
  Uniform weights default.
- **(c) the unmeasurable refusal.** Every read returns `:unmeasurable`, a
  vector of `{:criterion k :status :unmeasurable :reason … :source file:line}`.
- **(d) the binding.** `mission-c-readback`
  (`war_machine.clj:1557-1613`) reads the clocked mission's criteria, builds
  C_mis, scores risk_mis under the v0 status-quo forward model, and records it
  per mission action. `carry-mission-c` (`war_machine.clj:1615-1621`) attaches
  it beside `carry-active-mission`; `trace.clj:604-610` persists it
  present-only; trace schema 22 → 23 (`trace.clj:184-267`).

## 2. THE FIRST RESULT, and it is a null one: 0 of 9 criteria are measurable

Both fixtures U11 names were read, and on both, C_mis has **no factors at all**.

| fixture | shape | criteria | measurable | reason |
|---|---|---|---|---|
| `holes/labs/zaif-harness/runs/S4-identify-ingest.edn` | `:ingest-edn` | 3 | **0** | `:unresolved-observable` |
| `futon5a/holes/missions/M-expressions-of-interest.md:172-189` | `:markdown-numbered-list` | 6 | **0** | `:no-declared-measurement` |

The two reasons are different and the difference is the finding.

**MEASURABILITY IS TWO TESTS, NOT ONE.** Design §2 states one: "a criterion
with no `:measurable-by` contributes NOTHING silently". Applying it to the
exemplar the design itself names produces a wrong answer, because §2 says the
exemplar "types three criteria, each with `:measurable-by`" and **it does
not**: `:measurable-by` is on the three `:gap/r8` MISMATCH rows
(`S4-identify-ingest.edn:17`, `:21`, `:25`), while the three `:preferences/c`
CRITERIA rows carry `:carrier` (`:30`, `:31`, `:32`). Under the design's literal
test the zaif criteria would report as undeclared, which is not true of them —
they say how they are measured. So the reader accepts `:observable`,
`:measurable-by` and `:carrier` (`mission_c.clj:66-77`) and separates:

- `:no-declared-measurement` — the criterion does not say how it is measured.
  All six EoI criteria. The repair belongs to whoever writes IDENTIFY.
- `:unresolved-observable` — it DOES say, in PROSE ("zaif-harness +
  wm-contract worklists", "U8's gate test", "aif-equations.edn + U6 artifact"),
  which names a measurement for a human and no key this machine can read a
  current value of. All three zaif criteria. The repair is a declared
  `:observable <keyword>` resolving into a supplied vocabulary.
- `:undeclared-observable` — a keyword outside the supplied vocabulary.

**NO PROSE IS MATCHED TO A CHANNEL, ON PURPOSE.** A resolver that guessed
`"U8's gate test"` onto an observation channel would be inventing the join the
mission never declared. The record says the chain breaks at the declaration,
which is where it breaks.

**WHAT THIS MEANS FOR U12.** Its clause (b) — discrimination with named
numbers — has nothing to measure on either fixture as they stand today: not
because risk_mis fails to discriminate but because it is a typed absence. U12
must either plant an `:observable` on a criterion (as the test ns does) or
report that the corpus cannot answer (b). The extractor U12 builds first should
not be written expecting numbers from these two missions.

## 3. Live path, replayed over the three 2026-09-02 records

Replay only — `mission-c-readback` called on each record's `:active-mission`,
`:ranked-actions` and `:observation`. No live tick, no run lock, nothing
written under `data/`.

| run | clocked mission | source | criteria | measurable | status |
|---|---|---|---|---|---|
| `0a18c4f7` | — | — | — | — | `:absent :no-active-clock` |
| `4abad68c` | M-wm-aif-policy-grain-compliance | its mission doc | 0 | 0 | `:absent :no-measurable-criteria`, `:criteria-reason :no-completion-criteria-section` |
| `801976e7` | M-zaif-harness-v1 | the declared S4 ingest | 3 | 0 | `:absent :no-measurable-criteria`, 3 typed records |

Both clocked ticks: 133 mission actions, 13 non-mission. `0a18c4f7` carries the
focus read's OWN typed absence (`:no-active-clock`) rather than a generic one —
five distinct absence reasons are kept apart (`war_machine.clj:1573-1575`, `:1605`, `:1612-1613`)
because they want five different repairs, and a flat `0.0` for any of them
would read as *this mission's criteria are met*.

A third source shape was found and read while doing this:
`M-zaif-harness-v1.md:76` states its completion criteria as a bold inline
paragraph, not a heading plus list. Both shapes are in the live corpus, so both
are read and the record says which one answered (`:criteria-shape`). Its three
clauses agree in count with the hand ingest's three criteria — a cross-check
neither source could give alone.

## 4. Three choices that are not obvious, stated rather than left in the code

**(i) SURPRISAL, NOT A KL DRESSED UP AS ONE.** v0's Q(o|π) is a point mass at
the current measured value, so the per-criterion term is −ln C_k(o_k). For a
**Bernoulli** C that IS `pref/kl` exactly, and the test pins it numerically
against `pref/kl` rather than asserting it
(`mission_c_test.clj:172-180`, agreement < 1e-7). For a **range** C the point
mass has no density and KL is undefined; the term is the cross-entropy with the
point mass's divergent differential entropy dropped. Said in the docstring
(`mission_c.clj:338-353`) instead of hidden behind a σ² small enough to look
like a KL.

**(ii) UNIFORM WEIGHTS ARE OVER THE MEASURABLE SET, NOT OVER ALL CRITERIA.**
An unmeasurable criterion contributes nothing — including nothing to the
denominator. Uniform-over-all would let an unmeasurable criterion silently
shrink a measurable one's weight, which is the massless-contribution the design
forbids wearing different clothes. Recorded as
`:weight-basis :uniform-over-measurable`. A declared `:criterion-weights` that
does not cover every measurable criterion **throws** rather than filling gaps.

**(iii) POSITIONAL CRITERION IDS FROM PROSE.** A mission doc's numbered list
does not NAME its criteria, so the reader emits `:criterion-1 … :criterion-6`
(`mission_c.clj:173-180`). A slug derived from the prose would claim a name the
mission never gave it. The ingest's own `:criterion` keys ARE used, because
that file names them.

## 5. It moves no selection — structurally, not by promise

`mission-c-fields` is bound after every ranking and selection binding is final
(`war_machine.clj:5788-5799`) and attached by `carry-mission-c` outside the
judgement map. Nothing between the binding and `result0` reads it; no ranking,
weight, temperature, admissibility check or selector can. The flag-off
judgement is the same map key for key (`war_machine_test.clj` U11 block) and
the flag-off trace record differs from the flag-on one in exactly `:mission-c`
(`trace_test.clj`, `:timestamp` aside, which `trace-record` stamps per call).

Schema bumped 22 → 23 under the ledger's own rule (any key-set change bumps),
and for 22's stated reason: absence of `:mission-c` must be readable as *this
producer predates C_mis* versus *the flag was off on this tick*, and only the
version separates them.

## 6. The forward-model hole, and what it costs today

Q(o_k|π) at mission grain does not exist (design §3, not filled here). v0
scores criterion distance against the status quo, which is the same value for
every candidate. **risk_mis is therefore CONSTANT across the mission actions of
one tick.** That is on the record, not left to be re-derived:
`:action-sensitivity {:distinct-risk-values n :constant? bool}`
(`war_machine.clj:1608-1611`). The consequence for §3's claim that C_mis
"pulls G toward actions that address" unmet criteria: under v0 it cannot —
risk is a cost added uniformly, so it can only separate mission from
non-mission actions, never one mission action from another. U12 measures
whether that is enough; §7's revert condition is the honest reading if it is
not.

## 7. Reported, not repaired

- Design §2's sentence about the exemplar ("three criteria, each with
  `:measurable-by`") is wrong about that file, as §2 above establishes by line
  pointer. Not edited — DESIGN-c-vector.md is a record of a Joe-approved
  direction, and U13 is the row that records the design in the registries.
- `M-wm-aif-policy-grain-compliance.md` has no completion-criteria section at
  all, so the mission the machine was clocked onto in run `4abad68c` has no
  C_mis and cannot get one. Naming it here rather than opening a row for it.

## 8. Gates

clj-kondo 0 errors / 0 warnings on all six changed files;
`futon4/dev/check-parens.sh` OK on all six;
`clojure -M:test -m cognitect.test-runner -d test/futon2` → **1095 tests /
7046 assertions, 0 failures / 0 errors**, against I6's recorded baseline of
1073/6899 on a clean tree plus I6's own 3 tests (1076/6920) — the delta is
exactly this row's 19 deftests. Replay and tests only: no live run, no run
lock taken, nothing written under `data/`. Figure not regenerated (TN §9a).
Registries untouched — `:covers-key :none`.
