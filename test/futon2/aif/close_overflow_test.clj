(ns futon2.aif.close-overflow-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.wm.cascade-decision :as decision])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest cohort-checkpoint-writer-streams-large-edn
  (let [root (.toFile (Files/createTempDirectory
                       "cohort-stream-" (make-array FileAttribute 0)))
        path (io/file root "selection.edn")
        value {:checkpoint :selection
               :candidates (vec (repeat 20000 {:id {:target "T-1"
                                                    :precedence [:a :b]}
                                               :g 1.25}))}]
    (#'cohort/write-new! path value)
    (is (= value (edn/read-string (slurp path))))
    (is (> (.length path) 1000000))))

(deftest numeric-g-count-uses-production-candidate-shape
  (let [lanes [{:target "T-a"} {:target "T-b"} {:target "T-c"}]
        candidates [{:action {:target "T-a"} :controller-score 1.0}
                    {:id {:target "T-b"} :G-efe 2.0}
                    {:action {:target "T-c"} :controller-score :infinite}
                    {:action {:target "outside"} :controller-score 3.0}]]
    (is (= 2 (decision/numeric-g-target-count lanes candidates)))))
