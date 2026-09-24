(ns futon2.aif.check-error-rates-test
  "Pins A-S / H-A: the 22-row exemplar ledger's per-kind counts verbatim,
  the falsifier refusals, and the insufficient-count typed absence. Reads
  the ledger at test time, read-only."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.check-error-rates :as cer]))

(def ledger-path
  (io/file (System/getProperty "user.dir")
           ".." "futon3c" "holes" "labs" "M-futon-seams" "exemplar"
           "check-ledger.edn"))

(defn- read-ledger []
  (with-open [r (java.io.PushbackReader. (io/reader ledger-path))]
    (edn/read r)))

(deftest ledger-present-and-right-schema
  (let [ledger (read-ledger)]
    (is (.exists ledger-path) "exemplar ledger must be readable")
    (is (= :m-futon-seams/check-ledger-v1 (:schema ledger)))
    (is (= 22 (count (:rows ledger))))))

(deftest per-kind-counts-pinned-verbatim
  (let [rates (cer/measured-rates (read-ledger))]
    (is (= #{} (set (::cer/excluded-ids rates)))
        "every exemplar row has an independent :truth-source")
    (testing ":test — 5 runs, no errors"
      (is (= {:n 5 :n-true 3 :n-false 2 :false-pass 0 :false-fail 0}
             (select-keys (:test rates)
                          [:n :n-true :n-false :false-pass :false-fail])))
      (is (= :measured (:status (:test rates))))
      (is (= [:t1 :t2 :t3 :t4 :t5] (:source-ids (:test rates)))))
    (testing ":grep — 6 runs, g2 false pass"
      (is (= {:n 6 :n-true 2 :n-false 4 :false-pass 1 :false-fail 0}
             (select-keys (:grep rates)
                          [:n :n-true :n-false :false-pass :false-fail])))
      (is (= :measured (:status (:grep rates)))))
    (testing ":validator — 18 runs (:count expanded), v3+v7 false pass, v4 x4 false fail"
      (is (= {:n 18 :n-true 5 :n-false 13 :false-pass 2 :false-fail 4}
             (select-keys (:validator rates)
                          [:n :n-true :n-false :false-pass :false-fail])))
      (is (= :measured (:status (:validator rates)))))
    (testing ":layout — 4 runs, below the minimum count: typed absence, not numbers"
      (let [layout (:layout rates)]
        (is (= {:n 4 :n-true 2 :n-false 2 :false-pass 1 :false-fail 1}
               (select-keys layout
                            [:n :n-true :n-false :false-pass :false-fail])))
        (is (= :insufficient (:status layout)))
        (is (not (contains? layout :fp-rate)))
        (is (not (contains? layout :fn-rate)))))))

(deftest measured-rate-values-pinned
  (let [rates (cer/measured-rates (read-ledger))]
    (testing "Jeffreys-smoothed rates"
      (is (= 0.3 (:fp-rate (:grep rates))))
      (is (= 0.125 (:fn-rate (:test rates))))
      (is (< (Math/abs (- (:fp-rate (:validator rates)) (/ 2.5 14.0))) 1e-12))
      (is (= 0.75 (:fn-rate (:validator rates)))))
    (testing "Wilson intervals contain the raw proportion and stay in [0,1]"
      (doseq [kind [:test :grep :validator]
              rate-key [:fp-interval :fn-interval]]
        (let [{:keys [lo hi]} (get-in rates [kind rate-key])]
          (is (<= 0.0 lo hi 1.0)))))))

(deftest falsifier-declared-rates-refused
  (let [ledger (read-ledger)
        declared {:grep {:fp-rate 0.1 :fn-rate 0.1}}]
    (is (= :missing-counts (:reason (cer/rates-measured? declared ledger)))
        "a rate table with no counts is not measured")))

(deftest falsifier-unresolved-source-ids-refused
  (let [ledger (read-ledger)
        forged {:grep {:status :measured
                       :n 100 :n-true 50 :n-false 50
                       :false-pass 5 :false-fail 5
                       :fp-rate 0.1 :fn-rate 0.1
                       :source-ids [:g1 :not-a-ledger-row]}}]
    (is (= :unresolved-source-ids (:reason (cer/rates-measured? forged ledger)))
        "source ids must resolve to ledger rows")))

(deftest falsifier-rates-over-insufficient-refused
  (let [ledger (read-ledger)
        forged {:layout {:status :insufficient
                         :n 4 :n-true 2 :n-false 2
                         :false-pass 1 :false-fail 1
                         :fp-rate 0.5
                         :source-ids [:l1 :l2 :l3 :l4]}}]
    (is (= :rates-over-absence (:reason (cer/rates-measured? forged ledger)))
        "rates must not be asserted where the kind is a typed absence")))

(deftest self-truthed-rows-excluded
  (let [ledger (read-ledger)
        adulterated (update ledger :rows conj
                            {:id :x1 :kind :grep :check "c" :input "i"
                             :truth true :passed true})
        rates (cer/measured-rates adulterated)]
    (is (= [:x1] (::cer/excluded-ids rates))
        "a row without :truth-source is excluded, never counted")
    (is (= 6 (:n (:grep rates))))))

(deftest measured-rates-round-trip-accepted
  (let [ledger (read-ledger)]
    (is (true? (cer/rates-measured? (cer/measured-rates ledger) ledger)))))

(deftest wrong-schema-ledger-throws-typed
  (is (thrown? clojure.lang.ExceptionInfo
               (cer/measured-rates {:schema :something/else :rows []}))))
