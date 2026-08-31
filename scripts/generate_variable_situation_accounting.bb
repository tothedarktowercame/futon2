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
  {:belief #{"GenerativeModel" "generativeFactorMass" "TransitionKernel" "BeliefState"
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
               "logMultivariateBeta" "bayesFactorThreshold"}
   :demo #{"Fold" "FoldEscrowRecord" "FoldEscrowRecord.reconstructible" "actGate"
           "ActGateVerdict" "HaveWantArrow" "HaveWantArrowState"
           "HaveWantArrowComposition" "aliveness" "AlivenessFactor"}
   :records #{"Click" "Attempt" "Cohort"}})

(def named-only
  [{:name "Observation vector o" :area :belief}
   {:name "Embedding space" :area :belief}
   {:name "Active Inference Framework" :area :framing}
   {:name "EDN" :area :records}
   {:name "Substrate and Drawbridge" :area :records}
   {:name "No self-certification" :area :assurance}
   {:name "Strategic mission selection" :area :policy}
   {:name "Revision boundary" :area :records}
   {:name "A shared experimental substrate" :area :records}])

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

(defn build-registry []
  (let [contract (json/parse-string (slurp contract-file) true)
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
                         {:content-status (content-status d (bindings (:name d)))
                          :pointer-status (:status pointer)
                          :pointer-detail (dissoc pointer :status)})))
              (:declarations contract))
        named (mapv #(assoc % :owner (str "sec-glossary.tex paragraph:" (:name %))
                              :content-status :named-only :pointer-status :resolves
                              :pointer-detail {:resolution :paragraph-name}) named-only)
        rows (vec (concat declared named))]
    {:schema :wm/variable-situation-accounting-v1
     :as-of "2026-08-31"
     :authority {:contract-git-sha (get-in contract [:source :git-sha])
                 :contract-sha256 (sha256 contract-file)
                 :glossary-sha256 (sha256 glossary-file)
                 :witness-registry-sha256 (sha256 witness-file)}
     :axes {:content-status [:named-only :open-hole :closed-by-record-negative-space
                             :closed-by-record-with-witness :proven-against-pinned-source]
            :pointer-status [:resolves :drifted]}
     :rows rows
     :counts {:rows (count rows)
              :content (into (sorted-map) (frequencies (map :content-status rows)))
              :pointer (into (sorted-map) (frequencies (map :pointer-status rows)))}}))

(let [check? (some #{"--check"} *command-line-args*)
      value (build-registry)
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
