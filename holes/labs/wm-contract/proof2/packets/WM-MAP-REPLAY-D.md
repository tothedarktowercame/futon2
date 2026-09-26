# WM-MAP-REPLAY-D — would the wiring map have reported the flights' defects? (read-only)

claude-10, 2026-09-26, for claude-8. Read at futon2 1e1546f6 and futon3c
9111ad35 (map 69d111a8, 27 expected findings). The prover is
`futon3c/src/futon3c/diagramprover/wiring.clj`.

Nothing was run against the pre-fix shas. Each "would fire" below is read
from the prover's rules applied to the pre-fix source (`git show <fix>`).
The follow-up build's falsifier is where that gets run. No map edits, no
code edits, no flight, nothing under `data/`.

## The prover, as it bears on this

**What it compares.** It compares declared `:reads`/`:writes` against
keyword occurrences in each box's var, textually. It reports:

| finding | when |
|---|---|
| `:declaration-without-occurrence` | a declared field is absent from the var |
| `:occurrence-without-declaration` | a declared field occurs in a box's var that doesn't declare it |
| `:declared-read-not-found` / `:declared-write-not-found` | heuristic: the field occurs, but never in a read (or write) position |
| `:read-never-written`, `:written-never-read`, `:multiply-written` | across boxes |
| `:var-not-found`, `:site-unreadable` | the site can't be read |

**How positions are classified** (`classify-keyword`, `:249-280`):
- **Reads:** `get`/`get-in`, a keyword in function position, `:keys`.
- **Writes:** map-literal keys, `assoc`/`update`/`assoc-in`.
- **Anything else is `:unclassified`**, including a `select-keys` vector.

**Consequence: the prover only sees what is declared.** A defect is a finding
only if a box names the field that should flow. None of the nine fixes added
a box, so none was a finding before or after its fix.

## A. The click's refusal reason dropped before the flight record (fix 8588dba0)

| | |
|---|---|
| writer / reader / field | **W** `flight_runner.clj` `http-click-fn` writes `:abstention {:kind :missing :status :detail}`. **R** `flight.clj` `record-click` (`:231` pre-fix) takes `(select-keys abstention [:kind :missing :declines])`, so `:detail` and `:status` are dropped. Field: `:detail` (and `:status`). |
| boxes on the map today | none for either var; `:detail` not declared |
| at 8588dba0^, with boxes | **R** declares reads `:detail`: `:declaration-without-occurrence` (record-click has no `:detail`). **W** declares writes `:detail`: nothing fires (map-literal write present). |
| at HEAD | `:detail` now sits in `record-click`'s select-keys vector, which is `:unclassified`, so the heuristic reports `:declared-read-not-found`. **A false standing finding:** the prover doesn't count `select-keys` as a read (list ii, item 1). |

## B. A seat's reply read through the verdict joiner (fix e4176878)

| | |
|---|---|
| writer / reader / field | **W** `agency-answer-fn`'s `:job-text` port, bound to `task-execution-evidence/job-text`. It joins `:result-summary`, `:result` and the text events, so a one-block reply arrives as two copies. **R** `want_interpretation/parse-reply` reads `:text` and rejects `{:forms 2}`. Field: `:text` (its content duplicated). |
| boxes today | `:r3-flight-ask` at `agency-answer-fn` (writes `:library-root` only); none at `parse-reply` or `reply-text`; `:text` not declared |
| at e4176878^, with boxes | Nothing. `:text` flows correctly by name; what's wrong is *which function fills the port* and the *content* (two copies). Declaring `:result` read at `agency-answer-fn` would fire `:declaration-without-occurrence` both before **and** after the fix, since the key is read inside `reply-text`, so it discriminates nothing. |
| at HEAD | nothing |
| shape | a port bound to the wrong implementation; content duplication (list ii, items 2 and 3) |

## C. A construction exception sent as a nil request (fix 9eed8b7e)

| | |
|---|---|
| writer / reader / field | **W** `issue-request`'s catch: `{::refused (:interpretation/refusal (ex-data e))}`, which is nil when the ex-data has none. **R** `ask-one`'s `(if-let [r (::refused issued)] …)`: nil reads as "issued", and the map goes to the seat. Field: `::refused`. |
| boxes today | none |
| at 9eed8b7e^, with boxes | Nothing. The key is written and read. The defect is a **nil value** under a present key, read by truthiness. The key is also an auto-resolved `::refused`: the prover matches the literal text `:futon2.aif.flight-runner/refused`, which never appears. |
| at HEAD | nothing |
| shape | a nil value read by truthiness; auto-resolved keywords (list ii, items 4 and 5) |

