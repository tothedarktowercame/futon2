(ns futon2.patchboard-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.patchboard :as patchboard]))

(deftest figure-eight-clamp-map-agrees-with-reference-model
  (let [shape (patchboard/analyse (patchboard/clamp-shift-wiring 8))]
    (is (= [7] (:free shape)))
    (is (= [1 2 3 4 5 6] (:chain shape)))
    (is (= [0] (:unsat shape)))
    (is (= [{:bits "00101010" :value 42}
            {:bits "10101010" :value 170}]
           (:attractors shape)))))

(deftest identity-is-all-unsat-and-has-no-scaffold
  (testing "MetaCA identity"
    (let [shape (patchboard/analyse (patchboard/identity-wiring 8))]
      (is (empty? (:free shape)))
      (is (empty? (:chain shape)))
      (is (= (vec (range 8)) (:unsat shape)))
      (is (= 256 (count (:attractors shape))))))
  (testing "ant identity"
    (let [shape (patchboard/analyse (patchboard/identity-wiring 14)
                                    {:derive-attractors? false})]
      (is (empty? (:free shape)))
      (is (= (vec (range 14)) (:unsat shape))))))

(deftest non-injective-wirings-are-first-class
  (let [wiring [0 0 1 2 3 4 5 6]
        shape (patchboard/analyse wiring)]
    (is (patchboard/valid-wiring? wiring))
    (is (= 2 (count (filter zero? wiring))))
    (is (= [7] (:free shape)))))

(deftest malformed-wirings-fail-closed
  (is (false? (patchboard/valid-wiring? [0 2])))
  (is (thrown? clojure.lang.ExceptionInfo (patchboard/analyse [0 2])))
  (is (thrown? clojure.lang.ExceptionInfo
               (patchboard/analyse [0] {:pin 0.5}))))

(deftest terminal-abi-is-explicit
  (is (= 8 (count patchboard/metaca-terminals)))
  (is (= ["000" "001" "010" "100" "011" "101" "110" "111"]
         (mapv :condition patchboard/metaca-terminals)))
  (is (= 14 (count patchboard/ant-terminals)))
  (is (= #{:food-trace :pher-trace :friendly-home :novelty}
         (->> patchboard/ant-terminals
              (filter #(and (nil? (:outbound %)) (nil? (:homebound %))))
              (map :channel)
              set))))
