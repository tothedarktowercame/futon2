# I2 — CONSTRUCTOR: what the in-machine constructor builds, and where it stops

E-cascade-real, investigation I2 (kimi-3, 2026-09-24). DISCOVERY ONLY: fresh
private process (`clojure -M`), read-only on src/resources/data, no clicks, no
shared JVM, no writes under `data/`. Script committed alongside:
`i2_construct.clj` — repeat with
`clojure -M holes/labs/wm-contract/E-cascade-real/i2_construct.clj` from
`/home/joe/code/futon2`. The judge's admission check is a VERBATIM copy of
`candidate-want-progress` (`scripts/futon2/report/war_machine.clj:6549–6584`);
byte-diffed against the source. Run outputs quoted below are from the
committed script's stdout, not from reading the code.

## 1. Constructor shape (from `interpretation_construction.clj` @ 4328238d, `construction_moves.clj` @ 3b01790d)

`construct` takes: `:target`; nonempty `:want` (tokens); `:observation`
{token → boolean, and EVERY token in want ∪ guards ∪ produces must be boolean
— `:unknown` refuses `:observation-required`); `:interpretations`
{id {:guard {:needs #{} :forbids #{}} :produces #{}}}; `:interpretation-receipts`
{id nonempty-map} (a missing receipt refuses `:interpretation-receipt-missing`);
`:horizon` (pos-int); `:move-cost`; `:budget {:max-moves :max-expansions}`;
and `:evaluate-g` (candidate → finite double), injected unchanged.

It searches backward from the wants over `produces`/`needs`, compiles each
order through `moves/order-by-need`, model-checks reachability with
`cascade-model-manifest/rollout` at the given horizon (ns docstring: "Model
reachability is not an observed discharge"), keeps support-minimal plans, and
defers the final take to `construction/construct`, where a move is taken only
when `G(best current) − G(best proposed) − cost` is positive under the
injected G (`construction.clj:123–143`).

Output per candidate: `{:kind :cascade-candidate :target … :precedence […]
:need-edges … :construction-receipt {:kind :machine-constructed …}
:interpretation-receipts {…}}` — exactly the shape
`cascade_problems/constructed-candidates` (`cascade_problems.clj:53`) accepts:
non-empty vector `:precedence` plus a non-nil `:construction-receipt`. So the
constructor's success output is admission-shaped by construction; its typed
refusals (`:observation-required`, `:interpretation-receipt-missing`,
`:want-already-observed`, `:no-supported-order`, `:search-budget-exhausted`,
`:construction-not-taken`) are not candidates and would land as refusals.

The four library moves in `construction_moves.clj` (`read-what-exists`,
`borrow-a-sibling`, `order-by-need`, one more) are pure functions over
injected data; only `order-by-need` is on the constructor's internal path.

## 2. The run — one row per admitted target

Inputs per target: `cascade-sources/load-declared` (interpretations, receipts,
wants, C4-observed universes, horizon 4), as in CLICK2-D Part 2. Injected for
discovery: `:move-cost 1`, `:budget {:max-moves 4 :max-expansions 10000}`,
`:evaluate-g` = precedence length with the EMPTY family pinned worst (see the
G-sensitivity note in §3). Actual stdout:

| Target | Facts at HEAD (wants) | Constructor output (actual) | Admission (`candidate-want-progress`) |
|---|---|---|---|
| `T-repair-occ-444fb018cbbb…` | `:restoration-accepted true` | **refused `:want-already-observed`** — no candidates | not reached |
| `M-wm-08-external-f2` | `:route-a-rehearsal-reported true` | **refused `:want-already-observed`** | not reached |
| `M-f11-find-production-successor` | `h9ab212b3281d true`, `h2045faa0e7cc false` | **refused `:no-supported-order`**, expanded 1, finding `{:kind :unproduced-need :token :hole/h2045faa0e7cc :order []}` — the absent want is produced by NO declared interpretation | not reached |
| `M-aif-policy-conditioned-eig` | `h6378c65a4012 true`, `h0e270aa090bc true`, `h42fceb4ad48b false` | **CONSTRUCTED 1 candidate**: precedence `[:aif/two-layer-calibration]`, receipt `:machine-constructed` | **PASSES** (no `:no-new-wanted-token` decline — the rollout newly produces `h42fceb4ad48b`) |

Consistent with CLICK2-D's table on every point of overlap; the new fact is
row 4: given only the declared interpretations, the constructor DOES find the
route the hand-written candidates avoided.

## 3. M-aif-policy-conditioned-eig — the withdrawn route

**Yes.** The constructor builds `[:aif/two-layer-calibration]`, and the
judge's own admission check passes it. Nothing stops it.

Why nothing stops it: 8f97757b ("Withdraw the EIG calibration route until
held-out evidence exists") removed the CANDIDATE from the source file and left
the reason as a **comment** in `resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn`
("…cannot produce its token until a preregistered held-out split and
post-split outcomes exist… Restore when that evidence has a locator."). The
PATTERN `:aif/two-layer-calibration` remains declared in `:patterns` with its
receipt: guard `needs #{:hole/h6378c65a4012}` (true), `forbids
#{:hole/h42fceb4ad48b}` (false — the want itself), `produces
#{:hole/h42fceb4ad48b}`. The constructor's input contract
(`interpretation_construction.clj`, `pattern?`) sees exactly `:guard` +
`:produces`; the held-out-evidence requirement exists nowhere in that
vocabulary. `candidate-want-progress` rolls the same guard semantics, so
admission is blind to it too. What SHOULD stop it is a machine-readable
withdrawal — the comment names the remedy itself: "restore when that evidence
has a locator", i.e. today there is no locator for the held-out evidence, and
no key on the pattern saying so.

**G-sensitivity caveat (measured, not read):** an earlier run of the same
script with plain precedence-length as `:evaluate-g` returned
`:construction-not-taken` for this target — the empty initial family (G 0.0)
beat the constructed one (G 1.0), so `construction/construct` declined to take
the move. Whether the constructor EMITS the candidate is search-determined;
whether it is TAKEN is hostage to the injected G. Any in-judge wiring must
answer "whose G" before admission, not after.

## 4. Substrate missions

`(cascade-problems/substrate-targets)` enumerated **248** substrate targets
(open missions + live tickets). Declared interpretations exist for **5**
targets (the five `resources/wm/cascade-sources/*.edn` files), of which **3**
are substrate targets (`M-f11-find-production-successor`,
`M-aif-policy-conditioned-eig`, `M-expressions-of-interest`); the other two
declared sources (`M-wm-08-external-f2`, `T-repair-occ-444fb018…`) are not
substrate identities (the judge's union at `war_machine.clj:7067–7072` adds
them anyway). So: **3 of 248 substrate targets have any interpretations to
work from** (≈1.2%).

What would supply them, per the records: agent-authored `cascade-source-v1`
declarations (`cascade_proposals.clj` docstring: "An agent authors a complete
cascade-source-v1 declaration … Retrieval is explicit, outside the tick"); the
proposal supply (`data/wm-cascade-proposals/`, holding one proposal — D5); or
`mission-hole-wants/merge-into-sources` (`war_machine.clj:7044–7050`), which
today merges WANTS from mission text, not patterns/interpretations. Searched
for any other interpretation source: `load-declared` reads only that one
directory; none found.

## 5. Calling the constructor inside the judge (`war_machine.clj` ~7025–7085) — gaps

Described, not implemented. Each gap ties to a file and function.

1. **No G exists at that point.** `construction/construct`'s take rule
   (`construction.clj:123–143`, `best-g`/`g-norm`) needs a finite per-candidate
   G; the judge's scorer runs later inside `select-and-record-cascade!`. And
   the take is G-sensitive in practice (§3): wiring without a declared
   constructor-G makes construction/not-taken an artefact of whatever number
   is injected. File/function: `futon2.aif.construction/construct`,
   `futon2.aif.interpretation-construction/construct` (`:evaluate-g` input).
2. **No declared budget or move-cost source.** `construct` refuses
   `:budget-required`/`invalid-input` without explicit `:max-moves`,
   `:max-expansions`, `:move-cost`. Nothing in `judge-opts` or the sources
   declares them; inventing defaults is exactly the "no source is invented"
   rule the H5b comment at `war_machine.clj:7034–7038` states.
3. **Observation contract is stricter than the judge's.**
   `observe-facts` (`cascade_sources.clj:127`) emits `:unknown`; the
   constructor refuses `:observation-required` on ANY unknown token across
   want ∪ needs ∪ forbids ∪ produces. Targets the judge currently processes
   with unknowns would refuse earlier, with a different vocabulary — the
   decline reasons would need mapping or D8's record gap gets worse.
4. **Receipts are optional downstream, mandatory upstream.**
   `cascade-problems/assemble` treats `:interpretation-receipts` as optional
   (`:refused` "optional", docstring at `cascade_problems.clj:196–218`); the
   constructor refuses `:interpretation-receipt-missing` for any pattern
   without a nonempty receipt. 245 of 248 substrate targets fail here before
   any search (§4).
5. **The D3 placement rule.** `cascade_proposals.clj`'s docstring: "Never
   infer applicability or token production from signature prose … proposals
   never populate its executable :candidates … Retrieval is explicit, outside
   the tick." The constructor respects the first sentence (search is over
   declared interpretations, never prose) but violates the second if wired
   in-tick: it populates `:candidates` with `:machine-constructed` receipts.
   That rule has "no ruling found" (E-cascade-real D3) — the wiring needs the
   amendment recorded, not assumed.
6. **Withdrawal is invisible to the machine (the eig case).** Any in-judge
   constructor re-proposes `[:aif/two-layer-calibration]` on every click
   (§3) unless withdrawal becomes typed data the guard vocabulary can carry
   (a `:forbids` the world can make true, a `:status :withdrawn` on the
   pattern that `pattern?`/assemble refuses, or a locator for the
   held-out evidence that admission can observe false). Today it is a
   comment in an .edn file. File: `resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn`;
   commit 8f97757b.
7. **Horizon authority splits.** The judge's common horizon is the sources'
   max else T=2 with authority "p4ng 462aa79" (`war_machine.clj:7053–7059`);
   the constructor ran here at the source-declared 4. A plan unreachable at
   T=2 refuses `:want-unreachable-within-horizon` — wiring must say which
   horizon construction answers to, or construction and scoring will
   disagree about reachability on the same tick.
8. **Duplicate rollout, and refusal-channel plumbing.** `compile-plan`
   already rolls out each order (`interpretation_construction.clj:41–53`);
   `candidate-want-progress` (`war_machine.clj:6549`) rolls the same
   precedence again at admission. Minor compute, but the constructor's typed
   refusals (`:want-already-observed`, `:no-supported-order` + findings) have
   no channel into the tick run record — which is D8's defect ("the run
   record does not say why it abstained") reproduced at the new call site.

## Absence log

- Searched `src/`, `resources/wm/`, `data/wm-cascade-proposals/` for any
  interpretation source other than `resources/wm/cascade-sources/*.edn` and
  the one stored proposal: found none.
- The M-expressions-of-interest source was not run (not one of the four
  admitted targets); noted here so the count in §4 is not mistaken for a run.
