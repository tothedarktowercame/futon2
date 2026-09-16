(ns futon2.aif.cascade-g-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [futon2.aif.cascade-g :as g]
            [futon2.aif.machine-belief :as belief]))

(def model {:id "explicit-fixture" :revision "1"})
(def sample
  {:context {:policy/id "parent" :occurrence/id "fixture-only" :point 1}
   :q {:model model :state-support [:s0 :s1] :authority :isolated-test
       :mass {:s0 1/3 :s1 2/3}}
   :a {:model model :state-support [:s0 :s1] :outcome-support [:o0 :o1]
       ;; Synthetic authority record exercises the existing contract; not evidence.
       :authority :observed-estimate
       :rows {:s0 {:support [:o0 :o1] :mass {:o0 3/4 :o1 1/4}}
              :s1 {:support [:o0 :o1] :mass {:o0 1/5 :o1 4/5}}}}
   :c {:model model :support [:o0 :o1] :mass {:o0 2/5 :o1 3/5}
       :provenance {:source :isolated-test}}})

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind]))))

(deftest independent-reference
  (let [result (g/step-g sample)
        reference (edn/read-string
                   (slurp "holes/labs/wm-contract/runs/wm-10-scorer-2026-09-16/reference.edn"))]
    (doseq [[k v] reference]
      (let [[lo hi] (get result k) x (rationalize v)]
        (is (< lo x hi) (name k))
        (is (< (- hi lo) (/ 1 1000000000000000000000N)))))
    (is (= {:o0 23/60 :o1 37/60} (:outcome-mass result)))
    (is (= 1/4 (get-in result [:joint :s0 :o0])))
    (is (= sample (:inputs result)))
    (is (:exactly-normalized-inputs? result))
    ;; Risk minus information is the wrong formula, distinguish it here.
    (is (> (first (:g result))
           (- (second (:risk result)) (first (:mutual-information result)))))))

(deftest refusal-controls
  (doseq [[input expected]
          [[(assoc-in sample [:a :authority] :declared-prior) :declared-prior-refused]
           [(assoc-in sample [:c :mass] {:o0 0 :o1 1}) :infinite-risk]
           [(assoc-in sample [:c :model :revision] "other") :model-revision-mismatch]
           [(assoc-in sample [:c :support] [:o1 :o0]) :support-mismatch]
           [(assoc-in sample [:c :mass] {:o0 -1 :o1 2}) :invalid-mass]
           [(update sample :context dissoc :occurrence/id) :missing-context]
           [(update sample :c dissoc :provenance) :missing-provenance]
           [(assoc-in sample [:q :mass] {:s0 1/3 :s1 1/3}) :mass-invalid]]]
    (is (= expected (refusal #(g/step-g input))))))

(deftest schedule-and-identity
  (let [second-step (assoc-in sample [:context :point] 2)
        result (g/total-g {:schedule [1 2] :steps [sample second-step]})
        one (g/step-g sample)]
    (is (= (mapv #(* 2 %) (:g one)) (:g result)))
    (is (= :schedule-mismatch
           (refusal #(g/total-g {:schedule [2 1] :steps [sample second-step]}))))
    (is (= :comparison-context-mismatch
           (refusal #(g/total-g {:schedule [1 2]
                                :steps [sample (assoc-in second-step [:context :policy/id] "other")]}))))
    (is (= :invalid-schedule (refusal #(g/total-g {:schedule [] :steps []}))))))

(deftest zero-mass-and-deterministic-observation
  (let [s (-> sample
              (assoc-in [:q :mass] {:s0 1 :s1 0})
              (assoc-in [:a :rows :s0 :mass] {:o0 1 :o1 0})
              (assoc-in [:c :mass] {:o0 1 :o1 0}))
        r (g/step-g s)]
    (is (= [0 0] (:g r) (:equivalent-g r)))
    (is (= [0 0] (:mutual-information r)))))

(deftest represented-float-is-not-an-exact-kernel
  (let [r (g/step-g (assoc-in sample [:q :mass] {:s0 0.1 :s1 0.9}))]
    (is (false? (:exactly-normalized-inputs? r)))
    (is (= (+ (rationalize (BigDecimal. 0.1)) (rationalize (BigDecimal. 0.9)))
           (reduce + (vals (:outcome-mass r)))))))

(deftest seven-state-twelve-outcome-shape
  ;; Shape: row-7-belief-state-2026-09-12/input.edn and the ruled close
  ;; disposition carrier. Values and A are synthetic, not acquired observations.
  (let [states belief/state-support
        outcomes [:grounded-change :grounded-no-change :artifact-only :abstained
                  :no-selection :agent-unavailable :guardrail-refusal :dispatch-failed
                  :build-failed :substrate-unavailable :incomplete :cancelled]
        c (merge (zipmap outcomes (repeat 0))
                 {:grounded-change 1/2 :agent-unavailable 1/8 :build-failed 1/8
                  :incomplete 1/8 :no-selection 1/8})
        s (-> sample
              (assoc :q {:model model :state-support states :authority :isolated-test
                         :mass (zipmap states (repeat 1/7))})
              (assoc :a {:model model :state-support states :outcome-support outcomes
                         :authority :observed-estimate
                         :rows (zipmap states (repeat {:support outcomes :mass c}))})
              (assoc :c {:model model :support outcomes :mass c
                         :provenance {:source :synthetic-shaped-control}}))
        r (g/step-g s)]
    (is (= c (:outcome-mass r)))
    (is (= 7 (count (:joint r))))
    (is (= 12 (count (:outcome-mass r))))
    (is (<= (first (:risk r)) 0 (second (:risk r))))
    (is (<= (first (:mutual-information r)) 0 (second (:mutual-information r))))))

(deftest log-range-controls
  (is (= [0 0] (g/log-bounds 1)))
  (is (= :invalid-log-input (refusal #(g/log-bounds 0))))
  (is (= :invalid-log-input (refusal #(g/log-bounds 1.0))))
  (let [[lo hi] (g/log-bounds 1/2)] (is (< lo hi 0))))