## D. Registry reads losing `:message`/`:class`/`:timeout-ms` (fix 367be490)

| | |
|---|---|
| writer / reader / field | The same var in both roles, `observation_checks` C8 read (both call sites). The catch writes `{:status :unreachable :body (.getMessage e)}`, and the refusal builds `{:check :url :status}`, dropping the body. Field: `:message` (pre-fix the text is under `:body`). |
| boxes today | none |
| at 367be490^, with boxes | A box declaring reads `:message`: `:declaration-without-occurrence` (only `.getMessage` occurs; no `:message` keyword). |
| at HEAD | `:message` sits in `(select-keys resp [:message :class :timeout-ms])`, which is `:unclassified`, so the heuristic reports `:declared-read-not-found`. The same false standing finding as A. |
| shape | intra-var; in the model only if one box both writes and reads, or the catch is split into its own var (it now is, `registry-get`) |

## E. `:ex-kind` and the cause chain dropped (fix dafabccc)

| | |
|---|---|
| writer / reader / field | **W** the throw site's ex-data (`mission-registry`, `{:kind …}`) and its cause. **R** `issue-request`'s catch. Fields: `:kind`, and `ex-cause`, which is no keyword. |
| boxes today | none |
| at dafabccc^, with boxes | Declaring `issue-request` reads `:kind`: `:kind` occurs only as a map-literal write (`:kind :construction-threw`), so the heuristic reports `:declared-read-not-found`. It would fire. At HEAD `(:kind (ex-data e))` is a read, so it's silent: **in the model**. **But** `:kind` in the field universe would raise `:occurrence-without-declaration` at nearly every box site, since `:kind` is ubiquitous. The cause chain is a Java accessor, not a key, and is outside the model. |
| at HEAD | nothing (for `:kind`) |
| shape | an exception path (throw → catch across namespaces); generic keys; accessor-carried data (list ii, items 6, 7 and 8) |

## F. An escaped `HttpTimeoutException` aborting the flight with no record (fix f37c120e)

| | |
|---|---|
| writer / reader / field | There is no field. `read-job!` threw, and nothing between it and `flight_driver/run-flight!` caught it, so the record write never ran. The fix introduced `:ask-threw`, `:aborted` and `::aborted`. |
| boxes today | `:r3-flight-ask` at `agency-answer-fn`; none at `run!` or `run-flight!` |
| at f37c120e^, with boxes | Nothing, unless the fix's own fields are declared in advance. That records the fix; it doesn't detect the defect. |
| at HEAD | nothing |
| shape | control flow: an uncaught exception skipping a write (list ii, item 6) |

## G. The judge's refusal under `:kind`, the runner's classifier reading `:failure-kind`/`:outcome` (fixes 321d82c8, c7367eaa)

