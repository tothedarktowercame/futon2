(ns futon2.aif.check-error-rates-test
  "Pins A-S / H-A, Revision 2: eligibility by truth KIND. The 22-row
  exemplar ledger runs under the recorded classification fixture
  (test/fixtures/check-ledger-classification/m-futon-seams-v1.edn); the
  per-kind counts are pinned to what that classification yields. The
  one-arg form (no classification) is pinned to exclude every row as
  :unclassified. claude-8's review bad case (six self-truthed rows with
  free-text :truth-source) is a test verbatim. Reads the ledger and the
  fixture at test time, read-only."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.check-error-rates :as cer]))

(def ledger-path
  (io/file (System/getProperty "user.dir")
           ".." "futon3c" "holes" "labs" "M-futon-seams" "exemplar"
           "check-ledger.edn"))

(def classification-path
  (io/file (System/getProperty "user.dir")
           "test" "fixtures" "check-ledger-classification"
           "m-futon-seams-v1.edn"))

(defn- read-edn [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (edn/read r)))

(defn- read-ledger [] (read-edn ledger-path))

(defn- read-classification [] (read-edn classification-path))

(deftest ledger-present-and-right-schema
  (let [ledger (read-ledger)]
    (is (.exists ledger-path) "exemplar ledger must be readable")
    (is (= :m-futon-seams/check-ledger-v1 (:schema ledger)))
    (is (= 22 (count (:rows ledger))))))

(deftest classification-present-and-covers-every-row
  (let [ledger (read-ledger)
        classification (read-classification)]
    (is (.exists classification-path) "classification fixture must be readable")
    (is (some? (:classification-source classification))
        "the classification names who classified and when")
    (doseq [row (:rows ledger)]
      (is (contains? classification (:id row))
          (str "every ledger row is classified: " (:id row))))))

(deftest one-arg-form-excludes-everything-unclassified
  (let [rates (cer/measured-rates (read-ledger))]
    (is (= (into {} (map (fn [row] [(:id row) :unclassified]))
                 (:rows (read-ledger)))
           (::cer/excluded-ids rates))
        "a ledger nobody has classified: every row excluded :unclassified")
    (is (= {:status :absent :reason :no-classification}
           (::cer/classification-source rates)))
    (doseq [kind [:test :grep :validator :layout]]
      (is (= :insufficient (:status (get rates kind)))
          (str "kind " kind " is a typed absence when nothing is eligible"))
      (is (= 0 (:n (get rates kind))))
      (is (not (contains? (get rates kind) :fp-rate)))
      (is (not (contains? (get rates kind) :fn-rate))))))

(deftest per-kind-counts-pinned-verbatim
  (let [rates (cer/measured-rates (read-ledger) (read-classification))]
    (is (= {} (::cer/excluded-ids rates))
        "the classification admits all 22 rows: every row names an
        independent act; none is self-truthed (a finding, not a default)")
    (is (= "kimi-4" (get-in rates [::cer/classification-source :classifier])))
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
  (let [rates (cer/measured-rates (read-ledger) (read-classification))]
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

(deftest claude-8-review-bad-case-verbatim
  (testing "six rows with :truth true :passed true and :truth-source \"the
  check itself passed, so it held\" are NOT eligible — free text is not a
  kind (the exact case claude-8 ran against cc831860, where n = 6 and
  nothing was excluded)"
    (let [bad-rows (vec (repeat 6 {:id :s1 :kind :selfcheck :check "c" :input "i"
                                   :truth true :passed true
                                   :truth-source "the check itself passed, so it held"}))
          bad-rows (mapv #(assoc %1 :id (keyword (str "s" (inc %2))))
                         bad-rows (range 6))
          ledger {:schema :m-futon-seams/check-ledger-v1 :rows bad-rows}]
      (testing "unclassified (no kind anywhere): excluded :unclassified"
        (let [rates (cer/measured-rates ledger)]
          (is (= 6 (count (::cer/excluded-ids rates))))
          (is (every? #(= :unclassified %) (vals (::cer/excluded-ids rates))))
          (is (= :insufficient (:status (:selfcheck rates))))
          (is (= 0 (:n (:selfcheck rates))))))
      (testing "classified :self-truthed: excluded :self-truthed"
        (let [classification {:classification-source {:classifier "test"}
                              :s1 :self-truthed :s2 :self-truthed :s3 :self-truthed
                              :s4 :self-truthed :s5 :self-truthed :s6 :self-truthed}
              rates (cer/measured-rates ledger classification)]
          (is (= {:s1 :self-truthed :s2 :self-truthed :s3 :self-truthed
                  :s4 :self-truthed :s5 :self-truthed :s6 :self-truthed}
                 (::cer/excluded-ids rates)))
          (is (= :insufficient (:status (:selfcheck rates))))
          (is (= 0 (:n (:selfcheck rates)))))))))

(deftest truth-kind-on-row-wins-over-map
  (let [ledger {:schema :m-futon-seams/check-ledger-v1
                :rows [{:id :r1 :kind :test :check "c" :input "i"
                        :truth true :passed true
                        :truth-kind :constructed-bad-case}]}
        rates (cer/measured-rates ledger nil)]
    (is (= {} (::cer/excluded-ids rates))
        "a :truth-kind on the row itself makes it eligible without a map")
    (is (= 1 (:n (:test rates))))))

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

(deftest measured-rates-round-trip-accepted
  (let [ledger (read-ledger)
        classification (read-classification)]
    (is (true? (cer/rates-measured? (cer/measured-rates ledger classification)
                                    ledger)))))

(deftest wrong-schema-ledger-throws-typed
  (is (thrown? clojure.lang.ExceptionInfo
               (cer/measured-rates {:schema :something/else :rows []}))))
