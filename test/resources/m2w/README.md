# M2w fixture and execution receipt

Base: c4416cc30. `action.edn` is a verbatim select-keys projection of
`/home/joe/notes/M2v/decision-3-0.edn` at `[:decision :action]`, retaining
`:kind :target :want :declared-wants :observation-locators`.
`mission.md` was copied from futon5a/holes/missions/M-war-machine-wiring.md;
SHA256 f75873416ac1f328698a2d2292aa5515366bf722ebc232e2098d61ea847a4cdf
matches the retained action pin and is asserted by the test.
The test obtains facts through real mission-source on these bytes. It does not
invent a universe. The historical action without the new field remains unavailable.

Red: new namespace alone, 4 tests / 25 assertions / 14 failures / 0 errors.
Failures include missing domain inputs, initialization, universe, six reader calls,
typed absence and source universe. The base test temporarily used ns-resolve to
leave the old decision unchanged when the new helper was absent; the final test
calls the helper var directly. An earlier setup run omitted mandatory minting
capabilities; corrected before this red receipt.

Green final command:
clojure -X:test :nses '[futon2.aif.outer-policy-token-stage-test futon2.aif.selected-outer-cascade-policy-test futon2.aif.d-predecessor-task-authority-test futon2.aif.wm.family-selection-test futon2.aif.wm.outer-action-declared-wants-test]'
40 tests / 245 assertions / 0 failures / 0 errors, exit 0.
The first green run (four namespaces, 37/229) also passed; final replay followed
strengthening the wrong-target pin check and adding the existing sealed-handoff
namespace to validation. No suite run.

clj-kondo on the two source files and new test: 0 errors / 0 warnings, exit 0.
check-parens on those files: OK, exit 0. git diff --check: clean.
Logs are in receipts/ and /home/joe/notes/M2w/.

## Behavior and identity

family_selection/declared-mission-wants carries the mission-source universe
inside declared-wants, before bind-ranked and menu sealing (family_selection.clj
367-445). selected_outer_cascade_policy/decision calls with-token-stage after
verifying the carrier. It uses scoring-input-receipts/initial-belief (19-27),
with origin assembled-target-facts, and token-belief-carry/stage. The initialization
receipt also lives on the decision where scoring-input-receipts reads it.
The occurrence string is the sealed carrier SHA with outer-selection/ prefix;
it identifies selection provenance, not a minted execution or an observation.
Missing/empty/invalid facts and missing wants yield token-belief-stage-status
unavailable, with no token-belief-stage key. This preserves contains?-based
stage validation. The source and pin paths are checked against the action target.

No declared-wants key-set whitelist was found in src. New action content changes
menu-pin and selected carrier-pin, intentionally: they cover the new fact bytes.
selection-cascade-identity/bind-entry (83-150) retains the action with assoc-in;
its executable identity uses the arrangement and receipts-for (65-74), not
these mission facts, so cascade-digest is unaffected. Existing sealed-menu,
selected-carrier and compiled-handoff checks pass in outer-action-declared-wants-test.
Old sealed actions are not rewritten and get an explicit missing-universe finding.

## Stage reader audit

- d_predecessor_task_authority.clj:402-420 and 92-164: exercised through context,
  capture and artifact-tokens; all six real carrier locators reach stub C4 readers.
- token_outcome.clj:65-106: exercised; still prediction-input-unavailable and
  model-id nil. No precision-family, q0 or horizon is introduced.
- scoring_input_receipts.clj:89-136: the exact valid-stage? check is exercised
  against the decision's initial-belief-receipt. Its independent initial-belief
  recomputation uses the same facts. No claim that unrelated full-record habit
  validations now pass.
- token_belief_carry.clj:73-86: exact stage regeneration passes in the new test.
- revision_scanner.clj:46-62: wants can be read, but it additionally requires a
  recorded Q trajectory; this change supplies no trajectory.
- flight.clj:517: interpretations remain {}, so this does not authorize a measured
  F update or supply A/B. It still requires measured-a and observation inputs.
- preference_audit.clj:45-80: gains declared wants as record-only audit input;
  it still requires actual policy C/scoring provenance, which this stage cannot
  create. No scoring inputs or selection law are changed.
- run_narrative.clj:239,308,393: descriptive domain/want/interpretation reporting
  now sees one domain and empty interpretations; no decision or scoring authority.
- temporal_update.clj:80-97: requires a token-belief-input v3 or retained temporal
  predecessor to authorize initialization. Neither is supplied here.
- token_belief_predecessor.clj:132,227: remains the existing v1 stage semantics;
  decision does not call it or claim an admitted predecessor/observation update.
- candidate_derivations.clj:27-38 s0-of accepts the point-mass map shape, but no
  candidate derivation is built by this change. wm/cascade_decision.clj:1033-1040
  continues to mark the retired interpreted route unavailable.

No shared-JVM reload, click, scoring change, or live-store write.

## M2y-1 fixture extension

The action now additionally carries :precedence, unchanged from the same 90ca
source's [:decision :action :precedence]. All original projected values remain
unchanged. precedence.edn pins this exact 27-unit value separately; the new test
asserts its SHA256 and equality to the action field. Source and projection hashes,
execution logs and equality receipt are under receipts/m2y1/. Prediction remains
:prediction-input-unavailable, :model-id nil, with no :precedence-finding.
The two requested namespaces ran once: 8 tests / 58 assertions, all passing.
