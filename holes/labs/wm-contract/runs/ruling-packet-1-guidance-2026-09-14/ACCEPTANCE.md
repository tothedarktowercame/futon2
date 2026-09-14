# Acceptance — ruling packet 1: guidance + subject capture (81ce8543, receipt 52039f83)

Reviewer: claude-15, per the coding-handoff protocol. Author: codex-24
(job invoke-1789425762895-20894-0d03f008).

VERDICT: **ACCEPTED with one reviewer fix applied in-lane** (d1ff5aab).

What I checked (diffs read; receipt validated, not re-run):

- GUIDANCE: the deposit instruction now directs a repair target's subject
  pair to T's ORIGINAL obligation-record bytes (:before) and its honest
  state-at-attempt readback (:after, still :open), named subject-*.edn
  with :entity/id = the exact repair id; source-file pairs are demoted to
  supporting-*.edn. Matches ruling §3 exactly.
- ROLE VALIDATION: validate-revision-evidence-role is fail-closed on the
  filename convention — subject-* with a foreign :entity/id refuses
  :subject-entity-mismatch; supporting-* admits; ANY other name refuses
  :revision-role-ambiguous. Note (accepted consequence): prior habitual
  names like entity-runner.edn now refuse at close — with the containment
  boundary that yields a typed 007, and the updated author guidance is
  what prevents it; fail-closed is what the ruling ordered when the
  relation cannot be expressed.
- READBACK CONTROL: standing-decision-readback annotates a :resolved
  claim whose contract requires a production successor when the store
  read returns nothing (:resolution-unsupported-by-store) or errors
  (:resolution-store-unreadable), without changing any verdict or
  blocking any close; the resolution-read seam is injectable, defaulting
  to the immutable resolutions/ file of the exact repair id.
- TESTS: subject/supporting/ambiguous role cases; readback annotation
  present without a store record and absent with one; fixture stores and
  temp dirs only — no live store, JVM, or prereg touched (receipt
  confirms). Receipt 52039f83: kondo 0/0, parens PASS, fresh-JVM 178
  tests / 946 assertions across both namespaces, single run, pins listed.

REVIEWER FIX (in-lane, d1ff5aab): the annotation was computed and then
DISCARDED — valid-standing-decision? reduces the readback to a boolean
and nothing persisted it, so the store fact was invisible to exactly the
later observers the ruling names. ensure-standing-decision! now returns
the annotated readback and the runner threads
{:entity/id :decision :decided-by :standing/store-annotation} into the
grounded close as :standing-readback. Unit tests pin the returned
readback; the close-map threading is a mechanical assoc whose end-to-end
pin belongs in packet 2's measured-close tests (recorded here so packet
2's acceptance checks it). Observer-view builders must include
:standing-readback in close-conditioning from exercise 6 on.

Gates after fix: check-parens OK, kondo 0/0, fresh-JVM runner suite 170
tests / 920 assertions, 0/0.
