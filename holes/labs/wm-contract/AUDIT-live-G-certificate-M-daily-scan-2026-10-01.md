# Live G certificate audit — M-daily-scan

Status: bounded audit complete; terminal-record retention repaired; Lean G
certificate not yet claimed.

## Pinned observation

- Run: `2026-10-01-ecf915e4-d43a-44ca-9132-1827fab3ae7d`
- Click: `wm-click-2c53b900-293d-4c6b-9865-bcd54a20826f`
- Record SHA-256:
  `97b94bcfa955821fb100d711b8f04644cd9f83e59ccb288033cc519d414d62b0`
- Selected target/candidate: `M-daily-scan` / `:C1`
- Recorded selector G: `1.0498221244986776`
- Horizon: 4

Do not confuse this selector G with construction search's
`:construction-receipt/:g-of-best` value `19.350775064592945`. They are
different computations at different stages.

## Runtime-to-Lean map

| Certificate field | Runtime producer | Lean target | Live status |
|---|---|---|---|
| cascade and precedence | selected candidate | `GOverCascades.CascadePolicy` | present; construction relation already certified |
| transition/Q steps | `cascade-observation-scoring/score-candidate` rollout | `CascadeEFE` guarded transition / prediction | present in scorer certificate; omitted from the terminal scoring projection before this audit |
| observation model A | class-emission observation model | `CascadeEFE` observation kernel | consumed, but the decomposition census reports `:invalid-class-emission-row` for `M-formal-patterns -> :unknown` |
| preference C | step-indexed class preference | `CascadeEFE` preference kernel | present; steps 1–3 are `:ending/not-yet-evaluated`, step 4 is 11/20, 7/20, 1/20, 1/20 |
| initial D | `{#{} 1}` | initial probability kernel | present, point mass |
| risk | normalized sum of per-step `:risk` | `CascadeEFE.risk` | computed by scorer, formerly not retained in terminal scoring projection |
| ambiguity | normalized sum of per-step `:ambiguity` | `CascadeEFE.ambiguity` | computed by scorer, formerly not retained in terminal scoring projection |
| information gain | per-pattern beta information | `CascadeEFE.information` | recorded as zero here; exact correspondence still needs a theorem/adapter decision |
| total G | risk + ambiguity − information gain | `CascadeEFE.scoreCascade` | scalar present; inputs/terms were not jointly replayable from the terminal record before this audit |

Source pins at audit time:

- `cascade_observation_scoring.clj`:
  `c416f0e06ff0fdd42dd1d206e60f5b9b9f95d1d262d618da82e6cad9c10a17a4`
- `g_term_decomposition.clj`:
  `002c4a44a387e3316a595cdeb5a4b2aa845e9d28907f2e77a01cd7739dc56174`
- `policy.clj` after terminal-retention repair:
  `9e2280c1d8dc1860c3e39f411221967626a97bbc796f63100854d75f822e6444`
- `GOverCascades.lean`:
  `397aa260cd7689931b106c556868622917587ac0385012f38d9d218012598b9c`
- `CascadeEFE.lean`:
  `3ff989c108be155b2d2331e47d78d41f0b585933c226922422b13d100df1df4c`

## Finding and repair

The scorer constructs `:steps`, `:consumed-g`, and `:g-terms`, but
`policy/selection-certificate` retained only `:c`, `:c-source`, and
`:rates-provenance` under `:scoring`. The old run therefore cannot support a
field-by-field Lean replay of its scalar G without borrowing transient state.

The selection certificate now retains the scorer schema, evaluation scope,
observation model, steps, consumed A/C/D/Q, and numeric G terms for every
candidate. Existing fields remain unchanged. This repair affects future run
records; it does not rewrite the pinned historical record.

## Proposed certificate schema

One certificate per candidate must pin:

1. run/click/target/candidate identities and record digest;
2. the certified construction-receipt digest;
3. scorer schema, evaluation mode, scope, horizon, and normalization rule;
4. exact D and every predicted Q step;
5. A and every step-indexed C distribution;
6. raw and normalized risk, ambiguity, and information values;
7. recorded total G and a declared numerical comparison rule;
8. the Lean module/declaration and source/toolchain digests.

The certificate fails closed if any consumed input, step term, normalization
rule, or candidate identity is absent.

## Falsifier

Change one retained step risk, ambiguity, information term, normalization
rule, A row, C mass, predicted Q mass, or candidate identity while keeping the
recorded total G fixed. The generated Lean equality must then fail. A schema
that merely restates the scalar G, or recomputes from unpinned external state,
does not satisfy this certificate.

## Remaining blocker before a positive Lean certificate

The class-emission A consumed by this live run is not yet an instance of the
canonical `CascadeEFE` observation-kernel carrier, and its decomposition
census is explicitly `:missing` because `M-formal-patterns` has the invalid
class row `{:unknown 1}`. The next unit must define and review that projection
or repair the producer; it must not silently map `:unknown` to another class.
