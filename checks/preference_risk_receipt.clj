(ns checks.preference-risk-receipt
  "Finite execution certificate for the separated risk boundary. Run in its own
   process from futon2 with bb -cp .:src -m checks.preference-risk-receipt. Generated proofs bind actual runtime masses to Lean;
   this is not a proof of Clojure semantics or a live-run certificate."
  (:require [babashka.process :as process]
            [checks.disposition-kernel :as kernel]
            [checks.positive-proof-receipt :as receipt]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.disposition-risk :as risk]
            [futon2.aif.efe :as efe]
            [futon2.aif.ruled-outcome-c :as ruled]))

(def disposition-names
  (sorted-map :abstained "abstained" :agent-unavailable "agentUnavailable"
              :artifact-only "artifactOnly" :build-failed "buildFailed"
              :cancelled "cancelled" :dispatch-failed "dispatchFailed"
              :grounded-change "groundedChange" :grounded-no-change "groundedNoChange"
              :guardrail-refusal "guardrailRefusal" :incomplete "incomplete"
              :no-selection "noSelection" :substrate-unavailable "substrateUnavailable"))

(defn require! [ok label]
  (when-not ok (throw (ex-info (name label) {:refused? true :reason label}))))

(defn lean-number [x]
  (require! (or (integer? x) (ratio? x)) :nonexact-mass)
  (if (ratio? x) (str "(" (numerator x) " / " (denominator x) " : ℝ)")
      (str "(" x " : ℝ)")))

(defn binding-proof [prediction seed]
  (require! (= (set (keys disposition-names)) (set (keys prediction))
               (:support seed) (set (keys (:mass seed)))) :support-mismatch)
  (str "import DarkTower.WarMachine.PreferenceRiskWitness\n"
       "open DarkTower.WarMachine.F10RuledCarrier DarkTower.WarMachine.PreferenceRiskWitness\n"
       (str/join "\n"
                 (for [[k lean] disposition-names
                       [n masses] [["seed" (:mass seed)] ["groundedPrediction" prediction]]]
                   (str "example : " n ".mass () (organisationOutcome ." lean ") = "
                        (lean-number (get masses k)) " := by norm_num [" n ", organisationOutcome]\n")))
       "#print axioms concrete_scalarKL\n"))

(defn elaborate [source]
  (let [f (java.io.File/createTempFile "preference-risk-binding-" ".lean")]
    (try
      (spit f source)
      (let [r (process/shell {:dir "/home/joe/code/mathlib4" :continue true
                              :out :string :err :string}
                             "lake" "env" "lean" (.getAbsolutePath f))]
        {:exit (:exit r) :output (str (:out r) (:err r))})
      (finally (.delete f)))))

(defn refused? [f]
  (try (f) false
       (catch clojure.lang.ExceptionInfo e (true? (:refused? (ex-data e))))))

(defn near? [a b] (< (Math/abs (- (double a) (double b))) 1.0e-12))

(defn runtime-checks [artifact adapter]
  (let [state {:belief {:x 0.5} :observation {:mission-health 0.5}}
        action {:type :no-op}
        base {:ambiguity-mode :variance-sum :risk-mode :hinge}
        opts (assoc base :ruled-outcome-c-enabled? true :disposition-kernel adapter
                    :seeded-c ruled/seeded-c)
        off (efe/compute-efe state action base)
        disabled (efe/compute-efe state action (assoc opts :ruled-outcome-c-enabled? false))
        on (efe/compute-efe state action opts)
        twice (efe/compute-efe state action (assoc opts :ruled-outcome-c-weight 2.0))
        ln2 (Math/log 2.0)
        scalar (risk/disposition-risk {} adapter ruled/seeded-c)
        checks
        (sorted-map
         :scalar-ln2 (near? scalar ln2)
         :scorer-contribution (near? (:G-ruled-outcome-c on) scalar)
         :scorer-delta (near? (- (:controller-score on) (:controller-score off)) scalar)
         :weight-applied (near? (:G-ruled-outcome-c twice) (* 2 scalar))
         :weight-delta (near? (- (:controller-score twice) (:controller-score off)) (* 2 scalar))
         :opt-out-byte-identity (= (pr-str off) (pr-str disabled))
         :constant-not-observation-conditioned (= (adapter {:mission-health 0}) (adapter {:mission-health 1}))
         :bridge-explicitly-open (= :open (:observation-model-bridge (meta adapter)))
         :invalid-support-refused (refused? #(risk/constant-checkpoint-kernel (assoc artifact :support [])))
         :named-zero-refused
         (refused? #(risk/disposition-risk {} (constantly (assoc (zipmap (:support ruled/seeded-c) (repeat 0))
                                                                 :abstained 1)) ruled/seeded-c)))]
    (require! (every? true? (vals checks)) :runtime-boundary-failed)
    {:checks checks :scalar scalar :weighted-scalar (:G-ruled-outcome-c twice)
     :boundary :efe/compute-efe-controller-score :state state :action action}))

