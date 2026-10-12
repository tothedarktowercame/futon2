(ns futon2.aif.legacy-three-halves-registry-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.legacy-three-halves-registry :as sut]
            [futon2.aif.three-halves-square :as square]))

(def target "M-interim-director-proxy-metric-inventory")
(def pattern :orchestration/recorded-handoff)
(def receipt
  {:source {:path "futon3/library/orchestration/recorded-handoff.flexiarg"
            :sha256 "232ec874e6061ce48f7edc39cc6a7029c7005a934c5769abd26cf4ed93c30620"}})

(deftest exact-legacy-interpretation-has-a-valid-pinned-square
  (let [result (sut/square "/home/joe/code" target pattern receipt)]
    (is (= :valid (:status (square/validate result))))
    (is (= target (get-in result [:migration :target])))
    (is (= pattern (get-in result [:migration :pattern])))))

(deftest substitutions-have-no-migration-certificate
  (is (nil? (sut/square "/home/joe/code" "M-other" pattern receipt)))
  (is (nil? (sut/square "/home/joe/code" target :other/pattern receipt)))
  (is (nil? (sut/square "/home/joe/code" target pattern
                        (assoc-in receipt [:source :sha256] (apply str (repeat 64 "0")))))))
