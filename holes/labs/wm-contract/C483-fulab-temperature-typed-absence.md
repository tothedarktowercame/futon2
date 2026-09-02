# C483 — Fulab temperature: typed absence, and a refusal to sample without it

Worklist row `:AC7` (class `:I`). Decided by Joe's 2026-09-02 ruling on the
seven C130 migrations (`holes/problems/DECISIONS-PENDING.md`, futon2 `2f34c26`);
for this site the ruling reads "fulab: refuse without error". Census site
`src/futon2/aif/adapters/fulab.clj:81`, tally instance `:fulab-error`.

## The site

`compute-tau` read its second input as

```clojure
(double (or (:outcome-size-surplus context) 0.0))
```

so a context that never measured outcome size entered the temperature
computation as one that measured **no** surplus.

The substituted zero was not neutral. Surplus sits in the *denominator*:

```
tau = clamp(tau/scale / max(1e-6, uncertainty + surplus), tau/min, tau/max)
```

so substituting `0.0` yields the **highest** temperature the uncertainty term
alone permits — the flattest softmax over `-G/tau`, the most exploratory
sample. An unmeasured context was therefore not merely mislabelled; it was
sampled at maximum exploration and reported a `:tau`, `:logits` and `:probs`
computed off a number nobody observed.

## What C226 established, and what this row rests on

Two findings from `C226-fulab-prediction-error-call-path.md` (futon2 `b91f706`)
bound the change:

1. The quantity at this seam is a **nonnegative outcome-size surplus**
   `max(0, text-score(outcome) - 1)`, **not** canonical signed prediction error
   `o - μ`. The key was renamed accordingly at `13ed674`; the census row still
   reads `:input :prediction-error` because it records the site as catalogued.
2. **No call site in `src/`, `test/` or `checks/` constructs this adapter** or
   calls `fulab/new-adapter`. The seam is dormant. That is why AC7 types it
   rather than wiring it: a later connection must be unable either to fabricate
   a temperature or to clamp a signed ε to zero on its way in.

## The change

`temperature-record` (`src/futon2/aif/adapters/fulab.clj:128-188`) classifies
the temperature input as one of three typed records, stamped
`:producer-contract :fulab-temperature/v1`:

- **`:present`** — a surplus was supplied, carried as `:value`, with `:basis`
  naming which producer measured it: `:outcome-size-surplus` when the caller
  supplied it, `:computed-outcome-size` when the generic `update-beliefs`
  branch measured it off the observed outcome. **A supplied `0.0` lands here** —
  an outcome of minimum size is a reading, not an absence.
- **`:absent`** — no surplus was supplied. **No `:value` key**; `:absent` names
  the field and whether the key was present at all, so a nil-valued key and a
  missing key stay apart.
- **`:refused`** — three malformations, each named rather than clamped:
  - `:canonical-signed-error-at-surplus-seam` — the context carries
    `:prediction-error`. Checked **first**, so a caller that also supplied a
    surplus is still told which of the two quantities it is confused about.
  - `:malformed-outcome-size-surplus` — a non-number, `NaN`, or an infinity.
  - `:signed-value-at-surplus-seam` — a **negative** surplus, i.e. a value with
    the shape of signed ε arriving under the surplus name.

The last two were `throw`s before this row. They are records now for the reason
the ruling gives: every refusal has to be **persistable**, so AC8's harvester
can turn it into a proposed work item. A refusal that returns no sample is not
quieter than an exception — `select-pattern` chooses nothing either way.

`tau-from-record` (`:201-213`) returns a temperature only from a `:present`
record and `nil` otherwise. The arithmetic inside is unchanged.

## When the temperature is required, precisely

**Exactly when `select-pattern` would sample**: the caller named no `:chosen`
and there is a candidate set to sample from. Then a record that is not
`:present` **refuses** (`:262-321`) — `:chosen` is `nil`, `:aif :refused?` is
`true`, and the result carries **no `:tau`, `:logits` or `:probs`**, all three
of which the pre-AC7 code reported off the substituted zero.

Everywhere else tau is a diagnostic nobody branches on, so the derived value is
**omitted** and nothing is blocked:

- a caller-supplied `:chosen` stands, and an empty candidate set still returns
  `nil` as it always did;
- the pattern-action branch of `update-beliefs` (`:326-357`) still updates its
  evidence counts — that is the belief change at that site — and simply omits
  `:tau-updated`. This is AC2's split applied here: reject the member, not the
  collection;
- the generic branch (`:358-374`) is its own producer: it measures the surplus
  off the observed outcome, so its record is `:present` unless the caller also
  passed `:prediction-error`, which refuses whatever was computed.

The record reports the absence either way, carrying `:required?`.

## Persistence

Every result carries `:aif :temperature-events`
(`temperature-events`, `:190-199`): the **present-only** projection, empty
exactly when the surplus was read. That is the AC1–AC6 discipline — an empty
vector means the input was observed, a different claim from "the adapter did
not report".

There is no further carrier, and that is a fact about the site rather than an
omission: C226 found no in-repository caller, so there is nothing between this
adapter and `trace.clj` to project the records through. When a caller is
connected, `:temperature-events` is the key it reads.