| | |
|---|---|
| writer / reader / field | **W** `war_machine.clj` `cascade-family-parameters`, `cascade-decision-admitted` (`:6237 :6240 :6301 :6323 :6437 :6624`), `(ex-info "cascade decision refused" {:kind …})`. **R** `full_loop_runner.clj` `explicit-failure-kind` reads `:failure-kind`/`:outcome`. The fields disagree: `:kind` written, `:failure-kind` read. |
| boxes today | `:r9-decision` at `cascade-decision-admitted` (reads `:per-policy-argmax` only). `:r9-judge-refusal` at `judge-refusal-sorry` and `:r9-abstention-carrier` at `persist-run-record!` (added in map 50a68ccf, after the fix). `explicit-failure-kind` has no box. |
| the 50a68ccf boxes at c7367eaa^ | `judge-refusal-sorry` doesn't exist: `:var-not-found`. `persist-run-record!` has no `:judge-refusal`: `:declaration-without-occurrence`. **They fire, but only because they name the fix's own var and field. They record that the fix exists; they don't detect the mismatch.** |
| what would detect it | The contract as declared by the reader: `:r9-decision` writes `:failure-kind` (what the runner reads), and a box at `explicit-failure-kind` reads `:failure-kind`. At cdc492ec (the fix's parent), `cascade-decision-admitted` contains no `:failure-kind` (0 occurrences), so `:declaration-without-occurrence` fires. **At HEAD it still fires**: the fix went on the runner side (the runner now also reads `:kind`), and the judge still writes no `:failure-kind`. For the falsifier the declaration has to follow the fix: the judge box writes `:judge-refusal`'s source key `:kind`, which hits E's `:kind` problem, or the runner box reads the message-typed ex-data. **G is only partly in the model.** |
| shape | an exception path, and a key mismatch across a throw/catch (list ii, items 6 and 7) |

## H. The default cast standing in for an absent cast (fix 393660c7)

| | |
|---|---|
| writer / reader / field | `full_loop_runner` `config`: `(or (System/getenv …) default-author)`. Readers: the preflight and `run-opportunity-core!`. Fields: `:author`/`:reviewer`. |
| boxes today | `:dispatch` at `dispatch!` (writes `:mission-id`); nothing declares `:author`/`:reviewer` |
| at 393660c7^, with boxes | Nothing. The field is written and read. What's wrong is that the value is a literal standing in for an absence. |
| at HEAD | nothing |
| shape | a value standing in (provenance of a value) (list ii, item 9) |

## I. The relation: a target of unknown class refusing `:class-unknown-no-scalar-g` (fix 1e1546f6; map 10a4ad77)

| | |
|---|---|
| writer / reader / field | **W** `focus_receipt/classify-target` returns `{:target :class :relation :derived-via}`. **R** `war_machine.clj` `cascade-decision-admitted`: `:6527` binds the **return value** into `target-classifications`; `:6587` reads `(get scorer-class (:class c) :unknown)`; `:6585` writes `:target-class`, which goes to `class-observation-model` and then `observation_model` (`:75`, `:keys [… target-class …]`). |
| the hidden hop | `war_machine.clj:6527`, the return value of `(focus-receipt/classify-target …)` consumed by the caller, with no key between the two vars. `:r9-classify-target` and `:r9-embedding-neighbour` declare only their inputs (`:relations`, `:source-pins`, `:min-cosine`) and no output, so coverage shows them dead. |
| would a declared field show it | Yes. The value crosses as `:class`: a map-literal write in `classify-target`, and a read `(:class c)` in `cascade-decision-admitted`. Declaring `:r9-classify-target :writes [:class]` and `:r9-decision :reads [:class]` connects them. `:target-class` then carries it on: `:r9-decision` writes it, and a box at `observation_model` `class-emission` reads it. **Cost:** `:class` is generic (exception summaries write `:class` in `flight_runner` vars that are box sites, e.g. `read-fn`), so it may raise `:occurrence-without-declaration` there. `:target-class` is specific. |
| at 1e1546f6^, with boxes | A defect about the **value** (the class `:unknown`), not the flow. `:class` flowed. The prover would report nothing. |
| at HEAD | nothing |
| shape | return-value hops (list ii, item 10); value-level refusal (item 9) |

## Flight stops outside the map's model, which it should not pretend to cover

| stop | flight | what covers it |
|---|---|---|
| **The serving JVM running stale code** | 2 (and 4: `cascade-model-manifest`/`efe` stale since 63246b57) | `refuse-on-runner-source-drift!` (`full_loop_runner.clj`, `:stale-runner-source`) covers the runner namespace only. `load-identity/register!` records each namespace's loaded file, and the click reported "tick current except …" on flight 4, but nothing refuses on the other namespaces. **Partly covered.** |
| **futon1b hung** | 2, 3 | The C8 registry read's `*registry-timeout-ms*` 5000 (`observation_checks.clj`, D), typed `:registry-unreadable` with `:timeout-ms`. The mission-registry read's timeout surfaces through `:construction-threw` with `:cause` (E). Neither is a map concern. |
| **The process timeout** | 3 | Flight 3's `HttpTimeoutException` was the Agency job poll (`read-job!`, 10 s), not futon1b. It is now `:ask-threw`, and a Throwable out of any step writes `:status :aborted` (F). **Not covered:** a killed driver process (SIGTERM/SIGKILL from an outer `timeout`) still writes no record. There is no tripwire for it. |

## (i) Boxes and fields to add, so each defect in the prover's model is a finding at its pre-fix sha and nothing at HEAD

Five items. Only **A**, **D**, **E** (by the heuristic) and **I** (flow only)
are in the model. For **G**, the detecting declaration keeps firing at HEAD,
because the fix is on the runner side, so G is listed with its caveat.

1. **A:** box `flight-record-click` at `flight.clj` `record-click`, reads
   `[:detail :status]`; box `flight-http-click` at `flight_runner.clj`
   `http-click-fn`, writes `[:detail :status]`.
   - At 8588dba0^: `:declaration-without-occurrence` (`:detail`, `:status`).
   - At HEAD: nothing, **only after** the prover counts a `select-keys`
     vector as a read (list ii, item 1). Until then it's a false
     `:declared-read-not-found` at HEAD.
2. **D:** box `c8-registry-read` at `observation_checks.clj` `registry-get`,
   writes `[:message :class :timeout-ms]`; box at the C8 check var, reads the
   same.
   - At 367be490^: `registry-get` doesn't exist, so `:var-not-found`; the
     check var gives `:declaration-without-occurrence`.
   - At HEAD: nothing, again only with list ii, item 1. As written it names
     the fix's var, so it's partly circular like G. A pre-fix-honest
     alternative is one box at the check var declaring both roles.
3. **E:** box `issue-request` at `flight_runner.clj` `issue-request`, reads
   `[:kind]`.
   - At dafabccc^: `:declared-read-not-found` (heuristic).
   - At HEAD: nothing.
   - **Cost:** `:kind` in the field universe raises
     `:occurrence-without-declaration` at most box sites. Only worth it if the
     prover gains per-box field scoping (list ii, item 7).
4. **G:** box `runner-failure-kind` at `full_loop_runner.clj`
   `explicit-failure-kind`, reads `[:failure-kind :outcome]`; `:r9-decision`
   gains writes `[:failure-kind]`.
   - At cdc492ec: `:declaration-without-occurrence`, since the judge var
     carries no `:failure-kind`.
   - **At HEAD it still fires**, because the fix left the judge untouched and
     taught the runner `:kind`. For the falsifier to pass at HEAD, the
     runner box must declare the fix's read (`judge-refusal` reads `:kind`),
     which is E's `:kind` problem again. **So G can't be cleanly falsified by
     this prover as it stands.**
5. **I (the flow only):** `:r9-classify-target` writes `[:class]`,
   `:r9-decision` reads `[:class]` and writes `[:target-class]`, and a box at
   `observation_model` `class-emission` reads `[:target-class]`. That ends
   coverage's dead verdict on the two row-9 boxes. It doesn't detect I's
   defect, which is about a value.

**Falsifier for the follow-up build:** the map with items 1-4 run at
8588dba0^ reports A, and at cdc492ec reports G. At HEAD it reports neither
A (given list ii, item 1) nor any new finding except G's, which stays, per
item 4. The packet's expectation that G is silent at HEAD doesn't hold with
these declarations. That is itself the finding.

## (ii) Shapes the prover cannot see, for claude-11 (projection) and the diagramprover owner

Ten items, each with the defect it missed.

1. **`select-keys` is not a read position.** It shows up as false
   `:declared-read-not-found` at HEAD for A and D.
2. **A port bound to the wrong implementation** (a function passed as an
   option). Missed B.
3. **Content duplication or corruption under a correctly-named key.** Missed B.
4. **A nil value under a present key, read by truthiness** (`if-let` over
   `get`). Missed C.
5. **Auto-resolved keywords** (`::k`), never matched by literal text. Missed
   C's `::refused`, and would miss F's `::aborted`.
6. **Exception paths:** throw → catch across vars and namespaces, and
   uncaught escapes that skip a write. Missed E, F and G.
7. **Generic keys** (`:kind`, `:class`, `:status`), which can't enter the
   field universe without flooding every site. There is no per-box or
   per-path scoping. Blocks E and G, and costs I.
8. **Accessor-carried data** (`ex-cause`, `ex-message`, `.getMessage`): data
   that flows with no keyword. Missed E's cause chain and D's message.
9. **Value provenance:** a literal default or an `:unknown` class standing in
   for an absence, when the field flows correctly. Missed H and I (the value
   side).
10. **Return-value hops:** a function's return consumed by its caller with no
    key naming the boundary, as at `war_machine.clj:6527`. Shows as dead
    coverage for I's boxes. It can be patched by declaring the returned map's
    key (i.5), but the prover can't infer it.