(def source-paths
  ["checks/preference_risk_receipt.clj" "checks/disposition_kernel.clj"
   "src/futon2/aif/disposition_risk.clj" "src/futon2/aif/ruled_outcome_c.clj"
   "src/futon2/aif/efe.clj" "src/futon2/aif/full_loop_cohort.clj"
   "../mathlib4/DarkTower/WarMachine/PreferenceRiskBoundary.lean"
   "../mathlib4/DarkTower/WarMachine/PreferenceRiskWitness.lean"
   "../mathlib4/DarkTower/WarMachine/PreferenceRiskSeparation.lean"
   "../mathlib4/DarkTower/WarMachine/F10RuledCarrier.lean"
   "../mathlib4/lean-toolchain" "../mathlib4/lake-manifest.json"])

(defn pins []
  (let [holes "../mathlib4/DarkTower/WarMachine/Holes.lean"
        source (slurp holes)]
    (into (into (sorted-map) (map (fn [p] [p (receipt/sha256-file p)]) source-paths))
          (for [decl ["Vertex" "Outcome" "ProbabilityKernel" "PreferenceDistribution"]]
            [(str holes "#" decl) (receipt/sha256-text (receipt/declaration-text source decl))]))))

(defn -main [& [output-dir]]
  (require! (some? output-dir) :output-directory-required)
  (let [before (pins)
        artifact (kernel/read-kernel kernel/default-ledger)
        adapter (risk/constant-checkpoint-kernel artifact)
        prediction (adapter {})
        source (binding-proof prediction ruled/seeded-c)
        positive (elaborate source)
        _ (require! (and (zero? (:exit positive))
                         (not (str/includes? (:output positive) "sorryAx"))) :lean-binding-failed)
        ;; A normalized but different seed MUST fail the same binding proof.
        changed-seed (-> ruled/seeded-c (assoc-in [:mass :grounded-change] 1/4)
                         (assoc-in [:mass :agent-unavailable] 3/8))
        negative (elaborate (binding-proof prediction changed-seed))
        _ (require! (and (pos? (:exit negative))
                         (str/includes? (:output negative) "unsolved goals")) :seed-control-not-rejected)
        runtime (runtime-checks artifact adapter)
        result {:schema :separated-risk-execution-certificate/v1
                :as-of "2026-09-09" :basis before
                :source-cohort (:source artifact)
                :claim :finite-mass-binding-and-scalar-boundary
                :limits [:no-live-run :no-general-runtime-refinement-proof
                         :observation-model-bridge-open :source-pins-not-transitive-closure]
                :prediction (into (sorted-map) prediction)
                :preference (into (sorted-map) (:mass ruled/seeded-c))
                :binding-proof-sha256 (receipt/sha256-text source)
                :lean positive :controls {:changed-seed-rejected true}
                :runtime runtime}]
    (require! (= before (pins)) :source-drift-during-run)
    (require! (= (:source artifact) (:source (kernel/read-kernel kernel/default-ledger))) :cohort-drift-during-run)
    (.mkdirs (java.io.File. output-dir))
    (spit (str output-dir "/runtime-mass-binding.lean") source)
    (spit (str output-dir "/certificate.edn") (with-out-str (pp/pprint result)))
    (println "separated-risk-certificate PASS: 24 mass equalities; changed-seed rejected; runtime checks" (count (:checks runtime)))))
