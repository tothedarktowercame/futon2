# Correct required loop node unexercised

**Status:** OPEN

## Observed failure

Target: not retained; stage: :selection.

Context: {:phase :selection, :decision-context {:elided/reason :exceeds-durable-finding-budget, :elided/type :map, :elided/count 9, :elided/keys [:abstention :action :candidate-count :candidates :chosen :decision-status :policy-count :refusals :selection-law]}}.

Required loop nodes were not exercised by selection

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance evidence

Retain a reproduction or regression check that detects the reported failure and passes after the change, together with the scoped validation output. Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-d276a49116c14bd53fd025b0e320c7cb5bb6db344890caa334d11addd0fb5ba9](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-d276a49116c14bd53fd025b0e320c7cb5bb6db344890caa334d11addd0fb5ba9.edn)

Finding SHA-256: `ccd1e271ce86d4bc23aa77a4a9649b00a0df4f046d92488254e13b5a792824a6`
