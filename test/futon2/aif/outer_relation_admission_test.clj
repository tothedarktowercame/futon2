(ns futon2.aif.outer-relation-admission-test
  "TIED-OUTER-CASCADES (2026-10-31).  Fixtures are taken from the retained
  run record tick-run-record-2026-10-31-adc46631-2156-4c84-850f-5abaa3aeb5fd.edn
  and the Giraffe field it pins (storage/giraffe/20261031T185451.759Z-afdc9a60,
  manifest sha256 per record; pass.edn rows verified 2026-10-31):

  - field order begins M-peradam-grounding, M-pattern-retrieval-calibration;
  - facts M-peradam-grounding {:status :ready :unblocks 2 :cone 3},
    M-pattern-retrieval-calibration {:status :ready :unblocks 2 :cone 2},
    M-typed-memories {:status :ready :unblocks 1 :cone 2};
  - retained :outer/selection recorded :cascade-scores
    [3.3781368587563056 3.3781368587563056 3.3781368587563056], a three-way
    tie, :tied-cascades all three ids, :tie? false, prediction mass 1.0 on
    [:stop-the-line :none], :selected-relation :unknown;
  - at the record's startedAt, focus-receipt classify-target returns
    {:class :unknown, :reason :embedding-node-not-retained} for
    M-peradam-grounding and M-typed-memories (stated relation absent,
    embedding corpus futon6 mission-embed is stale); measured over the whole
    field: 501/501 ready targets classify :unknown;
  - the real cascade menu is the outer-makeup v1 three-cascade menu.

  The defect: `ending-class` maps an unknown relation class to nil, so every
  pick's kernel degenerated to the stop-the-line kernel and all three
  cascades scored bit-identically -- ignorance scored as a deliberate stop."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-score :as outer-score]
            [futon2.aif.outer-select :as outer-select]))

(def makeup
  "The real outer-makeup v1 menu (test/fixtures/outer-consumption/outer-makeup.edn
  :cascades; makeup sha f3125835… pinned by the record)."
  {:cascades [{:id :what-others-wait-on-first
               :patterns ["only-what-can-start-now" "what-others-wait-on-first"]
               :steps [:ready-gate :by-cone :by-unblocks]}
              {:id :only-what-can-start-now
               :patterns ["only-what-can-start-now"]
               :steps [:ready-gate]}
              {:id :find-out-before-building
               :patterns ["find-out-before-building"]
               :steps [{:gate-status :undetermined} :by-cone]}]})

(def state
  "Starter kits from the makeup (the record's exact learned counts are not
  retained; the mechanism, not the exact 3.378…, is under test)."
  {:c {:focused 55 :related 35 :unrelated 5 :stop-the-line 5
       :not-yet-evaluated 0}
   :state-counts {:ready {:doable 3 :blocked 1}
                  :not-ready {:doable 1 :blocked 3}
                  :undetermined {:doable 1 :blocked 1}}
   :gain-counts {:none 1 :one 2 :several 3}})

(def facts
  "Pass.edn rows of the pinned Giraffe run, 2026-10-31."
  {"M-peradam-grounding" {:status :ready :unblocks 2 :cone 3}
   "M-pattern-retrieval-calibration" {:status :ready :unblocks 2 :cone 2}
   "M-typed-memories" {:status :ready :unblocks 1 :cone 2}})

(def field
  "Field order as the pinned pass orders the admitted rows."
  [{:target "M-peradam-grounding" :reach 1/2}
   {:target "M-pattern-retrieval-calibration" :reach 1/2}
   {:target "M-typed-memories" :reach 1/2}])

(def adc46631-observation
  "The adc46631 condition: every relation classifies :unknown (measured
  501/501 over the real field at the record's decision time)."
  {:focus "WM"
   :relation (fn [_ _] :unknown)
   :field field
   :facts facts})

