# Row 14 live-wiring refusal — missing production policy/model authority

Date: 2026-09-14. This is a source-backed stop, not a live-mode flip.

## Verdict

The requested configuration flag cannot truthfully be enabled yet. The
already-reviewed `compute-efe` option accepts a *fully constructed* provider
and C. The live judge has neither the inputs nor the authority needed to build
that provider for its complete ranked domain. Adding a Boolean and filling
those missing inputs with the row-6 demonstration fixtures would promote
fixtures and the explicitly rejected positional A content into production.
That violates the row-14 implementation order and the machine-model contract.

No source or production run-sheet was changed.

## Exact gaps

1. **A is not admitted for live use.**
   `machine_predictive.clj:78-92` constructs
   `wm-state-outcome-prior-v1` by assigning the seven states to outcome
   positions. The row-14 discovery explicitly says positional order is not
   acceptable content and makes the reviewed A artifact and its model assembly
   packets 1 and 2 prerequisites (`TN-row14-discovery-2026-09-12.md`, section
   4). `WORK-REMAINING.md:288-320` likewise records packets 1/2/6 as remaining,
   despite packets 3/4/5 being done. This task excludes packets 1/2, so this
   packet cannot manufacture their authority.

2. **The judge ranks actions, not complete versioned row-9 policies.**
   `machine-predictive/predictive-outcome-kernel` requires a belief input, a
   controlled kernel, and complete policies (`machine_predictive.clj:96-128`).
   Each policy must name an entity and model revision and contain a nonempty
   vector of actions (`:13-30`). The packet-4 test supplies these explicitly in
   a two-policy fixture (`test/futon2/aif/efe_machine_q_test.clj:38-61`). The
   live judge passes ordinary WM candidate action maps to
   `efe/rank-actions` (`scripts/futon2/report/war_machine.clj:6395-6444`); it
   does not possess a versioned full policy-plan table or a reviewed mapping
   from every candidate occurrence to such a plan.

3. **The reviewed controlled kernel does not cover the live action domain.**
   The retained row-8 authority declares only `:advance-mission` and
   `:apply-cascade` (`runs/row-8-controlled-transition/production-match.edn`,
   `[:real-inputs :action-snapshot :classes]`). Its constructor refuses an
   undeclared action (`machine_transition.clj:38-46`; row-9 refuses at
   `machine_predictive.clj:27-30`). A retained real ranked record contains
   `:no-op`, `:address-sorry`, `:fire-pattern`, and `:learn-action-class` in
   addition to `:advance-mission` (for example
   `runs/2026-09-04-010-accepted/rationale/rationale-2026-09-04-7923ae0d-4514-40d9-8901-6e22995f5b94.edn`). Mapping these classes silently to either of the two
   admitted transition actions would invent model semantics; refusing them
   would make the required “every ranked action” enabled full-judge witness
   impossible.

4. **A config Boolean cannot supply the absent objects.**
   `c-fold-config/resolve-opts` currently materializes a pinned C fold from
   exact artifacts (`c_fold_config.clj:30-65`), and
   `configured-fold-efe-opts` forwards that resolved object
   (`war_machine.clj:5972-5977`). A machine-Q configuration boundary would need
   to resolve, from pinned reviewed artifacts, at least the admitted model/A,
   controlled transition kernel, post-carry entity belief, complete
   candidate-occurrence-to-policy plans, and machine C. Merely stamping
   `:machine-q-enabled? true` would attest to behavior the judge cannot perform.

## Smallest valid next packet

Resolve the already-recorded prerequisites rather than weaken them:

1. land and review row-14 packets 1 and 2 (measured A artifact and admitted
   model assembly);
2. extend the controlled transition authority or provide a reviewed total
   mapping for every action class in the actual ranked support;
3. add a pinned, versioned, complete candidate-occurrence-to-full-policy-plan
   artifact tied to the same model/run/entity belief;
4. only then add a default-off `:machine-q-enabled?` resolver that constructs
   the real provider and C, run the redirected full-judge positive and disabled
   identity witnesses, and flip the production sheet in its own commit.

The positive witness must refuse until all four authorities agree. The
configuration key proposed for that future boundary is
`:machine-q-enabled?`; it was **not added** here because no valid enabled value
can currently be materialized.

## Pins

Inspected futon2 tree: `ec5b161b4b53c80afed23d94f761266181c1382d`.

| artifact | SHA-256 |
|---|---|
| `src/futon2/aif/machine_predictive.clj` | `0bf8d72bd7017a16751c44d343cb2a928902af9793de3f953e20c370fac7f59b` |
| `src/futon2/aif/machine_transition.clj` | `697b54db1a9f40844f9feff3622f95414dfcd3d019a7469272fc5d9a109ab6f8` |
| `src/futon2/aif/c_fold_config.clj` | `b1e743a6b14152faf59e6604c60ef2a0ab54242f919303729ed9bd854c4b5605` |
| `scripts/futon2/report/war_machine.clj` | `50d02bb7179087597f468f361e16206d93400b1e410e27ce1c261bdde801981f` |
| `TN-row14-discovery-2026-09-12.md` | `c9694e660b62bd5c5271ec4293fc3f023b96b8137b388817b219676ecbbc6a0a` |
| `WORK-REMAINING.md` | `873024341532a544e2be3c2eb9c5f54253410bc8c589817f4730eabd18e1b9ac` |

No tests were run because this refusal packet changes no executable file.
