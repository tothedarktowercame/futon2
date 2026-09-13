# Independent offline join acceptance

Reviewed source 31fa78a8 with final tests 35e5b8ef and receipts 2a071ff8. All six source-pins.sha256 entries match current bytes, including the exact pinned F11 close. Retained raw gates show 4 tests/18 assertions with zero failures/errors, kondo zero warnings/errors and explicit three-file arxana-check-parens-cli OK. No passing checks or discovery runs were repeated.

The constructor resolves the context and its exact close/annotation pointers, reads their pinned bytes, revalidates the raw annotation through the accepted resolved-subject validator, and checks close entity/run/cohort/attempt/sequence/time joins. It emits a derived artifact with untouched close record/source and separate disposition/acquisition fields. Caller-shaped qualified objects are not accepted as authority. Duplicate/conflicting annotations at the same supplied authoritative point refuse.

Accepted only as an offline mechanism under an independently trusted configured context/resolver. No real observer/reviewer/context acquisition or production trust configuration is established. The F11 discovery remains absent for outcome entity, entity-state-at-close, run identity, annotation authority and context authority; zero labels/pairs/counts result. Annotation is a fallible supervised acquisition method, not physical truth. Distinct source contexts/cutoffs must still be adjudicated by that external authority; this helper does not establish their uniqueness or selection policy.

Real acquisition, prospective capture source/run identity and integration remain open. Next independent work is the still-required rejecting full-certificate predicate; moving the worker does not waive these Row 14 obligations.
