# WM-CAST-D — how a flight gives the tick its cast (read-only)

Read at futon2 `a300cb7a` and futon3c `90719fde`; the four futon2 files cited are clean in the tree.
Read-only: no code, nothing under `data/`, nothing loaded, no click, no flight. Two live reads that are
not loads and are named where they are used: `GET /api/alpha/agents` once, and `/proc/392550/environ`
for the JVM serving :7070.

**Question 1, in one line.** The click endpoint *accepts* a cast and `config` *fills* one from the
environment, but the flight sends neither and the serving JVM has neither set — so no flight can pass
`:agent-readiness` today, which by the standing rule is a wiring defect.

## 1. The path of a cast, and where the flight leaves it

| step | site | what happens to the cast |
|---|---|---|
| the JSON body | futon3c `transport/http.clj:8932-8940` (handler `:8921`, route `:9296`) | `legacy-opts` takes `:author`, `:reviewer` and `:repair-reviewer` from same-named payload keys, each gated on `nonblank-string?`. The flight arrives separately, `:8955-8960`, as `:flight-edn` parsed to `:flight` and validated only as a map with `:target` and `:wants` |
| the runner service | futon3c `wm/runner_service.clj:153-162` | `configured-runner-opts` binds the cohort and hands the opts to `futon2.aif.full-loop-runner/config` |
| the merge | futon2 `full_loop_runner.clj:266-273` | `(merge {defaults} opts)`, so a caller's cast wins; the defaults are `FUTON_WM_AUTHOR_AGENT`, `FUTON_WM_REVIEWER_AGENT`, `FUTON_WM_REPAIR_REVIEWER_AGENT`, each `System/getenv` and so **nil when unset** — `:author` is then present-with-nil, not absent |
| the check | `full_loop_runner.clj:4803-4807` | after selection, any nil among `[:author :reviewer]` throws "Selected an action, but no cast was given for it" with `:failure-detail {:absent :no-cast-given :roles …}` |

`393660c7` removed the literals `zai-5` / `codex-7` / `codex-1` and says so in place: "A cast is now the
caller's or the env's; one not given is absent."

**The flight uses neither channel.** `http-click-fn`'s POST body is four keys and no cast —
`flight_runner.clj:448-449`, `{:flight-edn … :run-id … :issuing-caller … :trigger …}` — and its opts map
(`:426`) destructures `[agency-base run-record-dir caller poll-ms post! get-status! read-record! sleep!
today]`, so a caller cannot supply one without changing the function. **`flight-edn` carries no cast
field**: `:author`/`:reviewer` appear nowhere in `flight.clj`, `flight_runner.clj` or `flight_driver.clj`,
and the runner reads only `[:flight :target]` from the parsed flight (`:4667`).

**And the environment is empty.** The JVM serving :7070 is pid 392550, started 2026-09-21 19:14:01. Its
environment holds four `FUTON_WM_*` variables — `BETA_DARK`, `FPI_DARK`, `RECORDING_CONTRACT`,
`TRACE_POLICY_DETAILS` — and **zero** variables ending `_AGENT` of any kind. So the seventh flight's close
was not a misconfiguration to be found: there is no configured path by which a flight click could have
named a cast.

Note what this does *not* need: a new refusal. `:agent-unavailable` with `{:absent :no-cast-given}` is
already register row 420 and already typed on the record.

## 2. What the cast is for, and how it relates to `--dispatch-seat`

**Two seats for the tick, and they must differ.** `:4803-4807` refuses when either `:author` or
`:reviewer` is nil; `:4817-4823` refuses when the reviewer is unavailable **or is the author**.
`:repair-reviewer` is a third role, not checked at this gate. The author is the seat the runner dispatches
the selected action to through its own Agency client — `agent-roster` (`:790-796`) GETs
`/api/alpha/agents` with a 10 s timeout and throws `:agent-unavailable` "Agency roster unavailable" on any
non-200. The reviewer is the independent-review seat, which is why author ≠ reviewer is enforced rather
than merely preferred.

**The flight's `--dispatch-seat` is a different, third seat.** `flight_driver.clj:18-21` calls it "the
seat that carries out a chosen candidate's steps"; `:200-203` builds `fr/agency-dispatch-step!` from it
with `(runner/config {})` for the agency base, and without the flag a decision records
`{:absent :no-dispatch-configured}`. `agency-dispatch-step!` (`flight_runner.clj:163-179`) bells that seat
**once per pattern step**, by requisition, mode work, polls the job to terminal, and returns typed answers
— `:seat-not-on-roster`, `:requisition-missing`, `:job-not-terminal-by-deadline`, `:not-answered`,
`:unparseable-response`, `:not-dispatched` — with "Nothing escapes as an exception" (`:178`).

So: **up to three seats, two dispatch mechanisms, two records.** The tick's author dispatch is written on
the run record (and, when it fails, on the repair finding and the ticket the tick published by itself);
the flight's step dispatch is written on the enactment record (§D4.3 of `REFUSAL-REGISTER-D.md`). They are
independent — the seventh flight had neither, which is why it closed `:agent-readiness` *and* recorded
`:enactment {:absent :no-dispatch-configured}`.

## 3. Who may be cast

**The mechanical rule** is `available?` (`full_loop_runner.clj:801-803`): the roster has a record for the
seat, `(true? (:invoke-ready? a))`, and `(= "idle" (name (:status a)))` — registered, invoke-ready, idle.
`restored?` (`:805-806`) is a separate state, and only the reviewer path wakes a restored seat
(`:4810-4816`).

