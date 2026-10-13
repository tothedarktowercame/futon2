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
    (is (= pattern (get-in result [:migration :pattern])))
    (is (= "75a8378a9bdc8672242c5f471c2824dc28583532"
           (get-in result [:objects :G :source :revision])))
    (is (= "860a5595a58d62866b1013715934ae23934cae49"
           (get-in result [:objects :I1 :source :revision])))))

(deftest substitutions-have-no-migration-certificate
  (is (nil? (sut/square "/home/joe/code" "M-other" pattern receipt)))
  (is (nil? (sut/square "/home/joe/code" target :other/pattern receipt)))
  (is (nil? (sut/square "/home/joe/code" target pattern
                        (assoc-in receipt [:source :sha256] (apply str (repeat 64 "0")))))))
