;; WM-04 S-4 observer adjudications for zai-18's 16 subjects.
;; Run: bb -cp src scripts/wm04/zai18_adjudicate.clj  (from futon2 root)
;; Writes holes/labs/wm-contract/wm04-pilot/adjudications/zai-18.edn
(require '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.observation-admission :as oa])

(def cutoff {:futon2 "06ecba8cf7143e12c04831dded7450f07d892d99"
             :futon3 "19f363fad159f768f03bd1f04df7392b69a96b0b"
             :futon3c "4e9c626635343420edfc819c9421e8c47bc969cd"
             :mathlib4 "3b19f6225e9dc5cf4cd645e230bb427b003b431f"
             :p4ng "f5805d864a3a19474fe0c4c8adb0b6e214f7ca48"})

(def worktree "/home/joe/code/p4ng-wm04-blind")

;; subject-id -> {finding occurrence evidence reason}
(def R
 {"R13-3"
  {:finding :present
   :occurrence "futon2@06ecba8c src/futon2/aif/shadow_cascade_g.clj wired on the receipt-construction score port (commit b8a55b7d, in cutoff ancestry); mathlib4@3b19f622 DarkTower/WarMachine/PolicyHorizon.lean"
   :evidence [{:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/shadow_cascade_g.clj"
               :what "computes per-arm (before/after construction branch) G over the token model through declared horizon 3, states/transitions/score contributions from the one model, records all inputs"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/receipt_construction.clj:571-591"
               :what "construction seam evaluates both arms through the shadow scorer, so each branch's prediction is retained on the production construction path"}
              {:repo "mathlib4" :sha "3b19f6225e9dc5cf4cd645e230bb427b003b431f" :path-or-ref "DarkTower/WarMachine/PolicyHorizon.lean"
               :what "horizonEFE sums per-step risk+ambiguity over one horizon; no temporal discount anywhere; fixture_depth_two_differs/fixture_stepIndexed_preference"}]
   :reason "Each branch's states, observations, transition interpretations and score contributions are retained through the declared horizon by the shadow scorer on the construction seam, and the aligned Lean adds no discount. Consumption by choice is a later step and is not claimed here."}

  "R16-2"
  {:finding :present
   :occurrence "futon2@06ecba8c src/futon2/aif/enact.clj (live, Joe-ratified 2026-07-02) plus the act-gate-bearing run records documented in holes/labs/wm-contract/C460-enacted-vs-selected.md"
   :evidence [{:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/enact.clj"
               :what "per tick builds act-gates for the judged ranked actions, takes the first :pass, enacts it, threads realized-outcome; attempts and guard decisions retained"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "holes/labs/wm-contract/C460-enacted-vs-selected.md"
               :what "documents actual run records: 5 with act-gate verdicts, 3 with an enacted mission"}]
   :reason "Authorized execution under actual available information with retained attempts and gate decisions exists and ran. C460 additionally found selection/enactment divergence, which belongs to the later correspondence step, not to this output."}

  "R3-2"
  {:finding :insufficient
   :occurrence "mathlib4@3b19f622 DarkTower/WarMachine/ExactBeliefTrajectory.lean (exactUpdate/exactBeliefAt) and futon2@06ecba8c src/futon2/aif/belief.clj categorical filter — no single update-context artifact found"
   :evidence [{:repo "mathlib4" :sha "3b19f6225e9dc5cf4cd645e230bb427b003b431f" :path-or-ref "DarkTower/WarMachine/ExactBeliefTrajectory.lean"
               :what "exactUpdate joins A, B, observation o and pre-update belief; exactBeliefAt threads executed actions; docstring notes no future message; but no typed rejection of wrong model or already-consumed event"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/belief.clj"
               :what "categorical filter executes the update; grep for update-context/future-evidence/already-consumed refusals found no such artifact"}]
   :reason "The join of prior, event, B and observation exists in Lean and a runtime filter exists, but I could not locate ONE update-context artifact that exhibits the three named rejections (future evidence, wrong model, event already consumed). Unknown is not absent."}

  "R4-3"
  {:finding :present
   :occurrence "futon2@06ecba8c src/futon2/aif/receipt_construction.clj:571-591 with shadow_cascade_g.clj (commit b8a55b7d in cutoff ancestry) over cascade_model_manifest.clj rollout/predict-observations"
   :evidence [{:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/receipt_construction.clj"
               :what "the construction seam passes both arms' resolved plans and beliefs through B and A over the declared horizon, retaining per-step predictions via the manifest model"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/cascade_model_manifest.clj"
               :what "rollout (PolicyRollout.rolloutState aligned) and predict-observations (A composition) with model/reading provenance"}
              {:repo "mathlib4" :sha "3b19f6225e9dc5cf4cd645e230bb427b003b431f" :path-or-ref "DarkTower/WarMachine/PolicyRollout.lean"
               :what "depth-T rollout definition the runtime aligns to"}]
   :reason "Complete predictions through B and A over the declared schedule are computed and retained on the actual construction path (organise's score port), not by an unused standalone predictor. The score is shadow-only with respect to choice; that boundary is a later step's obligation."}

  "R5-4"
  {:finding :absent
   :occurrence "futon2@06ecba8c src/futon2/aif/cascade_selection.clj + shadow_cascade_g.clj + decision_gate.clj — no recorded choice reading G scores"
   :evidence [{:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/shadow_cascade_g.clj"
               :what "its own contract states no selection, admission or shown-order decision consumes the shadow G"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/cascade_selection.clj"
               :what "the G-reading selection posterior exists as source but its docstring states it is NOT wired into the live path; the current selection head is the controller's"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/decision_gate.clj"
               :what "the live decision gate admits cascade decisions on F, not on G"}]
   :reason "Where the output would have to be — a recorded actual choice and selected/enacted identity reading the G scores at the selection head — it is not there: both the shadow scorer and the unwired selector say so in their own contracts at the cutoff."}

  "R7-1"
  {:finding :insufficient
   :occurrence "mathlib4@3b19f622 DarkTower/WarMachine/LikelihoodPrecision.lean — the ζ law exists; no channel-to-term provenance-binding artifact found"
   :evidence [{:repo "mathlib4" :sha "3b19f6225e9dc5cf4cd645e230bb427b003b431f" :path-or-ref "DarkTower/WarMachine/LikelihoodPrecision.lean"
               :what "states precisionLikelihood (A_ζ ∝ A^ζ), the gamma prior, expectedPrecision and betaPosterior (admitted update when ζ is learned) under ruling P9"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/"
               :what "no ζ runtime and no channel/observed-quantity/units provenance-binding artifact found (grep for zeta/ζ/precision provenance/channel-to-term)"}]
   :reason "The categorical precision law with prior and posterior is stated in Lean, but the application's named output — provenance naming channel, observed quantity, uncertainty convention, and channel-to-term bindings with units — was not locatable, and P9's rewrite makes the channel vocabulary itself legacy, so I cannot tell whether that shape or a categorical reformulation is the required artifact."}

  "R7-4"
  {:finding :absent
   :occurrence "mathlib4@3b19f622 LikelihoodPrecision.lean theorems only; no runtime ζ consumer in futon2@06ecba8c"
   :evidence [{:repo "mathlib4" :sha "3b19f6225e9dc5cf4cd645e230bb427b003b431f" :path-or-ref "DarkTower/WarMachine/LikelihoodPrecision.lean"
               :what "sensitivity exists as Lean theorems about the kernel (precisionLikelihood_zero/one), not as a traced consumer effect"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/"
               :what "grep for a ζ-consuming scorer/selector found none: cascade_g/shadow_cascade_g take no precision parameter; the checklist R7 clause at the cutoff still requires the demonstrated effect on Q(o|π) and G"}]
   :reason "The computed ζ value has no actual consumer at the cutoff: no runtime path takes a learned or declared ζ into Q or G, so no effective-use trace with a sensitivity control can exist. Where it would have to be (the scorer/selector inputs), it is not there."}

  "WM-06-C2"
  {:finding :absent
   :occurrence "no member-request artifact in futon2/futon3/futon3c/mathlib4 at the cutoff shas"
   :evidence [{:repo "futon2" :sha "06ecba8c" :path-or-ref "src + holes/labs/wm-contract"
               :what "grep for member request/applicable member/WM-13 finds only checklist copies, not a member-selection request artifact"}
              {:repo "futon3" :sha "19f363fa" :path-or-ref "repo grep" :what "no match"}
              {:repo "futon3c" :sha "4e9c6266" :path-or-ref "repo grep" :what "no match"}
              {:repo "mathlib4" :sha "3b19f6225e9dc5cf4cd645e230bb427b003b431f" :path-or-ref "DarkTower/WarMachine/"
               :what "TokenPreference.lean supplies one member (terminal semantics); no grain/observation-time member-selection artifact with WM-13 dependency declarations"}]
   :reason "The stated output — a member request naming the authorized member per grain/observation time with provenance and explicit unresolved WM-13 dependencies — would have to be a declaration artifact in the WM-06 work loci; none exists at the cutoff. The terminal member exists; the request does not."}

  "WM-08-C4"
  {:finding :present
   :occurrence "futon2@06ecba8c src/futon2/aif/find_receipt.clj (commit e8760f25, in cutoff ancestry) with runs/F11-find pinned records"
   :evidence [{:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/find_receipt.clj:275-299"
               :what "F1-F4 validation against independent CONTEXT and an external DESIGNATED set; the interpreter's relevance judgments never serve as the F4 designation; honest :vacuous vs :discriminating status"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "holes/labs/wm-contract/runs/F11-find/00-pin-expectation.edn"
               :what "independent F2 expectation pin from futon3 git history (pinned sha and authority), not the finder's own output"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "holes/labs/wm-contract/C500-find-rows-close.md"
               :what "the four predicates are the ruled (J9) ones, dispositioned by name with per-commit theorems"}]
   :reason "F1-F4 checks are bound to the ruled predicates and run over actual captured records with independent F2 expectations and external F4 designation; empty designation is recorded honestly vacuous, as the application itself allows."}

  "WM-12-C3"
  {:finding :absent
   :occurrence "the prediction/enactment firing-semantics correspondence at receipt_construction.clj vs CascadeEFE.lean — join-1, recorded unresolved at p4ng f5805d8"
   :evidence [{:repo "p4ng" :sha "f5805d864a3a19474fe0c4c8adb0b6e214f7ca48" :path-or-ref "CHECKLIST-fundamentals.md:346 (join-1) and :87 (WM05-U3)"
               :what "at the cutoff the once-only vs repeat-enabled firing correspondence is recorded as unresolved: same precedence or pattern vector is insufficient; WM05-U3 done-when guards/history/revisions agree — still OPEN"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/receipt_construction.clj"
               :what "acting-order/continuing-guards work exists (cb5f8b12) but no established correspondence declaring history, guard information, precedence, repetition and termination across levels"}]
   :reason "The stated output — a declared alignment of nested execution semantics with the level relation established — would have to be at the join-1 seam; the blinded checklist at the cutoff records that correspondence as still open."}

  "E02-2"
  {:finding :absent
   :occurrence "no commission-preimage/original-evidence acquisition artifact at the cutoff; only manifest machinery"
   :evidence [{:repo "p4ng" :sha "f5805d864a3a19474fe0c4c8adb0b6e214f7ca48" :path-or-ref "CHECKLIST-fundamentals.md (E02 clause)"
               :what "at the cutoff, exact commission preimages, freshness and independent semantic ownership are recorded as remaining required"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/evidence_manifest.clj"
               :what "close-evidence manifest v1 exists, but the application itself states a manifest is not acquired evidence"}
              {:repo "futon3c" :sha "4e9c6266" :path-or-ref "repo grep preimage/commission retention"
               :what "only unrelated excursion notes; no preimage resolution artifact"}]
   :reason "Where the output would have to be — acquisition records with exact commission preimages, literal bytes and authority/freshness pins — it is not there at the cutoff; the checklist's own E02 clause and repo greps agree."}

  "E09-3"
  {:finding :absent
   :occurrence "no demand/supply/load/choice record binding at the cutoff"
   :evidence [{:repo "p4ng" :sha "f5805d864a3a19474fe0c4c8adb0b6e214f7ca48" :path-or-ref "CHECKLIST-fundamentals.md (E09 clause)"
               :what "E09 recorded UNRESOLVED at the cutoff: load/demand/currency claims retained as claims, not bound records"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/capability_zones.clj"
               :what "capability-zone embedding membership with interim metrics — not actual demand/load/choice records; repo grep for demand/load bindings finds none"}]
   :reason "The stated output — actual demand, supply, load and choice records bound to the scoped claims — would have to be evidence artifacts reconciling the staged claims; at the cutoff the claims are retained as unresolved and no binding records exist."}

  "R14-2"
  {:finding :present
   :occurrence "futon2@06ecba8c src/futon2/aif/interoceptive_commitment.clj (R20→R14 chartered edge) with interoceptive_manifest.clj"
   :evidence [{:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/interoceptive_commitment.clj"
               :what "composes R20 trip/repair records: each trip joined to its repair (restoration) with typed refusals for missing joins, open vs discharged classification with retained reasons, declared law {:zero-open 1 :positive-open 1/2 :floor 1/2}, and the effective :machine-confidence factor"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/interoceptive_manifest.clj"
               :what "complete-directory manifest adapter binding production trip/repair roots with authority pins"}]
   :reason "Each monitoring factor is retained (open trips, excluded with reasons, repair joins, discharge statuses) together with the effective composed result under a declared law including a floor; production qualification gates are explicit rather than silently passed."}

  "WM-12-C6"
  {:finding :absent
   :occurrence "no effective-semantic-configuration receipt with the named controls at the cutoff"
   :evidence [{:repo "p4ng" :sha "f5805d864a3a19474fe0c4c8adb0b6e214f7ca48" :path-or-ref "CHECKLIST-fundamentals.md (WM-12 clause)"
               :what "at the cutoff the paired fairness/composition, nonfiring-versus-progressing and concrete compounding-misspecification controls are recorded as pending"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src + holes/labs"
               :what "grep for compounding/metadata-only/attestation-twin control receipts finds SPEC text requiring them, no receipt artifact binding enacted schedule, hierarchy, model and consumer with those controls"}]
   :reason "Where the output would have to be — a retained semantic-configuration receipt carrying the compounding-misspecification, cancellation/saturation and metadata-twin controls — it is not there; the cutoff checklist records those controls as pending."}

  "WM-13-C6"
  {:finding :absent
   :occurrence "mathlib4@3b19f622 DarkTower/WarMachine/FoldCWitness.lean:13 and futon2@06ecba8c c_fold_config.clj"
   :evidence [{:repo "mathlib4" :sha "3b19f6225e9dc5cf4cd645e230bb427b003b431f" :path-or-ref "DarkTower/WarMachine/FoldCWitness.lean:13"
               :what "the witness itself records: the runtime declaration currently marks NO ruled-sum layer folded"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src/futon2/aif/c_fold_config.clj"
               :what "the enabled RUN4 C fold materializes with no folded ruled-sum layer; no artifact retains which choice consumed a folded member or a preference revision with author/reason/grain"}]
   :reason "No actual folded member is passed to WM-06 and no choice-use or revision-retention record exists — the witness and the runtime configuration say so at the cutoff."}

  "WM-14-C5"
  {:finding :absent
   :occurrence "no failed-search retention + revised-query artifact at the cutoff"
   :evidence [{:repo "p4ng" :sha "f5805d864a3a19474fe0c4c8adb0b6e214f7ca48" :path-or-ref "CHECKLIST-fundamentals.md (WM-14 clause)"
               :what "at the cutoff WM-14 requires the feedback path reconsidering retrieval when picks do not help; its built evidence is interpretation/retrieval/receipt machinery only"}
              {:repo "futon2" :sha "06ecba8c" :path-or-ref "src"
               :what "grep for unhelpful/reconsider/search-revision records finds only an unrelated comment in policy_precision.clj"}
              {:repo "futon3" :sha "19f363fa" :path-or-ref "repo grep" :what "no match"}
              {:repo "futon3c" :sha "4e9c6266" :path-or-ref "repo grep" :what "only unrelated excursion notes"}]
   :reason "Where the output would have to be — a retained failed search strategy with its failure reason and a declared revised query/scope/procedure — it is not there at the cutoff in any of the four repos."}})

(def subjects
  (->> (json/parse-string (slurp "holes/labs/wm-contract/wm04-pilot/subjects.json") keyword)
       :subjects
       (filter #(= "zai-18" (:observer %)))
       (map #(into {} (map (fn [[k v]] [(keyword k) v])) %))))

(assert (= 16 (count subjects)) (str "expected 16 subjects, got " (count subjects)))
(assert (= (set (keys R)) (set (map :subject-id subjects)))
        "adjudication map and subject ids must agree exactly")

(def out
  (vec
   (for [s subjects
         :let [view (oa/observer-view s)
               r (get R (:subject-id s))]]
     (merge (oa/adjudication "zai-18" view (:finding r) cutoff)
            {:subject-id (:subject-id s)
             :worktree worktree
             :occurrence (:occurrence r)
             :evidence (:evidence r)
             :reason (:reason r)}))))

(def path "holes/labs/wm-contract/wm04-pilot/adjudications/zai-18.edn")
(spit path (with-out-str (clojure.pprint/pprint out)))
;; read-back check
(let [back (edn/read-string (slurp path))]
  (assert (= 16 (count back)))
  (assert (every? #(contains? oa/findings (:finding %)) back))
  (println "read-back OK:" (count back) "records;"
           (frequencies (map :finding back))))
(println "wrote" path)
