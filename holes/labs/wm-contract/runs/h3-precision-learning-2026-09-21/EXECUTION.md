# H3 policy precision learning — execution receipt

Implementation: `991e27a4170983c17e33b80c73fea5f4cd88d5c3`. Author: codex-6. Review owner: claude-12.
Discovery: `3e9a3141`; observation-clock premise correction: `7088a238`.
Authority: five-increment authorization and explicit observation-schedule amendment.

## Result and scope

The joint WM selector now consumes persistent rate state by default, with
`gamma = 1/beta`, `tau = beta`, and `:tau-source :carry-beta`. There is no
engineering-gain switch needed to activate this path. Initial declaration rates
must agree within their context; disagreement refuses instead of overwriting.
The existing cascade updater now consumes `:beta-posterior`, handles finite and
positive-infinite F through one numerical solver, and retains habit E in both
policy distributions. Its pi/pi0, residual, G/F/E and excluded support survive.

The scorer exposes its actual q0/rates/horizon/preference spec. Selection freezes
those inputs and the actually consumed policy E/G. The existing pre-dispatch D
capture retains this family, validates its selected action against the minted
occurrence and binds the dispatch bytes into author/reviewer prompts. Independent
task verification validates the family and its selected declaration's observation
schedule. This is a new forward capture, not a retrospective inferred R6 join.
The existing candidate-to-minted R6 field remains `:not-established`; E1/E2b
portfolio authority is neither replaced nor claimed.

Next-tick intake uses that verified family and declared event time to evaluate
F over the OLD menu. Unknown coordinates are marginalized exactly. Positive-
infinite F has zero posterior mass; all-impossible evidence holds with a model-
contradiction record. Context/model changes hold the last valid rate with an
explicit reinitialization record. Carry is sealed, serialized inside the ordinary
decision trace, and checked on consumption. The occurrence consumption set
prevents repeated learning, including when a record digest changes. Abstention
preserves the prior learned rate. Malformed persisted rate state refuses.

The separate `:observation-schedule {:tau {:value n :status :declared}}` field
is validated and retained per target. Present malformed values refuse at the
loader; absent placement holds. The event must lie within the frozen horizon.
No existing declaration was given a timing. Authoring a placement remains a
separate deliberate act; all four inspected production declarations still lack it.
Consequently this landing does NOT assert a real live learning occurrence or
mark the Lean H3 hole closed. The default path is built and tested, while current
production observations cannot be used to learn until placement is authored.

**a converged solver that the wrapper discards does not demonstrate learning.**

## Controls actually executed

- Wrapper finite-evidence regression consumes beta approximately 0.8120678866;
  opposite evidence moves it above 1. Constant evidence holds; adding a constant
  to F leaves the answer unchanged. Nonuniform E enters both distributions.
- Positive-infinite F receives exactly zero posterior mass; all-impossible F
  records model contradiction. Missing/invalid F, NaN/negative infinity,
  invalid carried state, duplicate identities, no finite G support,
  nonconvergence and a nonstraddling bracket have distinct outcomes.
- Partial-event product probability equals explicit complete-observation
  enumeration on a seven-token carrier. Unknown coordinates are not false;
  malformed rates on unknown coordinates are still rejected.
- Declared tau changes the prediction; out-of-horizon and unmeasured events
  refuse/hold. The original malformed schedule probe is now a real loader
  negative control. Preference placement does not set observation timing.
- Candidate reordering preserves learning. Tampered family/state, mismatched
  selected occurrence, replay, changed model and changed record digest are
  exercised. Actual trace write/read preserves rate and consumed identities.
- A real temporary Git task, produced artifact, verifier and persisted exact
  reader authenticate a newly bound family and yield an informative update.
  Raw Agency job fixtures bind the new dispatch hash; old prompts cannot
  authenticate a family appended afterward. This is a constructed control,
  not a live author/reviewer job or production event.
- The real selector consumes the learned rate with equality checks and changes
  policy probabilities. The real joint decision, against temporary stores,
  records initialized carry and missing-predecessor hold. An abstention control
  retains learned state rather than silently resetting it.

## Validation and exact scope

Final validation used `/home/joe/code/futon2-h3-validation`, initially detached
at `38b4c95c40ff5f6d2ff08636acce61e29322a3e6` plus ONLY the H3 patch. `precommit-pins.json`
records the exact bytes; every pinned source/test file matches implementation
commit `991e27a4170983c17e33b80c73fea5f4cd88d5c3`. After commit, that owned checkout was reset to
this implementation and the six namespaces were registered separately.

Both final roles agree: **71 tests / 402 assertions / zero failures/errors**.
No full suite was invoked. `validation-comparison.json` records each role,
base/implementation SHA, log hash, count comparison and warrant. The six registered
warrants are the post-commit runs, not additional retries of those runs.

| Namespace | Tests / assertions, each role | Registered warrant |
| --- | --- | --- |
| futon2.aif.cascade-beta-update-test | 8 / 39 | test-registry-14655f9ca5b0a6222916f3f43293433cbe6a2815801ba2c19f3f17c16b0f9027 |
| futon2.aif.policy-precision-test | 25 / 138 | test-registry-18973ec27ef8e7e3f204f73c30eb0ca8b1a395213e0bd248c7ca21e4ae019656 |
| futon2.aif.cascade-sources-test | 8 / 33 | test-registry-2d380fd33beaae81b70f16d954a501835849b5a3b05455b0fcc633c9689f211c |
| futon2.aif.d-predecessor-task-authority-test | 8 / 41 | test-registry-9873b488697d4b047c732018e1f5e25657f48d8221d14d5bcaf940356815d234 |
| futon2.aif.policy-precision-carry-test | 9 / 60 | test-registry-385a2d0ae0d517c20da7af7f3959a8341df55087368ff9fb2cb867912f72a46f |
| futon2.report.cascade-decision-test | 13 / 91 | test-registry-945eae67afd20a16753f7af137bb3bbb087a9f1d445cde6565225e31094ed612 |

Lint: zero errors/warnings (one existing informational `str` message in the
runner report). `futon4/dev/check-parens.el` reports OK. `git diff --check`
passed. The initial parens invocation omitted its CLI function and produced no
output; it was corrected before claiming a gate result. No result rests on it.

Exploratory draft logs are retained separately from the final `precommit-*`
logs. The first carry draft run failed because the constructed patterns lacked
`:guard :status :interpreted`; correction made the positive fixture meaningful.
A later draft exposed an abstention fixture without its mandatory reason and a
numeric test using structural `=` for 1 versus 1.0. The fixtures were corrected;
no production validator was weakened, no failure skipped. Subsequent additions
covered authenticated schedule binding, trace persistence and abstention carry;
the final isolated runs above validate the finished bytes. An initially wrong
registry-check CLI invocation returned usage only; the corrected check returned
`:missing-entry`, retained in `registry-check-corrected.log`.

Serving JVM: **committed, not loaded**. No click, restart, live task dispatch,
production admission, calibration, or declaration timing edit was performed.
The registered checkout is removed after preserving this receipt; its recorded
path can be recreated at the implementation SHA for reproduction. Canonical H3
source files match the tested commit. Other lanes' in-flight repair/runner files
are outside this commit and were not swept up; whole-tree cleanliness awaits
their owners' landings.
