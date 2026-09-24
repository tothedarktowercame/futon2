# PROOF-2 — ASSUME draft

Date: 2026-09-24
Status: adversarial draft for Joe, Codex, Zai GLM, and claude-8; not a proof and not a sign-off

## Repository discrepancies found before drafting

1. The shared checkout is at `/home/joe/code/futon2`, not `/home/joe/futon2` as stated in the request.
2. `PROOF-wm-works-2026-09-22.md` contains a later rewrite of ⟨1⟩8 saying that successive live certificates consume recorded trials, but its preserved ⟨1⟩9 verdict and final apparatus caveat say that no B-update-to-scorer mechanism exists. PROOF-2 therefore assumes neither conclusion. It must identify the particular B object, producer, persisted version, consumer, and causal choice comparison on consecutive ordinary clicks.
3. The r12 contract bundle describes sixteen contracts and their local falsifiers, but most holders are explicitly model-transcription or runtime-correspondence claims. It does not by itself establish live composition, provenance, parameter measurement, persistence, or causal use. Those are theorem obligations, not assumptions.

## ASSUME

A1. A click is one invocation of the production War Machine entry point that creates one immutable click identity and an ordered, linked set of scan, selection, dispatch, execution, adjudication, close, and learning records.

- **Objects:** click invocation; click id; attempt id; run record; stage record; close record; learning-ledger row.
- **Asserts:** “one click” denotes the whole live occurrence joined by recorded identities and timestamps, not an isolated scorer call or replay; grounded in the record shapes cited by `PROOF-wm-works-2026-09-22.md` and the records under `data/wm-runs/` and `data/wm-full-loop-machinery-*`.
- **Depends on:** none.
- **Falsified by:** two stages claimed for one click have different or absent click/attempt identities, a stage is mutable or retroactively filled, or an invocation can produce unlinked selections or closes.
- **Stub risk:** a prover could call only the scorer and call that a click; “whole live occurrence” and the required identity join exclude partial execution and replay.

A2. The immutable records produced by ordinary clicks are the only positive empirical evidence for PROOF-2; source code, tests, fixtures, narratives, recomputations, and contract declarations may define or falsify a CHECK but cannot make a theorem step pass.

- **Objects:** live run record; certificate; stage record; ledger; source file; test; fixture; narrative; recomputation.
- **Asserts:** every positive claim in steps 0–6 must be recoverable from immutable records emitted during the cited live clicks; a narrative is admissible only as an index to those records.
- **Depends on:** A1.
- **Falsified by:** a signed step relies for a required fact on a fixture, test result, hand calculation, later annotation, or prose not present in the live record.
- **Stub risk:** “the code exists” or “the fixture passes” could substitute for execution; the exclusivity and contemporaneous-record requirement closes that route.

A3. An ordinary click is a production entry-point invocation whose target supply and candidate supply are discovered without a proof-specific input, reference field, force flag, candidate list, parameter override, replay, or post-discovery human choice.

- **Objects:** click command; environment/configuration; target source; candidate source; candidate; parameter value; operator action.
- **Asserts:** ordinary operational configuration may be fixed in advance, but no value is introduced because it makes the current CHECK pass; excluded inputs include the first proof’s reference field, hand-admitted cascades, stipulated C rates, authored theta, and any hidden equivalent.
- **Depends on:** A1, A2.
- **Falsified by:** the invocation or process environment names candidates, fixes a winner-relevant term for the proof, loads a proof-only namespace/data file, replays a prior decision, or permits a human to choose after seeing the field.
- **Stub risk:** moving a fixture into configuration would evade a filename-based ban; the provenance and “introduced because it makes the CHECK pass” wording applies by function, not pathname.

A4. A real task is an independently existing, unresolved task or repair occurrence discovered from the canonical mission registry or ticket queue before the click, with stable identity, authority, acceptance condition, truthful repository locators, and feasible artifact scope.

