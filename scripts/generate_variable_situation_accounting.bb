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
   :policy #{"ControlPolicy" "ControlVocabulary" "cascadeGrainPi" "PolicyPriorKernel"}
   :learning #{"bayesianModelReduction" "modelReductionFreeEnergyChange"
               "logMultivariateBeta" "DirichletConcentrations" "bayesFactorThreshold"}
   :demo #{"Fold" "FoldEscrowRecord" "FoldEscrowRecord.reconstructible" "actGate"
           "ActGateVerdict" "HaveWantArrow" "HaveWantArrowState"
           "HaveWantArrowComposition" "aliveness" "AlivenessFactor"}
   :records #{"Click" "Attempt" "Cohort"}})

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
         "cannot be the same seat (NOTE-glossary-only-triage.md disposition 2).")}
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
    :content-status :named-only
    :contested
    (str "NOT re-typed. U14's acceptance says a contested typing goes to "
         ":needs-joe rather than being forced, and this one is contested on its "
         "REFERENT. NOTE-glossary-only-triage.md disposition 5 witnesses this row "
         "with \"valid-time retract+put and db-as-of-now queries (clock-lineage "
         "protocol; S4 review confirmed server-side exclusion)\" -- bitemporal "
         "store revision. But the row's owner resolves to sec-glossary.tex:84, "
         "\\paragraph{Revision (2026-08-31; cancellation boundary)}, which is "
         "about cohort 46's outcome taxonomy: a cancelled attempt stays durably "
         "visible but begins a new semantic stratum unless a cohort preregisters "
         "it. Nothing in that paragraph is about valid time. Closing the row on "
         "the note's witness would assert a witness for a claim the paragraph "
         "does not make -- the referent-drift defect class this registry exists to "
         "catch. A witness that DOES match the paragraph as written exists: "
         "futon2 src/futon2/aif/full_loop_cohort.clj:397 emits "
         ":stratum/id :post-preregistration/cancelled and :173 excludes "
         ":cancelled from the preregistered population, against the "
         "preregistration at futon2 holes/labs/M-aif-full-loop-46/cohort.edn. "
         "Joe's call: (a) close on the cohort witness, (b) supply the paragraph "
         "the note meant, or (c) leave :named-only.")}
   {:name "A shared experimental substrate" :area :records
    :content-status :closed-by-record-with-witness
    :witness ["futon2 holes/labs/zaif-harness/runs/U8a-report-sources.md (read-only probes executed against the live store)"
              "futon2 holes/labs/M-zaif-harness/z1_views.clj (the replayable views those probes ran)"]
    :witness-note
    (str "The evidence store today's U8a probes executed against "
         "(NOTE-glossary-only-triage.md disposition 4). The report names the "
         "executed queries and their results, so the shared-substrate claim of "
         "sec-glossary.tex:86 is witnessed by a run rather than by prose.")}])

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

(defn title-area [title]
  (let [t (str/lower-case (or title ""))]
    (cond
      (re-find #"belief|prediction error|precision|observation model|generative model" t) :belief
      (re-find #"expected free energy|variational free energy|risk|ambiguity|information gain|softmax" t) :scores
      (re-find #"preference" t) :preferences
      (re-find #"policy|habit|strategic mission" t) :policy
      (re-find #"bayesian model reduction" t) :learning
      (re-find #"fold|act-gate|have--want|aliveness" t) :demo
      (re-find #"click|attempt|cohort|edn|substrate|revision|experimental" t) :records
      :else :unclassified)))

(defn pointer-status [{:keys [owner] :as row}]
  (if (str/includes? owner "sec-glossary.tex:")
    (let [title (glossary-title-at owner)]
      (if (= (area-for row) (title-area title))
        {:status :resolves :resolved-title title}
        {:status :drifted :resolved-title title
         :reason :line-resolves-to-different-concept}))
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
                  (merge row
                         {:row-source :contract-declaration
                          :content-status (content-status d (bindings (:name d)))
                          :pointer-status (:status pointer)
                          :pointer-detail (dissoc pointer :status)})))
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
                        (assoc (dissoc g :owner)
                               :row-source :glossary-paragraph
                               :owner owner
                               :pointer-status (:status pointer)
                               :pointer-detail (dissoc pointer :status))))
                    glossary-rows)
        rows (vec (concat declared named))]
    {:schema :wm/variable-situation-accounting-v1
     :as-of "2026-08-31"
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
            :row-source [:contract-declaration :glossary-paragraph]}
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
                                      (filter #(= :glossary-paragraph (:row-source %)) rows))))}})))

(let [check? (some #{"--check"} *command-line-args*)
      empty-negative? (some #{"--negative-empty"} *command-line-args*)
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
