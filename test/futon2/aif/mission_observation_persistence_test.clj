(ns futon2.aif.mission-observation-persistence-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-cohort-test :as fixture]))

(def baseline
  {:schema :wm/mission-observation-baseline-v1 :status :captured
   :baseline-sha256 (apply str (repeat 64 "a"))
   :occurrence-id "occurrence" :revision "before" :rows []})

(deftest durable-baseline-is-before-dispatch-and-replay-stable
  (let [root (fixture/tmp-root)
        _ (cohort/activate! fixture/prereg-path root)
        attempt (:attempt/id (fixture/open! root "mission-baseline/order"))]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"construction must precede"
                          (cohort/append-pre-author-baseline!
                           fixture/prereg-path root attempt baseline)))
    (cohort/append-checkpoint! fixture/prereg-path root attempt :selection
                               {:sorry {:kind :test-selection}})
    (cohort/append-checkpoint! fixture/prereg-path root attempt :construction
                               {:sorry {:kind :test-construction}})
    (let [saved (cohort/append-pre-author-baseline!
                 fixture/prereg-path root attempt baseline)]
      (is (= baseline (:baseline saved)))
      (is (= saved (cohort/read-pre-author-baseline fixture/prereg-path root attempt)))
      (is (= saved (cohort/append-pre-author-baseline!
                    fixture/prereg-path root attempt baseline))
          "exact resume/replay is inert")
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"conflicting"
                            (cohort/append-pre-author-baseline!
                             fixture/prereg-path root attempt
                             (assoc baseline :revision "rebound"))))
      (cohort/append-checkpoint! fixture/prereg-path root attempt :dispatch
                                 {:sorry {:kind :test-dispatch}})
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"must precede dispatch"
                            (cohort/append-pre-author-baseline!
                             fixture/prereg-path root attempt baseline))))))