- **Objects:** registry task; repair ticket; task identity; authority; acceptance condition; locator; repository scope.
- **Asserts:** the sources are the registry described at `mission_registry.clj:458` and `data/wm-ticket-queue/queue.edn`, with repair ordering governed by Joe’s queue ruling and REPAIR-PLAN B4; proof-authored tasks are excluded.
- **Depends on:** A3.
- **Falsified by:** a selected target was created for PROOF-2, was already resolved before discovery, lacks an independently stated acceptance condition, has false locators, or cannot be completed in its admitted scope.
- **Stub risk:** a proof task could be planted in the real registry; temporal priority, independent authority, and unresolved status close that route.

A5. A candidate is an admitted executable policy for one real task, and a cascade is its ordered, finite sequence of interpreted patterns with recorded guards, precedence, declared token effects, locators, acceptance evidence, and construction provenance.

- **Objects:** candidate; cascade; pattern; guard; precedence edge; declared effect; source task; admission record; construction record.
- **Asserts:** the source-to-candidate chain is target discovery → substantive interpretation → observable acceptance → construction → review/publication through `cascade_sources/check-file!` → selection consumption, as required by REPAIR-PLAN B4.
- **Depends on:** A4.
- **Falsified by:** a scored arm has no source task, is only a proposal, has `:hand-admitted` construction, lacks an interpretation/effect/locator, or cannot be dispatched as recorded.
- **Stub risk:** wrapping a task id around a hand-written cascade could pass a shallow provenance check; the complete recorded derivation and executable/admitted requirements exclude it.

A6. “Several distinct cascades with differing declared effects” means at least two simultaneously eligible candidates whose normalized semantic descriptions differ before scoring in at least one executable first action or reachable declared token-state transition, not merely in ids, order, prose, metadata, or numerically irrelevant padding.

- **Objects:** candidate field; normalized cascade; first action; reachable state; declared effect; semantic equivalence relation.
- **Asserts:** distinctness and effect difference are recorded and mechanically comparable on the same ordinary click; REPAIR-PLAN B4’s minimum of two meaningfully different first actions is the floor, not proof that the field is comprehensive.
- **Depends on:** A5.
- **Falsified by:** fewer than two eligible candidates remain after admission, duplicate cascades differ only nominally, or all candidates induce the same reachable effects from the recorded state.
- **Stub risk:** cloning a candidate under new ids would satisfy cardinality; semantic normalization and reachable-effect difference close that route.

A7. The r12 machine-contract bundle fixes the meanings and local falsifiers of the mathematical components but grants no premise that a live click composes or consumes them correctly.

- **Objects:** the sixteen contracts in `mathlib4/DarkTower/WarMachine/machine-contracts-2026-09-16-r12/machine-contracts.json`; declaration; Clojure locus; Lean evidence; falsifier; live certificate.
- **Asserts:** each CHECK selects and records its applicable bundle falsifier before execution, then adds an integration falsifier for provenance and live use; bundle SHA-256 is `4495e754356e601aa74d81629f50f5890a2fd3dc2f2fb32667e57715df6922ea`.
- **Depends on:** A2.
- **Falsified by:** a CHECK has no predeclared falsifier, cites a different bundle, treats holder metadata as live evidence, or lacks an integration falsifier where the bundle tests only a local function.
- **Stub risk:** “contract correspondence” could smuggle in the whole theorem; explicitly limiting it to meanings and local falsifiers closes that route.

A8. E denotes the recorded habit prior over candidates consumed by `wm-policy-selection`; it is recorded only when each candidate’s value and provenance are on the selection certificate, consumed only when those exact values enter `sigma(log E − F − gamma G)`, and degenerate exactly when the values are uniform over the eligible field.

- **Objects:** E vector; eligible candidate field; selection certificate; posterior; `wm-policy-selection` contract.
- **Asserts:** E is bound by `wm-policy-selection` (`PolicySelection.selectionPosterior`) for its role in the law; “present” does not imply informative, and uniform E must be labelled degenerate.
- **Depends on:** A6, A7.
- **Falsified by:** E is absent per candidate, certificate and scorer inputs differ, uniform values are described as informative, or perturbing E under frozen other inputs fails the contract’s posterior-ratio law.
- **Stub risk:** logging a decorative E after selection could count as present; exact scorer-input identity and perturbation close that route.

