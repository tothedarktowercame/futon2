# Successor integration: existing selection already permits normal work

The independent acceptance of c6ed9f59/03884e80 stands. The claim that an
admitted historical verification is continually selected is not supported by
the current runner: stop-line filters status :open; historical admissions are
returned as :awaiting-validation and enter validation-lines instead.

Retained isolated actual run-opportunity! reproduction gives:

    {:historical-candidate-calls 0, :dispatches 0,
     :selected-action {:type :open-mission :target "M-selected"},
     :outcome :agent-unavailable}

Reviewer availability was intentionally absent to stop before dispatch.
The supplied status matches repair/open-obligations' verified-admission output.
This proves selection behavior only, not production successor completion.

The actual unresolved seam is full_loop_runner.clj :stop-line-validation:
all validation-lines call ordinary repair/resolve! before adjudication/close and
before durable run projection publication. Historical admissions have no
:repair/implementation, which the ordinary machine-failure resolver requires.
The existing run4-historical-successor/resolve-from-durable! is the intended
separate authority and cannot truthfully run before its durable bundle exists.

Implement typed deferral of historical validation lines through the ordinary
successful run path, retaining canonical repair/verification/execution identity
as provenance. After actual durable terminal publication, a server-owned
explicit successor operation must reread that evidence and call the existing
historical-resolution adapter. Do not fake a new implementation or remove the
validation obligation. Ordinary implementation-backed validation is unchanged.

The historical trial remains a non-task observation; a separately frozen
eligible task and fresh admission/cohort capacity are required for its successor.
Link those two trials with reviewed server-owned provenance rather than marking
the historical trial successful. No new selector action is justified solely by
the reviewed claim above. Positive integration must execute ordinary successor
validation and then durable resolution in that order, with same-attempt,
missing/failed bundle, replay and identity-drift refusals.

No live capacity, attempts, services, credentials or failed records changed.
