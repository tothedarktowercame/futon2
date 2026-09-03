#!/usr/bin/env bb
(require '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])
(import '[java.security MessageDigest])

(def root (.getCanonicalFile
           (io/file (or (System/getenv "FUTON2_ROOT") "/home/joe/code/futon2"))))
(def contract-file (io/file "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"))
(def glossary-file (io/file "/home/joe/code/p4ng/sec-glossary.tex"))
(def witness-file (io/file root "checks/witness-registry.edn"))
(def output-file (io/file root "holes/labs/wm-contract/variable-situation-accounting.edn"))

(def area-names
  {:belief #{"GenerativeModel" "generativeFactorMass" "TransitionKernel" "BeliefState" "ObservationVector"
             "beliefUpdate" "predictionError" "PrecisionMap" "observationKernel"
             "observationKernelRowMass"}
   :scores #{"variationalFreeEnergy" "expectedFreeEnergy" "G_eq_expectedFreeEnergy"
             "ambiguity" "observationEntropy" "softmax" "predictiveOutcomeRisk"
             "PredictiveOutcomeKernel" "ExpectedInformationGainValue"
             "expectedInformationGain" "parameterInformationGain" "modelUncertaintyBonus"
             "modelUncertaintyAndEIG" "ParameterPriorKernel" "ParameterPosteriorKernel"}
   :preferences #{"PreferenceDistribution"}
   ;; policyPosteriorImportsPolicyF / policyPrecisionIsGammaFromBeta (Holes.lean:6648,6651,
   ;; minted 2026-08-30, TN-edge-review H3/H4) are about the policy posterior itself --
   ;; pi = sigma(ln E - F - G) and the gamma that scales G in it (aif-equations.edn:181,218) --
   ;; so they sit with E (PolicyPriorKernel) and pi, not with the free energies they read.
   :policy #{"ControlPolicy" "ControlVocabulary" "cascadeGrainPi" "PolicyPriorKernel"
             "policyPosteriorImportsPolicyF" "policyPrecisionIsGammaFromBeta"}
   ;; dirichletAccumulationImportAbsent (Holes.lean:6645, H2) names the missing path into
   ;; R17's concentrations; its contract evidence is DirichletConcentrations, already here.
   :learning #{"bayesianModelReduction" "modelReductionFreeEnergyChange"
               "logMultivariateBeta" "DirichletConcentrations" "bayesFactorThreshold"
               "dirichletAccumulationImportAbsent"}
   :demo #{"Fold" "FoldEscrowRecord" "FoldEscrowRecord.reconstructible" "actGate"
           "ActGateVerdict" "HaveWantArrow" "HaveWantArrowState"
           "HaveWantArrowComposition" "aliveness" "AlivenessFactor"}
   :records #{"Click" "Attempt" "Cohort"}
   ;; enactedActionEqualsSelected (Holes.lean:6636, closed/refuted) and its successor bound
   ;; enactedEqualsSelectedWhenRankOneGated (Holes.lean:6642) both ask whether a RUN enacted
   ;; what it selected -- run-level conformance, alongside wmRunsOnce/wmRunConformsToWiring.
   ;; wmRunsOnce and wmRunConformsToWiring are named here because :U29 gave
   ;; them owners that name their records (WM-RUN1.md / WM-RUN2.md). The
   ;; fallback below files an owner starting "record:" under :records, which
   ;; would have moved two run-level attestations out of the run-level row of
   ;; the coverage table on a change to their POINTER rather than to what they
   ;; claim.
   :run #{"enactedActionEqualsSelected" "enactedEqualsSelectedWhenRankOneGated"
          "wmRunsOnce" "wmRunConformsToWiring"}})

