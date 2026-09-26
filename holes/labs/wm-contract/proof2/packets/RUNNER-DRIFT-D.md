# RUNNER-DRIFT-D — why the drift tripwire read "zero stale" while the gate was stale

Read at futon2 `7b4ecf98`; read-only, nothing changed, nothing proposed. Sized after the fifth flight of
M-autoclock-in (`flight-7f89646a`) stopped at a decision gate whose serving copy predated `dd938402`
(§D9.2 of `REFUSAL-REGISTER-D.md`). **Its own file, not `REGISTRY-LATENCY-D.md`**: that note is about how
long a futon1b evidence read takes; this is about which namespaces a staleness check looks at and what it
may do about them. Owner: the tripwire's.

## 1. Where the 75 come from — two memberships, both opt-in

`runner-source-drift` (`full_loop_runner.clj:476-498`) reports on `load-identity/report`'s result, and that
function (`load_identity.clj:133-139`) merges exactly two sets:

1. **A literal map**, `required-sources` (`load_identity.clj:10` onward): **69** entries, namespace symbol →
   absolute path. Its docstring says how it was built — "the declared set includes every namespace the click
   path actually loads, found by reading `futon3c.wm.runner-service` → `futon2.aif.full-loop-runner` requires
   (not by guessing)" (codex-20 review, 2026-09-22). That is a require graph walked by hand on a date, kept
   by editing.
2. **Self-registration**: whatever is in the `registry` atom, put there by `load-identity/register!`
   (`:101-119`), which **48** namespaces call.

Their union is the ~75 in the report. There is no rule in either path: a namespace is covered because
someone added a line to the map, or because the namespace opted in. Nothing derives membership from what
the click actually loaded.

## 2. Why the gate and the observation check are outside it

Neither is in the map, and neither calls `register!` — checked both ways. Nothing else would have put them
in. `decision_gate.clj` was last touched by `dd938402` on 2026-09-25, three days after the map was walked;
`observation_checks.clj` by `367be490` the same night.

**But membership is not the only gap, and on its own it would not have stopped this flight.**
`runner-source-drift` puts every namespace's status under `:namespaces` (`:497`), and
`refuse-on-runner-source-drift!` (`:500-515`) refuses on `(:runner/source-check check)` alone — which is
`own`, `futon2.aif.full-loop-runner` itself (`:489`). The docstring says it outright at `:481`: "Only this
runner's `:drift` retains refusal authority." `:namespaces` is read nowhere in `src/` or `scripts/`; the
only callers of `runner-source-drift` outside its own file are four tests. So the tripwire did not look at
75 namespaces and see nothing wrong — it looked at one, which was current, and wrote a report about the
others that nothing consumes. Adding the gate to the map would have added a line to that report.

## 3. The cheapest membership is a rule, and it already exists as a tool

**Every loaded `futon2.aif.*` / `futon2.report.*` namespace, enumerated from `all-ns` at check time.** No
list, nothing to keep current, and it cannot fall behind the require graph — a namespace is covered because
it is loaded. claude-8's `stale-scan-by-line.clj` (futon3c `holes/labs/M-wm-wiring/spike/`) is that rule
working in 21 lines: for each var with `:file` and `:line` metadata, read that line of the file on the
classpath and ask whether the var's name is still on it. That is what found all eight stale namespaces.

Why the existing sha comparison cannot simply be pointed at the wider set: `load-identity` compares a
**captured** loaded sha against disk, and the capture happens in `register!` at load time. A namespace that
never registered has nothing captured, so there is no loaded sha to compare — which is why the current
design needs either the map or the opt-in. The by-line check needs no cooperation at all, because it uses
metadata the compiler records for every var.

Its limit, stated rather than left implicit: a var whose name still sits on the same line reads current even
if its body changed. It detects **displacement**, not content drift. Content drift needs the captured sha,
which needs registration. For the fifth flight displacement was enough — `locator-refusal` did not exist in
the loaded copy at all.

## 4. Sizing, not proposing

| change | cost | what it does not do |
|---|---|---|
| add the two namespaces to `required-sources` | two lines | covers this case, not the next: the map was walked by hand on 2026-09-22 and `dd938402` landed on the 25th |
| have the two call `register!` | a line each, and their sha comparison becomes real | same "next one" problem; every future click-path namespace must remember |
| membership by the `all-ns` rule, for the **report** only | ~20 lines, the tool already exists; no list to maintain, and it would have named the gate | does not stop a flight — `:namespaces` is still read by nothing |
| give that report refusal authority | small code; **a decision, not an edit** | today's fail-open is deliberate (`:503-504`: "a dev checkout without the canonical file must not brick the loop"), and a rule refusing on any of ~152 loaded namespaces will refuse often during ordinary work. Any new refusal here is a register row before it is a commit |

**Not established here:** the eight stale namespaces' effect beyond the gate (only `observation-checks` and
`decision-gate` were traced to the flight's stop); how the serving JVM came to hold copies older than
`dd938402` while running code committed after it; and the loaded-vs-HEAD readings themselves, which are
claude-8's from the JVM before the 02:31:44Z reload and cannot be re-measured — what is re-checkable, and
was re-checked here, is what HEAD and `dd938402`'s parent say.
