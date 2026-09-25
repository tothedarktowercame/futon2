# WM-UNIVERSE-D — the fact universe of a machine-read mission (discovery, read-only)

claude-10, 2026-09-25, for claude-8 (packet WM-UNIVERSE-D). Read at futon2
89a7b9ee (the source is unchanged from dafabccc; 89a7b9ee adds register D6
only) and futon3c c2551db1. Records read: futon3c
`holes/labs/M-wm-wiring/spike/` (flight-ffcd772b's flight record, its tick run
record, its timeline), and futon2 `data/wm-interpretations/M-autoclock-in.edn`
(read, not written). No code, no test, no flight.

**The premise needs correcting before the six answers.** The second flight's
click abstained with `:universe-not-admitted :missing :universes` for **282
targets**, M-autoclock-in among them. A flight click should assemble one
target, because `flight-assembly-input` (`war_machine.clj:6037-6061`) narrows
`:targets` to the flight's target. The run record says why it did not: its
`[:runner/source :namespaces]` marks `futon2.report.war-machine` **`:stale`**.
The server loaded it at 2026-09-24T04:52:45Z (loaded sha256 `dfe9372c…`; the
file on disk is now `5f12216d…`). `flight-assembly-input` landed in 95aa28b2
at 2026-09-24T20:39:56Z, so the war_machine the click ran has no flight
handling at all. It ignored `:flight`, assembled every target from the
declared sources, and refused every read target at `:198`.

The same record marks seven more tick-side namespaces `:stale`:

| namespace | loaded at |
|---|---|
| `futon2.aif.policy` | 09-24 04:52 |
| `futon2.aif.cascade-problems` | 09-21 |
| `futon2.aif.cascade-sources` | 09-23 |
| `futon2.aif.efe` | 09-21 |
| `futon2.aif.interpretation-construction` | 09-22 |
| `futon2.aif.cascade-habit-store` | 09-21 |
| `futon2.aif.scoring-input-receipts` | 09-22 |

`full-loop-runner` is `:current` (loaded 09-25 22:52). So the click tested
none of the tick-side wiring from rows 1–15: step 9's `:candidate` in
policy.clj, step 3's `order-use` in efe.clj, step 8's fold read, and the
flight path itself.

**The flight side, running HEAD code at 2e509a34, had a universe.** Every
ask-step rejection with this kind reads
`{:kind :universe-not-admitted :missing :locators :tokens-without-checkable-locator
[h0c55… h0f3a… h59ad… h5c69… h64dd… h7934… h7afd…]}`. That is `:218`, not
`:198`. A universe was present, `{h1dae false, h203c false}`, reproduced
offline at HEAD against today's store with a stub observer. What was missing
was checkable locators for 7 of the tokens the problem reads.

## 1. What `observe-facts` and assembly need

- **The declaration.** A declaration's `:facts` is a vector of tokens, and its
  `:locators` maps a token to a check locator. The example is
  `resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn`:
  - `:facts [:admission/task-stated :hole/h6378c65a4012 :hole/h0e270aa090bc :hole/h42fceb4ad48b]`;
  - every locator is `{:class :C4 :repo "futon2" :sha … :path "holes/missions/M-aif-policy-conditioned-eig.md" :decl "- [x] **Mint the shared posterior updater, …"}`.
- **`observe-facts`** (`cascade_sources.clj:219-228`) passes `{fact (get locators fact)}` to `oc/observe` (`observation_checks.clj:555-571`). A token maps to true if observed, false if checked and not observed, else `:unknown`:
  - a token with no locator reaches the check with a nil `:class`, so `(get checks nil)` is nil;
  - it is refused `:no-mechanical-check` and lands in `:refused`;
  - it is therefore neither observed nor in `:results`, and it comes out `:unknown` (`:224-227`).
  - `load-declared` stores that as `[:universes t]` (`:275`, `:284`).
