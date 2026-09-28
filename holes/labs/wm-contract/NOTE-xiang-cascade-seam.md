# NOTE — M-象-cascade and the War Machine: what the receiving side needs

claude-8 (War Machine receiving side) to claude-17 (owner, M-象-cascade), 2026-09-28.
Joe agreed on 2026-09-28 19:30Z to a sixth completion criterion and to this note being sent.
Read against `futon3c/holes/missions/M-象-cascade.md` as drafted (Status: IDENTIFY) and futon2 at 867a5619c.
I did not read `xlate.py`, the 象 brief or M-象-2000.

## The gap, checked at the source

- `interpret-pattern` (`src/futon2/aif/cascade_model_manifest.clj:97`) splits IF, HOWEVER and THEN text into tokens. Its callers are `shadow_cascade_g.clj:78` and tests. Selection does not call it. The mission's statement is accurate.
- The machine gets its rules by two routes, both written by an agent: the files under `resources/wm/cascade-sources/` (5 today), and the interpretation seam (`want_interpretation.clj`, `interpretation_request.clj`, `interpretation_job.clj`), where an agent answers a request at click time and the retrieved candidates are marked unjudged.

## Proposed sixth completion criterion

> **6. The machine can load it.** One induced rule is admitted by the War Machine's loader and appears as a candidate in a plan-only run (`FUTON_WM_FLIGHT=plan`, `scripts/wm_scheduled_run.clj`). No flight, and no change to the cascade decision.

Without it, criteria 1 to 5 can all pass with no rule the machine can read, and "Feeds the War Machine" stays a relationship with no test.

## What the loader requires today

`src/futon2/aif/interpretation_evidence.clj`, `interpretations!` (lines 172-197), for a record of schema `:wm/interpreted-pattern-set-v1`. Each interpretation has exactly these keys: `:pattern :source :membership :clauses :guard :effect :authority :author :sha256`.

| Requirement | Where | Consequence for an induced rule |
|---|---|---|
| S1. `:clauses` has `:if`, `:however`, `:then`, each a citation into the pattern's own source | lines 184-188 | the pattern must exist in the library as text with those three clauses; a rule with no library pattern behind it cannot be cited |
| S2. `:guard` is over facts declared in the record's `:facts`; `:effect` maps declared facts to booleans | lines 189-193 | pre-facts and post-facts must be declared facts, and the rule's produces must be stated as fact -> true/false |
| S3. Every fact needs a locator the machine can evaluate | `observation_checks.clj` `locator-refusal` (classes C3 to C6 and C8); a fact that cannot be measured is the failure kind `:interpretation/unmeasurable-fact` | facts read from the evidence store, bells or parks need a locator class; I found none in `observation_checks.clj` that reads the evidence store |
| S4. `:authority` must equal `:documented-interpretation` | line 194 | see below |
| S5. `:sha256` is the digest of the interpretation without that key; `:author` equals the record's author | lines 195-197 | the stored rule is loaded as stored; the machine does not re-derive it |

## Owed by the War Machine side (mine)

- **An authority value for an induced rule.** The loader admits only `:documented-interpretation`. An induced rule labelled that way would misstate what it is, and the mission's 象/释义非授 says it is a hypothesis until it predicts. I will add a value for it on the machine's side, with the held-out counts carried beside it, once you say what the rule record holds. Until then criterion 6 cannot be met without mislabelling.
- **A locator class for the evidence store**, if S3 needs one. Tell me which facts the go-ahead family uses and where each is read from.

## Three risks, for your judgment

1. **The first family.** The go-ahead family describes what the operator does. In an unattended flight no operator turn exists, so a guard of "an offer is pending and the operator approves" never holds. It is a fair first test of induction because it has the most turns. A family where the operator's turn corrects agent work would give guards on the work itself, which the machine can use when Joe is away.
2. **Merges and splits.** The machine's receipts cite pattern ids and pin the library by hash (`interpretation_request.clj`). A merged id must stay resolvable, as an alias, or old receipts stop resolving. On 2026-09-28 a rule added later made an older cohort record unreadable (futon2 d49365b88, corrected at c91c647ae); this is the same fault from the other side.
3. **Proxies.** A rule induced from a proxy fact should carry that mark into the record the machine loads, so that the machine does not treat it as observed.

## What the machine gains

- a starting estimate for θ with hit and miss counts, where hand-written rules say "documented default";
- hierarchical lookup in place of the flat retrieval in `interpretation_request.clj` (40 by embedding, 8 by index);
- fewer rules written by an agent during a click.
