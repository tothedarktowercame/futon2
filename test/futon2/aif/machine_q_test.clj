(ns futon2.aif.machine-q-test
  "The runtime leg of :F1, checked against the Lean leg it transcribes.

   THE DEMONSTRATION READING BELOW IS A FIXTURE and lives in `test/` for that
   reason: under the `FUNDAMENTALS.edn` criterion a carrier declared for a
   demonstration inhabits nothing, so putting it in `src/` would let a reader
   mistake it for the machine's policy family, state space or belief reading.
   Those are four separate uninhabited fundamentals and choosing them is Joe's
   ruling. It mirrors `DarkTower.WarMachine.MachineQWitness` field for field so
   that the two compositions can be compared on the same numbers."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.machine-q :as mq]
            [futon2.aif.mission-epistemic-value :as mev]))

;; ---------------------------------------------------------------------------
;; The demonstration carriers -- MachineQWitness.lean:56-75, :86-95, :98-105
;; ---------------------------------------------------------------------------

(def alphabet
  "The mission's observation closure: ordinary evidence and the five typed
   absences it names (`holes/missions/M-aif-policy-conditioned-eig.md:81`),
   `MachineQWitness.lean:56-68`. Closed: no catch-all row."
  [:ordinary :no-result :failure :timeout :conflict :missing])

(def states [:informative :opaque])

(def observation-kernel
  "`aRow`, MachineQWitness.lean:113-126. Every row puts strictly positive mass
   on every typed absence."
  {:informative {:ordinary 1/2 :no-result 1/8 :failure 1/8
                 :timeout 1/8 :conflict 1/16 :missing 1/16}
   :opaque {:ordinary 1/16 :no-result 1/2 :failure 1/8
            :timeout 1/8 :conflict 1/8 :missing 1/16}})

(def transition-kernel
  "`bRow`, MachineQWitness.lean:140-147. Action-conditioned and
   state-independent, so an exhibited difference can only come from the
   planned action."
  (into {}
        (for [s states
              [u row] {:acquire {:informative 3/4 :opaque 1/4}
                       :review {:informative 1/4 :opaque 3/4}}]
          [[s u] row])))

(def model
  {:states states
   :outcomes alphabet
   :transition transition-kernel
   :observation observation-kernel})

(defn- logistic [x] (/ 1.0 (+ 1.0 (Math/exp (- (double x))))))

(def reading
  "`demoReading`, MachineQWitness.lean:200-215. The belief reading is a
   logistic read of one declared scalar -- normalised for every belief, which
   is what discharges the obligation, and demonstration-only."
  {:id :demo/evidence-acquisition
   :plan {:acquisition :acquire :review :review}
   :belief-mass (fn [belief s]
                  (let [w (logistic (get belief :support-coverage 0.0))]
                    (case s :informative w :opaque (- 1.0 w))))})

(def flat-reading
  "`flatReading`, MachineQWitness.lean:266-268: both policies commit to the
   same controlled step."
  (assoc reading :id :demo/flat :plan {:acquisition :acquire :review :acquire}))

(def beliefs
  [{} {:support-coverage -2.0} {:support-coverage 0.0} {:support-coverage 3.5}])

(defn- close? [a b] (< (Math/abs (- (double a) (double b))) 1.0e-12))

;; ---------------------------------------------------------------------------
;; The composition agrees with the Lean composition
;; ---------------------------------------------------------------------------

(deftest runtime-composition-reproduces-the-lean-row-masses-test
  (testing "demoOrdinaryRowMasses (MachineQWitness.lean:234-243): 25/64 and 11/64"
    (doseq [b beliefs]
      (let [k (mq/predictive-outcome-kernel model reading b)]
        (is (close? (get-in k [:rows :acquisition :ordinary]) (/ 25.0 64.0))
            (str "acquisition row at :ordinary, belief " b))
        (is (close? (get-in k [:rows :review :ordinary]) (/ 11.0 64.0))
            (str "review row at :ordinary, belief " b))))))