A9. C denotes the prospective preference distribution over outcomes for each candidate universe, derived from pre-existing task acceptance/value authority rather than stipulated class rates, and consumed through risk in G under `wm-token-preference` and `wm-policy-horizon`.

- **Objects:** preference specification; want/evidence sets; outcome universe; C distribution/log preference; risk term; task authority; certificate.
- **Asserts:** recorded means the certificate carries the full value or a content-addressed value plus derivation; consumed means that exact C supplies the risk used in the selected candidate’s G; calibrated status, source, and uncertainty are explicit.
- **Depends on:** A4, A7.
- **Falsified by:** C is 55/35/5/5 or another proof-stipulated rate, has no pre-click authority, differs between equal universes without derivation, is logged but not used in risk, or violates a `wm-token-preference` falsifier.
- **Stub risk:** relabelling stipulated rates “derived” would pass; a reproducible derivation from independently existing task authority and bad-case removal/perturbation are required.

A10. A denotes the observation likelihood from latent token states to recorded adjudicated outcomes, with false-positive/false-negative rates estimated from outcome records and bound by `wm-token-observation` and `wm-policy-rollout`, not filled by the zero-rate identity default.

- **Objects:** adjudication/outcome rows; rate-estimation population and window; A matrix/kernel; certificate; `wm-token-observation`; `wm-policy-rollout`.
- **Asserts:** measured means computed by a fixed, predeclared estimator from multiple eligible recorded outcomes with sample counts, time window, uncertainty or smoothing, and provenance on the certificate; consumed means the exact matrix enters predicted outcomes and downstream belief/G for this click.
- **Depends on:** A2, A7.
- **Falsified by:** identity/zero rates appear without being the measured result, any rate is stipulated, the source rows are absent or ineligible, A is merely logged, or changing a source row under the estimator cannot change the consumed A when mathematically relevant.
- **Stub risk:** one cherry-picked outcome or an estimator hard-coded to identity could be called measurement; the predeclared population, estimator, counts, and sensitivity requirement close that route.

A11. D denotes the candidate’s initial categorical belief over token states before rollout; once nontrivial A is available, D is a normalized posterior obtained by conditioning a recorded prior on recorded observations under A, as bound by `wm-exact-belief` and the state obligations of `wm-token-state`.

- **Objects:** prior; observation; A; D; token state; exact update; certificate.
- **Asserts:** posterior means Bayes-conditioned uncertainty, not `observedBelief` point mass on current facts; recorded and consumed mean the exact prior, observation, A identity, normalization/refusal, and resulting D used by rollout are certified.
- **Depends on:** A10.
- **Falsified by:** D is a point mass solely because facts were observed, equals a copied fact set without conditioning, is not normalized, lacks prior/A provenance, or violates the exact-update falsifier.
- **Stub risk:** a posterior can legitimately be point-mass in special data; the theorem cannot demand non-point-mass universally, so the CHECK must use an ordinary click whose measured A and evidence mathematically imply nonzero uncertainty and record that implication.

A12. F denotes the per-candidate variational/free-energy scalar in the selection law, computed from recorded live beliefs and observations under the declared machine free-energy definition, not a constant, alias of G, or post-selection diagnostic.

- **Objects:** F input beliefs/observations; per-candidate F; selection certificate; `wm-machine-prediction-error`; `wm-policy-selection`.
- **Asserts:** recorded means each F and its derivation/provenance are on the certificate; consumed means the exact F enters `log E − F − gamma G`; a causal F demonstration freezes E, gamma, and G and changes only record-grounded F inputs.
- **Depends on:** A8, A10, A11.
- **Falsified by:** `:not-consumed` or missing appears, all F values are an undocumented constant, F is computed after choice, or the posterior does not obey the selection contract’s F ratio under frozen other terms.
- **Stub risk:** choosing a click where habit already selects the same winner proves nothing; the required counterfactual must show habit-alone and full-law winners differ because of F, on recorded candidates and inputs.

A13. B denotes the versioned transition kernel used by cascade rollout, whose structure is bound by `wm-cascade-transition`, whose predictions are bound by `wm-policy-rollout`, and whose learned parameters are produced once from accepted, deduplicated outcome evidence.

