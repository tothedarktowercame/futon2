(ns futon2.aif.focus-class-terminal-preference-test
  "C6 conformance: the frozen focus gates the target class supplied to the
  real family scorer; an out-of-focus target refuses rather than taking the
  scorer's direct-call :focused default."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.focus-receipt :as focus]
            [futon2.aif.wm.family-selection :as family-selection]))

(def as-of "2026-10-23T12:00:00Z")
(def target-x "M-focus-fixture-x")
(def target-y "M-focus-fixture-y")
(def target-missing "M-focus-fixture-missing")

(def relation-rows
  [{:target target-x :facet "WM" :relation "focus"
    :effective-from "2026-10-01T00:00:00Z"
    :source {:repo "fixture" :commit "fixture" :path "focus-fixture"}}
   {:target target-y :facet "WM" :relation "useful-elsewhere"
    :effective-from "2026-10-01T00:00:00Z"
    :source {:repo "fixture" :commit "fixture" :path "focus-fixture"}}])

(defn focus-inputs
  "The sole varying input is the path that establishes the focus: aif -> WM,
  apm -> APM.  Both inputs retain the same target relation rows."
  [focus-path]
  {:windows [{:from "2026-10-23T00:00:00Z"
              :until "2026-10-24T00:00:00Z"
              :valid-through "2026-10-24T00:00:00Z"
              :commits [{:at "2026-10-23T01:00:00Z" :paths [focus-path]}]}]
   :facet-edges []
   :relations relation-rows})

(defn classification-map [inputs]
  (let [focus-info (focus/decision-focus-info inputs as-of)]
    {:focus-info focus-info
     :target-classes
     (into {}
           (for [target [target-x target-y]
                 :let [classification (focus/classify-target inputs focus-info as-of target)]]
             [target {:class (:class classification)
                      :scorer-class (get focus/scorer-class (:class classification))
                      :reason (when (= :unknown (:class classification))
                                (get-in classification [:relation :reason]))
                      :provenance {:status :frozen-focus
                                   :focus-status (:status focus-info)}}]))}))

(defn policy [target]
  {:target target
   :policy-id (str target "/policy")
   :kind :fixture
   :analysis {:sentences []}
   :cascade {:nodes [{:pattern (str target "/pattern") :fragment-index 0}]
             :edges []}})

(defn family [target]
  {:status :computed :target-id target :distinct-count 1 :reported-count 1
   :provenance {:analysis-digest "fixture-analysis" :excerpt-digest "fixture-excerpt"}
   :policies [(policy target)] :failures [] :failure-count 0})

(defn score [target target-classes]
  (-> (family-selection/score-family-field [(family target)] target-classes)
      first :scored first second))

(deftest frozen-focus-classes-control-the-terminal-emission-and-unknown-refuses
  (let [{a-focus :focus-info a-classes :target-classes}
        (classification-map (focus-inputs "src/futon2/aif/fixture.clj"))
        {b-focus :focus-info b-classes :target-classes}
        (classification-map (focus-inputs "src/apm/fixture.clj"))
        ax (score target-x a-classes)
        ay (score target-y a-classes)
        by (score target-y b-classes)
        b-x-row (first (family-selection/score-family-field [(family target-x)] b-classes))]
    (testing "real focus discovery and classification"
      (is (= "WM" (:focus a-focus)))
      (is (= "APM" (:focus b-focus)))
      (is (= :focus (get-in a-classes [target-x :class])))
      (is (= :useful-elsewhere (get-in a-classes [target-y :class])))
      (is (= :unknown (get-in b-classes [target-x :class])))
      (is (= :relation-outside-focus-facets (get-in b-classes [target-x :reason])))
      (is (= :useful-elsewhere (get-in b-classes [target-y :class]))))
    (testing "the same family scorer consumes focused versus elsewhere classes"
      (is (= :computed (:status ax)))
      (is (= :computed (:status ay)))
      (is (= :computed (:status by)))
      (is (= :focused (get-in ax [:class-emission :model :target-class target-x])))
      (is (= :unrelated (get-in ay [:class-emission :model :target-class target-y])))
      (is (= :unrelated (get-in by [:class-emission :model :target-class target-y])))
      (is (= 55/100 (get-in ax [:class-emission :model :class-preference
                                 (:horizon ax) :focused])))
      (is (= 5/100 (get-in ay [:class-emission :model :class-preference
                                (:horizon ay) :unrelated])))
      (is (= 5/100 (get-in by [:class-emission :model :class-preference
                                (:horizon by) :unrelated]))))
    (testing "out-of-focus X is refused, rather than silently made focused"
      (is (empty? (:scored b-x-row)))
      (is (= :target-class-unknown (get-in b-x-row [:failures 0 :kind])))
      (is (= :relation-outside-focus-facets (get-in b-x-row [:failures 0 :reason]))))))

(deftest target-absent-from-a-nonempty-class-map-refuses-instead-of-defaulting
  (let [target-classes {target-x {:class :focus :scorer-class :focused}}
        row (first (family-selection/score-family-field [(family target-missing)]
                                                        target-classes))]
    (is (empty? (:scored row)))
    (is (= :target-class-required (get-in row [:failures 0 :kind])))))
