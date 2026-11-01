# L5-4d-3 — graph pin, discovery gain, construction projection

Base: futon2 `943e595ae`, futon3 mapping v2 `56a6e4d`. Branch:
`codex11/l5-4d-3-discovery-projector`. No merge, reload, click, or live store mutation.

## Implemented

- `outer_observation/verified-input-pin` reads the run manifest, hashes actual
  input bytes and compares them. Missing manifest/input/pin is typed absence;
  mismatched bytes refuse `:giraffe-input-pin-mismatch`. The observation uses it.
  Retained input pin is `730407c514e2fd21905da2b533d92e046a454372c919fbd89089fc953ebf1667`.
- `outer_score/outer-model` uses `:none` gain for undetermined picks without
  reading cone/unblocks. It retains `:gain-source :discovery-yields-no-ready` in
  the score and prediction. The consumed discovery rule is in the kernel carrier,
  so its digest changes with this rule. Ordering by missing cone still refuses.
- `wm_run_facts` projects modern construction, digest identities, all G and
  prediction rows, abstention, and library count. Slice membership is recomputed
  from unit pins and the recorded manifest, including manifest/composite hashes;
  planted unit/slice/manifest corruption yields false. Missing pins render empty
  strings (Lean id zero), never a fabricated digest. Current choice uses digest.
  Modern relation evidence uses a known class's retained receipt digest; unknown
  stays absent. Historical projection stays on the old makeup-based path.
- The new sorted string-keyed library manifest exposed an absence walker lookup
  exception (keyword comparison against a sorted string map). The predicate now
  copies each inspected map into a hash map before its keyword lookups.

## Retained-field execution: refusal, not a conforming fixture

`~/notes/l5-4d-3/real-selection.clj` calls production `select` with the actual
constructed library menu. Pass bytes are verified against
`a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91`;
626 pinned open ids partition into 609 pass rows and 17 unread exclusions.
Reach uses the declared task-status prior. Scoring state is explicitly the makeup
initializer, not a claim to recover historical learned state. Focus is re-derived
at 2026-11-01T06:02Z using the versioned focus inputs (hash retained in inputs.edn);
classification receipts are produced once and retained, not replaced with a class
constant. This is a tooling replay on retained field inputs, not a live click.

Exact result:
```edn
{:kind :tie-at-minimum
 :cascades [:only-what-can-start-now :find-out-before-building :giraffe-order]
 :G 4.787491742782046}
```
The ready-only and Giraffe policies pick `M-pattern-retrieval-calibration`;
discovery picks `M-interim-director`. This focus-input run classifies both picks
`:unknown`, so all three land on the stopped kernel and tie. Discovery scoring
no longer throws `graph-term-unavailable`. Full input/receipt/refusal evidence is
retained under `~/notes/l5-4d-3/`; exact hashes are in the adjacent comparison JSON.

The separate retained-field regression with explicitly associated test relations
also reaches the tie gate (two ready policies), and asserts discovery's actual
`[:related :none]` landing. That controlled relation fixture is not presented as
the real relation result.

Per the packet's refusal branch, no chosen section was invented, no successful
ac5bc477 record copy was produced, and there are **no nine Lean outcomes to report**.
Q2 is `.known` at the checker boundary on the small successful constructor-backed
projection test. This is not a claim of Lean conformance on the refused real field.
Q11 still needs previous/current decision/input carriers in addition to the newly
projected scores and predictions; the small fixture truthfully remains NR there.

## Validation and limits

- Base: Ran 152 tests containing 599 assertions., 20 failures, 3 errors.
- Branch: Ran 155 tests containing 626 assertions., 20 failures, 3 errors.
- Failure names and multiplicities are identical; full suite is not green.
- Focused new witnesses plus scorer and both projector suites: 85 tests,
  278 passing assertions, zero failures/errors. Focused runner/log retained.
- Canonical data files: **45,737 before and after** both comparison runs.
  Every discovered FUTON_WM root/dir/log/lock/config override went to scratch.
- Kondo: zero warnings/errors. Parens: OK. Whitespace diff check clean.
- Comparison covers the requested original 12 outer namespaces, the two new
  menu namespaces and both projector namespaces. Adjacent JSON lists all 16.
- Existing retained discovery-blocker test changed into a scoring/typed-tie
  witness; no pre-existing baseline failure expectations were weakened.
- A first focused invocation used `-Sdeps :paths` and failed before tests because
  it replaced source paths. Corrected to an extra-paths alias; no broad suite.

The next decision belongs to the coordinator: the recorded unknown relations and
minimum tie cannot be silently repaired by this packet. No menu, preference kit,
relation class, or tie rule was changed to manufacture a chosen policy.
