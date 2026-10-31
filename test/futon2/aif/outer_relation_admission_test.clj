(ns futon2.aif.outer-relation-admission-test
  "TIED-OUTER-CASCADES (2026-10-31, claude-1 decision round).  Fixtures are
  taken from the retained run record
  tick-run-record-2026-10-31-adc46631-2156-4c84-850f-5abaa3aeb5fd.edn and the
  Giraffe field it pins (storage/giraffe/20261031T185451.759Z-afdc9a60):

  - field order begins M-peradam-grounding, M-pattern-retrieval-calibration;
  - facts M-peradam-grounding {:status :ready :unblocks 2 :cone 3},
    M-pattern-retrieval-calibration {:status :ready :unblocks 2 :cone 2},
    M-typed-memories {:status :ready :unblocks 1 :cone 2};
  - the retained record scored a three-way tie at 3.3781368587563056 with
    prediction mass 1.0 on [:stop-the-line :none] and :selected-relation
    :unknown -- the picks were relation-unknown targets (M-peradam-grounding
    classifies {:class :unknown, :reason :embedding-node-not-retained});
  - runs d5b9b513/e71c7380 really retained :selected-relation :associated for
    M-pattern-retrieval-calibration (embedding-neighbour channel).

  The decision: a relation-unknown candidate is NOT excluded -- Joe's rule
  says the machine must not abstain, and a small field is a failure.  Its
  scored stop-the-line landing stays (DarkTower scorerClass maps an unknown
  relation to none, hence the stop landing; a :not-yet-evaluated landing is
  inadmissible under the c-kit, measured on the real fields -- row report
  addendum 3), and the ignorance is retained beside each pick as
  :cascade-pick-relations."
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
  "The adc46631 condition at the record's decision time: every pick's
  relation classifies :unknown."
  {:focus "WM"
   :relation (fn [_ _] :unknown)
   :field field
   :facts facts})

(deftest unknown-relation-field-is-admitted-and-selects-with-ignorance-retained
  (testing "the adc46631 bad case, decided: no refusal and no exclusion -- the
            full field is admitted, selection proceeds, and each pick carries
            its relation so the degenerate stop reads as ignorance, not as a
            deliberate stop"
    (let [result (outer-select/select makeup state adc46631-observation)]
      (is (= ["M-peradam-grounding" "M-pattern-retrieval-calibration"
              "M-typed-memories"]
             (:outer-score/admitted result)))
      (is (empty? (:outer-score/excluded result)))
      ;; The picks are all relation-unknown, retained as such beside the picks.
      (is (= ["M-peradam-grounding" "M-peradam-grounding" :no-pick]
             (:cascade-picks result)))
      (is (= [{:target "M-peradam-grounding" :relation :unknown :relation-known? false}
              {:target "M-peradam-grounding" :relation :unknown :relation-known? false}
              {:target :no-pick :relation :no-pick :relation-known? nil}]
             (:cascade-pick-relations result)))
      (is (= "M-peradam-grounding"
             (get-in result [:selection :selected :target])))
      ;; Ignorance still scores as the stop landing (DarkTower scorerClass):
      ;; the prediction is the stopped model, but the record shows why.
      (is (= 1.0 (get-in result [:prediction :outcomes [:stop-the-line :none]])))
      (is (= [:stop-the-line :none] (get-in result [:prediction :landing]))))))

(deftest known-relation-evidence-separates-the-scores
  (testing "a classified field head (runs d5b9b513/e71c7380 really retained
            :selected-relation :associated for their pick) separates the
            menu: the picking cascades score a real model while the
            gate-emptied cascade stays stopped; the pick's known relation is
            retained beside the score"
    (let [observation (assoc adc46631-observation
                             :relation (fn [_ target]
                                         (if (= target "M-peradam-grounding")
                                           :associated
                                           :unknown)))
          result (outer-select/select makeup state observation)
          {:keys [cascade-scores cascade-pick-relations]} result]
      (is (= "M-peradam-grounding"
             (get-in result [:selection :selected :target])))
      (is (= [{:target "M-peradam-grounding" :relation :associated
               :relation-known? true}
              {:target "M-peradam-grounding" :relation :associated
               :relation-known? true}
              {:target :no-pick :relation :no-pick :relation-known? nil}]
             cascade-pick-relations))
      ;; The picking cascades score strictly below the stopped model, so the
      ;; choice rests on real evidence.
      (is (< (nth cascade-scores 0) (nth cascade-scores 2)))
      (is (< (nth cascade-scores 1) (nth cascade-scores 2)))
      ;; Same-pick equal scores are not a choice tie.
      (is (= (nth cascade-scores 0) (nth cascade-scores 1)))
      (is (false? (:tie? result)))
      ;; The degenerate stop-the-line prediction is gone for the pick.
      (let [outcomes (get-in result [:prediction :outcomes])]
        (is (= [:related :several] (get-in result [:prediction :landing])))
        (is (pos? (get outcomes [:related :several])))
        (is (< (get outcomes [:stop-the-line :none]) 1.0))))))

(deftest a-deliberately-stopped-cascade-still-scores-the-stopped-model
  (testing "the gate-emptied cascade (no candidate passes its gate) keeps the
            stopped kernel and landing -- removing the relation exclusion
            changed nothing for genuinely stopped cascades"
    (let [result (outer-select/select makeup state adc46631-observation)
          input {:focus (:focus adc46631-observation)
                 :relation (:relation adc46631-observation)
                 :facts facts :field field
                 :state-counts (:state-counts state)
                 :ending-counts (:c state) :gain-counts (:gain-counts state)
                 :cascade (:steps (nth (:cascades makeup) 2))}
          model (outer-score/outer-model input)]
      (is (nil? (:pick model)))
      (is (= [:stop-the-line :none] (:landing model)))
      (is (= (nth (:cascade-scores result) 2)
             (outer-score/outer-score input))))))

(deftest chosen-model-novelty-now-contributes
  (testing "with a known relation the Fisher novelty of the chosen model is
            positive (it was identically 0.0 in every degenerate tie), and
            the outer score is risk+ambiguity minus that novelty"
    (let [observation (assoc adc46631-observation
                             :relation (fn [_ target]
                                         (if (= target "M-peradam-grounding")
                                           :associated
                                           :unknown)))
          result (outer-select/select makeup state observation)
          input {:focus (:focus observation) :relation (:relation observation)
                 :facts facts
                 :field [{:target "M-peradam-grounding" :reach 1/2}]
                 :state-counts (:state-counts state)
                 :ending-counts (:c state) :gain-counts (:gain-counts state)
                 :cascade (:steps (nth (:cascades makeup) (:cascade-index result)))}
          model (outer-score/outer-model input)
          novelty (outer-score/outer-novelty model)]
      (is (pos? novelty))
      (is (= (nth (:cascade-scores result) (:cascade-index result))
             (outer-score/outer-score input))))))
