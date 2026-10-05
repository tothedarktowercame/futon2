(ns futon2.aif.downstream-coapplication-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.attempt-learning :as attempt-learning]
            [futon2.aif.learning-trial :as learning-trial]
            [futon2.aif.token-outcome :as token-outcome]))

(def fixture
  (edn/read-string
   (slurp (io/resource
           "fixtures/selected-want-outcome/2026-10-05-c9d25d6a-action.edn"))))

(defn decision []
  {:action (:action fixture)
   :selection-certificate (:selection-certificate fixture)})

(defn with-half-theta-and-one-step [d]
  (-> d
      (update-in [:action :precedence]
                 #(mapv (fn [pattern] (assoc pattern :theta 1/2)) %))
      (assoc-in [:selection-certificate :precision-family :model :horizon] 1)))

(defn without-order [d]
  (update-in d [:action :construction-receipt] dissoc :order))

(defn wanted-probabilities [prediction]
  (into {} (map (juxt :token :predicted)) (:wanted prediction)))

(defn produced-token [d pattern-id]
  (first (:produces (first (filter #(= pattern-id (:id %))
                                   (get-in d [:action :precedence]))))))

(defn bare-comparison [prediction]
  (token-outcome/compare-outcomes prediction [] nil))

(deftest frozen-prediction-uses-the-actions-order
  (let [recorded (token-outcome/freeze-prediction (decision))
        half-decision (with-half-theta-and-one-step (decision))
        co-applied (token-outcome/freeze-prediction half-decision)
        listed (token-outcome/freeze-prediction (without-order half-decision))
        reader (produced-token half-decision :ukrns/reader-run-path)
        capture (produced-token half-decision :war-machine/state-capture)
        handoff (produced-token half-decision :orchestration/recorded-handoff)
        warrant (produced-token half-decision :measurement/warrant-travels-with-the-number)]
    (testing "theta one reaches the same terminal state through a different transition"
      (is (= {:order :co-application} (:order-use recorded)))
      (is (= :co-application-frontier-theta-v1
             (get-in recorded [:rollout :evaluations 0 :model :semantics])))
      (is (every? #(= 1 (:predicted %)) (:wanted recorded))))
    (testing "at one half-theta step the enabled frontier fires together"
      (is (= {reader 1/2 capture 1/2 handoff 1/2 warrant 0}
             (wanted-probabilities co-applied)))
      (is (= {reader 1/2 capture 0 handoff 0 warrant 0}
             (wanted-probabilities listed))))
    (testing "an action without an order keeps the list transition"
      (is (= {:order {:absent :no-order-on-receipt}} (:order-use listed)))
      (is (= :first-enabled-union-theta-v1
             (get-in listed [:rollout :evaluations 0 :model :semantics]))))))

(deftest attempt-learning-retains-its-order-derived-rollout
  (doseq [d [(decision) (with-half-theta-and-one-step (decision))]]
    (let [prediction (token-outcome/freeze-prediction d)
          receipt (attempt-learning/receipt
                   {:comparison (bare-comparison prediction)})]
      ;; The tracked fixture has no retained signed endpoint record suitable
      ;; for attempt admission.  The receipt still performs and retains its
      ;; rollout before holding those trials on missing authority.
      (is (= {:order :co-application} (:order-use receipt)))
      (is (= (:belief (:rollout prediction))
             (:belief (:rollout receipt)))))))

(deftest learning-shadow-applies-order-after-changing-theta
  (let [d (with-half-theta-and-one-step (decision))
        prediction (token-outcome/freeze-prediction d)
        pattern (first (get-in d [:action :precedence]))
        shadow (#'learning-trial/shadow prediction pattern 1/2)]
    ;; No tracked execution-clock/source record exists for this click, so this
    ;; is the smallest direct check of the shadow rollout seam.
    (is (= {:order :co-application} (:order-use shadow)))
    (is (= :co-application-frontier-theta-v1
           (get-in shadow [:rollout :evaluations 0 :model :semantics])))
    (is (= (:belief (:rollout prediction))
           (get-in shadow [:rollout :belief])))))

(deftest learning-shadow-carries-the-changed-theta-into-the-frontier
  ;; The shadow asks what the prediction would be with one pattern's theta
  ;; changed. Under co-application the changed pattern fires beside the other
  ;; frontier patterns, so only its own want moves.
  (let [d (with-half-theta-and-one-step (decision))
        prediction (token-outcome/freeze-prediction d)
        capture-pattern (first (filter #(= :war-machine/state-capture (:id %))
                                       (get-in d [:action :precedence])))
        shadow (#'learning-trial/shadow prediction capture-pattern 1/4)
        belief (get-in shadow [:rollout :belief])
        marginal (fn [token]
                   (reduce-kv (fn [p state mass] (+ p (if (contains? state token) mass 0)))
                              0 belief))]
    (is (= 1/4 (:theta shadow)))
    (is (= 1/4 (marginal (produced-token d :war-machine/state-capture))))
    (is (= 1/2 (marginal (produced-token d :ukrns/reader-run-path))))
    (is (= 1/2 (marginal (produced-token d :orchestration/recorded-handoff))))
    (is (= 0 (marginal (produced-token d :measurement/warrant-travels-with-the-number))))
    (is (not= (:belief (:rollout prediction)) belief))))
