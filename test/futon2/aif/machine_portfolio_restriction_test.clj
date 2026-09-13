(ns futon2.aif.machine-portfolio-restriction-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-budget-authority :as authority]
            [futon2.aif.machine-portfolio-restriction :as restriction])
  (:import (java.nio.file Files)
           (java.security MessageDigest)))

(def fixture-root
  "holes/labs/wm-contract/runs/row-22-e1-authority-resolution-2026-09-13/fixtures")

(def files
  {:ranked-support "ranked-support.edn"
   :field-membership "field-membership.edn"
   :costs "costs.edn" :utilities "utilities.edn" :budgets "budgets.edn"})

(defn- hex [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))

(defn- digest [file]
  (hex (.digest (doto (MessageDigest/getInstance "SHA-256")
                  (.update (Files/readAllBytes (.toPath (io/file file))))))))

(defn- config []
  {:resolver/version authority/resolver-version
   :mode :isolated-test :root fixture-root
   :sources (into {} (map (fn [[label filename]]
                            [label {:relative-path filename
                                    :sha256 (digest (io/file fixture-root filename))}]))
                  files)})

(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(defn- with-e1 [e1 f]
  (with-redefs [authority/resolve-and-map (fn [_] e1)] (f)))

(deftest canonical-portfolio-restricts-full-support-by-occurrence
  (let [out (restriction/restrict-portfolio (config))
        full (:full-support out)]
    (is (= :isolated-test (:scope out)))
    (is (= 3 (count full)))
    (is (= (:action (full 0)) (:action (full 1))))
    (is (not= (:candidate/id (full 0)) (:candidate/id (full 1))))
    (is (= [[:e1-authority-run 4 0] [:e1-authority-run 4 2]]
           (:approved-occurrence-ids out)))
    (is (= [[:e1-authority-run 4 1]] (:excluded-occurrence-ids out)))
    (is (= (mapv :action [(full 0) (full 2)])
           (mapv :action (:approved-support out))))
    (is (:replay/identical? (restriction/replay (:replay/receipt out))))))

(deftest caller-selected-ids-are-not-an-input
  (let [out (restriction/restrict-portfolio
             (assoc (config) :selected-ids #{[:forged 0]} :scope :production))]
    (is (= [[:e1-authority-run 4 0] [:e1-authority-run 4 2]]
           (:approved-occurrence-ids out)))
    (is (= :isolated-test (:scope out)))))

(deftest malformed-canonical-output-refuses-closed
  (let [e1 (authority/resolve-and-map (config))
        selected (get-in e1 [:response :selected])
        rejected (get-in e1 [:response :rejected])]
    (testing "empty approved support"
      (is (= :e2a/empty-approved-support
             (with-e1 (-> e1
                          (assoc-in [:response :selected] [])
                          (assoc-in [:response :selected-ids] #{})
                          (assoc-in [:response :rejected] (vec (concat selected rejected)))
                          (update :accounting #(mapv (fn [r] (assoc r :disposition :rejected)) %)))
               #(restriction/restrict-portfolio (config))))))
    (testing "omitted occurrence"
      (is (= :e2a/omitted-portfolio-id
             (with-e1 (update-in e1 [:response :rejected] pop)
               #(restriction/restrict-portfolio (config))))))
    (testing "unknown occurrence"
      (is (= :e2a/unknown-portfolio-id
             (with-e1 (update-in e1 [:response :selected]
                                 conj {:id [:unknown 9] :action {:type :no-op}
                                       :proposal/action {:type :no-op} :rank 9})
               #(restriction/restrict-portfolio (config))))))
    (testing "duplicate selected occurrence"
      (is (= :e2a/duplicate-selected-id
             (with-e1 (update-in e1 [:response :selected] conj (first selected))
               #(restriction/restrict-portfolio (config))))))
    (testing "selected/rejected conflict"
      (is (= :e2a/conflicting-disposition
             (with-e1 (update-in e1 [:response :rejected] conj (first selected))
               #(restriction/restrict-portfolio (config))))))))

(deftest forged-accounting-and-action-mutation-refuse
  (let [e1 (authority/resolve-and-map (config))]
    (is (= :e2a/forged-accounting
           (with-e1 (assoc-in e1 [:accounting 0 :disposition] :rejected)
             #(restriction/restrict-portfolio (config)))))
    (is (= :e2a/action-mutation
           (with-e1 (assoc-in e1 [:response :selected 0 :action :target] "forged")
             #(restriction/restrict-portfolio (config)))))
    (is (= :e2a/accounting-order-mismatch
           (with-e1 (update e1 :accounting #(vec (reverse %)))
             #(restriction/restrict-portfolio (config)))))))

(deftest missing-verification-and-replay-mutation-refuse-or-diverge
  (let [out (restriction/restrict-portfolio (config))
        e1 (authority/resolve-and-map (config))]
    (is (= :e2a/e1-unverified
           (with-e1 (dissoc e1 :verification)
             #(restriction/restrict-portfolio (config)))))
    (is (false? (:replay/identical?
                 (restriction/replay
                  (assoc-in (:replay/receipt out)
                            [:output :approved-support 0 :action :target] "mutated")))))))
