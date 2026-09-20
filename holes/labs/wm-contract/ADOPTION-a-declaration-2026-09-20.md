# ADOPTED — the first A declaration (Joe, 2026-09-20, late evening)

Joe, on reading DECLARATION-a-draft-2026-09-20.md (6ae06134): "I am OK
to adopt it so that we can get it moving. This will presumably not be
the final word on A but it can give us something."

Effect: the draft's contents are the declared observation model for
judgement tokens — three channel rates (0.14 / 0.09 lower-bound /
0.25 frame-grain), default fp 0.15 / fn 0.05 (fn marked weakest), one
coupling parameter z ~ Bernoulli(0.15) with ~22x conditional ratio,
:z-semantics :per-step-redraw. Checkable channel stays rate-zero by
theorem. Not the final word: revisions are follow-up declarations
citing new measurement records, per the draft's own revision clause.

Wiring notes: attaching the rate to the live J locator and forwarding
observation-model/rate inputs through cascade-decision's scoring opts
are build items (the forwarding is recorded prerequisite #1); adoption
declares the model — it does not by itself move any live verdict, per
the draft's "what adoption does not change".
