# Independent review of 6b35706e

The two-line correction is sound and necessary. It joins the complete prior
model/revision/run/tick identity to the decoded context; the already-present
prior-to-next identity and tick increment then connect both projected carriers
to that context. The regression is focused on the executed coherently borrowed
prior/next run counterexample. Current source/test pins match
`lead-correction-pins.json`, and the retained changed-source run reports 16
tests / 91 assertions with clean kondo and explicit-path parens. I did not rerun
those accepted gates.

Acceptance is narrow: the codec remains structural and grants no authority.

## Remaining structural finding

A targeted isolated control at the reviewed bytes coherently changed the
decoded E2b-subject and outcome `:run/id` to `"borrowed"`, updated their exact
source/value pins, and left decoded context at `"e1-authority-run"`. The codec
still returned `:structurally-projected` (see
`independent-source-identity-control.stdout`). Current joins cover their
occurrence/action/class but not their complete model/revision/run/tick
identities. Thus 6b35706e repairs the reported prior/next regression, but a
future bounded codec repair must join every identity-bearing decoded source to
context and bind outcome-review/lifecycle subjects without weakening the
structural/no-authority boundary.
