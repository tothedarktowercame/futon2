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

  RELATION-2 supersedes the historical decision described by this fixture:
  Lean scorerClass maps an unknown relation to none, so it has no selectable
  scored class. Unknown candidates remain enumerated but are typed-excluded."
  (:require [futon2.aif.outer-menu-fixture :as menu-fixture]
            [clojure.test :refer [deftest is]]
            [futon2.aif.outer-select :as outer-select]))

(clojure.test/use-fixtures :each menu-fixture/with-fixture-constructor)

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

(defn selection-result [observation]
  (try (outer-select/select makeup state observation)
       (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest unrecognised-relation-is-enumerated-but-typed-excluded
  (let [unknown "T-unknown"
        known "T-known"
        unknown-receipt {:class :unknown
                         :relation {:status :absent :reason :no-area-row}
                         :derived-via {:corpus-sha256 "corpus-pin"
                                       :focus-manifest-sha256 "focus-pin"}}
        known-receipt {:class :associated
                       :relation {:status :present}}
        observation {:focus "WM"
                     :relation (fn [_ target]
                                 (if (= unknown target) :unknown :associated))
                     :field [{:target unknown :reach 1/2}
                             {:target known :reach 1/2}]
                     :facts {unknown {:status :ready :unblocks 9 :cone 9
                                      :relation {:receipt unknown-receipt}}
                             known {:status :ready :unblocks 1 :cone 1
                                    :relation {:receipt known-receipt}}}
                     :field-facts {:enumerated [unknown known]
                                   :admitted {unknown {:status :ready}
                                              known {:status :ready}}
                                   :excluded []}}
        r (outer-select/select
           {:cascades [{:id :winner :patterns ["fixture"]
                        :steps [:ready-gate :by-cone]}]}
           state observation)
        exclusion (first (:outer-score/excluded r))]
    (is (= [unknown known] (get-in r [:field-facts :enumerated])))
    (is (= [known] (:outer-score/admitted r)))
    (is (= 1 (count (:outer-score/excluded r))))
    (is (= {:target unknown :reason :relation-unrecognised
            :relation-class :unknown :relation-reason :no-area-row
            :corpus-sha256 "corpus-pin"
            :focus-manifest-sha256 "focus-pin"}
           (select-keys exclusion [:target :reason :relation-class :relation-reason
                                   :corpus-sha256 :focus-manifest-sha256])))
    (is (= known (get-in r [:selection :selected :target])))
    (is (= :associated (get-in r [:cascades 0 :relation :class])))
    (is (string? (get-in r [:cascades 0 :relation :receipt-digest])))))

(deftest known-relations-score-below-stop-with-one-prediction-class
  (let [observation (assoc adc46631-observation :relation (fn [_ _] :associated))
        r (selection-result observation) e r]
    (is (nil? (:kind r)))
    (is (= [:what-others-wait-on-first :only-what-can-start-now] (:members (first (:classes r)))))
    (is (= [:associated :associated :no-pick] (mapv :relation (:cascade-pick-relations e))))
    (is (< (first (:cascade-scores e)) (get-in e [:abstention :G])))
    (is (pos? (get-in e [:cascades 0 :terms :novelty])))
    (is (= (nth (:cascade-scores e) 2) (get-in e [:abstention :G])))
    (is (= [:stop-the-line :none] (get-in e [:cascades 2 :prediction :landing])))))
