# C125 — era discriminator census

Date: 2026-08-31  
Scope: executable code that chooses which contract a persisted record follows.
Schema labels that are only emitted, but never select a reader contract, are
listed separately.  This pass changes no producer, reader, corpus, or binding.

## Result

There are five executable discriminator families.  Two infer an era from time,
and three read a contract/version carried by the record itself.  Only the R8
time discriminator has the landed-together hazard: it gives the right answer
for the pinned corpus, but cannot identify the contract in general.

| Family | Executable discriminator | Producer/consumer history | Verdict |
|---|---|---|---|
| R2 observation channels | The full per-record ISO timestamp is compared with `2026-05-18T21:33:02.386043914Z` (`checks/r2_channel_contract.clj:18-22,139-148`). Missing time selects the strict current arm. | The producer transition is observable in the corpus: forms 1–2 end at `20:54:12Z` with 13 channels, while the adjacent form 3 at the named instant has 14 channels and `:annotation-health`. The era-aware consumer landed later in `f64ec24f`, so producer and consumer did not land together. | **Sound for this pinned corpus.** The boundary is located by adjacent record shapes, not by a day label. It is still inference rather than self-description; a future producer should stamp an observation-contract id. |
| R8 stored-F/controller-map regime | The trace *filename's day* is compared with `20260714` (`checks/r8_f_contract.clj:12,46-49,150-158`). | Commit `9d8f2dee` landed the producer changes together: trace-schema versions 3 and 6–12, including stored F, selection gain, and controller-map shape (`src/futon2/aif/trace.clj:151-176`). The corpus happens to have a clean gap (latest old July 9, earliest new July 14), so the current census is perfectly separated. | **Contingently correct, unsound as a discriminator.** An old binary writing on July 14, or a new binary writing under another filename date, is misclassified. Replace eventually with a self-declared `:producer-contract :r8/stored-f-controller/v1`, or a documented `:trace-schema-version` threshold plus an explicit unversioned legacy arm. |
| Trace record shape | `[:wm-version :trace-schema-version]`, emitted by `wm-version-stamp` and read by `wm-version-of` (`src/futon2/aif/trace.clj:343-355`). Absence is the explicit pre-v2 legacy case. | Version 2 introduced the stamp (`ac608745`). Versions 15–19 landed in `fd4034c2`, `6c874136`, `d30ea828`, `49d3366f`, and `6b4df8ff`; all five share 2026-08-31, so a date cannot distinguish them. | **Sound.** Producer and consumer landing together is safe because the producer writes the discriminator into the record. The same-day v15–v19 sequence is direct evidence that time would be inadequate. |
| Prediction-error precision | `:producer-contract :prediction-error/v1`; missing fields are `:legacy-era` only on unversioned records and `:malformed` on v1 (`src/futon2/aif/precision.clj:55-68`; producer at `free_energy.clj:128`). | Original production and consumption landed together in `28ce486`; there is no observable time at which one side but not the other existed. C120 added the record-carried contract in `02e2cda`. | **Sound after C120.** This is the canonical landed-together case: no timestamp boundary can honestly be located, while the self-declared tag can distinguish legacy from malformed. |
| R11 replay receipt | Exact `:schema/version` comparison against the supported version (`src/futon2/aif/hierarchical_budget_adapter.clj:94-104`). | Receipt creation and replay share the same declared constant and boundary. Unsupported versions fail loudly rather than selecting a date era. | **Sound.** It is self-declared and closed-world; there is no temporal inference. |

## Labels that are not era discriminators

`:repair/schema-version`, `:morning-brief/schema-version`,
`:event/schema-version`, `:trip/schema-version`, and
`:tripwire-coverage/schema-version` are emitted provenance in the searched
paths, but no current reader uses them to choose between historical contracts.
They therefore are not counted above.  Their labels are useful, but a label
without a consuming branch is not yet an era discriminator.

## General rule and catalogue disposition

The general rule holds: **a record should declare which contract it follows;
the contract should not be inferred from when the record was written.** Time is
acceptable evidence for a legacy corpus only when adjacent records locate the
transition, the corpus is pinned, and missing time selects the strict arm. It
is not a substitute for a producer contract.

This sharpens preemptive-repair class 8, `era-blind expectation`; it does not
need a tenth class. The sharpened defect is **era inferred from a
non-identifying clock**. Its falsifier is a producer/consumer change that lands
atomically, or two schema versions written within the same time bucket: if the
timestamp cannot distinguish them, the discriminator is unsound. The repair is
a producer-issued contract/schema tag with an explicit legacy arm. No catalogue
migration is made in this census pass.

## Reproduction

```sh
rg -n "annotation-health-boundary|default-boundary|schema-era|file-date|producer-contract|trace-schema-version|wm-version-of|schema/version" src checks
git blame -L 18,22 checks/r2_channel_contract.clj
git blame -L 144,198 src/futon2/aif/trace.clj
git show -s --format='%h %aI %s' 9d8f2de ac608745 fd4034c 6c87413 d30ea82 49d3366 6b4df8f 28ce486 02e2cda
```

No migration was performed. The absence-coercion census remains 11; scoring,
selection, trace data, and historical records are unchanged.
