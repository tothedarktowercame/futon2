(ns build-examples
  "Constructed D/Q interface fixtures, not production consumption code."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.exact-belief-adapter :as adapter]
            [futon2.aif.scoring-input-receipts :as receipts]))

(def artifact-dir (.getParent (io/file *file*)))
(def token [:example :fact])
(def universe #{token})
(def states [#{} universe])
(def point {universe 1})
(def prior {#{} 1})
(def uniform {#{} 1/2 universe 1/2})
(defn sparse [q] (into {} (filter (comp pos? val)) q))
(defn identity-row [s] {s 1})
(def declaration {:status :declared :parameter-basis :synthetic
                  :z-semantics :per-step-redraw :calibration-authority :none})
(def observation {:status :observed :occurrence-id "D-schema-example-1" :tau 1
                  :present universe :absent #{}})
(def common
  {:schema :wm/token-belief-update-v1
   :occurrence-id (:occurrence-id observation) :tau 1 :observation observation
   :z-semantics :per-step-redraw :model declaration
   :carrier {:universe universe :states states :token-count 1 :state-count 2
             :representation :support-plus-transition-closure}
   :predecessor {:status :admitted :scope :isolated-test
                 :source :constructed-identity-transition
                 :previous-occurrence-id "D-schema-example-0"}
   :observation-authority {:channel :checkable :contract :wm/observation-contract-v1
                           :admission {:scope :isolated-test :tokens universe}}})

(defn received [status calculation]
  (assoc common :status status :consumed (= :value status)
         :pre-belief (:prior calculation)
         :predicted-belief (sparse (:predicted-state calculation))
         :observation-probability (:observation-probability calculation)))

(def value-calculation
  (adapter/exact-update states identity-row identity-row universe point declaration))
(def value-receipt
  (assoc (received :value value-calculation)
         :policy :conditioned-posterior :calculation value-calculation
         :posterior (sparse (:posterior value-calculation)) :continuation-belief point
         :vacuity-license
         {:status :licensed
          :theorem 'DarkTower.WarMachine.BeliefConditionedRollout.exactUpdate_pointMass_vacuous
          :support #{universe} :constant-likelihood 1 :predicted-total 1}))

(def mixture
  [{:weight 1/2 :rates {token {:false-neg 0 :false-pos 0}}}
   {:weight 1/2 :rates {token {:false-neg 1/2 :false-pos 1/2}}}])
(def distributed-calculation
  (adapter/synthetic-mixture-update states mixture (constantly uniform) universe prior))
(def distributed-receipt
  (-> (received :value distributed-calculation)
      (assoc :policy :conditioned-posterior :calculation distributed-calculation
             :model (:model distributed-calculation)
             :posterior (:posterior distributed-calculation)
             :continuation-belief (:posterior distributed-calculation)
             :vacuity-license {:status :not-licensed :reason :likelihood-varies-on-support
                               :support (set (keys uniform))})
      (assoc-in [:predecessor :source] :constructed-mixing-transition)
      (assoc-in [:observation-authority :channel] :judgement)))

(def refused-calculation
  (adapter/exact-update states identity-row identity-row universe prior declaration))
(def refused-base
  (assoc (received :refused refused-calculation)
         :kind :belief-update-refused :refused-trajectory refused-calculation
         :vacuity-license {:status :not-applicable :reason :impossible-observation}))
(def judgement-refusal
  (-> refused-base
      (assoc :policy :refused-observation-discarded :continuation-belief prior)
      (assoc-in [:observation-authority :channel] :judgement)))
(def initialization
  (receipts/initial-belief [{:target :example :cascade-problem {:facts {:fact true}}}]))
(def checkable-refusal
  (assoc refused-base
         :policy :refused-reinitialized-from-observation :continuation-belief point
         :reinitialization {:authority :contract-admitted-checkable-observation
                            :occurrence-id (:occurrence-id observation) :tau 1
                            :observation observation :initialization-receipt initialization
                            :belief (:value initialization)}
         :finding {:kind :model-misfit
                   :reason :checkable-observation-has-zero-predictive-probability}))
(def missing-receipt
  (assoc common :status :missing :consumed false :tau nil
         :observation {:status :missing :reason :no-observation
                       :occurrence-id (:occurrence-id observation) :tau nil}
         :pre-belief prior :predicted-belief (model/rollout (constantly []) prior 1)
         :continuation-belief prior :policy :no-observation-prediction-only
         :vacuity-license {:status :not-applicable :reason :no-observation}))
(def invalid-calculation
  (adapter/exact-update states identity-row identity-row universe
                        {#{} 0.5 universe 0.5} declaration))
(def invalid-receipt
  (assoc common :status :invalid :consumed false
         :kind (:kind invalid-calculation) :detail (:detail invalid-calculation)
         :invalid-input invalid-calculation
         :vacuity-license {:status :not-applicable :reason :invalid-domain}))

(def examples
  {:schema/version :wm/d-receipt-examples-v1 :scope :isolated-test
   :examples
   (into (array-map)
         (for [[id r] [[:value-point-mass value-receipt]
                       [:value-distributed distributed-receipt]
                       [:refused-judgement judgement-refusal]
                       [:refused-checkable checkable-refusal]
                       [:missing missing-receipt]
                       [:invalid invalid-receipt]]]
           [id {:receipt r :q0 (if (= :invalid (:status r)) point (:continuation-belief r))
                :expected-intake (if (= :invalid (:status r)) :invalid :ready)}]))})

;; Execute the contract's field/absence/equality tables against real adapter
;; outputs. These fixture checks do not assert production admission.
(let [spec (edn/read-string (slurp (io/file artifact-dir "schema.edn")))]
  (doseq [[_ {:keys [receipt q0 expected-intake]}] (:examples examples)
          :let [r receipt rules (get-in spec [:status-rules (:status r)])
                channel (get-in r [:observation-authority :channel])
                branch (get-in rules [:channels channel])]]
    (assert (= (:receipt-schema spec) (:schema r)))
    (assert (= (:consumed rules) (:consumed r)))
    (doseq [k (concat (:required spec) (:required rules) (:required branch))]
      (assert (contains? r k) (str "Missing " k)))
    (doseq [k (concat (:absent rules) (:absent branch))]
      (assert (not (contains? r k)) (str "Unexpected " k)))
    (doseq [[a b] (concat (:equalities rules) (:equalities branch))]
      (assert (= (get-in r a) (get-in r b)) (str "Unequal " a " " b)))
    (when (= :ready expected-intake)
      (assert (= q0 (:continuation-belief r))))
    (when (= :observed (get-in r [:observation :status]))
      (assert (= (select-keys r [:occurrence-id :tau])
                 (select-keys (:observation r) [:occurrence-id :tau]))))))
(assert (= :ok (:status value-calculation)))
(assert (= point (:posterior value-receipt)))
(assert (= 1 (:observation-probability value-receipt)))
(assert (= #{universe} (set (keys (:predicted-belief value-receipt)))))
(assert (= 1 (get-in value-calculation [:likelihoods universe])))
(assert (= {#{} 1/4 universe 3/4} (:posterior distributed-receipt)))
(assert (= :refused (:status refused-calculation)))
(assert (zero? (:observation-probability refused-calculation)))
(assert (= point (:value initialization)))
(assert (= :invalid (:status invalid-calculation)))
(assert (= :invalid-prior (:kind invalid-calculation)))
(spit (io/file artifact-dir "examples.edn") (str (pr-str examples) "\n"))
(println "D-SCHEMA-EXAMPLES" {:cases (count (:examples examples))
                             :statuses (set (map (comp :status :receipt val) (:examples examples)))
                             :real-adapter-assertions :passed
                             :production-admission :not-claimed})