;; The nine glossary paragraphs that carry no owning contract declaration
;; (NOTE-owner-annotation-drift-2026-08-31.md, drift-corrected uncovered list).
;; Until 2026-09-02 all nine were forced :named-only, which types a built and
;; witnessed fact as if nothing stood behind it. U14 re-types them per
;; holes/labs/wm-contract/NOTE-glossary-only-triage.md (typing approved by Joe,
;; 2026-09-02): each row carries either the witness that closes it or the owner
;; that holds it open. :row-source keeps these rows distinguishable from
;; contract declarations, because the coverage table's closed/open columns count
;; DECLARATIONS -- that is what its caption says they are -- and a glossary row
;; added to those columns would double-count (Observation vector o is already
;; closed there as the declaration ObservationVector).
(def glossary-rows
  [{:name "Observation vector o" :area :belief
    :content-status :closed-by-record-with-witness
    :witness ["futon2 src/futon2/aif/observation.clj:11 observation-channels"
              "futon2 src/futon2/aif/belief.clj:919 channels-with-likelihood"
              "futon2 test/futon2/aif/observation_test.clj:38-44 (count 14 derived)"
              "futon2 test/futon2/aif/belief_test.clj:327-332, 494-500 (count 8 derived)"
              "futon2 src/futon2/aif/trace.clj (channel guards)"]
    :witness-note
    (str "Also owned by a contract declaration: ObservationVector, owner "
         "\"sec-glossary.tex paragraph:Observation vector o\", already "
         ":closed-by-record-with-witness in this file -- so this glossary row is "
         "a duplicate of a declaration and is NOT counted in the declaration "
         "columns. Formal residue (NOTE-glossary-only-triage.md disposition 1): "
         "the literal `Obs v` occurs in the Lean corpus only at "
         "mathlib4/DarkTower/WarMachine/Holes.lean:153, C's signature (the other "
         "two occurrences, :6556 and :6577, are prose quoting it), so when "
         "P-validated-R5 2a splits C per DESIGN-c-vector.md 5, o's mission-grain "
         "half is the criteria reader's :observable fields (U11).")}
   {:name "Embedding space" :area :belief
    :content-status :closed-by-record-with-witness
    :witness ["futon3c holes/excursions/pipeline-semilattice-clusters.edn (constellation data A4a reads)"
              "futon2 src/futon2/aif/a4a_substrate.clj (guarded star/candidate writes)"]
    :witness-note
    (str "Split verdict (NOTE-glossary-only-triage.md disposition 9): the "
         "infrastructure fact is closed by record; NO hole is minted for the "
         "exploratory proposal mechanism, because the Strategic mission selection "
         "paragraph itself demotes a mission-level embedding to \"neither "
         "foundational nor required\" (sec-glossary.tex:80).")}
   {:name "Active Inference Framework" :area :framing
    :content-status :framing
    :witness-note
    (str "Framing vocabulary, not machinery (NOTE-glossary-only-triage.md "
         "disposition 7): the paragraph names the framework the paper is written "
         "in, so there is nothing to build and nothing to hold open. Typed "
         ":framing so it stops counting as uncovered rather than being counted "
         "as covered.")}
   {:name "EDN" :area :records
    :content-status :closed-by-record-with-witness
    :witness ["futon2 src/futon2/aif/fold_escrow.clj (reader/checker for the deposits)"
              "futon6 data/fold-turns/ (the EDN records themselves)"]
    :witness-note
    (str "The record format every artifact in this lab is written in; witness is "
         "any typed record plus its reader (NOTE-glossary-only-triage.md "
         "disposition 6), instantiated here by the pair the glossary footnote "
         "names at sec-glossary.tex:72.")}
   {:name "Substrate and Drawbridge" :area :records
    :content-status :closed-by-record-with-witness
    :witness ["futon2 src/futon2/aif/actuator_a3.clj (Drawbridge helpers for substrate-2)"
              "futon2 src/futon2/aif/a4a_substrate.clj (guarded star/candidate writes)"]
    :witness-note
    (str "Live infrastructure per the glossary footnote at sec-glossary.tex:74 "
         "(NOTE-glossary-only-triage.md disposition 3).")}
   {:name "No self-certification" :area :assurance
    :content-status :closed-by-record-with-witness
    :witness ["futon0 scripts/futon0/futonzero/rollout_ledger.clj (birth-tagging)"
              "futon0 scripts/futon0/futonzero/reward_red_team.clj (birth-tagging)"
              "futon3c test/futon3c/aif/flight_record_test.clj (tag-discipline tests)"
              "futon2 src/futon2/aif/full_loop_runner.clj (review execution corroborated from the job-event stream)"
              "futon2 holes/labs/wm-contract/worklist_check.bb:48 (:done without :reviewed-by dies)"]
    :witness-note
    (str "The L1/L2 calibration rule of sec-glossary.tex:76, plus the "
         "operational enforcement this ledger runs under: worklist_check.bb:48 "
         "refuses a :done row with no :reviewed-by, so author and reviewer "
         "cannot be the same seat (NOTE-glossary-only-triage.md disposition 2).")
    ;; U20 (2026-09-03): additive only. The witnesses above stand and the row
    ;; stays closed; the caveat records a counter-instance the zaif lane
    ;; measured on 2026-09-02 and the repair it built the same evening, so the
    ;; accounting says both facts rather than only the closing one.
    :caveat-pointers
    ["futon2 holes/labs/zaif-harness/runs/U12-r9-finding.md (the finding :u12/worker-verdict-not-refused, its reviewer addendum, and the correction to that addendum)"
     "futon2 holes/labs/zaif-harness/runs/U14-r9-ingress-design-v1.md (the reviewed three-grade design)"
     "futon2 holes/labs/zaif-harness/runs/U14d-consumer-census.md (the five reading sites, per-command method)"
     "futon3c src/futon3c/apm/live_learning_phases.clj:618, :635, :1252, :1342 and src/futon3c/apm/learning_loop_dry_run.clj:43 (:receipt/independent-review? written as the literal true)"
     "futon3c src/futon3c/apm/frame_cycle_handlers.clj:20-25 and :193 (the guide gate that requires only that the flag be true)"
     "futon3c src/futon3c/apm/countdown_control.clj:1101-1104, called at :1168, :1241, :1320 (the flag computed as depositor distinct from reviewer)"
     "futon3c src/futon3c/apm/countdown_control.clj:1250-1251 and :1326-1327 (:zai-scribe-reviewer-is-depositor and :guide-promotion-reviewer-is-depositor, the two refusals that can fire)"
     "futon3c src/futon3c/apm/checked_handoff.clj:37-84 (the validator; :r9/worker-authored-verdict-refused at :60, :r9/verdict-event-malformed at :55)"
     "futon3c src/futon3c/apm/checked_handoff.clj:85-122 (grade-receipt; the :ungradeable-legacy floor at :121)"
     "futon3c src/futon3c/apm/frame_cycle_handlers.clj:13-18 and :197-200 (the closed grade vocabulary and its refusal)"
     "futon3c commits bf634fab, 6c088fc9, ef3ff249 (U14a); b3d770a4 (U14b); f7c373f4, 8a090c66 (U14c); b4604c0c (U14d) -- line numbers above read at futon3c ae88efe7"]
    :caveat-note
    (str "ADDITIVE (worklist :U20, 2026-09-03): the row stays closed on the "
         "witnesses above; this field records a live counter-instance and its "
         "repair, both dated 2026-09-02. THE COUNTER-INSTANCE: the APM "
         "promotion path carries a receipt field named "
         ":receipt/independent-review? that asserted the property instead of "
         "computing it. Five guide-path writers set it to the literal true "
         "(live_learning_phases.clj:618, :635, :1252, :1342 and "
         "learning_loop_dry_run.clj:43), and the gate that consults it -- "
         "guide-snapshot-evidence-valid? at frame_cycle_handlers.clj:20-25, "
         "branched at :193 -- asks only that it be true, so those receipts "
         "satisfied it vacuously. Three countdown writers do compute it, as a "
         "depositor-versus-reviewer seat-string comparison "
         "(countdown_control.clj:1101-1104, called at :1168, :1241, :1320), "
         "and two refusals can actually fire on the result "
         "(:zai-scribe-reviewer-is-depositor at :1250-1251, "
         ":guide-promotion-reviewer-is-depositor at :1326-1327): weak "
         "evidence, since distinct seat strings are not independence and no "
         "adjudicator rerun is witnessed, but not vacuous. The reviewer's "
         "first reading -- hardcoded true at every site -- was overclaimed "
         "from a truncated grep and is corrected inside U12-r9-finding.md; the "
         "two-population account is the corrected census, and it is that "
         "corrected version this caveat carries. THE REPAIR (U14 (a)-(d)): "
         "(a) a typed checked-handoff verdict event whose validator computes "
         "the grade and returns :r9/worker-authored-verdict-refused when the "
         "author seat equals the worker seat, and :r9/verdict-event-malformed "
         "when a seat is missing or blank (checked_handoff.clj:37-84); (b) the "
         "compared seats persisted onto the receipt, so a later grade "
         "re-derives from the strings the comparison consumed "
         "(countdown_control.clj:1101-1104); (c) :receipt/independence "
         ":asserted-unverified written alongside the legacy boolean at the "
         "five constant sites, with a closed grade vocabulary and a typed "
         "refusal for anything outside it (frame_cycle_handlers.clj:13-18, "
         ":197-200); (d) grade-receipt, which lets persisted seats override an "
         "asserted field and floors every pre-migration receipt at "
         ":ungradeable-legacy regardless of the boolean's value "
         "(checked_handoff.clj:85-122). WHAT THE REPAIR DOES NOT YET DO: no "
         "production code calls the validator -- searching futon3c src/ for "
         "checked-handoff returns only the namespace itself -- so "
         ":adjudicator-rerun-witnessed is reachable from tests only and no "
         "live verdict has passed through the author-equals-worker refusal; "
         "the dated flip of the five constant booleans is held for Joe. This "
         "caveat asserts no new witness for the row and changes no count.")}
   {:name "Strategic mission selection" :area :policy
    :content-status :open-hole
    :owner "futon2/holes/missions/M-wm-strategic-mission-selection.md"
    :witness-note
    (str "PROMOTED from :named-only (NOTE-glossary-only-triage.md disposition 8): "
         "the paragraph at sec-glossary.tex:80 calls the live three-factor "
         "additive mission value \"surrogates for outer-loop structure that the "
         "implementation has not yet represented\" and specs the principled layer "
         "(reason-bearing policy support; predicted mission outcomes in a forward "
         "model and G_S; proposal potentials; separate habit E_S; hierarchical "
         "Q(pi_S), Q(pi_T|pi_S)). Held open by the mission record above; the "
         "current additive model is futon2/holes/M-wm-three-factor-mission-value.md "
         "and the dark schema kernel is futon2/src/futon2/aif/mission_control_graph.clj. "
         "Cross-link: the risk term of G_S is KL(Q(o|pi) || C_mis), so U11-U13 "
         "already build a component (DESIGN-c-vector.md 3). This is a GLOSSARY-side "
         "hole, not a contract declaration: it does not appear in "
         "holes-contract.json and is therefore reported outside the declaration "
         "columns until a declaration is minted for it.")}
   {:name "Revision boundary" :area :records
    :content-status :closed-by-record-with-witness
    :witness ["futon2 src/futon2/aif/full_loop_cohort.clj:397 (emits the :post-preregistration/cancelled semantic stratum, with its reason and attempt list)"
              "futon2 src/futon2/aif/full_loop_cohort.clj:173 (excludes :cancelled attempts from the preregistered denominator and stopping window, keeping the dossier)"
              "futon2 holes/labs/M-aif-full-loop-46/cohort.edn (the preregistered outcome taxonomy the stratum is measured against)"]
    :witness-note
    (str "RULED (a) by Joe, 2026-09-02 ~20:15, recorded at "
         "NOTE-glossary-only-triage.md:107 and in this row's U14 ledger entry. "
         "The row was held :named-only because its typing was contested ON ITS "
         "REFERENT: NOTE-glossary-only-triage.md disposition 5 offered a "
         "bitemporal-store witness (valid-time retract+put, db-as-of-now "
         "queries), but the paragraph the row's owner resolves to is "
         "sec-glossary.tex:84, \\paragraph{Revision (2026-08-31; cancellation "
         "boundary)}, which says a cancelled attempt stays durably visible and "
         "begins a new semantic stratum unless a cohort preregisters it. "
         "Nothing in that paragraph is about valid time. The ruling closes the "
         "row on the cohort-46 cancellation machinery above, which is what the "
         "paragraph as written describes; disposition 5's valid-time material "
         "is NOT re-attached anywhere, because attaching it would assert a "
         "witness for a claim the paragraph does not make -- the referent-drift "
         "defect class this registry exists to catch.")}
   {:name "A shared experimental substrate" :area :records
    :content-status :closed-by-record-with-witness
    :witness ["futon2 holes/labs/zaif-harness/runs/U8a-report-sources.md (read-only probes executed against the live store)"
              "futon2 holes/labs/M-zaif-harness/z1_views.clj (the replayable views those probes ran)"]
    :witness-note
    (str "The evidence store today's U8a probes executed against "
         "(NOTE-glossary-only-triage.md disposition 4). The report names the "
         "executed queries and their results, so the shared-substrate claim of "
         "sec-glossary.tex:86 is witnessed by a run rather than by prose.")}])

