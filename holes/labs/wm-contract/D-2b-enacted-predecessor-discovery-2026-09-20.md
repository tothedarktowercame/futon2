# D 2b: selected construction is not executed B

Discovery after the receipt freeze (`c42b780d`), before consumption edits.

The authorized rule requires B from a previously selected **and enacted**
cascade, not a candidate prediction. The inspected production path does not
yet establish that association:

1. `receipt_construction.clj:443–499` validates previous construction,
   occurrence, manifests, digests and admission history. It returns
   `:acting-order-after` from the prior cascade diff. That order is produced
   by the `acting-order` simulation (`:49`, called at `:570`), not by an
   execution record. `previous!` (`:499` onward) is a strong construction
   history validator, but that does not give the simulated order execution
   authority.
2. `full_loop_runner.clj:4079–4107` writes the judgment trace before forming
   the construction checkpoint. Its `:selection-enaction` calls
   `selection-enaction-record` with the selected action in both positions
   and `{:source :runner-selection}`. The helper (`:1632`) explicitly
   describes entry into construction. It is not post-execution evidence
   of a token transition.
3. `war_machine.clj:5918` labels the lane result an enactment **plan**;
   `:5979` retains the claim `:enactment-plan` with independent checking
   required. Treating that value as executed B would change its authority.
4. `machine_enactment_correspondence.clj:88–98` provides a separately pinned
   selection/enactment verifier, but explicitly refuses production mode
   with `:e2b/production-authority-unavailable`. Isolated fixture success
   cannot authorize production.
5. Enumerating callers of `enact/close-loop!` in `src` and `scripts` finds
   the older `scripts/wm_scheduled_run.clj:108`. It records constructed
   wiring from the classical engine, not the full-loop's executed token B.
   `deps.edn` assigns the authoritative scheduled route to `full-loop-cli`
   and retains this older entry under `:wm-judgement-only`. Restoring that
   route as a source of production authority is not part of D wiring.

Searches covered `src/futon2` and `scripts/futon2` for enactment/realized-outcome
producers, and all `src`/`scripts` callers of `close-loop!`, `futon2.aif.enact`,
and `selection-enaction-record`. Trace persistence of optional enactment
fields is a reader capability, not evidence that the current producer writes
an admitted enacted predecessor.

## Consequence and next structural decision

The authorized `:carry-no-predecessor` branch can truthfully record these
failures and initialize fresh. It cannot be called successful live posterior
consumption if all production predecessor inputs take that branch.

Before implementing that claim, identify an existing production enactment
authority that binds a completed execution to the same prior token-carry
occurrence and token transition, or add that producer/validation contract.
Positive acceptance should exercise the real producer and reader; a matching
selection/construction record without enactment must be the negative control.
No hypothetical rollout, self-comparison, isolated-test authority or older
runner is substituted for executed B.

The schema remains frozen and useful to Q. No 2b consumption code, live
reload or new actuation was performed during this discovery.
