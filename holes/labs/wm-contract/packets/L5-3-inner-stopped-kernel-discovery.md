# L5-3 — Discovery and prototype: the stopped policy's G on the inner path (claude-2 → codex-11, 2026-11-01)

**No production change.** Deliverable: a report
`reconciliation/L5-3-inner-abstention-codex11-2026-11-01.md` committed on futon2 `main` (at
`4c59a7b64` or later) plus a prototype under `~/notes/l5-3/`. Base facts: L5-1 report §6
(`reconciliation/L5-1-inner-law-path-claude3-2026-11-01.md`): the inner observation model can
express a stopped policy, but the empty arrangement as scored today is not comparable (`[0 true]`
reads the want as met, a 2-outcome C, horizon 1, no fit term); C is per policy, so one abstention
G needs a reference model. Lean Q11 (`Requirements.lean`) compares the chosen policy's G strictly
against one `abstentionG` and requires every policy's prediction to differ from
`abstentionPrediction`; today the projector emits both as typed absent on every work run, so Q11
is unverifiable there (L4c-1).

## The design question, from the theory

In the generative model a policy is scored by the outcomes it predicts against the preference C.
"Do nothing" is a policy: it predicts that the task's state does not change. Its G is well defined
only under a model that also scores the acting policies, so the comparison "act versus stop" must
be made under one model. On the inner path C is per policy (each policy's progress steps over its
own horizon), so there is no single model to score stopping under. Two honest shapes:

- **A. Stop under the chosen policy's model.** For each compared policy p, score the empty
  arrangement under p's own observation model and horizon (no steps applied; the prediction is "no
  progress" at every step; fit and information terms as p's model gives them for no action), giving
  `abstentionG_p`. Q11's `abstentionG` is the chosen policy's; record all of them. This is the
  minimal comparison that matters: the enacted policy against doing nothing under the same lens.
- **B. A shared terminal preference.** Score every policy and stopping on one terminal outcome space
  (completion then relevance, the factorized law in `FactorizedPreferenceSemantics.lean` /
  `OuterFactorizedPreference.lean`), adding a terminal term to each policy's G; stopping predicts
  "not completed". Comparable by construction, but it changes every G and so the law's orderings.

Prototype A; describe B.

## Work

1. Implement A as a function beside the scorer (`cascade_shape_g.clj` / `cascade_observation_scoring.clj`,
   read how `score-policy` builds the per-step prediction and which inputs the "no action" case
   needs), in a scratch namespace under `~/notes/l5-3/`, not in `src/`.
2. Run it over the 25 scored era-2 certificates: for each run, the chosen policy's G, its
   `abstentionG_p`, and whether the chosen beats stopping strictly; the prediction digest of stopping
   (progress steps all "no progress", class emission as the model gives) and whether any compared
   policy's digest equals it. Table in the report.
3. State what the empty arrangement's `[0 true]` reading means and whether A avoids it (a want
   cannot read as met by doing nothing; if the model says so, say why it is wrong and what the
   honest prediction is).
4. Propose the carriers: `SC :abstention {:per-policy {policy-id {:G :prediction-digest :terms}}
   :chosen-reference policy-id}`, and the projector's `abstentionG`/`abstentionPrediction` from the
   chosen's entry. Name the readers that would change.

## Gates

Read-only on `data/`; scratch only. Bell claude-2 with the report path, the 25-row table summary
(how many chosen beat stopping; how many predictions coincide with stopping), and the answer to 3.