- **Objects:** cascade/pattern identity; transition parameter; B version/content hash; accepted outcome; ledger row; updater; scorer input; certificate.
- **Asserts:** learned means a predeclared update rule changes B from an earlier persisted version using a newly accepted outcome; recorded means old version, evidence identity, rule, new version, and exactly-once disposition are present; consumed means the exact persisted B version is read by rollout/scoring.
- **Depends on:** A5, A7, A10.
- **Falsified by:** B is declared/default, update evidence is unaccepted or duplicated, version provenance is absent, the scorer reconstructs another value, or transition/rollout falsifiers fail.
- **Stub risk:** updating an auxiliary theta that the selected rollout never reads recreates the first proof’s facade; identity through producer, storage, consumer, candidate, and certificate must be one continuous recorded chain.

A14. “The next click consumes B” means the immediately next successfully started ordinary click after the update commit point, in global click order with no omitted eligible click, reads the exact new B version before selection and uses it for the same compatible model identity; the causal claim additionally requires frozen-record rescoring to show the selected action differs between old and new B while all other scorer inputs remain identical.

- **Objects:** producing click n; update commit timestamp/sequence; click n+1; global click ledger; compatibility key; old/new B; two score vectors; selected action.
- **Asserts:** a mere later read, carry-forward field, prediction made after decision, or monotone trial count is insufficient; both temporal read-back and choice-changing discrimination are required.
- **Depends on:** A3, A13.
- **Falsified by:** an intervening click is omitted, n+1 uses a default or a post-decision value, model identities are incompatible, only a certificate label changes, or old/new B select the same action in the claimed demonstration.
- **Stub risk:** selecting a later convenient click or confusing the carried value with the decision value would pass a loose claim; immediate global order, pre-selection locus, exact version, and controlled causal comparison close it.

A15. Q denotes closed-loop conditioning in which an action chosen on click n produces an adjudicated outcome, measured A maps that outcome into evidence, D is updated by conditioning, B is updated when eligible, and a subsequent ordinary decision consumes the resulting state; Q is not a separately logged boolean or a record-only shadow.

- **Objects:** action; execution; adjudicated outcome; A; D; B; subsequent selection; identity joins; applicable observation, belief, transition, rollout, and selection contracts.
- **Asserts:** closure is one end-to-end provenance chain over live records and requires the conditioning build; every arrow is a consumed data dependency, not mere co-occurrence.
- **Depends on:** A10, A11, A13, A14.
- **Falsified by:** any arrow lacks an identity join, an outcome is synthesized, conditioning is bypassed, updated state is not consumed, or Q is asserted solely by a status field.
- **Stub risk:** emitting a `:closed-loop true` receipt would trivialize the theorem; the explicit chain and data-dependency requirement close that route.

A16. A term is recorded only if its exact consumed value, semantic identity/version, producer provenance, and candidate association appear on the immutable selection certificate or through a content-addressed reference fixed at selection time; `:consumed-value-not-recorded`, `:not-consumed`, `:missing`, record-only, or an after-the-fact reconstruction are failures.

- **Objects:** E, C, A, D, F, Q, B; selection certificate; content-addressed record; candidate; producer.
- **Asserts:** “recorded” and “consumed” are independent predicates: a term may be recorded but unused or used but unauditable, and either fails step 6; the baseline example is `[:decision :selection-certificate :g-term-decomposition]` in run `2026-09-23-1790131591`.
- **Depends on:** A2, A8–A15.
- **Falsified by:** any term lacks value/version/provenance/association, any forbidden status appears, or the referenced content can change after selection.
- **Stub risk:** a label or summary could masquerade as the value; exact value or immutable content address plus producer provenance closes that route.

A17. Sensitivity claims in steps 3 and 5 are causal comparisons over the same live candidate field and record-grounded inputs, with exactly the named term varied between its two historically real values and every other scorer input byte-identical.