## Measured, both directions

Against the pre-AC7 namespace loaded into the same process
(`git show HEAD:…/fulab.clj` renamed to `pre-ac7.fulab`), on
`{:decision/id :d0 :candidates [:a :b]}` with no surplus supplied:

| | `:chosen` | `:tau` | `:probs` | `sampled?` |
|---|---|---|---|---|
| pre-AC7 | `:a` | `1.0` | `{:a 0.5 :b 0.5}` | `true` |
| post-AC7 | `nil` | *(no key)* | *(no key)* | `nil` |

post-AC7 emits
`{:producer-contract :fulab-temperature/v1 :status :absent :reason
:outcome-size-surplus-not-supplied :absent [{:field :outcome-size-surplus
:key-present? false}] :required? true}`.

**On the healthy paths nothing moved.** Four contexts — surplus `1.0`; surplus
`0.0`; surplus `0.0` with a caller-supplied `:chosen`; an empty candidate set —
give `:aif` maps **equal** to pre-AC7 once the two added keys (`:refused?`,
`:temperature-events`) are removed. The generic `update-beliefs` branch is
equal likewise (surplus `2.0`, tau `1/3`). One expression was reverted to its
original form during this row for exactly that reason: rewriting
`(and sampled …)` as `(and (some? sampled) …)` changed a reported `:sampled?`
from `nil` to `false` on three of the four, which is a difference this row has
no business making.

## Gates

- `clj-kondo` on the two changed files: **0 errors, 0 warnings**.
- `futon4/dev/check-parens.el` on both: **OK**.
- `clojure -M:test -m cognitect.test-runner -d test/futon2`: **1025 tests,
  6381 assertions, 0 failures, 0 errors** (was 1017/6312 at AC6: +8 deftests,
  +69 assertions, all in `test/futon2/aif/fulab_adapter_test.clj`).
- `bb checks/preemptive_absence_coercion_lint.clj`: **PASS, findings 0** (was
  one `:absence-coerced` finding for this site, the last `:blocked` row in
  `checks/absence-coercion-dispositions.edn`); `--negative` still rejects its
  planted mutation.

## Planted cases

`test/futon2/aif/fulab_adapter_test.clj` — absent surplus refuses to sample and
reports the record; a nil-valued key is absence and says the key was there; a
measured `0.0` samples and reports the pre-AC7 tau; a supplied `1.0` preserves
tau `0.5`; the temperature is not required with a caller-supplied `:chosen` or
an empty candidate set; a negative surplus refuses and names itself; `NaN`,
`∞` and a string each refuse as `:malformed-outcome-size-surplus`;
`:prediction-error` refuses at `select-pattern`, is named even when a
well-formed surplus was also supplied, and refuses the generic branch's own
measured surplus; the pattern-action branch keeps its counts with the surplus
unread; `temperature-events` is present-only; the two bases are told apart.

## Registries

- `checks/absence-coercion-dispositions.edn`: the fulab row moves
  `:blocked` → `:fix-now` with a `:control` and `:implemented-by :AC7`;
  `:summary` becomes `{:fix-now 16 :exempt-with-reason 1 :blocked 0 :retired 1}`.
  It was the last `:blocked` row, so the absence lint now has no findings at all.
- `p4ng/empirics-futon/defect-repair-tally.edn`: `:fulab-error`
  `:open` → `:repaired`.
- `holes/labs/wm-contract/C12-absence-census.edn` is **not** edited: it records
  the population as catalogued, and AC1–AC6 left it alone for the same reason.
- No ruling is written by this row. `DECISIONS-PENDING.md` keeps its "Fulab
  temperature without prediction error" entry as the question the 2026-09-02
  ruling answered; the answer lives in the ruling section, not in a new entry.

## One apparatus repair, found by running the gate

`bash p4ng/empirics-futon/negative_controls.sh` was **already red at HEAD**,
before this row was taken: `pointer_check.bb` reported
`UNRESOLVED fulab.clj:81 (file not found)` twice. Both pointers come from the
`:AC7` row's own `:statement` and `:acceptance` in `worklist.edn`, and both are
**valid** — `src/futon2/aif/adapters/fulab.clj` exists and has 378 lines. The
checker could not resolve them because its `roots` allowlist has no
`src/futon2/aif/adapters/` entry.

That is the third instance of the defect class the script's own comments
record: C472 slice (b) appended four missing roots, C473 slice (c) appended the
three test roots, each after a valid pointer reported "file not found" in the
one direction that looks like the ledger's fault. The root added here
(`p4ng/empirics-futon/pointer_check.bb`) is appended, not inserted, so no
existing first-match resolution changes. After it: **613 pointers in 3 files, 0
unresolved**; `negative_controls.sh` **PASS (16 negative, 10 positive)**.

The recurrence is worth naming rather than just fixing: a hand-maintained
directory allowlist makes every pointer into a not-yet-cited directory a false
negative waiting to happen, and it fails closed against the registry rather
than against itself. Whether to resolve pointers by search instead of by
allowlist is not decided here.