;; U27's fence typing (Joe 2026-09-03, the visibility amendment on :U27): every
;; row whose :content-status is :open-hole carries how it could be closed, so
;; :U34 can draw a line in Box 2 between what is buildable now and what waits on
;; the machine. The axis is exactly one question -- does closing this need
;; evidence from a live War Machine tick? -- and it is NEITHER a difficulty
;; ordering NOR a readiness ordering. wmRunsOnce is :run-gated with its run
;; evidence already in hand (it stays open because the Lean proposition is
;; world-level, C114), and C is :pre-run-closable and blocked on a scope
;; amendment nobody has made. :readiness is the separate axis and is what a
;; reader should consult before concluding anything from the fence side.
;;
;; DECLARED, not derived: the contract carries no field from which this could be
;; computed, so each entry is an authored reading of that hole's declared close
;; path, evidenced row by row in
;; holes/labs/wm-contract/runs/U27-hole-closability/audit.edn (16 rows, 57
;; pointers, all resolving). The generator refuses to emit if an :open-hole row
;; has no entry -- U34's render fails on an untyped hole and this is the same
;; rule one step upstream.
(def hole-closability
  {"C"
   {:closability :pre-run-closable :readiness :not-ready
    :basis (str "The DESIGN-c-vector.md §5 split (C_int / C_mis) is an owner "
                "amendment (P-validated-R5 §2a); nothing in it needs a tick. "
                "The refusal's ground still holds as measured: U12 found one "
                "risk_mis value across all 133 mission actions and all three "
                "2026-09-02 records :absent.")}
   "nonDegenerateAblationLaw"
   {:closability :pre-run-closable :readiness :contested
    :basis (str "Evidence complete twice over -- Holes.lean:217 proves the "
                "pinned instance and checks/ablation_exact_dyadic_witness.clj "
                "passes with a rejecting negative control -- yet mathlib4 "
                "86186c3744 moved it mkClosed -> mkHole in the SAME commit "
                "that added the proof. Closing it would reverse the author's "
                "decision on no new evidence. Raised as :J9.")}
   "find"
   {:closability :pre-run-closable :readiness :not-ready
    :basis "A standing implementation refusal; an owner ruling closes it, no record can."}
   "findF1Containment"
   {:closability :pre-run-closable :readiness :not-ready
    :basis (str "The 2026-08-31 scope amendment narrowed it to the recorded-row "
                "invariant, but the pinned FindReceiptRow is not in Lean: the "
                "name occurs at its definition and its registry row only. "
                "Closing means transcribing the row and proving by decide.")}
   "findF2Receipted"
   {:closability :pre-run-closable :readiness :not-ready
    :basis "As findF1Containment: no Lean value instantiates the narrowed predicate."}
   "findF3NonSelfCertifying"
   {:closability :pre-run-closable :readiness :not-ready
    :basis "As findF1Containment: no Lean value instantiates the narrowed predicate."}
   "findF4Falsifiable"
   {:closability :pre-run-closable :readiness :not-ready
    :basis "As findF1Containment: no Lean value instantiates the narrowed predicate."}
   "organise"
   {:closability :pre-run-closable :readiness :not-ready
    :basis (str "The declaration names its own gate: whether the refusal weakens "
                "to definable is LA2's to decide from a running policy-grain "
                "rule. That running is checks/playout_snatch.clj, not a tick.")}
   "dirichletAccumulationImportAbsent"
   {:closability :pre-run-closable :readiness :not-ready
    :basis (str "An absence over code paths, settled by reading the tree; a run "
                "could only falsify it. The provenance walk is done and written "
                "down (TN §9a) but not bound: the name does not occur in "
                "checks/witness-registry.edn, so there is no check, no fixture "
                "and no rejecting control.")}
   "preferenceStackLiveRecorded"
   {:closability :run-gated :readiness :not-ready
    :runtime-evidence (str "A trace record carrying mission-c criteria records "
                           "beside its :preference-stack -- a tick whose "
                           "mission-c-readback is neither :no-active-clock nor "
                           ":no-measurable-criteria.")
    :basis (str "The claim quantifies over running instances. The C_int half is "
                "on all three 2026-09-02 records; mission-c-readback occurs zero "
                "times in wm-trace-2026-09-02.edn. Whether the C_mis half would "
                "CLOSE it is unsettled: the Lean docstring says PERMANENT (C114) "
                "and DESIGN-c-vector.md door 7 says close-by-record.")}
   "wmRunsOnce"
   {:closability :run-gated :readiness :witnessed-and-held-open
    :runtime-evidence (str "A completed tick leaving a TickRunRecord. IT EXISTS "
                           "-- the pinned 2026-08-30 receipt, three 2026-09-02 "
                           "receipts, four more from the S4 stage run.")
    :basis (str "Run-gated here does NOT mean awaiting a run. The witness passes; "
                "the hole stays open because the Lean proposition is world-level "
                "and C114 declined to narrow it to a pinned receipt.")}
   "wmRunConformsToWiring"
   {:closability :run-gated :readiness :not-ready
    :runtime-evidence (str "TN §11 R2's run: ~20 shadow ticks on current code, "
                           "records carrying the route, judged by R3 against the "
                           "re-synced topology.")
    :basis (str "The topology to judge against has not been re-synced -- :U29 and "
                ":U31 are both open -- and the 2026-09-02 receipts still read 6 "
                "of 9 hops unmapped against 21 drawn edges, as 2026-08-30 did.")}
   "enactedEqualsSelectedWhenRankOneGated"
   {:closability :run-gated :readiness :not-ready
    :runtime-evidence (str "A record joining a rank-1 selection that passes its "
                           "OWN act gate to the enacted action.")
    :basis (str "The antecedent has never occurred on record, and no record since "
                "the pin carries the enactment half at all: :realized-outcome "
                "occurs 0 times in the 09-01, 09-02 and S4 traces. The S-stage "
                "runs are shadow runs, so they cannot produce it by construction.")}
   "policyPrecisionIsGammaFromBeta"
   {:closability :run-gated :readiness :not-ready
    :runtime-evidence (str "A PERSISTED record carrying :tau with :tau-source "
                           "naming carry-beta's :beta-source, from a tick under "
                           "FUTON_WM_TAU_MODE=variational-beta-gamma with writes "
                           "enabled.")
    :basis (str "The wiring exists and the record does not: S3 is a replay, its "
                "one live tau = beta tick ran under a write-suppressing "
                "preflight, and all 18 :tau-source values in "
                "wm-trace-2026-09-01.edn are :selection-gain-only.")}
   "policyPosteriorImportsPolicyF"
   {:closability :run-gated :readiness :contested
    :runtime-evidence (str "A record whose Q(pi) carries the per-policy F term. "
                           "IT EXISTS: runs/2026-09-01-s4/wm-trace-s4.edn, 3 of "
                           "4 ticks with :f-pi-posterior :applied? true.")
    :basis (str "The only hole whose named observation is already on persisted "
                "record. Its disposition is contested because the declaration's "
                "evidence and falsifier fields name the SAME observation, so one "
                "reading closes it by witness and the other refutes it -- which "
                "under this contract's own precedent (mkRefutedByRecord) is also "
                "a close. FUTON_WM_FPI_POSTERIOR is default-off, which neither "
                "field mentions. Raised as :J10.")}
   "Strategic mission selection"
   {:closability :run-gated :readiness :not-ready
    :runtime-evidence (str "A record in which the mission value carried is the "
                           "principled one -- G_S over forward-model predicted "
                           "mission outcomes with its own habit E_S -- rather "
                           "than the three-factor additive surrogate.")
    :basis (str "sec-glossary.tex:80's claim is about what the implementation "
                "represents on the live path, so a unit test of the layer would "
                "not discharge it. Glossary-side, not a contract declaration.")}})