- **Assembly** (`cascade_problems.clj:139-230`, `assemble-one`) accepts `:unknown` values. It checks, first applicable wins:
  1. `:198`: the universe is a non-empty map, else `:universe-not-admitted :universes`;
  2. `:201` and `:206`: interpretations;
  3. then `:want`;
  4. `:218`: `unlocated-tokens` (`:134-137`), every token in `problem-tokens` (`:86-97`: the universe's keys, the want, and every interpreted pattern's guard and produces) with no locator of a checkable class, refused `:universe-not-admitted :locators` with the tokens named.
- **The requirement at `:218`** is the P5 comment at `:213-217`: every token is observed mechanically.
- **Downstream.** The constructor takes `:unknown` as not established (`:108-130`), and lists those tokens on each candidate's receipt as `:unknown-read-as-not-established`. G is then taken over the problem's tokens. The register's row-13 KEEP note says this: dropping unlocated tokens would change the universe G is taken over (W6 comparability).

## 2. What the read step already has

- **The store for M-autoclock-in** holds:
  - 11 extracted criteria (`:kind :extracted-criterion`, each with `:line`, `:stated` a verbatim span, `:statement`, `:phase "EXTRACTED"`);
  - 2 published locators, both `:C8` registered runs: `:exit/h1dae607747ce` (namespace `futon3c.agency.clock-decision-test`) and `:exit/h203ce9577ebe`;
  - 6 read constraint edges: `h7934→h5712`, `h7934→h203c`, `h203c→h64dd`, `h5c69→h1dae`, `h0c55→h59ad`, `h0c55→h0f3a`;
  - no published patterns.
- **The flight** asked for 9 of the criteria as wants. Its locator readings came out:
  - 2 published;
  - 5 rejected `:cue-not-in-criterion` (`mission_reading.clj:170`: the seat's cue, word-normalised, is not a substring of the criterion's `:stated`);
  - 2 rejected `:check-refused`, the C8 timeouts WM-SPIKE-FIX-II D now names;
  - 1 (`h57127b470ddb`) raised owner questions.
- **The tokens are the same kind of thing** as a declaration's `:facts`. The hand declaration's facts are exit conditions too: `:hole/h…` tokens for the mission's `- [x]` checkboxes, observed `:C4` by the checkbox's text, plus one admission token.
  - The a-exits reader (`flight.clj:45-95`) turns criteria into wants through `mission-criteria/wants`. Its `:universe` holds the criteria with a stated verdict (`**Met when:**`, a C4 locator; register D6.1 (ii)).
  - Then `:85` adds each machine-located token, observed through its published locator.
  - So a read mission's universe already consists of its located exit tokens. What differs from a declaration is the observation class (C8 runs, not C4 checkbox text) and how many tokens are located (2 of 9).

## 3. The candidate definition, and the walk

**The definition, stated so it can be refused:** the universe of a
machine-read mission is its extracted criterion tokens and the tokens its
constraints name, each observed through its published locator, `:unknown`
where none.

**Walk at HEAD on this flight's record**, with a server that has reloaded:

1. **Today's rule already writes part of this.** HEAD writes the located part (`flight.clj:85`). `flight-edn` carries it into the tick (`war_machine.clj:6050`). So `:198` passes: `{h1dae false, h203c false}`.
2. **The click refuses next at `:201`, `:no-admitted-interpretation`.** The store has no published patterns, and the tick's declared sources have none for this target.
3. **The ask step cannot publish one.** Each trial interpretation (`want_interpretation.clj:271-280`) reaches `:218` and is refused `:locators` for the 7 unlocated want tokens, exactly as recorded.
4. **Adding the unlocated tokens as `:unknown` does not change step 3.** `unlocated-tokens` reads the locators, not the universe's values, and the wants are in `problem-tokens` whatever the universe holds.

So the candidate definition does not by itself admit anything: the refusal is
at `:218`, and no candidate can be constructed on this record. The definition
matters only together with a change to `:218`, where an `:unknown` token is
admitted unobserved. That reverses P5 for read missions.

**Without that change, the route out is locators:** 7 more checkable
locators. The record shows these fail for two reasons:
- five were rejected by a word-substring cue check;
- two were the C8 read timeout.

