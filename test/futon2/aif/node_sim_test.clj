(ns futon2.aif.node-sim-test
  "The :F3 harness, checked on the carriers it ships with.

   THE CARRIERS ARE READ FROM THE COMMITTED FILE, not restated here. A copy in
   this namespace could drift from the one the runner uses, and then the tests
   would be green about numbers nobody runs. `holes/labs/wm-contract/sim/`
   is where a declared carrier lives; `test/` is where it is exercised."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.node-sim :as ns-sim]))

(def carriers-decl
  (edn/read-string (slurp "holes/labs/wm-contract/sim/R5-carriers.edn")))

(def equations
  (edn/read-string {:default (fn [_ v] v)}
                   (slurp "holes/labs/wm-contract/aif-equations.edn")))

(def registry-rows (filterv #(= :R5 (:node %)) (:equations equations)))

(def carriers (ns-sim/carriers! carriers-decl))

(defn- run-pilot
  ([] (run-pilot nil))
  ([node-fn]
   (ns-sim/simulate-node (cond-> {:carriers carriers :registry-rows registry-rows}
                           node-fn (assoc :node-fn node-fn)))))

(defn- close? [a b] (< (Math/abs (- (double a) (double b))) ns-sim/tolerance))

;; ---------------------------------------------------------------------------
;; The pilot passes, and the numbers are pinned
;; ---------------------------------------------------------------------------

(deftest r5-pilot-passes
  (let [r (run-pilot)]
    (is (= :pass (:verdict r)) (str "failed checks: " (:failed r)))
    (is (empty? (:failed r)))
    (testing "every check reports a status the verdict is built from"
      (is (every? #{:pass :finding} (map :status (:checks r)))))))

(deftest r5-values-are-pinned
  (let [{:keys [values reference]} (run-pilot)]
    (testing "risk separates the two policies; acquisition is the preferred one"
      (is (close? 0.24393607778459092 (get-in values [:risk :acquisition])))
      (is (close? 0.7566450629942134 (get-in values [:risk :review])))
      (is (< (get-in values [:risk :acquisition]) (get-in values [:risk :review]))))
    (testing "ambiguity does not separate them -- the two observation rows are permutations"
      (is (close? 1.4729377586898837 (get-in values [:ambiguity :acquisition])))
      (is (= (get-in values [:ambiguity :acquisition]) (get-in values [:ambiguity :review]))))
    (testing "the exact prime coefficients, checkable by hand"
      (is (= {2 -29/32, 5 55/64, 7 7/32, 11 -25/64} (get-in reference [:exact :risk :acquisition])))
      (is (= {2 -23/32, 7 7/64, 13 13/32} (get-in reference [:exact :risk :review])))
      (is (= {2 17/8} (get-in reference [:exact :ambiguity :acquisition])))
      (testing "17/8 ln 2 is 2.125 bits, the entropy of {1/2,1/8,1/8,1/8,1/16,1/16}"
        (is (close? (* 17/8 (Math/log 2.0)) (get-in values [:ambiguity :acquisition])))))))

(deftest kernel-reproduces-the-lean-witness
  (let [r (run-pilot)
        rows (get-in r [:inputs :outcome-rows])]
    (testing "MachineQWitness.lean:234-241 demoOrdinaryRowMasses"
      (is (close? 25/64 (get-in rows [:acquisition :ordinary])))
      (is (close? 11/64 (get-in rows [:review :ordinary]))))
    (is (= :pass (:status (first (filter #(= :kernel-reproduces-pinned (:check %)) (:checks r))))))))

;; ---------------------------------------------------------------------------
;; NEGATIVE CONTROLS: a planted wrong formula must fail the harness
;; ---------------------------------------------------------------------------

(defn- with-terms
  "A node built from a risk function and an ambiguity function, so a control
   plants exactly one wrong thing."
  ([risk-fn amb-fn] (with-terms risk-fn amb-fn +))
  ([risk-fn amb-fn combine]
   (fn [{:keys [policies] :as inputs}]
     (let [risk (into {} (map (fn [pi] [pi (risk-fn inputs pi)])) policies)
           amb (into {} (map (fn [pi] [pi (amb-fn inputs pi)])) policies)]
       {:risk risk :ambiguity amb
        :G (into {} (map (fn [pi] [pi (combine (get risk pi) (get amb pi))])) policies)}))))

(defn- true-risk [{:keys [outcome-rows preference]} pi]
  (ns-sim/kl-divergence (get outcome-rows pi) preference))

(defn- true-ambiguity [{:keys [state-rows observation]} pi]
  (ns-sim/expected-observation-entropy (get state-rows pi) observation))

(deftest planted-wrong-formulas-fail
  (testing "risk with the KL taken the other way round"
    (let [r (run-pilot (with-terms (fn [{:keys [outcome-rows preference]} pi]
                                     (ns-sim/kl-divergence preference (get outcome-rows pi)))
                                   true-ambiguity))]
      (is (= :fail (:verdict r)))
      (is (some #{:node-agrees-with-reference} (:failed r)))))

  (testing "risk as cross-entropy -- the -SUM Q ln Q term dropped"
    (let [r (run-pilot (with-terms (fn [{:keys [outcome-rows preference]} pi]
                                     (- (reduce (fn [acc [o m]]
                                                  (+ acc (* (double m) (Math/log (double (get preference o))))))
                                                0.0
                                                (get outcome-rows pi))))
                                   true-ambiguity))]
      (is (= :fail (:verdict r)))
      (is (some #{:node-agrees-with-reference} (:failed r)))
      (testing "and Gibbs' equality case catches it independently"
        (is (some #{:risk-zero-at-own-row} (:failed r))))))

  (testing "ambiguity as the entropy of the predictive row instead of the expected row entropy"
    (let [r (run-pilot (with-terms true-risk
                                   (fn [{:keys [outcome-rows]} pi]
                                     (ns-sim/row-entropy (get outcome-rows pi)))))]
      (is (= :fail (:verdict r)))
      (is (some #{:node-agrees-with-reference} (:failed r)))))

  (testing "ambiguity summed over states instead of weighted by Q(s|pi)"
    (let [r (run-pilot (with-terms true-risk
                                   (fn [{:keys [state-rows observation]} pi]
                                     (reduce (fn [acc [s _]] (+ acc (ns-sim/row-entropy (get observation s))))
                                             0.0
                                             (get state-rows pi)))))]
      (is (= :fail (:verdict r)))
      (is (some #{:node-agrees-with-reference} (:failed r)))
      (testing "and it leaves the entropy range, which catches it without the reference"
        (is (some #{:ambiguity-in-entropy-range} (:failed r))))))

  (testing "G as risk - ambiguity: both terms right, the other decomposition's sign"
    (let [r (run-pilot (with-terms true-risk true-ambiguity -))]
      (is (= :fail (:verdict r)))
      (is (some #{:two-term-core} (:failed r)))))

  (testing "an engineering leg smuggled into the two-term core"
    (let [r (run-pilot (fn [inputs]
                         (let [base ((:run (:R5 ns-sim/node-registry)) inputs)]
                           (assoc base :structural-pressure {:acquisition 0.0 :review 0.0}))))]
      (is (= :fail (:verdict r)))
      (is (some #{:two-term-core} (:failed r))))))

;; ---------------------------------------------------------------------------
;; Carrier refusals
;; ---------------------------------------------------------------------------

(defn- refusal-of
  [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest preference-boundary-refuses
  (let [outcomes (:outcomes carriers-decl)
        c (:preference carriers-decl)]
    (testing "the Lean positivePreference hypothesis, as a runtime refusal"
      (is (= :refusal/preference-zero-on-support
             (refusal-of #(ns-sim/preference-distribution!
                           (-> c (assoc :ordinary (+ (:ordinary c) (:missing c))) (assoc :missing 0))
                           outcomes)))))
    (testing "a C that is not stated over the declared alphabet"
      (is (= :refusal/preference-support-mismatch
             (refusal-of #(ns-sim/preference-distribution! (dissoc c :missing) outcomes)))))
    (testing "a C that does not sum to one"
      (is (= :refusal/preference-unnormalised
             (refusal-of #(ns-sim/preference-distribution! (assoc c :ordinary 1/2) outcomes)))))
    (testing "a positive control: the declared C is accepted"
      (is (= c (ns-sim/preference-distribution! c outcomes))))))

(deftest carriers-refuse-inexact-masses
  (testing "the reference route is stated over exact rationals, so a double is refused rather than rounded"
    (let [decl (assoc-in carriers-decl [:preference :ordinary] 0.6875)]
      (is (= :refusal/inexact-carrier-mass
             (refusal-of #(ns-sim/reference-r5
                           {:model (:model (ns-sim/carriers! decl))
                            :plan (:plan decl)
                            :belief (:mass (first (:beliefs decl)))
                            :preference (:preference decl)
                            :policies [:acquisition :review]})))))))

(deftest node-not-in-registry-is-refused
  (is (= :refusal/node-not-in-registry
         (refusal-of #(ns-sim/simulate-node {:carriers (assoc carriers :node :R99)
                                             :registry-rows registry-rows})))))
