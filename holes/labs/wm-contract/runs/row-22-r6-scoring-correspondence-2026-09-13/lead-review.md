# R6 scoring contract review — 20655

Subject dba62e40. All twelve source pins match current bytes. Independently inspected rank-actions, selection-scores and the WM call sites; searched the E5 field names across scorer, policy, WM and rollout. No tests or selector ran.

Accepted finding: E5 prior/step-score-delta/base fields are consumed by rollout, not the live WM controller-score/habit-prior-bias selection path. The broader source contains unrelated :prior initialization uses; those are not E5 candidate-field consumers. E6a currently lacks an implemented semantic hop, not merely a serialization or verifier adapter. Existing canonical R6 numerical computation can be described independently, but cannot demonstrate propagation from E5.

No new model law adopted. Mapping E5 prior to habit bias or delta to controller G is not a routine implementation detail and cannot be introduced under the guise of correspondence. Preserve all E5/E6 obligations: do not retire the edge, route a convenient field, or change prior/clipG/temperature. Fixed147/145+2 field and failed sufficient bound remain unchanged.

Next bounded analysis: source-grounded semantic design alternatives and compatibility assessment against adopted E5/E6 and canonical selection/rollout laws. Identify exact missing mathematical assumption and a concrete recommended contract for lead review; no code, runtime, modified prior or accepting experiment. Other E6b/Row14/18/19/F11 work remains independent and open.