(defn with-closability
  "Attach U27's fence typing to an :open-hole row. Fail-closed: an open hole with
   no declared typing is an error here rather than an untyped row that :U34 would
   have to refuse downstream."
  [row]
  (if (= :open-hole (:content-status row))
    (if-let [t (hole-closability (:name row))]
      (merge row t)
      (throw (ex-info (str "open hole carries no U27 closability typing: " (:name row))
                      {:error :untyped-open-hole :name (:name row)})))
    row))

(defn sha256 [file]
  (let [d (MessageDigest/getInstance "SHA-256")]
    (.update d (.getBytes (slurp file) "UTF-8"))
    (format "%064x" (BigInteger. 1 (.digest d)))))

(defn area-for [{:keys [name owner]}]
  (or (some (fn [[area names]] (when (contains? names name) area)) area-names)
      (cond
        (or (str/includes? owner "R19") (#{"C" "machineHasNoC"} name)) :preferences
        (str/includes? owner "validated-R5") :demo
        (str/starts-with? owner "Joe 2026-08-31") :run
        (or (str/starts-with? owner "P-R2") (str/starts-with? owner "P-R8")
            (str/starts-with? owner "P-R9") (str/starts-with? owner "record:")
            (str/includes? owner "delivery-lifecycle")) :records
        :else :unclassified)))

(defn glossary-title-at [owner]
  (when-let [[_ n] (re-find #"sec-glossary\.tex:(\d+)" owner)]
    (let [line (Long/parseLong n)
          lines (vec (str/split-lines (slurp glossary-file)))]
      (some (fn [s] (second (re-find #"\\paragraph\{([^}]+)\}" s)))
            (reverse (take line lines))))))

;; :U29 added the five paragraph titles this table did not name -- "Model
;; uncertainty and EIG", "Predictive outcome distribution", "Dirichlet
;; concentration parameters", "Log multivariate beta", "Bayes factor
;; threshold". Their absence was not harmless: a declaration whose owner had
;; drifted OFF one of them landed on a neighbour the table did name and read
;; :resolves, so the gap hid drift rather than reporting it.
(defn title-area [title]
  (let [t (str/lower-case (or title ""))]
    (cond
      (re-find #"belief|prediction error|precision|observation model|generative model" t) :belief
      (re-find #"expected free energy|variational free energy|risk|ambiguity|information gain|softmax|model uncertainty|predictive outcome" t) :scores
      (re-find #"preference" t) :preferences
      (re-find #"policy|habit|strategic mission" t) :policy
      (re-find #"bayesian model reduction|dirichlet|multivariate beta|bayes factor" t) :learning
      (re-find #"fold|act-gate|have--want|aliveness" t) :demo
      (re-find #"click|attempt|cohort|edn|substrate|revision|experimental" t) :records
      :else :unclassified)))

;; Every line a glossary owner cites, not only the first: an owner may name
;; several paragraphs ("9,15,17,19,31") and the area check reads the first one
;; alone.
(defn cited-glossary-lines [owner]
  (when-let [[_ spec] (re-find #"sec-glossary\.tex:([^ ]+)" owner)]
    (mapv #(Long/parseLong %) (re-seq #"\d+" spec))))

(defn glossary-paragraph-lines []
  (into #{} (keep-indexed (fn [i s] (when (re-find #"\\paragraph\{" s) (inc (long i))))
                          (str/split-lines (slurp glossary-file)))))

(defn pointer-status [{:keys [owner] :as row}]
  (if (str/includes? owner "sec-glossary.tex:")
    ;; Two questions, because passing the first one alone is how a pointer can
    ;; be wrong and green at once (:U29): (1) does the paragraph the first
    ;; cited line lands in belong to this row's area, and (2) is every cited
    ;; line the paragraph's OWN line? A citation into a paragraph's body
    ;; survives a small insertion above it by sliding onto the next
    ;; paragraph's text while still reporting the old area.
    (let [title (glossary-title-at owner)
          paragraph-lines (glossary-paragraph-lines)
          off-paragraph (vec (remove paragraph-lines (cited-glossary-lines owner)))]
      (cond
        (not= (area-for row) (title-area title))
        {:status :drifted :resolved-title title
         :reason :line-resolves-to-different-concept}
        (seq off-paragraph)
        {:status :drifted :resolved-title title
         :reason :cited-line-is-not-a-paragraph-start
         :off-paragraph-lines off-paragraph}
        :else {:status :resolves :resolved-title title}))
    (cond
      (str/starts-with? owner "record: futon2:")
      (let [[_ path] (re-find #"record: futon2:([^ ]+)" owner)]
        (if (.isFile (io/file root path)) {:status :resolves :resolved-path path}
            {:status :drifted :reason :record-path-absent :resolved-path path}))
      (str/starts-with? owner "Joe 2026-08-31")
      {:status :drifted :reason :owner-does-not-name-record}
      :else {:status :resolves :resolution :stable-problem-or-record-owner})))

(defn content-status [declaration binding]
  (cond
    (= "hole" (:kind declaration)) :open-hole
    (and binding (= :passed (:result binding)) (= :pinned-git-v1 (:freshness binding)))
    :proven-against-pinned-source
    (and binding (= :passed (:result binding))) :closed-by-record-with-witness
    :else :closed-by-record-negative-space))

(defn witness-names [binding]
  (let [w (:witnesses binding)]
    (if (sequential? w) w [w])))

(defn build-registry
  ([] (build-registry (json/parse-string (slurp contract-file) true)))
  ([contract]
  (let [_ (when (empty? (:declarations contract))
            (throw (ex-info "model coverage unavailable: zero contract declarations"
                            {:error :zero-declarations})))
        witnesses (edn/read-string (slurp witness-file))
        bindings (into {}
                       (mapcat (fn [binding]
                                 (map (fn [name] [name binding])
                                      (witness-names binding))))
                       witnesses)
        declared
        (mapv (fn [d]
                (let [row {:name (:name d) :area (area-for d) :owner (:owner d)}
                      pointer (pointer-status (assoc d :area (:area row)))]
                  (with-closability
                    (merge row
                           {:row-source :contract-declaration
                            :content-status (content-status d (bindings (:name d)))
                            :pointer-status (:status pointer)
                            :pointer-detail (dissoc pointer :status)}))))
              (:declarations contract))
        ;; A glossary row's owner is the paragraph name unless U14 gave it a
        ;; record owner (the promoted hole); a record owner resolves only if
        ;; the file it names is on disk, so a moved mission cannot read as
        ;; owned.
        named (mapv (fn [g]
                      (let [record-owner (:owner g)
                            owner (or record-owner
                                      (str "sec-glossary.tex paragraph:" (:name g)))
                            pointer (if record-owner
                                      (if (.isFile (io/file (str (.getParentFile root) "/" record-owner)))
                                        {:resolution :record-owner :resolved-path record-owner
                                         :status :resolves}
                                        {:reason :record-path-absent :resolved-path record-owner
                                         :status :drifted})
                                      {:resolution :paragraph-name :status :resolves})]
                        (with-closability
                          (assoc (dissoc g :owner)
                                 :row-source :glossary-paragraph
                                 :owner owner
                                 :pointer-status (:status pointer)
                                 :pointer-detail (dissoc pointer :status)))))
                    glossary-rows)
        rows (vec (concat declared named))]
    {:schema :wm/variable-situation-accounting-v1
     :as-of "2026-09-03"
     :authority {:contract-git-sha (get-in contract [:source :git-sha])
                 :contract-sha256 (sha256 contract-file)
                 :glossary-sha256 (sha256 glossary-file)
                 :witness-registry-sha256 (sha256 witness-file)}
     ;; :framing is U14's addition: a glossary paragraph that names the frame
     ;; the paper is written in has nothing to build and nothing to hold open,
     ;; so neither :named-only (reads as uncovered) nor a closed status (reads
     ;; as built) is true of it.
     :axes {:content-status [:named-only :framing :open-hole
                             :closed-by-record-negative-space
                             :closed-by-record-with-witness :proven-against-pinned-source]
            :pointer-status [:resolves :drifted]
            :row-source [:contract-declaration :glossary-paragraph]
            ;; U27's fence axis. Present on :open-hole rows only, and total over
            ;; them by construction (with-closability throws otherwise).
            :closability [:pre-run-closable :run-gated]
            :readiness [:not-ready :contested :witnessed-and-held-open]}
     :rows rows
     :counts {:rows (count rows)
              :content (into (sorted-map) (frequencies (map :content-status rows)))
              :pointer (into (sorted-map) (frequencies (map :pointer-status rows)))
              :row-source (into (sorted-map) (frequencies (map :row-source rows)))
              ;; Counted separately because the two populations are counted for
              ;; different questions: declarations answer "how much of the model
              ;; is formalised", glossary paragraphs answer "how much of the
              ;; glossary has anything behind it at all".
              :glossary-content
              (into (sorted-map)
                    (frequencies (map :content-status
                                      (filter #(= :glossary-paragraph (:row-source %)) rows))))
              ;; U34 reads these two: the Total row's fence split counts the
              ;; DECLARATION holes (the column it sits in), while :closability
              ;; covers every open hole including the glossary-side one.
              :closability
              (into (sorted-map)
                    (frequencies (keep :closability rows)))
              :declaration-closability
              (into (sorted-map)
                    (frequencies (keep :closability
                                       (filter #(= :contract-declaration (:row-source %)) rows))))}})))

;; U27 negative control: an open hole with no fence typing must stop the
;; generator, not emit an untyped row for :U34 to refuse downstream. Planted
;; with a one-declaration contract whose hole name is in no table.
(defn negative-untyped! []
  (try
    (build-registry
     {:source {:git-sha "planted"}
      :declarations [{:name "plantedUntypedHole" :kind "hole"
                      :owner "P-validated-R5 §2a" :holder "by-record"
                      :decided "2026-09-03"}]})
    (binding [*out* *err*]
      (println "variable-situation-accounting: FAIL untyped open hole accepted"))
    (System/exit 2)
    (catch clojure.lang.ExceptionInfo e
      (if (= :untyped-open-hole (:error (ex-data e)))
        (do (println "variable-situation-accounting: PASS untyped open hole rejected"
                     (pr-str (ex-data e)))
            (System/exit 0))
        (do (binding [*out* *err*]
              (println "variable-situation-accounting: FAIL wrong rejection" (ex-data e)))
            (System/exit 2))))))

;; :U29 negative control. The drifted count is a published number (Box 2 says
;; how many pointers drifted), so nothing distinguishes "no pointer drifted"
;; from "the checker stopped being able to say so" unless a planted one is
;; caught. Both reasons are planted, because they fail differently: an owner
;; that names the wrong paragraph, and an owner that cites a line INSIDE the
;; right paragraph -- the second is how drift hides, since the area still
;; matches and only the exact line has moved.
(defn negative-drift! []
  (let [plant (fn [owner]
                (-> (build-registry
                     {:source {:git-sha "planted"}
                      :declarations [{:name "softmax" :kind "closed" :owner owner
                                      :holder "by-record" :decided "2026-09-03"}]})
                    :rows first))
        wrong-paragraph (plant "sec-glossary.tex:39 · P-glossary-mathematics")
        body-line (plant "sec-glossary.tex:36 · P-glossary-mathematics")
        correct (plant "sec-glossary.tex:35 · P-glossary-mathematics")
        expect (fn [label row status reason]
                 (when-not (and (= status (:pointer-status row))
                                (= reason (:reason (:pointer-detail row))))
                   (binding [*out* *err*]
                     (println "variable-situation-accounting: FAIL" label
                              (pr-str (select-keys row [:pointer-status :pointer-detail]))))
                   (System/exit 2)))]
    (expect "wrong paragraph accepted" wrong-paragraph :drifted :line-resolves-to-different-concept)
    (expect "paragraph-body citation accepted" body-line :drifted :cited-line-is-not-a-paragraph-start)
    (expect "correct pointer rejected" correct :resolves nil)
    (println "variable-situation-accounting: PASS planted drift rejected"
             (pr-str [(:pointer-detail wrong-paragraph) (:pointer-detail body-line)]))
    (System/exit 0)))

(let [check? (some #{"--check"} *command-line-args*)
      empty-negative? (some #{"--negative-empty"} *command-line-args*)
      untyped-negative? (some #{"--negative-untyped"} *command-line-args*)
      drift-negative? (some #{"--negative-drift"} *command-line-args*)
      _ (when untyped-negative? (negative-untyped!))
      _ (when drift-negative? (negative-drift!))
      value (if empty-negative?
              (try
                (build-registry {:source {} :declarations []})
                (binding [*out* *err*]
                  (println "variable-situation-accounting: FAIL empty contract accepted"))
                (System/exit 2)
                (catch Exception _
                  (println "variable-situation-accounting: PASS empty contract rejected")
                  (System/exit 0)))
              (build-registry))
      rendered (with-out-str (pp/pprint value))]
  (if check?
    (if (and (.isFile output-file) (= value (edn/read-string (slurp output-file))))
      (println "variable-situation-accounting: PASS" (:counts value))
      (do (binding [*out* *err*] (println "variable-situation-accounting: STALE"))
          (System/exit 1)))
    (let [tmp (io/file (str (.getPath output-file) ".tmp"))]
      (io/make-parents output-file)
      (spit tmp rendered)
      (java.nio.file.Files/move (.toPath tmp) (.toPath output-file)
                                (into-array java.nio.file.CopyOption
                                            [java.nio.file.StandardCopyOption/REPLACE_EXISTING
                                             java.nio.file.StandardCopyOption/ATOMIC_MOVE]))
      (println "variable-situation-accounting: WROTE" (:counts value)))))
