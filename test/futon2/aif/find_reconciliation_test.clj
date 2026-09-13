(ns futon2.aif.find-reconciliation-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.find-reconciliation :as reconciliation]))

(def recorded
  (edn/read-string
   (slurp "holes/labs/wm-contract/runs/F11-find/01-find-snatch-live.edn")))

(defn shift-lines [snapshot]
  (update snapshot :scenarios
          (fn [scenarios]
            (mapv (fn [scenario]
                    (update scenario :round-results
                            (fn [rounds]
                              (mapv (fn [round]
                                      (update-in round [:find :receipts]
                                                 (fn [receipts]
                                                   (into {} (map (fn [[id receipt]]
                                                                   [id (update-in receipt [:warrant :if-lines]
                                                                                  #(mapv inc %))])
                                                                 receipts)))))
                                    rounds))))
                  scenarios))))

(deftest recorded-population-and-order
  (let [shifted (shift-lines recorded)
        reordered (update shifted :scenarios
                          #(mapv (fn [s] (update s :round-results (comp vec reverse)))
                                 (reverse %)))
        expected (reconciliation/report recorded shifted)]
    (is (= 24 (count (:repository recorded))))
    (is (= 96 (:receipts-compared expected)))
    (is (true? (:difference-is-line-coordinates-only? expected)))
    (is (= expected (reconciliation/report recorded reordered)))
    (is (false? (:difference-is-line-coordinates-only?
                 (reconciliation/report recorded recorded))))))

(deftest structural-loss-and-duplication-cannot-hide-behind-lines
  (let [shifted (shift-lines recorded)
        receipt-id (first (keys (get-in shifted [:scenarios 0 :round-results 0 :find :receipts])))]
    (doseq [[kind mutated]
            [[:scenarios (update shifted :scenarios #(vec (rest %)))]
             [:rounds (update-in shifted [:scenarios 0 :round-results] #(vec (rest %)))]
             [:receipts (update-in shifted [:scenarios 0 :round-results 0 :find :receipts]
                                   dissoc receipt-id)]
             [:scenarios (update shifted :scenarios #(conj % (first %)))]
             [:rounds (update-in shifted [:scenarios 0 :round-results] #(conj % (first %)))]]]
      (doseq [[pin live] [[recorded mutated] [mutated recorded]]]
        (let [report (reconciliation/report pin live)]
          (is (false? (:difference-is-line-coordinates-only? report)))
          (is (false? (:receipt-populations-identical? report)))
          (is (some #(= kind (:kind %)) (:structural-differences report))))))))

(deftest clause-text-control
  (let [shifted (shift-lines recorded)
        id (first (keys (get-in shifted [:scenarios 0 :round-results 0 :find :receipts])))
        mutated (assoc-in shifted [:scenarios 0 :round-results 0 :find :receipts id
                                   :warrant :if-text] "not the authored clause")
        result (reconciliation/report recorded mutated)]
    (is (true? (:receipt-populations-identical? result)))
    (is (false? (:difference-is-line-coordinates-only? result)))
    (is (= 1 (get-in result [:differing-fields :warrant/if-text])))))

(deftest certificate-carries-a-falsifiable-verdict-with-identity
  (let [rep (assoc (reconciliation/report recorded recorded)
                   :pin-path "futon3:checks/find-snatch.edn"
                   :pin-sha256 "aa11" :live-path "runs/x.edn" :live-sha256 "bb22")
        cert (reconciliation/certificate rep {:run-id "test-run" :generated-at "t0"})]
    (is (= :wm/f2-reconciliation-certificate-v1 (:schema cert)))
    (is (true? (:records-reconcile? cert)))
    (is (= "bb22" (:live-sha256 cert)))
    (is (= 96 (:receipts-compared cert)))
    (is (zero? (:receipts-differing cert))))
  (let [mutated (update-in recorded [:scenarios 0 :round-results 0 :find :receipts]
                           (fn [receipts]
                             (let [[id receipt] (first receipts)]
                               (assoc receipts id
                                      (assoc-in receipt [:warrant :if-text] "another clause")))))
        rep (assoc (reconciliation/report recorded mutated)
                   :pin-path "p" :pin-sha256 "aa11" :live-path "l" :live-sha256 "cc33")]
    (is (false? (:records-reconcile?
                 (reconciliation/certificate rep {:run-id "test-run" :generated-at "t0"})))
        "a clause-text difference makes the certificate verdict false")))

(deftest certificate-refuses-unverifiable-input
  (let [rep (reconciliation/report recorded recorded)]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"identity fields"
                          (reconciliation/certificate rep {:run-id "r" :generated-at "t"}))
        "a report without digests cannot certify")
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"run identity"
                          (reconciliation/certificate
                           (assoc rep :pin-sha256 "a" :live-sha256 "b" :pin-path "p" :live-path "l")
                           {:generated-at "t"})))))

(deftest certificate-drift-ignores-only-volatile-identity
  (let [rep (assoc (reconciliation/report recorded recorded)
                   :pin-path "p" :pin-sha256 "aa" :live-path "l" :live-sha256 "bb")
        cert (reconciliation/certificate rep {:run-id "r1" :generated-at "t1"})
        recert (reconciliation/certificate rep {:run-id "r2" :generated-at "t2"})]
    (is (empty? (reconciliation/certificate-drift cert recert))
        "re-certification of the same records under a new run identity is not drift")
    (let [moved (assoc recert :live-sha256 "cc" :records-reconcile? false)
          drift (reconciliation/certificate-drift cert moved)]
      (is (= #{:live-sha256 :records-reconcile?} (set (map :field drift))))
      (is (= "bb" (:committed (first (filter #(= :live-sha256 (:field %)) drift))))))))
