(ns futon2.aif.selection-cascade-test
  "Tests for the cascade machinery moved from the deleted
  futon2.aif.outer-score to futon2.aif.selection-cascade
  (M-cascade-live-causal step 2b). Each test pins behaviour that remains
  live; the outer-score model/push/score-model tests were deleted with
  their subject."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.selection-cascade :as score]))

(deftest k32-what-others-wait-on-first
  (testing "Lean K3.2 whatOthersWaitOnFirst example"
    (let [field [{:target 1 :reach 1} {:target 2 :reach 1} {:target 3 :reach 1}]
          fs {1 {:status :ready :unblocks 2 :cone 2}
              2 {:status :not-ready :unblocks 0 :cone 0}
              3 {:status :not-ready :unblocks 0 :cone 0}}
          cascade [:ready-gate :by-cone :by-unblocks]]
      (is (= {:target 1 :reach 1} (score/cascade-pick cascade fs field))))))

(deftest placed-gate-keeps-only-open-work-placements
  (let [placements {:work {:status "open" :ways ["x"] :action "work"}
                    :cluster {:status "open" :ways ["x"] :action "cluster:y"}
                    :held {:status "open" :ways ["x"] :action "held"}
                    :audit {:status "open" :ways ["x"] :action "audit"}
                    :close {:status "open" :ways ["x"] :action "close"}
                    :closed {:status "closed" :ways ["x"] :action "work"}
                    :absent {:status :absent :reason :no-placement-yet}
                    :no-ways {:status "open" :ways [] :action "work"}}
        field (mapv (fn [target] {:target target}) (keys placements))
        fs (update-vals placements #(hash-map :placement %))]
    (is (= [{:target :work} {:target :cluster}]
           (score/apply-selection-step fs field :placed-gate)))
    (doseq [target [:held :audit :close :closed :absent :no-ways]]
      (is (empty? (score/apply-selection-step fs [{:target target}] :placed-gate))
          (str target " must be dropped")))
    (try
      (score/apply-selection-step {:unwired {:status :ready}}
                                  [{:target :unwired}] :placed-gate)
      (is false "missing :placement must refuse")
      (catch clojure.lang.ExceptionInfo e
        (is (= :outer-score/placement-unavailable (:kind (ex-data e))))
        (is (= :unwired (:target (ex-data e))))))))

(deftest focus-gate-uses-the-selection-relation
  (let [field [{:target :elsewhere} {:target :focus} {:target :unknown}]
        fs {:elsewhere {:status :ready :unblocks 0 :cone 0}
            :focus {:status :ready :unblocks 0 :cone 0}
            :unknown {:status :ready :unblocks 0 :cone 0}}
        relation (fn [_ target]
                   ({:elsewhere :useful-elsewhere :focus :focus :unknown :unknown} target))
        context {:relation-class #(relation :declared-focus %)}]
    (is (= [{:target :focus}]
           (score/apply-selection-step fs field :focus-gate context)))
    (is (= :focus
           (:target (score/cascade-pick [:ready-gate :focus-gate :by-cone :by-unblocks]
                                        fs field context))))
    (is (= [{:step :ready-gate :in 3 :out 3}
            {:step :focus-gate :in 3 :out 1}
            {:step :by-cone :pick-value 0}
            {:step :by-unblocks :pick-value 0}]
           (:trace (score/apply-selection-cascade-traced
                    [:ready-gate :focus-gate :by-cone :by-unblocks]
                    fs field context))))))

(deftest focus-gate-never-admits-missing-unknown-or-not-ready-focus
  (let [field [{:target :elsewhere} {:target :focus}]
        fs {:elsewhere {:status :ready :unblocks 0 :cone 0}
            :focus {:status :not-ready :unblocks 0 :cone 0}}
        cascade [:ready-gate :focus-gate]
        context (fn [relation] {:relation-class #(relation :declared-focus %)})]
    (is (nil? (score/cascade-pick cascade fs field
                                  (context (fn [_ _] :useful-elsewhere))))
        "a focus manifest declaring nothing leaves no focus pick")
    (is (nil? (score/cascade-pick cascade fs field
                                  (context (fn [_ target]
                                             (if (= :focus target) :focus :useful-elsewhere)))))
        "the ready gate excludes an on-focus task that is not ready")
    (is (nil? (score/cascade-pick [:focus-gate] fs field
                                  (context (fn [_ _] :unknown))))
        "an unrecognised relation is never treated as focus")))

(deftest typed-steps-and-rank-match-the-interpreter
  (is (= [:ready-gate :focus-gate :placed-gate :by-cone :by-unblocks]
         (mapv score/typed-step [:ready-gate :focus-gate :placed-gate :by-cone :by-unblocks])))
  (is (= [{:named "repair obligation?"} :ready-gate {:named "verifiable result required"}]
         (mapv score/typed-step ["repair obligation?" :ready-gate "verifiable result required"])))
  (is (= [{:status-gate :undetermined} :by-rank]
         (mapv score/typed-step [{:gate-status :undetermined} :by-rank])))
  (is (thrown? clojure.lang.ExceptionInfo
               (score/apply-selection-step {} [] "repair obligation?")))
  (let [field [{:target :a} {:target :b} {:target :c}]
        fs {:a {:status :ready :rank 1} :b {:status :undetermined :rank 3}
            :c {:status :undetermined :rank 3}}]
    (is (= [{:target :b} {:target :c}]
           (score/apply-selection-cascade [{:gate-status :undetermined} :by-rank] fs field)))))

(deftest abstention-altitude-is-declared-even-when-zero
  (let [file (java.io.File/createTempFile "outer-abstention-altitude" ".edn")]
    (spit file (pr-str {:schema :wm/outer-abstention-altitude-v1 :nats 0
                        :authority "test pause" :authorized-at "2026-11-01T00:00Z"}))
    (binding [score/*abstention-altitude-path* (.getPath file)]
      (is (= 0 (:nats (score/abstention-altitude)))))))

(deftest missing-abstention-altitude-refuses-by-name
  (binding [score/*abstention-altitude-path* "/nonexistent/outer-abstention-altitude.edn"]
    (let [failure (try (score/abstention-altitude)
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :abstention-altitude-unconfigured (:kind failure))))))