**The roster, read once on 2026-09-26:** 61 seats, 27 of them idle and invoke-ready. `claude-5` is idle
and invoke-ready, so it qualifies mechanically. `codex-1` is on the roster; **`zai-5`, `codex-22`,
`codex-24` and `codex-7` are not**, and neither `wm-author`, `wm-reviewer` nor `wm-repair-reviewer` is
registered at all.

**The ruling on record** is `futon2/holes/labs/M-aif-full-loop-53/CASTING-AMENDMENT-2026-09-14.md`. §2:
"Joe's instruction (2026-09-14, voxterm session): claude-15 leaves the loop cast entirely — the
orchestrator/participant dual role is confusing, and the author-seat readiness gate burned two attempts
(cohorts 50/52) on my seat being busy. Worker seats only." §3 names author `zai-5`, reviewer `codex-22`,
repair-reviewer `codex-24`, "passed explicitly in the click payload" — so the payload channel is the
established practice for ordinary clicks, and the flight is the thing that does not use it. §4 keeps
claude-15's roles outside the machinery.

Two consequences follow from those two paragraphs without needing a new ruling. The amendment's named cast
**cannot be used today** — all three seats are off the roster. And the amendment's stated reason bears on
casting a flight's own seat: `available?` requires `idle`, while a seat that is flying reads `invoking`
(both `claude-3` and `claude-8` read `invoking` in the same roster snapshot, each mid-turn), so a seat
cannot both fly and satisfy the readiness gate at the moment the click runs. Whether `claude-5` may be cast
is therefore a question for whoever owns the cast, not a reading of the code; what the code says is that it
would have to be a seat that is idle when the click fires, and not the one flying.

## 4. Defect (iv): the flight record drops the selection and the close kind

**The selection is computed and then discarded.** `record-summary` (`flight_runner.clj:402-417`) reads
`[:decision :chosen]` at `:408` and returns `:chosen (select-keys chosen [:candidate :precedence])` at
`:414`; `http-click-fn` returns that map at `:470`; `flight.clj`'s `run!` merges it into `record-click`'s
argument. **`record-click` (`flight.clj:206-242`) never destructures it** — `:215` takes
`[click-id wants want-source before after unreached-wants abstention]` — and the click entry it builds at
`:223-230` has a fixed key set with no field for it. The data arrives and the record has nowhere to put it.

**The close kind is dropped twice, and the second drop is the deeper one.** `record-summary` reads only
`[:decision :chosen]` and `[:decision :abstention]`; it never reads the record's terminal outcome. And
`record-click` has no parameter for one: its single channel for the tick's own answer is `:abstention`
(`:231`, `select-keys [:kind :missing :declines :status :detail]`), and a tick that *selected* and then
failed at `:agent-readiness` writes no abstention at all — the carrier's branches are for a nil decision
(§D8.2). So `:agent-unavailable` has no path onto a flight record.

**Pinned on the seventh flight's own record** (`flight-278b6988.edn`): `:agent-unavailable` occurs zero
times; the only `:chosen` in the file is `:chosen-because`, the target's provenance; the click entry's keys
are exactly `record-click`'s fixed set; `:enactment {:absent :no-dispatch-configured}`.

## 5. The smallest change, sized and not made

Three separable pieces. **Only the first is needed for the eighth flight to pass `:agent-readiness`.**

| piece | change | cost | what it does not do |
|---|---|---|---|
| (1a) name the cast in the environment | set `FUTON_WM_AUTHOR_AGENT` and `FUTON_WM_REVIEWER_AGENT` to two distinct registered idle seats | zero code | does not satisfy "a cast named **on the record**" — it is invisible to every reader of the click; it applies to every click that JVM serves, not to this flight; and `System/getenv` reads the process environment, so it needs a restart of the :7070 JVM, which is Joe's call |
| (1b) name the cast in the click | one destructuring key in `http-click-fn`'s opts (`flight_runner.clj:426`) and two `nonblank-string?`-gated assocs on the POST body (`:448-449`), plus the driver flags that carry them | ~6 lines, one test box, no new field on any record — the endpoint keys already exist and are already validated | nothing else; a click naming no seat still names none, so `393660c7`'s "no default cast" is untouched |
| (2) choose the seats | two distinct seats, registered and idle at click time, "worker seats only" per the 2026-09-14 amendment | none | not mine to choose, and the amendment's own cast is unusable — `zai-5`, `codex-22`, `codex-24` are all off the roster |
| (3) defect (iv) | `:chosen` onto the click entry: one key at `flight.clj:215` and one in the `cond->` at `:223-230`, since `record-summary` already supplies it. The close kind needs more: a terminal-outcome read added to `record-summary` (`:408-417`) and a field on the click entry | the selection half is two lines; the outcome half is a **new field on the flight record**, which under the standing rule wants its AIF-validity case stated first | — |

The case for the outcome half, if it is wanted: today a flight that selected a candidate and then failed
readiness is byte-indistinguishable on its own record from one that never selected. That is an absence
read as a value, which is the rule the standing rulings already state; the field would type it.

**Recommendation, for whoever writes the fix packet:** (1b) then (3), in that order and in separate
commits — (1b) is what lets the eighth flight reach the author dispatch, and (3) is what lets its record
say so. (1a) would get a flight past the gate sooner and would leave no evidence that it did.
