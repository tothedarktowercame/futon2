(ns futon2.aif.legacy-three-halves-registry-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.legacy-three-halves-registry :as sut]
            [futon2.aif.three-halves-square :as square]))

(def target "M-interim-director-proxy-metric-inventory")
(def pattern :orchestration/recorded-handoff)
(def patterns
  {pattern "232ec874e6061ce48f7edc39cc6a7029c7005a934c5769abd26cf4ed93c30620"
   :ukrns/reader-run-path "d86ea60823aa9647ff460f510bf5b394490b224e8a911b6ad3677ffc50dcd06b"
   :war-machine/state-capture "b45598fb80725dfb4b7deadf520355feaca633dc9d1238c037c7fad654b6d363"
   :measurement/warrant-travels-with-the-number "415a41bc2fc385ebc02e7cae2dc20d0b484790afa9b14a7958fc66853652e7e3"})
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

(deftest every-declared-legacy-interpretation-has-a-valid-pinned-square
  (doseq [[pattern-id source-sha] patterns]
    (let [receipt {:source {:path (str "futon3/library/" (namespace pattern-id) "/"
                                      (name pattern-id) ".flexiarg")
                            :sha256 source-sha}}
          result (sut/square "/home/joe/code" target pattern-id receipt)]
      (is (= :valid (:status (square/validate result))) (str pattern-id))
      (is (= pattern-id (get-in result [:migration :pattern]))))))

(deftest substitutions-have-no-migration-certificate
  (is (nil? (sut/square "/home/joe/code" "M-other" pattern receipt)))
  (is (nil? (sut/square "/home/joe/code" target :other/pattern receipt)))
  (is (nil? (sut/square "/home/joe/code" target pattern
                        (assoc-in receipt [:source :sha256] (apply str (repeat 64 "0")))))))