- **Objects:** ordinary click certificate; candidate field; scorer inputs; baseline and comparison values; posterior/action; term F or B.
- **Asserts:** the live click supplies both the real field and one actually consumed value; the alternate is another recorded value from the specified history, not invented, and recomputation is negative/causal evidence supporting—but never replacing—the live record.
- **Depends on:** A2, A12, A14.
- **Falsified by:** candidates, E, C, A, D, gamma, tie rule, or code identity differ; the alternate value is fabricated; or only scores, not the selected action, differ.
- **Stub risk:** changing several inputs or constructing a favorable counterfactual could manufacture discrimination; single-variable, historically real values and byte-identity close that route.

A18. Each signed CHECK includes a pre-registered bad case constructed by the reviewer and fails closed on that case before the corresponding positive live record is evaluated.

- **Objects:** CHECK; bundle falsifier; integration falsifier; bad case; failure receipt; reviewer signature.
- **Asserts:** the reviewer chooses a bad case capable of detecting the exact stub risk named in the assumption; failure is machine-observed and retained, but does not itself prove the positive case.
- **Depends on:** A7 and the applicable term assumption.
- **Falsified by:** the bad case is chosen by the implementer alone, run after the reviewer sees the positive outcome, unexpectedly passes, or exercises a different path.
- **Stub risk:** a harmless malformed input could be called the bad case; it must negate the claimed property while satisfying unrelated premises and traverse the same consumer path.

A19. The baseline is a signed tuple, not a moving branch: futon2 `89e3001620bf8fd02d7555bd05506afe561680c1`, mathlib4 `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`, futon1b `3abf51fe79a5bbb00911f6298a2fd9f950d47b95`, the r12 bundle hash in A7, and the immutable 2026-09-23 run hash `7314951f0ad6d339d561a9f7ec4f5dc14042873e4602873dddb590701ba61f25`.

- **Objects:** repository commit; contract bundle; run record; runtime load identity; later proof click.
- **Asserts:** these hashes identify the diagnostic starting evidence, not code presumed correct; each proof click separately records the commits and loaded source digests actually used, and any repair advances the baseline explicitly.
- **Depends on:** A2, A7.
- **Falsified by:** a cited file differs at the hash, a live JVM loads another source, dirty/uncommitted code participates, or a step silently mixes records from incompatible baselines.
- **Stub risk:** freezing commits could be mistaken for assuming correctness; the statement grants identity only and requires per-click load evidence.

A20. Independent sign-off requires Joe, a Codex reviewer who did not implement the step, and Zai GLM; no Claude seat counts as independent review of work authored or reviewed by another Claude seat.

- **Objects:** author; implementer; reviewer; model family/version; signature; step revision.
- **Asserts:** roles and model versions are recorded as required by REPAIR-PLAN A10; any material change clears earlier signatures, and an implementer cannot review its own step.
- **Depends on:** none.
- **Falsified by:** Claude-on-Claude is counted as independent, author and reviewer are the same agent/model role, model identity is absent, or a signature survives a material change.
- **Stub risk:** renaming seats could manufacture independence; independence is by model family and role, not seat label.

A21. The seven theorem clauses are conjunctive over a declared finite set of linked ordinary clicks, and “the same live records” means no clause may switch to a fixture, reference field, incompatible candidate universe, or unlinked historical run.

- **Objects:** proof click set; link relation; theorem clause; candidate universe; parameter versions; certificates.
- **Asserts:** the proof may need multiple clicks because B and Q are temporal, but it must publish the complete ordered set and the joins by which each clause refers to the same tasks, outcomes, models, and parameter history.
- **Depends on:** A1–A20.
- **Falsified by:** a clause cites an unlisted run, incompatible task/model, omitted intervening click, or evidence barred by A2/A3.
- **Stub risk:** proving each clause on a bespoke favorable case recreates the short-list facade; one declared linked sequence and compatibility joins close that route.

## Assumptions from the first proof refused