## 4. Where the write goes

- **Flight side:** `source-wants` `:a-exits` writes `:universe` (`flight.clj:70`, `:85`). `click-wants` carries it (`:183-187`). `judge-opts` sends it (`:197`). `target-view` passes it to `flight-assembly-input` (`flight_runner.clj:231`).
- **Tick side:** the chain carries the flight's universe:
  1. the click handler reads `:flight-edn` into `:flight` (futon3c `http.clj:8956`);
  2. `run-opportunity!` passes `:flight` to the judge (`full_loop_runner.clj:4590`);
  3. `generate-war-machine` calls `flight-assembly-input` with it (`war_machine.clj:7238`);
  4. that function merges the flight's `:universe` into `[:sources :universes target]` (`:6050`) and narrows `:targets`.
- **So no tick-side write is needed** for a flight's target. A read target's universe reaches the tick through the flight.
- **Only outside a flight** (the outer cascade choosing among all targets) does the tick read universes solely from declared sources (`cascade_sources.clj:284`). Admitting a read target there is a tick-side write. The outer cascade is the map's one unbuilt box.
- **What failed on the second flight** was the serving JVM's stale war_machine, not the wiring.

## 5. The definition question for Joe and Clause T

The question is whether a read mission's exit tokens enter the universe as
`:unknown` when no checkable locator exists, so that G is taken over tokens
the machine cannot observe. The alternative is that every token G is taken
over must be mechanically observable, as `:218` requires today.

**Under the first answer:**
- Assembly admits a read mission once one interpretation publishes.
- The constructor treats unknown tokens as not established (`:108-130`).
- A cascade can then be built toward a want whose production no check can ever observe. The flight could never see it advance, and W6 comparability across targets would compare Gs over unobservable outcomes.
- IDENTIFY 4 gains nothing to satisfy: no contract is added, and one is removed. But the flight's per-click progress (`:advanced`) would stay empty for those tokens by construction, so an efficiency measurement would read "no progress" where the truth is "unobservable".
- On the map, `r6`'s assembly box would stop reading `:locators` as an admission condition. No new writer.

**Under the second answer:**
- The contract a flight must satisfy is one checkable locator per wanted token.
- It is already written, and the read step already asks for it.
- The cost is locator readings that pass. On this record that is 7 more, blocked today by the cue check (5) and the C8 timeout (2).
- The map is unchanged: the flight source writes `:universe`, `flight-assembly-input` reads it, and the expected findings do not move.

## 6. Recommendation, size, falsifier

**Recommendation: keep `:218` (the second answer) and take the locator route.
It is also the smaller build.**

1. **Before any build: reload** `futon2.report.war-machine`, `futon2.aif.policy`, `cascade-problems`, `cascade-sources`, `efe`, `interpretation-construction`, `cascade-habit-store` and `scoring-input-receipts` from master in the serving JVM. That is Joe's or claude-8's call under the no-loads rule. Without it no flight click tests the wiring.
2. **A discovery packet on `:cue-not-in-criterion`** (`mission_reading.clj:160-176`). Read the 5 rejected cues against their criteria and say whether the word-substring check is refusing faithful quotes (markdown, ellipsis, line joins) or the seat is quoting outside the criterion.
3. **Then, if the check is too strict,** one fix in `validate-locator` with its test box (the 5 rejected cues as fixtures).
4. **No map rows change.**

**Size:** one file, one function, one test namespace, after one reload and one
discovery.

**Falsifier:** a third flight of M-autoclock-in, with the tick reloaded, must
produce a tick run record that does the following. The definition was wrong
if a located token is still refused, or if the click refuses `:198`.
- Its `:runner/source` marks every listed namespace `:current`.
- Its abstention lists **one** target, M-autoclock-in, not 282.
- It refuses at `:201` `:no-admitted-interpretation` while fewer than all wanted tokens are located, or at `:218` naming exactly the still-unlocated tokens.
- Once every want has a published checkable locator, the ask step's trial assemblies pass `:218` and fail or succeed on interpretation grounds alone.
