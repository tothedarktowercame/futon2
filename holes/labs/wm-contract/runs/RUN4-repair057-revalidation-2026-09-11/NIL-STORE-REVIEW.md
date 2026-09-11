# Review f339841f: missing admission still compared equal

Original e2a3859d reproduction now refuses. A stronger retained reproduction
uses the real activated/closed cohort produced by the actual historical runner
test, then the real shared admission/click fixture and public historical reader.
An empty repair store, nil repair ID/transition, nil verification evidence,
unverified cohort digest and false successor flag still yielded a historical
bundle classified awaiting-validation/unknown infrastructure with successor=true.

Root cause: supplied nil transition equaled the absent store lookup. Local
correction requires nonblank repair ID and a map transition before equality.
The retained historical-nil-store-review.clj now asserts the exact refusal
:historical-verification-store-mismatch. Actual cohort/checkpoint/close and
reader are unstubbed; historical execution itself uses the existing runner test
port. This is a negative qualification, not the required complete positive run.
Projection tests independently remain 2 tests / 7 assertions green; source
lint and parentheses pass.

Remaining review requirements: pin/reread the cohort preregistration digest,
validate activation and exact close/checkpoint evidence rather than only ledger
summary; join every repair summary/execution field to the canonical admission
and run record. The code currently does not compare :cohort :sha256, ignores
:execution-attempt at the reader, and does not require the projected successor
flag true even though it outputs true. These require real-store positive and
mutation coverage. No complete materialized async fixture has yet passed.

c9025c18 route synthesis is also conditional only on historical selection, not
successful admission. Ensure a refusal or exception is not described by an edge
named verified-admission. Route labels must reflect actual stages executed.

All runtime activity in this review used disposable test roots. No live
capacity, failed attempt, service, credentials or repair evidence changed.