1. **“On one decision input.”** Refused because it permits a hand-built field that never exercises ordinary candidate authoring or cross-click state.
2. **The frozen two-candidate reference field.** Refused as positive evidence; it remains only a regression fixture.
3. **Joe’s stipulated 55/35/5/5 preference C.** Refused under R1: a stipulated winner-separating parameter proves neither measurement nor ordinary consumption.
4. **A broad baseline including whatever runtime data/configuration the reference input uses.** Refused because it leaves the evidence population and parameter provenance open-ended; A19 pins identities and grants no correctness.
5. **Contract correspondence as an implicit composition premise.** Refused because the bundle’s holders and falsifiers do not prove the live path composes A, D, F, Q, or learned B.
6. **Frozen-input discrimination as sufficient for next-click consumption.** Refused because recomputation is not a live read and can target an auxiliary parameter; A14 requires both.
7. **A chosen work item reaching an accepted close as a premise.** Refused because acceptance and outcome eligibility must be emitted by the ordinary loop, not assumed to make learning available.

## Additional assumptions the theorem needs

1. **Eligibility of learning evidence:** only adjudicated, accepted, deduplicated outcomes with stable identities may estimate A or update B; otherwise “learned” can mean trained on refusals, duplicates, or self-authored labels. This is included in A10 and A13 but should become its own proof invariant if the implementation has multiple ledgers.
2. **Temporal order and concurrency:** a total or causally adequate global order must identify “next click” despite concurrent starts. A14 currently assumes such an order is recorded; if it is not, the theorem needs a serialization mechanism before step 5 can be stated.
3. **Identifiability/minimum evidence:** the estimator for A must predeclare population, sample count, smoothing, and uncertainty. The theorem need not prescribe a universal minimum n, but reviewers must reject an estimator whose output is insensitive or wholly prior-determined.
4. **Version compatibility:** A, D, B, token universe, cascade interpretation, and candidate field need explicit compatibility keys; otherwise a value can be “read back” into a different model where its meaning changes.
5. **Completeness boundary for the field:** step 0 proves several real alternatives, not that every real task receives a candidate. The proof must state its discovery horizon and record all exclusions, or “the field exists” can hide systematic omission.

## Clauses that cannot go through exactly as stated

1. **Step 2: “D is no longer point-mass on the facts once A exists.”** This is mathematically too strong: conditioning under a measured A can legitimately yield a point mass for decisive evidence or a degenerate prior. Smallest restatement: “On an ordinary click whose recorded prior, measured non-identity A, and observation imply nonzero posterior uncertainty, D equals the contracted posterior and is not the observed-facts point mass; on every click D records whether degeneracy is mathematically implied.”
2. **Step 3: “on a click where F differs between candidates the choice differs from habit alone.”** F differing does not entail winner reversal. Smallest restatement: “On at least one ordinary click, the consumed F causes the full-law selected action to differ from the selected action under the same recorded field with F removed, while all other inputs are frozen; every click still records and consumes F.”
3. **Step 5: “the NEXT click’s scorer consumes it, with the choice differing because of it.”** The immediate next click may have no compatible candidate or may be insensitive to B. If the intended operational contract is strict, the machine must defer/refuse incompatible selection rather than weaken “next.” Otherwise the smallest honest restatement is: “the next ordinary click with an eligible candidate for the same compatibility key consumes it, with all intervening clicks recorded; on at least one such consecutive compatible pair, the choice differs because of B.” Joe should choose the strict operational version if immediate feedback is the desired contract.
4. **Step 6: “Every term is recorded on every click,” including Q.** Q is a relation across clicks, not naturally a scalar consumed by each selection. Smallest restatement: “Every per-click input E, C, A, D, F, and B is recorded and consumption-certified on every click; every click records its Q link/status, and Q passes only for a linked sequence that closes the loop.”
5. **The Q clause “needs A plus the conditioning build; last.”** As written it lacks an observable pass condition. Smallest restatement: adopt A15’s end-to-end identity chain and require a reviewer-created broken-link case to fail.

## Draft verdict on adequacy

These assumptions do not assert that any defect is repaired. They fix vocabulary, evidence admissibility, baseline identity, causal comparison, temporal linkage, and review independence. Each theorem clause must still be discharged on the declared linked ordinary-click sequence, after its contract and integration falsifiers fail on reviewer-constructed bad cases. Any use of a reference field, stipulated value, hand-admitted cascade, record-only shadow, fixture, or unlinked later record is a failed step, not partial credit.
