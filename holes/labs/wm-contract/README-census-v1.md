# War Machine trace witness census v1

This census reads the 52 dated files matching
`data/wm-trace/wm-trace-*.edn`. They contain **791 top-level EDN records**;
that is the denominator for every family. Record indices in the EDN artifact
are zero-based. The hidden lane index and non-EDN shadow-step file are not
trace records and are excluded.

| family | witnesses | records checked | files touched |
|---|---:|---:|---:|
| 1 identity threading | 0 | 791 | 0 |
| 2 non-empty handle | 88 | 791 | 5 |
| 4 durability before fold | 88 | 791 | 5 |
| 5 declared domain | 88 | 791 | 5 |
| 6 separation of powers | 31 | 791 | 7 |
| 7 exit / status pins | 4 | 791 | 1 |

Family 1 is the sole zero-witness family. Family 7 is the sole single-digit
family, with four records in one file. All four records carry both score keys,
but every `:coverage-score-delta` value is `nil`; the count measures field
presence as requested, not satisfaction of the proposed numeric pin.

The field shapes differ materially from a flat-key reading of §2.1b:

- A realized outcome is `[:realized-outcome]`; its numeric-or-absent leg is
  named `[:realized-outcome :realized-G]`, not `:realized` or `:outcome`.
  Of 88 handles, 85 realized legs are numbers and three are `nil`.
- The only tick key in any record is `[:realized-outcome :tick]`. There is no
  record-level `:tick`, so no record witnesses both identities required by
  family 1. `:timestamp` exists, but it is not named or represented as the
  contract's tick and was not silently substituted for one.
- The enacted identifier is `[:enactment :mission]`, not an enacted
  `:policy`. `[:realized-outcome :policy]` also exists on the same 88 records,
  but family 5 was counted from the enacted mission.
- Author and reviewer are nested at `[:wm-version :author]` and
  `[:wm-version :reviewer]`.
- The paired family-7 fields occur inside one candidate at
  `[:ranked-actions 112 :action :act-gate]`, rather than as record-level keys.

The corpus corrects the incident date in `E-R8-red-ring-fill`. The final
record carrying `:realized-outcome` is record 12 of
`wm-trace-2026-07-06.edn` at `2026-07-06T12:04:27.412283747Z`. The immediately
following record—record 13 in that same file, at
`2026-07-06T13:04:30.639215623Z`—is the first record after which the field
never appears again. Therefore the disappearance date established by the
corpus is **2026-07-06**, not 2026-07-09. July 9 is the next dated trace file,
but the omission had already begun within the July 6 file.

The earlier per-file table and this census use different units, so its values
do not constrain these per-record counts. Its file-presence estimates do agree
with the relevant witness spans: five files for realized outcomes/enactments,
seven for author/reviewer, and one for the paired score keys.

---

## REVIEW CORRECTION — claude-13, 2026-08-27

**The unit was wrong, and the numbers above are inflated by roughly 29×.**

Each `wm-trace-*.edn` file is **one top-level map = one tick**, not a sequence of
records. The census walked nested structures — `:ranked-actions` alone holds
**6,656** entries across the corpus — and counted those as records, giving "791
records" and "88 witnesses". Recomputed at tick level:

| family | census-v1 | **corrected** | of |
|---|---:|---:|---:|
| 1 identity threading | 0 | **0** | 52 ticks |
| 2 non-empty handle | 88 | **3** | 52 |
| 4 durability | 88 | **3** | 52 |
| 5 declared domain | 88 | **3** | 52 |
| 6 separation of powers | 31 | **7** | 52 |
| 7 exit / status pins | 4 | **0** | 52 |

`:realized-outcome` occurs in exactly **three** files — `2026-07-03`, `-07-04`,
`-07-05` — and each is a **single map**, not a collection:

    {:policy "M-bayesian-structure-learning", :expected-G -0.2,
     :realized-G -0.5, :tick 1783235004856}

Family 7 is 0 rather than 4 for the reason the census itself gives: every
`:coverage-score-delta` is `nil`, so no gate is fully witnessed.

**What survives, and it matters.** The census's headline finding is **correct in
corrected form**: the realized outcomes stop *before* the 2026-07-08 producer
substitution. The last is **2026-07-05**; `-07-06` onward carries none.
`E-R8-red-ring-fill` dates the break to 07-09. The census's "record 12 → 13 of
`wm-trace-2026-07-06.edn`" is an artifact of the flattened walk — that file has
one record — but the direction of the correction stands.

**Discrepancy to resolve, not to assume settled.** `E-R8-red-ring-fill` states
the producer *"fired **88 times**, 2026-07-02..07-06, in `futon2/data/wm-trace/`"*
— the number it used to overturn its own premise. **The corpus contains three.**
Either the 88 came from another artifact, or the files have been rewritten, or
the count measured something else. This needs settling before either number is
cited again; it is the same shape as the wrong-corpus error that excursion itself
coined a ticket for.

**Consequence for Tier 0.** The trace corpus exercises the central families
**three times**. A contract validated over it would be almost entirely
unexercised. That does not sink Tier 0 — a **retro-trip needs a boundary, not
volume**, and three present plus forty-nine absent dates the break precisely —
but the qualification record must claim dating, not coverage.

Verification script: `/tmp/c13/census2.bb` (tick-level, `clojure.edn` with a
`:default` tag reader — the traces carry `#object[…]` literals, which is itself
worth noting against family 3, since such a record cannot be reconstructed).