(deftest every-row-is-normalised-test
  (testing "the receipt this runtime can give for MachineQ.lean:168-181"
    (doseq [b beliefs]
      (let [k (mq/predictive-outcome-kernel model reading b)]
        (is (every? #(< % mq/tolerance) (vals (:normalisation-residual k))))
        (is (= 2 (count (:rows k))))
        (is (every? #(= (set alphabet) (set (keys %))) (vals (:rows k)))
            "no declared outcome is dropped from a row")))))

(deftest policy-conditioned-difference-is-exhibited-test
  (testing "demoPolicyConditionedDifference (MachineQWitness.lean:246-252)"
    (let [k (mq/predictive-outcome-kernel model reading {:support-coverage 1.0})]
      (is (not= (get-in k [:rows :acquisition]) (get-in k [:rows :review])))
      (is (> (Math/abs (- (get-in k [:rows :acquisition :ordinary])
                          (get-in k [:rows :review :ordinary])))
             0.2)))))

(deftest negative-control-equal-plans-coincide-test
  (testing "flatReadingRowsCoincide (MachineQWitness.lean:270-274): with both
            policies committing to the same step the rows COINCIDE, so the
            difference above is the planned action's and not the machinery's"
    (doseq [b beliefs]
      (let [k (mq/predictive-outcome-kernel model flat-reading b)]
        (is (= (get-in k [:rows :acquisition]) (get-in k [:rows :review])))))))

(deftest the-construction-law-holds-test
  (testing "rowsEqualOfEqualPlans (MachineQ.lean:210-218) as an executable check"
    (let [b {:support-coverage 0.75}]
      (is (mq/rows-equal-for-equal-plans?
           (mq/predictive-outcome-kernel model reading b) (:plan reading)))
      (is (mq/rows-equal-for-equal-plans?
           (mq/predictive-outcome-kernel model flat-reading b) (:plan flat-reading))))))

;; ---------------------------------------------------------------------------
;; The boundary
;; ---------------------------------------------------------------------------

(defn- refusal-of
  [thunk]
  (try (thunk) nil (catch clojure.lang.ExceptionInfo e (mq/refusal-reason e))))

(deftest gaussian-proxy-is-refused-at-the-boundary-test
  (testing "the live R4 output, fed in as if it were Q(o|pi)"
    (let [prediction (fm/predict {:observation {:coverage 0.4} :belief {}}
                                 {:type :no-op})]
      (is (= :refusal/action-grain-gaussian-proxy
             (refusal-of #(mq/predictive-outcome-row! prediction alphabet))))
      (is (= :refusal/action-grain-gaussian-proxy
             (refusal-of #(mq/predictive-outcome-row!
                           (:next-observation prediction) alphabet)))))))

(deftest fixture-q-is-refused-at-the-boundary-test
  (testing "mission-epistemic-value/binary-latent-model's hand-built two-point
            model -- U24's survey path, which :F1 says to reject rather than
            couple to"
    (let [fixture (mev/binary-latent-model 0.5)]
      (is (= :refusal/fixture-q-hand-built
             (refusal-of #(mq/predictive-outcome-row! fixture alphabet))))
      (is (= :refusal/fixture-q-hand-built
             (refusal-of #(mq/predictive-outcome-row! (mev/binary-latent-model 1.0)
                                                      alphabet)))))))

(deftest policy-distribution-is-refused-at-the-boundary-test
  (testing "Q(pi) is downstream of G, which consumes Q(o|pi)"
    (is (= :refusal/policy-selection-distribution
           (refusal-of #(mq/predictive-outcome-row!
                         {:softmax-weights {{:type :no-op} 1.0}} alphabet))))
    (is (= :refusal/policy-selection-distribution
           (refusal-of #(mq/predictive-outcome-row!
                         {{:type :no-op} 0.5 {:type :survey :target "M-x"} 0.5}
                         alphabet))))))

(deftest malformed-rows-are-refused-with-typed-reasons-test
  (is (= :refusal/unnormalised
         (refusal-of #(mq/predictive-outcome-row!
                       (zipmap alphabet (repeat 0.1)) alphabet))))
  (is (= :refusal/outcome-outside-declared-alphabet
         (refusal-of #(mq/predictive-outcome-row!
                       (assoc (zipmap alphabet (repeat (/ 1.0 7.0)))
                              :undeclared (/ 1.0 7.0))
                       alphabet))))
  (is (= :refusal/outcome-missing-from-row
         (refusal-of #(mq/predictive-outcome-row! {:ordinary 1.0} alphabet)))
      "an alphabet gap is not the same as a stated zero")
  (is (= :refusal/non-probability-mass
         (refusal-of #(mq/predictive-outcome-row!
                       (assoc (zipmap alphabet (repeat 0.25)) :ordinary -0.25)
                       alphabet))))
  (is (= :refusal/empty-support
         (refusal-of #(mq/predictive-outcome-row! {} alphabet))))
  (is (= :refusal/not-a-map
         (refusal-of #(mq/predictive-outcome-row! [0.5 0.5] alphabet))))
  (testing "a non-numeric mass is REFUSED, not a ClassCastException"
    ;; The row sum used to be computed eagerly, so `double` on a map or a
    ;; string threw before the reason could be named. :F1 slice 2 found it by
    ;; feeding the recorded :mu-post (entity -> distribution) and an F7 cascade
    ;; candidate to the boundary.
    (is (= :refusal/non-probability-mass
           (refusal-of #(mq/predictive-outcome-row!
                         (assoc (zipmap alphabet (repeat (/ 1.0 7.0)))
                                :ordinary {:strengthened 1.0})
                         alphabet))))
    (is (= :refusal/non-probability-mass
           (refusal-of #(mq/predictive-outcome-row!
                         (assoc (zipmap alphabet (repeat (/ 1.0 7.0)))
                                :ordinary "M-expressions-of-interest")
                         alphabet))))))

(deftest model-and-reading-obligations-are-checked-test
  (testing "the support hypotheses QReading states (MachineQ.lean:98-101)"
    (is (= :refusal/observation-support-mismatch
           (refusal-of #(mq/generative-model!
                         (assoc-in model [:observation :opaque]
                                   {:ordinary 1/2 :no-result 1/2})))))
    (is (= :refusal/transition-row-unnormalised
           (refusal-of #(mq/generative-model!
                         (assoc-in model [:transition [:informative :acquire]]
                                   {:informative 0.9 :opaque 0.9}))))))
  (testing "a belief reading that is not a distribution is refused, not renormalised"
    (is (= :refusal/belief-reading-not-normalised
           (refusal-of #(mq/belief-distribution!
                         model (assoc reading :belief-mass (fn [_ _] 0.9)) {})))))
  (testing "a policy the reading gives no step for"
    (is (= :refusal/policy-not-in-plan
           (refusal-of #(mq/predicted-state-distribution model reading {} :wander))))))

;; ---------------------------------------------------------------------------
;; The flag: off is byte-identical
;; ---------------------------------------------------------------------------

(def ^:private historical-predict-keys
  #{:next-observation :next-belief :action :predicted-events})

(deftest flag-off-is-byte-identical-test
  (let [state {:observation {:coverage 0.4} :belief {}}
        action {:type :no-op}
        seam {:model model :reading reading}]
    (testing "no reading bound, flag unset: the historical map"
      (let [p (fm/predict state action)]
        (is (= historical-predict-keys (set (keys p))))))
    (testing "a reading bound but the flag clear: the SAME map, no key, no arithmetic"
      (binding [mq/*enabled* false
                mq/*seam-reading* seam]
        (is (= (fm/predict state action) (fm/predict state action)))
        (is (= historical-predict-keys (set (keys (fm/predict state action)))))))
    (testing "the flag set but no reading exhibited: still the historical map"
      (binding [mq/*enabled* true]
        (is (= historical-predict-keys (set (keys (fm/predict state action)))))))
    (testing "flag off output equals flag on output with :machine-q removed"
      (let [off (binding [mq/*enabled* false mq/*seam-reading* seam]
                  (fm/predict state action))
            on (binding [mq/*enabled* true mq/*seam-reading* seam]
                 (fm/predict state action))]
        (is (contains? on :machine-q))
        (is (= off (dissoc on :machine-q))
            "the seam is additive: it changes nothing the prediction already carried")))))

(deftest seam-attachment-carries-the-row-for-the-planned-action-test
  (binding [mq/*enabled* true
            mq/*seam-reading* {:model model :reading reading}]
    (let [q (mq/seam-attachment {:support-coverage 1.0} :acquire)]
      (is (= [:acquisition] (:policies-planning-action q)))
      (is (close? (get-in q [:row :ordinary]) (/ 25.0 64.0)))
      (is (true? (:law/rows-equal-for-equal-plans q)))
      (is (= :demo/evidence-acquisition (:reading q))))
    (testing "an action no policy plans is a typed absence, not a default row"
      (let [q (mq/seam-attachment {} :no-op)]
        (is (= :no-policy-plans-this-action (:row-status q)))
        (is (not (contains? q :row)))))))
