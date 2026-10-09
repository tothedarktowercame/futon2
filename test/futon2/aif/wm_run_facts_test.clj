(ns futon2.aif.wm-run-facts-test
  (:require [clojure.test :refer [deftest is]]
            [wm-run-facts :as facts]))

(def snap {:open-missions #{"M-x"} :open-excursions #{} :open-tickets #{}
           :patterns #{"p1" "p2"} :seats #{"author"}
           :pins {:test true}})

(def record
  {:decision {:selection-certificate
              {:candidates [{:id :c1 :target "M-x" :precedence [:p1]
                             :g 1.0}]
               :policies [{:id :pi1}]}}
   :participants {:author "author"}
   :terminal-receipt {:outcome :changed}})

(deftest typed-not-supplied-increments-absence-count
  (let [base (facts/facts-for-record record "r" snap nil nil)
        planted (assoc-in record [:decision :selection-certificate :f]
                          {:status :not-supplied :reason :plant})
        changed (facts/facts-for-record planted "r" snap nil nil)]
    (is (= (inc (get-in base [:facts "pathAbsenceCount"]))
           (get-in changed [:facts "pathAbsenceCount"])))))

(deftest missing-slice-is-not-recomputable-not-zero-or-conforming
  (let [export (facts/facts-for-record record "r" snap nil nil)
        construction (get-in export [:facts "targetConstruction"])]
    (is (contains? construction "not-recomputable"))
    (is (not= 0 construction))
    (is (not= [] construction))))

(deftest recorded-construction-is-exported-with-the-actual-union-pool
  (let [carried (assoc-in record [:decision :selection-certificate :target-construction]
                          [{:target "M-x" :slice [:p1 :p2] :pool [:p1 :p2]
                            :slice-from-whole-library true :library-size 1436
                            :policy-count 2}])
        exported (:facts (facts/facts-for-record carried "r" snap nil nil))]
    (is (= [{"targets" ["M-x"] "slice" [":p1" ":p2"]
             "pool" [":p1" ":p2"] "sliceFromWholeLibrary" true
             "policyCount" 2}]
           (exported "targetConstruction")))
    (is (= 2 (exported "constructorPatternCount")))))

(deftest typed-absent-construction-carrier-is-never-read-as-an-empty-pool
  (let [carried (assoc-in record [:decision :selection-certificate :target-construction]
                          {:status :absent :reason :query-time-construction-slice-not-recorded})
        exported (:facts (facts/facts-for-record carried "r" snap nil nil))]
    (is (contains? (exported "targetConstruction") "not-recomputable"))
    (is (contains? (exported "constructorPatternCount") "not-recomputable"))))

(deftest runfacts-field-set-is-exact
  (let [exported (:facts (facts/facts-for-record record "r" snap nil nil))]
    (is (= (set facts/run-fact-fields) (set (keys exported))))))

(deftest new-world-carrier-removes-record-nr-fields
  (let [world {:open-tasks {:missions {:ids ["M-x"]}
                            :excursions {:ids []} :tickets {:ids []}}
               :enumerated-tasks {:ids ["M-x"]}
               :seat-roster {:codex {:ids ["author"]}}
               :selection-input-digest "digest"
               :selection-ended-at "2026-09-30T00:00:00Z"
               :interpretation-issued-at nil
               :failures []}
        exported (:facts (facts/facts-for-record
                          (assoc record :world-at-selection world)
                          "r" snap nil nil))]
    (is (= ["M-x"] (exported "enumeratedTasks")))
    (is (= "digest" (exported "currentInputDigest")))
    (is (= ["author"] (exported "seatsAvailable")))
    (is (= "selectionBeforeInterpretation" (exported "interpretationOrder")))))