(defn- refusal-of
  [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest unknown-relation-pick-is-refused-not-scored-as-stopped
  (testing "the adc46631 bad case: a whole unknown-relation field is a typed
            refusal with a producer request, not a three-way tie whose
            prediction reads as a deliberate stop-the-line"
    (let [refusal (refusal-of #(outer-select/select makeup state adc46631-observation))]
      (is (= :outer-score/no-known-relation-members (:kind refusal)))
      (is (= ["M-peradam-grounding" "M-pattern-retrieval-calibration" "M-typed-memories"]
             (mapv :target (:outer-score/excluded refusal))))
      (is (every? #(= :outer-score/relation-unavailable (:kind %))
                  (:outer-score/excluded refusal)))
      (is (every? #(= :relation-class-unknown (get-in % [:terms 0 :reason]))
                  (:outer-score/excluded refusal)))
      (is (= [:focus-receipt-classify-target]
             (distinct (mapv :producer (:knowledge-producer-requests refusal))))))))

(deftest known-relation-evidence-separates-the-scores
  (testing "one classified target (runs d5b9b513/e71c7380 really retained
            :selected-relation :associated for M-pattern-retrieval-calibration)
            separates the menu: the picking cascades score a real model, the
            gate-emptied cascade stays the stopped model, and the picks are
            retained beside the scores"
    (let [observation (assoc adc46631-observation
                             :relation (fn [_ target]
                                         (when (= target "M-pattern-retrieval-calibration")
                                           :associated)))
          result (outer-select/select makeup state observation)
          {:keys [cascade-scores cascade-picks]} result]
      ;; The unknown-relation candidates never reach scoring.
      (is (= ["M-pattern-retrieval-calibration"] (:outer-score/admitted result)))
      (is (= "M-pattern-retrieval-calibration"
             (get-in result [:selection :selected :target])))
      ;; Per-cascade picks are retained: same pick, same pick, no pick.
      (is (= ["M-pattern-retrieval-calibration"
              "M-pattern-retrieval-calibration"
              :no-pick]
             cascade-picks))
      ;; The picking cascades score strictly below the stopped model, so the
      ;; tie is gone and the choice rests on real evidence.
      (is (< (nth cascade-scores 0) (nth cascade-scores 2)))
      (is (< (nth cascade-scores 1) (nth cascade-scores 2)))
      ;; Same-pick equal scores are not a choice tie.
      (is (= (nth cascade-scores 0) (nth cascade-scores 1)))
      (is (false? (:tie? result)))
      ;; The degenerate stop-the-line prediction is gone: the landing is a
      ;; valued outcome class and mass leaves [:stop-the-line :none].
      (let [outcomes (get-in result [:prediction :outcomes])]
        (is (= [:related :several] (get-in result [:prediction :landing])))
        (is (pos? (get outcomes [:related :several])))
        (is (< (get outcomes [:stop-the-line :none]) 1.0))))))

(deftest chosen-model-novelty-now-contributes
  (testing "with a known relation the Fisher novelty of the chosen model is
            positive (it was identically 0.0 in every degenerate tie), and
            the outer score is risk+ambiguity minus that novelty"
    (let [observation (assoc adc46631-observation
                             :relation (fn [_ target]
                                         (when (= target "M-pattern-retrieval-calibration")
                                           :associated)))
          result (outer-select/select makeup state observation)
          input {:focus (:focus observation) :relation (:relation observation)
                 :facts facts
                 :field [{:target "M-pattern-retrieval-calibration" :reach 1/2}]
                 :state-counts (:state-counts state)
                 :ending-counts (:c state) :gain-counts (:gain-counts state)
                 :cascade (:steps (nth (:cascades makeup) (:cascade-index result)))}
          model (outer-score/outer-model input)
          novelty (outer-score/outer-novelty model)]
      (is (pos? novelty))
      (is (= (nth (:cascade-scores result) (:cascade-index result))
             (outer-score/outer-score input))))))

(deftest mixed-unknown-field-keeps-known-members-and-requests
  (testing "a field where only part classifies unknown admits the known rest
            and retains one exclusion and request per unknown target"
    (let [observation (assoc adc46631-observation
                             :relation (fn [_ target]
                                         (when (contains? #{"M-pattern-retrieval-calibration"
                                                            "M-typed-memories"}
                                                         target)
                                           (if (= target "M-typed-memories")
                                             :useful-elsewhere
                                             :associated))))
          result (outer-select/select makeup state observation)]
      (is (= ["M-pattern-retrieval-calibration" "M-typed-memories"]
             (:outer-score/admitted result)))
      (is (= ["M-peradam-grounding"]
             (mapv :target (filter #(= :outer-score/relation-unavailable (:kind %))
                                   (:outer-score/excluded result)))))
      ;; by-cone keeps M-pattern (cone 2, :associated) over M-typed
      ;; (cone 2, :useful-elsewhere) only on the stable field order.
      (is (= "M-pattern-retrieval-calibration"
             (get-in result [:selection :selected :target]))))))
